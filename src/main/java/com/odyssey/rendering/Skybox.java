package com.odyssey.rendering;

import com.odyssey.core.ResourceManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

public class Skybox {

    private int vao;
    private int vbo;
    private int textureId;
    private Shader shader;

    public Skybox() {
        float[] vertices = {
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

        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL11.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL11.GL_ARRAY_BUFFER, vertices, GL11.GL_STATIC_DRAW);
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0);

        List<String> faces = new ArrayList<>();
        faces.add("src/main/resources/textures/skybox/right.jpg");
        faces.add("src/main/resources/textures/skybox/left.jpg");
        faces.add("src/main/resources/textures/skybox/top.jpg");
        faces.add("src/main/resources/textures/skybox/bottom.jpg");
        faces.add("src/main/resources/textures/skybox/front.jpg");
        faces.add("src/main/resources/textures/skybox/back.jpg");

        textureId = loadCubemap(faces);

        shader = new Shader("skybox");
        shader.compile();
    }

    public void render(Camera camera, Matrix4f projection) {
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        shader.bind();
        shader.setUniform("view", camera.getViewMatrix());
        shader.setUniform("projection", projection);

        GL30.glBindVertexArray(vao);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, textureId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);
        GL30.glBindVertexArray(0);
        GL11.glDepthFunc(GL11.GL_LESS);
    }

    private int loadCubemap(List<String> faces) {
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);

        for (int i = 0; i < faces.size(); i++) {
            try {
                Texture texture = ResourceManager.getInstance().loadTexture(faces.get(i));
                GL11.glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_RGB, texture.getWidth(), texture.getHeight(), 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, texture.getByteBuffer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);

        return textureID;
    }
}
