package net.caffeinemc.mods.sodium.client.gui.screen;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.console.Console;
import net.caffeinemc.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfigCorruptedScreen extends Screen {
    private static final List<Component> TEXT_BODY = IntStream.rangeClosed(1, 9)
            .mapToObj(i -> Component.translatable("sodium.console.config_corrupt.contents." + i))
            .collect(Collectors.toList());

    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;

    private static final int SCREEN_PADDING = 32;

    private final @Nullable Screen prevScreen;
    private final Function<Screen, Screen> nextScreen;

    public ConfigCorruptedScreen(@Nullable Screen prevScreen, @Nullable Function<Screen, Screen> nextScreen) {
        super(Component.translatable("sodium.console.config_corrupt.short"));

        this.prevScreen = prevScreen;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = this.height - SCREEN_PADDING - BUTTON_HEIGHT;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.continue"), (btn) -> {
            Console.instance().logMessage(MessageLevel.INFO, Component.translatable("sodium.console.config_file_was_reset"), 3.0);

            SodiumClientMod.restoreDefaultOptions();
            Minecraft.getInstance().setScreen(this.nextScreen.apply(this.prevScreen));
        }).bounds(this.width - SCREEN_PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), (btn) -> {
            Minecraft.getInstance().setScreen(this.prevScreen);
        }).bounds(SCREEN_PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(this.font, Component.translatable("sodium.name_renderer"), 32, 32, 0xffffff);
        graphics.drawString(this.font, Component.translatable("sodium.console.config_corrupt.title"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) {
                continue;
            }

            graphics.drawString(this.font, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
