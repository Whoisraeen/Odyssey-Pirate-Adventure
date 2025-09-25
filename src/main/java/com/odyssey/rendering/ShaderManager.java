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
    private String shaderDirectory = "src/main/resources/shaders/";
    
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
        loadShader("pbr", "src/main/resources/shaders/pbr.vert", "src/main/resources/shaders/pbr.frag");
    }
    
    /**
     * Create basic shader for simple mesh rendering.
     */
    private void createBasicShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec3 a_Normal;\n" +
                "layout (location = 2) in vec2 a_TexCoord;\n" +
                "\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_NormalMatrix;\n" +
                "\n" +
                "out vec3 v_WorldPos;\n" +
                "out vec3 v_Normal;\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "    v_WorldPos = worldPos.xyz;\n" +
                "    v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_WorldPos;\n" +
                "in vec3 v_Normal;\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform vec3 u_Color;\n" +
                "uniform vec3 u_CameraPosition;\n" +
                "uniform vec3 u_LightDirection;\n" +
                "uniform vec3 u_LightColor;\n" +
                "uniform float u_AmbientStrength;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 color = texture(u_Texture, v_TexCoord).rgb * u_Color;\n" +
                "    \n" +
                "    // Simple lighting\n" +
                "    vec3 ambient = u_AmbientStrength * u_LightColor;\n" +
                "    \n" +
                "    vec3 norm = normalize(v_Normal);\n" +
                "    vec3 lightDir = normalize(-u_LightDirection);\n" +
                "    float diff = max(dot(norm, lightDir), 0.0);\n" +
                "    vec3 diffuse = diff * u_LightColor;\n" +
                "    \n" +
                "    vec3 result = (ambient + diffuse) * color;\n" +
                "    FragColor = vec4(result, 1.0);\n" +
                "}";
        
        createShaderFromSource(BASIC_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create ocean shader for water rendering.
     */
    private void createOceanShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec3 a_Normal;\n" +
                "layout (location = 2) in vec2 a_TexCoord;\n" +
                "\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_WaveHeight;\n" +
                "uniform float u_WaveFrequency;\n" +
                "\n" +
                "out vec3 v_WorldPos;\n" +
                "out vec3 v_Normal;\n" +
                "out vec2 v_TexCoord;\n" +
                "out float v_WaveHeight;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 pos = a_Position;\n" +
                "    \n" +
                "    // Simple wave animation\n" +
                "    float wave = sin(pos.x * u_WaveFrequency + u_Time) * cos(pos.z * u_WaveFrequency + u_Time);\n" +
                "    pos.y += wave * u_WaveHeight;\n" +
                "    v_WaveHeight = wave;\n" +
                "    \n" +
                "    vec4 worldPos = u_ModelMatrix * vec4(pos, 1.0);\n" +
                "    v_WorldPos = worldPos.xyz;\n" +
                "    v_Normal = a_Normal; // TODO: Calculate wave normals\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_WorldPos;\n" +
                "in vec3 v_Normal;\n" +
                "in vec2 v_TexCoord;\n" +
                "in float v_WaveHeight;\n" +
                "\n" +
                "uniform vec3 u_CameraPosition;\n" +
                "uniform vec3 u_LightDirection;\n" +
                "uniform vec3 u_LightColor;\n" +
                "uniform vec3 u_WaterColor;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 viewDir = normalize(u_CameraPosition - v_WorldPos);\n" +
                "    vec3 lightDir = normalize(-u_LightDirection);\n" +
                "    vec3 normal = normalize(v_Normal);\n" +
                "    \n" +
                "    // Water color based on depth and waves\n" +
                "    vec3 baseColor = u_WaterColor + vec3(0.1) * v_WaveHeight;\n" +
                "    \n" +
                "    // Simple fresnel effect\n" +
                "    float fresnel = pow(1.0 - max(dot(viewDir, normal), 0.0), 2.0);\n" +
                "    \n" +
                "    // Lighting\n" +
                "    float diff = max(dot(normal, lightDir), 0.0);\n" +
                "    vec3 reflectDir = reflect(-lightDir, normal);\n" +
                "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);\n" +
                "    \n" +
                "    vec3 color = baseColor * (0.3 + 0.7 * diff) + vec3(spec);\n" +
                "    color = mix(color, vec3(0.8, 0.9, 1.0), fresnel * 0.5);\n" +
                "    \n" +
                "    FragColor = vec4(color, 0.8 + fresnel * 0.2);\n" +
                "}";
        
        createShaderFromSource(OCEAN_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create ship shader for ship rendering.
     */
    private void createShipShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec3 a_Normal;\n" +
                "layout (location = 2) in vec2 a_TexCoord;\n" +
                "\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_NormalMatrix;\n" +
                "\n" +
                "out vec3 v_WorldPos;\n" +
                "out vec3 v_Normal;\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "    v_WorldPos = worldPos.xyz;\n" +
                "    v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_WorldPos;\n" +
                "in vec3 v_Normal;\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_DiffuseTexture;\n" +
                "uniform sampler2D u_NormalTexture;\n" +
                "uniform vec3 u_CameraPosition;\n" +
                "uniform vec3 u_LightDirection;\n" +
                "uniform vec3 u_LightColor;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 albedo = texture(u_DiffuseTexture, v_TexCoord).rgb;\n" +
                "    vec3 normal = normalize(v_Normal);\n" +
                "    \n" +
                "    // Lighting calculation\n" +
                "    vec3 lightDir = normalize(-u_LightDirection);\n" +
                "    vec3 viewDir = normalize(u_CameraPosition - v_WorldPos);\n" +
                "    \n" +
                "    // Ambient\n" +
                "    vec3 ambient = 0.2 * u_LightColor;\n" +
                "    \n" +
                "    // Diffuse\n" +
                "    float diff = max(dot(normal, lightDir), 0.0);\n" +
                "    vec3 diffuse = diff * u_LightColor;\n" +
                "    \n" +
                "    // Specular\n" +
                "    vec3 reflectDir = reflect(-lightDir, normal);\n" +
                "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64.0);\n" +
                "    vec3 specular = spec * u_LightColor * 0.5;\n" +
                "    \n" +
                "    vec3 result = (ambient + diffuse + specular) * albedo;\n" +
                "    FragColor = vec4(result, 1.0);\n" +
                "}";
        
        createShaderFromSource(SHIP_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create terrain shader for island rendering.
     */
    private void createTerrainShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec3 a_Normal;\n" +
                "layout (location = 2) in vec2 a_TexCoord;\n" +
                "\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_NormalMatrix;\n" +
                "\n" +
                "out vec3 v_WorldPos;\n" +
                "out vec3 v_Normal;\n" +
                "out vec2 v_TexCoord;\n" +
                "out float v_Height;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "    v_WorldPos = worldPos.xyz;\n" +
                "    v_Normal = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    v_Height = a_Position.y;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * worldPos;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_WorldPos;\n" +
                "in vec3 v_Normal;\n" +
                "in vec2 v_TexCoord;\n" +
                "in float v_Height;\n" +
                "\n" +
                "uniform sampler2D u_SandTexture;\n" +
                "uniform sampler2D u_GrassTexture;\n" +
                "uniform sampler2D u_RockTexture;\n" +
                "uniform vec3 u_CameraPosition;\n" +
                "uniform vec3 u_LightDirection;\n" +
                "uniform vec3 u_LightColor;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    // Texture blending based on height\n" +
                "    vec3 sand = texture(u_SandTexture, v_TexCoord * 16.0).rgb;\n" +
                "    vec3 grass = texture(u_GrassTexture, v_TexCoord * 8.0).rgb;\n" +
                "    vec3 rock = texture(u_RockTexture, v_TexCoord * 4.0).rgb;\n" +
                "    \n" +
                "    float sandFactor = smoothstep(0.0, 2.0, v_Height);\n" +
                "    float grassFactor = smoothstep(2.0, 8.0, v_Height) * (1.0 - smoothstep(15.0, 25.0, v_Height));\n" +
                "    float rockFactor = smoothstep(15.0, 25.0, v_Height);\n" +
                "    \n" +
                "    vec3 albedo = mix(sand, grass, grassFactor);\n" +
                "    albedo = mix(albedo, rock, rockFactor);\n" +
                "    \n" +
                "    // Lighting\n" +
                "    vec3 normal = normalize(v_Normal);\n" +
                "    vec3 lightDir = normalize(-u_LightDirection);\n" +
                "    \n" +
                "    vec3 ambient = 0.3 * u_LightColor;\n" +
                "    float diff = max(dot(normal, lightDir), 0.0);\n" +
                "    vec3 diffuse = diff * u_LightColor;\n" +
                "    \n" +
                "    vec3 result = (ambient + diffuse) * albedo;\n" +
                "    FragColor = vec4(result, 1.0);\n" +
                "}";
        
        createShaderFromSource(TERRAIN_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create skybox shader.
     */
    private void createSkyboxShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "\n" +
                "out vec3 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoord = a_Position;\n" +
                "    \n" +
                "    // Remove translation from view matrix\n" +
                "    mat4 view = mat4(mat3(u_ViewMatrix));\n" +
                "    vec4 pos = u_ProjectionMatrix * view * vec4(a_Position, 1.0);\n" +
                "    \n" +
                "    // Ensure skybox is always at far plane\n" +
                "    gl_Position = pos.xyww;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_TexCoord;\n" +
                "\n" +
                "uniform samplerCube u_Skybox;\n" +
                "uniform float u_Time;\n" +
                "uniform vec3 u_SunDirection;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 color = texture(u_Skybox, v_TexCoord).rgb;\n" +
                "    \n" +
                "    // Simple atmospheric scattering effect\n" +
                "    float sunDot = max(dot(normalize(v_TexCoord), normalize(u_SunDirection)), 0.0);\n" +
                "    vec3 sunColor = vec3(1.0, 0.8, 0.6) * pow(sunDot, 256.0);\n" +
                "    \n" +
                "    color += sunColor;\n" +
                "    FragColor = vec4(color, 1.0);\n" +
                "}";
        
        createShaderFromSource(SKYBOX_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create UI shader for interface elements.
     */
    private void createUIShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec2 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform vec2 u_Position;\n" +
                "uniform vec2 u_Scale;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 pos = a_Position * u_Scale + u_Position;\n" +
                "    gl_Position = u_ProjectionMatrix * vec4(pos, 0.0, 1.0);\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform vec4 u_Color;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
                "    FragColor = texColor * u_Color;\n" +
                "}";
        
        createShaderFromSource(UI_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create particle shader for effects.
     */
    private void createParticleShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "layout (location = 2) in vec4 a_Color;\n" +
                "layout (location = 3) in float a_Size;\n" +
                "\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "out vec4 v_Color;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * vec4(a_Position, 1.0);\n" +
                "    gl_PointSize = a_Size;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    v_Color = a_Color;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "in vec4 v_Color;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
                "    FragColor = texColor * v_Color;\n" +
                "}";
        
        createShaderFromSource(PARTICLE_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create shadow mapping shader.
     */
    private void createShadowShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "uniform mat4 u_LightSpaceMatrix;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = u_LightSpaceMatrix * u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "\n" +
                "void main() {\n" +
                "    // Depth is automatically written to depth buffer\n" +
                "}";
        
        createShaderFromSource(SHADOW_SHADER, vertexSource, fragmentSource);
    }
    
    /**
     * Create post-processing shader.
     */
    private void createPostProcessShader() {
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec2 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_ColorTexture;\n" +
                "uniform float u_Exposure;\n" +
                "uniform float u_Gamma;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 color = texture(u_ColorTexture, v_TexCoord).rgb;\n" +
                "    \n" +
                "    // Tone mapping\n" +
                "    color = vec3(1.0) - exp(-color * u_Exposure);\n" +
                "    \n" +
                "    // Gamma correction\n" +
                "    color = pow(color, vec3(1.0 / u_Gamma));\n" +
                "    \n" +
                "    FragColor = vec4(color, 1.0);\n" +
                "}";
        
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
            String vertexSource = ResourceManager.getInstance().loadResource(vertexFile);
            String fragmentSource = ResourceManager.getInstance().loadResource(fragmentFile);
            
            Shader shader = Shader.create(name, vertexSource, fragmentSource);
            shaders.put(name, shader);
            
            // Store source for hot-reloading
            ShaderSource source = new ShaderSource(vertexSource, fragmentSource);
            // source.vertexPath = Paths.get(vertexFile);
            // source.fragmentPath = Paths.get(fragmentFile);
            // source.lastModified = Math.max(
            //     Files.getLastModifiedTime(source.vertexPath).toMillis(),
            //     Files.getLastModifiedTime(source.fragmentPath).toMillis()
            // );
            shaderSources.put(name, source);
            
            logger.info("Loaded shader '{}' from files: {}, {}", name, vertexFile, fragmentFile);
            return shader;
            
        } catch (Exception e) {
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