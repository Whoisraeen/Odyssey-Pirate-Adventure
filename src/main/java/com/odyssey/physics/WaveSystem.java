package com.odyssey.physics;

import com.odyssey.util.Logger;
import com.odyssey.util.NoiseGenerator;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced wave system for realistic ocean wave simulation.
 * Handles wave generation, propagation, interference, and interaction with objects.
 */
public class WaveSystem {
    
    // Wave parameters
    private final List<Wave> waves;
    private final NoiseGenerator noiseGenerator;
    private final Logger logger;
    
    // Environmental parameters
    private Vector2f windDirection;
    private float windSpeed;
    private float waveAmplitude;
    private float waveFrequency; // Reserved for future wave frequency calculations
    private float waveSpeed; // Reserved for future wave speed calculations
    
    // Simulation parameters
    private float time;
    private static final int MAX_WAVES = 8;
    private static final float WAVE_DECAY = 0.95f;
    private static final float MIN_WAVE_HEIGHT = 0.1f;
    
    public WaveSystem() {
        this.waves = new ArrayList<>();
        this.noiseGenerator = new NoiseGenerator();
        this.logger = Logger.getLogger(WaveSystem.class);
        
        // Default parameters
        this.windDirection = new Vector2f(1.0f, 0.0f);
        this.windSpeed = 5.0f;
        this.waveAmplitude = 2.0f;
        this.waveFrequency = 0.1f;
        this.waveSpeed = 8.0f;
        this.time = 0.0f;
    }
    
    /**
     * Initialize the wave system
     */
    public void initialize() {
        initializeWaves();
        logger.info("WaveSystem initialized with {} wave components", waves.size());
    }
    
    /**
     * Clean up wave system resources
     */
    public void cleanup() {
        waves.clear();
        logger.info("Wave system cleaned up");
    }
    
    /**
     * Updates the wave system
     */
    public void update(double deltaTime) {
        float dt = (float) deltaTime;
        time += dt;
        
        // Update existing waves
        for (Wave wave : waves) {
            wave.update(dt);
        }
        
        // Remove decayed waves
        waves.removeIf(wave -> wave.amplitude < MIN_WAVE_HEIGHT);
        
        // Occasionally add new waves based on wind conditions
        if (ThreadLocalRandom.current().nextFloat() < 0.1f * dt && waves.size() < MAX_WAVES) {
            generateRandomWave();
        }
    }
    
    /**
     * Updates the wave system (legacy method)
     */
    public void update(float deltaTime) {
        update((double) deltaTime);
    }
    
    /**
     * Calculates wave height at a specific position
     */
    public float getWaveHeight(float x, float z) {
        float totalHeight = 0.0f;
        
        // Sum all wave contributions
        for (Wave wave : waves) {
            totalHeight += wave.getHeightAt(x, z, time);
        }
        
        // Add noise for small-scale detail
        float noise = noiseGenerator.noise(x * 0.1f, z * 0.1f, time * 0.5f) * 0.2f;
        totalHeight += noise;
        
        return totalHeight;
    }
    
    /**
     * Calculates wave normal at a specific position
     */
    public Vector3f getWaveNormal(float x, float z) {
        float epsilon = 0.1f;
        
        // Calculate height at nearby points for gradient
        float heightCenter = getWaveHeight(x, z);
        float heightX = getWaveHeight(x + epsilon, z);
        float heightZ = getWaveHeight(x, z + epsilon);
        
        // Calculate gradient
        float dx = (heightX - heightCenter) / epsilon;
        float dz = (heightZ - heightCenter) / epsilon;
        
        // Normal vector
        Vector3f normal = new Vector3f(-dx, 1.0f, -dz);
        return normal.normalize();
    }
    
    /**
     * Calculates wave velocity at a specific position
     */
    public Vector3f getWaveVelocity(float x, float z) {
        Vector3f totalVelocity = new Vector3f();
        
        for (Wave wave : waves) {
            Vector3f waveVel = wave.getVelocityAt(x, z, time);
            totalVelocity.add(waveVel);
        }
        
        return totalVelocity;
    }
    
    /**
     * Creates a wave disturbance at a specific location
     */
    public void createDisturbance(float x, float z, float intensity, float radius) {
        // Create circular wave
        Wave disturbanceWave = new Wave();
        disturbanceWave.origin = new Vector2f(x, z);
        disturbanceWave.amplitude = intensity;
        disturbanceWave.wavelength = radius * 2.0f;
        disturbanceWave.frequency = 2.0f * (float) Math.PI / disturbanceWave.wavelength;
        disturbanceWave.speed = (float) Math.sqrt(9.81f * disturbanceWave.wavelength / (2.0f * Math.PI));
        disturbanceWave.direction = new Vector2f(0, 0); // Circular wave
        disturbanceWave.phase = 0.0f;
        disturbanceWave.waveType = WaveType.CIRCULAR;
        disturbanceWave.creationTime = time;
        
        if (waves.size() < MAX_WAVES) {
            waves.add(disturbanceWave);
            logger.info("Created wave disturbance at ({}, {}) with intensity {}", x, z, intensity);
        }
    }
    
