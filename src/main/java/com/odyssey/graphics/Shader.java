package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Shader program wrapper for The Odyssey.
 * Handles shader compilation, linking, and uniform management.
 */
public class Shader {
    private static final Logger logger = LoggerFactory.getLogger(Shader.class);
    
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    private Map<String, Integer> uniformLocations;
    
    public Shader() {
        this.uniformLocations = new HashMap<>();
    }
    
    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }
    
    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }
    
    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }
        
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }
        
        return shaderId;
    }
    
    public void link() throws Exception {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Error creating Shader program");
        }
        
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
        
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            String validationLog = glGetProgramInfoLog(programId, 1024);
            logger.warn("Shader validation warning: {}", validationLog);
            
            // Check if this is a critical validation error that should fail linking
            if (validationLog.toLowerCase().contains("error") || 
                validationLog.toLowerCase().contains("failed") ||
                validationLog.toLowerCase().contains("invalid")) {
                throw new Exception("Critical shader validation error: " + validationLog);
            }
        }
        
        // Cache all uniform locations at link time for better performance
        cacheUniformLocations();
    }
    
    public void bind() {
        glUseProgram(programId);
    }
    
    public void unbind() {
        glUseProgram(0);
    }
    
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        // Delete individual shaders to prevent memory leak
        if (vertexShaderId != 0) {
            glDeleteShader(vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDeleteShader(fragmentShaderId);
        }
    }
    
    /**
     * Caches all uniform locations at link time for better performance.
     */
    private void cacheUniformLocations() {
        int uniformCount = glGetProgrami(programId, GL_ACTIVE_UNIFORMS);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < uniformCount; i++) {
                String uniformName = glGetActiveUniform(programId, i, stack.mallocInt(1), stack.mallocInt(1));
                if (uniformName != null && !uniformName.isEmpty()) {
                    // Remove array notation if present (e.g., "lights[0]" becomes "lights")
                    if (uniformName.contains("[")) {
                        uniformName = uniformName.substring(0, uniformName.indexOf('['));
                    }
                    
                    int location = glGetUniformLocation(programId, uniformName);
                    if (location >= 0) {
                        uniformLocations.put(uniformName, location);
                    }
                }
            }
        }
        
        logger.debug("Cached {} uniform locations for shader program {}", uniformLocations.size(), programId);
    }
    
    private int getUniformLocation(String uniformName) {
        // Check cache first to avoid GPU stalls during rendering
        Integer cachedLocation = uniformLocations.get(uniformName);
        if (cachedLocation != null) {
            return cachedLocation;
        }
        
        // Only perform expensive lookup if not cached
        int location = glGetUniformLocation(programId, uniformName);
        if (location < 0) {
            logger.warn("Could not find uniform: {} in shader program {}", uniformName, programId);
            // Cache negative results to avoid repeated lookups
            uniformLocations.put(uniformName, -1);
            return -1;
        }
        
        uniformLocations.put(uniformName, location);
        return location;
    }
    
    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            value.get(buffer);
            glUniformMatrix4fv(getUniformLocation(uniformName), false, buffer);
        }
    }
    
    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(getUniformLocation(uniformName), value.x, value.y, value.z, value.w);
    }
    
    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(getUniformLocation(uniformName), value.x, value.y, value.z);
    }
    
    public void setUniform(String uniformName, Vector2f value) {
        glUniform2f(getUniformLocation(uniformName), value.x, value.y);
    }
    
    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniformLocation(uniformName), value);
    }
    
    public void setUniform(String uniformName, int value) {
        glUniform1i(getUniformLocation(uniformName), value);
    }
    
    public void setUniform(String uniformName, boolean value) {
        glUniform1i(getUniformLocation(uniformName), value ? 1 : 0);
    }
    
    public int getProgramId() {
        return programId;
    }
}