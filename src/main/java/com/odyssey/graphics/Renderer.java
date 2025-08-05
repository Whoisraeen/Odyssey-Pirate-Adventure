package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Main renderer for The Odyssey.
 * Handles all rendering operations including voxel world, ocean, ships, and effects.
 */
public class Renderer {
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);
    
    private final GameConfig config;
    
    // Matrices
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f modelMatrix;
    
    // Camera
    private Vector3f cameraPosition;
    private Vector3f cameraRotation;
    
    // Rendering state
    private boolean wireframeMode = false;
    private float fieldOfView = 70.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;
    
    public Renderer(GameConfig config) {
        this.config = config;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
        this.cameraPosition = new Vector3f(0, 10, 0);
        this.cameraRotation = new Vector3f(0, 0, 0);
    }
    
    public void initialize() {
        logger.info("Initializing renderer...");
        
        // Set up projection matrix
        updateProjectionMatrix(config.getWindowWidth(), config.getWindowHeight());
        
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        
        // Enable face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        logger.info("Renderer initialized successfully");
    }
    
    public void beginFrame() {
        // Clear the screen
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Update view matrix based on camera
        updateViewMatrix();
        
        // Set wireframe mode if enabled
        if (wireframeMode) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    public void endFrame() {
        // Check for OpenGL errors
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            logger.warn("OpenGL error: {}", error);
        }
    }
    
    private void updateProjectionMatrix(int width, int height) {
        float aspectRatio = (float) width / height;
        projectionMatrix.setPerspective((float) Math.toRadians(fieldOfView), aspectRatio, nearPlane, farPlane);
    }
    
    private void updateViewMatrix() {
        viewMatrix.identity()
                .rotateX((float) Math.toRadians(cameraRotation.x))
                .rotateY((float) Math.toRadians(cameraRotation.y))
                .rotateZ((float) Math.toRadians(cameraRotation.z))
                .translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
    }
    
    public void updateProjection(int width, int height) {
        updateProjectionMatrix(width, height);
        glViewport(0, 0, width, height);
    }
    
    // Camera controls
    public void setCameraPosition(float x, float y, float z) {
        cameraPosition.set(x, y, z);
    }
    
    public void setCameraRotation(float x, float y, float z) {
        cameraRotation.set(x, y, z);
    }
    
    public void moveCameraRelative(float forward, float right, float up) {
        // Calculate movement direction based on camera rotation
        float yaw = (float) Math.toRadians(cameraRotation.y);
        float pitch = (float) Math.toRadians(cameraRotation.x);
        
        Vector3f forwardDir = new Vector3f(
            (float) Math.sin(yaw) * (float) Math.cos(pitch),
            (float) -Math.sin(pitch),
            (float) -Math.cos(yaw) * (float) Math.cos(pitch)
        );
        
        Vector3f rightDir = new Vector3f(
            (float) Math.cos(yaw),
            0,
            (float) Math.sin(yaw)
        );
        
        Vector3f upDir = new Vector3f(0, 1, 0);
        
        cameraPosition.add(
            forwardDir.x * forward + rightDir.x * right + upDir.x * up,
            forwardDir.y * forward + rightDir.y * right + upDir.y * up,
            forwardDir.z * forward + rightDir.z * right + upDir.z * up
        );
    }
    
    public void rotateCameraRelative(float deltaX, float deltaY) {
        cameraRotation.y += deltaX;
        cameraRotation.x += deltaY;
        
        // Clamp pitch to prevent flipping
        cameraRotation.x = Math.max(-90, Math.min(90, cameraRotation.x));
        
        // Normalize yaw
        while (cameraRotation.y > 180) cameraRotation.y -= 360;
        while (cameraRotation.y < -180) cameraRotation.y += 360;
    }
    
    // Rendering utilities
    public void renderCube(float x, float y, float z, float size) {
        // This is a placeholder for cube rendering
        // In a real implementation, this would use vertex buffers and shaders
        modelMatrix.identity().translate(x, y, z).scale(size);
        
        // For now, just log the render call
        if (config.isDebugMode()) {
            logger.trace("Rendering cube at ({}, {}, {}) with size {}", x, y, z, size);
        }
    }
    
    public void renderWater(float x, float y, float z, float width, float height) {
        // Placeholder for water rendering
        // This will be implemented with proper water shaders
        modelMatrix.identity().translate(x, y, z).scale(width, 1, height);
        
        if (config.isDebugMode()) {
            logger.trace("Rendering water at ({}, {}, {}) with size {}x{}", x, y, z, width, height);
        }
    }
    
    public void cleanup() {
        logger.info("Cleaning up renderer resources");
        // Cleanup will be implemented when we have actual resources to clean up
    }
    
    // Getters and setters
    public Matrix4f getProjectionMatrix() { return new Matrix4f(projectionMatrix); }
    public Matrix4f getViewMatrix() { return new Matrix4f(viewMatrix); }
    public Matrix4f getModelMatrix() { return new Matrix4f(modelMatrix); }
    
    public Vector3f getCameraPosition() { return new Vector3f(cameraPosition); }
    public Vector3f getCameraRotation() { return new Vector3f(cameraRotation); }
    
    public boolean isWireframeMode() { return wireframeMode; }
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }
    
    public float getFieldOfView() { return fieldOfView; }
    public void setFieldOfView(float fieldOfView) { 
        this.fieldOfView = fieldOfView;
        updateProjectionMatrix(config.getWindowWidth(), config.getWindowHeight());
    }
    
    public float getNearPlane() { return nearPlane; }
    public void setNearPlane(float nearPlane) { 
        this.nearPlane = nearPlane;
        updateProjectionMatrix(config.getWindowWidth(), config.getWindowHeight());
    }
    
    public float getFarPlane() { return farPlane; }
    public void setFarPlane(float farPlane) { 
        this.farPlane = farPlane;
        updateProjectionMatrix(config.getWindowWidth(), config.getWindowHeight());
    }
}