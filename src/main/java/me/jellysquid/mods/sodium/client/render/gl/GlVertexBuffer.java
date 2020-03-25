package me.jellysquid.mods.sodium.client.render.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.Matrix4f;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

public class GlVertexBuffer extends GlHandle {
    private static final VertexBufferFunctions FUNC = VertexBufferFunctions.pickBest(GL.getCapabilities());

    private VertexFormat vertexFormat;
    private int vertexCount = 0;

    private final int target;

    public GlVertexBuffer(int target) {
        this.setHandle(FUNC.glGenBuffers());
        this.target = target;
    }

    public void unbind() {
        FUNC.glBindBuffer(this.target, 0);
    }

    public void bind() {
        FUNC.glBindBuffer(this.target, this.handle());
    }

    public void draw(Matrix4f matrix, int mode) {
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrix);
        RenderSystem.drawArrays(mode, 0, this.vertexCount);
        RenderSystem.popMatrix();
    }

    public void delete() {
        FUNC.glDeleteBuffers(this.handle());

        this.invalidateHandle();
    }

    public static boolean isSupported() {
        return FUNC != GlVertexBuffer.VertexBufferFunctions.UNSUPPORTED;
    }

    public void upload(BufferUploadData data) {
        ByteBuffer buffer = data.buffer;
        VertexFormat format = data.format;

        this.vertexCount = buffer.remaining() / format.getVertexSize();
        this.vertexFormat = format;

        FUNC.glBindBuffer(this.target, this.handle());
        FUNC.glBufferData(this.target, buffer, GL15.GL_STATIC_DRAW);
        FUNC.glBindBuffer(this.target, 0);
    }

    private enum VertexBufferFunctions {
        BASE {
            @Override
            public void glBindBuffer(int target, int id) {
                GL15.glBindBuffer(target, id);
            }

            @Override
            public int glGenBuffers() {
                return GL15.glGenBuffers();
            }

            @Override
            public void glDeleteBuffers(int id) {
                GL15.glDeleteBuffers(id);
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                GL15.glBufferData(target, data, usage);
            }
        },
        ARB {
            @Override
            public void glBindBuffer(int target, int id) {
                ARBVertexBufferObject.glBindBufferARB(target, id);
            }

            @Override
            public int glGenBuffers() {
                return ARBVertexBufferObject.glGenBuffersARB();
            }

            @Override
            public void glDeleteBuffers(int id) {
                ARBVertexBufferObject.glDeleteBuffersARB(id);
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                ARBVertexBufferObject.glBufferDataARB(target, data, usage);
            }
        },
        UNSUPPORTED {
            @Override
            public void glBindBuffer(int target, int id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int glGenBuffers() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glDeleteBuffers(int id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                throw new UnsupportedOperationException();
            }
        };

        public static VertexBufferFunctions pickBest(GLCapabilities capabilities) {
            if (capabilities.OpenGL15) {
                return BASE;
            } else if (capabilities.GL_ARB_vertex_buffer_object) {
                return ARB;
            }

            return UNSUPPORTED;
        }

        public abstract void glBindBuffer(int target, int id);

        public abstract int glGenBuffers();

        public abstract void glDeleteBuffers(int id);

        public abstract void glBufferData(int target, ByteBuffer data, int usage);
    }

}
