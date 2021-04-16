package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.state.GlStateTracker;
import me.jellysquid.mods.sodium.client.gl.tessellation.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLRenderDevice implements RenderDevice {
    private final GlStateTracker stateTracker = new GlStateTracker();
    private final CommandList commandList = new ImmediateCommandList(this.stateTracker);
    private final DrawCommandList drawCommandList = new ImmediateDrawCommandList();

    private boolean isActive;
    private GlTessellation activeTessellation;

    @Override
    public CommandList createCommandList() {
        GLRenderDevice.this.checkDeviceActive();

        return this.commandList;
    }

    @Override
    public void makeActive() {
        if (this.isActive) {
            return;
        }

        this.stateTracker.clearRestoreState();
        this.isActive = true;
    }

    @Override
    public void makeInactive() {
        if (!this.isActive) {
            return;
        }

        this.stateTracker.applyRestoreState();
        this.isActive = false;
    }

    private void checkDeviceActive() {
        if (!this.isActive) {
            throw new IllegalStateException("Tried to access device from unmanaged context");
        }
    }

    private class ImmediateCommandList implements CommandList {
        private final GlStateTracker stateTracker;

        private ImmediateCommandList(GlStateTracker stateTracker) {
            this.stateTracker = stateTracker;
        }

        @Override
        public void bindVertexArray(GlVertexArray array) {
            if (this.stateTracker.makeVertexArrayActive(array)) {
                GlFunctions.VERTEX_ARRAY.glBindVertexArray(array.handle());
            }
        }

        @Override
        public void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer) {
            this.bindBuffer(GlBufferTarget.ARRAY_BUFFER, glBuffer);

            GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), byteBuffer, glBuffer.getUsageHint().getId());

            glBuffer.setSize(byteBuffer.limit());
        }

        @Override
        public void copyBufferSubData(GlBuffer src, GlMutableBuffer dst, long readOffset, long writeOffset, long bytes) {
            if (dst.getSize() <= writeOffset + bytes) {
                throw new IllegalArgumentException("Not enough space in destination buffer (writeOffset + bytes > bufferSize)");
            }

            this.bindBuffer(GlBufferTarget.COPY_READ_BUFFER, src);
            this.bindBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst);

            GlFunctions.BUFFER_COPY.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, bytes);
        }

        @Override
        public void bindBuffer(GlBufferTarget target, GlBuffer buffer) {
            if (this.stateTracker.makeBufferActive(target, buffer)) {
                GL20C.glBindBuffer(target.getTargetParameter(), buffer.handle());
            }
        }

        @Override
        public void unbindBuffer(GlBufferTarget target) {
            if (this.stateTracker.makeBufferActive(target, null)) {
                GL20C.glBindBuffer(target.getTargetParameter(), GlBuffer.NULL_BUFFER_ID);
            }
        }

        @Override
        public void unbindVertexArray() {
            if (this.stateTracker.makeVertexArrayActive(null)) {
                GlFunctions.VERTEX_ARRAY.glBindVertexArray(GlVertexArray.NULL_ARRAY_ID);
            }
        }

        @Override
        public void invalidateBuffer(GlMutableBuffer glBuffer) {
            this.allocateBuffer(GlBufferTarget.ARRAY_BUFFER, glBuffer, 0L);
        }

        @Override
        public void allocateBuffer(GlBufferTarget target, GlMutableBuffer buffer, long bufferSize) {
            this.bindBuffer(target, buffer);

            GL20C.glBufferData(target.getTargetParameter(), bufferSize, buffer.getUsageHint().getId());
            buffer.setSize(bufferSize);
        }

        @Override
        public void deleteBuffer(GlBuffer buffer) {
            int handle = buffer.handle();
            buffer.invalidateHandle();

            GL20C.glDeleteBuffers(handle);
        }

        @Override
        public void deleteVertexArray(GlVertexArray array) {
            int handle = array.handle();
            array.invalidateHandle();

            GlFunctions.VERTEX_ARRAY.glDeleteVertexArrays(handle);
        }

        @Override
        public void flush() {
            // NO-OP
        }

        @Override
        public DrawCommandList beginTessellating(GlTessellation tessellation) {
            GLRenderDevice.this.activeTessellation = tessellation;
            GLRenderDevice.this.activeTessellation.bind(GLRenderDevice.this.commandList);

            return GLRenderDevice.this.drawCommandList;
        }

        @Override
        public void deleteTessellation(GlTessellation tessellation) {
            tessellation.delete(this);
        }

        @Override
        public GlVertexArray createVertexArray() {
            return new GlVertexArray(GLRenderDevice.this);
        }

        @Override
        public GlMutableBuffer createMutableBuffer(GlBufferUsage usage) {
            return new GlMutableBuffer(GLRenderDevice.this, usage);
        }

        @Override
        public GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings) {
            if (GlVertexArrayTessellation.isSupported()) {
                GlVertexArrayTessellation tessellation = new GlVertexArrayTessellation(new GlVertexArray(GLRenderDevice.this), primitiveType, bindings);
                tessellation.init(this);

                return tessellation;
            } else {
                return new GlFallbackTessellation(primitiveType, bindings);
            }
        }
    }

    private class ImmediateDrawCommandList implements DrawCommandList {
        public ImmediateDrawCommandList() {

        }

        @Override
        public void multiDrawArrays(IntBuffer first, IntBuffer count) {
            GlPrimitiveType primitiveType = GLRenderDevice.this.activeTessellation.getPrimitiveType();
            GL20C.glMultiDrawArrays(primitiveType.getId(), first, count);
        }

        @Override
        public void multiDrawArraysIndirect(long pointer, int count, int stride) {
            GlPrimitiveType primitiveType = GLRenderDevice.this.activeTessellation.getPrimitiveType();
            GlFunctions.INDIRECT_DRAW.glMultiDrawArraysIndirect(primitiveType.getId(), pointer, count, stride);
        }

        @Override
        public void endTessellating() {
            GLRenderDevice.this.activeTessellation.unbind(GLRenderDevice.this.commandList);
            GLRenderDevice.this.activeTessellation = null;
        }

        @Override
        public void flush() {
            if (GLRenderDevice.this.activeTessellation != null) {
                this.endTessellating();
            }
        }
    }
}
