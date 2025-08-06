package com.odyssey.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized shader management system for The Odyssey.
 * Handles shader loading, compilation, caching, and hot-reloading.
 */
public class ShaderManager {
    private static final Logger logger = LoggerFactory.getLogger(ShaderManager.class);
    
    private final Map<String, Shader> shaderCache = new ConcurrentHashMap<>();
    private final Map<String, String> shaderSources = new HashMap<>();
    private final Map<String, Long> shaderTimestamps = new HashMap<>();
    
    private boolean hotReloadEnabled = false;
    private String shaderDirectory = "src/main/resources/shaders/";
    
    /**
     * Initializes the shader manager with default settings.
     */
    public ShaderManager() {
        this(false);
    }
    
    /**
     * Initializes the shader manager.
     * 
     * @param enableHotReload whether to enable hot-reloading of shader files
     */
    public ShaderManager(boolean enableHotReload) {
        this.hotReloadEnabled = enableHotReload;
        logger.info("ShaderManager initialized with hot-reload: {}", enableHotReload);
    }
    
    /**
     * Sets the directory where shader files are located.
     * 
     * @param directory the shader directory path
     */
    public void setShaderDirectory(String directory) {
        this.shaderDirectory = directory;
        logger.info("Shader directory set to: {}", directory);
    }
    
    /**
     * Loads and compiles a shader program from vertex and fragment shader sources.
     * 
     * @param name the unique name for this shader
     * @param vertexSource the vertex shader source code
     * @param fragmentSource the fragment shader source code
     * @return the compiled shader program
     * @throws Exception if shader compilation fails
     */
    public Shader loadShader(String name, String vertexSource, String fragmentSource) throws Exception {
        if (shaderCache.containsKey(name)) {
            logger.debug("Returning cached shader: {}", name);
            return shaderCache.get(name);
        }
        
        logger.info("Compiling shader: {}", name);
        
        Shader shader = new Shader();
        try {
            shader.createVertexShader(vertexSource);
            shader.createFragmentShader(fragmentSource);
            shader.link();
            
            shaderCache.put(name, shader);
            shaderSources.put(name + "_vertex", vertexSource);
            shaderSources.put(name + "_fragment", fragmentSource);
            
            logger.info("Successfully compiled shader: {}", name);
            return shader;
        } catch (Exception e) {
            shader.cleanup();
            logger.error("Failed to compile shader: {}", name, e);
            throw e;
        }
    }
    
    /**
     * Loads a shader program from files.
     * 
     * @param name the unique name for this shader
     * @param vertexFile the vertex shader file path (relative to shader directory)
     * @param fragmentFile the fragment shader file path (relative to shader directory)
     * @return the compiled shader program
     * @throws Exception if file loading or shader compilation fails
     */
    public Shader loadShaderFromFiles(String name, String vertexFile, String fragmentFile) throws Exception {
        Path vertexPath = Paths.get(shaderDirectory, vertexFile);
        Path fragmentPath = Paths.get(shaderDirectory, fragmentFile);
        
        if (!Files.exists(vertexPath)) {
            throw new IOException("Vertex shader file not found: " + vertexPath);
        }
        if (!Files.exists(fragmentPath)) {
            throw new IOException("Fragment shader file not found: " + fragmentPath);
        }
        
        String vertexSource = Files.readString(vertexPath);
        String fragmentSource = Files.readString(fragmentPath);
        
        // Store file timestamps for hot-reloading
        if (hotReloadEnabled) {
            try {
                shaderTimestamps.put(name + "_vertex", Files.getLastModifiedTime(vertexPath).toMillis());
                shaderTimestamps.put(name + "_fragment", Files.getLastModifiedTime(fragmentPath).toMillis());
            } catch (IOException e) {
                logger.warn("Could not get file timestamps for hot-reload: {}", name, e);
            }
        }
        
        return loadShader(name, vertexSource, fragmentSource);
    }
    
    /**
     * Gets a cached shader by name.
     * 
     * @param name the shader name
     * @return the shader, or null if not found
     */
    public Shader getShader(String name) {
        return shaderCache.get(name);
    }
    
    /**
     * Checks if a shader exists in the cache.
     * 
     * @param name the shader name
     * @return true if the shader exists
     */
    public boolean hasShader(String name) {
        return shaderCache.containsKey(name);
    }
    
    /**
     * Reloads a shader from its cached sources.
     * 
     * @param name the shader name
     * @return true if reload was successful
     */
    public boolean reloadShader(String name) {
        if (!shaderCache.containsKey(name)) {
            logger.warn("Cannot reload shader that doesn't exist: {}", name);
            return false;
        }
        
        String vertexSource = shaderSources.get(name + "_vertex");
        String fragmentSource = shaderSources.get(name + "_fragment");
        
        if (vertexSource == null || fragmentSource == null) {
            logger.warn("Cannot reload shader, sources not cached: {}", name);
            return false;
        }
        
        try {
            // Clean up old shader
            Shader oldShader = shaderCache.get(name);
            oldShader.cleanup();
            
            // Compile new shader
            Shader newShader = new Shader();
            newShader.createVertexShader(vertexSource);
            newShader.createFragmentShader(fragmentSource);
            newShader.link();
            
            shaderCache.put(name, newShader);
            logger.info("Successfully reloaded shader: {}", name);
            return true;
        } catch (Exception e) {
            logger.error("Failed to reload shader: {}", name, e);
            return false;
        }
    }
    
