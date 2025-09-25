#version 330 core

out vec4 FragColor;

in vec2 TexCoords;
in vec3 WorldPos;
in vec3 Normal;

// Material parameters
uniform sampler2D albedoMap;
uniform sampler2D normalMap;
uniform sampler2D metallicMap;
uniform sampler2D roughnessMap;
uniform sampler2D aoMap;
uniform samplerCube irradianceMap;
uniform samplerCube prefilterMap;
uniform sampler2D brdfLUT;

// Lights
uniform vec3 lightPositions[4];
uniform vec3 lightColors[4];
uniform vec3 camPos;

// Cascaded Shadow Maps
uniform sampler2D shadowMaps[4];
uniform mat4 lightSpaceMatrices[4];
uniform float cascadeSplits[4];
uniform int numCascades;
uniform bool enablePCF;
uniform int pcfSamples;
uniform float pcfRadius;
uniform float shadowBias;

const float PI = 3.14159265359;

// PBR functions
float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return num / denom;
}

float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;

    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return num / denom;
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// PCF shadow sampling
float PCFShadow(sampler2D shadowMap, vec4 fragPosLightSpace, float bias) {
    // Perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    
    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;
    
    // Check if fragment is outside shadow map bounds
    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || 
        projCoords.y < 0.0 || projCoords.y > 1.0) {
        return 0.0; // No shadow outside bounds
    }
    
    float currentDepth = projCoords.z;
    float shadow = 0.0;
    
    if (enablePCF && pcfSamples > 1) {
        // PCF with multiple samples
        vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
        float radius = pcfRadius * texelSize.x;
        
        int samples = 0;
        int halfSamples = pcfSamples / 2;
        
        for (int x = -halfSamples; x <= halfSamples; ++x) {
            for (int y = -halfSamples; y <= halfSamples; ++y) {
                vec2 offset = vec2(x, y) * radius;
                float pcfDepth = texture(shadowMap, projCoords.xy + offset).r;
                shadow += (currentDepth - bias > pcfDepth) ? 1.0 : 0.0;
                samples++;
                
                if (samples >= pcfSamples) break;
            }
            if (samples >= pcfSamples) break;
        }
        shadow /= float(samples);
    } else {
        // Simple shadow mapping
        float closestDepth = texture(shadowMap, projCoords.xy).r;
        shadow = (currentDepth - bias > closestDepth) ? 1.0 : 0.0;
    }
    
    return shadow;
}

// Cascaded Shadow Map calculation
float CalculateCSMShadow(vec3 fragPos, vec3 normal, vec3 lightDir) {
    // Calculate view space depth
    vec4 viewPos = inverse(mat4(1.0)) * vec4(fragPos, 1.0); // This should use actual view matrix
    float depth = abs(viewPos.z);
    
    // Find appropriate cascade
    int cascadeIndex = numCascades - 1;
    for (int i = 0; i < numCascades - 1; ++i) {
        if (depth < cascadeSplits[i]) {
            cascadeIndex = i;
            break;
        }
    }
    
    // Calculate fragment position in light space for selected cascade
    vec4 fragPosLightSpace = lightSpaceMatrices[cascadeIndex] * vec4(fragPos, 1.0);
    
    // Calculate bias based on surface angle to light
    float bias = max(shadowBias * (1.0 - dot(normal, lightDir)), shadowBias * 0.1);
    
    // Apply cascade-specific bias scaling
    float cascadeBias = bias * (1.0 + float(cascadeIndex) * 0.5);
    
    // Calculate shadow using appropriate shadow map
    float shadow = 0.0;
    if (cascadeIndex == 0) {
        shadow = PCFShadow(shadowMaps[0], fragPosLightSpace, cascadeBias);
    } else if (cascadeIndex == 1) {
        shadow = PCFShadow(shadowMaps[1], fragPosLightSpace, cascadeBias);
    } else if (cascadeIndex == 2) {
        shadow = PCFShadow(shadowMaps[2], fragPosLightSpace, cascadeBias);
    } else if (cascadeIndex == 3) {
        shadow = PCFShadow(shadowMaps[3], fragPosLightSpace, cascadeBias);
    }
    
    // Fade shadows at cascade boundaries to reduce popping
    float fadeStart = 0.9f;
    if (cascadeIndex < numCascades - 1) {
        float nextCascadeStart = (cascadeIndex == 0) ? 0.0 : cascadeSplits[cascadeIndex - 1];
        float currentCascadeEnd = cascadeSplits[cascadeIndex];
        float cascadeRange = currentCascadeEnd - nextCascadeStart;
        float fadeDistance = cascadeRange * (1.0 - fadeStart);
        float distanceInCascade = depth - nextCascadeStart;
        
        if (distanceInCascade > cascadeRange * fadeStart) {
            // Calculate next cascade shadow for blending
            vec4 nextFragPosLightSpace = lightSpaceMatrices[cascadeIndex + 1] * vec4(fragPos, 1.0);
            float nextShadow = 0.0;
            
            if (cascadeIndex + 1 == 1) {
                nextShadow = PCFShadow(shadowMaps[1], nextFragPosLightSpace, cascadeBias);
            } else if (cascadeIndex + 1 == 2) {
                nextShadow = PCFShadow(shadowMaps[2], nextFragPosLightSpace, cascadeBias);
            } else if (cascadeIndex + 1 == 3) {
                nextShadow = PCFShadow(shadowMaps[3], nextFragPosLightSpace, cascadeBias);
            }
            
            float blendFactor = (distanceInCascade - cascadeRange * fadeStart) / fadeDistance;
            shadow = mix(shadow, nextShadow, clamp(blendFactor, 0.0, 1.0));
        }
    }
    
    return shadow;
}

