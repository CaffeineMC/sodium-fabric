package me.jellysquid.mods.sodium.client.gui;

import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.util.Rect2i;
import net.minecraft.client.util.TextFormat;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.Validate;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

public class SodiumOptionsGUI extends Screen {
    private final List<OptionPage> pages = new ArrayList<>();

    private final List<ControlElement<?>> controls = new ArrayList<>();
    private final List<Drawable> drawable = new ArrayList<>();

    private final Screen prevScreen;

    private OptionPage currentPage;

    private FlatButtonWidget applyButton, resetButton, closeButton;

    public SodiumOptionsGUI(Screen prevScreen) {
        super(new TranslatableText("Sodium Options"));

        this.prevScreen = prevScreen;

        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.performance());
    }

    public void setPage(OptionPage page) {
        this.currentPage = page;

        this.rebuildGUI();
    }

    @Override
    protected void init() {
        super.init();

        this.rebuildGUI();
    }

    private void rebuildGUI() {
        this.controls.clear();
        this.children.clear();
        this.drawable.clear();

        if (this.currentPage == null) {
            if (this.pages.isEmpty()) {
                throw new IllegalStateException("No pages are available?!");
            }

            // Just use the first page for now
            this.currentPage = this.pages.get(0);
        }

        this.rebuildGUIPages();
        this.rebuildGUIOptions();

        this.applyButton = new FlatButtonWidget(new Rect2i(10, this.height - 30, 80, 20), "Apply", this::applyChanges);
        this.closeButton = new FlatButtonWidget(new Rect2i(96, this.height - 30, 80, 20), "Close", this::popScreen);

        this.children.add(this.applyButton);
        this.children.add(this.closeButton);

        this.resetButton = new FlatButtonWidget(new Rect2i(this.width - 96, this.height - 30, 80, 20), "Reset", this::resetChanges);

        this.children.add(this.resetButton);

        for (Element element : this.children) {
            if (element instanceof Drawable) {
                this.drawable.add((Drawable) element);
            }
        }
    }

    private void resetChanges() {
        for (Option<?> option : this.currentPage.getOptions()) {
            option.reset();
        }
    }

    private void rebuildGUIPages() {
        int x = 10;
        int y = 6;

        for (OptionPage page : this.pages) {
            int width = 10 + this.font.getStringWidth(page.getName());

            FlatButtonWidget button = new FlatButtonWidget(new Rect2i(x, y, width, 16), page.getName(), () -> this.setPage(page));
            button.setSelected(this.currentPage == page);

            x += width + 6;

            this.children.add(button);
        }
    }

    private void rebuildGUIOptions() {
        int x = 10;
        int y = 28;

        for (OptionGroup group : this.currentPage.getGroups()) {
            for (Option<?> option : group.getOptions()) {
                Control<?> control = option.getControl();
                ControlElement<?> element = control.createElement(new Rect2i(x, y, 200, 18));

                Validate.notNull(element);

                this.controls.add(element);
                this.children.add(element);

                y += 18;
            }

            y += 4;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        super.renderBackground();

        this.update(mouseX, mouseY, delta);

        for (Drawable drawable : this.drawable) {
            drawable.render(mouseX, mouseY, delta);
        }
    }

    private void update(int mouseX, int mouseY, float delta) {
        ControlElement<?> hovered = null;
        boolean hasChanges = false;

        for (ControlElement<?> element : this.controls) {
            if (element.isHovered()) {
                hovered = element;
            }

            if (element.getOption().hasChanged()) {
                hasChanges = true;
            }
        }

        this.applyButton.setEnabled(hasChanges);
        this.resetButton.setEnabled(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        if (hovered != null) {
            this.renderOptionTooltip(hovered, mouseX, mouseY, delta);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void renderOptionTooltip(ControlElement<?> element, int mouseX, int mouseY, float delta) {
        Rect2i dim = element.getDimensions();


        int textPadding = 3;
        int boxPadding = 3;
        int boxX = dim.getX() + dim.getWidth() + boxPadding;
        int boxY = dim.getY() + boxPadding;

        int boxWidth = Math.min(this.width - (dim.getX() + dim.getWidth() + (boxPadding * 2)), 280);

        Option<?> option = element.getOption();
        List<String> tooltip = new ArrayList<>(this.font.wrapStringToWidthAsList(option.getTooltip(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            tooltip.add("");
            tooltip.add(TextFormat.GRAY + "Performance Impact: " + impact.toDisplayString());
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;

        this.fillGradient(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000, 0xE0000000);

        for (int i = 0; i < tooltip.size(); i++) {
            String str = tooltip.get(i);

            if (str.isEmpty()) {
                continue;
            }

            this.font.draw(str, boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF);
        }
    }

    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        for (Option<?> option : this.currentPage.getOptions()) {
            if (!option.hasChanged()) {
                continue;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }

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

    private void popScreen() {
        MinecraftClient.getInstance().openScreen(this.prevScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            MinecraftClient.getInstance().openScreen(new VideoOptionsScreen(this.prevScreen, MinecraftClient.getInstance().options));

            return true;
        }

        return false;
    }
}
