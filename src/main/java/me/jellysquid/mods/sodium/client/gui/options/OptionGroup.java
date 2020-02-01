package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    private final ImmutableList<Option<?>> options;

    private OptionGroup(ImmutableList<Option<?>> options) {
        this.options = options;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();

        public Builder add(Option<?> option) {
            this.options.add(option);

            return this;
        }

        public OptionGroup build() {
            Validate.notEmpty(this.options, "At least one option must be specified");

            return new OptionGroup(ImmutableList.copyOf(this.options));
        }
    }
}
