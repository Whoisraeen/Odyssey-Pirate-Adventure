package com.odyssey.ui;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * TrueType font renderer using STB TrueType.
 * Provides high-quality text rendering with proper glyph metrics and kerning.
 */
public class FontRenderer {
    private static final Logger logger = LoggerFactory.getLogger(FontRenderer.class);
    
    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final int FIRST_CHAR = 32; // Space character
    private static final int NUM_CHARS = 96;  // ASCII printable characters
    
    private final Map<String, Font> loadedFonts = new HashMap<>();
    private Font defaultFont;
    
    // Shader for text rendering
    private int shaderProgram;
    private int vao, vbo;
    private int projectionUniform;
    private int textureUniform;
    private int colorUniform;
    
    /**
     * Represents a loaded font with its texture atlas and glyph data.
     */
    public static class Font {
        private final String name;
        private final float size;
        private final int textureId;
        private final STBTTBakedChar.Buffer charData;
        private final STBTTFontinfo fontInfo;
        private final ByteBuffer fontBuffer;
        private final float ascent;
        private final float descent;
        private final float lineGap;
        
        Font(String name, float size, int textureId, STBTTBakedChar.Buffer charData, 
             STBTTFontinfo fontInfo, ByteBuffer fontBuffer, float ascent, float descent, float lineGap) {
            this.name = name;
            this.size = size;
            this.textureId = textureId;
            this.charData = charData;
            this.fontInfo = fontInfo;
            this.fontBuffer = fontBuffer;
            this.ascent = ascent;
            this.descent = descent;
            this.lineGap = lineGap;
        }
        
        public String getName() { return name; }
        public float getSize() { return size; }
        public int getTextureId() { return textureId; }
        public STBTTBakedChar.Buffer getCharData() { return charData; }
        public float getLineHeight() { return ascent - descent + lineGap; }
        public float getAscent() { return ascent; }
        public float getDescent() { return descent; }
        
        public void cleanup() {
            if (textureId != 0) {
                glDeleteTextures(textureId);
            }
            if (charData != null) {
                charData.free();
            }
            if (fontBuffer != null) {
                MemoryUtil.memFree(fontBuffer);
            }
        }
    }
    
    /**
     * Initializes the font renderer.
     */
    public void initialize() {
        logger.info("Initializing FontRenderer");
        
        createShader();
        createBuffers();
        
        // Load default font
        try {
            loadDefaultFont();
        } catch (Exception e) {
            logger.error("Failed to load default font", e);
            throw new RuntimeException("Could not initialize font renderer", e);
        }
        
        logger.info("FontRenderer initialized successfully");
    }
    
