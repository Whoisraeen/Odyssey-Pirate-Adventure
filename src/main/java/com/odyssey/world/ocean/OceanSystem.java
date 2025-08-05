package com.odyssey.world.ocean;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
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
    
    public OceanSystem(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing ocean system...");
        
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
        // Render ocean surface
        renderOceanSurface(renderer);
        
        // Render underwater effects if player is submerged
        // This will be implemented when we have proper underwater rendering
        
        // Render marine life
        marineEcosystem.render(renderer);
    }
    
    private void renderOceanSurface(Renderer renderer) {
        // For now, render a simple water plane
        // This will be replaced with proper water shaders and wave geometry
        
        float renderDistance = config.getRenderDistance() * 16; // Convert chunks to blocks
        
        for (float x = -renderDistance; x < renderDistance; x += 16) {
            for (float z = -renderDistance; z < renderDistance; z += 16) {
                float waterHeight = getWaterHeightAt(x, z);
                renderer.renderWater(x, waterHeight, z, 16, 16);
            }
        }
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
    
    public void cleanup() {
        logger.info("Cleaning up ocean system");
        
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
    
    public TidalSystem getTidalSystem() { return tidalSystem; }
    public WaveSystem getWaveSystem() { return waveSystem; }
    public CurrentSystem getCurrentSystem() { return currentSystem; }
    public MarineEcosystem getMarineEcosystem() { return marineEcosystem; }
}