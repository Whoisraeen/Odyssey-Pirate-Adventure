package com.odyssey.world.noise;

import java.util.Random;

/**
 * Advanced noise generator for procedural world generation.
 * Provides multiple noise functions including Perlin-like noise, fractal noise, and ridged noise.
 */
public class NoiseGenerator {
    
    private final Random random;
    private final long seed;
    
    // Noise parameters
    private static final int[] PERMUTATION = new int[512];
    private static final float[] GRADIENT_X = new float[256];
    private static final float[] GRADIENT_Y = new float[256];
    
    static {
        // Initialize permutation table and gradients
        Random staticRandom = new Random(12345);
        for (int i = 0; i < 256; i++) {
            PERMUTATION[i] = i;
            PERMUTATION[i + 256] = i;
            
            float angle = staticRandom.nextFloat() * (float) Math.PI * 2;
            GRADIENT_X[i] = (float) Math.cos(angle);
            GRADIENT_Y[i] = (float) Math.sin(angle);
        }
        
        // Shuffle permutation table
        for (int i = 255; i > 0; i--) {
            int j = staticRandom.nextInt(i + 1);
            int temp = PERMUTATION[i];
            PERMUTATION[i] = PERMUTATION[j];
            PERMUTATION[j] = temp;
            PERMUTATION[i + 256] = PERMUTATION[i];
        }
    }
    
    public NoiseGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Generate 2D Perlin-like noise
     */
    public float sample(float x, float y) {
        return noise(x, y);
    }
    
    /**
     * Generate 3D noise
     */
    public float sample3D(float x, float y, float z) {
        return noise3D(x, y, z);
    }
    
    /**
     * Generate 3D noise (public interface)
     */
    public float noise(float x, float y, float z) {
        return noise3D(x, y, z);
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
     * Generate ridged noise (inverted absolute value)
     */
    public float ridged(float x, float y, int octaves, float persistence, float lacunarity) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            float n = Math.abs(noise(x * frequency, y * frequency));
            n = 1.0f - n;
            n = n * n;
            total += n * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Generate turbulence (absolute value of fractal noise)
     */
    public float turbulence(float x, float y, int octaves, float persistence, float lacunarity) {
        return Math.abs(fractal(x, y, octaves, persistence, lacunarity));
    }
    
    /**
     * Core 2D noise function
     */
    private float noise(float x, float y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        
        x -= Math.floor(x);
        y -= Math.floor(y);
        
        float u = fade(x);
        float v = fade(y);
        
        int A = PERMUTATION[X] + Y;
        int B = PERMUTATION[X + 1] + Y;
        
        return lerp(v, lerp(u, grad(PERMUTATION[A], x, y),
                              grad(PERMUTATION[B], x - 1, y)),
                       lerp(u, grad(PERMUTATION[A + 1], x, y - 1),
                              grad(PERMUTATION[B + 1], x - 1, y - 1)));
    }
    
    /**
     * Core 3D noise function
     */
    private float noise3D(float x, float y, float z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        float u = fade(x);
        float v = fade(y);
        float w = fade(z);
        
        int A = PERMUTATION[X] + Y;
        int AA = PERMUTATION[A] + Z;
        int AB = PERMUTATION[A + 1] + Z;
        int B = PERMUTATION[X + 1] + Y;
        int BA = PERMUTATION[B] + Z;
        int BB = PERMUTATION[B + 1] + Z;
        
        return lerp(w, lerp(v, lerp(u, grad3D(PERMUTATION[AA], x, y, z),
                                      grad3D(PERMUTATION[BA], x - 1, y, z)),
                              lerp(u, grad3D(PERMUTATION[AB], x, y - 1, z),
                                     grad3D(PERMUTATION[BB], x - 1, y - 1, z))),
                      lerp(v, lerp(u, grad3D(PERMUTATION[AA + 1], x, y, z - 1),
                                     grad3D(PERMUTATION[BA + 1], x - 1, y, z - 1)),
                             lerp(u, grad3D(PERMUTATION[AB + 1], x, y - 1, z - 1),
                                    grad3D(PERMUTATION[BB + 1], x - 1, y - 1, z - 1))));
    }
    
    /**
     * Fade function for smooth interpolation
     */
    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Linear interpolation
     */
    private float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }
    
    /**
     * 2D gradient function
     */
    private float grad(int hash, float x, float y) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : h == 12 || h == 14 ? x : 0;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    /**
     * 3D gradient function
     */
    private float grad3D(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
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
    
    // Getters
    public long getSeed() { return seed; }
}