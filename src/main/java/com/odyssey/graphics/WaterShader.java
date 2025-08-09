package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced water shader system with Gerstner waves, reflections, and refraction.
 * Provides realistic water rendering with multiple wave layers and lighting effects.
 */
public class WaterShader {
    private static final Logger logger = LoggerFactory.getLogger(WaterShader.class);
    
    /**
     * Water quality settings for performance scaling.
     */
    public enum WaterQuality {
        LOW(2, false, false),
        MEDIUM(4, true, false),
        HIGH(6, true, true),
        ULTRA(8, true, true);
        
        private final int waveCount;
        private final boolean enableReflections;
        private final boolean enableRefraction;
        
        WaterQuality(int waveCount, boolean enableReflections, boolean enableRefraction) {
            this.waveCount = waveCount;
            this.enableReflections = enableReflections;
            this.enableRefraction = enableRefraction;
        }
        
        public int getWaveCount() { return waveCount; }
        public boolean isReflectionsEnabled() { return enableReflections; }
        public boolean isRefractionEnabled() { return enableRefraction; }
    }
    
    /**
     * Gerstner wave parameters for realistic wave simulation.
     */
    public static class GerstnerWave {
        public Vector3f direction;
        public float amplitude;
        public float wavelength;
        public float speed;
        public float steepness;
        
        public GerstnerWave(Vector3f direction, float amplitude, float wavelength, float speed, float steepness) {
            this.direction = new Vector3f(direction).normalize();
            this.amplitude = amplitude;
            this.wavelength = wavelength;
            this.speed = speed;
            this.steepness = Math.min(steepness, 1.0f / (wavelength * amplitude * 4.0f)); // Prevent loops
        }
        
        public float getFrequency() {
            return 2.0f * (float)Math.PI / wavelength;
        }
        
        public float getPhaseConstant() {
            return speed * getFrequency();
        }
    }
    
    private final Shader waterShader;
    private final WaterQuality quality;
    private final GerstnerWave[] waves;
    
    // Water properties
    private Vector3f deepWaterColor = new Vector3f(0.0f, 0.2f, 0.4f);
    private Vector3f shallowWaterColor = new Vector3f(0.0f, 0.4f, 0.6f);
    private Vector3f foamColor = new Vector3f(0.8f, 0.9f, 1.0f);
    private float transparency = 0.8f;
    private float roughness = 0.1f;
    private float metallic = 0.0f;
    private float fresnelStrength = 1.0f;
    private float causticsStrength = 0.3f;
    
    // Environmental properties
    private float weatherIntensity = 0.0f;
    private Vector3f biomeWaterColor = new Vector3f(0.0f, 0.4f, 0.6f);
    private float underwaterDepth = 0.0f;
    
    // Reflection and refraction
    private int reflectionFramebuffer = 0;
    private int reflectionTexture = 0;
    private int reflectionDepthBuffer = 0;
    private int refractionFramebuffer = 0;
    private int refractionTexture = 0;
    private int refractionDepthTexture = 0;
    private int dudvTexture = 0;
    private int normalTexture = 0;
    private int causticsTexture = 0;
    
    private final int reflectionWidth = 1024;
    private final int reflectionHeight = 1024;
    private final int refractionWidth = 1280;
    private final int refractionHeight = 720;
    
