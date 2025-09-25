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
uniform float u_attenuation;
uniform int u_directions;
uniform int u_steps;
uniform float u_intensity;

uniform vec2 u_screenSize;
uniform vec2 u_invScreenSize;

const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

// Convert depth to linear view space
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * u_nearPlane * u_farPlane) / (u_farPlane + u_nearPlane - z * (u_farPlane - u_nearPlane));
}

// Reconstruct world position from depth
vec3 reconstructWorldPos(vec2 texCoord, float depth) {
    vec4 clipSpacePos = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpacePos = u_inverseProjectionMatrix * clipSpacePos;
    viewSpacePos /= viewSpacePos.w;
    return viewSpacePos.xyz;
}

// Calculate horizon angle for a given direction
float calculateHorizonAngle(vec3 viewPos, vec3 viewNormal, vec2 direction, vec2 texCoord) {
    float maxAngle = -1.0;
    
    for (int step = 1; step <= u_steps; step++) {
        float stepSize = float(step) / float(u_steps);
        vec2 sampleCoord = texCoord + direction * u_radius * stepSize * u_invScreenSize;
        
        // Check bounds
        if (sampleCoord.x < 0.0 || sampleCoord.x > 1.0 || sampleCoord.y < 0.0 || sampleCoord.y > 1.0) {
            continue;
        }
        
        float sampleDepth = texture(u_depthTexture, sampleCoord).r;
        vec3 sampleViewPos = reconstructWorldPos(sampleCoord, sampleDepth);
        
        vec3 sampleVector = sampleViewPos - viewPos;
        float sampleDistance = length(sampleVector);
        
        // Apply distance attenuation
        float attenuation = 1.0 - smoothstep(0.0, u_radius, sampleDistance);
        
        if (attenuation > 0.0) {
            vec3 sampleDir = normalize(sampleVector);
            float angle = dot(sampleDir, viewNormal);
            
            // Apply bias to prevent self-occlusion
            angle = max(angle - u_bias, 0.0);
            
            maxAngle = max(maxAngle, angle * attenuation);
        }
    }
    
    return maxAngle;
}

void main() {
    float depth = texture(u_depthTexture, TexCoord).r;
    
    // Skip background pixels
    if (depth >= 1.0) {
        FragColor = 1.0;
        return;
    }
    
    vec3 viewPos = reconstructWorldPos(TexCoord, depth);
    vec3 viewNormal = normalize(texture(u_normalTexture, TexCoord).xyz * 2.0 - 1.0);
    
    // Get noise for rotation
    vec2 noiseScale = u_screenSize / 4.0;
    vec3 noise = texture(u_noiseTexture, TexCoord * noiseScale).xyz;
    
    float occlusion = 0.0;
    
    // Sample in multiple directions around the hemisphere
    for (int i = 0; i < u_directions; i++) {
        float angle = (float(i) / float(u_directions)) * TWO_PI;
        
        // Add noise rotation
        angle += noise.x * TWO_PI;
        
        vec2 direction = vec2(cos(angle), sin(angle));
        
        // Calculate horizon angle for this direction
        float horizonAngle = calculateHorizonAngle(viewPos, viewNormal, direction, TexCoord);
        
        // Convert horizon angle to occlusion
        if (horizonAngle > 0.0) {
            occlusion += sin(horizonAngle * PI * 0.5);
        }
    }
    
    // Average and apply intensity
    occlusion /= float(u_directions);
    occlusion = pow(occlusion, u_intensity);
    
    // Output ambient occlusion (1.0 = no occlusion, 0.0 = full occlusion)
    FragColor = 1.0 - occlusion;
}