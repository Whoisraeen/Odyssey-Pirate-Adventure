package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;
import org.joml.Vector3f;

/**
 * Component that represents the position, rotation, and scale of an entity in 3D space.
 * Upgraded to support 3D coordinates for marine ecosystem and world positioning.
 */
public class TransformComponent implements Component {
    public Vector3f position;
    public Vector3f rotation; // Euler angles in radians (pitch, yaw, roll)
    public Vector3f scale;
    
    public TransformComponent() {
        this(0, 0, 0);
    }
    
    public TransformComponent(float x, float y, float z) {
        this(new Vector3f(x, y, z), new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));
    }
    
    public TransformComponent(Vector3f position, Vector3f rotation, Vector3f scale) {
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = new Vector3f(scale);
    }
    
    /**
     * Get the forward direction vector based on yaw rotation.
     */
    public Vector3f getForward() {
        float yaw = rotation.y;
        return new Vector3f(
            (float) Math.sin(yaw), 
            0, 
            (float) Math.cos(yaw)
        ).normalize();
    }
    
    /**
     * Get the right direction vector based on yaw rotation.
     */
    public Vector3f getRight() {
        float yaw = rotation.y;
        return new Vector3f(
            (float) Math.cos(yaw), 
            0, 
            (float) -Math.sin(yaw)
        ).normalize();
    }
    
    /**
     * Get the up direction vector based on pitch and roll.
     */
    public Vector3f getUp() {
        // For marine creatures, up is typically world Y-axis unless rolling
        return new Vector3f(0, 1, 0);
    }
    
    /**
     * Translate the position by the given offset.
     */
    public void translate(Vector3f offset) {
        position.add(offset);
    }
    
    /**
     * Translate the position by the given offset.
     */
    public void translate(float x, float y, float z) {
        position.add(x, y, z);
    }
    
    /**
     * Rotate by the given angles in radians (pitch, yaw, roll).
     */
    public void rotate(float pitch, float yaw, float roll) {
        rotation.add(pitch, yaw, roll);
        normalizeRotation();
    }
    
    /**
     * Rotate around Y-axis (yaw) - most common for marine creatures.
     */
    public void rotateY(float yaw) {
        rotation.y += yaw;
        normalizeRotation();
    }
    
    /**
     * Scale by the given factors.
     */
    public void scale(float scaleX, float scaleY, float scaleZ) {
        scale.mul(scaleX, scaleY, scaleZ);
    }
    
    /**
     * Normalize rotation angles to [-π, π] range.
     */
    private void normalizeRotation() {
        rotation.x = normalizeAngle(rotation.x);
        rotation.y = normalizeAngle(rotation.y);
        rotation.z = normalizeAngle(rotation.z);
    }
    
    private float normalizeAngle(float angle) {
        while (angle < -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
    
    @Override
    public Component copy() {
        return new TransformComponent(new Vector3f(position), new Vector3f(rotation), new Vector3f(scale));
    }
    
    @Override
    public String toString() {
        return String.format("Transform{pos=%.2f,%.2f,%.2f, rot=%.2f,%.2f,%.2f, scale=%.2f,%.2f,%.2f}", 
                           position.x, position.y, position.z, 
                           rotation.x, rotation.y, rotation.z,
                           scale.x, scale.y, scale.z);
    }
}