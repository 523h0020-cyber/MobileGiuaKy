package com.example.mobilegiuaky.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * ⚠️ BUG INTENTIONAL - CPU HEAVY OPERATIONS ⚠️
 * 
 * This class contains inefficient algorithms that consume excessive CPU
 * for demo purposes with Android Profiler.
 */
public class HeavyProcessor {
    
    /**
     * ⚠️ BAD: Inefficient bubble sort on large dataset
     * O(n²) complexity causes high CPU usage
     */
    public static int[] inefficientSort(int[] array) {
        int n = array.length;
        int[] result = array.clone();
        
        // Bubble sort - O(n²) - very inefficient for large arrays
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (result[j] > result[j + 1]) {
                    // Swap
                    int temp = result[j];
                    result[j] = result[j + 1];
                    result[j + 1] = temp;
                }
                
                // ⚠️ EXTRA BAD: Unnecessary computation inside loop
                double wastedComputation = Math.sin(Math.random()) * Math.cos(Math.random());
            }
        }
        return result;
    }
    
    /**
     * ⚠️ BAD: Heavy string processing on main thread
     */
    public static String heavyStringProcessing(String input, int iterations) {
        StringBuilder result = new StringBuilder(input);
        
        for (int i = 0; i < iterations; i++) {
            // ⚠️ BAD: Multiple string operations
            result.append(input);
            result.reverse();
            result.append(String.valueOf(i));
            
            // ⚠️ EXTRA BAD: Creating new objects in loop
            String temp = result.toString().toUpperCase().toLowerCase();
            result = new StringBuilder(temp);
        }
        
        return result.toString();
    }
    
    /**
     * ⚠️ BAD: Inefficient search - linear search repeated multiple times
     */
    public static int inefficientSearch(String query, String[] items) {
        int matchCount = 0;
        
        for (int repeat = 0; repeat < 100; repeat++) { // ⚠️ Unnecessary repetition
            for (int i = 0; i < items.length; i++) {
                // ⚠️ BAD: Creating new strings in loop
                String lowerItem = items[i].toLowerCase();
                String lowerQuery = query.toLowerCase();
                
                // ⚠️ BAD: Multiple string operations
                if (lowerItem.contains(lowerQuery) || 
                    lowerItem.startsWith(lowerQuery) ||
                    lowerItem.endsWith(lowerQuery) ||
                    calculateLevenshteinDistance(lowerItem, lowerQuery) < 3) {
                    matchCount++;
                }
            }
        }
        
        return matchCount;
    }
    
    /**
     * Levenshtein distance - expensive string comparison
     */
    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * ⚠️ BAD: Heavy image processing on main thread
     * This should NEVER be done in onBindViewHolder
     */
    public static Bitmap heavyImageProcessing(Bitmap original) {
        if (original == null) return null;
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Create mutable bitmap
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        
        // ⚠️ BAD: Pixel-by-pixel manipulation - very slow
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = result.getPixel(x, y);
                
                // Apply grayscale filter
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                
                int gray = (red + green + blue) / 3;
                
                // ⚠️ EXTRA BAD: Unnecessary math in pixel loop
                double brightness = Math.pow(gray / 255.0, 0.8) * 255;
                gray = (int) brightness;
                
                result.setPixel(x, y, Color.rgb(gray, gray, gray));
            }
        }
        
        return result;
    }
    
    /**
     * ⚠️ BAD: Generate large random data array
     */
    public static int[] generateLargeDataset(int size) {
        Random random = new Random();
        int[] data = new int[size];
        
        for (int i = 0; i < size; i++) {
            data[i] = random.nextInt(10000);
        }
        
        return data;
    }
    
    /**
     * ⚠️ BAD: Compress and decompress repeatedly
     */
    public static byte[] wasteResources(byte[] data, int iterations) {
        byte[] current = data;
        
        for (int i = 0; i < iterations; i++) {
            // Compress
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            for (byte b : current) {
                compressed.write(b);
            }
            
            // Decompress (pointless but CPU intensive)
            current = compressed.toByteArray();
        }
        
        return current;
    }
}
