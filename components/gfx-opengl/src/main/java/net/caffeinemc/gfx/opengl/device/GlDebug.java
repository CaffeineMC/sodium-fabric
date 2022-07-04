/*
 * Copyright LWJGL. All rights reserved. Modified by IMS for use in Iris, then modified by burger for use in GFX.
 * License terms: https://www.lwjgl.org/license
 */
package net.caffeinemc.gfx.opengl.device;

import java.io.PrintStream;
import java.util.Formatter;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryUtil;

public final class GlDebug {
    
    public static void enableDebugMessages() {
        PrintStream stream = APIUtil.DEBUG_STREAM;
        
        GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
            StringBuilder builder = new StringBuilder("[OpenGL] Debug Message:\n");
            addDetail(builder, "ID", String.format("0x%X", id));
            addDetail(builder, "Source", getDebugSource(source));
            addDetail(builder, "Type", getDebugType(type));
            addDetail(builder, "Severity", getDebugSeverity(severity));
            addDetail(builder, "Message", GLDebugMessageCallback.getMessage(length, message));
            addTrace(builder);
            stream.println(builder);
        });
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[])null, true);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[])null, false);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_LOW, (int[])null, false);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[])null, false);
        GL43C.glDebugMessageCallback(proc, MemoryUtil.NULL);
        stream.println("[GFX] OpenGL debug messages enabled for this stream");
    }
    
    
    public static void disableDebugMessages() {
        PrintStream stream = APIUtil.DEBUG_STREAM;
        
        GL43C.glDebugMessageCallback(null, MemoryUtil.NULL);
        stream.println("[GFX] OpenGL debug messages disabled");
    }
    
    private static final int UNNEEDED_STACK_FRAMES = 4;
    
    private static void trace(Consumer<String> output) {
        StackWalker.getInstance().walk(stackFrameStream -> {
            stackFrameStream
                    .skip(UNNEEDED_STACK_FRAMES)
                    .forEachOrdered(stackFrame -> output.accept(stackFrame.toString()));
            return null;
        });
    }
    
    private static void addTrace(StringBuilder builder) {
        trace(new Consumer<>() {
            boolean first = true;
    
            public void accept(String str) {
                if (this.first) {
                    addDetail(builder, "Stacktrace", str);
                    this.first = false;
                } else {
                    addDetailLine(builder, "Stacktrace", str);
                }
            }
        });
    }
    
    private static void addDetail(StringBuilder builder, String type, String message) {
        new Formatter(builder).format("\t%s: %s\n", type, message);
    }
    
    private static void addDetailLine(StringBuilder builder, String type, String message) {
        builder.append("\t");
        builder.append(" ".repeat(type.length() + 2)); // add 2 to compensate for the ": "
        builder.append(message).append("\n");
    }
    
    private static String getDebugSource(int source) {
        return switch (source) {
            case 33350 -> "API";
            case 33351 -> "WINDOW SYSTEM";
            case 33352 -> "SHADER COMPILER";
            case 33353 -> "THIRD PARTY";
            case 33354 -> "APPLICATION";
            case 33355 -> "OTHER";
            default -> APIUtil.apiUnknownToken(source);
        };
    }
    
    private static String getDebugType(int type) {
        return switch (type) {
            case 33356 -> "ERROR";
            case 33357 -> "DEPRECATED BEHAVIOR";
            case 33358 -> "UNDEFINED BEHAVIOR";
            case 33359 -> "PORTABILITY";
            case 33360 -> "PERFORMANCE";
            case 33361 -> "OTHER";
            case 33384 -> "MARKER";
            default -> APIUtil.apiUnknownToken(type);
        };
    }
    
    private static String getDebugSeverity(int severity) {
        return switch (severity) {
            case 33387 -> "NOTIFICATION";
            case 37190 -> "HIGH";
            case 37191 -> "MEDIUM";
            case 37192 -> "LOW";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }
}