package com.example.mobilegiuaky.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * ⚠️ BUG INTENTIONAL - MEMORY LEAK DEMO ⚠️
 * 
 * This Singleton class holds a reference to Activity Context,
 * causing Memory Leak because the Activity cannot be garbage collected.
 * 
 * LeakCanary will detect this leak!
 * 
 * PROBLEM: Singleton survives configuration changes and activity lifecycle,
 * but holds a strong reference to the Activity context.
 */
public class LeakyManager {
    
    private static LeakyManager instance;
    
    // ⚠️ BAD: Holding Activity Context in Singleton
    private Context context;
    
    // ⚠️ BAD: Handler can also cause leaks
    private Handler handler;
    
    // ⚠️ BAD: Callback holding reference to Activity
    private OnDataLoadedListener listener;
    
    private LeakyManager() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    public static LeakyManager getInstance() {
        if (instance == null) {
            instance = new LeakyManager();
        }
        return instance;
    }
    
    /**
     * ⚠️ BUG: This method stores Activity Context in singleton
     * The Activity will never be garbage collected!
     */
    public void init(Context context) {
        // ⚠️ BAD: Should use context.getApplicationContext() instead
        this.context = context;
    }
    
    /**
     * ⚠️ BUG: Listener is never cleared, causing leak
     */
    public void setOnDataLoadedListener(OnDataLoadedListener listener) {
        this.listener = listener;
    }
    
    /**
     * ⚠️ BUG: Delayed runnable holds reference to callback
     */
    public void loadDataWithDelay() {
        // Simulate delayed data loading
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onDataLoaded("Data loaded!");
                }
            }
        }, 5000); // 5 second delay - if Activity destroyed before this, LEAK!
    }
    
    public Context getContext() {
        return context;
    }
    
    /**
     * ⚠️ FIX: This method should be called in onDestroy()
     * But we intentionally DON'T call it to demonstrate the leak
     */
    public void cleanup() {
        this.context = null;
        this.listener = null;
        handler.removeCallbacksAndMessages(null);
    }
    
    public interface OnDataLoadedListener {
        void onDataLoaded(String data);
    }
}
