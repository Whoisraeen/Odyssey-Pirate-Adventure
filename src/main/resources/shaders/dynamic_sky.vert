#version 330 core
layout (location = 0) in vec3 aPos;

uniform mat4 projection;
uniform mat4 view;

out vec3 FragPos;

void main()
{
    FragPos = aPos;
    gl_Position = vec4(aPos, 1.0);
}