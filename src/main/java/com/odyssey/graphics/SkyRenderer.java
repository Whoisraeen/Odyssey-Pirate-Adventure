package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Renders a dynamic sky dome that changes based on time of day.
 * Provides atmospheric scattering, sun/moon rendering, and star field.
 */
public class SkyRenderer {
    private static final Logger logger = LoggerFactory.getLogger(SkyRenderer.class);
    
    // Sky dome geometry
    private int skyVAO;
    private int skyVBO;
    private int skyEBO;
    private int vertexCount;
    
    // Sky shader
    private Shader skyShader;
    private ShaderManager shaderManager;
    
    // Sky dome parameters
    private static final int SKY_DOME_SEGMENTS = 32;
    private static final float SKY_DOME_RADIUS = 500.0f;
    
    public SkyRenderer(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
    }
    
    /**
     * Initializes the sky renderer.
     */
    public void initialize() {
        createSkyDome();
        createSkyShader();
        logger.info("Sky renderer initialized");
    }
    
    /**
     * Creates the sky dome geometry.
     */
    private void createSkyDome() {
        // Generate sky dome vertices
        float[] vertices = generateSkyDomeVertices();
        int[] indices = generateSkyDomeIndices();
        
        // Create VAO
        skyVAO = glGenVertexArrays();
        glBindVertexArray(skyVAO);
        
        // Create VBO
        skyVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skyVBO);
        
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        memFree(vertexBuffer);
        
