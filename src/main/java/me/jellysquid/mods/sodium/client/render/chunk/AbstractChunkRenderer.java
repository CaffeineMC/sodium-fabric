package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.matrix.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.render.matrix.MatrixUtil;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public abstract class AbstractChunkRenderer<T extends ChunkRenderData> implements ChunkRenderer<T> {
    private Matrix4f modelMatrix;
    private Matrix4fExtended modelMatrixExt;
    private FloatBuffer matrixBuffer;

    @Override
    public void begin(MatrixStack matrixStack) {
        RenderSystem.pushMatrix();
        matrixStack.push();

        this.modelMatrix = matrixStack.peek().getModel();
        this.modelMatrixExt = ((Matrix4fExtended) (Object) this.modelMatrix);
        this.matrixBuffer = MatrixUtil.writeToBuffer(this.modelMatrix);
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.matrixBuffer = null;
        this.modelMatrixExt = null;
        this.modelMatrix = null;

        matrixStack.pop();
        RenderSystem.popMatrix();
    }

    protected void beginChunkRender(ChunkRender<T> chunk, double x, double y, double z) {
        BlockPos origin = chunk.getOrigin();

        float offsetX = (float) (origin.getX() - x);
        float offsetY = (float) (origin.getY() - y);
        float offsetZ = (float) (origin.getZ() - z);

        this.modelMatrixExt.writeTranslation(this.matrixBuffer, offsetX, offsetY, offsetZ);

        GL11.glLoadMatrixf(this.matrixBuffer);
    }
}
