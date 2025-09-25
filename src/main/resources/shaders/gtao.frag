#version 330 core

in vec2 TexCoord;
out float FragColor;

uniform sampler2D u_depthTexture;
uniform sampler2D u_normalTexture;
uniform sampler2D u_noiseTexture;

uniform mat4 u_projectionMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_inverseProjectionMatrix;

uniform vec3 u_cameraPosition;
uniform float u_nearPlane;
uniform float u_farPlane;

uniform float u_radius;
uniform float u_bias;
uniform float u_power;
uniform int u_sliceCount;
uniform int u_stepsPerSlice;
uniform float u_intensity;

uniform vec2 u_screenSize;
uniform vec2 u_invScreenSize;

const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;
const float HALF_PI = 1.57079632679;

// Convert depth to linear view space
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * u_nearPlane * u_farPlane) / (u_farPlane + u_nearPlane - z * (u_farPlane - u_nearPlane));
}

// Reconstruct view space position from depth
vec3 reconstructViewPos(vec2 texCoord, float depth) {
    vec4 clipSpacePos = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpacePos = u_inverseProjectionMatrix * clipSpacePos;
    viewSpacePos /= viewSpacePos.w;
    return viewSpacePos.xyz;
}

// Calculate integrated visibility for a slice
float integrateSlice(vec3 viewPos, vec3 viewNormal, vec2 sliceDir, vec2 texCoord) {
    float visibility = 0.0;
    
    // Sample along the slice direction
    for (int step = 1; step <= u_stepsPerSlice; step++) {
        float stepSize = (float(step) / float(u_stepsPerSlice)) * u_radius;
        vec2 sampleCoord = texCoord + sliceDir * stepSize * u_invScreenSize;
        
        // Check bounds
        if (sampleCoord.x < 0.0 || sampleCoord.x > 1.0 || sampleCoord.y < 0.0 || sampleCoord.y > 1.0) {
            continue;
        }
        
        float sampleDepth = texture(u_depthTexture, sampleCoord).r;
        vec3 sampleViewPos = reconstructViewPos(sampleCoord, sampleDepth);
        
        vec3 sampleVector = sampleViewPos - viewPos;
        float sampleDistance = length(sampleVector);
        
        // Skip samples that are too far
        if (sampleDistance > u_radius) {
            continue;
        }
        
        vec3 sampleDir = normalize(sampleVector);
        
        // Calculate horizon angle
        float cosHorizon = dot(sampleDir, viewNormal);
        
        // Apply bias to prevent self-occlusion
        cosHorizon = max(cosHorizon - u_bias, 0.0);
        
        // Calculate visibility contribution
        float horizonAngle = acos(clamp(cosHorizon, 0.0, 1.0));
        
        // Distance attenuation
        float attenuation = 1.0 - smoothstep(0.0, u_radius, sampleDistance);
        
        // Integrate visibility over the hemisphere
        float sliceVisibility = sin(horizonAngle) * attenuation;
        visibility = max(visibility, sliceVisibility);
    }
    
    return visibility;
}

void main() {
    float depth = texture(u_depthTexture, TexCoord).r;
    
    // Skip background pixels
    if (depth >= 1.0) {
        FragColor = 1.0;
        return;
    }
    
    vec3 viewPos = reconstructViewPos(TexCoord, depth);
    vec3 viewNormal = normalize(texture(u_normalTexture, TexCoord).xyz * 2.0 - 1.0);
    
    // Get noise for rotation
    vec2 noiseScale = u_screenSize / 4.0;
    vec3 noise = texture(u_noiseTexture, TexCoord * noiseScale).xyz;
    
    float totalOcclusion = 0.0;
    
    // Sample multiple slices around the hemisphere
    for (int slice = 0; slice < u_sliceCount; slice++) {
        float sliceAngle = (float(slice) / float(u_sliceCount)) * PI;
        
        // Add noise rotation
        sliceAngle += noise.x * TWO_PI;
        
        vec2 sliceDir = vec2(cos(sliceAngle), sin(sliceAngle));
        
        // Integrate visibility for this slice
        float sliceVisibility = integrateSlice(viewPos, viewNormal, sliceDir, TexCoord);
        
        totalOcclusion += sliceVisibility;
    }
    
    // Average occlusion across all slices
    totalOcclusion /= float(u_sliceCount);
    
    // Apply power curve for artistic control
    totalOcclusion = pow(totalOcclusion, u_power);
    
    // Apply intensity
    totalOcclusion *= u_intensity;
    
    // Output ambient occlusion (1.0 = no occlusion, 0.0 = full occlusion)
    FragColor = 1.0 - clamp(totalOcclusion, 0.0, 1.0);
}