void main() {
    // Sample material properties
    vec3 albedo = pow(texture(albedoMap, TexCoords).rgb, 2.2);
    float metallic = texture(metallicMap, TexCoords).r;
    float roughness = texture(roughnessMap, TexCoords).r;
    float ao = texture(aoMap, TexCoords).r;
    
    // Calculate normal from normal map
    vec3 N = normalize(Normal);
    vec3 normalMapSample = texture(normalMap, TexCoords).rgb * 2.0 - 1.0;
    
    // Create TBN matrix (simplified - assumes normal map is in tangent space)
    vec3 T = normalize(cross(N, vec3(0.0, 1.0, 0.0)));
    vec3 B = cross(N, T);
    mat3 TBN = mat3(T, B, N);
    N = normalize(TBN * normalMapSample);
    
    vec3 V = normalize(camPos - WorldPos);
    
    // Calculate reflectance at normal incidence
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);
    
    // Reflectance equation
    vec3 Lo = vec3(0.0);
    
    // Calculate contribution from each light
    for (int i = 0; i < 4; ++i) {
        vec3 L = normalize(lightPositions[i] - WorldPos);
        vec3 H = normalize(V + L);
        float distance = length(lightPositions[i] - WorldPos);
        float attenuation = 1.0 / (distance * distance);
        vec3 radiance = lightColors[i] * attenuation;
        
        // Cook-Torrance BRDF
        float NDF = DistributionGGX(N, H, roughness);
        float G = GeometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
        
        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - metallic;
        
        vec3 numerator = NDF * G * F;
        float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.0001;
        vec3 specular = numerator / denominator;
        
        float NdotL = max(dot(N, L), 0.0);
        
        // Calculate shadow for primary light (index 0)
        float shadow = 0.0;
        if (i == 0) {
            shadow = CalculateCSMShadow(WorldPos, N, L);
        }
        
        Lo += (kD * albedo / PI + specular) * radiance * NdotL * (1.0 - shadow);
    }
    
    // Ambient lighting (IBL)
    vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
    
    vec3 kS = F;
    vec3 kD = 1.0 - kS;
    kD *= 1.0 - metallic;
    
    vec3 irradiance = texture(irradianceMap, N).rgb;
    vec3 diffuse = irradiance * albedo;
    
    // Sample both the pre-filter map and the BRDF lut and combine them together
    const float MAX_REFLECTION_LOD = 4.0;
    vec3 R = reflect(-V, N);
    vec3 prefilteredColor = textureLod(prefilterMap, R, roughness * MAX_REFLECTION_LOD).rgb;
    vec2 brdf = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
    vec3 specular = prefilteredColor * (F * brdf.x + brdf.y);
    
    vec3 ambient = (kD * diffuse + specular) * ao;
    
    vec3 color = ambient + Lo;
    
    // HDR tonemapping
    color = color / (color + vec3(1.0));
    
    // Gamma correction
    color = pow(color, vec3(1.0/2.2));
    
    FragColor = vec4(color, 1.0);
}