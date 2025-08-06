package com.odyssey.graphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Deferred rendering pipeline for The Odyssey.
 * Implements G-Buffer rendering for PBR materials and lighting calculations.
 * Supports multiple render targets and post-processing effects.
 */
public class DeferredRenderer {
    private static final Logger logger = LoggerFactory.getLogger(DeferredRenderer.class);
    
    // G-Buffer components
    private int gBufferFBO;
    private int gAlbedoTexture;      // RGB: albedo, A: metallic
    private int gNormalTexture;      // RGB: world normal, A: roughness
    private int gPositionTexture;    // RGB: world position, A: depth
    private int gEmissionTexture;    // RGB: emission, A: AO
    private int gDepthTexture;       // Depth buffer
    
    // Screen-space quad for lighting pass
    private int quadVAO;
    private int quadVBO;
    
    // Shaders
    private PBRShader pbrShader;
    private LightingPassShader lightingShader;
    private Shader postProcessShader;
    
    // Render targets
    private int screenWidth;
    private int screenHeight;
    
    // Post-processing framebuffers
    private int postProcessFBO;
    private int colorTexture;
    private int brightTexture;  // For bloom
    
    private boolean initialized = false;
    
    /**
     * Initializes the deferred renderer with the specified screen dimensions.
     */
    public void initialize(int width, int height) throws Exception {
        if (initialized) {
            logger.warn("Deferred renderer already initialized");
            return;
        }
        
        this.screenWidth = width;
        this.screenHeight = height;
        
        createGBuffer();
        createPostProcessFramebuffer();
        createScreenQuad();
        initializeShaders();
        
        initialized = true;
        logger.info("Deferred renderer initialized with resolution {}x{}", width, height);
    }
    
    /**
     * Creates the G-Buffer framebuffer and textures.
     */
    private void createGBuffer() {
        // Generate framebuffer
        gBufferFBO = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, gBufferFBO);
        
