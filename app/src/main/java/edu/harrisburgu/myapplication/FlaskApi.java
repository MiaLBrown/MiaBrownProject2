package edu.harrisburgu.myapplication;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface FlaskApi {
    //POST method for recognizing a face by sending a multipart request
    @Multipart
    @POST("/recognize")//POST endpoint for facial recognition
    Call<RecognitionResponse> recognizeFace(@Part MultipartBody.Part image);//image is sent as a part of the multipart request
}

