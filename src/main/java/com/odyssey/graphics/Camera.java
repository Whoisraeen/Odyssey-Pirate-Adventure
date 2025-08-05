package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camera system for The Odyssey.
 * Handles view transformations, movement, and projection.
 */
public class Camera {
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);
    
    // Camera properties
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f velocity;
    
    // View properties
    private float fieldOfView;
    private float nearPlane;
    private float farPlane;
    private float aspectRatio;
    
    // Matrices
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    
    // Movement properties
    private float movementSpeed = 10.0f;
    private float mouseSensitivity = 0.1f;
    private float smoothing = 0.1f;
    
    // Smoothed rotation for better camera feel
    private Vector3f targetRotation;
    private Vector3f smoothedRotation;
    
    public Camera() {
        this.position = new Vector3f(0, 20, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.velocity = new Vector3f();
        this.targetRotation = new Vector3f();
        this.smoothedRotation = new Vector3f();
        
        this.fieldOfView = 70.0f;
        this.nearPlane = 0.1f;
        this.farPlane = 1000.0f;
        this.aspectRatio = 16.0f / 9.0f;
        
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        updateProjectionMatrix();
    }
    
    public void update(double deltaTime) {
        // Apply velocity to position
        position.add(
            (float) (velocity.x * deltaTime),
            (float) (velocity.y * deltaTime),
            (float) (velocity.z * deltaTime)
        );
        
        // Smooth camera rotation for better feel
        smoothedRotation.lerp(targetRotation, (float) (smoothing * deltaTime * 60));
        rotation.set(smoothedRotation);
        
        // Apply drag to velocity
        velocity.mul(0.9f);
        
        updateViewMatrix();
    }
    
    private void updateViewMatrix() {
        viewMatrix.identity()
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateZ((float) Math.toRadians(rotation.z))
                .translate(-position.x, -position.y, -position.z);
    }
    
    private void updateProjectionMatrix() {
        projectionMatrix.setPerspective(
            (float) Math.toRadians(fieldOfView),
            aspectRatio,
            nearPlane,
            farPlane
        );
    }
    
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateProjectionMatrix();
    }
    
    public void moveForward(float amount) {
        float yaw = (float) Math.toRadians(rotation.y);
        float pitch = (float) Math.toRadians(rotation.x);
        
        Vector3f forward = new Vector3f(
            (float) Math.sin(yaw) * (float) Math.cos(pitch),
            (float) -Math.sin(pitch),
            (float) -Math.cos(yaw) * (float) Math.cos(pitch)
        );
        
        velocity.add(forward.mul(amount * movementSpeed));
    }
    
    public void moveRight(float amount) {
        float yaw = (float) Math.toRadians(rotation.y);
        
        Vector3f right = new Vector3f(
            (float) Math.cos(yaw),
            0,
            (float) Math.sin(yaw)
        );
        
        velocity.add(right.mul(amount * movementSpeed));
    }
    
    public void moveUp(float amount) {
        velocity.y += amount * movementSpeed;
    }
    
    public void moveBackward(float amount) {
        moveForward(-amount);
    }
    
    public void moveLeft(float amount) {
        moveRight(-amount);
    }
    
    public void moveDown(float amount) {
        velocity.y -= amount * movementSpeed;
    }
    
    public void rotateX(float amount) {
        targetRotation.x += amount;
        // Clamp pitch to prevent camera flipping
        targetRotation.x = Math.max(-89.0f, Math.min(89.0f, targetRotation.x));
    }
    
    public void rotateY(float amount) {
        targetRotation.y += amount;
        // Normalize yaw
        while (targetRotation.y > 180) targetRotation.y -= 360;
        while (targetRotation.y < -180) targetRotation.y += 360;
    }
    
    public void rotate(float deltaX, float deltaY) {
        targetRotation.y += deltaX * mouseSensitivity;
        targetRotation.x += deltaY * mouseSensitivity;
        
        // Clamp pitch to prevent camera flipping
        targetRotation.x = Math.max(-89.0f, Math.min(89.0f, targetRotation.x));
        
        // Normalize yaw
        while (targetRotation.y > 180) targetRotation.y -= 360;
        while (targetRotation.y < -180) targetRotation.y += 360;
    }
    
    public Vector3f getForwardVector() {
        float yaw = (float) Math.toRadians(rotation.y);
        float pitch = (float) Math.toRadians(rotation.x);
        
        return new Vector3f(
            (float) Math.sin(yaw) * (float) Math.cos(pitch),
            (float) -Math.sin(pitch),
            (float) -Math.cos(yaw) * (float) Math.cos(pitch)
        );
    }
    
    public Vector3f getRightVector() {
        float yaw = (float) Math.toRadians(rotation.y);
        return new Vector3f(
            (float) Math.cos(yaw),
            0,
            (float) Math.sin(yaw)
        );
    }
    
    public Vector3f getUpVector() {
        return new Vector3f(0, 1, 0);
    }
    
    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setPosition(float x, float y, float z) { this.position.set(x, y, z); }
    
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public void setRotation(Vector3f rotation) { 
        this.rotation.set(rotation);
        this.targetRotation.set(rotation);
        this.smoothedRotation.set(rotation);
    }
    public void setRotation(float x, float y, float z) { 
        this.rotation.set(x, y, z);
        this.targetRotation.set(x, y, z);
        this.smoothedRotation.set(x, y, z);
    }
    
    public Matrix4f getViewMatrix() { return new Matrix4f(viewMatrix); }
    public Matrix4f getProjectionMatrix() { return new Matrix4f(projectionMatrix); }
    
    public float getFieldOfView() { return fieldOfView; }
    public void setFieldOfView(float fieldOfView) {
        this.fieldOfView = fieldOfView;
        updateProjectionMatrix();
    }
    
    public float getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(float movementSpeed) { this.movementSpeed = movementSpeed; }
    
    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float mouseSensitivity) { this.mouseSensitivity = mouseSensitivity; }
}