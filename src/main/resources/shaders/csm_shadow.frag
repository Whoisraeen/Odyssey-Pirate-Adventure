#version 330 core

// No output needed - depth is automatically written to depth buffer
// This shader is used for shadow map generation

void main() {
    // Depth value is automatically written to gl_FragDepth
    // We can optionally modify it here if needed for special effects
    
    // For standard shadow mapping, we don't need to do anything
    // The depth buffer will contain the distance from light to fragment
}