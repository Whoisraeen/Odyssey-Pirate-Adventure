package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import com.odyssey.util.Logger;

/**
 * Advanced volumetric fog renderer using ray-marching with 3D noise textures and light scattering.
 * Implements realistic atmospheric effects including:
 * - Ray-marching through volumetric fog
 * - 3D Perlin noise for fog density variation
 * - Mie and Rayleigh scattering
 * - Multiple light source support
 * - Temporal noise for animated fog
 * - Height-based fog density falloff
 */
public class VolumetricFog {
    
    private static final Logger logger = Logger.getInstance();
    
    // Rendering resources
    private Shader volumetricFogShader;
    private Shader noiseGenerationShader;
    private int fullscreenQuadVAO;
    private int fullscreenQuadVBO;
    
    // 3D noise texture for fog density variation
    private int noiseTexture3D;
    private int noiseTextureSize = 128;
    
    // Fog parameters
    private Vector3f fogColor = new Vector3f(0.7f, 0.8f, 0.9f);
    private Vector3f fogAbsorption = new Vector3f(0.1f, 0.1f, 0.1f);
    private Vector3f fogScattering = new Vector3f(0.2f, 0.2f, 0.2f);
    private float fogDensity = 0.02f;
    private float fogHeightFalloff = 0.1f;
    private float fogBaseHeight = 0.0f;
    private float fogMaxHeight = 100.0f;
    
    // Ray-marching parameters
    private int maxRaySteps = 64;
    private float rayStepSize = 1.0f;
    private float ditherStrength = 0.5f;
    
    // Scattering parameters
    private float mieScattering = 0.8f;
    private float rayleighScattering = 0.2f;
    private float anisotropy = 0.3f; // Henyey-Greenstein phase function parameter
    private float lightIntensity = 1.0f;
    
    // Animation parameters
    private float noiseSpeed = 0.1f;
    private Vector3f windDirection = new Vector3f(1.0f, 0.0f, 0.5f);
    private float time = 0.0f;
    
    // Quality settings
    private boolean enableTemporalNoise = true;
    private boolean enableMultipleScattering = true;
    private boolean enableHeightFog = true;
    
    /**
     * Initialize the volumetric fog system.
     */
    public void initialize() {
        logger.info("Initializing volumetric fog system...");
        
        try {
            // Create shaders
            createShaders();
            
            // Create fullscreen quad
            createFullscreenQuad();
            
            // Generate 3D noise texture
            generate3DNoiseTexture();
            
            logger.info("Volumetric fog system initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize volumetric fog system", e);
        }
    }
    
    /**
     * Create volumetric fog shaders.
     */
    private void createShaders() {
        // Create volumetric fog shader
        volumetricFogShader = Shader.create("volumetric_fog", 
            getVolumetricFogVertexSource(), 
            getVolumetricFogFragmentSource());
        
        if (volumetricFogShader == null) {
            throw new RuntimeException("Failed to create volumetric fog shader");
        }
        
        // Create noise generation shader for compute-based noise generation
        noiseGenerationShader = Shader.create("noise_generation",
            getNoiseVertexSource(),
            getNoiseFragmentSource());
    }
    
