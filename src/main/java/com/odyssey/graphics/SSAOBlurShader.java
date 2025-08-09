package com.odyssey.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shader for blurring SSAO output to reduce noise.
 */
public class SSAOBlurShader extends Shader {
    private static final Logger logger = LoggerFactory.getLogger(SSAOBlurShader.class);
    
    public SSAOBlurShader() throws Exception {
        super();
        createVertexShader(getVertexShaderSource());
        createFragmentShader(getFragmentShaderSource());
        link();
    }
    
    public void setSSAOTexture(int textureUnit) {
        setUniform("u_ssaoInput", textureUnit);
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
            
            uniform sampler2D u_ssaoInput;
            
            void main() {
                vec2 texelSize = 1.0 / vec2(textureSize(u_ssaoInput, 0));
                float result = 0.0;
                for (int x = -2; x < 2; ++x) {
                    for (int y = -2; y < 2; ++y) {
                        vec2 offset = vec2(float(x), float(y)) * texelSize;
                        result += texture(u_ssaoInput, TexCoords + offset).r;
                    }
                }
                FragColor = result / (4.0 * 4.0);
            }
            """;
    }
}