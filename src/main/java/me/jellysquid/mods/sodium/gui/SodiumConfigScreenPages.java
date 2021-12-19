package me.jellysquid.mods.sodium.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.config.SodiumRenderConfig;
import me.jellysquid.mods.sodium.config.render.*;
import me.jellysquid.mods.sodium.render.chunk.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.config.render.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.gui.options.ControlValueFormatter;
import me.jellysquid.mods.sodium.gui.options.CyclingControl;
import me.jellysquid.mods.sodium.gui.options.SliderControl;
import me.jellysquid.mods.sodium.gui.options.TickBoxControl;
import me.jellysquid.mods.sodium.config.render.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.config.render.storage.SodiumOptionsStorage;
import me.jellysquid.mods.sodium.render.entity.renderer.InstancedEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.Option;
import net.minecraft.client.option.*;
import net.minecraft.client.util.Window;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.List;

public class SodiumConfigScreenPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.renderDistance"))
                        .setTooltip(new TranslatableText("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.quantity("Chunks")))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setName(new LiteralText("Detail Distance"))
                        .setTooltip(new LiteralText("Controls the distance at which more detailed models will be used in world rendering. Lower " +
                                "values reduce the distance at which detail begins to be eliminated, increasing performance."))
                        .setControl(option -> new SliderControl(option, 1, 33, 1, ControlValueFormatter.minMaxQuantity(2, 32, "Auto", "Maximum", "chunks")))
                        .setBinding((opts, value) -> opts.quality.detailDistance = value, opts -> opts.quality.detailDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.gamma"))
                        .setTooltip(new TranslatableText("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma = value * 0.01D, (opts) -> (int) (opts.gamma / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.guiScale"))
                        .setTooltip(new TranslatableText("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            client.onResolutionChanged();
                        }, opts -> opts.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(new TranslatableText("options.fullscreen"))
                        .setTooltip(new TranslatableText("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen) {
                                window.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen = window.isFullscreen();
                            }
                        }, (opts) -> opts.fullscreen)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(new TranslatableText("options.vsync"))
                        .setTooltip(new TranslatableText("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VSYNC))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.framerateLimit"))
                        .setTooltip(new TranslatableText("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 260, 5, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.maxFps = value;
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.maxFps)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(new TranslatableText("options.viewBobbing"))
                        .setTooltip(new TranslatableText("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName(new TranslatableText("options.attackIndicator"))
                        .setTooltip(new TranslatableText("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new Text[] { new TranslatableText("options.off"), new TranslatableText("options.attack.crosshair"), new TranslatableText("options.attack.hotbar") }))
                        .setBinding((opts, value) -> opts.attackIndicator = value, (opts) -> opts.attackIndicator)
                        .build())
                .build());

        return new OptionPage(new TranslatableText("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
                        .setName(new TranslatableText("options.graphics"))
                        .setTooltip(new TranslatableText("sodium.options.graphics_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, GraphicsMode.class, new Text[] { new TranslatableText("options.graphics.fast"), new TranslatableText("options.graphics.fancy"), new TranslatableText("options.graphics.fabulous") }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode = value,
                                opts -> opts.graphicsMode)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(CloudRenderMode.class, vanillaOpts)
                        .setName(new TranslatableText("sodium.options.clouds_quality.name"))
                        .setTooltip(new TranslatableText("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, CloudRenderMode.class, new Text[] { new TranslatableText("options.off"), new TranslatableText("options.clouds.fast"), new TranslatableText("options.clouds.fancy") }))
                        .setBinding((opts, value) -> {
                            opts.cloudRenderMode = value;

                            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                                Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
                                if (framebuffer != null) {
                                    framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                }
                            }
                        }, opts -> opts.cloudRenderMode)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumRenderConfig.GraphicsQuality.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.weather_quality.name"))
                        .setTooltip(new TranslatableText("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumRenderConfig.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                        .setName(new TranslatableText("sodium.options.particle_quality.name"))
                        .setTooltip(new TranslatableText("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticlesMode.class, new Text[] { new TranslatableText("options.particles.all"), new TranslatableText("options.particles.decreased"), new TranslatableText("options.particles.minimal") }))
                        .setBinding((opts, value) -> opts.particles = value, (opts) -> opts.particles)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.biomeBlendRadius"))
                        .setTooltip(new TranslatableText("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.quantityOrDisabled("block(s)", "None")))
                        .setBinding((opts, value) -> opts.biomeBlendRadius = value, opts -> opts.biomeBlendRadius)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.entityDistanceScaling"))
                        .setTooltip(new TranslatableText("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.entityDistanceScaling = value / 100.0F, opts -> Math.round(opts.entityDistanceScaling * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(new TranslatableText("options.entityShadows"))
                        .setTooltip(new TranslatableText("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows = value, opts -> opts.entityShadows)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.vignette.name"))
                        .setTooltip(new TranslatableText("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(new TranslatableText("options.mipmapLevels"))
                        .setTooltip(new TranslatableText("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage(new TranslatableText("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionPage performance() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setName(new LiteralText("Chunk Update Threads"))
                        .setTooltip(new LiteralText("Specifies the number of threads to use for chunk building. Using more threads can speed up chunk " +
                                "loading and updates, but may negatively impact frame times."))
                        .setControl(o -> new SliderControl(o, 0, Runtime.getRuntime().availableProcessors(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default")))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new LiteralText("Always Defer Chunk Updates"))
                        .setTooltip(new LiteralText("If enabled, rendering will never wait for chunk updates to finish, even if they are important. This can " +
                                "greatly improve frame rates in some scenarios, but could also create significant visual lag in the world."))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build())
                .build()
        );

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.use_block_face_culling.name"))
                        .setTooltip(new TranslatableText("sodium.options.use_block_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(new TranslatableText("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.use_entity_culling.name"))
                        .setTooltip(new TranslatableText("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new LiteralText("Use Model Instancing"))
                        .setTooltip(new LiteralText("If enabled, supported models will be packed into VBOs and instanced. " +
                                "This removes unnecessary uploads to the GPU by only sending transformations for existing vertices. " +
                                "This also removes many unnecessary draw calls by packing many similar entities into batches. " +
                                "This can drastically improve framerate when many entities are visible." +
                                "\n\nRequires OpenGL 4.4 or ARB_shader_storage_buffer_object, ARB_shading_language_packing, and ARB_buffer_storage."))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setEnabled(InstancedEntityRenderer.isSupported(SodiumRender.DEVICE))
                        .setBinding((opts, value) -> opts.performance.useModelInstancing = value, opts -> opts.performance.useModelInstancing)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.use_particle_culling.name"))
                        .setTooltip(new TranslatableText("sodium.options.use_particle_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useParticleCulling = value, opts -> opts.performance.useParticleCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(new TranslatableText("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .build());

        return new OptionPage(new TranslatableText("sodium.options.pages.performance"), ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumRenderConfig.ArenaMemoryAllocator.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.chunk_memory_allocator.name"))
                        .setTooltip(new TranslatableText("sodium.options.chunk_memory_allocator.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumRenderConfig.ArenaMemoryAllocator.class))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.advanced.arenaMemoryAllocator = value, opts -> opts.advanced.arenaMemoryAllocator)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.use_persistent_mapping.name"))
                        .setTooltip(new TranslatableText("sodium.options.use_persistent_mapping.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(MappedStagingBuffer.isSupported(SodiumRender.DEVICE))
                        .setBinding((opts, value) -> opts.advanced.useAdvancedStagingBuffers = value, opts -> opts.advanced.useAdvancedStagingBuffers)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setName(new LiteralText("Max Pre-Rendered Frames"))
                        .setTooltip(new LiteralText("Specifies the maximum number of frames the CPU can be waiting on the GPU to finish rendering. " +
                                "Very low or high values may create frame rate instability."))
                        .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.quantity("frames")))
                        .setBinding((opts, value) -> opts.advanced.maxPreRenderedFrames = value, opts -> opts.advanced.maxPreRenderedFrames)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(new TranslatableText("sodium.options.allow_direct_memory_access.name"))
                        .setTooltip(new TranslatableText("sodium.options.allow_direct_memory_access.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.advanced.allowDirectMemoryAccess = value, opts -> opts.advanced.allowDirectMemoryAccess)
                        .build()
                )
                .build());

        return new OptionPage(new TranslatableText("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }

}
