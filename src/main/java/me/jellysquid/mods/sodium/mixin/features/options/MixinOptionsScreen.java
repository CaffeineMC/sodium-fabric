package me.jellysquid.mods.sodium.mixin.features.options;

import me.jellysquid.mods.sodium.gui.screen.UserConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class MixinOptionsScreen extends Screen {
    protected MixinOptionsScreen(Component title) {
        super(title);
    }

    @Dynamic
    @Inject(method = "lambda$init$4", at = @At("HEAD"), cancellable = true)
    private void open(Button widget, CallbackInfo ci) {
        this.minecraft.setScreen(new UserConfigScreen(this));

        ci.cancel();
    }
}
