package me.jellysquid.mods.sodium.mixin.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameOptions.class)
public class MixinGameOptions {
    @Shadow
    public int viewDistance;

    @Shadow
    public boolean fancyGraphics;

    /**
     * @author JellySquid
     * @reason Make the cloud render mode user-configurable
     */
    @Overwrite
    public CloudRenderMode getCloudRenderMode() {
        SodiumGameOptions options = SodiumClientMod.options();

        if (this.viewDistance < 4 || !options.quality.enableClouds) {
            return CloudRenderMode.OFF;
        }

        return options.quality.cloudQuality.isFancy(this.fancyGraphics) ? CloudRenderMode.FANCY : CloudRenderMode.FAST;
    }
}
