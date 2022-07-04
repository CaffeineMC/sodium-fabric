/*
 * Copyright LWJGL. All rights reserved. Modified by IMS for use in Iris, then modified by burger for use in GFX.
 * License terms: https://www.lwjgl.org/license
 */
package net.caffeinemc.gfx.opengl.device;

import java.io.PrintStream;
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
            stream.println("[OpenGL] Debug Message:");
            printDetail(stream, "ID", String.format("0x%X", id));
            printDetail(stream, "Source", getDebugSource(source));
            printDetail(stream, "Type", getDebugType(type));
            printDetail(stream, "Severity", getDebugSeverity(severity));
            printDetail(stream, "Message", GLDebugMessageCallback.getMessage(length, message));
            printTrace(stream);
        });
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[])null, true);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[])null, true);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_LOW, (int[])null, true);
        GL43C.glDebugMessageControl(GL11C.GL_DONT_CARE, GL11C.GL_DONT_CARE, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[])null, true);
        GL43C.glDebugMessageCallback(proc, MemoryUtil.NULL);
        stream.println("[GFX] OpenGL debug messages enabled for this stream");
    }
    
    
    public static void disableDebugMessages() {
        PrintStream stream = APIUtil.DEBUG_STREAM;
        
        GL43C.glDebugMessageCallback(null, MemoryUtil.NULL);
        stream.println("[GFX] OpenGL debug messages disabled");
    }
    
    private static void trace(Consumer<String> output) {
        /*
         * We can not just use a fixed stacktrace element offset, because some methods
         * are intercepted and some are not. So, check the package name.
         */
        StackTraceElement[] elems = filterStackTrace(new Throwable(), 4).getStackTrace();
        for (StackTraceElement ste : elems) {
            output.accept(ste.toString());
        }
    }
    
    public static Throwable filterStackTrace(Throwable throwable, int offset) {
        StackTraceElement[] elems = throwable.getStackTrace();
        StackTraceElement[] filtered = new StackTraceElement[elems.length];
        int j = 0;
        for (int i = offset; i < elems.length; i++) {
            String className = elems[i].getClassName();
            if (className == null) {
                className = "";
            }
            filtered[j++] = elems[i];
        }
        StackTraceElement[] newElems = new StackTraceElement[j];
        System.arraycopy(filtered, 0, newElems, 0, j);
        throwable.setStackTrace(newElems);
        return throwable;
    }
    
    private static void printTrace(PrintStream stream) {
        trace(new Consumer<>() {
            boolean first = true;
    
            public void accept(String str) {
                if (this.first) {
                    printDetail(stream, "Stacktrace", str);
                    this.first = false;
                } else {
                    printDetailLine(stream, "Stacktrace", str);
                }
            }
        });
    }
    
    private static void printDetail(PrintStream stream, String type, String message) {
        stream.printf("\t%s: %s\n", type, message);
    }
    
    private static void printDetailLine(PrintStream stream, String type, String message) {
        stream.append("    ");
        for (int i = 0; i < type.length(); i++) {
            stream.append(" ");
        }
        stream.append(message).append("\n");
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