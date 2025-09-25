#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;
in vec4 ClipSpace;
in vec2 ReflectionTexCoord;
in vec2 RefractionTexCoord;
in vec3 ToCameraVector;
in vec3 FromLightVector;
in float FoamFactor;

out vec4 FragColor;

// Textures
uniform sampler2D reflectionTexture;
uniform sampler2D refractionTexture;
uniform sampler2D dudvMap;
uniform sampler2D normalMap;
uniform sampler2D foamTexture;
uniform sampler2D depthMap;
uniform sampler2D causticsTexture;

// Water properties
uniform vec3 waterColor;
uniform vec3 deepWaterColor;
uniform float waterTransparency;
uniform float reflectionStrength;
uniform float refractionStrength;
uniform float waveStrength;
uniform float foamThreshold;
uniform float time;

// Lighting
uniform vec3 lightColor;
uniform vec3 lightPos;
uniform vec3 cameraPos;
uniform float shininess;
uniform float specularStrength;

// Screen-space reflection parameters
uniform mat4 view;
uniform mat4 projection;
uniform float ssrMaxDistance;
uniform int ssrMaxSteps;
uniform float ssrStepSize;

// Caustics parameters
uniform float causticsStrength;
uniform float causticsScale;
uniform float causticsSpeed;

// Water depth parameters
uniform float nearPlane;
uniform float farPlane;

/**
 * Convert depth buffer value to linear depth
 */
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0; // Convert to NDC
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

/**
 * Calculate water depth based on depth buffer
 */
float calculateWaterDepth() {
    vec2 ndc = (ClipSpace.xy / ClipSpace.w) * 0.5 + 0.5;
    float floorDistance = linearizeDepth(texture(depthMap, ndc).r);
    float waterDistance = linearizeDepth(gl_FragCoord.z);
    return floorDistance - waterDistance;
}

/**
 * Improved Fresnel calculation using Schlick's approximation
 */
float calculateFresnel(vec3 viewDir, vec3 normal, float f0) {
    float cosTheta = max(dot(viewDir, normal), 0.0);
    return f0 + (1.0 - f0) * pow(1.0 - cosTheta, 5.0);
}

/**
 * Screen-space reflection calculation
 */
vec3 calculateSSR(vec3 worldPos, vec3 normal, vec3 viewDir) {
    // Calculate reflection direction in world space
    vec3 reflectionDir = reflect(-viewDir, normal);
    
    // Transform to view space
    vec3 viewPos = (view * vec4(worldPos, 1.0)).xyz;
    vec3 viewReflectionDir = (view * vec4(reflectionDir, 0.0)).xyz;
    
    // Ray marching in screen space
    vec3 rayPos = viewPos;
    vec3 rayStep = viewReflectionDir * ssrStepSize;
    
    for (int i = 0; i < ssrMaxSteps; i++) {
        rayPos += rayStep;
        
        // Project to screen space
        vec4 projectedPos = projection * vec4(rayPos, 1.0);
        vec2 screenPos = (projectedPos.xy / projectedPos.w) * 0.5 + 0.5;
        
        // Check if we're outside screen bounds
        if (screenPos.x < 0.0 || screenPos.x > 1.0 || screenPos.y < 0.0 || screenPos.y > 1.0) {
            break;
        }
        
        // Sample depth buffer
        float sampledDepth = linearizeDepth(texture(depthMap, screenPos).r);
        float rayDepth = -rayPos.z;
        
        // Check for intersection
        if (rayDepth > sampledDepth && rayDepth - sampledDepth < ssrMaxDistance) {
            return texture(reflectionTexture, screenPos).rgb;
        }
    }
    
    // Fallback to regular reflection texture
    return texture(reflectionTexture, ReflectionTexCoord).rgb;
}

/**
 * Calculate caustics effect
 */
