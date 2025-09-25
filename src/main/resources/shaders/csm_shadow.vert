#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;

uniform mat4 u_ModelMatrix;
uniform mat4 u_LightSpaceMatrix;

void main() {
    // Transform vertex position to light space
    gl_Position = u_LightSpaceMatrix * u_ModelMatrix * vec4(aPos, 1.0);
}