    /**
     * Sets wind parameters
     */
    public void setWind(Vector2f direction, float speed) {
        this.windDirection = new Vector2f(direction).normalize();
        this.windSpeed = speed;
        
        // Update wave parameters based on wind
        updateWaveParameters();
    }
    
    /**
     * Gets current wind direction
     */
    public Vector2f getWindDirection() {
        return new Vector2f(windDirection);
    }
    
    /**
     * Gets the current wave frequency parameter
     */
    public float getWaveFrequency() {
        return waveFrequency;
    }
    
    /**
     * Gets the current wave speed parameter
     */
    public float getWaveSpeed() {
        return waveSpeed;
    }
    
    /**
     * Calculates wave force on an object
     */
    public Vector3f calculateWaveForce(Vector3f position, float objectRadius, float mass) {
        Vector3f force = new Vector3f();
        
        // Get wave properties at object position
        float waveHeight = getWaveHeight(position.x, position.z);
        Vector3f waveVelocity = getWaveVelocity(position.x, position.z);
        Vector3f waveNormal = getWaveNormal(position.x, position.z);
        
        // Calculate submersion depth
        float submersionDepth = Math.max(0, waveHeight - position.y);
        float submersionRatio = Math.min(1.0f, submersionDepth / (objectRadius * 2.0f));
        
        if (submersionRatio > 0) {
            // Buoyancy force
            float buoyancyMagnitude = 1000.0f * 9.81f * submersionRatio * objectRadius * objectRadius * objectRadius * 4.0f / 3.0f * (float) Math.PI;
            Vector3f buoyancyForce = new Vector3f(0, buoyancyMagnitude, 0);
            force.add(buoyancyForce);
            
            // Wave drag force
            float dragCoefficient = 0.5f;
            float dragMagnitude = 0.5f * 1000.0f * waveVelocity.lengthSquared() * dragCoefficient * objectRadius * objectRadius * (float) Math.PI;
            Vector3f dragForce = new Vector3f(waveVelocity).normalize().mul(dragMagnitude * submersionRatio);
            force.add(dragForce);
            
            // Wave impact force (based on wave steepness)
            Vector3f impactForce = new Vector3f(waveNormal).mul(waveVelocity.length() * mass * 0.1f);
            force.add(impactForce);
        }
        
        return force;
    }
    
