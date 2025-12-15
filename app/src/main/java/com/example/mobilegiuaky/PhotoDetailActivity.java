package com.example.mobilegiuaky;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.mobilegiuaky.model.Photo;
import com.example.mobilegiuaky.utils.ImageDownloader;
import com.example.mobilegiuaky.utils.OptimizedImageDownloader;
import com.example.mobilegiuaky.utils.LeakyManager;

/**
 * Photo Detail Activity - Demonstrates Memory Leaks and Main Thread Blocking
 * 
 * This activity demonstrates:
 * - Memory Leak via Singleton holding Activity Context
 * - Memory Leak via unregistered Listener/Callback
 * - Main Thread blocking when downloading images
 * - LeakCanary detection
 */
public class PhotoDetailActivity extends AppCompatActivity implements LeakyManager.OnDataLoadedListener {

    private static final String TAG = "PhotoDetailActivity";

    // UI Components
    private ImageView ivPhotoLarge;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvFileInfo;
    private TextView tvStatus;
    private Button btnDownloadBad;
    private Button btnDownloadGood;
    private Button btnCauseLeakButton;
    private ProgressBar progressBar;
    private SwitchCompat switchLeakMode;

    // Data
    private Photo photo;
    private Handler mainHandler;

    // ⚠️ Memory Leak flags
    private boolean leakModeEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_photo_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // Get photo from intent
        photo = (Photo) getIntent().getSerializableExtra("photo");

        initViews();
        setupListeners();
        displayPhotoData();

