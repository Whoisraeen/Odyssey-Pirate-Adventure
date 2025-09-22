package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Material system for The Odyssey rendering pipeline.
 * 
 * Manages shader properties, textures, and rendering states for different
 * surface types including ships, ocean, terrain, and UI elements.
 */
public class Material {
    
    private static final Logger logger = Logger.getLogger(Material.class);
    
    // Material identification
    private String name;
    private MaterialType type;
    
    // Shader and textures
    private Shader shader;
    private Map<String, Texture> textures;
    private Map<String, Integer> textureSlots;
    
    // Material properties
    private Map<String, Object> properties;
    
    // Rendering state
    private BlendMode blendMode;
    private CullMode cullMode;
    private DepthTest depthTest;
    private boolean wireframe;
    private float lineWidth;
    
    /**
     * Material types for different rendering needs.
     */
    public enum MaterialType {
        OPAQUE,
        TRANSPARENT,
        CUTOUT,
        OCEAN,
        SHIP,
        TERRAIN,
        SKYBOX,
        UI,
        PARTICLE,
        SHADOW,
        POST_PROCESS
    }
    
    /**
     * Blend modes for transparency.
     */
    public enum BlendMode {
        NONE(false, 0, 0, 0, 0),
        ALPHA(true, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA),
        ADDITIVE(true, GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE),
        MULTIPLY(true, GL_DST_COLOR, GL_ZERO, GL_DST_ALPHA, GL_ZERO),
        PREMULTIPLIED(true, GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        
        public final boolean enabled;
        public final int srcRGB, dstRGB, srcAlpha, dstAlpha;
        
        BlendMode(boolean enabled, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
            this.enabled = enabled;
            this.srcRGB = srcRGB;
            this.dstRGB = dstRGB;
            this.srcAlpha = srcAlpha;
            this.dstAlpha = dstAlpha;
        }
    }
    
    /**
     * Face culling modes.
     */
    public enum CullMode {
        NONE(false, 0),
        BACK(true, GL_BACK),
        FRONT(true, GL_FRONT),
        FRONT_AND_BACK(true, GL_FRONT_AND_BACK);
        
        public final boolean enabled;
        public final int mode;
        
        CullMode(boolean enabled, int mode) {
            this.enabled = enabled;
            this.mode = mode;
        }
    }
    
    /**
     * Depth testing configuration.
     */
    public enum DepthTest {
        DISABLED(false, 0, false),
        LESS(true, GL_LESS, true),
        LESS_EQUAL(true, GL_LEQUAL, true),
        GREATER(true, GL_GREATER, true),
        GREATER_EQUAL(true, GL_GEQUAL, true),
        EQUAL(true, GL_EQUAL, true),
        NOT_EQUAL(true, GL_NOTEQUAL, true),
        ALWAYS(true, GL_ALWAYS, true),
        READ_ONLY(true, GL_LESS, false);
        
        public final boolean enabled;
        public final int func;
        public final boolean writeDepth;
        
        DepthTest(boolean enabled, int func, boolean writeDepth) {
            this.enabled = enabled;
            this.func = func;
            this.writeDepth = writeDepth;
        }
    }
    
    /**
     * Material builder for easy construction.
     */
    public static class Builder {
        private String name = "Material";
        private MaterialType type = MaterialType.OPAQUE;
        private Shader shader = null;
        private Map<String, Texture> textures = new HashMap<>();
        private Map<String, Object> properties = new HashMap<>();
        private BlendMode blendMode = BlendMode.NONE;
        private CullMode cullMode = CullMode.BACK;
        private DepthTest depthTest = DepthTest.LESS;
        private boolean wireframe = false;
        private float lineWidth = 1.0f;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder type(MaterialType type) {
            this.type = type;
            return this;
        }
        
        public Builder shader(Shader shader) {
            this.shader = shader;
            return this;
        }
        
        public Builder texture(String name, Texture texture) {
            textures.put(name, texture);
            return this;
        }
        
        public Builder diffuse(Texture texture) {
            return texture("u_diffuse", texture);
        }
        
        public Builder normal(Texture texture) {
            return texture("u_normal", texture);
        }
        
