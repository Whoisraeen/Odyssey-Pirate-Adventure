package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;
import com.odyssey.game.math.Vector2f;

/**
 * Component that defines how an entity should be rendered.
 */
public class RenderableComponent implements Component {
    public String texturePath;
    public Vector2f textureOffset; // UV offset for sprite sheets
    public Vector2f textureSize; // UV size for sprite sheets (1,1 = full texture)
    public float[] color; // RGBA color tint (1,1,1,1 = no tint)
    public int renderLayer; // Higher values render on top
    public boolean visible;
    public float scale;
    
    public RenderableComponent() {
        this("", 0);
    }
    
    public RenderableComponent(String texturePath) {
        this(texturePath, 0);
    }
    
    public RenderableComponent(String texturePath, int renderLayer) {
        this.texturePath = texturePath;
        this.textureOffset = new Vector2f(0, 0);
        this.textureSize = new Vector2f(1, 1);
        this.color = new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // White, fully opaque
        this.renderLayer = renderLayer;
        this.visible = true;
        this.scale = 1.0f;
    }
    
    /**
     * Set the color tint for this renderable.
     */
    public void setColor(float r, float g, float b, float a) {
        this.color[0] = r;
        this.color[1] = g;
        this.color[2] = b;
        this.color[3] = a;
    }
    
    /**
     * Set the color tint for this renderable.
     */
    public void setColor(float[] rgba) {
        if (rgba.length >= 4) {
            System.arraycopy(rgba, 0, this.color, 0, 4);
        }
    }
    
    /**
     * Set the alpha (transparency) of this renderable.
     */
    public void setAlpha(float alpha) {
        this.color[3] = Math.max(0, Math.min(1, alpha));
    }
    
    /**
     * Get the alpha (transparency) of this renderable.
     */
    public float getAlpha() {
        return this.color[3];
    }
    
    /**
     * Set the texture coordinates for sprite sheet rendering.
     */
    public void setTextureCoords(float offsetX, float offsetY, float sizeX, float sizeY) {
        this.textureOffset.set(offsetX, offsetY);
        this.textureSize.set(sizeX, sizeY);
    }
    
    /**
     * Set the texture coordinates for sprite sheet rendering.
     */
    public void setTextureCoords(Vector2f offset, Vector2f size) {
        this.textureOffset.set(offset);
        this.textureSize.set(size);
    }
    
    /**
     * Check if this renderable is currently visible.
     */
    public boolean isVisible() {
        return visible && color[3] > 0;
    }
    
    @Override
    public Component copy() {
        RenderableComponent copy = new RenderableComponent(texturePath, renderLayer);
        copy.textureOffset.set(textureOffset);
        copy.textureSize.set(textureSize);
        copy.setColor(color);
        copy.visible = visible;
        copy.scale = scale;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Renderable{texture=%s, layer=%d, visible=%s, alpha=%.2f}", 
                           texturePath, renderLayer, visible, color[3]);
    }
}