package com.core.reactive.corereactive.overlay;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Overlay {
    private Long window;

    public long init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Error initiating GLFW");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
        long window = GLFW.glfwCreateWindow(1920, 1080, "Overlay Window", 0, 0);
        this.window = window;
        if (window == 0) {
            throw new IllegalStateException("Error creating window");
        }
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);

        GLFW.glfwSetWindowPos(window, 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 1920, 0, 1080, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        return window;
    }
    public void terminate() {
        GLFW.glfwTerminate();
    }
}
