package me.jellysquid.mods.sodium.client.gui.screen;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfigCorruptedScreen extends Screen {
    private static final List<Text> TEXT_BODY = IntStream.rangeClosed(1, 7)
            .mapToObj(i -> Text.translatable("sodium.console.config_corrupt.contents." + i))
            .collect(Collectors.toList());

    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;

    private static final int SCREEN_PADDING = 32;

    private final @Nullable Screen prevScreen;
    private final Function<Screen, Screen> nextScreen;

    public ConfigCorruptedScreen(@Nullable Screen prevScreen, @Nullable Function<Screen, Screen> nextScreen) {
        super(Text.translatable("sodium.console.config_corrupt.short"));

        this.prevScreen = prevScreen;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = this.height - SCREEN_PADDING - BUTTON_HEIGHT;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.continue"), (btn) -> {
            Console.instance().logMessage(MessageLevel.INFO, Text.translatable("sodium.console.config_file_was_reset"), 3.0);

            SodiumClientMod.restoreDefaultOptions();
            MinecraftClient.getInstance().setScreen(this.nextScreen.apply(this.prevScreen));
        }).dimensions(this.width - SCREEN_PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), (btn) -> {
            MinecraftClient.getInstance().setScreen(this.prevScreen);
        }).dimensions(SCREEN_PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.drawTextWithShadow(this.textRenderer, Text.translatable("sodium.name_renderer"), 32, 32, 0xffffff);
        drawContext.drawTextWithShadow(this.textRenderer, Text.translatable("sodium.console.config_corrupt.title"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) {
                continue;
            }

            drawContext.drawTextWithShadow(this.textRenderer, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
