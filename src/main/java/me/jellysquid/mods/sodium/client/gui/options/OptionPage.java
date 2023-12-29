package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OptionPage {
    public static final Identifier DEFAULT_ID = new Identifier(SodiumClientMod.ID, "empty");

    private final Identifier id;
    private final Text name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    @Deprecated
    public OptionPage(Text name, ImmutableList<OptionGroup> groups) {
        this(OptionPage.DEFAULT_ID, name, groups);
        SodiumClientMod.logger().warn("Id must be specified in OptionPage '{}'", name.getString());
    }

    public OptionPage(Identifier id, Text name, ImmutableList<OptionGroup> groups) {
        this.id = id;
        this.name = name;
        this.groups = groups;

        ImmutableList.Builder<Option<?>> builder = ImmutableList.builder();

        for (OptionGroup group : groups) {
            builder.addAll(group.getOptions());
        }

        this.options = builder.build();
    }

    public Identifier getId() {
        return id;
    }

    public ImmutableList<OptionGroup> getGroups() {
        return this.groups;
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public Text getName() {
        return this.name;
    }

}
