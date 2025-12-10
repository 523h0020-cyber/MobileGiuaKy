package com.example.mobilegiuaky.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

/**
 * ✅ OPTIMIZED VERSION - Non-Leaky Manager
 * 
 * This class demonstrates the CORRECT way to:
 * - Use Application Context instead of Activity Context
 * - Use WeakReference for callbacks
 * - Properly cleanup resources
 */
public class NonLeakyManager {
    
    private static NonLeakyManager instance;
    
    // ✅ GOOD: Use Application Context (survives Activity lifecycle)
    private Context applicationContext;
    
    // ✅ GOOD: Use WeakReference for listeners (allows GC)
    private WeakReference<OnDataLoadedListener> listenerRef;
    
    // ✅ GOOD: Handler with proper cleanup
    private Handler handler;
    private Runnable pendingRunnable;
    
    private NonLeakyManager() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    public static NonLeakyManager getInstance() {
        if (instance == null) {
            synchronized (NonLeakyManager.class) {
                if (instance == null) {
                    instance = new NonLeakyManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * ✅ GOOD: Initialize with Application Context
     */
    public void init(Context context) {
        // Always use Application Context for singletons
        this.applicationContext = context.getApplicationContext();
    }
    
    /**
     * ✅ GOOD: Use WeakReference for listener
     */
    public void setOnDataLoadedListener(OnDataLoadedListener listener) {
        this.listenerRef = new WeakReference<>(listener);
    }
    
    /**
     * ✅ GOOD: Store runnable reference for cleanup
     */
    public void loadDataWithDelay() {
        // Remove any pending callbacks first
        if (pendingRunnable != null) {
            handler.removeCallbacks(pendingRunnable);
        }
        
        pendingRunnable = () -> {
            OnDataLoadedListener listener = listenerRef != null ? listenerRef.get() : null;
            if (listener != null) {
                listener.onDataLoaded("Data loaded!");
            }
        };
        
        handler.postDelayed(pendingRunnable, 5000);
    }
    
    /**
     * ✅ GOOD: Cleanup method that actually clears references
     */
    public void cleanup() {
        if (pendingRunnable != null) {
            handler.removeCallbacks(pendingRunnable);
            pendingRunnable = null;
        }
        
        if (listenerRef != null) {
            listenerRef.clear();
            listenerRef = null;
        }
        
        // Note: We don't clear applicationContext as it's safe to keep
    }
    
    /**
     * ✅ GOOD: Remove specific listener
     */
    public void removeListener() {
        if (listenerRef != null) {
            listenerRef.clear();
            listenerRef = null;
        }
    }
    
    public Context getContext() {
        return applicationContext;
    }
    
    public interface OnDataLoadedListener {
        void onDataLoaded(String data);
    }
}
