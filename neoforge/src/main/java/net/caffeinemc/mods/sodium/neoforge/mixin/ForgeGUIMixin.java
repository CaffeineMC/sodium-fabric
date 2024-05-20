package net.caffeinemc.mods.sodium.neoforge.mixin;

import com.google.common.base.Strings;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes a bug exclusive to Forge 1.20.1 that makes it not render text efficiently.
 */
@Mixin(value = ForgeGui.class, remap = false)
public abstract class ForgeGUIMixin {
    @Shadow
    private Font font;

    @Shadow
    public abstract Minecraft getMinecraft();

    @Inject(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", shift = At.Shift.AFTER), cancellable = true)
    private void sodium$rewriteForgeHUD(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal = 0) ArrayList<String> listL, @Local(ordinal = 1) ArrayList<String> listR) {
        ci.cancel();

        guiGraphics.drawManaged(() -> {
            renderBackdrop(guiGraphics, listL, true);
            renderBackdrop(guiGraphics, listR, false);
            renderText(guiGraphics, listL, true);
            renderText(guiGraphics, listR, false);
        });

        this.getMinecraft().getProfiler().pop();
    }

    private void renderText(GuiGraphics guiGraphics, ArrayList<String> list, boolean left) {
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (!Strings.isNullOrEmpty(string)) {
                int height = 9;
                int width = this.font.width(string);

                int x1 = left ? 2 : this.getMinecraft().getWindow().getGuiScaledWidth() - 2 - width;
                int y1 = 2 + (height * i);

                guiGraphics.drawString(font, string, x1, y1, 0xe0e0e0, false);
            }
        }
    }

    private void renderBackdrop(GuiGraphics guiGraphics, List<String> list, boolean left) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int color = 0x90505050;

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (Strings.isNullOrEmpty(string)) {
                continue;
            }

            int height = 9;
            int width = this.font.width(string);

            int x = left ? 2 : this.getMinecraft().getWindow().getGuiScaledWidth() - 2 - width;
            int y = 2 + height * i;

            int x1 = x - 1;
            int y1 = y - 1;
            int x2 = x + width + 1;
            int y2 = y + height - 1;

            guiGraphics.fill(x1, y1, x2, y2, -1873784752);
        }
    }
}