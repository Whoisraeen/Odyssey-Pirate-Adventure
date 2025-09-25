#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_ColorTexture;
uniform sampler2D u_DepthTexture;
uniform float u_FocusDistance;
uniform float u_FocusRange;
uniform float u_BokehRadius;
uniform float u_NearPlane;
uniform float u_FarPlane;
uniform vec2 u_ScreenSize;

// Linearize depth from depth buffer
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0; // Convert to NDC
    return (2.0 * u_NearPlane * u_FarPlane) / (u_FarPlane + u_NearPlane - z * (u_FarPlane - u_NearPlane));
}

// Calculate circle of confusion
float calculateCoC(float depth) {
    float focusedDepth = u_FocusDistance;
    float blurAmount = abs(depth - focusedDepth) / u_FocusRange;
    return clamp(blurAmount * u_BokehRadius, 0.0, u_BokehRadius);
}

// Hexagonal bokeh pattern
vec2 hexagonalPattern(int i) {
    float angle = float(i) * 2.39996323; // Golden angle
    float radius = sqrt(float(i) + 0.5) / sqrt(float(19)); // Normalize to unit circle
    return vec2(cos(angle), sin(angle)) * radius;
}

void main() {
    vec2 texelSize = 1.0 / u_ScreenSize;
    
    // Sample depth and calculate circle of confusion
    float depth = texture(u_DepthTexture, TexCoord).r;
    float linearDepth = linearizeDepth(depth);
    float coc = calculateCoC(linearDepth);
    
    vec3 color = texture(u_ColorTexture, TexCoord).rgb;
    
    // If no blur needed, return original color
    if (coc < 0.5) {
        FragColor = vec4(color, 1.0);
        return;
    }
    
    // Bokeh blur sampling
    vec3 blurredColor = color;
    float totalWeight = 1.0;
    
    // Sample in hexagonal pattern for better bokeh
    for (int i = 1; i < 19; i++) {
        vec2 offset = hexagonalPattern(i) * coc * texelSize;
        vec2 sampleCoord = TexCoord + offset;
        
        // Check if sample is within screen bounds
        if (sampleCoord.x >= 0.0 && sampleCoord.x <= 1.0 && 
            sampleCoord.y >= 0.0 && sampleCoord.y <= 1.0) {
            
            vec3 sampleColor = texture(u_ColorTexture, sampleCoord).rgb;
            float sampleDepth = texture(u_DepthTexture, sampleCoord).r;
            float sampleLinearDepth = linearizeDepth(sampleDepth);
            float sampleCoC = calculateCoC(sampleLinearDepth);
            
            // Weight based on depth similarity and CoC
            float weight = 1.0;
            if (sampleLinearDepth > linearDepth) {
                weight = clamp(sampleCoC, 0.0, 1.0);
            }
            
            blurredColor += sampleColor * weight;
            totalWeight += weight;
        }
    }
    
    blurredColor /= totalWeight;
    
    // Blend based on circle of confusion
    float blendFactor = clamp(coc / u_BokehRadius, 0.0, 1.0);
    vec3 finalColor = mix(color, blurredColor, blendFactor);
    
    FragColor = vec4(finalColor, 1.0);
}