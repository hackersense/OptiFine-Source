package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.CompoundKey;

public abstract class RenderType extends RenderStateShard
{
    private static final int MEGABYTE = 1048576;
    public static final int BIG_BUFFER_SIZE = 4194304;
    public static final int SMALL_BUFFER_SIZE = 786432;
    public static final int TRANSIENT_BUFFER_SIZE = 1536;
    private static final RenderType SOLID = create(
                "solid",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                4194304,
                true,
                false,
                RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_SOLID_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
            );
    private static final RenderType CUTOUT_MIPPED = create(
                "cutout_mipped",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                4194304,
                true,
                false,
                RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_MIPPED_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
            );
    private static final RenderType CUTOUT = create(
                "cutout",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                786432,
                true,
                false,
                RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_SHADER).setTextureState(BLOCK_SHEET).createCompositeState(true)
            );
    private static final RenderType TRANSLUCENT = create(
                "translucent", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true, translucentState(RENDERTYPE_TRANSLUCENT_SHADER)
            );
    private static final RenderType TRANSLUCENT_MOVING_BLOCK = create(
                "translucent_moving_block", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, false, true, translucentMovingBlockState()
            );
    private static final Function<ResourceLocation, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(p_292067_0_ -> createArmorCutoutNoCull("armor_cutout_no_cull", p_292067_0_, false));
    private static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize(
                p_285690_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285690_0_, false, false))
        .setTransparencyState(NO_TRANSPARENCY)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(true);
        return create("entity_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize(
                p_285702_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285702_0_, false, false))
        .setTransparencyState(NO_TRANSPARENCY)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(true);
        return create("entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
                (p_285696_0_, p_285696_1_) ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285696_0_, false, false))
        .setTransparencyState(NO_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(p_285696_1_);
        return create("entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
                (p_285686_0_, p_285686_1_) ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285686_0_, false, false))
        .setTransparencyState(NO_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
        .createCompositeState(p_285686_1_);
        return create(
            "entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate
        );
    }
            );
    private static final Function<ResourceLocation, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
                p_285687_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285687_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setOutputState(ITEM_ENTITY_TARGET)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
        .createCompositeState(true);
        return create("item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(
                p_285695_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285695_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(true);
        return create("entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
                (p_285688_0_, p_285688_1_) ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285688_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(p_285688_1_);
        return create("entity_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
                (p_285694_0_, p_285694_1_) ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285694_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setWriteMaskState(COLOR_WRITE)
        .setOverlayState(OVERLAY)
        .createCompositeState(p_285694_1_);
        return create("entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_SMOOTH_CUTOUT = Util.memoize(
                p_285698_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285698_0_, false, false))
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .createCompositeState(true);
        return create("entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> BEACON_BEAM = Util.memoize(
                (p_234329_0_, p_234329_1_) ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_234329_0_, false, false))
        .setTransparencyState(p_234329_1_ ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
        .setWriteMaskState(p_234329_1_ ? COLOR_WRITE : COLOR_DEPTH_WRITE)
        .createCompositeState(false);
        return create("beacon_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_DECAL = Util.memoize(
                p_285700_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_DECAL_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285700_0_, false, false))
        .setDepthTestState(EQUAL_DEPTH_TEST)
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .createCompositeState(false);
        return create("entity_decal", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
                p_285691_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_NO_OUTLINE_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285691_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .setWriteMaskState(COLOR_WRITE)
        .createCompositeState(false);
        return create("entity_no_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> ENTITY_SHADOW = Util.memoize(
                p_285684_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_SHADOW_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285684_0_, false, false))
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(CULL)
        .setLightmapState(LIGHTMAP)
        .setOverlayState(OVERLAY)
        .setWriteMaskState(COLOR_WRITE)
        .setDepthTestState(LEQUAL_DEPTH_TEST)
        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
        .createCompositeState(false);
        return create("entity_shadow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false, rendertype$compositestate);
    }
            );
    private static final Function<ResourceLocation, RenderType> DRAGON_EXPLOSION_ALPHA = Util.memoize(
                p_285683_0_ ->
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_ENTITY_ALPHA_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(p_285683_0_, false, false))
        .setCullState(NO_CULL)
        .createCompositeState(true);
        return create("entity_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
    }
            );
    private static final BiFunction<ResourceLocation, RenderStateShard.TransparencyStateShard, RenderType> EYES = Util.memoize(
                (p_304056_0_, p_304056_1_) ->
    {
        RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_304056_0_, false, false);
        return create(
            "eyes",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(renderstateshard$texturestateshard)
            .setTransparencyState(p_304056_1_)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
        );
    }
            );
    private static final RenderType LEASH = create(
                "leash",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.TRIANGLE_STRIP,
                1536,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LEASH_SHADER).setTextureState(NO_TEXTURE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)
            );
    private static final RenderType WATER_MASK = create(
                "water_mask",
                DefaultVertexFormat.POSITION,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_WATER_MASK_SHADER).setTextureState(NO_TEXTURE).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
            );
    private static final RenderType ARMOR_ENTITY_GLINT = create(
                "armor_entity_glint",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setTransparencyState(GLINT_TRANSPARENCY)
                .setTexturingState(ENTITY_GLINT_TEXTURING)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
            );
    private static final RenderType GLINT_TRANSLUCENT = create(
                "glint_translucent",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setTransparencyState(GLINT_TRANSPARENCY)
                .setTexturingState(GLINT_TEXTURING)
                .setOutputState(ITEM_ENTITY_TARGET)
                .createCompositeState(false)
            );
    private static final RenderType GLINT = create(
                "glint",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_GLINT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setTransparencyState(GLINT_TRANSPARENCY)
                .setTexturingState(GLINT_TEXTURING)
                .createCompositeState(false)
            );
    private static final RenderType ENTITY_GLINT = create(
                "entity_glint",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setTransparencyState(GLINT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setTexturingState(ENTITY_GLINT_TEXTURING)
                .createCompositeState(false)
            );
    private static final RenderType ENTITY_GLINT_DIRECT = create(
                "entity_glint_direct",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setTransparencyState(GLINT_TRANSPARENCY)
                .setTexturingState(ENTITY_GLINT_TEXTURING)
                .createCompositeState(false)
            );
    private static final Function<ResourceLocation, RenderType> CRUMBLING = Util.memoize(
                p_285703_0_ ->
    {
        RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_285703_0_, false, false);
        return create(
            "crumbling",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_CRUMBLING_SHADER)
            .setTextureState(renderstateshard$texturestateshard)
            .setTransparencyState(CRUMBLING_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .setLayeringState(POLYGON_OFFSET_LAYERING)
            .createCompositeState(false)
        );
    }
            );
    private static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
                p_304055_0_ -> create(
                    "text",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    786432,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_304055_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
                )
            );
    private static final RenderType TEXT_BACKGROUND = create(
                "text_background",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER).setTextureState(NO_TEXTURE).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).createCompositeState(false)
            );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(
                p_304057_0_ -> create(
                    "text_intensity",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    786432,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_304057_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
                )
            );
    private static final Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
                p_285685_0_ -> create(
                    "text_polygon_offset",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285685_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
                )
            );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
                p_285704_0_ -> create(
                    "text_intensity_polygon_offset",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285704_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
                )
            );
    private static final Function<ResourceLocation, RenderType> TEXT_SEE_THROUGH = Util.memoize(
                p_285689_0_ -> create(
                    "text_see_through",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285689_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
                )
            );
    private static final RenderType TEXT_BACKGROUND_SEE_THROUGH = create(
                "text_background_see_through",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER)
                .setTextureState(NO_TEXTURE)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setDepthTestState(NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)
            );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
                p_285697_0_ -> create(
                    "text_intensity_see_through",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285697_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
                )
            );
    private static final RenderType LIGHTNING = create(
                "lightning",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_DEPTH_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).setOutputState(WEATHER_TARGET).createCompositeState(false)
            );
    private static final RenderType DRAGON_RAYS = create(
                "dragon_rays",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                1536,
                false,
                false,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).createCompositeState(false)
            );
    private static final RenderType DRAGON_RAYS_DEPTH = create(
                "dragon_rays_depth",
                DefaultVertexFormat.POSITION,
                VertexFormat.Mode.TRIANGLES,
                1536,
                false,
                false,
                RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_SHADER).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
            );
    private static final RenderType TRIPWIRE = create("tripwire", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, true, true, tripwireState());
    private static final RenderType END_PORTAL = create(
                "end_portal",
                DefaultVertexFormat.POSITION,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_END_PORTAL_SHADER)
                .setTextureState(
                    RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
                )
                .createCompositeState(false)
            );
    private static final RenderType END_GATEWAY = create(
                "end_gateway",
                DefaultVertexFormat.POSITION,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_END_GATEWAY_SHADER)
                .setTextureState(
                    RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
                )
                .createCompositeState(false)
            );
    private static final RenderType CLOUDS = createClouds(false);
    private static final RenderType CLOUDS_DEPTH_ONLY = createClouds(true);
    public static final RenderType.CompositeRenderType LINES = create(
                "lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_LINES_SHADER)
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setCullState(NO_CULL)
                .createCompositeState(false)
            );
    public static final RenderType.CompositeRenderType LINE_STRIP = create(
                "line_strip",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINE_STRIP,
                1536,
                RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_LINES_SHADER)
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setCullState(NO_CULL)
                .createCompositeState(false)
            );
    private static final Function<Double, RenderType.CompositeRenderType> DEBUG_LINE_STRIP = Util.memoize(
                p_285693_0_ -> create(
                    "debug_line_strip",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.DEBUG_LINE_STRIP,
                    1536,
                    RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(p_285693_0_)))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
                )
            );
    private static final RenderType.CompositeRenderType DEBUG_FILLED_BOX = create(
                "debug_filled_box",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLE_STRIP,
                1536,
                false,
                true,
                RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType DEBUG_QUADS = create(
                "debug_quads",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType DEBUG_STRUCTURE_QUADS = create(
                "debug_structure_quads",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType DEBUG_SECTION_QUADS = create(
                "debug_section_quads",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(CULL).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType GUI = create(
                "gui",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType GUI_OVERLAY = create(
                "gui_overlay",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType GUI_TEXT_HIGHLIGHT = create(
                "gui_text_highlight",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setColorLogicState(OR_REVERSE_COLOR_LOGIC).createCompositeState(false)
            );
    private static final RenderType.CompositeRenderType GUI_GHOST_RECIPE_OVERLAY = create(
                "gui_ghost_recipe_overlay",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(GREATER_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
            );
    private static final ImmutableList<RenderType> CHUNK_BUFFER_LAYERS = ImmutableList.of(solid(), cutoutMipped(), cutout(), translucent(), tripwire());
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final int bufferSize;
    private final boolean affectsCrumbling;
    private final boolean sortOnUpload;
    private int id = -1;
    public static final RenderType[] CHUNK_RENDER_TYPES;
    private static Map<CompoundKey, RenderType> RENDER_TYPES;
    private int chunkLayerId = -1;

    public int ordinal()
    {
        return this.id;
    }

    public boolean isNeedsSorting()
    {
        return this.sortOnUpload;
    }

    private static RenderType[] getChunkRenderTypesArray()
    {
        RenderType[] arendertype = chunkBufferLayers().toArray(new RenderType[0]);
        int i = 0;

        while (i < arendertype.length)
        {
            RenderType rendertype = arendertype[i];
            rendertype.id = i++;
        }

        return arendertype;
    }

    public final int getChunkLayerId()
    {
        return this.chunkLayerId;
    }

    public static RenderType solid()
    {
        return SOLID;
    }

    public static RenderType cutoutMipped()
    {
        return CUTOUT_MIPPED;
    }

    public static RenderType cutout()
    {
        return CUTOUT;
    }

    private static RenderType.CompositeState translucentState(RenderStateShard.ShaderStateShard p_173208_)
    {
        return RenderType.CompositeState.builder()
               .setLightmapState(LIGHTMAP)
               .setShaderState(p_173208_)
               .setTextureState(BLOCK_SHEET_MIPPED)
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setOutputState(TRANSLUCENT_TARGET)
               .createCompositeState(true);
    }

    public static RenderType translucent()
    {
        return TRANSLUCENT;
    }

    private static RenderType.CompositeState translucentMovingBlockState()
    {
        return RenderType.CompositeState.builder()
               .setLightmapState(LIGHTMAP)
               .setShaderState(RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER)
               .setTextureState(BLOCK_SHEET_MIPPED)
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setOutputState(ITEM_ENTITY_TARGET)
               .createCompositeState(true);
    }

    public static RenderType translucentMovingBlock()
    {
        return TRANSLUCENT_MOVING_BLOCK;
    }

    private static RenderType.CompositeRenderType createArmorCutoutNoCull(String p_297971_, ResourceLocation p_300992_, boolean p_297695_)
    {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_300992_, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setDepthTestState(p_297695_ ? EQUAL_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                .createCompositeState(true);
        return create(p_297971_, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }

    public static RenderType armorCutoutNoCull(ResourceLocation p_110432_)
    {
        p_110432_ = getCustomTexture(p_110432_);
        return ARMOR_CUTOUT_NO_CULL.apply(p_110432_);
    }

    public static RenderType createArmorDecalCutoutNoCull(ResourceLocation p_298982_)
    {
        return createArmorCutoutNoCull("armor_decal_cutout_no_cull", p_298982_, true);
    }

    public static RenderType entitySolid(ResourceLocation p_110447_)
    {
        p_110447_ = getCustomTexture(p_110447_);
        return EmissiveTextures.isRenderEmissive() ? ENTITY_CUTOUT.apply(p_110447_) : ENTITY_SOLID.apply(p_110447_);
    }

    public static RenderType entityCutout(ResourceLocation p_110453_)
    {
        p_110453_ = getCustomTexture(p_110453_);
        return ENTITY_CUTOUT.apply(p_110453_);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation p_110444_, boolean p_110445_)
    {
        p_110444_ = getCustomTexture(p_110444_);
        return ENTITY_CUTOUT_NO_CULL.apply(p_110444_, p_110445_);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation p_110459_)
    {
        return entityCutoutNoCull(p_110459_, true);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation p_110449_, boolean p_110450_)
    {
        p_110449_ = getCustomTexture(p_110449_);
        return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(p_110449_, p_110450_);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation p_110465_)
    {
        return entityCutoutNoCullZOffset(p_110465_, true);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation p_110468_)
    {
        p_110468_ = getCustomTexture(p_110468_);
        return ITEM_ENTITY_TRANSLUCENT_CULL.apply(p_110468_);
    }

    public static RenderType entityTranslucentCull(ResourceLocation p_110471_)
    {
        p_110471_ = getCustomTexture(p_110471_);
        return ENTITY_TRANSLUCENT_CULL.apply(p_110471_);
    }

    public static RenderType entityTranslucent(ResourceLocation p_110455_, boolean p_110456_)
    {
        p_110455_ = getCustomTexture(p_110455_);
        return ENTITY_TRANSLUCENT.apply(p_110455_, p_110456_);
    }

    public static RenderType entityTranslucent(ResourceLocation p_110474_)
    {
        return entityTranslucent(p_110474_, true);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation p_234336_, boolean p_234337_)
    {
        p_234336_ = getCustomTexture(p_234336_);
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(p_234336_, p_234337_);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation p_234339_)
    {
        p_234339_ = getCustomTexture(p_234339_);
        return entityTranslucentEmissive(p_234339_, true);
    }

    public static RenderType entitySmoothCutout(ResourceLocation p_110477_)
    {
        p_110477_ = getCustomTexture(p_110477_);
        return ENTITY_SMOOTH_CUTOUT.apply(p_110477_);
    }

    public static RenderType beaconBeam(ResourceLocation p_110461_, boolean p_110462_)
    {
        p_110461_ = getCustomTexture(p_110461_);
        return BEACON_BEAM.apply(p_110461_, p_110462_);
    }

    public static RenderType entityDecal(ResourceLocation p_110480_)
    {
        p_110480_ = getCustomTexture(p_110480_);
        return ENTITY_DECAL.apply(p_110480_);
    }

    public static RenderType entityNoOutline(ResourceLocation p_110483_)
    {
        p_110483_ = getCustomTexture(p_110483_);
        return ENTITY_NO_OUTLINE.apply(p_110483_);
    }

    public static RenderType entityShadow(ResourceLocation p_110486_)
    {
        p_110486_ = getCustomTexture(p_110486_);
        return ENTITY_SHADOW.apply(p_110486_);
    }

    public static RenderType dragonExplosionAlpha(ResourceLocation p_173236_)
    {
        p_173236_ = getCustomTexture(p_173236_);
        return DRAGON_EXPLOSION_ALPHA.apply(p_173236_);
    }

    public static RenderType eyes(ResourceLocation p_110489_)
    {
        p_110489_ = getCustomTexture(p_110489_);
        return EYES.apply(p_110489_, ADDITIVE_TRANSPARENCY);
    }

    public static RenderType breezeEyes(ResourceLocation p_311465_)
    {
        p_311465_ = getCustomTexture(p_311465_);
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(p_311465_, false);
    }

    public static RenderType breezeWind(ResourceLocation p_311543_, float p_312161_, float p_310801_)
    {
        p_311543_ = getCustomTexture(p_311543_);
        return create(
                   "breeze_wind",
                   DefaultVertexFormat.NEW_ENTITY,
                   VertexFormat.Mode.QUADS,
                   1536,
                   false,
                   true,
                   RenderType.CompositeState.builder()
                   .setShaderState(RENDERTYPE_BREEZE_WIND_SHADER)
                   .setTextureState(new RenderStateShard.TextureStateShard(p_311543_, false, false))
                   .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(p_312161_, p_310801_))
                   .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                   .setCullState(NO_CULL)
                   .setLightmapState(LIGHTMAP)
                   .setOverlayState(NO_OVERLAY)
                   .createCompositeState(false)
               );
    }

    public static RenderType energySwirl(ResourceLocation p_110437_, float p_110438_, float p_110439_)
    {
        p_110437_ = getCustomTexture(p_110437_);
        return create(
                   "energy_swirl",
                   DefaultVertexFormat.NEW_ENTITY,
                   VertexFormat.Mode.QUADS,
                   1536,
                   false,
                   true,
                   RenderType.CompositeState.builder()
                   .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                   .setTextureState(new RenderStateShard.TextureStateShard(p_110437_, false, false))
                   .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(p_110438_, p_110439_))
                   .setTransparencyState(ADDITIVE_TRANSPARENCY)
                   .setCullState(NO_CULL)
                   .setLightmapState(LIGHTMAP)
                   .setOverlayState(OVERLAY)
                   .createCompositeState(false)
               );
    }

    public static RenderType leash()
    {
        return LEASH;
    }

    public static RenderType waterMask()
    {
        return WATER_MASK;
    }

    public static RenderType outline(ResourceLocation p_110492_)
    {
        p_110492_ = getCustomTexture(p_110492_);
        return RenderType.CompositeRenderType.OUTLINE.apply(p_110492_, NO_CULL);
    }

    public static RenderType armorEntityGlint()
    {
        return ARMOR_ENTITY_GLINT;
    }

    public static RenderType glintTranslucent()
    {
        return GLINT_TRANSLUCENT;
    }

    public static RenderType glint()
    {
        return GLINT;
    }

    public static RenderType entityGlint()
    {
        return ENTITY_GLINT;
    }

    public static RenderType entityGlintDirect()
    {
        return ENTITY_GLINT_DIRECT;
    }

    public static RenderType crumbling(ResourceLocation p_110495_)
    {
        p_110495_ = getCustomTexture(p_110495_);
        return CRUMBLING.apply(p_110495_);
    }

    public static RenderType text(ResourceLocation p_110498_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getText(p_110498_) : TEXT.apply(p_110498_);
    }

    public static RenderType textBackground()
    {
        return TEXT_BACKGROUND;
    }

    public static RenderType textIntensity(ResourceLocation p_173238_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensity(p_173238_) : TEXT_INTENSITY.apply(p_173238_);
    }

    public static RenderType textPolygonOffset(ResourceLocation p_181445_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextPolygonOffset(p_181445_) : TEXT_POLYGON_OFFSET.apply(p_181445_);
    }

    public static RenderType textIntensityPolygonOffset(ResourceLocation p_181447_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensityPolygonOffset(p_181447_) : TEXT_INTENSITY_POLYGON_OFFSET.apply(p_181447_);
    }

    public static RenderType textSeeThrough(ResourceLocation p_110501_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextSeeThrough(p_110501_) : TEXT_SEE_THROUGH.apply(p_110501_);
    }

    public static RenderType textBackgroundSeeThrough()
    {
        return TEXT_BACKGROUND_SEE_THROUGH;
    }

    public static RenderType textIntensitySeeThrough(ResourceLocation p_173241_)
    {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensitySeeThrough(p_173241_) : TEXT_INTENSITY_SEE_THROUGH.apply(p_173241_);
    }

    public static RenderType lightning()
    {
        return LIGHTNING;
    }

    public static RenderType dragonRays()
    {
        return DRAGON_RAYS;
    }

    public static RenderType dragonRaysDepth()
    {
        return DRAGON_RAYS_DEPTH;
    }

    private static RenderType.CompositeState tripwireState()
    {
        return RenderType.CompositeState.builder()
               .setLightmapState(LIGHTMAP)
               .setShaderState(RENDERTYPE_TRIPWIRE_SHADER)
               .setTextureState(BLOCK_SHEET_MIPPED)
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setOutputState(WEATHER_TARGET)
               .createCompositeState(true);
    }

    public static RenderType tripwire()
    {
        return TRIPWIRE;
    }

    public static RenderType endPortal()
    {
        return END_PORTAL;
    }

    public static RenderType endGateway()
    {
        return END_GATEWAY;
    }

    private static RenderType.CompositeRenderType createClouds(boolean p_332692_)
    {
        return create(
                   "clouds",
                   DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                   VertexFormat.Mode.QUADS,
                   786432,
                   false,
                   false,
                   RenderType.CompositeState.builder()
                   .setShaderState(RENDERTYPE_CLOUDS_SHADER)
                   .setTextureState(new RenderStateShard.TextureStateShard(LevelRenderer.CLOUDS_LOCATION, false, false))
                   .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                   .setCullState(NO_CULL)
                   .setWriteMaskState(p_332692_ ? DEPTH_WRITE : COLOR_DEPTH_WRITE)
                   .setOutputState(CLOUDS_TARGET)
                   .createCompositeState(true)
               );
    }

    public static RenderType clouds()
    {
        return CLOUDS;
    }

    public static RenderType cloudsDepthOnly()
    {
        return CLOUDS_DEPTH_ONLY;
    }

    public static RenderType lines()
    {
        return LINES;
    }

    public static RenderType lineStrip()
    {
        return LINE_STRIP;
    }

    public static RenderType debugLineStrip(double p_270166_)
    {
        return DEBUG_LINE_STRIP.apply(p_270166_);
    }

    public static RenderType debugFilledBox()
    {
        return DEBUG_FILLED_BOX;
    }

    public static RenderType debugQuads()
    {
        return DEBUG_QUADS;
    }

    public static RenderType debugStructureQuads()
    {
        return DEBUG_STRUCTURE_QUADS;
    }

    public static RenderType debugSectionQuads()
    {
        return DEBUG_SECTION_QUADS;
    }

    public static RenderType gui()
    {
        return GUI;
    }

    public static RenderType guiOverlay()
    {
        return GUI_OVERLAY;
    }

    public static RenderType guiTextHighlight()
    {
        return GUI_TEXT_HIGHLIGHT;
    }

    public static RenderType guiGhostRecipeOverlay()
    {
        return GUI_GHOST_RECIPE_OVERLAY;
    }

    public RenderType(
        String p_173178_,
        VertexFormat p_173179_,
        VertexFormat.Mode p_173180_,
        int p_173181_,
        boolean p_173182_,
        boolean p_173183_,
        Runnable p_173184_,
        Runnable p_173185_
    )
    {
        super(p_173178_, p_173184_, p_173185_);
        this.format = p_173179_;
        this.mode = p_173180_;
        this.bufferSize = p_173181_;
        this.affectsCrumbling = p_173182_;
        this.sortOnUpload = p_173183_;
    }

    static RenderType.CompositeRenderType create(
        String p_173210_, VertexFormat p_173211_, VertexFormat.Mode p_173212_, int p_173213_, RenderType.CompositeState p_173214_
    )
    {
        return create(p_173210_, p_173211_, p_173212_, p_173213_, false, false, p_173214_);
    }

    static RenderType.CompositeRenderType create(
        String p_173216_,
        VertexFormat p_173217_,
        VertexFormat.Mode p_173218_,
        int p_173219_,
        boolean p_173220_,
        boolean p_173221_,
        RenderType.CompositeState p_173222_
    )
    {
        return new RenderType.CompositeRenderType(p_173216_, p_173217_, p_173218_, p_173219_, p_173220_, p_173221_, p_173222_);
    }

    public void draw(MeshData p_343145_)
    {
        this.setupRenderState();

        if (Config.isShaders())
        {
            RenderUtils.setFlushRenderBuffers(false);
            Shaders.pushProgram();
            ShadersRender.preRender(this);
        }

        BufferUploader.drawWithShader(p_343145_);

        if (Config.isShaders())
        {
            ShadersRender.postRender(this);
            Shaders.popProgram();
            RenderUtils.setFlushRenderBuffers(true);
        }

        this.clearRenderState();
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public static List<RenderType> chunkBufferLayers()
    {
        return CHUNK_BUFFER_LAYERS;
    }

    public int bufferSize()
    {
        return this.bufferSize;
    }

    public VertexFormat format()
    {
        return this.format;
    }

    public VertexFormat.Mode mode()
    {
        return this.mode;
    }

    public Optional<RenderType> outline()
    {
        return Optional.empty();
    }

    public boolean isOutline()
    {
        return false;
    }

    public boolean affectsCrumbling()
    {
        return this.affectsCrumbling;
    }

    public boolean canConsolidateConsecutiveGeometry()
    {
        return !this.mode.connectedPrimitives;
    }

    public boolean sortOnUpload()
    {
        return this.sortOnUpload;
    }

    public static ResourceLocation getCustomTexture(ResourceLocation locationIn)
    {
        if (Config.isRandomEntities())
        {
            locationIn = RandomEntities.getTextureLocation(locationIn);
        }

        if (EmissiveTextures.isActive())
        {
            locationIn = EmissiveTextures.getEmissiveTexture(locationIn);
        }

        return locationIn;
    }

    public boolean isEntitySolid()
    {
        return this.getName().equals("entity_solid");
    }

    public static int getCountRenderStates()
    {
        return LINES.state.states.size();
    }

    public ResourceLocation getTextureLocation()
    {
        return null;
    }

    public boolean isGlint()
    {
        return this.getTextureLocation() == ItemRenderer.ENCHANTED_GLINT_ENTITY | this.getTextureLocation() == ItemRenderer.ENCHANTED_GLINT_ITEM;
    }

    public boolean isAtlasTextureBlocks()
    {
        ResourceLocation resourcelocation = this.getTextureLocation();
        return resourcelocation == TextureAtlas.LOCATION_BLOCKS;
    }

    static
    {
        int i = 0;

        for (RenderType rendertype : chunkBufferLayers())
        {
            rendertype.chunkLayerId = i++;
        }

        CHUNK_RENDER_TYPES = getChunkRenderTypesArray();
    }

    static final class CompositeRenderType extends RenderType
    {
        static final BiFunction<ResourceLocation, RenderStateShard.CullStateShard, RenderType> OUTLINE = Util.memoize(
                    (p_337854_0_, p_337854_1_) -> RenderType.create(
                        "outline",
                        DefaultVertexFormat.POSITION_TEX_COLOR,
                        VertexFormat.Mode.QUADS,
                        1536,
                        RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_OUTLINE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(p_337854_0_, false, false))
                        .setCullState(p_337854_1_)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setOutputState(OUTLINE_TARGET)
                        .createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)
                    )
                );
        private final RenderType.CompositeState state;
        private final Optional<RenderType> outline;
        private final boolean isOutline;
        private Map<ResourceLocation, RenderType.CompositeRenderType> mapTextured = new HashMap<>();

        CompositeRenderType(
            String p_173258_,
            VertexFormat p_173259_,
            VertexFormat.Mode p_173260_,
            int p_173261_,
            boolean p_173262_,
            boolean p_173263_,
            RenderType.CompositeState p_173264_
        )
        {
            super(
                p_173258_,
                p_173259_,
                p_173260_,
                p_173261_,
                p_173262_,
                p_173263_,
                () -> RenderStateManager.setupRenderStates(p_173264_.states),
                () -> RenderStateManager.clearRenderStates(p_173264_.states)
            );
            this.state = p_173264_;
            this.outline = p_173264_.outlineProperty == RenderType.OutlineProperty.AFFECTS_OUTLINE
                             ? p_173264_.textureState.cutoutTexture().map(locationIn -> OUTLINE.apply(locationIn, p_173264_.cullState))
                             : Optional.empty();
            this.isOutline = p_173264_.outlineProperty == RenderType.OutlineProperty.IS_OUTLINE;
        }

        @Override
        public Optional<RenderType> outline()
        {
            return this.outline;
        }

        @Override
        public boolean isOutline()
        {
            return this.isOutline;
        }

        protected final RenderType.CompositeState state()
        {
            return this.state;
        }

        @Override
        public String toString()
        {
            return "RenderType[" + this.name + ":" + this.state + "]";
        }

        public RenderType.CompositeRenderType getTextured(ResourceLocation textureLocation)
        {
            if (textureLocation == null)
            {
                return this;
            }
            else
            {
                Optional<ResourceLocation> optional = this.state.textureState.cutoutTexture();

                if (!optional.isPresent())
                {
                    return this;
                }
                else
                {
                    ResourceLocation resourcelocation = optional.get();

                    if (resourcelocation == null)
                    {
                        return this;
                    }
                    else if (textureLocation.equals(resourcelocation))
                    {
                        return this;
                    }
                    else
                    {
                        RenderType.CompositeRenderType rendertype$compositerendertype = this.mapTextured.get(textureLocation);

                        if (rendertype$compositerendertype == null)
                        {
                            RenderType.CompositeState.CompositeStateBuilder rendertype$compositestate$compositestatebuilder = this.state.getCopyBuilder();
                            rendertype$compositestate$compositestatebuilder.setTextureState(
                                new RenderStateShard.TextureStateShard(textureLocation, this.state.textureState.isBlur(), this.state.textureState.isMipmap())
                            );
                            RenderType.CompositeState rendertype$compositestate = rendertype$compositestate$compositestatebuilder.createCompositeState(this.isOutline);
                            rendertype$compositerendertype = create(
                                                                 this.name,
                                                                 this.format(),
                                                                 this.mode(),
                                                                 this.bufferSize(),
                                                                 this.affectsCrumbling(),
                                                                 this.isNeedsSorting(),
                                                                 rendertype$compositestate
                                                             );
                            this.mapTextured.put(textureLocation, rendertype$compositerendertype);
                        }

                        return rendertype$compositerendertype;
                    }
                }
            }
        }

        @Override
        public ResourceLocation getTextureLocation()
        {
            Optional<ResourceLocation> optional = this.state.textureState.cutoutTexture();
            return !optional.isPresent() ? null : optional.get();
        }
    }

    protected static final class CompositeState
    {
        final RenderStateShard.EmptyTextureStateShard textureState;
        private final RenderStateShard.ShaderStateShard shaderState;
        private final RenderStateShard.TransparencyStateShard transparencyState;
        private final RenderStateShard.DepthTestStateShard depthTestState;
        final RenderStateShard.CullStateShard cullState;
        private final RenderStateShard.LightmapStateShard lightmapState;
        private final RenderStateShard.OverlayStateShard overlayState;
        private final RenderStateShard.LayeringStateShard layeringState;
        private final RenderStateShard.OutputStateShard outputState;
        private final RenderStateShard.TexturingStateShard texturingState;
        private final RenderStateShard.WriteMaskStateShard writeMaskState;
        private final RenderStateShard.LineStateShard lineState;
        private final RenderStateShard.ColorLogicStateShard colorLogicState;
        final RenderType.OutlineProperty outlineProperty;
        final ImmutableList<RenderStateShard> states;

        CompositeState(
            RenderStateShard.EmptyTextureStateShard p_286632_,
            RenderStateShard.ShaderStateShard p_286843_,
            RenderStateShard.TransparencyStateShard p_286280_,
            RenderStateShard.DepthTestStateShard p_286228_,
            RenderStateShard.CullStateShard p_286226_,
            RenderStateShard.LightmapStateShard p_286744_,
            RenderStateShard.OverlayStateShard p_286754_,
            RenderStateShard.LayeringStateShard p_286895_,
            RenderStateShard.OutputStateShard p_286435_,
            RenderStateShard.TexturingStateShard p_286893_,
            RenderStateShard.WriteMaskStateShard p_286628_,
            RenderStateShard.LineStateShard p_286768_,
            RenderStateShard.ColorLogicStateShard p_286578_,
            RenderType.OutlineProperty p_286290_
        )
        {
            this.textureState = p_286632_;
            this.shaderState = p_286843_;
            this.transparencyState = p_286280_;
            this.depthTestState = p_286228_;
            this.cullState = p_286226_;
            this.lightmapState = p_286744_;
            this.overlayState = p_286754_;
            this.layeringState = p_286895_;
            this.outputState = p_286435_;
            this.texturingState = p_286893_;
            this.writeMaskState = p_286628_;
            this.lineState = p_286768_;
            this.colorLogicState = p_286578_;
            this.outlineProperty = p_286290_;
            this.states = ImmutableList.of(
                                 this.textureState,
                                 this.shaderState,
                                 this.transparencyState,
                                 this.depthTestState,
                                 this.cullState,
                                 this.lightmapState,
                                 this.overlayState,
                                 this.layeringState,
                                 this.outputState,
                                 this.texturingState,
                                 this.writeMaskState,
                                 this.colorLogicState,
                                 this.lineState
                             );
        }

        @Override
        public String toString()
        {
            return "CompositeState[" + this.states + ", outlineProperty=" + this.outlineProperty + "]";
        }

        public static RenderType.CompositeState.CompositeStateBuilder builder()
        {
            return new RenderType.CompositeState.CompositeStateBuilder();
        }

        public RenderType.CompositeState.CompositeStateBuilder getCopyBuilder()
        {
            RenderType.CompositeState.CompositeStateBuilder rendertype$compositestate$compositestatebuilder = new RenderType.CompositeState.CompositeStateBuilder();
            rendertype$compositestate$compositestatebuilder.setTextureState(this.textureState);
            rendertype$compositestate$compositestatebuilder.setShaderState(this.shaderState);
            rendertype$compositestate$compositestatebuilder.setTransparencyState(this.transparencyState);
            rendertype$compositestate$compositestatebuilder.setDepthTestState(this.depthTestState);
            rendertype$compositestate$compositestatebuilder.setCullState(this.cullState);
            rendertype$compositestate$compositestatebuilder.setLightmapState(this.lightmapState);
            rendertype$compositestate$compositestatebuilder.setOverlayState(this.overlayState);
            rendertype$compositestate$compositestatebuilder.setLayeringState(this.layeringState);
            rendertype$compositestate$compositestatebuilder.setOutputState(this.outputState);
            rendertype$compositestate$compositestatebuilder.setTexturingState(this.texturingState);
            rendertype$compositestate$compositestatebuilder.setWriteMaskState(this.writeMaskState);
            rendertype$compositestate$compositestatebuilder.setLineState(this.lineState);
            return rendertype$compositestate$compositestatebuilder;
        }

        public static class CompositeStateBuilder
        {
            private RenderStateShard.EmptyTextureStateShard textureState = RenderStateShard.NO_TEXTURE;
            private RenderStateShard.ShaderStateShard shaderState = RenderStateShard.NO_SHADER;
            private RenderStateShard.TransparencyStateShard transparencyState = RenderStateShard.NO_TRANSPARENCY;
            private RenderStateShard.DepthTestStateShard depthTestState = RenderStateShard.LEQUAL_DEPTH_TEST;
            private RenderStateShard.CullStateShard cullState = RenderStateShard.CULL;
            private RenderStateShard.LightmapStateShard lightmapState = RenderStateShard.NO_LIGHTMAP;
            private RenderStateShard.OverlayStateShard overlayState = RenderStateShard.NO_OVERLAY;
            private RenderStateShard.LayeringStateShard layeringState = RenderStateShard.NO_LAYERING;
            private RenderStateShard.OutputStateShard outputState = RenderStateShard.MAIN_TARGET;
            private RenderStateShard.TexturingStateShard texturingState = RenderStateShard.DEFAULT_TEXTURING;
            private RenderStateShard.WriteMaskStateShard writeMaskState = RenderStateShard.COLOR_DEPTH_WRITE;
            private RenderStateShard.LineStateShard lineState = RenderStateShard.DEFAULT_LINE;
            private RenderStateShard.ColorLogicStateShard colorLogicState = RenderStateShard.NO_COLOR_LOGIC;

            CompositeStateBuilder()
            {
            }

            public RenderType.CompositeState.CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard p_173291_)
            {
                this.textureState = p_173291_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setShaderState(RenderStateShard.ShaderStateShard p_173293_)
            {
                this.shaderState = p_173293_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTransparencyState(RenderStateShard.TransparencyStateShard p_110686_)
            {
                this.transparencyState = p_110686_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setDepthTestState(RenderStateShard.DepthTestStateShard p_110664_)
            {
                this.depthTestState = p_110664_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setCullState(RenderStateShard.CullStateShard p_110662_)
            {
                this.cullState = p_110662_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard p_110672_)
            {
                this.lightmapState = p_110672_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard p_110678_)
            {
                this.overlayState = p_110678_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard p_110670_)
            {
                this.layeringState = p_110670_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard p_110676_)
            {
                this.outputState = p_110676_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard p_110684_)
            {
                this.texturingState = p_110684_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setWriteMaskState(RenderStateShard.WriteMaskStateShard p_110688_)
            {
                this.writeMaskState = p_110688_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLineState(RenderStateShard.LineStateShard p_110674_)
            {
                this.lineState = p_110674_;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setColorLogicState(RenderStateShard.ColorLogicStateShard p_286236_)
            {
                this.colorLogicState = p_286236_;
                return this;
            }

            public RenderType.CompositeState createCompositeState(boolean p_110692_)
            {
                return this.createCompositeState(p_110692_ ? RenderType.OutlineProperty.AFFECTS_OUTLINE : RenderType.OutlineProperty.NONE);
            }

            public RenderType.CompositeState createCompositeState(RenderType.OutlineProperty p_110690_)
            {
                return new RenderType.CompositeState(
                           this.textureState,
                           this.shaderState,
                           this.transparencyState,
                           this.depthTestState,
                           this.cullState,
                           this.lightmapState,
                           this.overlayState,
                           this.layeringState,
                           this.outputState,
                           this.texturingState,
                           this.writeMaskState,
                           this.lineState,
                           this.colorLogicState,
                           p_110690_
                       );
            }
        }
    }

    static enum OutlineProperty
    {
        NONE("none"),
        IS_OUTLINE("is_outline"),
        AFFECTS_OUTLINE("affects_outline");

        private final String name;

        private OutlineProperty(final String p_110702_)
        {
            this.name = p_110702_;
        }

        @Override
        public String toString()
        {
            return this.name;
        }
    }
}
