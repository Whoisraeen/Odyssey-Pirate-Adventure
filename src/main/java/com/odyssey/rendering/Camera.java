package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera class for 3D rendering in The Odyssey.
 * 
 * Provides view matrix calculation, camera movement, and various camera modes
 * suitable for ocean exploration and ship navigation.
 */
public class Camera {
    
    // Camera position and orientation
    private Vector3f position;
    private Vector3f forward;
    private Vector3f up;
    private Vector3f right;
    
    // Euler angles (in radians)
    private float pitch;
    private float yaw;
    private float roll;
    
    // Camera properties
    private float movementSpeed = 10.0f;
    private float mouseSensitivity = 0.002f;
    private float zoomSpeed = 2.0f;
    
    // Camera constraints
    private float minPitch = (float) Math.toRadians(-89.0f);
    private float maxPitch = (float) Math.toRadians(89.0f);
    
    // Camera modes
    public enum CameraMode {
        FREE_LOOK,      // Free camera movement
        FIRST_PERSON,   // First person view (on ship)
        THIRD_PERSON,   // Third person view (following ship)
        ORBITAL,        // Orbital camera around target
        CINEMATIC       // Cinematic camera with smooth movements
    }
    
    private CameraMode mode = CameraMode.FREE_LOOK;
    
    // Target for orbital and third-person modes
    private Vector3f target;
    private float orbitDistance = 10.0f;
    private float orbitHeight = 5.0f;
    
    // Smooth movement
    private boolean smoothMovement = true;
    private float smoothFactor = 0.1f;
    private Vector3f targetPosition;
    private Vector3f targetForward;
    
    // View matrix
    private Matrix4f viewMatrix;
    private boolean viewMatrixDirty = true;
    
    // Projection matrix properties
    private Matrix4f projectionMatrix;
    private float fov = 75.0f;
    private float aspectRatio = 16.0f / 9.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;
    private boolean projectionMatrixDirty = true;
    
    /**
     * Create a new camera with default settings.
     */
    public Camera() {
        this.position = new Vector3f(0.0f, 0.0f, 0.0f);
        this.forward = new Vector3f(0.0f, 0.0f, -1.0f);
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.right = new Vector3f(1.0f, 0.0f, 0.0f);
        
        this.target = new Vector3f();
        this.targetPosition = new Vector3f();
        this.targetForward = new Vector3f();
        
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        // Initialize angles from forward vector
        updateAnglesFromForward();
    }
    
    /**
     * Create a camera at a specific position.
     */
    public Camera(Vector3f position) {
        this();
        this.position.set(position);
        this.targetPosition.set(position);
    }
    
    /**
     * Update the camera (called each frame).
     */
    public void update(float deltaTime) {
        switch (mode) {
            case FREE_LOOK:
                updateFreeLook(deltaTime);
                break;
            case FIRST_PERSON:
                updateFirstPerson(deltaTime);
                break;
            case THIRD_PERSON:
                updateThirdPerson(deltaTime);
                break;
            case ORBITAL:
                updateOrbital(deltaTime);
                break;
            case CINEMATIC:
                updateCinematic(deltaTime);
                break;
        }
        
        // Apply smooth movement if enabled
        if (smoothMovement && mode != CameraMode.FREE_LOOK) {
            applySmoothMovement(deltaTime);
        }
        
        // Update view matrix if needed
        if (viewMatrixDirty) {
            updateViewMatrix();
        }
    }
    
    /**
     * Update free look camera mode.
     */
    private void updateFreeLook(float deltaTime) {
        // Free look mode updates are handled by input events
        // This method can be used for any continuous updates needed
    }
    
    /**
     * Update first person camera mode.
     */
    private void updateFirstPerson(float deltaTime) {
        // In first person mode, camera follows target exactly
        if (target != null) {
            position.set(target);
            viewMatrixDirty = true;
        }
    }
    
    /**
     * Update third person camera mode.
     */
    private void updateThirdPerson(float deltaTime) {
        if (target != null) {
            // Calculate desired position behind and above target
            Vector3f offset = new Vector3f(forward).negate().mul(orbitDistance);
            offset.y += orbitHeight;
            
            targetPosition.set(target).add(offset);
            targetForward.set(target).sub(position).normalize();
            
            viewMatrixDirty = true;
        }
    }
    
