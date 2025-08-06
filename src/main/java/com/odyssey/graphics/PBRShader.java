package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PBR (Physically Based Rendering) shader implementation for The Odyssey.
 * Supports metallic-roughness workflow with multiple render targets for deferred rendering.
 * Handles albedo, normal, metallic, roughness, and emission textures.
 */
public class PBRShader {
    private static final Logger logger = LoggerFactory.getLogger(PBRShader.class);
    
    private Shader shader;
    private boolean initialized = false;
    
    // Texture unit assignments
    private static final int ALBEDO_TEXTURE_UNIT = 0;
    private static final int NORMAL_TEXTURE_UNIT = 1;
    private static final int METALLIC_TEXTURE_UNIT = 2;
    private static final int ROUGHNESS_TEXTURE_UNIT = 3;
    private static final int AO_TEXTURE_UNIT = 4;
    private static final int EMISSION_TEXTURE_UNIT = 5;
    private static final int IRRADIANCE_TEXTURE_UNIT = 6;
    private static final int PREFILTER_TEXTURE_UNIT = 7;
    private static final int BRDF_LUT_TEXTURE_UNIT = 8;
    
    /**
     * Initializes the PBR shader with vertex and fragment shader sources.
     */
    public void initialize() throws Exception {
        if (initialized) {
            logger.warn("PBR shader already initialized");
            return;
        }
        
        String vertexShaderSource = createVertexShaderSource();
        String fragmentShaderSource = createFragmentShaderSource();
        
        shader = new Shader();
        shader.createVertexShader(vertexShaderSource);
        shader.createFragmentShader(fragmentShaderSource);
        shader.link();
        
        // Set texture unit uniforms
        shader.bind();
        setTextureUniforms();
        shader.unbind();
        
        initialized = true;
        logger.info("PBR shader initialized successfully");
    }
    
    /**
     * Sets up texture unit uniforms for PBR textures.
     */
    private void setTextureUniforms() {
        shader.setUniform("u_albedoTexture", ALBEDO_TEXTURE_UNIT);
        shader.setUniform("u_normalTexture", NORMAL_TEXTURE_UNIT);
        shader.setUniform("u_metallicTexture", METALLIC_TEXTURE_UNIT);
        shader.setUniform("u_roughnessTexture", ROUGHNESS_TEXTURE_UNIT);
        shader.setUniform("u_aoTexture", AO_TEXTURE_UNIT);
        shader.setUniform("u_emissionTexture", EMISSION_TEXTURE_UNIT);
        shader.setUniform("u_irradianceMap", IRRADIANCE_TEXTURE_UNIT);
        shader.setUniform("u_prefilterMap", PREFILTER_TEXTURE_UNIT);
        shader.setUniform("u_brdfLUT", BRDF_LUT_TEXTURE_UNIT);
    }
    
    /**
     * Binds the PBR shader and sets up rendering state.
     */
    public void bind() {
        if (!initialized) {
            throw new IllegalStateException("PBR shader not initialized");
        }
        shader.bind();
    }
    
    /**
     * Unbinds the PBR shader.
     */
    public void unbind() {
        shader.unbind();
    }
    
    /**
     * Sets transformation matrices for the shader.
     */
    public void setMatrices(Matrix4f modelMatrix, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        shader.setUniform("u_modelMatrix", modelMatrix);
        shader.setUniform("u_viewMatrix", viewMatrix);
        shader.setUniform("u_projectionMatrix", projectionMatrix);
        
        // Calculate and set normal matrix
        Matrix4f normalMatrix = new Matrix4f(modelMatrix).invert().transpose();
        shader.setUniform("u_normalMatrix", normalMatrix);
    }
    
    /**
     * Sets camera position for view-dependent calculations.
     */
    public void setCameraPosition(Vector3f cameraPosition) {
        shader.setUniform("u_cameraPosition", cameraPosition);
    }
    
