package com.odyssey.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Post-processing effects system for The Odyssey.
 * Handles bloom, tonemapping, volumetric lighting, atmospheric scattering,
 * depth of field, and chromatic aberration effects.
 */
public class PostProcessingSystem {
    private static final Logger logger = LoggerFactory.getLogger(PostProcessingSystem.class);
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Bloom effect
    private int[] bloomFBOs;
    private int[] bloomTextures;
    private Shader bloomDownsampleShader;
    private Shader bloomUpsampleShader;
    private Shader bloomCombineShader;
    private static final int BLOOM_MIPS = 6;
    
    // Volumetric lighting
    private int volumetricFBO;
    private int volumetricTexture;
    private Shader volumetricShader;
    
    // Atmospheric scattering
    private Shader atmosphericShader;
    
    // Depth of field
    private int dofFBO;
    private int dofTexture;
    private Shader dofShader;
    
    // Chromatic aberration
    private Shader chromaticAberrationShader;
    
    // Final composition
    private Shader finalCompositionShader;
    
    // Screen quad
    private int quadVAO;
    private int quadVBO;
    
    // Effect parameters
    private float bloomThreshold = 1.0f;
    private float bloomIntensity = 0.1f;
    private float exposure = 1.0f;
    private float gamma = 2.2f;
    private boolean bloomEnabled = true;
    private boolean volumetricLightingEnabled = true;
    private boolean atmosphericScatteringEnabled = true;
    private boolean depthOfFieldEnabled = false;
    private boolean chromaticAberrationEnabled = false;
    
    // Atmospheric parameters
    private Vector3f sunDirection = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    private Vector3f sunColor = new Vector3f(1.0f, 0.9f, 0.7f);
    private float atmosphericDensity = 1.0f;
    private float scatteringStrength = 0.5f;
    
    // DOF parameters
    private float focusDistance = 10.0f;
    private float focusRange = 5.0f;
    private float bokehRadius = 2.0f;
    
    // Chromatic aberration parameters
    private float chromaticIntensity = 0.002f;
    
    private boolean initialized = false;
    
    /**
     * Initializes the post-processing system.
     */
    public void initialize(int width, int height) throws Exception {
        if (initialized) {
            logger.warn("Post-processing system already initialized");
            return;
        }
        
        this.screenWidth = width;
        this.screenHeight = height;
        
        createScreenQuad();
        createBloomFramebuffers();
        createVolumetricFramebuffer();
        createDOFFramebuffer();
        initializeShaders();
        
        initialized = true;
        logger.info("Post-processing system initialized with resolution {}x{}", width, height);
    }
    