        // Create EBO
        skyEBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, skyEBO);
        
        IntBuffer indexBuffer = memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        memFree(indexBuffer);
        
        vertexCount = indices.length;
        
        // Set vertex attributes (position only)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glBindVertexArray(0);
        
        logger.debug("Created sky dome with {} vertices", vertexCount);
    }
    
    /**
     * Generates vertices for the sky dome.
     */
    private float[] generateSkyDomeVertices() {
        int rings = SKY_DOME_SEGMENTS / 2;
        int sectors = SKY_DOME_SEGMENTS;
        
        float[] vertices = new float[(rings + 1) * (sectors + 1) * 3];
        int index = 0;
        
        for (int r = 0; r <= rings; r++) {
            float phi = (float) (Math.PI * r / rings / 2.0); // Only upper hemisphere
            float y = (float) (SKY_DOME_RADIUS * Math.cos(phi));
            float ringRadius = (float) (SKY_DOME_RADIUS * Math.sin(phi));
            
            for (int s = 0; s <= sectors; s++) {
                float theta = (float) (2.0 * Math.PI * s / sectors);
                float x = (float) (ringRadius * Math.cos(theta));
                float z = (float) (ringRadius * Math.sin(theta));
                
                vertices[index++] = x;
                vertices[index++] = y;
                vertices[index++] = z;
            }
        }
        
        return vertices;
    }
    
    /**
     * Generates indices for the sky dome.
     */
    private int[] generateSkyDomeIndices() {
        int rings = SKY_DOME_SEGMENTS / 2;
        int sectors = SKY_DOME_SEGMENTS;
        
        int[] indices = new int[rings * sectors * 6];
        int index = 0;
        
        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                int current = r * (sectors + 1) + s;
                int next = current + sectors + 1;
                
                // First triangle
                indices[index++] = current;
                indices[index++] = next;
                indices[index++] = current + 1;
                
                // Second triangle
                indices[index++] = current + 1;
                indices[index++] = next;
                indices[index++] = next + 1;
            }
        }
        
        return indices;
    }
    
    /**
     * Creates the sky shader with advanced atmospheric scattering.
     */
    private void createSkyShader() {
        String vertexShaderSource = """
            #version 330 core
            
            layout (location = 0) in vec3 aPos;
            
            uniform mat4 u_projectionMatrix;
            uniform mat4 u_viewMatrix;
            uniform vec3 u_cameraPos;
            
            out vec3 worldPos;
            out vec3 skyDirection;
            out vec3 viewDirection;
            
            void main() {
                worldPos = aPos;
                skyDirection = normalize(aPos);
                viewDirection = normalize(aPos - u_cameraPos);
                
                // Remove translation from view matrix for skybox effect
                mat4 viewNoTranslation = mat4(mat3(u_viewMatrix));
                vec4 pos = u_projectionMatrix * viewNoTranslation * vec4(aPos, 1.0);
                
                // Ensure sky is always at far plane
                gl_Position = pos.xyww;
            }
            """;
        
        String fragmentShaderSource = """
            #version 330 core
            
            in vec3 worldPos;
            in vec3 skyDirection;
            in vec3 viewDirection;
            
            uniform vec3 u_sunDirection;
            uniform vec3 u_moonDirection;
            uniform vec3 u_sunColor;
            uniform vec3 u_moonColor;
            uniform float u_sunIntensity;
            uniform float u_moonIntensity;
            uniform float u_timeOfDay;
            uniform vec3 u_cameraPos;
            
            // Atmospheric scattering parameters
            uniform float u_planetRadius;
            uniform float u_atmosphereRadius;
            uniform vec3 u_rayleighCoeff;
            uniform float u_mieCoeff;
            uniform float u_rayleighScaleHeight;
            uniform float u_mieScaleHeight;
            uniform float u_mieDirectionalG;
            
            out vec4 FragColor;
            
            const int SAMPLE_COUNT = 16;
            const int LIGHT_SAMPLE_COUNT = 8;
            
            // Ray-sphere intersection
            vec2 raySphereIntersect(vec3 rayOrigin, vec3 rayDir, float sphereRadius) {
                float a = dot(rayDir, rayDir);
                float b = 2.0 * dot(rayOrigin, rayDir);
                float c = dot(rayOrigin, rayOrigin) - sphereRadius * sphereRadius;
                float discriminant = b * b - 4.0 * a * c;
                
                if (discriminant < 0.0) {
                    return vec2(-1.0, -1.0);
                }
                
                float sqrtDiscriminant = sqrt(discriminant);
                float t1 = (-b - sqrtDiscriminant) / (2.0 * a);
                float t2 = (-b + sqrtDiscriminant) / (2.0 * a);
                
                return vec2(t1, t2);
            }
            
            // Density function for atmospheric layers
            float densityAtHeight(float height, float scaleHeight) {
                return exp(-height / scaleHeight);
            }
            
            // Phase function for Rayleigh scattering
            float rayleighPhase(float cosTheta) {
                return 3.0 / (16.0 * 3.14159265) * (1.0 + cosTheta * cosTheta);
            }
            
            // Phase function for Mie scattering (Henyey-Greenstein)
            float miePhase(float cosTheta, float g) {
                float g2 = g * g;
                return 3.0 / (8.0 * 3.14159265) * ((1.0 - g2) * (1.0 + cosTheta * cosTheta)) / 
                       ((2.0 + g2) * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
            }
            
            // Calculate optical depth along a ray
            float opticalDepth(vec3 rayOrigin, vec3 rayDir, float rayLength, float scaleHeight) {
                vec3 samplePoint = rayOrigin;
                vec3 sampleStep = rayDir * (rayLength / float(LIGHT_SAMPLE_COUNT));
                float opticalDepth = 0.0;
                
                for (int i = 0; i < LIGHT_SAMPLE_COUNT; i++) {
                    float height = length(samplePoint) - u_planetRadius;
                    if (height < 0.0) break;
                    
                    opticalDepth += densityAtHeight(height, scaleHeight) * (rayLength / float(LIGHT_SAMPLE_COUNT));
                    samplePoint += sampleStep;
                }
                
                return opticalDepth;
            }
            
            // Main atmospheric scattering calculation
            vec3 calculateScattering(vec3 rayOrigin, vec3 rayDir, vec3 sunDir) {
                // Find intersection with atmosphere
                vec2 atmosphereIntersect = raySphereIntersect(rayOrigin, rayDir, u_atmosphereRadius);
                if (atmosphereIntersect.x < 0.0) {
                    return vec3(0.0);
                }
                
                // Find intersection with planet (if any)
                vec2 planetIntersect = raySphereIntersect(rayOrigin, rayDir, u_planetRadius);
                float rayLength = (planetIntersect.x > 0.0) ? planetIntersect.x : atmosphereIntersect.y;
                
                // Ensure we start from atmosphere entry point
                float rayStart = max(atmosphereIntersect.x, 0.0);
                rayLength = max(rayLength - rayStart, 0.0);
                
                vec3 startPoint = rayOrigin + rayDir * rayStart;
                vec3 sampleStep = rayDir * (rayLength / float(SAMPLE_COUNT));
                vec3 samplePoint = startPoint + sampleStep * 0.5;
                
                vec3 rayleighScattering = vec3(0.0);
                vec3 mieScattering = vec3(0.0);
                
                for (int i = 0; i < SAMPLE_COUNT; i++) {
                    float height = length(samplePoint) - u_planetRadius;
                    if (height < 0.0) break;
                    
                    // Calculate densities
                    float rayleighDensity = densityAtHeight(height, u_rayleighScaleHeight);
                    float mieDensity = densityAtHeight(height, u_mieScaleHeight);
                    
                    // Calculate optical depth to sun
                    vec2 sunIntersect = raySphereIntersect(samplePoint, sunDir, u_atmosphereRadius);
                    float sunRayLength = sunIntersect.y;
                    
                    float rayleighOpticalDepthSun = opticalDepth(samplePoint, sunDir, sunRayLength, u_rayleighScaleHeight);
                    float mieOpticalDepthSun = opticalDepth(samplePoint, sunDir, sunRayLength, u_mieScaleHeight);
                    
                    // Calculate optical depth to camera
                    float rayleighOpticalDepthCamera = opticalDepth(startPoint, rayDir, length(samplePoint - startPoint), u_rayleighScaleHeight);
                    float mieOpticalDepthCamera = opticalDepth(startPoint, rayDir, length(samplePoint - startPoint), u_mieScaleHeight);
                    
                    // Calculate transmittance
                    vec3 rayleighTransmittance = exp(-(u_rayleighCoeff * (rayleighOpticalDepthSun + rayleighOpticalDepthCamera) + 
                                                     u_mieCoeff * (mieOpticalDepthSun + mieOpticalDepthCamera)));
                    
                    // Accumulate scattering
                    rayleighScattering += rayleighDensity * rayleighTransmittance;
                    mieScattering += mieDensity * rayleighTransmittance;
                    
                    samplePoint += sampleStep;
                }
                
                // Apply phase functions
                float cosTheta = dot(rayDir, sunDir);
                vec3 rayleighColor = rayleighScattering * u_rayleighCoeff * rayleighPhase(cosTheta);
                vec3 mieColor = mieScattering * u_mieCoeff * miePhase(cosTheta, u_mieDirectionalG);
                
                return (rayleighColor + mieColor) * u_sunIntensity * (rayLength / float(SAMPLE_COUNT));
            }
            
            void main() {
                vec3 direction = normalize(skyDirection);
                
                // Camera position relative to planet center
                vec3 cameraPos = vec3(0.0, u_planetRadius + 1000.0, 0.0); // 1km above surface
                
                // Calculate atmospheric scattering
                vec3 scatteredColor = calculateScattering(cameraPos, direction, -u_sunDirection);
                
                // Apply exposure and tone mapping
                scatteredColor = 1.0 - exp(-scatteredColor * 1.0);
                
                // Add sun disk
                float sunDot = max(dot(direction, -u_sunDirection), 0.0);
                float sunDisk = smoothstep(0.9998, 0.9999, sunDot) * u_sunIntensity;
                scatteredColor += u_sunColor * sunDisk;
                
                // Moon contribution (simplified)
                float moonDot = max(dot(direction, -u_moonDirection), 0.0);
                float moonHalo = pow(moonDot, 512.0) * u_moonIntensity;
                scatteredColor += u_moonColor * moonHalo;
                
                // Simple stars (only visible at night)
                float starIntensity = 1.0 - u_sunIntensity;
                if (starIntensity > 0.1 && direction.y > 0.1) {
                    float starNoise = fract(sin(dot(direction.xz * 100.0, vec2(12.9898, 78.233))) * 43758.5453);
                    if (starNoise > 0.998) {
                        scatteredColor += vec3(1.0) * starIntensity * 0.5;
                    }
                }
                
                FragColor = vec4(scatteredColor, 1.0);
            }
            """;
        
        try {
            skyShader = new Shader();
            skyShader.createVertexShader(vertexShaderSource);
            skyShader.createFragmentShader(fragmentShaderSource);
            skyShader.link();
            
            logger.info("Sky shader created successfully");
        } catch (Exception e) {
            logger.error("Failed to create sky shader", e);
            throw new RuntimeException("Sky shader creation failed", e);
        }
    }
    
    /**
     * Renders the sky dome.
     * @param viewMatrix The camera view matrix
     * @param projectionMatrix The camera projection matrix
     */
    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        render(viewMatrix, projectionMatrix, null);
    }
    
    /**
     * Renders the sky dome with time of day information.
     * @param viewMatrix The camera view matrix
     * @param projectionMatrix The camera projection matrix
     * @param timeOfDaySystem The time of day system (can be null for default values)
     */
    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix, TimeOfDaySystem timeOfDaySystem) {
        if (skyShader == null || skyVAO == 0) {
            return;
        }
        
        // Disable depth writing but keep depth testing
        glDepthMask(false);
        glDepthFunc(GL_LEQUAL);
        
        skyShader.bind();
        
        // Remove translation from view matrix (keep rotation only)
        Matrix4f skyViewMatrix = new Matrix4f(viewMatrix);
        skyViewMatrix.m30(0).m31(0).m32(0);
        
        skyShader.setUniform("u_projectionMatrix", projectionMatrix);
        skyShader.setUniform("u_viewMatrix", skyViewMatrix);
        skyShader.setUniform("u_cameraPos", new Vector3f(0.0f, 0.0f, 0.0f)); // Camera position for atmospheric calculations
        
        // Atmospheric scattering parameters (Earth-like values)
        skyShader.setUniform("u_planetRadius", 6371000.0f); // Earth radius in meters
        skyShader.setUniform("u_atmosphereRadius", 6471000.0f); // Atmosphere extends 100km above surface
        skyShader.setUniform("u_rayleighCoeff", new Vector3f(5.5e-6f, 13.0e-6f, 22.4e-6f)); // Rayleigh scattering coefficients for RGB
        skyShader.setUniform("u_mieCoeff", 21e-6f); // Mie scattering coefficient
        skyShader.setUniform("u_rayleighScaleHeight", 8000.0f); // Rayleigh scale height in meters
        skyShader.setUniform("u_mieScaleHeight", 1200.0f); // Mie scale height in meters
        skyShader.setUniform("u_mieDirectionalG", 0.758f); // Mie preferred scattering direction
        
        // Set time of day uniforms
        if (timeOfDaySystem != null) {
            skyShader.setUniform("u_sunDirection", timeOfDaySystem.getSunDirection());
            skyShader.setUniform("u_moonDirection", timeOfDaySystem.getMoonDirection());
            skyShader.setUniform("u_sunColor", timeOfDaySystem.getSunColor());
            skyShader.setUniform("u_moonColor", timeOfDaySystem.getMoonColor());
            skyShader.setUniform("u_sunIntensity", timeOfDaySystem.getSunIntensity() * 22.0f); // Increased intensity for scattering
            skyShader.setUniform("u_moonIntensity", timeOfDaySystem.getMoonIntensity());
            skyShader.setUniform("u_timeOfDay", (float) timeOfDaySystem.getDayTime());
        } else {
            // Default values for fallback
            skyShader.setUniform("u_sunDirection", new Vector3f(0.5f, 0.8f, 0.3f).normalize());
            skyShader.setUniform("u_moonDirection", new Vector3f(-0.5f, 0.6f, -0.3f).normalize());
            skyShader.setUniform("u_sunColor", new Vector3f(1.0f, 0.9f, 0.7f));
            skyShader.setUniform("u_moonColor", new Vector3f(0.8f, 0.8f, 1.0f));
            skyShader.setUniform("u_sunIntensity", 22.0f); // Increased intensity for scattering
            skyShader.setUniform("u_moonIntensity", 0.3f);
            skyShader.setUniform("u_timeOfDay", 0.5f); // 0.0 = midnight, 0.5 = noon, 1.0 = midnight
        }
        
        // Render sky dome
        glBindVertexArray(skyVAO);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        skyShader.unbind();
        
        // Restore depth state
        glDepthMask(true);
        glDepthFunc(GL_LESS);
    }
    
    /**
     * Cleans up sky renderer resources.
     */
    public void cleanup() {
        if (skyVAO != 0) {
            glDeleteVertexArrays(skyVAO);
        }
        if (skyVBO != 0) {
            glDeleteBuffers(skyVBO);
        }
        if (skyEBO != 0) {
            glDeleteBuffers(skyEBO);
        }
        if (skyShader != null) {
            skyShader.cleanup();
        }
        
        logger.info("Sky renderer cleanup completed");
    }
}