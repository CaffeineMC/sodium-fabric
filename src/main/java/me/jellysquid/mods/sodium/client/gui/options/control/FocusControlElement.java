package me.jellysquid.mods.sodium.client.gui.options.control;


import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/*
 * Vanilla has a focus control at AbstractParentElement but it's fairly limited since it does not provide support for
 * sub child components.
 * This can improved so far, working as intended for OptionPageContainerWidget
 * @author FlashyReese
 */
public class FocusControlElement extends AbstractWidget{

    public List<Element> children = new ArrayList<>();
    public ControlElement<?> focusedElement;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element element : this.children) {
            if (element instanceof ControlElement) {
                if(this.focusedElement == null || !this.focusedElement.isMouseOver(mouseX, mouseY)){
                    this.focusedElement = (ControlElement<?>) element;
                    if (element.mouseClicked(mouseX, mouseY, button)) return true;
                }else if (this.focusedElement.isMouseOver(mouseX, mouseY)) {
                    if (this.focusedElement.mouseClicked(mouseX, mouseY, button)) return true;
                }
            } else if (element.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }



    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Element element : this.children) {
            if (element instanceof ControlElement) {
                if(this.focusedElement == null || !this.focusedElement.isMouseOver(mouseX, mouseY)){
                    this.focusedElement = (ControlElement<?>) element;
                    if (element.mouseReleased(mouseX, mouseY, button)) return true;
                }else if (this.focusedElement.isMouseOver(mouseX, mouseY)) {
                    if (this.focusedElement.mouseReleased(mouseX, mouseY, button)) return true;
                }
            } else if (element.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Element element : this.children) {
            if (element instanceof ControlElement) {
                if(this.focusedElement == null || !this.focusedElement.isMouseOver(mouseX, mouseY)){
                    this.focusedElement = (ControlElement<?>) element;
                    if (element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
                }else if (this.focusedElement.isMouseOver(mouseX, mouseY)) {
                    if (this.focusedElement.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
                }
            } else if (element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (Element element : this.children) {
            if (element instanceof ControlElement) {
                if(this.focusedElement == null || !this.focusedElement.isMouseOver(mouseX, mouseY)){
                    this.focusedElement = (ControlElement<?>) element;
                    if (element.mouseScrolled(mouseX, mouseY, amount)) return true;
                }else if (this.focusedElement.isMouseOver(mouseX, mouseY)) {
                    if (this.focusedElement.mouseScrolled(mouseX, mouseY, amount)) return true;
                }
            } else if (element.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {

    }
}
