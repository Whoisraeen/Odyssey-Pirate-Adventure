package com.odyssey.graphics;

import com.odyssey.core.jobs.JobSystem;
import com.odyssey.core.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentPipeline.
 */
public class ContentPipelineTest {
    
    private ContentPipeline contentPipeline;
    private AssetManager assetManager;
    private TextureAtlasManager atlasManager;
    private JobSystem jobSystem;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        GameConfig config = new GameConfig();
        jobSystem = new JobSystem(config);
        assetManager = new AssetManager(64, 4, jobSystem);
        StreamingTextureManager streamingManager = new StreamingTextureManager(assetManager, jobSystem, 64, 128);
        atlasManager = new TextureAtlasManager(streamingManager);
        contentPipeline = new ContentPipeline(assetManager, atlasManager, jobSystem);
    }
    
    @AfterEach
    void tearDown() {
        if (contentPipeline != null) {
            contentPipeline.shutdown();
        }
        if (jobSystem != null) {
            jobSystem.shutdown(5000);
        }
    }
    
    @Test
    void testAssetDiscovery() throws Exception {
        // Create test texture files
        createTestTexture(tempDir.resolve("diffuse.png"), 64, 64, Color.RED);
        createTestTexture(tempDir.resolve("normal_n.png"), 64, 64, Color.BLUE);
        createTestTexture(tempDir.resolve("specular_s.png"), 32, 32, Color.GREEN);
        
        // Add resource pack and wait for scanning
        CompletableFuture<Void> scanFuture = contentPipeline.addResourcePack(tempDir.toString());
        scanFuture.get(5, TimeUnit.SECONDS);
        
        // Verify assets were discovered
        Map<String, ContentPipeline.AssetInfo> assets = contentPipeline.getDiscoveredAssets();
        assertEquals(3, assets.size());
        
        // Check diffuse texture
        ContentPipeline.AssetInfo diffuse = assets.get("diffuse.png");
        assertNotNull(diffuse);
        assertEquals("diffuse.png", diffuse.getName());
        assertEquals(ContentPipeline.AssetType.TEXTURE_DIFFUSE, diffuse.getType());
        assertEquals(64, diffuse.getWidth());
        assertEquals(64, diffuse.getHeight());
        
        // Check normal texture
        ContentPipeline.AssetInfo normal = assets.get("normal_n.png");
        assertNotNull(normal);
        assertEquals(ContentPipeline.AssetType.TEXTURE_NORMAL, normal.getType());
        
        // Check specular texture
        ContentPipeline.AssetInfo specular = assets.get("specular_s.png");
        assertNotNull(specular);
        assertEquals(ContentPipeline.AssetType.TEXTURE_SPECULAR, specular.getType());
        assertEquals(32, specular.getWidth());
        assertEquals(32, specular.getHeight());
    }
    
    @Test
    void testTextureValidation() throws Exception {
        // Create valid texture (power of 2, square)
        createTestTexture(tempDir.resolve("valid.png"), 64, 64, Color.WHITE);
        
        // Create invalid texture (non-power of 2)
        createTestTexture(tempDir.resolve("invalid.png"), 100, 100, Color.BLACK);
        
        // Create non-square texture
        createTestTexture(tempDir.resolve("nonsquare.png"), 64, 32, Color.GRAY);
        
        // Scan and validate
        CompletableFuture<Void> scanFuture = contentPipeline.addResourcePack(tempDir.toString());
        scanFuture.get(5, TimeUnit.SECONDS);
        
        Map<String, ContentPipeline.ValidationResult> results = contentPipeline.getValidationResults();
        assertEquals(3, results.size());
        
        // Valid texture should pass
        ContentPipeline.ValidationResult validResult = results.get("valid.png");
        assertNotNull(validResult);
        assertTrue(validResult.isValid());
        assertTrue(validResult.getErrors().isEmpty());
        
        // Invalid texture should have warnings
        ContentPipeline.ValidationResult invalidResult = results.get("invalid.png");
        assertNotNull(invalidResult);
        assertTrue(invalidResult.isValid()); // Still valid, just warnings
        assertFalse(invalidResult.getWarnings().isEmpty());
        
        // Non-square texture should have warnings
        ContentPipeline.ValidationResult nonsquareResult = results.get("nonsquare.png");
        assertNotNull(nonsquareResult);
        assertTrue(nonsquareResult.isValid());
        assertFalse(nonsquareResult.getWarnings().isEmpty());
    }
    
    @Test
    void testAssetTypeDetection() throws Exception {
        // Create textures with different naming conventions
        createTestTexture(tempDir.resolve("block_diffuse.png"), 16, 16, Color.RED);
        createTestTexture(tempDir.resolve("block_normal.png"), 16, 16, Color.BLUE);
        createTestTexture(tempDir.resolve("block_specular.png"), 16, 16, Color.GREEN);
        createTestTexture(tempDir.resolve("block_emission.png"), 16, 16, Color.YELLOW);
        createTestTexture(tempDir.resolve("water_animated.png"), 16, 16, Color.CYAN);
        
        // Scan assets
        CompletableFuture<Void> scanFuture = contentPipeline.addResourcePack(tempDir.toString());
        scanFuture.get(5, TimeUnit.SECONDS);
        
        Map<String, ContentPipeline.AssetInfo> assets = contentPipeline.getDiscoveredAssets();
        
        assertEquals(ContentPipeline.AssetType.TEXTURE_DIFFUSE, 
                    assets.get("block_diffuse.png").getType());
        assertEquals(ContentPipeline.AssetType.TEXTURE_NORMAL, 
                    assets.get("block_normal.png").getType());
        assertEquals(ContentPipeline.AssetType.TEXTURE_SPECULAR, 
                    assets.get("block_specular.png").getType());
        assertEquals(ContentPipeline.AssetType.TEXTURE_EMISSION, 
                    assets.get("block_emission.png").getType());
        assertEquals(ContentPipeline.AssetType.TEXTURE_ANIMATED, 
                    assets.get("water_animated.png").getType());
    }
    
    @Test
    void testFileWatching() throws Exception {
        // Add resource pack
        CompletableFuture<Void> scanFuture = contentPipeline.addResourcePack(tempDir.toString());
        scanFuture.get(5, TimeUnit.SECONDS);
        
        // Initially no assets
        assertEquals(0, contentPipeline.getDiscoveredAssets().size());
        
        // Create a new texture file
        createTestTexture(tempDir.resolve("new_texture.png"), 32, 32, Color.MAGENTA);
        
        // Wait for file watcher to detect the change
        Thread.sleep(1000);
        
        // Verify new asset was discovered
        Map<String, ContentPipeline.AssetInfo> assets = contentPipeline.getDiscoveredAssets();
        assertTrue(assets.containsKey("new_texture.png"));
        
        // Delete the file
        Files.delete(tempDir.resolve("new_texture.png"));
        
        // Wait for file watcher to detect the deletion
        Thread.sleep(1000);
        
        // Verify asset was removed
        assets = contentPipeline.getDiscoveredAssets();
        assertFalse(assets.containsKey("new_texture.png"));
    }
    
    @Test
    void testReportGeneration() throws Exception {
        // Create various test textures
        createTestTexture(tempDir.resolve("diffuse1.png"), 64, 64, Color.RED);
        createTestTexture(tempDir.resolve("diffuse2.png"), 32, 32, Color.GREEN);
        createTestTexture(tempDir.resolve("normal_n.png"), 64, 64, Color.BLUE);
        createTestTexture(tempDir.resolve("invalid.png"), 100, 100, Color.BLACK); // Non-power of 2
        
        // Scan assets
        CompletableFuture<Void> scanFuture = contentPipeline.addResourcePack(tempDir.toString());
        scanFuture.get(5, TimeUnit.SECONDS);
        
        // Generate report
        String report = contentPipeline.generateReport();
        
        assertNotNull(report);
        assertTrue(report.contains("Content Pipeline Report"));
        assertTrue(report.contains("Total Assets: 4"));
        assertTrue(report.contains("TEXTURE_DIFFUSE: 2"));
        assertTrue(report.contains("TEXTURE_NORMAL: 1"));
        assertTrue(report.contains("Valid: 4")); // All should be valid (warnings don't make invalid)
    }
    
    @Test
    void testMultipleResourcePacks() throws Exception {
        // Create two resource pack directories
        Path pack1 = tempDir.resolve("pack1");
        Path pack2 = tempDir.resolve("pack2");
        Files.createDirectories(pack1);
        Files.createDirectories(pack2);
        
        // Create textures in each pack
        createTestTexture(pack1.resolve("texture1.png"), 16, 16, Color.RED);
        createTestTexture(pack2.resolve("texture2.png"), 32, 32, Color.BLUE);
        
        // Add both resource packs
        CompletableFuture<Void> scan1 = contentPipeline.addResourcePack(pack1.toString());
        CompletableFuture<Void> scan2 = contentPipeline.addResourcePack(pack2.toString());
        
        CompletableFuture.allOf(scan1, scan2).get(5, TimeUnit.SECONDS);
        
        // Verify both textures were discovered
        Map<String, ContentPipeline.AssetInfo> assets = contentPipeline.getDiscoveredAssets();
        assertEquals(2, assets.size());
        assertTrue(assets.containsKey("texture1.png"));
        assertTrue(assets.containsKey("texture2.png"));
    }
    
    /**
     * Creates a test texture file.
     *
     * @param path Path to create the texture at
     * @param width Texture width
     * @param height Texture height
     * @param color Fill color
     * @throws IOException If creation fails
     */
    private void createTestTexture(Path path, int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        
        ImageIO.write(image, "PNG", path.toFile());
    }
}