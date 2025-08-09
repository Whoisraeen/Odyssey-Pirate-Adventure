package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporal Anti-Aliasing (TAA) shader for reducing aliasing artifacts.
 * Uses reprojection and neighborhood clamping for stable results.
 */
public class TAAShader {
    private static final Logger logger = LoggerFactory.getLogger(TAAShader.class);
    
    private Shader shader;
    
    public void initialize() throws Exception {
        shader = new Shader();
        shader.createVertexShader(createVertexShader());
        shader.createFragmentShader(createFragmentShader());
        shader.link();
        
        // Create uniforms
        shader.createUniform("u_currentTexture");
        shader.createUniform("u_historyTexture");
        shader.createUniform("u_velocityTexture");
        shader.createUniform("u_depthTexture");
        shader.createUniform("u_previousViewProjectionMatrix");
        shader.createUniform("u_currentViewProjectionMatrix");
        shader.createUniform("u_invCurrentViewProjectionMatrix");
        shader.createUniform("u_screenSize");
        shader.createUniform("u_frameIndex");
        shader.createUniform("u_blendFactor");
        shader.createUniform("u_velocityScale");
        
        logger.info("TAA shader initialized successfully");
    }
    
    public void bind() {
        shader.bind();
    }
    
    public void unbind() {
        shader.unbind();
    }
    
    public void setCurrentTexture(int textureUnit) {
        shader.setUniform("u_currentTexture", textureUnit);
    }
    
    public void setHistoryTexture(int textureUnit) {
        shader.setUniform("u_historyTexture", textureUnit);
    }
    
    public void setVelocityTexture(int textureUnit) {
        shader.setUniform("u_velocityTexture", textureUnit);
    }
    
    public void setDepthTexture(int textureUnit) {
        shader.setUniform("u_depthTexture", textureUnit);
    }
    
    public void setPreviousViewProjectionMatrix(Matrix4f matrix) {
        shader.setUniform("u_previousViewProjectionMatrix", matrix);
    }
    
    public void setCurrentViewProjectionMatrix(Matrix4f matrix) {
        shader.setUniform("u_currentViewProjectionMatrix", matrix);
    }
    
    public void setInverseCurrentViewProjectionMatrix(Matrix4f matrix) {
        shader.setUniform("u_invCurrentViewProjectionMatrix", matrix);
    }
    
    public void setScreenSize(Vector2f screenSize) {
        shader.setUniform("u_screenSize", screenSize);
    }
    
    public void setFrameIndex(int frameIndex) {
        shader.setUniform("u_frameIndex", frameIndex);
    }
    
    public void setBlendFactor(float blendFactor) {
        shader.setUniform("u_blendFactor", blendFactor);
    }
    
