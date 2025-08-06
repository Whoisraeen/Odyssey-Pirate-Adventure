package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import com.odyssey.core.jobs.JobSystem;
import com.odyssey.core.jobs.JobSystem.JobSystemStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

/**
 * Test suite for the asset streaming pipeline.
 * Tests the AssetManager functionality without requiring OpenGL context.
 */
public class AssetStreamingTest {
    
    private AssetManager assetManager;
    private JobSystem jobSystem;
    
    @BeforeEach
    void setUp() {
        // Initialize job system for async operations
        GameConfig config = new GameConfig();
        config.setJobSystemWorkerCount(2);
        config.setJobQueueCapacity(100);
        config.setGlobalJobQueueCapacity(200);
        config.setJobSystemStatisticsEnabled(true);
        
        jobSystem = new JobSystem(config);
        jobSystem.initialize();
        
        // Initialize asset management components
        assetManager = new AssetManager(64, 2, jobSystem); // 64MB cache, 2 concurrent loads
    }
    
    @AfterEach
    void tearDown() {
        if (assetManager != null) {
            assetManager.clearCache();
        }
        if (jobSystem != null) {
            jobSystem.shutdown(5); // 5 second timeout
        }
    }
    
    @Test
    void testAssetManagerBasicFunctionality() {
        // Test basic asset loading
        assertNotNull(assetManager);
        
        // Test cache statistics
        String stats = assetManager.getCacheStats();
        assertNotNull(stats);
        
        // Test memory management
        assetManager.clearCache();
        String statsAfterClear = assetManager.getCacheStats();
        assertNotNull(statsAfterClear);
    }
    
    @Test
    void testAsynchronousLoadingBehavior() throws Exception {
        // Test that loading operations are truly asynchronous
        long startTime = System.currentTimeMillis();
        
        // Create mock asset loading futures (these will fail but that's expected in unit tests)
        CompletableFuture<byte[]> future1 = assetManager.loadAssetAsync("test1.png", AssetManager.AssetPriority.MEDIUM, null);
        CompletableFuture<byte[]> future2 = assetManager.loadAssetAsync("test2.png", AssetManager.AssetPriority.MEDIUM, null);
        CompletableFuture<byte[]> future3 = assetManager.loadAssetAsync("test3.png", AssetManager.AssetPriority.MEDIUM, null);
        
        // Should return immediately (async)
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime < 100, "Async operations should return quickly");
        
        // Test completion handling
        CompletableFuture<Void> allComplete = CompletableFuture.allOf(future1, future2, future3);
        
        // Don't wait for completion in unit test (files don't exist)
        // Just verify the futures are created properly
        assertNotNull(future1);
        assertNotNull(future2);
        assertNotNull(future3);
        assertNotNull(allComplete);
    }
    
    @Test
    void testJobSystemIntegration() {
        // Test that JobSystem is initialized
        assertNotNull(jobSystem);
        
        // Test that we can submit jobs through AssetManager
        CompletableFuture<byte[]> future = assetManager.loadAssetAsync("test.png", AssetManager.AssetPriority.MEDIUM, null);
        assertNotNull(future);
        
        // Test job system statistics
        JobSystemStatistics stats = jobSystem.getStatistics();
        assertNotNull(stats);
    }
}