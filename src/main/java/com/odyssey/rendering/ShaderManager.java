package com.odyssey.rendering;

import com.odyssey.core.ResourceManager;
import com.odyssey.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shader programs for The Odyssey rendering system.
 * 
 * Handles loading, compilation, caching, and hot-reloading of shaders.
 * Provides built-in shaders for common rendering tasks.
 */
public class ShaderManager {
    
    private static final Logger logger = Logger.getLogger(ShaderManager.class);
    
    // Shader cache
    private Map<String, Shader> shaders;
    
    // Shader source cache for hot-reloading
    private Map<String, ShaderSource> shaderSources;
    
    // Default shader directory
    private String shaderDirectory = "assets/shaders/";
    
    // Built-in shader names
    public static final String BASIC_SHADER = "basic";
    public static final String OCEAN_SHADER = "ocean";
    public static final String SHIP_SHADER = "ship";
    public static final String TERRAIN_SHADER = "terrain";
    public static final String SKYBOX_SHADER = "skybox";
    public static final String UI_SHADER = "ui";
    public static final String PARTICLE_SHADER = "particle";
    public static final String SHADOW_SHADER = "shadow";
    public static final String POST_PROCESS_SHADER = "post_process";
    
    /**
     * Container for shader source code.
     */
    private static class ShaderSource {
        String vertexSource;
        String fragmentSource;
        String geometrySource;
        Path vertexPath;
        Path fragmentPath;
        Path geometryPath;
        long lastModified;
        
        ShaderSource(String vertexSource, String fragmentSource) {
            this.vertexSource = vertexSource;
            this.fragmentSource = fragmentSource;
            this.lastModified = System.currentTimeMillis();
        }
        
        ShaderSource(String vertexSource, String geometrySource, String fragmentSource) {
            this(vertexSource, fragmentSource);
            this.geometrySource = geometrySource;
        }
    }
    
