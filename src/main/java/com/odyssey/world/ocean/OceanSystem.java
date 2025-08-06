package com.odyssey.world.ocean;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import com.odyssey.graphics.WaterShader;
import com.odyssey.graphics.ShaderManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced ocean simulation system for The Odyssey.
 * Implements dynamic tidal mechanics, wave physics, and marine ecosystem simulation.
 */
public class OceanSystem {
    private static final Logger logger = LoggerFactory.getLogger(OceanSystem.class);
    
    private final GameConfig config;
    
    // Water rendering
    private WaterShader waterShader;
    private ShaderManager shaderManager;
    
    // Tidal system
    private TidalSystem tidalSystem;
    
    // Wave system
    private WaveSystem waveSystem;
    
    // Current system
    private CurrentSystem currentSystem;
    
    // Marine ecosystem
    private MarineEcosystem marineEcosystem;
    
    // Ocean properties
    private float baseWaterLevel = 64.0f; // Sea level
    private float currentWaterLevel = 64.0f;
    private float waveHeight = 1.0f;
    private Vector3f windDirection = new Vector3f(1, 0, 0);
    private float windStrength = 5.0f;
    
    // Time tracking
    private double worldTime = 0.0;
    
    public OceanSystem(GameConfig config, ShaderManager shaderManager) {
        this.config = config;
        this.shaderManager = shaderManager;
    }
    
    public void initialize() {
        logger.info("Initializing ocean system...");
        
        // Initialize water shader system
        WaterShader.WaterQuality waterQuality = determineWaterQuality();
        waterShader = new WaterShader(shaderManager, waterQuality);
        logger.info("Initialized water shader with quality: " + waterQuality);
        
        // Initialize tidal system
        if (config.isEnableTidalSystem()) {
            tidalSystem = new TidalSystem();
            tidalSystem.initialize();
        }
        
        // Initialize wave system
        if (config.isEnableDynamicWaves()) {
            waveSystem = new WaveSystem(config);
            waveSystem.initialize();
        }
        
        // Initialize current system
        currentSystem = new CurrentSystem();
        currentSystem.initialize();
        
        // Initialize marine ecosystem
        marineEcosystem = new MarineEcosystem();
        marineEcosystem.initialize();
        
        logger.info("Ocean system initialized successfully");
    }
    
    public void update(double deltaTime) {
        worldTime += deltaTime;
        
        // Update tidal system
        if (tidalSystem != null) {
            tidalSystem.update(deltaTime);
            currentWaterLevel = baseWaterLevel + tidalSystem.getTidalOffset();
        }
        
        // Update wave system
        if (waveSystem != null) {
            waveSystem.update(deltaTime, windDirection, windStrength);
        }
        
        // Update current system
        currentSystem.update(deltaTime, windDirection, windStrength);
        
        // Update marine ecosystem
        marineEcosystem.update(deltaTime);
    }
    
    public void render(Renderer renderer) {
        // Render ocean surface with advanced water shader
        renderOceanSurface(renderer);
        
        // Render underwater effects if player is submerged
        // This will be implemented when we have proper underwater rendering
        
        // Render marine life
        marineEcosystem.render(renderer);
    }
    
    /**
     * Renders the ocean surface using advanced water shaders with reflections and refractions.
     */
    private void renderOceanSurface(Renderer renderer) {
        if (waterShader == null) {
            logger.warn("Water shader not initialized, skipping ocean rendering");
            return;
        }
        
        // Get camera and lighting information from renderer
        Matrix4f projectionMatrix = renderer.getCamera().getProjectionMatrix();
        Matrix4f viewMatrix = renderer.getCamera().getViewMatrix();
        Matrix4f modelMatrix = new Matrix4f().identity();
        Vector3f cameraPosition = renderer.getCamera().getPosition();
        Vector3f lightDirection = renderer.getLightDirection();
        Vector3f lightColor = renderer.getLightColor();
        Vector3f ambientColor = renderer.getAmbientColor();
        
        // Render reflections if enabled
        if (waterShader.getQuality().isReflectionsEnabled()) {
            renderReflections(renderer, cameraPosition);
        }
        
        // Render refractions if enabled
        if (waterShader.getQuality().isRefractionEnabled()) {
            renderRefractions(renderer, cameraPosition);
        }
        
        // Restore main framebuffer
        waterShader.unbindFramebuffer();
        renderer.restoreViewport();
        
        // Render the water surface
        waterShader.render(
            projectionMatrix, viewMatrix, modelMatrix,
            cameraPosition, lightDirection, lightColor, ambientColor,
            (float)worldTime
        );
        
        // Render water geometry
        renderWaterGeometry(renderer);
    }
    
    /**
     * Renders reflections to the reflection framebuffer.
     */
    private void renderReflections(Renderer renderer, Vector3f cameraPosition) {
        waterShader.bindReflectionFramebuffer();
        
        // Create reflection camera (flip Y position and invert pitch)
        Vector3f reflectionCameraPos = new Vector3f(
            cameraPosition.x,
            2 * currentWaterLevel - cameraPosition.y,
            cameraPosition.z
        );
        
        // Set up clipping plane to prevent rendering below water
        Vector3f clippingPlane = new Vector3f(0, 1, 0); // Normal pointing up
        float clippingDistance = currentWaterLevel;
        
        // Render scene from reflection camera perspective
        renderer.renderSceneForReflection(reflectionCameraPos, clippingPlane, clippingDistance);
    }
    
