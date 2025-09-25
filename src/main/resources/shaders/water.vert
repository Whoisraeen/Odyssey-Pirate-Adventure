#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoord;
out vec4 ClipSpace;
out vec2 ReflectionTexCoord;
out vec2 RefractionTexCoord;
out vec3 ToCameraVector;
out vec3 FromLightVector;
out float FoamFactor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform vec3 cameraPos;
uniform vec3 lightPos;
uniform float time;

// Gerstner wave parameters (up to 8 waves)
uniform int numWaves;
uniform float waveAmplitudes[8];
uniform float waveFrequencies[8];
uniform float wavePhases[8];
uniform vec2 waveDirections[8];
uniform float waveSteepness[8];
uniform float waveSpeeds[8];

// Water parameters
uniform float waveStrength;
uniform float foamThreshold;

/**
 * Calculate Gerstner wave displacement and normal for a single wave
 */
vec3 calculateGerstnerWave(int waveIndex, vec3 worldPos, out vec3 normal, out float foam) {
    float amplitude = waveAmplitudes[waveIndex];
    float frequency = waveFrequencies[waveIndex];
    float phase = wavePhases[waveIndex];
    vec2 direction = waveDirections[waveIndex];
    float steepness = waveSteepness[waveIndex];
    float speed = waveSpeeds[waveIndex];
    
    float k = frequency; // Wave number
    float w = speed * k; // Angular frequency
    float dot = dot(direction, worldPos.xz);
    float theta = k * dot - w * time + phase;
    float cosTheta = cos(theta);
    float sinTheta = sin(theta);
    
    // Gerstner wave displacement
    float Q = steepness / (k * amplitude);
    
    vec3 displacement;
    displacement.x = Q * amplitude * direction.x * cosTheta;
    displacement.y = amplitude * sinTheta;
    displacement.z = Q * amplitude * direction.y * cosTheta;
    
    // Calculate normal using partial derivatives
    float WA = w * amplitude;
    float dDx_dx = 1.0 - Q * direction.x * direction.x * WA * sinTheta;
    float dDx_dz = -Q * direction.x * direction.y * WA * sinTheta;
    float dDy_dx = direction.x * WA * cosTheta;
    float dDy_dz = direction.y * WA * cosTheta;
    float dDz_dx = -Q * direction.y * direction.x * WA * sinTheta;
    float dDz_dz = 1.0 - Q * direction.y * direction.y * WA * sinTheta;
    
    // Tangent vectors
    vec3 tangentX = vec3(dDx_dx, dDy_dx, dDz_dx);
    vec3 tangentZ = vec3(dDx_dz, dDy_dz, dDz_dz);
    
    // Normal from cross product
    normal = normalize(cross(tangentX, tangentZ));
    
    // Foam calculation
    float crestFactor = (cosTheta + 1.0) * 0.5; // 0-1 range
    float steepnessFactor = steepness * steepness;
    foam = crestFactor * steepnessFactor * amplitude;
    
    return displacement;
}

void main() {
    vec3 worldPos = vec3(model * vec4(aPos, 1.0));
    
    // Calculate combined Gerstner wave displacement
    vec3 totalDisplacement = vec3(0.0);
    vec3 totalNormal = vec3(0.0, 1.0, 0.0);
    float totalFoam = 0.0;
    
    for (int i = 0; i < numWaves && i < 8; i++) {
        vec3 waveNormal;
        float waveFoam;
        vec3 waveDisplacement = calculateGerstnerWave(i, worldPos, waveNormal, waveFoam);
        
        totalDisplacement += waveDisplacement * waveStrength;
        totalNormal += waveNormal;
        totalFoam += waveFoam;
    }
    
    // Normalize the combined normal
    totalNormal = normalize(totalNormal);
    
    // Apply displacement to world position
    worldPos += totalDisplacement;
    
    // Calculate final position
    gl_Position = projection * view * vec4(worldPos, 1.0);
    ClipSpace = gl_Position;
    
    // Pass data to fragment shader
    FragPos = worldPos;
    Normal = totalNormal;
    TexCoord = aTexCoord;
    
    // Calculate reflection and refraction texture coordinates
    vec4 ndc = ClipSpace;
    ndc.xyz /= ndc.w;
    ndc.xyz = ndc.xyz * 0.5 + 0.5;
    ReflectionTexCoord = vec2(ndc.x, -ndc.y);
    RefractionTexCoord = vec2(ndc.x, ndc.y);
    
    // Calculate vectors for lighting
    ToCameraVector = normalize(cameraPos - FragPos);
    FromLightVector = normalize(FragPos - lightPos);
    
    // Pass foam factor (clamped to prevent over-foaming)
    FoamFactor = clamp(totalFoam / foamThreshold, 0.0, 1.0);
}