package com.odyssey.world.ocean;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.GerstnerOceanRenderer;
import com.odyssey.graphics.Renderer;

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
    
    // Water rendering - unified system
    private GerstnerOceanRenderer oceanRenderer;  // Consolidated Gerstner wave renderer
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
        logger.info("Initializing unified ocean system...");
        
        // Initialize the consolidated ocean renderer
        oceanRenderer = new GerstnerOceanRenderer(shaderManager);
        
        // Set quality based on config
        GerstnerOceanRenderer.WaterQuality quality = determineWaterQuality();
        oceanRenderer.setQuality(quality);
        logger.info("Initialized unified ocean renderer with quality: {}", quality);
        
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
        
        // Update ocean renderer with wind data
        if (oceanRenderer != null) {
            oceanRenderer.updateWaves(windDirection, windStrength);
        }
        
        // Update current system
        currentSystem.update(deltaTime, windDirection, windStrength);
        
        // Update marine ecosystem
        marineEcosystem.update(deltaTime);
    }
    

    
    public void render(Renderer renderer) {
        // Render ocean using the unified renderer
        if (oceanRenderer != null) {
            Vector3f sunDirection = renderer.getLightDirection();
            oceanRenderer.render(renderer.getCamera(), sunDirection, (float)worldTime);
        }
        
        // Render underwater effects if player is submerged
        // This will be implemented when we have proper underwater rendering
        
        // Render marine life
        marineEcosystem.render(renderer);
    }
    

    
    /**
     * Get the water height at a specific world position
     */
    public float getWaterHeightAt(float x, float z) {
        // Use unified ocean renderer for accurate wave heights
        if (oceanRenderer != null) {
            return oceanRenderer.getWaveHeightAt(x, z, (float)worldTime);
        }
        
        // Fallback to base water level with wave system
        float height = currentWaterLevel;
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
     * Update environmental data for the ocean system
     */
    public void updateEnvironmentalData(float weatherIntensity, Vector3f biomeWaterColor, float underwaterDepth) {
        // Update wind strength based on weather intensity
        this.windStrength = Math.max(0.1f, weatherIntensity * 2.0f);
        
        // Update ocean renderer with environmental data if available
        if (oceanRenderer != null) {
            // Pass environmental data to the ocean renderer
            // This could include water color, transparency, etc.
            // For now, just log the update
            logger.debug("Updated ocean environmental data - weather: {}, depth: {}", 
                weatherIntensity, underwaterDepth);
        }
        
        // Update marine ecosystem with environmental changes
        if (marineEcosystem != null) {
            // Environmental changes could affect marine life behavior
            // This is a placeholder for future implementation
        }
    }
    
    /**
     * Determines the appropriate water quality based on system capabilities and config.
     */
    private GerstnerOceanRenderer.WaterQuality determineWaterQuality() {
        // Check ocean simulation quality from config
        float oceanQuality = config.getOceanSimulationQuality();
        
        if (oceanQuality >= 0.8f) {
            return GerstnerOceanRenderer.WaterQuality.ULTRA;
        } else if (oceanQuality >= 0.6f) {
            return GerstnerOceanRenderer.WaterQuality.QUALITY;
        } else if (oceanQuality >= 0.4f) {
            return GerstnerOceanRenderer.WaterQuality.BALANCED;
        } else {
            return GerstnerOceanRenderer.WaterQuality.PERFORMANCE;
        }
    }
    
    /**
     * Auto-detects appropriate water quality based on system performance.
     */
    private GerstnerOceanRenderer.WaterQuality autoDetectWaterQuality() {
        // Simple heuristic based on available memory and config settings
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        if (maxMemory > 4L * 1024 * 1024 * 1024) { // > 4GB
            return GerstnerOceanRenderer.WaterQuality.ULTRA;
        } else if (maxMemory > 2L * 1024 * 1024 * 1024) { // > 2GB
            return GerstnerOceanRenderer.WaterQuality.QUALITY;
        } else if (maxMemory > 1L * 1024 * 1024 * 1024) { // > 1GB
            return GerstnerOceanRenderer.WaterQuality.BALANCED;
        } else {
            return GerstnerOceanRenderer.WaterQuality.PERFORMANCE;
        }
    }
    
    public void cleanup() {
        logger.info("Cleaning up unified ocean system");
        
        if (oceanRenderer != null) oceanRenderer.cleanup();
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
    
    public GerstnerOceanRenderer getOceanRenderer() { return oceanRenderer; }
    public TidalSystem getTidalSystem() { return tidalSystem; }
    public WaveSystem getWaveSystem() { return waveSystem; }
    public CurrentSystem getCurrentSystem() { return currentSystem; }
    public MarineEcosystem getMarineEcosystem() { return marineEcosystem; }
}