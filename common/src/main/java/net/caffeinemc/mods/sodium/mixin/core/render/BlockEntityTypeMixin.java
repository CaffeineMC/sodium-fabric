package net.caffeinemc.mods.sodium.mixin.core.render;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin<T extends BlockEntity> implements ExtendedBlockEntityType<T> {
    @Unique
    private Predicate<? super T>[] sodium$renderPredicates = new Predicate[0];

    @Override
    public Predicate<? super T>[] sodium$getRenderPredicates() {
        return sodium$renderPredicates;
    }

    @Override
    public void sodium$addRenderPredicate(Predicate<? super T> predicate) {
        sodium$renderPredicates = ArrayUtils.add(sodium$renderPredicates, predicate);
    }

    @Override
    public boolean sodium$removeRenderPredicate(Predicate<? super T> predicate) {
        int prevSize = sodium$renderPredicates.length;

        sodium$renderPredicates = ArrayUtils.removeElement(sodium$renderPredicates, predicate);

        return sodium$renderPredicates.length != prevSize;
    }
}
