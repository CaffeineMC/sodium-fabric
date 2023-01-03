package me.jellysquid.mods.sodium.client.util.workarounds;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;

public class DriverProbeEntrypoint {
    public static void main(String[] args) {
        GLFW.glfwInit();
        GLFW.glfwDefaultWindowHints();;
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        var window = GLFW.glfwCreateWindow(854, 480, "Minecraft", MemoryUtil.NULL, MemoryUtil.NULL);

        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        var fields = new HashMap<String, String>();
        fields.put("vendor", GL30C.glGetString(GL30C.GL_VENDOR));
        fields.put("version", GL30C.glGetString(GL30C.GL_VERSION));
        fields.put("renderer", GL30C.glGetString(GL30C.GL_RENDERER));

        System.out.print(DriverProbe.encodeResponse(fields));

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

}
