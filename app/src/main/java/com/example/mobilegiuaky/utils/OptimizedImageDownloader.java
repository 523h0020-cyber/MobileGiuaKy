package com.example.mobilegiuaky.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ✅ OPTIMIZED VERSION - Image Downloader
 * 
 * This class demonstrates the CORRECT way to handle:
 * - Network operations (on background thread)
 * - File I/O (on background thread)
 * - UI updates (on main thread via Handler)
 */
public class OptimizedImageDownloader {
    
    private static final String TAG = "OptimizedDownloader";
    
    // Thread pool for background operations
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * ✅ GOOD: Download image on background thread
     */
    public static void downloadImage(String imageUrl, DownloadCallback callback) {
        executor.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                    
                    Log.d(TAG, "✅ Image downloaded successfully on background thread");
                    
                    final Bitmap finalBitmap = bitmap;
                    mainHandler.post(() -> callback.onSuccess(finalBitmap));
                } else {
                    throw new Exception("HTTP Error: " + responseCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Download error: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * ✅ GOOD: Save image on background thread
     */
    public static void saveImage(Context context, Bitmap bitmap, String fileName, SaveCallback callback) {
        executor.execute(() -> {
            try {
                File directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (directory == null) {
                    directory = context.getFilesDir();
                }
                
                File file = new File(directory, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                
                // Use JPEG for photos (smaller file size, faster compression)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                
                fos.flush();
                fos.close();
                
                Log.d(TAG, "✅ Image saved to: " + file.getAbsolutePath());
                
                final String filePath = file.getAbsolutePath();
                mainHandler.post(() -> callback.onSuccess(filePath));
                
            } catch (Exception e) {
                Log.e(TAG, "Save error: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    /**
     * ✅ GOOD: Download and save in one operation
     */
    public static void downloadAndSave(Context context, String imageUrl, String fileName, 
                                        DownloadAndSaveCallback callback) {
        downloadImage(imageUrl, new DownloadCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                saveImage(context, bitmap, fileName, new SaveCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        callback.onComplete(bitmap, filePath);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Save failed: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError("Download failed: " + error);
            }
        });
    }
    
    /**
     * Shutdown executor when app is closing
     */
    public static void shutdown() {
        executor.shutdown();
    }
    
    // Callback interfaces
    public interface DownloadCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String error);
    }
    
    public interface SaveCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }
    
    public interface DownloadAndSaveCallback {
        void onComplete(Bitmap bitmap, String filePath);
        void onError(String error);
    }
}
