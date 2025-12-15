package com.example.mobilegiuaky;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilegiuaky.adapter.PhotoAdapter;
import com.example.mobilegiuaky.api.ApiClient;
import com.example.mobilegiuaky.api.ApiService;
import com.example.mobilegiuaky.model.Photo;
import com.example.mobilegiuaky.utils.HeavyProcessor;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main Activity - Photo Gallery with Performance Issues Demo
 * 
 * This activity demonstrates:
 * - Jank/Lag when scrolling RecyclerView (bad adapter implementation)
 * - High CPU usage when searching/sorting
 * - Toggle between BAD and GOOD implementations
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Components
    private RecyclerView recyclerView;
    private PhotoAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private EditText etSearch;
    private Button btnSort;
    private Button btnRefresh;
    private Button btnStressCpu;
    private SwitchCompat switchBadMode;

    // Data
    private List<Photo> photoList = new ArrayList<>();
    private ApiService apiService;
    private Handler mainHandler;

    // Mode
    private boolean useBadImplementation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize
        mainHandler = new Handler(Looper.getMainLooper());
        apiService = ApiClient.getApiService();

        // Setup UI
        initViews();
        setupRecyclerView();
        setupListeners();

        // Load data
        loadPhotos();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        etSearch = findViewById(R.id.etSearch);
        btnSort = findViewById(R.id.btnSort);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnStressCpu = findViewById(R.id.btnStressCpu);
        switchBadMode = findViewById(R.id.switchBadMode);

        // Set initial mode
        switchBadMode.setChecked(useBadImplementation);
        updateStatusText();
    }

    private void setupRecyclerView() {
        adapter = new PhotoAdapter(this, photoList);
        adapter.setUseBadImplementation(useBadImplementation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Click listener - open detail
        adapter.setOnPhotoClickListener((photo, position) -> {
            Intent intent = new Intent(MainActivity.this, PhotoDetailActivity.class);
            intent.putExtra("photo", photo);
            startActivity(intent);
        });
    }

    private void setupListeners() {
        // Toggle bad/good mode
        switchBadMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useBadImplementation = isChecked;
            adapter.setUseBadImplementation(isChecked);
            adapter.notifyDataSetChanged();
            updateStatusText();

            String mode = isChecked ? "BAD (Laggy)" : "GOOD (Smooth)";
            Toast.makeText(this, "Mode: " + mode, Toast.LENGTH_SHORT).show();
        });

        // Search with TextWatcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Sort button
        btnSort.setOnClickListener(v -> performSort());

        // Refresh button
        btnRefresh.setOnClickListener(v -> loadPhotos());

        // Stress CPU button
        btnStressCpu.setOnClickListener(v -> stressCpu());

        // Admin button
        Button btnAdmin = findViewById(R.id.btnAdmin);
        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(intent);
        });
    }

    private void loadPhotos() {
        showLoading(true);
        tvStatus.setText("Loading photos...");

        apiService.getPhotos().enqueue(new Callback<List<Photo>>() {
            @Override
            public void onResponse(Call<List<Photo>> call, Response<List<Photo>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    photoList = response.body();
                    adapter.updateData(photoList);
                    updateStatusText();
                    Log.d(TAG, "Loaded " + photoList.size() + " photos");
                } else {
                    tvStatus.setText("Error loading photos");
                    Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    
                    // Load demo data if API fails
                    loadDemoData();
                }
            }

            @Override
            public void onFailure(Call<List<Photo>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "API Error: " + t.getMessage());
                tvStatus.setText("Connection error - Loading demo data");
                
                // Load demo data
                loadDemoData();
            }
        });
    }

    /**
     * Load demo data when API is not available
     */
    private void loadDemoData() {
        photoList = new ArrayList<>();
        
        // Generate 50 demo photos with placeholder images
        String[] placeholderUrls = {
            "https://picsum.photos/400/300?random=",
            "https://via.placeholder.com/400x300.png?text=Photo+",
            "https://loremflickr.com/400/300?random="
        };
        
        for (int i = 1; i <= 50; i++) {
            Photo photo = new Photo();
            photo.setId(i);
            photo.setTitle("Photo Title " + i);
            photo.setDescription("This is a detailed description for photo number " + i + 
                    ". It contains some text to demonstrate the app functionality. " +
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
            photo.setImageUrl(placeholderUrls[i % 3] + i);
            photo.setFileName("photo_" + i + ".jpg");
            photo.setFileSizeKb(100 + (i * 10));
            photoList.add(photo);
        }
        
        adapter.updateData(photoList);
        updateStatusText();
        Toast.makeText(this, "Loaded " + photoList.size() + " demo photos", Toast.LENGTH_SHORT).show();
    }

    private void performSearch(String query) {
        long startTime = System.currentTimeMillis();

        if (useBadImplementation) {
            // ⚠️ BAD: Inefficient search on main thread
            adapter.searchBad(query);
        } else {
            // ✅ GOOD: Efficient search
            adapter.searchGood(query);
        }

        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Search took: " + duration + "ms (Bad mode: " + useBadImplementation + ")");
    }

    private void performSort() {
        long startTime = System.currentTimeMillis();
        tvStatus.setText("Sorting...");

        if (useBadImplementation) {
            // ⚠️ BAD: Inefficient bubble sort on main thread
            adapter.sortByTitleBad();
        } else {
            // ✅ GOOD: Efficient sort
            adapter.sortByTitleGood();
        }

        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Sort took: " + duration + "ms (Bad mode: " + useBadImplementation + ")");

        updateStatusText();
        Toast.makeText(this, "Sort completed in " + duration + "ms", Toast.LENGTH_SHORT).show();
    }

    /**
     * ⚠️ Stress CPU - for demo purposes
     */
    private void stressCpu() {
        tvStatus.setText("Stressing CPU...");
        progressBar.setVisibility(View.VISIBLE);

        // ⚠️ BAD: Heavy computation on main thread
        if (useBadImplementation) {
            // Generate and sort large dataset ON MAIN THREAD
            int[] data = HeavyProcessor.generateLargeDataset(2000);
            int[] sorted = HeavyProcessor.inefficientSort(data);
            
            // Heavy string processing
            String result = HeavyProcessor.heavyStringProcessing("Performance Test", 100);
            
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("CPU stress complete (BAD - on main thread)");
            Toast.makeText(this, "Done! Check Profiler for CPU spike", Toast.LENGTH_LONG).show();
        } else {
            // ✅ GOOD: Heavy computation on background thread
            new Thread(() -> {
                int[] data = HeavyProcessor.generateLargeDataset(2000);
                int[] sorted = HeavyProcessor.inefficientSort(data);
                String result = HeavyProcessor.heavyStringProcessing("Performance Test", 100);
                
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("CPU stress complete (GOOD - on background thread)");
                    Toast.makeText(MainActivity.this, "Done on background thread!", Toast.LENGTH_LONG).show();
                });
            }).start();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateStatusText() {
        String mode = useBadImplementation ? "⚠️ BAD MODE (Laggy)" : "✅ GOOD MODE (Smooth)";
        tvStatus.setText(mode + " | Photos: " + photoList.size());
    }

    /**
     * 🔄 REAL-TIME SYNC: Auto-refresh khi app resume
     * Để sync với thay đổi từ Admin app
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Auto refreshing data...");
        loadPhotos();  // Reload từ server để sync với Admin app
    }
}