    /**
     * Loads the default system font.
     */
    private void loadDefaultFont() throws IOException {
        // Try to load a system font or embedded font
        String[] fontPaths = {
            "/fonts/arial.ttf",
            "/fonts/default.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/System/Library/Fonts/Arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        };
        
        for (String fontPath : fontPaths) {
            try {
                if (fontPath.startsWith("/")) {
                    // Try to load from resources
                    InputStream is = getClass().getResourceAsStream(fontPath);
                    if (is != null) {
                        defaultFont = loadFontFromStream("default", is, 16.0f);
                        logger.info("Loaded default font from resources: {}", fontPath);
                        return;
                    }
                } else {
                    // Try to load from file system
                    java.io.File file = new java.io.File(fontPath);
                    if (file.exists()) {
                        defaultFont = loadFontFromFile("default", fontPath, 16.0f);
                        logger.info("Loaded default font from file: {}", fontPath);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not load font from {}: {}", fontPath, e.getMessage());
            }
        }
        
        // If no system font found, create a simple bitmap font
        logger.warn("No system font found, creating fallback bitmap font");
        defaultFont = createFallbackFont();
    }
    
    /**
     * Creates a simple fallback font when no TrueType fonts are available.
     */
    private Font createFallbackFont() {
        // Create a simple 8x8 bitmap font texture
        ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H);
        
        // Fill with a simple pattern for each character
        for (int i = 0; i < BITMAP_W * BITMAP_H; i++) {
            bitmap.put(i, (byte) ((i % 16 < 8) ? 255 : 0));
        }
        
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, BITMAP_W, BITMAP_H, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        MemoryUtil.memFree(bitmap);
        
        // Create simple character data
        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(NUM_CHARS);
        for (int i = 0; i < NUM_CHARS; i++) {
            STBTTBakedChar charInfo = charData.get(i);
            charInfo.x0(i * 8).y0(0).x1((i + 1) * 8).y1(8);
            charInfo.xoff(0).yoff(0).xadvance(8);
            charInfo.xoff2(0).yoff2(0);
        }
        
        return new Font("fallback", 8.0f, textureId, charData, null, null, 8.0f, 0.0f, 2.0f);
    }
    
    /**
     * Loads a font from a file.
     */
    public Font loadFontFromFile(String name, String filePath, float size) throws IOException {
        try (InputStream is = new java.io.FileInputStream(filePath)) {
            return loadFontFromStream(name, is, size);
        }
    }
    
    /**
     * Loads a font from an input stream.
     */
    public Font loadFontFromStream(String name, InputStream stream, float size) throws IOException {
        // Read font data
        byte[] fontData = stream.readAllBytes();
        ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontData.length);
        fontBuffer.put(fontData);
        fontBuffer.flip();
        
        // Initialize font info
        STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
        if (!stbtt_InitFont(fontInfo, fontBuffer)) {
            fontInfo.free();
            MemoryUtil.memFree(fontBuffer);
            throw new IOException("Failed to initialize font: " + name);
        }
        
        // Get font metrics
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            
            stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
            
            float scale = stbtt_ScaleForPixelHeight(fontInfo, size);
            float fontAscent = ascent.get(0) * scale;
            float fontDescent = descent.get(0) * scale;
            float fontLineGap = lineGap.get(0) * scale;
            
            // Create bitmap
            ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H);
            STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(NUM_CHARS);
            
            stbtt_BakeFontBitmap(fontBuffer, size, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);
            
            // Create OpenGL texture
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, BITMAP_W, BITMAP_H, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            
            MemoryUtil.memFree(bitmap);
            
            Font font = new Font(name, size, textureId, charData, fontInfo, fontBuffer, 
                               fontAscent, fontDescent, fontLineGap);
            loadedFonts.put(name, font);
            
            logger.info("Loaded font: {} (size: {})", name, size);
            return font;
        }
    }
    
    /**
     * Renders text using the default font.
     */
    public void renderText(String text, float x, float y, Vector4f color) {
        renderText(text, x, y, color, defaultFont);
    }
    
    /**
     * Renders text using the specified font.
     */
    public void renderText(String text, float x, float y, Vector4f color, String fontName) {
        Font font = loadedFonts.get(fontName);
        if (font == null) {
            font = defaultFont;
        }
        renderText(text, x, y, color, font);
    }
    
    /**
     * Renders text using the specified font object.
     */
    public void renderText(String text, float x, float y, Vector4f color, Font font) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
        
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTextureId());
        
        glUniform1i(textureUniform, 0);
        glUniform4f(colorUniform, color.x, color.y, color.z, color.w);
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xPos = stack.floats(x);
            FloatBuffer yPos = stack.floats(y);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);
            
            float[] vertices = new float[6 * 4]; // 6 vertices, 4 components each (x, y, u, v)
            int vertexIndex = 0;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) {
                    continue;
                }
                
                stbtt_GetBakedQuad(font.getCharData(), BITMAP_W, BITMAP_H, c - FIRST_CHAR, xPos, yPos, quad, true);
                
                // First triangle
                vertices[vertexIndex++] = quad.x0(); vertices[vertexIndex++] = quad.y0(); 
                vertices[vertexIndex++] = quad.s0(); vertices[vertexIndex++] = quad.t0();
                
                vertices[vertexIndex++] = quad.x1(); vertices[vertexIndex++] = quad.y0(); 
                vertices[vertexIndex++] = quad.s1(); vertices[vertexIndex++] = quad.t0();
                
                vertices[vertexIndex++] = quad.x1(); vertices[vertexIndex++] = quad.y1(); 
                vertices[vertexIndex++] = quad.s1(); vertices[vertexIndex++] = quad.t1();
                
                // Second triangle
                vertices[vertexIndex++] = quad.x0(); vertices[vertexIndex++] = quad.y0(); 
                vertices[vertexIndex++] = quad.s0(); vertices[vertexIndex++] = quad.t0();
                
                vertices[vertexIndex++] = quad.x1(); vertices[vertexIndex++] = quad.y1(); 
                vertices[vertexIndex++] = quad.s1(); vertices[vertexIndex++] = quad.t1();
                
                vertices[vertexIndex++] = quad.x0(); vertices[vertexIndex++] = quad.y1(); 
                vertices[vertexIndex++] = quad.s0(); vertices[vertexIndex++] = quad.t1();
                
                // Render if buffer is full or at end of string
                if (vertexIndex >= vertices.length - 24 || i == text.length() - 1) {
                    glBindBuffer(GL_ARRAY_BUFFER, vbo);
                    glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                    glDrawArrays(GL_TRIANGLES, 0, vertexIndex / 4);
                    vertexIndex = 0;
                }
            }
        }
        
        glDisable(GL_BLEND);
        glBindVertexArray(0);
        glUseProgram(0);
    }
    
    /**
     * Calculates the width of text in pixels.
     */
    public float getTextWidth(String text, Font font) {
        if (font == null || text == null || text.isEmpty()) {
            return 0.0f;
        }
        
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) {
                continue;
            }
            
            STBTTBakedChar charInfo = font.getCharData().get(c - FIRST_CHAR);
            width += charInfo.xadvance();
        }
        
        return width;
    }
    
    /**
     * Calculates the width of text using the default font.
     */
    public float getTextWidth(String text) {
        return getTextWidth(text, defaultFont);
    }
    
    /**
     * Gets the default font.
     */
    public Font getDefaultFont() {
        return defaultFont;
    }
    
    /**
     * Gets a loaded font by name.
     */
    public Font getFont(String name) {
        return loadedFonts.get(name);
    }
    
    /**
     * Creates the text rendering shader.
     */
    private void createShader() {
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec2 aTexCoord;
            
            uniform mat4 projection;
            
            out vec2 TexCoord;
            
            void main() {
                gl_Position = projection * vec4(aPos, 0.0, 1.0);
                TexCoord = aTexCoord;
            }
            """;
        
        String fragmentShaderSource = """
            #version 330 core
            in vec2 TexCoord;
            
            uniform sampler2D fontTexture;
            uniform vec4 textColor;
            
            out vec4 FragColor;
            
            void main() {
                float alpha = texture(fontTexture, TexCoord).r;
                FragColor = vec4(textColor.rgb, textColor.a * alpha);
            }
            """;
        
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
        
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(shaderProgram);
            throw new RuntimeException("Failed to link shader program: " + log);
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        projectionUniform = glGetUniformLocation(shaderProgram, "projection");
        textureUniform = glGetUniformLocation(shaderProgram, "fontTexture");
        colorUniform = glGetUniformLocation(shaderProgram, "textColor");
    }
    
    /**
     * Compiles a shader.
     */
    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("Failed to compile shader: " + log);
        }
        
        return shader;
    }
    
    /**
     * Creates vertex buffers for text rendering.
     */
    private void createBuffers() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        
        // Allocate buffer for maximum text length
        glBufferData(GL_ARRAY_BUFFER, 1024 * 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    /**
     * Sets the projection matrix for text rendering.
     */
    public void setProjectionMatrix(org.joml.Matrix4f projection) {
        glUseProgram(shaderProgram);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            projection.get(buffer);
            glUniformMatrix4fv(projectionUniform, false, buffer);
        }
        
        glUseProgram(0);
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        logger.info("Cleaning up FontRenderer");
        
        for (Font font : loadedFonts.values()) {
            font.cleanup();
        }
        loadedFonts.clear();
        
        if (defaultFont != null) {
            defaultFont.cleanup();
        }
        
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (shaderProgram != 0) {
            glDeleteProgram(shaderProgram);
        }
    }
}