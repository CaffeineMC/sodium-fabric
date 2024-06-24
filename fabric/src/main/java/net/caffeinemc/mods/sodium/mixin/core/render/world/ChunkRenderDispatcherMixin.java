package net.caffeinemc.mods.sodium.mixin.core.render.world;


import com.google.common.collect.Lists;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithExpectedSize(I)Ljava/util/ArrayList;", remap = false))
    private ArrayList<ChunkBufferBuilderPack> modifyThreadPoolSize(int prev) {
        // Do not allow any resources to be allocated
        return Lists.newArrayListWithExpectedSize(0);
    }
}
