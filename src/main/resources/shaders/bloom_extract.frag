#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_SceneTexture;
uniform float u_Threshold;
uniform float u_Knee;
uniform float u_Intensity;

// Luminance calculation
float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// Soft threshold function for smooth bloom transition
float softThreshold(float lum, float threshold, float knee) {
    float soft = threshold + knee;
    if (lum < threshold) {
        return 0.0;
    } else if (lum < soft) {
        float t = (lum - threshold) / knee;
        return t * t * (3.0 - 2.0 * t);
    } else {
        return 1.0;
    }
}

void main() {
    vec3 color = texture(u_SceneTexture, TexCoord).rgb;
    
    // Calculate luminance
    float lum = luminance(color);
    
    // Apply soft threshold
    float bloomFactor = softThreshold(lum, u_Threshold, u_Knee);
    
    // Extract bright areas
    vec3 bloomColor = color * bloomFactor * u_Intensity;
    
    FragColor = vec4(bloomColor, 1.0);
}