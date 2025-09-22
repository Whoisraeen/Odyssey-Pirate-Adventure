package com.odyssey.rendering;

import com.odyssey.util.Logger;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenGL shader program wrapper for The Odyssey.
 * 
 * Handles shader compilation, linking, uniform management, and provides
 * a convenient interface for setting shader parameters.
 */
public class Shader {
    
    private static final Logger logger = Logger.getLogger(Shader.class);
    
    // Shader program ID
    private int programId;
    private String name;
    
    // Shader stage IDs
    private int vertexShaderId;
    private int fragmentShaderId;
    private int geometryShaderId;
    
    // Uniform location cache
    private Map<String, Integer> uniformLocations;
    
    // Shader state
    private boolean compiled = false;
    private boolean bound = false;
    
    /**
     * Create a new shader with a name.
     */
    public Shader(String name) {
        this.name = name;
        this.uniformLocations = new HashMap<>();
        this.programId = GL20.glCreateProgram();
        
        if (programId == 0) {
            throw new RuntimeException("Failed to create shader program");
        }
        
        logger.debug("Created shader program '{}' with ID: {}", name, programId);
    }
    
    /**
     * Create and compile a shader from vertex and fragment source code.
     */
    public static Shader create(String name, String vertexSource, String fragmentSource) {
        Shader shader = new Shader(name);
        
        try {
            shader.compileVertexShader(vertexSource);
            shader.compileFragmentShader(fragmentSource);
            shader.link();
            return shader;
        } catch (Exception e) {
            shader.cleanup();
            throw e;
        }
    }
    
    /**
     * Create and compile a shader with geometry shader.
     */
    public static Shader create(String name, String vertexSource, String geometrySource, String fragmentSource) {
        Shader shader = new Shader(name);
        
        try {
            shader.compileVertexShader(vertexSource);
            shader.compileGeometryShader(geometrySource);
            shader.compileFragmentShader(fragmentSource);
            shader.link();
            return shader;
        } catch (Exception e) {
            shader.cleanup();
            throw e;
        }
    }
    
