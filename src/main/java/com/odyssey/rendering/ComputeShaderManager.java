package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Manages compute shaders for GPU-accelerated post-processing effects
 * Provides optimized implementations for bloom, tone mapping, and other effects
 */
public class ComputeShaderManager {
    private static final Logger logger = new Logger(ComputeShaderManager.class);
    
    private Map<String, Integer> computePrograms;
    private Map<String, Integer> computeShaders;
    private boolean initialized;
    
    // Compute shader work group sizes
    private static final int WORK_GROUP_SIZE_X = 16;
    private static final int WORK_GROUP_SIZE_Y = 16;
    private static final int WORK_GROUP_SIZE_Z = 1;
    
    /**
     * Creates a new compute shader manager
     */
    public ComputeShaderManager() {
        this.computePrograms = new HashMap<>();
        this.computeShaders = new HashMap<>();
        this.initialized = false;
    }
    
    /**
     * Initializes the compute shader manager and loads default shaders
     */
    public void initialize() {
        if (initialized) {
            logger.warn("ComputeShaderManager already initialized");
            return;
        }
        
        try {
            // Load compute shaders for post-processing effects
            loadComputeShader("bloom_downsample", "shaders/compute/bloom_downsample.comp");
            loadComputeShader("bloom_upsample", "shaders/compute/bloom_upsample.comp");
            loadComputeShader("tone_mapping", "shaders/compute/tone_mapping.comp");
            loadComputeShader("gaussian_blur_horizontal", "shaders/compute/gaussian_blur_h.comp");
            loadComputeShader("gaussian_blur_vertical", "shaders/compute/gaussian_blur_v.comp");
            loadComputeShader("luminance_histogram", "shaders/compute/luminance_histogram.comp");
            loadComputeShader("exposure_adaptation", "shaders/compute/exposure_adaptation.comp");
            
            initialized = true;
            logger.info("ComputeShaderManager initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize ComputeShaderManager: " + e.getMessage());
            throw new RuntimeException("ComputeShaderManager initialization failed", e);
        }
    }
    
