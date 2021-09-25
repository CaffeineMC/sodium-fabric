package me.jellysquid.mods.sodium.client.gui.screen;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigCorruptedScreen extends Screen {
    private static final String TEXT_BODY_RAW = """
        A problem occurred while trying to load the configuration file. This
        can happen when the file has been corrupted on disk, or when trying
        to manually edit the file by hand.
        
        We can attempt to fix this problem automatically by restoring the
        config file back to known-good defaults, but you will lose any
        changes that have since been made to your video settings.
        
        More information about the error can be found in the log file.
        """;

    private static final List<Text> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(LiteralText::new)
            .collect(Collectors.toList());

    private static final Text TEXT_BUTTON_RESTORE_DEFAULTS = new LiteralText("Restore defaults");
    private static final Text TEXT_BUTTON_CLOSE_GAME = new LiteralText("Close game");

    private final Supplier<Screen> child;

    public ConfigCorruptedScreen(Supplier<Screen> child) {
        super(new LiteralText("Config corruption detected"));

        this.child = child;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(new ButtonWidget(32, this.height - 40, 174, 20, TEXT_BUTTON_RESTORE_DEFAULTS, (btn) -> {
            SodiumClientMod.restoreDefaultOptions();
            MinecraftClient.getInstance().openScreen(this.child.get());
        }));

        this.addDrawableChild(new ButtonWidget(this.width - 174 - 32, this.height - 40, 174, 20, TEXT_BUTTON_CLOSE_GAME, (btn) -> {
            MinecraftClient.getInstance().scheduleStop();
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        super.render(matrices, mouseX, mouseY, delta);

        drawTextWithShadow(matrices, this.textRenderer, new LiteralText("Sodium Renderer"), 32, 32, 0xffffff);
        drawTextWithShadow(matrices, this.textRenderer, new LiteralText("Could not load configuration file"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).asString().isEmpty()) {
                continue;
            }

            drawTextWithShadow(matrices, this.textRenderer, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