        public Builder specular(Texture texture) {
            return texture("u_specular", texture);
        }
        
        public Builder roughness(Texture texture) {
            return texture("u_roughness", texture);
        }
        
        public Builder metallic(Texture texture) {
            return texture("u_metallic", texture);
        }
        
        public Builder ao(Texture texture) {
            return texture("u_ao", texture);
        }
        
        public Builder emission(Texture texture) {
            return texture("u_emission", texture);
        }
        
        public Builder property(String name, Object value) {
            properties.put(name, value);
            return this;
        }
        
        public Builder color(Vector3f color) {
            return property("u_color", color);
        }
        
        public Builder color(Vector4f color) {
            return property("u_color", color);
        }
        
        public Builder color(float r, float g, float b) {
            return property("u_color", new Vector3f(r, g, b));
        }
        
        public Builder color(float r, float g, float b, float a) {
            return property("u_color", new Vector4f(r, g, b, a));
        }
        
        public Builder metallic(float metallic) {
            return property("u_metallic", metallic);
        }
        
        public Builder roughness(float roughness) {
            return property("u_roughness", roughness);
        }
        
        public Builder emission(Vector3f emission) {
            return property("u_emission", emission);
        }
        
        public Builder emissionStrength(float strength) {
            return property("u_emissionStrength", strength);
        }
        
        public Builder tiling(Vector2f tiling) {
            return property("u_tiling", tiling);
        }
        
        public Builder offset(Vector2f offset) {
            return property("u_offset", offset);
        }
        
        public Builder blendMode(BlendMode blendMode) {
            this.blendMode = blendMode;
            return this;
        }
        
        public Builder cullMode(CullMode cullMode) {
            this.cullMode = cullMode;
            return this;
        }
        
        public Builder depthTest(DepthTest depthTest) {
            this.depthTest = depthTest;
            return this;
        }
        
        public Builder wireframe(boolean wireframe) {
            this.wireframe = wireframe;
            return this;
        }
        
        public Builder lineWidth(float lineWidth) {
            this.lineWidth = lineWidth;
            return this;
        }
        
        public Material build() {
            Material material = new Material(name, type);
            material.shader = shader;
            material.textures.putAll(textures);
            material.properties.putAll(properties);
            material.blendMode = blendMode;
            material.cullMode = cullMode;
            material.depthTest = depthTest;
            material.wireframe = wireframe;
            material.lineWidth = lineWidth;
            material.assignTextureSlots();
            return material;
        }
    }
    
    /**
     * Create material.
     */
    public Material(String name, MaterialType type) {
        this.name = name;
        this.type = type;
        this.textures = new HashMap<>();
        this.textureSlots = new HashMap<>();
        this.properties = new HashMap<>();
        this.blendMode = BlendMode.NONE;
        this.cullMode = CullMode.BACK;
        this.depthTest = DepthTest.LESS;
        this.wireframe = false;
        this.lineWidth = 1.0f;
        
        // Set default properties based on type
        setDefaultProperties();
    }
    
