package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import com.odyssey.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Cascaded Shadow Maps implementation with multiple frustum ranges and PCF support.
 * Provides high-quality shadows across different distance ranges from the camera.
 */
public class CascadedShadowMap {
    
    private static final Logger logger = Logger.getInstance();
    
    // Shadow map configuration
    private int[] fbos;
    private int[] shadowMaps;
    private int shadowMapSize;
    private int numCascades;
    
    // Cascade configuration
    private float[] cascadeDistances;
    private Matrix4f[] lightSpaceMatrices;
    private float[] cascadeSplits;
    
    // Light configuration
    private Vector3f lightDirection;
    private float shadowDistance;
    private float lambda; // For PSSM (Practical Split Scheme)
    
    // PCF configuration
    private boolean enablePCF;
    private int pcfSamples;
    private float pcfRadius;
    
    /**
     * Creates a new Cascaded Shadow Map system.
     * 
     * @param shadowMapSize Resolution of each shadow map (e.g., 2048)
     * @param numCascades Number of cascade levels (typically 3-4)
     * @param shadowDistance Maximum shadow rendering distance
     */
    public CascadedShadowMap(int shadowMapSize, int numCascades, float shadowDistance) {
        this.shadowMapSize = shadowMapSize;
        this.numCascades = numCascades;
        this.shadowDistance = shadowDistance;
        this.lambda = 0.5f; // Balance between uniform and logarithmic splits
        this.enablePCF = true;
        this.pcfSamples = 16;
        this.pcfRadius = 1.0f;
        
        this.lightDirection = new Vector3f(0.0f, -1.0f, 0.0f);
        this.cascadeDistances = new float[numCascades + 1];
        this.lightSpaceMatrices = new Matrix4f[numCascades];
        this.cascadeSplits = new float[numCascades];
        
        initializeCascadeSplits();
        initializeShadowMaps();
        
        logger.info("Initialized Cascaded Shadow Maps: {}x{} resolution, {} cascades, {} distance", 
                   shadowMapSize, shadowMapSize, numCascades, shadowDistance);
    }
    
    /**
     * Initialize cascade split distances using Practical Split Scheme (PSSM).
     */
    private void initializeCascadeSplits() {
        float nearPlane = 0.1f;
        float farPlane = shadowDistance;
        
        cascadeDistances[0] = nearPlane;
        cascadeDistances[numCascades] = farPlane;
        
        // Calculate cascade splits using PSSM
        for (int i = 1; i < numCascades; i++) {
            float ratio = (float) i / numCascades;
            
            // Uniform split
            float uniformSplit = nearPlane + (farPlane - nearPlane) * ratio;
            
            // Logarithmic split
            float logSplit = nearPlane * (float) Math.pow(farPlane / nearPlane, ratio);
            
            // Practical split (blend of uniform and logarithmic)
            cascadeDistances[i] = lambda * uniformSplit + (1.0f - lambda) * logSplit;
            cascadeSplits[i - 1] = cascadeDistances[i];
        }
        cascadeSplits[numCascades - 1] = farPlane;
        
        logger.debug("Cascade splits: {}", java.util.Arrays.toString(cascadeSplits));
    }
    