    /**
     * Creates a screen-space quad for post-processing.
     */
    private void createScreenQuad() {
        float[] quadVertices = {
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
        
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Creates bloom framebuffers with multiple mip levels.
     */
    private void createBloomFramebuffers() {
        bloomFBOs = new int[BLOOM_MIPS];
        bloomTextures = new int[BLOOM_MIPS];
        
        for (int i = 0; i < BLOOM_MIPS; i++) {
            int mipWidth = screenWidth >> (i + 1);
            int mipHeight = screenHeight >> (i + 1);
            
            bloomFBOs[i] = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, bloomFBOs[i]);
            
            bloomTextures[i] = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTextures[i]);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, mipWidth, mipHeight,
                             0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                       GL11.GL_TEXTURE_2D, bloomTextures[i], 0);
            
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Bloom framebuffer " + i + " is not complete");
            }
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Bloom framebuffers created with {} mip levels", BLOOM_MIPS);
    }
    
    /**
     * Creates volumetric lighting framebuffer.
     */
    private void createVolumetricFramebuffer() {
        volumetricFBO = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, volumetricFBO);
        
        volumetricTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, volumetricTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth / 2, screenHeight / 2,
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                   GL11.GL_TEXTURE_2D, volumetricTexture, 0);
        
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Volumetric framebuffer is not complete");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Volumetric lighting framebuffer created");
    }
    
    /**
     * Creates depth of field framebuffer.
     */
    private void createDOFFramebuffer() {
        dofFBO = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, dofFBO);
        
        dofTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dofTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight,
                         0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                   GL11.GL_TEXTURE_2D, dofTexture, 0);
        
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("DOF framebuffer is not complete");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Depth of field framebuffer created");
    }
    
    /**
     * Initializes all post-processing shaders.
     */
    private void initializeShaders() throws Exception {
        // Bloom shaders
        bloomDownsampleShader = new Shader();
        bloomDownsampleShader.createVertexShader(createQuadVertexShader());
        bloomDownsampleShader.createFragmentShader(createBloomDownsampleFragmentShader());
        bloomDownsampleShader.link();
        
        bloomUpsampleShader = new Shader();
        bloomUpsampleShader.createVertexShader(createQuadVertexShader());
        bloomUpsampleShader.createFragmentShader(createBloomUpsampleFragmentShader());
        bloomUpsampleShader.link();
        
        bloomCombineShader = new Shader();
        bloomCombineShader.createVertexShader(createQuadVertexShader());
        bloomCombineShader.createFragmentShader(createBloomCombineFragmentShader());
        bloomCombineShader.link();
        
        // Volumetric lighting shader
        volumetricShader = new Shader();
        volumetricShader.createVertexShader(createQuadVertexShader());
        volumetricShader.createFragmentShader(createVolumetricFragmentShader());
        volumetricShader.link();
        
        // Atmospheric scattering shader
        atmosphericShader = new Shader();
        atmosphericShader.createVertexShader(createQuadVertexShader());
        atmosphericShader.createFragmentShader(createAtmosphericFragmentShader());
        atmosphericShader.link();
        
        // Depth of field shader
        dofShader = new Shader();
        dofShader.createVertexShader(createQuadVertexShader());
        dofShader.createFragmentShader(createDOFFragmentShader());
        dofShader.link();
        
        // Chromatic aberration shader
        chromaticAberrationShader = new Shader();
        chromaticAberrationShader.createVertexShader(createQuadVertexShader());
        chromaticAberrationShader.createFragmentShader(createChromaticAberrationFragmentShader());
        chromaticAberrationShader.link();
        
        // Final composition shader
        finalCompositionShader = new Shader();
        finalCompositionShader.createVertexShader(createQuadVertexShader());
        finalCompositionShader.createFragmentShader(createFinalCompositionFragmentShader());
        finalCompositionShader.link();
        
        logger.info("Post-processing shaders initialized");
    }
    
    /**
     * Applies bloom effect to the bright texture.
     */
    public void applyBloom(int brightTexture) {
        if (!bloomEnabled) return;
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        
        // Downsample pass
        bloomDownsampleShader.bind();
        bloomDownsampleShader.setUniform("u_threshold", bloomThreshold);
        
        for (int i = 0; i < BLOOM_MIPS; i++) {
            int mipWidth = screenWidth >> (i + 1);
            int mipHeight = screenHeight >> (i + 1);
            
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, bloomFBOs[i]);
            GL11.glViewport(0, 0, mipWidth, mipHeight);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            if (i == 0) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, brightTexture);
            } else {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTextures[i - 1]);
            }
            bloomDownsampleShader.setUniform("u_texture", 0);
            bloomDownsampleShader.setUniform("u_texelSize", new Vector2f(1.0f / mipWidth, 1.0f / mipHeight));
            
            renderQuad();
        }
        bloomDownsampleShader.unbind();
        
        // Upsample pass
        bloomUpsampleShader.bind();
        bloomUpsampleShader.setUniform("u_intensity", bloomIntensity);
        
        for (int i = BLOOM_MIPS - 2; i >= 0; i--) {
            int mipWidth = screenWidth >> (i + 1);
            int mipHeight = screenHeight >> (i + 1);
            
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, bloomFBOs[i]);
            GL11.glViewport(0, 0, mipWidth, mipHeight);
            
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTextures[i + 1]);
            bloomUpsampleShader.setUniform("u_texture", 0);
            bloomUpsampleShader.setUniform("u_texelSize", new Vector2f(1.0f / mipWidth, 1.0f / mipHeight));
            
            renderQuad();
            
            GL11.glDisable(GL11.GL_BLEND);
        }
        bloomUpsampleShader.unbind();
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Applies volumetric lighting effect.
     */
    public void applyVolumetricLighting(int depthTexture, int shadowMap) {
        if (!volumetricLightingEnabled) return;
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, volumetricFBO);
        GL11.glViewport(0, 0, screenWidth / 2, screenHeight / 2);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        volumetricShader.bind();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        volumetricShader.setUniform("u_depthTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMap);
        volumetricShader.setUniform("u_shadowMap", 1);
        
        volumetricShader.setUniform("u_sunDirection", sunDirection);
        volumetricShader.setUniform("u_sunColor", sunColor);
        volumetricShader.setUniform("u_scatteringStrength", scatteringStrength);
        
        renderQuad();
        
        volumetricShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Applies atmospheric scattering effect.
     */
    public void applyAtmosphericScattering(int colorTexture, int depthTexture) {
        if (!atmosphericScatteringEnabled) return;
        
        atmosphericShader.bind();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        atmosphericShader.setUniform("u_colorTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        atmosphericShader.setUniform("u_depthTexture", 1);
        
        atmosphericShader.setUniform("u_sunDirection", sunDirection);
        atmosphericShader.setUniform("u_sunColor", sunColor);
        atmosphericShader.setUniform("u_atmosphericDensity", atmosphericDensity);
        
        renderQuad();
        
        atmosphericShader.unbind();
    }
    
    /**
     * Applies depth of field effect.
     */
    public void applyDepthOfField(int colorTexture, int depthTexture) {
        if (!depthOfFieldEnabled) return;
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, dofFBO);
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        
        dofShader.bind();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        dofShader.setUniform("u_colorTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        dofShader.setUniform("u_depthTexture", 1);
        
        dofShader.setUniform("u_focusDistance", focusDistance);
        dofShader.setUniform("u_focusRange", focusRange);
        dofShader.setUniform("u_bokehRadius", bokehRadius);
        dofShader.setUniform("u_texelSize", new Vector2f(1.0f / screenWidth, 1.0f / screenHeight));
        
        renderQuad();
        
        dofShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Applies chromatic aberration effect.
     */
    public void applyChromaticAberration(int colorTexture) {
        if (!chromaticAberrationEnabled) return;
        
        chromaticAberrationShader.bind();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        chromaticAberrationShader.setUniform("u_colorTexture", 0);
        chromaticAberrationShader.setUniform("u_intensity", chromaticIntensity);
        
        renderQuad();
        
        chromaticAberrationShader.unbind();
    }
    
    /**
     * Performs final composition with tonemapping and gamma correction.
     */
    public void performFinalComposition(int colorTexture, int bloomTexture, int volumetricTexture) {
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        finalCompositionShader.bind();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        finalCompositionShader.setUniform("u_colorTexture", 0);
        
        if (bloomEnabled && bloomTexture != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTexture);
            finalCompositionShader.setUniform("u_bloomTexture", 1);
            finalCompositionShader.setUniform("u_bloomEnabled", true);
        } else {
            finalCompositionShader.setUniform("u_bloomEnabled", false);
        }
        
        if (volumetricLightingEnabled && volumetricTexture != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, volumetricTexture);
            finalCompositionShader.setUniform("u_volumetricTexture", 2);
            finalCompositionShader.setUniform("u_volumetricEnabled", true);
        } else {
            finalCompositionShader.setUniform("u_volumetricEnabled", false);
        }
        
        finalCompositionShader.setUniform("u_exposure", exposure);
        finalCompositionShader.setUniform("u_gamma", gamma);
        finalCompositionShader.setUniform("u_bloomIntensity", bloomIntensity);
        
        renderQuad();
        
        finalCompositionShader.unbind();
    }
    
    /**
     * Renders a screen-space quad.
     */
    private void renderQuad() {
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Gets the bloom texture for the specified mip level.
     */
    public int getBloomTexture(int mipLevel) {
        if (mipLevel >= 0 && mipLevel < BLOOM_MIPS) {
            return bloomTextures[mipLevel];
        }
        return 0;
    }
    
    /**
     * Gets the volumetric lighting texture.
     */
    public int getVolumetricTexture() {
        return volumetricTexture;
    }
    
    /**
     * Gets the depth of field texture.
     */
    public int getDOFTexture() {
        return dofTexture;
    }
    
    // Getters and setters for effect parameters
    public void setBloomThreshold(float threshold) { this.bloomThreshold = threshold; }
    public void setBloomIntensity(float intensity) { this.bloomIntensity = intensity; }
    public void setExposure(float exposure) { this.exposure = exposure; }
    public void setGamma(float gamma) { this.gamma = gamma; }
    public void setBloomEnabled(boolean enabled) { this.bloomEnabled = enabled; }
    public void setVolumetricLightingEnabled(boolean enabled) { this.volumetricLightingEnabled = enabled; }
    public void setAtmosphericScatteringEnabled(boolean enabled) { this.atmosphericScatteringEnabled = enabled; }
    public void setDepthOfFieldEnabled(boolean enabled) { this.depthOfFieldEnabled = enabled; }
    public void setChromaticAberrationEnabled(boolean enabled) { this.chromaticAberrationEnabled = enabled; }
    
    public void setSunDirection(Vector3f direction) { this.sunDirection.set(direction).normalize(); }
    public void setSunColor(Vector3f color) { this.sunColor.set(color); }
    public void setAtmosphericDensity(float density) { this.atmosphericDensity = density; }
    public void setScatteringStrength(float strength) { this.scatteringStrength = strength; }
    
    public void setFocusDistance(float distance) { this.focusDistance = distance; }
    public void setFocusRange(float range) { this.focusRange = range; }
    public void setBokehRadius(float radius) { this.bokehRadius = radius; }
    
    public void setChromaticIntensity(float intensity) { this.chromaticIntensity = intensity; }
    
    /**
     * Creates a basic quad vertex shader.
     */
    private String createQuadVertexShader() {
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
     * Creates bloom downsample fragment shader.
     */
    private String createBloomDownsampleFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_texture;
            uniform float u_threshold;
            uniform vec2 u_texelSize;
            
            void main() {
                vec3 color = texture(u_texture, v_texCoord).rgb;
                
                // Apply threshold for first mip level
                float brightness = dot(color, vec3(0.2126, 0.7152, 0.0722));
                if (brightness > u_threshold) {
                    fragColor = vec4(color, 1.0);
                } else {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                }
            }
            """;
    }
    
    /**
     * Creates bloom upsample fragment shader.
     */
    private String createBloomUpsampleFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_texture;
            uniform float u_intensity;
            uniform vec2 u_texelSize;
            
            void main() {
                vec3 color = texture(u_texture, v_texCoord).rgb;
                fragColor = vec4(color * u_intensity, 1.0);
            }
            """;
    }
    
    /**
     * Creates bloom combine fragment shader.
     */
    private String createBloomCombineFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform sampler2D u_bloomTexture;
            uniform float u_bloomIntensity;
            
            void main() {
                vec3 color = texture(u_colorTexture, v_texCoord).rgb;
                vec3 bloom = texture(u_bloomTexture, v_texCoord).rgb;
                
                color += bloom * u_bloomIntensity;
                
                fragColor = vec4(color, 1.0);
            }
            """;
    }
    
    /**
     * Creates volumetric lighting fragment shader.
     */
    private String createVolumetricFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_depthTexture;
            uniform sampler2D u_shadowMap;
            uniform vec3 u_sunDirection;
            uniform vec3 u_sunColor;
            uniform float u_scatteringStrength;
            
            const int NUM_SAMPLES = 32;
            const float DENSITY = 0.01;
            
            void main() {
                float depth = texture(u_depthTexture, v_texCoord).r;
                
                vec3 scattering = vec3(0.0);
                float stepSize = depth / float(NUM_SAMPLES);
                
                for (int i = 0; i < NUM_SAMPLES; i++) {
                    float currentDepth = float(i) * stepSize;
                    
                    // Simple volumetric scattering calculation
                    float scatter = exp(-currentDepth * DENSITY) * u_scatteringStrength;
                    scattering += u_sunColor * scatter;
                }
                
                scattering /= float(NUM_SAMPLES);
                
                fragColor = vec4(scattering, 1.0);
            }
            """;
    }
    
    /**
     * Creates atmospheric scattering fragment shader.
     */
    private String createAtmosphericFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform sampler2D u_depthTexture;
            uniform vec3 u_sunDirection;
            uniform vec3 u_sunColor;
            uniform float u_atmosphericDensity;
            
            void main() {
                vec3 color = texture(u_colorTexture, v_texCoord).rgb;
                float depth = texture(u_depthTexture, v_texCoord).r;
                
                // Simple atmospheric scattering
                float distance = depth * 1000.0; // Convert to world units
                float scattering = exp(-distance * u_atmosphericDensity * 0.0001);
                
                vec3 atmosphericColor = u_sunColor * (1.0 - scattering);
                color = mix(atmosphericColor, color, scattering);
                
                fragColor = vec4(color, 1.0);
            }
            """;
    }
    
    /**
     * Creates depth of field fragment shader.
     */
    private String createDOFFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform sampler2D u_depthTexture;
            uniform float u_focusDistance;
            uniform float u_focusRange;
            uniform float u_bokehRadius;
            uniform vec2 u_texelSize;
            
            void main() {
                vec3 color = texture(u_colorTexture, v_texCoord).rgb;
                float depth = texture(u_depthTexture, v_texCoord).r;
                
                // Convert depth to world space distance
                float distance = depth * 1000.0;
                
                // Calculate blur amount based on focus
                float blur = abs(distance - u_focusDistance) / u_focusRange;
                blur = clamp(blur, 0.0, 1.0);
                
                if (blur > 0.1) {
                    // Simple box blur for DOF
                    vec3 blurredColor = vec3(0.0);
                    float samples = 0.0;
                    
                    float radius = blur * u_bokehRadius;
                    for (float x = -radius; x <= radius; x += 1.0) {
                        for (float y = -radius; y <= radius; y += 1.0) {
                            vec2 offset = vec2(x, y) * u_texelSize;
                            blurredColor += texture(u_colorTexture, v_texCoord + offset).rgb;
                            samples += 1.0;
                        }
                    }
                    
                    color = blurredColor / samples;
                }
                
                fragColor = vec4(color, 1.0);
            }
            """;
    }
    
    /**
     * Creates chromatic aberration fragment shader.
     */
    private String createChromaticAberrationFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform float u_intensity;
            
            void main() {
                vec2 center = vec2(0.5);
                vec2 offset = (v_texCoord - center) * u_intensity;
                
                float r = texture(u_colorTexture, v_texCoord + offset).r;
                float g = texture(u_colorTexture, v_texCoord).g;
                float b = texture(u_colorTexture, v_texCoord - offset).b;
                
                fragColor = vec4(r, g, b, 1.0);
            }
            """;
    }
    
    /**
     * Creates final composition fragment shader.
     */
    private String createFinalCompositionFragmentShader() {
        return """
            #version 330 core
            
            out vec4 fragColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_colorTexture;
            uniform sampler2D u_bloomTexture;
            uniform sampler2D u_volumetricTexture;
            uniform bool u_bloomEnabled;
            uniform bool u_volumetricEnabled;
            uniform float u_exposure;
            uniform float u_gamma;
            uniform float u_bloomIntensity;
            
            // ACES tonemapping
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
                
                // Add bloom
                if (u_bloomEnabled) {
                    vec3 bloom = texture(u_bloomTexture, v_texCoord).rgb;
                    color += bloom * u_bloomIntensity;
                }
                
                // Add volumetric lighting
                if (u_volumetricEnabled) {
                    vec3 volumetric = texture(u_volumetricTexture, v_texCoord).rgb;
                    color += volumetric;
                }
                
                // Exposure
                color *= u_exposure;
                
                // Tonemapping
                color = aces(color);
                
                // Gamma correction
                color = pow(color, vec3(1.0 / u_gamma));
                
                fragColor = vec4(color, 1.0);
            }
            """;
    }
    
    /**
     * Resizes all framebuffers.
     */
    public void resize(int width, int height) {
        if (width == screenWidth && height == screenHeight) {
            return;
        }
        
        screenWidth = width;
        screenHeight = height;
        
        // Cleanup old buffers
        cleanupBuffers();
        
        // Recreate buffers
        try {
            createBloomFramebuffers();
            createVolumetricFramebuffer();
            createDOFFramebuffer();
            logger.info("Post-processing system resized to {}x{}", width, height);
        } catch (Exception e) {
            logger.error("Failed to resize post-processing system", e);
        }
    }
    
    /**
     * Cleans up framebuffer resources.
     */
    private void cleanupBuffers() {
        if (bloomFBOs != null) {
            for (int i = 0; i < BLOOM_MIPS; i++) {
                if (bloomFBOs[i] != 0) {
                    GL30.glDeleteFramebuffers(bloomFBOs[i]);
                    GL11.glDeleteTextures(bloomTextures[i]);
                }
            }
        }
        
        if (volumetricFBO != 0) {
            GL30.glDeleteFramebuffers(volumetricFBO);
            GL11.glDeleteTextures(volumetricTexture);
        }
        
        if (dofFBO != 0) {
            GL30.glDeleteFramebuffers(dofFBO);
            GL11.glDeleteTextures(dofTexture);
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
        
        if (bloomDownsampleShader != null) bloomDownsampleShader.cleanup();
        if (bloomUpsampleShader != null) bloomUpsampleShader.cleanup();
        if (bloomCombineShader != null) bloomCombineShader.cleanup();
        if (volumetricShader != null) volumetricShader.cleanup();
        if (atmosphericShader != null) atmosphericShader.cleanup();
        if (dofShader != null) dofShader.cleanup();
        if (chromaticAberrationShader != null) chromaticAberrationShader.cleanup();
        if (finalCompositionShader != null) finalCompositionShader.cleanup();
        
        initialized = false;
        logger.info("Post-processing system cleaned up");
    }
}