    /**
     * Renders refractions to the refraction framebuffer.
     */
    private void renderRefractions(Renderer renderer, Vector3f cameraPosition) {
        waterShader.bindRefractionFramebuffer();
        
        // Set up clipping plane to only render below water
        Vector3f clippingPlane = new Vector3f(0, -1, 0); // Normal pointing down
        float clippingDistance = -currentWaterLevel;
        
        // Render underwater scene
        renderer.renderSceneForRefraction(cameraPosition, clippingPlane, clippingDistance);
    }
    
    /**
     * Renders the actual water geometry (plane or mesh).
     */
    private void renderWaterGeometry(Renderer renderer) {
        float renderDistance = config.getRenderDistance() * 16; // Convert chunks to blocks
        
        // Render water as a large plane for now
        // In a more advanced implementation, this could be a tessellated mesh
        // that follows the camera and provides higher detail near the viewer
        renderer.renderWaterPlane(
            -renderDistance, currentWaterLevel, -renderDistance,
            renderDistance * 2, renderDistance * 2
        );
    }
    
    /**
     * Get the water height at a specific world position
     */
    public float getWaterHeightAt(float x, float z) {
        float height = currentWaterLevel;
        
        // Add wave displacement if wave system is enabled
        if (waveSystem != null) {
            height += waveSystem.getWaveHeightAt(x, z, worldTime);
        }
        
        return height;
    }
    
    /**
     * Get the water velocity at a specific position (for ship physics)
     */
    public Vector3f getWaterVelocityAt(float x, float z) {
        Vector3f velocity = new Vector3f();
        
        // Add current velocity
        if (currentSystem != null) {
            velocity.add(currentSystem.getCurrentAt(x, z));
        }
        
        // Add wave velocity if wave system is enabled
        if (waveSystem != null) {
            velocity.add(waveSystem.getWaveVelocityAt(x, z, worldTime));
        }
        
        return velocity;
    }
    
    /**
     * Check if a position is underwater
     */
    public boolean isUnderwater(Vector3f position) {
        return position.y < getWaterHeightAt(position.x, position.z);
    }
    
    /**
     * Get the depth at a specific position (negative if above water)
     */
    public float getDepthAt(Vector3f position) {
        return getWaterHeightAt(position.x, position.z) - position.y;
    }
    
    /**
     * Apply buoyancy force to an object
     */
    public Vector3f calculateBuoyancy(Vector3f position, float volume, float density) {
        float depth = getDepthAt(position);
        
        if (depth <= 0) {
            return new Vector3f(0, 0, 0); // Above water, no buoyancy
        }
        
        // Calculate submerged volume (simplified)
        float submergedVolume = Math.min(volume, depth * volume);
        
        // Buoyancy force = displaced water weight
        float waterDensity = 1000.0f; // kg/m³
        float buoyancyForce = submergedVolume * waterDensity * 9.81f; // F = ρVg
        
        return new Vector3f(0, buoyancyForce, 0);
    }
    
    /**
     * Determines the appropriate water quality based on system capabilities and config.
     */
    private WaterShader.WaterQuality determineWaterQuality() {
        // Check ocean simulation quality from config
        float oceanQuality = config.getOceanSimulationQuality();
        
        if (oceanQuality >= 0.8f) {
            return WaterShader.WaterQuality.ULTRA;
        } else if (oceanQuality >= 0.6f) {
            return WaterShader.WaterQuality.HIGH;
        } else if (oceanQuality >= 0.4f) {
            return WaterShader.WaterQuality.MEDIUM;
        } else {
            return WaterShader.WaterQuality.LOW;
        }
    }
    
    /**
     * Auto-detects appropriate water quality based on system performance.
     */
    private WaterShader.WaterQuality autoDetectWaterQuality() {
        // Simple heuristic based on available memory and config settings
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        if (maxMemory > 4L * 1024 * 1024 * 1024) { // > 4GB
            return WaterShader.WaterQuality.ULTRA;
        } else if (maxMemory > 2L * 1024 * 1024 * 1024) { // > 2GB
            return WaterShader.WaterQuality.HIGH;
        } else if (maxMemory > 1L * 1024 * 1024 * 1024) { // > 1GB
            return WaterShader.WaterQuality.MEDIUM;
        } else {
            return WaterShader.WaterQuality.LOW;
        }
    }
    
    public void cleanup() {
        logger.info("Cleaning up ocean system");
        
        if (waterShader != null) waterShader.cleanup();
        if (marineEcosystem != null) marineEcosystem.cleanup();
        if (currentSystem != null) currentSystem.cleanup();
        if (waveSystem != null) waveSystem.cleanup();
        if (tidalSystem != null) tidalSystem.cleanup();
    }
    
    // Getters and setters
    public float getBaseWaterLevel() { return baseWaterLevel; }
    public void setBaseWaterLevel(float level) { this.baseWaterLevel = level; }
    
    public float getCurrentWaterLevel() { return currentWaterLevel; }
    
    public Vector3f getWindDirection() { return new Vector3f(windDirection); }
    public void setWindDirection(Vector3f direction) { this.windDirection.set(direction.normalize()); }
    
    public float getWindStrength() { return windStrength; }
    public void setWindStrength(float strength) { this.windStrength = Math.max(0, strength); }
    
    public double getWorldTime() { return worldTime; }
    
    public WaterShader getWaterShader() { return waterShader; }
    public TidalSystem getTidalSystem() { return tidalSystem; }
    public WaveSystem getWaveSystem() { return waveSystem; }
    public CurrentSystem getCurrentSystem() { return currentSystem; }
    public MarineEcosystem getMarineEcosystem() { return marineEcosystem; }
}