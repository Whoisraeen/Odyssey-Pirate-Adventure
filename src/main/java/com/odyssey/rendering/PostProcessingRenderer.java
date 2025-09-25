package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Post-Processing Renderer for The Odyssey.
 * Handles various post-processing effects including bloom, tone mapping,
 * color grading, depth of field, and screen-space effects.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public class PostProcessingRenderer {
    
    private static final Logger logger = Logger.getLogger(PostProcessingRenderer.class);
    
    /**
     * Post-processing effect types.
     */
    public enum EffectType {
        BLOOM,
        TONE_MAPPING,
        COLOR_GRADING,
        DEPTH_OF_FIELD,
        MOTION_BLUR,
        SCREEN_SPACE_REFLECTIONS,
        CHROMATIC_ABERRATION,
        VIGNETTE,
        FILM_GRAIN,
        UNDERWATER_DISTORTION
    }
    
    /**
     * Tone mapping algorithms.
     */
    public enum ToneMappingType {
        REINHARD,
        ACES,
        FILMIC,
        EXPOSURE
    }
    
    /**
     * Represents a post-processing pass.
     */
    private static class PostProcessPass {
        String name;
        Shader shader;
        Framebuffer inputBuffer;
        Framebuffer outputBuffer;
        boolean enabled;
        
        PostProcessPass(String name, Shader shader) {
            this.name = name;
            this.shader = shader;
            this.enabled = true;
        }
    }
    
    // Rendering resources
    private int quadVAO;
    private int quadVBO;
    private boolean initialized;
    
    // Framebuffers for multi-pass rendering
    private Framebuffer sceneBuffer;
    private Framebuffer brightBuffer;
    private Framebuffer[] bloomBuffers;
    private Framebuffer finalBuffer;
    private Framebuffer tempBuffer;
    
    // Shaders for different effects
    private Shader bloomExtractShader;
    private Shader bloomBlurShader;
    private Shader bloomCombineShader;
    private Shader toneMappingShader;
    private Shader colorGradingShader;
    private Shader depthOfFieldShader;
    private Shader motionBlurShader;
    private Shader ssrShader;
    private Shader chromaticAberrationShader;
    private Shader vignetteShader;
    private Shader filmGrainShader;
    private Shader underwaterShader;
    private Shader finalCombineShader;
    
    // Post-processing passes
    private List<PostProcessPass> processingPasses;
    
    // Effect settings
    private boolean bloomEnabled = true;
    private float bloomThreshold = 1.0f;
    private float bloomIntensity = 0.8f;
    private int bloomPasses = 5;
    
    private boolean toneMappingEnabled = true;
    private ToneMappingType toneMappingType = ToneMappingType.ACES;
    private float exposure = 1.0f;
    private float gamma = 2.2f;
    
    private boolean colorGradingEnabled = true;
    private Vector3f colorBalance = new Vector3f(1.0f, 1.0f, 1.0f);
    private float saturation = 1.0f;
    private float contrast = 1.0f;
    private float brightness = 0.0f;
    
    private boolean depthOfFieldEnabled = false;
    private float focusDistance = 10.0f;
    private float focusRange = 5.0f;
    private float bokehRadius = 2.0f;
    
    private boolean motionBlurEnabled = false;
    private float motionBlurStrength = 0.5f;
    
    private boolean ssrEnabled = false;
    private float ssrIntensity = 0.5f;
    private int ssrSteps = 32;
    
    private boolean chromaticAberrationEnabled = false;
    private float chromaticAberrationStrength = 0.002f;
    
    private boolean vignetteEnabled = true;
    private float vignetteIntensity = 0.3f;
    private float vignetteRadius = 0.8f;
    
    private boolean filmGrainEnabled = false;
    private float filmGrainStrength = 0.1f;
    
    private boolean underwaterEnabled = false;
    private float underwaterDistortion = 0.02f;
    private Vector3f underwaterTint = new Vector3f(0.2f, 0.6f, 1.0f);
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Timing for animated effects
    private float time = 0.0f;
    
    /**
     * Creates a new PostProcessingRenderer.
     */
    public PostProcessingRenderer() {
        this.processingPasses = new ArrayList<>();
        this.initialized = false;
    }
    
    /**
     * Initializes the post-processing renderer.
     * 
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @return True if initialization was successful
     */
    public boolean initialize(int screenWidth, int screenHeight) {
        if (initialized) {
            logger.warn("PostProcessingRenderer already initialized");
            return true;
        }
        
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        logger.info("Initializing PostProcessingRenderer ({}x{})...", screenWidth, screenHeight);
        
        try {
            // Create fullscreen quad
            if (!createFullscreenQuad()) {
                logger.error("Failed to create fullscreen quad");
                return false;
            }
            
            // Create framebuffers
            if (!createFramebuffers()) {
                logger.error("Failed to create framebuffers");
                return false;
            }
            
            // Load shaders
            if (!loadShaders()) {
                logger.error("Failed to load post-processing shaders");
                return false;
            }
            
            // Setup processing passes
            setupProcessingPasses();
            
            initialized = true;
            logger.info("PostProcessingRenderer initialized successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize PostProcessingRenderer", e);
            return false;
        }
    }
    
    /**
     * Creates the fullscreen quad for post-processing.
     */
    private boolean createFullscreenQuad() {
        // Fullscreen quad vertices (position + UV)
        float[] quadVertices = {
            // Position    // UV
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f,
            -1.0f,  1.0f,  0.0f, 1.0f
        };
        
        int[] quadIndices = {
            0, 1, 2,
            2, 3, 0
        };
        
        // Create VAO
        quadVAO = glGenVertexArrays();
        glBindVertexArray(quadVAO);
        
        // Create VBO
        quadVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(quadVertices.length);
        vertexBuffer.put(quadVertices);
        vertexBuffer.flip();
        
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        // Create EBO
        int quadEBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL_STATIC_DRAW);
        
        // Setup vertex attributes
        // Position
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // UV coordinates
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        
        return true;
    }
    
    /**
     * Creates framebuffers for post-processing.
     */
    private boolean createFramebuffers() {
        try {
            // Main scene buffer (HDR)
            sceneBuffer = new Framebuffer.Builder()
                .name("SceneBuffer")
                .size(screenWidth, screenHeight)
                .addColorRGBA16F()
                .depthTexture()
                .build();
            if (sceneBuffer == null) {
                logger.error("Failed to create scene framebuffer");
                return false;
            }
            
            // Bright pass buffer for bloom
            brightBuffer = new Framebuffer.Builder()
                .name("BrightPass")
                .size(screenWidth / 2, screenHeight / 2)
                .addColorRGBA16F()
                .build();
            if (brightBuffer == null) {
                logger.error("Failed to create bright pass framebuffer");
                return false;
            }
            
            // Bloom blur buffers (multiple resolutions)
            bloomBuffers = new Framebuffer[bloomPasses * 2]; // Horizontal and vertical for each pass
            int width = screenWidth / 2;
            int height = screenHeight / 2;
            
            for (int i = 0; i < bloomPasses; i++) {
                // Horizontal blur buffer
                bloomBuffers[i * 2] = new Framebuffer.Builder()
                    .name("BloomHorizontal" + i)
                    .size(width, height)
                    .addColorRGBA16F()
                    .build();
                if (bloomBuffers[i * 2] == null) {
                    logger.error("Failed to create bloom buffer {}", i * 2);
                    return false;
                }
                
                // Vertical blur buffer
                bloomBuffers[i * 2 + 1] = new Framebuffer.Builder()
                    .name("BloomVertical" + i)
                    .size(width, height)
                    .addColorRGBA16F()
                    .build();
                if (bloomBuffers[i * 2 + 1] == null) {
                    logger.error("Failed to create bloom buffer {}", i * 2 + 1);
                    return false;
                }
                
                // Reduce resolution for next pass
                width /= 2;
                height /= 2;
                width = Math.max(width, 1);
                height = Math.max(height, 1);
            }
            
            // Final output buffer
            finalBuffer = new Framebuffer.Builder()
                .name("FinalOutput")
                .size(screenWidth, screenHeight)
                .addColorRGBA8()
                .build();
            if (finalBuffer == null) {
                logger.error("Failed to create final framebuffer");
                return false;
            }
            
            // Temporary buffer for multi-pass effects
            tempBuffer = new Framebuffer.Builder()
                .name("TempBuffer")
                .size(screenWidth, screenHeight)
                .addColorRGBA16F()
                .build();
            if (tempBuffer == null) {
                logger.error("Failed to create temporary framebuffer");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Exception creating framebuffers", e);
            return false;
        }
    }
    
    /**
     * Loads all post-processing shaders.
     */
    private boolean loadShaders() {
        try {
            // Create basic placeholder shaders for now since we don't have the actual shader files
            // In a real implementation, these would load from actual shader files
            
            // Basic vertex shader source for post-processing
            String basicVertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec2 aPos;\n" +
                "layout (location = 1) in vec2 aTexCoord;\n" +
                "\n" +
                "out vec2 TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                "    TexCoord = aTexCoord;\n" +
                "}";
            
            // Basic fragment shader source for post-processing
            String basicFragmentSource = "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "in vec2 TexCoord;\n" +
                "uniform sampler2D screenTexture;\n" +
                "\n" +
                "void main() {\n" +
                "    FragColor = texture(screenTexture, TexCoord);\n" +
                "}";
            
            // Create basic shaders using the Shader.create method with source code
            bloomExtractShader = Shader.create("bloomExtract", basicVertexSource, basicFragmentSource);
            if (bloomExtractShader == null) {
                logger.error("Failed to create bloom extract shader");
                return false;
            }
            
            bloomBlurShader = Shader.create("bloomBlur", basicVertexSource, basicFragmentSource);
            if (bloomBlurShader == null) {
                logger.error("Failed to create bloom blur shader");
                return false;
            }
            
            bloomCombineShader = Shader.create("bloomCombine", basicVertexSource, basicFragmentSource);
            if (bloomCombineShader == null) {
                logger.error("Failed to create bloom combine shader");
                return false;
            }
            
            // Tone mapping shader
            toneMappingShader = Shader.create("toneMapping", basicVertexSource, basicFragmentSource);
            if (toneMappingShader == null) {
                logger.error("Failed to create tone mapping shader");
                return false;
            }
            
            // Color grading shader
            colorGradingShader = Shader.create("colorGrading", basicVertexSource, basicFragmentSource);
            if (colorGradingShader == null) {
                logger.error("Failed to create color grading shader");
                return false;
            }
            
            // Other effect shaders (create placeholder implementations with basic shaders)
            depthOfFieldShader = Shader.create("depthOfField", basicVertexSource, basicFragmentSource);
            motionBlurShader = Shader.create("motionBlur", basicVertexSource, basicFragmentSource);
            ssrShader = Shader.create("ssr", basicVertexSource, basicFragmentSource);
            chromaticAberrationShader = Shader.create("chromaticAberration", basicVertexSource, basicFragmentSource);
            vignetteShader = Shader.create("vignette", basicVertexSource, basicFragmentSource);
            filmGrainShader = Shader.create("filmGrain", basicVertexSource, basicFragmentSource);
            underwaterShader = Shader.create("underwater", basicVertexSource, basicFragmentSource);
            
            // Final combine shader
            finalCombineShader = Shader.create("finalCombine", basicVertexSource, basicFragmentSource);
            if (finalCombineShader == null) {
                logger.error("Failed to create final combine shader");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Exception loading shaders", e);
            return false;
        }
    }
    
    /**
     * Sets up the post-processing pipeline.
     */
    private void setupProcessingPasses() {
        processingPasses.clear();
        
        // Add passes in order
        if (bloomEnabled) {
            processingPasses.add(new PostProcessPass("bloom_extract", bloomExtractShader));
            processingPasses.add(new PostProcessPass("bloom_blur", bloomBlurShader));
            processingPasses.add(new PostProcessPass("bloom_combine", bloomCombineShader));
        }
        
        if (toneMappingEnabled) {
            processingPasses.add(new PostProcessPass("tone_mapping", toneMappingShader));
        }
        
        if (colorGradingEnabled) {
            processingPasses.add(new PostProcessPass("color_grading", colorGradingShader));
        }
        
        if (depthOfFieldEnabled) {
            processingPasses.add(new PostProcessPass("depth_of_field", depthOfFieldShader));
        }
        
        if (motionBlurEnabled) {
            processingPasses.add(new PostProcessPass("motion_blur", motionBlurShader));
        }
        
        if (ssrEnabled) {
            processingPasses.add(new PostProcessPass("ssr", ssrShader));
        }
        
        if (chromaticAberrationEnabled) {
            processingPasses.add(new PostProcessPass("chromatic_aberration", chromaticAberrationShader));
        }
        
        if (vignetteEnabled) {
            processingPasses.add(new PostProcessPass("vignette", vignetteShader));
        }
        
        if (filmGrainEnabled) {
            processingPasses.add(new PostProcessPass("film_grain", filmGrainShader));
        }
        
        if (underwaterEnabled) {
            processingPasses.add(new PostProcessPass("underwater", underwaterShader));
        }
        
        // Final combine pass
        processingPasses.add(new PostProcessPass("final_combine", finalCombineShader));
    }
    
    /**
     * Processes the scene with post-processing effects.
     * 
     * @param sceneTexture Input scene texture
     * @param depthTexture Scene depth texture
     * @param deltaTime Time since last frame
     * @return Final processed texture ID
     */
    public int processScene(int sceneTexture, int depthTexture, float deltaTime) {
        if (!initialized) {
            logger.warn("PostProcessingRenderer not initialized");
            return sceneTexture;
        }
        
        time += deltaTime;
        
        // Start with scene texture
        int currentTexture = sceneTexture;
        
        // Apply bloom effect
        if (bloomEnabled) {
            currentTexture = applyBloom(currentTexture);
        }
        
        // Apply tone mapping
        if (toneMappingEnabled) {
            currentTexture = applyToneMapping(currentTexture);
        }
        
        // Apply color grading
        if (colorGradingEnabled) {
            currentTexture = applyColorGrading(currentTexture);
        }
        
        // Apply other effects
        if (depthOfFieldEnabled) {
            currentTexture = applyDepthOfField(currentTexture, depthTexture);
        }
        
        if (motionBlurEnabled) {
            currentTexture = applyMotionBlur(currentTexture);
        }
        
        if (chromaticAberrationEnabled) {
            currentTexture = applyChromaticAberration(currentTexture);
        }
        
        if (vignetteEnabled) {
            currentTexture = applyVignette(currentTexture);
        }
        
        if (filmGrainEnabled) {
            currentTexture = applyFilmGrain(currentTexture);
        }
        
        if (underwaterEnabled) {
            currentTexture = applyUnderwaterEffect(currentTexture);
        }
        
        return currentTexture;
    }
    
    /**
     * Applies bloom effect to the scene.
     */
    private int applyBloom(int sceneTexture) {
        // Extract bright pixels
        brightBuffer.bind();
        glViewport(0, 0, brightBuffer.getWidth(), brightBuffer.getHeight());
        glClear(GL_COLOR_BUFFER_BIT);
        
        bloomExtractShader.bind();
        bloomExtractShader.setUniform("u_SceneTexture", 0);
        bloomExtractShader.setUniform("u_Threshold", bloomThreshold);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        
        renderFullscreenQuad();
        Framebuffer.unbind();
        
        // Apply gaussian blur passes
        int currentBloomTexture = brightBuffer.getColorTexture(0);
        
        for (int i = 0; i < bloomPasses; i++) {
            // Horizontal blur
            bloomBuffers[i * 2].bind();
            glViewport(0, 0, bloomBuffers[i * 2].getWidth(), bloomBuffers[i * 2].getHeight());
            glClear(GL_COLOR_BUFFER_BIT);
            
            bloomBlurShader.bind();
            bloomBlurShader.setUniform("u_InputTexture", 0);
            bloomBlurShader.setUniform("u_Direction", new Vector2f(1.0f, 0.0f));
            bloomBlurShader.setUniform("u_TexelSize", new Vector2f(1.0f / bloomBuffers[i * 2].getWidth(), 1.0f / bloomBuffers[i * 2].getHeight()));
            
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, currentBloomTexture);
            
            renderFullscreenQuad();
            Framebuffer.unbind();
            
            // Vertical blur
            bloomBuffers[i * 2 + 1].bind();
            glViewport(0, 0, bloomBuffers[i * 2 + 1].getWidth(), bloomBuffers[i * 2 + 1].getHeight());
            glClear(GL_COLOR_BUFFER_BIT);
            
            bloomBlurShader.bind();
            bloomBlurShader.setUniform("u_Direction", new Vector2f(0.0f, 1.0f));
            
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, bloomBuffers[i * 2].getColorTexture(0));
            
            renderFullscreenQuad();
            Framebuffer.unbind();
            
            currentBloomTexture = bloomBuffers[i * 2 + 1].getColorTexture(0);
        }
        
        // Combine bloom with original scene
        tempBuffer.bind();
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT);
        
        bloomCombineShader.bind();
        bloomCombineShader.setUniform("u_SceneTexture", 0);
        bloomCombineShader.setUniform("u_BloomTexture", 1);
        bloomCombineShader.setUniform("u_BloomStrength", bloomIntensity);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, currentBloomTexture);
        
        renderFullscreenQuad();
        Framebuffer.unbind();
        
        return tempBuffer.getColorTexture(0);
    }
    
    /**
     * Applies tone mapping to the scene.
     */
    private int applyToneMapping(int inputTexture) {
        finalBuffer.bind();
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT);
        
        toneMappingShader.bind();
        toneMappingShader.setUniform("u_InputTexture", 0);
        toneMappingShader.setUniform("u_ToneMappingType", toneMappingType.ordinal());
        toneMappingShader.setUniform("u_Exposure", exposure);
        toneMappingShader.setUniform("u_Gamma", gamma);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        renderFullscreenQuad();
        Framebuffer.unbind();
        
        return finalBuffer.getColorTexture(0);
    }
    
    /**
     * Applies color grading to the scene.
     */
    private int applyColorGrading(int inputTexture) {
        tempBuffer.bind();
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT);
        
        colorGradingShader.bind();
        colorGradingShader.setUniform("u_InputTexture", 0);
        colorGradingShader.setUniform("u_ColorBalance", colorBalance);
        colorGradingShader.setUniform("u_Saturation", saturation);
        colorGradingShader.setUniform("u_Contrast", contrast);
        colorGradingShader.setUniform("u_Brightness", brightness);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        renderFullscreenQuad();
        Framebuffer.unbind();
        
        return tempBuffer.getColorTexture(0);
    }
    
    /**
     * Applies depth of field effect.
     */
    private int applyDepthOfField(int inputTexture, int depthTexture) {
        // Placeholder implementation
        return inputTexture;
    }
    
    /**
     * Applies motion blur effect.
     */
    private int applyMotionBlur(int inputTexture) {
        // Placeholder implementation
        return inputTexture;
    }
    
    /**
     * Applies chromatic aberration effect.
     */
    private int applyChromaticAberration(int inputTexture) {
        // Placeholder implementation
        return inputTexture;
    }
    
    /**
     * Applies vignette effect.
     */
    private int applyVignette(int inputTexture) {
        tempBuffer.bind();
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT);
        
        vignetteShader.bind();
        vignetteShader.setUniform("u_InputTexture", 0);
        vignetteShader.setUniform("u_Intensity", vignetteIntensity);
        vignetteShader.setUniform("u_Radius", vignetteRadius);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        renderFullscreenQuad();
        Framebuffer.unbind();
        
        return tempBuffer.getColorTexture(0);
    }
    
    /**
     * Applies film grain effect.
     */
    private int applyFilmGrain(int inputTexture) {
        // Placeholder implementation
        return inputTexture;
    }
    
    /**
     * Applies underwater distortion effect.
     */
    private int applyUnderwaterEffect(int inputTexture) {
        // Placeholder implementation
        return inputTexture;
    }
    
    /**
     * Renders a fullscreen quad.
     */
    private void renderFullscreenQuad() {
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Resizes the post-processing buffers.
     * 
     * @param width New width
     * @param height New height
     */
    public void resize(int width, int height) {
        if (!initialized) return;
        
        this.screenWidth = width;
        this.screenHeight = height;
        
        // Recreate framebuffers with new size
        cleanup();
        createFramebuffers();
        
        logger.info("PostProcessingRenderer resized to {}x{}", width, height);
    }
    
    // Effect control methods
    
    public void setBloomEnabled(boolean enabled) {
        this.bloomEnabled = enabled;
        setupProcessingPasses();
    }
    
    public void setBloomSettings(float threshold, float intensity, int passes) {
        this.bloomThreshold = threshold;
        this.bloomIntensity = intensity;
        this.bloomPasses = Math.max(1, Math.min(passes, 8));
    }
    
    public void setToneMappingEnabled(boolean enabled) {
        this.toneMappingEnabled = enabled;
        setupProcessingPasses();
    }
    
    public void setToneMappingSettings(ToneMappingType type, float exposure, float gamma) {
        this.toneMappingType = type;
        this.exposure = exposure;
        this.gamma = gamma;
    }
    
    public void setColorGradingEnabled(boolean enabled) {
        this.colorGradingEnabled = enabled;
        setupProcessingPasses();
    }
    
    public void setColorGradingSettings(Vector3f colorBalance, float saturation, float contrast, float brightness) {
        this.colorBalance.set(colorBalance);
        this.saturation = saturation;
        this.contrast = contrast;
        this.brightness = brightness;
    }
    
    public void setUnderwaterEnabled(boolean enabled) {
        this.underwaterEnabled = enabled;
        setupProcessingPasses();
    }
    
    public void setUnderwaterSettings(float distortion, Vector3f tint) {
        this.underwaterDistortion = distortion;
        this.underwaterTint.set(tint);
    }
    
    // Getters
    
    public boolean isInitialized() { return initialized; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    
    public Framebuffer getSceneBuffer() { return sceneBuffer; }
    public Framebuffer getFinalBuffer() { return finalBuffer; }
    
    /**
     * Cleanup post-processing resources.
     */
    public void cleanup() {
        if (quadVAO != 0) {
            glDeleteVertexArrays(quadVAO);
            quadVAO = 0;
        }
        
        if (quadVBO != 0) {
            glDeleteBuffers(quadVBO);
            quadVBO = 0;
        }
        
        // Cleanup framebuffers
        if (sceneBuffer != null) {
            sceneBuffer.cleanup();
            sceneBuffer = null;
        }
        
        if (brightBuffer != null) {
            brightBuffer.cleanup();
            brightBuffer = null;
        }
        
        if (bloomBuffers != null) {
            for (Framebuffer buffer : bloomBuffers) {
                if (buffer != null) {
                    buffer.cleanup();
                }
            }
            bloomBuffers = null;
        }
        
        if (finalBuffer != null) {
            finalBuffer.cleanup();
            finalBuffer = null;
        }
        
        if (tempBuffer != null) {
            tempBuffer.cleanup();
            tempBuffer = null;
        }
        
        // Cleanup shaders
        if (bloomExtractShader != null) {
            bloomExtractShader.cleanup();
            bloomExtractShader = null;
        }
        
        if (bloomBlurShader != null) {
            bloomBlurShader.cleanup();
            bloomBlurShader = null;
        }
        
        if (bloomCombineShader != null) {
            bloomCombineShader.cleanup();
            bloomCombineShader = null;
        }
        
        if (toneMappingShader != null) {
            toneMappingShader.cleanup();
            toneMappingShader = null;
        }
        
        if (colorGradingShader != null) {
            colorGradingShader.cleanup();
            colorGradingShader = null;
        }
        
        if (finalCombineShader != null) {
            finalCombineShader.cleanup();
            finalCombineShader = null;
        }
        
        processingPasses.clear();
        initialized = false;
        
        logger.info("PostProcessingRenderer cleaned up");
    }
}