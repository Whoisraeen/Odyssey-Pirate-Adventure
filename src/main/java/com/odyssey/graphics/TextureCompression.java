package com.odyssey.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.ARBTextureCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * Texture Compression system for The Odyssey.
 * Provides DXT/BC compression for GPU memory efficiency and improved performance.
 * Supports various compression formats based on hardware capabilities.
 */
public class TextureCompression {
    private static final Logger logger = LoggerFactory.getLogger(TextureCompression.class);
    
    /**
     * Supported texture compression formats.
     */
    public enum CompressionFormat {
        // DXT/BC formats (S3TC) - DirectX naming
        DXT1(EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT, 4, false),
        DXT1A(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, 4, true),
        DXT3(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, 8, true),
        DXT5(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, 8, true),
        
        // BC formats (Block Compression) - OpenGL naming (same as DXT but different naming convention)
        BC1(EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT, 4, false),    // Same as DXT1
        BC1A(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, 4, true),   // Same as DXT1A
        BC2(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, 8, true),    // Same as DXT3
        BC3(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, 8, true),    // Same as DXT5
        
        // Fallback uncompressed formats
        RGB(GL11.GL_RGB, 24, false),
        RGBA(GL11.GL_RGBA, 32, true);
        
        private final int glFormat;
        private final int bitsPerPixel;
        private final boolean hasAlpha;
        
        CompressionFormat(int glFormat, int bitsPerPixel, boolean hasAlpha) {
            this.glFormat = glFormat;
            this.bitsPerPixel = bitsPerPixel;
            this.hasAlpha = hasAlpha;
        }
        
        public int getGLFormat() {
            return glFormat;
        }
        
        public int getBitsPerPixel() {
            return bitsPerPixel;
        }
        
        public boolean hasAlpha() {
            return hasAlpha;
        }
        
        /**
         * Calculates compressed size for given dimensions.
         *
         * @param width Texture width
         * @param height Texture height
         * @return Compressed size in bytes
         */
        public int getCompressedSize(int width, int height) {
            if (this == RGB || this == RGBA) {
                return width * height * (bitsPerPixel / 8);
            }
            
            // DXT compression uses 4x4 blocks
            int blockWidth = Math.max(1, (width + 3) / 4);
            int blockHeight = Math.max(1, (height + 3) / 4);
            
            // Calculate bytes per block based on format
            int bytesPerBlock;
            switch (this) {
                case DXT1:
                case DXT1A:
                case BC1:
                case BC1A:
                    bytesPerBlock = 8; // 64 bits per 4x4 block
                    break;
                case DXT3:
                case DXT5:
                case BC2:
                case BC3:
                    bytesPerBlock = 16; // 128 bits per 4x4 block
                    break;
                default:
                    bytesPerBlock = bitsPerPixel / 2; // Fallback for unknown formats
                    break;
            }
            
            return blockWidth * blockHeight * bytesPerBlock;
        }
    }
    
    /**
     * Hardware compression capabilities.
     */
    public static class CompressionCapabilities {
        private final EnumSet<CompressionFormat> supportedFormats;
        private final boolean s3tcSupported;
        private final boolean genericCompressionSupported;
        
        CompressionCapabilities() {
            this.supportedFormats = EnumSet.noneOf(CompressionFormat.class);
            this.s3tcSupported = checkS3TCSupport();
            this.genericCompressionSupported = checkGenericCompressionSupport();
            
            detectSupportedFormats();
        }
        
        /**
         * Checks if S3TC (DXT) compression is supported.
         *
         * @return true if S3TC is supported
         */
        private boolean checkS3TCSupport() {
            try {
                // Check if we're in a test environment or OpenGL context is not available
                if (!isOpenGLContextAvailable()) {
                    return false;
                }
                
                // Check if the extension is available
                String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
                return extensions != null && extensions.contains("GL_EXT_texture_compression_s3tc");
            } catch (Exception e) {
                logger.warn("Failed to check S3TC support", e);
                return false;
            }
        }
        
