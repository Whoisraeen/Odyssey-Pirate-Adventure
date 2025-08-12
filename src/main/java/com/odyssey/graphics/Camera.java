package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camera system for The Odyssey.
 * Handles view transformations, movement, and projection with Minecraft-style camera modes.
 */
public class Camera {
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);
    
    // Camera modes (like Minecraft)
    public enum CameraMode {
        FIRST_PERSON,      // F5 once - first person view
        THIRD_PERSON_BACK, // F5 twice - third person behind player
        THIRD_PERSON_FRONT // F5 three times - third person in front of player
    }
    
    // Camera properties
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f velocity;
    
    // Player-centric camera system
    private Vector3f playerPosition;
    private Vector3f playerRotation;
    private CameraMode cameraMode;
    private float thirdPersonDistance = 5.0f;
    private float thirdPersonHeight = 1.5f;
    
    // View properties
    private float fieldOfView;
    private float nearPlane;
    private float farPlane;
    private float aspectRatio;
    
    // Matrices
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    
    // TAA support
    private Matrix4f previousViewMatrix;
    private Matrix4f previousProjectionMatrix;
    private Vector2f jitterOffset;
    private int frameIndex = 0;
    private boolean taaEnabled = true;
    
    // Movement properties
    private float movementSpeed = 10.0f;
    private float mouseSensitivity = 0.1f;
    private float smoothing = 0.1f;
    
    // Smoothed rotation for better camera feel
    private Vector3f targetRotation;
    private Vector3f smoothedRotation;
    
    // Underwater state tracking
    private boolean isUnderwater = false;
    private float underwaterDepth = 0.0f;
    private float seaLevel = 64.0f;  // Should match GerstnerOceanRenderer
    private float previousUnderwaterDepth = 0.0f;
    
    public Camera() {
        this.position = new Vector3f(0, 100, 50); // Position above ocean and cubes, with some distance back
        this.rotation = new Vector3f(-20, 0, 0); // Look down slightly to see the ocean and cubes
        this.velocity = new Vector3f();
        this.targetRotation = new Vector3f(-20, 0, 0);
        this.smoothedRotation = new Vector3f(-20, 0, 0);
        
        // Initialize player-centric camera system
        this.playerPosition = new Vector3f(0, 100, 50);
        this.playerRotation = new Vector3f(-20, 0, 0);
        this.cameraMode = CameraMode.FIRST_PERSON;
        
        this.fieldOfView = 70.0f;
        this.nearPlane = 0.1f;
        this.farPlane = 1000.0f;
        this.aspectRatio = 16.0f / 9.0f;
        
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        // Initialize TAA matrices
        this.previousViewMatrix = new Matrix4f();
        this.previousProjectionMatrix = new Matrix4f();
        this.jitterOffset = new Vector2f();
        
        updateProjectionMatrix();
    }
    
    public void update(double deltaTime) {
        // Store previous matrices for TAA
        if (taaEnabled) {
            previousViewMatrix.set(viewMatrix);
            previousProjectionMatrix.set(projectionMatrix);
        }
        
        // Apply velocity to player position (player movement)
        playerPosition.add(
            (float) (velocity.x * deltaTime),
            (float) (velocity.y * deltaTime),
            (float) (velocity.z * deltaTime)
        );
        
        // Smooth camera rotation for better feel
        smoothedRotation.lerp(targetRotation, (float) (smoothing * deltaTime * 60));
        playerRotation.set(smoothedRotation);
        
        // Update camera position based on camera mode
        updateCameraPosition();
        
        // Update underwater state
        updateUnderwaterState();
        
        // Apply drag to velocity
        velocity.mul(0.9f);
        
        // Update TAA jitter
        if (taaEnabled) {
            updateTAAJitter();
        }
        
        updateViewMatrix();
        updateProjectionMatrix();
    }
    
    private void updateCameraPosition() {
        switch (cameraMode) {
            case FIRST_PERSON:
                // Camera is at player position with player rotation
                position.set(playerPosition);
                rotation.set(playerRotation);
                break;
                
            case THIRD_PERSON_BACK:
                // Camera is behind the player
                float yawBack = (float) Math.toRadians(playerRotation.y);
                position.set(
                    playerPosition.x - (float) Math.sin(yawBack) * thirdPersonDistance,
                    playerPosition.y + thirdPersonHeight,
                    playerPosition.z + (float) Math.cos(yawBack) * thirdPersonDistance
                );
                rotation.set(playerRotation);
                break;
                
            case THIRD_PERSON_FRONT:
                // Camera is in front of the player, looking back at them
                float yawFront = (float) Math.toRadians(playerRotation.y);
                position.set(
                    playerPosition.x + (float) Math.sin(yawFront) * thirdPersonDistance,
                    playerPosition.y + thirdPersonHeight,
                    playerPosition.z - (float) Math.cos(yawFront) * thirdPersonDistance
                );
                // Look back at the player (180 degrees from player rotation)
                rotation.set(playerRotation.x, playerRotation.y + 180.0f, playerRotation.z);
                break;
        }
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
        
        // Apply TAA jitter to projection matrix
        if (taaEnabled) {
            applyTAAJitter();
        }
    }
    
    /**
     * Updates TAA jitter using Halton sequence for stable temporal sampling.
     */
    private void updateTAAJitter() {
        frameIndex++;
        
        // Halton sequence for 2D jitter (base 2 and 3)
        float jitterX = haltonSequence(frameIndex, 2) - 0.5f;
        float jitterY = haltonSequence(frameIndex, 3) - 0.5f;
        
        // Scale jitter by pixel size
        jitterOffset.set(jitterX / (screenWidth * 0.5f), jitterY / (screenHeight * 0.5f));
    }
    
    /**
     * Applies TAA jitter to the projection matrix.
     */
    private void applyTAAJitter() {
        // Apply sub-pixel jitter to projection matrix
        projectionMatrix.m20(projectionMatrix.m20() + jitterOffset.x);
        projectionMatrix.m21(projectionMatrix.m21() + jitterOffset.y);
    }
    
    /**
     * Generates Halton sequence value for TAA jitter.
     */
    private float haltonSequence(int index, int base) {
        float result = 0.0f;
        float f = 1.0f / base;
        int i = index;
        while (i > 0) {
            result += f * (i % base);
            i /= base;
            f /= base;
        }
        return result;
    }
    
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateProjectionMatrix();
    }
    
    /**
     * Cycles through camera modes like Minecraft's F5 key
     */
    public void cycleCameraMode() {
        switch (cameraMode) {
            case FIRST_PERSON:
                cameraMode = CameraMode.THIRD_PERSON_BACK;
                logger.info("Switched to third person (back) view");
                break;
            case THIRD_PERSON_BACK:
                cameraMode = CameraMode.THIRD_PERSON_FRONT;
                logger.info("Switched to third person (front) view");
                break;
            case THIRD_PERSON_FRONT:
                cameraMode = CameraMode.FIRST_PERSON;
                logger.info("Switched to first person view");
                break;
        }
    }
    
    public void moveForward(float amount) {
        float yaw = (float) Math.toRadians(playerRotation.y);
        float pitch = (float) Math.toRadians(playerRotation.x);
        
        Vector3f forward = new Vector3f(
            (float) Math.sin(yaw) * (float) Math.cos(pitch),
            (float) -Math.sin(pitch),
            (float) -Math.cos(yaw) * (float) Math.cos(pitch)
        );
        
        velocity.add(forward.mul(amount * movementSpeed));
    }
    
    public void moveRight(float amount) {
        float yaw = (float) Math.toRadians(playerRotation.y);
        
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
    public void setPosition(Vector3f position) { 
        this.playerPosition.set(position);
        updateCameraPosition();
        updateViewMatrix();
    }
    public void setPosition(float x, float y, float z) { 
        this.playerPosition.set(x, y, z);
        updateCameraPosition();
        updateViewMatrix();
    }
    
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public Vector3f getPlayerPosition() { return new Vector3f(playerPosition); }
    public Vector3f getPlayerRotation() { return new Vector3f(playerRotation); }
    public CameraMode getCameraMode() { return cameraMode; }
    
    public void setRotation(Vector3f rotation) { 
        this.playerRotation.set(rotation);
        this.targetRotation.set(rotation);
        this.smoothedRotation.set(rotation);
        updateCameraPosition();
        updateViewMatrix();
    }
    public void setRotation(float x, float y, float z) { 
        this.playerRotation.set(x, y, z);
        this.targetRotation.set(x, y, z);
        this.smoothedRotation.set(x, y, z);
        updateCameraPosition();
        updateViewMatrix();
    }
    
    public void setPlayerPosition(Vector3f position) {
        this.playerPosition.set(position);
        updateCameraPosition();
        updateViewMatrix();
    }
    
    public void setPlayerRotation(Vector3f rotation) {
        this.playerRotation.set(rotation);
        this.targetRotation.set(rotation);
        this.smoothedRotation.set(rotation);
        updateCameraPosition();
        updateViewMatrix();
    }
    
    public Matrix4f getViewMatrix() { return new Matrix4f(viewMatrix); }
    
    public void setViewMatrix(Matrix4f viewMatrix) {
        this.viewMatrix.set(viewMatrix);
    }
    
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
    
    /**
     * Updates underwater state based on camera position relative to sea level.
     */
    private void updateUnderwaterState() {
        previousUnderwaterDepth = underwaterDepth;
        
        // Check if camera is below sea level
        if (position.y < seaLevel) {
            isUnderwater = true;
            underwaterDepth = seaLevel - position.y;
        } else {
            isUnderwater = false;
            underwaterDepth = 0.0f;
        }
        
        // Log underwater state changes for debugging
        if (isUnderwater && previousUnderwaterDepth == 0.0f) {
            logger.info("Camera entered underwater at depth: {}", underwaterDepth);
        } else if (!isUnderwater && previousUnderwaterDepth > 0.0f) {
            logger.info("Camera exited underwater");
        }
    }
    
    /**
     * Updates underwater state with dynamic wave height from ocean system.
     * This should be called by the ocean system to provide accurate wave heights.
     */
    public void updateUnderwaterState(float waveHeightAtPosition) {
        previousUnderwaterDepth = underwaterDepth;
        
        // Check if camera is below the dynamic water surface
        if (position.y < waveHeightAtPosition) {
            isUnderwater = true;
            underwaterDepth = waveHeightAtPosition - position.y;
        } else {
            isUnderwater = false;
            underwaterDepth = 0.0f;
        }
        
        // Log underwater state changes for debugging
        if (isUnderwater && previousUnderwaterDepth == 0.0f) {
            logger.info("Camera entered underwater at depth: {} (wave height: {})", underwaterDepth, waveHeightAtPosition);
        } else if (!isUnderwater && previousUnderwaterDepth > 0.0f) {
            logger.info("Camera exited underwater");
        }
    }
    
    // Underwater state getters
    public boolean isUnderwater() { return isUnderwater; }
    public float getUnderwaterDepth() { return underwaterDepth; }
    public float getSeaLevel() { return seaLevel; }
    public void setSeaLevel(float seaLevel) { this.seaLevel = seaLevel; }
    
    // TAA getters and setters
    public Matrix4f getPreviousViewMatrix() { return new Matrix4f(previousViewMatrix); }
    public Matrix4f getPreviousProjectionMatrix() { return new Matrix4f(previousProjectionMatrix); }
    public Vector2f getJitterOffset() { return new Vector2f(jitterOffset); }
    public boolean isTAAEnabled() { return taaEnabled; }
    public void setTAAEnabled(boolean enabled) { this.taaEnabled = enabled; }
    public void setScreenSize(int width, int height) { 
        this.screenWidth = width; 
        this.screenHeight = height; 
    }
}