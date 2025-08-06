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
    
    // Camera system
    private Camera camera;
    
    // Shaders
    private Shader basicShader;
    private Shader oceanShader;
    
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
    }
    
    public void initialize() throws Exception {
        logger.info("Initializing renderer...");
        
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
        
        // Initialize camera
        camera = new Camera();
        camera.setPosition(0.0f, 80.0f, 50.0f);  // Position above and behind the scene
        camera.setRotation(-20.0f, 0.0f, 0.0f);  // Look down slightly
        camera.setFieldOfView(70.0f);
        camera.update(0.0);  // Force immediate update of matrices
        
        // Set initial aspect ratio (will be updated in beginFrame)
        camera.setAspectRatio(1920.0f / 1080.0f);
        
        // Initialize shaders
        initializeShaders();
        
        // Initialize meshes
        initializeMeshes();
        
        logger.info("Renderer initialized successfully");
    }
    
    private void initializeShaders() {
        try {
            // Basic shader for solid objects
            basicShader = new Shader();
            basicShader.createVertexShader(getBasicVertexShader());
            basicShader.createFragmentShader(getBasicFragmentShader());
            basicShader.link();
            
            // Ocean shader for water rendering
            oceanShader = new Shader();
            oceanShader.createVertexShader(getOceanVertexShader());
            oceanShader.createFragmentShader(getOceanFragmentShader());
            oceanShader.link();
            
            logger.info("Shaders initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize shaders", e);
            throw new RuntimeException("Shader initialization failed", e);
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
        // Render ocean plane
        renderOcean();
        
        // Render some test cubes for visual feedback
        renderTestCubes();
        
        // Render debug information if enabled
        if (config.isDebugMode()) {
            renderDebugInfo();
        }
    }
    
    private void renderOcean() {
        // Render ocean using ocean shader
        oceanShader.bind();
        
        // Set uniforms
        oceanShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        oceanShader.setUniform("viewMatrix", camera.getViewMatrix());
        oceanShader.setUniform("lightDirection", lightDirection);
        oceanShader.setUniform("lightColor", lightColor);
        oceanShader.setUniform("ambientColor", ambientColor);
        oceanShader.setUniform("cameraPosition", camera.getPosition());
        oceanShader.setUniform("time", (float) (System.currentTimeMillis() / 1000.0));
        
        // Render ocean plane at sea level
        modelMatrix.identity().translate(0, 64, 0).scale(200);
        oceanShader.setUniform("modelMatrix", modelMatrix);
        
        planeMesh.render();
        
        oceanShader.unbind();
    }
    
    private void renderTestCubes() {
        basicShader.bind();
        
        // Set uniforms
        basicShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        basicShader.setUniform("viewMatrix", camera.getViewMatrix());
        basicShader.setUniform("lightDirection", lightDirection);
        basicShader.setUniform("lightColor", lightColor);
        basicShader.setUniform("ambientColor", ambientColor);
        
        // Render a grid of test cubes above the ocean
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                modelMatrix.identity().translate(x * 20, 75, z * 20).scale(3);
                basicShader.setUniform("modelMatrix", modelMatrix);
                
                // Different colors for different positions
                float r = (x + 2) / 4.0f;
                float g = (z + 2) / 4.0f;
                float b = 0.5f;
                basicShader.setUniform("objectColor", new Vector3f(r, g, b));
                
                cubeMesh.render();
            }
        }
        
        basicShader.unbind();
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
        logger.info("Cleaning up renderer resources");
        
        if (basicShader != null) {
            basicShader.cleanup();
        }
        if (oceanShader != null) {
            oceanShader.cleanup();
        }
        if (cubeMesh != null) {
            cubeMesh.cleanup();
        }
        if (planeMesh != null) {
            planeMesh.cleanup();
        }
    }
    
    // Shader source code
    private String getBasicVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 position;
            layout (location = 1) in vec3 normal;
            layout (location = 2) in vec2 texCoord;
            
            uniform mat4 projectionMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 modelMatrix;
            
            out vec3 fragPos;
            out vec3 fragNormal;
            out vec2 fragTexCoord;
            
            void main() {
                vec4 worldPos = modelMatrix * vec4(position, 1.0);
                fragPos = worldPos.xyz;
                fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                fragTexCoord = texCoord;
                
                gl_Position = projectionMatrix * viewMatrix * worldPos;
            }
            """;
    }
    
    private String getBasicFragmentShader() {
        return """
            #version 330 core
            
            in vec3 fragPos;
            in vec3 fragNormal;
            in vec2 fragTexCoord;
            
            uniform vec3 lightDirection;
            uniform vec3 lightColor;
            uniform vec3 ambientColor;
            uniform vec3 objectColor;
            
            out vec4 fragColor;
            
            void main() {
                vec3 normal = normalize(fragNormal);
                vec3 lightDir = normalize(-lightDirection);
                
                // Ambient lighting
                vec3 ambient = ambientColor;
                
                // Diffuse lighting
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 diffuse = diff * lightColor;
                
                vec3 result = (ambient + diffuse) * objectColor;
                fragColor = vec4(result, 1.0);
            }
            """;
    }
    
    private String getOceanVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 position;
            layout (location = 1) in vec3 normal;
            layout (location = 2) in vec2 texCoord;
            
            uniform mat4 projectionMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 modelMatrix;
            uniform float time;
            
            out vec3 fragPos;
            out vec3 fragNormal;
            out vec2 fragTexCoord;
            out float waveHeight;
            
            void main() {
                vec3 pos = position;
                
                // Create waves
                float wave1 = sin(pos.x * 0.1 + time * 2.0) * 0.5;
                float wave2 = cos(pos.z * 0.15 + time * 1.5) * 0.3;
                float wave3 = sin((pos.x + pos.z) * 0.08 + time * 2.5) * 0.2;
                
                waveHeight = wave1 + wave2 + wave3;
                pos.y += waveHeight;
                
                vec4 worldPos = modelMatrix * vec4(pos, 1.0);
                fragPos = worldPos.xyz;
                fragNormal = normal;
                fragTexCoord = texCoord;
                
                gl_Position = projectionMatrix * viewMatrix * worldPos;
            }
            """;
    }
    
    private String getOceanFragmentShader() {
        return """
            #version 330 core
            
            in vec3 fragPos;
            in vec3 fragNormal;
            in vec2 fragTexCoord;
            in float waveHeight;
            
            uniform vec3 lightDirection;
            uniform vec3 lightColor;
            uniform vec3 ambientColor;
            uniform vec3 cameraPosition;
            uniform float time;
            
            out vec4 fragColor;
            
            void main() {
                vec3 normal = normalize(fragNormal);
                vec3 lightDir = normalize(-lightDirection);
                vec3 viewDir = normalize(cameraPosition - fragPos);
                
                // Ocean colors
                vec3 deepWater = vec3(0.0, 0.2, 0.4);
                vec3 shallowWater = vec3(0.0, 0.4, 0.6);
                vec3 foam = vec3(0.8, 0.9, 1.0);
                
                // Mix colors based on wave height
                float waveIntensity = abs(waveHeight) * 2.0;
                vec3 waterColor = mix(deepWater, shallowWater, waveIntensity);
                if (waveHeight > 0.4) {
                    waterColor = mix(waterColor, foam, (waveHeight - 0.4) * 2.0);
                }
                
                // Ambient lighting
                vec3 ambient = ambientColor * 0.6;
                
                // Diffuse lighting
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 diffuse = diff * lightColor * 0.8;
                
                // Specular lighting (water reflection)
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
                vec3 specular = spec * lightColor * 0.5;
                
                vec3 result = (ambient + diffuse + specular) * waterColor;
                fragColor = vec4(result, 0.8);
            }
            """;
    }
    
    // Camera interface
    public Camera getCamera() { return camera; }
    
    // Getters and setters
    public Matrix4f getModelMatrix() { return new Matrix4f(modelMatrix); }
    
    public boolean isWireframeMode() { return wireframeMode; }
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }
}