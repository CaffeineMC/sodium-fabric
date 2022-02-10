package me.jellysquid.mods.sodium.gui.screen;

import me.jellysquid.mods.sodium.SodiumClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UserConfigErrorScreen extends Screen {
    private static final String TEXT_BODY_RAW = """
        A problem occurred while trying to load the configuration file. This
        can happen when the file has been corrupted on disk, or when trying
        to manually edit the file by hand.
        
        We can attempt to fix this problem automatically by restoring the
        config file back to known-good defaults, but you will lose any
        changes that have since been made to your video settings.
        
        More information about the error can be found in the log file.
        """;

    private static final List<Component> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(TextComponent::new)
            .collect(Collectors.toList());

    private static final Component TEXT_BUTTON_RESTORE_DEFAULTS = new TextComponent("Restore defaults");
    private static final Component TEXT_BUTTON_CLOSE_GAME = new TextComponent("Close game");

    private final Supplier<Screen> child;

    public UserConfigErrorScreen(Supplier<Screen> child) {
        super(new TextComponent("Config corruption detected"));

        this.child = child;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(new Button(32, this.height - 40, 174, 20, TEXT_BUTTON_RESTORE_DEFAULTS, (btn) -> {
            SodiumClientMod.restoreDefaultOptions();
            Minecraft.getInstance().setScreen(this.child.get());
        }));

        this.addRenderableWidget(new Button(this.width - 174 - 32, this.height - 40, 174, 20, TEXT_BUTTON_CLOSE_GAME, (btn) -> {
            Minecraft.getInstance().stop();
        }));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float delta) {
        this.renderBackground(pose);

        super.render(pose, mouseX, mouseY, delta);

        drawString(pose, this.font, new TextComponent("Sodium Renderer"), 32, 32, 0xffffff);
        drawString(pose, this.font, new TextComponent("Could not load configuration file"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getContents().isEmpty()) {
                continue;
            }

            drawString(pose, this.font, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