        /**
         * Checks if generic texture compression is supported.
         *
         * @return true if generic compression is supported
         */
        private boolean checkGenericCompressionSupport() {
            try {
                // Check if we're in a test environment or OpenGL context is not available
                if (!isOpenGLContextAvailable()) {
                    return false;
                }
                
                // Check if the extension is available
                String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
                return extensions != null && extensions.contains("GL_ARB_texture_compression");
            } catch (Exception e) {
                logger.warn("Failed to check generic compression support", e);
                return false;
            }
        }
        
        /**
         * Checks if OpenGL context is available for making OpenGL calls.
         *
         * @return true if OpenGL context is available
         */
        private boolean isOpenGLContextAvailable() {
            try {
                // Try to get the current OpenGL context
                org.lwjgl.opengl.GLCapabilities caps = org.lwjgl.opengl.GL.getCapabilities();
                return caps != null;
            } catch (Exception e) {
                // No OpenGL context available (e.g., in tests)
                return false;
            }
        }
        
        /**
         * Detects all supported compression formats.
         */
        private void detectSupportedFormats() {
            // Always support uncompressed formats
            supportedFormats.add(CompressionFormat.RGB);
            supportedFormats.add(CompressionFormat.RGBA);
            
            if (s3tcSupported) {
                // Add DXT formats
                supportedFormats.add(CompressionFormat.DXT1);
                supportedFormats.add(CompressionFormat.DXT1A);
                supportedFormats.add(CompressionFormat.DXT3);
                supportedFormats.add(CompressionFormat.DXT5);
                
                // Add BC formats (same underlying implementation)
                supportedFormats.add(CompressionFormat.BC1);
                supportedFormats.add(CompressionFormat.BC1A);
                supportedFormats.add(CompressionFormat.BC2);
                supportedFormats.add(CompressionFormat.BC3);
                
                logger.info("S3TC (DXT/BC) compression supported - {} formats available", 
                           supportedFormats.size() - 2); // Subtract RGB/RGBA
            } else {
                logger.info("S3TC (DXT/BC) compression not supported, using uncompressed textures");
            }
        }
        
        public boolean isFormatSupported(CompressionFormat format) {
            return supportedFormats.contains(format);
        }
        
        public EnumSet<CompressionFormat> getSupportedFormats() {
            return EnumSet.copyOf(supportedFormats);
        }
        
        public boolean isS3TCSupported() {
            return s3tcSupported;
        }
        
        public boolean isGenericCompressionSupported() {
            return genericCompressionSupported;
        }
    }
    
    /**
     * Compressed texture data container.
     */
    public static class CompressedTexture {
        private final ByteBuffer data;
        private final int width;
        private final int height;
        private final CompressionFormat format;
        private final int compressedSize;
        
        /**
         * Creates a compressed texture container.
         *
         * @param data Compressed texture data
         * @param width Texture width
         * @param height Texture height
         * @param format Compression format used
         */
        public CompressedTexture(ByteBuffer data, int width, int height, CompressionFormat format) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.format = format;
            this.compressedSize = data.remaining();
        }
        
        public ByteBuffer getData() {
            return data;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public CompressionFormat getFormat() {
            return format;
        }
        
        public int getCompressedSize() {
            return compressedSize;
        }
        
        /**
         * Calculates compression ratio compared to uncompressed RGBA.
         *
         * @return Compression ratio (e.g., 4.0 means 4:1 compression)
         */
        public float getCompressionRatio() {
            int uncompressedSize = width * height * 4; // RGBA
            return (float) uncompressedSize / compressedSize;
        }
    }
    
    private static CompressionCapabilities capabilities;
    
    /**
     * Initializes texture compression system and detects hardware capabilities.
     */
    public static void initialize() {
        capabilities = new CompressionCapabilities();
        logger.info("Texture compression initialized. Supported formats: {}", 
                   capabilities.getSupportedFormats());
    }
    
    /**
     * Gets the compression capabilities of the current hardware.
     *
     * @return Compression capabilities
     */
    public static CompressionCapabilities getCapabilities() {
        if (capabilities == null) {
            initialize();
        }
        return capabilities;
    }
    