    /**
     * Create a new shader manager.
     */
    public ShaderManager() {
        this.shaders = new ConcurrentHashMap<>();
        this.shaderSources = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the shader manager and load built-in shaders.
     */
    public void initialize() {
        logger.info("Initializing shader manager...");
        
        // Create built-in shaders
        createBuiltInShaders();
        
        logger.info("Shader manager initialized with {} shaders", shaders.size());
    }
    
    /**
     * Create built-in shaders with embedded source code.
     */
    private void createBuiltInShaders() {
        // Basic shader for simple rendering
        createBasicShader();
        
        // Ocean shader for water rendering
        createOceanShader();
        
        // Ship shader for ship rendering
        createShipShader();
        
        // Terrain shader for island rendering
        createTerrainShader();
        
        // Skybox shader
        createSkyboxShader();
        
        // UI shader for interface elements
        createUIShader();
        
        // Particle shader for effects
        createParticleShader();
        
        // Shadow mapping shader
        createShadowShader();
        
        // Post-processing shader
        createPostProcessShader();
    }
    
    /**
     * Create basic shader for simple mesh rendering.
     */
    private void createBasicShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            layout (location = 1) in vec3 a_Normal;
            layout (location = 2) in vec2 a_TexCoord;
            
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            uniform mat4 u_NormalMatrix;
            
            out vec3 v_WorldPos;
            out vec3 v_Normal;
            out vec2 v_TexCoord;
            
            void main() {
                vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
                v_WorldPos = worldPos.xyz;
                v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);
                v_TexCoord = a_TexCoord;
                
                gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec3 v_WorldPos;
            in vec3 v_Normal;
            in vec2 v_TexCoord;
            
            uniform sampler2D u_Texture;
            uniform vec3 u_Color;
            uniform vec3 u_CameraPosition;
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            uniform float u_AmbientStrength;
            
            out vec4 FragColor;
            
            void main() {
                vec3 color = texture(u_Texture, v_TexCoord).rgb * u_Color;
                
                // Simple lighting
                vec3 ambient = u_AmbientStrength * u_LightColor;
                
                vec3 norm = normalize(v_Normal);
                vec3 lightDir = normalize(-u_LightDirection);
                float diff = max(dot(norm, lightDir), 0.0);
                vec3 diffuse = diff * u_LightColor;
                
                vec3 result = (ambient + diffuse) * color;
                FragColor = vec4(result, 1.0);
            }
            """;
        
        createShaderFromSource(BASIC_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create ocean shader for water rendering.
     */
    private void createOceanShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            layout (location = 1) in vec3 a_Normal;
            layout (location = 2) in vec2 a_TexCoord;
            
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            uniform float u_Time;
            uniform float u_WaveHeight;
            uniform float u_WaveFrequency;
            
            out vec3 v_WorldPos;
            out vec3 v_Normal;
            out vec2 v_TexCoord;
            out float v_WaveHeight;
            
            void main() {
                vec3 pos = a_Position;
                
                // Simple wave animation
                float wave = sin(pos.x * u_WaveFrequency + u_Time) * cos(pos.z * u_WaveFrequency + u_Time);
                pos.y += wave * u_WaveHeight;
                v_WaveHeight = wave;
                
                vec4 worldPos = u_ModelMatrix * vec4(pos, 1.0);
                v_WorldPos = worldPos.xyz;
                v_Normal = a_Normal; // TODO: Calculate wave normals
                v_TexCoord = a_TexCoord;
                
                gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec3 v_WorldPos;
            in vec3 v_Normal;
            in vec2 v_TexCoord;
            in float v_WaveHeight;
            
            uniform vec3 u_CameraPosition;
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            uniform vec3 u_WaterColor;
            uniform float u_Time;
            
            out vec4 FragColor;
            
            void main() {
                vec3 viewDir = normalize(u_CameraPosition - v_WorldPos);
                vec3 lightDir = normalize(-u_LightDirection);
                vec3 normal = normalize(v_Normal);
                
                // Water color based on depth and waves
                vec3 baseColor = u_WaterColor + vec3(0.1) * v_WaveHeight;
                
                // Simple fresnel effect
                float fresnel = pow(1.0 - max(dot(viewDir, normal), 0.0), 2.0);
                
                // Lighting
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
                
                vec3 color = baseColor * (0.3 + 0.7 * diff) + vec3(spec);
                color = mix(color, vec3(0.8, 0.9, 1.0), fresnel * 0.5);
                
                FragColor = vec4(color, 0.8 + fresnel * 0.2);
            }
            """;
        
        createShaderFromSource(OCEAN_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create ship shader for ship rendering.
     */
    private void createShipShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            layout (location = 1) in vec3 a_Normal;
            layout (location = 2) in vec2 a_TexCoord;
            
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            uniform mat4 u_NormalMatrix;
            
            out vec3 v_WorldPos;
            out vec3 v_Normal;
            out vec2 v_TexCoord;
            
            void main() {
                vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
                v_WorldPos = worldPos.xyz;
                v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);
                v_TexCoord = a_TexCoord;
                
                gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec3 v_WorldPos;
            in vec3 v_Normal;
            in vec2 v_TexCoord;
            
            uniform sampler2D u_DiffuseTexture;
            uniform sampler2D u_NormalTexture;
            uniform vec3 u_CameraPosition;
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            
            out vec4 FragColor;
            
            void main() {
                vec3 albedo = texture(u_DiffuseTexture, v_TexCoord).rgb;
                vec3 normal = normalize(v_Normal);
                
                // Lighting calculation
                vec3 lightDir = normalize(-u_LightDirection);
                vec3 viewDir = normalize(u_CameraPosition - v_WorldPos);
                
                // Ambient
                vec3 ambient = 0.2 * u_LightColor;
                
                // Diffuse
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 diffuse = diff * u_LightColor;
                
                // Specular
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64.0);
                vec3 specular = spec * u_LightColor * 0.5;
                
                vec3 result = (ambient + diffuse + specular) * albedo;
                FragColor = vec4(result, 1.0);
            }
            """;
        
        createShaderFromSource(SHIP_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create terrain shader for island rendering.
     */
    private void createTerrainShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            layout (location = 1) in vec3 a_Normal;
            layout (location = 2) in vec2 a_TexCoord;
            
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            uniform mat4 u_NormalMatrix;
            
            out vec3 v_WorldPos;
            out vec3 v_Normal;
            out vec2 v_TexCoord;
            out float v_Height;
            
            void main() {
                vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
                v_WorldPos = worldPos.xyz;
                v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);
                v_TexCoord = a_TexCoord;
                v_Height = a_Position.y;
                
                gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec3 v_WorldPos;
            in vec3 v_Normal;
            in vec2 v_TexCoord;
            in float v_Height;
            
            uniform sampler2D u_SandTexture;
            uniform sampler2D u_GrassTexture;
            uniform sampler2D u_RockTexture;
            uniform vec3 u_CameraPosition;
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            
            out vec4 FragColor;
            
            void main() {
                // Texture blending based on height
                vec3 sand = texture(u_SandTexture, v_TexCoord * 16.0).rgb;
                vec3 grass = texture(u_GrassTexture, v_TexCoord * 8.0).rgb;
                vec3 rock = texture(u_RockTexture, v_TexCoord * 4.0).rgb;
                
                float sandFactor = smoothstep(0.0, 2.0, v_Height);
                float grassFactor = smoothstep(2.0, 8.0, v_Height) * (1.0 - smoothstep(15.0, 25.0, v_Height));
                float rockFactor = smoothstep(15.0, 25.0, v_Height);
                
                vec3 albedo = mix(sand, grass, grassFactor);
                albedo = mix(albedo, rock, rockFactor);
                
                // Lighting
                vec3 normal = normalize(v_Normal);
                vec3 lightDir = normalize(-u_LightDirection);
                
                vec3 ambient = 0.3 * u_LightColor;
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 diffuse = diff * u_LightColor;
                
                vec3 result = (ambient + diffuse) * albedo;
                FragColor = vec4(result, 1.0);
            }
            """;
        
        createShaderFromSource(TERRAIN_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create skybox shader.
     */
    private void createSkyboxShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            
            out vec3 v_TexCoord;
            
            void main() {
                v_TexCoord = a_Position;
                
                // Remove translation from view matrix
                mat4 view = mat4(mat3(u_ViewMatrix));
                vec4 pos = u_ProjectionMatrix * view * vec4(a_Position, 1.0);
                
                // Ensure skybox is always at far plane
                gl_Position = pos.xyww;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec3 v_TexCoord;
            
            uniform samplerCube u_Skybox;
            uniform float u_Time;
            uniform vec3 u_SunDirection;
            
            out vec4 FragColor;
            
            void main() {
                vec3 color = texture(u_Skybox, v_TexCoord).rgb;
                
                // Simple atmospheric scattering effect
                float sunDot = max(dot(normalize(v_TexCoord), normalize(u_SunDirection)), 0.0);
                vec3 sunColor = vec3(1.0, 0.8, 0.6) * pow(sunDot, 256.0);
                
                color += sunColor;
                FragColor = vec4(color, 1.0);
            }
            """;
        
        createShaderFromSource(SKYBOX_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create UI shader for interface elements.
     */
    private void createUIShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec2 a_Position;
            layout (location = 1) in vec2 a_TexCoord;
            
            uniform mat4 u_ProjectionMatrix;
            uniform vec2 u_Position;
            uniform vec2 u_Scale;
            
            out vec2 v_TexCoord;
            
            void main() {
                vec2 pos = a_Position * u_Scale + u_Position;
                gl_Position = u_ProjectionMatrix * vec4(pos, 0.0, 1.0);
                v_TexCoord = a_TexCoord;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec2 v_TexCoord;
            
            uniform sampler2D u_Texture;
            uniform vec4 u_Color;
            
            out vec4 FragColor;
            
            void main() {
                vec4 texColor = texture(u_Texture, v_TexCoord);
                FragColor = texColor * u_Color;
            }
            """;
        
        createShaderFromSource(UI_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create particle shader for effects.
     */
    private void createParticleShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            layout (location = 1) in vec2 a_TexCoord;
            layout (location = 2) in vec4 a_Color;
            layout (location = 3) in float a_Size;
            
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;
            
            out vec2 v_TexCoord;
            out vec4 v_Color;
            
            void main() {
                gl_Position = u_ProjectionMatrix * u_ViewMatrix * vec4(a_Position, 1.0);
                gl_PointSize = a_Size;
                v_TexCoord = a_TexCoord;
                v_Color = a_Color;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec2 v_TexCoord;
            in vec4 v_Color;
            
            uniform sampler2D u_Texture;
            
            out vec4 FragColor;
            
            void main() {
                vec4 texColor = texture(u_Texture, v_TexCoord);
                FragColor = texColor * v_Color;
            }
            """;
        
        createShaderFromSource(PARTICLE_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create shadow mapping shader.
     */
    private void createShadowShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec3 a_Position;
            
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_LightSpaceMatrix;
            
            void main() {
                gl_Position = u_LightSpaceMatrix * u_ModelMatrix * vec4(a_Position, 1.0);
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            void main() {
                // Depth is automatically written to depth buffer
            }
            """;
        
        createShaderFromSource(SHADOW_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create post-processing shader.
     */
    private void createPostProcessShader() {
        String vertexSource = """
            #version 330 core
            
            layout (location = 0) in vec2 a_Position;
            layout (location = 1) in vec2 a_TexCoord;
            
            out vec2 v_TexCoord;
            
            void main() {
                gl_Position = vec4(a_Position, 0.0, 1.0);
                v_TexCoord = a_TexCoord;
            }
            """;
        
        String fragmentSource = """
            #version 330 core
            
            in vec2 v_TexCoord;
            
            uniform sampler2D u_ColorTexture;
            uniform float u_Exposure;
            uniform float u_Gamma;
            
            out vec4 FragColor;
            
            void main() {
                vec3 color = texture(u_ColorTexture, v_TexCoord).rgb;
                
                // Tone mapping
                color = vec3(1.0) - exp(-color * u_Exposure);
                
                // Gamma correction
                color = pow(color, vec3(1.0 / u_Gamma));
                
                FragColor = vec4(color, 1.0);
            }
            """;
        
        createShaderFromSource(POST_PROCESS_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create shader from source code.
     */
    private void createShaderFromSource(String name, String vertexSource, String fragmentSource) {
        try {
            Shader shader = Shader.create(name, vertexSource, fragmentSource);
            shaders.put(name, shader);
            shaderSources.put(name, new ShaderSource(vertexSource, fragmentSource));
            logger.debug("Created built-in shader: {}", name);
        } catch (Exception e) {
            logger.error("Failed to create built-in shader '{}': {}", name, e.getMessage());
        }
    }
    
    /**
     * Load shader from files.
     */
    public Shader loadShader(String name, String vertexFile, String fragmentFile) {
        try {
            Path vertexPath = Paths.get(shaderDirectory, vertexFile);
            Path fragmentPath = Paths.get(shaderDirectory, fragmentFile);
            
            String vertexSource = Files.readString(vertexPath);
            String fragmentSource = Files.readString(fragmentPath);
            
            Shader shader = Shader.create(name, vertexSource, fragmentSource);
            shaders.put(name, shader);
            
            // Store source for hot-reloading
            ShaderSource source = new ShaderSource(vertexSource, fragmentSource);
            source.vertexPath = vertexPath;
            source.fragmentPath = fragmentPath;
            source.lastModified = Math.max(
                Files.getLastModifiedTime(vertexPath).toMillis(),
                Files.getLastModifiedTime(fragmentPath).toMillis()
            );
            shaderSources.put(name, source);
            
            logger.info("Loaded shader '{}' from files: {}, {}", name, vertexFile, fragmentFile);
            return shader;
            
        } catch (IOException e) {
            logger.error("Failed to load shader '{}': {}", name, e.getMessage());
            return null;
        }
    }
    
    /**
     * Load shader with geometry shader from files.
     */
    public Shader loadShader(String name, String vertexFile, String geometryFile, String fragmentFile) {
        try {
            Path vertexPath = Paths.get(shaderDirectory, vertexFile);
            Path geometryPath = Paths.get(shaderDirectory, geometryFile);
            Path fragmentPath = Paths.get(shaderDirectory, fragmentFile);
            
            String vertexSource = Files.readString(vertexPath);
            String geometrySource = Files.readString(geometryPath);
            String fragmentSource = Files.readString(fragmentPath);
            
            Shader shader = Shader.create(name, vertexSource, geometrySource, fragmentSource);
            shaders.put(name, shader);
            
            // Store source for hot-reloading
            ShaderSource source = new ShaderSource(vertexSource, geometrySource, fragmentSource);
            source.vertexPath = vertexPath;
            source.geometryPath = geometryPath;
            source.fragmentPath = fragmentPath;
            source.lastModified = Math.max(
                Math.max(
                    Files.getLastModifiedTime(vertexPath).toMillis(),
                    Files.getLastModifiedTime(geometryPath).toMillis()
                ),
                Files.getLastModifiedTime(fragmentPath).toMillis()
            );
            shaderSources.put(name, source);
            
            logger.info("Loaded shader '{}' from files: {}, {}, {}", name, vertexFile, geometryFile, fragmentFile);
            return shader;
            
        } catch (IOException e) {
            logger.error("Failed to load shader '{}': {}", name, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get shader by name.
     */
    public Shader getShader(String name) {
        return shaders.get(name);
    }
    
    /**
     * Check if shader exists.
     */
    public boolean hasShader(String name) {
        return shaders.containsKey(name);
    }
    
    /**
     * Remove shader.
     */
    public void removeShader(String name) {
        Shader shader = shaders.remove(name);
        if (shader != null) {
            shader.cleanup();
        }
        shaderSources.remove(name);
        logger.debug("Removed shader: {}", name);
    }
    
    /**
     * Reload shader from files (hot-reloading).
     */
    public boolean reloadShader(String name) {
        ShaderSource source = shaderSources.get(name);
        if (source == null || source.vertexPath == null || source.fragmentPath == null) {
            logger.warn("Cannot reload shader '{}': no file paths stored", name);
            return false;
        }
        
        try {
            // Check if files have been modified
            long vertexModified = Files.getLastModifiedTime(source.vertexPath).toMillis();
            long fragmentModified = Files.getLastModifiedTime(source.fragmentPath).toMillis();
            long geometryModified = 0;
            
            if (source.geometryPath != null) {
                geometryModified = Files.getLastModifiedTime(source.geometryPath).toMillis();
            }
            
            long maxModified = Math.max(Math.max(vertexModified, fragmentModified), geometryModified);
            
            if (maxModified <= source.lastModified) {
                return false; // No changes
            }
            
            // Reload shader files
            String vertexSource = Files.readString(source.vertexPath);
            String fragmentSource = Files.readString(source.fragmentPath);
            String geometrySource = null;
            
            if (source.geometryPath != null) {
                geometrySource = Files.readString(source.geometryPath);
            }
            
            Shader shader = shaders.get(name);
            if (shader != null) {
                if (geometrySource != null) {
                    // TODO: Implement geometry shader reloading
                    logger.warn("Geometry shader reloading not yet implemented for '{}'", name);
                    return false;
                } else {
                    shader.reload(vertexSource, fragmentSource);
                }
                
                // Update source cache
                source.vertexSource = vertexSource;
                source.fragmentSource = fragmentSource;
                source.geometrySource = geometrySource;
                source.lastModified = maxModified;
                
                logger.info("Reloaded shader: {}", name);
                return true;
            }
            
        } catch (IOException e) {
            logger.error("Failed to reload shader '{}': {}", name, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Reload all shaders that have been modified.
     */
    public int reloadModifiedShaders() {
        int reloadedCount = 0;
        
        for (String name : shaderSources.keySet()) {
            if (reloadShader(name)) {
                reloadedCount++;
            }
        }
        
        if (reloadedCount > 0) {
            logger.info("Reloaded {} modified shaders", reloadedCount);
        }
        
        return reloadedCount;
    }
    
    /**
     * Set shader directory.
     */
    public void setShaderDirectory(String directory) {
        this.shaderDirectory = directory;
        if (!directory.endsWith("/")) {
            this.shaderDirectory += "/";
        }
    }
    
    /**
     * Get all shader names.
     */
    public String[] getShaderNames() {
        return shaders.keySet().toArray(new String[0]);
    }
    
    /**
     * Get shader count.
     */
    public int getShaderCount() {
        return shaders.size();
    }
    
    /**
     * Cleanup all shaders.
     */
    public void cleanup() {
        logger.info("Cleaning up shader manager...");
        
        for (Shader shader : shaders.values()) {
            shader.cleanup();
        }
        
        shaders.clear();
        shaderSources.clear();
        
        logger.info("Shader manager cleanup complete");
    }
}