#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_InputTexture;
uniform vec2 u_Direction;
uniform vec2 u_TexelSize;
uniform float u_BlurRadius;

// Gaussian blur weights for 9-tap filter
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(u_InputTexture, TexCoord).rgb * weights[0];
    
    for (int i = 1; i < 5; ++i) {
        vec2 offset = u_Direction * u_TexelSize * float(i) * u_BlurRadius;
        result += texture(u_InputTexture, TexCoord + offset).rgb * weights[i];
        result += texture(u_InputTexture, TexCoord - offset).rgb * weights[i];
    }
    
    FragColor = vec4(result, 1.0);
}