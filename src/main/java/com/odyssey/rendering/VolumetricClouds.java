package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import com.odyssey.util.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * Advanced volumetric cloud renderer using ray-marching with multiple noise octaves.
 * Implements realistic cloud effects including:
 * - Multi-layered cloud systems (cumulus, stratus, cirrus)
 * - Worley noise for cloud shapes and detail
 * - Multiple scattering for realistic lighting
 * - Weather system integration
 * - Temporal animation with wind effects
 * - Cloud density and coverage variation
 */
public class VolumetricClouds {
    
    private static final Logger logger = Logger.getLogger(VolumetricClouds.class);
    
    // Rendering resources
    private Shader volumetricCloudsShader;
    private int fullscreenQuadVAO;
    private int fullscreenQuadVBO;
    
    // 3D noise textures for cloud generation
    private int shapeNoiseTexture3D;     // Low-frequency Worley noise for cloud shapes
    private int detailNoiseTexture3D;    // High-frequency Perlin noise for cloud details
    private int weatherTexture2D;        // 2D texture for weather patterns
    private int curlNoiseTexture2D;      // 2D curl noise for wind distortion
    
    private int noiseTextureSize = 128;
    private int weatherTextureSize = 512;
    
    // Cloud layer parameters
    private float cloudLayerBottomHeight = 1500.0f;
    private float cloudLayerTopHeight = 4000.0f;
    private float cloudLayerThickness = 2500.0f;
    
    // Cloud appearance parameters
    private Vector3f cloudColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private float cloudCoverage = 0.5f;
    private float cloudDensity = 0.8f;
    private float cloudAbsorption = 0.3f;
    private float cloudScattering = 0.7f;
    
    // Ray-marching parameters
    private int maxCloudSteps = 128;
    private int maxLightSteps = 6;
    private float cloudStepSize = 50.0f;
    private float lightStepSize = 100.0f;
    
    // Lighting parameters
    private float silverLining = 0.7f;      // Forward scattering intensity
    private float silverSpread = 0.1f;      // Forward scattering spread
    private float darkEdgeOffset = 0.2f;    // Back scattering offset
    private float darkEdgeAmbient = 0.1f;   // Ambient lighting in shadows
    
    // Animation and weather parameters
    private Vector3f windDirection = new Vector3f(1.0f, 0.0f, 0.3f);
    private float windSpeed = 10.0f;
    private float cloudEvolution = 0.1f;    // Cloud shape evolution speed
    private float time = 0.0f;
    
    // Weather system parameters
    private float weatherScale = 0.00005f;  // Scale of weather patterns
    private float weatherStrength = 1.0f;   // Strength of weather influence
    
    // Quality settings
    private boolean enableDetailNoise = true;
    private boolean enableWeatherSystem = true;
    private boolean enableMultipleScattering = true;
    private boolean enableTemporalUpsampling = false;
    
    /**
     * Initialize the volumetric clouds system.
     */
    public void initialize() {
        logger.info("Initializing volumetric clouds system...");
        
        try {
            // Create shader
            createShader();
            
            // Create fullscreen quad
            createFullscreenQuad();
            
            // Generate noise textures
            generateCloudTextures();
            
            logger.info("Volumetric clouds system initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize volumetric clouds system", e);
        }
    }
    
    /**
     * Create volumetric clouds shader.
     */
    private void createShader() {
        volumetricCloudsShader = Shader.create("volumetric_clouds", 
            getVolumetricCloudsVertexSource(), 
            getVolumetricCloudsFragmentSource());
        
        if (volumetricCloudsShader == null) {
            throw new RuntimeException("Failed to create volumetric clouds shader");
        }
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
        GL30.glBindBuffer(GL_ARRAY_BUFFER, fullscreenQuadVBO);
        GL30.glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        // Position attribute
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        
        // Texture coordinate attribute
        GL30.glEnableVertexAttribArray(1);
        GL30.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Generate all cloud-related textures.
     */
    private void generateCloudTextures() {
        generateShapeNoiseTexture();
        generateDetailNoiseTexture();
        generateWeatherTexture();
        generateCurlNoiseTexture();
    }
    
    /**
     * Generate 3D shape noise texture using Worley noise.
     */
    private void generateShapeNoiseTexture() {
        shapeNoiseTexture3D = GL11.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, shapeNoiseTexture3D);
        
        // Set texture parameters
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        
        // Generate Worley noise data for cloud shapes
        byte[] noiseData = new byte[noiseTextureSize * noiseTextureSize * noiseTextureSize * 4];
        generateWorleyNoise3D(noiseData, noiseTextureSize);
        
        // Upload texture data (RGBA format for multiple octaves)
        java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(noiseData.length);
        buffer.put(noiseData);
        buffer.flip();
        
        GL32.glTexImage3D(GL32.GL_TEXTURE_3D, 0, GL11.GL_RGBA8, 
                         noiseTextureSize, noiseTextureSize, noiseTextureSize, 
                         0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, 0);
    }
    
