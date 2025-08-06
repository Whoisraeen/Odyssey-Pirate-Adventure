package com.odyssey.graphics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;

/**
 * Unit tests for texture compression functionality.
 */
public class TextureCompressionTest {
    
    private ByteBuffer testImageData;
    private static final int TEST_WIDTH = 64;
    private static final int TEST_HEIGHT = 64;
    private static final int BYTES_PER_PIXEL = 4; // RGBA
    
    @BeforeEach
    public void setUp() {
        // Create test image data (64x64 RGBA)
        int dataSize = TEST_WIDTH * TEST_HEIGHT * BYTES_PER_PIXEL;
        testImageData = ByteBuffer.allocateDirect(dataSize);
        
        // Fill with gradient pattern for testing
        for (int y = 0; y < TEST_HEIGHT; y++) {
            for (int x = 0; x < TEST_WIDTH; x++) {
                byte r = (byte) ((x * 255) / TEST_WIDTH);
                byte g = (byte) ((y * 255) / TEST_HEIGHT);
                byte b = (byte) (((x + y) * 255) / (TEST_WIDTH + TEST_HEIGHT));
                byte a = (byte) 255; // Full alpha
                
                testImageData.put(r).put(g).put(b).put(a);
            }
        }
        testImageData.flip();
        
        // Reset compression statistics
        TextureCompression.CompressionStats.reset();
    }
    
    @AfterEach
    public void tearDown() {
        if (testImageData != null) {
            testImageData.clear();
        }
    }
    
    @Test
    public void testCompressionFormatEnum() {
        // Test that all compression formats have valid OpenGL constants
        assertNotNull(TextureCompression.CompressionFormat.DXT1.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.DXT1A.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.DXT3.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.DXT5.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.BC1.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.BC1A.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.BC2.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.BC3.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.RGB.getGLFormat());
        assertNotNull(TextureCompression.CompressionFormat.RGBA.getGLFormat());
    }
    
    @Test
    public void testCompressionCapabilities() {
        // Test without initializing OpenGL context - just test the enum and basic functionality
        // Should always support uncompressed formats
        assertTrue(TextureCompression.CompressionFormat.RGB.getGLFormat() > 0);
        assertTrue(TextureCompression.CompressionFormat.RGBA.getGLFormat() > 0);
        
        // Test format properties
        assertFalse(TextureCompression.CompressionFormat.RGB.hasAlpha());
        assertTrue(TextureCompression.CompressionFormat.RGBA.hasAlpha());
        assertTrue(TextureCompression.CompressionFormat.DXT1A.hasAlpha());
        assertTrue(TextureCompression.CompressionFormat.DXT5.hasAlpha());
    }
    
    @Test
    public void testCompressedTexture() {
        ByteBuffer compressedData = ByteBuffer.allocateDirect(1024);
        TextureCompression.CompressedTexture texture = 
            new TextureCompression.CompressedTexture(
                compressedData, 
                TEST_WIDTH, 
                TEST_HEIGHT,
                TextureCompression.CompressionFormat.DXT1
            );
        
        assertEquals(compressedData, texture.getData());
        assertEquals(TextureCompression.CompressionFormat.DXT1, texture.getFormat());
        assertEquals(TEST_WIDTH, texture.getWidth());
        assertEquals(TEST_HEIGHT, texture.getHeight());
        assertTrue(texture.getCompressionRatio() > 1.0f);
    }
    
    @Test
    public void testCompressionSizeCalculation() {
        // Test compressed size calculation for different formats
        int width = 64;
        int height = 64;
        
        // DXT1 should compress 4x4 blocks to 8 bytes each
        int dxt1Size = TextureCompression.CompressionFormat.DXT1.getCompressedSize(width, height);
        int expectedDXT1Size = (width / 4) * (height / 4) * 8;
        assertEquals(expectedDXT1Size, dxt1Size);
        
        // DXT5 should compress 4x4 blocks to 16 bytes each
        int dxt5Size = TextureCompression.CompressionFormat.DXT5.getCompressedSize(width, height);
        int expectedDXT5Size = (width / 4) * (height / 4) * 16;
        assertEquals(expectedDXT5Size, dxt5Size);
        
        // Uncompressed formats
        int rgbSize = TextureCompression.CompressionFormat.RGB.getCompressedSize(width, height);
        assertEquals(width * height * 3, rgbSize);
        
        int rgbaSize = TextureCompression.CompressionFormat.RGBA.getCompressedSize(width, height);
        assertEquals(width * height * 4, rgbaSize);
    }
    
    @Test
    public void testCompressionFormatProperties() {
        // Test DXT1 properties
        assertEquals(4, TextureCompression.CompressionFormat.DXT1.getBitsPerPixel());
        assertFalse(TextureCompression.CompressionFormat.DXT1.hasAlpha());
        
        // Test DXT1A properties
        assertEquals(4, TextureCompression.CompressionFormat.DXT1A.getBitsPerPixel());
        assertTrue(TextureCompression.CompressionFormat.DXT1A.hasAlpha());
        
        // Test DXT5 properties
        assertEquals(8, TextureCompression.CompressionFormat.DXT5.getBitsPerPixel());
        assertTrue(TextureCompression.CompressionFormat.DXT5.hasAlpha());
        
        // Test BC formats have same properties as DXT equivalents
        assertEquals(TextureCompression.CompressionFormat.DXT1.getBitsPerPixel(),
                    TextureCompression.CompressionFormat.BC1.getBitsPerPixel());
        assertEquals(TextureCompression.CompressionFormat.DXT5.getBitsPerPixel(),
                    TextureCompression.CompressionFormat.BC3.getBitsPerPixel());
    }
    
