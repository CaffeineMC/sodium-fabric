package me.jellysquid.mods.sodium.client.gui.widgets;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TabControlScrollPaneWidget extends AbstractWidget implements Drawable {

    private final Dim2i dim;
    private final List<OptionPage> pages;
    private OptionPage selectedPage;
    private final TextRenderer textRenderer;
    private final List<Element> children = new ArrayList<>();
    private final List<ControlElement<?>> controls = new ArrayList<>();
    private final List<Drawable> drawable = new ArrayList<>();
    private final FlatButtonWidget applyButton;
    private final FlatButtonWidget undoButton;
    private final FlatButtonWidget closeButton;
    private boolean hasPendingChanges, areAllComponentsVisible, isDraggingScrollBar;
    private int scrollY, scrollYMax;
    private Dim2i scrollBarBounds;
    private Dim2i scrollBarThumbBounds;
    private ControlElement<?> hoveredElement;
    private ControlElement<?> focusedElement;

    public TabControlScrollPaneWidget(Dim2i dim, List<OptionPage> pages, TextRenderer textRenderer, FlatButtonWidget applyButton,
                                      FlatButtonWidget undoButton, FlatButtonWidget closeButton) {
        this.dim = dim;
        this.pages = pages;
        if (this.pages.isEmpty()) {
            throw new IllegalStateException("No pages are available?!");
        }
        this.setPage(pages.get(0));
        this.textRenderer = textRenderer;
        this.applyButton = applyButton;
        this.undoButton = undoButton;
        this.closeButton = closeButton;
    }


    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        this.updateControls();
        if (!this.areAllComponentsVisible) {
            this.renderScrollBar();
        }
        applyScissor(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getWidth(), this.dim.getHeight(), () -> {
            for (Drawable drawable : this.drawable) {
                if (drawable == this.focusedElement) continue;
                drawable.render(matrixStack, mouseX, mouseY, delta);
            }
        });
        if (this.hoveredElement != null) {
            this.renderOptionTooltip(matrixStack, this.hoveredElement);
        }
        if (this.focusedElement != null) {
            this.focusedElement.render(matrixStack, mouseX, mouseY, delta);
        }
    }

    public void renderScrollBar() {
        this.drawRect(this.scrollBarBounds.getOriginX(), this.scrollBarBounds.getOriginY(), this.scrollBarBounds.getLimitX(),
                this.scrollBarBounds.getLimitY(), 0xE0000000);
        this.drawRect(this.scrollBarThumbBounds.getOriginX(), this.scrollBarThumbBounds.getOriginY(),
                this.scrollBarThumbBounds.getLimitX(), this.scrollBarThumbBounds.getLimitY(), 0xE0FFFFFF);
    }

    public void setPage(OptionPage page) {
        this.selectedPage = page;
        this.scrollY = 0;
        this.buildGUI();
    }

    public void buildGUI() {
        this.focusedElement = null;
        this.controls.clear();
        this.children.clear();
        this.drawable.clear();

        this.buildGUIPages();
        this.buildGUIOptions();

        for (Element element : this.children) {
            if (element instanceof Drawable) {
                this.drawable.add((Drawable) element);
            }
        }

        this.scrollBarBounds = new Dim2i(this.dim.getLimitX() - 6, this.dim.getOriginY(), 6, this.dim.getHeight());
        this.scrollBarThumbBounds = new Dim2i(this.scrollBarBounds.getOriginX() + 1, this.scrollBarBounds.getOriginY()
                + 1 - scrollY, 4, this.scrollBarBounds.getHeight() + this.scrollYMax - 2);
    }

    public void buildGUIPages() {
        int y = 0;
        for (OptionPage page : this.pages) {
            FlatButtonWidget button = new FlatButtonWidget(new Dim2i(this.dim.getOriginX(), this.dim.getOriginY() +
                    y, this.dim.getWidth() / 3 - 4, 16), page.getName(), () -> this.setPage(page));
            button.setSelected(this.selectedPage == page);

            y += 16;
            this.children.add(button);
        }
    }

    public void buildGUIOptions() {
        int y = 0;

        for (OptionGroup group : this.selectedPage.getGroups()) {
            // Add each option's control element
            for (Option<?> option : group.getOptions()) {
                Control<?> control = option.getControl();
                ControlElement<?> element = control.createElement(new Dim2i(this.dim.getOriginX() + this.dim.getWidth()
                        / 3 + 4, this.dim.getOriginY() + y + scrollY, this.dim.getLimitX() -
                        (this.dim.getOriginX() + this.dim.getWidth() / 3 + 4) - 10, 18));
                this.controls.add(element);
                this.children.add(element);

                // Move down to the next option
                y += 18;
            }

            // Add padding beneath each option group
            y += 4;
        }
        y -= 4;// Remove padding at the end of last group
        this.areAllComponentsVisible = y <= this.dim.getHeight();
        this.scrollYMax = this.dim.getHeight() - y;
    }

    private void updateControls() {
        ControlElement<?> hovered = this.getActiveControls()
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(null);

        boolean hasChanges = this.getAllOptions()
                .anyMatch(Option::hasChanged);

        for (OptionPage page : this.pages) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    hasChanges = true;
                }
            }
        }

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.undoButton.setEnabled(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
        this.hoveredElement = hovered;
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private Stream<ControlElement<?>> getActiveControls() {
        return this.controls.stream();
    }

    public boolean hasPendingChanges() {
        return this.hasPendingChanges;
    }

    public void applyScissor(int x, int y, int width, int height, Runnable action) {
        int scale = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        GL11.glScissor(x * scale, MinecraftClient.getInstance().getWindow().getHeight() - (y + height) * scale,
                width * scale, height * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        action.run();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    private void renderOptionTooltip(MatrixStack matrixStack, ControlElement<?> element) {
        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = this.dim.getWidth() / 3 - boxPadding;
        int textWidth = boxWidth - (textPadding * 2);

        int boxY = dim.getOriginY();
        int boxX = dim.getOriginX() - boxPadding - boxWidth - 4;

        Option<?> option = element.getOption();

        StringVisitable title = new LiteralText(option.getName()).formatted(Formatting.GRAY);

        List<OrderedText> text = new ArrayList<>(this.textRenderer.wrapLines(title, textWidth));
        text.addAll(this.textRenderer.wrapLines(option.getTooltip(), textWidth));

        int boxHeight = (text.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.dim.getHeight() - 8;

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxYLimit - boxYCutoff;
        }

        this.drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xF0000000);

        for (int i = 0; i < text.size(); i++) {
            OrderedText str = text.get(i);

            if (str.toString().isEmpty()) {
                continue;
            }

            this.textRenderer.draw(matrixStack, str, boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF);
        }
    }

    private void setScrollYFromMouse(double d) {
        this.setScrollY((d - (double) (this.scrollBarBounds.getOriginY())) / (double) (this.scrollBarBounds.getHeight()));
        this.buildGUI();
    }

    private void setScrollY(double d) {
        d = MathHelper.clamp(Math.round(this.scrollYMax * d), this.scrollYMax, 0f);
        this.scrollY = (int) d;
    }

    /*
    Avoids user to scroll out of scroll bound
     */
    private boolean scrollBarMouseScrolled(double amount) {
        int scrollMultiplier = 4;
        if (!this.areAllComponentsVisible) {
            if (this.scrollY + amount * scrollMultiplier <= 0 && this.scrollY + amount * scrollMultiplier >= this.scrollYMax) {
                this.scrollY += amount * scrollMultiplier;
            } else if (this.scrollY + amount * scrollMultiplier < 0) {
                this.scrollY = this.scrollYMax;
            } else if (this.scrollY + amount * scrollMultiplier > this.scrollYMax) {
                this.scrollY = 0;
            }
            this.buildGUI();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.areAllComponentsVisible && button == 0 && this.scrollBarBounds.containsCursor((int) mouseX, (int) mouseY)) {
            this.setScrollYFromMouse(mouseY);
            if (!isDraggingScrollBar) {
                isDraggingScrollBar = true;
            }
            return true;
        }
        if (this.dim.containsCursor(mouseX, mouseY)) {
            for (Element element : this.children) {
                if (element instanceof ControlElement) {
                    if (this.focusedElement == null) {
                        this.focusedElement = (ControlElement<?>) element;
                        this.focusedElement.setFocused(true);
                        if (element.mouseClicked(mouseX, mouseY, button)) return true;
                    } else {
                        if (this.focusedElement.isMouseOver(mouseX, mouseY)) {
                            if (this.focusedElement.mouseClicked(mouseX, mouseY, button)) {
                                this.focusedElement.setFocused(false);
                                return true;
                            }
                        } else {
                            this.focusedElement.setFocused(false);
                            this.focusedElement = (ControlElement<?>) element;
                            this.focusedElement.setFocused(true);
                            if (element.mouseClicked(mouseX, mouseY, button)) return true;
                        }
                    }
                } else if (element.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isDraggingScrollBar) {
                isDraggingScrollBar = false;
            }
        }
        if (this.dim.containsCursor(mouseX, mouseY)) {
            for (Element element : this.children) {
                if (element instanceof ControlElement ? ((ControlElement<?>) element).isFocused() && element.mouseReleased(mouseX, mouseY, button) : element.mouseReleased(mouseX, mouseY, button))
                    return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.areAllComponentsVisible && button == 0 &&
                this.scrollBarBounds.containsCursor(this.scrollBarBounds.getOriginX(), (int) mouseY) &&
                isDraggingScrollBar) {
            this.setScrollYFromMouse(mouseY);

            return true;
        }

        if (this.dim.containsCursor(mouseX, mouseY) && !isDraggingScrollBar) {
            for (Element element : this.children) {
                if (element instanceof ControlElement ? ((ControlElement<?>) element).isFocused() && element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) : element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
                    return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.dim.containsCursor(mouseX, mouseY)) {
            for (Element element : this.children) {
                if (element instanceof ControlElement ? ((ControlElement<?>) element).isFocused() && element.mouseScrolled(mouseX, mouseY, amount) : element.mouseScrolled(mouseX, mouseY, amount))
                    return true;
            }
            if (this.scrollBarMouseScrolled(amount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
