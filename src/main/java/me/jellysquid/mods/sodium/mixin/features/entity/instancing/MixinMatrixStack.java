package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.matrix.MatrixStackExtended;
import me.jellysquid.mods.sodium.render.entity.data.InstanceBatch;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MatrixStack.class)
public class MixinMatrixStack implements MatrixStackExtended {

    private InstanceBatch batch;

    @Override
    public InstanceBatch getBatch() {
        return batch;
    }

    @Override
    public void setBatch(InstanceBatch batch) {
        this.batch = batch;
    }
}
