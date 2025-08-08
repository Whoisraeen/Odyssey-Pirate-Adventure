package com.odyssey.ui;

import com.odyssey.graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Simple UI rendering system for menus and overlays.
 * Provides basic text and rectangle rendering capabilities.
 */
public class UIRenderer {
    private static final Logger logger = LoggerFactory.getLogger(UIRenderer.class);
    
    private int shaderProgram;
    private int vao, vbo;
    private int projectionLocation;
    private Matrix4f projectionMatrix;
    private int windowWidth, windowHeight;
    
    // Font rendering
    private FontRenderer fontRenderer;
    
    // UI elements to render
    private final List<UIElement> elements = new ArrayList<>();
    
    public void initialize(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        
        logger.info("Initializing UI renderer with window size: {}x{}", windowWidth, windowHeight);
        
        createShaders();
        createBuffers();
        updateProjection();
        
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.initialize();
        fontRenderer.setProjectionMatrix(projectionMatrix);
        
        logger.info("UI renderer initialized successfully");
    }
    
    private void createShaders() {
        // Simple vertex shader for UI
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec4 aColor;
            
            uniform mat4 projection;
            
            out vec4 vertexColor;
            
            void main() {
                gl_Position = projection * vec4(aPos, 0.0, 1.0);
                vertexColor = aColor;
            }
            """;
        
        // Simple fragment shader for UI
        String fragmentShaderSource = """
            #version 330 core
            in vec4 vertexColor;
            out vec4 FragColor;
            
            void main() {
                FragColor = vertexColor;
            }
            """;
        
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
        
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        
        // Check for linking errors
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(shaderProgram);
            logger.error("Shader program linking failed: {}", log);
            throw new RuntimeException("Failed to link shader program");
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        // Get uniform locations
        projectionLocation = glGetUniformLocation(shaderProgram, "projection");
    }
    
    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            logger.error("Shader compilation failed: {}", log);
            throw new RuntimeException("Failed to compile shader");
        }
        
        return shader;
    }
    
    private void createBuffers() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        
        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Color attribute
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 6 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    private void updateProjection() {
        projectionMatrix = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
    }
    
    public void resize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        updateProjection();
        
        // Update font renderer projection
        if (fontRenderer != null) {
            fontRenderer.setProjectionMatrix(projectionMatrix);
        }
    }
    
    public void beginFrame() {
        elements.clear();
    }
    
    public void endFrame() {
        if (elements.isEmpty()) {
            return;
        }
        
        logger.debug("UI endFrame rendering {} elements", elements.size());
        
        // Set up UI rendering state
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Prepare vertex data
        float[] vertices = new float[elements.size() * 36]; // 6 vertices per quad, 6 floats per vertex
        int index = 0;
        
        for (int i = 0; i < elements.size(); i++) {
            UIElement element = elements.get(i);
            float[] quad = element.getVertices();
            System.arraycopy(quad, 0, vertices, index, quad.length);
            
            // Store vertices for rendering
            
            index += quad.length;
        }
        
        // Upload and render
        glUseProgram(shaderProgram);
        
        glUniformMatrix4fv(projectionLocation, false, projectionMatrix.get(new float[16]));
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        
        glDrawArrays(GL_TRIANGLES, 0, elements.size() * 6);
        
        // Check for OpenGL errors
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            logger.error("OpenGL error during UI rendering: {}", error);
        }
        
        glBindVertexArray(0);
        glUseProgram(0);
        
        // Restore OpenGL state
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void drawRectangle(float x, float y, float width, float height, Vector4f color) {
        elements.add(new RectangleElement(x, y, width, height, color));
        logger.debug("Added rectangle: x={}, y={}, w={}, h={}, color=({},{},{},{})", 
            x, y, width, height, color.x, color.y, color.z, color.w);
    }
    
    public void drawText(String text, float x, float y, float size, Vector4f color) {
        if (fontRenderer != null) {
            // Use proper font rendering
            logger.debug("Drawing text: '{}' at ({}, {}) size={}", text, x, y, size);
            
            // Create a font at the requested size if needed
            FontRenderer.Font font = fontRenderer.getDefaultFont();
            if (font != null && Math.abs(font.getSize() - size) > 1.0f) {
                // For now, scale the default font - in a full implementation,
                // we'd cache fonts at different sizes
                float scale = size / font.getSize();
                // Apply scaling during rendering (simplified approach)
            }
            
            fontRenderer.renderText(text, x, y, color);
        } else {
            // Fallback to placeholder rectangles
            logger.debug("Drawing text placeholder: '{}' at ({}, {}) size={}", text, x, y, size);
            
            float textWidth = text.length() * size * 0.6f;
            float textHeight = size;
            
            // Draw a background rectangle for better visibility
            elements.add(new RectangleElement(x - 4, y - 4, textWidth + 8, textHeight + 8, 
                new Vector4f(0.0f, 0.0f, 0.0f, 0.7f)));
            
            // Draw the text rectangle with the specified color
            elements.add(new RectangleElement(x, y, textWidth, textHeight, color));
        }
    }
    
    /**
     * Calculates the width of text in pixels for layout purposes.
     */
    public float getTextWidth(String text, float size) {
        if (fontRenderer != null) {
            FontRenderer.Font font = fontRenderer.getDefaultFont();
            if (font != null) {
                float scale = size / font.getSize();
                return fontRenderer.getTextWidth(text, font) * scale;
            }
        }
        
        // Fallback calculation
        return text.length() * size * 0.6f;
    }
    
    /**
     * Gets the line height for the given font size.
     */
    public float getLineHeight(float size) {
        if (fontRenderer != null) {
            FontRenderer.Font font = fontRenderer.getDefaultFont();
            if (font != null) {
                float scale = size / font.getSize();
                return font.getLineHeight() * scale;
            }
        }
        
        // Fallback calculation
        return size * 1.2f;
    }
    
    public void cleanup() {
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
        
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (shaderProgram != 0) glDeleteProgram(shaderProgram);
        
        logger.info("UI renderer cleaned up");
    }
    
    // UI Element classes
    private static abstract class UIElement {
        public abstract float[] getVertices();
    }
    
    private static class RectangleElement extends UIElement {
        private final float x, y, width, height;
        private final Vector4f color;
        
        public RectangleElement(float x, float y, float width, float height, Vector4f color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
        
        @Override
        public float[] getVertices() {
            return new float[] {
                // Triangle 1
                x, y, color.x, color.y, color.z, color.w,
                x + width, y, color.x, color.y, color.z, color.w,
                x, y + height, color.x, color.y, color.z, color.w,
                
                // Triangle 2
                x + width, y, color.x, color.y, color.z, color.w,
                x + width, y + height, color.x, color.y, color.z, color.w,
                x, y + height, color.x, color.y, color.z, color.w
            };
        }
    }
}