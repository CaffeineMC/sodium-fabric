package net.caffeinemc.mods.sodium.neoforge.mixin.core.render.world;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    /**
     * @author MrMangoHands
     * @reason Redirect iteration of visible block entities to our renderer
     */
    @Overwrite
    public void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
        SodiumWorldRenderer.instance().iterateVisibleBlockEntities(blockEntityConsumer);
    }
}
