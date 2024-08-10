package net.caffeinemc.mods.sodium.mixin.core.render;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin<T extends BlockEntity> implements ExtendedBlockEntityType<T> {
    @Unique
    private Predicate<? super T> sodium$renderPredicate = $ -> true;

    @Override
    public Predicate<? super T> sodium$getRenderPredicate() {
        return sodium$renderPredicate;
    }

    @Override
    public void sodium$setRenderPredicate(Predicate<? super T> predicate) {
        this.sodium$renderPredicate = predicate;
    }
}
