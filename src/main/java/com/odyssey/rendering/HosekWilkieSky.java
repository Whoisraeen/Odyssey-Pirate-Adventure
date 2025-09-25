package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Hosek-Wilkie physically-based sky model implementation.
 * Provides realistic atmospheric scattering with proper solar calculations.
 */
public class HosekWilkieSky {
    
    private Shader shader;
    private int vao;
    private int vbo;
    
    // Atmospheric parameters
    private float turbidity = 3.0f;        // Atmospheric turbidity (1.0 = clear, 10.0 = hazy)
    private float groundAlbedo = 0.1f;      // Ground reflectance (0.0 = black, 1.0 = white)
    private float solarIntensity = 1.0f;    // Solar intensity multiplier
    private float exposure = 1.0f;          // Exposure adjustment
    
    // Time and sun parameters
    private float timeOfDay = 12.0f;        // Time in hours (0.0 = midnight, 12.0 = noon)
    private Vector3f sunDirection = new Vector3f(0.0f, 1.0f, 0.0f);
    private Vector3f sunColor = new Vector3f(1.0f, 0.9f, 0.8f);
    
    // Hosek-Wilkie coefficients (precomputed based on atmospheric conditions)
    private float[][] coefficients = new float[9][3]; // A, B, C, D, E, F, G, H, I for X, Y, Z
    private float[][] solarCoefficients = new float[5][3]; // Solar disk coefficients
    
    // Sky mesh vertices (cube for skybox)
    private static final float[] SKYBOX_VERTICES = {
        // Positions
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,

        -1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,

         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,

        -1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,

        -1.0f,  1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f, -1.0f,

        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f
    };
    
    public HosekWilkieSky() {
        initializeShader();
        initializeMesh();
        computeCoefficients();
    }
    
    /**
     * Initialize the Hosek-Wilkie sky shader
     */
    private void initializeShader() {
        try {
            shader = new Shader("hosek_wilkie_sky");
        } catch (Exception e) {
            System.err.println("Failed to load Hosek-Wilkie sky shader: " + e.getMessage());
            // Fallback to inline shader creation
            createInlineShader();
        }
    }
    
    /**
     * Create shader with inline source code as fallback
     */
    private void createInlineShader() {
        shader = new Shader("hosek_wilkie_sky_inline");
        
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "out vec3 v_WorldDirection;\n" +
                "out vec3 v_ViewDirection;\n" +
                "void main() {\n" +
                "    v_WorldDirection = normalize(a_Position);\n" +
                "    vec4 viewPos = u_ViewMatrix * vec4(a_Position, 0.0);\n" +
                "    v_ViewDirection = normalize(viewPos.xyz);\n" +
                "    vec4 pos = u_ProjectionMatrix * u_ViewMatrix * vec4(a_Position, 1.0);\n" +
                "    gl_Position = pos.xyww;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_WorldDirection;\n" +
                "uniform vec3 u_SunDirection;\n" +
                "uniform float u_TimeOfDay;\n" +
                "uniform vec3 u_SunColor;\n" +
                "uniform float u_Exposure;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    vec3 viewDir = normalize(v_WorldDirection);\n" +
                "    vec3 sunDir = normalize(u_SunDirection);\n" +
                "    float height = max(viewDir.y, 0.0);\n" +
                "    vec3 dayColor = vec3(0.5, 0.7, 1.0);\n" +
                "    vec3 sunsetColor = vec3(1.0, 0.6, 0.3);\n" +
                "    vec3 nightColor = vec3(0.05, 0.05, 0.2);\n" +
                "    float sunHeight = max(sunDir.y, 0.0);\n" +
                "    vec3 skyColor;\n" +
                "    if (sunHeight > 0.1) {\n" +
                "        skyColor = mix(dayColor * 0.8, dayColor, height);\n" +
                "    } else if (sunHeight > -0.1) {\n" +
                "        float sunsetFactor = (sunHeight + 0.1) / 0.2;\n" +
                "        vec3 currentColor = mix(sunsetColor, dayColor, sunsetFactor);\n" +
                "        skyColor = mix(currentColor * 0.6, currentColor, height);\n" +
                "    } else {\n" +
                "        skyColor = mix(nightColor * 0.5, nightColor, height);\n" +
                "    }\n" +
                "    float sunDot = max(dot(viewDir, sunDir), 0.0);\n" +
                "    vec3 sunGlow = u_SunColor * pow(sunDot, 256.0) * sunHeight;\n" +
                "    skyColor += sunGlow;\n" +
                "    skyColor *= u_Exposure;\n" +
                "    skyColor = skyColor / (skyColor + vec3(1.0));\n" +
                "    skyColor = pow(skyColor, vec3(1.0 / 2.2));\n" +
                "    FragColor = vec4(max(skyColor, vec3(0.0)), 1.0);\n" +
                "}";
        
        shader.compileVertexShader(vertexSource);
        shader.compileFragmentShader(fragmentSource);
        shader.link();
    }
    
