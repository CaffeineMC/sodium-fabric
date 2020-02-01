package me.jellysquid.mods.sodium.common.config.parser.binding;

import me.jellysquid.mods.sodium.common.config.annotations.Category;

public class CategoryBinding {
    private final Category option;
    private final Class<?> type;
    private final Object instance;

    public CategoryBinding(Category option, Object instance) {
        this.option = option;
        this.type = instance.getClass();
        this.instance = instance;
    }

    public Class<?> getType() {
        return this.type;
    }

    public String getName() {
        return this.option.value();
    }

    public Object getInstance() {
        return this.instance;
    }
}
