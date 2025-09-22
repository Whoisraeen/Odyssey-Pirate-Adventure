package com.odyssey.rendering;

import com.odyssey.core.GameConfig;
import com.odyssey.util.Logger;
import com.odyssey.util.Timer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Main rendering system for The Odyssey.
 * 
 * This class coordinates all rendering operations, manages the rendering pipeline,
 * and provides high-level rendering functionality for the game world, ocean,
 * ships, and UI elements.
 */
public class Renderer {
    
    private static final Logger logger = Logger.getLogger(Renderer.class);
    
    // OpenGL capabilities
    private GLCapabilities glCapabilities;
    
    // Rendering components
    private ShaderManager shaderManager;
    private TextureManager textureManager;
    private MeshManager meshManager;
    private FramebufferManager framebufferManager;
    
    // Camera and matrices
    private Camera camera;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f viewProjectionMatrix;
    
    // Rendering state
    private boolean initialized = false;
    private int windowWidth;
    private int windowHeight;
    private float aspectRatio;
    
    // Performance tracking
    private int frameCount = 0;
    private int drawCalls = 0;
    private int verticesRendered = 0;
    private Timer performanceTimer;
    
    // Rendering settings
    private boolean wireframeMode = false; // Reserved for future wireframe rendering
    private boolean depthTesting = true; // Reserved for future depth testing control
    private boolean faceCulling = true; // Reserved for future face culling control
    private boolean blending = false;
    private Vector3f clearColor = new Vector3f(0.1f, 0.2f, 0.4f); // Ocean blue
    
    // Render queues
    private List<RenderCommand> opaqueQueue = new ArrayList<>();
    private List<RenderCommand> transparentQueue = new ArrayList<>();
    private List<RenderCommand> uiQueue = new ArrayList<>();
    
    /**
     * Create a new renderer instance.
     */
    public Renderer() {
        this.performanceTimer = new Timer();
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.viewProjectionMatrix = new Matrix4f();
    }
    
    /**
     * Initialize the renderer.
     */
    public void initialize(int windowWidth, int windowHeight) {
        logger.info("Initializing renderer...");
        
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.aspectRatio = (float) windowWidth / windowHeight;
        
        // Initialize OpenGL context
        initializeOpenGL();
        
        // Initialize rendering components
        initializeComponents();
        
        // Set up initial rendering state
        setupRenderingState();
        
        // Create default camera
        camera = new Camera();
        camera.setPosition(0.0f, 10.0f, 0.0f);
        camera.lookAt(new Vector3f(0.0f, 0.0f, -1.0f));
        
        // Update projection matrix
        updateProjectionMatrix();
        
        initialized = true;
        logger.info("Renderer initialized successfully");
        logOpenGLInfo();
    }
    
    /**
     * Initialize OpenGL context and capabilities.
     */
    private void initializeOpenGL() {
        // Create OpenGL capabilities
        glCapabilities = GL.createCapabilities();
        
        if (!glCapabilities.OpenGL33) {
            throw new RuntimeException("OpenGL 3.3 or higher is required");
        }
        
        logger.info("OpenGL context created successfully");
    }
    
    /**
     * Initialize rendering components.
     */
    private void initializeComponents() {
        shaderManager = new ShaderManager();
        textureManager = new TextureManager();
        meshManager = new MeshManager();
        framebufferManager = new FramebufferManager();
        
        shaderManager.initialize();
        textureManager.initialize();
        meshManager.initialize();
        framebufferManager.initialize();
        
        logger.info("Rendering components initialized");
    }
    
    /**
     * Set up initial rendering state.
     */
    private void setupRenderingState() {
        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        
        // Enable face culling
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);
        
        // Set clear color
        GL11.glClearColor(clearColor.x, clearColor.y, clearColor.z, 1.0f);
        
        // Enable seamless cubemap filtering
        if (glCapabilities.GL_ARB_seamless_cube_map) {
            GL11.glEnable(GL43.GL_TEXTURE_CUBE_MAP_SEAMLESS);
        }
        
        // Set viewport
        GL11.glViewport(0, 0, windowWidth, windowHeight);
        