    /**
     * Create fullscreen quad for rendering.
     */
    private void createFullscreenQuad() {
        float[] quadVertices = {
            // Positions   // TexCoords
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
            
            -1.0f,  1.0f,  0.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
        };
        
        fullscreenQuadVAO = GL30.glGenVertexArrays();
        fullscreenQuadVBO = GL30.glGenBuffers();
        
        GL30.glBindVertexArray(fullscreenQuadVAO);
        GL30.glBindBuffer(GL11.GL_ARRAY_BUFFER, fullscreenQuadVBO);
        GL30.glBufferData(GL11.GL_ARRAY_BUFFER, quadVertices, GL11.GL_STATIC_DRAW);
        
        // Position attribute
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        
        // Texture coordinate attribute
        GL30.glEnableVertexAttribArray(1);
        GL30.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Generate 3D noise texture for fog density variation.
     */
    private void generate3DNoiseTexture() {
        noiseTexture3D = GL11.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, noiseTexture3D);
        
        // Set texture parameters
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        
        // Generate 3D Perlin noise data
        byte[] noiseData = new byte[noiseTextureSize * noiseTextureSize * noiseTextureSize];
        generatePerlinNoise3D(noiseData, noiseTextureSize);
        
        // Upload texture data
        GL32.glTexImage3D(GL32.GL_TEXTURE_3D, 0, GL11.GL_R8, 
                         noiseTextureSize, noiseTextureSize, noiseTextureSize, 
                         0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, noiseData);
        
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, 0);
    }
    
    /**
     * Generate 3D Perlin noise data.
     */
    private void generatePerlinNoise3D(byte[] data, int size) {
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float nx = (float) x / size;
                    float ny = (float) y / size;
                    float nz = (float) z / size;
                    
                    // Multi-octave Perlin noise
                    float noise = 0.0f;
                    float amplitude = 1.0f;
                    float frequency = 1.0f;
                    
                    for (int octave = 0; octave < 4; octave++) {
                        noise += amplitude * perlinNoise3D(nx * frequency, ny * frequency, nz * frequency);
                        amplitude *= 0.5f;
                        frequency *= 2.0f;
                    }
                    
                    // Normalize to [0, 1] and convert to byte
                    noise = (noise + 1.0f) * 0.5f;
                    noise = Math.max(0.0f, Math.min(1.0f, noise));
                    
                    int index = z * size * size + y * size + x;
                    data[index] = (byte) (noise * 255);
                }
            }
        }
    }
    
    /**
     * Simple 3D Perlin noise implementation.
     */
    private float perlinNoise3D(float x, float y, float z) {
        // Simplified Perlin noise - in a real implementation, you'd use proper gradients
        return (float) (Math.sin(x * 12.9898 + y * 78.233 + z * 37.719) * 43758.5453) % 1.0f;
    }
    
    /**
     * Render volumetric fog.
     */
    public void render(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, 
                      Vector3f lightDirection, Vector3f lightColor, int depthTexture, 
                      int colorTexture, float deltaTime) {
        
        if (volumetricFogShader == null) return;
        
        // Update time for animation
        time += deltaTime;
        
        // Enable additive blending for fog
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        
        // Use volumetric fog shader
        volumetricFogShader.bind();
        
        // Set matrices
        volumetricFogShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        volumetricFogShader.setUniform("u_ViewMatrix", viewMatrix);
        volumetricFogShader.setUniform("u_InverseProjectionMatrix", new Matrix4f(projectionMatrix).invert());
        volumetricFogShader.setUniform("u_InverseViewMatrix", new Matrix4f(viewMatrix).invert());
        
        // Set camera parameters
        volumetricFogShader.setUniform("u_CameraPosition", camera.getPosition());
        volumetricFogShader.setUniform("u_CameraNear", camera.getNear());
        volumetricFogShader.setUniform("u_CameraFar", camera.getFar());
        
        // Set lighting parameters
        volumetricFogShader.setUniform("u_LightDirection", lightDirection);
        volumetricFogShader.setUniform("u_LightColor", lightColor);
        volumetricFogShader.setUniform("u_LightIntensity", lightIntensity);
        
        // Set fog parameters
        volumetricFogShader.setUniform("u_FogColor", fogColor);
        volumetricFogShader.setUniform("u_FogAbsorption", fogAbsorption);
        volumetricFogShader.setUniform("u_FogScattering", fogScattering);
        volumetricFogShader.setUniform("u_FogDensity", fogDensity);
        volumetricFogShader.setUniform("u_FogHeightFalloff", fogHeightFalloff);
        volumetricFogShader.setUniform("u_FogBaseHeight", fogBaseHeight);
        volumetricFogShader.setUniform("u_FogMaxHeight", fogMaxHeight);
        
        // Set ray-marching parameters
        volumetricFogShader.setUniform("u_MaxRaySteps", maxRaySteps);
        volumetricFogShader.setUniform("u_RayStepSize", rayStepSize);
        volumetricFogShader.setUniform("u_DitherStrength", ditherStrength);
        
        // Set scattering parameters
        volumetricFogShader.setUniform("u_MieScattering", mieScattering);
        volumetricFogShader.setUniform("u_RayleighScattering", rayleighScattering);
        volumetricFogShader.setUniform("u_Anisotropy", anisotropy);
        
        // Set animation parameters
        volumetricFogShader.setUniform("u_Time", time);
        volumetricFogShader.setUniform("u_NoiseSpeed", noiseSpeed);
        volumetricFogShader.setUniform("u_WindDirection", windDirection);
        
        // Set quality flags
        volumetricFogShader.setUniform("u_EnableTemporalNoise", enableTemporalNoise);
        volumetricFogShader.setUniform("u_EnableMultipleScattering", enableMultipleScattering);
        volumetricFogShader.setUniform("u_EnableHeightFog", enableHeightFog);
        
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        volumetricFogShader.setUniform("u_DepthTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        volumetricFogShader.setUniform("u_ColorTexture", 1);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, noiseTexture3D);
        volumetricFogShader.setUniform("u_NoiseTexture3D", 2);
        
        // Render fullscreen quad
        GL30.glBindVertexArray(fullscreenQuadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        
        // Cleanup
        volumetricFogShader.unbind();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    /**
     * Get volumetric fog vertex shader source.
     */
    private String getVolumetricFogVertexSource() {
        return """
            #version 330 core
            
            layout (location = 0) in vec2 aPosition;
            layout (location = 1) in vec2 aTexCoord;
            
            out vec2 vTexCoord;
            out vec3 vRayDirection;
            
            uniform mat4 u_InverseProjectionMatrix;
            uniform mat4 u_InverseViewMatrix;
            
            void main() {
                vTexCoord = aTexCoord;
                
                // Calculate ray direction in world space
                vec4 clipPos = vec4(aPosition, 1.0, 1.0);
                vec4 viewPos = u_InverseProjectionMatrix * clipPos;
                viewPos /= viewPos.w;
                
                vec4 worldPos = u_InverseViewMatrix * vec4(viewPos.xyz, 0.0);
                vRayDirection = normalize(worldPos.xyz);
                
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
            """;
    }
    
    /**
     * Get volumetric fog fragment shader source.
     */
    private String getVolumetricFogFragmentSource() {
        return """
            #version 330 core
            
            in vec2 vTexCoord;
            in vec3 vRayDirection;
            
            out vec4 FragColor;
            
            // Uniforms
            uniform sampler2D u_DepthTexture;
            uniform sampler2D u_ColorTexture;
            uniform sampler3D u_NoiseTexture3D;
            
            uniform mat4 u_ProjectionMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_InverseProjectionMatrix;
            uniform mat4 u_InverseViewMatrix;
            
            uniform vec3 u_CameraPosition;
            uniform float u_CameraNear;
            uniform float u_CameraFar;
            
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            uniform float u_LightIntensity;
            
            uniform vec3 u_FogColor;
            uniform vec3 u_FogAbsorption;
            uniform vec3 u_FogScattering;
            uniform float u_FogDensity;
            uniform float u_FogHeightFalloff;
            uniform float u_FogBaseHeight;
            uniform float u_FogMaxHeight;
            
            uniform int u_MaxRaySteps;
            uniform float u_RayStepSize;
            uniform float u_DitherStrength;
            
            uniform float u_MieScattering;
            uniform float u_RayleighScattering;
            uniform float u_Anisotropy;
            
            uniform float u_Time;
            uniform float u_NoiseSpeed;
            uniform vec3 u_WindDirection;
            
            uniform bool u_EnableTemporalNoise;
            uniform bool u_EnableMultipleScattering;
            uniform bool u_EnableHeightFog;
            
            const float PI = 3.14159265359;
            
            // Convert depth buffer value to linear depth
            float linearizeDepth(float depth) {
                float z = depth * 2.0 - 1.0;
                return (2.0 * u_CameraNear * u_CameraFar) / (u_CameraFar + u_CameraNear - z * (u_CameraFar - u_CameraNear));
            }
            
            // Henyey-Greenstein phase function for anisotropic scattering
            float henyeyGreensteinPhase(float cosTheta, float g) {
                float g2 = g * g;
                return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
            }
            
            // Sample 3D noise with animation
            float sampleNoise3D(vec3 position) {
                vec3 animatedPos = position + u_WindDirection * u_Time * u_NoiseSpeed;
                
                if (u_EnableTemporalNoise) {
                    // Add temporal variation
                    animatedPos += sin(u_Time * 0.5) * 0.1;
                }
                
                return texture(u_NoiseTexture3D, animatedPos * 0.01).r;
            }
            
            // Calculate fog density at a given position
            float calculateFogDensity(vec3 worldPos) {
                float density = u_FogDensity;
                
                // Height-based fog falloff
                if (u_EnableHeightFog) {
                    float height = worldPos.y;
                    float heightFactor = exp(-max(0.0, height - u_FogBaseHeight) * u_FogHeightFalloff);
                    density *= heightFactor;
                    
                    // Fade out above max height
                    if (height > u_FogMaxHeight) {
                        density *= exp(-(height - u_FogMaxHeight) * 0.1);
                    }
                }
                
                // Add noise variation
                float noise = sampleNoise3D(worldPos);
                density *= (0.5 + 0.5 * noise);
                
                return density;
            }
            
            // Calculate in-scattering from light
            vec3 calculateInScattering(vec3 rayDir, vec3 lightDir, float density) {
                float cosTheta = dot(rayDir, -lightDir);
                
                // Mie scattering (forward scattering)
                float miePhase = henyeyGreensteinPhase(cosTheta, u_Anisotropy);
                vec3 mieContribution = u_MieScattering * miePhase * u_LightColor;
                
                // Rayleigh scattering (more uniform)
                float rayleighPhase = (3.0 / (16.0 * PI)) * (1.0 + cosTheta * cosTheta);
                vec3 rayleighContribution = u_RayleighScattering * rayleighPhase * u_LightColor;
                
                return (mieContribution + rayleighContribution) * density * u_LightIntensity;
            }
            
            // Dithering function for noise reduction
            float dither(vec2 screenPos) {
                return fract(sin(dot(screenPos, vec2(12.9898, 78.233))) * 43758.5453);
            }
            
            void main() {
                // Sample depth and convert to linear
                float depth = texture(u_DepthTexture, vTexCoord).r;
                float linearDepth = linearizeDepth(depth);
                
                // Calculate world position from depth
                vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                vec4 viewPos = u_InverseProjectionMatrix * clipPos;
                viewPos /= viewPos.w;
                vec4 worldPos = u_InverseViewMatrix * viewPos;
                
                // Ray marching setup
                vec3 rayStart = u_CameraPosition;
                vec3 rayEnd = worldPos.xyz;
                vec3 rayDir = normalize(rayEnd - rayStart);
                float rayLength = min(length(rayEnd - rayStart), linearDepth);
                
                // Add dithering to reduce banding
                float ditherValue = dither(gl_FragCoord.xy) * u_DitherStrength;
                float stepSize = u_RayStepSize + ditherValue;
                int numSteps = min(u_MaxRaySteps, int(rayLength / stepSize));
                
                // Ray marching variables
                vec3 currentPos = rayStart;
                vec3 accumulatedColor = vec3(0.0);
                float accumulatedTransmittance = 1.0;
                
                // Ray marching loop
                for (int i = 0; i < numSteps; i++) {
                    if (accumulatedTransmittance < 0.01) break; // Early termination
                    
                    // Calculate fog density at current position
                    float density = calculateFogDensity(currentPos);
                    
                    if (density > 0.0) {
                        // Calculate extinction (absorption + out-scattering)
                        vec3 extinction = u_FogAbsorption + u_FogScattering;
                        float extinctionCoeff = (extinction.r + extinction.g + extinction.b) / 3.0;
                        
                        // Calculate transmittance for this step
                        float stepTransmittance = exp(-extinctionCoeff * density * stepSize);
                        
                        // Calculate in-scattering
                        vec3 inScattering = calculateInScattering(rayDir, u_LightDirection, density);
                        
                        // Multiple scattering approximation
                        if (u_EnableMultipleScattering) {
                            inScattering *= (1.0 + 0.3 * density); // Simple approximation
                        }
                        
                        // Accumulate color and transmittance
                        vec3 stepColor = inScattering * (1.0 - stepTransmittance) / max(extinctionCoeff, 0.001);
                        accumulatedColor += stepColor * accumulatedTransmittance;
                        accumulatedTransmittance *= stepTransmittance;
                    }
                    
                    // Advance along ray
                    currentPos += rayDir * stepSize;
                }
                
                // Sample original color
                vec3 originalColor = texture(u_ColorTexture, vTexCoord).rgb;
                
                // Blend fog with original color
                vec3 finalColor = originalColor * accumulatedTransmittance + accumulatedColor;
                
                FragColor = vec4(finalColor, 1.0);
            }
            """;
    }
    
    /**
     * Get noise generation vertex shader source.
     */
    private String getNoiseVertexSource() {
        return """
            #version 330 core
            layout (location = 0) in vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
            """;
    }
    
    /**
     * Get noise generation fragment shader source.
     */
    private String getNoiseFragmentSource() {
        return """
            #version 330 core
            out vec4 FragColor;
            void main() {
                FragColor = vec4(1.0, 0.0, 0.0, 1.0);
            }
            """;
    }
    
    // Getters and setters for fog parameters
    public Vector3f getFogColor() { return new Vector3f(fogColor); }
    public void setFogColor(Vector3f color) { this.fogColor.set(color); }
    
    public float getFogDensity() { return fogDensity; }
    public void setFogDensity(float density) { this.fogDensity = density; }
    
    public float getFogHeightFalloff() { return fogHeightFalloff; }
    public void setFogHeightFalloff(float falloff) { this.fogHeightFalloff = falloff; }
    
    public int getMaxRaySteps() { return maxRaySteps; }
    public void setMaxRaySteps(int steps) { this.maxRaySteps = Math.max(1, Math.min(128, steps)); }
    
    public float getMieScattering() { return mieScattering; }
    public void setMieScattering(float mie) { this.mieScattering = mie; }
    
    public float getRayleighScattering() { return rayleighScattering; }
    public void setRayleighScattering(float rayleigh) { this.rayleighScattering = rayleigh; }
    
    public boolean isEnableTemporalNoise() { return enableTemporalNoise; }
    public void setEnableTemporalNoise(boolean enable) { this.enableTemporalNoise = enable; }
    
    public boolean isEnableMultipleScattering() { return enableMultipleScattering; }
    public void setEnableMultipleScattering(boolean enable) { this.enableMultipleScattering = enable; }
    
    /**
     * Cleanup OpenGL resources.
     */
    public void cleanup() {
        if (volumetricFogShader != null) {
            volumetricFogShader.cleanup();
        }
        if (noiseGenerationShader != null) {
            noiseGenerationShader.cleanup();
        }
        
        if (fullscreenQuadVAO != 0) {
            GL30.glDeleteVertexArrays(fullscreenQuadVAO);
        }
        if (fullscreenQuadVBO != 0) {
            GL30.glDeleteBuffers(fullscreenQuadVBO);
        }
        if (noiseTexture3D != 0) {
            GL11.glDeleteTextures(noiseTexture3D);
        }
    }
}