    /**
     * Sets PBR material properties.
     */
    public void setMaterialProperties(TextureAtlasManager.PBRMaterial material) {
        // Set texture flags based on available textures
        shader.setUniform("u_hasAlbedoTexture", material.getAlbedo() != null);
        shader.setUniform("u_hasNormalTexture", material.getNormal() != null);
        shader.setUniform("u_hasMetallicTexture", material.getMetallic() != null);
        shader.setUniform("u_hasRoughnessTexture", material.getRoughness() != null);
        shader.setUniform("u_hasAOTexture", material.getAO() != null);
        shader.setUniform("u_hasEmissionTexture", material.getEmission() != null);
        
        // Set default material factors (these could be made configurable)
        shader.setUniform("u_albedoFactor", new Vector3f(1.0f, 1.0f, 1.0f));
        shader.setUniform("u_metallicFactor", 1.0f);
        shader.setUniform("u_roughnessFactor", 1.0f);
        shader.setUniform("u_emissionFactor", new Vector3f(0.0f, 0.0f, 0.0f));
        shader.setUniform("u_normalScale", 1.0f);
        shader.setUniform("u_occlusionStrength", 1.0f);
    }
    
    /**
     * Sets material factors directly without requiring a PBRMaterial object.
     * Useful for simple materials or temporary rendering.
     */
    public void setMaterialFactors(Vector3f albedo, float metallic, float roughness, Vector3f emission, float ao) {
        // Set texture flags to false since we're using factors only
        shader.setUniform("u_hasAlbedoTexture", false);
        shader.setUniform("u_hasNormalTexture", false);
        shader.setUniform("u_hasMetallicTexture", false);
        shader.setUniform("u_hasRoughnessTexture", false);
        shader.setUniform("u_hasAOTexture", false);
        shader.setUniform("u_hasEmissionTexture", false);
        
        // Set material factors
        shader.setUniform("u_albedoFactor", albedo);
        shader.setUniform("u_metallicFactor", metallic);
        shader.setUniform("u_roughnessFactor", roughness);
        shader.setUniform("u_emissionFactor", emission);
        shader.setUniform("u_normalScale", 1.0f);
        shader.setUniform("u_occlusionStrength", ao);
    }
    

    
    /**
     * Binds material textures for PBR rendering.
     * 
     * @param material The PBR material to bind
     */
    public void bindMaterialTextures(TextureAtlasManager.PBRMaterial material) {
        // Bind albedo texture
        if (material.getAlbedo() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + ALBEDO_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getAlbedo().getAtlas().getTextureId());
        }
        
