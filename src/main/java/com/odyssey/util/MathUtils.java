package com.odyssey.util;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Matrix4f;

/**
 * Mathematical utility functions for The Odyssey.
 * 
 * This class provides common mathematical operations, constants, and utility
 * functions frequently used in game development, including interpolation,
 * noise generation, and geometric calculations.
 */
public final class MathUtils {
    
    // Mathematical constants
    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = 2.0f * PI;
    public static final float HALF_PI = PI * 0.5f;
    public static final float QUARTER_PI = PI * 0.25f;
    public static final float DEG_TO_RAD = PI / 180.0f;
    public static final float RAD_TO_DEG = 180.0f / PI;
    public static final float EPSILON = 1e-6f;
    public static final float GOLDEN_RATIO = 1.618033988749f;
    
    // Common values
    public static final float SQRT_2 = (float) Math.sqrt(2.0);
    public static final float SQRT_3 = (float) Math.sqrt(3.0);
    public static final float INV_SQRT_2 = 1.0f / SQRT_2;
    public static final float INV_SQRT_3 = 1.0f / SQRT_3;
    
    // Prevent instantiation
    private MathUtils() {}
    
    /**
     * Clamp a value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamp a value between min and max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamp a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
    
    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
    
    /**
     * Smooth step interpolation (3t² - 2t³).
     */
    public static float smoothstep(float t) {
        t = clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
    
    /**
     * Smoother step interpolation (6t⁵ - 15t⁴ + 10t³).
     */
    public static float smootherstep(float t) {
        t = clamp(t, 0.0f, 1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }
    
    /**
     * Inverse linear interpolation - find t given a, b, and value.
     */
    public static float inverseLerp(float a, float b, float value) {
        if (Math.abs(b - a) < EPSILON) {
            return 0.0f;
        }
        return (value - a) / (b - a);
    }
    
    /**
     * Remap a value from one range to another.
     */
    public static float remap(float value, float fromMin, float fromMax, float toMin, float toMax) {
        float t = inverseLerp(fromMin, fromMax, value);
        return lerp(toMin, toMax, t);
    }
    
    /**
     * Check if two floats are approximately equal.
     */
    public static boolean approximately(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }
    
    /**
     * Check if two floats are approximately equal with custom epsilon.
     */
    public static boolean approximately(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }
    
    /**
     * Wrap an angle to the range [0, 2π].
     */
    public static float wrapAngle(float angle) {
        angle = angle % TWO_PI;
        if (angle < 0) {
            angle += TWO_PI;
        }
        return angle;
    }
    
    /**
     * Wrap an angle to the range [-π, π].
     */
    public static float wrapAngleSigned(float angle) {
        angle = wrapAngle(angle);
        if (angle > PI) {
            angle -= TWO_PI;
        }
        return angle;
    }
    
    /**
     * Calculate the shortest angular distance between two angles.
     */
    public static float angleDifference(float a, float b) {
        float diff = wrapAngleSigned(b - a);
        return diff;
    }
    
    /**
     * Spherical linear interpolation for angles.
     */
    public static float slerpAngle(float a, float b, float t) {
        float diff = angleDifference(a, b);
        return a + diff * t;
    }
    
    /**
     * Fast inverse square root approximation (Quake III algorithm).
     */
    public static float fastInverseSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x);
        return x;
    }
    
    /**
     * Fast square root using inverse square root.
     */
    public static float fastSqrt(float x) {
        return x * fastInverseSqrt(x);
    }
    
