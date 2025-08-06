package com.odyssey.test;

import com.odyssey.core.GameConfig;
import com.odyssey.core.memory.MemoryManager;
import com.odyssey.core.jobs.JobSystem;
import com.odyssey.world.World;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkPosition;
import com.odyssey.world.chunk.ChunkMeshGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Performance regression test for CI/CD pipeline.
 * Benchmarks critical game systems to detect performance regressions.
 */
public class PerformanceRegressionTest {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceRegressionTest.class);
    
    // Performance thresholds (in milliseconds)
    private static final long CHUNK_GENERATION_THRESHOLD = 50;
    private static final long CHUNK_MESHING_THRESHOLD = 30;
    private static final long MEMORY_ALLOCATION_THRESHOLD = 10;
    private static final long JOB_SYSTEM_THRESHOLD = 5;
    
    // Test parameters
    private static final int CHUNK_GEN_ITERATIONS = 50;
    private static final int MESHING_ITERATIONS = 25;
    private static final int MEMORY_ITERATIONS = 1000;
    private static final int JOB_ITERATIONS = 100;
    
    public static void main(String[] args) {
        boolean headless = false;
        boolean benchmark = false;
        
        // Parse command line arguments
        for (String arg : args) {
            switch (arg) {
                case "--headless":
                    headless = true;
                    break;
                case "--benchmark":
                    benchmark = true;
                    break;
            }
        }
        
        if (!headless || !benchmark) {
            logger.error("This test must be run with --headless --benchmark flags");
            System.exit(1);
        }
        
        logger.info("Starting performance regression test");
        
        try {
            runPerformanceTests();
            logger.info("Performance regression test completed successfully");
        } catch (Exception e) {
            logger.error("Performance regression test failed", e);
            System.exit(1);
        }
    }
    
    private static void runPerformanceTests() throws Exception {
        // Create results directory
        File resultsDir = new File("target/performance-test-results");
        resultsDir.mkdirs();
        
        try (FileWriter resultsWriter = new FileWriter(new File(resultsDir, "performance-results.txt"))) {
            resultsWriter.write("Performance Regression Test Results\\n");
            resultsWriter.write("==================================\\n\\n");
            
            // Test chunk generation performance
            testChunkGenerationPerformance(resultsWriter);
            
            // Test chunk meshing performance
            testChunkMeshingPerformance(resultsWriter);
            
            // Test memory allocation performance
            testMemoryAllocationPerformance(resultsWriter);
            
            // Test job system performance
            testJobSystemPerformance(resultsWriter);
            
            resultsWriter.write("\\nAll performance tests completed successfully!\\n");
        }
    }
    
    private static void testChunkGenerationPerformance(FileWriter resultsWriter) throws Exception {
        resultsWriter.write("Chunk Generation Performance Test\\n");
        resultsWriter.write("---------------------------------\\n");
        
        GameConfig config = new GameConfig();
        config.setHeadless(true);
        config.setWorldSeed(12345L);
        
        World world = new World(config);
        world.initialize();
        
        WorldGenerator generator = new WorldGenerator(config);
        
        List<Long> times = new ArrayList<>();
        
        for (int i = 0; i < CHUNK_GEN_ITERATIONS; i++) {
            ChunkPosition pos = new ChunkPosition(i % 10, i / 10);
            
            long startTime = System.nanoTime();
            Chunk chunk = generator.generateChunk(pos);
            long endTime = System.nanoTime();
            
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            times.add(duration);
            
            if (chunk == null) {
                throw new RuntimeException("Chunk generation returned null");
            }
        }
        
        world.cleanup();
        
        // Calculate statistics
        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        resultsWriter.write("Iterations: " + CHUNK_GEN_ITERATIONS + "\\n");
        resultsWriter.write("Average time: " + String.format("%.2f", avgTime) + "ms\\n");
        resultsWriter.write("Min time: " + minTime + "ms\\n");
        resultsWriter.write("Max time: " + maxTime + "ms\\n");
        resultsWriter.write("Threshold: " + CHUNK_GENERATION_THRESHOLD + "ms\\n");
        
        if (avgTime > CHUNK_GENERATION_THRESHOLD) {
            throw new RuntimeException("Chunk generation performance regression detected! Average: " + 
                String.format("%.2f", avgTime) + "ms > " + CHUNK_GENERATION_THRESHOLD + "ms");
        }
        
        resultsWriter.write("Status: PASSED\\n\\n");
        logger.info("Chunk generation performance test passed - Average: {:.2f}ms", avgTime);
    }
    
    private static void testChunkMeshingPerformance(FileWriter resultsWriter) throws Exception {
        resultsWriter.write("Chunk Meshing Performance Test\\n");
        resultsWriter.write("------------------------------\\n");
        
        GameConfig config = new GameConfig();
        config.setHeadless(true);
        config.setWorldSeed(12345L);
        
        World world = new World(config);
        world.initialize();
        
        WorldGenerator generator = new WorldGenerator(config);
        ChunkMeshGenerator meshGenerator = new ChunkMeshGenerator();
        
        // Pre-generate chunks for meshing
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < MESHING_ITERATIONS; i++) {
            ChunkPosition pos = new ChunkPosition(i % 5, i / 5);
            chunks.add(generator.generateChunk(pos));
        }
        
        List<Long> times = new ArrayList<>();
        
        for (Chunk chunk : chunks) {
            long startTime = System.nanoTime();
            meshGenerator.generateMesh(chunk);
            long endTime = System.nanoTime();
            
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            times.add(duration);
        }
        
        world.cleanup();
        
        // Calculate statistics
        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        resultsWriter.write("Iterations: " + MESHING_ITERATIONS + "\\n");
        resultsWriter.write("Average time: " + String.format("%.2f", avgTime) + "ms\\n");
        resultsWriter.write("Min time: " + minTime + "ms\\n");
        resultsWriter.write("Max time: " + maxTime + "ms\\n");
        resultsWriter.write("Threshold: " + CHUNK_MESHING_THRESHOLD + "ms\\n");
        
        if (avgTime > CHUNK_MESHING_THRESHOLD) {
            throw new RuntimeException("Chunk meshing performance regression detected! Average: " + 
                String.format("%.2f", avgTime) + "ms > " + CHUNK_MESHING_THRESHOLD + "ms");
        }
        
        resultsWriter.write("Status: PASSED\\n\\n");
        logger.info("Chunk meshing performance test passed - Average: {:.2f}ms", avgTime);
    }
    
    private static void testMemoryAllocationPerformance(FileWriter resultsWriter) throws Exception {
        resultsWriter.write("Memory Allocation Performance Test\\n");
        resultsWriter.write("----------------------------------\\n");
        
        GameConfig config = new GameConfig();
        config.setHeadless(true);
        
        MemoryManager memoryManager = new MemoryManager(config);
        memoryManager.initialize();
        
        List<Long> times = new ArrayList<>();
        
        for (int i = 0; i < MEMORY_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            
            // Simulate typical memory allocation patterns
            Object[] objects = new Object[100];
            for (int j = 0; j < objects.length; j++) {
                objects[j] = new byte[1024]; // 1KB objects
            }
            
            long endTime = System.nanoTime();
            
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            times.add(duration);
            
            // Clear references to allow GC
            for (int j = 0; j < objects.length; j++) {
                objects[j] = null;
            }
        }
        
        memoryManager.cleanup();
        
        // Calculate statistics
        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        resultsWriter.write("Iterations: " + MEMORY_ITERATIONS + "\\n");
        resultsWriter.write("Average time: " + String.format("%.4f", avgTime) + "ms\\n");
        resultsWriter.write("Min time: " + minTime + "ms\\n");
        resultsWriter.write("Max time: " + maxTime + "ms\\n");
        resultsWriter.write("Threshold: " + MEMORY_ALLOCATION_THRESHOLD + "ms\\n");
        
        if (avgTime > MEMORY_ALLOCATION_THRESHOLD) {
            throw new RuntimeException("Memory allocation performance regression detected! Average: " + 
                String.format("%.4f", avgTime) + "ms > " + MEMORY_ALLOCATION_THRESHOLD + "ms");
        }
        
        resultsWriter.write("Status: PASSED\\n\\n");
        logger.info("Memory allocation performance test passed - Average: {:.4f}ms", avgTime);
    }
    
    private static void testJobSystemPerformance(FileWriter resultsWriter) throws Exception {
        resultsWriter.write("Job System Performance Test\\n");
        resultsWriter.write("---------------------------\\n");
        
        GameConfig config = new GameConfig();
        config.setHeadless(true);
        
        JobSystem jobSystem = new JobSystem(config);
        jobSystem.initialize();
        
        List<Long> times = new ArrayList<>();
        
        for (int i = 0; i < JOB_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            
            // Submit a batch of simple jobs
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Simulate some work
                    int sum = 0;
                    for (int k = 0; k < 1000; k++) {
                        sum += k;
                    }
                }, jobSystem.getExecutor());
                futures.add(future);
            }
            
            // Wait for all jobs to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long endTime = System.nanoTime();
            
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            times.add(duration);
        }
        
        jobSystem.shutdown(5);
        
        // Calculate statistics
        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        resultsWriter.write("Iterations: " + JOB_ITERATIONS + "\\n");
        resultsWriter.write("Average time: " + String.format("%.2f", avgTime) + "ms\\n");
        resultsWriter.write("Min time: " + minTime + "ms\\n");
        resultsWriter.write("Max time: " + maxTime + "ms\\n");
        resultsWriter.write("Threshold: " + JOB_SYSTEM_THRESHOLD + "ms\\n");
        
        if (avgTime > JOB_SYSTEM_THRESHOLD) {
            throw new RuntimeException("Job system performance regression detected! Average: " + 
                String.format("%.2f", avgTime) + "ms > " + JOB_SYSTEM_THRESHOLD + "ms");
        }
        
        resultsWriter.write("Status: PASSED\\n\\n");
        logger.info("Job system performance test passed - Average: {:.2f}ms", avgTime);
    }
}