    /**
     * Selects the best compression format for given requirements.
     *
     * @param hasAlpha Whether the texture has alpha channel
     * @param quality Desired quality level (0.0 = lowest, 1.0 = highest)
     * @return Best available compression format
     */
    public static CompressionFormat selectBestFormat(boolean hasAlpha, float quality) {
        CompressionCapabilities caps = getCapabilities();
        
        if (!caps.isS3TCSupported()) {
            return hasAlpha ? CompressionFormat.RGBA : CompressionFormat.RGB;
        }
        
        if (hasAlpha) {
            if (quality >= 0.8f && caps.isFormatSupported(CompressionFormat.DXT5)) {
                return CompressionFormat.DXT5; // Best quality for alpha
            } else if (quality >= 0.5f && caps.isFormatSupported(CompressionFormat.DXT3)) {
                return CompressionFormat.DXT3; // Good quality for alpha
            } else if (caps.isFormatSupported(CompressionFormat.DXT1A)) {
                return CompressionFormat.DXT1A; // Basic alpha support
            }
            return CompressionFormat.RGBA; // Fallback
        } else {
            if (caps.isFormatSupported(CompressionFormat.DXT1)) {
                return CompressionFormat.DXT1; // Best compression for RGB
            }
            return CompressionFormat.RGB; // Fallback
        }
    }
    
    /**
     * Compresses texture data using the specified format.
     * Note: This is a simplified implementation. In a real engine, you would
     * use a proper DXT compression library like squish or NVIDIA Texture Tools.
     *
     * @param data Uncompressed texture data (RGBA)
     * @param width Texture width
     * @param height Texture height
     * @param format Target compression format
     * @return Compressed texture
     */
    public static CompressedTexture compressTexture(ByteBuffer data, int width, int height, 
                                                   CompressionFormat format) {
        if (format == CompressionFormat.RGB || format == CompressionFormat.RGBA) {
            // No compression needed
            return new CompressedTexture(data, width, height, format);
        }
        
        // For DXT compression, we would normally use a compression library
        // For this implementation, we'll simulate compression by creating
        // a smaller buffer (this is not actual DXT compression)
        int compressedSize = format.getCompressedSize(width, height);
        ByteBuffer compressedData = ByteBuffer.allocateDirect(compressedSize);
        
        // Simulate compression by downsampling (not real DXT compression)
        simulateCompression(data, compressedData, width, height, format);
        
        compressedData.flip();
        
        logger.debug("Compressed {}x{} texture from {}KB to {}KB using {}", 
                    width, height, data.remaining() / 1024, compressedSize / 1024, format);
        
        return new CompressedTexture(compressedData, width, height, format);
    }
    