    /**
     * Set default properties based on material type.
     */
    private void setDefaultProperties() {
        switch (type) {
            case OPAQUE -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.5f);
                setProperty("u_ao", 1.0f);
            }
            case TRANSPARENT -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 0.5f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.1f);
                blendMode = BlendMode.ALPHA;
                cullMode = CullMode.NONE;
            }
            case OCEAN -> {
                setProperty("u_color", new Vector4f(0.1f, 0.3f, 0.6f, 0.8f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.1f);
                setProperty("u_waveHeight", 2.0f);
                setProperty("u_waveSpeed", 1.0f);
                setProperty("u_foamThreshold", 0.8f);
                blendMode = BlendMode.ALPHA;
                cullMode = CullMode.BACK;
            }
            case SHIP -> {
                setProperty("u_color", new Vector4f(0.8f, 0.6f, 0.4f, 1.0f));
                setProperty("u_metallic", 0.1f);
                setProperty("u_roughness", 0.7f);
                setProperty("u_weathering", 0.3f);
            }
            case TERRAIN -> {
                setProperty("u_color", new Vector4f(0.5f, 0.7f, 0.3f, 1.0f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.8f);
                setProperty("u_grassHeight", 0.1f);
            }
            case SKYBOX -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                setProperty("u_exposure", 1.0f);
                cullMode = CullMode.FRONT;
                depthTest = DepthTest.LESS_EQUAL;
            }
            case UI -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                blendMode = BlendMode.ALPHA;
                cullMode = CullMode.NONE;
                depthTest = DepthTest.DISABLED;
            }
            case CUTOUT -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.5f);
                setProperty("u_cutoff", 0.5f);
                cullMode = CullMode.BACK;
            }
            case SHADOW -> {
                setProperty("u_color", new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 1.0f);
                depthTest = DepthTest.LESS;
            }
            case POST_PROCESS -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                setProperty("u_metallic", 0.0f);
                setProperty("u_roughness", 0.0f);
                blendMode = BlendMode.NONE;
                cullMode = CullMode.NONE;
                depthTest = DepthTest.DISABLED;
            }
            case PARTICLE -> {
                setProperty("u_color", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                setProperty("u_size", 1.0f);
                blendMode = BlendMode.ADDITIVE;
                cullMode = CullMode.NONE;
                depthTest = DepthTest.READ_ONLY;
            }
        }
        
        // Common properties
        setProperty("u_tiling", new Vector2f(1.0f, 1.0f));
        setProperty("u_offset", new Vector2f(0.0f, 0.0f));
    }
    
    /**
     * Assign texture slots automatically.
     */
    private void assignTextureSlots() {
        int slot = 0;
        for (String textureName : textures.keySet()) {
            textureSlots.put(textureName, slot++);
        }
    }
    
    /**
     * Bind material for rendering.
     */
    public void bind() {
        if (shader == null) {
            logger.warn("Material '{}' has no shader", name);
            return;
        }
        
        // Bind shader
        shader.bind();
        
        // Set rendering state
        applyRenderState();
        
        // Bind textures
        bindTextures();
        
        // Set shader properties
        setShaderProperties();
    }
    
    /**
     * Apply rendering state.
     */
    private void applyRenderState() {
        // Blend mode
        if (blendMode.enabled) {
            glEnable(GL_BLEND);
            glBlendFunc(blendMode.srcRGB, blendMode.dstRGB);
        } else {
            glDisable(GL_BLEND);
        }
        
        // Culling
        if (cullMode.enabled) {
            glEnable(GL_CULL_FACE);
            glCullFace(cullMode.mode);
        } else {
            glDisable(GL_CULL_FACE);
        }
        
        // Depth testing
        if (depthTest.enabled) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(depthTest.func);
            glDepthMask(depthTest.writeDepth);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        
        // Wireframe
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glLineWidth(lineWidth);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    /**
     * Bind textures to their assigned slots.
     */
    private void bindTextures() {
        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            String name = entry.getKey();
            Texture texture = entry.getValue();
            Integer slot = textureSlots.get(name);
            
            if (slot != null && texture != null) {
                glActiveTexture(GL_TEXTURE0 + slot);
                texture.bind();
                shader.setUniform(name, slot);
            }
        }
    }
    
    /**
     * Set shader properties from material properties.
     */
    private void setShaderProperties() {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Float f) {
                shader.setUniform(name, f);
            } else if (value instanceof Integer i) {
                shader.setUniform(name, i);
            } else if (value instanceof Boolean b) {
                shader.setUniform(name, b);
            } else if (value instanceof Vector2f v) {
                shader.setUniform(name, v);
            } else if (value instanceof Vector3f v) {
                shader.setUniform(name, v);
            } else if (value instanceof Vector4f v) {
                shader.setUniform(name, v);
            }
        }
    }
    
    /**
     * Unbind material.
     */
    public void unbind() {
        // Reset to default state
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDepthMask(true);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        // Unbind shader
        if (shader != null) {
            shader.unbind();
        }
    }
    
    /**
     * Set texture.
     */
    public void setTexture(String name, Texture texture) {
        if (texture != null) {
            textures.put(name, texture);
            if (!textureSlots.containsKey(name)) {
                textureSlots.put(name, textureSlots.size());
            }
        } else {
            textures.remove(name);
            textureSlots.remove(name);
        }
    }
    
    /**
     * Get texture.
     */
    public Texture getTexture(String name) {
        return textures.get(name);
    }
    
    /**
     * Set property.
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    /**
     * Get property.
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, Class<T> type) {
        Object value = properties.get(name);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Get property with default value.
     */
    public <T> T getProperty(String name, Class<T> type, T defaultValue) {
        T value = getProperty(name, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Clone material.
     */
    public Material clone() {
        Material cloned = new Material(name + "_clone", type);
        cloned.shader = shader;
        cloned.textures.putAll(textures);
        cloned.textureSlots.putAll(textureSlots);
        
        // Deep copy properties
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Vector2f v) {
                cloned.properties.put(entry.getKey(), new Vector2f(v));
            } else if (value instanceof Vector3f v) {
                cloned.properties.put(entry.getKey(), new Vector3f(v));
            } else if (value instanceof Vector4f v) {
                cloned.properties.put(entry.getKey(), new Vector4f(v));
            } else {
                cloned.properties.put(entry.getKey(), value);
            }
        }
        
        cloned.blendMode = blendMode;
        cloned.cullMode = cullMode;
        cloned.depthTest = depthTest;
        cloned.wireframe = wireframe;
        cloned.lineWidth = lineWidth;
        
        return cloned;
    }
    
    /**
     * Create common material presets.
     */
    public static class Presets {
        
        public static Material createDefault(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.OPAQUE)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .build();
        }
        
        public static Material createOcean(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.OCEAN)
                .color(0.1f, 0.3f, 0.6f, 0.8f)
                .roughness(0.1f)
                .metallic(0.0f)
                .blendMode(BlendMode.ALPHA)
                .build();
        }
        
        public static Material createWood(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.SHIP)
                .color(0.8f, 0.6f, 0.4f, 1.0f)
                .roughness(0.7f)
                .metallic(0.0f)
                .build();
        }
        
        public static Material createMetal(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.OPAQUE)
                .color(0.7f, 0.7f, 0.7f, 1.0f)
                .roughness(0.2f)
                .metallic(1.0f)
                .build();
        }
        
        public static Material createGlass(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.TRANSPARENT)
                .color(1.0f, 1.0f, 1.0f, 0.3f)
                .roughness(0.0f)
                .metallic(0.0f)
                .blendMode(BlendMode.ALPHA)
                .cullMode(CullMode.NONE)
                .build();
        }
        
        public static Material createUI(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.UI)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .blendMode(BlendMode.ALPHA)
                .cullMode(CullMode.NONE)
                .depthTest(DepthTest.DISABLED)
                .build();
        }
        
        public static Material createParticle(String name) {
            return new Builder()
                .name(name)
                .type(MaterialType.PARTICLE)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .blendMode(BlendMode.ADDITIVE)
                .cullMode(CullMode.NONE)
                .depthTest(DepthTest.READ_ONLY)
                .build();
        }
    }
    
    // Getters and setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public MaterialType getType() {
        return type;
    }
    
    public void setType(MaterialType type) {
        this.type = type;
    }
    
    public Shader getShader() {
        return shader;
    }
    
    public void setShader(Shader shader) {
        this.shader = shader;
    }
    
    public BlendMode getBlendMode() {
        return blendMode;
    }
    
    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
    }
    
    public CullMode getCullMode() {
        return cullMode;
    }
    
    public void setCullMode(CullMode cullMode) {
        this.cullMode = cullMode;
    }
    
    public DepthTest getDepthTest() {
        return depthTest;
    }
    
    public void setDepthTest(DepthTest depthTest) {
        this.depthTest = depthTest;
    }
    
    public boolean isWireframe() {
        return wireframe;
    }
    
    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }
    
    public Map<String, Texture> getTextures() {
        return new HashMap<>(textures);
    }
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Material material = (Material) o;
        return Objects.equals(name, material.name) && type == material.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
    
    @Override
    public String toString() {
        return String.format("Material{name='%s', type=%s, shader=%s, textures=%d, properties=%d}", 
                           name, type, shader != null ? shader.getName() : "null", 
                           textures.size(), properties.size());
    }
}