package me.jellysquid.mods.sodium.client.gui.widgets;

import net.minecraft.client.MinecraftClient;

public class WidgetColors {
    public static int getBackgroundColor() {
        return usesHighContrast() ? 0xff000000: 0x90000000;
    }

    public static int getHoveredFocusedColor() {
        return usesHighContrast() ? 0xe50d4039 : 0xE0000000;
    }

    public static int getBorderColor() {
        return usesHighContrast() ? 0xff57ffe1 : -1;
    }

    public static int getSliderColor() {
        return usesHighContrast() ? 0xffb88bf8 : 0xFFFFFFFF;
    }

    public static int getDisabledColor() {
        return usesHighContrast() ? 0xff949494 : 0x60000000;
    }

    public static boolean usesHighContrast() {
        return MinecraftClient.getInstance().options.getHighContrast().getValue();
    }
}
