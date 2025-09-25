package com.odyssey.rendering;

import com.odyssey.core.GameConfig;
import com.odyssey.core.TimeOfDaySystem;
import com.odyssey.ui.LoadGameMenu;
import com.odyssey.ui.MainMenu;
import com.odyssey.util.Logger;
import com.odyssey.util.Timer;
import com.odyssey.rendering.ComputeShaderManager;
import com.odyssey.rendering.GraphicsSettings;

import com.odyssey.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

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

    // Rendering components
    private WaterRenderer waterRenderer;
    private AmbientOcclusionRenderer aoRenderer;
    private TextureAtlas textureAtlas;
    private PostProcessingRenderer postProcessingRenderer;
    private TextRenderer textRenderer;
    private ShadowMap shadowMap;
    private Skybox skybox;
    private IBL ibl;
    private VolumetricFog volumetricFog;
    private VolumetricClouds volumetricClouds;
    private CascadedShadowMap cascadedShadowMap;
    private DynamicSky dynamicSky;
    private HosekWilkieSky hosekWilkieSky;
    private TimeOfDaySystem timeOfDaySystem;
    private GraphicsSettings graphicsSettings;
    private ComputeShaderManager computeShaderManager;

    // Camera and matrices
    private Camera camera;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f viewProjectionMatrix;

    // World
    private World world;

    // Rendering state
    private boolean initialized = false;
    private int windowWidth;
    private int windowHeight;
    private float aspectRatio;

    private int quadVAO;

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

    // Advanced Rendering Settings
    private boolean waterRenderingEnabled = true;
    private boolean ambientOcclusionEnabled = true;
    private boolean postProcessingEnabled = true;
    private boolean voxelAOEnabled = true;
    private int waterQuality = 2; // 0=Low, 1=Medium, 2=High, 3=Ultra
    private int aoQuality = 1; // 0=Low, 1=Medium, 2=High
    private float renderDistance = 256.0f;

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
        framebufferManager.createColorFramebuffer("scene", windowWidth, windowHeight);

        // Initialize advanced rendering components
        waterRenderer = new WaterRenderer();
        aoRenderer = new AmbientOcclusionRenderer();
        textureAtlas = new TextureAtlas();
        postProcessingRenderer = new PostProcessingRenderer();
        textRenderer = new TextRenderer();

        waterRenderer.initialize(null, null); // Physics systems will be set later
        aoRenderer.initialize(null, windowWidth, windowHeight); // World will be set later
        textureAtlas.generateAtlas();
        postProcessingRenderer.initialize(windowWidth, windowHeight);
        textRenderer.initialize();
        shadowMap = new ShadowMap(2048, 2048);
        skybox = new Skybox();
        ibl = new IBL();
        volumetricFog = new VolumetricFog();
        volumetricFog.initialize();
        volumetricClouds = new VolumetricClouds();
        volumetricClouds.initialize();
        cascadedShadowMap = new CascadedShadowMap(2048, 3);
        dynamicSky = new DynamicSky();
        hosekWilkieSky = new HosekWilkieSky();
        timeOfDaySystem = new TimeOfDaySystem();
        timeOfDaySystem.initialize();
        graphicsSettings = new GraphicsSettings();
        graphicsSettings.applyPreset(GraphicsSettings.QualityPreset.MEDIUM);
        computeShaderManager = new ComputeShaderManager();
        computeShaderManager.initialize();

        shaderManager.loadShader("opaque", "shaders/opaque.vert", "shaders/opaque.frag");
        shaderManager.loadShader("quad", "shaders/quad.vert", "shaders/quad.frag");

        // Load PBR textures
        loadPBRTextures();

        logger.info("Rendering components initialized");
    }

    /**
     * Load PBR textures for physically-based rendering.
     */
    private void loadPBRTextures() {
        logger.info("Loading PBR textures...");
        
        // Load default PBR textures - create fallback textures if files don't exist
        try {
            // Try to load actual texture files first
            textureManager.loadTexture("albedoMap", "textures/pbr/default_albedo.png");
        } catch (Exception e) {
            // Create default white albedo texture
            logger.warn("Could not load albedo texture, using default white texture");
            textureManager.getWhiteTexture(); // This will be used as fallback
        }
        
        try {
            textureManager.loadTexture("normalMap", "textures/pbr/default_normal.png");
        } catch (Exception e) {
            // Create default normal map (flat normal pointing up)
            logger.warn("Could not load normal texture, creating default normal map");
            createDefaultNormalMap();
        }
        
        try {
            textureManager.loadTexture("metallicMap", "textures/pbr/default_metallic.png");
        } catch (Exception e) {
            // Create default metallic texture (non-metallic)
            logger.warn("Could not load metallic texture, using default black texture");
            textureManager.getBlackTexture();
        }
        
        try {
            textureManager.loadTexture("roughnessMap", "textures/pbr/default_roughness.png");
        } catch (Exception e) {
            // Create default roughness texture (medium roughness)
            logger.warn("Could not load roughness texture, creating default roughness map");
            createDefaultRoughnessMap();
        }
        
        try {
            textureManager.loadTexture("aoMap", "textures/pbr/default_ao.png");
        } catch (Exception e) {
            // Create default AO texture (no occlusion)
            logger.warn("Could not load AO texture, using default white texture");
            textureManager.getWhiteTexture();
        }
        
        logger.info("PBR textures loaded successfully");
    }
    
    /**
     * Create a default normal map texture (flat normal pointing up).
     */
    private void createDefaultNormalMap() {
        java.nio.ByteBuffer normalData = java.nio.ByteBuffer.allocateDirect(4);
        normalData.put((byte) 128).put((byte) 128).put((byte) 255).put((byte) 255); // Normal pointing up (0.5, 0.5, 1.0)
        normalData.flip();
        textureManager.createTexture("normalMap", 1, 1, GL_RGBA, normalData);
    }
    
    /**
     * Create a default roughness map texture (medium roughness).
     */
    private void createDefaultRoughnessMap() {
        java.nio.ByteBuffer roughnessData = java.nio.ByteBuffer.allocateDirect(4);
        roughnessData.put((byte) 128).put((byte) 128).put((byte) 128).put((byte) 255); // Medium roughness (0.5)
        roughnessData.flip();
        textureManager.createTexture("roughnessMap", 1, 1, GL_RGBA, roughnessData);
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
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Update camera matrices
        updateCameraMatrices();
    }

    /**
     * End the current frame.
     */
    public void endFrame() {
        // Render all queued commands
        renderQueues();

        // Apply post-processing
        int finalTexture = postProcessingRenderer.processScene(
                framebufferManager.getFramebuffer("scene").getColorTexture(0),
                framebufferManager.getFramebuffer("scene").getDepthTexture(),
                performanceTimer.getDeltaTime()
        );

        // Render final texture to screen
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Shader quadShader = shaderManager.getShader("quad");
        if (quadShader != null) {
            quadShader.bind();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, finalTexture);

            renderFullscreenQuad();
        }

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
        // Set common uniforms
        Shader shader = command.getShader();
        if (shader != null) {
            shader.setUniform("u_ViewMatrix", viewMatrix);
            shader.setUniform("u_ProjectionMatrix", projectionMatrix);
            shader.setUniform("u_ViewProjectionMatrix", viewProjectionMatrix);
            shader.setUniform("u_CameraPosition", camera.getPosition());

            // Set shadow map uniforms
            shader.setUniform("shadowMap", 1);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMap());
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
    public void render(World world, Camera camera) {
        if (world == null) {
            return;
        }

        // If a camera is provided, use it. Otherwise, use the renderer's internal camera.
        Camera currentCamera = (camera != null) ? camera : this.camera;

        // Update camera matrices
        currentCamera.updateViewMatrix(viewMatrix);
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);

        // Calculate light direction (for now, use a fixed directional light)
        Vector3f lightDirection = new Vector3f(-0.3f, -0.7f, -0.2f).normalize();

        // Render shadow maps first
        renderShadowMap(world, currentCamera, projectionMatrix, viewMatrix, lightDirection);

        // Bind scene framebuffer
        framebufferManager.getFramebuffer("scene").bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Render world chunks with CSM PBR shader
        Shader csmPbrShader = shaderManager.getShader("csm_pbr");
        if (csmPbrShader == null) {
            // Fallback to regular PBR shader if CSM PBR is not available
            csmPbrShader = shaderManager.getShader("pbr");
        }
        
        csmPbrShader.bind();
        csmPbrShader.setUniform("projection", projectionMatrix);
        csmPbrShader.setUniform("view", viewMatrix);
        csmPbrShader.setUniform("camPos", currentCamera.getPosition());

        // Set light uniforms
        for (int i = 0; i < 4; i++) {
            csmPbrShader.setUniform("lightPositions[" + i + "]", new Vector3f(0.0f, 10.0f, 0.0f));
            csmPbrShader.setUniform("lightColors[" + i + "]", new Vector3f(1.0f, 1.0f, 1.0f));
        }

        // Set CSM uniforms
        csmPbrShader.setUniform("numCascades", cascadedShadowMap.getNumCascades());
        csmPbrShader.setUniform("enablePCF", cascadedShadowMap.isEnablePCF());
        csmPbrShader.setUniform("pcfSamples", cascadedShadowMap.getPcfSamples());
        csmPbrShader.setUniform("pcfRadius", cascadedShadowMap.getPcfRadius());
        csmPbrShader.setUniform("shadowBias", 0.005f);
        
        // Set cascade splits and light space matrices
        float[] cascadeSplits = cascadedShadowMap.getCascadeSplits();
        Matrix4f[] lightSpaceMatrices = cascadedShadowMap.getLightSpaceMatrices();
        
        for (int i = 0; i < cascadedShadowMap.getNumCascades(); i++) {
            csmPbrShader.setUniform("cascadeSplits[" + i + "]", cascadeSplits[i]);
            csmPbrShader.setUniform("lightSpaceMatrices[" + i + "]", lightSpaceMatrices[i]);
        }

        // Bind PBR textures
        glActiveTexture(GL_TEXTURE0);
        textureManager.getTexture("albedoMap").bind();
        csmPbrShader.setUniform("albedoMap", 0);
        
        glActiveTexture(GL_TEXTURE1);
        textureManager.getTexture("normalMap").bind();
        csmPbrShader.setUniform("normalMap", 1);
        
        glActiveTexture(GL_TEXTURE2);
        textureManager.getTexture("metallicMap").bind();
        csmPbrShader.setUniform("metallicMap", 2);
        
        glActiveTexture(GL_TEXTURE3);
        textureManager.getTexture("roughnessMap").bind();
        csmPbrShader.setUniform("roughnessMap", 3);
        
        glActiveTexture(GL_TEXTURE4);
        textureManager.getTexture("aoMap").bind();
        csmPbrShader.setUniform("aoMap", 4);

        // Bind IBL textures
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ibl.getIrradianceMap());
        csmPbrShader.setUniform("irradianceMap", 5);
        
        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ibl.getPrefilterMap());
        csmPbrShader.setUniform("prefilterMap", 6);
        
        glActiveTexture(GL_TEXTURE7);
        glBindTexture(GL_TEXTURE_2D, ibl.getBrdfLUTTexture());
        csmPbrShader.setUniform("brdfLUT", 7);

        // Bind shadow map textures
        for (int i = 0; i < cascadedShadowMap.getNumCascades(); i++) {
            glActiveTexture(GL_TEXTURE8 + i);
            glBindTexture(GL_TEXTURE_2D, cascadedShadowMap.getShadowMap(i));
            csmPbrShader.setUniform("shadowMaps[" + i + "]", 8 + i);
        }

        java.util.Collection<com.odyssey.world.Chunk> chunks = world.getChunks();
        for (com.odyssey.world.Chunk chunk : chunks) {
            if (chunk.getMesh() != null) {
                csmPbrShader.setUniform("model", chunk.getModelMatrix());
                chunk.getMesh().render();
            }
        }

        if (graphicsSettings.isWaterRenderingEnabled()) {
            waterRenderer.render(currentCamera, projectionMatrix, viewMatrix, new Vector3f(-1, -1, -1));
        }

        if (graphicsSettings.isAmbientOcclusionEnabled()) {
            // Render G-Buffer for SSAO
            aoRenderer.renderGBuffer(currentCamera, projectionMatrix, viewMatrix, opaqueQueue);

            // Render SSAO
            aoRenderer.renderSSAO(projectionMatrix);
        }

        // Unbind scene framebuffer
        framebufferManager.getFramebuffer("scene").unbind();

        // Render skybox (fallback)
        if (graphicsSettings.isSkyRenderingEnabled()) {
            skybox.render(camera, projectionMatrix);
        }

        // Render volumetric fog
        if (graphicsSettings.isVolumetricFogEnabled()) {
            volumetricFog.render(camera, projectionMatrix, framebufferManager.getFramebuffer("scene").getDepthTexture());
        }

        // Render volumetric clouds
        if (graphicsSettings.isVolumetricCloudsEnabled()) {
            Vector3f lightDirection = new Vector3f(0.0f, -1.0f, 0.0f).normalize();
            Vector3f lightColor = new Vector3f(1.0f, 0.9f, 0.8f);
            Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.8f);
            volumetricClouds.render(camera, projectionMatrix, viewMatrix, lightDirection, lightColor, sunColor,
                                   framebufferManager.getFramebuffer("scene").getDepthTexture(),
                                   framebufferManager.getFramebuffer("scene").getColorTexture(),
                                   Timer.getDeltaTime());
        }

        // Update time of day system
        timeOfDaySystem.update(Timer.getDeltaTime());
         
        // Render Hosek-Wilkie sky
        if (graphicsSettings.isSkyRenderingEnabled()) {
            Vector3f sunDirection = timeOfDaySystem.getSunDirection();
            hosekWilkieSky.setSunDirection(sunDirection);
            hosekWilkieSky.setTurbidity(timeOfDaySystem.getTurbidity());
            hosekWilkieSky.setGroundAlbedo(timeOfDaySystem.getAlbedo());
            hosekWilkieSky.render(camera, projectionMatrix);
         
            // Render dynamic sky as fallback
            dynamicSky.render(camera, projectionMatrix, sunDirection);
        }
    }

    private void renderShadowMap(World world, Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f lightDirection) {
        // Update CSM light space matrices based on camera frustum
        cascadedShadowMap.updateLightSpaceMatrices(camera, projectionMatrix, viewMatrix, lightDirection);
        
        Shader shadowShader = shaderManager.getShader("csm_shadow");
        if (shadowShader == null) {
            shadowShader = shaderManager.getShader("shadow"); // Fallback to old shader
        }
        shadowShader.bind();

        // Render each cascade
        for (int i = 0; i < cascadedShadowMap.getNumCascades(); i++) {
            cascadedShadowMap.bind(i);
            glClear(GL_DEPTH_BUFFER_BIT);

            // Set light space matrix for current cascade
            Matrix4f lightSpaceMatrix = cascadedShadowMap.getLightSpaceMatrix(i);
            shadowShader.setUniform("u_LightSpaceMatrix", lightSpaceMatrix);

            // Render world chunks
            java.util.Collection<com.odyssey.world.Chunk> chunks = world.getChunks();
            for (com.odyssey.world.Chunk chunk : chunks) {
                if (chunk.getMesh() != null) {
                    shadowShader.setUniform("u_ModelMatrix", chunk.getModelMatrix());
                    chunk.getMesh().render();
                }
            }
        }

        cascadedShadowMap.unbind();
        shadowShader.unbind();
    }

    /**
     * Render UI elements.
     * This method processes the UI queue and renders UI elements using TextRenderer.
     */
    public void renderUI() {
        if (!initialized) {
            logger.warn("Renderer not initialized, cannot render UI");
            return;
        }

        if (textRenderer == null) {
            logger.warn("TextRenderer is null in Renderer.renderUI()");
            return;
        }

        // Enable blending for text rendering
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Disable depth testing for UI elements
        glDisable(GL_DEPTH_TEST);

        // Process UI queue
        for (RenderCommand command : uiQueue) {
            // For now, we assume UI commands are for text rendering
            // This will be expanded to handle other UI elements
        }

        // Clear UI queue for next frame
        uiQueue.clear();

        // Restore rendering state
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderMainMenu(MainMenu mainMenu) {
        if (!initialized) {
            logger.warn("Renderer not initialized, cannot render main menu");
            return;
        }
        mainMenu.render(windowWidth, windowHeight);
    }

    public void renderLoadGameMenu(LoadGameMenu loadGameMenu) {
        if (!initialized) {
            logger.warn("Renderer not initialized, cannot render load game menu");
            return;
        }
        loadGameMenu.render(windowWidth, windowHeight);
    }

    public void renderLoadingScreen(String message, float progress) {
        if (!initialized) return;

        glClearColor(0.05f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        textRenderer.renderText(message, windowWidth / 2.0f, windowHeight / 2.0f, 1.0f, new float[]{1,1,1}, windowWidth, windowHeight);
        // ... progress bar rendering ...
    }

    public void renderPauseMenu() {
        if (!initialized) return;

        // Render semi-transparent overlay
        // ...

        textRenderer.renderText("PAUSED", windowWidth / 2.0f, windowHeight / 2.0f, 2.0f, new float[]{1,1,1}, windowWidth, windowHeight);
    }

    public void clearScreen() {
        glClearColor(clearColor.x, clearColor.y, clearColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void renderFullscreenQuad() {
        if (quadVAO == 0) {
            float quadVertices[] = {
                // positions   // texCoords
                -1.0f,  1.0f,  0.0f, 1.0f,
                -1.0f, -1.0f,  0.0f, 0.0f,
                 1.0f, -1.0f,  1.0f, 0.0f,

                -1.0f,  1.0f,  0.0f, 1.0f,
                 1.0f, -1.0f,  1.0f, 0.0f,
                 1.0f,  1.0f,  1.0f, 1.0f
            };
            quadVAO = glGenVertexArrays();
            int quadVBO = glGenBuffers();
            glBindVertexArray(quadVAO);
            glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
            glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, (2 * Float.BYTES));
        }
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void setWorld(World world) {
        this.world = world;
        this.aoRenderer.initialize(world, windowWidth, windowHeight);
    }

    public World getWorld() {
        return world;
    }

    public void renderWorld() {
        if (world != null) {
            render(world, camera);
        }
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

        framebufferManager.onWindowResize(newWidth, newHeight);
        postProcessingRenderer.resize(newWidth, newHeight);
        aoRenderer.resize(newWidth, newHeight);


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

    public void setWaterRenderingEnabled(boolean enabled) {
        this.waterRenderingEnabled = enabled;
    }

    public void setAmbientOcclusionEnabled(boolean enabled) {
        this.ambientOcclusionEnabled = enabled;
    }

    public void setPostProcessingEnabled(boolean enabled) {
        this.postProcessingEnabled = enabled;
    }

    public void setVoxelAOEnabled(boolean enabled) {
        this.voxelAOEnabled = enabled;
    }

    public void setWaterQuality(int quality) {
        this.waterQuality = quality;
    }

    public void setAOQuality(int quality) {
        this.aoQuality = quality;
    }

    public void setRenderDistance(float distance) {
        this.renderDistance = distance;
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
     * Get text renderer.
     */
    public TextRenderer getTextRenderer() {
        return textRenderer;
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

        // Cleanup advanced rendering components
        if (waterRenderer != null) {
            waterRenderer.cleanup();
        }

        if (aoRenderer != null) {
            aoRenderer.cleanup();
        }

        if (textureAtlas != null) {
            textureAtlas.cleanup();
        }

        if (postProcessingRenderer != null) {
            postProcessingRenderer.cleanup();
        }
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        
        if (hosekWilkieSky != null) {
            hosekWilkieSky.cleanup();
        }
        
        if (timeOfDaySystem != null) {
            timeOfDaySystem.cleanup();
        }
        
        if (computeShaderManager != null) {
            computeShaderManager.cleanup();
        }

        initialized = false;
        logger.info("Renderer cleanup complete");
    }

    /**
     * Gets the graphics settings instance for configuration
     * @return The graphics settings instance
     */
    public GraphicsSettings getGraphicsSettings() {
        return graphicsSettings;
    }

    /**
     * Updates graphics settings and applies changes
     * @param settings The new graphics settings
     */
    public void setGraphicsSettings(GraphicsSettings settings) {
        this.graphicsSettings = settings;
        // Apply any immediate changes that require renderer updates
        logger.info("Graphics settings updated");
    /**
      * Gets the compute shader manager for GPU-accelerated effects
      * @return The compute shader manager instance
      */
     public ComputeShaderManager getComputeShaderManager() {
         return computeShaderManager;
     }
}
