package me.jellysquid.mods.sodium.mixin.chunk_building;

import me.jellysquid.mods.sodium.client.render.chunk.CloneableBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.util.List;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements CloneableBufferBuilder {
    @Shadow
    private VertexFormat format;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private int nextDrawStart;

    @Shadow
    private int lastParameterIndex;

    @Shadow
    @Final
    private List<BufferBuilder.DrawArrayParameters> parameters;

    @Shadow
    public abstract void clear();

    @Override
    public BufferUploadData copyData() {
        BufferBuilder.DrawArrayParameters params = this.parameters.get(this.lastParameterIndex++);

        this.buffer.position(this.nextDrawStart);

        this.nextDrawStart += params.getCount() * params.getVertexFormat().getVertexSize();
        this.buffer.limit(this.nextDrawStart);

        if (this.lastParameterIndex == this.parameters.size() && this.vertexCount == 0) {
            this.clear();
        }

        ByteBuffer copy = ByteBuffer.allocateDirect(this.buffer.limit());
        copy.put(this.buffer);
        copy.flip();

        this.buffer.clear();

        return new BufferUploadData(copy, this.format);

    }
}
