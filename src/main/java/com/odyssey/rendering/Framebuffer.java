package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * Framebuffer object for render-to-texture operations in The Odyssey.
 * 
 * Supports multiple color attachments, depth/stencil buffers, and various
 * texture formats for advanced rendering techniques.
 */
public class Framebuffer {
    
    private static final Logger logger = Logger.getLogger(Framebuffer.class);
    
    // OpenGL objects
    private int fbo;
    private List<Integer> colorTextures;
    private int depthTexture;
    private int depthStencilRbo;
    
    // Properties
    private int width;
    private int height;
    private String name;
    private boolean multisampled;
    private int samples;
    
    // Attachment configuration
    private List<ColorAttachment> colorAttachments;
    private DepthAttachment depthAttachment;
    
    /**
     * Color attachment configuration.
     */
    public static class ColorAttachment {
        public final int format;
        public final int internalFormat;
        public final int type;
        public final boolean generateMipmaps;
        public final int minFilter;
        public final int magFilter;
        public final int wrapS;
        public final int wrapT;
        
        public ColorAttachment(int format, int internalFormat, int type) {
            this(format, internalFormat, type, false, GL_LINEAR, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_CLAMP_TO_EDGE);
        }
        
        public ColorAttachment(int format, int internalFormat, int type, boolean generateMipmaps,
                             int minFilter, int magFilter, int wrapS, int wrapT) {
            this.format = format;
            this.internalFormat = internalFormat;
            this.type = type;
            this.generateMipmaps = generateMipmaps;
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.wrapS = wrapS;
            this.wrapT = wrapT;
        }
        
        // Common attachment types
        public static ColorAttachment rgba8() {
            return new ColorAttachment(GL_RGBA, GL_RGBA8, GL_UNSIGNED_BYTE);
        }
        
        public static ColorAttachment rgba16f() {
            return new ColorAttachment(GL_RGBA, GL_RGBA16F, GL_FLOAT);
        }
        
        public static ColorAttachment rgba32f() {
            return new ColorAttachment(GL_RGBA, GL_RGBA32F, GL_FLOAT);
        }
        
        public static ColorAttachment rgb8() {
            return new ColorAttachment(GL_RGB, GL_RGB8, GL_UNSIGNED_BYTE);
        }
        
        public static ColorAttachment rgb16f() {
            return new ColorAttachment(GL_RGB, GL_RGB16F, GL_FLOAT);
        }
        
        public static ColorAttachment rg16f() {
            return new ColorAttachment(GL_RG, GL_RG16F, GL_FLOAT);
        }
        
        public static ColorAttachment r8() {
            return new ColorAttachment(GL_RED, GL_R8, GL_UNSIGNED_BYTE);
        }
        
        public static ColorAttachment r16f() {
            return new ColorAttachment(GL_RED, GL_R16F, GL_FLOAT);
        }
    }
    
    /**
     * Depth attachment configuration.
     */
    public static class DepthAttachment {
        public final Type type;
        public final int format;
        public final int internalFormat;
        public final int attachmentType;
        public final boolean useTexture;
        
        public enum Type {
            DEPTH_ONLY,
            DEPTH_STENCIL
        }
        
        private DepthAttachment(Type type, int format, int internalFormat, int attachmentType, boolean useTexture) {
            this.type = type;
            this.format = format;
            this.internalFormat = internalFormat;
            this.attachmentType = attachmentType;
            this.useTexture = useTexture;
        }
        
        public static DepthAttachment depthTexture() {
            return new DepthAttachment(Type.DEPTH_ONLY, GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24, GL_DEPTH_ATTACHMENT, true);
        }
        
        public static DepthAttachment depthStencilTexture() {
            return new DepthAttachment(Type.DEPTH_STENCIL, GL_DEPTH_STENCIL, GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL_ATTACHMENT, true);
        }
        
        public static DepthAttachment depthRbo() {
            return new DepthAttachment(Type.DEPTH_ONLY, GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24, GL_DEPTH_ATTACHMENT, false);
        }
        
        public static DepthAttachment depthStencilRbo() {
            return new DepthAttachment(Type.DEPTH_STENCIL, GL_DEPTH_STENCIL, GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL_ATTACHMENT, false);
        }
    }
    
    /**
     * Framebuffer builder for easy configuration.
     */
    public static class Builder {
        private String name = "Framebuffer";
        private int width = 1024;
        private int height = 1024;
        private boolean multisampled = false;
        private int samples = 4;
        private List<ColorAttachment> colorAttachments = new ArrayList<>();
        private DepthAttachment depthAttachment = null;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder multisampled(int samples) {
            this.multisampled = true;
            this.samples = samples;
            return this;
        }
        
