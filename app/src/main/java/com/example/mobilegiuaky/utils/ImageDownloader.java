package com.example.mobilegiuaky.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ⚠️ BUG INTENTIONAL - MAIN THREAD BLOCKING ⚠️
 * 
 * This class downloads images on the MAIN THREAD,
 * causing ANR (Application Not Responding) and UI freezing.
 */
public class ImageDownloader {
    
    private static final String TAG = "ImageDownloader";
    
    /**
     * ⚠️ BAD: Download image on Main Thread
     * This will cause ANR and UI freeze!
     */
    public static Bitmap downloadOnMainThread(String imageUrl) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();
            
            inputStream = connection.getInputStream();
            
            // ⚠️ BAD: Decoding bitmap on main thread
            bitmap = BitmapFactory.decodeStream(inputStream);
            
            // ⚠️ EXTRA BAD: Add artificial delay to make it worse
            Thread.sleep(2000);
            
            Log.d(TAG, "Image downloaded successfully on MAIN THREAD (BAD!)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return bitmap;
    }
    
    /**
     * ⚠️ BAD: Save file on Main Thread with heavy processing
     */
    public static boolean saveImageOnMainThread(Context context, Bitmap bitmap, String fileName) {
        if (bitmap == null) return false;
        
        try {
            File directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null) {
                directory = context.getFilesDir();
            }
            
            File file = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            
            // ⚠️ BAD: Heavy compression on main thread
            // Compression quality 100 = maximum quality = most CPU intensive
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            
            // ⚠️ EXTRA BAD: Artificial delay
            Thread.sleep(1000);
            
            fos.flush();
            fos.close();
            
            Log.d(TAG, "Image saved to: " + file.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ GOOD: Download image on background thread (FIXED VERSION)
     */
    public static void downloadOnBackgroundThread(String imageUrl, OnDownloadCompleteListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                HttpURLConnection connection = null;
                InputStream inputStream = null;
                
                try {
                    URL url = new URL(imageUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    connection.connect();
                    
                    inputStream = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    
                    Log.d(TAG, "Image downloaded successfully on BACKGROUND THREAD (GOOD!)");
                    
                    if (listener != null) {
                        final Bitmap finalBitmap = bitmap;
                        listener.onSuccess(finalBitmap);
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error downloading image: " + e.getMessage());
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                        if (connection != null) connection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    public interface OnDownloadCompleteListener {
        void onSuccess(Bitmap bitmap);
        void onError(String error);
    }
}
