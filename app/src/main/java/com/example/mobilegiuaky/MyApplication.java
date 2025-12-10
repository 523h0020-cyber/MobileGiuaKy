package com.example.mobilegiuaky;

import android.app.Application;

/**
 * Custom Application class
 * LeakCanary will be automatically initialized in debug builds
 */
public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // LeakCanary is auto-initialized via ContentProvider in debug builds
        // No manual initialization needed for LeakCanary 2.x
    }
}