    /**
     * Creates a new water shader system.
     * @param shaderManager The shader manager to use for shader creation
     * @param quality The water quality settings
     */
    public WaterShader(ShaderManager shaderManager, WaterQuality quality) {
        this.quality = quality;
        this.waves = generateDefaultWaves(quality.getWaveCount());
        
        // Create water shader
        String vertexShader = generateVertexShader();
        String fragmentShader = generateFragmentShader();
        try {
            this.waterShader = shaderManager.loadShader("water_advanced", vertexShader, fragmentShader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create water shader", e);
        }
        
        if (waterShader == null) {
            throw new RuntimeException("Failed to create water shader");
        }
        
        // Initialize framebuffers for reflections and refractions
        if (quality.isReflectionsEnabled()) {
            initializeReflectionFramebuffer();
        }
        if (quality.isRefractionEnabled()) {
            initializeRefractionFramebuffer();
        }
        
        // Load water textures
        loadWaterTextures();
        
        logger.info("Initialized water shader system with quality: " + quality);
    }
    
    /**
     * Generates default Gerstner waves for realistic water simulation.
     */
    private GerstnerWave[] generateDefaultWaves(int waveCount) {
        GerstnerWave[] waves = new GerstnerWave[waveCount];
        
        // Primary wave (largest)
        waves[0] = new GerstnerWave(
            new Vector3f(1.0f, 0.0f, 0.3f),
            1.2f, 60.0f, 2.0f, 0.8f
        );
        
        if (waveCount > 1) {
            // Secondary wave
            waves[1] = new GerstnerWave(
                new Vector3f(0.7f, 0.0f, -0.7f),
                0.8f, 31.0f, 1.8f, 0.6f
            );
        }
        
        // Generate additional smaller waves
        for (int i = 2; i < waveCount; i++) {
            float angle = (float)(Math.PI * 2.0 * i / waveCount);
            Vector3f direction = new Vector3f(
                (float)Math.cos(angle),
                0.0f,
                (float)Math.sin(angle)
            );
            
            float amplitude = 0.3f / (i + 1);
            float wavelength = 15.0f / (i + 1);
            float speed = 1.5f + (float)Math.random() * 0.5f;
            float steepness = 0.4f / (i + 1);
            
            waves[i] = new GerstnerWave(direction, amplitude, wavelength, speed, steepness);
        }
        
        return waves;
    }
    
    /**
     * Generates the vertex shader for advanced water rendering.
     */
    private String generateVertexShader() {
        StringBuilder shader = new StringBuilder();
        shader.append("#version 330 core\n");
        shader.append("\n");
        shader.append("layout (location = 0) in vec3 position;\n");
        shader.append("layout (location = 1) in vec3 normal;\n");
        shader.append("layout (location = 2) in vec2 texCoord;\n");
        shader.append("\n");
        shader.append("uniform mat4 projectionMatrix;\n");
        shader.append("uniform mat4 viewMatrix;\n");
        shader.append("uniform mat4 modelMatrix;\n");
        shader.append("uniform float time;\n");
        shader.append("uniform vec3 cameraPosition;\n");
        shader.append("\n");
        
        // Wave uniforms
        for (int i = 0; i < quality.getWaveCount(); i++) {
            shader.append("uniform vec3 waveDirection").append(i).append(";\n");
            shader.append("uniform float waveAmplitude").append(i).append(";\n");
            shader.append("uniform float waveFrequency").append(i).append(";\n");
            shader.append("uniform float wavePhase").append(i).append(";\n");
            shader.append("uniform float waveSteepness").append(i).append(";\n");
        }
        
        shader.append("\n");
        shader.append("out vec3 fragPos;\n");
        shader.append("out vec3 fragNormal;\n");
        shader.append("out vec2 fragTexCoord;\n");
        shader.append("out vec4 clipSpace;\n");
        shader.append("out vec3 toCameraVector;\n");
        shader.append("out float waveHeight;\n");
        shader.append("\n");
        shader.append("void main() {\n");
        shader.append("    vec3 pos = position;\n");
        shader.append("    vec3 normal = vec3(0.0, 1.0, 0.0);\n");
        shader.append("    float totalHeight = 0.0;\n");
        shader.append("\n");
        
        // Enhanced Gerstner wave calculation with improved displacement
        for (int i = 0; i < quality.getWaveCount(); i++) {
            shader.append("    // Enhanced Gerstner Wave ").append(i).append("\n");
            shader.append("    float phase").append(i).append(" = dot(waveDirection").append(i).append(".xz, pos.xz) * waveFrequency").append(i).append(" + time * wavePhase").append(i).append(";\n");
            shader.append("    float waveValue").append(i).append(" = sin(phase").append(i).append(");\n");
            shader.append("    float waveDerivative").append(i).append(" = cos(phase").append(i).append(");\n");
            shader.append("    float waveSecondDerivative").append(i).append(" = -sin(phase").append(i).append(");\n");
            shader.append("    \n");
            shader.append("    // Gerstner wave displacement (creates realistic cresting)\n");
            shader.append("    float steepnessFactor").append(i).append(" = waveSteepness").append(i).append(" * waveAmplitude").append(i).append(";\n");
            shader.append("    pos.x += waveDirection").append(i).append(".x * steepnessFactor").append(i).append(" * waveValue").append(i).append(";\n");
            shader.append("    pos.z += waveDirection").append(i).append(".z * steepnessFactor").append(i).append(" * waveValue").append(i).append(";\n");
            shader.append("    pos.y += waveAmplitude").append(i).append(" * waveValue").append(i).append(";\n");
            shader.append("    \n");
            shader.append("    // Dynamic normal calculation for realistic lighting\n");
            shader.append("    float normalFactor").append(i).append(" = waveFrequency").append(i).append(" * waveAmplitude").append(i).append(";\n");
            shader.append("    normal.x -= waveDirection").append(i).append(".x * normalFactor").append(i).append(" * waveDerivative").append(i).append(";\n");
            shader.append("    normal.z -= waveDirection").append(i).append(".z * normalFactor").append(i).append(" * waveDerivative").append(i).append(";\n");
            shader.append("    \n");
            shader.append("    // Add wave interaction and foam generation\n");
            shader.append("    totalHeight += waveAmplitude").append(i).append(" * waveValue").append(i).append(";\n");
            shader.append("\n");
        }
        
        shader.append("    normal = normalize(normal);\n");
        shader.append("    waveHeight = totalHeight;\n");
        shader.append("\n");
        shader.append("    vec4 worldPos = modelMatrix * vec4(pos, 1.0);\n");
        shader.append("    fragPos = worldPos.xyz;\n");
        shader.append("    fragNormal = normalize((modelMatrix * vec4(normal, 0.0)).xyz);\n");
        shader.append("    fragTexCoord = texCoord;\n");
        shader.append("    \n");
        shader.append("    clipSpace = projectionMatrix * viewMatrix * worldPos;\n");
        shader.append("    toCameraVector = cameraPosition - worldPos.xyz;\n");
        shader.append("    \n");
        shader.append("    gl_Position = clipSpace;\n");
        shader.append("}\n");
        
        return shader.toString();
    }
    
    /**
     * Generates the fragment shader for advanced water rendering.
     */
    private String generateFragmentShader() {
        StringBuilder shader = new StringBuilder();
        shader.append("#version 330 core\n");
        shader.append("\n");
        shader.append("in vec3 fragPos;\n");
        shader.append("in vec3 fragNormal;\n");
        shader.append("in vec2 fragTexCoord;\n");
        shader.append("in vec4 clipSpace;\n");
        shader.append("in vec3 toCameraVector;\n");
        shader.append("in float waveHeight;\n");
        shader.append("\n");
        shader.append("uniform vec3 lightDirection;\n");
        shader.append("uniform vec3 lightColor;\n");
        shader.append("uniform vec3 ambientColor;\n");
        shader.append("uniform float time;\n");
        shader.append("uniform vec3 deepWaterColor;\n");
        shader.append("uniform vec3 shallowWaterColor;\n");
        shader.append("uniform vec3 foamColor;\n");
        shader.append("uniform float transparency;\n");
        shader.append("uniform float roughness;\n");
        shader.append("uniform float fresnelStrength;\n");
        shader.append("uniform float metallic;\n");
        shader.append("uniform vec3 cameraPosition;\n");
        shader.append("uniform float weatherIntensity;\n");
        shader.append("uniform vec3 biomeWaterColor;\n");
        shader.append("uniform float underwaterDepth;\n");
        shader.append("\n");
        
        if (quality.isReflectionsEnabled()) {
            shader.append("uniform sampler2D reflectionTexture;\n");
        }
        if (quality.isRefractionEnabled()) {
            shader.append("uniform sampler2D refractionTexture;\n");
            shader.append("uniform sampler2D refractionDepthTexture;\n");
        }
        shader.append("uniform sampler2D dudvTexture;\n");
        shader.append("uniform sampler2D normalTexture;\n");
        shader.append("uniform sampler2D causticsTexture;\n");
        shader.append("uniform float causticsStrength;\n");
        shader.append("\n");
        shader.append("out vec4 fragColor;\n");
        shader.append("\n");
        shader.append("void main() {\n");
        shader.append("    vec3 normal = normalize(fragNormal);\n");
        shader.append("    vec3 lightDir = normalize(-lightDirection);\n");
        shader.append("    vec3 viewDir = normalize(toCameraVector);\n");
        shader.append("    \n");
        
        // Screen space coordinates for reflections/refractions
        if (quality.isReflectionsEnabled() || quality.isRefractionEnabled()) {
            shader.append("    vec2 ndc = (clipSpace.xy / clipSpace.w) / 2.0 + 0.5;\n");
            shader.append("    vec2 reflectionTexCoords = vec2(ndc.x, -ndc.y);\n");
            shader.append("    vec2 refractionTexCoords = ndc;\n");
            shader.append("    \n");
            
            // Distortion from DuDv map
            shader.append("    vec2 distortedTexCoords = texture(dudvTexture, vec2(fragTexCoord.x + time * 0.02, fragTexCoord.y)).rg * 0.1;\n");
            shader.append("    distortedTexCoords = fragTexCoord + vec2(distortedTexCoords.x, distortedTexCoords.y + time * 0.02);\n");
            shader.append("    vec2 totalDistortion = (texture(dudvTexture, distortedTexCoords).rg * 2.0 - 1.0) * 0.02;\n");
            shader.append("    \n");
            shader.append("    reflectionTexCoords += totalDistortion;\n");
            shader.append("    reflectionTexCoords.x = clamp(reflectionTexCoords.x, 0.001, 0.999);\n");
            shader.append("    reflectionTexCoords.y = clamp(reflectionTexCoords.y, -0.999, -0.001);\n");
            shader.append("    \n");
            shader.append("    refractionTexCoords += totalDistortion;\n");
            shader.append("    refractionTexCoords = clamp(refractionTexCoords, 0.001, 0.999);\n");
            shader.append("    \n");
        }
        
        // Sample textures
        if (quality.isReflectionsEnabled()) {
            shader.append("    vec4 reflectionColor = texture(reflectionTexture, reflectionTexCoords);\n");
        } else {
            shader.append("    vec4 reflectionColor = vec4(0.5, 0.7, 1.0, 1.0);\n"); // Sky color fallback
        }
        
        if (quality.isRefractionEnabled()) {
            shader.append("    vec4 refractionColor = texture(refractionTexture, refractionTexCoords);\n");
        } else {
            shader.append("    vec4 refractionColor = vec4(deepWaterColor, 1.0);\n");
        }
        
        // Normal mapping
        shader.append("    vec4 normalMapColor = texture(normalTexture, distortedTexCoords);\n");
        shader.append("    vec3 normalMapNormal = vec3(normalMapColor.r * 2.0 - 1.0, normalMapColor.b, normalMapColor.g * 2.0 - 1.0);\n");
        shader.append("    normal = normalize(normal + normalMapNormal * 0.5);\n");
        shader.append("    \n");
        
        // Fresnel effect
        shader.append("    float fresnel = dot(viewDir, normal);\n");
        shader.append("    fresnel = pow(1.0 - fresnel, fresnelStrength);\n");
        shader.append("    fresnel = clamp(fresnel, 0.0, 1.0);\n");
        shader.append("    \n");
        
        // Water color based on depth and waves
        shader.append("    float waveIntensity = abs(waveHeight) * 2.0;\n");
        shader.append("    vec3 waterColor = mix(deepWaterColor, shallowWaterColor, waveIntensity);\n");
        shader.append("    if (waveHeight > 0.4) {\n");
        shader.append("        waterColor = mix(waterColor, foamColor, (waveHeight - 0.4) * 2.0);\n");
        shader.append("    }\n");
        shader.append("    \n");
        
        // Caustics calculation
        shader.append("    vec2 causticsCoords1 = fragTexCoord * 4.0 + vec2(time * 0.03, time * 0.02);\n");
        shader.append("    vec2 causticsCoords2 = fragTexCoord * 3.0 + vec2(-time * 0.02, time * 0.04);\n");
        shader.append("    float caustics1 = texture(causticsTexture, causticsCoords1).r;\n");
        shader.append("    float caustics2 = texture(causticsTexture, causticsCoords2).r;\n");
        shader.append("    float causticsPattern = min(caustics1, caustics2) * causticsStrength;\n");
        shader.append("    \n");
        
        // Enhanced PBR lighting calculations
        shader.append("    // Ambient lighting with underwater depth consideration\n");
        shader.append("    vec3 ambient = ambientColor * mix(0.6, 0.2, underwaterDepth);\n");
        shader.append("    \n");
        shader.append("    // Diffuse lighting\n");
        shader.append("    float NdotL = max(dot(normal, lightDir), 0.0);\n");
        shader.append("    vec3 diffuse = NdotL * lightColor * 0.8;\n");
        shader.append("    \n");
        shader.append("    // Enhanced specular with multiple highlight sizes\n");
        shader.append("    vec3 halfwayDir = normalize(lightDir + viewDir);\n");
        shader.append("    float NdotH = max(dot(normal, halfwayDir), 0.0);\n");
        shader.append("    float VdotH = max(dot(viewDir, halfwayDir), 0.0);\n");
        shader.append("    \n");
        shader.append("    // Sun glints (sharp specular highlights)\n");
        shader.append("    float sunGlintPower = mix(256.0, 64.0, roughness);\n");
        shader.append("    float sunGlint = pow(NdotH, sunGlintPower);\n");
        shader.append("    \n");
        shader.append("    // Broader specular reflection\n");
        shader.append("    float specularPower = 1.0 / max(roughness, 0.01);\n");
        shader.append("    float specular = pow(NdotH, specularPower);\n");
        shader.append("    \n");
        shader.append("    // Combine specular effects\n");
        shader.append("    vec3 specularColor = (sunGlint * 2.0 + specular) * lightColor * mix(0.04, 1.0, metallic);\n");
        shader.append("    \n");
        shader.append("    // Weather intensity affects wave visibility and lighting\n");
        shader.append("    diffuse *= mix(1.0, 0.7, weatherIntensity);\n");
        shader.append("    specularColor *= mix(1.0, 1.5, weatherIntensity); // Storms create more dramatic highlights\n");
        shader.append("    \n");
        
        // Enhanced final color mixing with biome integration
        shader.append("    // Blend biome-specific water color\n");
        shader.append("    vec3 biomeBlendedWater = mix(waterColor, biomeWaterColor, 0.3);\n");
        shader.append("    \n");
        shader.append("    // Mix reflection and refraction based on Fresnel\n");
        shader.append("    vec3 finalColor = mix(refractionColor.rgb, reflectionColor.rgb, fresnel);\n");
        shader.append("    finalColor = mix(finalColor, biomeBlendedWater, 0.25);\n");
        shader.append("    \n");
        shader.append("    // Apply lighting\n");
        shader.append("    finalColor = (ambient + diffuse) * finalColor + specularColor;\n");
        shader.append("    \n");
        shader.append("    // Enhanced caustics with depth and weather consideration\n");
        shader.append("    float causticsIntensity = causticsPattern * mix(1.0, 0.3, weatherIntensity);\n");
        shader.append("    causticsIntensity *= mix(1.0, 0.1, underwaterDepth); // Fade with depth\n");
        shader.append("    finalColor += causticsIntensity * lightColor * NdotL;\n");
        shader.append("    \n");
        shader.append("    // Subsurface scattering for underwater effect\n");
        shader.append("    if (underwaterDepth > 0.0) {\n");
        shader.append("        vec3 subsurfaceColor = mix(vec3(0.0, 0.4, 0.8), biomeWaterColor, 0.5);\n");
        shader.append("        finalColor = mix(finalColor, subsurfaceColor, underwaterDepth * 0.8);\n");
        shader.append("    }\n");
        shader.append("    \n");
        shader.append("    // Weather effects on transparency and color\n");
        shader.append("    float finalTransparency = transparency * mix(1.0, 0.8, weatherIntensity);\n");
        shader.append("    \n");
        shader.append("    fragColor = vec4(finalColor, finalTransparency);\n");
        shader.append("}\n");
        
        return shader.toString();
    }
    
    /**
     * Initializes the reflection framebuffer for screen-space reflections.
     */
    private void initializeReflectionFramebuffer() {
        reflectionFramebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, reflectionFramebuffer);
        
        // Color texture
        reflectionTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, reflectionTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, reflectionWidth, reflectionHeight, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, reflectionTexture, 0);
        