    /**
     * Update orbital camera mode.
     */
    private void updateOrbital(float deltaTime) {
        if (target != null) {
            // Calculate orbital position
            float x = target.x + orbitDistance * (float) Math.cos(yaw) * (float) Math.cos(pitch);
            float y = target.y + orbitDistance * (float) Math.sin(pitch) + orbitHeight;
            float z = target.z + orbitDistance * (float) Math.sin(yaw) * (float) Math.cos(pitch);
            
            targetPosition.set(x, y, z);
            targetForward.set(target).sub(position).normalize();
            
            viewMatrixDirty = true;
        }
    }
    
    /**
     * Update cinematic camera mode.
     */
    private void updateCinematic(float deltaTime) {
        // Cinematic mode can be used for scripted camera movements
        // Implementation depends on specific cinematic requirements
    }
    
    /**
     * Apply smooth movement interpolation.
     */
    private void applySmoothMovement(float deltaTime) {
        float factor = Math.min(1.0f, smoothFactor * deltaTime * 60.0f); // 60 FPS reference
        
        // Smoothly interpolate position
        position.lerp(targetPosition, factor);
        
        // Smoothly interpolate forward direction
        forward.lerp(targetForward, factor);
        forward.normalize();
        
        // Recalculate right and up vectors
        updateVectors();
    }
    
    /**
     * Move the camera forward/backward.
     */
    public void moveForward(float distance) {
        Vector3f movement = new Vector3f(forward).mul(distance);
        position.add(movement);
        viewMatrixDirty = true;
    }
    
    /**
     * Move the camera right/left.
     */
    public void moveRight(float distance) {
        Vector3f movement = new Vector3f(right).mul(distance);
        position.add(movement);
        viewMatrixDirty = true;
    }
    
    /**
     * Move the camera up/down.
     */
    public void moveUp(float distance) {
        Vector3f movement = new Vector3f(up).mul(distance);
        position.add(movement);
        viewMatrixDirty = true;
    }
    
    /**
     * Rotate the camera by mouse movement.
     */
    public void rotate(float deltaX, float deltaY) {
        yaw += deltaX * mouseSensitivity;
        pitch -= deltaY * mouseSensitivity;
        
        // Clamp pitch to prevent gimbal lock
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        
        // Update forward vector from angles
        updateForwardFromAngles();
        updateVectors();
        viewMatrixDirty = true;
    }
    
    /**
     * Set camera rotation from angles.
     */
    public void setRotation(float pitch, float yaw, float roll) {
        this.pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        this.yaw = yaw;
        this.roll = roll;
        
        updateForwardFromAngles();
        updateVectors();
        viewMatrixDirty = true;
    }
    
    /**
     * Look at a specific point.
     */
    public void lookAt(Vector3f target) {
        Vector3f direction = new Vector3f(target).sub(position).normalize();
        this.forward.set(direction);
        
        updateAnglesFromForward();
        updateVectors();
        viewMatrixDirty = true;
    }
    
    /**
     * Set camera position.
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        viewMatrixDirty = true;
    }
    
    /**
     * Set camera position.
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        viewMatrixDirty = true;
    }
    
    /**
     * Get camera position.
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Get camera forward vector.
     */
    public Vector3f getForward() {
        return new Vector3f(forward);
    }
    
    /**
     * Get camera up vector.
     */
    public Vector3f getUp() {
        return new Vector3f(up);
    }
    
    /**
     * Get camera right vector.
     */
    public Vector3f getRight() {
        return new Vector3f(right);
    }
    
    /**
     * Get pitch angle in radians.
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Get yaw angle in radians.
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Get roll angle in radians.
     */
    public float getRoll() {
        return roll;
    }
    
    /**
     * Set camera mode.
     */
    public void setMode(CameraMode mode) {
        this.mode = mode;
    }
    
    /**
     * Get camera mode.
     */
    public CameraMode getMode() {
        return mode;
    }
    
    /**
     * Set target for orbital and third-person modes.
     */
    public void setTarget(Vector3f target) {
        this.target.set(target);
    }
    
    /**
     * Set orbit distance for orbital mode.
     */
    public void setOrbitDistance(float distance) {
        this.orbitDistance = Math.max(1.0f, distance);
    }
    
    /**
     * Set orbit height for third-person and orbital modes.
     */
    public void setOrbitHeight(float height) {
        this.orbitHeight = height;
    }
    
