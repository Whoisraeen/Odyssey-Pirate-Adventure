package com.odyssey.rendering;

import com.odyssey.util.Logger;
import com.odyssey.world.Block;
import com.odyssey.world.Chunk;
import com.odyssey.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Ambient Occlusion Renderer for The Odyssey.
 * Provides screen-space ambient occlusion (SSAO) and voxel-based ambient occlusion
 * to enhance depth perception and visual quality in the voxel world.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public class AmbientOcclusionRenderer {
    
    private static final Logger logger = Logger.getLogger(AmbientOcclusionRenderer.class);
    
    // SSAO parameters
    private static final int SSAO_KERNEL_SIZE = 128;
    private static final int SSAO_NOISE_SIZE = 4;
    private static final float SSAO_RADIUS = 0.5f;
    private static final float SSAO_BIAS = 0.025f;
    private static final float SSAO_POWER = 2.0f;
    
    // Rendering components
    private Shader gBufferShader;
    private Shader ssaoShader;
    private Shader ssaoBlurShader;
    private Shader voxelAOShader;
    
    // Framebuffers and textures
    private Framebuffer gBuffer;
    private Framebuffer ssaoBuffer;
    private Framebuffer ssaoBlurBuffer;
    private Texture noiseTexture;
    
    // SSAO data
    private FloatBuffer ssaoKernel;
    private FloatBuffer ssaoNoise;
    
    // Voxel AO cache
    private Map<Vector3i, Float> voxelAOCache;
    private int maxCacheSize = 10000;
    
    // Settings
    private boolean enableSSAO = true;
    private boolean enableVoxelAO = true;
    private float ssaoIntensity = 1.0f;
    private float voxelAOIntensity = 0.8f;
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    
    // World reference
    private World world;
    
    // Random number generator for SSAO
    private Random random;
    
    /**
     * Creates a new AmbientOcclusionRenderer.
     */
    public AmbientOcclusionRenderer() {
        this.voxelAOCache = new HashMap<>();
        this.random = new Random();
    }
    
    /**
     * Initializes the ambient occlusion renderer.
     * 
     * @param world The world reference for voxel AO calculations
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     */
    public void initialize(World world, int screenWidth, int screenHeight) {
        this.world = world;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        logger.info("Initializing ambient occlusion renderer...");
        
        // Load shaders
        loadShaders();
        
        // Create framebuffers
        createFramebuffers();
        
        // Generate SSAO data
        generateSSAOKernel();
        generateSSAONoise();
        
        logger.info("Ambient occlusion renderer initialized successfully");
    }
    
    /**
     * Loads ambient occlusion shaders.
     */
    private void loadShaders() {
        try {
            gBufferShader = new Shader("gBuffer");
            gBufferShader.compileVertexShader(getGBufferVertexShader());
            gBufferShader.compileFragmentShader(getGBufferFragmentShader());
            gBufferShader.link();

            // SSAO shader
            ssaoShader = new Shader("SSAO");
            ssaoShader.compileVertexShader(getSSAOVertexShader());
            ssaoShader.compileFragmentShader(getSSAOFragmentShader());
            ssaoShader.link();
            
            // SSAO blur shader
            ssaoBlurShader = new Shader("SSAO Blur");
            ssaoBlurShader.compileVertexShader(getSSAOBlurVertexShader());
            ssaoBlurShader.compileFragmentShader(getSSAOBlurFragmentShader());
            ssaoBlurShader.link();
            
            // Voxel AO shader
            voxelAOShader = new Shader("Voxel AO");
            voxelAOShader.compileVertexShader(getVoxelAOVertexShader());
            voxelAOShader.compileFragmentShader(getVoxelAOFragmentShader());
            voxelAOShader.link();
            
            logger.debug("Ambient occlusion shaders loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load ambient occlusion shaders", e);
        }
    }
    
    /**
     * Creates framebuffers for ambient occlusion rendering.
     */
    private void createFramebuffers() {
        try {
            // G-Buffer for position and normal data
            gBuffer = new Framebuffer.Builder()
                .name("G-Buffer")
                .size(screenWidth, screenHeight)
                .addColorAttachment(new Framebuffer.ColorAttachment(GL_RGB, GL_RGB16F, GL_FLOAT))
                .addColorAttachment(new Framebuffer.ColorAttachment(GL_RGB, GL_RGB16F, GL_FLOAT))
                .depthTexture()
                .build();
            
            // SSAO buffer
            ssaoBuffer = new Framebuffer.Builder()
                .name("SSAO Buffer")
                .size(screenWidth, screenHeight)
                .addColorAttachment(new Framebuffer.ColorAttachment(GL_RED, GL_R8, GL_UNSIGNED_BYTE))
                .build();
            
            // SSAO blur buffer
            ssaoBlurBuffer = new Framebuffer.Builder()
                .name("SSAO Blur Buffer")
                .size(screenWidth, screenHeight)
                .addColorAttachment(new Framebuffer.ColorAttachment(GL_RED, GL_R8, GL_UNSIGNED_BYTE))
                .build();
            
            logger.debug("Ambient occlusion framebuffers created successfully");
        } catch (Exception e) {
            logger.error("Failed to create ambient occlusion framebuffers", e);
        }
    }
    
    /**
     * Generates the SSAO kernel for sampling.
     */
    private void generateSSAOKernel() {
        ssaoKernel = BufferUtils.createFloatBuffer(SSAO_KERNEL_SIZE * 3);

        for (int i = 0; i < SSAO_KERNEL_SIZE; i++) {
            // Generate random hemisphere sample
            Vector3f sample = new Vector3f(
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat()
            );
            sample.normalize();
            sample.mul(random.nextFloat());

            // Scale samples to be more distributed close to origin
            float scale = (float) i / SSAO_KERNEL_SIZE;
            scale = lerp(0.1f, 1.0f, scale * scale);
            sample.mul(scale);

            ssaoKernel.put(sample.x);
            ssaoKernel.put(sample.y);
            ssaoKernel.put(sample.z);
        }

        ssaoKernel.flip();
        logger.debug("Generated SSAO kernel with {} samples", SSAO_KERNEL_SIZE);
    }
    
    /**
     * Generates noise texture for SSAO.
     */
    private void generateSSAONoise() {
        ssaoNoise = BufferUtils.createFloatBuffer(SSAO_NOISE_SIZE * SSAO_NOISE_SIZE * 3);
        
        for (int i = 0; i < SSAO_NOISE_SIZE * SSAO_NOISE_SIZE; i++) {
            Vector3f noise = new Vector3f(
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat() * 2.0f - 1.0f,
                0.0f
            );
            
            ssaoNoise.put(noise.x);
            ssaoNoise.put(noise.y);
            ssaoNoise.put(noise.z);
        }
        
        ssaoNoise.flip();
        
        // Convert FloatBuffer to ByteBuffer for texture data
        ByteBuffer noiseData = BufferUtils.createByteBuffer(ssaoNoise.remaining() * 4);
        while (ssaoNoise.hasRemaining()) {
            noiseData.putFloat(ssaoNoise.get());
        }
        noiseData.flip();
        
        // Create noise texture
        noiseTexture = Texture.create("SSAO_Noise", SSAO_NOISE_SIZE, SSAO_NOISE_SIZE, GL_RGB16F, GL_RGB, GL_FLOAT);
        noiseTexture.updateData(noiseData);
        noiseTexture.setWrap(GL_REPEAT, GL_REPEAT);
        
        logger.debug("Generated SSAO noise texture");
    }
    
    /**
     * Renders the G-Buffer pass for SSAO.
     * 
     * @param camera The camera
     * @param projectionMatrix Projection matrix
     * @param viewMatrix View matrix
     * @param opaqueQueue The list of opaque render commands
     */
    public void renderGBuffer(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, List<RenderCommand> opaqueQueue) {
        if (!enableSSAO || gBuffer == null) {
            return;
        }
        
        // Bind G-Buffer
        gBuffer.bind();
        
        // Clear buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Enable multiple render targets
        int[] drawBuffers = {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1};
        glDrawBuffers(drawBuffers);
        
        gBufferShader.bind();
        gBufferShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        gBufferShader.setUniform("u_ViewMatrix", viewMatrix);

        for (RenderCommand command : opaqueQueue) {
            gBufferShader.setUniform("u_ModelMatrix", command.getModelMatrix());
            command.getMesh().render();
        }
        
        // Unbind G-Buffer
        Framebuffer.unbind();
    }
    
    /**
     * Renders screen-space ambient occlusion.
     * 
     * @param projectionMatrix Projection matrix
     */
    public void renderSSAO(Matrix4f projectionMatrix) {
        if (!enableSSAO || ssaoShader == null || ssaoBuffer == null) {
            return;
        }
        
        // Bind SSAO buffer
        ssaoBuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Bind SSAO shader
        ssaoShader.bind();
        
        // Set uniforms
        ssaoShader.setUniform("u_ProjectionMatrix", projectionMatrix);
        ssaoShader.setUniform("u_SSAORadius", SSAO_RADIUS);
        ssaoShader.setUniform("u_SSAOBias", SSAO_BIAS);
        ssaoShader.setUniform("u_SSAOPower", SSAO_POWER);
        ssaoShader.setUniform("u_SSAOIntensity", ssaoIntensity);
        ssaoShader.setUniform("u_ScreenSize", new Vector2f((float) screenWidth, (float) screenHeight));
        
        // Bind textures
        glActiveTexture(GL_TEXTURE0);
        gBuffer.bindColorTexture(0, 0);
        ssaoShader.setUniform("u_PositionTexture", 0);
        
        glActiveTexture(GL_TEXTURE1);
        gBuffer.bindColorTexture(1, 1);
        ssaoShader.setUniform("u_NormalTexture", 1);
        
        glActiveTexture(GL_TEXTURE2);
        noiseTexture.bind();
        ssaoShader.setUniform("u_NoiseTexture", 2);
        
        // Set kernel samples
        for (int i = 0; i < SSAO_KERNEL_SIZE; i++) {
            Vector3f sample = new Vector3f(
                ssaoKernel.get(i * 3),
                ssaoKernel.get(i * 3 + 1),
                ssaoKernel.get(i * 3 + 2)
            );
            ssaoShader.setUniform("u_Samples[" + i + "]", sample);
        }
        
        // Render fullscreen quad
        renderFullscreenQuad();
        
        // Unbind shader and buffer
        ssaoShader.unbind();
        Framebuffer.unbind();
        
        // Blur SSAO
        blurSSAO();
    }
    
    /**
     * Blurs the SSAO texture to reduce noise.
     */
    private void blurSSAO() {
        if (ssaoBlurShader == null || ssaoBlurBuffer == null) {
            return;
        }

        // Bind blur buffer
        ssaoBlurBuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT);

        // Bind blur shader
        ssaoBlurShader.bind();

        // Bind SSAO texture
        glActiveTexture(GL_TEXTURE0);
        ssaoBuffer.bindColorTexture(0, 0);
        ssaoBlurShader.setUniform("u_SSAOTexture", 0);

        glActiveTexture(GL_TEXTURE1);
        gBuffer.bindColorTexture(0, 1);
        ssaoBlurShader.setUniform("u_PositionTexture", 1);

        // Render fullscreen quad
        renderFullscreenQuad();

        // Unbind shader and buffer
        ssaoBlurShader.unbind();
        Framebuffer.unbind();
    }
    
    /**
     * Calculates voxel-based ambient occlusion for a block face.
     * 
     * @param worldPos World position of the block
     * @param faceNormal Normal vector of the face
     * @return Ambient occlusion factor (0.0 = fully occluded, 1.0 = no occlusion)
     */
    public float calculateVoxelAO(Vector3i worldPos, Vector3f faceNormal) {
        if (!enableVoxelAO || world == null) {
            return 1.0f;
        }
        
        // Check cache first
        Vector3i cacheKey = new Vector3i(worldPos);
        if (voxelAOCache.containsKey(cacheKey)) {
            return voxelAOCache.get(cacheKey);
        }
        
        float aoFactor = 1.0f;
        int occludingBlocks = 0;
        int totalSamples = 0;
        
        // Sample surrounding blocks based on face normal
        Vector3i[] sampleOffsets = getSampleOffsets(faceNormal);
        
        for (Vector3i offset : sampleOffsets) {
            Vector3i samplePos = new Vector3i(worldPos).add(offset);
            
            // Check if block exists at sample position
            if (isBlockSolid(samplePos)) {
                occludingBlocks++;
            }
            totalSamples++;
        }
        
        // Calculate AO factor
        if (totalSamples > 0) {
            float occlusionRatio = (float) occludingBlocks / totalSamples;
            aoFactor = 1.0f - (occlusionRatio * voxelAOIntensity);
        }
        
        // Cache result
        if (voxelAOCache.size() < maxCacheSize) {
            voxelAOCache.put(cacheKey, aoFactor);
        }
        
        return Math.max(0.1f, aoFactor); // Minimum AO to prevent complete darkness
    }
    
    /**
     * Gets sample offsets for AO calculation based on face normal.
     */
    private Vector3i[] getSampleOffsets(Vector3f faceNormal) {
        // Determine which face we're calculating AO for
        if (Math.abs(faceNormal.y) > 0.9f) {
            // Top/bottom face
            return new Vector3i[]{
                new Vector3i(-1, 0, -1), new Vector3i(0, 0, -1), new Vector3i(1, 0, -1),
                new Vector3i(-1, 0, 0),                          new Vector3i(1, 0, 0),
                new Vector3i(-1, 0, 1),  new Vector3i(0, 0, 1),  new Vector3i(1, 0, 1)
            };
        } else if (Math.abs(faceNormal.x) > 0.9f) {
            // Left/right face
            return new Vector3i[]{
                new Vector3i(0, -1, -1), new Vector3i(0, 0, -1), new Vector3i(0, 1, -1),
                new Vector3i(0, -1, 0),                          new Vector3i(0, 1, 0),
                new Vector3i(0, -1, 1),  new Vector3i(0, 0, 1),  new Vector3i(0, 1, 1)
            };
        } else {
            // Front/back face
            return new Vector3i[]{
                new Vector3i(-1, -1, 0), new Vector3i(0, -1, 0), new Vector3i(1, -1, 0),
                new Vector3i(-1, 0, 0),                          new Vector3i(1, 0, 0),
                new Vector3i(-1, 1, 0),  new Vector3i(0, 1, 0),  new Vector3i(1, 1, 0)
            };
        }
    }
    
    /**
     * Checks if a block at the given position is solid.
     */
    private boolean isBlockSolid(Vector3i pos) {
        if (world == null) {
            return false;
        }
        
        // Get chunk coordinates
        int chunkX = pos.x >> 4; // Divide by 16
        int chunkZ = pos.z >> 4;
        
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }
        
        // Get local coordinates within chunk
        int localX = pos.x & 15; // Modulo 16
        int localY = pos.y;
        int localZ = pos.z & 15;
        
        // Check bounds
        if (localY < 0 || localY >= Chunk.CHUNK_HEIGHT) {
            return false;
        }
        
        // Check if block is solid
        return chunk.getBlock(localX, localY, localZ) != Block.BlockType.AIR;
    }
    
    /**
     * Renders a fullscreen quad for post-processing.
     */
    private void renderFullscreenQuad() {
        // Simple fullscreen quad rendering
        glBegin(GL_TRIANGLES);
        
        // First triangle
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(-1.0f, -1.0f);
        
        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(1.0f, -1.0f);
        
        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(1.0f, 1.0f);
        
        // Second triangle
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(-1.0f, -1.0f);
        
        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(1.0f, 1.0f);
        
        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(-1.0f, 1.0f);
        
        glEnd();
    }
    
    /**
     * Linear interpolation utility.
     */
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
    
    /**
     * Clears the voxel AO cache.
     */
    public void clearVoxelAOCache() {
        voxelAOCache.clear();
        logger.debug("Cleared voxel AO cache");
    }
    
    /**
     * Resizes the ambient occlusion renderer.
     */
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        
        // Recreate framebuffers with new size
        if (gBuffer != null) {
            gBuffer.cleanup();
        }
        if (ssaoBuffer != null) {
            ssaoBuffer.cleanup();
        }
        if (ssaoBlurBuffer != null) {
            ssaoBlurBuffer.cleanup();
        }
        
        createFramebuffers();
        logger.debug("Resized ambient occlusion renderer to {}x{}", width, height);
    }
    
    // Shader source code

    private String getGBufferVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec3 aPos;\n" +
                "layout (location = 1) in vec2 aTexCoord;\n" +
                "layout (location = 2) in vec3 aNormal;\n" +
                "\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "\n" +
                "out vec3 FragPos;\n" +
                "out vec3 Normal;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 viewPos = u_ViewMatrix * u_ModelMatrix * vec4(aPos, 1.0);\n" +
                "    FragPos = viewPos.xyz;\n" +
                "    Normal = mat3(u_ViewMatrix * u_ModelMatrix) * aNormal;\n" +
                "    gl_Position = u_ProjectionMatrix * viewPos;\n" +
                "}";
    }

    private String getGBufferFragmentShader() {
        return "#version 330 core\n" +
                "layout (location = 0) out vec3 gPosition;\n" +
                "layout (location = 1) out vec3 gNormal;\n" +
                "\n" +
                "in vec3 FragPos;\n" +
                "in vec3 Normal;\n" +
                "\n" +
                "void main()\n" +
                "{    \n" +
                "    gPosition = FragPos;\n" +
                "    gNormal = normalize(Normal);\n" +
                "}";
    }
    
    private String getSSAOVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec2 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
                "}";
    }

    private String getSSAOFragmentShader() {
        return "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_PositionTexture;\n" +
                "uniform sampler2D u_NormalTexture;\n" +
                "uniform sampler2D u_NoiseTexture;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform vec3 u_Samples[64];\n" +
                "uniform float u_SSAORadius;\n" +
                "uniform float u_SSAOBias;\n" +
                "uniform float u_SSAOPower;\n" +
                "uniform float u_SSAOIntensity;\n" +
                "uniform vec2 u_ScreenSize;\n" +
                "\n" +
                "out float FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 fragPos = texture(u_PositionTexture, v_TexCoord).xyz;\n" +
                "    vec3 normal = normalize(texture(u_NormalTexture, v_TexCoord).rgb);\n" +
                "    vec3 randomVec = normalize(texture(u_NoiseTexture, v_TexCoord * u_ScreenSize / 4.0).xyz);\n" +
                "    \n" +
                "    // Create TBN matrix\n" +
                "    vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));\n" +
                "    vec3 bitangent = cross(normal, tangent);\n" +
                "    mat3 TBN = mat3(tangent, bitangent, normal);\n" +
                "    \n" +
                "    float occlusion = 0.0;\n" +
                "    for (int i = 0; i < 64; ++i) {\n" +
                "        // Get sample position\n" +
                "        vec3 samplePos = TBN * u_Samples[i];\n" +
                "        samplePos = fragPos + samplePos * u_SSAORadius;\n" +
                "        \n" +
                "        // Project sample position\n" +
                "        vec4 offset = vec4(samplePos, 1.0);\n" +
                "        offset = u_ProjectionMatrix * offset;\n" +
                "        offset.xyz /= offset.w;\n" +
                "        offset.xyz = offset.xyz * 0.5 + 0.5;\n" +
                "        \n" +
                "        // Get sample depth\n" +
                "        float sampleDepth = texture(u_PositionTexture, offset.xy).z;\n" +
                "        \n" +
                "        // Range check and accumulate\n" +
                "        float rangeCheck = smoothstep(0.0, 1.0, u_SSAORadius / abs(fragPos.z - sampleDepth));\n" +
                "        occlusion += (sampleDepth >= samplePos.z + u_SSAOBias ? 1.0 : 0.0) * rangeCheck;\n" +
                "    }\n" +
                "    \n" +
                "    occlusion = 1.0 - (occlusion / 64.0);\n" +
                "    FragColor = pow(occlusion, u_SSAOPower) * u_SSAOIntensity;\n" +
                "}";
    }
    
    private String getSSAOBlurVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec2 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
                "}";
    }

    private String getSSAOBlurFragmentShader() {
        return "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_SSAOTexture;\n" +
                "uniform sampler2D u_PositionTexture;\n" +
                "\n" +
                "out float FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 texelSize = 1.0 / textureSize(u_SSAOTexture, 0);\n" +
                "    float result = 0.0;\n" +
                "    float totalWeight = 0.0;\n" +
                "    vec3 centerPos = texture(u_PositionTexture, v_TexCoord).xyz;\n" +
                "    \n" +
                "    for (int x = -2; x < 2; ++x) {\n" +
                "        for (int y = -2; y < 2; ++y) {\n" +
                "            vec2 offset = vec2(float(x), float(y)) * texelSize;\n" +
                "            vec3 samplePos = texture(u_PositionTexture, v_TexCoord + offset).xyz;\n" +
                "            float depthDiff = abs(centerPos.z - samplePos.z);\n" +
                "            float weight = exp(-depthDiff * 10.0);\n" +
                "            result += texture(u_SSAOTexture, v_TexCoord + offset).r * weight;\n" +
                "            totalWeight += weight;\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    FragColor = result / totalWeight;\n" +
                "}";
    }
    
    private String getVoxelAOVertexShader() {
        return "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "layout (location = 1) in vec2 a_TexCoord;\n" +
                "layout (location = 2) in vec3 a_Normal;\n" +
                "layout (location = 3) in float a_AO;\n" +
                "\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "out vec3 v_Normal;\n" +
                "out float v_AO;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    v_Normal = mat3(u_ModelMatrix) * a_Normal;\n" +
                "    v_AO = a_AO;\n" +
                "    \n" +
                "    gl_Position = u_ProjectionMatrix * u_ViewMatrix * u_ModelMatrix * vec4(a_Position, 1.0);\n" +
                "}";
    }

    private String getVoxelAOFragmentShader() {
        return "#version 330 core\n" +
                "in vec2 v_TexCoord;\n" +
                "in vec3 v_Normal;\n" +
                "in float v_AO;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_VoxelAOIntensity;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
                "    \n" +
                "    // Apply voxel AO\n" +
                "    float aoFactor = mix(1.0, v_AO, u_VoxelAOIntensity);\n" +
                "    texColor.rgb *= aoFactor;\n" +
                "    \n" +
                "    FragColor = texColor;\n" +
                "}";
    }
    
    // Getters and setters
    
    public void setEnableSSAO(boolean enable) {
        this.enableSSAO = enable;
    }
    
    public void setEnableVoxelAO(boolean enable) {
        this.enableVoxelAO = enable;
    }
    
    public void setSSAOIntensity(float intensity) {
        this.ssaoIntensity = Math.max(0.0f, Math.min(2.0f, intensity));
    }
    
    public void setVoxelAOIntensity(float intensity) {
        this.voxelAOIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    public int getSSAOTexture() {
        return ssaoBlurBuffer != null ? ssaoBlurBuffer.getColorTexture(0) : ssaoBuffer.getColorTexture(0);
    }
    
    /**
     * Cleanup ambient occlusion renderer resources.
     */
    public void cleanup() {
        if (gBufferShader != null) {
            gBufferShader.cleanup();
        }
        if (ssaoShader != null) {
            ssaoShader.cleanup();
        }
        if (ssaoBlurShader != null) {
            ssaoBlurShader.cleanup();
        }
        if (voxelAOShader != null) {
            voxelAOShader.cleanup();
        }
        if (gBuffer != null) {
            gBuffer.cleanup();
        }
        if (ssaoBuffer != null) {
            ssaoBuffer.cleanup();
        }
        if (ssaoBlurBuffer != null) {
            ssaoBlurBuffer.cleanup();
        }
        // Clean up framebuffers (they handle their own texture cleanup)
        if (gBuffer != null) {
            gBuffer.cleanup();
        }
        if (ssaoBuffer != null) {
            ssaoBuffer.cleanup();
        }
        if (ssaoBlurBuffer != null) {
            ssaoBlurBuffer.cleanup();
        }
        
        // Clean up standalone textures
        if (noiseTexture != null) {
            noiseTexture.cleanup();
        }
        
        voxelAOCache.clear();
        
        logger.info("Ambient occlusion renderer cleaned up");
    }
}
