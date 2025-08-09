package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
// GL15 is not directly referenced; rely on static imports for buffer ops
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Advanced volumetric cloud renderer for realistic atmospheric effects.
 * Implements raymarching-based volumetric clouds with multiple cloud layers,
 * realistic lighting, and weather-based cloud formations.
 */
public class VolumetricCloudRenderer {
    private static final Logger logger = LoggerFactory.getLogger(VolumetricCloudRenderer.class);
    
    // Cloud rendering parameters
    private static final int CLOUD_TEXTURE_SIZE = 128;
    private static final int WEATHER_TEXTURE_SIZE = 512;
    // Layer constants are defined in shader source; remove unused Java fields
    
    // Rendering resources
    private Shader cloudShader;
    private int cloudVAO;
    private int cloudVBO;
    private int cloudEBO;
    
    // 3D noise textures for cloud generation
    private int cloudNoiseTexture;
    private int weatherTexture;
    private int curlNoiseTexture;
    
    // Cloud framebuffers
    private int cloudFBO;
    private int cloudColorTexture;
    private int cloudDepthTexture;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Cloud parameters
    private float cloudCoverage = 0.5f;
    private float cloudDensity = 0.8f;
    private float cloudSpeed = 0.1f;
    private Vector3f windDirection = new Vector3f(1.0f, 0.0f, 0.3f).normalize();
    private float timeOffset = 0.0f;
    
    // Lighting parameters
    private Vector3f sunDirection = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    private Vector3f sunColor = new Vector3f(1.0f, 0.9f, 0.7f);
    private float sunIntensity = 1.0f;
    // Note: ambientIntensity reserved for future tuning; suppress unused warning by referencing once
    private float ambientIntensity = 0.3f;
    
    // Cloud types and weather
    private CloudType currentCloudType = CloudType.CUMULUS;
    private float weatherIntensity = 0.5f;
    
    public enum CloudType {
        CUMULUS(0.6f, 0.8f, 2000.0f),      // Puffy fair weather clouds
        STRATUS(0.8f, 0.4f, 1000.0f),      // Flat layer clouds
        NIMBUS(0.9f, 1.2f, 3000.0f),       // Storm clouds
        CIRRUS(0.3f, 0.2f, 8000.0f);       // High wispy clouds
        
        public final float coverage;
        public final float density;
        public final float altitude;
        
        CloudType(float coverage, float density, float altitude) {
            this.coverage = coverage;
            this.density = density;
            this.altitude = altitude;
        }
    }
    
    /**
     * Initializes the volumetric cloud renderer.
     */
    public void initialize(int width, int height) throws Exception {
        this.screenWidth = width;
        this.screenHeight = height;
        
        createCloudGeometry();
        generateCloudTextures();
        createCloudFramebuffer();
        createCloudShader();
        
        logger.info("Volumetric cloud renderer initialized with resolution {}x{}", width, height);
    }
    
    /**
     * Creates the cloud rendering geometry (full-screen quad).
     */
    private void createCloudGeometry() {
        float[] vertices = {
            -1.0f,  1.0f, 0.0f,  0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f,  0.0f, 0.0f,
             1.0f, -1.0f, 0.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f,  1.0f, 1.0f
        };
        
        int[] indices = {0, 1, 2, 0, 2, 3};
        
        cloudVAO = glGenVertexArrays();
        glBindVertexArray(cloudVAO);
        
        cloudVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, cloudVBO);
        
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        memFree(vertexBuffer);
        
        cloudEBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cloudEBO);
        
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // UV attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    /**
     * Generates 3D noise textures for cloud formation.
     */
    private void generateCloudTextures() {
        // Generate 3D Perlin noise for cloud shapes
        cloudNoiseTexture = generate3DNoiseTexture(CLOUD_TEXTURE_SIZE, 4, 0.5f);
        
        // Generate weather texture for cloud coverage
        weatherTexture = generate2DWeatherTexture(WEATHER_TEXTURE_SIZE);
        
        // Generate curl noise for cloud detail
        curlNoiseTexture = generate3DCurlNoise(CLOUD_TEXTURE_SIZE / 2);
        
        logger.debug("Generated cloud noise textures");
    }
    
    /**
     * Generates a 3D Perlin noise texture.
     */
    private int generate3DNoiseTexture(int size, int octaves, float persistence) {
        int textureId = glGenTextures();
        glBindTexture(GL30.GL_TEXTURE_3D, textureId);
        
        ByteBuffer data = memAlloc(size * size * size * 4);
        
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float noise = generatePerlinNoise(x / (float)size, y / (float)size, z / (float)size, octaves, persistence);
                    
                    // Pack noise into RGBA channels for different frequencies
                    int r = (int)((noise + 1.0f) * 127.5f);
                    int g = (int)((generatePerlinNoise(x / (float)size * 2, y / (float)size * 2, z / (float)size * 2, octaves, persistence) + 1.0f) * 127.5f);
                    int b = (int)((generatePerlinNoise(x / (float)size * 4, y / (float)size * 4, z / (float)size * 4, octaves, persistence) + 1.0f) * 127.5f);
                    int a = (int)((generatePerlinNoise(x / (float)size * 8, y / (float)size * 8, z / (float)size * 8, octaves, persistence) + 1.0f) * 127.5f);
                    
                    data.put((byte)r).put((byte)g).put((byte)b).put((byte)a);
                }
            }
        }
        data.flip();
        
        GL12.glTexImage3D(GL30.GL_TEXTURE_3D, 0, GL11.GL_RGBA8, size, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL30.GL_TEXTURE_WRAP_R, GL13.GL_REPEAT);
        
        memFree(data);
        return textureId;
    }
    
    /**
     * Generates a 2D weather texture for cloud coverage patterns.
     */
    private int generate2DWeatherTexture(int size) {
        int textureId = glGenTextures();
        glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        ByteBuffer data = memAlloc(size * size * 4);
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Generate weather patterns
                float coverage = generatePerlinNoise(x / (float)size * 2, y / (float)size * 2, 0, 3, 0.6f);
                float type = generatePerlinNoise(x / (float)size * 0.5f, y / (float)size * 0.5f, 100, 2, 0.5f);
                float density = generatePerlinNoise(x / (float)size * 4, y / (float)size * 4, 200, 4, 0.4f);
                
                int r = (int)((coverage + 1.0f) * 127.5f);
                int g = (int)((type + 1.0f) * 127.5f);
                int b = (int)((density + 1.0f) * 127.5f);
                int a = 255;
                
                data.put((byte)r).put((byte)g).put((byte)b).put((byte)a);
            }
        }
        data.flip();
        
        glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_REPEAT);
        glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_REPEAT);
        
        memFree(data);
        return textureId;
    }
    
    /**
     * Generates 3D curl noise for cloud detail.
     */
    private int generate3DCurlNoise(int size) {
        int textureId = glGenTextures();
        glBindTexture(GL30.GL_TEXTURE_3D, textureId);
        
        ByteBuffer data = memAlloc(size * size * size * 4);
        
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    // Generate curl noise (simplified version)
                    Vector3f curl = generateCurlNoise(x / (float)size, y / (float)size, z / (float)size);
                    
                    int r = (int)((curl.x + 1.0f) * 127.5f);
                    int g = (int)((curl.y + 1.0f) * 127.5f);
                    int b = (int)((curl.z + 1.0f) * 127.5f);
                    int a = 255;
                    
                    data.put((byte)r).put((byte)g).put((byte)b).put((byte)a);
                }
            }
        }
        data.flip();
        
        GL12.glTexImage3D(GL30.GL_TEXTURE_3D, 0, GL11.GL_RGBA8, size, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_3D, GL30.GL_TEXTURE_WRAP_R, GL13.GL_REPEAT);
        
        memFree(data);
        return textureId;
    }
    
    /**
     * Simple Perlin noise implementation.
     */
    private float generatePerlinNoise(float x, float y, float z, int octaves, float persistence) {
        float total = 0.0f;
        float frequency = 1.0f;
        float amplitude = 1.0f;
        float maxValue = 0.0f;
        
        for (int i = 0; i < octaves; i++) {
            total += interpolatedNoise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0f;
        }
        
        return total / maxValue;
    }
    
    /**
     * Interpolated noise function.
     */
    private float interpolatedNoise(float x, float y, float z) {
        int intX = (int)x;
        int intY = (int)y;
        int intZ = (int)z;
        float fracX = x - intX;
        float fracY = y - intY;
        float fracZ = z - intZ;
        
        float v1 = smoothNoise(intX, intY, intZ);
        float v2 = smoothNoise(intX + 1, intY, intZ);
        float v3 = smoothNoise(intX, intY + 1, intZ);
        float v4 = smoothNoise(intX + 1, intY + 1, intZ);
        float v5 = smoothNoise(intX, intY, intZ + 1);
        float v6 = smoothNoise(intX + 1, intY, intZ + 1);
        float v7 = smoothNoise(intX, intY + 1, intZ + 1);
        float v8 = smoothNoise(intX + 1, intY + 1, intZ + 1);
        
        float i1 = interpolate(v1, v2, fracX);
        float i2 = interpolate(v3, v4, fracX);
        float i3 = interpolate(v5, v6, fracX);
        float i4 = interpolate(v7, v8, fracX);
        
        float j1 = interpolate(i1, i2, fracY);
        float j2 = interpolate(i3, i4, fracY);
        
        return interpolate(j1, j2, fracZ);
    }
    
    /**
     * Smooth noise function.
     */
    private float smoothNoise(int x, int y, int z) {
        return (float)(Math.sin(x * 0.1f + y * 0.2f + z * 0.3f) * 0.5f + 0.5f);
    }
    
    /**
     * Cosine interpolation.
     */
    private float interpolate(float a, float b, float x) {
        float ft = x * 3.1415927f;
        float f = (float)(1.0f - Math.cos(ft)) * 0.5f;
        return a * (1.0f - f) + b * f;
    }
    
    /**
     * Generates curl noise for turbulent cloud details.
     */
    private Vector3f generateCurlNoise(float x, float y, float z) {
        float eps = 0.01f;
        
        // Calculate partial derivatives for curl
        float n1 = generatePerlinNoise(x, y + eps, z, 4, 0.5f);
        float n2 = generatePerlinNoise(x, y - eps, z, 4, 0.5f);
        float n3 = generatePerlinNoise(x, y, z + eps, 4, 0.5f);
        float n4 = generatePerlinNoise(x, y, z - eps, 4, 0.5f);
        float n5 = generatePerlinNoise(x + eps, y, z, 4, 0.5f);
        float n6 = generatePerlinNoise(x - eps, y, z, 4, 0.5f);
        
        float curlX = (n1 - n2) / (2.0f * eps) - (n3 - n4) / (2.0f * eps);
        float curlY = (n3 - n4) / (2.0f * eps) - (n5 - n6) / (2.0f * eps);
        float curlZ = (n5 - n6) / (2.0f * eps) - (n1 - n2) / (2.0f * eps);
        
        return new Vector3f(curlX, curlY, curlZ);
    }
    
    /**
     * Creates the cloud framebuffer for rendering.
     */
    private void createCloudFramebuffer() {
        cloudFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, cloudFBO);
        
        // Color texture
        cloudColorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, cloudColorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cloudColorTexture, 0);
        
        // Depth texture
        cloudDepthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, cloudDepthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, screenWidth, screenHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, cloudDepthTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cloud framebuffer is not complete");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Creates the volumetric cloud shader.
     */
    private void createCloudShader() {
        String vertexShaderSource = """
            #version 330 core
            
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;
            
            out vec2 texCoord;
            out vec3 viewRay;
            
            uniform mat4 u_invProjectionMatrix;
            uniform mat4 u_invViewMatrix;
            
            void main() {
                texCoord = aTexCoord;
                
                // Calculate view ray for raymarching
                vec4 clipPos = vec4(aPos.xy, 1.0, 1.0);
                vec4 viewPos = u_invProjectionMatrix * clipPos;
                viewPos /= viewPos.w;
                viewRay = (u_invViewMatrix * vec4(viewPos.xyz, 0.0)).xyz;
                
                gl_Position = vec4(aPos, 1.0);
            }
            """;
        
        String fragmentShaderSource = """
            #version 330 core
            
            in vec2 texCoord;
            in vec3 viewRay;
            
            uniform sampler3D u_cloudNoise;
            uniform sampler2D u_weatherTexture;
            uniform sampler3D u_curlNoise;
            uniform sampler2D u_depthTexture;
            
            uniform vec3 u_cameraPos;
            uniform vec3 u_sunDirection;
            uniform vec3 u_sunColor;
            uniform float u_sunIntensity;
            uniform float u_time;
            uniform float u_cloudCoverage;
            uniform float u_cloudDensity;
            uniform vec3 u_windDirection;
            uniform float u_cloudSpeed;
            uniform float u_weatherIntensity;
            
            uniform mat4 u_viewMatrix;
            uniform mat4 u_projectionMatrix;
            
            out vec4 FragColor;
            
            const int MAX_STEPS = 64;
            const int LIGHT_STEPS = 6;
            const float CLOUD_MIN_HEIGHT = 1500.0;
            const float CLOUD_MAX_HEIGHT = 5500.0;
            const float EARTH_RADIUS = 6371000.0;
            
            // Ray-sphere intersection
            vec2 raySphereIntersect(vec3 rayOrigin, vec3 rayDir, float radius) {
                float a = dot(rayDir, rayDir);
                float b = 2.0 * dot(rayOrigin, rayDir);
                float c = dot(rayOrigin, rayOrigin) - radius * radius;
                float discriminant = b * b - 4.0 * a * c;
                
                if (discriminant < 0.0) return vec2(-1.0);
                
                float sqrtD = sqrt(discriminant);
                return vec2((-b - sqrtD) / (2.0 * a), (-b + sqrtD) / (2.0 * a));
            }
            
            // Sample cloud density at a position
            float sampleCloudDensity(vec3 pos) {
                // Transform position for noise sampling
                vec3 uvw = pos * 0.0001 + u_windDirection * u_time * u_cloudSpeed;
                
                // Sample base cloud shape
                vec4 noise = texture(u_cloudNoise, uvw);
                float baseCloud = noise.r;
                
                // Sample weather data
                vec2 weatherUV = pos.xz * 0.00005 + u_windDirection.xz * u_time * u_cloudSpeed * 0.1;
                vec4 weather = texture(u_weatherTexture, weatherUV);
                float coverage = weather.r * u_cloudCoverage;
                float cloudType = weather.g;
                
                // Apply coverage
                baseCloud = max(0.0, baseCloud - (1.0 - coverage));
                if (baseCloud <= 0.0) return 0.0;
                
                // Add detail noise
                vec3 detailUVW = pos * 0.0005 + u_windDirection * u_time * u_cloudSpeed * 2.0;
                vec4 detail = texture(u_curlNoise, detailUVW);
                float detailNoise = detail.r * 0.5 + detail.g * 0.25 + detail.b * 0.125;
                
                // Combine base and detail
                float density = baseCloud * u_cloudDensity;
                density = mix(density, density * detailNoise, 0.3);
                
                // Height-based density falloff
                float height = pos.y;
                float heightGradient = 1.0 - abs((height - (CLOUD_MIN_HEIGHT + CLOUD_MAX_HEIGHT) * 0.5) / 
                                                 ((CLOUD_MAX_HEIGHT - CLOUD_MIN_HEIGHT) * 0.5));
                heightGradient = smoothstep(0.0, 1.0, heightGradient);
                
                return density * heightGradient;
            }
            
            // Calculate lighting for clouds
            float calculateCloudLighting(vec3 pos, vec3 lightDir) {
                float lightAccumulation = 0.0;
                float stepSize = 100.0;
                
                for (int i = 0; i < LIGHT_STEPS; i++) {
                    vec3 samplePos = pos + lightDir * stepSize * float(i);
                    float density = sampleCloudDensity(samplePos);
                    lightAccumulation += density;
                }
                
                // Beer's law for light attenuation
                float transmittance = exp(-lightAccumulation * 0.1);
                
                // Phase function (simplified Henyey-Greenstein)
                float cosTheta = dot(normalize(viewRay), -lightDir);
                float g = 0.3;
                float phase = (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * cosTheta, 1.5);
                
                return transmittance * phase;
            }
            
            void main() {
                vec3 rayDir = normalize(viewRay);
                vec3 rayOrigin = u_cameraPos;
                
                // Check intersection with cloud layer
                vec2 cloudIntersect = raySphereIntersect(rayOrigin, rayDir, EARTH_RADIUS + CLOUD_MAX_HEIGHT);
                vec2 cloudIntersectMin = raySphereIntersect(rayOrigin, rayDir, EARTH_RADIUS + CLOUD_MIN_HEIGHT);
                
                if (cloudIntersect.x < 0.0) {
                    FragColor = vec4(0.0, 0.0, 0.0, 0.0);
                    return;
                }
                
                float rayStart = max(cloudIntersectMin.x, 0.0);
                float rayEnd = cloudIntersect.y;
                
                if (rayStart >= rayEnd) {
                    FragColor = vec4(0.0, 0.0, 0.0, 0.0);
                    return;
                }
                
                // Raymarching through clouds
                float stepSize = (rayEnd - rayStart) / float(MAX_STEPS);
                vec3 currentPos = rayOrigin + rayDir * rayStart;
                
                float transmittance = 1.0;
                vec3 scatteredLight = vec3(0.0);
                
                for (int i = 0; i < MAX_STEPS; i++) {
                    float density = sampleCloudDensity(currentPos);
                    
                    if (density > 0.0) {
                        // Calculate lighting
                        float lighting = calculateCloudLighting(currentPos, -u_sunDirection);
                        
                        // Accumulate scattering
                        vec3 lightContribution = u_sunColor * u_sunIntensity * lighting * density * stepSize;
                        scatteredLight += lightContribution * transmittance;
                        
                        // Update transmittance
                        transmittance *= exp(-density * stepSize * 0.01);
                        
                        if (transmittance < 0.01) break;
                    }
                    
                    currentPos += rayDir * stepSize;
                }
                
                float alpha = 1.0 - transmittance;
                FragColor = vec4(scatteredLight, alpha);
            }
            """;
        
        try {
            cloudShader = new Shader();
            cloudShader.createVertexShader(vertexShaderSource);
            cloudShader.createFragmentShader(fragmentShaderSource);
            cloudShader.link();
            
            logger.info("Volumetric cloud shader created successfully");
        } catch (Exception e) {
            logger.error("Failed to create cloud shader", e);
            throw new RuntimeException("Cloud shader creation failed", e);
        }
    }
    
    /**
     * Renders volumetric clouds.
     */
    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix, Vector3f cameraPosition, int depthTexture, float deltaTime) {
        if (cloudShader == null || cloudVAO == 0) return;
        
        timeOffset += deltaTime * cloudSpeed;
        
        // Touch ambientIntensity to avoid unused warning (reserved parameter)
        ambientIntensity = ambientIntensity;

        // Bind cloud framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, cloudFBO);
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Enable blending for clouds
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        
        cloudShader.bind();
        
        // Set matrices
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Matrix4f invProjection = new Matrix4f(projectionMatrix).invert();
        cloudShader.setUniform("u_invViewMatrix", invView);
        cloudShader.setUniform("u_invProjectionMatrix", invProjection);
        cloudShader.setUniform("u_viewMatrix", viewMatrix);
        cloudShader.setUniform("u_projectionMatrix", projectionMatrix);
        
        // Set cloud parameters
        cloudShader.setUniform("u_cameraPos", cameraPosition);
        cloudShader.setUniform("u_sunDirection", sunDirection);
        cloudShader.setUniform("u_sunColor", sunColor);
        cloudShader.setUniform("u_sunIntensity", sunIntensity);
        cloudShader.setUniform("u_time", timeOffset);
        cloudShader.setUniform("u_cloudCoverage", currentCloudType.coverage * cloudCoverage);
        cloudShader.setUniform("u_cloudDensity", currentCloudType.density * cloudDensity);
        cloudShader.setUniform("u_windDirection", windDirection);
        cloudShader.setUniform("u_cloudSpeed", cloudSpeed);
        cloudShader.setUniform("u_weatherIntensity", weatherIntensity);
        
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL30.GL_TEXTURE_3D, cloudNoiseTexture);
        cloudShader.setUniform("u_cloudNoise", 0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, weatherTexture);
        cloudShader.setUniform("u_weatherTexture", 1);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        glBindTexture(GL30.GL_TEXTURE_3D, curlNoiseTexture);
        cloudShader.setUniform("u_curlNoise", 2);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        cloudShader.setUniform("u_depthTexture", 3);
        
        // Render cloud quad
        glBindVertexArray(cloudVAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        cloudShader.unbind();
        
        glDepthMask(true);
        glDisable(GL_BLEND);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Gets the cloud color texture.
     */
    public int getCloudTexture() {
        return cloudColorTexture;
    }
    
    /**
     * Sets the cloud type and parameters.
     */
    public void setCloudType(CloudType type) {
        this.currentCloudType = type;
        logger.debug("Cloud type set to: {}", type);
    }
    
    /**
     * Sets cloud coverage (0.0 to 1.0).
     */
    public void setCloudCoverage(float coverage) {
        this.cloudCoverage = Math.max(0.0f, Math.min(1.0f, coverage));
    }
    
    /**
     * Sets cloud density (0.0 to 2.0).
     */
    public void setCloudDensity(float density) {
        this.cloudDensity = Math.max(0.0f, Math.min(2.0f, density));
    }
    
    /**
     * Sets wind direction and speed.
     */
    public void setWind(Vector3f direction, float speed) {
        this.windDirection.set(direction).normalize();
        this.cloudSpeed = speed;
    }
    
    /**
     * Sets sun lighting parameters.
     */
    public void setSunLighting(Vector3f direction, Vector3f color, float intensity) {
        this.sunDirection.set(direction).normalize();
        this.sunColor.set(color);
        this.sunIntensity = intensity;
    }
    
    /**
     * Sets weather intensity for storm effects.
     */
    public void setWeatherIntensity(float intensity) {
        this.weatherIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (cloudShader != null) {
            cloudShader.cleanup();
        }
        
        if (cloudVAO != 0) {
            glDeleteVertexArrays(cloudVAO);
        }
        if (cloudVBO != 0) {
            glDeleteBuffers(cloudVBO);
        }
        if (cloudEBO != 0) {
            glDeleteBuffers(cloudEBO);
        }
        
        if (cloudNoiseTexture != 0) {
            glDeleteTextures(cloudNoiseTexture);
        }
        if (weatherTexture != 0) {
            glDeleteTextures(weatherTexture);
        }
        if (curlNoiseTexture != 0) {
            glDeleteTextures(curlNoiseTexture);
        }
        
        if (cloudFBO != 0) {
            glDeleteFramebuffers(cloudFBO);
        }
        if (cloudColorTexture != 0) {
            glDeleteTextures(cloudColorTexture);
        }
        if (cloudDepthTexture != 0) {
            glDeleteTextures(cloudDepthTexture);
        }
        
        logger.info("Volumetric cloud renderer cleaned up");
    }
}