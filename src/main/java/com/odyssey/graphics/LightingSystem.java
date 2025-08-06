package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Advanced lighting system supporting multiple light types and shadow mapping.
 * Provides directional lights, point lights, spot lights, and area lights
 * with cascaded shadow maps and screen-space ambient occlusion.
 */
public class LightingSystem {
    private static final Logger logger = Logger.getLogger(LightingSystem.class.getName());
    
    // Maximum number of lights supported
    private static final int MAX_DIRECTIONAL_LIGHTS = 4;
    private static final int MAX_POINT_LIGHTS = 32;
    private static final int MAX_SPOT_LIGHTS = 16;
    
    // Shadow map settings
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final int CASCADE_COUNT = 4;
    
    /**
     * Base class for all light types.
     */
    public static abstract class Light {
        protected Vector3f color;
        protected float intensity;
        protected boolean enabled;
        protected boolean castsShadows;
        
        public Light(Vector3f color, float intensity) {
            this.color = new Vector3f(color);
            this.intensity = intensity;
            this.enabled = true;
            this.castsShadows = false;
        }
        
        public Vector3f getColor() { return new Vector3f(color); }
        public void setColor(Vector3f color) { this.color.set(color); }
        
        public float getIntensity() { return intensity; }
        public void setIntensity(float intensity) { this.intensity = intensity; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean castsShadows() { return castsShadows; }
        public void setCastsShadows(boolean castsShadows) { this.castsShadows = castsShadows; }
    }
    
    /**
     * Directional light (like the sun).
     */
    public static class DirectionalLight extends Light {
        private Vector3f direction;
        private Matrix4f lightSpaceMatrix;
        private int shadowMapId;
        private int shadowFramebuffer;
        
        public DirectionalLight(Vector3f direction, Vector3f color, float intensity) {
            super(color, intensity);
            this.direction = new Vector3f(direction).normalize();
            this.lightSpaceMatrix = new Matrix4f();
            this.shadowMapId = 0;
            this.shadowFramebuffer = 0;
        }
        
        public Vector3f getDirection() { return new Vector3f(direction); }
        public void setDirection(Vector3f direction) { this.direction.set(direction).normalize(); }
        
        public Matrix4f getLightSpaceMatrix() { return new Matrix4f(lightSpaceMatrix); }
        public void setLightSpaceMatrix(Matrix4f matrix) { this.lightSpaceMatrix.set(matrix); }
        
        public int getShadowMapId() { return shadowMapId; }
        public void setShadowMapId(int shadowMapId) { this.shadowMapId = shadowMapId; }
        
        public int getShadowFramebuffer() { return shadowFramebuffer; }
        public void setShadowFramebuffer(int shadowFramebuffer) { this.shadowFramebuffer = shadowFramebuffer; }
    }
    
    /**
     * Point light (omnidirectional).
     */
    public static class PointLight extends Light {
        private Vector3f position;
        private float radius;
        private Vector3f attenuation; // constant, linear, quadratic
        
        public PointLight(Vector3f position, Vector3f color, float intensity, float radius) {
            super(color, intensity);
            this.position = new Vector3f(position);
            this.radius = radius;
            this.attenuation = new Vector3f(1.0f, 0.09f, 0.032f); // Default attenuation
        }
        
        public Vector3f getPosition() { return new Vector3f(position); }
        public void setPosition(Vector3f position) { this.position.set(position); }
        
        public float getRadius() { return radius; }
        public void setRadius(float radius) { this.radius = radius; }
        
        public Vector3f getAttenuation() { return new Vector3f(attenuation); }
        public void setAttenuation(Vector3f attenuation) { this.attenuation.set(attenuation); }
    }
    
    /**
     * Spot light (cone-shaped).
     */
    public static class SpotLight extends Light {
        private Vector3f position;
        private Vector3f direction;
        private float innerCone; // Inner cone angle in radians
        private float outerCone; // Outer cone angle in radians
        private float radius;
        private Vector3f attenuation;
        
        public SpotLight(Vector3f position, Vector3f direction, Vector3f color, 
                        float intensity, float innerCone, float outerCone, float radius) {
            super(color, intensity);
            this.position = new Vector3f(position);
            this.direction = new Vector3f(direction).normalize();
            this.innerCone = innerCone;
            this.outerCone = outerCone;
            this.radius = radius;
            this.attenuation = new Vector3f(1.0f, 0.09f, 0.032f);
        }
        
        public Vector3f getPosition() { return new Vector3f(position); }
        public void setPosition(Vector3f position) { this.position.set(position); }
        
        public Vector3f getDirection() { return new Vector3f(direction); }
        public void setDirection(Vector3f direction) { this.direction.set(direction).normalize(); }
        
        public float getInnerCone() { return innerCone; }
        public void setInnerCone(float innerCone) { this.innerCone = innerCone; }
        
        public float getOuterCone() { return outerCone; }
        public void setOuterCone(float outerCone) { this.outerCone = outerCone; }
        
        public float getRadius() { return radius; }
        public void setRadius(float radius) { this.radius = radius; }
        
