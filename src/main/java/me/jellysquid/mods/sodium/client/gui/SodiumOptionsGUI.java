package me.jellysquid.mods.sodium.client.gui;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.TabControlScrollPaneWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class SodiumOptionsGUI extends Screen {
    private final List<OptionPage> pages = new ArrayList<>();

    private final List<Drawable> drawable = new ArrayList<>();

    private final Screen prevScreen;

    private TabControlScrollPaneWidget settingsTabControlScrollPane;

    public SodiumOptionsGUI(Screen prevScreen) {
        super(new TranslatableText("Sodium Options"));

        this.prevScreen = prevScreen;

        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.advanced());
    }

    @Override
    protected void init() {
        super.init();

        this.buildGUI();
    }

    private void buildGUI() {
        this.children.clear();
        this.drawable.clear();

        FlatButtonWidget undoButton = new FlatButtonWidget(new Dim2i(this.width - 211, this.height - 30, 65, 20),
                I18n.translate("sodium.options.buttons.undo"), this::undoChanges);
        FlatButtonWidget applyButton = new FlatButtonWidget(new Dim2i(this.width - 142, this.height - 30, 65, 20),
                I18n.translate("sodium.options.buttons.apply"), this::applyChanges);
        FlatButtonWidget closeButton = new FlatButtonWidget(new Dim2i(this.width - 73, this.height - 30, 65, 20),
                I18n.translate("sodium.options.buttons.close"), this::onClose);

        this.settingsTabControlScrollPane = new TabControlScrollPaneWidget(new Dim2i(4, 4, this.width - 8,
                this.height - 8 - 32), pages, this.textRenderer, applyButton, undoButton, closeButton);


        this.children.add(this.settingsTabControlScrollPane);
        this.children.add(undoButton);
        this.children.add(applyButton);
        this.children.add(closeButton);

        for (Element element : this.children) {
            if (element instanceof Drawable) {
                this.drawable.add((Drawable) element);
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrixStack);
        for (Drawable drawable : this.drawable) {
            drawable.render(matrixStack, mouseX, mouseY, delta);
        }
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        MinecraftClient client = MinecraftClient.getInstance();

        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            client.worldRenderer.reload();
        }

        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            client.resetMipmapLevels(client.options.mipmapLevels);
            client.reloadResourcesConcurrently();
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save();
        }
    }

    private void undoChanges() {
        this.getAllOptions()
                .forEach(Option::reset);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            MinecraftClient.getInstance().openScreen(new VideoOptionsScreen(this.prevScreen, MinecraftClient.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.settingsTabControlScrollPane.hasPendingChanges();
    }

    @Override
    public void onClose() {
        this.client.openScreen(this.prevScreen);
    }
}