    @Test
    public void testFormatSelection() {
        // Test format selection logic without OpenGL context
        // Since we can't initialize OpenGL in unit tests, we'll test the logic
        
        // Test with different quality levels
        TextureCompression.CompressionFormat highQualityAlpha = 
            TextureCompression.selectBestFormat(true, 0.9f);
        assertNotNull(highQualityAlpha);
        
        TextureCompression.CompressionFormat lowQualityAlpha = 
            TextureCompression.selectBestFormat(true, 0.3f);
        assertNotNull(lowQualityAlpha);
        
        TextureCompression.CompressionFormat noAlpha = 
            TextureCompression.selectBestFormat(false, 0.8f);
        assertNotNull(noAlpha);
        
        // Without OpenGL context, should fall back to uncompressed formats
        assertEquals(TextureCompression.CompressionFormat.RGBA, highQualityAlpha);
        assertEquals(TextureCompression.CompressionFormat.RGBA, lowQualityAlpha);
        assertEquals(TextureCompression.CompressionFormat.RGB, noAlpha);
    }
    
    @Test
    public void testCompressionStatistics() {
        // Initial state
        assertEquals("No textures compressed yet", 
                    TextureCompression.CompressionStats.getOverallStats());
        assertEquals(0, TextureCompression.CompressionStats.getTotalMemorySaved());
        
        // Simulate recording compression statistics
        TextureCompression.CompressionStats.recordCompression(16384, 4096); // 4:1 compression
        
        // Check statistics were recorded
        String stats = TextureCompression.CompressionStats.getOverallStats();
        assertTrue(stats.contains("1 textures"));
        assertTrue(stats.contains("4.0:1 ratio"));
        assertTrue(TextureCompression.CompressionStats.getTotalMemorySaved() > 0);
        assertEquals(12288, TextureCompression.CompressionStats.getTotalMemorySaved());
        
        // Test reset
        TextureCompression.CompressionStats.reset();
        assertEquals("No textures compressed yet", 
                    TextureCompression.CompressionStats.getOverallStats());
    }
    
    @Test
    public void testCompressionQualityAnalyzer() {
        // Create a mock compressed texture for testing
        ByteBuffer mockData = ByteBuffer.allocateDirect(1024);
        TextureCompression.CompressedTexture mockTexture = 
            new TextureCompression.CompressedTexture(
                mockData, 
                TEST_WIDTH, 
                TEST_HEIGHT,
                TextureCompression.CompressionFormat.DXT1
            );
        
        double psnr = TextureCompression.CompressionQualityAnalyzer
            .calculatePSNR(testImageData, mockTexture);
        
        assertTrue(psnr > 0);
        assertTrue(psnr <= 50.0); // Should be reasonable for DXT1
        
        String quality = TextureCompression.CompressionQualityAnalyzer
            .assessQuality(psnr);
        assertNotNull(quality);
        assertTrue(quality.equals("Excellent") || quality.equals("Good") || 
                  quality.equals("Acceptable") || quality.equals("Poor") || 
                  quality.equals("Unacceptable"));
        
        // Test different quality levels
        assertEquals("Excellent", TextureCompression.CompressionQualityAnalyzer.assessQuality(45.0));
        assertEquals("Good", TextureCompression.CompressionQualityAnalyzer.assessQuality(37.0));
        assertEquals("Acceptable", TextureCompression.CompressionQualityAnalyzer.assessQuality(32.0));
        assertEquals("Poor", TextureCompression.CompressionQualityAnalyzer.assessQuality(27.0));
        assertEquals("Unacceptable", TextureCompression.CompressionQualityAnalyzer.assessQuality(20.0));
    }
    
    @Test
    public void testCompressionRatioCalculation() {
        // Test compression ratio calculation with mock data
        ByteBuffer smallData = ByteBuffer.allocateDirect(1024); // 1KB compressed
        TextureCompression.CompressedTexture texture = 
            new TextureCompression.CompressedTexture(
                smallData, 
                TEST_WIDTH, 
                TEST_HEIGHT,
                TextureCompression.CompressionFormat.DXT1
            );
        
        // Original size would be 64*64*4 = 16384 bytes
        // Compressed size is 1024 bytes
        // Ratio should be 16384/1024 = 16.0
        assertEquals(16.0f, texture.getCompressionRatio(), 0.01f);
    }
    
    @Test
    public void testMemorySavingsCalculation() {
        String savings = TextureCompression.calculateMemorySavings(16384, 4096);
        
        assertNotNull(savings);
        assertTrue(savings.contains("4.0:1 ratio"));
        assertTrue(savings.contains("75.0% memory saved"));
        assertTrue(savings.contains("16KB -> 4KB"));
    }
}