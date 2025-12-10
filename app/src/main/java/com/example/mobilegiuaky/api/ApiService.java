package com.example.mobilegiuaky.api;

import com.example.mobilegiuaky.model.Photo;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

/**
 * Retrofit API Service interface
 */
public interface ApiService {
    
    // Get all photos
    @GET("api/photos")
    Call<List<Photo>> getPhotos();
    
    // Get single photo by ID
    @GET("api/photos/{id}")
    Call<Photo> getPhotoById(@Path("id") int id);
    
    // Download image from URL (for demo purposes)
    @GET
    Call<ResponseBody> downloadImage(@Url String imageUrl);
}
