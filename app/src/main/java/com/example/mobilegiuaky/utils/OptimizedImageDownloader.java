package com.example.mobilegiuaky.utils;
import android.provider.MediaStore;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.content.ContentValues;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.net.Uri;
import android.content.ContentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
                String mimeType = "image/jpeg";
                OutputStream fos;
                String savedPath = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+ (API 29+): Use MediaStore (DOWNLOADS)
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    values.put(MediaStore.Downloads.IS_PENDING, 1);
                    ContentResolver resolver = context.getContentResolver();
                    Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new Exception("Failed to create new MediaStore record");
                    fos = resolver.openOutputStream(uri);
                    if (fos == null) throw new Exception("Failed to open output stream");
                    // Save bitmap
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.flush();
                    fos.close();
                    // Mark as not pending
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);
                    savedPath = uri.toString();
                } else {
                    // Sử dụng thư mục Download công khai thay vì thư mục riêng của app
                    File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                    // Tạo thư mục nếu chưa tồn tại
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    File file = new File(directory, fileName);
                    fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();

                    savedPath = file.getAbsolutePath();

                    // QUAN TRỌNG: Báo cho hệ thống biết có file mới để nó quét và hiển thị ngay
                    // Nếu không có dòng này, file có thể không hiện lên ngay lập tức
                    android.media.MediaScannerConnection.scanFile(context,
                            new String[]{file.toString()}, null, null);
                }
                Log.d(TAG, "✅ Image saved to: " + savedPath);
                final String filePath = savedPath;
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
