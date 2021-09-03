package me.jellysquid.mods.sodium.mixin.features.font;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.interop.vanilla.resource.SinglePreparationResourceReloaderCallback;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;

@Mixin(FontManager.class)
public class MixinFontManager {
    @Shadow
    @Final
    private Map<Identifier, FontStorage> fontStorages;

    @Shadow
    private Map<Identifier, Identifier> idOverrides;

    @Shadow
    @Final
    private FontStorage missingStorage;

    @Mutable
    @Shadow
    @Final
    private ResourceReloader resourceReloadListener;
    private Map<Identifier, FontStorage> fontStorageOverrides = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void reinit(TextureManager manager, CallbackInfo ci) {
        Class<SinglePreparationResourceReloader<?>> type = (Class<SinglePreparationResourceReloader<?>>) this.resourceReloadListener.getClass();
        this.resourceReloadListener = new SinglePreparationResourceReloaderCallback<>(type.cast(this.resourceReloadListener), this::rebuild);
    }

    /**
     * @author JellySquid
     * @reason Avoid double lookup
     */
    @Overwrite
    public TextRenderer createTextRenderer() {
        return new TextRenderer(this::getFontStorageOverride);
    }

    private void rebuild() {
        this.fontStorageOverrides = this.fontStorages.size() < 4 ? new Object2ObjectArrayMap<>(this.fontStorages) : new Object2ObjectOpenHashMap<>(this.fontStorages);

        for (Map.Entry<Identifier, Identifier> entry : this.idOverrides.entrySet()) {
            this.fontStorageOverrides.put(entry.getKey(), this.getFontStorageOverride(entry.getValue()));
        }
    }

    private FontStorage getFontStorageOverride(Identifier id) {
        return this.fontStorageOverrides.getOrDefault(id, this.missingStorage);
    }
}
