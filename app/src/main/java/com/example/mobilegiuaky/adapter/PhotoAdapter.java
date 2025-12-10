package com.example.mobilegiuaky.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobilegiuaky.R;
import com.example.mobilegiuaky.model.Photo;
import com.example.mobilegiuaky.utils.HeavyProcessor;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ⚠️ BUG INTENTIONAL - JANK/LAG IN RECYCLERVIEW ⚠️
 * 
 * This adapter contains multiple performance issues that cause:
 * - Dropped frames (jank)
 * - UI stuttering when scrolling
 * - Low FPS
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    
    private static final String TAG = "PhotoAdapter";
    
    private List<Photo> photoList;
    private List<Photo> originalList; // For search/filter
    private Context context;
    private OnPhotoClickListener listener;
    
    // ⚠️ Toggle this to switch between BAD and GOOD implementation
    private boolean useBadImplementation = true;
    
    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo, int position);
    }
    
    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList != null ? photoList : new ArrayList<>();
        this.originalList = new ArrayList<>(this.photoList);
    }
    
    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.listener = listener;
    }
    
    public void setUseBadImplementation(boolean useBad) {
        this.useBadImplementation = useBad;
    }
    
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photoList.get(position);
        
        if (useBadImplementation) {
            bindViewHolderBad(holder, photo);
        } else {
            bindViewHolderGood(holder, photo);
        }
        
        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(photo, position);
            }
        });
    }
    
    /**
     * ⚠️ BAD IMPLEMENTATION - Causes JANK/LAG
     */
    private void bindViewHolderBad(PhotoViewHolder holder, Photo photo) {
        // Set text
        holder.tvTitle.setText(photo.getTitle());
        holder.tvDescription.setText(photo.getDescription());
        holder.tvFileSize.setText(photo.getFileSizeKb() + " KB");
        
        // ⚠️ BUG 1: Heavy string processing on MAIN THREAD
        String processedTitle = HeavyProcessor.heavyStringProcessing(photo.getTitle(), 50);
        Log.d(TAG, "Processed title length: " + processedTitle.length());
        
        // ⚠️ BUG 2: Sorting array on MAIN THREAD in onBindViewHolder
        int[] randomData = HeavyProcessor.generateLargeDataset(500);
        int[] sortedData = HeavyProcessor.inefficientSort(randomData);
        Log.d(TAG, "Sorted data, first element: " + sortedData[0]);
        
        // ⚠️ BUG 3: Download image on MAIN THREAD (blocks UI)
        // This is extremely bad - never do network on main thread!
        try {
            if (photo.getImageUrl() != null && !photo.getImageUrl().isEmpty()) {
                Bitmap bitmap = downloadImageSync(photo.getImageUrl());
                if (bitmap != null) {
                    // ⚠️ BUG 4: Heavy image processing on main thread
                    Bitmap processed = HeavyProcessor.heavyImageProcessing(bitmap);
                    holder.ivPhoto.setImageBitmap(processed != null ? processed : bitmap);
                } else {
                    holder.ivPhoto.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            holder.ivPhoto.setImageResource(R.drawable.ic_launcher_foreground);
        }
        
        // ⚠️ BUG 5: Creating new objects unnecessarily
        for (int i = 0; i < 100; i++) {
            String waste = new String("Wasted memory " + i);
            StringBuilder sb = new StringBuilder(waste);
        }
    }
    
    /**
     * ✅ GOOD IMPLEMENTATION - Smooth scrolling
     */
    private void bindViewHolderGood(PhotoViewHolder holder, Photo photo) {
        // Set text directly (no heavy processing)
        holder.tvTitle.setText(photo.getTitle());
        holder.tvDescription.setText(photo.getDescription());
        holder.tvFileSize.setText(photo.getFileSizeKb() + " KB");
        
        // ✅ Use Glide for efficient image loading
        Glide.with(context)
                .load(photo.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(holder.ivPhoto);
    }
    
    /**
     * ⚠️ BAD: Synchronous image download on calling thread
     */
    private Bitmap downloadImageSync(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public int getItemCount() {
        return photoList != null ? photoList.size() : 0;
    }
    
    /**
     * ⚠️ BAD: Inefficient search implementation
     */
    public void searchBad(String query) {
        if (query == null || query.isEmpty()) {
            photoList = new ArrayList<>(originalList);
            notifyDataSetChanged();
            return;
        }
        
        // ⚠️ BAD: Create new list every time
        List<Photo> filteredList = new ArrayList<>();
        
        // ⚠️ BAD: Multiple iterations and string operations
        for (int repeat = 0; repeat < 10; repeat++) { // Unnecessary repetition
            for (Photo photo : originalList) {
                // ⚠️ BAD: Creating new strings in loop
                String title = photo.getTitle().toLowerCase();
                String desc = photo.getDescription().toLowerCase();
                String q = query.toLowerCase();
                
                // ⚠️ BAD: Heavy processing for each item
                HeavyProcessor.heavyStringProcessing(title, 20);
                
                if (title.contains(q) || desc.contains(q)) {
                    if (!filteredList.contains(photo)) {
                        filteredList.add(photo);
                    }
                }
            }
        }
        
        photoList = filteredList;
        notifyDataSetChanged();
    }
    
    /**
     * ✅ GOOD: Efficient search implementation
     */
    public void searchGood(String query) {
        if (query == null || query.isEmpty()) {
            photoList = new ArrayList<>(originalList);
            notifyDataSetChanged();
            return;
        }
        
        String lowerQuery = query.toLowerCase();
        List<Photo> filteredList = new ArrayList<>();
        
        for (Photo photo : originalList) {
            if (photo.getTitle().toLowerCase().contains(lowerQuery) ||
                photo.getDescription().toLowerCase().contains(lowerQuery)) {
                filteredList.add(photo);
            }
        }
        
        photoList = filteredList;
        notifyDataSetChanged();
    }
    
    /**
     * ⚠️ BAD: Inefficient sort
     */
    public void sortByTitleBad() {
        // ⚠️ BAD: Bubble sort O(n²)
        for (int i = 0; i < photoList.size() - 1; i++) {
            for (int j = 0; j < photoList.size() - i - 1; j++) {
                // ⚠️ BAD: Heavy processing in comparison
                HeavyProcessor.heavyStringProcessing(photoList.get(j).getTitle(), 10);
                
                if (photoList.get(j).getTitle().compareTo(photoList.get(j + 1).getTitle()) > 0) {
                    Photo temp = photoList.get(j);
                    photoList.set(j, photoList.get(j + 1));
                    photoList.set(j + 1, temp);
                }
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * ✅ GOOD: Efficient sort using Collections
     */
    public void sortByTitleGood() {
        photoList.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
        notifyDataSetChanged();
    }
    
    public void updateData(List<Photo> newPhotos) {
        this.photoList = newPhotos != null ? newPhotos : new ArrayList<>();
        this.originalList = new ArrayList<>(this.photoList);
        notifyDataSetChanged();
    }
    
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvFileSize;
        
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
        }
    }
}
