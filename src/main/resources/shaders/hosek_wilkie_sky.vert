#version 330 core

layout (location = 0) in vec3 a_Position;

uniform mat4 u_ViewMatrix;
uniform mat4 u_ProjectionMatrix;

out vec3 v_WorldDirection;
out vec3 v_ViewDirection;

void main() {
    // Pass world direction for atmospheric calculations
    v_WorldDirection = normalize(a_Position);
    
    // Calculate view direction for scattering
    vec4 viewPos = u_ViewMatrix * vec4(a_Position, 0.0);
    v_ViewDirection = normalize(viewPos.xyz);
    
    // Transform to clip space, ensuring skybox is always at far plane
    vec4 pos = u_ProjectionMatrix * u_ViewMatrix * vec4(a_Position, 1.0);
    gl_Position = pos.xyww;
}