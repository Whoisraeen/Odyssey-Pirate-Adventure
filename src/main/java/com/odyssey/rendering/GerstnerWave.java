package com.odyssey.rendering;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Gerstner wave implementation for realistic ocean wave simulation.
 * Provides proper wave displacement, normal calculation, and foam generation.
 */
public class GerstnerWave {
    
    // Wave parameters
    public float amplitude;      // Wave height
    public float frequency;      // Wave frequency (2π/wavelength)
    public float phase;          // Phase offset
    public Vector2f direction;   // Wave direction (normalized)
    public float steepness;      // Wave steepness (0-1, controls sharpness)
    public float speed;          // Wave speed
    
    // Derived parameters
    private float wavelength;
    private float k;             // Wave number (2π/wavelength)
    private float w;             // Angular frequency
    
    /**
     * Create a Gerstner wave with basic parameters.
     */
    public GerstnerWave(float amplitude, float frequency, float phase, Vector2f direction) {
        this(amplitude, frequency, phase, direction, 0.5f, calculateWaveSpeed(frequency));
    }
    
    /**
     * Create a Gerstner wave with full parameters.
     */
    public GerstnerWave(float amplitude, float frequency, float phase, Vector2f direction, float steepness, float speed) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.phase = phase;
        this.direction = new Vector2f(direction).normalize();
        this.steepness = Math.max(0.0f, Math.min(1.0f, steepness));
        this.speed = speed;
        
        updateDerivedParameters();
    }
    
    /**
     * Update derived wave parameters.
     */
    private void updateDerivedParameters() {
        this.wavelength = (2.0f * (float)Math.PI) / frequency;
        this.k = frequency; // Wave number
        this.w = speed * k; // Angular frequency
    }
    
    /**
     * Calculate realistic wave speed based on frequency (deep water approximation).
     */
    private static float calculateWaveSpeed(float frequency) {
        float g = 9.81f; // Gravity
        return (float)Math.sqrt(g / frequency);
    }
    
    /**
     * Calculate wave displacement at given position and time.
     * 
     * @param x World X coordinate
     * @param z World Z coordinate  
     * @param time Current time
     * @return Wave displacement (x, y, z)
     */
    public Vector3f calculateDisplacement(float x, float z, float time) {
        float dot = direction.x * x + direction.y * z;
        float theta = k * dot - w * time + phase;
        float cosTheta = (float)Math.cos(theta);
        float sinTheta = (float)Math.sin(theta);
        
        // Gerstner wave displacement
        float Q = steepness / (k * amplitude);
        
        float dx = Q * amplitude * direction.x * cosTheta;
        float dy = amplitude * sinTheta;
        float dz = Q * amplitude * direction.y * cosTheta;
        
        return new Vector3f(dx, dy, dz);
    }
    
    /**
     * Calculate wave normal at given position and time.
     * 
     * @param x World X coordinate
     * @param z World Z coordinate
     * @param time Current time
     * @return Wave normal vector
     */
    public Vector3f calculateNormal(float x, float z, float time) {
        float dot = direction.x * x + direction.y * z;
        float theta = k * dot - w * time + phase;
        float cosTheta = (float)Math.cos(theta);
        float sinTheta = (float)Math.sin(theta);
        
        float Q = steepness / (k * amplitude);
        float WA = w * amplitude;
        
        // Partial derivatives for normal calculation
        float dDx_dx = 1.0f - Q * direction.x * direction.x * WA * sinTheta;
        float dDx_dz = -Q * direction.x * direction.y * WA * sinTheta;
        float dDy_dx = direction.x * WA * cosTheta;
        float dDy_dz = direction.y * WA * cosTheta;
        float dDz_dx = -Q * direction.y * direction.x * WA * sinTheta;
        float dDz_dz = 1.0f - Q * direction.y * direction.y * WA * sinTheta;
        
        // Calculate normal using cross product of tangent vectors
        Vector3f tangentX = new Vector3f(dDx_dx, dDy_dx, dDz_dx);
        Vector3f tangentZ = new Vector3f(dDx_dz, dDy_dz, dDz_dz);
        
        Vector3f normal = new Vector3f();
        tangentX.cross(tangentZ, normal);
        return normal.normalize();
    }
    
    /**
     * Calculate wave foam factor at given position and time.
     * 
     * @param x World X coordinate
     * @param z World Z coordinate
     * @param time Current time
     * @return Foam intensity (0-1)
     */
    public float calculateFoam(float x, float z, float time) {
        float dot = direction.x * x + direction.y * z;
        float theta = k * dot - w * time + phase;
        float cosTheta = (float)Math.cos(theta);
        
        // Foam appears at wave crests and is influenced by steepness
        float crestFactor = (cosTheta + 1.0f) * 0.5f; // 0-1 range
        float steepnessFactor = steepness * steepness; // Quadratic falloff
        
        return crestFactor * steepnessFactor * amplitude;
    }
    
    /**
     * Get the wavelength of this wave.
     */
    public float getWavelength() {
        return wavelength;
    }
    
    /**
     * Get the wave number (k = 2π/wavelength).
     */
    public float getWaveNumber() {
        return k;
    }
    
    /**
     * Get the angular frequency (ω = speed * k).
     */
    public float getAngularFrequency() {
        return w;
    }
    
    /**
     * Update wave parameters and recalculate derived values.
     */
    public void updateParameters(float amplitude, float frequency, float phase, Vector2f direction, float steepness, float speed) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.phase = phase;
        this.direction.set(direction).normalize();
        this.steepness = Math.max(0.0f, Math.min(1.0f, steepness));
        this.speed = speed;
        
        updateDerivedParameters();
    }
    
    // Getter methods for wave parameters
    public float getAmplitude() {
        return amplitude;
    }
    
    public float getFrequency() {
        return frequency;
    }
    
    public float getPhase() {
        return phase;
    }
    
    public Vector2f getDirection() {
        return new Vector2f(direction);
    }
    
    public float getSteepness() {
        return steepness;
    }
    
    public float getSpeed() {
        return speed;
    }
}
