package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
// OpenGL bindings are referenced with fully qualified class names to avoid unused-import warnings
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lighting pass shader for deferred PBR rendering.
 * Reads from G-Buffer and performs PBR lighting calculations including:
 * - Cook-Torrance BRDF
 * - Image-Based Lighting (IBL)
 * - Multiple light types (directional, point, spot)
 * - Shadow mapping
 */
public class LightingPassShader {
    private static final Logger logger = LoggerFactory.getLogger(LightingPassShader.class);
    
    private Shader shader;
    private boolean initialized = false;
    
    // G-Buffer texture units
    private static final int GBUFFER_ALBEDO_UNIT = 0;
    private static final int GBUFFER_NORMAL_UNIT = 1;
    private static final int GBUFFER_DEPTH_UNIT = 2;  // Changed from POSITION to DEPTH
    private static final int GBUFFER_EMISSION_UNIT = 3;
    
    // IBL texture units
    private static final int IRRADIANCE_TEXTURE_UNIT = 4;
    private static final int PREFILTER_TEXTURE_UNIT = 5;
    private static final int BRDF_LUT_TEXTURE_UNIT = 6;
    
    // Shadow map texture units
    private static final int SHADOW_MAP_UNIT = 7;
    
    /**
     * Initializes the lighting pass shader.
     */
    public void initialize() throws Exception {
        if (initialized) {
            logger.warn("Lighting pass shader already initialized");
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
        logger.info("Lighting pass shader initialized successfully");
    }
    
    /**
     * Sets up texture unit uniforms.
     */
    private void setTextureUniforms() {
        shader.setUniform("u_gAlbedo", GBUFFER_ALBEDO_UNIT);
        shader.setUniform("u_gNormal", GBUFFER_NORMAL_UNIT);
        shader.setUniform("u_gDepth", GBUFFER_DEPTH_UNIT);  // Changed from u_gPosition
        shader.setUniform("u_gEmission", GBUFFER_EMISSION_UNIT);
        shader.setUniform("u_irradianceMap", IRRADIANCE_TEXTURE_UNIT);
        shader.setUniform("u_prefilterMap", PREFILTER_TEXTURE_UNIT);
        shader.setUniform("u_brdfLUT", BRDF_LUT_TEXTURE_UNIT);
        shader.setUniform("u_shadowMap", SHADOW_MAP_UNIT);
    }
    
    /**
     * Binds the lighting pass shader.
     */
    public void bind() {
        if (!initialized) {
            throw new IllegalStateException("Lighting pass shader not initialized");
        }
        shader.bind();
    }
    
    /**
     * Unbinds the lighting pass shader.
     */
    public void unbind() {
        shader.unbind();
    }
    
    /**
     * Binds G-Buffer textures for reading.
     */
    public void bindGBufferTextures(int albedoTexture, int normalTexture, int depthTexture, int emissionTexture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + GBUFFER_ALBEDO_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, albedoTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + GBUFFER_NORMAL_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + GBUFFER_DEPTH_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + GBUFFER_EMISSION_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, emissionTexture);
    }
    
