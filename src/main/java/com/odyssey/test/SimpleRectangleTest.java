package com.odyssey.test;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

/**
 * Simple test to verify red rectangle rendering works.
 * This is a minimal LWJGL application to test our rendering setup.
 */
public class SimpleRectangleTest {
    
    private long window;
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        
        // Create window
        window = GLFW.glfwCreateWindow(800, 600, "Red Rectangle Test", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Make OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // Enable v-sync
        GLFW.glfwShowWindow(window);
        
        // Initialize OpenGL
        GL.createCapabilities();
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }
    
    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Clear screen
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            
            // Draw red rectangle
            GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red color
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-0.5f, -0.5f); // Bottom-left
            GL11.glVertex2f(0.5f, -0.5f);  // Bottom-right
            GL11.glVertex2f(0.5f, 0.5f);   // Top-right
            GL11.glVertex2f(-0.5f, 0.5f);  // Top-left
            GL11.glEnd();
            
            // Swap buffers and poll events
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }
    
    private void cleanup() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Red Rectangle Test...");
        new SimpleRectangleTest().run();
        System.out.println("Test completed.");
    }
}