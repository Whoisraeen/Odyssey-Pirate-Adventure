#version 330 core

out vec4 FragColor;

in vec3 FragPos;

uniform sampler2D depthMap;
uniform mat4 projection;
uniform mat4 view;

uniform vec3 fogColor;
uniform float fogDensity;

void main()
{    
    float depth = texture(depthMap, gl_FragCoord.xy / textureSize(depthMap, 0)).r;
    vec4 worldPos = inverse(projection * view) * vec4(FragPos.xy, depth, 1.0);
    worldPos /= worldPos.w;

    float dist = length(worldPos.xyz - inverse(view)[3].xyz);
    float fogAmount = 1.0 - exp(-dist * fogDensity);

    FragColor = vec4(fogColor, fogAmount);
}