    /**
     * Checks for modified shader files and reloads them if hot-reload is enabled.
     * This should be called periodically (e.g., once per frame in debug mode).
     */
    public void checkForUpdates() {
        if (!hotReloadEnabled) {
            return;
        }
        
        for (String shaderName : shaderCache.keySet()) {
            String vertexKey = shaderName + "_vertex";
            String fragmentKey = shaderName + "_fragment";
            
            Long vertexTimestamp = shaderTimestamps.get(vertexKey);
            Long fragmentTimestamp = shaderTimestamps.get(fragmentKey);
            
            if (vertexTimestamp != null || fragmentTimestamp != null) {
                // Check if files have been modified
                // This is a simplified check - in a real implementation,
                // you'd want to check actual file timestamps
                // For now, we'll just provide the infrastructure
            }
        }
    }
    
    /**
     * Creates built-in shaders that are commonly used.
     */
    public void loadBuiltinShaders() throws Exception {
        logger.info("Loading built-in shaders...");
        
        // Basic shader for solid objects
        loadShader("basic", getBasicVertexShader(), getBasicFragmentShader());
        
        // Ocean shader for water rendering
        loadShader("ocean", getOceanVertexShader(), getOceanFragmentShader());
        
        // UI shader for interface rendering
        loadShader("ui", getUIVertexShader(), getUIFragmentShader());
        
        // Skybox shader
        loadShader("skybox", getSkyboxVertexShader(), getSkyboxFragmentShader());
        
        logger.info("Built-in shaders loaded successfully");
    }
    
    /**
     * Cleans up all cached shaders.
     */
    public void cleanup() {
        logger.info("Cleaning up ShaderManager resources");
        
        for (Shader shader : shaderCache.values()) {
            shader.cleanup();
        }
        
        shaderCache.clear();
        shaderSources.clear();
        shaderTimestamps.clear();
    }
    
    // Built-in shader sources
    private String getBasicVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 position;
            layout (location = 1) in vec3 normal;
            layout (location = 2) in vec2 texCoord;
            
            uniform mat4 u_projectionMatrix;
            uniform mat4 u_viewMatrix;
            uniform mat4 u_modelMatrix;
            
            out vec3 fragPos;
            out vec3 fragNormal;
            out vec2 fragTexCoord;
            
            void main() {
                vec4 worldPos = u_modelMatrix * vec4(position, 1.0);
                fragPos = worldPos.xyz;
                fragNormal = mat3(transpose(inverse(u_modelMatrix))) * normal;
                fragTexCoord = texCoord;
                
                gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
            }
            """;
    }
    
    private String getBasicFragmentShader() {
        return """
            #version 330 core
            
            in vec3 fragPos;
            in vec3 fragNormal;
            in vec2 fragTexCoord;
            
            uniform vec3 u_color;
            
            out vec4 fragColor;
            
            void main() {
                vec3 normal = normalize(fragNormal);
                
                // Simple directional lighting
                vec3 lightDir = normalize(vec3(-0.5, -1.0, -0.3));
                float diff = max(dot(normal, lightDir), 0.0);
                
                // Ambient + diffuse lighting
                vec3 ambient = vec3(0.3);
                vec3 diffuse = vec3(0.7) * diff;
                
                vec3 result = (ambient + diffuse) * u_color;
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
                
                // Create waves using multiple sine waves
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
    
    private String getUIVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec2 position;
            layout (location = 1) in vec2 texCoord;
            layout (location = 2) in vec4 color;
            
            uniform mat4 projectionMatrix;
            
            out vec2 fragTexCoord;
            out vec4 fragColor;
            
            void main() {
                fragTexCoord = texCoord;
                fragColor = color;
                gl_Position = projectionMatrix * vec4(position, 0.0, 1.0);
            }
            """;
    }
    
    private String getUIFragmentShader() {
        return """
            #version 330 core
            
            in vec2 fragTexCoord;
            in vec4 fragColor;
            
            uniform sampler2D uiTexture;
            uniform bool useTexture;
            
            out vec4 outColor;
            
            void main() {
                if (useTexture) {
                    outColor = texture(uiTexture, fragTexCoord) * fragColor;
                } else {
                    outColor = fragColor;
                }
            }
            """;
    }
    
    private String getSkyboxVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 position;
            
            uniform mat4 projectionMatrix;
            uniform mat4 viewMatrix;
            
            out vec3 texCoords;
            
            void main() {
                texCoords = position;
                vec4 pos = projectionMatrix * viewMatrix * vec4(position, 1.0);
                gl_Position = pos.xyww; // Ensure skybox is always at far plane
            }
            """;
    }
    
    private String getSkyboxFragmentShader() {
        return """
            #version 330 core
            
            in vec3 texCoords;
            
            uniform samplerCube skybox;
            uniform vec3 fogColor;
            uniform float fogDensity;
            
            out vec4 fragColor;
            
            void main() {
                vec4 skyColor = texture(skybox, texCoords);
                
                // Apply atmospheric fog
                float distance = length(texCoords);
                float fogFactor = exp(-fogDensity * distance);
                fogFactor = clamp(fogFactor, 0.0, 1.0);
                
                fragColor = mix(vec4(fogColor, 1.0), skyColor, fogFactor);
            }
            """;
    }
}