package me.jellysquid.mods.sodium.common.config.parser;

import com.moandjiezana.toml.Toml;
import me.jellysquid.mods.sodium.common.config.annotations.Category;
import me.jellysquid.mods.sodium.common.config.annotations.Option;
import me.jellysquid.mods.sodium.common.config.parser.binding.CategoryBinding;
import me.jellysquid.mods.sodium.common.config.parser.binding.OptionBinding;
import me.jellysquid.mods.sodium.common.config.parser.types.BooleanSerializer;
import me.jellysquid.mods.sodium.common.config.parser.types.OptionSerializer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ConfigParser {
    private static final HashMap<Class<?>, OptionSerializer> optionSerializers = new HashMap<>();

    static {
        optionSerializers.put(boolean.class, new BooleanSerializer());
    }

    public static <T> T deserialize(Class<T> type, File file) throws ParseException {
        return deserialize(type, new Toml().read(file));
    }

    public static <T> T deserialize(Class<T> type, Toml toml) throws ParseException {
        T obj = create(type);
        deserializeInto(obj, toml);

        return obj;
    }

    private static <T> T create(Class<T> clazz) throws ParseException {
        Constructor<T> constructor;

        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new ParseException("The config type is missing a no-arg constructor");
        }

        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ParseException("The config type could not be instantiated", e);
        }
    }

    private static void deserializeInto(Object config, Toml toml) throws ParseException {
        for (CategoryBinding category : getSerializableCategoryFields(config)) {
            Toml table = toml.getTable(category.getName());

            if (table == null) {
                continue;
            }

            for (OptionBinding option : getSerializableOptionFields(category)) {
                try {
                    deserializeOption(table, option);
                } catch (ParseException e) {
                    throw new ParseException(String.format("Could not deserialize option %s in category %s", option.getName(), category.getName()), e);
                }
            }
        }
    }

    private static Collection<CategoryBinding> getSerializableCategoryFields(Object config) throws ParseException {
        Class<?> type = config.getClass();
        List<CategoryBinding> bindings = new ArrayList<>();

        for (Field field : type.getFields()) {
            Category marker = field.getType().getAnnotation(Category.class);

            if (marker != null) {
                Object inst;

                try {
                    inst = field.get(config);
                } catch (IllegalAccessException e) {
                    throw new ParseException("Could not retrieve category field instance", e);
                }

                if (inst == null) {
                    throw new ParseException("Category field must be non-null");
                }

                bindings.add(new CategoryBinding(marker, inst));
            }
        }

        return bindings;
    }

    private static Collection<OptionBinding> getSerializableOptionFields(CategoryBinding category) {
        Class<?> type = category.getType();
        List<OptionBinding> bindings = new ArrayList<>();

        for (Field field : type.getFields()) {
            Option marker = field.getAnnotation(Option.class);

            if (marker != null) {
                bindings.add(new OptionBinding(marker, field, category.getInstance()));
            }
        }

        return bindings;
    }

    private static void deserializeOption(Toml toml, OptionBinding binding) throws ParseException {
        OptionSerializer serializer = getSerializerForType(binding.getFieldType());

        try {
            serializer.read(toml, binding);
        } catch (IllegalAccessException e) {
            throw new ParseException("Could not mutate field", e);
        }
    }

    private static OptionSerializer getSerializerForType(Class<?> type) throws ParseException {
        OptionSerializer serializer = optionSerializers.get(type);

        if (serializer == null) {
            throw new ParseException("No serializer exists for the type " + type.getName());
        }

        return serializer;
    }

    public static class ParseException extends IOException {
        public ParseException(String msg) {
            super(msg);
        }

        public ParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
