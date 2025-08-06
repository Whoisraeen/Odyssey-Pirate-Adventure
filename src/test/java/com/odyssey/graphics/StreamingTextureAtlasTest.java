package com.odyssey.graphics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.odyssey.core.jobs.JobSystem;
import com.odyssey.core.GameConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for streaming texture atlas functionality.
 */
public class StreamingTextureAtlasTest {
    
    @TempDir
    Path tempDir;
    
    private JobSystem jobSystem;
    private StreamingTextureManager streamingManager;
    private TextureAtlasManager atlasManager;
    private LODTextureAtlasSystem lodSystem;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create game config and job system
        GameConfig config = new GameConfig();
        jobSystem = new JobSystem(config);
        
        // Create streaming texture manager (creates its own AssetManager)
        streamingManager = new StreamingTextureManager(jobSystem, 64, 256, 0.8f, true);
        
        // Create atlas manager
        atlasManager = new TextureAtlasManager(streamingManager, 128);
        
        // Create LOD system
        lodSystem = new LODTextureAtlasSystem(atlasManager);
        
        // Create test texture files
        createTestTextures();
    }
    
    private void createTestTextures() throws IOException {
        // Create test texture data (simple colored squares)
        createTestTexture("test_block.png", 64, 64, new byte[]{(byte)255, 0, 0, (byte)255}); // Red
        createTestTexture("test_entity.png", 32, 32, new byte[]{0, (byte)255, 0, (byte)255}); // Green
        createTestTexture("test_ui.png", 16, 16, new byte[]{0, 0, (byte)255, (byte)255}); // Blue
        
        // Create LOD versions
        createTestTexture("test_lod_ultra.png", 128, 128, new byte[]{(byte)255, (byte)255, 0, (byte)255}); // Yellow
        createTestTexture("test_lod_high.png", 96, 96, new byte[]{(byte)255, 0, (byte)255, (byte)255}); // Magenta
        createTestTexture("test_lod_medium.png", 64, 64, new byte[]{0, (byte)255, (byte)255, (byte)255}); // Cyan
        createTestTexture("test_lod_low.png", 32, 32, new byte[]{(byte)128, (byte)128, (byte)128, (byte)255}); // Gray
        createTestTexture("test_lod_minimal.png", 16, 16, new byte[]{(byte)64, (byte)64, (byte)64, (byte)255}); // Dark gray
    }
    
    private void createTestTexture(String filename, int width, int height, byte[] color) throws IOException {
        Path texturePath = tempDir.resolve(filename);
        
        // Create simple texture data
        ByteBuffer textureData = ByteBuffer.allocate(width * height * 4);
        for (int i = 0; i < width * height; i++) {
            textureData.put(color);
        }
        
        // Write to file (simplified - in real implementation would be PNG format)
        Files.write(texturePath, textureData.array());
    }
    
    @Test
    void testAsyncTextureLoading() throws ExecutionException, InterruptedException, TimeoutException {
        String texturePath = tempDir.resolve("test_block.png").toString();
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> future = 
            atlasManager.requestTextureAsync("test_block", texturePath, 
                                           TextureAtlasManager.AtlasCategory.BLOCKS,
                                           TextureAtlasManager.StreamingPriority.HIGH, null);
        
        TextureAtlasManager.AtlasTextureReference reference = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(reference);
        assertEquals(TextureAtlasManager.AtlasCategory.BLOCKS, reference.getCategory());
        assertNotNull(reference.getRegion());
        assertNotNull(reference.getAtlas());
    }
    
    @Test
    void testSyncTextureLoading() {
        String texturePath = tempDir.resolve("test_entity.png").toString();
        
        TextureAtlasManager.AtlasTextureReference reference = 
            atlasManager.requestTextureSync("test_entity", texturePath,
                                          TextureAtlasManager.AtlasCategory.ENTITIES,
                                          TextureAtlasManager.StreamingPriority.MEDIUM);
        
        assertNotNull(reference);
        assertEquals(TextureAtlasManager.AtlasCategory.ENTITIES, reference.getCategory());
    }
    
    @Test
    void testMultipleTextureCategories() throws ExecutionException, InterruptedException, TimeoutException {
        // Load textures in different categories
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> blockFuture = 
            atlasManager.requestTextureAsync("block1", tempDir.resolve("test_block.png").toString(),
                                           TextureAtlasManager.AtlasCategory.BLOCKS,
                                           TextureAtlasManager.StreamingPriority.HIGH, null);
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> entityFuture = 
            atlasManager.requestTextureAsync("entity1", tempDir.resolve("test_entity.png").toString(),
                                           TextureAtlasManager.AtlasCategory.ENTITIES,
                                           TextureAtlasManager.StreamingPriority.HIGH, null);
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> uiFuture = 
            atlasManager.requestTextureAsync("ui1", tempDir.resolve("test_ui.png").toString(),
                                           TextureAtlasManager.AtlasCategory.UI,
                                           TextureAtlasManager.StreamingPriority.HIGH, null);
        
        TextureAtlasManager.AtlasTextureReference blockRef = blockFuture.get(5, TimeUnit.SECONDS);
        TextureAtlasManager.AtlasTextureReference entityRef = entityFuture.get(5, TimeUnit.SECONDS);
        TextureAtlasManager.AtlasTextureReference uiRef = uiFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(blockRef);
        assertNotNull(entityRef);
        assertNotNull(uiRef);
        
        assertEquals(TextureAtlasManager.AtlasCategory.BLOCKS, blockRef.getCategory());
        assertEquals(TextureAtlasManager.AtlasCategory.ENTITIES, entityRef.getCategory());
        assertEquals(TextureAtlasManager.AtlasCategory.UI, uiRef.getCategory());
    }
    
    @Test
    void testLODTextureRegistration() throws ExecutionException, InterruptedException, TimeoutException {
        Map<LODTextureAtlasSystem.LODLevel, String> lodPaths = new HashMap<>();
        lodPaths.put(LODTextureAtlasSystem.LODLevel.ULTRA, tempDir.resolve("test_lod_ultra.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.HIGH, tempDir.resolve("test_lod_high.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.MEDIUM, tempDir.resolve("test_lod_medium.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.LOW, tempDir.resolve("test_lod_low.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.MINIMAL, tempDir.resolve("test_lod_minimal.png").toString());
        
        CompletableFuture<LODTextureAtlasSystem.LODTexture> future = 
            lodSystem.registerLODTexture("test_lod", TextureAtlasManager.AtlasCategory.BLOCKS, lodPaths);
        
        LODTextureAtlasSystem.LODTexture lodTexture = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(lodTexture);
        assertEquals("test_lod", lodTexture.getBaseName());
        assertEquals(TextureAtlasManager.AtlasCategory.BLOCKS, lodTexture.getCategory());
        assertTrue(lodTexture.hasLOD(LODTextureAtlasSystem.LODLevel.MEDIUM)); // Default loaded
    }
    
    @Test
    void testLODDistanceBasedSelection() throws ExecutionException, InterruptedException, TimeoutException {
        // Register LOD texture
        Map<LODTextureAtlasSystem.LODLevel, String> lodPaths = new HashMap<>();
        lodPaths.put(LODTextureAtlasSystem.LODLevel.ULTRA, tempDir.resolve("test_lod_ultra.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.HIGH, tempDir.resolve("test_lod_high.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.MEDIUM, tempDir.resolve("test_lod_medium.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.LOW, tempDir.resolve("test_lod_low.png").toString());
        
        lodSystem.registerLODTexture("distance_test", TextureAtlasManager.AtlasCategory.BLOCKS, lodPaths)
                .get(5, TimeUnit.SECONDS);
        
        // Test different distances
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> closeRef = 
            lodSystem.requestTexture("distance_test", 10.0f, null);
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> mediumRef = 
            lodSystem.requestTexture("distance_test", 50.0f, null);
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> farRef = 
            lodSystem.requestTexture("distance_test", 100.0f, null);
        
        assertNotNull(closeRef.get(5, TimeUnit.SECONDS));
        assertNotNull(mediumRef.get(5, TimeUnit.SECONDS));
        assertNotNull(farRef.get(5, TimeUnit.SECONDS));
        
        LODTextureAtlasSystem.LODTexture lodTexture = lodSystem.getLODTexture("distance_test");
        assertNotNull(lodTexture);
    }
    
    @Test
    void testMemoryManagement() throws ExecutionException, InterruptedException, TimeoutException {
        // Load many textures to test memory limits
        for (int i = 0; i < 10; i++) {
            String textureName = "memory_test_" + i;
            String texturePath = tempDir.resolve("test_block.png").toString();
            
            atlasManager.requestTextureAsync(textureName, texturePath,
                                           TextureAtlasManager.AtlasCategory.BLOCKS,
                                           TextureAtlasManager.StreamingPriority.LOW, null)
                    .get(5, TimeUnit.SECONDS);
        }
        
        // Check that textures were loaded
        assertTrue(atlasManager.getTotalTextureCount() > 0);
        
        // Get statistics
        String stats = atlasManager.getStreamingStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("Atlas Streaming"));
    }
    
    @Test
    void testPriorityHandling() throws ExecutionException, InterruptedException, TimeoutException {
        String texturePath = tempDir.resolve("test_block.png").toString();
        
        // Test different priorities
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> criticalFuture = 
            atlasManager.requestTextureAsync("critical", texturePath,
                                           TextureAtlasManager.AtlasCategory.BLOCKS,
                                           TextureAtlasManager.StreamingPriority.CRITICAL, null);
        
        CompletableFuture<TextureAtlasManager.AtlasTextureReference> backgroundFuture = 
            atlasManager.requestTextureAsync("background", texturePath,
                                           TextureAtlasManager.AtlasCategory.BLOCKS,
                                           TextureAtlasManager.StreamingPriority.BACKGROUND, null);
        
        // Critical should complete first (in theory)
        TextureAtlasManager.AtlasTextureReference criticalRef = criticalFuture.get(5, TimeUnit.SECONDS);
        TextureAtlasManager.AtlasTextureReference backgroundRef = backgroundFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(criticalRef);
        assertNotNull(backgroundRef);
    }
    
    @Test
    void testLODStatistics() throws ExecutionException, InterruptedException, TimeoutException {
        // Register a few LOD textures
        Map<LODTextureAtlasSystem.LODLevel, String> lodPaths = new HashMap<>();
        lodPaths.put(LODTextureAtlasSystem.LODLevel.MEDIUM, tempDir.resolve("test_lod_medium.png").toString());
        lodPaths.put(LODTextureAtlasSystem.LODLevel.LOW, tempDir.resolve("test_lod_low.png").toString());
        
        lodSystem.registerLODTexture("stats_test1", TextureAtlasManager.AtlasCategory.BLOCKS, lodPaths)
                .get(5, TimeUnit.SECONDS);
        lodSystem.registerLODTexture("stats_test2", TextureAtlasManager.AtlasCategory.ENTITIES, lodPaths)
                .get(5, TimeUnit.SECONDS);
        
        String stats = lodSystem.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("LOD Texture Atlas System Statistics"));
        assertTrue(stats.contains("Total LOD textures: 2"));
    }
    
    @Test
    void testCleanup() throws ExecutionException, InterruptedException, TimeoutException {
        // Load some textures
        atlasManager.requestTextureAsync("cleanup_test", tempDir.resolve("test_block.png").toString(),
                                       TextureAtlasManager.AtlasCategory.BLOCKS,
                                       TextureAtlasManager.StreamingPriority.MEDIUM, null)
                .get(5, TimeUnit.SECONDS);
        
        // Register LOD texture
        Map<LODTextureAtlasSystem.LODLevel, String> lodPaths = new HashMap<>();
        lodPaths.put(LODTextureAtlasSystem.LODLevel.MEDIUM, tempDir.resolve("test_lod_medium.png").toString());
        
        lodSystem.registerLODTexture("cleanup_lod", TextureAtlasManager.AtlasCategory.BLOCKS, lodPaths)
                .get(5, TimeUnit.SECONDS);
        
        // Cleanup
        lodSystem.cleanup();
        atlasManager.cleanup();
        streamingManager.shutdown();
        assetManager.shutdown();
        jobSystem.shutdown();
        
        // Should not throw exceptions
        assertTrue(true);
    }
}