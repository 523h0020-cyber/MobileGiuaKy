package com.example.mobilegiuaky;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilegiuaky.adapter.PhotoAdapter;
import com.example.mobilegiuaky.api.ApiClient;
import com.example.mobilegiuaky.api.ApiService;
import com.example.mobilegiuaky.model.Photo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🔐 ADMIN ACTIVITY - Photo Management
 * Chức năng: Thêm/Xóa ảnh, Test performance
 */
public class AdminActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private static final String TAG = "AdminActivity";

    private RecyclerView recyclerView;
    private PhotoAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvStatus, tvPhotoCount;
    private Button btnAddPhoto, btnAddMultiple, btnDeleteAll, btnRefresh;
    private SwitchCompat switchBadUpdate;

    private List<Photo> photoList = new ArrayList<>();
    private ApiService apiService;
    private Handler mainHandler;
    private boolean useBadUpdate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Panel");
        }

        mainHandler = new Handler(Looper.getMainLooper());
        apiService = ApiClient.getApiService();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadPhotos();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnAddMultiple = findViewById(R.id.btnAddMultiple);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnRefresh = findViewById(R.id.btnRefresh);
        switchBadUpdate = findViewById(R.id.switchBadUpdate);
        switchBadUpdate.setChecked(useBadUpdate);
        updateStatusText();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PhotoAdapter(this, photoList);
        adapter.setOnPhotoClickListener(this);
        adapter.setUseBadImplementation(false);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        switchBadUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useBadUpdate = isChecked;
            updateStatusText();
            Toast.makeText(this, isChecked ? "⚠️ BAD UPDATE" : "✅ GOOD UPDATE", Toast.LENGTH_SHORT).show();
        });

        btnAddPhoto.setOnClickListener(v -> showAddPhotoDialog());
        btnAddMultiple.setOnClickListener(v -> showAddMultipleDialog());
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());
        btnRefresh.setOnClickListener(v -> loadPhotos());
    }

    private void updateStatusText() {
        tvStatus.setText(useBadUpdate ? "⚠️ BAD UPDATE MODE" : "✅ GOOD UPDATE MODE");
    }

    private void loadPhotos() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Loading...");

        apiService.getPhotos().enqueue(new Callback<List<Photo>>() {
            @Override
            public void onResponse(Call<List<Photo>> call, Response<List<Photo>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    photoList.clear();
                    photoList.addAll(response.body());
                    updateList();
                    tvPhotoCount.setText("Total: " + photoList.size() + " photos");
                    updateStatusText();
                } else {
                    tvStatus.setText("Error loading");
                }
            }

            @Override
            public void onFailure(Call<List<Photo>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Error: " + t.getMessage());
            }
        });
    }

    private void showAddPhotoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_photo, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etImageUrl = dialogView.findViewById(R.id.etImageUrl);

        int randomId = new Random().nextInt(1000);
        etTitle.setText("New Photo " + randomId);
        etDescription.setText("Added by admin #" + randomId);
        etImageUrl.setText("https://picsum.photos/800/600?random=" + randomId);

        new AlertDialog.Builder(this)
               .setView(dialogView)
               .setTitle("Add New Photo")
               .setPositiveButton("Add", (dialog, which) -> {
                   String title = etTitle.getText().toString().trim();
                   String description = etDescription.getText().toString().trim();
                   String imageUrl = etImageUrl.getText().toString().trim();
                   if (!title.isEmpty() && !imageUrl.isEmpty()) {
                       createPhoto(title, description, imageUrl);
                   }
               })
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void showAddMultipleDialog() {
        final EditText input = new EditText(this);
        input.setHint("Number (1-50)");
        input.setText("10");

        new AlertDialog.Builder(this)
               .setView(input)
               .setTitle("Bulk Add (Performance Test)")
               .setPositiveButton("Add", (dialog, which) -> {
                   try {
                       int count = Integer.parseInt(input.getText().toString().trim());
                       if (count >= 1 && count <= 50) {
                           createMultiplePhotos(count);
                       }
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void createPhoto(String title, String description, String imageUrl) {
        progressBar.setVisibility(View.VISIBLE);
        Photo newPhoto = new Photo();
        newPhoto.setTitle(title);
        newPhoto.setDescription(description);
        newPhoto.setImageUrl(imageUrl);
        newPhoto.setFileName("photo_" + System.currentTimeMillis() + ".jpg");
        newPhoto.setFileSizeKb(500 + new Random().nextInt(1500));

        long startTime = System.currentTimeMillis();
        apiService.createPhoto(newPhoto).enqueue(new Callback<Photo>() {
            @Override
            public void onResponse(Call<Photo> call, Response<Photo> response) {
                long duration = System.currentTimeMillis() - startTime;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    photoList.add(0, response.body());
                    updateList();
                    tvPhotoCount.setText("Total: " + photoList.size());
                    tvStatus.setText("✅ Added in " + duration + "ms");
                    Toast.makeText(AdminActivity.this, "Photo added!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Photo> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("❌ Error");
            }
        });
    }

    private void createMultiplePhotos(int count) {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("⏳ Creating " + count + " photos...");

        final int[] successCount = {0};
        final int[] failCount = {0};
        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Photo newPhoto = new Photo();
            newPhoto.setTitle("Bulk Photo " + (i + 1));
            newPhoto.setDescription("Admin bulk test #" + (i + 1));
            newPhoto.setImageUrl("https://picsum.photos/800/600?random=" + System.currentTimeMillis() + i);
            newPhoto.setFileName("bulk_" + i + ".jpg");
            newPhoto.setFileSizeKb(500 + new Random().nextInt(1500));

            apiService.createPhoto(newPhoto).enqueue(new Callback<Photo>() {
                @Override
                public void onResponse(Call<Photo> call, Response<Photo> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        successCount[0]++;
                        photoList.add(0, response.body());
                    } else {
                        failCount[0]++;
                    }
                    checkBulkComplete(count, successCount[0], failCount[0], startTime);
                }

                @Override
                public void onFailure(Call<Photo> call, Throwable t) {
                    failCount[0]++;
                    checkBulkComplete(count, successCount[0], failCount[0], startTime);
                }
            });

            try { Thread.sleep(50); } catch (InterruptedException e) {}
        }
    }

    private void checkBulkComplete(int total, int success, int fail, long startTime) {
        if (success + fail >= total) {
            long duration = System.currentTimeMillis() - startTime;
            progressBar.setVisibility(View.GONE);

            long updateStart = System.currentTimeMillis();
            updateList();
            long updateDuration = System.currentTimeMillis() - updateStart;

            tvPhotoCount.setText("Total: " + photoList.size());
            tvStatus.setText(String.format("✅ %d/%d in %dms (UI:%dms)", success, total, duration, updateDuration));
            Toast.makeText(this, String.format("API:%dms, UI:%dms", duration, updateDuration), Toast.LENGTH_LONG).show();
        }
    }

    private void updateList() {
        long startTime = System.nanoTime();
        adapter.notifyDataSetChanged();
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        Log.d(TAG, "List update: " + duration + "ms");
    }

    private void confirmDeleteAll() {
        if (photoList.isEmpty()) return;
        new AlertDialog.Builder(this)
            .setTitle("Delete All?")
            .setMessage("Delete all " + photoList.size() + " photos?")
            .setPositiveButton("Delete", (dialog, which) -> deleteAllPhotos())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteAllPhotos() {
        progressBar.setVisibility(View.VISIBLE);
        List<Photo> photosToDelete = new ArrayList<>(photoList);
        final int total = photosToDelete.size();
        final int[] deletedCount = {0};
        final long startTime = System.currentTimeMillis();

        for (Photo photo : photosToDelete) {
            apiService.deletePhoto(photo.getId()).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    deletedCount[0]++;
                    if (response.isSuccessful()) photoList.remove(photo);
                    checkDeleteComplete(total, deletedCount[0], startTime);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    deletedCount[0]++;
                    checkDeleteComplete(total, deletedCount[0], startTime);
                }
            });
        }
    }

    private void checkDeleteComplete(int total, int deleted, long startTime) {
        if (deleted >= total) {
            long duration = System.currentTimeMillis() - startTime;
            progressBar.setVisibility(View.GONE);
            updateList();
            tvPhotoCount.setText("Total: " + photoList.size());
            tvStatus.setText(String.format("✅ Deleted %d in %dms", deleted, duration));
        }
    }

    @Override
    public void onPhotoClick(Photo photo, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete?")
            .setMessage("Delete \"" + photo.getTitle() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> deleteSinglePhoto(photo, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteSinglePhoto(Photo photo, int position) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.deletePhoto(photo.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    photoList.remove(position);
                    adapter.notifyItemRemoved(position);
                    tvPhotoCount.setText("Total: " + photoList.size());
                    Toast.makeText(AdminActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
