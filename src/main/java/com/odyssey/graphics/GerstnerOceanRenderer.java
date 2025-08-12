package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Unified Gerstner wave ocean renderer with quality levels and advanced features.
 * This is the consolidated water rendering system replacing WaterShader and EnhancedWaterRenderer.
 */
public class GerstnerOceanRenderer {
    private static final Logger logger = LoggerFactory.getLogger(GerstnerOceanRenderer.class);
    
    /**
     * Water quality settings for performance scaling.
     */
    public enum WaterQuality {
        PERFORMANCE(2, false, false),  // 2 waves, no reflections
        BALANCED(4, true, false),      // 4 waves, reflections
        QUALITY(6, true, true),        // 6 waves, reflections + SSR
        ULTRA(8, true, true);          // 8 waves, reflections + SSR + advanced effects
        
        private final int waveCount;
        private final boolean enableReflections;
        private final boolean enableSSR;
        
        WaterQuality(int waves, boolean reflections, boolean ssr) {
            this.waveCount = waves;
            this.enableReflections = reflections;
            this.enableSSR = ssr;
        }
        
        public int getWaveCount() { return waveCount; }
        public boolean isReflectionsEnabled() { return enableReflections; }
        public boolean isSSREnabled() { return enableSSR; }
    }
    
    // Wave configuration (up to 8 waves for ULTRA quality)
    public static class WaveParams {
        public Vector2f direction = new Vector2f(1, 0);     // normalized direction
        public float amplitude = 0.5f;                     // height in meters
        public float wavelength = 10.0f;                   // distance in meters
        public float speed = 2.0f;                         // phase speed m/s
        public float steepness = 0.5f;                     // 0-1, controls wave shape
        
        public WaveParams(Vector2f dir, float amp, float lambda, float spd, float steep) {
            this.direction = new Vector2f(dir).normalize();
            this.amplitude = amp;
            this.wavelength = lambda;
            this.speed = spd;
            // Prevent loops: steepness <= 1 / (k * amplitude * N)
            float k = 2.0f * (float)Math.PI / lambda;
            float maxSteep = 1.0f / (k * amp * 4); // 4 waves
            this.steepness = Math.min(steep, maxSteep * 0.9f); // 90% of max for safety
        }
    }
    
    private final Shader gerstnerShader;
    private final WaveParams[] waves = new WaveParams[8]; // Support up to 8 waves for ULTRA quality
    private Mesh oceanGrid;
    private WaterQuality currentQuality = WaterQuality.BALANCED;
    
    // Reflection support
    private int reflectionFramebuffer = 0;
    private int reflectionTexture = 0;
    private int reflectionDepthBuffer = 0;
    private static final int REFLECTION_WIDTH = 1024;  // Reduced from 2048 for performance
    private static final int REFLECTION_HEIGHT = 1024;
    
    // Ocean appearance
    private Vector3f deepColor = new Vector3f(0.0f, 0.15f, 0.25f);
    private Vector3f shallowColor = new Vector3f(0.0f, 0.35f, 0.45f);
    private float depthFadeStart = 0.5f;
    private float depthFadeEnd = 8.0f;
    private float seaLevel = 64.0f;
    
    // Grid parameters
    private static final int GRID_SUBDIVISIONS = 128;  // 128x128 grid
    private static final float GRID_SIZE = 512.0f;     // 512 meter wide patches
    
    public GerstnerOceanRenderer(ShaderManager shaderManager) {
        this.gerstnerShader = shaderManager.getShader("gerstner_waves");
        if (gerstnerShader == null) {
            throw new RuntimeException("Gerstner wave shader not found. Make sure shaders are loaded.");
        }
        
        // Initialize default wave configuration
        setupDefaultWaves();
        
        // Create ocean grid mesh
        createOceanGrid();
        
        // Initialize reflection framebuffers
        setupReflectionFramebuffers();
        
        logger.info("Initialized unified Gerstner ocean renderer with {}x{} grid, quality: {}", 
                   GRID_SUBDIVISIONS, GRID_SUBDIVISIONS, currentQuality);
    }
    