        // Albedo + Metallic texture
        gAlbedoTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gAlbedoTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
                                   GL11.GL_TEXTURE_2D, gAlbedoTexture, 0);
        
        // Normal + Roughness texture
        gNormalTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gNormalTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, 
                                   GL11.GL_TEXTURE_2D, gNormalTexture, 0);
        
        // Position + Depth texture
        gPositionTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gPositionTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, 
                                   GL11.GL_TEXTURE_2D, gPositionTexture, 0);
        
        // Emission + AO texture
        gEmissionTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gEmissionTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT3, 
                                   GL11.GL_TEXTURE_2D, gEmissionTexture, 0);
        
        // Depth texture
        gDepthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gDepthTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, screenWidth, screenHeight, 
                         0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 
                                   GL11.GL_TEXTURE_2D, gDepthTexture, 0);
        
        // Set draw buffers
        int[] drawBuffers = {
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_COLOR_ATTACHMENT1,
            GL30.GL_COLOR_ATTACHMENT2,
            GL30.GL_COLOR_ATTACHMENT3
        };
        GL20.glDrawBuffers(drawBuffers);
        
        // Check framebuffer completeness
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("G-Buffer framebuffer is not complete");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("G-Buffer created successfully");
    }
    
    /**
     * Creates the post-processing framebuffer.
     */
    private void createPostProcessFramebuffer() {
        postProcessFBO = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, postProcessFBO);
        
        // Color texture
        colorTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
                                   GL11.GL_TEXTURE_2D, colorTexture, 0);
        
        // Bright texture for bloom
        brightTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, brightTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, 
                                   GL11.GL_TEXTURE_2D, brightTexture, 0);
        
        int[] drawBuffers = {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1};
        GL20.glDrawBuffers(drawBuffers);
        
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Post-process framebuffer is not complete");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Post-process framebuffer created successfully");
    }
    
    /**
     * Creates a screen-space quad for lighting and post-processing passes.
     */
    private void createScreenQuad() {
        float[] quadVertices = {
            // positions   // texCoords
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
        };
        
        int[] quadIndices = {0, 1, 2, 0, 2, 3};
        
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL30.glGenBuffers();
        int quadEBO = GL30.glGenBuffers();
        
        GL30.glBindVertexArray(quadVAO);
        
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, quadVBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, quadVertices, GL30.GL_STATIC_DRAW);
        
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL30.GL_STATIC_DRAW);
        
        // Position attribute
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
        logger.info("Screen quad created successfully");
    }
    
    /**
     * Initializes all required shaders.
     */
    private void initializeShaders() throws Exception {
        pbrShader = new PBRShader();
        pbrShader.initialize();
        
        // Create lighting pass shader
        lightingShader = new LightingPassShader();
        lightingShader.initialize();
        
        // Create post-process shader
        postProcessShader = new Shader();
        postProcessShader.createVertexShader(createPostProcessVertexShader());
        postProcessShader.createFragmentShader(createPostProcessFragmentShader());
        postProcessShader.link();
        
        logger.info("Deferred rendering shaders initialized");
    }
    
    /**
     * Begins the geometry pass by binding the G-Buffer.
     */
    public void beginGeometryPass() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, gBufferFBO);
        
        // Validate dimensions match actual window
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int actualWidth = viewport[2];
        int actualHeight = viewport[3];
        
        // Check if our G-Buffer size matches the actual viewport
        if (actualWidth != screenWidth || actualHeight != screenHeight) {
            logger.warn("G-Buffer size mismatch: G-Buffer={}x{}, Viewport={}x{}", 
                       screenWidth, screenHeight, actualWidth, actualHeight);
            // Auto-resize if dimensions are significantly different
            if (Math.abs(actualWidth - screenWidth) > 1 || Math.abs(actualHeight - screenHeight) > 1) {
                logger.info("Auto-resizing G-Buffer to match viewport");
                resize(actualWidth, actualHeight);
            }
        }
        
        // Set viewport to our G-Buffer dimensions
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
    }
    
    /**
     * Ends the geometry pass.
     */
    public void endGeometryPass() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Performs the lighting pass using the G-Buffer data.
     */
    public void performLightingPass(LightingSystem lightingSystem, Vector3f cameraPosition) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, postProcessFBO);
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        lightingShader.bind();
        
        // Bind G-Buffer textures
        lightingShader.bindGBufferTextures(gAlbedoTexture, gNormalTexture, gPositionTexture, gEmissionTexture);
        
        // Bind IBL textures if available
        if (lightingSystem.isIBLEnabled()) {
            lightingShader.bindIBLTextures(
                lightingSystem.getIrradianceMap(),
                lightingSystem.getPrefilterMap(),
                lightingSystem.getBRDFLUT()
            );
        }
        
        // Bind shadow map if shadows are enabled
        if (lightingSystem.areShadowsEnabled()) {
            // TODO: Get shadow map from lighting system when implemented
            // lightingShader.bindShadowMap(lightingSystem.getShadowMap());
        }
        
        // Set camera position
        lightingShader.setCameraPosition(cameraPosition);
        
        // Set lighting parameters
        lightingShader.setLightingParameters(lightingSystem);
        
        // Set light data
        setLightData(lightingSystem);
        
        // Render screen quad
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        
        lightingShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Sets light data for the lighting shader.
     */
    private void setLightData(LightingSystem lightingSystem) {
        // Set directional lights
        LightingSystem.DirectionalLight[] directionalLights = lightingSystem.getDirectionalLights().toArray(new LightingSystem.DirectionalLight[0]);
        lightingShader.setDirectionalLights(directionalLights);
        
        // Set point lights
        LightingSystem.PointLight[] pointLights = lightingSystem.getPointLights().toArray(new LightingSystem.PointLight[0]);
        lightingShader.setPointLights(pointLights);
        
        // Set spot lights
        LightingSystem.SpotLight[] spotLights = lightingSystem.getSpotLights().toArray(new LightingSystem.SpotLight[0]);
        lightingShader.setSpotLights(spotLights);
    }
    
    /**
     * Performs post-processing effects and renders to screen.
     */
    public void performPostProcessPass() {
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        postProcessShader.bind();
        
        // Bind color texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        postProcessShader.setUniform("u_colorTexture", 0);
        
        // Bind bright texture for bloom
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, brightTexture);
        postProcessShader.setUniform("u_brightTexture", 1);
        
        // Render screen quad
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        
        postProcessShader.unbind();
    }
    
    /**
     * Gets the PBR shader for geometry rendering.
     */
    public PBRShader getPBRShader() {
        return pbrShader;
    }
    
    /**
     * Gets the final rendered texture.
     */
    public int getFinalTexture() {
        return colorTexture;
    }
    
    /**
     * Gets the depth texture from the G-Buffer.
     */
    public int getDepthTexture() {
        return gDepthTexture;
    }
    
    /**
     * Gets the G-Buffer framebuffer object ID.
     * Used for depth buffer copying and other advanced operations.
     */
    public int getGBufferFBO() {
        return gBufferFBO;
    }
    
    /**
     * Presents the final result to the screen.
     */
    public void presentFinalResult() {
        performPostProcessPass();
    }
    
    /**
     * Resizes the deferred renderer buffers.
     */
    public void resize(int width, int height) {
        if (width == screenWidth && height == screenHeight) {
            return;
        }
        
        screenWidth = width;
        screenHeight = height;
        
        // Cleanup old buffers
        cleanupBuffers();
        
        // Recreate buffers with new size
        try {
            createGBuffer();
            createPostProcessFramebuffer();
            logger.info("Deferred renderer resized to {}x{}", width, height);
        } catch (Exception e) {
            logger.error("Failed to resize deferred renderer", e);
        }
    }
    

    
    /**
     * Creates the post-process vertex shader source.
     */
    private String createPostProcessVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec2 a_position;
            layout (location = 1) in vec2 a_texCoord;
            
            out vec2 v_texCoord;
            
            void main() {
                v_texCoord = a_texCoord;
                gl_Position = vec4(a_position, 0.0, 1.0);
            }
            """;
    }
    
    /**
     * Creates the post-process fragment shader source.
     */
    private String createPostProcessFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform sampler2D u_brightTexture;
            
            // Simple tonemapping
            vec3 aces(vec3 color) {
                float a = 2.51;
                float b = 0.03;
                float c = 2.43;
                float d = 0.59;
                float e = 0.14;
                return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
            }
            
            void main() {
                vec3 color = texture(u_colorTexture, v_texCoord).rgb;
                vec3 bloom = texture(u_brightTexture, v_texCoord).rgb;
                
                // Simple bloom addition
                color += bloom * 0.1;
                
                // Tonemapping
                color = aces(color);
                
                // Gamma correction
                color = pow(color, vec3(1.0 / 2.2));
                
                fragColor = vec4(color, 1.0);
            }
            """;
    }
    
    /**
     * Cleans up framebuffer resources.
     */
    private void cleanupBuffers() {
        if (gBufferFBO != 0) {
            GL30.glDeleteFramebuffers(gBufferFBO);
            GL11.glDeleteTextures(gAlbedoTexture);
            GL11.glDeleteTextures(gNormalTexture);
            GL11.glDeleteTextures(gPositionTexture);
            GL11.glDeleteTextures(gEmissionTexture);
            GL11.glDeleteTextures(gDepthTexture);
        }
        
        if (postProcessFBO != 0) {
            GL30.glDeleteFramebuffers(postProcessFBO);
            GL11.glDeleteTextures(colorTexture);
            GL11.glDeleteTextures(brightTexture);
        }
    }
    
    /**
     * Cleans up all resources.
     */
    public void cleanup() {
        cleanupBuffers();
        
        if (quadVAO != 0) {
            GL30.glDeleteVertexArrays(quadVAO);
            GL30.glDeleteBuffers(quadVBO);
        }
        
        if (pbrShader != null) {
            pbrShader.cleanup();
        }
        
        if (lightingShader != null) {
            lightingShader.cleanup();
        }
        
        if (postProcessShader != null) {
            postProcessShader.cleanup();
        }
        
        initialized = false;
        logger.info("Deferred renderer cleaned up");
    }
}