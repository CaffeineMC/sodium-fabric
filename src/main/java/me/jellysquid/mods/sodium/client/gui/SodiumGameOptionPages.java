package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.*;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import javax.sound.sampled.BooleanControl;
import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.renderDistance"))
                        .setTooltip(Text.translatable("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.getViewDistance().setValue(value), options -> options.getViewDistance().getValue())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.simulationDistance"))
                        .setTooltip(Text.translatable("sodium.options.simulation_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.getSimulationDistance().setValue(value), options -> options.getSimulationDistance().getValue())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.gamma"))
                        .setTooltip(Text.translatable("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.getGamma().setValue(value * 0.01D), (opts) -> (int) (opts.getGamma().getValue() / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.guiScale"))
                        .setTooltip(Text.translatable("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, MinecraftClient.getInstance().getWindow().calculateScaleFactor(0, MinecraftClient.getInstance().forcesUnicodeFont()), 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.getGuiScale().setValue(value);

                            MinecraftClient client = MinecraftClient.getInstance();
                            client.onResolutionChanged();
                        }, opts -> opts.getGuiScale().getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.fullscreen"))
                        .setTooltip(Text.translatable("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.getFullscreen().setValue(value);

                            MinecraftClient client = MinecraftClient.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.getFullscreen().getValue()) {
                                window.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.getFullscreen().setValue(window.isFullscreen());
                            }
                        }, (opts) -> opts.getFullscreen().getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vsync"))
                        .setTooltip(Text.translatable("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(MinecraftClient.getInstance().options.getEnableVsync()))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.framerateLimit"))
                        .setTooltip(Text.translatable("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.getMaxFps().setValue(value);
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.getMaxFps().getValue())
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.viewBobbing"))
                        .setTooltip(Text.translatable("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(MinecraftClient.getInstance().options.getBobView()))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName(Text.translatable("options.attackIndicator"))
                        .setTooltip(Text.translatable("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new Text[] { Text.translatable("options.off"), Text.translatable("options.attack.crosshair"), Text.translatable("options.attack.hotbar") }))
                        .setBinding((opts, value) -> opts.getAttackIndicator().setValue(value), (opts) -> opts.getAttackIndicator().getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.autosaveIndicator"))
                        .setTooltip(Text.translatable("sodium.options.autosave_indicator.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.getShowAutosaveIndicator().setValue(value), opts -> opts.getShowAutosaveIndicator().getValue())
                        .build())
                .build());

        return new OptionPage(Text.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
                        .setName(Text.translatable("options.graphics"))
                        .setTooltip(Text.translatable("sodium.options.graphics_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, GraphicsMode.class, new Text[] { Text.translatable("options.graphics.fast"), Text.translatable("options.graphics.fancy"), Text.translatable("options.graphics.fabulous") }))
                        .setBinding(
                                (opts, value) -> opts.getGraphicsMode().setValue(value),
                                opts -> opts.getGraphicsMode().getValue())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.renderClouds"))
                        .setTooltip(Text.translatable("sodium.options.clouds_quality.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.getCloudRenderMode().setValue(value ? CloudRenderMode.FANCY : CloudRenderMode.OFF);

                            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                                Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
                                if (framebuffer != null) {
                                    framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                }
                            }
                        }, opts -> opts.getCloudRenderMode().getValue() == CloudRenderMode.FANCY)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName(Text.translatable("soundCategory.weather"))
                        .setTooltip(Text.translatable("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.leaves_quality.name"))
                        .setTooltip(Text.translatable("sodium.options.leaves_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                        .setName(Text.translatable("options.particles"))
                        .setTooltip(Text.translatable("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticlesMode.class, new Text[] { Text.translatable("options.particles.all"), Text.translatable("options.particles.decreased"), Text.translatable("options.particles.minimal") }))
                        .setBinding((opts, value) -> opts.getParticles().setValue(value), (opts) -> opts.getParticles().getValue())
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.ao"))
                        .setTooltip(Text.translatable("sodium.options.smooth_lighting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.getAo().setValue(value), opts -> opts.getAo().getValue())
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.biomeBlendRadius"))
                        .setTooltip(Text.translatable("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.biomeBlend()))
                        .setBinding((opts, value) -> opts.getBiomeBlendRadius().setValue(value), opts -> opts.getBiomeBlendRadius().getValue())
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.entityDistanceScaling"))
                        .setTooltip(Text.translatable("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.getEntityDistanceScaling().setValue(value / 100.0), opts -> Math.round(opts.getEntityDistanceScaling().getValue().floatValue() * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.entityShadows"))
                        .setTooltip(Text.translatable("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.getEntityShadows().setValue(value), opts -> opts.getEntityShadows().getValue())
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.vignette.name"))
                        .setTooltip(Text.translatable("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.mipmapLevels"))
                        .setTooltip(Text.translatable("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.getMipmapLevels().setValue(value), opts -> opts.getMipmapLevels().getValue())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage(Text.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionPage performance() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.chunk_update_threads.name"))
                        .setTooltip(Text.translatable("sodium.options.chunk_update_threads.tooltip"))
                        .setControl(o -> new SliderControl(o, 0, Runtime.getRuntime().availableProcessors(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default")))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.always_defer_chunk_updates.name"))
                        .setTooltip(Text.translatable("sodium.options.always_defer_chunk_updates.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build())
                .build()
        );

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.use_block_face_culling.name"))
                        .setTooltip(Text.translatable("sodium.options.use_block_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(Text.translatable("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.use_entity_culling.name"))
                        .setTooltip(Text.translatable("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(Text.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .build());

        return new OptionPage(Text.translatable("sodium.options.pages.performance"), ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.ArenaMemoryAllocator.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.chunk_memory_allocator.name"))
                        .setTooltip(Text.translatable("sodium.options.chunk_memory_allocator.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.ArenaMemoryAllocator.class))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.advanced.arenaMemoryAllocator = value, opts -> opts.advanced.arenaMemoryAllocator)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.use_persistent_mapping.name"))
                        .setTooltip(Text.translatable("sodium.options.use_persistent_mapping.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(MappedStagingBuffer.isSupported(RenderDevice.INSTANCE))
                        .setBinding((opts, value) -> opts.advanced.useAdvancedStagingBuffers = value, opts -> opts.advanced.useAdvancedStagingBuffers)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.cpu_render_ahead_limit.name"))
                        .setTooltip(Text.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.translateVariable("sodium.options.cpu_render_ahead_limit.value")))
                        .setBinding((opts, value) -> opts.advanced.cpuRenderAheadLimit = value, opts -> opts.advanced.cpuRenderAheadLimit)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(Text.translatable("sodium.options.allow_direct_memory_access.name"))
                        .setTooltip(Text.translatable("sodium.options.allow_direct_memory_access.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.advanced.allowDirectMemoryAccess = value, opts -> opts.advanced.allowDirectMemoryAccess)
                        .build()
                )
                .build());

        return new OptionPage(Text.translatable("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }
}
