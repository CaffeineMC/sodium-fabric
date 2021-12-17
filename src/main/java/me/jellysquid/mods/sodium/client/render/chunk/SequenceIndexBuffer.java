package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;

public class SequenceIndexBuffer {
    private final GlMutableBuffer buffer;

    private int size;

    public SequenceIndexBuffer(RenderDevice device, int initialSize) {
        try (CommandList commandList = device.createCommandList()) {
            this.buffer = commandList.createMutableBuffer();
            this.allocate(commandList, initialSize);
        }
    }

    public boolean ensureCapacity(CommandList commandList, int size) {
        if (this.size >= size) {
            return false;
        }

        this.allocate(commandList, Math.max(this.size * 2, this.size + size));

        return true;
    }

    private void allocate(CommandList commandList, int size) {
        commandList.allocateStorage(this.buffer, (size * 6L) * 4L, GlBufferUsage.STATIC_DRAW);

        var map =  commandList.mapBuffer(this.buffer, 0, this.buffer.getSize(),
                EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE));

        var intBuf = map.getMemoryBuffer()
                .asIntBuffer();

        for (int quadIndex = 0; quadIndex < size; quadIndex++) {
            var vertexIndex = quadIndex * 4;
            intBuf.put(vertexIndex + 0);
            intBuf.put(vertexIndex + 1);
            intBuf.put(vertexIndex + 2);
            intBuf.put(vertexIndex + 2);
            intBuf.put(vertexIndex + 3);
            intBuf.put(vertexIndex + 0);
        }

        commandList.unmap(map);

        this.size = size;
    }

    public GlBuffer getBufferObject() {
        return this.buffer;
    }
}