    /**
     * Initializes default wave set
     */
    private void initializeWaves() {
        // Create primary wave trains
        for (int i = 0; i < 4; i++) {
            Wave wave = new Wave();
            
            // Vary wave properties
            float angleOffset = (float) (i * Math.PI / 2.0 + ThreadLocalRandom.current().nextGaussian() * 0.2);
            wave.direction = new Vector2f(
                (float) Math.cos(angleOffset),
                (float) Math.sin(angleOffset)
            );
            
            wave.amplitude = waveAmplitude * (0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
            wave.wavelength = 20.0f + ThreadLocalRandom.current().nextFloat() * 30.0f;
            wave.frequency = 2.0f * (float) Math.PI / wave.wavelength;
            wave.speed = (float) Math.sqrt(9.81f * wave.wavelength / (2.0f * Math.PI));
            wave.phase = ThreadLocalRandom.current().nextFloat() * 2.0f * (float) Math.PI;
            wave.waveType = WaveType.DIRECTIONAL;
            wave.creationTime = 0.0f;
            
            waves.add(wave);
        }
    }
    
    /**
     * Generates a random wave based on current conditions
     */
    private void generateRandomWave() {
        Wave wave = new Wave();
        
        // Base direction on wind with some randomness
        float windAngle = (float) Math.atan2(windDirection.y, windDirection.x);
        float angleVariation = (ThreadLocalRandom.current().nextFloat() - 0.5f) * (float) Math.PI / 3.0f;
        float waveAngle = windAngle + angleVariation;
        
        wave.direction = new Vector2f(
            (float) Math.cos(waveAngle),
            (float) Math.sin(waveAngle)
        );
        
        // Scale properties with wind speed
        float windFactor = windSpeed / 10.0f;
        wave.amplitude = waveAmplitude * windFactor * (0.3f + ThreadLocalRandom.current().nextFloat() * 0.7f);
        wave.wavelength = 10.0f + ThreadLocalRandom.current().nextFloat() * 40.0f * windFactor;
        wave.frequency = 2.0f * (float) Math.PI / wave.wavelength;
        wave.speed = (float) Math.sqrt(9.81f * wave.wavelength / (2.0f * Math.PI));
        wave.phase = ThreadLocalRandom.current().nextFloat() * 2.0f * (float) Math.PI;
        wave.waveType = WaveType.DIRECTIONAL;
        wave.creationTime = time;
        
        waves.add(wave);
    }
    
    /**
     * Updates wave parameters based on environmental conditions
     */
    private void updateWaveParameters() {
        float windFactor = windSpeed / 10.0f;
        waveAmplitude = 1.0f + windFactor * 2.0f;
        waveFrequency = 0.05f + windFactor * 0.1f;
        waveSpeed = 5.0f + windFactor * 5.0f;
    }
    
    /**
     * Wave types
     */
    private enum WaveType {
        DIRECTIONAL,  // Travels in a specific direction
        CIRCULAR,     // Radiates from a point
        STANDING      // Stationary wave pattern
    }
    
    /**
     * Individual wave representation
     */
    private static class Wave {
        Vector2f direction;      // Wave direction (for directional waves)
        Vector2f origin;         // Wave origin (for circular waves)
        float amplitude;         // Wave height
        float wavelength;        // Distance between wave crests
        float frequency;         // Wave frequency
        float speed;             // Wave propagation speed
        float phase;             // Phase offset
        WaveType waveType;       // Type of wave
        float creationTime;      // When the wave was created
        
        Wave() {
            this.direction = new Vector2f();
            this.origin = new Vector2f();
        }
        
        /**
         * Updates wave properties over time
         */
        void update(float deltaTime) {
            // Apply wave decay over time
            amplitude *= WAVE_DECAY;
        }
        
        /**
         * Calculates wave height at a position
         */
        float getHeightAt(float x, float z, float currentTime) {
            float waveValue = 0.0f;
            
            switch (waveType) {
                case DIRECTIONAL:
                    // Plane wave traveling in direction
                    float dotProduct = x * direction.x + z * direction.y;
                    waveValue = amplitude * (float) Math.sin(frequency * dotProduct - speed * frequency * currentTime + phase);
                    break;
                    
                case CIRCULAR:
                    // Circular wave radiating from origin
                    float distance = (float) Math.sqrt((x - origin.x) * (x - origin.x) + (z - origin.y) * (z - origin.y));
                    float waveAge = currentTime - creationTime;
                    float waveRadius = speed * waveAge;
                    
                    if (distance > 0 && Math.abs(distance - waveRadius) < wavelength) {
                        // Wave amplitude decreases with distance
                        float distanceAmplitude = amplitude / (1.0f + distance * 0.1f);
                        waveValue = distanceAmplitude * (float) Math.sin(frequency * distance - speed * frequency * currentTime + phase);
                    }
                    break;
                    
                case STANDING:
                    // Standing wave pattern
                    waveValue = amplitude * (float) Math.sin(frequency * x) * (float) Math.sin(frequency * z) * (float) Math.cos(speed * frequency * currentTime + phase);
                    break;
            }
            
            return waveValue;
        }
        
        /**
         * Calculates wave velocity at a position
         */
        Vector3f getVelocityAt(float x, float z, float currentTime) {
            Vector3f velocity = new Vector3f();
            
            switch (waveType) {
                case DIRECTIONAL:
                    // Orbital motion for deep water waves
                    float dotProduct = x * direction.x + z * direction.y;
                    float wavePhase = frequency * dotProduct - speed * frequency * currentTime + phase;
                    
                    float horizontalVel = amplitude * frequency * speed * (float) Math.cos(wavePhase);
                    float verticalVel = amplitude * frequency * speed * (float) Math.sin(wavePhase);
                    
                    velocity.x = horizontalVel * direction.x;
                    velocity.y = verticalVel;
                    velocity.z = horizontalVel * direction.y;
                    break;
                    
                case CIRCULAR:
                    // Radial velocity for circular waves
                    float distance = (float) Math.sqrt((x - origin.x) * (x - origin.x) + (z - origin.y) * (z - origin.y));
                    if (distance > 0.001f) {
                        float radialVel = amplitude * frequency * speed * (float) Math.cos(frequency * distance - speed * frequency * currentTime + phase);
                        velocity.x = radialVel * (x - origin.x) / distance;
                        velocity.z = radialVel * (z - origin.y) / distance;
                    }
                    break;
                    
                case STANDING:
                    // No horizontal movement for standing waves
                    velocity.y = -amplitude * speed * frequency * (float) Math.sin(frequency * x) * (float) Math.sin(frequency * z) * (float) Math.sin(speed * frequency * currentTime + phase);
                    break;
            }
            
            return velocity;
        }
    }
}