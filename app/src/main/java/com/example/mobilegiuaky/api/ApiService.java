package com.example.mobilegiuaky.api;

import com.example.mobilegiuaky.model.Photo;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
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
    
    // Create new photo (Admin only)
    @POST("api/photos")
    Call<Photo> createPhoto(@Body Photo photo);
    
    // Delete photo (Admin only)
    @DELETE("api/photos/{id}")
    Call<ResponseBody> deletePhoto(@Path("id") int id);
    
    // Health check
    @GET("api/health")
    Call<ResponseBody> healthCheck();
    
    // Download image from URL (for demo purposes)
    @GET
    Call<ResponseBody> downloadImage(@Url String imageUrl);
}