        // Bind normal map
        if (material.getNormal() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + NORMAL_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getNormal().getAtlas().getTextureId());
        }
        
        // Bind metallic map
        if (material.getMetallic() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + METALLIC_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getMetallic().getAtlas().getTextureId());
        }
        
        // Bind roughness map
        if (material.getRoughness() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + ROUGHNESS_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getRoughness().getAtlas().getTextureId());
        }
        
        // Bind AO map
        if (material.getAO() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + AO_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getAO().getAtlas().getTextureId());
        }
        
        // Bind emission map
        if (material.getEmission() != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + EMISSION_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getEmission().getAtlas().getTextureId());
        }
    }
    
    /**
     * Binds IBL (Image-Based Lighting) textures.
     */
    public void bindIBLTextures(int irradianceMap, int prefilterMap, int brdfLUT) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + IRRADIANCE_TEXTURE_UNIT);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, irradianceMap);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + PREFILTER_TEXTURE_UNIT);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, prefilterMap);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + BRDF_LUT_TEXTURE_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, brdfLUT);
    }
    
    /**
     * Sets lighting parameters for PBR calculations.
     */
    public void setLightingParameters(LightingSystem lightingSystem) {
        // Set ambient lighting
        shader.setUniform("u_ambientColor", lightingSystem.getAmbientColor());
        shader.setUniform("u_ambientIntensity", lightingSystem.getAmbientIntensity());
        
        // Set IBL parameters
        shader.setUniform("u_iblEnabled", lightingSystem.isIBLEnabled());
        
        // Set light counts
        shader.setUniform("u_directionalLightCount", lightingSystem.getDirectionalLightCount());
        shader.setUniform("u_pointLightCount", lightingSystem.getPointLightCount());
        shader.setUniform("u_spotLightCount", lightingSystem.getSpotLightCount());
    }
    
    /**
     * Creates the vertex shader source code for PBR rendering.
     */
    private String createVertexShaderSource() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 a_position;
            layout (location = 1) in vec3 a_normal;
            layout (location = 2) in vec2 a_texCoord;
            layout (location = 3) in vec3 a_tangent;
            layout (location = 4) in vec3 a_bitangent;
            
            uniform mat4 u_modelMatrix;
            uniform mat4 u_viewMatrix;
            uniform mat4 u_projectionMatrix;
            uniform mat4 u_normalMatrix;
            
            out vec3 v_worldPos;
            out vec3 v_normal;
            out vec2 v_texCoord;
            out mat3 v_TBN;
            
            void main() {
                vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
                v_worldPos = worldPos.xyz;
                v_texCoord = a_texCoord;
                
                // Transform normal to world space
                v_normal = normalize(mat3(u_normalMatrix) * a_normal);
                
                // Calculate TBN matrix for normal mapping
                vec3 T = normalize(mat3(u_normalMatrix) * a_tangent);
                vec3 B = normalize(mat3(u_normalMatrix) * a_bitangent);
                vec3 N = v_normal;
                v_TBN = mat3(T, B, N);
                
                gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;
            }
            """;
    }
    
    /**
     * Creates the fragment shader source code for PBR rendering.
     */
    private String createFragmentShaderSource() {
        return """
            #version 330 core
            
            // G-Buffer outputs for deferred rendering
            layout (location = 0) out vec4 g_albedo;      // RGB: albedo, A: metallic
            layout (location = 1) out vec4 g_normal;      // RGB: world normal, A: roughness
            layout (location = 2) out vec4 g_position;    // RGB: world position, A: depth
            layout (location = 3) out vec4 g_emission;    // RGB: emission, A: AO
            
            in vec3 v_worldPos;
            in vec3 v_normal;
            in vec2 v_texCoord;
            in mat3 v_TBN;
            
            // Material textures
            uniform sampler2D u_albedoTexture;
            uniform sampler2D u_normalTexture;
            uniform sampler2D u_metallicTexture;
            uniform sampler2D u_roughnessTexture;
            uniform sampler2D u_aoTexture;
            uniform sampler2D u_emissionTexture;
            
            // Material properties
            uniform vec3 u_albedoFactor;
            uniform float u_metallicFactor;
            uniform float u_roughnessFactor;
            uniform vec3 u_emissionFactor;
            uniform float u_normalScale;
            uniform float u_occlusionStrength;
            
            // Texture flags
            uniform bool u_hasAlbedoTexture;
            uniform bool u_hasNormalTexture;
            uniform bool u_hasMetallicTexture;
            uniform bool u_hasRoughnessTexture;
            uniform bool u_hasAOTexture;
            uniform bool u_hasEmissionTexture;
            
            vec3 getNormalFromMap() {
                if (!u_hasNormalTexture) {
                    return normalize(v_normal);
                }
                
                vec3 tangentNormal = texture(u_normalTexture, v_texCoord).xyz * 2.0 - 1.0;
                tangentNormal.xy *= u_normalScale;
                
                return normalize(v_TBN * tangentNormal);
            }
            
            void main() {
                // Sample material properties
                vec3 albedo = u_albedoFactor;
                if (u_hasAlbedoTexture) {
                    albedo *= texture(u_albedoTexture, v_texCoord).rgb;
                }
                
                float metallic = u_metallicFactor;
                if (u_hasMetallicTexture) {
                    metallic *= texture(u_metallicTexture, v_texCoord).r;
                }
                
                float roughness = u_roughnessFactor;
                if (u_hasRoughnessTexture) {
                    roughness *= texture(u_roughnessTexture, v_texCoord).r;
                }
                
                vec3 emission = u_emissionFactor;
                if (u_hasEmissionTexture) {
                    emission *= texture(u_emissionTexture, v_texCoord).rgb;
                }
                
                float ao = 1.0;
                if (u_hasAOTexture) {
                    ao = mix(1.0, texture(u_aoTexture, v_texCoord).r, u_occlusionStrength);
                }
                
                vec3 normal = getNormalFromMap();
                
                // Pack data into G-Buffer
                g_albedo = vec4(albedo, metallic);
                g_normal = vec4(normal * 0.5 + 0.5, roughness);
                g_position = vec4(v_worldPos, gl_FragCoord.z);
                g_emission = vec4(emission, ao);
            }
            """;
    }
    
    /**
     * Cleans up shader resources.
     */
    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        initialized = false;
        logger.info("PBR shader cleaned up");
    }
}