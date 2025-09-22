package com.odyssey.rendering;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import com.odyssey.util.Logger;

import java.nio.ByteBuffer;

/**
 * Text rendering system for The Odyssey.
 * Handles font loading and text rendering using OpenGL.
 */
public class TextRenderer {
    
    private static final Logger LOGGER = Logger.getLogger(TextRenderer.class);
    
    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final float FONT_SIZE = 24.0f; // Reserved for future font scaling implementation
    
    private int fontTexture;
    // Removed STBTruetype.stbtt_bakedchar.Buffer charData; - using simple bitmap approach
    private Shader textShader;
    private int vao, vbo;
    private boolean initialized = false;
    
    /**
     * Initialize the text renderer.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("TextRenderer already initialized");
            return;
        }
        
        LOGGER.info("Initializing TextRenderer...");
        
        try {
            // Load font and create texture
            loadFont();
            
            // Create shader for text rendering
            createTextShader();
            
            // Set up vertex buffer for text rendering
            setupVertexBuffer();
            
            initialized = true;
            LOGGER.info("TextRenderer initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TextRenderer", e);
            throw new RuntimeException("TextRenderer initialization failed", e);
        }
    }
    
    /**
     * Load font and create bitmap texture.
     */
    private void loadFont() {
        // Create a simple bitmap font texture (fallback approach)
        ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H);
        
        // Fill with a simple pattern for now (white background)
        for (int i = 0; i < BITMAP_W * BITMAP_H; i++) {
            bitmap.put((byte) 255);
        }
        bitmap.flip();
        
        // Create OpenGL texture
        fontTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, BITMAP_W, BITMAP_H, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, bitmap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        MemoryUtil.memFree(bitmap);
        
        LOGGER.debug("Font texture created");
    }
    
    /**
     * Create shader for text rendering.
     */
    private void createTextShader() {
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec2 position;
            layout (location = 1) in vec2 texCoord;
            
            uniform mat4 projection;
            
            out vec2 TexCoord;
            
            void main() {
                gl_Position = projection * vec4(position, 0.0, 1.0);
                TexCoord = texCoord;
            }
            """;
        
        String fragmentShaderSource = """
            #version 330 core
            in vec2 TexCoord;
            
            uniform sampler2D fontTexture;
            uniform vec3 textColor;
            
            out vec4 FragColor;
            
            void main() {
                float alpha = texture(fontTexture, TexCoord).a;
                FragColor = vec4(textColor, alpha);
            }
            """;
        
        textShader = new Shader("text");
        textShader.compileVertexShader(vertexShaderSource);
        textShader.compileFragmentShader(fragmentShaderSource);
        textShader.link();
        
        LOGGER.debug("Text shader created");
    }
    
    /**
     * Set up vertex buffer for text rendering.
     */
    private void setupVertexBuffer() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        
        // Allocate buffer for quad vertices (position + texcoord)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        
        // Position attribute
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
        
        LOGGER.debug("Text vertex buffer set up");
    }
    
    /**
     * Render text at the specified position.
     */
    public void renderText(String text, float x, float y, float scale, float[] color, int windowWidth, int windowHeight) {
        if (!initialized) return;
        
        // Enable blending for text transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Use text shader
        textShader.bind();
        
        // Set projection matrix (orthographic)
        float[] projectionMatrix = {
            2.0f / windowWidth, 0.0f, 0.0f, -1.0f,
            0.0f, -2.0f / windowHeight, 0.0f, 1.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
        
        int projectionLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "projection");
        GL20.glUniformMatrix4fv(projectionLoc, false, projectionMatrix);
        
        // Set text color
        int colorLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "textColor");
        GL20.glUniform3f(colorLoc, color[0], color[1], color[2]);
        
        // Bind font texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        
        // Bind VAO
        GL30.glBindVertexArray(vao);
        
        // Render each character (simplified - just render rectangles for now)
        float charWidth = 16.0f * scale;
        float charHeight = 24.0f * scale;
        
        for (int i = 0; i < text.length(); i++) {
            float charX = x + i * charWidth;
            float charY = y;
            
            // Create quad vertices
            float[] vertices = {
                charX, charY + charHeight, 0.0f, 0.0f,
                charX, charY, 0.0f, 1.0f,
                charX + charWidth, charY, 1.0f, 1.0f,
                
                charX, charY + charHeight, 0.0f, 0.0f,
                charX + charWidth, charY, 1.0f, 1.0f,
                charX + charWidth, charY + charHeight, 1.0f, 0.0f
            };
            
            // Update VBO
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);
            
            // Draw quad
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }
        
        // Cleanup
        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        textShader.unbind();
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (fontTexture != 0) {
            GL11.glDeleteTextures(fontTexture);
        }
        if (textShader != null) {
            textShader.cleanup();
        }
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
        }
        
        initialized = false;
        LOGGER.info("TextRenderer cleaned up");
    }
}