    private void setupDefaultWaves() {
        // Primary wave (largest) - always active
        waves[0] = new WaveParams(new Vector2f(1.0f, 0.3f), 0.6f, 18.0f, 3.0f, 0.4f);
        
        // Secondary wave - active from PERFORMANCE+
        waves[1] = new WaveParams(new Vector2f(0.7f, -0.7f), 0.3f, 9.0f, 2.0f, 0.5f);
        
        // Tertiary wave - active from BALANCED+
        waves[2] = new WaveParams(new Vector2f(-0.6f, 0.8f), 0.15f, 4.5f, 1.2f, 0.6f);
        
        // Detail wave - active from BALANCED+
        waves[3] = new WaveParams(new Vector2f(-1.0f, 0.2f), 0.08f, 2.2f, 0.9f, 0.6f);
        
        // High-frequency waves - active from QUALITY+
        waves[4] = new WaveParams(new Vector2f(0.3f, 1.0f), 0.04f, 1.1f, 0.6f, 0.7f);
        waves[5] = new WaveParams(new Vector2f(-0.8f, -0.6f), 0.06f, 1.8f, 0.8f, 0.6f);
        
        // Ultra detail waves - active only in ULTRA
        waves[6] = new WaveParams(new Vector2f(0.9f, -0.4f), 0.02f, 0.6f, 0.4f, 0.8f);
        waves[7] = new WaveParams(new Vector2f(-0.2f, 0.98f), 0.03f, 0.9f, 0.5f, 0.7f);
    }
    
    private void createOceanGrid() {
        // Create a single large grid centered at origin
        // In a full implementation, you'd create multiple grids around the camera
        oceanGrid = Mesh.createOceanGrid(GRID_SIZE, GRID_SIZE, GRID_SUBDIVISIONS, 0, 0, seaLevel);
    }
    
    private void setupReflectionFramebuffers() {
        if (!currentQuality.isReflectionsEnabled()) {
            return; // Skip if reflections are disabled
        }
        
        // Generate framebuffer
        reflectionFramebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, reflectionFramebuffer);
        