    /**
     * Compile vertex shader from source code.
     */
    public void compileVertexShader(String source) {
        vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, source, "vertex");
        GL20.glAttachShader(programId, vertexShaderId);
    }
    
    /**
     * Compile fragment shader from source code.
     */
    public void compileFragmentShader(String source) {
        fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, source, "fragment");
        GL20.glAttachShader(programId, fragmentShaderId);
    }
    
    /**
     * Compile geometry shader from source code.
     */
    public void compileGeometryShader(String source) {
        geometryShaderId = compileShader(GL32.GL_GEOMETRY_SHADER, source, "geometry");
        GL20.glAttachShader(programId, geometryShaderId);
    }
    
    /**
     * Compile a shader of the specified type.
     */
    private int compileShader(int type, String source, String typeName) {
        int shaderId = GL20.glCreateShader(type);
        
        if (shaderId == 0) {
            throw new RuntimeException("Failed to create " + typeName + " shader");
        }
        
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        
        // Check compilation status
        int status = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (status == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId);
            throw new RuntimeException("Failed to compile " + typeName + " shader '" + name + "':\n" + log);
        }
        
        logger.debug("Compiled {} shader for '{}'", typeName, name);
        return shaderId;
    }
    
    /**
     * Link the shader program.
     */
    public void link() {
        GL20.glLinkProgram(programId);
        
        // Check linking status
        int status = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (status == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId);
            throw new RuntimeException("Failed to link shader program '" + name + "':\n" + log);
        }
        
        // Validate program
        GL20.glValidateProgram(programId);
        status = GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS);
        if (status == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId);
            logger.warn("Shader program '{}' validation warning:\n{}", name, log);
        }
        
        // Detach and delete shader objects (they're no longer needed)
        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId);
            GL20.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
        }
        
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId);
            GL20.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        
        if (geometryShaderId != 0) {
            GL20.glDetachShader(programId, geometryShaderId);
            GL20.glDeleteShader(geometryShaderId);
            geometryShaderId = 0;
        }
        
        compiled = true;
        logger.info("Successfully linked shader program '{}'", name);
    }
    
    /**
     * Bind the shader for use.
     */
    public void bind() {
        if (!compiled) {
            throw new IllegalStateException("Shader '" + name + "' is not compiled");
        }
        
        GL20.glUseProgram(programId);
        bound = true;
    }
    
    /**
     * Unbind the shader.
     */
    public void unbind() {
        GL20.glUseProgram(0);
        bound = false;
    }
    
    /**
     * Get uniform location (with caching).
     */
    private int getUniformLocation(String name) {
        Integer location = uniformLocations.get(name);
        if (location == null) {
            location = GL20.glGetUniformLocation(programId, name);
            uniformLocations.put(name, location);
            
            if (location == -1) {
                logger.debug("Uniform '{}' not found in shader '{}'", name, this.name);
            }
        }
        return location;
    }
    
    /**
     * Set integer uniform.
     */
    public void setUniform(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }
    
    /**
     * Set float uniform.
     */
    public void setUniform(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }
    
    /**
     * Set boolean uniform.
     */
    public void setUniform(String name, boolean value) {
        setUniform(name, value ? 1 : 0);
    }
    
    /**
     * Set Vector2f uniform.
     */
    public void setUniform(String name, Vector2f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform2f(location, value.x, value.y);
        }
    }
    
    /**
     * Set Vector3f uniform.
     */
    public void setUniform(String name, Vector3f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform3f(location, value.x, value.y, value.z);
        }
    }
    
    /**
     * Set Vector4f uniform.
     */
    public void setUniform(String name, Vector4f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform4f(location, value.x, value.y, value.z, value.w);
        }
    }
    
    /**
     * Set Matrix3f uniform.
     */
    public void setUniform(String name, Matrix3f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(9);
                matrix.get(buffer);
                GL20.glUniformMatrix3fv(location, false, buffer);
            }
        }
    }
    
    /**
     * Set Matrix4f uniform.
     */
    public void setUniform(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                matrix.get(buffer);
                GL20.glUniformMatrix4fv(location, false, buffer);
            }
        }
    }
    
    /**
     * Set float array uniform.
     */
    public void setUniform(String name, float[] values) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1fv(location, values);
        }
    }
    
    /**
     * Set integer array uniform.
     */
    public void setUniform(String name, int[] values) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1iv(location, values);
        }
    }
    
    /**
     * Set texture uniform (binds texture to specified unit).
     */
    public void setTexture(String name, int textureUnit) {
        setUniform(name, textureUnit);
    }
    
    /**
     * Set multiple Vector3f uniforms (for arrays).
     */
    public void setUniform(String name, Vector3f[] vectors) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(vectors.length * 3);
                for (Vector3f vector : vectors) {
                    buffer.put(vector.x).put(vector.y).put(vector.z);
                }
                buffer.flip();
                GL20.glUniform3fv(location, buffer);
            }
        }
    }
    
    /**
     * Set multiple Matrix4f uniforms (for arrays).
     */
    public void setUniform(String name, Matrix4f[] matrices) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(matrices.length * 16);
                for (Matrix4f matrix : matrices) {
                    matrix.get(buffer);
                }
                buffer.flip();
                GL20.glUniformMatrix4fv(location, false, buffer);
            }
        }
    }
    
    /**
     * Check if uniform exists in shader.
     */
    public boolean hasUniform(String name) {
        return getUniformLocation(name) != -1;
    }
    
    /**
     * Get shader program ID.
     */
    public int getProgramId() {
        return programId;
    }
    
    /**
     * Get shader name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Check if shader is compiled.
     */
    public boolean isCompiled() {
        return compiled;
    }
    
    /**
     * Check if shader is currently bound.
     */
    public boolean isBound() {
        return bound;
    }
    
    /**
     * Get all uniform names and their locations.
     */
    public Map<String, Integer> getUniforms() {
        return new HashMap<>(uniformLocations);
    }
    
    /**
     * Clear uniform location cache.
     */
    public void clearUniformCache() {
        uniformLocations.clear();
    }
    
    /**
     * Reload shader (useful for hot-reloading during development).
     */
    public void reload(String vertexSource, String fragmentSource) {
        logger.info("Reloading shader '{}'", name);
        
        // Store old program ID
        int oldProgramId = programId;
        boolean wasBound = bound;
        
        try {
            // Create new program
            programId = GL20.glCreateProgram();
            if (programId == 0) {
                throw new RuntimeException("Failed to create new shader program");
            }
            
            // Reset state
            compiled = false;
            bound = false;
            uniformLocations.clear();
            
            // Compile and link new shader
            compileVertexShader(vertexSource);
            compileFragmentShader(fragmentSource);
            link();
            
            // Delete old program
            GL20.glDeleteProgram(oldProgramId);
            
            // Restore binding state
            if (wasBound) {
                bind();
            }
            
            logger.info("Successfully reloaded shader '{}'", name);
            
        } catch (Exception e) {
            // Restore old program on failure
            GL20.glDeleteProgram(programId);
            programId = oldProgramId;
            compiled = true;
            
            if (wasBound) {
                bind();
            }
            
            logger.error("Failed to reload shader '{}': {}", name, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Cleanup shader resources.
     */
    public void cleanup() {
        if (bound) {
            unbind();
        }
        
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        
        // Clean up individual shaders if they weren't deleted during linking
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
        }
        
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        
        if (geometryShaderId != 0) {
            GL20.glDeleteShader(geometryShaderId);
            geometryShaderId = 0;
        }
        
        uniformLocations.clear();
        compiled = false;
        
        logger.debug("Cleaned up shader '{}'", name);
    }
    
    @Override
    public String toString() {
        return String.format("Shader{name='%s', programId=%d, compiled=%s, bound=%s}", 
                           name, programId, compiled, bound);
    }
}