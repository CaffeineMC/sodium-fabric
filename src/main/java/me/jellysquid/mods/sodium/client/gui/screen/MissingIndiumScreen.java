package me.jellysquid.mods.sodium.client.gui.screen;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MissingIndiumScreen extends Screen {
    public static boolean shouldShow() {
        if (FabricLoader.getInstance().isModLoaded("indium")) {
            return false;
        }

        List<ModContainer> modsRequiringRenderer = FabricLoader.getInstance().getAllMods().stream()
                .filter(mod -> mod.getMetadata().containsCustomValue("requires_renderer"))
                .toList();

        if (modsRequiringRenderer.size() > 0) {
            SodiumClientMod.logger().error("Found %d mods that require Indium to be installed: %s".formatted(
                    modsRequiringRenderer.size(),
                    modsRequiringRenderer.stream().map(mod -> mod.getMetadata().getId()).collect(Collectors.joining(", "))
            ));
            return true;
        } else {
            return false;
        }
    }

    private static final String TEXT_BODY_RAW = """
            Some mods require an implementation of the Fabric Rendering API to be
            installed. Such an implementation is provided by Indium.
            Please download and install Indium for these mods to render correctly.

            The list of such mods can be found in the log file.
            """;

    private static final List<Text> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(Text::literal)
            .collect(Collectors.toList());

    private static final Text TEXT_BUTTON_DOWNLOAD_INDIUM_CURSEFORGE = Text.literal("CurseForge");
    private static final Text TEXT_BUTTON_DOWNLOAD_INDIUM_MODRINTH = Text.literal("Modrinth");
    private static final Text TEXT_BUTTON_IGNORE_WARNING = Text.literal("Ignore warning");

    private final Supplier<Screen> child;

    public MissingIndiumScreen(Supplier<Screen> child) {
        super(Text.literal("Missing Indium"));

        this.child = child;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(TEXT_BUTTON_DOWNLOAD_INDIUM_CURSEFORGE, (btn) -> {
            Util.getOperatingSystem().open("https://www.curseforge.com/minecraft/mc-mods/indium");
        }).dimensions(32, this.height - 40, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(TEXT_BUTTON_DOWNLOAD_INDIUM_MODRINTH, (btn) -> {
            Util.getOperatingSystem().open("https://modrinth.com/mod/indium");
        }).dimensions(this.width / 2 - 120 / 2, this.height - 40, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(TEXT_BUTTON_IGNORE_WARNING, (btn) -> {
            MinecraftClient.getInstance().setScreen(this.child.get());
        }).dimensions(this.width - 120 - 32, this.height - 40, 120, 20).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        super.render(matrices, mouseX, mouseY, delta);

        drawTextWithShadow(matrices, this.textRenderer, Text.literal("Sodium Renderer"), 32, 32, 0xffffff);
        drawTextWithShadow(matrices, this.textRenderer, Text.literal("Missing Indium"), 32, 48, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) {
                continue;
            }

            drawTextWithShadow(matrices, this.textRenderer, TEXT_BODY.get(i), 32, 68 + (i * 12), 0xffffff);
        }
    }
}
