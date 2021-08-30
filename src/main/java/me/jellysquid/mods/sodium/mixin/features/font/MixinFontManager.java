package me.jellysquid.mods.sodium.mixin.features.font;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.util.font.FontManagerExtended;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(FontManager.class)
public class MixinFontManager implements FontManagerExtended {
    @Shadow
    @Final
    private Map<Identifier, FontStorage> fontStorages;

    @Shadow
    private Map<Identifier, Identifier> idOverrides;

    @Shadow
    @Final
    private FontStorage missingStorage;

    private Map<Identifier, FontStorage> fontStorageOverrides = Collections.emptyMap();

    /**
     * @author JellySquid
     * @reason Avoid double lookup
     */
    @Overwrite
    public TextRenderer createTextRenderer() {
        return new TextRenderer(this::getFontStorageOverride);
    }

    @Override
    public void rebuild() {
        this.fontStorageOverrides = this.fontStorages.size() < 4 ? new Object2ObjectArrayMap<>(this.fontStorages) : new Object2ObjectOpenHashMap<>(this.fontStorages);

        for (Map.Entry<Identifier, Identifier> entry : this.idOverrides.entrySet()) {
            this.fontStorageOverrides.put(entry.getKey(), this.getFontStorageOverride(entry.getValue()));
        }
    }

    private FontStorage getFontStorageOverride(Identifier id) {
        return this.fontStorageOverrides.getOrDefault(id, this.missingStorage);
    }

    @Mixin(targets = "net/minecraft/client/font/FontManager$1")
    private static class MixinReloadListener {
        @Shadow(aliases = "field_18216", remap = false)
        @Final
        private FontManager fontManager;

        @Inject(method = "apply", at = @At("RETURN"))
        private void postApply(Map<Identifier, List<Font>> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
            ((FontManagerExtended) this.fontManager).rebuild();
        }
    }
}
