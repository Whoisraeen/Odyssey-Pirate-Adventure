#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D u_SceneTexture;
uniform sampler2D u_BloomTexture0;
uniform sampler2D u_BloomTexture1;
uniform sampler2D u_BloomTexture2;
uniform sampler2D u_BloomTexture3;
uniform sampler2D u_BloomTexture4;
uniform sampler2D u_BloomTexture5;
uniform sampler2D u_BloomTexture6;
uniform sampler2D u_BloomTexture7;

uniform float u_BloomIntensity;
uniform float u_BloomRadius;
uniform int u_BloomLevels;

void main() {
    vec3 sceneColor = texture(u_SceneTexture, TexCoord).rgb;
    vec3 bloomColor = vec3(0.0);
    
    // Combine multiple bloom mip levels with different weights
    if (u_BloomLevels > 0) bloomColor += texture(u_BloomTexture0, TexCoord).rgb * 1.0;
    if (u_BloomLevels > 1) bloomColor += texture(u_BloomTexture1, TexCoord).rgb * 0.8;
    if (u_BloomLevels > 2) bloomColor += texture(u_BloomTexture2, TexCoord).rgb * 0.6;
    if (u_BloomLevels > 3) bloomColor += texture(u_BloomTexture3, TexCoord).rgb * 0.4;
    if (u_BloomLevels > 4) bloomColor += texture(u_BloomTexture4, TexCoord).rgb * 0.3;
    if (u_BloomLevels > 5) bloomColor += texture(u_BloomTexture5, TexCoord).rgb * 0.2;
    if (u_BloomLevels > 6) bloomColor += texture(u_BloomTexture6, TexCoord).rgb * 0.1;
    if (u_BloomLevels > 7) bloomColor += texture(u_BloomTexture7, TexCoord).rgb * 0.05;
    
    // Normalize bloom contribution
    bloomColor /= float(u_BloomLevels);
    
    // Combine scene and bloom
    vec3 finalColor = sceneColor + bloomColor * u_BloomIntensity;
    
    FragColor = vec4(finalColor, 1.0);
}