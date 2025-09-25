package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class CascadedShadowMap {

    private int[] fbos;
    private int[] shadowMaps;
    private int shadowMapSize;
    private int numCascades;

    public CascadedShadowMap(int shadowMapSize, int numCascades) {
        this.shadowMapSize = shadowMapSize;
        this.numCascades = numCascades;

        fbos = new int[numCascades];
        shadowMaps = new int[numCascades];

        GL30.glGenFramebuffers(fbos);
        GL11.glGenTextures(shadowMaps);

        for (int i = 0; i < numCascades; i++) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMaps[i]);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, shadowMapSize, shadowMapSize, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_BORDER);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_BORDER);
            float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
            GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i]);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, shadowMaps[i], 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    public void bind(int cascade) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[cascade]);
        GL11.glViewport(0, 0, shadowMapSize, shadowMapSize);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public int getShadowMap(int cascade) {
        return shadowMaps[cascade];
    }
}
