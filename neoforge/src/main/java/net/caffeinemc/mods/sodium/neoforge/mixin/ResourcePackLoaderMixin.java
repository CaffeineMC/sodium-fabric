package net.caffeinemc.mods.sodium.neoforge.mixin;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforgespi.locating.IModFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;

@Mixin(ResourcePackLoader.class)
public class ResourcePackLoaderMixin {
    @Unique
    private static final IModFile SODIUM_FILE = LoadingModList.get().getModFileById("sodium").getFile();

    @Redirect(remap = false, method = "packFinder", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private static Set<Map.Entry<IModFile, Pack.ResourcesSupplier>> changeSet(Map<IModFile, Pack.ResourcesSupplier> instance) {
        ReferenceLinkedOpenHashSet<Map.Entry<IModFile, Pack.ResourcesSupplier>> sortedSet = new ReferenceLinkedOpenHashSet<>();
        instance.entrySet().stream().sorted((e0, e1) -> {
            if (e0.getKey() == SODIUM_FILE) {
                return -1;
            }
            if (e1.getKey() == SODIUM_FILE) {
                return 1;
            }
            return 0;
        }).forEach(sortedSet::add);
        return sortedSet;
    }
}