        // Depth buffer
        reflectionDepthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, reflectionDepthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, reflectionWidth, reflectionHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, reflectionDepthBuffer);
        
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Reflection framebuffer not complete!");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Initialized reflection framebuffer: " + reflectionWidth + "x" + reflectionHeight);
    }
    
    /**
     * Initializes the refraction framebuffer for underwater distortion effects.
     */
    private void initializeRefractionFramebuffer() {
        refractionFramebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, refractionFramebuffer);
        
        // Color texture
        refractionTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, refractionTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, refractionWidth, refractionHeight, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, refractionTexture, 0);
        
        // Depth texture
        refractionDepthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, refractionDepthTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, refractionWidth, refractionHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, refractionDepthTexture, 0);
        
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Refraction framebuffer not complete!");
        }
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        logger.info("Initialized refraction framebuffer: " + refractionWidth + "x" + refractionHeight);
    }
    
    /**
     * Loads water-specific textures (DuDv map, normal map, caustics, and animated surface).
     */
    private void loadWaterTextures() {
        // Create procedural textures for advanced effects
        dudvTexture = createProceduralDudvTexture();
        normalTexture = createProceduralNormalTexture();
        causticsTexture = createProceduralCausticsTexture();
        
        // TODO: Integrate with TextureAtlasManager for animated water surface textures
        // This would load the animated water surface textures created in resources/textures/water/
        // and blend them with the procedural wave simulation for enhanced detail
        
        logger.info("Loaded water textures with procedural generation");
    }
    
    /**
     * Creates a procedural DuDv texture for water distortion.
     */
    private int createProceduralDudvTexture() {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        int size = 256;
        ByteBuffer data = BufferUtils.createByteBuffer(size * size * 3);
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Simple noise pattern for distortion
                float u = (float)x / size;
                float v = (float)y / size;
                
                float noise1 = (float)(Math.sin(u * Math.PI * 8) * Math.cos(v * Math.PI * 6));
                float noise2 = (float)(Math.cos(u * Math.PI * 6) * Math.sin(v * Math.PI * 8));
                
                data.put((byte)((noise1 * 0.5f + 0.5f) * 255));     // R
                data.put((byte)((noise2 * 0.5f + 0.5f) * 255));     // G
                data.put((byte)0);                                   // B
            }
        }
        data.flip();
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, size, size, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        return textureId;
    }
    
    /**
     * Creates a procedural normal texture for water surface detail.
     */
    private int createProceduralNormalTexture() {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        int size = 256;
        ByteBuffer data = BufferUtils.createByteBuffer(size * size * 3);
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Generate normal map from height field
                float u = (float)x / size;
                float v = (float)y / size;
                
                float height = (float)(Math.sin(u * Math.PI * 16) * Math.cos(v * Math.PI * 16) * 0.1);
                
                // Calculate normal from height gradient
                float heightL = (float)(Math.sin((u - 1.0f/size) * Math.PI * 16) * Math.cos(v * Math.PI * 16) * 0.1);
                float heightR = (float)(Math.sin((u + 1.0f/size) * Math.PI * 16) * Math.cos(v * Math.PI * 16) * 0.1);
                float heightD = (float)(Math.sin(u * Math.PI * 16) * Math.cos((v - 1.0f/size) * Math.PI * 16) * 0.1);
                float heightU = (float)(Math.sin(u * Math.PI * 16) * Math.cos((v + 1.0f/size) * Math.PI * 16) * 0.1);
                
                Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU).normalize();
                
                data.put((byte)((normal.x * 0.5f + 0.5f) * 255));     // R
                data.put((byte)((normal.y * 0.5f + 0.5f) * 255));     // G
                data.put((byte)((normal.z * 0.5f + 0.5f) * 255));     // B
            }
        }
        data.flip();
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, size, size, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        return textureId;
    }
    
    /**
     * Creates a procedural caustics texture for underwater light patterns.
     */
    private int createProceduralCausticsTexture() {
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        int size = 512;
        ByteBuffer data = BufferUtils.createByteBuffer(size * size * 3);
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float u = (float)x / size;
                float v = (float)y / size;
                
                // Create caustics pattern using multiple sine waves
                float caustic1 = (float)Math.sin(u * Math.PI * 12 + v * Math.PI * 8);
                float caustic2 = (float)Math.sin(u * Math.PI * 8 - v * Math.PI * 12);
                float caustic3 = (float)Math.sin((u + v) * Math.PI * 10);
                float caustic4 = (float)Math.sin((u - v) * Math.PI * 14);
                
                // Combine and enhance the pattern
                float combined = (caustic1 + caustic2 + caustic3 + caustic4) * 0.25f;
                combined = Math.max(0.0f, combined); // Only positive values for light
                combined = (float)Math.pow(combined, 2.0); // Enhance contrast
                
                // Create focused light spots
                float spotPattern = (float)(Math.sin(u * Math.PI * 6) * Math.sin(v * Math.PI * 6));
                spotPattern = Math.max(0.0f, spotPattern);
                spotPattern = (float)Math.pow(spotPattern, 3.0);
                
                float finalIntensity = Math.min(1.0f, combined + spotPattern * 0.5f);
                
                byte intensity = (byte)(finalIntensity * 255);
                data.put(intensity); // R
                data.put(intensity); // G
                data.put(intensity); // B
            }
        }
        data.flip();
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, size, size, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        return textureId;
    }
    
    /**
     * Binds the reflection framebuffer for rendering reflections.
     */
    public void bindReflectionFramebuffer() {
        if (quality.isReflectionsEnabled()) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, reflectionFramebuffer);
            GL11.glViewport(0, 0, reflectionWidth, reflectionHeight);
        }
    }
    
    /**
     * Binds the refraction framebuffer for rendering refractions.
     */
    public void bindRefractionFramebuffer() {
        if (quality.isRefractionEnabled()) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, refractionFramebuffer);
            GL11.glViewport(0, 0, refractionWidth, refractionHeight);
        }
    }
    
    /**
     * Unbinds framebuffers and restores default framebuffer.
     */
    public void unbindFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Renders water with all advanced effects.
     */
    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, Matrix4f modelMatrix,
                      Vector3f cameraPosition, Vector3f lightDirection, Vector3f lightColor,
                      Vector3f ambientColor, float time, Mesh waterMesh) {
        
        waterShader.bind();
        
        // Set matrices
        waterShader.setUniform("projectionMatrix", projectionMatrix);
        waterShader.setUniform("viewMatrix", viewMatrix);
        waterShader.setUniform("modelMatrix", modelMatrix);
        
        // Set lighting
        waterShader.setUniform("lightDirection", lightDirection);
        waterShader.setUniform("lightColor", lightColor);
        waterShader.setUniform("ambientColor", ambientColor);
        waterShader.setUniform("cameraPosition", cameraPosition);
        waterShader.setUniform("time", time);
        
        // Set water properties
        waterShader.setUniform("deepWaterColor", deepWaterColor);
        waterShader.setUniform("shallowWaterColor", shallowWaterColor);
        waterShader.setUniform("foamColor", foamColor);
        waterShader.setUniform("transparency", transparency);
        waterShader.setUniform("roughness", roughness);
        waterShader.setUniform("metallic", metallic);
        waterShader.setUniform("fresnelStrength", fresnelStrength);
        waterShader.setUniform("causticsStrength", causticsStrength);
        
        // Set enhanced properties
        waterShader.setUniform("weatherIntensity", weatherIntensity);
        waterShader.setUniform("biomeWaterColor", biomeWaterColor);
        waterShader.setUniform("underwaterDepth", underwaterDepth);
        
        // Set wave parameters
        for (int i = 0; i < waves.length; i++) {
            GerstnerWave wave = waves[i];
            waterShader.setUniform("waveDirection" + i, wave.direction);
            waterShader.setUniform("waveAmplitude" + i, wave.amplitude);
            waterShader.setUniform("waveFrequency" + i, wave.getFrequency());
            waterShader.setUniform("wavePhase" + i, wave.getPhaseConstant());
            waterShader.setUniform("waveSteepness" + i, wave.steepness);
        }
        
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dudvTexture);
        waterShader.setUniform("dudvTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexture);
        waterShader.setUniform("normalTexture", 1);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, causticsTexture);
        waterShader.setUniform("causticsTexture", 2);
        
        if (quality.isReflectionsEnabled()) {
            GL13.glActiveTexture(GL13.GL_TEXTURE3);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, reflectionTexture);
            waterShader.setUniform("reflectionTexture", 3);
        }
        
        if (quality.isRefractionEnabled()) {
            GL13.glActiveTexture(GL13.GL_TEXTURE4);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, refractionTexture);
            waterShader.setUniform("refractionTexture", 4);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE5);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, refractionDepthTexture);
            waterShader.setUniform("refractionDepthTexture", 5);
        }
        
        // Enable blending for transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Render water geometry
        if (waterMesh != null) {
            waterMesh.render();
        } else {
            logger.warn("Water mesh is null, skipping geometry rendering");
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        waterShader.unbind();
    }
    
    /**
     * Sets water color properties.
     */
    public void setWaterColors(Vector3f deepWater, Vector3f shallowWater, Vector3f foam) {
        this.deepWaterColor.set(deepWater);
        this.shallowWaterColor.set(shallowWater);
        this.foamColor.set(foam);
    }
    
    /**
     * Sets water material properties.
     */
    public void setWaterProperties(float transparency, float roughness, float metallic, float fresnelStrength) {
        this.transparency = Math.max(0.0f, Math.min(1.0f, transparency));
        this.roughness = Math.max(0.01f, Math.min(1.0f, roughness));
        this.metallic = Math.max(0.0f, Math.min(1.0f, metallic));
        this.fresnelStrength = Math.max(0.0f, fresnelStrength);
    }
    
    /**
     * Sets the caustics effect strength.
     * @param strength Caustics intensity (0.0 = disabled, 1.0 = maximum)
     */
    public void setCausticsStrength(float strength) {
        this.causticsStrength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    /**
     * Sets weather intensity for dynamic water effects.
     * @param intensity Weather intensity (0.0 = calm, 1.0 = storm)
     */
    public void setWeatherIntensity(float intensity) {
        this.weatherIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /**
     * Sets biome-specific water color.
     * @param biomeColor The water color for the current biome
     */
    public void setBiomeWaterColor(Vector3f biomeColor) {
        this.biomeWaterColor.set(biomeColor);
    }
    
    /**
     * Sets underwater depth for subsurface effects.
     * @param depth Underwater depth (0.0 = surface, 1.0 = deep underwater)
     */
    public void setUnderwaterDepth(float depth) {
        this.underwaterDepth = Math.max(0.0f, Math.min(1.0f, depth));
    }
    
    /**
     * Updates the model matrix for the water geometry.
     * This should be called when the water shader is already bound.
     * @param modelMatrix The new model matrix for water geometry
     */
    public void updateModelMatrix(Matrix4f modelMatrix) {
        if (waterShader != null) {
            waterShader.setUniform("modelMatrix", modelMatrix);
        }
    }
    
    /**
     * Updates wave parameters for dynamic water behavior.
     */
    public void updateWave(int index, GerstnerWave wave) {
        if (index >= 0 && index < waves.length) {
            waves[index] = wave;
        }
    }
    
    /**
     * Gets the water quality setting.
     */
    public WaterQuality getQuality() {
        return quality;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (reflectionFramebuffer != 0) {
            GL30.glDeleteFramebuffers(reflectionFramebuffer);
            GL11.glDeleteTextures(reflectionTexture);
            GL30.glDeleteRenderbuffers(reflectionDepthBuffer);
        }
        
        if (refractionFramebuffer != 0) {
            GL30.glDeleteFramebuffers(refractionFramebuffer);
            GL11.glDeleteTextures(refractionTexture);
            GL11.glDeleteTextures(refractionDepthTexture);
        }
        
        if (dudvTexture != 0) {
            GL11.glDeleteTextures(dudvTexture);
        }
        
        if (normalTexture != 0) {
            GL11.glDeleteTextures(normalTexture);
        }
        
        if (causticsTexture != 0) {
            GL11.glDeleteTextures(causticsTexture);
        }
        
        logger.info("Cleaned up water shader resources");
    }
}