package com.odyssey.test;

import com.odyssey.core.GameConfig;
import com.odyssey.world.World;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless world generation fuzzing test for CI/CD pipeline.
 * Tests world generation stability and performance without graphics.
 */
public class HeadlessWorldGenTest {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessWorldGenTest.class);
    
    private static final int DEFAULT_FUZZ_ITERATIONS = 100;
    private static final int CHUNKS_PER_ITERATION = 16;
    private static final long SEED_BASE = 12345L;
    
    public static void main(String[] args) {
        boolean headless = false;
        int fuzzIterations = DEFAULT_FUZZ_ITERATIONS;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--headless":
                    headless = true;
                    break;
                case "--fuzz-iterations":
                    if (i + 1 < args.length) {
                        try {
                            fuzzIterations = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid fuzz iterations value: {}", args[i]);
                            System.exit(1);
                        }
                    }
                    break;
            }
        }
        
        if (!headless) {
            logger.error("This test must be run in headless mode");
            System.exit(1);
        }
        
        logger.info("Starting headless world generation fuzzing test with {} iterations", fuzzIterations);
        
        try {
            runFuzzTest(fuzzIterations);
            logger.info("Headless world generation test completed successfully");
        } catch (Exception e) {
            logger.error("Headless world generation test failed", e);
            System.exit(1);
        }
    }
    
    private static void runFuzzTest(int iterations) throws Exception {
        // Create results directory
        File resultsDir = new File("target/worldgen-test-results");
        resultsDir.mkdirs();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        try (FileWriter resultsWriter = new FileWriter(new File(resultsDir, "worldgen-fuzz-results.txt"))) {
            resultsWriter.write("Headless World Generation Fuzz Test Results\\n");
            resultsWriter.write("===========================================\\n");
            resultsWriter.write("Iterations: " + iterations + "\\n");
            resultsWriter.write("Chunks per iteration: " + CHUNKS_PER_ITERATION + "\\n\\n");
            
            for (int i = 0; i < iterations; i++) {
                long seed = SEED_BASE + i;
                boolean success = testWorldGeneration(seed, i, resultsWriter);
                
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                
                // Log progress every 10% of iterations
                if ((i + 1) % Math.max(1, iterations / 10) == 0) {
                    logger.info("Progress: {}/{} iterations completed", i + 1, iterations);
                }
            }
            
            // Write summary
            resultsWriter.write("\\n\\nSUMMARY\\n");
            resultsWriter.write("=======\\n");
            resultsWriter.write("Total iterations: " + iterations + "\\n");
            resultsWriter.write("Successful: " + successCount.get() + "\\n");
            resultsWriter.write("Failed: " + failureCount.get() + "\\n");
            resultsWriter.write("Success rate: " + String.format("%.2f%%", (successCount.get() * 100.0) / iterations) + "\\n");
            
            logger.info("Test summary - Success: {}, Failures: {}, Success rate: {:.2f}%", 
                successCount.get(), failureCount.get(), (successCount.get() * 100.0) / iterations);
            
            if (failureCount.get() > 0) {
                throw new RuntimeException("World generation fuzz test had " + failureCount.get() + " failures");
            }
        }
    }
    
    private static boolean testWorldGeneration(long seed, int iteration, FileWriter resultsWriter) {
        try {
            resultsWriter.write("Iteration " + iteration + " (seed: " + seed + "): ");
            
            // Create headless game config
            GameConfig config = new GameConfig();
            config.setHeadless(true);
            config.setWorldSeed(seed);
            
            // Create world and generator
            World world = new World(config);
            world.initialize();
            
            WorldGenerator generator = new WorldGenerator(config);
            
            // Generate chunks in a pattern around origin
            Random random = new Random(seed);
            long startTime = System.currentTimeMillis();
            
            for (int j = 0; j < CHUNKS_PER_ITERATION; j++) {
                int chunkX = random.nextInt(21) - 10; // -10 to 10
                int chunkZ = random.nextInt(21) - 10; // -10 to 10
                
                ChunkPosition pos = new ChunkPosition(chunkX, chunkZ);
                Chunk chunk = generator.generateChunk(pos);
                
                // Validate chunk
                if (chunk == null) {
                    throw new RuntimeException("Generated chunk is null at position " + pos);
                }
                
                // Basic validation - check that chunk has some blocks
                boolean hasBlocks = false;
                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 256; y++) {
                        for (int z = 0; z < 32; z++) {
                            if (chunk.getBlock(x, y, z) != 0) { // Assuming 0 is air
                                hasBlocks = true;
                                break;
                            }
                        }
                        if (hasBlocks) break;
                    }
                    if (hasBlocks) break;
                }
                
                if (!hasBlocks) {
                    throw new RuntimeException("Generated chunk has no blocks at position " + pos);
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            world.cleanup();
            
            resultsWriter.write("SUCCESS (took " + duration + "ms)\\n");
            return true;
            
        } catch (Exception e) {
            try {
                resultsWriter.write("FAILED - " + e.getMessage() + "\\n");
            } catch (IOException ioException) {
                logger.error("Failed to write failure result", ioException);
            }
            logger.error("World generation failed for iteration {} with seed {}", iteration, seed, e);
            return false;
        }
    }
}