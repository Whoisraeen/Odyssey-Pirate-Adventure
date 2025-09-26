package com.odyssey.rendering;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.odyssey.util.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * Image-Based Lighting (IBL) implementation for physically-based rendering.
 * Handles HDR environment map processing, irradiance convolution, and BRDF lookup table generation.
 */
public class IBL {
    private static final Logger logger = Logger.getLogger(IBL.class);
    
    private int environmentMap;
    private int irradianceMap;
    private int prefilterMap;
    private int brdfLUTTexture;
    
    // Shader programs for IBL processing
    private int equirectangularToCubemapShader;
    private int irradianceShader;
    private int prefilterShader;
    private int brdfShader;
    
    // Framebuffer and renderbuffer for processing
    private int captureFBO;
    private int captureRBO;
    
    // Cube geometry for rendering
    private int cubeVAO;
    private int quadVAO;
    
    /**
     * Initialize IBL system with HDR environment map processing.
     */
    public IBL() {
        logger.info("Initializing Image-Based Lighting system...");
        
        setupFramebuffer();
        setupGeometry();
        createShaders();
        generateIBLTextures();
        
        logger.info("IBL system initialized successfully");
    }
    
    /**
     * Set up framebuffer for IBL processing.
     */
    private void setupFramebuffer() {
        // Create framebuffer for capturing cubemap faces
        captureFBO = GL30.glGenFramebuffers();
        captureRBO = GL30.glGenRenderbuffers();
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFBO);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, captureRBO);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, 512, 512);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, captureRBO);
    }
    
    /**
     * Set up cube and quad geometry for rendering.
     */
    private void setupGeometry() {
        // Cube vertices for skybox rendering
        float[] cubeVertices = {
            // positions          
            -1.0f,  1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            -1.0f,  1.0f, -1.0f,
             1.0f,  1.0f, -1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
             1.0f, -1.0f,  1.0f
        };
        
        // Create cube VAO
        cubeVAO = GL30.glGenVertexArrays();
        int cubeVBO = GL30.glGenBuffers();
        
        GL30.glBindVertexArray(cubeVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, cubeVBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, cubeVertices, GL30.GL_STATIC_DRAW);
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        
        // Quad vertices for BRDF LUT generation
        float[] quadVertices = {
            // positions   // texCoords
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
        };
        
        int[] quadIndices = {0, 1, 2, 0, 2, 3};
        
        // Create quad VAO
        quadVAO = GL30.glGenVertexArrays();
        int quadVBO = GL30.glGenBuffers();
        int quadEBO = GL30.glGenBuffers();
        
        GL30.glBindVertexArray(quadVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, quadVBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, quadVertices, GL30.GL_STATIC_DRAW);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL30.GL_STATIC_DRAW);
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL30.glEnableVertexAttribArray(1);
        GL30.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    }
    
    /**
     * Create shader programs for IBL processing.
     */
    private void createShaders() {
        // Create equirectangular to cubemap shader
        String equirectVertSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            
            out vec3 WorldPos;
            
            uniform mat4 projection;
            uniform mat4 view;
            
            void main() {
                WorldPos = aPos;
                gl_Position = projection * view * vec4(WorldPos, 1.0);
            }
            """;
            
        String equirectFragSource = """
            #version 330 core
            out vec4 FragColor;
            in vec3 WorldPos;
            
            uniform sampler2D equirectangularMap;
            
            const vec2 invAtan = vec2(0.1591, 0.3183);
            vec2 SampleSphericalMap(vec3 v) {
                vec2 uv = vec2(atan(v.z, v.x), asin(v.y));
                uv *= invAtan;
                uv += 0.5;
                return uv;
            }
            
            void main() {
                vec2 uv = SampleSphericalMap(normalize(WorldPos));
                vec3 color = texture(equirectangularMap, uv).rgb;
                FragColor = vec4(color, 1.0);
            }
            """;
            
        equirectangularToCubemapShader = createShaderProgram(equirectVertSource, equirectFragSource);
        
        // Create irradiance convolution shader
        String irradianceFragSource = """
            #version 330 core
            out vec4 FragColor;
            in vec3 WorldPos;
            
            uniform samplerCube environmentMap;
            
            const float PI = 3.14159265359;
            
            void main() {
                vec3 N = normalize(WorldPos);
                vec3 irradiance = vec3(0.0);
                
                vec3 up    = vec3(0.0, 1.0, 0.0);
                vec3 right = normalize(cross(up, N));
                up         = normalize(cross(N, right));
                
                float sampleDelta = 0.025;
                float nrSamples = 0.0;
                for(float phi = 0.0; phi < 2.0 * PI; phi += sampleDelta) {
                    for(float theta = 0.0; theta < 0.5 * PI; theta += sampleDelta) {
                        vec3 tangentSample = vec3(sin(theta) * cos(phi),  sin(theta) * sin(phi), cos(theta));
                        vec3 sampleVec = tangentSample.x * right + tangentSample.y * up + tangentSample.z * N;
                        
                        irradiance += texture(environmentMap, sampleVec).rgb * cos(theta) * sin(theta);
                        nrSamples++;
                    }
                }
                irradiance = PI * irradiance * (1.0 / float(nrSamples));
                
                FragColor = vec4(irradiance, 1.0);
            }
            """;
            
        irradianceShader = createShaderProgram(equirectVertSource, irradianceFragSource);
        
        // Create prefilter shader
        String prefilterFragSource = """
            #version 330 core
            out vec4 FragColor;
            in vec3 WorldPos;
            
            uniform samplerCube environmentMap;
            uniform float roughness;
            
            const float PI = 3.14159265359;
            
            float DistributionGGX(vec3 N, vec3 H, float roughness) {
                float a = roughness*roughness;
                float a2 = a*a;
                float NdotH = max(dot(N, H), 0.0);
                float NdotH2 = NdotH*NdotH;
                
                float num = a2;
                float denom = (NdotH2 * (a2 - 1.0) + 1.0);
                denom = PI * denom * denom;
                
                return num / denom;
            }
            
            float RadicalInverse_VdC(uint bits) {
                bits = (bits << 16u) | (bits >> 16u);
                bits = ((bits & 0x55555555u) << 1u) | ((bits & 0xAAAAAAAAu) >> 1u);
                bits = ((bits & 0x33333333u) << 2u) | ((bits & 0xCCCCCCCCu) >> 2u);
                bits = ((bits & 0x0F0F0F0Fu) << 4u) | ((bits & 0xF0F0F0F0u) >> 4u);
                bits = ((bits & 0x00FF00FFu) << 8u) | ((bits & 0xFF00FF00u) >> 8u);
                return float(bits) * 2.3283064365386963e-10;
            }
            
            vec2 Hammersley(uint i, uint N) {
                return vec2(float(i)/float(N), RadicalInverse_VdC(i));
            }
            
            vec3 ImportanceSampleGGX(vec2 Xi, vec3 N, float roughness) {
                float a = roughness*roughness;
                
                float phi = 2.0 * PI * Xi.x;
                float cosTheta = sqrt((1.0 - Xi.y) / (1.0 + (a*a - 1.0) * Xi.y));
                float sinTheta = sqrt(1.0 - cosTheta*cosTheta);
                
                vec3 H;
                H.x = cos(phi) * sinTheta;
                H.y = sin(phi) * sinTheta;
                H.z = cosTheta;
                
                vec3 up        = abs(N.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
                vec3 tangent   = normalize(cross(up, N));
                vec3 bitangent = cross(N, tangent);
                
                vec3 sampleVec = tangent * H.x + bitangent * H.y + N * H.z;
                return normalize(sampleVec);
            }
            
            void main() {
                vec3 N = normalize(WorldPos);
                vec3 R = N;
                vec3 V = R;
                
                const uint SAMPLE_COUNT = 1024u;
                vec3 prefilteredColor = vec3(0.0);
                float totalWeight = 0.0;
                
                for(uint i = 0u; i < SAMPLE_COUNT; ++i) {
                    vec2 Xi = Hammersley(i, SAMPLE_COUNT);
                    vec3 H = ImportanceSampleGGX(Xi, N, roughness);
                    vec3 L = normalize(2.0 * dot(V, H) * H - V);
                    
                    float NdotL = max(dot(N, L), 0.0);
                    if(NdotL > 0.0) {
                        float D = DistributionGGX(N, H, roughness);
                        float NdotH = max(dot(N, H), 0.0);
                        float HdotV = max(dot(H, V), 0.0);
                        float pdf = D * NdotH / (4.0 * HdotV) + 0.0001;
                        
                        float resolution = 512.0;
                        float saTexel = 4.0 * PI / (6.0 * resolution * resolution);
                        float saSample = 1.0 / (float(SAMPLE_COUNT) * pdf + 0.0001);
                        
                        float mipLevel = roughness == 0.0 ? 0.0 : 0.5 * log2(saSample / saTexel);
                        
                        prefilteredColor += textureLod(environmentMap, L, mipLevel).rgb * NdotL;
                        totalWeight += NdotL;
                    }
                }
                
                prefilteredColor = prefilteredColor / totalWeight;
                FragColor = vec4(prefilteredColor, 1.0);
            }
            """;
            
        prefilterShader = createShaderProgram(equirectVertSource, prefilterFragSource);
        
        // Create BRDF LUT shader
        String brdfVertSource = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec2 aTexCoords;
            
            out vec2 TexCoords;
            
            void main() {
                TexCoords = aTexCoords;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
            """;
            
        String brdfFragSource = """
            #version 330 core
            out vec2 FragColor;
            in vec2 TexCoords;
            
            const float PI = 3.14159265359;
            
            float RadicalInverse_VdC(uint bits) {
                bits = (bits << 16u) | (bits >> 16u);
                bits = ((bits & 0x55555555u) << 1u) | ((bits & 0xAAAAAAAAu) >> 1u);
                bits = ((bits & 0x33333333u) << 2u) | ((bits & 0xCCCCCCCCu) >> 2u);
                bits = ((bits & 0x0F0F0F0Fu) << 4u) | ((bits & 0xF0F0F0F0u) >> 4u);
                bits = ((bits & 0x00FF00FFu) << 8u) | ((bits & 0xFF00FF00u) >> 8u);
                return float(bits) * 2.3283064365386963e-10;
            }
            
            vec2 Hammersley(uint i, uint N) {
                return vec2(float(i)/float(N), RadicalInverse_VdC(i));
            }
            
            vec3 ImportanceSampleGGX(vec2 Xi, vec3 N, float roughness) {
                float a = roughness*roughness;
                
                float phi = 2.0 * PI * Xi.x;
                float cosTheta = sqrt((1.0 - Xi.y) / (1.0 + (a*a - 1.0) * Xi.y));
                float sinTheta = sqrt(1.0 - cosTheta*cosTheta);
                
                vec3 H;
                H.x = cos(phi) * sinTheta;
                H.y = sin(phi) * sinTheta;
                H.z = cosTheta;
                
                return H;
            }
            
            float GeometrySchlickGGX(float NdotV, float roughness) {
                float a = roughness;
                float k = (a * a) / 2.0;
                
                float num = NdotV;
                float denom = NdotV * (1.0 - k) + k;
                
                return num / denom;
            }
            
            float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
                float NdotV = max(dot(N, V), 0.0);
                float NdotL = max(dot(N, L), 0.0);
                float ggx2 = GeometrySchlickGGX(NdotV, roughness);
                float ggx1 = GeometrySchlickGGX(NdotL, roughness);
                
                return ggx1 * ggx2;
            }
            
            vec2 IntegrateBRDF(float NdotV, float roughness) {
                vec3 V;
                V.x = sqrt(1.0 - NdotV*NdotV);
                V.y = 0.0;
                V.z = NdotV;
                
                float A = 0.0;
                float B = 0.0;
                
                vec3 N = vec3(0.0, 0.0, 1.0);
                
                const uint SAMPLE_COUNT = 1024u;
                for(uint i = 0u; i < SAMPLE_COUNT; ++i) {
                    vec2 Xi = Hammersley(i, SAMPLE_COUNT);
                    vec3 H = ImportanceSampleGGX(Xi, N, roughness);
                    vec3 L = normalize(2.0 * dot(V, H) * H - V);
                    
                    float NdotL = max(L.z, 0.0);
                    float NdotH = max(H.z, 0.0);
                    float VdotH = max(dot(V, H), 0.0);
                    
                    if(NdotL > 0.0) {
                        float G = GeometrySmith(N, V, L, roughness);
                        float G_Vis = (G * VdotH) / (NdotH * NdotV);
                        float Fc = pow(1.0 - VdotH, 5.0);
                        
                        A += (1.0 - Fc) * G_Vis;
                        B += Fc * G_Vis;
                    }
                }
                A /= float(SAMPLE_COUNT);
                B /= float(SAMPLE_COUNT);
                return vec2(A, B);
            }
            
            void main() {
                vec2 integratedBRDF = IntegrateBRDF(TexCoords.x, TexCoords.y);
                FragColor = integratedBRDF;
            }
            """;
            
        brdfShader = createShaderProgram(brdfVertSource, brdfFragSource);
    }
    
    /**
     * Generate IBL textures from HDR environment map.
     */
    private void generateIBLTextures() {
        // Create environment cubemap
        environmentMap = GL11.glGenTextures();
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, environmentMap);
        for (int i = 0; i < 6; ++i) {
            GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL30.GL_RGB16F, 512, 512, 0, GL11.GL_RGB, GL11.GL_FLOAT, 0);
        }
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL32.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Create irradiance map
        irradianceMap = GL11.glGenTextures();
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, irradianceMap);
        for (int i = 0; i < 6; ++i) {
            GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL30.GL_RGB16F, 32, 32, 0, GL11.GL_RGB, GL11.GL_FLOAT, 0);
        }
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL32.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Create prefilter map
        prefilterMap = GL11.glGenTextures();
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, prefilterMap);
        for (int i = 0; i < 6; ++i) {
            GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL30.GL_RGB16F, 128, 128, 0, GL11.GL_RGB, GL11.GL_FLOAT, 0);
        }
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL32.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glGenerateMipmap(GL13.GL_TEXTURE_CUBE_MAP);
        
        // Create BRDF LUT
        brdfLUTTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, brdfLUTTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RG16F, 512, 512, 0, GL30.GL_RG, GL11.GL_FLOAT, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Generate BRDF LUT
        generateBRDFLUT();
        
        logger.info("IBL textures generated successfully");
    }
    
    /**
     * Generate BRDF lookup table.
     */
    private void generateBRDFLUT() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFBO);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, captureRBO);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, 512, 512);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, brdfLUTTexture, 0);
        
        GL11.glViewport(0, 0, 512, 512);
        GL20.glUseProgram(brdfShader);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Create a shader program from vertex and fragment source code.
     */
    private int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);
        
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);
        
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        return program;
    }

    public int getIrradianceMap() {
        return irradianceMap;
    }

    public int getPrefilterMap() {
        return prefilterMap;
    }

    public int getBrdfLUTTexture() {
        return brdfLUTTexture;
    }
    
    public int getEnvironmentMap() {
        return environmentMap;
    }
    
    /**
     * Clean up IBL resources.
     */
    public void cleanup() {
        GL11.glDeleteTextures(environmentMap);
        GL11.glDeleteTextures(irradianceMap);
        GL11.glDeleteTextures(prefilterMap);
        GL11.glDeleteTextures(brdfLUTTexture);
        
        GL20.glDeleteProgram(equirectangularToCubemapShader);
        GL20.glDeleteProgram(irradianceShader);
        GL20.glDeleteProgram(prefilterShader);
        GL20.glDeleteProgram(brdfShader);
        
        GL30.glDeleteFramebuffers(captureFBO);
        GL30.glDeleteRenderbuffers(captureRBO);
        GL30.glDeleteVertexArrays(cubeVAO);
        GL30.glDeleteVertexArrays(quadVAO);
    }
}