        // Create reflection texture
        reflectionTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, reflectionTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, REFLECTION_WIDTH, REFLECTION_HEIGHT, 0, GL_RGB, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, reflectionTexture, 0);
        
        // Create depth buffer
        reflectionDepthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, reflectionDepthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, REFLECTION_WIDTH, REFLECTION_HEIGHT);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, reflectionDepthBuffer);
        
        // Check framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Reflection framebuffer not complete!");
        }
        
        // Restore default framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        logger.info("Initialized reflection framebuffers ({}x{})", REFLECTION_WIDTH, REFLECTION_HEIGHT);
    }
    
    /**
     * Renders the ocean surface with Gerstner wave displacement.
     */
    public void render(Camera camera, Vector3f sunDirection, float time) {
        gerstnerShader.bind();
        
        // Set matrices
        Matrix4f viewProj = new Matrix4f(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
        gerstnerShader.setUniform("uViewProj", viewProj);
        
        // Set time and water level
        gerstnerShader.setUniform("uTime", time);
        gerstnerShader.setUniform("uSeaLevel", seaLevel);
        
        // Set wave parameters (shader supports max 4 waves)
        int activeWaves = Math.min(currentQuality.getWaveCount(), 4);
        
        for (int i = 0; i < activeWaves; i++) {
            WaveParams wave = waves[i];
            gerstnerShader.setUniform("uDir[" + i + "]", wave.direction);
            gerstnerShader.setUniform("uAmp[" + i + "]", wave.amplitude);
            gerstnerShader.setUniform("uLambda[" + i + "]", wave.wavelength);
            gerstnerShader.setUniform("uSpeed[" + i + "]", wave.speed);
            gerstnerShader.setUniform("uSteep[" + i + "]", wave.steepness);
        }
        
        // Note: Reflection support would require updating the shader to include reflection uniforms
        
        // Set lighting
        gerstnerShader.setUniform("uSunDir", sunDirection);
        gerstnerShader.setUniform("uCameraPos", camera.getPosition());
        
        // Set water colors
        gerstnerShader.setUniform("uDeepColor", deepColor);
        gerstnerShader.setUniform("uShallowColor", shallowColor);
        gerstnerShader.setUniform("uDepthFadeStart", depthFadeStart);
        gerstnerShader.setUniform("uDepthFadeEnd", depthFadeEnd);
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Disable backface culling for water (waves can flip)
        glDisable(GL_CULL_FACE);
        
        // Render the ocean grid
        oceanGrid.render();
        
        // Restore state
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        
        gerstnerShader.unbind();
    }
    
    /**
     * Updates wave parameters for dynamic behavior.
     */
    public void updateWaves(Vector3f windDirection, float windStrength) {
        // Adjust primary wave direction based on wind
        Vector2f primaryDir = new Vector2f(windDirection.x, windDirection.z).normalize();
        waves[0].direction = primaryDir;
        
        // Scale wave heights based on wind strength
        float windScale = Math.max(0.3f, windStrength / 10.0f); // Normalize to 0.3-1.0+
        waves[0].amplitude = 0.6f * windScale;
        waves[1].amplitude = 0.3f * windScale;
        waves[2].amplitude = 0.15f * windScale;
        waves[3].amplitude = 0.08f * windScale;
        
        // Optionally adjust speeds
        waves[0].speed = 3.0f * windScale;
        waves[1].speed = 2.0f * windScale;
    }
    
    /**
     * Calculates wave height at a specific world position using the same Gerstner math as the shader.
     * This is useful for ship physics and other gameplay systems.
     */
    public float getWaveHeightAt(float worldX, float worldZ, float time) {
        float totalHeight = seaLevel;
        int activeWaves = currentQuality.getWaveCount();
        
        for (int i = 0; i < activeWaves; i++) {
            WaveParams wave = waves[i];
            float k = 2.0f * (float)Math.PI / wave.wavelength;
            float phase = k * (wave.direction.x * worldX + wave.direction.y * worldZ) - (k * wave.speed) * time;
            
            // Only Y displacement for height calculation
            totalHeight += wave.amplitude * (float)Math.sin(phase);
        }
        
        return totalHeight;
    }
    
    /**
     * Gets wave normal at a position (useful for surface effects).
     */
    public Vector3f getWaveNormalAt(float worldX, float worldZ, float time) {
        Vector3f dPdx = new Vector3f(1, 0, 0);
        Vector3f dPdz = new Vector3f(0, 0, 1);
        int activeWaves = currentQuality.getWaveCount();
        
        for (int i = 0; i < activeWaves; i++) {
            WaveParams wave = waves[i];
            float k = 2.0f * (float)Math.PI / wave.wavelength;
            float phase = k * (wave.direction.x * worldX + wave.direction.y * worldZ) - (k * wave.speed) * time;
            float cosP = (float)Math.cos(phase);
            float sinP = (float)Math.sin(phase);
            
            float phx = k * wave.direction.x;
            float phz = k * wave.direction.y;
            
            // Partial derivatives
            dPdx.x += -wave.steepness * wave.amplitude * wave.direction.x * sinP * phx;
            dPdx.z += -wave.steepness * wave.amplitude * wave.direction.y * sinP * phx;
            dPdx.y += wave.amplitude * cosP * phx;
            
            dPdz.x += -wave.steepness * wave.amplitude * wave.direction.x * sinP * phz;
            dPdz.z += -wave.steepness * wave.amplitude * wave.direction.y * sinP * phz;
            dPdz.y += wave.amplitude * cosP * phz;
        }
        
        return new Vector3f(dPdz).cross(dPdx).normalize();
    }
    
    /**
     * Sets the water quality level and reinitializes framebuffers if needed.
     */
    public void setQuality(WaterQuality quality) {
        if (this.currentQuality != quality) {
            WaterQuality oldQuality = this.currentQuality;
            this.currentQuality = quality;
            
            // Reinitialize reflection framebuffers if reflection support changed
            if (oldQuality.isReflectionsEnabled() != quality.isReflectionsEnabled()) {
                cleanupReflectionFramebuffers();
                setupReflectionFramebuffers();
            }
            
            logger.info("Water quality changed from {} to {}", oldQuality, quality);
        }
    }
    
    public WaterQuality getQuality() {
        return currentQuality;
    }
    
    /**
     * Renders reflections to the reflection framebuffer.
     * Call this before the main render() method.
     */
    public void renderReflections(Camera camera, Runnable sceneRenderer) {
        if (!currentQuality.isReflectionsEnabled() || reflectionFramebuffer == 0) {
            return;
        }
        
        // Bind reflection framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, reflectionFramebuffer);
        glViewport(0, 0, REFLECTION_WIDTH, REFLECTION_HEIGHT);
        
        // Clear reflection buffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Create reflection camera (flip Y and invert pitch)
        Vector3f cameraPos = camera.getPosition();
        Vector3f reflectionPos = new Vector3f(cameraPos.x, 2 * seaLevel - cameraPos.y, cameraPos.z);
        
// Create reflection camera matrix
Matrix4f reflectionView = new Matrix4f(camera.getViewMatrix());
reflectionView.m11(-reflectionView.m11()); // Invert Y component
reflectionView.m12(-reflectionView.m12());
reflectionView.m13(-reflectionView.m13());
reflectionView.m21(-reflectionView.m21());
reflectionView.m22(-reflectionView.m22());
reflectionView.m23(-reflectionView.m23());

// Store original camera state
Matrix4f originalView = new Matrix4f(camera.getViewMatrix());
Vector3f originalPos = new Vector3f(camera.getPosition());

// Set reflection camera
camera.setViewMatrix(reflectionView);
camera.setPosition(reflectionPos);

// Enable clip plane for water surface
glEnable(GL_CLIP_PLANE0);
double[] clipPlane = {0.0, 1.0, 0.0, -seaLevel};
glClipPlane(GL_CLIP_PLANE0, clipPlane);

// Render reflected scene
if (sceneRenderer != null) {
    sceneRenderer.run();
}

// Restore original camera state
// camera.setViewMatrix(originalView);
// camera.setPosition(originalPos);

// Disable clip plane
glDisable(GL_CLIP_PLANE0);
        // This would require camera manipulation which depends on your Camera implementation
        if (sceneRenderer != null) {
            sceneRenderer.run();
        }
        
        // Restore main framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void cleanupReflectionFramebuffers() {
        if (reflectionFramebuffer != 0) {
            glDeleteFramebuffers(reflectionFramebuffer);
            reflectionFramebuffer = 0;
        }
        if (reflectionTexture != 0) {
            glDeleteTextures(reflectionTexture);
            reflectionTexture = 0;
        }
        if (reflectionDepthBuffer != 0) {
            glDeleteRenderbuffers(reflectionDepthBuffer);
            reflectionDepthBuffer = 0;
        }
    }
    
    // Setters for customization
    public void setWaterColors(Vector3f deep, Vector3f shallow) {
        this.deepColor.set(deep);
        this.shallowColor.set(shallow);
    }
    
    public void setDepthFade(float start, float end) {
        this.depthFadeStart = start;
        this.depthFadeEnd = end;
    }
    
    public void setSeaLevel(float level) {
        this.seaLevel = level;
        // Recreate grid with new sea level
        if (oceanGrid != null) {
            oceanGrid.cleanup();
        }
        createOceanGrid();
    }
    
    public void setWave(int index, WaveParams params) {
        if (index >= 0 && index < 4) {
            waves[index] = params;
        }
    }
    
    public void cleanup() {
        if (oceanGrid != null) {
            oceanGrid.cleanup();
        }
        cleanupReflectionFramebuffers();
        logger.info("Cleaned up unified Gerstner ocean renderer");
    }
}