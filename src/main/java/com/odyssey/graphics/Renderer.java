package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import com.odyssey.core.jobs.JobSystem;
import com.odyssey.graphics.LightingSystem;
import com.odyssey.graphics.MaterialManager;
import com.odyssey.graphics.Mesh;
import com.odyssey.graphics.Shader;
import com.odyssey.graphics.ShaderManager;
import com.odyssey.graphics.StreamingTextureManager;
import com.odyssey.graphics.TextureAtlasManager;
import com.odyssey.world.ocean.OceanSystem;
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
 * Uses a centralized shader management system for improved performance and maintainability.
 */
public class Renderer {
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);
    
    private final GameConfig config;
    
    // Core rendering systems
    private Camera camera;
    private ShaderManager shaderManager;
    private MaterialManager materialManager;
    private BatchRenderer batchRenderer;
    private TextureAtlasManager textureAtlasManager;
    private LightingSystem lightingSystem;
    private OceanSystem oceanSystem;
    
    // PBR and Deferred Rendering Systems
    private DeferredRenderer deferredRenderer;
    private PostProcessingSystem postProcessingSystem;
    private PBRShader pbrShader;
    
    // Rendering mode
    private boolean useDeferredRendering = true;
    private boolean usePBRMaterials = true;
    
    // Meshes
    private Mesh cubeMesh;
    private Mesh planeMesh;
    
    // Matrices
    private Matrix4f modelMatrix;
    
    // Rendering state
    private boolean wireframeMode = false;
    
    // Lighting
    private Vector3f lightDirection = new Vector3f(0.3f, -0.7f, 0.2f).normalize();
    private Vector3f lightColor = new Vector3f(1.0f, 0.95f, 0.8f);
    private Vector3f ambientColor = new Vector3f(0.3f, 0.4f, 0.6f);
    
    public Renderer(GameConfig config) {
        this.config = config;
        this.camera = new Camera();
        this.modelMatrix = new Matrix4f();
        this.shaderManager = new ShaderManager(config.isDebugMode());
        this.materialManager = new MaterialManager();
        this.batchRenderer = new BatchRenderer();
        
        // Create a temporary JobSystem for StreamingTextureManager
        // This will be replaced when the main JobSystem is available
        JobSystem tempJobSystem = new JobSystem(config);
        StreamingTextureManager streamingTextureManager = new StreamingTextureManager(
            tempJobSystem, 256, 512, 0.8f, true);
        this.textureAtlasManager = new TextureAtlasManager(streamingTextureManager);
        this.lightingSystem = new LightingSystem();
    }
    
    public void initialize() throws Exception {
        logger.info("Initializing renderer...");
        
        // Initialize OpenGL state
        initializeOpenGLState();
        
        // Initialize camera
        initializeCamera();
        
        // Initialize shader system
        initializeShaders();
        
        // Initialize material system
        initializeMaterials();
        
        // Initialize texture atlas system
        initializeTextureAtlases();
        
        // Initialize lighting system
        initializeLighting();
        
        // Initialize ocean system
        initializeOceanSystem();
        
        // Initialize PBR and deferred rendering systems
        initializePBRAndDeferredRendering();
        
        // Initialize meshes
        initializeMeshes();
        
        logger.info("Renderer initialized successfully");
    }
    
    /**
     * Initializes OpenGL rendering state.
     */
    private void initializeOpenGLState() {
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
        
        // Set clear color (ocean blue)
        glClearColor(0.1f, 0.3f, 0.8f, 1.0f);
        
        logger.debug("OpenGL state initialized");
    }
    
    /**
     * Initializes the camera system.
     */
    private void initializeCamera() {
        camera = new Camera();
        camera.setPosition(0.0f, 80.0f, 50.0f);  // Position above and behind the scene
        camera.setRotation(-20.0f, 0.0f, 0.0f);  // Look down slightly
        camera.setFieldOfView(70.0f);
        camera.update(0.0);  // Force immediate update of matrices
        
        // Set initial aspect ratio (will be updated in beginFrame)
        camera.setAspectRatio(1920.0f / 1080.0f);
        
        logger.debug("Camera initialized");
    }
    
    /**
     * Initializes the shader system and loads built-in shaders.
     */
    private void initializeShaders() {
        try {
            // Load all built-in shaders through the shader manager
            shaderManager.loadBuiltinShaders();
            
            logger.info("Shaders initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize shaders", e);
            throw new RuntimeException("Shader initialization failed", e);
        }
    }
    
    /**
     * Initializes the material system with default materials.
     */
    private void initializeMaterials() {
        materialManager.initialize();
    }
    
    /**
     * Initializes the texture atlas system with default textures.
     */
    private void initializeTextureAtlases() {
        textureAtlasManager.preloadDefaultTextures();
    }
    
    /**
     * Initializes the lighting system with default lights.
     */
    private void initializeLighting() {
        lightingSystem.initialize();
    }
    
    /**
      * Initializes the ocean system with advanced water rendering.
      */
     private void initializeOceanSystem() {
         try {
             oceanSystem = new OceanSystem(config, shaderManager);
             oceanSystem.initialize();
             logger.info("Ocean system initialized successfully");
         } catch (Exception e) {
             logger.error("Failed to initialize ocean system", e);
             oceanSystem = null; // Fall back to simple ocean rendering
         }
     }
    
    /**
       * Initializes the PBR and deferred rendering systems.
       */
      private void initializePBRAndDeferredRendering() {
          try {
              if (usePBRMaterials) {
                  pbrShader = new PBRShader();
                  pbrShader.initialize();
                  logger.info("PBR shader system initialized successfully");
              }
              
              if (useDeferredRendering) {
                  deferredRenderer = new DeferredRenderer();
                  deferredRenderer.initialize(config.getWindowWidth(), config.getWindowHeight());
                  logger.info("Deferred rendering system initialized successfully");
              }
              
              postProcessingSystem = new PostProcessingSystem();
              postProcessingSystem.initialize(config.getWindowWidth(), config.getWindowHeight());
              logger.info("Post-processing system initialized successfully");
              
          } catch (Exception e) {
              logger.error("Failed to initialize PBR/deferred rendering systems", e);
              // Fall back to forward rendering
              useDeferredRendering = false;
              usePBRMaterials = false;
          }
      }
    
    private void initializeMeshes() {
        cubeMesh = Mesh.createCube();
        planeMesh = Mesh.createPlane(50.0f);
        logger.info("Meshes initialized successfully");
    }
    
    public void beginFrame(double deltaTime) {
        beginFrame(deltaTime, 0.1f, 0.3f, 0.8f, 1.0f); // Default ocean blue
    }
    
    public void beginFrame(double deltaTime, float clearR, float clearG, float clearB, float clearA) {
        // Check for shader hot-reloading in debug mode
        if (config.isDebugMode()) {
            shaderManager.checkForUpdates();
        }
        
        // Update animated materials
        materialManager.update(deltaTime);
        
        // Update lighting system
        lightingSystem.updateLights(camera.getViewMatrix(), camera.getProjectionMatrix());
        
        // Update viewport
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int width = viewport[2];
        int height = viewport[3];
        
        if (width > 0 && height > 0) {
            camera.setAspectRatio((float) width / height);
            glViewport(0, 0, width, height);
        }
        
        // Set clear color and clear the screen
        glClearColor(clearR, clearG, clearB, clearA);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Update camera
        camera.update(deltaTime);
        
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
    
    public void updateProjection(int width, int height) {
        camera.setAspectRatio((float) width / height);
        glViewport(0, 0, width, height);
    }
    
    /**
     * Handle window resize events
     */
    public void handleResize(int width, int height) {
        updateProjection(width, height);
        logger.debug("Renderer handled resize to {}x{}", width, height);
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
    
    public void renderScene() {
        if (useDeferredRendering && deferredRenderer != null) {
            renderSceneDeferred();
        } else {
            renderSceneForward();
        }
        
        // Render debug information if enabled
        if (config.isDebugMode()) {
            renderDebugInfo();
        }
    }
    
    /**
     * Renders the scene using deferred rendering pipeline.
     */
    private void renderSceneDeferred() {
        // Geometry pass - render to G-Buffer
        deferredRenderer.beginGeometryPass();
        
        // Render ocean with PBR materials
        if (usePBRMaterials && pbrShader != null) {
            renderOceanPBR();
        } else {
            renderOcean();
        }
        
        // Render test cubes with PBR materials
        if (usePBRMaterials && pbrShader != null) {
            renderTestCubesPBR();
        } else {
            renderTestCubes();
        }
        
        deferredRenderer.endGeometryPass();
        
        // Lighting pass
        deferredRenderer.performLightingPass(lightingSystem, camera.getPosition());
        
        // Post-processing pass
        if (postProcessingSystem != null) {
            // Apply bloom effect using the bright texture from deferred renderer
            postProcessingSystem.applyBloom(deferredRenderer.getFinalTexture());
            
            // Apply volumetric lighting (using depth texture and a placeholder shadow map)
            postProcessingSystem.applyVolumetricLighting(deferredRenderer.getDepthTexture(), 0);
            
            // Perform final composition with tonemapping and gamma correction
            postProcessingSystem.performFinalComposition(
                deferredRenderer.getFinalTexture(), 
                0, // bloom texture (would need to be retrieved from bloom system)
                0  // volumetric texture (would need to be retrieved from volumetric system)
            );
        }
        
        // Present final result
        deferredRenderer.presentFinalResult();
    }
    
    /**
     * Renders the scene using forward rendering pipeline (fallback).
     */
    private void renderSceneForward() {
        // Render ocean plane
        renderOcean();
        
        // Render some test cubes for visual feedback
        renderTestCubes();
    }
    
    /**
     * Renders the ocean using PBR materials.
     */
    private void renderOceanPBR() {
        if (oceanSystem != null) {
            // The ocean system should be updated to support PBR materials
            oceanSystem.render(this);
        } else {
            renderSimpleOceanPBR();
        }
    }
    
    /**
     * Renders the ocean using forward rendering.
     */
    private void renderOcean() {
        if (oceanSystem != null) {
            oceanSystem.render(this);
        } else {
            renderSimpleOcean();
        }
    }
    
    /**
     * Renders a simple ocean using basic shader.
     */
    private void renderSimpleOcean() {
        // Use basic shader for forward rendering
        Shader basicShader = shaderManager.getShader("basic");
        if (basicShader == null) {
            logger.warn("Basic shader not found, skipping ocean rendering");
            return;
        }
        
        basicShader.bind();
        
        // Set matrices
        basicShader.setUniform("u_projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("u_viewMatrix", camera.getViewMatrix());
        
        // Set ocean color
        basicShader.setUniform("u_color", new Vector3f(0.1f, 0.3f, 0.8f)); // Ocean blue
        
        // Render ocean plane at sea level
        modelMatrix.identity().translate(0, 64, 0).scale(200);
        basicShader.setUniform("u_modelMatrix", modelMatrix);
        
        planeMesh.render();
        
        basicShader.unbind();
    }
    
    /**
     * Renders test cubes using forward rendering.
     */
    private void renderTestCubes() {
        // Use basic shader for forward rendering
        Shader basicShader = shaderManager.getShader("basic");
        if (basicShader == null) {
            logger.warn("Basic shader not found, skipping test cubes rendering");
            return;
        }
        
        basicShader.bind();
        
        // Set matrices
        basicShader.setUniform("u_projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("u_viewMatrix", camera.getViewMatrix());
        
        // Render a grid of test cubes with different colors
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                modelMatrix.identity().translate(x * 20, 75, z * 20).scale(3);
                basicShader.setUniform("u_modelMatrix", modelMatrix);
                
                // Different colors for different positions
                Vector3f color = new Vector3f(
                    0.5f + (x + 2) / 8.0f,
                    0.5f + (z + 2) / 8.0f,
                    0.7f
                );
                basicShader.setUniform("u_color", color);
                
                cubeMesh.render();
            }
        }
        
        basicShader.unbind();
    }
    
    /**
     * Renders a simple ocean using PBR shader.
     */
    private void renderSimpleOceanPBR() {
        if (pbrShader == null) {
            renderSimpleOcean();
            return;
        }
        
        pbrShader.bind();
        
        // Set matrices
        modelMatrix.identity().translate(0, 64, 0).scale(200);
        pbrShader.setMatrices(modelMatrix, camera.getViewMatrix(), camera.getProjectionMatrix());
        pbrShader.setCameraPosition(camera.getPosition());
        
        // Set lighting parameters
        pbrShader.setLightingParameters(lightingSystem);
        
        // Set ocean material properties
        Vector3f oceanAlbedo = new Vector3f(0.1f, 0.3f, 0.8f); // Ocean blue
        pbrShader.setMaterialFactors(oceanAlbedo, 0.0f, 0.1f, new Vector3f(0.0f), 1.0f);
        
        planeMesh.render();
        
        pbrShader.unbind();
    }
    
    /**
     * Renders test cubes using PBR materials.
     */
    private void renderTestCubesPBR() {
        if (pbrShader == null) {
            renderTestCubes();
            return;
        }
        
        pbrShader.bind();
        
        // Set lighting parameters
        pbrShader.setLightingParameters(lightingSystem);
        pbrShader.setCameraPosition(camera.getPosition());
        
        // Render a grid of test cubes with different PBR materials
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                modelMatrix.identity().translate(x * 20, 75, z * 20).scale(3);
                pbrShader.setMatrices(modelMatrix, camera.getViewMatrix(), camera.getProjectionMatrix());
                
                // Different PBR materials for different positions
                float metallic = (x + 2) / 4.0f;
                float roughness = (z + 2) / 4.0f;
                
                Vector3f albedo = new Vector3f(
                    0.5f + (x + 2) / 8.0f,
                    0.5f + (z + 2) / 8.0f,
                    0.7f
                );
                
                // Set material factors for this cube
                pbrShader.setMaterialFactors(albedo, metallic, Math.max(0.1f, roughness), new Vector3f(0.0f), 1.0f);
                
                cubeMesh.render();
            }
        }
        
        pbrShader.unbind();
    }
    
    private void renderDebugInfo() {
        // Render debug information like camera position, FPS, etc.
        // This could be implemented with a debug UI system later
        if (config.isDebugMode()) {
            Vector3f pos = camera.getPosition();
            logger.debug("Camera position: ({:.2f}, {:.2f}, {:.2f})", pos.x, pos.y, pos.z);
        }
    }
    
    public void cleanup() {
        logger.info("Cleaning up renderer resources...");
        
        // Cleanup new PBR and deferred rendering systems
        if (pbrShader != null) {
            pbrShader.cleanup();
        }
        
        if (deferredRenderer != null) {
            deferredRenderer.cleanup();
        }
        
        if (postProcessingSystem != null) {
            postProcessingSystem.cleanup();
        }
        
        // Cleanup existing systems
        if (batchRenderer != null) {
            batchRenderer.cleanup();
        }
        
        if (lightingSystem != null) {
            lightingSystem.cleanup();
        }
        
        if (oceanSystem != null) {
            oceanSystem.cleanup();
        }
        
        if (textureAtlasManager != null) {
            textureAtlasManager.cleanup();
        }
        
        if (materialManager != null) {
            materialManager.cleanup();
        }
        
        if (shaderManager != null) {
            shaderManager.cleanup();
        }
        
        // Cleanup meshes
        if (cubeMesh != null) {
            cubeMesh.cleanup();
        }
        
        if (planeMesh != null) {
            planeMesh.cleanup();
        }
        
        logger.info("Renderer cleanup completed");
    }
    

    
    // Camera interface
    // Getter methods for existing systems
    public Camera getCamera() {
        return camera;
    }
    
    public ShaderManager getShaderManager() {
        return shaderManager;
    }
    
    public MaterialManager getMaterialManager() {
        return materialManager;
    }
    
    public BatchRenderer getBatchRenderer() {
        return batchRenderer;
    }
    
    public TextureAtlasManager getTextureAtlasManager() {
        return textureAtlasManager;
    }
    
    public LightingSystem getLightingSystem() {
        return lightingSystem;
    }
    
    public OceanSystem getOceanSystem() {
        return oceanSystem;
    }
    
    // Getter methods for new PBR and deferred rendering systems
    public PBRShader getPBRShader() {
        return pbrShader;
    }
    
    public DeferredRenderer getDeferredRenderer() {
        return deferredRenderer;
    }
    
    public PostProcessingSystem getPostProcessingSystem() {
        return postProcessingSystem;
    }
    
    public boolean isUsingDeferredRendering() {
        return useDeferredRendering;
    }
    
    public boolean isUsingPBRMaterials() {
        return usePBRMaterials;
    }
    
    public void setUseDeferredRendering(boolean useDeferredRendering) {
        this.useDeferredRendering = useDeferredRendering;
    }
    
    public void setUsePBRMaterials(boolean usePBRMaterials) {
        this.usePBRMaterials = usePBRMaterials;
    }
    
    public void renderWaterPlane(float x, float y, float z, float width, float height) {
        modelMatrix.identity().translate(x + width/2, y, z + height/2).scale(width, 1, height);
        
        // The actual rendering will be handled by the water shader
        planeMesh.render();
        
        if (config.isDebugMode()) {
            logger.trace("Rendering water plane at ({}, {}, {}) with size {}x{}", x, y, z, width, height);
        }
    }
    
    /**
     * Renders the scene for reflection framebuffer.
     * This renders the scene from a reflected camera position for water reflections.
     */
    public void renderSceneForReflection(Vector3f reflectionCameraPos, Vector3f clippingPlane, float clippingDistance) {
        // Save current camera state
        Vector3f originalPos = new Vector3f(camera.getPosition());
        
        // Set reflection camera position
        camera.setPosition(reflectionCameraPos.x, reflectionCameraPos.y, reflectionCameraPos.z);
        camera.update(0.0); // Force immediate update
        
        // Enable clipping plane to prevent rendering below water
        glEnable(GL_CLIP_DISTANCE0);
        
        // Render the scene (excluding water)
        renderTestCubes(); // Render objects that should be reflected
        
        // Disable clipping
        glDisable(GL_CLIP_DISTANCE0);
        
        // Restore original camera position
        camera.setPosition(originalPos.x, originalPos.y, originalPos.z);
        camera.update(0.0);
    }
    
    /**
     * Renders the scene for refraction framebuffer.
     * This renders the underwater scene for water refraction effects.
     */
    public void renderSceneForRefraction(Vector3f cameraPosition, Vector3f clippingPlane, float clippingDistance) {
        // Enable clipping plane to only render below water
        glEnable(GL_CLIP_DISTANCE0);
        
        // Render underwater objects and terrain
        // For now, just render a darker version of the test cubes to simulate underwater
        renderUnderwater();
        
        // Disable clipping
        glDisable(GL_CLIP_DISTANCE0);
    }
    
    /**
     * Renders underwater scene elements.
     */
    private void renderUnderwater() {
        // Get basic shader from shader manager
        Shader basicShader = shaderManager.getShader("basic");
        if (basicShader == null) {
            return;
        }
        
        basicShader.bind();
        
        // Set uniforms with darker underwater lighting
        basicShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("viewMatrix", camera.getViewMatrix());
        basicShader.setUniform("lightDirection", lightDirection);
        
        // Darker underwater lighting
        Vector3f underwaterLightColor = new Vector3f(lightColor).mul(0.3f);
        Vector3f underwaterAmbientColor = new Vector3f(0.1f, 0.2f, 0.4f);
        
        basicShader.setUniform("lightColor", underwaterLightColor);
        basicShader.setUniform("ambientColor", underwaterAmbientColor);
        
        // Render some underwater objects (simplified for now)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                modelMatrix.identity().translate(x * 30, 50, z * 30).scale(2);
                basicShader.setUniform("modelMatrix", modelMatrix);
                
                // Blue-green underwater tint
                basicShader.setUniform("objectColor", new Vector3f(0.2f, 0.4f, 0.6f));
                
                cubeMesh.render();
            }
        }
        
        basicShader.unbind();
    }
    
    /**
     * Restores the main viewport after framebuffer rendering.
     */
    public void restoreViewport() {
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        glViewport(0, 0, viewport[2], viewport[3]);
    }
    
    // Lighting getters for water shader
    public Vector3f getLightDirection() { return new Vector3f(lightDirection); }
    public Vector3f getLightColor() { return new Vector3f(lightColor); }
    public Vector3f getAmbientColor() { return new Vector3f(ambientColor); }
    

    
    // Getters and setters
    public Matrix4f getModelMatrix() { return new Matrix4f(modelMatrix); }
    
    public boolean isWireframeMode() { return wireframeMode; }
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }
}