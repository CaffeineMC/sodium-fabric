package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import net.minecraft.text.Text;

public class OptionTabPage extends OptionTab {
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    public OptionTabPage(Text name, ImmutableList<OptionGroup> groups) {
        super(name);
        this.groups = groups;

        ImmutableList.Builder<Option<?>> builder = ImmutableList.builder();

        for (OptionGroup group : groups) {
            builder.addAll(group.getOptions());
        }

        this.options = builder.build();
    }

    public ImmutableList<OptionGroup> getGroups() {
        return this.groups;
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

}
