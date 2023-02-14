package me.jellysquid.mods.sodium.mixin.features.optimized_stitching;

import net.minecraft.client.texture.TextureStitcher;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(TextureStitcher.class)
public class MixinTextureStitcher<T extends TextureStitcher.Stitchable> {
    @Shadow
    @Final
    private List<TextureStitcher.Slot<T>> slots;
    @Shadow
    private int width;
    @Shadow
    private int height;
    @Shadow
    @Final
    private int maxWidth;
    @Shadow
    @Final
    private int maxHeight;

    /**
     * This method is a copy of the vanilla method with the growWidth calculation rewritten.
     *
     * @author PepperCode1
     * @reason Optimize region creation to allow for a smaller atlas
     */
    @Overwrite
    private boolean growAndFit(TextureStitcher.Holder<T> holder) {
        int newEffectiveWidth = MathHelper.smallestEncompassingPowerOfTwo(width + holder.width());
        int newEffectiveHeight = MathHelper.smallestEncompassingPowerOfTwo(height + holder.height());
        boolean canFitWidth = newEffectiveWidth <= maxWidth;
        boolean canFitHeight = newEffectiveHeight <= maxHeight;

        if (!canFitWidth && !canFitHeight) {
            return false;
        }

        // Sodium start
        boolean growWidth;
        /*
         * Vanilla performs logic that can result in the atlas height
         * exceeding the maximum allowed value. The easiest solution
         * is employed here - short-circuiting the logic if only one
         * dimension can fit. This entirely prevents the issue and has
         * the bonus of making the code easier to understand.
         */
        if (canFitWidth & canFitHeight) {
            // Effective size calculation moved from head to be inside if block
            int effectiveWidth = MathHelper.smallestEncompassingPowerOfTwo(width);
            int effectiveHeight = MathHelper.smallestEncompassingPowerOfTwo(height);
            boolean wouldGrowEffectiveWidth = effectiveWidth != newEffectiveWidth;
            boolean wouldGrowEffectiveHeight = effectiveHeight != newEffectiveHeight;

            if (wouldGrowEffectiveWidth) {
                /*
                 * The logic here differs from vanilla slightly as it combines the
                 * vanilla checks together into the if statement.
                 *
                 * If the effective height would not be grown:
                 *   Under the same conditions, vanilla would grow the width instead
                 *   of the height. This is inefficient because it can potentially
                 *   increase the atlas width, which is (usually) unnecessary since
                 *   there is already enough free space on the bottom to not grow the
                 *   atlas size.
                 *
                 * If the effective height would be grown but the width is greater
                 * than the height:
                 *   Under the same conditions, vanilla would always grow the height.
                 *   However, not performing the additional check would cause some
                 *   edge cases to result in cut-off sprites. The following case is
                 *   one such example.
                 *   1. The first sprite is added. Its dimensions measure 128x32.
                 *   2. The second sprite is added. Its dimensions measure 256x16.
                 */
                if (wouldGrowEffectiveHeight && effectiveWidth <= effectiveHeight) {
                    /*
                     * If both the width and height would be grown, definitively grow the
                     * width if it is less than or equal to the height.
                     */
                    growWidth = true;
                } else {
                    /*
                     * At this point, the height should be grown to maximize atlas space
                     * usage, but this is not always possible to do. Hence, the following
                     * check is employed to ensure that the height can actually be grown.
                     *
                     * If the height is grown, the new region's width is equal to the total
                     * width. However, if the holder's width is larger than the total width,
                     * the new region will not be able to contain it. Therefore, the width
                     * should be grown instead.
                     *
                     * By extension, this check does not have to be performed if growWidth
                     * is already true.
                     *
                     * This check does not have to be performed when wouldGrowEffectiveWidth
                     * is false because this means that the total width would be less than
                     * doubled, meaning that the holder width is less than the total width.
                     *
                     * A similar check does not have to be performed for the heights due to
                     * the sprite sorting - taller sprites are always added before shorter
                     * ones.
                     *
                     * Vanilla does not perform this check.
                     */
                    growWidth = holder.width() > width;
                }
            } else {
                if (wouldGrowEffectiveHeight) {
                    /*
                     * If the height would be grown but the width would not be, grow the
                     * width to fill up the unused space on the right.
                     *
                     * Under the same conditions, vanilla would grow the height instead
                     * of the width. This is inefficient because it can potentially
                     * increase the atlas height, which is unnecessary since there is
                     * already enough free space on the right to not grow the atlas
                     * size.
                     */
                    growWidth = true;
                } else {
                    /*
                     * If neither the width nor height would be grown, grow the dimension
                     * that is smaller than the other. If both dimensions are already the
                     * same, grow the width.
                     */
                    growWidth = effectiveWidth <= effectiveHeight;
                }
            }
        } else {
            growWidth = canFitWidth;
        }
        // Sodium end

        TextureStitcher.Slot<T> slot;
        if (growWidth) {
            if (height == 0) {
                height = holder.height();
            }

            slot = new TextureStitcher.Slot<>(width, 0, holder.width(), height);
            width += holder.width();
        } else {
            slot = new TextureStitcher.Slot<>(0, height, width, holder.height());
            height += holder.height();
        }

        slot.fit(holder);
        slots.add(slot);
        return true;
    }
}