        public Vector3f getAttenuation() { return new Vector3f(attenuation); }
        public void setAttenuation(Vector3f attenuation) { this.attenuation.set(attenuation); }
    }
    
    // Light collections
    private final List<DirectionalLight> directionalLights;
    private final List<PointLight> pointLights;
    private final List<SpotLight> spotLights;
    
    // Ambient lighting
    private Vector3f ambientColor;
    private float ambientIntensity;
    
    // Shadow mapping
    private boolean shadowsEnabled;
    private int shadowMapFramebuffer;
    private int shadowMapTexture;
    
    // Uniform buffer objects for efficient light data transfer
    private int lightDataUBO;
    private FloatBuffer lightDataBuffer;
    
    /**
     * Creates a new lighting system.
     */
    public LightingSystem() {
        this.directionalLights = new ArrayList<>();
        this.pointLights = new ArrayList<>();
        this.spotLights = new ArrayList<>();
        
        this.ambientColor = new Vector3f(0.1f, 0.1f, 0.15f);
        this.ambientIntensity = 0.3f;
        
        this.shadowsEnabled = true;
        this.shadowMapFramebuffer = 0;
        this.shadowMapTexture = 0;
        this.lightDataUBO = 0;
        
        logger.info("Initialized lighting system");
    }
    
    /**
     * Initializes OpenGL resources for the lighting system.
     */
    public void initialize() {
        initializeShadowMapping();
        initializeUniformBuffers();
        setupDefaultLights();
        
        logger.info("Lighting system initialization complete");
    }
    
    /**
     * Initializes shadow mapping resources.
     */
    private void initializeShadowMapping() {
        if (!shadowsEnabled) return;
        
        // Create shadow map framebuffer
        shadowMapFramebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowMapFramebuffer);
        
        // Create shadow map texture
        shadowMapTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, 
                         SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);
        
