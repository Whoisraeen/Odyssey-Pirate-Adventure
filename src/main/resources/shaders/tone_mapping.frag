#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_InputTexture;
uniform int u_ToneMappingType;
uniform float u_Exposure;
uniform float u_Gamma;
uniform float u_WhitePoint;

// Luminance calculation
float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// Reinhard tone mapping
vec3 reinhardToneMapping(vec3 color) {
    return color / (color + vec3(1.0));
}

// Reinhard with white point
vec3 reinhardExtendedToneMapping(vec3 color, float whitePoint) {
    float lum = luminance(color);
    float numerator = lum * (1.0 + (lum / (whitePoint * whitePoint)));
    float mapped = numerator / (1.0 + lum);
    return color * (mapped / lum);
}

// ACES Filmic tone mapping
vec3 acesToneMapping(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

// Uncharted 2 filmic tone mapping
vec3 uncharted2ToneMapping(vec3 color) {
    const float A = 0.15;
    const float B = 0.50;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.02;
    const float F = 0.30;
    
    vec3 x = color;
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

// Simple exposure tone mapping
vec3 exposureToneMapping(vec3 color, float exposure) {
    return vec3(1.0) - exp(-color * exposure);
}

// Hable tone mapping (Uncharted 2)
vec3 hableToneMapping(vec3 color) {
    const float A = 0.22;
    const float B = 0.30;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.01;
    const float F = 0.30;
    
    vec3 x = color;
    vec3 curr = ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
    
    float W = 11.2;
    vec3 whiteScale = 1.0 / (((W * (A * W + C * B) + D * E) / (W * (A * W + B) + D * F)) - E / F);
    
    return curr * whiteScale;
}

void main() {
    vec3 hdrColor = texture(u_InputTexture, TexCoord).rgb;
    
    // Apply exposure
    hdrColor *= u_Exposure;
    
    vec3 mapped;
    
    // Apply tone mapping based on type
    switch (u_ToneMappingType) {
        case 0: // Reinhard
            mapped = reinhardToneMapping(hdrColor);
            break;
        case 1: // ACES
            mapped = acesToneMapping(hdrColor);
            break;
        case 2: // Filmic (Uncharted 2)
            mapped = hableToneMapping(hdrColor);
            break;
        case 3: // Exposure
            mapped = exposureToneMapping(hdrColor, 1.0);
            break;
        case 4: // Reinhard Extended
            mapped = reinhardExtendedToneMapping(hdrColor, u_WhitePoint);
            break;
        default:
            mapped = acesToneMapping(hdrColor); // Default to ACES
            break;
    }
    
    // Apply gamma correction
    mapped = pow(mapped, vec3(1.0 / u_Gamma));
    
    FragColor = vec4(mapped, 1.0);
}