        logger.debug("Initial rendering state configured");
    }
    
    /**
     * Begin a new frame.
     */
    public void beginFrame() {
        performanceTimer.startFrame();
        
        // Reset performance counters
        drawCalls = 0;
        verticesRendered = 0;
        
        // Clear render queues
        opaqueQueue.clear();
        transparentQueue.clear();
        uiQueue.clear();
        
        // Clear buffers
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Update camera matrices
        updateCameraMatrices();
    }
    
    /**
     * End the current frame.
     */
    public void endFrame() {
        // Render all queued commands
        renderQueues();
        
        performanceTimer.endFrame();
        frameCount++;
        
        // Log performance statistics periodically
        if (frameCount % 60 == 0) {
            logPerformanceStats();
        }
    }
    
    /**
     * Render all queued render commands.
     */
    private void renderQueues() {
        // Render opaque objects first
        renderQueue(opaqueQueue, false);
        
        // Render transparent objects (sorted back-to-front)
        sortTransparentQueue();
        renderQueue(transparentQueue, true);
        
        // Render UI elements last
        renderQueue(uiQueue, true);
    }
    
    /**
     * Render a specific queue of render commands.
     */
    private void renderQueue(List<RenderCommand> queue, boolean enableBlending) {
        if (queue.isEmpty()) {
            return;
        }
        
        // Set blending state
        if (enableBlending && !blending) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            blending = true;
        } else if (!enableBlending && blending) {
            GL11.glDisable(GL11.GL_BLEND);
            blending = false;
        }
        
        // Render each command
        for (RenderCommand command : queue) {
            executeRenderCommand(command);
        }
    }
    
    /**
     * Execute a single render command.
     */
    private void executeRenderCommand(RenderCommand command) {
        // Bind shader
        Shader shader = command.getShader();
        if (shader != null) {
            shader.bind();
            
            // Set common uniforms
            shader.setUniform("u_ViewMatrix", viewMatrix);
            shader.setUniform("u_ProjectionMatrix", projectionMatrix);
            shader.setUniform("u_ViewProjectionMatrix", viewProjectionMatrix);
            shader.setUniform("u_CameraPosition", camera.getPosition());
        }
        
        // Bind textures
        List<Texture> textures = command.getTextures();
        for (int i = 0; i < textures.size(); i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            textures.get(i).bind();
        }
        
        // Set model matrix
        Matrix4f modelMatrix = command.getModelMatrix();
        if (shader != null && modelMatrix != null) {
            shader.setUniform("u_ModelMatrix", modelMatrix);
            
            // Calculate and set normal matrix
            Matrix4f normalMatrix = new Matrix4f(modelMatrix).invert().transpose();
            shader.setUniform("u_NormalMatrix", normalMatrix);
        }
        
        // Render mesh
        Mesh mesh = command.getMesh();
        if (mesh != null) {
            mesh.render();
            drawCalls++;
            verticesRendered += mesh.getVertexCount();
        }
        
        // Unbind shader
        if (shader != null) {
            shader.unbind();
        }
    }
    
    /**
     * Sort transparent queue back-to-front for proper alpha blending.
     */
    private void sortTransparentQueue() {
        Vector3f cameraPos = camera.getPosition();
        
        transparentQueue.sort((a, b) -> {
            // Calculate distance from camera to object
            Vector3f posA = a.getWorldPosition();
            Vector3f posB = b.getWorldPosition();
            
            float distA = cameraPos.distanceSquared(posA);
            float distB = cameraPos.distanceSquared(posB);
            
            // Sort back-to-front (larger distance first)
            return Float.compare(distB, distA);
        });
    }
    
    /**
     * Submit a render command to the appropriate queue.
     */
    public void submit(RenderCommand command) {
        switch (command.getRenderQueue()) {
            case OPAQUE:
                opaqueQueue.add(command);
                break;
            case TRANSPARENT:
                transparentQueue.add(command);
                break;
            case UI:
                uiQueue.add(command);
                break;
        }
    }
    
    /**
     * Render the world.
     *
     * @param world The world to render
     * @param camera The camera to render from
     */
    public void render(com.odyssey.world.World world, Camera camera) {
        if (world == null) {
            return;
        }

        // If a camera is provided, use it. Otherwise, use the renderer's internal camera.
        Camera currentCamera = (camera != null) ? camera : this.camera;

        // Update camera matrices
        currentCamera.updateViewMatrix(viewMatrix);
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);

        // Get the list of chunks from the world
        java.util.Collection<com.odyssey.world.Chunk> chunks = world.getChunks();

        // Create a render command for each chunk and submit it to the renderer
        for (com.odyssey.world.Chunk chunk : chunks) {
            if (chunk.getMesh() != null) {
                RenderCommand command = new RenderCommand(
                    chunk.getMesh(),
                    shaderManager.getShader("chunk"), // Assuming a "chunk" shader exists
                    chunk.getModelMatrix()
                );
                command.setRenderQueue(RenderCommand.RenderQueue.OPAQUE);
                submit(command);
            }
        }
    }

    /**
     * Render the UI.
     */
    public void renderUI() {
        // TODO: Implement UI rendering
    }

    /**
     * Update camera matrices.
     */
    private void updateCameraMatrices() {
        camera.updateViewMatrix(viewMatrix);
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
    }
    
    /**
     * Update projection matrix.
     */
    private void updateProjectionMatrix() {
        GameConfig config = GameConfig.getInstance();
        float fov = config.getFloat("graphics.fov", 75.0f);
        float nearPlane = config.getFloat("graphics.nearPlane", 0.1f);
        float farPlane = config.getFloat("graphics.farPlane", 1000.0f);
        
        projectionMatrix.setPerspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
        
        logger.debug("Projection matrix updated - FOV: {}, Aspect: {}, Near: {}, Far: {}", 
                    fov, aspectRatio, nearPlane, farPlane);
    }
    
    /**
     * Handle window resize.
     */
    public void onWindowResize(int newWidth, int newHeight) {
        this.windowWidth = newWidth;
        this.windowHeight = newHeight;
        this.aspectRatio = (float) newWidth / newHeight;
        
        GL11.glViewport(0, 0, newWidth, newHeight);
        updateProjectionMatrix();
        
        // Notify framebuffer manager of resize (method not implemented yet)
        // framebufferManager.onWindowResize(newWidth, newHeight);
        
        logger.info("Renderer resized to {}x{}", newWidth, newHeight);
    }
    
    /**
     * Gets the current wireframe mode setting
     */
    public boolean isWireframeMode() {
        return wireframeMode;
    }
    
    /**
     * Gets the current depth testing setting
     */
    public boolean isDepthTesting() {
        return depthTesting;
    }
    
    /**
     * Gets the current face culling setting
     */
    public boolean isFaceCulling() {
        return faceCulling;
    }
    
    /**
     * Set wireframe mode.
     */
    public void setWireframeMode(boolean enabled) {
        this.wireframeMode = enabled;
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, enabled ? GL11.GL_LINE : GL11.GL_FILL);
        logger.debug("Wireframe mode: {}", enabled);
    }
    
    /**
     * Set clear color.
     */
    public void setClearColor(float r, float g, float b) {
        clearColor.set(r, g, b);
        GL11.glClearColor(r, g, b, 1.0f);
    }
    
    /**
     * Get the current camera.
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Set the camera.
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    /**
     * Get shader manager.
     */
    public ShaderManager getShaderManager() {
        return shaderManager;
    }
    
    /**
     * Get texture manager.
     */
    public TextureManager getTextureManager() {
        return textureManager;
    }
    
    /**
     * Get mesh manager.
     */
    public MeshManager getMeshManager() {
        return meshManager;
    }
    
    /**
     * Get framebuffer manager.
     */
    public FramebufferManager getFramebufferManager() {
        return framebufferManager;
    }
    
    /**
     * Log OpenGL information.
     */
    private void logOpenGLInfo() {
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String version = GL11.glGetString(GL11.GL_VERSION);
        String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        
        logger.info("OpenGL Info:");
        logger.info("  Vendor: {}", vendor);
        logger.info("  Renderer: {}", renderer);
        logger.info("  Version: {}", version);
        logger.info("  GLSL Version: {}", glslVersion);
        
        // Log supported extensions (first few)
        int numExtensions = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
        logger.info("  Extensions: {} supported", numExtensions);
        
        // Check for important extensions
        checkExtensionSupport("GL_ARB_vertex_array_object", glCapabilities.GL_ARB_vertex_array_object);
        checkExtensionSupport("GL_ARB_framebuffer_object", glCapabilities.GL_ARB_framebuffer_object);
        checkExtensionSupport("GL_ARB_texture_float", glCapabilities.GL_ARB_texture_float);
        checkExtensionSupport("GL_ARB_seamless_cube_map", glCapabilities.GL_ARB_seamless_cube_map);
    }
    
    /**
     * Check and log extension support.
     */
    private void checkExtensionSupport(String extensionName, boolean supported) {
        logger.info("  {}: {}", extensionName, supported ? "YES" : "NO");
    }
    
    /**
     * Log performance statistics.
     */
    private void logPerformanceStats() {
        int fps = performanceTimer.getFPS();
        float frameTime = performanceTimer.getFrameTimeMillis();
        
        logger.debug(Logger.PERFORMANCE, 
                    "Rendering Stats - FPS: {}, Frame: {:.2f}ms, Draw Calls: {}, Vertices: {}",
                    fps, frameTime, drawCalls, verticesRendered);
    }
    
    /**
     * Check if renderer is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get current frame count.
     */
    public int getFrameCount() {
        return frameCount;
    }
    
    /**
     * Get current draw call count.
     */
    public int getDrawCalls() {
        return drawCalls;
    }
    
    /**
     * Get current vertices rendered count.
     */
    public int getVerticesRendered() {
        return verticesRendered;
    }
    
    /**
     * Cleanup renderer resources.
     */
    public void cleanup() {
        logger.info("Cleaning up renderer...");
        
        if (shaderManager != null) {
            shaderManager.cleanup();
        }
        
        if (textureManager != null) {
            textureManager.cleanup();
        }
        
        if (meshManager != null) {
            meshManager.cleanup();
        }
        
        if (framebufferManager != null) {
            framebufferManager.cleanup();
        }
        
        initialized = false;
        logger.info("Renderer cleanup complete");
    }
}