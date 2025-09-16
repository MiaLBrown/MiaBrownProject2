import cv2
import zipfile

# Ask the user for a name in the format firstname_lastname
while True:
    file_name_prefix = input("Enter a name in the format firstname_lastname: ").strip()
    if "_" in file_name_prefix and len(file_name_prefix.split("_")) == 2 and all(part.isalpha() for part in file_name_prefix.split("_")):
        # Add trailing underscore automatically
        file_name_prefix += "_"
        break
    print("Invalid format. Please use the format firstname_lastname.")

# Path to the ZIP file
zip_file_path = r"C:\Users\honey\Downloads\archive.zip"

# Open the camera
camera = cv2.VideoCapture(0)

if not camera.isOpened():
    print("Error: Could not open the camera.")
else:
    # Create a ZIP file in append mode
    with zipfile.ZipFile(zip_file_path, 'a') as archive:
        for i in range(76):
            return_value, image = camera.read()
            if not return_value:
                print(f"Error capturing image {i}.")
                continue
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

            # Use the OpenCV provided Haar Cascade for face detection
            face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

            # Detect faces in the input image
            faces = face_cascade.detectMultiScale(gray, 1.3, 4)
            print(f'Number of faces detected: {len(faces)}')

            if len(faces) > 0:
                for j, (x, y, w, h) in enumerate(faces):
                    # Crop the face region from the image
                    face = image[y:y + h, x:x + w]

                    # Resize the face to 160x160 pixels
                    resized_face = cv2.resize(face, (160, 160))

                    # Encode the resized face image as JPEG in memory
                    success, encoded_face = cv2.imencode('.jpg', resized_face)
                    if not success:
                        print(f"Error encoding resized face {i}.")
                        continue

                    # Create a unique filename for each cropped and resized face
                    image_name = f"Faces/Faces/{file_name_prefix}{i}.jpg"
                    
                    # Write the encoded resized face image bytes to the ZIP file
                    archive.writestr(image_name, encoded_face.tobytes())

    print(f"Images saved successfully to {zip_file_path}.")

# Release the camera
camera.release()
