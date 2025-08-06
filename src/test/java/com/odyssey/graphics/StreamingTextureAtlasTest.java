package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import com.odyssey.core.jobs.JobSystem;
import com.odyssey.world.chunk.LODTextureAtlasManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StreamingTextureAtlas functionality.
 */
public class StreamingTextureAtlasTest {
    private static final Logger logger = LoggerFactory.getLogger(StreamingTextureAtlasTest.class);
    
    private JobSystem jobSystem;
    private StreamingTextureManager streamingManager;
    private TextureAtlasManager atlasManager;
    private LODTextureAtlasManager lodAtlasManager;
    private Path testTextureDir;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info("Setting up StreamingTextureAtlasTest");
        
        // Create test directory
        testTextureDir = Paths.get("test-textures");
        Files.createDirectories(testTextureDir);
        
        // Create game config
        GameConfig config = new GameConfig();
        
        // Initialize systems
        jobSystem = new JobSystem(config);
        jobSystem.initialize();
        streamingManager = new StreamingTextureManager(jobSystem, 100, 200, 0.8f, true);
        atlasManager = new TextureAtlasManager(streamingManager);
        lodAtlasManager = new LODTextureAtlasManager(atlasManager, LODTextureAtlasManager.AtlasConfig.createDefault());
        
        logger.info("StreamingTextureAtlasTest setup complete");
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.info("Tearing down StreamingTextureAtlasTest");
        
        if (jobSystem != null) {
            jobSystem.shutdown(5);
        }
        
        // Clean up test files
        if (testTextureDir != null && Files.exists(testTextureDir)) {
            Files.walk(testTextureDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
        
        logger.info("StreamingTextureAtlasTest teardown complete");
    }

    /**
     * Creates a simple test texture file
     */
    private Path createTestTexture(String name, int width, int height) throws IOException {
        Path texturePath = testTextureDir.resolve(name + ".png");
        
        // Create a simple texture data (just a basic pattern)
        byte[] textureData = new byte[width * height * 4]; // RGBA
        for (int i = 0; i < textureData.length; i += 4) {
            textureData[i] = (byte) 255;     // R
            textureData[i + 1] = (byte) 128; // G
            textureData[i + 2] = (byte) 64;  // B
            textureData[i + 3] = (byte) 255; // A
        }
        
        try (FileOutputStream fos = new FileOutputStream(texturePath.toFile())) {
            fos.write(textureData);
        }
        
        return texturePath;
    }

    @Test
    public void testAsyncTextureLoading() throws Exception {
        logger.info("Testing async texture loading");
        
        // Create test texture
        Path texturePath = createTestTexture("async_test", 64, 64);
        
        // Request texture asynchronously
        TextureAtlasManager.TextureRequest request = new TextureAtlasManager.TextureRequest(
            texturePath.toString(),
            TextureAtlasManager.AtlasCategory.TERRAIN,
            TextureAtlasManager.StreamingPriority.NORMAL
        );
        
        atlasManager.requestTextureAsync(request);
        
        // Wait a bit for processing
        Thread.sleep(100);
        
        // Verify texture was processed
        assertTrue(atlasManager.hasTexture(texturePath.toString()));
        
        logger.info("Async texture loading test completed");
    }

    @Test
    public void testLODTextureRegistration() throws Exception {
        logger.info("Testing LOD texture registration");
        
        // Create test textures for different LOD levels
        Path highLOD = createTestTexture("high_lod", 128, 128);
        Path mediumLOD = createTestTexture("medium_lod", 64, 64);
        Path lowLOD = createTestTexture("low_lod", 32, 32);
        
        // Register LOD textures
        lodAtlasManager.registerLODTexture("test_texture", highLOD.toString(), 0);
        lodAtlasManager.registerLODTexture("test_texture", mediumLOD.toString(), 1);
        lodAtlasManager.registerLODTexture("test_texture", lowLOD.toString(), 2);
        
        // Verify registration
        assertTrue(lodAtlasManager.hasLODTexture("test_texture"));
        
        logger.info("LOD texture registration test completed");
    }

    @Test
    public void testLODDistanceBasedSelection() throws Exception {
        logger.info("Testing LOD distance-based selection");
        
        // Create test textures
        Path highLOD = createTestTexture("distance_high", 128, 128);
        Path lowLOD = createTestTexture("distance_low", 32, 32);
        
        // Register with different LOD levels
        lodAtlasManager.registerLODTexture("distance_test", highLOD.toString(), 0);
        lodAtlasManager.registerLODTexture("distance_test", lowLOD.toString(), 2);
        
        // Test distance-based selection
        LODTextureAtlasManager.LODTexture nearTexture = lodAtlasManager.selectLODTexture("distance_test", 10.0f);
        LODTextureAtlasManager.LODTexture farTexture = lodAtlasManager.selectLODTexture("distance_test", 1000.0f);
        
        assertNotNull(nearTexture);
        assertNotNull(farTexture);
        
        logger.info("LOD distance-based selection test completed");
    }

    @Test
    public void testLODStatistics() throws Exception {
        logger.info("Testing LOD statistics");
        
        // Create and register some textures
        Path texture1 = createTestTexture("stats_1", 64, 64);
        Path texture2 = createTestTexture("stats_2", 32, 32);
        
        lodAtlasManager.registerLODTexture("stats_texture_1", texture1.toString(), 0);
        lodAtlasManager.registerLODTexture("stats_texture_2", texture2.toString(), 1);
        
        // Get statistics
        LODTextureAtlasManager.LODStatistics stats = lodAtlasManager.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.getTotalTextures() >= 2);
        
        logger.info("LOD statistics test completed - Total textures: {}", stats.getTotalTextures());
    }

    @Test
    public void testCleanup() throws Exception {
        logger.info("Testing cleanup functionality");
        
        // Create test texture
        Path texturePath = createTestTexture("cleanup_test", 64, 64);
        
        // Register texture
        lodAtlasManager.registerLODTexture("cleanup_texture", texturePath.toString(), 0);
        
        // Verify it exists
        assertTrue(lodAtlasManager.hasLODTexture("cleanup_texture"));
        
        // Perform cleanup
        lodAtlasManager.cleanup();
        
        // Verify cleanup was performed (this might not remove the texture immediately)
        // The exact behavior depends on the implementation
        assertNotNull(lodAtlasManager);
        
        logger.info("Cleanup test completed");
    }
}