    /**
     * Initialize shadow map framebuffers and textures.
     */
    private void initializeShadowMaps() {
        fbos = new int[numCascades];
        shadowMaps = new int[numCascades];
        
        GL30.glGenFramebuffers(fbos);
        GL11.glGenTextures(shadowMaps);
        
        for (int i = 0; i < numCascades; i++) {
            // Create depth texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMaps[i]);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, 
                             shadowMapSize, shadowMapSize, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
            
            // Set texture parameters for PCF
            if (enablePCF) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            }
            
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_BORDER);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_BORDER);
            
            // Set border color to white (no shadow)
            float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
            GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);
            
            // Create framebuffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i]);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 
                                       GL11.GL_TEXTURE_2D, shadowMaps[i], 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            
            // Check framebuffer completeness
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                logger.error("Shadow map framebuffer {} is not complete!", i);
            }
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Update light space matrices for all cascades based on camera and light direction.
     * 
     * @param camera The camera to calculate frustums from
     * @param lightDir The directional light direction
     */
    public void updateLightSpaceMatrices(Camera camera, Vector3f lightDir) {
        this.lightDirection = new Vector3f(lightDir);
        
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix();
        Matrix4f invViewProj = new Matrix4f(projectionMatrix).mul(viewMatrix).invert();
        
        for (int i = 0; i < numCascades; i++) {
            float nearPlane = (i == 0) ? cascadeDistances[0] : cascadeDistances[i];
            float farPlane = cascadeDistances[i + 1];
            
            // Calculate frustum corners in world space
            List<Vector3f> frustumCorners = calculateFrustumCorners(invViewProj, nearPlane, farPlane);
            
            // Calculate light space matrix for this cascade
            lightSpaceMatrices[i] = calculateLightSpaceMatrix(frustumCorners, lightDir);
        }
    }
    
    /**
     * Calculate frustum corners in world space for a given near/far plane.
     */
    private List<Vector3f> calculateFrustumCorners(Matrix4f invViewProj, float nearPlane, float farPlane) {
        List<Vector3f> corners = new ArrayList<>();
        
        // NDC coordinates for frustum corners
        float[] ndcCorners = {
            -1.0f, -1.0f, -1.0f, // Near bottom-left
            1.0f, -1.0f, -1.0f,  // Near bottom-right
            1.0f, 1.0f, -1.0f,   // Near top-right
            -1.0f, 1.0f, -1.0f,  // Near top-left
            -1.0f, -1.0f, 1.0f,  // Far bottom-left
            1.0f, -1.0f, 1.0f,   // Far bottom-right
            1.0f, 1.0f, 1.0f,    // Far top-right
            -1.0f, 1.0f, 1.0f    // Far top-left
        };
        
        // Transform NDC corners to world space
        for (int i = 0; i < 8; i++) {
            Vector4f corner = new Vector4f(ndcCorners[i * 3], ndcCorners[i * 3 + 1], ndcCorners[i * 3 + 2], 1.0f);
            corner.mul(invViewProj);
            corner.div(corner.w); // Perspective divide
            corners.add(new Vector3f(corner.x, corner.y, corner.z));
        }
        
        // Adjust near and far plane distances
        Vector3f center = new Vector3f();
        for (Vector3f corner : corners) {
            center.add(corner);
        }
        center.div(8.0f);
        
        // Scale frustum based on cascade distances
        for (int i = 0; i < 4; i++) {
            Vector3f nearCorner = corners.get(i);
            Vector3f farCorner = corners.get(i + 4);
            
            // Interpolate between original near/far based on cascade distances
            Vector3f direction = new Vector3f(farCorner).sub(nearCorner);
            float length = direction.length();
            direction.normalize();
            
            // Adjust corners to cascade-specific distances
            corners.set(i, new Vector3f(nearCorner).add(new Vector3f(direction).mul(nearPlane)));
            corners.set(i + 4, new Vector3f(nearCorner).add(new Vector3f(direction).mul(farPlane)));
        }
        
        return corners;
    }
    
    /**
     * Calculate light space matrix for a set of frustum corners.
     */
    private Matrix4f calculateLightSpaceMatrix(List<Vector3f> frustumCorners, Vector3f lightDir) {
        // Calculate frustum center
        Vector3f center = new Vector3f();
        for (Vector3f corner : frustumCorners) {
            center.add(corner);
        }
        center.div(frustumCorners.size());
        
        // Create light view matrix
        Vector3f lightPos = new Vector3f(center).sub(new Vector3f(lightDir).mul(50.0f));
        Vector3f up = Math.abs(lightDir.y) > 0.9f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, center, up);
        
        // Transform frustum corners to light space
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        
        for (Vector3f corner : frustumCorners) {
            Vector4f lightSpaceCorner = new Vector4f(corner, 1.0f);
            lightSpaceCorner.mul(lightView);
            
            minX = Math.min(minX, lightSpaceCorner.x);
            maxX = Math.max(maxX, lightSpaceCorner.x);
            minY = Math.min(minY, lightSpaceCorner.y);
            maxY = Math.max(maxY, lightSpaceCorner.y);
            minZ = Math.min(minZ, lightSpaceCorner.z);
            maxZ = Math.max(maxZ, lightSpaceCorner.z);
        }
        
        // Extend Z range to include potential shadow casters
        float zExtension = (maxZ - minZ) * 0.5f;
        minZ -= zExtension;
        maxZ += zExtension;
        
        // Create orthographic projection matrix
        Matrix4f lightProjection = new Matrix4f().ortho(minX, maxX, minY, maxY, minZ, maxZ);
        
        return new Matrix4f(lightProjection).mul(lightView);
    }
    
    /**
     * Bind shadow map for rendering to a specific cascade.
     */
    public void bind(int cascade) {
        if (cascade < 0 || cascade >= numCascades) {
            logger.warn("Invalid cascade index: {}", cascade);
            return;
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[cascade]);
        GL11.glViewport(0, 0, shadowMapSize, shadowMapSize);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        
        // Enable depth testing and disable color writes
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glColorMask(false, false, false, false);
    }
    
    /**
     * Unbind shadow map framebuffer.
     */
    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glColorMask(true, true, true, true);
    }
    
    /**
     * Get shadow map texture for a specific cascade.
     */
    public int getShadowMap(int cascade) {
        if (cascade < 0 || cascade >= numCascades) {
            logger.warn("Invalid cascade index: {}", cascade);
            return 0;
        }
        return shadowMaps[cascade];
    }
    
    /**
     * Get light space matrix for a specific cascade.
     */
    public Matrix4f getLightSpaceMatrix(int cascade) {
        if (cascade < 0 || cascade >= numCascades) {
            logger.warn("Invalid cascade index: {}", cascade);
            return new Matrix4f();
        }
        return new Matrix4f(lightSpaceMatrices[cascade]);
    }
    
    /**
     * Get all light space matrices.
     */
    public Matrix4f[] getLightSpaceMatrices() {
        Matrix4f[] matrices = new Matrix4f[numCascades];
        for (int i = 0; i < numCascades; i++) {
            matrices[i] = new Matrix4f(lightSpaceMatrices[i]);
        }
        return matrices;
    }
    
    /**
     * Get cascade split distances.
     */
    public float[] getCascadeSplits() {
        return cascadeSplits.clone();
    }
    
    /**
     * Get cascade distances (including near and far).
     */
    public float[] getCascadeDistances() {
        return cascadeDistances.clone();
    }
    
    // Getters and setters
    public int getNumCascades() { return numCascades; }
    public int getShadowMapSize() { return shadowMapSize; }
    public float getShadowDistance() { return shadowDistance; }
    public boolean isPCFEnabled() { return enablePCF; }
    public int getPCFSamples() { return pcfSamples; }
    public float getPCFRadius() { return pcfRadius; }
    public float getLambda() { return lambda; }
    
    public void setShadowDistance(float shadowDistance) {
        this.shadowDistance = shadowDistance;
        initializeCascadeSplits();
    }
    
    public void setPCFEnabled(boolean enablePCF) {
        this.enablePCF = enablePCF;
        // Reinitialize shadow maps with new filtering
        cleanup();
        initializeShadowMaps();
    }
    
    public void setPCFSamples(int pcfSamples) {
        this.pcfSamples = Math.max(1, Math.min(64, pcfSamples));
    }
    
    public void setPCFRadius(float pcfRadius) {
        this.pcfRadius = Math.max(0.1f, pcfRadius);
    }
    
    public void setLambda(float lambda) {
        this.lambda = Math.max(0.0f, Math.min(1.0f, lambda));
        initializeCascadeSplits();
    }
    
    /**
     * Clean up OpenGL resources.
     */
    public void cleanup() {
        if (fbos != null) {
            GL30.glDeleteFramebuffers(fbos);
        }
        if (shadowMaps != null) {
            GL11.glDeleteTextures(shadowMaps);
        }
        logger.debug("Cleaned up Cascaded Shadow Maps");
    }
}
