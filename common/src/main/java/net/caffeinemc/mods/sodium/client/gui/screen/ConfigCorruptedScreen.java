package net.caffeinemc.mods.sodium.client.gui.screen;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
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

public class ConfigCorruptedScreen extends Screen {
    private static final String TEXT_BODY_RAW = """
        A problem occurred while trying to load the configuration file. This
        can happen when the file has been corrupted on disk, or when trying
        to manually edit the file by hand.
        
        If you continue, the configuration file will be reset back to known-good
        defaults, and you will lose any changes that have since been made to your
        Video Settings.
        
        More information about the error can be found in the log file.
        """;

    private static final List<Component> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(Component::literal)
            .collect(Collectors.toList());

    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;

    private static final int SCREEN_PADDING = 32;

    private final @Nullable Screen prevScreen;
    private final Function<Screen, Screen> nextScreen;

    public ConfigCorruptedScreen(@Nullable Screen prevScreen, @Nullable Function<Screen, Screen> nextScreen) {
        super(Component.literal("Sodium failed to load the configuration file"));

        this.prevScreen = prevScreen;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = this.height - SCREEN_PADDING - BUTTON_HEIGHT;

        this.addRenderableWidget(Button.builder(Component.literal("Continue"), (btn) -> {
            Console.instance().logMessage(MessageLevel.INFO, "sodium.console.config_file_was_reset", true, 3.0);

            SodiumClientMod.restoreDefaultOptions();
            Minecraft.getInstance().setScreen(this.nextScreen.apply(this.prevScreen));
        }).bounds(this.width - SCREEN_PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.literal("Go back"), (btn) -> {
            Minecraft.getInstance().setScreen(this.prevScreen);
        }).bounds(SCREEN_PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(this.font, Component.literal("Sodium Renderer"), 32, 32, 0xffffff);
        graphics.drawString(this.font, Component.literal("Could not load the configuration file"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) {
                continue;
            }

            graphics.drawString(this.font, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