    /**
     * Binds IBL textures for image-based lighting.
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
     * Binds shadow map texture.
     */
    public void bindShadowMap(int shadowMapTexture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHADOW_MAP_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexture);
    }
    
    /**
     * Binds the SSAO texture.
     */
    public void bindSSAOTexture(int ssaoTexture) {
        // Bind SSAO texture to texture unit 11
        shader.setUniform("u_ssaoTexture", 11);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + 11);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, ssaoTexture);
    }
    
    /**
     * Sets camera position for view-dependent calculations.
     */
    public void setCameraPosition(Vector3f cameraPosition) {
        shader.setUniform("u_cameraPosition", cameraPosition);
    }
    
    /**
     * Sets inverse view and projection matrices for position reconstruction.
     */
    public void setInverseMatrices(Matrix4f invViewMatrix, Matrix4f invProjectionMatrix) {
        shader.setUniform("u_invViewMatrix", invViewMatrix);
        shader.setUniform("u_invProjectionMatrix", invProjectionMatrix);
    }
    
    /**
     * Sets light space matrices for cascaded shadow mapping.
     */
    public void setShadowMatrices(Matrix4f[] lightSpaceMatrices) {
        for (int i = 0; i < lightSpaceMatrices.length && i < 4; i++) {
            shader.setUniform("u_lightSpaceMatrix[" + i + "]", lightSpaceMatrices[i]);
        }
    }
    
    /**
     * Sets lighting parameters from the lighting system.
     */
    public void setLightingParameters(LightingSystem lightingSystem) {
        // Ambient lighting
        shader.setUniform("u_ambientColor", lightingSystem.getAmbientColor());
        shader.setUniform("u_ambientIntensity", lightingSystem.getAmbientIntensity());
        
        // IBL settings
        shader.setUniform("u_iblEnabled", lightingSystem.isIBLEnabled());
        
        // Light counts
        shader.setUniform("u_directionalLightCount", lightingSystem.getDirectionalLightCount());
        shader.setUniform("u_pointLightCount", lightingSystem.getPointLightCount());
        shader.setUniform("u_spotLightCount", lightingSystem.getSpotLightCount());
        
        // Shadow settings
        shader.setUniform("u_shadowsEnabled", lightingSystem.areShadowsEnabled());
    }
    
    /**
     * Sets directional light parameters.
     */
    public void setDirectionalLights(LightingSystem.DirectionalLight[] lights) {
        for (int i = 0; i < lights.length && i < 4; i++) {
            LightingSystem.DirectionalLight light = lights[i];
            String prefix = "u_directionalLights[" + i + "]";
            
            shader.setUniform(prefix + ".direction", light.getDirection());
            shader.setUniform(prefix + ".color", light.getColor());
            shader.setUniform(prefix + ".intensity", light.getIntensity());
            shader.setUniform(prefix + ".castsShadows", light.castsShadows());
        }
    }
    
    /**
     * Sets point light parameters.
     */
    public void setPointLights(LightingSystem.PointLight[] lights) {
        for (int i = 0; i < lights.length && i < 32; i++) {
            LightingSystem.PointLight light = lights[i];
            String prefix = "u_pointLights[" + i + "]";
            
            shader.setUniform(prefix + ".position", light.getPosition());
            shader.setUniform(prefix + ".color", light.getColor());
            shader.setUniform(prefix + ".intensity", light.getIntensity());
            shader.setUniform(prefix + ".radius", light.getRadius());
            shader.setUniform(prefix + ".attenuation", light.getAttenuation());
        }
    }
    
    /**
     * Sets spot light parameters.
     */
    public void setSpotLights(LightingSystem.SpotLight[] lights) {
        for (int i = 0; i < lights.length && i < 16; i++) {
            LightingSystem.SpotLight light = lights[i];
            String prefix = "u_spotLights[" + i + "]";
            
            shader.setUniform(prefix + ".position", light.getPosition());
            shader.setUniform(prefix + ".direction", light.getDirection());
            shader.setUniform(prefix + ".color", light.getColor());
            shader.setUniform(prefix + ".intensity", light.getIntensity());
            shader.setUniform(prefix + ".innerCone", light.getInnerCone());
            shader.setUniform(prefix + ".outerCone", light.getOuterCone());
            shader.setUniform(prefix + ".radius", light.getRadius());
        }
    }
    
    /**
     * Creates the vertex shader source for the lighting pass.
     */
    private String createVertexShaderSource() {
        return """
            #version 330 core
            
            layout (location = 0) in vec3 a_position;
            layout (location = 1) in vec2 a_texCoord;
            
            out vec2 v_texCoord;
            
            void main() {
                v_texCoord = a_texCoord;
                gl_Position = vec4(a_position, 1.0);
            }
            """;
    }
    
    /**
     * Creates the fragment shader source for PBR lighting calculations.
     */
    private String createFragmentShaderSource() {
        return """
            #version 330 core
            
            in vec2 v_texCoord;
            out vec4 fragColor;
            
            // G-Buffer textures
            uniform sampler2D u_gAlbedo;
            uniform sampler2D u_gNormal;
            uniform sampler2D u_gDepth;     // Changed from u_gPosition to u_gDepth
            uniform sampler2D u_gEmission;
            
            // IBL textures
            uniform samplerCube u_irradianceMap;
            uniform samplerCube u_prefilterMap;
            uniform sampler2D u_brdfLUT;
            
            // Shadow map
            uniform sampler2D u_shadowMap;
            uniform mat4 u_lightSpaceMatrix[4];
            
            // SSAO
            uniform sampler2D u_ssaoTexture;
            
            // Camera and matrices for position reconstruction
            uniform vec3 u_cameraPosition;
            uniform mat4 u_invViewMatrix;
            uniform mat4 u_invProjectionMatrix;
            
            // Ambient lighting
            uniform vec3 u_ambientColor;
            uniform float u_ambientIntensity;
            
            // Lighting settings
            uniform bool u_iblEnabled;
            uniform bool u_shadowsEnabled;
            
            // Light counts
            uniform int u_directionalLightCount;
            uniform int u_pointLightCount;
            uniform int u_spotLightCount;
            
            // Light structures
            struct DirectionalLight {
                vec3 direction;
                vec3 color;
                float intensity;
                bool castsShadows;
            };
            
            struct PointLight {
                vec3 position;
                vec3 color;
                float intensity;
                float radius;
                vec3 attenuation;
            };
            
            struct SpotLight {
                vec3 position;
                vec3 direction;
                vec3 color;
                float intensity;
                float innerCone;
                float outerCone;
                float radius;
            };
            
            // Light arrays
            uniform DirectionalLight u_directionalLights[4];
            uniform PointLight u_pointLights[32];
            uniform SpotLight u_spotLights[16];
            
            // Constants
            const float PI = 3.14159265359;
            const float EPSILON = 0.001;
            
            // Shadow mapping function with PCF
            float calculateShadow(vec3 worldPos, vec3 normal, vec3 lightDir) {
                if (!u_shadowsEnabled) return 1.0;
                
                // Transform world position to light space
                vec4 lightSpacePos = u_lightSpaceMatrix[0] * vec4(worldPos, 1.0);
                
                // Perform perspective divide
                vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
                
                // Transform to [0,1] range
                projCoords = projCoords * 0.5 + 0.5;
                
                // Check if position is outside shadow map
                if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || 
                    projCoords.y < 0.0 || projCoords.y > 1.0) {
                    return 1.0;
                }
                
                // Get closest depth value from shadow map
                float closestDepth = texture(u_shadowMap, projCoords.xy).r;
                
                // Get depth of current fragment from light's perspective
                float currentDepth = projCoords.z;
                
                // Calculate bias to prevent shadow acne
                // Tuned normal-dependent bias to reduce acne while minimizing peter-panning
                float bias = max(0.0015 * (1.0 - dot(normal, lightDir)), 0.0005);
                
                // PCF (Percentage Closer Filtering) for soft shadows
                float shadow = 0.0;
                vec2 texelSize = 1.0 / textureSize(u_shadowMap, 0);
                
                for(int x = -2; x <= 2; ++x) {
                    for(int y = -2; y <= 2; ++y) {
                        float pcfDepth = texture(u_shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                        shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
                    }
                }
                shadow /= 25.0;
                
                return 1.0 - shadow;
            }
            
            // PBR functions
            vec3 fresnelSchlick(float cosTheta, vec3 F0) {
                return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
            }
            
            vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
                return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
            }
            
            float distributionGGX(vec3 N, vec3 H, float roughness) {
                float a = roughness * roughness;
                float a2 = a * a;
                float NdotH = max(dot(N, H), 0.0);
                float NdotH2 = NdotH * NdotH;
                
                float num = a2;
                float denom = (NdotH2 * (a2 - 1.0) + 1.0);
                denom = PI * denom * denom;
                
                return num / denom;
            }
            
            float geometrySchlickGGX(float NdotV, float roughness) {
                float r = (roughness + 1.0);
                float k = (r * r) / 8.0;
                
                float num = NdotV;
                float denom = NdotV * (1.0 - k) + k;
                
                return num / denom;
            }
            
            float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
                float NdotV = max(dot(N, V), 0.0);
                float NdotL = max(dot(N, L), 0.0);
                float ggx2 = geometrySchlickGGX(NdotV, roughness);
                float ggx1 = geometrySchlickGGX(NdotL, roughness);
                
                return ggx1 * ggx2;
            }
            
            vec3 calculateDirectionalLight(DirectionalLight light, vec3 worldPos, vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, vec3 F0) {
                vec3 L = normalize(-light.direction);
                vec3 H = normalize(V + L);
                
                float NdotL = max(dot(N, L), 0.0);
                
                if (NdotL <= 0.0) return vec3(0.0);
                
                // Calculate shadow factor
                float shadowFactor = 1.0;
                if (light.castsShadows) {
                    shadowFactor = calculateShadow(worldPos, N, L);
                }
                
                // Cook-Torrance BRDF
                float NDF = distributionGGX(N, H, roughness);
                float G = geometrySmith(N, V, L, roughness);
                vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
                
                vec3 kS = F;
                vec3 kD = vec3(1.0) - kS;
                kD *= 1.0 - metallic;
                
                vec3 numerator = NDF * G * F;
                float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + EPSILON;
                vec3 specular = numerator / denominator;
                
                vec3 radiance = light.color * light.intensity;
                return (kD * albedo / PI + specular) * radiance * NdotL * shadowFactor;
            }
            
            vec3 calculatePointLight(PointLight light, vec3 worldPos, vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, vec3 F0) {
                vec3 L = normalize(light.position - worldPos);
                vec3 H = normalize(V + L);
                float distance = length(light.position - worldPos);
                
                // Attenuation
                float attenuation = 1.0 / (light.attenuation.x + light.attenuation.y * distance + light.attenuation.z * distance * distance);
                
                // Radius falloff
                float falloff = clamp(1.0 - (distance / light.radius), 0.0, 1.0);
                attenuation *= falloff * falloff;
                
                float NdotL = max(dot(N, L), 0.0);
                
                if (NdotL <= 0.0 || attenuation <= 0.0) return vec3(0.0);
                
                // Cook-Torrance BRDF
                float NDF = distributionGGX(N, H, roughness);
                float G = geometrySmith(N, V, L, roughness);
                vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
                
                vec3 kS = F;
                vec3 kD = vec3(1.0) - kS;
                kD *= 1.0 - metallic;
                
                vec3 numerator = NDF * G * F;
                float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + EPSILON;
                vec3 specular = numerator / denominator;
                
                vec3 radiance = light.color * light.intensity * attenuation;
                return (kD * albedo / PI + specular) * radiance * NdotL;
            }
            
            vec3 calculateSpotLight(SpotLight light, vec3 worldPos, vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, vec3 F0) {
                vec3 L = normalize(light.position - worldPos);
                vec3 H = normalize(V + L);
                float distance = length(light.position - worldPos);
                
                // Spot light cone
                float theta = dot(L, normalize(-light.direction));
                float epsilon = light.innerCone - light.outerCone;
                float intensity = clamp((theta - light.outerCone) / epsilon, 0.0, 1.0);
                
                // Distance attenuation
                float attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * distance * distance);
                
                // Radius falloff
                float falloff = clamp(1.0 - (distance / light.radius), 0.0, 1.0);
                attenuation *= falloff * falloff * intensity;
                
                float NdotL = max(dot(N, L), 0.0);
                
                if (NdotL <= 0.0 || attenuation <= 0.0) return vec3(0.0);
                
                // Cook-Torrance BRDF
                float NDF = distributionGGX(N, H, roughness);
                float G = geometrySmith(N, V, L, roughness);
                vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
                
                vec3 kS = F;
                vec3 kD = vec3(1.0) - kS;
                kD *= 1.0 - metallic;
                
                vec3 numerator = NDF * G * F;
                float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + EPSILON;
                vec3 specular = numerator / denominator;
                
                vec3 radiance = light.color * light.intensity * attenuation;
                return (kD * albedo / PI + specular) * radiance * NdotL;
            }
            
            vec3 calculateIBL(vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, vec3 F0) {
                if (!u_iblEnabled) return vec3(0.0);
                
                vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
                
                vec3 kS = F;
                vec3 kD = 1.0 - kS;
                kD *= 1.0 - metallic;
                
                vec3 irradiance = texture(u_irradianceMap, N).rgb;
                vec3 diffuse = irradiance * albedo;
                
                vec3 R = reflect(-V, N);
                const float MAX_REFLECTION_LOD = 4.0;
                vec3 prefilteredColor = textureLod(u_prefilterMap, R, roughness * MAX_REFLECTION_LOD).rgb;
                vec2 brdf = texture(u_brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
                vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);
                
                return kD * diffuse + specular;
            }
            
            // Reconstruct world position from depth
            vec3 reconstructWorldPosition(vec2 texCoord, float depth) {
                // Convert screen coordinates to NDC
                vec4 clipSpacePos = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                
                // Transform to view space
                vec4 viewSpacePos = u_invProjectionMatrix * clipSpacePos;
                viewSpacePos /= viewSpacePos.w;
                
                // Transform to world space
                vec4 worldSpacePos = u_invViewMatrix * viewSpacePos;
                return worldSpacePos.xyz;
            }
            
            // Decode octahedral normal from RG channels
            vec3 decodeOctahedralNormal(vec2 encoded) {
                vec3 n = vec3(encoded.x, encoded.y, 1.0 - abs(encoded.x) - abs(encoded.y));
                if (n.z < 0.0) {
                    n.xy = (1.0 - abs(n.yx)) * sign(n.xy);
                }
                return normalize(n);
            }
            
            void main() {
                // Sample G-Buffer
                vec4 gAlbedo = texture(u_gAlbedo, v_texCoord);
                vec4 gNormal = texture(u_gNormal, v_texCoord);
                float gDepth = texture(u_gDepth, v_texCoord).r;
                vec4 gEmission = texture(u_gEmission, v_texCoord);
                
                // Extract material properties
                vec3 albedo = gAlbedo.rgb;
                float metallic = gAlbedo.a;
                vec3 normal = decodeOctahedralNormal(gNormal.rg);  // Decode from RG channels
                float roughness = gNormal.b;  // Roughness from B channel
                vec3 worldPos = reconstructWorldPosition(v_texCoord, gDepth);  // Reconstruct from depth
                vec3 emission = gEmission.rgb;
                
                // Sample SSAO
                float ssao = texture(u_ssaoTexture, v_texCoord).r;
                float ao = gNormal.a * ssao;
                
                // Calculate view direction
                vec3 V = normalize(u_cameraPosition - worldPos);
                
                // Calculate F0 (surface reflection at zero incidence)
                vec3 F0 = vec3(0.04);
                F0 = mix(F0, albedo, metallic);
                
                // Accumulate lighting
                vec3 Lo = vec3(0.0);
                
                // Directional lights
                for (int i = 0; i < u_directionalLightCount && i < 4; i++) {
                    Lo += calculateDirectionalLight(u_directionalLights[i], worldPos, normal, V, albedo, metallic, roughness, F0);
                }
                
                // Point lights
                for (int i = 0; i < u_pointLightCount && i < 32; i++) {
                    Lo += calculatePointLight(u_pointLights[i], worldPos, normal, V, albedo, metallic, roughness, F0);
                }
                
                // Spot lights
                for (int i = 0; i < u_spotLightCount && i < 16; i++) {
                    Lo += calculateSpotLight(u_spotLights[i], worldPos, normal, V, albedo, metallic, roughness, F0);
                }
                
                // Image-based lighting
                vec3 ambient = calculateIBL(normal, V, albedo, metallic, roughness, F0);
                
                // Add ambient lighting fallback
                if (!u_iblEnabled) {
                    ambient = u_ambientColor * u_ambientIntensity * albedo * ao;
                }
                
                // Final color
                vec3 color = ambient + Lo + emission;
                
                fragColor = vec4(color, 1.0);
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
        logger.info("Lighting pass shader cleaned up");
    }
}