    /**
     * Loads a compute shader from file
     * @param name The name to associate with the shader
     * @param computePath The path to the compute shader file
     */
    public void loadComputeShader(String name, String computePath) {
        try {
            // Load compute shader source
            String computeSource = loadShaderSource(computePath);
            
            // Create and compile compute shader
            int computeShader = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(computeShader, computeSource);
            glCompileShader(computeShader);
            
            // Check compilation status
            if (glGetShaderi(computeShader, GL_COMPILE_STATUS) == GL_FALSE) {
                String error = glGetShaderInfoLog(computeShader);
                glDeleteShader(computeShader);
                throw new RuntimeException("Compute shader compilation failed (" + computePath + "): " + error);
            }
            
            // Create program and attach shader
            int program = glCreateProgram();
            glAttachShader(program, computeShader);
            glLinkProgram(program);
            
            // Check linking status
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                String error = glGetProgramInfoLog(program);
                glDeleteProgram(program);
                glDeleteShader(computeShader);
                throw new RuntimeException("Compute shader linking failed (" + name + "): " + error);
            }
            
            // Store shader and program
            computeShaders.put(name, computeShader);
            computePrograms.put(name, program);
            
            logger.info("Loaded compute shader: " + name);
            
        } catch (IOException e) {
            logger.error("Failed to load compute shader " + name + ": " + e.getMessage());
            throw new RuntimeException("Failed to load compute shader: " + name, e);
        }
    }
    
    /**
     * Dispatches a compute shader with the specified work group dimensions
     * @param shaderName The name of the compute shader to dispatch
     * @param numGroupsX Number of work groups in X dimension
     * @param numGroupsY Number of work groups in Y dimension
     * @param numGroupsZ Number of work groups in Z dimension
     */
    public void dispatch(String shaderName, int numGroupsX, int numGroupsY, int numGroupsZ) {
        Integer program = computePrograms.get(shaderName);
        if (program == null) {
            logger.error("Compute shader not found: " + shaderName);
            return;
        }
        
        glUseProgram(program);
        glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }
    
    /**
     * Dispatches a compute shader for a 2D texture with automatic work group calculation
     * @param shaderName The name of the compute shader to dispatch
     * @param textureWidth Width of the target texture
     * @param textureHeight Height of the target texture
     */
    public void dispatch2D(String shaderName, int textureWidth, int textureHeight) {
        int numGroupsX = (textureWidth + WORK_GROUP_SIZE_X - 1) / WORK_GROUP_SIZE_X;
        int numGroupsY = (textureHeight + WORK_GROUP_SIZE_Y - 1) / WORK_GROUP_SIZE_Y;
        dispatch(shaderName, numGroupsX, numGroupsY, 1);
    }
    
    /**
     * Binds a texture as an image for compute shader access
     * @param unit The image unit to bind to
     * @param texture The texture ID
     * @param level Mipmap level
     * @param layered Whether the texture is layered
     * @param layer Layer index (if layered)
     * @param access Access mode (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @param format Internal format of the texture
     */
    public void bindImage(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }
    
    /**
     * Sets a uniform value for the currently bound compute shader
     * @param shaderName The name of the compute shader
     * @param uniformName The name of the uniform
     * @param value The value to set
     */
    public void setUniform(String shaderName, String uniformName, float value) {
        Integer program = computePrograms.get(shaderName);
        if (program == null) {
            logger.error("Compute shader not found: " + shaderName);
            return;
        }
        
        glUseProgram(program);
        int location = glGetUniformLocation(program, uniformName);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }
    
    /**
     * Sets a uniform value for the currently bound compute shader
     * @param shaderName The name of the compute shader
     * @param uniformName The name of the uniform
     * @param value The value to set
     */
    public void setUniform(String shaderName, String uniformName, int value) {
        Integer program = computePrograms.get(shaderName);
        if (program == null) {
            logger.error("Compute shader not found: " + shaderName);
            return;
        }
        
        glUseProgram(program);
        int location = glGetUniformLocation(program, uniformName);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }
    
    /**
     * Sets a uniform vector value for the currently bound compute shader
     * @param shaderName The name of the compute shader
     * @param uniformName The name of the uniform
     * @param x X component
     * @param y Y component
     */
    public void setUniform2f(String shaderName, String uniformName, float x, float y) {
        Integer program = computePrograms.get(shaderName);
        if (program == null) {
            logger.error("Compute shader not found: " + shaderName);
            return;
        }
        
        glUseProgram(program);
        int location = glGetUniformLocation(program, uniformName);
        if (location != -1) {
            glUniform2f(location, x, y);
        }
    }
    
    /**
     * Gets the program ID for a compute shader
     * @param shaderName The name of the compute shader
     * @return The OpenGL program ID, or null if not found
     */
    public Integer getProgram(String shaderName) {
        return computePrograms.get(shaderName);
    }
    
    /**
     * Checks if a compute shader is loaded
     * @param shaderName The name of the compute shader
     * @return True if the shader is loaded
     */
    public boolean hasShader(String shaderName) {
        return computePrograms.containsKey(shaderName);
    }
    
    /**
     * Loads shader source code from a file
     * @param path The path to the shader file
     * @return The shader source code
     * @throws IOException If the file cannot be read
     */
    private String loadShaderSource(String path) throws IOException {
        try {
            return new String(Files.readAllBytes(Paths.get("src/main/resources/" + path)));
        } catch (IOException e) {
            // Try loading from classpath as fallback
            try (var inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
                if (inputStream == null) {
                    throw new IOException("Shader file not found: " + path);
                }
                return new String(inputStream.readAllBytes());
            }
        }
    }
    
    /**
     * Waits for all compute shader operations to complete
     */
    public void memoryBarrier() {
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }
    
    /**
     * Waits for specific compute shader operations to complete
     * @param barriers Bitfield specifying which barriers to wait for
     */
    public void memoryBarrier(int barriers) {
        glMemoryBarrier(barriers);
    }
    
    /**
     * Cleans up all compute shader resources
     */
    public void cleanup() {
        if (!initialized) {
            return;
        }
        
        // Delete all programs
        for (Integer program : computePrograms.values()) {
            if (program != null) {
                glDeleteProgram(program);
            }
        }
        
        // Delete all shaders
        for (Integer shader : computeShaders.values()) {
            if (shader != null) {
                glDeleteShader(shader);
            }
        }
        
        computePrograms.clear();
        computeShaders.clear();
        initialized = false;
        
        logger.info("ComputeShaderManager cleanup complete");
    }
    
    /**
     * Gets the recommended work group size for X dimension
     * @return Work group size X
     */
    public static int getWorkGroupSizeX() {
        return WORK_GROUP_SIZE_X;
    }
    
    /**
     * Gets the recommended work group size for Y dimension
     * @return Work group size Y
     */
    public static int getWorkGroupSizeY() {
        return WORK_GROUP_SIZE_Y;
    }
    
    /**
     * Gets the recommended work group size for Z dimension
     * @return Work group size Z
     */
    public static int getWorkGroupSizeZ() {
        return WORK_GROUP_SIZE_Z;
    }
}