        // ⚠️ BUG: Initialize LeakyManager with Activity Context (causes LEAK)
        if (leakModeEnabled) {
            causeMemoryLeak();
        }
    }

    private void initViews() {
        ivPhotoLarge = findViewById(R.id.ivPhotoLarge);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvFileInfo = findViewById(R.id.tvFileInfo);
        tvStatus = findViewById(R.id.tvStatus);
        btnDownloadBad = findViewById(R.id.btnDownloadBad);
        btnDownloadGood = findViewById(R.id.btnDownloadGood);
        btnCauseLeakButton = findViewById(R.id.btnCauseLeak);
        progressBar = findViewById(R.id.progressBar);
        switchLeakMode = findViewById(R.id.switchLeakMode);

        switchLeakMode.setChecked(leakModeEnabled);
    }

    private void setupListeners() {
        // Switch leak mode
        switchLeakMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            leakModeEnabled = isChecked;
            String mode = isChecked ? "⚠️ LEAK MODE ON" : "✅ LEAK MODE OFF";
            tvStatus.setText(mode);
            Toast.makeText(this, mode, Toast.LENGTH_SHORT).show();
        });

        // ⚠️ BAD: Download on Main Thread
        btnDownloadBad.setOnClickListener(v -> downloadImageBad());

        // ✅ GOOD: Download on Background Thread
        btnDownloadGood.setOnClickListener(v -> downloadImageGood());

        // Button to manually cause leak
        btnCauseLeakButton.setOnClickListener(v -> {
            causeMemoryLeak();
            Toast.makeText(this, "Memory leak caused! Check LeakCanary", Toast.LENGTH_LONG).show();
        });
    }

    private void displayPhotoData() {
        if (photo == null) {
            tvTitle.setText("No photo data");
            return;
        }

        tvTitle.setText(photo.getTitle());
        tvDescription.setText(photo.getDescription());
        tvFileInfo.setText(String.format("File: %s | Size: %d KB", 
                photo.getFileName(), photo.getFileSizeKb()));

        // Load image with Glide
        Glide.with(this)
                .load(photo.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivPhotoLarge);
    }

    /**
     * ⚠️ BUG INTENTIONAL - MEMORY LEAK
     * 
     * This method causes memory leak by:
     * 1. Storing Activity Context in Singleton
     * 2. Registering listener that won't be unregistered
     * 3. Starting delayed task that holds reference
     */
    private void causeMemoryLeak() {
        // ⚠️ LEAK 1: Singleton holds Activity Context
        LeakyManager.getInstance().init(this); // Should use getApplicationContext()
        
        // ⚠️ LEAK 2: Listener registration without cleanup
        LeakyManager.getInstance().setOnDataLoadedListener(this);
        
        // ⚠️ LEAK 3: Delayed callback that may execute after Activity destroyed
        LeakyManager.getInstance().loadDataWithDelay();
        
        Log.w(TAG, "⚠️ MEMORY LEAK CAUSED! Activity context stored in Singleton");
        tvStatus.setText("⚠️ Memory Leak Active - Check LeakCanary");
    }

    /**
     * ⚠️ BAD: Download image on Main Thread
     * This will freeze the UI completely!
     */
    private void downloadImageBad() {
        if (photo == null || photo.getImageUrl() == null) {
            Toast.makeText(this, "No image URL", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("⚠️ Downloading on MAIN THREAD - UI will freeze!");
        progressBar.setVisibility(View.VISIBLE);

        // ⚠️ BAD: Network operation on main thread
        // This will cause ANR (Application Not Responding)!
        Bitmap bitmap = ImageDownloader.downloadOnMainThread(photo.getImageUrl());

        if (bitmap != null) {
            ivPhotoLarge.setImageBitmap(bitmap);
            
            // ⚠️ BAD: Save on main thread too
            boolean saved = ImageDownloader.saveImageOnMainThread(this, bitmap, 
                    "downloaded_" + photo.getId() + ".png");
            
            tvStatus.setText(saved ? "Downloaded & saved (BAD)" : "Download failed");
        } else {
            tvStatus.setText("Download failed");
        }

        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Download complete (BAD way - UI was frozen!)", Toast.LENGTH_LONG).show();
    }

    /**
     * ✅ GOOD: Download image on Background Thread
     */
    private void downloadImageGood() {
        if (photo == null || photo.getImageUrl() == null) {
            Toast.makeText(this, "No image URL", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("✅ Downloading on BACKGROUND THREAD...");
        progressBar.setVisibility(View.VISIBLE);

        ImageDownloader.downloadOnBackgroundThread(photo.getImageUrl(), 
            new ImageDownloader.OnDownloadCompleteListener() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    mainHandler.post(() -> {
                        if (!isDestroyed() && !isFinishing()) {
                            ivPhotoLarge.setImageBitmap(bitmap);
                            // Save to Downloads using OptimizedImageDownloader
                            String fileName = "downloaded_" + photo.getId() + ".jpg";
                            OptimizedImageDownloader.downloadAndSave(PhotoDetailActivity.this, photo.getImageUrl(), fileName,
                                    new OptimizedImageDownloader.DownloadAndSaveCallback() {
                                        @Override
                                        public void onComplete(Bitmap bm, String filePath) {
                                            if (!isDestroyed() && !isFinishing()) {
                                                progressBar.setVisibility(View.GONE);
                                                tvStatus.setText("✅ Downloaded & saved: " + fileName);
                                                Toast.makeText(PhotoDetailActivity.this,
                                                        "Saved to Downloads: " + fileName, Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onError(String error) {
                                            if (!isDestroyed() && !isFinishing()) {
                                                progressBar.setVisibility(View.GONE);
                                                tvStatus.setText("Save error: " + error);
                                                Toast.makeText(PhotoDetailActivity.this,
                                                        "Save error: " + error, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        if (!isDestroyed() && !isFinishing()) {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Download error: " + error);
                            Toast.makeText(PhotoDetailActivity.this, 
                                    "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
    }

    /**
     * Callback from LeakyManager (demonstrates leak)
     */
    @Override
    public void onDataLoaded(String data) {
        // This may be called after Activity is destroyed!
        Log.d(TAG, "Data loaded callback: " + data);
        if (!isDestroyed() && !isFinishing()) {
            tvStatus.setText("Callback received: " + data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // ⚠️ BUG: We intentionally DON'T cleanup to demonstrate leak
        // In real code, you should call:
        // LeakyManager.getInstance().cleanup();
        
        if (!leakModeEnabled) {
            // Only cleanup if leak mode is disabled
            LeakyManager.getInstance().cleanup();
            Log.d(TAG, "✅ Cleaned up LeakyManager");
        } else {
            Log.w(TAG, "⚠️ NOT cleaning up - Memory Leak will occur!");
        }
    }
}