    /**
     * Calculate distance between two 2D points.
     */
    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate distance between two 3D points.
     */
    public static float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Calculate squared distance between two 2D points (faster than distance).
     */
    public static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }
    
    /**
     * Calculate squared distance between two 3D points (faster than distance).
     */
    public static float distanceSquared(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * Check if a point is inside a circle.
     */
    public static boolean pointInCircle(float px, float py, float cx, float cy, float radius) {
        return distanceSquared(px, py, cx, cy) <= radius * radius;
    }
    
    /**
     * Check if a point is inside a sphere.
     */
    public static boolean pointInSphere(float px, float py, float pz, float cx, float cy, float cz, float radius) {
        return distanceSquared(px, py, pz, cx, cy, cz) <= radius * radius;
    }
    
    /**
     * Check if a point is inside an axis-aligned bounding box.
     */
    public static boolean pointInAABB(float px, float py, float minX, float minY, float maxX, float maxY) {
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }
    
    /**
     * Check if a point is inside an axis-aligned bounding box (3D).
     */
    public static boolean pointInAABB(float px, float py, float pz, 
                                     float minX, float minY, float minZ, 
                                     float maxX, float maxY, float maxZ) {
        return px >= minX && px <= maxX && 
               py >= minY && py <= maxY && 
               pz >= minZ && pz <= maxZ;
    }
    
    /**
     * Generate a pseudo-random float between 0 and 1 using a seed.
     */
    public static float random(int seed) {
        seed = (seed ^ 61) ^ (seed >>> 16);
        seed *= 9;
        seed = seed ^ (seed >>> 4);
        seed *= 0x27d4eb2d;
        seed = seed ^ (seed >>> 15);
        return (seed & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
    
    /**
     * Generate a pseudo-random float between min and max using a seed.
     */
    public static float random(int seed, float min, float max) {
        return lerp(min, max, random(seed));
    }
    
    /**
     * Simple 1D noise function.
     */
    public static float noise1D(float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        float u = smoothstep(f);
        
        return lerp(random(i), random(i + 1), u);
    }
    
    /**
     * Simple 2D noise function.
     */
    public static float noise2D(float x, float y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        float fx = x - ix;
        float fy = y - iy;
        
        float ux = smoothstep(fx);
        float uy = smoothstep(fy);
        
        float a = random(ix + iy * 57);
        float b = random(ix + 1 + iy * 57);
        float c = random(ix + (iy + 1) * 57);
        float d = random(ix + 1 + (iy + 1) * 57);
        
        return lerp(lerp(a, b, ux), lerp(c, d, ux), uy);
    }
    
    /**
     * Simple 3D noise function.
     */
    public static float noise(float x, float y, float z) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        float fx = x - ix;
        float fy = y - iy;
        float fz = z - iz;
        
        float ux = smoothstep(fx);
        float uy = smoothstep(fy);
        float uz = smoothstep(fz);
        
        // 8 corner values for 3D interpolation
        float a = random(ix + iy * 57 + iz * 113);
        float b = random(ix + 1 + iy * 57 + iz * 113);
        float c = random(ix + (iy + 1) * 57 + iz * 113);
        float d = random(ix + 1 + (iy + 1) * 57 + iz * 113);
        float e = random(ix + iy * 57 + (iz + 1) * 113);
        float f = random(ix + 1 + iy * 57 + (iz + 1) * 113);
        float g = random(ix + (iy + 1) * 57 + (iz + 1) * 113);
        float h = random(ix + 1 + (iy + 1) * 57 + (iz + 1) * 113);
        
        // Trilinear interpolation
        float i1 = lerp(a, b, ux);
        float i2 = lerp(c, d, ux);
        float i3 = lerp(e, f, ux);
        float i4 = lerp(g, h, ux);
        
        float j1 = lerp(i1, i2, uy);
        float j2 = lerp(i3, i4, uy);
        
        return lerp(j1, j2, uz);
    }
    
    /**
     * Fractional Brownian Motion (fBm) noise.
     */
    public static float fbm(float x, float y, int octaves, float persistence, float scale) {
        float value = 0.0f;
        float amplitude = 1.0f;
        float frequency = scale;
        float maxValue = 0.0f;
        
        for (int i = 0; i < octaves; i++) {
            value += noise2D(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0f;
        }
        
        return value / maxValue;
    }
    
    /**
     * Calculate the next power of 2 greater than or equal to the given value.
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }
    
    /**
     * Check if a value is a power of 2.
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
    
    /**
     * Calculate the sign of a value (-1, 0, or 1).
     */
    public static int sign(float value) {
        return Float.compare(value, 0.0f);
    }
    
    /**
     * Calculate the sign of a value (-1, 0, or 1).
     */
    public static int sign(int value) {
        return Integer.compare(value, 0);
    }
    
    /**
     * Move a value towards a target by a maximum delta.
     */
    public static float moveTowards(float current, float target, float maxDelta) {
        if (Math.abs(target - current) <= maxDelta) {
            return target;
        }
        return current + sign(target - current) * maxDelta;
    }
    
    /**
     * Exponential decay towards a target value.
     */
    public static float exponentialDecay(float current, float target, float decay, float deltaTime) {
        return target + (current - target) * (float) Math.exp(-decay * deltaTime);
    }
    
    /**
     * Spring interpolation towards a target.
     */
    public static float spring(float current, float target, float velocity, float stiffness, float damping, float deltaTime) {
        float force = stiffness * (target - current) - damping * velocity;
        velocity += force * deltaTime;
        current += velocity * deltaTime;
        return current;
    }
    
    /**
     * Barycentric interpolation in a triangle.
     */
    public static float barycentricInterpolation(Vector3f p1, Vector3f p2, Vector3f p3, 
                                               Vector2f pos, float v1, float v2, float v3) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        float l3 = 1.0f - l1 - l2;
        return l1 * v1 + l2 * v2 + l3 * v3;
    }
    
    /**
     * Convert world coordinates to screen coordinates.
     */
    public static Vector2f worldToScreen(Vector3f worldPos, Matrix4f viewMatrix, Matrix4f projMatrix, 
                                        int screenWidth, int screenHeight) {
        Vector4f clipPos = new Vector4f(worldPos, 1.0f);
        viewMatrix.transform(clipPos);
        projMatrix.transform(clipPos);
        
        if (clipPos.w != 0.0f) {
            clipPos.x /= clipPos.w;
            clipPos.y /= clipPos.w;
        }
        
        float screenX = (clipPos.x + 1.0f) * 0.5f * screenWidth;
        float screenY = (1.0f - clipPos.y) * 0.5f * screenHeight;
        
        return new Vector2f(screenX, screenY);
    }
    
    /**
     * Generate a hash code for a 2D coordinate.
     */
    public static int hash2D(int x, int y) {
        return x * 73856093 ^ y * 19349663;
    }
    
    /**
     * Generate a hash code for a 3D coordinate.
     */
    public static int hash3D(int x, int y, int z) {
        return x * 73856093 ^ y * 19349663 ^ z * 83492791;
    }
}