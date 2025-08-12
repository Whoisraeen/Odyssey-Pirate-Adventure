package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shader for Screen Space Ambient Occlusion (SSAO).
 */
public class SSAOShader extends Shader {
    // Logger kept for potential debug output
    private static final Logger logger = LoggerFactory.getLogger(SSAOShader.class);
    
    public SSAOShader() throws Exception {
        super();
        createVertexShader(getVertexShaderSource());
        createFragmentShader(getFragmentShaderSource());
        link();
    }
    
    public void setGDepthTexture(int textureUnit) {
        setUniform("u_gDepth", textureUnit);
    }
    
    public void setGNormalTexture(int textureUnit) {
        setUniform("u_gNormal", textureUnit);
    }
    
    public void setNoiseTexture(int textureUnit) {
        setUniform("u_texNoise", textureUnit);
    }
    
    public void setSamples(Vector3f[] samples) {
        for (int i = 0; i < samples.length; i++) {
            setUniform("u_samples[" + i + "]", samples[i]);
        }
    }
    
    public void setProjectionMatrix(Matrix4f projection) {
        setUniform("u_projection", projection);
    }
    
    public void setViewMatrix(Matrix4f view) {
        setUniform("u_view", view);
    }
    
    public void setInverseMatrices(Matrix4f invView, Matrix4f invProjection) {
        setUniform("u_invView", invView);
        setUniform("u_invProjection", invProjection);
    }
    
    public void setRadius(float radius) {
        setUniform("u_radius", radius);
    }
    
    public void setBias(float bias) {
        setUniform("u_bias", bias);
    }
    
    public void setPower(float power) {
        setUniform("u_power", power);
    }
    
    public void setScreenSize(Vector2f screenSize) {
        setUniform("u_screenSize", screenSize);
    }
    
    private String getVertexShaderSource() {
        return """
            #version 330 core
            
            layout (location = 0) in vec2 position;
            layout (location = 1) in vec2 texCoords;
            
            out vec2 TexCoords;
            
            void main() {
                TexCoords = texCoords;
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """;
    }
    
    private String getFragmentShaderSource() {
        return """
            #version 330 core
            
            out float FragColor;
            
            in vec2 TexCoords;
            
            uniform sampler2D u_gDepth;
            uniform sampler2D u_gNormal;
            uniform sampler2D u_texNoise;
            
            uniform vec3 u_samples[64];
            uniform mat4 u_projection;
            uniform mat4 u_view;
            uniform mat4 u_invView;
            uniform mat4 u_invProjection;
            uniform float u_radius;
            uniform float u_bias;
            uniform float u_power;
            uniform vec2 u_screenSize;
            
            // Reconstruct world position from depth
            vec3 reconstructWorldPosition(vec2 texCoord, float depth) {
                vec4 clipSpacePos = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                vec4 viewSpacePos = u_invProjection * clipSpacePos;
                viewSpacePos /= viewSpacePos.w;
                vec4 worldSpacePos = u_invView * viewSpacePos;
                return worldSpacePos.xyz;
            }
            
            // Decode octahedral normal from RG channels
            vec3 decodeOctahedralNormal(vec2 encoded) {
                vec3 n = vec3(encoded.x, encoded.y, 1.0 - abs(encoded.x) - abs(encoded.y));
                if (n.z < 0.0) {
                    n.xy = (1.0 - abs(n.yx)) * sign(n.xy);
                }
                return normalize(n);
            }
            
            void main() {
                float depth = texture(u_gDepth, TexCoords).r;
                vec3 fragPos = reconstructWorldPosition(TexCoords, depth);
                vec4 gNormal = texture(u_gNormal, TexCoords);
                vec3 normal = decodeOctahedralNormal(gNormal.rg);
                vec3 randomVec = normalize(texture(u_texNoise, TexCoords * (u_screenSize / 4.0)).xyz);
                
                // Create TBN change-of-basis matrix: from tangent-space to view-space
                vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
                vec3 bitangent = cross(normal, tangent);
                mat3 TBN = mat3(tangent, bitangent, normal);
                
                float occlusion = 0.0;
                for(int i = 0; i < 64; ++i) {
                    // Get sample position
                    vec3 samplePos = TBN * u_samples[i]; // from tangent to view-space
                    samplePos = fragPos + samplePos * u_radius;
                    
                    // Project sample position (to sample texture) (to get position on screen/texture)
                    vec4 offset = vec4(samplePos, 1.0);
                    offset = u_projection * offset; // from view to clip-space
                    offset.xyz /= offset.w; // perspective divide
                    offset.xyz = offset.xyz * 0.5 + 0.5; // transform to range 0.0 - 1.0
                    
                    // Get sample depth
                    float sampleDepth = texture(u_gDepth, offset.xy).r; // get depth value of kernel sample
                    vec3 sampleWorldPos = reconstructWorldPosition(offset.xy, sampleDepth);
                    
                    // Range check & accumulate
                    float rangeCheck = smoothstep(0.0, 1.0, u_radius / abs(fragPos.z - sampleWorldPos.z));
                    occlusion += (sampleWorldPos.z >= samplePos.z + u_bias ? 1.0 : 0.0) * rangeCheck;
                }
                occlusion = 1.0 - (occlusion / 64.0);
                
                FragColor = pow(occlusion, u_power);
            }
            """;
    }
}