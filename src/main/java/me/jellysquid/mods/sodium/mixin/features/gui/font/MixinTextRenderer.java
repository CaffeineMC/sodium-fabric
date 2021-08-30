package me.jellysquid.mods.sodium.mixin.features.gui.font;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraft/client/font/TextRenderer$Drawer")
public class MixinTextRenderer {
    @Shadow(aliases = "field_24240", remap = false)
    @Final
    private TextRenderer parent;

    private Identifier prevFontStorageId;
    private FontStorage prevFontStorage;

    @Redirect(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getFontStorage(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/font/FontStorage;"))
    private FontStorage redirectGetFontStorage(TextRenderer textRenderer, Identifier id) {
        if (this.prevFontStorageId == id) {
            return this.prevFontStorage;
        }

        this.prevFontStorageId = id;
        this.prevFontStorage = this.parent.getFontStorage(id);

        return this.prevFontStorage;
    }
}
