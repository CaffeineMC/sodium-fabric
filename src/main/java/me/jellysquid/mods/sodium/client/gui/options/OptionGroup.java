package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    public static final Identifier DEFAULT_ID = new Identifier(SodiumClientMod.ID, "empty");

    private final Identifier id;
    private final ImmutableList<Option<?>> options;

    private OptionGroup(Identifier id, ImmutableList<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public Identifier getId() {
        return id;
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public static class Builder {
        private Identifier id;
        private final List<Option<?>> options = new ArrayList<>();

        public Builder setId(Identifier id) {
            this.id = id;

            return this;
        }

        public Builder add(Option<?> option) {
            this.options.add(option);

            return this;
        }

        // TODO: uncomment validate line after any breaking update
        public OptionGroup build() {
            // Validate.notNull(this.id, "Id must be specified");
            Validate.notEmpty(this.options, "At least one option must be specified");

            if (this.id == null) {
                this.id = OptionGroup.DEFAULT_ID;
                SodiumClientMod.logger().warn("Id must be specified in OptionGroup which contains {}, this might throw a exception on a next release", this.options.get(0).getName().getString());
            }

            return new OptionGroup(this.id, ImmutableList.copyOf(this.options));
        }
    }
}