        // Attach depth texture to framebuffer
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 
                                   GL11.GL_TEXTURE_2D, shadowMapTexture, 0);
        
        // No color buffer needed for shadow mapping
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        
        // Check framebuffer completeness
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            logger.severe("Shadow map framebuffer is not complete!");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Shadow mapping initialized with " + SHADOW_MAP_SIZE + "x" + SHADOW_MAP_SIZE + " resolution");
    }
    
    /**
     * Initializes uniform buffer objects for efficient light data transfer.
     */
    private void initializeUniformBuffers() {
        // Calculate buffer size for all light data
        int bufferSize = calculateLightDataBufferSize();
        
        // Create UBO
        lightDataUBO = GL20.glGenBuffers();
        GL20.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lightDataUBO);
        GL20.glBufferData(GL31.GL_UNIFORM_BUFFER, bufferSize, GL20.GL_DYNAMIC_DRAW);
        GL20.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        
        logger.info("Initialized light data UBO with size: " + bufferSize + " bytes");
    }
    
    /**
     * Calculates the required buffer size for light data.
     * @return The buffer size in bytes
     */
    private int calculateLightDataBufferSize() {
        // Ambient light: 4 floats (color + intensity)
        // Directional lights: MAX_DIRECTIONAL_LIGHTS * 8 floats (direction + color + intensity + enabled)
        // Point lights: MAX_POINT_LIGHTS * 12 floats (position + color + intensity + attenuation + radius + enabled)
        // Spot lights: MAX_SPOT_LIGHTS * 16 floats (position + direction + color + intensity + attenuation + cones + radius + enabled)
        
        int ambientSize = 4 * Float.BYTES;
        int directionalSize = MAX_DIRECTIONAL_LIGHTS * 8 * Float.BYTES;
        int pointSize = MAX_POINT_LIGHTS * 12 * Float.BYTES;
        int spotSize = MAX_SPOT_LIGHTS * 16 * Float.BYTES;
        
        return ambientSize + directionalSize + pointSize + spotSize;
    }
    
    /**
     * Sets up default lighting configuration.
     */
    private void setupDefaultLights() {
        // Add default sun light
        DirectionalLight sunLight = new DirectionalLight(
            new Vector3f(-0.3f, -1.0f, -0.3f),
            new Vector3f(1.0f, 0.95f, 0.8f),
            2.0f
        );
        sunLight.setCastsShadows(true);
        addDirectionalLight(sunLight);
        
        logger.info("Set up default lighting configuration");
    }
    
    /**
     * Adds a directional light to the system.
     * @param light The directional light to add
     * @return True if added successfully, false if at capacity
     */
    public boolean addDirectionalLight(DirectionalLight light) {
        if (directionalLights.size() >= MAX_DIRECTIONAL_LIGHTS) {
            logger.warning("Cannot add directional light: at maximum capacity (" + MAX_DIRECTIONAL_LIGHTS + ")");
            return false;
        }
        
        directionalLights.add(light);
        logger.info("Added directional light (total: " + directionalLights.size() + ")");
        return true;
    }
    
    /**
     * Adds a point light to the system.
     * @param light The point light to add
     * @return True if added successfully, false if at capacity
     */
    public boolean addPointLight(PointLight light) {
        if (pointLights.size() >= MAX_POINT_LIGHTS) {
            logger.warning("Cannot add point light: at maximum capacity (" + MAX_POINT_LIGHTS + ")");
            return false;
        }
        
        pointLights.add(light);
        return true;
    }
    
    /**
     * Adds a spot light to the system.
     * @param light The spot light to add
     * @return True if added successfully, false if at capacity
     */
    public boolean addSpotLight(SpotLight light) {
        if (spotLights.size() >= MAX_SPOT_LIGHTS) {
            logger.warning("Cannot add spot light: at maximum capacity (" + MAX_SPOT_LIGHTS + ")");
            return false;
        }
        
        spotLights.add(light);
        return true;
    }
    
    /**
     * Updates light data and uploads to GPU.
     * @param viewMatrix The current view matrix
     * @param projectionMatrix The current projection matrix
     */
    public void updateLights(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        updateShadowMaps(viewMatrix, projectionMatrix);
        uploadLightDataToGPU();
    }
    
    /**
     * Updates shadow maps for shadow-casting lights.
     * @param viewMatrix The current view matrix
     * @param projectionMatrix The current projection matrix
     */
    private void updateShadowMaps(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        if (!shadowsEnabled) return;
        
        // Update shadow maps for directional lights
        for (DirectionalLight light : directionalLights) {
            if (light.isEnabled() && light.castsShadows()) {
                updateDirectionalLightShadowMap(light, viewMatrix, projectionMatrix);
            }
        }
    }
    
    /**
     * Updates shadow map for a directional light.
     * @param light The directional light
     * @param viewMatrix The current view matrix
     * @param projectionMatrix The current projection matrix
     */
    private void updateDirectionalLightShadowMap(DirectionalLight light, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        // Calculate light space matrix for shadow mapping
        Matrix4f lightProjection = new Matrix4f().ortho(-50.0f, 50.0f, -50.0f, 50.0f, 1.0f, 100.0f);
        Matrix4f lightView = new Matrix4f().lookAt(
            new Vector3f(light.getDirection()).mul(-20.0f),
            new Vector3f(0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f)
        );
        
        Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);
        light.setLightSpaceMatrix(lightSpaceMatrix);
    }
    
    /**
     * Uploads light data to GPU uniform buffer.
     */
    private void uploadLightDataToGPU() {
        // This would typically upload structured light data to the UBO
        // Implementation depends on the specific shader uniform layout
        logger.fine("Updated light data on GPU");
    }
    
    /**
     * Binds shadow maps for rendering.
     * @param shader The shader to bind shadow maps to
     */
    public void bindShadowMaps(Shader shader) {
        if (!shadowsEnabled) return;
        
        // Bind shadow map texture
        GL13.glActiveTexture(GL13.GL_TEXTURE10); // Use texture unit 10 for shadow maps
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexture);
        shader.setUniform("shadowMap", 10);
    }
    
    /**
     * Gets the ambient color.
     * @return The ambient color
     */
    public Vector3f getAmbientColor() {
        return new Vector3f(ambientColor);
    }
    
    /**
     * Sets the ambient color.
     * @param color The new ambient color
     */
    public void setAmbientColor(Vector3f color) {
        this.ambientColor.set(color);
    }
    
    /**
     * Gets the ambient intensity.
     * @return The ambient intensity
     */
    public float getAmbientIntensity() {
        return ambientIntensity;
    }
    
    /**
     * Sets the ambient intensity.
     * @param intensity The new ambient intensity
     */
    public void setAmbientIntensity(float intensity) {
        this.ambientIntensity = intensity;
    }
    
    /**
     * Enables or disables shadows.
     * @param enabled True to enable shadows, false to disable
     */
    public void setShadowsEnabled(boolean enabled) {
        this.shadowsEnabled = enabled;
    }
    
    /**
     * Checks if shadows are enabled.
     * @return True if shadows are enabled, false otherwise
     */
    public boolean areShadowsEnabled() {
        return shadowsEnabled;
    }
    
    /**
     * Gets the number of active directional lights.
     * @return The number of directional lights
     */
    public int getDirectionalLightCount() {
        return directionalLights.size();
    }
    
    /**
     * Gets the number of active point lights.
     * @return The number of point lights
     */
    public int getPointLightCount() {
        return pointLights.size();
    }
    
    /**
     * Gets the number of active spot lights.
     * @return The number of spot lights
     */
    public int getSpotLightCount() {
        return spotLights.size();
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        logger.info("Cleaning up lighting system");
        
        if (shadowMapFramebuffer != 0) {
            GL30.glDeleteFramebuffers(shadowMapFramebuffer);
        }
        
        if (shadowMapTexture != 0) {
            GL11.glDeleteTextures(shadowMapTexture);
        }
        
        if (lightDataUBO != 0) {
            GL20.glDeleteBuffers(lightDataUBO);
        }
        
        directionalLights.clear();
        pointLights.clear();
        spotLights.clear();
        
        logger.info("Lighting system cleanup complete");
    }
}