package me.jellysquid.mods.sodium.mixin.features.debug;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.render.entity.DebugInfo;
import me.jellysquid.mods.thingl.util.NativeBuffer;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud {
    @Shadow
    private static long toMiB(long bytes) {
        throw new UnsupportedOperationException();
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> redirectRightTextEarly(Object[] elements) {
        ArrayList<String> strings = Lists.newArrayList((String[]) elements);
        strings.add("");
        strings.add("Sodium Renderer");
        strings.add(Formatting.UNDERLINE + getFormattedVersionText());

        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(renderer.getMemoryDebugStrings());
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());

                break;
            }
        }

        return strings;
    }

    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void addInstancingText(CallbackInfoReturnable<List<String>> cir) {
        List<String> strings = cir.getReturnValue();
        strings.add("[Baked Models] Model Buffer: " + DebugInfo.getSizeReadable(DebugInfo.currentModelBufferSize) + DebugInfo.MODEL_BUFFER_SUFFIX);
        strings.add("[Baked Models] Part Buffer: " + DebugInfo.getSizeReadable(DebugInfo.currentPartBufferSize) + DebugInfo.PART_BUFFER_SUFFIX);
        strings.add("[Baked Models] Translucent Index Buffer: " + DebugInfo.getSizeReadable(DebugInfo.currentTranslucencyEboSize) + DebugInfo.TRANSLUCENCY_EBO_SUFFIX);

        int totalInstances = 0;
        int totalSets = 0;
        List<String> tempStrings = new ArrayList<>();
        for (Map.Entry<String, DebugInfo.ModelDebugInfo> entry : DebugInfo.modelToDebugInfoMap.entrySet()) {
            DebugInfo.ModelDebugInfo modelDebugInfo = entry.getValue();
            tempStrings.add("[Baked Models] " + entry.getKey() + ": " + modelDebugInfo.instances + " Instances / " + modelDebugInfo.sets + " Sets");
            totalInstances += modelDebugInfo.instances;
            totalSets += modelDebugInfo.sets;
        }
        strings.add("[Baked Models] Total: " + totalInstances + " Instances / " + totalSets + " Sets");
        strings.addAll(tempStrings);

        DebugInfo.currentModelBufferSize = 0;
        DebugInfo.currentPartBufferSize = 0;
        DebugInfo.currentTranslucencyEboSize = 0;
        DebugInfo.modelToDebugInfoMap.clear();
    }

    private static String getFormattedVersionText() {
        String version = SodiumClient.getVersion();
        Formatting color;

        if (version.endsWith("-dirty")) {
            color = Formatting.RED;
        } else if (version.contains("+rev.")) {
            color = Formatting.LIGHT_PURPLE;
        } else {
            color = Formatting.GREEN;
        }

        return color + version;
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + toMiB(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }
}
