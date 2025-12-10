package com.example.mobilegiuaky.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit API Client - Singleton pattern
 */
public class ApiClient {
    
    // Change this to your server IP address
    // For emulator: use 10.0.2.2 (localhost of host machine)
    // For real device: use your computer's local IP (e.g., 192.168.1.x)
    private static final String BASE_URL = "http://10.0.2.2:3000/";
    
    private static Retrofit retrofit = null;
    
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
