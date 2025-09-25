#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_CurrentFrame;
uniform sampler2D u_HistoryFrame;
uniform sampler2D u_VelocityTexture;
uniform sampler2D u_DepthTexture;
uniform float u_BlendFactor;
uniform float u_VarianceClipping;
uniform vec2 u_ScreenSize;
uniform vec2 u_JitterOffset;

// RGB to YCoCg conversion for better temporal stability
vec3 rgbToYCoCg(vec3 rgb) {
    float Y = dot(rgb, vec3(0.25, 0.5, 0.25));
    float Co = dot(rgb, vec3(0.5, 0.0, -0.5));
    float Cg = dot(rgb, vec3(-0.25, 0.5, -0.25));
    return vec3(Y, Co, Cg);
}

vec3 yCoCgToRgb(vec3 yCoCg) {
    float Y = yCoCg.x;
    float Co = yCoCg.y;
    float Cg = yCoCg.z;
    
    float r = Y + Co - Cg;
    float g = Y + Cg;
    float b = Y - Co - Cg;
    
    return vec3(r, g, b);
}

// Catmull-Rom filtering for better history sampling
vec3 catmullRomFilter(sampler2D tex, vec2 uv, vec2 texelSize) {
    vec2 samplePos = uv / texelSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;
    
    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);
    
    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / (w1 + w2);
    
    vec2 texPos0 = texPos1 - vec2(1.0);
    vec2 texPos3 = texPos1 + vec2(2.0);
    vec2 texPos12 = texPos1 + offset12;
    
    texPos0 *= texelSize;
    texPos3 *= texelSize;
    texPos12 *= texelSize;
    
    vec3 result = vec3(0.0);
    result += texture(tex, vec2(texPos0.x, texPos0.y)).rgb * w0.x * w0.y;
    result += texture(tex, vec2(texPos12.x, texPos0.y)).rgb * w12.x * w0.y;
    result += texture(tex, vec2(texPos3.x, texPos0.y)).rgb * w3.x * w0.y;
    
    result += texture(tex, vec2(texPos0.x, texPos12.y)).rgb * w0.x * w12.y;
    result += texture(tex, vec2(texPos12.x, texPos12.y)).rgb * w12.x * w12.y;
    result += texture(tex, vec2(texPos3.x, texPos12.y)).rgb * w3.x * w12.y;
    
    result += texture(tex, vec2(texPos0.x, texPos3.y)).rgb * w0.x * w3.y;
    result += texture(tex, vec2(texPos12.x, texPos3.y)).rgb * w12.x * w3.y;
    result += texture(tex, vec2(texPos3.x, texPos3.y)).rgb * w3.x * w3.y;
    
    return result;
}

void main() {
    vec2 texelSize = 1.0 / u_ScreenSize;
    
    // Sample current frame
    vec3 currentColor = texture(u_CurrentFrame, TexCoord).rgb;
    
    // Sample velocity for reprojection
    vec2 velocity = texture(u_VelocityTexture, TexCoord).xy;
    vec2 historyUV = TexCoord - velocity;
    
    // Check if history sample is valid
    bool validHistory = historyUV.x >= 0.0 && historyUV.x <= 1.0 && 
                       historyUV.y >= 0.0 && historyUV.y <= 1.0;
    
    if (!validHistory) {
        FragColor = vec4(currentColor, 1.0);
        return;
    }
    
    // Sample history with Catmull-Rom filtering
    vec3 historyColor = catmullRomFilter(u_HistoryFrame, historyUV, texelSize);
    
    // Convert to YCoCg for better temporal stability
    vec3 currentYCoCg = rgbToYCoCg(currentColor);
    vec3 historyYCoCg = rgbToYCoCg(historyColor);
    
    // Neighborhood clamping for variance clipping
    vec3 minColor = currentYCoCg;
    vec3 maxColor = currentYCoCg;
    vec3 avgColor = currentYCoCg;
    
    // Sample 3x3 neighborhood
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            if (x == 0 && y == 0) continue;
            
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            vec3 neighborColor = rgbToYCoCg(texture(u_CurrentFrame, TexCoord + offset).rgb);
            
            minColor = min(minColor, neighborColor);
            maxColor = max(maxColor, neighborColor);
            avgColor += neighborColor;
        }
    }
    avgColor /= 9.0;
    
    // Variance clipping
    vec3 variance = (maxColor - minColor) * u_VarianceClipping;
    vec3 clampedHistory = clamp(historyYCoCg, avgColor - variance, avgColor + variance);
    
    // Adaptive blend factor based on velocity and color difference
    float colorDiff = length(currentYCoCg - clampedHistory);
    float velocityMagnitude = length(velocity * u_ScreenSize);
    
    float adaptiveBlend = u_BlendFactor;
    adaptiveBlend = mix(adaptiveBlend, 1.0, clamp(colorDiff * 4.0, 0.0, 1.0));
    adaptiveBlend = mix(adaptiveBlend, 1.0, clamp(velocityMagnitude * 0.1, 0.0, 1.0));
    
    // Blend current and history
    vec3 blendedYCoCg = mix(clampedHistory, currentYCoCg, adaptiveBlend);
    
    // Convert back to RGB
    vec3 finalColor = yCoCgToRgb(blendedYCoCg);
    
    FragColor = vec4(finalColor, 1.0);
}