    /**
     * Generate 3D detail noise texture using Perlin noise.
     */
    private void generateDetailNoiseTexture() {
        detailNoiseTexture3D = GL11.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, detailNoiseTexture3D);
        
        // Set texture parameters
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL32.GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        
        // Generate Perlin noise data for cloud details
        byte[] noiseData = new byte[noiseTextureSize * noiseTextureSize * noiseTextureSize * 3];
        generatePerlinNoise3D(noiseData, noiseTextureSize);
        
        // Upload texture data (RGB format for 3 octaves)
        java.nio.ByteBuffer detailBuffer = org.lwjgl.BufferUtils.createByteBuffer(noiseData.length);
        detailBuffer.put(noiseData);
        detailBuffer.flip();
        
        GL32.glTexImage3D(GL32.GL_TEXTURE_3D, 0, GL11.GL_RGB8, 
                         noiseTextureSize, noiseTextureSize, noiseTextureSize, 
                         0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, detailBuffer);
        
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, 0);
    }
    
    /**
     * Generate 2D weather texture for coverage and type patterns.
     */
    private void generateWeatherTexture() {
        weatherTexture2D = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, weatherTexture2D);
        
        // Set texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        
        // Generate weather pattern data
        byte[] weatherData = new byte[weatherTextureSize * weatherTextureSize * 3];
        generateWeatherPattern(weatherData, weatherTextureSize);
        
        // Create ByteBuffer for texture upload
        java.nio.ByteBuffer weatherBuffer = org.lwjgl.BufferUtils.createByteBuffer(weatherData.length);
        weatherBuffer.put(weatherData);
        weatherBuffer.flip();
        
        // Upload texture data (RGB: coverage, type, wetness)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, 
                         weatherTextureSize, weatherTextureSize, 
                         0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, weatherBuffer);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Generate 2D curl noise texture for wind distortion.
     */
    private void generateCurlNoiseTexture() {
        curlNoiseTexture2D = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, curlNoiseTexture2D);
        
        // Set texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        
        // Generate curl noise data
        byte[] curlData = new byte[weatherTextureSize * weatherTextureSize * 2];
        generateCurlNoise(curlData, weatherTextureSize);
        
        // Create ByteBuffer for texture upload
        java.nio.ByteBuffer curlBuffer = org.lwjgl.BufferUtils.createByteBuffer(curlData.length);
        curlBuffer.put(curlData);
        curlBuffer.flip();
        
        // Upload texture data (RG: curl vector field)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL_RG8, 
                         weatherTextureSize, weatherTextureSize, 
                         0, GL_RG, GL11.GL_UNSIGNED_BYTE, curlBuffer);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Generate Worley noise for cloud shapes.
     */
    private void generateWorleyNoise3D(byte[] data, int size) {
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float nx = (float) x / size;
                    float ny = (float) y / size;
                    float nz = (float) z / size;
                    
                    // Generate multiple octaves of Worley noise
                    float[] octaves = new float[4];
                    float[] frequencies = {1.0f, 2.0f, 4.0f, 8.0f};
                    
                    for (int octave = 0; octave < 4; octave++) {
                        octaves[octave] = worleyNoise3D(nx * frequencies[octave], 
                                                      ny * frequencies[octave], 
                                                      nz * frequencies[octave]);
                    }
                    
                    int index = (z * size * size + y * size + x) * 4;
                    for (int i = 0; i < 4; i++) {
                        data[index + i] = (byte) (octaves[i] * 255);
                    }
                }
            }
        }
    }
    
    /**
     * Generate Perlin noise for cloud details.
     */
    private void generatePerlinNoise3D(byte[] data, int size) {
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float nx = (float) x / size;
                    float ny = (float) y / size;
                    float nz = (float) z / size;
                    
                    // Generate 3 octaves of Perlin noise
                    float[] octaves = new float[3];
                    float[] frequencies = {4.0f, 8.0f, 16.0f};
                    
                    for (int octave = 0; octave < 3; octave++) {
                        octaves[octave] = perlinNoise3D(nx * frequencies[octave], 
                                                       ny * frequencies[octave], 
                                                       nz * frequencies[octave]);
                        octaves[octave] = (octaves[octave] + 1.0f) * 0.5f; // Normalize to [0,1]
                    }
                    
                    int index = (z * size * size + y * size + x) * 3;
                    for (int i = 0; i < 3; i++) {
                        data[index + i] = (byte) (octaves[i] * 255);
                    }
                }
            }
        }
    }
    
    /**
     * Generate weather pattern data.
     */
    private void generateWeatherPattern(byte[] data, int size) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (float) x / size;
                float ny = (float) y / size;
                
                // Coverage pattern (low frequency)
                float coverage = perlinNoise2D(nx * 2.0f, ny * 2.0f) * 0.5f + 0.5f;
                
                // Cloud type pattern (medium frequency)
                float type = perlinNoise2D(nx * 4.0f, ny * 4.0f) * 0.5f + 0.5f;
                
                // Wetness pattern (high frequency)
                float wetness = perlinNoise2D(nx * 8.0f, ny * 8.0f) * 0.5f + 0.5f;
                
                int index = (y * size + x) * 3;
                data[index] = (byte) (coverage * 255);
                data[index + 1] = (byte) (type * 255);
                data[index + 2] = (byte) (wetness * 255);
            }
        }
    }
    
    /**
     * Generate curl noise for wind distortion.
     */
    private void generateCurlNoise(byte[] data, int size) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (float) x / size;
                float ny = (float) y / size;
                
                // Calculate curl of a potential field
                float eps = 1.0f / size;
                float n1 = perlinNoise2D(nx, ny + eps);
                float n2 = perlinNoise2D(nx, ny - eps);
                float n3 = perlinNoise2D(nx + eps, ny);
                float n4 = perlinNoise2D(nx - eps, ny);
                
                float curlX = (n1 - n2) / (2.0f * eps);
                float curlY = (n4 - n3) / (2.0f * eps);
                
                // Normalize and convert to [0,1]
                curlX = curlX * 0.5f + 0.5f;
                curlY = curlY * 0.5f + 0.5f;
                
                int index = (y * size + x) * 2;
                data[index] = (byte) (curlX * 255);
                data[index + 1] = (byte) (curlY * 255);
            }
        }
    }
    
    /**
     * Simple Worley noise implementation.
     */
    private float worleyNoise3D(float x, float y, float z) {
        // Simplified Worley noise - find distance to nearest point
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        
        float minDist = Float.MAX_VALUE;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cellX = xi + dx;
                    int cellY = yi + dy;
                    int cellZ = zi + dz;
                    
                    // Generate random point in cell
                    float pointX = cellX + hash3D(cellX, cellY, cellZ);
                    float pointY = cellY + hash3D(cellX + 1, cellY, cellZ);
                    float pointZ = cellZ + hash3D(cellX, cellY + 1, cellZ);
                    
                    // Calculate distance
                    float dist = (float) Math.sqrt((x - pointX) * (x - pointX) + 
                                                  (y - pointY) * (y - pointY) + 
                                                  (z - pointZ) * (z - pointZ));
                    minDist = Math.min(minDist, dist);
                }
            }
        }
        
        return Math.max(0.0f, Math.min(1.0f, minDist));
    }
    
    /**
     * Simple 3D Perlin noise implementation.
     */
    private float perlinNoise3D(float x, float y, float z) {
        return (float) (Math.sin(x * 12.9898 + y * 78.233 + z * 37.719) * 43758.5453) % 1.0f;
    }
    
    /**
     * Simple 2D Perlin noise implementation.
     */
    private float perlinNoise2D(float x, float y) {
        return (float) (Math.sin(x * 12.9898 + y * 78.233) * 43758.5453) % 1.0f;
    }
    
    /**
     * Simple 3D hash function.
     */
    private float hash3D(int x, int y, int z) {
        int n = x * 374761393 + y * 668265263 + z * 1274126177;
        n = (n ^ (n >> 13)) * 1274126177;
        return ((n ^ (n >> 16)) & 0x7fffffff) / (float) 0x7fffffff;
    }
    
    /**
     * Render volumetric clouds.
     */
    public void render(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, 
                      Vector3f lightDirection, Vector3f lightColor, Vector3f sunColor,
                      int depthTexture, int colorTexture, float deltaTime) {
        
        if (volumetricCloudsShader == null) return;
        
        // Update time for animation
        time += deltaTime;
        
        // Enable alpha blending for clouds
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        
        // Use volumetric clouds shader
        volumetricCloudsShader.bind();
        
        // Set matrices
        volumetricCloudsShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        volumetricCloudsShader.setUniform("u_ViewMatrix", viewMatrix);
        volumetricCloudsShader.setUniform("u_InverseProjectionMatrix", new Matrix4f(projectionMatrix).invert());
        volumetricCloudsShader.setUniform("u_InverseViewMatrix", new Matrix4f(viewMatrix).invert());
        
        // Set camera parameters
        volumetricCloudsShader.setUniform("u_CameraPosition", camera.getPosition());
        volumetricCloudsShader.setUniform("u_CameraNear", camera.getNear());
        volumetricCloudsShader.setUniform("u_CameraFar", camera.getFar());
        
        // Set lighting parameters
        volumetricCloudsShader.setUniform("u_LightDirection", lightDirection);
        volumetricCloudsShader.setUniform("u_LightColor", lightColor);
        volumetricCloudsShader.setUniform("u_SunColor", sunColor);
        
        // Set cloud layer parameters
        volumetricCloudsShader.setUniform("u_CloudLayerBottomHeight", cloudLayerBottomHeight);
        volumetricCloudsShader.setUniform("u_CloudLayerTopHeight", cloudLayerTopHeight);
        volumetricCloudsShader.setUniform("u_CloudLayerThickness", cloudLayerThickness);
        
        // Set cloud appearance parameters
        volumetricCloudsShader.setUniform("u_CloudColor", cloudColor);
        volumetricCloudsShader.setUniform("u_CloudCoverage", cloudCoverage);
        volumetricCloudsShader.setUniform("u_CloudDensity", cloudDensity);
        volumetricCloudsShader.setUniform("u_CloudAbsorption", cloudAbsorption);
        volumetricCloudsShader.setUniform("u_CloudScattering", cloudScattering);
        
        // Set ray-marching parameters
        volumetricCloudsShader.setUniform("u_MaxCloudSteps", maxCloudSteps);
        volumetricCloudsShader.setUniform("u_MaxLightSteps", maxLightSteps);
        volumetricCloudsShader.setUniform("u_CloudStepSize", cloudStepSize);
        volumetricCloudsShader.setUniform("u_LightStepSize", lightStepSize);
        
        // Set lighting parameters
        volumetricCloudsShader.setUniform("u_SilverLining", silverLining);
        volumetricCloudsShader.setUniform("u_SilverSpread", silverSpread);
        volumetricCloudsShader.setUniform("u_DarkEdgeOffset", darkEdgeOffset);
        volumetricCloudsShader.setUniform("u_DarkEdgeAmbient", darkEdgeAmbient);
        
        // Set animation parameters
        volumetricCloudsShader.setUniform("u_Time", time);
        volumetricCloudsShader.setUniform("u_WindDirection", windDirection);
        volumetricCloudsShader.setUniform("u_WindSpeed", windSpeed);
        volumetricCloudsShader.setUniform("u_CloudEvolution", cloudEvolution);
        
        // Set weather parameters
        volumetricCloudsShader.setUniform("u_WeatherScale", weatherScale);
        volumetricCloudsShader.setUniform("u_WeatherStrength", weatherStrength);
        
        // Set quality flags
        volumetricCloudsShader.setUniform("u_EnableDetailNoise", enableDetailNoise);
        volumetricCloudsShader.setUniform("u_EnableWeatherSystem", enableWeatherSystem);
        volumetricCloudsShader.setUniform("u_EnableMultipleScattering", enableMultipleScattering);
        
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        volumetricCloudsShader.setUniform("u_DepthTexture", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        volumetricCloudsShader.setUniform("u_ColorTexture", 1);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, shapeNoiseTexture3D);
        volumetricCloudsShader.setUniform("u_ShapeNoiseTexture", 2);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL32.glBindTexture(GL32.GL_TEXTURE_3D, detailNoiseTexture3D);
        volumetricCloudsShader.setUniform("u_DetailNoiseTexture", 3);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, weatherTexture2D);
        volumetricCloudsShader.setUniform("u_WeatherTexture", 4);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE5);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, curlNoiseTexture2D);
        volumetricCloudsShader.setUniform("u_CurlNoiseTexture", 5);
        
        // Render fullscreen quad
        GL30.glBindVertexArray(fullscreenQuadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        
        // Cleanup
        volumetricCloudsShader.unbind();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    /**
     * Get volumetric clouds vertex shader source.
     */
    private String getVolumetricCloudsVertexSource() {
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
     * Get volumetric clouds fragment shader source.
     */
    private String getVolumetricCloudsFragmentSource() {
        return """
            #version 330 core
            
            in vec2 vTexCoord;
            in vec3 vRayDirection;
            
            out vec4 FragColor;
            
            // Uniforms
            uniform sampler2D u_DepthTexture;
            uniform sampler2D u_ColorTexture;
            uniform sampler3D u_ShapeNoiseTexture;
            uniform sampler3D u_DetailNoiseTexture;
            uniform sampler2D u_WeatherTexture;
            uniform sampler2D u_CurlNoiseTexture;
            
            uniform mat4 u_ProjectionMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_InverseProjectionMatrix;
            uniform mat4 u_InverseViewMatrix;
            
            uniform vec3 u_CameraPosition;
            uniform float u_CameraNear;
            uniform float u_CameraFar;
            
            uniform vec3 u_LightDirection;
            uniform vec3 u_LightColor;
            uniform vec3 u_SunColor;
            
            uniform float u_CloudLayerBottomHeight;
            uniform float u_CloudLayerTopHeight;
            uniform float u_CloudLayerThickness;
            
            uniform vec3 u_CloudColor;
            uniform float u_CloudCoverage;
            uniform float u_CloudDensity;
            uniform float u_CloudAbsorption;
            uniform float u_CloudScattering;
            
            uniform int u_MaxCloudSteps;
            uniform int u_MaxLightSteps;
            uniform float u_CloudStepSize;
            uniform float u_LightStepSize;
            
            uniform float u_SilverLining;
            uniform float u_SilverSpread;
            uniform float u_DarkEdgeOffset;
            uniform float u_DarkEdgeAmbient;
            
            uniform float u_Time;
            uniform vec3 u_WindDirection;
            uniform float u_WindSpeed;
            uniform float u_CloudEvolution;
            
            uniform float u_WeatherScale;
            uniform float u_WeatherStrength;
            
            uniform bool u_EnableDetailNoise;
            uniform bool u_EnableWeatherSystem;
            uniform bool u_EnableMultipleScattering;
            
            const float PI = 3.14159265359;
            
            // Convert depth buffer value to linear depth
            float linearizeDepth(float depth) {
                float z = depth * 2.0 - 1.0;
                return (2.0 * u_CameraNear * u_CameraFar) / (u_CameraFar + u_CameraNear - z * (u_CameraFar - u_CameraNear));
            }
            
            // Henyey-Greenstein phase function
            float henyeyGreensteinPhase(float cosTheta, float g) {
                float g2 = g * g;
                return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
            }
            
            // Sample cloud density at a position
            float sampleCloudDensity(vec3 position) {
                // Apply wind animation
                vec3 animatedPos = position + u_WindDirection * u_Time * u_WindSpeed;
                
                // Sample shape noise (Worley noise for cloud shapes)
                vec4 shapeNoise = texture(u_ShapeNoiseTexture, animatedPos * 0.0001);
                
                // Combine shape noise octaves with different weights
                float shapeFBM = dot(shapeNoise, vec4(0.625, 0.25, 0.125, 0.0625));
                
                // Apply weather system if enabled
                float weatherInfluence = 1.0;
                if (u_EnableWeatherSystem) {
                    vec2 weatherUV = position.xz * u_WeatherScale;
                    vec3 weather = texture(u_WeatherTexture, weatherUV).rgb;
                    
                    float coverage = weather.r;
                    float cloudType = weather.g;
                    float wetness = weather.b;
                    
                    // Modify coverage based on weather
                    weatherInfluence = smoothstep(coverage - 0.1, coverage + 0.1, shapeFBM);
                    weatherInfluence *= u_WeatherStrength;
                }
                
                // Base cloud density
                float density = shapeFBM * weatherInfluence;
                
                // Apply coverage threshold
                density = smoothstep(1.0 - u_CloudCoverage, 1.0, density);
                
                // Add detail noise if enabled
                if (u_EnableDetailNoise && density > 0.0) {
                    vec3 detailPos = animatedPos * 0.001 + vec3(u_Time * u_CloudEvolution);
                    vec3 detailNoise = texture(u_DetailNoiseTexture, detailPos).rgb;
                    
                    float detailFBM = dot(detailNoise, vec3(0.625, 0.25, 0.125));
                    
                    // Erode cloud edges with detail noise
                    density = density - (1.0 - detailFBM) * 0.4;
                }
                
                // Height-based density falloff
                float heightFactor = 1.0;
                if (position.y < u_CloudLayerBottomHeight) {
                    heightFactor = 0.0;
                } else if (position.y > u_CloudLayerTopHeight) {
                    heightFactor = 0.0;
                } else {
                    float heightGradient = (position.y - u_CloudLayerBottomHeight) / u_CloudLayerThickness;
                    heightFactor = 4.0 * heightGradient * (1.0 - heightGradient);
                }
                
                return max(0.0, density * heightFactor * u_CloudDensity);
            }
            
            // Calculate lighting for clouds
            vec3 calculateCloudLighting(vec3 position, vec3 rayDir, vec3 lightDir, float density) {
                float cosTheta = dot(rayDir, -lightDir);
                
                // Sample light transmission through clouds
                float lightTransmission = 1.0;
                vec3 lightSamplePos = position;
                
                for (int i = 0; i < u_MaxLightSteps; i++) {
                    lightSamplePos += lightDir * u_LightStepSize;
                    float lightDensity = sampleCloudDensity(lightSamplePos);
                    lightTransmission *= exp(-lightDensity * u_CloudAbsorption * u_LightStepSize);
                    
                    if (lightTransmission < 0.01) break;
                }
                
                // Phase function for scattering
                float forwardScattering = henyeyGreensteinPhase(cosTheta, 0.8);
                float backScattering = henyeyGreensteinPhase(cosTheta, -0.2);
                
                // Silver lining effect (forward scattering)
                float silverLining = u_SilverLining * pow(max(0.0, cosTheta), u_SilverSpread);
                
                // Dark edge effect (back scattering)
                float darkEdge = u_DarkEdgeOffset + u_DarkEdgeAmbient * (1.0 + cosTheta);
                
                // Combine scattering effects
                float scattering = mix(darkEdge, silverLining, forwardScattering);
                
                // Multiple scattering approximation
                if (u_EnableMultipleScattering) {
                    scattering += 0.3 * density; // Simple approximation
                }
                
                return u_SunColor * lightTransmission * scattering * u_CloudScattering;
            }
            
            // Ray-sphere intersection for cloud layer bounds
            vec2 rayCloudLayerIntersection(vec3 rayOrigin, vec3 rayDir) {
                float t1 = (u_CloudLayerBottomHeight - rayOrigin.y) / rayDir.y;
                float t2 = (u_CloudLayerTopHeight - rayOrigin.y) / rayDir.y;
                
                if (t1 > t2) {
                    float temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                
                return vec2(max(0.0, t1), max(0.0, t2));
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
                
                // Ray setup
                vec3 rayStart = u_CameraPosition;
                vec3 rayDir = normalize(vRayDirection);
                
                // Find intersection with cloud layer
                vec2 cloudIntersection = rayCloudLayerIntersection(rayStart, rayDir);
                
                if (cloudIntersection.y <= cloudIntersection.x) {
                    // No intersection with cloud layer
                    FragColor = texture(u_ColorTexture, vTexCoord);
                    return;
                }
                
                // Limit ray length by scene depth
                float maxRayLength = min(cloudIntersection.y, linearDepth);
                float rayLength = maxRayLength - cloudIntersection.x;
                
                if (rayLength <= 0.0) {
                    FragColor = texture(u_ColorTexture, vTexCoord);
                    return;
                }
                
                // Ray marching setup
                int numSteps = min(u_MaxCloudSteps, int(rayLength / u_CloudStepSize));
                float stepSize = rayLength / float(numSteps);
                
                vec3 currentPos = rayStart + rayDir * cloudIntersection.x;
                vec3 accumulatedColor = vec3(0.0);
                float accumulatedTransmittance = 1.0;
                
                // Ray marching loop
                for (int i = 0; i < numSteps; i++) {
                    if (accumulatedTransmittance < 0.01) break; // Early termination
                    
                    // Sample cloud density
                    float density = sampleCloudDensity(currentPos);
                    
                    if (density > 0.0) {
                        // Calculate lighting
                        vec3 lighting = calculateCloudLighting(currentPos, rayDir, u_LightDirection, density);
                        
                        // Calculate transmittance for this step
                        float extinction = u_CloudAbsorption + u_CloudScattering;
                        float stepTransmittance = exp(-extinction * density * stepSize);
                        
                        // Accumulate color and transmittance
                        vec3 stepColor = lighting * (1.0 - stepTransmittance) / max(extinction, 0.001);
                        accumulatedColor += stepColor * accumulatedTransmittance;
                        accumulatedTransmittance *= stepTransmittance;
                    }
                    
                    // Advance along ray
                    currentPos += rayDir * stepSize;
                }
                
                // Sample original color
                vec3 originalColor = texture(u_ColorTexture, vTexCoord).rgb;
                
                // Blend clouds with original color
                vec3 finalColor = originalColor * accumulatedTransmittance + accumulatedColor;
                
                FragColor = vec4(finalColor, 1.0);
            }
            """;
    }
    
    // Getters and setters for cloud parameters
    public float getCloudCoverage() { return cloudCoverage; }
    public void setCloudCoverage(float coverage) { this.cloudCoverage = Math.max(0.0f, Math.min(1.0f, coverage)); }
    
    public float getCloudDensity() { return cloudDensity; }
    public void setCloudDensity(float density) { this.cloudDensity = Math.max(0.0f, density); }
    
    public Vector3f getWindDirection() { return new Vector3f(windDirection); }
    public void setWindDirection(Vector3f direction) { this.windDirection.set(direction.normalize()); }
    
    public float getWindSpeed() { return windSpeed; }
    public void setWindSpeed(float speed) { this.windSpeed = speed; }
    
    public int getMaxCloudSteps() { return maxCloudSteps; }
    public void setMaxCloudSteps(int steps) { this.maxCloudSteps = Math.max(1, Math.min(256, steps)); }
    
    public boolean isEnableDetailNoise() { return enableDetailNoise; }
    public void setEnableDetailNoise(boolean enable) { this.enableDetailNoise = enable; }
    
    public boolean isEnableWeatherSystem() { return enableWeatherSystem; }
    public void setEnableWeatherSystem(boolean enable) { this.enableWeatherSystem = enable; }
    
    /**
     * Cleanup OpenGL resources.
     */
    public void cleanup() {
        if (volumetricCloudsShader != null) {
            volumetricCloudsShader.cleanup();
        }
        
        if (fullscreenQuadVAO != 0) {
            GL30.glDeleteVertexArrays(fullscreenQuadVAO);
        }
        if (fullscreenQuadVBO != 0) {
            GL30.glDeleteBuffers(fullscreenQuadVBO);
        }
        
        if (shapeNoiseTexture3D != 0) {
            GL11.glDeleteTextures(shapeNoiseTexture3D);
        }
        if (detailNoiseTexture3D != 0) {
            GL11.glDeleteTextures(detailNoiseTexture3D);
        }
        if (weatherTexture2D != 0) {
            GL11.glDeleteTextures(weatherTexture2D);
        }
        if (curlNoiseTexture2D != 0) {
            GL11.glDeleteTextures(curlNoiseTexture2D);
        }
    }
}