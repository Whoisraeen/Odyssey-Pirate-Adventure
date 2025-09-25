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
     * Check for OpenGL errors and log them.
     */
    private void checkGLError(String operation) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            LOGGER.error("OpenGL error during {}: {}", operation, error);
        }
    }
    
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
            LOGGER.info("Loading font texture...");
            loadFont();
            checkGLError("font loading");
            
            // Create shader for text rendering
            LOGGER.info("Creating text shader...");
            createTextShader();
            checkGLError("shader creation");
            
            // Set up vertex buffer for text rendering
            LOGGER.info("Setting up vertex buffer...");
            setupVertexBuffer();
            checkGLError("vertex buffer setup");
            
            initialized = true;
            LOGGER.info("TextRenderer initialized successfully");
            LOGGER.info("Font texture ID: {}, Shader program ID: {}, VAO: {}, VBO: {}", 
                       fontTexture, textShader != null ? textShader.getProgramId() : -1, vao, vbo);
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TextRenderer", e);
            throw new RuntimeException("TextRenderer initialization failed", e);
        }
    }
    
    /**
     * Load font and create bitmap texture.
     */
    private void loadFont() {
        LOGGER.info("Creating font bitmap texture {}x{}", BITMAP_W, BITMAP_H);
        
        // Create a simple bitmap font texture (fallback approach)
        // Using RGBA format for better compatibility
        ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H * 4);
        
        // Fill with a simple pattern - white rectangles on transparent background
        for (int y = 0; y < BITMAP_H; y++) {
            for (int x = 0; x < BITMAP_W; x++) {
                // Create a simple character pattern - white squares for visibility
                boolean isCharArea = (x % 32 < 24) && (y % 32 < 24) && (x % 32 > 4) && (y % 32 > 4);
                
                if (isCharArea) {
                    // White with full alpha
                    bitmap.put((byte) 255); // R
                    bitmap.put((byte) 255); // G
                    bitmap.put((byte) 255); // B
                    bitmap.put((byte) 255); // A
                } else {
                    // Transparent
                    bitmap.put((byte) 0);   // R
                    bitmap.put((byte) 0);   // G
                    bitmap.put((byte) 0);   // B
                    bitmap.put((byte) 0);   // A
                }
            }
        }
        bitmap.flip();
        
        // Create OpenGL texture
        fontTexture = GL11.glGenTextures();
        LOGGER.info("Generated font texture ID: {}", fontTexture);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        checkGLError("bind texture");
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, BITMAP_W, BITMAP_H, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bitmap);
        checkGLError("texture upload");
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        checkGLError("texture parameters");
        
        MemoryUtil.memFree(bitmap);
        
        LOGGER.info("Font texture created successfully with RGBA format, ID: {}", fontTexture);
    }
    
    /**
     * Create shader for text rendering.
     */
    private void createTextShader() {
        LOGGER.info("Creating text shader...");
        
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
                vec4 sampled = texture(fontTexture, TexCoord);
                FragColor = vec4(textColor, 1.0) * sampled;
            }
            """;
        
        textShader = new Shader("text");
        textShader.compileVertexShader(vertexShaderSource);
        textShader.compileFragmentShader(fragmentShaderSource);
        textShader.link();
        
        LOGGER.info("Text shader created successfully, program ID: {}", textShader.getProgramId());
        
        // Verify shader uniforms
        textShader.bind();
        int projectionLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "projection");
        int colorLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "textColor");
        int textureLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "fontTexture");
        
        LOGGER.info("Shader uniform locations - projection: {}, textColor: {}, fontTexture: {}", 
                   projectionLoc, colorLoc, textureLoc);
        
        textShader.unbind();
    }
    
    /**
     * Set up vertex buffer for text rendering.
     */
    private void setupVertexBuffer() {
        LOGGER.info("Setting up vertex buffer...");
        
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        
        LOGGER.info("Generated VAO: {}, VBO: {}", vao, vbo);
        
        GL30.glBindVertexArray(vao);
        checkGLError("bind VAO");
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        checkGLError("bind VBO");
        
        // Allocate buffer for quad vertices (position + texcoord)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        checkGLError("buffer data");
        
        // Position attribute
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        checkGLError("position attribute");
        
        // Texture coordinate attribute
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        checkGLError("texture coordinate attribute");
        
        GL30.glBindVertexArray(0);
        
        LOGGER.info("Text vertex buffer set up successfully");
    }
    
    /**
     * Render text at the specified position.
     */
    public void renderText(String text, float x, float y, float scale, float[] color, int windowWidth, int windowHeight) {
        if (!initialized) {
            LOGGER.warn("TextRenderer not initialized, cannot render text");
            return;
        }
        
        LOGGER.info("=== RENDERING TEXT ===");
        LOGGER.info("Text: '{}', Position: ({}, {}), Scale: {}, Color: [{}, {}, {}]", 
                   text, x, y, scale, color[0], color[1], color[2]);
        LOGGER.info("Window dimensions: {}x{}", windowWidth, windowHeight);
        LOGGER.info("Font texture ID: {}, Shader program ID: {}, VAO: {}, VBO: {}", 
                   fontTexture, textShader.getProgramId(), vao, vbo);
        
        // Enable blending for text transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        checkGLError("enable blending");
        
        // Use text shader
        textShader.bind();
        checkGLError("bind shader");
        LOGGER.info("Shader bound successfully");
        
        // Set projection matrix (orthographic)
        float[] projectionMatrix = {
            2.0f / windowWidth, 0.0f, 0.0f, -1.0f,
            0.0f, -2.0f / windowHeight, 0.0f, 1.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
        
        int projectionLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "projection");
        LOGGER.info("Projection uniform location: {}", projectionLoc);
        GL20.glUniformMatrix4fv(projectionLoc, false, projectionMatrix);
        checkGLError("set projection matrix");
        
        // Set text color
        int colorLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "textColor");
        LOGGER.info("Color uniform location: {}", colorLoc);
        GL20.glUniform3f(colorLoc, color[0], color[1], color[2]);
        checkGLError("set text color");
        
        // Set font texture uniform
        int textureLoc = GL20.glGetUniformLocation(textShader.getProgramId(), "fontTexture");
        LOGGER.info("Texture uniform location: {}", textureLoc);
        GL20.glUniform1i(textureLoc, 0);
        checkGLError("set texture uniform");
        
        // Bind font texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        checkGLError("bind font texture");
        LOGGER.info("Font texture bound to unit 0");
        
        // Bind VAO
        GL30.glBindVertexArray(vao);
        checkGLError("bind VAO");
        LOGGER.info("VAO bound");
        
        // Render each character (simplified - just render rectangles for now)
        float charWidth = 16.0f * scale;
        float charHeight = 24.0f * scale;
        
        LOGGER.info("Character dimensions: {}x{}", charWidth, charHeight);
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float charX = x + i * charWidth;
            float charY = y;
            
            LOGGER.info("Rendering character '{}' at ({}, {})", c, charX, charY);
            
            // Create quad vertices (position + texture coordinates)
            float[] vertices = {
                // Triangle 1
                charX, charY + charHeight, 0.0f, 0.0f,  // Top-left
                charX, charY, 0.0f, 1.0f,               // Bottom-left
                charX + charWidth, charY, 1.0f, 1.0f,   // Bottom-right
                
                // Triangle 2
                charX, charY + charHeight, 0.0f, 0.0f,  // Top-left
                charX + charWidth, charY, 1.0f, 1.0f,   // Bottom-right
                charX + charWidth, charY + charHeight, 1.0f, 0.0f  // Top-right
            };
            
            // Update VBO
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);
            checkGLError("update VBO");
            
            // Draw quad
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            checkGLError("draw arrays");
            LOGGER.info("Drew character '{}' with 6 vertices", c);
        }
        
        // Cleanup
        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        textShader.unbind();
        GL11.glDisable(GL11.GL_BLEND);
        checkGLError("cleanup");
        
        LOGGER.info("Text rendering completed for: '{}' - {} characters rendered", text, text.length());
        LOGGER.info("=== END TEXT RENDERING ===");
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