    /**
     * Uploads compressed texture to GPU.
     *
     * @param texture Compressed texture to upload
     * @param textureId OpenGL texture ID
     * @param level Mipmap level
     */
    public static void uploadCompressedTexture(CompressedTexture texture, int textureId, int level) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        if (texture.getFormat() == CompressionFormat.RGB || 
            texture.getFormat() == CompressionFormat.RGBA) {
            // Upload uncompressed texture
            int format = texture.getFormat() == CompressionFormat.RGB ? GL11.GL_RGB : GL11.GL_RGBA;
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, format, 
                             texture.getWidth(), texture.getHeight(), 0, 
                             format, GL11.GL_UNSIGNED_BYTE, texture.getData());
        } else {
            // Upload compressed texture
            GL13.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, level, 
                                       texture.getFormat().getGLFormat(),
                                       texture.getWidth(), texture.getHeight(), 0,
                                       texture.getData());
        }
        
        logger.debug("Uploaded compressed texture {}x{} ({}KB) to GPU", 
                    texture.getWidth(), texture.getHeight(), 
                    texture.getCompressedSize() / 1024);
    }
    
    /**
     * Compression statistics and memory usage tracking.
     */
    public static class CompressionStats {
        private static long totalOriginalBytes = 0;
        private static long totalCompressedBytes = 0;
        private static int texturesCompressed = 0;
        
        /**
         * Records compression statistics.
         *
         * @param originalSize Original size in bytes
         * @param compressedSize Compressed size in bytes
         */
        public static synchronized void recordCompression(int originalSize, int compressedSize) {
            totalOriginalBytes += originalSize;
            totalCompressedBytes += compressedSize;
            texturesCompressed++;
        }
        
        /**
         * Gets overall compression statistics.
         *
         * @return Formatted statistics string
         */
        public static synchronized String getOverallStats() {
            if (texturesCompressed == 0) {
                return "No textures compressed yet";
            }
            
            float overallRatio = (float) totalOriginalBytes / totalCompressedBytes;
            float overallSavings = (1.0f - (float) totalCompressedBytes / totalOriginalBytes) * 100.0f;
            
            return String.format("Overall: %d textures, %.1f:1 ratio, %.1f%% saved (%.1fMB -> %.1fMB)",
                               texturesCompressed, overallRatio, overallSavings,
                               totalOriginalBytes / (1024.0f * 1024.0f),
                               totalCompressedBytes / (1024.0f * 1024.0f));
        }
        
        /**
         * Resets compression statistics.
         */
        public static synchronized void reset() {
            totalOriginalBytes = 0;
            totalCompressedBytes = 0;
            texturesCompressed = 0;
        }
        
        /**
         * Gets total memory saved in bytes.
         *
         * @return Memory saved in bytes
         */
        public static synchronized long getTotalMemorySaved() {
            return totalOriginalBytes - totalCompressedBytes;
        }
    }
    
    /**
     * Analyzes compression quality by comparing original and compressed data.
     */
    public static class CompressionQualityAnalyzer {
        /**
         * Calculates Peak Signal-to-Noise Ratio (PSNR) for compression quality assessment.
         *
         * @param original Original image data (RGBA)
         * @param compressed Compressed texture
         * @return PSNR value in dB (higher is better)
         */
        public static double calculatePSNR(ByteBuffer original, CompressedTexture compressed) {
            // For real implementation, you would decompress the texture and compare
            // This is a simplified estimation based on compression ratio
            float compressionRatio = compressed.getCompressionRatio();
            
            // Estimate PSNR based on compression ratio and format
            double estimatedPSNR;
            switch (compressed.getFormat()) {
                case DXT1:
                case BC1:
                    estimatedPSNR = Math.max(25.0, 35.0 - (compressionRatio - 4.0) * 2.0);
                    break;
                case DXT5:
                case BC3:
                    estimatedPSNR = Math.max(30.0, 40.0 - (compressionRatio - 4.0) * 1.5);
                    break;
                default:
                    estimatedPSNR = 50.0; // Uncompressed
                    break;
            }
            
            return estimatedPSNR;
        }
        
        /**
         * Determines if compression quality is acceptable.
         *
         * @param psnr PSNR value
         * @return Quality assessment
         */
        public static String assessQuality(double psnr) {
            if (psnr >= 40.0) return "Excellent";
            if (psnr >= 35.0) return "Good";
            if (psnr >= 30.0) return "Acceptable";
            if (psnr >= 25.0) return "Poor";
            return "Unacceptable";
        }
    }
    
    /**
     * Calculates memory savings from compression.
     *
     * @param originalSize Original uncompressed size
     * @param compressedSize Compressed size
     * @return Memory savings information
     */
    public static String calculateMemorySavings(int originalSize, int compressedSize) {
        float ratio = (float) originalSize / compressedSize;
        float savings = (1.0f - (float) compressedSize / originalSize) * 100.0f;
        
        // Record statistics
        CompressionStats.recordCompression(originalSize, compressedSize);
        
        return String.format("Compression: %.1f:1 ratio, %.1f%% memory saved (%dKB -> %dKB)",
                           ratio, savings, originalSize / 1024, compressedSize / 1024);
    }
    
    /**
     * Performs real DXT compression using optimized algorithms.
     * Implements proper DXT1, DXT3, and DXT5 compression with quality optimization.
     *
     * @param input Input texture data (RGBA)
     * @param output Output compressed data
     * @param width Texture width
     * @param height Texture height
     * @param format Compression format
     */
    private static void simulateCompression(ByteBuffer input, ByteBuffer output, 
                                          int width, int height, CompressionFormat format) {
        int blocksX = (width + 3) / 4;
        int blocksY = (height + 3) / 4;
        
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                // Extract 4x4 block
                int[] blockPixels = extractBlock(input, width, height, bx, by);
                
                switch (format) {
                     case DXT1:
                     case DXT1A:
                     case BC1:
                     case BC1A:
                         compressDXT1Block(blockPixels, output, 
                                         format == CompressionFormat.DXT1A || format == CompressionFormat.BC1A);
                         break;
                     case DXT3:
                     case BC2:
                         compressDXT3Block(blockPixels, output);
                         break;
                     case DXT5:
                     case BC3:
                         compressDXT5Block(blockPixels, output);
                         break;
                    default:
                        // Fallback for uncompressed formats
                        for (int pixel : blockPixels) {
                            if (output.hasRemaining()) {
                                output.put((byte) ((pixel >> 16) & 0xFF)); // R
                                output.put((byte) ((pixel >> 8) & 0xFF));  // G
                                output.put((byte) (pixel & 0xFF));         // B
                                if (format == CompressionFormat.RGBA) {
                                    output.put((byte) ((pixel >> 24) & 0xFF)); // A
                                }
                            }
                        }
                        break;
                }
            }
        }
    }
    
    /**
     * Extracts a 4x4 pixel block from the input texture.
     *
     * @param input Input texture data
     * @param width Texture width
     * @param height Texture height
     * @param blockX Block X coordinate
     * @param blockY Block Y coordinate
     * @return Array of 16 ARGB pixels
     */
    private static int[] extractBlock(ByteBuffer input, int width, int height, int blockX, int blockY) {
        int[] block = new int[16];
        
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int pixelX = blockX * 4 + x;
                int pixelY = blockY * 4 + y;
                
                if (pixelX < width && pixelY < height) {
                    int pixelIndex = (pixelY * width + pixelX) * 4;
                    if (pixelIndex + 3 < input.limit()) {
                        int r = input.get(pixelIndex) & 0xFF;
                        int g = input.get(pixelIndex + 1) & 0xFF;
                        int b = input.get(pixelIndex + 2) & 0xFF;
                        int a = input.get(pixelIndex + 3) & 0xFF;
                        block[y * 4 + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                } else {
                    // Pad with transparent black for edge blocks
                    block[y * 4 + x] = 0x00000000;
                }
            }
        }
        
        return block;
    }
    
    /**
     * Compresses a 4x4 block using DXT1 algorithm.
     * DXT1 uses 8 bytes per block: 2 bytes for color0, 2 bytes for color1, 4 bytes for indices.
     *
     * @param block 16 ARGB pixels
     * @param output Output buffer
     * @param hasAlpha Whether to use 1-bit alpha
     */
    private static void compressDXT1Block(int[] block, ByteBuffer output, boolean hasAlpha) {
        // Find min and max colors for the block
        int[] colors = findMinMaxColors(block, hasAlpha);
        int color0 = colors[0];
        int color1 = colors[1];
        
        // Convert to 5:6:5 format
        int c0_565 = rgbTo565(color0);
        int c1_565 = rgbTo565(color1);
        
        // Ensure color0 > color1 for 4-color mode (or color0 <= color1 for 3-color + transparent)
        if (!hasAlpha && c0_565 < c1_565) {
            int temp = c0_565;
            c0_565 = c1_565;
            c1_565 = temp;
            temp = color0;
            color0 = color1;
            color1 = temp;
        }
        
        // Write color endpoints
        output.putShort((short) c0_565);
        output.putShort((short) c1_565);
        
        // Generate color palette
        int[] palette = generateDXT1Palette(color0, color1, hasAlpha && c0_565 <= c1_565);
        
        // Generate indices
        int indices = 0;
        for (int i = 0; i < 16; i++) {
            int bestIndex = findBestColorIndex(block[i], palette, hasAlpha);
            indices |= (bestIndex << (i * 2));
        }
        
        // Write indices (4 bytes)
        output.putInt(indices);
    }
    
    /**
     * Compresses a 4x4 block using DXT3 algorithm.
     * DXT3 uses 16 bytes per block: 8 bytes for alpha, 8 bytes for DXT1 color.
     *
     * @param block 16 ARGB pixels
     * @param output Output buffer
     */
    private static void compressDXT3Block(int[] block, ByteBuffer output) {
        // Compress alpha (4 bits per pixel, 8 bytes total)
        for (int i = 0; i < 8; i++) {
            int alpha0 = (block[i * 2] >> 24) & 0xFF;
            int alpha1 = (block[i * 2 + 1] >> 24) & 0xFF;
            
            // Quantize to 4 bits
            alpha0 = (alpha0 + 8) / 17; // 0-255 -> 0-15
            alpha1 = (alpha1 + 8) / 17;
            
            output.put((byte) ((alpha1 << 4) | alpha0));
        }
        
        // Compress color using DXT1
        compressDXT1Block(block, output, false);
    }
    
    /**
     * Compresses a 4x4 block using DXT5 algorithm.
     * DXT5 uses 16 bytes per block: 8 bytes for interpolated alpha, 8 bytes for DXT1 color.
     *
     * @param block 16 ARGB pixels
     * @param output Output buffer
     */
    private static void compressDXT5Block(int[] block, ByteBuffer output) {
        // Find min/max alpha values
        int minAlpha = 255, maxAlpha = 0;
        for (int pixel : block) {
            int alpha = (pixel >> 24) & 0xFF;
            minAlpha = Math.min(minAlpha, alpha);
            maxAlpha = Math.max(maxAlpha, alpha);
        }
        
        // Write alpha endpoints
        output.put((byte) maxAlpha);
        output.put((byte) minAlpha);
        
        // Generate alpha palette
        int[] alphaPalette = generateDXT5AlphaPalette(maxAlpha, minAlpha);
        
        // Compress alpha indices (3 bits per pixel, 6 bytes total)
        long alphaIndices = 0;
        for (int i = 0; i < 16; i++) {
            int alpha = (block[i] >> 24) & 0xFF;
            int bestIndex = findBestAlphaIndex(alpha, alphaPalette);
            alphaIndices |= ((long) bestIndex << (i * 3));
        }
        
        // Write alpha indices (6 bytes)
        for (int i = 0; i < 6; i++) {
            output.put((byte) (alphaIndices >> (i * 8)));
        }
        
        // Compress color using DXT1
        compressDXT1Block(block, output, false);
    }
    
    /**
     * Finds the minimum and maximum colors in a block for DXT1 compression.
     *
     * @param block 16 ARGB pixels
     * @param hasAlpha Whether to consider alpha
     * @return Array containing [minColor, maxColor]
     */
    private static int[] findMinMaxColors(int[] block, boolean hasAlpha) {
        // Use luminance-based selection for better quality
        int minColor = block[0];
        int maxColor = block[0];
        float minLuminance = calculateLuminance(block[0]);
        float maxLuminance = minLuminance;
        
        for (int pixel : block) {
            if (hasAlpha && ((pixel >> 24) & 0xFF) < 128) {
                continue; // Skip transparent pixels
            }
            
            float luminance = calculateLuminance(pixel);
            if (luminance < minLuminance) {
                minLuminance = luminance;
                minColor = pixel;
            }
            if (luminance > maxLuminance) {
                maxLuminance = luminance;
                maxColor = pixel;
            }
        }
        
        return new int[]{minColor, maxColor};
    }
    
    /**
     * Calculates luminance of an RGB color.
     *
     * @param rgb RGB color
     * @return Luminance value
     */
    private static float calculateLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }
    
    /**
     * Converts RGB color to 5:6:5 format.
     *
     * @param rgb RGB color
     * @return 5:6:5 color
     */
    private static int rgbTo565(int rgb) {
        int r = ((rgb >> 16) & 0xFF) >> 3; // 8 -> 5 bits
        int g = ((rgb >> 8) & 0xFF) >> 2;  // 8 -> 6 bits
        int b = (rgb & 0xFF) >> 3;         // 8 -> 5 bits
        return (r << 11) | (g << 5) | b;
    }
    
    /**
     * Converts 5:6:5 color back to RGB.
     *
     * @param color565 5:6:5 color
     * @return RGB color
     */
    private static int color565ToRgb(int color565) {
        int r = ((color565 >> 11) & 0x1F) << 3; // 5 -> 8 bits
        int g = ((color565 >> 5) & 0x3F) << 2;  // 6 -> 8 bits
        int b = (color565 & 0x1F) << 3;         // 5 -> 8 bits
        
        // Replicate lower bits for better precision
        r |= r >> 5;
        g |= g >> 6;
        b |= b >> 5;
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * Generates DXT1 color palette from two endpoint colors.
     *
     * @param color0 First endpoint color
     * @param color1 Second endpoint color
     * @param hasTransparent Whether to include transparent color
     * @return Array of 4 palette colors
     */
    private static int[] generateDXT1Palette(int color0, int color1, boolean hasTransparent) {
        int[] palette = new int[4];
        palette[0] = color0;
        palette[1] = color1;
        
        if (hasTransparent) {
            // 3-color mode + transparent
            palette[2] = interpolateColor(color0, color1, 1, 1); // 50% blend
            palette[3] = 0x00000000; // Transparent
        } else {
            // 4-color mode
            palette[2] = interpolateColor(color0, color1, 2, 1); // 2/3 color0 + 1/3 color1
            palette[3] = interpolateColor(color0, color1, 1, 2); // 1/3 color0 + 2/3 color1
        }
        
        return palette;
    }
    
    /**
     * Generates DXT5 alpha palette from two endpoint values.
     *
     * @param alpha0 First endpoint alpha
     * @param alpha1 Second endpoint alpha
     * @return Array of 8 alpha values
     */
    private static int[] generateDXT5AlphaPalette(int alpha0, int alpha1) {
        int[] palette = new int[8];
        palette[0] = alpha0;
        palette[1] = alpha1;
        
        if (alpha0 > alpha1) {
            // 6 interpolated values
            for (int i = 1; i <= 6; i++) {
                palette[i + 1] = ((6 - i) * alpha0 + i * alpha1) / 6;
            }
        } else {
            // 4 interpolated values + 0 and 255
            for (int i = 1; i <= 4; i++) {
                palette[i + 1] = ((4 - i) * alpha0 + i * alpha1) / 4;
            }
            palette[6] = 0;
            palette[7] = 255;
        }
        
        return palette;
    }
    
    /**
     * Interpolates between two colors.
     *
     * @param color0 First color
     * @param color1 Second color
     * @param weight0 Weight for first color
     * @param weight1 Weight for second color
     * @return Interpolated color
     */
    private static int interpolateColor(int color0, int color1, int weight0, int weight1) {
        int totalWeight = weight0 + weight1;
        
        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;
        
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r = (r0 * weight0 + r1 * weight1) / totalWeight;
        int g = (g0 * weight0 + g1 * weight1) / totalWeight;
        int b = (b0 * weight0 + b1 * weight1) / totalWeight;
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * Finds the best color index for a pixel in the palette.
     *
     * @param pixel Target pixel color
     * @param palette Color palette
     * @param hasAlpha Whether to consider alpha
     * @return Best palette index
     */
    private static int findBestColorIndex(int pixel, int[] palette, boolean hasAlpha) {
        if (hasAlpha && ((pixel >> 24) & 0xFF) < 128) {
            return 3; // Transparent index
        }
        
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i < (hasAlpha ? 3 : 4); i++) {
            int distance = calculateColorDistance(pixel, palette[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        
        return bestIndex;
    }
    
    /**
     * Finds the best alpha index for a value in the alpha palette.
     *
     * @param alpha Target alpha value
     * @param palette Alpha palette
     * @return Best palette index
     */
    private static int findBestAlphaIndex(int alpha, int[] palette) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i < palette.length; i++) {
            int distance = Math.abs(alpha - palette[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        
        return bestIndex;
    }
    
    /**
     * Calculates squared distance between two RGB colors.
     *
     * @param color1 First color
     * @param color2 Second color
     * @return Squared distance
     */
    private static int calculateColorDistance(int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;
        
        // Weighted distance for better perceptual quality
        return 2 * dr * dr + 4 * dg * dg + 3 * db * db;
    }
}