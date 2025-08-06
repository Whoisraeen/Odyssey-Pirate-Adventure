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
    private Shader lightingShader;
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
        
        // Create lighting shader
        lightingShader = new Shader();
        lightingShader.createVertexShader(createLightingVertexShader());
        lightingShader.createFragmentShader(createLightingFragmentShader());
        lightingShader.link();
        
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
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gAlbedoTexture);
        lightingShader.setUniform("u_gAlbedo", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gNormalTexture);
        lightingShader.setUniform("u_gNormal", 1);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gPositionTexture);
        lightingShader.setUniform("u_gPosition", 2);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gEmissionTexture);
        lightingShader.setUniform("u_gEmission", 3);
        
        // Set camera position
        lightingShader.setUniform("u_cameraPosition", cameraPosition);
        
        // Set lighting parameters
        setLightingUniforms(lightingSystem);
        
        // Render screen quad
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        
        lightingShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Sets lighting uniforms for the lighting shader.
     */
    private void setLightingUniforms(LightingSystem lightingSystem) {
        lightingShader.setUniform("u_ambientColor", lightingSystem.getAmbientColor());
        lightingShader.setUniform("u_ambientIntensity", lightingSystem.getAmbientIntensity());
        lightingShader.setUniform("u_directionalLightCount", lightingSystem.getDirectionalLightCount());
        lightingShader.setUniform("u_pointLightCount", lightingSystem.getPointLightCount());
        lightingShader.setUniform("u_spotLightCount", lightingSystem.getSpotLightCount());
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
     * Creates the lighting vertex shader source.
     */
    private String createLightingVertexShader() {
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
     * Creates the lighting fragment shader source.
     */
    private String createLightingFragmentShader() {
        return """
            #version 330 core
            
            layout (location = 0) out vec4 fragColor;
            layout (location = 1) out vec4 brightColor;
            
            in vec2 v_texCoord;
            
            uniform sampler2D u_gAlbedo;
            uniform sampler2D u_gNormal;
            uniform sampler2D u_gPosition;
            uniform sampler2D u_gEmission;
            
            uniform vec3 u_cameraPosition;
            uniform vec3 u_ambientColor;
            uniform float u_ambientIntensity;
            
            const float PI = 3.14159265359;
            
            vec3 fresnelSchlick(float cosTheta, vec3 F0) {
                return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
            }
            
            float distributionGGX(vec3 N, vec3 H, float roughness) {
                float a = roughness * roughness;
                float a2 = a * a;
                float NdotH = max(dot(N, H), 0.0);
                float NdotH2 = NdotH * NdotH;
                
                float num = a2;
                float denom = (NdotH2 * (a2 - 1.0) + 1.0);
                denom = PI * denom * denom;
                
                return num / denom;
            }
            
            float geometrySchlickGGX(float NdotV, float roughness) {
                float r = (roughness + 1.0);
                float k = (r * r) / 8.0;
                
                float num = NdotV;
                float denom = NdotV * (1.0 - k) + k;
                
                return num / denom;
            }
            
            float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
                float NdotV = max(dot(N, V), 0.0);
                float NdotL = max(dot(N, L), 0.0);
                float ggx2 = geometrySchlickGGX(NdotV, roughness);
                float ggx1 = geometrySchlickGGX(NdotL, roughness);
                
                return ggx1 * ggx2;
            }
            
            void main() {
                // Sample G-Buffer
                vec4 albedoMetallic = texture(u_gAlbedo, v_texCoord);
                vec4 normalRoughness = texture(u_gNormal, v_texCoord);
                vec4 positionDepth = texture(u_gPosition, v_texCoord);
                vec4 emissionAO = texture(u_gEmission, v_texCoord);
                
                vec3 albedo = albedoMetallic.rgb;
                float metallic = albedoMetallic.a;
                vec3 normal = normalize(normalRoughness.rgb * 2.0 - 1.0);
                float roughness = normalRoughness.a;
                vec3 worldPos = positionDepth.rgb;
                vec3 emission = emissionAO.rgb;
                float ao = emissionAO.a;
                
                // Calculate view direction
                vec3 V = normalize(u_cameraPosition - worldPos);
                
                // Calculate F0 (surface reflection at zero incidence)
                vec3 F0 = vec3(0.04);
                F0 = mix(F0, albedo, metallic);
                
                // Start with ambient lighting
                vec3 color = u_ambientColor * u_ambientIntensity * albedo * ao;
                
                // Add emission
                color += emission;
                
                // Simple directional light for now
                vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
                vec3 lightColor = vec3(1.0);
                
                vec3 L = lightDir;
                vec3 H = normalize(V + L);
                float NdotL = max(dot(normal, L), 0.0);
                
                if (NdotL > 0.0) {
                    // Cook-Torrance BRDF
                    float NDF = distributionGGX(normal, H, roughness);
                    float G = geometrySmith(normal, V, L, roughness);
                    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
                    
                    vec3 kS = F;
                    vec3 kD = vec3(1.0) - kS;
                    kD *= 1.0 - metallic;
                    
                    vec3 numerator = NDF * G * F;
                    float denominator = 4.0 * max(dot(normal, V), 0.0) * NdotL + 0.0001;
                    vec3 specular = numerator / denominator;
                    
                    color += (kD * albedo / PI + specular) * lightColor * NdotL;
                }
                
                fragColor = vec4(color, 1.0);
                
                // Extract bright areas for bloom
                float brightness = dot(color, vec3(0.2126, 0.7152, 0.0722));
                brightColor = brightness > 1.0 ? vec4(color, 1.0) : vec4(0.0, 0.0, 0.0, 1.0);
            }
            """;
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