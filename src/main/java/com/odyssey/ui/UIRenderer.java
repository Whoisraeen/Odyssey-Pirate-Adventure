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
    
    // Reusable buffers for performance
    private float[] projectionMatrixBuffer = new float[16];
    private float[] vertexBuffer;
    
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
        if (shaderProgram == 0) {
            throw new RuntimeException("Failed to create UI shader program");
        }
        
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        
        // Check for linking errors
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(shaderProgram);
            logger.error("UI Shader program linking failed: {}", log);
            throw new RuntimeException("Failed to link UI shader program: " + log);
        }
        
        logger.info("UI shaders compiled and linked successfully");
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        // Get uniform locations
        projectionLocation = glGetUniformLocation(shaderProgram, "projection");
        if (projectionLocation == -1) {
            logger.warn("Could not find 'projection' uniform in UI shader");
        } else {
            logger.info("Found 'projection' uniform at location: {}", projectionLocation);
        }
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
        logger.info("Updated UI projection matrix for window size: {}x{}", windowWidth, windowHeight);
        logger.info("Projection matrix: left=0, right={}, bottom={}, top=0", windowWidth, windowHeight);
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
        logger.info("UI endFrame called with {} elements", elements.size());
        
        if (elements.isEmpty()) {
            logger.warn("No UI elements to render! Menu should have added elements.");
            return;
        }

        logger.info("UI endFrame rendering {} elements", elements.size());

        // Save current OpenGL state
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean blendEnabled = glIsEnabled(GL_BLEND);
        int[] blendSrc = new int[1];
        int[] blendDst = new int[1];
        if (blendEnabled) {
            glGetIntegerv(GL_BLEND_SRC_ALPHA, blendSrc);
            glGetIntegerv(GL_BLEND_DST_ALPHA, blendDst);
        }

        // Set up UI rendering state
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Separate rectangles and text elements
        List<RectangleElement> rectangles = new ArrayList<>();
        List<TextElement> textElements = new ArrayList<>();
        
        for (UIElement element : elements) {
            if (element instanceof RectangleElement) {
                rectangles.add((RectangleElement) element);
            } else if (element instanceof TextElement) {
                textElements.add((TextElement) element);
            }
        }

        // Render rectangles first
        if (!rectangles.isEmpty()) {
            logger.info("Rendering {} rectangles", rectangles.size());
            
            // Ensure vertex buffer is large enough
            int requiredSize = rectangles.size() * 36; // 6 vertices per quad, 6 floats per vertex
            if (vertexBuffer == null || vertexBuffer.length < requiredSize) {
                vertexBuffer = new float[requiredSize];
            }
            
            int index = 0;
            for (RectangleElement element : rectangles) {
                float[] quad = element.getVertices();
                System.arraycopy(quad, 0, vertexBuffer, index, quad.length);
                index += quad.length;
            }

            // Upload and render rectangles
            logger.debug("Using shader program: {}", shaderProgram);
            glUseProgram(shaderProgram);
            
            if (projectionLocation != -1) {
                glUniformMatrix4fv(projectionLocation, false, projectionMatrix.get(projectionMatrixBuffer));
                logger.debug("Set projection matrix uniform");
            } else {
                logger.warn("Projection location is -1, cannot set uniform!");
            }

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

            glDrawArrays(GL_TRIANGLES, 0, rectangles.size() * 6);
            
            // Check for OpenGL errors during rectangle rendering
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error during rectangle rendering: {}", error);
            } else {
                logger.debug("Rectangle rendering completed successfully");
            }

            glBindVertexArray(0);
            glUseProgram(0);
        }

        // Render text elements after rectangles
        if (!textElements.isEmpty() && fontRenderer != null) {
            logger.debug("Rendering {} text elements", textElements.size());
            for (TextElement textElement : textElements) {
                logger.debug("Rendering text '{}' at ({}, {}) size={} within window {}x{}", 
                    textElement.text, textElement.x, textElement.y, textElement.size, windowWidth, windowHeight);
                fontRenderer.renderText(textElement.text, textElement.x, textElement.y, textElement.size, textElement.color);
            }
        }

        // Check for OpenGL errors
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            logger.error("OpenGL error during UI rendering: {}", error);
        }

        // Restore OpenGL state
        if (depthTestEnabled) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        
        if (blendEnabled) {
            glEnable(GL_BLEND);
            if (blendSrc[0] != 0 && blendDst[0] != 0) {
                glBlendFunc(blendSrc[0], blendDst[0]);
            }
        } else {
            glDisable(GL_BLEND);
        }
    }
    
    public void drawRectangle(float x, float y, float width, float height, Vector4f color) {
        elements.add(new RectangleElement(x, y, width, height, color));
        logger.debug("Added rectangle: x={}, y={}, w={}, h={}, color=({},{},{},{})", 
            x, y, width, height, color.x, color.y, color.z, color.w);
    }
    
    public void drawText(String text, float x, float y, float size, Vector4f color) {
        if (fontRenderer != null) {
            // Queue text for rendering after rectangles
            logger.debug("Queueing text: '{}' at ({}, {}) size={}", text, x, y, size);
            elements.add(new TextElement(text, x, y, size, color));
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
    
    private static class TextElement extends UIElement {
        public final String text;
        public final float x, y, size;
        public final Vector4f color;
        
        public TextElement(String text, float x, float y, float size, Vector4f color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
        
        @Override
        public float[] getVertices() {
            // Text elements don't use vertices directly
            return new float[0];
        }
    }
}