    public void setVelocityScale(float velocityScale) {
        shader.setUniform("u_velocityScale", velocityScale);
    }
    
    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
    }
    
    private String createVertexShader() {
        return """
            #version 330 core
            
            layout (location = 0) in vec2 a_position;
            layout (location = 1) in vec2 a_texCoord;
            
            out vec2 v_texCoord;
            
            void main() {
                v_texCoord = a_texCoord;
                gl_Position = vec4(a_position, 0.0, 1.0);
            }
            """;
    }
    
    private String createFragmentShader() {
        return """
            #version 330 core
            
            in vec2 v_texCoord;
            out vec4 fragColor;
            
            uniform sampler2D u_currentTexture;
            uniform sampler2D u_historyTexture;
            uniform sampler2D u_velocityTexture;
            uniform sampler2D u_depthTexture;
            
            uniform mat4 u_previousViewProjectionMatrix;
            uniform mat4 u_currentViewProjectionMatrix;
            uniform mat4 u_invCurrentViewProjectionMatrix;
            uniform vec2 u_screenSize;
            uniform int u_frameIndex;
            uniform float u_blendFactor;
            uniform float u_velocityScale;
            
            // Convert depth to world position
            vec3 reconstructWorldPosition(vec2 texCoord, float depth) {
                vec4 clipSpacePos = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                vec4 worldPos = u_invCurrentViewProjectionMatrix * clipSpacePos;
                return worldPos.xyz / worldPos.w;
            }
            
            // Sample with Catmull-Rom filtering
            vec4 sampleCatmullRom(sampler2D tex, vec2 uv) {
                vec2 texelSize = 1.0 / textureSize(tex, 0);
                vec2 samplePos = uv / texelSize;
                vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
                
                vec2 f = samplePos - texPos1;
                vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
                vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
                vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
                vec2 w3 = f * f * (-0.5 + 0.5 * f);
                
                vec2 w12 = w1 + w2;
                vec2 offset12 = w2 / (w1 + w2);
                
                vec2 texPos0 = texPos1 - 1.0;
                vec2 texPos3 = texPos1 + 2.0;
                vec2 texPos12 = texPos1 + offset12;
                
                texPos0 *= texelSize;
                texPos3 *= texelSize;
                texPos12 *= texelSize;
                
                vec4 result = vec4(0.0);
                result += texture(tex, vec2(texPos0.x, texPos0.y)) * w0.x * w0.y;
                result += texture(tex, vec2(texPos12.x, texPos0.y)) * w12.x * w0.y;
                result += texture(tex, vec2(texPos3.x, texPos0.y)) * w3.x * w0.y;
                
                result += texture(tex, vec2(texPos0.x, texPos12.y)) * w0.x * w12.y;
                result += texture(tex, vec2(texPos12.x, texPos12.y)) * w12.x * w12.y;
                result += texture(tex, vec2(texPos3.x, texPos12.y)) * w3.x * w12.y;
                
                result += texture(tex, vec2(texPos0.x, texPos3.y)) * w0.x * w3.y;
                result += texture(tex, vec2(texPos12.x, texPos3.y)) * w12.x * w3.y;
                result += texture(tex, vec2(texPos3.x, texPos3.y)) * w3.x * w3.y;
                
                return result;
            }
            
            // Neighborhood clamping for temporal stability
            vec4 clipAABB(vec4 aabbMin, vec4 aabbMax, vec4 prevSample) {
                vec4 center = 0.5 * (aabbMax + aabbMin);
                vec4 extents = 0.5 * (aabbMax - aabbMin);
                
                vec4 offset = prevSample - center;
                vec4 ts = abs(extents) / max(abs(offset), vec4(1e-6));
                float t = min(min(ts.x, ts.y), min(ts.z, ts.w));
                
                return center + offset * clamp(t, 0.0, 1.0);
            }
            
            void main() {
                vec2 texelSize = 1.0 / u_screenSize;
                
                // Sample current frame
                vec4 currentColor = texture(u_currentTexture, v_texCoord);
                
                // Get velocity from G-buffer
                vec2 velocity = texture(u_velocityTexture, v_texCoord).xy * u_velocityScale;
                
                // Calculate reprojected UV
                vec2 prevUV = v_texCoord - velocity;
                
                // Check if reprojected UV is valid
                if (prevUV.x < 0.0 || prevUV.x > 1.0 || prevUV.y < 0.0 || prevUV.y > 1.0) {
                    // Disocclusion - use current frame
                    fragColor = currentColor;
                    return;
                }
                
                // Sample history with Catmull-Rom filtering
                vec4 historyColor = sampleCatmullRom(u_historyTexture, prevUV);
                
                // Neighborhood clamping
                vec4 colorMin = vec4(1e6);
                vec4 colorMax = vec4(-1e6);
                vec4 colorSum = vec4(0.0);
                
                // 3x3 neighborhood
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        vec2 offset = vec2(float(x), float(y)) * texelSize;
                        vec4 neighborColor = texture(u_currentTexture, v_texCoord + offset);
                        
                        colorMin = min(colorMin, neighborColor);
                        colorMax = max(colorMax, neighborColor);
                        colorSum += neighborColor;
                    }
                }
                
                // Clamp history to neighborhood
                historyColor = clipAABB(colorMin, colorMax, historyColor);
                
                // Adaptive blend factor based on velocity magnitude
                float velocityMagnitude = length(velocity);
                float adaptiveBlend = mix(u_blendFactor, 1.0, clamp(velocityMagnitude * 10.0, 0.0, 1.0));
                
                // Temporal accumulation
                vec4 finalColor = mix(historyColor, currentColor, adaptiveBlend);
                
                fragColor = finalColor;
            }
            """;
    }
}