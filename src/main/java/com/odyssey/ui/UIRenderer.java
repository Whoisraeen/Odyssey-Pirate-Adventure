
package com.odyssey.ui;

import com.odyssey.graphics.Shader;
import com.odyssey.graphics.ShaderManager;
import com.odyssey.graphics.Window;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UIRenderer {
    private static final Logger logger = LoggerFactory.getLogger(UIRenderer.class);

    private final Window window;
    private final Shader uiShader;
    private final FontRenderer fontRenderer;

    private final int vao;
    private final int vbo;

    private final List<Float> vertexData = new ArrayList<>();
    private static final int MAX_VERTICES = 10000; // Max vertices for UI elements per frame

    public UIRenderer(Window window, ShaderManager shaderManager, FontRenderer fontRenderer) {
        this.window = window;
        this.uiShader = shaderManager.getShader("ui");
        this.fontRenderer = fontRenderer;

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, MAX_VERTICES * 5 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        // Position attribute
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Color attribute
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
    }

    public void beginFrame() {
        vertexData.clear();
    }

    public void endFrame() {
        if (vertexData.isEmpty()) {
            return;
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        float[] data = new float[vertexData.size()];
        for (int i = 0; i < vertexData.size(); i++) {
            data[i] = vertexData.get(i);
        }
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, data);

        uiShader.bind();
        Matrix4f projection = new Matrix4f().ortho(0.0f, window.getWidth(), window.getHeight(), 0.0f, -1.0f, 1.0f);
        uiShader.setUniform("u_projectionMatrix", projection);

        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexData.size() / 5);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL30.glBindVertexArray(0);

        uiShader.unbind();

        // Check for OpenGL errors
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            logger.error("OpenGL error during UI rendering: " + error);
        }
    }

    public void addRectangle(float x, float y, float width, float height, Vector4f color) {
        // Top-left
        vertexData.add(x);
        vertexData.add(y);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);

        // Bottom-left
        vertexData.add(x);
        vertexData.add(y + height);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);

        // Bottom-right
        vertexData.add(x + width);
        vertexData.add(y + height);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);

        // Top-left
        vertexData.add(x);
        vertexData.add(y);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);

        // Bottom-right
        vertexData.add(x + width);
        vertexData.add(y + height);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);

        // Top-right
        vertexData.add(x + width);
        vertexData.add(y);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);
    }

    public void addText(String text, float x, float y, Vector4f color) {
        fontRenderer.renderText(text, x, y, color);
    }

    // Convenience methods for UI components
    public void drawRectangle(float x, float y, float width, float height, Vector4f color) {
        addRectangle(x, y, width, height, color);
    }

    public void drawText(String text, float x, float y, Vector4f color) {
        fontRenderer.renderText(text, x, y, color);
    }

    public void drawText(String text, float x, float y, float fontSize, Vector4f color) {
        fontRenderer.renderText(text, x, y, fontSize, color);
    }

    public float getTextWidth(String text, float fontSize) {
        return fontRenderer.getTextWidth(text, fontSize);
    }

    public float getLineHeight(float fontSize) {
        return fontRenderer.getLineHeight(fontSize);
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
    }
}
