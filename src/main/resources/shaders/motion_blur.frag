#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_ColorTexture;
uniform sampler2D u_VelocityTexture;
uniform sampler2D u_DepthTexture;
uniform float u_MotionBlurStrength;
uniform int u_MotionBlurSamples;
uniform float u_MaxBlurRadius;
uniform vec2 u_ScreenSize;

void main() {
    vec2 texelSize = 1.0 / u_ScreenSize;
    
    // Sample velocity
    vec2 velocity = texture(u_VelocityTexture, TexCoord).xy;
    
    // Scale velocity by strength and clamp to max radius
    velocity *= u_MotionBlurStrength;
    float velocityLength = length(velocity);
    if (velocityLength > u_MaxBlurRadius) {
        velocity = normalize(velocity) * u_MaxBlurRadius;
    }
    
    // If velocity is too small, no blur needed
    if (velocityLength < 0.5) {
        FragColor = texture(u_ColorTexture, TexCoord);
        return;
    }
    
    vec3 color = vec3(0.0);
    float totalWeight = 0.0;
    
    // Sample along motion vector
    int samples = u_MotionBlurSamples;
    for (int i = 0; i < samples; i++) {
        float t = (float(i) / float(samples - 1)) - 0.5; // -0.5 to 0.5
        vec2 sampleCoord = TexCoord + velocity * t * texelSize;
        
        // Check bounds
        if (sampleCoord.x >= 0.0 && sampleCoord.x <= 1.0 && 
            sampleCoord.y >= 0.0 && sampleCoord.y <= 1.0) {
            
            vec3 sampleColor = texture(u_ColorTexture, sampleCoord).rgb;
            
            // Weight samples towards center
            float weight = 1.0 - abs(t * 2.0);
            weight = weight * weight; // Quadratic falloff
            
            color += sampleColor * weight;
            totalWeight += weight;
        }
    }
    
    if (totalWeight > 0.0) {
        color /= totalWeight;
    } else {
        color = texture(u_ColorTexture, TexCoord).rgb;
    }
    
    FragColor = vec4(color, 1.0);
}