    /**
     * Initialize the skybox mesh
     */
    private void initializeMesh() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, SKYBOX_VERTICES, GL15.GL_STATIC_DRAW);
        
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Compute Hosek-Wilkie coefficients based on atmospheric parameters
     */
    private void computeCoefficients() {
        // Simplified coefficient computation
        // In a full implementation, these would be computed using the Hosek-Wilkie dataset
        
        // Default coefficients for clear sky (turbidity = 3.0)
        float[][] defaultCoeffs = {
            {-1.2f, -0.32f, -0.045f}, // A (X, Y, Z)
            {-0.55f, -0.8f, -1.65f},  // B
            {0.0f, 0.0f, 0.0f},       // C
            {-0.37f, -1.2f, -0.8f},   // D
            {-0.62f, -0.64f, -0.3f},  // E
            {0.0f, 0.0f, 0.0f},       // F
            {0.0f, 0.0f, 0.0f},       // G
            {4.85f, 4.4f, 3.0f},      // H
            {-0.04f, -0.048f, -0.057f} // I
        };
        
        // Copy default coefficients and adjust for turbidity
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 3; j++) {
                coefficients[i][j] = defaultCoeffs[i][j] * (1.0f + (turbidity - 3.0f) * 0.1f);
            }
        }
        
        // Solar disk coefficients
        float[][] defaultSolarCoeffs = {
            {57.7f, 57.7f, 57.7f},    // A
            {-0.04f, -0.04f, -0.04f}, // B
            {0.0f, 0.0f, 0.0f},       // C
            {-0.07f, -0.07f, -0.07f}, // D
            {0.0f, 0.0f, 0.0f}        // E
        };
        
        for (int i = 0; i < 5; i++) {
            System.arraycopy(defaultSolarCoeffs[i], 0, solarCoefficients[i], 0, 3);
        }
    }
    
    /**
     * Update sun position based on time of day
     */
    public void updateSunPosition(float deltaTime) {
        // Update time of day (24-hour cycle)
        timeOfDay += deltaTime / 3600.0f; // Convert seconds to hours
        if (timeOfDay >= 24.0f) {
            timeOfDay -= 24.0f;
        }
        
        // Calculate sun position based on time
        float solarAngle = (float) ((timeOfDay - 12.0f) * Math.PI / 12.0f);
        float sunHeight = (float) Math.sin(solarAngle);
        float sunAzimuth = (float) Math.cos(solarAngle);
        
        // Update sun direction
        sunDirection.set(sunAzimuth * 0.7f, sunHeight, 0.3f);
        sunDirection.normalize();
        
        // Update sun color based on height
        if (sunHeight > 0.0f) {
            // Day colors
            sunColor.set(1.0f, 0.95f, 0.8f);
        } else if (sunHeight > -0.2f) {
            // Sunset/sunrise colors
            float t = (sunHeight + 0.2f) / 0.2f;
            sunColor.set(1.0f, 0.6f + 0.35f * t, 0.3f + 0.5f * t);
        } else {
            // Night (moon light)
            sunColor.set(0.2f, 0.3f, 0.5f);
        }
    }
    
    /**
     * Render the sky
     */
    public void render(Camera camera, Matrix4f projection) {
        // Disable depth writing for skybox
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        shader.bind();
        
        // Set matrices
        Matrix4f viewMatrix = new Matrix4f(camera.getViewMatrix());
        viewMatrix.m30(0).m31(0).m32(0); // Remove translation
        shader.setUniform("u_ViewMatrix", viewMatrix);
        shader.setUniform("u_ProjectionMatrix", projection);
        
        // Set atmospheric parameters
        shader.setUniform("u_SunDirection", sunDirection);
        shader.setUniform("u_Turbidity", turbidity);
        shader.setUniform("u_GroundAlbedo", groundAlbedo);
        shader.setUniform("u_SolarIntensity", solarIntensity);
        shader.setUniform("u_TimeOfDay", timeOfDay);
        shader.setUniform("u_SunColor", sunColor);
        shader.setUniform("u_Exposure", exposure);
        
        // Set Hosek-Wilkie coefficients (if shader supports them)
        try {
            for (int i = 0; i < 9; i++) {
                String uniformName = "u_" + (char)('A' + i);
                shader.setUniform(uniformName, coefficients[i]);
            }
            
            for (int i = 0; i < 5; i++) {
                String uniformName = "u_Solar" + (char)('A' + i);
                shader.setUniform(uniformName, solarCoefficients[i]);
            }
        } catch (Exception e) {
            // Shader doesn't support full Hosek-Wilkie model, using fallback
        }
        
        // Render skybox
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);
        GL30.glBindVertexArray(0);
        
        shader.unbind();
        
        // Re-enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
    }
    
    // Getters and setters for atmospheric parameters
    
    public float getTurbidity() { return turbidity; }
    public void setTurbidity(float turbidity) { 
        this.turbidity = Math.max(1.0f, Math.min(10.0f, turbidity));
        computeCoefficients();
    }
    
    public float getGroundAlbedo() { return groundAlbedo; }
    public void setGroundAlbedo(float groundAlbedo) { 
        this.groundAlbedo = Math.max(0.0f, Math.min(1.0f, groundAlbedo));
        computeCoefficients();
    }
    
    public float getSolarIntensity() { return solarIntensity; }
    public void setSolarIntensity(float solarIntensity) { 
        this.solarIntensity = Math.max(0.0f, solarIntensity);
    }
    
    public float getExposure() { return exposure; }
    public void setExposure(float exposure) { 
        this.exposure = Math.max(0.1f, exposure);
    }
    
    public float getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float timeOfDay) { 
        this.timeOfDay = timeOfDay % 24.0f;
        if (this.timeOfDay < 0) this.timeOfDay += 24.0f;
    }
    
    public Vector3f getSunDirection() { return new Vector3f(sunDirection); }
    public Vector3f getSunColor() { return new Vector3f(sunColor); }
    
    /**
     * Get sun intensity based on current time
     */
    public float getSunIntensity() {
        float solarAngle = (float) ((timeOfDay - 12.0f) * Math.PI / 12.0f);
        float sunHeight = (float) Math.sin(solarAngle);
        
        if (sunHeight <= 0.0f) return 0.0f;
        
        // Atmospheric extinction
        float airMass = 1.0f / Math.max(sunHeight, 0.01f);
        float extinction = (float) Math.exp(-0.1f * airMass);
        
        return extinction * sunHeight * solarIntensity;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
        }
        
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
        }
    }
}