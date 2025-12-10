package com.example.mobilegiuaky.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ✅ OPTIMIZED VERSION - Data Processing
 * 
 * This class demonstrates efficient algorithms and proper threading
 * for CPU-intensive operations.
 */
public class OptimizedProcessor {
    
    private static final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );
    
    /**
     * ✅ GOOD: Use built-in efficient sort (TimSort - O(n log n))
     */
    public static int[] efficientSort(int[] array) {
        int[] result = array.clone();
        Arrays.sort(result);
        return result;
    }
    
    /**
     * ✅ GOOD: Efficient string processing
     */
    public static String efficientStringProcessing(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Use StringBuilder efficiently
        StringBuilder result = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            result.append(Character.toLowerCase(input.charAt(i)));
        }
        return result.toString();
    }
    
    /**
     * ✅ GOOD: Binary search for sorted data - O(log n)
     */
    public static int binarySearch(String query, String[] sortedItems) {
        return Arrays.binarySearch(sortedItems, query, String.CASE_INSENSITIVE_ORDER);
    }
    
    /**
     * ✅ GOOD: Efficient filter with early termination
     */
    public static <T> List<T> efficientFilter(List<T> items, java.util.function.Predicate<T> predicate) {
        return items.stream()
                .filter(predicate)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * ✅ GOOD: Run heavy computation on background thread
     */
    public static Future<?> runOnBackground(Runnable task) {
        return executor.submit(task);
    }
    
    /**
     * ✅ GOOD: Parallel processing for large datasets
     */
    public static int[] parallelSort(int[] array) {
        int[] result = array.clone();
        Arrays.parallelSort(result);
        return result;
    }
    
    /**
     * Shutdown executor
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
