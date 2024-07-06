package net.caffeinemc.mods.sodium.neoforge.mixin;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraftforge.client.loading.ClientModLoader;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;

@Mixin(ClientModLoader.class)
public class MixinClientModLoader {
    @Unique
    private static final IModFile SODIUM_FILE = LoadingModList.get().getModFileById("sodium").getFile();

    @Redirect(remap = false, method = "clientPackFinder", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private static Set<Map.Entry<IModFile, ? extends PathPackResources>> changeSet(Map<IModFile, ? extends PathPackResources> instance) {
        ReferenceLinkedOpenHashSet<Map.Entry<IModFile, ? extends PathPackResources>> sortedSet = new ReferenceLinkedOpenHashSet<>();
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