        public Builder addColorAttachment(ColorAttachment attachment) {
            colorAttachments.add(attachment);
            return this;
        }
        
        public Builder addColorRGBA8() {
            return addColorAttachment(ColorAttachment.rgba8());
        }
        
        public Builder addColorRGBA16F() {
            return addColorAttachment(ColorAttachment.rgba16f());
        }
        
        public Builder addColorRGBA32F() {
            return addColorAttachment(ColorAttachment.rgba32f());
        }
        
        public Builder depthTexture() {
            this.depthAttachment = DepthAttachment.depthTexture();
            return this;
        }
        
        public Builder depthStencilTexture() {
            this.depthAttachment = DepthAttachment.depthStencilTexture();
            return this;
        }
        
        public Builder depthRbo() {
            this.depthAttachment = DepthAttachment.depthRbo();
            return this;
        }
        
        public Builder depthStencilRbo() {
            this.depthAttachment = DepthAttachment.depthStencilRbo();
            return this;
        }
        
        public Framebuffer build() {
            if (colorAttachments.isEmpty() && depthAttachment == null) {
                throw new IllegalStateException("Framebuffer must have at least one attachment");
            }
            
            Framebuffer framebuffer = new Framebuffer(name, width, height, multisampled, samples);
            framebuffer.colorAttachments = new ArrayList<>(colorAttachments);
            framebuffer.depthAttachment = depthAttachment;
            framebuffer.create();
            return framebuffer;
        }
    }
    
