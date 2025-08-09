package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import com.odyssey.core.jobs.JobSystem;
// Keep imports minimal
import com.odyssey.world.ocean.OceanSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;
// rely on higher-level API calls; no direct GL15/GL20 references needed
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL11;

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
    private SkyRenderer skyRenderer;
    private VolumetricCloudRenderer volumetricCloudRenderer;
    private TimeOfDaySystem currentTimeOfDaySystem;
    
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
    
    // Framebuffer efficiency - cache framebuffer state to avoid redundant operations
    private boolean needsDepthCopy = true;
    
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
        
        // Initialize sky renderer
        initializeSkyRenderer();
        
        // Initialize volumetric cloud renderer
        initializeVolumetricCloudRenderer();
        
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

        // Enable sRGB framebuffer for correct gamma when presenting to default FBO
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
        
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
    
    /**
     * Initializes the sky renderer.
     */
    private void initializeSkyRenderer() {
        try {
            skyRenderer = new SkyRenderer(shaderManager);
            skyRenderer.initialize();
            logger.info("Sky renderer initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize sky renderer", e);
            skyRenderer = null; // Fall back to no sky rendering
        }
    }
    
    /**
     * Initializes the volumetric cloud renderer.
     */
    private void initializeVolumetricCloudRenderer() {
        try {
            volumetricCloudRenderer = new VolumetricCloudRenderer();
            // Use default resolution, will be updated in handleResize
            volumetricCloudRenderer.initialize(1920, 1080);
            logger.info("Volumetric cloud renderer initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize volumetric cloud renderer", e);
            volumetricCloudRenderer = null; // Fall back to no cloud rendering
        }
    }
    
    public void beginFrame(double deltaTime) {
        beginFrame(deltaTime, 0.1f, 0.3f, 0.8f, 1.0f); // Default ocean blue
    }
    
    public void beginFrame(double deltaTime, float clearR, float clearG, float clearB, float clearA) {
        // Reset framebuffer efficiency flags for new frame
        needsDepthCopy = true;
        
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
            camera.setScreenWidth(width);
            camera.setScreenHeight(height);
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
        
        // Resize deferred renderer framebuffers
        if (deferredRenderer != null) {
            deferredRenderer.resize(width, height);
        }
        
        // Resize post-processing system framebuffers
        if (postProcessingSystem != null) {
            postProcessingSystem.resize(width, height);
        }
        
        // Reinitialize volumetric cloud renderer with new resolution
        if (volumetricCloudRenderer != null) {
            try {
                volumetricCloudRenderer.cleanup();
                volumetricCloudRenderer.initialize(width, height);
            } catch (Exception e) {
                logger.error("Failed to resize volumetric cloud renderer", e);
            }
        }
        
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
        // Geometry pass - render to G-Buffer (solid geometry only)
        deferredRenderer.beginGeometryPass();
        
        // Render ocean with PBR materials
        if (usePBRMaterials && pbrShader != null) {
            renderOceanPBR();
        } else {
            renderOcean();
        }
        
        // Test cubes removed - ready for world generation
        
        deferredRenderer.endGeometryPass();
        
        // SSAO pass - generate ambient occlusion
        deferredRenderer.performSSAOPass(camera.getProjectionMatrix(), camera.getViewMatrix());
        
        // Lighting pass
        deferredRenderer.performLightingPass(lightingSystem, camera.getPosition());
        
        // TAA pass - apply temporal anti-aliasing after lighting
        if (deferredRenderer.isTAAEnabled()) {
            deferredRenderer.performTAAPass(camera, deferredRenderer.getColorTexture());
        }
        
        // Present final result with underwater effects support
        if (postProcessingSystem != null) {
            // Get sun direction from lighting system
            Vector3f sunDirection = new Vector3f(0.0f, -1.0f, 0.5f).normalize(); // Default sun direction
            if (lightingSystem != null && !lightingSystem.getDirectionalLights().isEmpty()) {
                sunDirection = lightingSystem.getDirectionalLights().get(0).getDirection();
            }
            
            // Get current time for animated effects
            float time = System.currentTimeMillis() / 1000.0f;
            
            deferredRenderer.presentFinalResult(postProcessingSystem, camera, sunDirection, time);
        } else {
            // Fallback to standard post-processing
            deferredRenderer.presentFinalResult();
        }
        
        // Render sky as overlay on final framebuffer (after deferred pipeline)
        // This ensures sky renders to the screen, not the G-Buffer
        if (skyRenderer != null && currentTimeOfDaySystem != null) {
            // Copy depth buffer from G-Buffer to default framebuffer for proper depth testing
            // Only copy when sky renderer is active and time of day system is available
            copyDepthBuffer();
            
            // Render sky with proper depth testing
            skyRenderer.render(camera.getViewMatrix(), camera.getProjectionMatrix(), currentTimeOfDaySystem);
        }
        
        // Render volumetric clouds after sky
        if (volumetricCloudRenderer != null) {
            // Get depth texture from deferred renderer's G-Buffer
            int depthTexture = (deferredRenderer != null) ? deferredRenderer.getDepthTexture() : 0;
            float deltaTime = 0.016f; // Approximate 60 FPS for now
            volumetricCloudRenderer.render(
                camera.getViewMatrix(),
                camera.getProjectionMatrix(),
                camera.getPosition(),
                depthTexture,
                deltaTime
            );
        }
    }
    
    /**
     * Renders the scene using forward rendering pipeline (fallback).
     */
    private void renderSceneForward() {
        // Render sky first (background)
        if (skyRenderer != null) {
            skyRenderer.render(camera.getViewMatrix(), camera.getProjectionMatrix(), currentTimeOfDaySystem);
        }
        
        // Render volumetric clouds after sky
        if (volumetricCloudRenderer != null) {
            // No depth texture available in forward rendering
            int depthTexture = 0;
            float deltaTime = 0.016f; // Approximate 60 FPS for now
            volumetricCloudRenderer.render(
                camera.getViewMatrix(),
                camera.getProjectionMatrix(),
                camera.getPosition(),
                depthTexture,
                deltaTime
            );
        }
        
        // Render ocean plane
        renderOcean();
        
        // Test cubes removed - ready for world generation
    }
    
    /**
     * Renders the ocean using PBR materials.
     */
    private void renderOceanPBR() {
        // Use the enhanced ocean system with advanced water shader
        if (oceanSystem != null) {
            oceanSystem.render(this);
        } else {
            // Fallback to simple PBR ocean if ocean system is not available
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
        
        logger.debug("Rendering simple ocean with basic shader");
        
        basicShader.bind();
        
        // Set matrices
        basicShader.setUniform("u_projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("u_viewMatrix", camera.getViewMatrix());
        
        // Set ocean color
        basicShader.setUniform("u_color", new Vector3f(0.1f, 0.3f, 0.8f)); // Ocean blue
        
        // Render ocean plane at sea level
        modelMatrix.identity().translate(0, 64, 0).scale(200);
        basicShader.setUniform("u_modelMatrix", modelMatrix);
        
        logger.debug("Ocean plane position: y=64, scale=200, camera position: {}", camera.getPosition());
        
        planeMesh.render();
        
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
     * Copies the depth buffer from G-Buffer to the default framebuffer.
     * This allows sky rendering to properly depth test against rendered geometry.
     * Uses caching to avoid redundant operations.
     */
    private void copyDepthBuffer() {
        if (deferredRenderer == null || !needsDepthCopy) return;
        
        try {
            // Get window dimensions
            int width = config.getWindowWidth();
            int height = config.getWindowHeight();
            
            // Bind G-Buffer as read framebuffer
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, deferredRenderer.getGBufferFBO());
            
            // Bind default framebuffer as draw framebuffer
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
            
            // Copy depth buffer
            GL30.glBlitFramebuffer(
                0, 0, width, height,  // Source rectangle
                0, 0, width, height,  // Destination rectangle
                GL11.GL_DEPTH_BUFFER_BIT,  // Buffer to copy
                GL11.GL_NEAREST  // Filter
            );
            
            // Restore default framebuffer binding
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            
            // Mark depth copy as completed for this frame
            needsDepthCopy = false;
            
        } catch (Exception e) {
            logger.warn("Failed to copy depth buffer for sky rendering: {}", e.getMessage());
        }
    }
    
    private void renderDebugInfo() {
        // Render debug information like camera position, FPS, etc.
        // This could be implemented with a debug UI system later
        if (config.isDebugMode()) {
            Vector3f pos = camera.getPosition();
            logger.debug("Camera position: ({}, {}, {})", 
                String.format("%.2f", pos.x), String.format("%.2f", pos.y), String.format("%.2f", pos.z));
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
        
        if (skyRenderer != null) {
            skyRenderer.cleanup();
        }
        
        if (volumetricCloudRenderer != null) {
            volumetricCloudRenderer.cleanup();
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
    
    public SkyRenderer getSkyRenderer() {
        return skyRenderer;
    }
    
    public VolumetricCloudRenderer getVolumetricCloudRenderer() {
        return volumetricCloudRenderer;
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
        // Check if we're in deferred rendering mode and use appropriate shader
        if (useDeferredRendering && pbrShader != null) {
            // Use PBR shader for deferred rendering
            pbrShader.bind();
            
            modelMatrix.identity().translate(x + width/2, y, z + height/2).scale(width, 1, height);
            pbrShader.setMatrices(modelMatrix, camera.getViewMatrix(), camera.getProjectionMatrix());
            pbrShader.setCameraPosition(camera.getPosition());
            pbrShader.setLightingParameters(lightingSystem);
            
            // Set ocean material properties
            Vector3f oceanAlbedo = new Vector3f(0.1f, 0.3f, 0.8f); // Ocean blue
            pbrShader.setMaterialFactors(oceanAlbedo, 0.0f, 0.1f, new Vector3f(0.0f), 1.0f);
            
            planeMesh.render();
            pbrShader.unbind();
        } else {
            // Use basic shader for forward rendering
            Shader basicShader = shaderManager.getShader("basic");
            if (basicShader != null) {
                basicShader.bind();
                
                modelMatrix.identity().translate(x + width/2, y, z + height/2).scale(width, 1, height);
                basicShader.setUniform("u_projectionMatrix", camera.getProjectionMatrix());
                basicShader.setUniform("u_viewMatrix", camera.getViewMatrix());
                basicShader.setUniform("u_modelMatrix", modelMatrix);
                basicShader.setUniform("u_color", new Vector3f(0.1f, 0.3f, 0.8f)); // Ocean blue
                
                planeMesh.render();
                basicShader.unbind();
            } else {
                // Fallback: just render without shader (this was the original problem)
                modelMatrix.identity().translate(x + width/2, y, z + height/2).scale(width, 1, height);
                planeMesh.render();
            }
        }
        
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
        // Test cubes removed - ready for world generation
        
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
        
        // Set uniforms with correct names for basic shader
        basicShader.setUniform("u_projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("u_viewMatrix", camera.getViewMatrix());
        
        // Render some underwater objects (simplified for now)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                modelMatrix.identity().translate(x * 30, 50, z * 30).scale(2);
                basicShader.setUniform("u_modelMatrix", modelMatrix);
                
                // Blue-green underwater tint
                basicShader.setUniform("u_color", new Vector3f(0.2f, 0.4f, 0.6f));
                
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
    
    // Mesh getters for advanced rendering systems
    public Mesh getPlaneMesh() { return planeMesh; }
    public Mesh getCubeMesh() { return cubeMesh; }
    
    /**
     * Updates celestial lighting based on time of day system.
     * @param timeOfDaySystem The time of day system providing celestial data
     */
    public void updateCelestialLighting(TimeOfDaySystem timeOfDaySystem) {
        if (lightingSystem != null) {
            lightingSystem.updateCelestialLights(timeOfDaySystem);
        }
        
        // Store time of day system for sky rendering
        this.currentTimeOfDaySystem = timeOfDaySystem;
        
        // Update legacy lighting variables for compatibility with existing shaders
        lightDirection.set(timeOfDaySystem.getSunDirection());
        lightColor.set(timeOfDaySystem.getSunColor());
        ambientColor.set(timeOfDaySystem.getAmbientColor());
    }
    

    
    // Getters and setters
    public Matrix4f getModelMatrix() { return new Matrix4f(modelMatrix); }
    
    public boolean isWireframeMode() { return wireframeMode; }
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }
}