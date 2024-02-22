package net.caffeinemc.mods.sodium.client.gui.options;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum OptionFlag implements TextProvider {
    REQUIRES_RENDERER_RELOAD(ChatFormatting.GREEN, "sodium.options.flag_renderer_reload"),
    REQUIRES_RENDERER_UPDATE(ChatFormatting.YELLOW, "sodium.options.flag_renderer_update"),
    REQUIRES_ASSET_RELOAD(ChatFormatting.GOLD, "sodium.options.flag_assets_reload"),
    REQUIRES_GAME_RESTART(ChatFormatting.RED, "sodium.options.flag_game_restart");

    private final Component text;

    OptionFlag(ChatFormatting color, String text) {
        this.text = Component.translatable(text).withStyle(color);
    }

    @Override
    public Component getLocalizedName() {
        return this.text;
    }
}
