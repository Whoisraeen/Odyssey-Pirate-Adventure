package com.odyssey.util;

import java.util.Random;

/**
 * Simple noise generator utility for The Odyssey.
 * Provides basic noise functions for various game systems.
 */
public class NoiseGenerator {
    
    private final Random random;
    private final long seed;
    
    /**
     * Create a noise generator with a random seed.
     */
    public NoiseGenerator() {
        this(System.currentTimeMillis());
    }
    
    /**
     * Create a noise generator with a specific seed.
     */
    public NoiseGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Generate 2D noise
     */
    public float noise(float x, float y) {
        return MathUtils.noise2D(x, y);
    }
    
    /**
     * Generate 3D noise
     */
    public float noise(float x, float y, float z) {
        return MathUtils.noise(x, y, z);
    }
    
    /**
     * Generate fractal noise with multiple octaves
     */
    public float fractal(float x, float y, int octaves, float persistence, float lacunarity) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Generate random float between -1 and 1
     */
    public float randomFloat() {
        return random.nextFloat() * 2.0f - 1.0f;
    }
    
    /**
     * Generate random float between min and max
     */
    public float randomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
    
    /**
     * Generate random integer between min and max (inclusive)
     */
    public int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * Get the seed used by this generator
     */
    public long getSeed() {
        return seed;
    }
}