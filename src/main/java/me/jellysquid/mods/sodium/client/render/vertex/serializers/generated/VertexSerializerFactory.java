package me.jellysquid.mods.sodium.client.render.vertex.serializers.generated;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.MemoryTransfer;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.*;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class VertexSerializerFactory {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static Bytecode generate(List<MemoryTransfer> memoryCopies, VertexFormatDescription srcFormat, VertexFormatDescription dstFormat, String identifier) {
        var name = "me/jellysquid/mods/sodium/client/render/vertex/serializers/generated/VertexSerializer$Impl$" + identifier;

        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V17, Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC, name, null,
                Type.getInternalName(Object.class),
                new String[] { Type.getInternalName(VertexSerializer.class) });

        {
            // Local variable table slots
            final int localThis = 0;

            // Constructor method
            MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();

            // Call the super class constructor
            Label labelInit = new Label();
            methodVisitor.visitLabel(labelInit);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, localThis);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    "java/lang/Object", "<init>", "()V", false);

            // Exit the function
            Label labelExit = new Label();
            methodVisitor.visitLabel(labelExit);
            methodVisitor.visitInsn(Opcodes.RETURN);

            // Describe the local variable table
            Label labelEnd = new Label();
            methodVisitor.visitLabel(labelEnd);
            methodVisitor.visitLocalVariable("this",
                    "L" + name + ";", null, labelInit, labelEnd, localThis);
            methodVisitor.visitMaxs(2, 1);
            methodVisitor.visitEnd();
        }

        {
            // Local variable table slots
            final int localThis = 0;
            final int localSrcPointer = 1;
            final int localDstPointer = 3;
            final int localVertexCount = 5;
            final int localVertexIndex = 6;

            // Serialization method
            MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "serialize", "(JJI)V", null, null);
            methodVisitor.visitCode();

            // Set up the loop's accumulator (vertexIndex)
            Label labelLoopInit = new Label();
            methodVisitor.visitLabel(labelLoopInit);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitVarInsn(Opcodes.ISTORE, localVertexIndex);

            // Set up the loop's operands for a comparison (vertexIndex, vertexStart)
            Label labelLoopConditionSetup = new Label();
            methodVisitor.visitLabel(labelLoopConditionSetup);
            methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, localVertexIndex);
            methodVisitor.visitVarInsn(Opcodes.ILOAD, localVertexCount);

            // Check the loop's condition (vertexIndex < vertexCount)
            Label labelLoopConditionComparison = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, labelLoopConditionComparison);

            // Generate instructions for each copy command
            for (var op : memoryCopies) {
                int i = 0;

                while (i < op.length()) {
                    int remaining = op.length() - i;

                    Label labelMemoryTransfer = new Label();
                    methodVisitor.visitLabel(labelMemoryTransfer);

                    // Calculate the destination pointer
                    methodVisitor.visitVarInsn(Opcodes.LLOAD, localDstPointer);
                    methodVisitor.visitLdcInsn((long) (op.dst() + i));
                    methodVisitor.visitInsn(Opcodes.LADD);

                    // Calculate the source pointer
                    methodVisitor.visitVarInsn(Opcodes.LLOAD, localSrcPointer);
                    methodVisitor.visitLdcInsn((long) (op.src() + i));
                    methodVisitor.visitInsn(Opcodes.LADD);

                    if (remaining >= 8) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memGetLong", "(J)J", false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memPutLong", "(JJ)V", false);

                        i += 8;
                    } else if (remaining >= 4) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memGetInt", "(J)I", false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memPutInt", "(JI)V", false);

                        i += 4;
                    } else if (remaining >= 2) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memGetShort", "(J)S", false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memPutShort", "(JS)V", false);

                        i += 2;
                    } else {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memGetByte", "(J)B", false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MemoryUtil.class), "memPutByte", "(JB)V", false);

                        i += 1;
                    }
                }
            }

            // Increment source pointer
            Label labelIncrementSourcePointer = new Label();
            methodVisitor.visitLabel(labelIncrementSourcePointer);
            methodVisitor.visitVarInsn(Opcodes.LLOAD, localSrcPointer);
            methodVisitor.visitLdcInsn((long) srcFormat.stride);
            methodVisitor.visitInsn(Opcodes.LADD);
            methodVisitor.visitVarInsn(Opcodes.LSTORE, localSrcPointer);

            // Increment destination pointer
            Label labelIncrementDestinationPointer = new Label();
            methodVisitor.visitLabel(labelIncrementDestinationPointer);
            methodVisitor.visitVarInsn(Opcodes.LLOAD, localDstPointer);
            methodVisitor.visitLdcInsn((long) dstFormat.stride);
            methodVisitor.visitInsn(Opcodes.LADD);
            methodVisitor.visitVarInsn(Opcodes.LSTORE, localDstPointer);

            // Restart the loop if the condition still holds, otherwise exit
            Label labelRestartLoop = new Label();
            methodVisitor.visitLabel(labelRestartLoop);
            methodVisitor.visitIincInsn(localVertexIndex, 1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, labelLoopConditionSetup);
            methodVisitor.visitLabel(labelLoopConditionComparison);
            methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            methodVisitor.visitInsn(Opcodes.RETURN);

            // Describe the local variable table
            Label labelExit = new Label();
            methodVisitor.visitLabel(labelExit);
            methodVisitor.visitLocalVariable("this", "L" + name + ";", null, labelLoopInit, labelExit, localThis);
            methodVisitor.visitLocalVariable("src", "J", null, labelLoopInit, labelExit, localSrcPointer);
            methodVisitor.visitLocalVariable("dst", "J", null, labelLoopInit, labelExit, localDstPointer);
            methodVisitor.visitLocalVariable("vertexCount", "I", null, labelLoopInit, labelExit, localVertexCount);
            methodVisitor.visitLocalVariable("vertexIndex", "I", null, labelLoopConditionSetup, labelLoopConditionComparison, localVertexIndex);
            methodVisitor.visitMaxs(6, 7);
            methodVisitor.visitEnd();
        }

        classWriter.visitEnd();

        return new Bytecode(classWriter.toByteArray());
    }

    public static Class<?> define(Bytecode bytecode) {
        try {
            return LOOKUP.defineClass(bytecode.data);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access generated class", e);
        }
    }

    public static final class Bytecode {
        private final byte[] data;

        // We really don't want people defining random classes in our package
        private Bytecode(byte[] data) {
            this.data = data;
        }

        // Ensure nobody can get a mutable reference to the bytecode
        public byte[] copy() {
            return this.data.clone();
        }
    }
}
