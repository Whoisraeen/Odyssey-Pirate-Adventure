package com.odyssey.game.math;

/**
 * A 2D vector class for mathematical operations.
 */
public class Vector2f {
    public float x, y;
    
    public Vector2f() {
        this(0, 0);
    }
    
    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2f(Vector2f other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    /**
     * Set the components of this vector.
     */
    public Vector2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    /**
     * Set this vector to be equal to another vector.
     */
    public Vector2f set(Vector2f other) {
        this.x = other.x;
        this.y = other.y;
        return this;
    }
    
    /**
     * Add another vector to this vector.
     */
    public Vector2f add(Vector2f other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }
    
    /**
     * Add components to this vector.
     */
    public Vector2f add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }
    
    /**
     * Subtract another vector from this vector.
     */
    public Vector2f subtract(Vector2f other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }
    
    /**
     * Multiply this vector by a scalar.
     */
    public Vector2f multiply(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }
    
    /**
     * Multiply this vector by components.
     */
    public Vector2f multiply(float x, float y) {
        this.x *= x;
        this.y *= y;
        return this;
    }
    
    /**
     * Divide this vector by a scalar.
     */
    public Vector2f divide(float scalar) {
        if (scalar != 0) {
            this.x /= scalar;
            this.y /= scalar;
        }
        return this;
    }
    
    /**
     * Get the length (magnitude) of this vector.
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    /**
     * Get the squared length of this vector (more efficient than length()).
     */
    public float lengthSquared() {
        return x * x + y * y;
    }
    
    /**
     * Normalize this vector to unit length.
     */
    public Vector2f normalize() {
        float len = length();
        if (len > 0) {
            divide(len);
        }
        return this;
    }
    
    /**
     * Get the distance to another vector.
     */
    public float distance(Vector2f other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Get the squared distance to another vector.
     */
    public float distanceSquared(Vector2f other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * Calculate the dot product with another vector.
     */
    public float dot(Vector2f other) {
        return x * other.x + y * other.y;
    }
    
    /**
     * Calculate the cross product with another vector (returns scalar).
     */
    public float cross(Vector2f other) {
        return x * other.y - y * other.x;
    }
    
    /**
     * Get the angle of this vector in radians.
     */
    public float angle() {
        return (float) Math.atan2(y, x);
    }
    
    /**
     * Get the angle between this vector and another in radians.
     */
    public float angleTo(Vector2f other) {
        return (float) Math.atan2(cross(other), dot(other));
    }
    
    /**
     * Rotate this vector by the given angle in radians.
     */
    public Vector2f rotate(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float newX = x * cos - y * sin;
        float newY = x * sin + y * cos;
        x = newX;
        y = newY;
        return this;
    }
    
    /**
     * Linearly interpolate between this vector and another.
     */
    public Vector2f lerp(Vector2f target, float t) {
        x += t * (target.x - x);
        y += t * (target.y - y);
        return this;
    }
    
    /**
     * Create a copy of this vector.
     */
    public Vector2f copy() {
        return new Vector2f(x, y);
    }
    
    /**
     * Check if this vector is zero.
     */
    public boolean isZero() {
        return x == 0 && y == 0;
    }
    
    /**
     * Set this vector to zero.
     */
    public Vector2f zero() {
        x = 0;
        y = 0;
        return this;
    }
    
    // Static utility methods
    
    /**
     * Create a vector from an angle and magnitude.
     */
    public static Vector2f fromAngle(float angle, float magnitude) {
        return new Vector2f(
            (float) Math.cos(angle) * magnitude,
            (float) Math.sin(angle) * magnitude
        );
    }
    
    /**
     * Calculate the distance between two vectors.
     */
    public static float distance(Vector2f a, Vector2f b) {
        return a.distance(b);
    }
    
    /**
     * Calculate the dot product of two vectors.
     */
    public static float dot(Vector2f a, Vector2f b) {
        return a.dot(b);
    }
    
    /**
     * Calculate the cross product of two vectors.
     */
    public static float cross(Vector2f a, Vector2f b) {
        return a.cross(b);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2f vector2f = (Vector2f) obj;
        return Float.compare(vector2f.x, x) == 0 && Float.compare(vector2f.y, y) == 0;
    }
    
    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }
    
    @Override
    public String toString() {
        return String.format("Vector2f(%.2f, %.2f)", x, y);
    }
}