    /**
     * Create framebuffer.
     */
    private Framebuffer(String name, int width, int height, boolean multisampled, int samples) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.multisampled = multisampled;
        this.samples = samples;
        this.colorTextures = new ArrayList<>();
        this.colorAttachments = new ArrayList<>();
    }
    
    /**
     * Create framebuffer objects.
     */
    private void create() {
        // Generate framebuffer
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        
        // Create color attachments
        for (int i = 0; i < colorAttachments.size(); i++) {
            ColorAttachment attachment = colorAttachments.get(i);
            int texture = createColorTexture(attachment, i);
            colorTextures.add(texture);
        }
        
        // Create depth attachment
        if (depthAttachment != null) {
            if (depthAttachment.useTexture) {
                depthTexture = createDepthTexture(depthAttachment);
            } else {
                depthStencilRbo = createDepthRbo(depthAttachment);
            }
        }
        
        // Set draw buffers
        if (!colorAttachments.isEmpty()) {
            IntBuffer drawBuffers = BufferUtils.createIntBuffer(colorAttachments.size());
            for (int i = 0; i < colorAttachments.size(); i++) {
                drawBuffers.put(GL_COLOR_ATTACHMENT0 + i);
            }
            drawBuffers.flip();
            glDrawBuffers(drawBuffers);
        } else {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
        
        // Check framebuffer completeness
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String error = getFramebufferStatusString(status);
            throw new RuntimeException("Framebuffer '" + name + "' is not complete: " + error);
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        logger.debug("Created framebuffer '{}' ({}x{}, {} color attachments, depth: {})", 
                   name, width, height, colorAttachments.size(), depthAttachment != null);
    }
    
    /**
     * Create color texture attachment.
     */
    private int createColorTexture(ColorAttachment attachment, int index) {
        int texture = glGenTextures();
        
        if (multisampled) {
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texture);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, attachment.internalFormat, width, height, true);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, GL_TEXTURE_2D_MULTISAMPLE, texture, 0);
        } else {
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, attachment.internalFormat, width, height, 0, 
                        attachment.format, attachment.type, (java.nio.ByteBuffer) null);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, attachment.minFilter);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, attachment.magFilter);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, attachment.wrapS);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, attachment.wrapT);
            
            if (attachment.generateMipmaps) {
                glGenerateMipmap(GL_TEXTURE_2D);
            }
            
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, GL_TEXTURE_2D, texture, 0);
        }
        
        return texture;
    }
    
    /**
     * Create depth texture attachment.
     */
    private int createDepthTexture(DepthAttachment attachment) {
        int texture = glGenTextures();
        
        if (multisampled) {
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texture);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, attachment.internalFormat, width, height, true);
            glFramebufferTexture2D(GL_FRAMEBUFFER, attachment.attachmentType, GL_TEXTURE_2D_MULTISAMPLE, texture, 0);
        } else {
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, attachment.internalFormat, width, height, 0, 
                        attachment.format, GL_FLOAT, (java.nio.ByteBuffer) null);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            
            float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
            
            glFramebufferTexture2D(GL_FRAMEBUFFER, attachment.attachmentType, GL_TEXTURE_2D, texture, 0);
        }
        
        return texture;
    }
    
    /**
     * Create depth renderbuffer attachment.
     */
    private int createDepthRbo(DepthAttachment attachment) {
        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        
        if (multisampled) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, attachment.internalFormat, width, height);
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, attachment.internalFormat, width, height);
        }
        
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment.attachmentType, GL_RENDERBUFFER, rbo);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        
        return rbo;
    }
    
    /**
     * Bind framebuffer for rendering.
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }
    
    /**
     * Bind framebuffer for reading.
     */
    public void bindRead() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
    }
    
    /**
     * Bind framebuffer for drawing.
     */
    public void bindDraw() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
    }
    
    /**
     * Unbind framebuffer (bind default framebuffer).
     */
    public static void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Clear framebuffer.
     */
    public void clear() {
        clear(0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
    }
    
    /**
     * Clear framebuffer with specific values.
     */
    public void clear(float r, float g, float b, float a, float depth) {
        bind();
        glClearColor(r, g, b, a);
        glClearDepth(depth);
        
        int mask = 0;
        if (!colorAttachments.isEmpty()) {
            mask |= GL_COLOR_BUFFER_BIT;
        }
        if (depthAttachment != null) {
            mask |= GL_DEPTH_BUFFER_BIT;
            if (depthAttachment.type == DepthAttachment.Type.DEPTH_STENCIL) {
                mask |= GL_STENCIL_BUFFER_BIT;
            }
        }
        
        glClear(mask);
    }
    
    /**
     * Resize framebuffer.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        
        cleanup();
        
        this.width = newWidth;
        this.height = newHeight;
        
        create();
        
        logger.debug("Resized framebuffer '{}' to {}x{}", name, width, height);
    }
    
    /**
     * Blit to another framebuffer.
     */
    public void blitTo(Framebuffer target, int mask, int filter) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target != null ? target.fbo : 0);
        
        int targetWidth = target != null ? target.width : width;
        int targetHeight = target != null ? target.height : height;
        
        glBlitFramebuffer(0, 0, width, height, 0, 0, targetWidth, targetHeight, mask, filter);
        
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }
    
    /**
     * Blit to default framebuffer.
     */
    public void blitToScreen(int mask, int filter) {
        blitTo(null, mask, filter);
    }
    
    /**
     * Bind color texture for reading.
     */
    public void bindColorTexture(int index, int textureUnit) {
        if (index >= 0 && index < colorTextures.size()) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            if (multisampled) {
                glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorTextures.get(index));
            } else {
                glBindTexture(GL_TEXTURE_2D, colorTextures.get(index));
            }
        }
    }
    
    /**
     * Bind depth texture for reading.
     */
    public void bindDepthTexture(int textureUnit) {
        if (depthTexture != 0) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            if (multisampled) {
                glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, depthTexture);
            } else {
                glBindTexture(GL_TEXTURE_2D, depthTexture);
            }
        }
    }
    
    /**
     * Generate mipmaps for color textures.
     */
    public void generateMipmaps() {
        if (multisampled) {
            logger.warn("Cannot generate mipmaps for multisampled framebuffer");
            return;
        }
        
        for (int i = 0; i < colorTextures.size(); i++) {
            if (colorAttachments.get(i).generateMipmaps) {
                glBindTexture(GL_TEXTURE_2D, colorTextures.get(i));
                glGenerateMipmap(GL_TEXTURE_2D);
            }
        }
    }
    
    // Getters
    
    public int getFBO() {
        return fbo;
    }
    
    public String getName() {
        return name;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean isMultisampled() {
        return multisampled;
    }
    
    public int getSamples() {
        return samples;
    }
    
    public int getColorTextureCount() {
        return colorTextures.size();
    }
    
    public int getColorTexture(int index) {
        return index >= 0 && index < colorTextures.size() ? colorTextures.get(index) : 0;
    }
    
    public int getDepthTexture() {
        return depthTexture;
    }
    
    public boolean hasDepthTexture() {
        return depthTexture != 0;
    }
    
    public boolean hasDepthBuffer() {
        return depthTexture != 0 || depthStencilRbo != 0;
    }
    
    /**
     * Check if framebuffer is valid.
     */
    public boolean isValid() {
        return fbo != 0 && glIsFramebuffer(fbo);
    }
    
    /**
     * Get memory usage estimate in bytes.
     */
    public long getMemoryUsage() {
        long totalMemory = 0;
        
        // Color attachments
        for (ColorAttachment attachment : colorAttachments) {
            int bytesPerPixel = getBytesPerPixel(attachment.internalFormat);
            long textureMemory = (long) width * height * bytesPerPixel;
            if (multisampled) {
                textureMemory *= samples;
            }
            totalMemory += textureMemory;
        }
        
        // Depth attachment
        if (depthAttachment != null) {
            int bytesPerPixel = getBytesPerPixel(depthAttachment.internalFormat);
            long depthMemory = (long) width * height * bytesPerPixel;
            if (multisampled) {
                depthMemory *= samples;
            }
            totalMemory += depthMemory;
        }
        
        return totalMemory;
    }
    
    /**
     * Get bytes per pixel for internal format.
     */
    private int getBytesPerPixel(int internalFormat) {
        return switch (internalFormat) {
            case GL_R8 -> 1;
            case GL_RG8 -> 2;
            case GL_RGB8 -> 3;
            case GL_RGBA8 -> 4;
            case GL_R16F -> 2;
            case GL_RG16F -> 4;
            case GL_RGB16F -> 6;
            case GL_RGBA16F -> 8;
            case GL_R32F -> 4;
            case GL_RG32F -> 8;
            case GL_RGB32F -> 12;
            case GL_RGBA32F -> 16;
            case GL_DEPTH_COMPONENT16 -> 2;
            case GL_DEPTH_COMPONENT24 -> 3;
            case GL_DEPTH_COMPONENT32F -> 4;
            case GL_DEPTH24_STENCIL8 -> 4;
            default -> 4; // Default estimate
        };
    }
    
    /**
     * Get framebuffer status string.
     */
    private String getFramebufferStatusString(int status) {
        return switch (status) {
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "Incomplete attachment";
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "Missing attachment";
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "Incomplete draw buffer";
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "Incomplete read buffer";
            case GL_FRAMEBUFFER_UNSUPPORTED -> "Unsupported";
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "Incomplete multisample";
            default -> "Unknown error (" + status + ")";
        };
    }
    
    /**
     * Create common framebuffer configurations.
     */
    public static class Presets {
        
        public static Framebuffer createColorDepth(String name, int width, int height) {
            return new Builder()
                .name(name)
                .size(width, height)
                .addColorRGBA8()
                .depthTexture()
                .build();
        }
        
        public static Framebuffer createHDRColorDepth(String name, int width, int height) {
            return new Builder()
                .name(name)
                .size(width, height)
                .addColorRGBA16F()
                .depthTexture()
                .build();
        }
        
        public static Framebuffer createGBuffer(String name, int width, int height) {
            return new Builder()
                .name(name)
                .size(width, height)
                .addColorRGBA8()      // Albedo + Metallic
                .addColorRGBA8()      // Normal + Roughness
                .addColorRGBA16F()    // Position + AO
                .addColorRGBA8()      // Motion vectors + other
                .depthTexture()
                .build();
        }
        
        public static Framebuffer createShadowMap(String name, int size) {
            return new Builder()
                .name(name)
                .size(size, size)
                .depthTexture()
                .build();
        }
        
        public static Framebuffer createMultisampledColorDepth(String name, int width, int height, int samples) {
            return new Builder()
                .name(name)
                .size(width, height)
                .multisampled(samples)
                .addColorRGBA8()
                .depthRbo()
                .build();
        }
    }
    
    /**
     * Cleanup framebuffer resources.
     */
    public void cleanup() {
        if (fbo != 0) {
            glDeleteFramebuffers(fbo);
            fbo = 0;
        }
        
        for (int texture : colorTextures) {
            if (texture != 0) {
                glDeleteTextures(texture);
            }
        }
        colorTextures.clear();
        
        if (depthTexture != 0) {
            glDeleteTextures(depthTexture);
            depthTexture = 0;
        }
        
        if (depthStencilRbo != 0) {
            glDeleteRenderbuffers(depthStencilRbo);
            depthStencilRbo = 0;
        }
        
        logger.debug("Cleaned up framebuffer: {}", name);
    }
    
    @Override
    public String toString() {
        return String.format("Framebuffer{name='%s', size=%dx%d, colors=%d, depth=%s, fbo=%d}", 
                           name, width, height, colorTextures.size(), hasDepthBuffer(), fbo);
    }
}