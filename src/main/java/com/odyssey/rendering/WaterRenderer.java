package com.odyssey.rendering;

import com.odyssey.core.ResourceManager;
import com.odyssey.physics.OceanPhysics;
import com.odyssey.physics.WaveSystem;
import com.odyssey.util.Logger;
import com.odyssey.util.Timer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Advanced water rendering system for The Odyssey.
 * Handles realistic ocean rendering with wave normals, foam, reflections, and refractions.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public class WaterRenderer {
    
    private static final Logger logger = Logger.getLogger(WaterRenderer.class);
    
    // Rendering components
    private Shader waterShader;
    private Shader foamShader;
    private Mesh waterMesh;
    private Texture normalMap;
    private Texture foamTexture;
    private Texture reflectionTexture;
    private Texture refractionTexture;
    private Texture dudvTexture;
    private Texture normalTexture;
    private Texture causticsTexture;
    
    // Physics integration
    private OceanPhysics oceanPhysics;
    private WaveSystem waveSystem;
    
    // Water properties
    private Vector3f waterColor = new Vector3f(0.1f, 0.3f, 0.6f);
    private Vector3f deepWaterColor = new Vector3f(0.05f, 0.15f, 0.4f);
    private float waterTransparency = 0.8f;
    private float waveStrength = 1.0f;
    private float foamThreshold = 0.5f;
    private float reflectionStrength = 0.7f;
    private float refractionStrength = 0.3f;

    private List<GerstnerWave> gerstnerWaves = new ArrayList<>();
    
    // Rendering parameters
    private int waterResolution = 256;
    private float waterTileSize = 1.0f;
    private float normalMapScale = 1.0f;
    private Vector2f normalMapOffset = new Vector2f(0.0f, 0.0f);
    private float normalMapSpeed = 0.1f;
    
    // Performance settings
    private boolean enableReflections = true;
    private boolean enableRefractions = true;
    private boolean enableFoam = true;
    private boolean enableWaveNormals = true;
    private int maxWaterDistance = 500;
    
    // Timing
    private Timer timer;
    private float animationTime = 0.0f;
    
    // Vertex data
    private FloatBuffer vertexBuffer;
    private int vao, vbo, ebo;
    private int indexCount;
    
    /**
     * Creates a new WaterRenderer.
     */
    public WaterRenderer() {
        this.timer = new Timer();
    }
    
    /**
     * Initializes the water renderer.
     * 
     * @param oceanPhysics The ocean physics system
     * @param waveSystem The wave system
     */
    public void initialize(OceanPhysics oceanPhysics, WaveSystem waveSystem) {
        this.oceanPhysics = oceanPhysics;
        this.waveSystem = waveSystem;
        
        logger.info("Initializing water renderer...");
        
        // Load shaders
        loadShaders();
        
        // Load textures
        loadTextures();
        
        // Generate water mesh
                generateWaterMesh();
        
                initializeDefaultWaves();
    
                logger.info("Water renderer initialized successfully");    }
    
    /**
     * Loads water rendering shaders.
     */
    private void loadShaders() {
        try {
            // Try to load shaders from resource files first
            String waterVertexSource = loadShaderSource("water.vert");
            String waterFragmentSource = loadShaderSource("water.frag");
            String foamVertexSource = loadShaderSource("foam.vert");
            String foamFragmentSource = loadShaderSource("foam.frag");
            
            // Create water shader
            waterShader = Shader.create("WaterShader", waterVertexSource, waterFragmentSource);
            
            // Create foam shader
            foamShader = Shader.create("FoamShader", foamVertexSource, foamFragmentSource);
            
            logger.debug("Water shaders loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load water shaders: " + e.getMessage());
        }
    }
    
    /**
     * Loads water textures.
     */
    private void loadTextures() {
        try {
            // Load normal map texture
            normalMap = Texture.loadFromFile("WaterNormal", "textures/water_normal.png");
            
            // Create foam texture (procedural)
            foamTexture = Texture.create("WaterFoam", 256, 256, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            
            // Create reflection and refraction textures
            reflectionTexture = Texture.create("WaterReflection", 1024, 1024, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            refractionTexture = Texture.create("WaterRefraction", 1024, 1024, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            
            logger.debug("Water textures loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load water textures: " + e.getMessage());
        }
    }
    
    /**
     * Generates the water mesh.
     */
    private void generateWaterMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Generate grid vertices
        for (int z = 0; z <= waterResolution; z++) {
            for (int x = 0; x <= waterResolution; x++) {
                float worldX = (x - waterResolution / 2.0f) * waterTileSize;
                float worldZ = (z - waterResolution / 2.0f) * waterTileSize;
                
                // Position
                vertices.add(worldX);
                vertices.add(0.0f); // Y will be calculated in vertex shader
                vertices.add(worldZ);
                
                // Texture coordinates
                vertices.add((float) x / waterResolution);
                vertices.add((float) z / waterResolution);
                
                // Normal (will be calculated in shader)
                vertices.add(0.0f);
                vertices.add(1.0f);
                vertices.add(0.0f);
            }
        }
        
        // Generate indices for triangles
        for (int z = 0; z < waterResolution; z++) {
            for (int x = 0; x < waterResolution; x++) {
                int topLeft = z * (waterResolution + 1) + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * (waterResolution + 1) + x;
                int bottomRight = bottomLeft + 1;
                
                // First triangle
                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);
                
                // Second triangle
                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
            }
        }
        
        // Convert to arrays
        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        
        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        // Create water mesh with proper constructor
        waterMesh = new Mesh("WaterMesh", vertexArray, indexArray);
        
        // Add vertex attributes
        waterMesh.addAttribute(new Mesh.VertexAttribute(0, 3, GL_FLOAT, false, 0));  // Position
        waterMesh.addAttribute(new Mesh.VertexAttribute(1, 2, GL_FLOAT, false, 12)); // TexCoord
        waterMesh.addAttribute(new Mesh.VertexAttribute(2, 3, GL_FLOAT, false, 20)); // Normal
        
        // Upload mesh data to GPU
        waterMesh.upload();
        
        indexCount = indices.size();
        
        logger.debug("Generated water mesh with {} vertices and {} indices", 
                    vertices.size() / 8, indices.size());
    }
    
    /**
     * Update Gerstner wave parameters in shader
     */
    private void updateWaveUniforms() {
        if (waterShader == null) return;
        
        waterShader.use();
        
        // Set number of active waves
        int numWaves = Math.min(gerstnerWaves.size(), 8);
        waterShader.setInt("numWaves", numWaves);
        
        // Update wave parameters
        for (int i = 0; i < numWaves; i++) {
            GerstnerWave wave = gerstnerWaves.get(i);
            
            waterShader.setFloat("waveAmplitudes[" + i + "]", wave.getAmplitude());
            waterShader.setFloat("waveFrequencies[" + i + "]", wave.getFrequency());
            waterShader.setFloat("wavePhases[" + i + "]", wave.getPhase());
            waterShader.setUniform("waveDirections[" + i + "]", wave.getDirection());
            waterShader.setFloat("waveSteepness[" + i + "]", wave.getSteepness());
            waterShader.setFloat("waveSpeeds[" + i + "]", wave.getSpeed());
        }
        
        // Clear unused wave slots
        for (int i = numWaves; i < 8; i++) {
            waterShader.setFloat("waveAmplitudes[" + i + "]", 0.0f);
        }
        
        waterShader.unbind();
    }
    
    /**
     * Render water surface with Gerstner waves and advanced effects
     */
    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, Matrix4f modelMatrix, 
                      Vector3f cameraPos, Vector3f lightPos, Vector3f lightColor, float deltaTime) {
        
        if (waterShader == null || waterMesh == null) return;
        
        // Update time for wave animation
        animationTime += deltaTime;
        
        // Update normal map offset for animation
        normalMapOffset.x += normalMapSpeed * deltaTime;
        normalMapOffset.y += normalMapSpeed * 0.7f * deltaTime;
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Use water shader
        waterShader.use();
        
        // Update matrices
        waterShader.setUniform("projection", projectionMatrix);
        waterShader.setUniform("view", viewMatrix);
        waterShader.setUniform("model", modelMatrix);
        
        // Update lighting
        waterShader.setUniform("cameraPos", cameraPos);
        waterShader.setUniform("lightPos", lightPos);
        waterShader.setUniform("lightColor", lightColor);
        
        // Update time
        waterShader.setFloat("time", animationTime);
        
        // Update water colors
        waterShader.setUniform("waterColor", waterColor);
        waterShader.setUniform("deepWaterColor", deepWaterColor);
        
        // Update wave parameters
        updateWaveUniforms();
        
        // Bind textures
        bindWaterTextures();
        
        // Render water mesh
        waterMesh.render();
        
        // Cleanup
        unbindTextures();
        waterShader.unbind();
        glDisable(GL_BLEND);
    }
    
    /**
     * Legacy render method for backward compatibility
     */
    public void render(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f lightDirection) {
        if (waterShader == null || waterMesh == null) {
            return;
        }
        
        // Update animation time
        animationTime += timer.getDeltaTime();
        
        // Update normal map offset for animation
        normalMapOffset.x += normalMapSpeed * timer.getDeltaTime();
        normalMapOffset.y += normalMapSpeed * 0.7f * timer.getDeltaTime();
        
        // Bind water shader
        waterShader.bind();
        
        // Set matrices
        waterShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        waterShader.setUniform("u_ViewMatrix", viewMatrix);
        waterShader.setUniform("u_ModelMatrix", new Matrix4f());
        
        // Set camera properties
        waterShader.setUniform("u_CameraPosition", camera.getPosition());
        waterShader.setUniform("u_CameraDirection", camera.getForward());
        
        // Set lighting
        waterShader.setUniform("u_LightDirection", lightDirection);
        waterShader.setUniform("u_LightColor", new Vector3f(1.0f, 1.0f, 0.9f));
        
        // Set water properties
        waterShader.setUniform("u_WaterColor", waterColor);
        waterShader.setUniform("u_DeepWaterColor", deepWaterColor);
        waterShader.setUniform("u_WaterTransparency", waterTransparency);
        waterShader.setUniform("u_WaveStrength", waveStrength);
        waterShader.setUniform("u_ReflectionStrength", reflectionStrength);
        waterShader.setUniform("u_RefractionStrength", refractionStrength);
        
        // Set animation properties
        waterShader.setUniform("u_Time", animationTime);
        waterShader.setUniform("u_NormalMapOffset", normalMapOffset);
        waterShader.setUniform("u_NormalMapScale", normalMapScale);

        // Set wave system data
        for (int i = 0; i < gerstnerWaves.size() && i < 8; i++) {
            GerstnerWave wave = gerstnerWaves.get(i);
            waterShader.setUniform("waves[" + i + "].amplitude", wave.getAmplitude());
            waterShader.setUniform("waves[" + i + "].frequency", wave.getFrequency());
            waterShader.setUniform("waves[" + i + "].phase", wave.getPhase());
            waterShader.setUniform("waves[" + i + "].direction", wave.getDirection());
        }
        
        // Bind textures
        glActiveTexture(GL_TEXTURE0);
        if (normalMap != null) {
            normalMap.bind();
        }
        waterShader.setUniform("u_NormalMap", 0);
        
        if (enableReflections && reflectionTexture != null) {
            glActiveTexture(GL_TEXTURE1);
            reflectionTexture.bind();
            waterShader.setUniform("u_ReflectionTexture", 1);
            waterShader.setUniform("u_EnableReflections", true);
        } else {
            waterShader.setUniform("u_EnableReflections", false);
        }
        
        if (enableRefractions && refractionTexture != null) {
            glActiveTexture(GL_TEXTURE2);
            refractionTexture.bind();
            waterShader.setUniform("u_RefractionTexture", 2);
            waterShader.setUniform("u_EnableRefractions", true);
        } else {
            waterShader.setUniform("u_EnableRefractions", false);
        }
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Render water mesh
        waterMesh.render();
        
        // Render foam if enabled
        if (enableFoam) {
            renderFoam(camera, projectionMatrix, viewMatrix, lightDirection);
        }
        
        // Disable blending
        glDisable(GL_BLEND);
        
        // Unbind shader
        waterShader.unbind();
    }
    
    /**
     * Bind all water-related textures
     */
    private void bindWaterTextures() {
        // Reflection texture (slot 0)
        glActiveTexture(GL_TEXTURE0);
        if (reflectionTexture != null) {
            glBindTexture(GL_TEXTURE_2D, reflectionTexture.getTextureId());
        }
        
        // Refraction texture (slot 1)
        glActiveTexture(GL_TEXTURE1);
        if (refractionTexture != null) {
            glBindTexture(GL_TEXTURE_2D, refractionTexture.getTextureId());
        }
        
        // DuDv map (slot 2)
        glActiveTexture(GL_TEXTURE2);
        if (dudvTexture != null) {
            glBindTexture(GL_TEXTURE_2D, dudvTexture.getTextureId());
        }
        
        // Normal map (slot 3)
        glActiveTexture(GL_TEXTURE3);
        if (normalTexture != null) {
            glBindTexture(GL_TEXTURE_2D, normalTexture.getTextureId());
        }
        
        // Foam texture (slot 4)
        glActiveTexture(GL_TEXTURE4);
        if (foamTexture != null) {
            glBindTexture(GL_TEXTURE_2D, foamTexture.getTextureId());
        }
        
        // Depth map (slot 5) - should be provided by the main renderer
        glActiveTexture(GL_TEXTURE5);
        // Depth texture binding would be handled by the main rendering pipeline
        
        // Caustics texture (slot 6)
        glActiveTexture(GL_TEXTURE6);
        if (causticsTexture != null) {
            glBindTexture(GL_TEXTURE_2D, causticsTexture.getTextureId());
        }
    }
    
    /**
     * Unbind all textures
     */
    private void unbindTextures() {
        for (int i = 0; i < 7; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }
    
    /**
     * Renders foam effects on wave crests.
     */
    private void renderFoam(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f lightDirection) {
        if (foamShader == null || foamTexture == null) {
            return;
        }
        
        // Bind foam shader
        foamShader.bind();
        
        // Set matrices
        foamShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        foamShader.setUniform("u_ViewMatrix", viewMatrix);
        foamShader.setUniform("u_ModelMatrix", new Matrix4f());
        
        // Set foam properties
        foamShader.setUniform("u_FoamThreshold", foamThreshold);
        foamShader.setUniform("u_Time", animationTime);
        
        // Bind foam texture
        glActiveTexture(GL_TEXTURE0);
        foamTexture.bind();
        foamShader.setUniform("u_FoamTexture", 0);
        
        // Enable additive blending for foam
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        
        // Render foam (using same mesh but different shader)
        waterMesh.render();
        
        // Unbind shader
        foamShader.unbind();
    }
    
    /**
     * Add a new Gerstner wave to the water system
     */
    public void addGerstnerWave(GerstnerWave wave) {
        if (gerstnerWaves.size() < 8) { // Maximum 8 waves for shader compatibility
            gerstnerWaves.add(wave);
        }
    }
    
    /**
     * Remove a Gerstner wave from the water system
     */
    public void removeGerstnerWave(int index) {
        if (index >= 0 && index < gerstnerWaves.size()) {
            gerstnerWaves.remove(index);
        }
    }
    
    /**
     * Clear all Gerstner waves
     */
    public void clearGerstnerWaves() {
        gerstnerWaves.clear();
    }
    
    /**
     * Get the current list of Gerstner waves
     */
    public List<GerstnerWave> getGerstnerWaves() {
        return new ArrayList<>(gerstnerWaves);
    }
    
    /**
     * Calculate water height at a specific world position using Gerstner waves
     */
    public float getWaterHeightAt(float x, float z) {
        float totalHeight = 0.0f;
        
        for (GerstnerWave wave : gerstnerWaves) {
            Vector3f displacement = wave.calculateDisplacement(x, z, animationTime);
            totalHeight += displacement.y * waveStrength;
        }
        
        return totalHeight;
    }
    
    /**
     * Calculate water normal at a specific world position using Gerstner waves
     */
    public Vector3f getWaterNormalAt(float x, float z) {
        Vector3f totalNormal = new Vector3f(0.0f, 1.0f, 0.0f);
        
        for (GerstnerWave wave : gerstnerWaves) {
            Vector3f waveNormal = wave.calculateNormal(x, z, animationTime);
            totalNormal.add(waveNormal);
        }
        
        return totalNormal.normalize();
    }
    
    /**
     * Initialize default Gerstner waves for realistic ocean simulation
     */
    private void initializeDefaultWaves() {
        gerstnerWaves.clear();
        
        // Large primary wave
        gerstnerWaves.add(new GerstnerWave(
            2.0f,                           // amplitude
            0.1f,                           // frequency
            0.0f,                           // phase
            new Vector2f(1.0f, 0.0f),      // direction
            0.8f,                           // steepness
            5.0f                            // speed
        ));
        
        // Medium secondary wave
        gerstnerWaves.add(new GerstnerWave(
            1.2f,                           // amplitude
            0.15f,                          // frequency
            1.5f,                           // phase
            new Vector2f(0.7f, 0.7f),      // direction
            0.6f,                           // steepness
            4.0f                            // speed
        ));
        
        // Small detail wave
        gerstnerWaves.add(new GerstnerWave(
            0.8f,                           // amplitude
            0.25f,                          // frequency
            3.0f,                           // phase
            new Vector2f(-0.5f, 0.8f),     // direction
            0.4f,                           // steepness
            3.0f                            // speed
        ));
        
        // High-frequency ripples
        gerstnerWaves.add(new GerstnerWave(
            0.3f,                           // amplitude
            0.5f,                           // frequency
            2.2f,                           // phase
            new Vector2f(0.3f, -0.9f),     // direction
            0.2f,                           // steepness
            2.0f                            // speed
        ));
    }
    
    /**
     * Calculates wave normal at a specific position.
     * 
     * @param x World X coordinate
     * @param z World Z coordinate
     * @return The wave normal vector
     */
    public Vector3f calculateWaveNormal(float x, float z) {
        if (waveSystem == null) {
            return getWaterNormalAt(x, z);
        }
        
        // Sample wave heights at nearby points
        float epsilon = 0.1f;
        float heightCenter = waveSystem.getWaveHeight(x, z);
        float heightRight = waveSystem.getWaveHeight(x + epsilon, z);
        float heightUp = waveSystem.getWaveHeight(x, z + epsilon);
        
        // Calculate tangent vectors
        Vector3f tangentX = new Vector3f(epsilon, heightRight - heightCenter, 0.0f);
        Vector3f tangentZ = new Vector3f(0.0f, heightUp - heightCenter, epsilon);
        
        // Calculate normal via cross product
        Vector3f normal = new Vector3f();
        tangentX.cross(tangentZ, normal);
        normal.normalize();
        
        return normal;
    }
    
    /**
     * Loads shader source code from file.
     */
    private String loadShaderSource(String filename) {
        try {
            // Try to load from resources first
            ResourceManager resourceManager = ResourceManager.getInstance();
            String shaderSource = resourceManager.loadResource("shaders/" + filename);
            
            if (shaderSource != null && !shaderSource.trim().isEmpty()) {
                logger.debug("Loaded shader from resources: {}", filename);
                return shaderSource;
            }
            
            logger.warn("Could not load shader from resources: {}, using built-in fallback", filename);
            
        } catch (Exception e) {
            logger.warn("Error loading shader from resources: {}, using built-in fallback: {}", filename, e.getMessage());
        }
        
        // Fallback to built-in shader code if resource loading fails
        if (filename.contains("water.vert")) {
            return getWaterVertexShader();
        } else if (filename.contains("water.frag")) {
            return getWaterFragmentShader();
        } else if (filename.contains("foam.vert")) {
            return getFoamVertexShader();
        } else if (filename.contains("foam.frag")) {
            return getFoamFragmentShader();
        }
        
        logger.error("Unknown shader file: {}", filename);
        return "";
    }
    
    /**
     * Returns the water vertex shader source.
     */
    private String getWaterVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "layout (location = 2) in vec3 a_Normal;\n" +
                "\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_WaveStrength;\n" +
                "uniform bool u_EnableWaveNormals;\n" +
                "\n" +
                "out vec3 v_WorldPosition;\n" +
                "out vec2 v_TexCoord;\n" +
                "out vec3 v_Normal;\n" +
                "out vec4 v_ClipSpace;\n" +
                "\n" +
                "// Simple wave function\n" +
                "float wave(vec2 pos, float time) {\n" +
                "    return sin(pos.x * 0.1 + time) * cos(pos.y * 0.1 + time * 0.7) * u_WaveStrength;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 worldPosition = u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "    \n" +
                "    // Apply wave displacement\n" +
                "    if (u_EnableWaveNormals) {\n" +
                "        worldPosition.y += wave(worldPosition.xz, u_Time);\n" +
                "    }\n" +
                "    \n" +
                "    v_WorldPosition = worldPosition.xyz;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    v_Normal = a_Normal;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPosition;\n" +
                "    v_ClipSpace = gl_Position;\n" +
                "}";
    }
    
    /**
     * Returns the water fragment shader source.
     */
    private String getWaterFragmentShader() {
        return "#version 330 core\n" +
                "in vec3 v_WorldPosition;\n" +
                "in vec2 v_TexCoord;\n" +
                "in vec3 v_Normal;\n" +
                "in vec4 v_ClipSpace;\n" +
                "\n" +
                "uniform vec3 u_CameraPosition;\n" +
                "uniform vec3 u_LightDirection;\n" +
                "uniform vec3 u_LightColor;\n" +
                "uniform vec3 u_WaterColor;\n" +
                "uniform vec3 u_DeepWaterColor;\n" +
                "uniform float u_WaterTransparency;\n" +
                "uniform float u_ReflectionStrength;\n" +
                "uniform float u_RefractionStrength;\n" +
                "uniform vec2 u_NormalMapOffset;\n" +
                "uniform float u_NormalMapScale;\n" +
                "uniform bool u_EnableReflections;\n" +
                "uniform bool u_EnableRefractions;\n" +
                "\n" +
                "uniform sampler2D u_NormalMap;\n" +
                "uniform sampler2D u_ReflectionTexture;\n" +
                "uniform sampler2D u_RefractionTexture;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 ndc = (v_ClipSpace.xy / v_ClipSpace.w) / 2.0 + 0.5;\n" +
                "    \n" +
                "    // Sample normal map\n" +
                "    vec2 normalTexCoord = v_TexCoord * u_NormalMapScale + u_NormalMapOffset;\n" +
                "    vec3 normalMapSample = texture(u_NormalMap, normalTexCoord).rgb * 2.0 - 1.0;\n" +
                "    vec3 normal = normalize(v_Normal + normalMapSample * 0.1);\n" +
                "    \n" +
                "    // Calculate view direction\n" +
                "    vec3 viewDir = normalize(u_CameraPosition - v_WorldPosition);\n" +
                "    \n" +
                "    // Calculate reflection\n" +
                "    vec3 reflectionColor = u_WaterColor;\n" +
                "    if (u_EnableReflections) {\n" +
                "        vec2 reflectionCoord = vec2(ndc.x, -ndc.y);\n" +
                "        reflectionColor = texture(u_ReflectionTexture, reflectionCoord).rgb;\n" +
                "    }\n" +
                "    \n" +
                "    // Calculate refraction\n" +
                "    vec3 refractionColor = u_DeepWaterColor;\n" +
                "    if (u_EnableRefractions) {\n" +
                "        vec2 refractionCoord = ndc;\n" +
                "        refractionColor = texture(u_RefractionTexture, refractionCoord).rgb;\n" +
                "    }\n" +
                "    \n" +
                "    // Fresnel effect\n" +
                "    float fresnel = pow(1.0 - max(dot(viewDir, normal), 0.0), 2.0);\n" +
                "    \n" +
                "    // Mix reflection and refraction\n" +
                "    vec3 finalColor = mix(refractionColor, reflectionColor, fresnel * u_ReflectionStrength);\n" +
                "    \n" +
                "    // Add water tint\n" +
                "    finalColor = mix(finalColor, u_WaterColor, 0.3);\n" +
                "    \n" +
                "    // Simple lighting\n" +
                "    float lightIntensity = max(dot(normal, -u_LightDirection), 0.0);\n" +
                "    finalColor *= (0.3 + 0.7 * lightIntensity);\n" +
                "    \n" +
                "    FragColor = vec4(finalColor, u_WaterTransparency);\n" +
                "}";
    }
    
    private String getFoamVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "layout (location = 2) in vec3 a_Normal;\n" +
                "\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "out float v_FoamIntensity;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 worldPosition = u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "    \n" +
                "    // Calculate foam intensity based on wave steepness\n" +
                "    v_FoamIntensity = sin(worldPosition.x * 0.1 + u_Time) * cos(worldPosition.z * 0.1 + u_Time);\n" +
                "    v_FoamIntensity = max(0.0, v_FoamIntensity - 0.5) * 2.0;\n" +
                "    \n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPosition;\n" +
                "}";
    }

    private String getFoamFragmentShader() {
        return "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "in float v_FoamIntensity;\n" +
                "\n" +
                "uniform sampler2D u_FoamTexture;\n" +
                "uniform float u_FoamThreshold;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    if (v_FoamIntensity < u_FoamThreshold) {\n" +
                "        discard;\n" +
                "    }\n" +
                "    \n" +
                "    vec4 foamColor = texture(u_FoamTexture, v_TexCoord);\n" +
                "    foamColor.a *= v_FoamIntensity;\n" +
                "    \n" +
                "    FragColor = foamColor;\n" +
                "}";
    }
    
    // Getters and setters
    
    public void setWaterColor(Vector3f color) {
        this.waterColor.set(color);
    }
    
    public void setDeepWaterColor(Vector3f color) {
        this.deepWaterColor.set(color);
    }
    
    public void setWaterTransparency(float transparency) {
        this.waterTransparency = Math.max(0.0f, Math.min(1.0f, transparency));
    }
    
    public void setWaveStrength(float strength) {
        this.waveStrength = strength;
    }
    
    public void setReflectionStrength(float strength) {
        this.reflectionStrength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    public void setRefractionStrength(float strength) {
        this.refractionStrength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    public void setEnableReflections(boolean enable) {
        this.enableReflections = enable;
    }
    
    public void setEnableRefractions(boolean enable) {
        this.enableRefractions = enable;
    }
    
    public void setEnableFoam(boolean enable) {
        this.enableFoam = enable;
    }
    
    public void setEnableWaveNormals(boolean enable) {
        this.enableWaveNormals = enable;
    }
    
    /**
     * Cleanup water renderer resources.
     */
    public void cleanup() {
        if (waterShader != null) {
            waterShader.cleanup();
        }
        if (foamShader != null) {
            foamShader.cleanup();
        }
        if (waterMesh != null) {
            waterMesh.cleanup();
        }
        if (normalMap != null) {
            normalMap.cleanup();
        }
        if (foamTexture != null) {
            foamTexture.cleanup();
        }
        if (reflectionTexture != null) {
            reflectionTexture.cleanup();
        }
        if (refractionTexture != null) {
            refractionTexture.cleanup();
        }
        
        logger.info("Water renderer cleaned up");
    }
}
