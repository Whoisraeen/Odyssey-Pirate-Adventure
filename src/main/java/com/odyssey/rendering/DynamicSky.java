package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class DynamicSky {

    private Shader shader;
    private int vao;
    private int vbo;

    public DynamicSky() {
        // Create shader using ShaderManager's built-in skybox shader
        shader = new Shader("dynamic_sky");
        
        // Create skybox shader source
        String vertexSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 a_Position;\n" +
                "\n" +
                "uniform mat4 u_ViewMatrix;\n" +
                "uniform mat4 u_ProjectionMatrix;\n" +
                "\n" +
                "out vec3 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    v_TexCoord = a_Position;\n" +
                "    vec4 pos = u_ProjectionMatrix * u_ViewMatrix * vec4(a_Position, 1.0);\n" +
                "    gl_Position = pos.xyww;\n" +
                "}";
        
        String fragmentSource = "#version 330 core\n" +
                "in vec3 v_TexCoord;\n" +
                "\n" +
                "uniform vec3 u_SunDirection;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 direction = normalize(v_TexCoord);\n" +
                "    \n" +
                "    // Sky gradient based on height\n" +
                "    float height = direction.y;\n" +
                "    vec3 skyColor = mix(vec3(0.5, 0.7, 1.0), vec3(0.1, 0.3, 0.8), height);\n" +
                "    \n" +
                "    // Sun effect\n" +
                "    float sunDot = max(dot(direction, normalize(u_SunDirection)), 0.0);\n" +
                "    vec3 sunColor = vec3(1.0, 0.8, 0.6) * pow(sunDot, 256.0);\n" +
                "    \n" +
                "    vec3 color = skyColor + sunColor;\n" +
                "    FragColor = vec4(color, 1.0);\n" +
                "}";
        
        // Compile shader
        shader.compileVertexShader(vertexSource);
        shader.compileFragmentShader(fragmentSource);
        shader.link();

        float[] vertices = {
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
    }

    public void render(Camera camera, Matrix4f projection, Vector3f sunDirection) {
        shader.bind();
        shader.setUniform("u_ViewMatrix", camera.getViewMatrix());
        shader.setUniform("u_ProjectionMatrix", projection);
        shader.setUniform("u_SunDirection", sunDirection);
        shader.setUniform("u_Time", System.currentTimeMillis() / 1000.0f);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
    }
}
