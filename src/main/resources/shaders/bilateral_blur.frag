#version 330 core

in vec2 TexCoord;
out float FragColor;

uniform sampler2D u_ssaoTexture;
uniform sampler2D u_depthTexture;
uniform sampler2D u_normalTexture;

uniform vec2 u_screenSize;
uniform vec2 u_invScreenSize;
uniform float u_blurRadius;
uniform float u_depthThreshold;
uniform float u_normalThreshold;

// Gaussian weights for 5x5 kernel
const float gaussianWeights[25] = float[](
    0.003765, 0.015019, 0.023792, 0.015019, 0.003765,
    0.015019, 0.059912, 0.094907, 0.059912, 0.015019,
    0.023792, 0.094907, 0.150342, 0.094907, 0.023792,
    0.015019, 0.059912, 0.094907, 0.059912, 0.015019,
    0.003765, 0.015019, 0.023792, 0.015019, 0.003765
);

// Convert depth to linear view space
float linearizeDepth(float depth) {
    float nearPlane = 0.1;
    float farPlane = 1000.0;
    float z = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

void main() {
    vec2 texelSize = u_invScreenSize;
    
    float centerDepth = linearizeDepth(texture(u_depthTexture, TexCoord).r);
    vec3 centerNormal = normalize(texture(u_normalTexture, TexCoord).xyz * 2.0 - 1.0);
    float centerSSAO = texture(u_ssaoTexture, TexCoord).r;
    
    float totalWeight = 0.0;
    float blurredSSAO = 0.0;
    
    int radius = int(u_blurRadius);
    
    // 5x5 bilateral blur kernel
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            vec2 sampleCoord = TexCoord + offset;
            
            // Check bounds
            if (sampleCoord.x < 0.0 || sampleCoord.x > 1.0 || sampleCoord.y < 0.0 || sampleCoord.y > 1.0) {
                continue;
            }
            
            float sampleDepth = linearizeDepth(texture(u_depthTexture, sampleCoord).r);
            vec3 sampleNormal = normalize(texture(u_normalTexture, sampleCoord).xyz * 2.0 - 1.0);
            float sampleSSAO = texture(u_ssaoTexture, sampleCoord).r;
            
            // Calculate depth difference
            float depthDiff = abs(centerDepth - sampleDepth);
            float depthWeight = exp(-depthDiff / u_depthThreshold);
            
            // Calculate normal similarity
            float normalSimilarity = dot(centerNormal, sampleNormal);
            float normalWeight = pow(max(normalSimilarity, 0.0), 32.0);
            
            // Apply normal threshold
            if (normalSimilarity < u_normalThreshold) {
                normalWeight = 0.0;
            }
            
            // Get Gaussian weight
            int kernelIndex = (y + 2) * 5 + (x + 2);
            float gaussianWeight = gaussianWeights[kernelIndex];
            
            // Combine weights
            float finalWeight = depthWeight * normalWeight * gaussianWeight;
            
            totalWeight += finalWeight;
            blurredSSAO += sampleSSAO * finalWeight;
        }
    }
    
    // Normalize result
    if (totalWeight > 0.0) {
        FragColor = blurredSSAO / totalWeight;
    } else {
        FragColor = centerSSAO;
    }
}