vec3 calculateCaustics(vec3 worldPos, float waterDepth) {
    // Multiple caustics layers for more realistic effect
    vec2 causticsUV1 = worldPos.xz * causticsScale + time * causticsSpeed * 0.1;
    vec2 causticsUV2 = worldPos.xz * causticsScale * 1.3 - time * causticsSpeed * 0.08;
    vec2 causticsUV3 = worldPos.xz * causticsScale * 0.7 + time * causticsSpeed * 0.12;
    
    float caustics1 = texture(causticsTexture, causticsUV1).r;
    float caustics2 = texture(causticsTexture, causticsUV2).r;
    float caustics3 = texture(causticsTexture, causticsUV3).r;
    
    // Combine caustics layers
    float caustics = (caustics1 * caustics2 + caustics3) * 0.5;
    
    // Fade caustics with depth
    float depthFade = exp(-waterDepth * 0.1);
    caustics *= depthFade * causticsStrength;
    
    return vec3(caustics);
}

void main() {
    // Calculate water depth
    float waterDepth = calculateWaterDepth();
    
    // Sample distortion from DuDv map
    vec2 distortedTexCoords = texture(dudvMap, vec2(TexCoord.x + time * 0.02, TexCoord.y)).rg * 0.1;
    distortedTexCoords = TexCoord + vec2(distortedTexCoords.x, distortedTexCoords.y + time * 0.02);
    vec2 totalDistortion = (texture(dudvMap, distortedTexCoords).rg * 2.0 - 1.0) * waveStrength * 0.02;
    
    // Apply distortion to reflection and refraction coordinates
    vec2 reflectionCoords = ReflectionTexCoord + totalDistortion;
    vec2 refractionCoords = RefractionTexCoord + totalDistortion;
    
    // Clamp coordinates to prevent sampling outside texture bounds
    reflectionCoords = clamp(reflectionCoords, 0.001, 0.999);
    refractionCoords = clamp(refractionCoords, 0.001, 0.999);
    
    // Sample reflection and refraction
    vec3 reflectionColor = calculateSSR(FragPos, Normal, ToCameraVector);
    vec3 refractionColor = texture(refractionTexture, refractionCoords).rgb;
    
    // Sample normal map for surface detail
    vec3 normalMapSample = texture(normalMap, distortedTexCoords).rgb;
    vec3 surfaceNormal = normalize(Normal + (normalMapSample * 2.0 - 1.0) * 0.2);
    
    // Calculate Fresnel effect
    float fresnel = calculateFresnel(ToCameraVector, surfaceNormal, 0.02);
    
    // Mix reflection and refraction based on Fresnel
    vec3 waterSurfaceColor = mix(refractionColor, reflectionColor, fresnel);
    
    // Apply water color tinting based on depth
    float depthFactor = clamp(waterDepth * 0.1, 0.0, 1.0);
    vec3 finalWaterColor = mix(waterColor, deepWaterColor, depthFactor);
    waterSurfaceColor = mix(waterSurfaceColor, finalWaterColor, waterTransparency * depthFactor);
    
    // Add caustics effect
    vec3 caustics = calculateCaustics(FragPos, waterDepth);
    waterSurfaceColor += caustics;
    
    // Calculate specular lighting
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, surfaceNormal);
    float spec = pow(max(dot(ToCameraVector, reflectDir), 0.0), shininess);
    vec3 specular = specularStrength * spec * lightColor;
    
    // Add foam effect
    vec3 foamColor = texture(foamTexture, TexCoord * 8.0 + time * 0.1).rgb;
    float foamMask = smoothstep(0.4, 0.8, FoamFactor);
    waterSurfaceColor = mix(waterSurfaceColor, foamColor, foamMask);
    
    // Add specular highlights
    waterSurfaceColor += specular;
    
    // Apply depth-based transparency
    float alpha = clamp(waterDepth * 0.2, 0.1, 1.0);
    
    FragColor = vec4(waterSurfaceColor, alpha);
}