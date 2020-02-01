package me.jellysquid.mods.sodium.common.config.parser.binding;

import me.jellysquid.mods.sodium.common.config.annotations.Option;

import java.lang.reflect.Field;

public class OptionBinding {
    private final Option option;
    private final Field field;
    private final Class<?> fieldType;
    private final Object owner;

    public OptionBinding(Option option, Field field, Object owner) {
        this.option = option;
        this.field = field;
        this.fieldType = field.getType();
        this.owner = owner;
    }

    public Class<?> getFieldType() {
        return this.fieldType;
    }

    public String getName() {
        return this.option.value();
    }

    public void setBoolean(boolean val) throws IllegalAccessException {
        this.field.setBoolean(this.owner, val);
    }
}
