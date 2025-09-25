package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class VolumetricFog {

    private Shader shader;
    private int vao;
    private int vbo;

    public VolumetricFog() {
        shader = new Shader("volumetric_fog");
        shader.compile();

        float[] vertices = {
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL11.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL11.GL_ARRAY_BUFFER, vertices, GL11.GL_STATIC_DRAW);
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
    }

    public void render(Camera camera, Matrix4f projection, int depthTexture) {
        shader.bind();
        shader.setUniform("view", camera.getViewMatrix());
        shader.setUniform("projection", projection);
        shader.setUniform("depth", 0);

        GL11.glActiveTexture(GL11.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
    }
}
