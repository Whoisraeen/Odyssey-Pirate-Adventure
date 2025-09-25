package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class DynamicSky {

    private Shader shader;
    private int vao;
    private int vbo;

    public DynamicSky() {
        shader = new Shader("dynamic_sky");
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

    public void render(Camera camera, Matrix4f projection, Vector3f sunDirection) {
        shader.bind();
        shader.setUniform("view", camera.getViewMatrix());
        shader.setUniform("projection", projection);
        shader.setUniform("sunDirection", sunDirection);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
    }
}