    /**
     * Set movement speed.
     */
    public void setMovementSpeed(float speed) {
        this.movementSpeed = Math.max(0.1f, speed);
    }
    
    /**
     * Set mouse sensitivity.
     */
    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.0001f, sensitivity);
    }
    
    /**
     * Enable/disable smooth movement.
     */
    public void setSmoothMovement(boolean enabled) {
        this.smoothMovement = enabled;
    }
    
    /**
     * Set smooth movement factor.
     */
    public void setSmoothFactor(float factor) {
        this.smoothFactor = Math.max(0.01f, Math.min(1.0f, factor));
    }
    
    /**
     * Update view matrix and store in provided matrix.
     */
    public void updateViewMatrix(Matrix4f viewMatrix) {
        if (viewMatrixDirty) {
            updateViewMatrix();
        }
        viewMatrix.set(this.viewMatrix);
    }
    
    /**
     * Update the internal view matrix.
     */
    private void updateViewMatrix() {
        Vector3f center = new Vector3f(position).add(forward);
        viewMatrix.setLookAt(position, center, up);
        viewMatrixDirty = false;
    }
    
    /**
     * Update forward vector from pitch and yaw angles.
     */
    private void updateForwardFromAngles() {
        float cosPitch = (float) Math.cos(pitch);
        forward.x = (float) Math.cos(yaw) * cosPitch;
        forward.y = (float) Math.sin(pitch);
        forward.z = (float) Math.sin(yaw) * cosPitch;
        forward.normalize();
    }
    
    /**
     * Update angles from forward vector.
     */
    private void updateAnglesFromForward() {
        pitch = (float) Math.asin(forward.y);
        yaw = (float) Math.atan2(forward.z, forward.x);
    }
    
    /**
     * Update right and up vectors from forward vector.
     */
    private void updateVectors() {
        // Calculate right vector (cross product of world up and forward)
        Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        forward.cross(worldUp, right);
        right.normalize();
        
        // Calculate up vector (cross product of forward and right)
        right.cross(forward, up);
        up.normalize();
    }
    
    /**
     * Get view matrix.
     */
    public Matrix4f getViewMatrix() {
        if (viewMatrixDirty) {
            updateViewMatrix();
        }
        return new Matrix4f(viewMatrix);
    }
    
    /**
     * Get projection matrix.
     */
    public Matrix4f getProjectionMatrix() {
        if (projectionMatrixDirty) {
            updateProjectionMatrix();
        }
        return new Matrix4f(projectionMatrix);
    }
    
    /**
     * Get near plane distance.
     */
    public float getNearPlane() {
        return nearPlane;
    }
    
    /**
     * Get near plane distance (alias for compatibility).
     */
    public float getNear() {
        return nearPlane;
    }
    
    /**
     * Get far plane distance.
     */
    public float getFarPlane() {
        return farPlane;
    }
    
    /**
     * Get far plane distance (alias for compatibility).
     */
    public float getFar() {
        return farPlane;
    }
    
    /**
     * Set projection parameters.
     */
    public void setProjection(float fov, float aspectRatio, float nearPlane, float farPlane) {
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.projectionMatrixDirty = true;
    }
    
    /**
     * Set aspect ratio.
     */
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        this.projectionMatrixDirty = true;
    }
    
    /**
     * Update the internal projection matrix.
     */
    private void updateProjectionMatrix() {
        projectionMatrix.setPerspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
        projectionMatrixDirty = false;
    }
    
    /**
     * Zoom in/out (for orbital mode).
     */
    public void zoom(float delta) {
        if (mode == CameraMode.ORBITAL) {
            orbitDistance = Math.max(1.0f, orbitDistance + delta * zoomSpeed);
            viewMatrixDirty = true;
        }
    }
    
    /**
     * Reset camera to default state.
     */
    public void reset() {
        position.set(0.0f, 0.0f, 0.0f);
        forward.set(0.0f, 0.0f, -1.0f);
        up.set(0.0f, 1.0f, 0.0f);
        right.set(1.0f, 0.0f, 0.0f);
        
        pitch = 0.0f;
        yaw = 0.0f;
        roll = 0.0f;
        
        orbitDistance = 10.0f;
        orbitHeight = 5.0f;
        
        mode = CameraMode.FREE_LOOK;
        viewMatrixDirty = true;
    }
}