package edu.harrisburgu.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 100;
    private static final String TAG = "MainActivity";

    private EditText editTextName;
    private TextureView textureView;
    private String cameraID;
    private Uri imageUri;  // Declare the imageUri here to resolve the "Cannot resolve symbol" issue

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextName = findViewById(R.id.edit_text_name);
        Button savePersonButton = findViewById(R.id.button_save);
        Button StartCamera = findViewById(R.id.buttonStartCamera);

        // Launch camera to capture an image
        StartCamera.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, REQUEST_PERMISSION);
        });

        // Set up TextureView for camera preview
        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(textureListener);

        // Request permissions and start camera
        checkPermissions();

        savePersonButton.setOnClickListener(this::savePerson);
    }

    private void checkPermissions() {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        // Get camera manager to access camera hardware
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            // Loop through available cameras and select the front camera
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraID = cameraId;// Assign camera ID of front camera
                    break;
                }
            }
            // If no front camera found, show error
            if (cameraID == null) {
                Toast.makeText(this, "No front camera found", Toast.LENGTH_SHORT).show();
                return;
            }
            // Open the front camera if permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraID, stateCallback, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access failed", e);
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Set up camera preview once camera is opened
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture != null) {
                texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
                Surface surface = new Surface(texture);

                try {
                    CaptureRequest.Builder requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    requestBuilder.addTarget(surface);

                    camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(requestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Preview failed", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera configuration failed");
                        }
                    }, null);

                } catch (CameraAccessException e) {
                    Log.e(TAG, "Camera configuration error", e);
                }
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();// Close camera when disconnected
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            camera.close();// Close camera on error
        }
    };

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            startCamera();// Start camera when texture is available
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    private void savePerson(View view) {
        // Get and validate the name entered by the user
        String name = editTextName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure that an image has been selected
        if (imageUri == null) {
            Log.e("SavePerson", "Image URI is null!");
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get image file path and proceed to upload
        String imagePath = getRealPathFromURI(imageUri);
        if (imagePath == null) {
            Log.e("SavePerson", "Image path is null!");
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SavePerson", "Image saved to: " + imagePath);

        // Convert the image path to a File object
        File imageFile = new File(imagePath);

        // Upload the image to the server
        uploadImage(imageFile);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION && resultCode == RESULT_OK) {
            // Check if data is not null
            if (data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data"); // Get the bitmap from the camera intent
                if (imageBitmap != null) {
                    // Save the image to a file and get the file path
                    String imagePath = saveImage(imageBitmap);
                    if (imagePath != null) {
                        // Assign imageUri after saving the image
                        imageUri = Uri.fromFile(new File(imagePath));

                        // Now you can pass imagePath to upload
                        File imageFile = new File(imagePath);
                        uploadImage(imageFile);
                    } else {
                        Log.e("MainActivity", "Failed to save image");
                    }
                }
            }
        }
    }

    private String saveImage(Bitmap bitmap) {
        File storageDir = new File(getExternalFilesDir(null), "captured_images");
        if (!storageDir.exists()) {
            // Use mkdirs() to ensure the directory is created
            if (!storageDir.mkdirs()) {
                Log.e("MainActivity", "Failed to create directory");
                return null;
            }
        }

        // Save image file
        File imageFile = new File(storageDir, "captured_image.jpg");
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("MainActivity", "Error saving image", e);
            return null;
        }
    }

    // Helper method to get the real path from URI
    public String getRealPathFromURI(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file path from URI", e);
        }
        return null;
    }

    private void uploadImage(File imageFile) {
        Log.d("UploadImage", "Uploading image from path: " + imageFile);

        // Retrofit instance to make API calls
        //Retrofit retrofit = RetrofitClient.getClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.0.202:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        FlaskApi apiService = retrofit.create(FlaskApi.class); // Create the API service

        // Prepare the image file to send in the request
        if (!imageFile.exists()) {
            Log.e("UploadImage", "File does not exist: " + imageFile);
            return;
        }

        // Creating request body for the image
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        apiService.recognizeFace(imagePart).enqueue(new Callback<RecognitionResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecognitionResponse> call, @NonNull Response<RecognitionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String recognizedName = response.body().getName();
                    Toast.makeText(MainActivity.this, "Recognized name: " + recognizedName, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("UploadImage", "Recognition failed");
                    Toast.makeText(MainActivity.this, "Recognition failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecognitionResponse> call, @NonNull Throwable t) {
                Log.e("UploadImage", "Error during request", t);
            }
        });

    }
}
