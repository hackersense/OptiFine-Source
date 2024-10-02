package net.optifine.reflect;

import com.google.common.collect.ImmutableMap.Builder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.ArmorStandModel;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.ChestedHorseModel;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.LeashKnotModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.LlamaSpitModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.TadpoleModel;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EvokerFangsRenderer;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.LlamaSpitRenderer;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.Ticket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.ForgeFaceData;
import net.minecraftforge.eventbus.api.Event;
import net.optifine.Log;
import net.optifine.util.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

public class Reflector
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean logForge = registerResolvable("*** Reflector Forge ***");
    public static ReflectorClass BrandingControl = new ReflectorClass("net.minecraftforge.internal.BrandingControl");
    public static ReflectorMethod BrandingControl_getBrandings = new ReflectorMethod(BrandingControl, "getBrandings");
    public static ReflectorMethod BrandingControl_getBranding = new ReflectorMethod(BrandingControl, "getBranding");
    public static ReflectorMethod BrandingControl_forEachLine = new ReflectorMethod(BrandingControl, "forEachLine");
    public static ReflectorMethod BrandingControl_forEachAboveCopyrightLine = new ReflectorMethod(BrandingControl, "forEachAboveCopyrightLine");
    public static ReflectorClass CapabilityProvider = new ReflectorClass("net.minecraftforge.common.capabilities.CapabilityProvider");
    public static ReflectorMethod CapabilityProvider_gatherCapabilities = new ReflectorMethod(CapabilityProvider, "gatherCapabilities", new Class[0]);
    public static ReflectorClass ClientModLoader = new ReflectorClass("net.minecraftforge.client.loading.ClientModLoader");
    public static ReflectorMethod ClientModLoader_isLoading = new ReflectorMethod(ClientModLoader, "isLoading");
    public static ReflectorClass ChunkEvent_Load = new ReflectorClass("net.minecraftforge.event.level.ChunkEvent$Load");
    public static ReflectorConstructor ChunkEvent_Load_Constructor = new ReflectorConstructor(ChunkEvent_Load, new Class[] {ChunkAccess.class, boolean.class});
    public static ReflectorClass ChunkEvent_Unload = new ReflectorClass("net.minecraftforge.event.level.ChunkEvent$Unload");
    public static ReflectorConstructor ChunkEvent_Unload_Constructor = new ReflectorConstructor(ChunkEvent_Unload, new Class[] {ChunkAccess.class});
    public static ReflectorClass ColorResolverManager = new ReflectorClass("net.minecraftforge.client.ColorResolverManager");
    public static ReflectorMethod ColorResolverManager_registerBlockTintCaches = ColorResolverManager.makeMethod("registerBlockTintCaches");
    public static ReflectorClass CrashReportAnalyser = new ReflectorClass("net.minecraftforge.logging.CrashReportAnalyser");
    public static ReflectorMethod CrashReportAnalyser_appendSuspectedMods = new ReflectorMethod(CrashReportAnalyser, "appendSuspectedMods");
    public static ReflectorClass CrashReportExtender = new ReflectorClass("net.minecraftforge.logging.CrashReportExtender");
    public static ReflectorMethod CrashReportExtender_extendSystemReport = new ReflectorMethod(CrashReportExtender, "extendSystemReport");
    public static ReflectorMethod CrashReportExtender_generateEnhancedStackTraceT = new ReflectorMethod(
        CrashReportExtender, "generateEnhancedStackTrace", new Class[] {Throwable.class}
    );
    public static ReflectorMethod CrashReportExtender_generateEnhancedStackTraceSTE = new ReflectorMethod(
        CrashReportExtender, "generateEnhancedStackTrace", new Class[] {StackTraceElement[].class}
    );
    public static ReflectorClass EntityRenderersEvent_CreateSkullModels = new ReflectorClass(
        "net.minecraftforge.client.event.EntityRenderersEvent$CreateSkullModels"
    );
    public static ReflectorConstructor EntityRenderersEvent_CreateSkullModels_Constructor = EntityRenderersEvent_CreateSkullModels.makeConstructor(
                new Class[] {Builder.class, EntityModelSet.class}
            );
    public static ReflectorClass EventBus = new ReflectorClass("net.minecraftforge.eventbus.api.IEventBus");
    public static ReflectorMethod EventBus_post = new ReflectorMethod(EventBus, "post", new Class[] {Event.class});
    public static ReflectorClass ForgeModelBlockRenderer = new ReflectorClass("net.minecraftforge.client.model.lighting.ForgeModelBlockRenderer");
    public static ReflectorConstructor ForgeModelBlockRenderer_Constructor = new ReflectorConstructor(ForgeModelBlockRenderer, new Class[] {BlockColors.class});
    public static ReflectorClass ForgeBlockModelShapes = new ReflectorClass(BlockModelShaper.class);
    public static ReflectorMethod ForgeBlockModelShapes_getTexture3 = new ReflectorMethod(
        ForgeBlockModelShapes, "getTexture", new Class[] {BlockState.class, Level.class, BlockPos.class}
    );
    public static ReflectorClass ForgeBlockElementFace = new ReflectorClass(BlockElementFace.class);
    public static ReflectorMethod ForgeBlockElementFace_data = ForgeBlockElementFace.makeMethod("data");
    public static ReflectorClass IForgeBlockState = new ReflectorClass("net.minecraftforge.common.extensions.IForgeBlockState");
    public static ReflectorMethod IForgeBlockState_getSoundType3 = new ReflectorMethod(
        IForgeBlockState, "getSoundType", new Class[] {LevelReader.class, BlockPos.class, Entity.class}
    );
    public static ReflectorMethod IForgeBlockState_getStateAtViewpoint = new ReflectorMethod(IForgeBlockState, "getStateAtViewpoint");
    public static ReflectorMethod IForgeBlockState_shouldDisplayFluidOverlay = new ReflectorMethod(IForgeBlockState, "shouldDisplayFluidOverlay");
    public static ReflectorClass IForgeEntity = new ReflectorClass("net.minecraftforge.common.extensions.IForgeEntity");
    public static ReflectorMethod IForgeEntity_canUpdate = new ReflectorMethod(IForgeEntity, "canUpdate", new Class[0]);
    public static ReflectorMethod IForgeEntity_getEyeInFluidType = new ReflectorMethod(IForgeEntity, "getEyeInFluidType");
    public static ReflectorMethod IForgeEntity_getParts = new ReflectorMethod(IForgeEntity, "getParts");
    public static ReflectorMethod IForgeEntity_hasCustomOutlineRendering = new ReflectorMethod(IForgeEntity, "hasCustomOutlineRendering");
    public static ReflectorMethod IForgeEntity_isMultipartEntity = new ReflectorMethod(IForgeEntity, "isMultipartEntity");
    public static ReflectorMethod IForgeEntity_onAddedToWorld = new ReflectorMethod(IForgeEntity, "onAddedToWorld");
    public static ReflectorMethod IForgeEntity_onRemovedFromWorld = new ReflectorMethod(IForgeEntity, "onRemovedFromWorld");
    public static ReflectorMethod IForgeEntity_shouldRiderSit = new ReflectorMethod(IForgeEntity, "shouldRiderSit");
    public static ReflectorClass ForgeEventFactory = new ReflectorClass("net.minecraftforge.event.ForgeEventFactory");
    public static ReflectorMethod ForgeEventFactory_canEntityDespawn = new ReflectorMethod(ForgeEventFactory, "canEntityDespawn");
    public static ReflectorMethod ForgeEventFactory_fireChunkTicketLevelUpdated = new ReflectorMethod(ForgeEventFactory, "fireChunkTicketLevelUpdated");
    public static ReflectorMethod ForgeEventFactory_getMobGriefingEvent = new ReflectorMethod(ForgeEventFactory, "getMobGriefingEvent");
    public static ReflectorMethod ForgeEventFactory_onChunkDataSave = new ReflectorMethod(ForgeEventFactory, "onChunkDataSave");
    public static ReflectorMethod ForgeEventFactory_onChunkLoad = new ReflectorMethod(ForgeEventFactory, "onChunkLoad");
    public static ReflectorMethod ForgeEventFactory_onChunkUnload = new ReflectorMethod(ForgeEventFactory, "onChunkUnload");
    public static ReflectorMethod ForgeEventFactory_onDifficultyChange = new ReflectorMethod(ForgeEventFactory, "onDifficultyChange");
    public static ReflectorMethod ForgeEventFactory_onEntityJoinLevel = new ReflectorMethod(
        ForgeEventFactory, "onEntityJoinLevel", new Class[] {Entity.class, Level.class}
    );
    public static ReflectorMethod ForgeEventFactory_onEntityLeaveLevel = new ReflectorMethod(ForgeEventFactory, "onEntityLeaveLevel");
    public static ReflectorMethod ForgeEventFactory_onLevelLoad = new ReflectorMethod(ForgeEventFactory, "onLevelLoad");
    public static ReflectorMethod ForgeEventFactory_onLivingChangeTargetMob = new ReflectorMethod(ForgeEventFactory, "onLivingChangeTargetMob");
    public static ReflectorMethod ForgeEventFactory_onPlaySoundAtEntity = new ReflectorMethod(ForgeEventFactory, "onPlaySoundAtEntity");
    public static ReflectorMethod ForgeEventFactory_onPlaySoundAtPosition = new ReflectorMethod(ForgeEventFactory, "onPlaySoundAtPosition");
    public static ReflectorClass ForgeEventFactoryClient = new ReflectorClass("net.minecraftforge.client.event.ForgeEventFactoryClient");
    public static ReflectorMethod ForgeEventFactoryClient_fireComputeCameraAngles = new ReflectorMethod(ForgeEventFactoryClient, "fireComputeCameraAngles");
    public static ReflectorMethod ForgeEventFactoryClient_fireComputeFov = new ReflectorMethod(ForgeEventFactoryClient, "fireComputeFov");
    public static ReflectorMethod ForgeEventFactoryClient_fireRenderNameTagEvent = new ReflectorMethod(ForgeEventFactoryClient, "fireRenderNameTagEvent");
    public static ReflectorMethod ForgeEventFactoryClient_onGatherLayers = new ReflectorMethod(ForgeEventFactoryClient, "onGatherLayers");
    public static ReflectorMethod ForgeEventFactoryClient_onRegisterShaders = new ReflectorMethod(ForgeEventFactoryClient, "onRegisterShaders");
    public static ReflectorMethod ForgeEventFactoryClient_onRenderItemInFrame = new ReflectorMethod(ForgeEventFactoryClient, "onRenderItemInFrame");
    public static ReflectorMethod ForgeEventFactoryClient_onRenderLivingPre = new ReflectorMethod(ForgeEventFactoryClient, "onRenderLivingPre");
    public static ReflectorMethod ForgeEventFactoryClient_onRenderLivingPost = new ReflectorMethod(ForgeEventFactoryClient, "onRenderLivingPost");
    public static ReflectorMethod ForgeEventFactoryClient_onScreenshot = new ReflectorMethod(ForgeEventFactoryClient, "onScreenshot");
    public static ReflectorClass ForgeFaceData = new ReflectorClass(ForgeFaceData.class);
    public static ReflectorMethod ForgeFaceData_calculateNormals = ForgeFaceData.makeMethod("calculateNormals");
    public static ReflectorClass ForgeHooks = new ReflectorClass("net.minecraftforge.common.ForgeHooks");
    public static ReflectorMethod ForgeHooks_getDyeColorFromItemStack = new ReflectorMethod(ForgeHooks, "getDyeColorFromItemStack");
    public static ReflectorClass ForgeHooksClient = new ReflectorClass("net.minecraftforge.client.ForgeHooksClient");
    public static ReflectorMethod ForgeHooksClient_onCustomizeBossEventProgress = new ReflectorMethod(ForgeHooksClient, "onCustomizeBossEventProgress");
    public static ReflectorMethod ForgeHooksClient_onRenderTooltipColor = new ReflectorMethod(ForgeHooksClient, "onRenderTooltipColor");
    public static ReflectorMethod ForgeHooksClient_dispatchRenderStageRT = new ReflectorMethod(
        ForgeHooksClient,
        "dispatchRenderStage",
        new Class[] {RenderType.class, LevelRenderer.class, Matrix4f.class, Matrix4f.class, int.class, Camera.class, Frustum.class}
    );
    public static ReflectorMethod ForgeHooksClient_drawScreen = new ReflectorMethod(ForgeHooksClient, "drawScreen");
    public static ReflectorMethod ForgeHooksClient_fillNormal = new ReflectorMethod(ForgeHooksClient, "fillNormal", new Class[] {int[].class, Direction.class});
    public static ReflectorMethod ForgeHooksClient_gatherTooltipComponents6 = new ReflectorMethod(
        ForgeHooksClient, "gatherTooltipComponents", new Class[] {ItemStack.class, List.class, int.class, int.class, int.class, Font.class}
    );
    public static ReflectorMethod ForgeHooksClient_gatherTooltipComponents7 = new ReflectorMethod(
        ForgeHooksClient, "gatherTooltipComponents", new Class[] {ItemStack.class, List.class, Optional.class, int.class, int.class, int.class, Font.class}
    );
    public static ReflectorMethod ForgeHooksClient_gatherTooltipComponentsFromElements = new ReflectorMethod(
        ForgeHooksClient, "gatherTooltipComponentsFromElements"
    );
    public static ReflectorMethod ForgeHooksClient_onKeyInput = new ReflectorMethod(ForgeHooksClient, "onKeyInput");
    public static ReflectorMethod ForgeHooksClient_getFogColor = new ReflectorMethod(ForgeHooksClient, "getFogColor");
    public static ReflectorMethod ForgeHooksClient_getArmorModel = new ReflectorMethod(ForgeHooksClient, "getArmorModel");
    public static ReflectorMethod ForgeHooksClient_getArmorTexture = new ReflectorMethod(ForgeHooksClient, "getArmorTexture");
    public static ReflectorMethod ForgeHooksClient_getFluidSprites = new ReflectorMethod(ForgeHooksClient, "getFluidSprites");
    public static ReflectorMethod ForgeHooksClient_getFieldOfViewModifier = new ReflectorMethod(ForgeHooksClient, "getFieldOfViewModifier");
    public static ReflectorMethod ForgeHooksClient_getGuiFarPlane = new ReflectorMethod(ForgeHooksClient, "getGuiFarPlane");
    public static ReflectorMethod ForgeHooksClient_getShaderImportLocation = new ReflectorMethod(ForgeHooksClient, "getShaderImportLocation");
    public static ReflectorMethod ForgeHooksClient_isNameplateInRenderDistance = new ReflectorMethod(ForgeHooksClient, "isNameplateInRenderDistance");
    public static ReflectorMethod ForgeHooksClient_loadEntityShader = new ReflectorMethod(ForgeHooksClient, "loadEntityShader");
    public static ReflectorMethod ForgeHooksClient_loadTextureAtlasSprite = new ReflectorMethod(ForgeHooksClient, "loadTextureAtlasSprite");
    public static ReflectorMethod ForgeHooksClient_loadSpriteContents = new ReflectorMethod(ForgeHooksClient, "loadSpriteContents");
    public static ReflectorMethod ForgeHooksClient_makeParticleRenderTypeComparator = new ReflectorMethod(ForgeHooksClient, "makeParticleRenderTypeComparator");
    public static ReflectorMethod ForgeHooksClient_onCustomizeChatEvent = new ReflectorMethod(ForgeHooksClient, "onCustomizeChatEvent");
    public static ReflectorMethod ForgeHooksClient_onCustomizeDebugEvent = new ReflectorMethod(ForgeHooksClient, "onCustomizeDebugEvent");
    public static ReflectorMethod ForgeHooksClient_onDrawHighlight = new ReflectorMethod(ForgeHooksClient, "onDrawHighlight");
    public static ReflectorMethod ForgeHooksClient_onFogRender = new ReflectorMethod(ForgeHooksClient, "onFogRender");
    public static ReflectorMethod ForgeHooksClient_onRegisterAdditionalModels = new ReflectorMethod(ForgeHooksClient, "onRegisterAdditionalModels");
    public static ReflectorMethod ForgeHooksClient_onRenderTooltipPre = new ReflectorMethod(ForgeHooksClient, "onRenderTooltipPre");
    public static ReflectorMethod ForgeHooksClient_onScreenCharTyped = new ReflectorMethod(ForgeHooksClient, "onScreenCharTyped");
    public static ReflectorMethod ForgeHooksClient_onScreenKeyPressed = new ReflectorMethod(ForgeHooksClient, "onScreenKeyPressed");
    public static ReflectorMethod ForgeHooksClient_onScreenKeyReleased = new ReflectorMethod(ForgeHooksClient, "onScreenKeyReleased");
    public static ReflectorMethod ForgeHooksClient_onTextureStitchedPost = new ReflectorMethod(ForgeHooksClient, "onTextureStitchedPost");
    public static ReflectorMethod ForgeHooksClient_renderBlockOverlay = new ReflectorMethod(ForgeHooksClient, "renderBlockOverlay");
    public static ReflectorMethod ForgeHooksClient_renderFireOverlay = new ReflectorMethod(ForgeHooksClient, "renderFireOverlay");
    public static ReflectorMethod ForgeHooksClient_renderWaterOverlay = new ReflectorMethod(ForgeHooksClient, "renderWaterOverlay");
    public static ReflectorMethod ForgeHooksClient_renderMainMenu = new ReflectorMethod(ForgeHooksClient, "renderMainMenu");
    public static ReflectorMethod ForgeHooksClient_renderSpecificFirstPersonHand = new ReflectorMethod(ForgeHooksClient, "renderSpecificFirstPersonHand");
    public static ReflectorMethod ForgeHooksClient_shouldCauseReequipAnimation = new ReflectorMethod(ForgeHooksClient, "shouldCauseReequipAnimation");
    public static ReflectorClass ForgeConfig = new ReflectorClass("net.minecraftforge.common.ForgeConfig");
    public static ReflectorField ForgeConfig_CLIENT = new ReflectorField(ForgeConfig, "CLIENT");
    public static ReflectorClass ForgeConfig_Client = new ReflectorClass("net.minecraftforge.common.ForgeConfig$Client");
    public static ReflectorField ForgeConfig_Client_calculateAllNormals = new ReflectorField(ForgeConfig_Client, "calculateAllNormals");
    public static ReflectorField ForgeConfig_Client_forgeLightPipelineEnabled = new ReflectorField(ForgeConfig_Client, "experimentalForgeLightPipelineEnabled");
    public static ReflectorField ForgeConfig_Client_useCombinedDepthStencilAttachment = new ReflectorField(
        ForgeConfig_Client, "useCombinedDepthStencilAttachment"
    );
    public static ReflectorClass ForgeConfigSpec = new ReflectorClass("net.minecraftforge.common.ForgeConfigSpec");
    public static ReflectorField ForgeConfigSpec_childConfig = new ReflectorField(ForgeConfigSpec, "childConfig");
    public static ReflectorClass ForgeConfigSpec_ConfigValue = new ReflectorClass("net.minecraftforge.common.ForgeConfigSpec$ConfigValue");
    public static ReflectorField ForgeConfigSpec_ConfigValue_defaultSupplier = new ReflectorField(ForgeConfigSpec_ConfigValue, "defaultSupplier");
    public static ReflectorField ForgeConfigSpec_ConfigValue_spec = new ReflectorField(ForgeConfigSpec_ConfigValue, "spec");
    public static ReflectorMethod ForgeConfigSpec_ConfigValue_get = new ReflectorMethod(ForgeConfigSpec_ConfigValue, "get");
    public static ReflectorClass ForgeIChunk = new ReflectorClass(ChunkAccess.class);
    public static ReflectorMethod ForgeIChunk_getWorldForge = new ReflectorMethod(ForgeIChunk, "getWorldForge");
    public static ReflectorClass IForgeItemStack = new ReflectorClass("net.minecraftforge.common.extensions.IForgeItemStack");
    public static ReflectorMethod IForgeItemStack_getHighlightTip = new ReflectorMethod(IForgeItemStack, "getHighlightTip");
    public static ReflectorClass ForgeItemTags = new ReflectorClass(ItemTags.class);
    public static ReflectorMethod ForgeItemTags_create = ForgeItemTags.makeMethod("create", new Class[] {String.class, String.class});
    public static ReflectorClass ForgeKeyBinding = new ReflectorClass(KeyMapping.class);
    public static ReflectorMethod ForgeKeyBinding_setKeyConflictContext = new ReflectorMethod(ForgeKeyBinding, "setKeyConflictContext");
    public static ReflectorMethod ForgeKeyBinding_setKeyModifierAndCode = new ReflectorMethod(ForgeKeyBinding, "setKeyModifierAndCode");
    public static ReflectorMethod ForgeKeyBinding_getKeyModifier = new ReflectorMethod(ForgeKeyBinding, "getKeyModifier");
    public static ReflectorClass ForgeTicket = new ReflectorClass(Ticket.class);
    public static ReflectorField ForgeTicket_forceTicks = ForgeTicket.makeField("forceTicks");
    public static ReflectorMethod ForgeTicket_isForceTicks = ForgeTicket.makeMethod("isForceTicks");
    public static ReflectorClass IForgeBlockEntity = new ReflectorClass("net.minecraftforge.common.extensions.IForgeBlockEntity");
    public static ReflectorMethod IForgeBlockEntity_getRenderBoundingBox = new ReflectorMethod(IForgeBlockEntity, "getRenderBoundingBox");
    public static ReflectorMethod IForgeBlockEntity_hasCustomOutlineRendering = new ReflectorMethod(IForgeBlockEntity, "hasCustomOutlineRendering");
    public static ReflectorClass IForgeDimensionSpecialEffects = new ReflectorClass("net.minecraftforge.client.extensions.IForgeDimensionSpecialEffects");
    public static ReflectorMethod IForgeDimensionSpecialEffects_adjustLightmapColors = IForgeDimensionSpecialEffects.makeMethod("adjustLightmapColors");
    public static ReflectorMethod IForgeDimensionSpecialEffects_renderClouds = IForgeDimensionSpecialEffects.makeMethod("renderClouds");
    public static ReflectorMethod IForgeDimensionSpecialEffects_renderSky = IForgeDimensionSpecialEffects.makeMethod("renderSky");
    public static ReflectorMethod IForgeDimensionSpecialEffects_tickRain = IForgeDimensionSpecialEffects.makeMethod("tickRain");
    public static ReflectorMethod IForgeDimensionSpecialEffects_renderSnowAndRain = IForgeDimensionSpecialEffects.makeMethod("renderSnowAndRain");
    public static ReflectorClass ForgeVersion = new ReflectorClass("net.minecraftforge.versions.forge.ForgeVersion");
    public static ReflectorMethod ForgeVersion_getVersion = ForgeVersion.makeMethod("getVersion");
    public static ReflectorClass ImmediateWindowHandler = new ReflectorClass("net.minecraftforge.fml.loading.ImmediateWindowHandler");
    public static ReflectorMethod ImmediateWindowHandler_positionWindow = ImmediateWindowHandler.makeMethod("positionWindow");
    public static ReflectorMethod ImmediateWindowHandler_setupMinecraftWindow = ImmediateWindowHandler.makeMethod("setupMinecraftWindow");
    public static ReflectorMethod ImmediateWindowHandler_updateFBSize = ImmediateWindowHandler.makeMethod("updateFBSize");
    public static ReflectorClass ItemDecoratorHandler = new ReflectorClass("net.minecraftforge.client.ItemDecoratorHandler");
    public static ReflectorMethod ItemDecoratorHandler_of = ItemDecoratorHandler.makeMethod("of", new Class[] {ItemStack.class});
    public static ReflectorMethod ItemDecoratorHandler_render = ItemDecoratorHandler.makeMethod("render");
    public static ReflectorClass ForgeItemModelShaper = new ReflectorClass("net.minecraftforge.client.model.ForgeItemModelShaper");
    public static ReflectorConstructor ForgeItemModelShaper_Constructor = new ReflectorConstructor(ForgeItemModelShaper, new Class[] {ModelManager.class});
    public static ReflectorClass KeyConflictContext = new ReflectorClass("net.minecraftforge.client.settings.KeyConflictContext");
    public static ReflectorField KeyConflictContext_IN_GAME = new ReflectorField(KeyConflictContext, "IN_GAME");
    public static ReflectorClass KeyModifier = new ReflectorClass("net.minecraftforge.client.settings.KeyModifier");
    public static ReflectorMethod KeyModifier_valueFromString = new ReflectorMethod(KeyModifier, "valueFromString");
    public static ReflectorField KeyModifier_NONE = new ReflectorField(KeyModifier, "NONE");
    public static ReflectorClass Launch = new ReflectorClass("net.minecraft.launchwrapper.Launch");
    public static ReflectorField Launch_blackboard = new ReflectorField(Launch, "blackboard");
    public static ReflectorClass MinecraftForge = new ReflectorClass("net.minecraftforge.common.MinecraftForge");
    public static ReflectorField MinecraftForge_EVENT_BUS = new ReflectorField(MinecraftForge, "EVENT_BUS");
    public static ReflectorClass ModContainer = new ReflectorClass("net.minecraftforge.fml.ModContainer");
    public static ReflectorMethod ModContainer_getModId = new ReflectorMethod(ModContainer, "getModId");
    public static ReflectorClass ModList = new ReflectorClass("net.minecraftforge.fml.ModList");
    public static ReflectorField ModList_mods = ModList.makeField("mods");
    public static ReflectorMethod ModList_get = ModList.makeMethod("get");
    public static ReflectorClass ModListScreen = new ReflectorClass("net.minecraftforge.client.gui.ModListScreen");
    public static ReflectorConstructor ModListScreen_Constructor = new ReflectorConstructor(ModListScreen, new Class[] {Screen.class});
    public static ReflectorClass ModLoader = new ReflectorClass("net.minecraftforge.fml.ModLoader");
    public static ReflectorMethod ModLoader_get = ModLoader.makeMethod("get");
    public static ReflectorMethod ModLoader_postEvent = ModLoader.makeMethod("postEvent");
    public static ReflectorClass TitleScreenModUpdateIndicator = new ReflectorClass("net.minecraftforge.client.gui.TitleScreenModUpdateIndicator");
    public static ReflectorMethod TitleScreenModUpdateIndicator_init = TitleScreenModUpdateIndicator.makeMethod(
                "init", new Class[] {TitleScreen.class, Button.class}
            );
    public static ReflectorClass PartEntity = new ReflectorClass("net.minecraftforge.entity.PartEntity");
    public static ReflectorClass PlayLevelSoundEvent = new ReflectorClass("net.minecraftforge.event.PlayLevelSoundEvent");
    public static ReflectorMethod PlayLevelSoundEvent_getSound = new ReflectorMethod(PlayLevelSoundEvent, "getSound");
    public static ReflectorMethod PlayLevelSoundEvent_getSource = new ReflectorMethod(PlayLevelSoundEvent, "getSource");
    public static ReflectorMethod PlayLevelSoundEvent_getNewVolume = new ReflectorMethod(PlayLevelSoundEvent, "getNewVolume");
    public static ReflectorMethod PlayLevelSoundEvent_getNewPitch = new ReflectorMethod(PlayLevelSoundEvent, "getNewPitch");
    public static ReflectorClass QuadBakingVertexConsumer = new ReflectorClass("net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer");
    public static ReflectorField QuadBakingVertexConsumer_QUAD_DATA_SIZE = QuadBakingVertexConsumer.makeField("QUAD_DATA_SIZE");
    public static ReflectorClass QuadTransformers = new ReflectorClass("net.minecraftforge.client.model.QuadTransformers");
    public static ReflectorMethod QuadTransformers_applyingLightmap = QuadTransformers.makeMethod("applyingLightmap", new Class[] {int.class, int.class});
    public static ReflectorMethod QuadTransformers_applyingColor = QuadTransformers.makeMethod("applyingColor", new Class[] {int.class});
    public static ReflectorClass IQuadTransformer = new ReflectorClass("net.minecraftforge.client.model.IQuadTransformer");
    public static ReflectorField IQuadTransformer_STRIDE = IQuadTransformer.makeField("STRIDE");
    public static ReflectorMethod IQuadTransformer_processInPlace = IQuadTransformer.makeMethod("processInPlace", new Class[] {BakedQuad.class});
    public static ReflectorClass RenderBlockScreenEffectEvent_OverlayType = new ReflectorClass(
        "net.minecraftforge.client.event.RenderBlockScreenEffectEvent$OverlayType"
    );
    public static ReflectorField RenderBlockScreenEffectEvent_OverlayType_BLOCK = new ReflectorField(RenderBlockScreenEffectEvent_OverlayType, "BLOCK");
    public static ReflectorClass CustomizeGuiOverlayEvent_BossEventProgress = new ReflectorClass(
        "net.minecraftforge.client.event.CustomizeGuiOverlayEvent$BossEventProgress"
    );
    public static ReflectorMethod CustomizeGuiOverlayEvent_BossEventProgress_getIncrement = CustomizeGuiOverlayEvent_BossEventProgress.makeMethod(
                "getIncrement"
            );
    public static ReflectorClass RenderLevelStageEvent_Stage = new ReflectorClass(RenderLevelStageEvent.Stage.class);
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_SKY = RenderLevelStageEvent_Stage.makeField("AFTER_SKY");
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_ENTITIES = RenderLevelStageEvent_Stage.makeField("AFTER_ENTITIES");
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_BLOCK_ENTITIES = RenderLevelStageEvent_Stage.makeField("AFTER_BLOCK_ENTITIES");
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_PARTICLES = RenderLevelStageEvent_Stage.makeField("AFTER_PARTICLES");
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_WEATHER = RenderLevelStageEvent_Stage.makeField("AFTER_WEATHER");
    public static ReflectorField RenderLevelStageEvent_Stage_AFTER_LEVEL = RenderLevelStageEvent_Stage.makeField("AFTER_LEVEL");
    public static ReflectorMethod RenderLevelStageEvent_dispatch = RenderLevelStageEvent_Stage.makeMethod("dispatch");
    public static ReflectorClass RenderNameTagEvent = new ReflectorClass("net.minecraftforge.client.event.RenderNameTagEvent");
    public static ReflectorMethod RenderNameTagEvent_getContent = new ReflectorMethod(RenderNameTagEvent, "getContent");
    public static ReflectorClass RenderTooltipEvent = new ReflectorClass("net.minecraftforge.client.event.RenderTooltipEvent");
    public static ReflectorMethod RenderTooltipEvent_getFont = RenderTooltipEvent.makeMethod("getFont");
    public static ReflectorMethod RenderTooltipEvent_getX = RenderTooltipEvent.makeMethod("getX");
    public static ReflectorMethod RenderTooltipEvent_getY = RenderTooltipEvent.makeMethod("getY");
    public static ReflectorClass RenderTooltipEvent_Color = new ReflectorClass("net.minecraftforge.client.event.RenderTooltipEvent$Color");
    public static ReflectorMethod RenderTooltipEvent_Color_getBackgroundStart = RenderTooltipEvent_Color.makeMethod("getBackgroundStart");
    public static ReflectorMethod RenderTooltipEvent_Color_getBackgroundEnd = RenderTooltipEvent_Color.makeMethod("getBackgroundEnd");
    public static ReflectorMethod RenderTooltipEvent_Color_getBorderStart = RenderTooltipEvent_Color.makeMethod("getBorderStart");
    public static ReflectorMethod RenderTooltipEvent_Color_getBorderEnd = RenderTooltipEvent_Color.makeMethod("getBorderEnd");
    public static ReflectorClass ScreenshotEvent = new ReflectorClass("net.minecraftforge.client.event.ScreenshotEvent");
    public static ReflectorMethod ScreenshotEvent_getCancelMessage = new ReflectorMethod(ScreenshotEvent, "getCancelMessage");
    public static ReflectorMethod ScreenshotEvent_getScreenshotFile = new ReflectorMethod(ScreenshotEvent, "getScreenshotFile");
    public static ReflectorMethod ScreenshotEvent_getResultMessage = new ReflectorMethod(ScreenshotEvent, "getResultMessage");
    public static ReflectorClass ServerLifecycleHooks = new ReflectorClass("net.minecraftforge.server.ServerLifecycleHooks");
    public static ReflectorMethod ServerLifecycleHooks_handleServerAboutToStart = new ReflectorMethod(ServerLifecycleHooks, "handleServerAboutToStart");
    public static ReflectorMethod ServerLifecycleHooks_handleServerStarting = new ReflectorMethod(ServerLifecycleHooks, "handleServerStarting");
    public static ReflectorClass TerrainParticle = new ReflectorClass(TerrainParticle.class);
    public static ReflectorMethod TerrainParticle_updateSprite = TerrainParticle.makeMethod("updateSprite");
    public static ReflectorClass TooltipRenderUtil = new ReflectorClass(TooltipRenderUtil.class);
    public static ReflectorMethod TooltipRenderUtil_renderTooltipBackground10 = TooltipRenderUtil.makeMethod(
                "renderTooltipBackground",
                new Class[] {GuiGraphics.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class}
            );
    private static boolean logVanilla = registerResolvable("*** Reflector Vanilla ***");
    public static ReflectorClass AbstractArrow = new ReflectorClass(AbstractArrow.class);
    public static ReflectorField AbstractArrow_inGround = new ReflectorField(
        new FieldLocatorTypes(AbstractArrow.class, new Class[] {BlockState.class}, boolean.class, new Class[] {int.class}, "AbstractArrow.inGround")
    );
    public static ReflectorClass BannerBlockEntity = new ReflectorClass(BannerBlockEntity.class);
    public static ReflectorField BannerBlockEntity_customName = BannerBlockEntity.makeField(Component.class);
    public static ReflectorClass BaseContainerBlockEntity = new ReflectorClass(BaseContainerBlockEntity.class);
    public static ReflectorField BaseContainerBlockEntity_customName = BaseContainerBlockEntity.makeField(Component.class);
    public static ReflectorClass Enchantments = new ReflectorClass(Enchantments.class);
    public static ReflectorFields Enchantments_ResourceKeys = new ReflectorFields(Enchantments, ResourceKey.class, -1);
    public static ReflectorClass EntityItem = new ReflectorClass(ItemEntity.class);
    public static ReflectorField EntityItem_ITEM = new ReflectorField(EntityItem, EntityDataAccessor.class);
    public static ReflectorClass EnderDragonRenderer = new ReflectorClass(EnderDragonRenderer.class);
    public static ReflectorField EnderDragonRenderer_model = new ReflectorField(EnderDragonRenderer, EnderDragonRenderer.DragonModel.class);
    public static ReflectorClass GuiEnchantment = new ReflectorClass(EnchantmentScreen.class);
    public static ReflectorField GuiEnchantment_bookModel = new ReflectorField(GuiEnchantment, BookModel.class);
    public static ReflectorClass ItemOverride = new ReflectorClass(ItemOverride.class);
    public static ReflectorField ItemOverride_listResourceValues = new ReflectorField(ItemOverride, List.class);
    public static ReflectorClass ItemStack = new ReflectorClass(ItemStack.class);
    public static ReflectorField ItemStack_components = ItemStack.makeField(PatchedDataComponentMap.class);
    public static ReflectorClass LayerLlamaDecor = new ReflectorClass(LlamaDecorLayer.class);
    public static ReflectorField LayerLlamaDecor_model = new ReflectorField(LayerLlamaDecor, LlamaModel.class);
    public static ReflectorClass Minecraft = new ReflectorClass(Minecraft.class);
    public static ReflectorField Minecraft_debugFPS = new ReflectorField(
        new FieldLocatorTypes(Minecraft.class, new Class[] {Supplier.class}, int.class, new Class[] {String.class}, "debugFPS")
    );
    public static ReflectorField Minecraft_fontResourceManager = new ReflectorField(Minecraft, FontManager.class);
    public static ReflectorClass ModelArmorStand = new ReflectorClass(ArmorStandModel.class);
    public static ReflectorFields ModelArmorStand_ModelRenderers = new ReflectorFields(ModelArmorStand, ModelPart.class, 4);
    public static ReflectorClass ModelBee = new ReflectorClass(BeeModel.class);
    public static ReflectorFields ModelBee_ModelRenderers = new ReflectorFields(ModelBee, ModelPart.class, 2);
    public static ReflectorClass ModelBoar = new ReflectorClass(HoglinModel.class);
    public static ReflectorFields ModelBoar_ModelRenderers = new ReflectorFields(ModelBoar, ModelPart.class, 9);
    public static ReflectorClass ModelBook = new ReflectorClass(BookModel.class);
    public static ReflectorField ModelBook_root = new ReflectorField(ModelBook, ModelPart.class);
    public static ReflectorClass ModelChicken = new ReflectorClass(ChickenModel.class);
    public static ReflectorFields ModelChicken_ModelRenderers = new ReflectorFields(ModelChicken, ModelPart.class, 8);
    public static ReflectorClass ModelDragon = new ReflectorClass(EnderDragonRenderer.DragonModel.class);
    public static ReflectorFields ModelDragon_ModelRenderers = new ReflectorFields(ModelDragon, ModelPart.class, 20);
    public static ReflectorClass RenderEnderCrystal = new ReflectorClass(EndCrystalRenderer.class);
    public static ReflectorFields RenderEnderCrystal_modelRenderers = new ReflectorFields(RenderEnderCrystal, ModelPart.class, 3);
    public static ReflectorClass ModelDragonHead = new ReflectorClass(DragonHeadModel.class);
    public static ReflectorField ModelDragonHead_head = new ReflectorField(ModelDragonHead, ModelPart.class, 0);
    public static ReflectorField ModelDragonHead_jaw = new ReflectorField(ModelDragonHead, ModelPart.class, 1);
    public static ReflectorClass ModelHorse = new ReflectorClass(HorseModel.class);
    public static ReflectorFields ModelHorse_ModelRenderers = new ReflectorFields(ModelHorse, ModelPart.class, 11);
    public static ReflectorClass ModelHorseChests = new ReflectorClass(ChestedHorseModel.class);
    public static ReflectorFields ModelHorseChests_ModelRenderers = new ReflectorFields(ModelHorseChests, ModelPart.class, 2);
    public static ReflectorClass ModelAxolotl = new ReflectorClass(AxolotlModel.class);
    public static ReflectorFields ModelAxolotl_ModelRenderers = new ReflectorFields(ModelAxolotl, ModelPart.class, 10);
    public static ReflectorClass ModelFox = new ReflectorClass(FoxModel.class);
    public static ReflectorFields ModelFox_ModelRenderers = new ReflectorFields(ModelFox, ModelPart.class, 7);
    public static ReflectorClass RenderLeashKnot = new ReflectorClass(LeashKnotRenderer.class);
    public static ReflectorField RenderLeashKnot_leashKnotModel = new ReflectorField(RenderLeashKnot, LeashKnotModel.class);
    public static ReflectorClass ModelLlama = new ReflectorClass(LlamaModel.class);
    public static ReflectorFields ModelLlama_ModelRenderers = new ReflectorFields(ModelLlama, ModelPart.class, 8);
    public static ReflectorClass ModelOcelot = new ReflectorClass(OcelotModel.class);
    public static ReflectorFields ModelOcelot_ModelRenderers = new ReflectorFields(ModelOcelot, ModelPart.class, 8);
    public static ReflectorClass ModelPiglin = new ReflectorClass(PiglinModel.class);
    public static ReflectorFields ModelPiglin_ModelRenderers = new ReflectorFields(ModelPiglin, ModelPart.class, 2);
    public static ReflectorClass ModelPiglinHead = new ReflectorClass(PiglinHeadModel.class);
    public static ReflectorFields ModelPiglinHead_ModelRenderers = new ReflectorFields(ModelPiglinHead, ModelPart.class, 3);
    public static ReflectorClass ModelQuadruped = new ReflectorClass(QuadrupedModel.class);
    public static ReflectorFields ModelQuadruped_ModelRenderers = new ReflectorFields(ModelQuadruped, ModelPart.class, 6);
    public static ReflectorClass ModelRabbit = new ReflectorClass(RabbitModel.class);
    public static ReflectorFields ModelRabbit_ModelRenderers = new ReflectorFields(ModelRabbit, ModelPart.class, 12);
    public static ReflectorClass ModelShulker = new ReflectorClass(ShulkerModel.class);
    public static ReflectorFields ModelShulker_ModelRenderers = new ReflectorFields(ModelShulker, ModelPart.class, 3);
    public static ReflectorClass ModelShield = new ReflectorClass(ShieldModel.class);
    public static ReflectorFields ModelShield_ModelRenderers = new ReflectorFields(ModelShield, ModelPart.class, 3);
    public static ReflectorClass ModelSkull = new ReflectorClass(SkullModel.class);
    public static ReflectorFields ModelSkull_renderers = new ReflectorFields(ModelSkull, ModelPart.class, 2);
    public static ReflectorClass ModelTadpole = new ReflectorClass(TadpoleModel.class);
    public static ReflectorFields ModelTadpole_ModelRenderers = new ReflectorFields(ModelTadpole, ModelPart.class, 2);
    public static ReflectorClass ModelTrident = new ReflectorClass(TridentModel.class);
    public static ReflectorField ModelTrident_root = new ReflectorField(ModelTrident, ModelPart.class);
    public static ReflectorClass ModelTurtle = new ReflectorClass(TurtleModel.class);
    public static ReflectorField ModelTurtle_body2 = new ReflectorField(ModelTurtle, ModelPart.class, 0);
    public static ReflectorClass ModelWolf = new ReflectorClass(WolfModel.class);
    public static ReflectorFields ModelWolf_ModelRenderers = new ReflectorFields(ModelWolf, ModelPart.class, 10);
    public static ReflectorClass OptiFineResourceLocator = ReflectorForge.getReflectorClassOptiFineResourceLocator();
    public static ReflectorMethod OptiFineResourceLocator_getOptiFineResourceStream = new ReflectorMethod(OptiFineResourceLocator, "getOptiFineResourceStream");
    public static ReflectorClass Potion = new ReflectorClass(Potion.class);
    public static ReflectorField Potion_baseName = Potion.makeField(String.class);
    public static ReflectorClass RenderBoat = new ReflectorClass(BoatRenderer.class);
    public static ReflectorField RenderBoat_boatResources = new ReflectorField(RenderBoat, Map.class);
    public static ReflectorClass RenderEvokerFangs = new ReflectorClass(EvokerFangsRenderer.class);
    public static ReflectorField RenderEvokerFangs_model = new ReflectorField(RenderEvokerFangs, EvokerFangsModel.class);
    public static ReflectorClass RenderLlamaSpit = new ReflectorClass(LlamaSpitRenderer.class);
    public static ReflectorField RenderLlamaSpit_model = new ReflectorField(RenderLlamaSpit, LlamaSpitModel.class);
    public static ReflectorClass RenderPufferfish = new ReflectorClass(PufferfishRenderer.class);
    public static ReflectorField RenderPufferfish_modelSmall = new ReflectorField(RenderPufferfish, EntityModel.class, 0);
    public static ReflectorField RenderPufferfish_modelMedium = new ReflectorField(RenderPufferfish, EntityModel.class, 1);
    public static ReflectorField RenderPufferfish_modelBig = new ReflectorField(RenderPufferfish, EntityModel.class, 2);
    public static ReflectorClass RenderMinecart = new ReflectorClass(MinecartRenderer.class);
    public static ReflectorField RenderMinecart_modelMinecart = new ReflectorField(RenderMinecart, EntityModel.class);
    public static ReflectorClass RenderShulkerBullet = new ReflectorClass(ShulkerBulletRenderer.class);
    public static ReflectorField RenderShulkerBullet_model = new ReflectorField(RenderShulkerBullet, ShulkerBulletModel.class);
    public static ReflectorClass RenderTrident = new ReflectorClass(ThrownTridentRenderer.class);
    public static ReflectorField RenderTrident_modelTrident = new ReflectorField(RenderTrident, TridentModel.class);
    public static ReflectorClass RenderTropicalFish = new ReflectorClass(TropicalFishRenderer.class);
    public static ReflectorField RenderTropicalFish_modelA = new ReflectorField(RenderTropicalFish, ColorableHierarchicalModel.class, 0);
    public static ReflectorField RenderTropicalFish_modelB = new ReflectorField(RenderTropicalFish, ColorableHierarchicalModel.class, 1);
    public static ReflectorClass RenderWindCharge = new ReflectorClass(WindChargeRenderer.class);
    public static ReflectorField RenderWindCharge_model = new ReflectorField(RenderWindCharge, WindChargeModel.class);
    public static ReflectorClass TropicalFishPatternLayer = new ReflectorClass(TropicalFishPatternLayer.class);
    public static ReflectorField TropicalFishPatternLayer_modelA = new ReflectorField(TropicalFishPatternLayer, TropicalFishModelA.class);
    public static ReflectorField TropicalFishPatternLayer_modelB = new ReflectorField(TropicalFishPatternLayer, TropicalFishModelB.class);
    public static ReflectorClass RenderWitherSkull = new ReflectorClass(WitherSkullRenderer.class);
    public static ReflectorField RenderWitherSkull_model = new ReflectorField(RenderWitherSkull, SkullModel.class);
    public static ReflectorClass SimpleBakedModel = new ReflectorClass(SimpleBakedModel.class);
    public static ReflectorField SimpleBakedModel_generalQuads = SimpleBakedModel.makeField(List.class);
    public static ReflectorField SimpleBakedModel_faceQuads = SimpleBakedModel.makeField(Map.class);
    public static ReflectorClass TileEntityBannerRenderer = new ReflectorClass(BannerRenderer.class);
    public static ReflectorFields TileEntityBannerRenderer_modelRenderers = new ReflectorFields(TileEntityBannerRenderer, ModelPart.class, 3);
    public static ReflectorClass TileEntityBedRenderer = new ReflectorClass(BedRenderer.class);
    public static ReflectorField TileEntityBedRenderer_headModel = new ReflectorField(TileEntityBedRenderer, ModelPart.class, 0);
    public static ReflectorField TileEntityBedRenderer_footModel = new ReflectorField(TileEntityBedRenderer, ModelPart.class, 1);
    public static ReflectorClass TileEntityBellRenderer = new ReflectorClass(BellRenderer.class);
    public static ReflectorField TileEntityBellRenderer_modelRenderer = new ReflectorField(TileEntityBellRenderer, ModelPart.class);
    public static ReflectorClass TileEntityBeacon = new ReflectorClass(BeaconBlockEntity.class);
    public static ReflectorField TileEntityBeacon_customName = new ReflectorField(TileEntityBeacon, Component.class);
    public static ReflectorField TileEntityBeacon_levels = new ReflectorField(
        new FieldLocatorTypes(BeaconBlockEntity.class, new Class[] {List.class}, int.class, new Class[] {int.class}, "BeaconBlockEntity.levels")
    );
    public static ReflectorClass TileEntityChestRenderer = new ReflectorClass(ChestRenderer.class);
    public static ReflectorFields TileEntityChestRenderer_modelRenderers = new ReflectorFields(TileEntityChestRenderer, ModelPart.class, 9);
    public static ReflectorClass TileEntityConduitRenderer = new ReflectorClass(ConduitRenderer.class);
    public static ReflectorFields TileEntityConduitRenderer_modelRenderers = new ReflectorFields(TileEntityConduitRenderer, ModelPart.class, 4);
    public static ReflectorClass TileEntityDecoratedPotRenderer = new ReflectorClass(DecoratedPotRenderer.class);
    public static ReflectorFields TileEntityDecoratedPotRenderer_modelRenderers = new ReflectorFields(TileEntityDecoratedPotRenderer, ModelPart.class, 7);
    public static ReflectorClass TileEntityEnchantmentTableRenderer = new ReflectorClass(EnchantTableRenderer.class);
    public static ReflectorField TileEntityEnchantmentTableRenderer_modelBook = new ReflectorField(TileEntityEnchantmentTableRenderer, BookModel.class);
    public static ReflectorClass TileEntityHangingSignRenderer = new ReflectorClass(HangingSignRenderer.class);
    public static ReflectorField TileEntityHangingSignRenderer_hangingSignModels = new ReflectorField(TileEntityHangingSignRenderer, Map.class);
    public static ReflectorClass TileEntityLecternRenderer = new ReflectorClass(LecternRenderer.class);
    public static ReflectorField TileEntityLecternRenderer_modelBook = new ReflectorField(TileEntityLecternRenderer, BookModel.class);
    public static ReflectorClass TileEntityShulkerBoxRenderer = new ReflectorClass(ShulkerBoxRenderer.class);
    public static ReflectorField TileEntityShulkerBoxRenderer_model = new ReflectorField(TileEntityShulkerBoxRenderer, ShulkerModel.class);
    public static ReflectorClass TileEntitySignRenderer = new ReflectorClass(SignRenderer.class);
    public static ReflectorField TileEntitySignRenderer_signModels = new ReflectorField(TileEntitySignRenderer, Map.class);

    public static void callVoid(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return;
            }

            method.invoke(null, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
        }
    }

    public static boolean callBoolean(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return false;
            }
            else
            {
                Boolean obool = (Boolean)method.invoke(null, params);
                return obool;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return false;
        }
    }

    public static int callInt(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0;
            }
            else
            {
                Integer integer = (Integer)method.invoke(null, params);
                return integer;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return 0;
        }
    }

    public static long callLong(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0L;
            }
            else
            {
                Long olong = (Long)method.invoke(null, params);
                return olong;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return 0L;
        }
    }

    public static float callFloat(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0.0F;
            }
            else
            {
                Float f = (Float)method.invoke(null, params);
                return f;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return 0.0F;
        }
    }

    public static double callDouble(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0.0;
            }
            else
            {
                Double d0 = (Double)method.invoke(null, params);
                return d0;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return 0.0;
        }
    }

    public static String callString(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();
            return method == null ? null : (String)method.invoke(null, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return null;
        }
    }

    public static Object call(ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();
            return method == null ? null : method.invoke(null, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, null, refMethod, params);
            return null;
        }
    }

    public static void callVoid(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            if (obj == null)
            {
                return;
            }

            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return;
            }

            method.invoke(obj, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
        }
    }

    public static boolean callBoolean(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return false;
            }
            else
            {
                Boolean obool = (Boolean)method.invoke(obj, params);
                return obool;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return false;
        }
    }

    public static int callInt(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0;
            }
            else
            {
                Integer integer = (Integer)method.invoke(obj, params);
                return integer;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return 0;
        }
    }

    public static long callLong(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0L;
            }
            else
            {
                Long olong = (Long)method.invoke(obj, params);
                return olong;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return 0L;
        }
    }

    public static float callFloat(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0.0F;
            }
            else
            {
                Float f = (Float)method.invoke(obj, params);
                return f;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return 0.0F;
        }
    }

    public static double callDouble(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();

            if (method == null)
            {
                return 0.0;
            }
            else
            {
                Double d0 = (Double)method.invoke(obj, params);
                return d0;
            }
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return 0.0;
        }
    }

    public static String callString(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();
            return method == null ? null : (String)method.invoke(obj, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return null;
        }
    }

    public static Object call(Object obj, ReflectorMethod refMethod, Object... params)
    {
        try
        {
            Method method = refMethod.getTargetMethod();
            return method == null ? null : method.invoke(obj, params);
        }
        catch (Throwable throwable)
        {
            handleException(throwable, obj, refMethod, params);
            return null;
        }
    }

    public static Object getFieldValue(ReflectorField refField)
    {
        return getFieldValue(null, refField);
    }

    public static Object getFieldValue(Object obj, ReflectorField refField)
    {
        try
        {
            Field field = refField.getTargetField();
            return field == null ? null : field.get(obj);
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return null;
        }
    }

    public static boolean getFieldValueBoolean(Object obj, ReflectorField refField, boolean def)
    {
        try
        {
            Field field = refField.getTargetField();
            return field == null ? def : field.getBoolean(obj);
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return def;
        }
    }

    public static Object getFieldValue(ReflectorFields refFields, int index)
    {
        ReflectorField reflectorfield = refFields.getReflectorField(index);
        return reflectorfield == null ? null : getFieldValue(reflectorfield);
    }

    public static Object getFieldValue(Object obj, ReflectorFields refFields, int index)
    {
        ReflectorField reflectorfield = refFields.getReflectorField(index);
        return reflectorfield == null ? null : getFieldValue(obj, reflectorfield);
    }

    public static float getFieldValueFloat(Object obj, ReflectorField refField, float def)
    {
        try
        {
            Field field = refField.getTargetField();
            return field == null ? def : field.getFloat(obj);
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return def;
        }
    }

    public static int getFieldValueInt(ReflectorField refField, int def)
    {
        return getFieldValueInt(null, refField, def);
    }

    public static int getFieldValueInt(Object obj, ReflectorField refField, int def)
    {
        try
        {
            Field field = refField.getTargetField();
            return field == null ? def : field.getInt(obj);
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return def;
        }
    }

    public static long getFieldValueLong(Object obj, ReflectorField refField, long def)
    {
        try
        {
            Field field = refField.getTargetField();
            return field == null ? def : field.getLong(obj);
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return def;
        }
    }

    public static boolean setFieldValue(ReflectorField refField, Object value)
    {
        return setFieldValue(null, refField, value);
    }

    public static boolean setFieldValue(Object obj, ReflectorFields refFields, int index, Object value)
    {
        ReflectorField reflectorfield = refFields.getReflectorField(index);

        if (reflectorfield == null)
        {
            return false;
        }
        else
        {
            setFieldValue(obj, reflectorfield, value);
            return true;
        }
    }

    public static boolean setFieldValue(Object obj, ReflectorField refField, Object value)
    {
        try
        {
            Field field = refField.getTargetField();

            if (field == null)
            {
                return false;
            }
            else
            {
                field.set(obj, value);
                return true;
            }
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return false;
        }
    }

    public static boolean setFieldValueInt(ReflectorField refField, int value)
    {
        return setFieldValueInt(null, refField, value);
    }

    public static boolean setFieldValueInt(Object obj, ReflectorField refField, int value)
    {
        try
        {
            Field field = refField.getTargetField();

            if (field == null)
            {
                return false;
            }
            else
            {
                field.setInt(obj, value);
                return true;
            }
        }
        catch (Throwable throwable)
        {
            Log.error("", throwable);
            return false;
        }
    }

    public static boolean postForgeBusEvent(ReflectorConstructor constr, Object... params)
    {
        Object object = newInstance(constr, params);
        return object == null ? false : postForgeBusEvent(object);
    }

    public static boolean postForgeBusEvent(Object event)
    {
        if (event == null)
        {
            return false;
        }
        else
        {
            Object object = getFieldValue(MinecraftForge_EVENT_BUS);

            if (object == null)
            {
                return false;
            }
            else
            {
                return !(call(object, EventBus_post, event) instanceof Boolean obool) ? false : obool;
            }
        }
    }

    public static Object newInstance(ReflectorConstructor constr, Object... params)
    {
        Constructor constructor = constr.getTargetConstructor();

        if (constructor == null)
        {
            return null;
        }
        else
        {
            try
            {
                return constructor.newInstance(params);
            }
            catch (Throwable throwable)
            {
                handleException(throwable, constr, params);
                return null;
            }
        }
    }

    public static boolean matchesTypes(Class[] pTypes, Class[] cTypes)
    {
        if (pTypes.length != cTypes.length)
        {
            return false;
        }
        else
        {
            for (int i = 0; i < cTypes.length; i++)
            {
                Class oclass = pTypes[i];
                Class oclass1 = cTypes[i];

                if (oclass != oclass1)
                {
                    return false;
                }
            }

            return true;
        }
    }

    private static void dbgCall(boolean isStatic, String callType, ReflectorMethod refMethod, Object[] params, Object retVal)
    {
        String s = refMethod.getTargetMethod().getDeclaringClass().getName();
        String s1 = refMethod.getTargetMethod().getName();
        String s2 = "";

        if (isStatic)
        {
            s2 = " static";
        }

        Log.dbg(callType + s2 + " " + s + "." + s1 + "(" + ArrayUtils.arrayToString(params) + ") => " + retVal);
    }

    private static void dbgCallVoid(boolean isStatic, String callType, ReflectorMethod refMethod, Object[] params)
    {
        String s = refMethod.getTargetMethod().getDeclaringClass().getName();
        String s1 = refMethod.getTargetMethod().getName();
        String s2 = "";

        if (isStatic)
        {
            s2 = " static";
        }

        Log.dbg(callType + s2 + " " + s + "." + s1 + "(" + ArrayUtils.arrayToString(params) + ")");
    }

    private static void dbgFieldValue(boolean isStatic, String accessType, ReflectorField refField, Object val)
    {
        String s = refField.getTargetField().getDeclaringClass().getName();
        String s1 = refField.getTargetField().getName();
        String s2 = "";

        if (isStatic)
        {
            s2 = " static";
        }

        Log.dbg(accessType + s2 + " " + s + "." + s1 + " => " + val);
    }

    private static void handleException(Throwable e, Object obj, ReflectorMethod refMethod, Object[] params)
    {
        if (e instanceof InvocationTargetException)
        {
            if (e.getCause() instanceof RuntimeException runtimeexception)
            {
                throw runtimeexception;
            }
            else
            {
                Log.error("", e);
            }
        }
        else
        {
            Log.warn("*** Exception outside of method ***");
            Log.warn("Method deactivated: " + refMethod.getTargetMethod());
            refMethod.deactivate();

            if (e instanceof IllegalArgumentException)
            {
                Log.warn("*** IllegalArgumentException ***");
                Log.warn("Method: " + refMethod.getTargetMethod());
                Log.warn("Object: " + obj);
                Log.warn("Parameter classes: " + ArrayUtils.arrayToString(getClasses(params)));
                Log.warn("Parameters: " + ArrayUtils.arrayToString(params));
            }

            Log.warn("", e);
        }
    }

    private static void handleException(Throwable e, ReflectorConstructor refConstr, Object[] params)
    {
        if (e instanceof InvocationTargetException)
        {
            Log.error("", e);
        }
        else
        {
            Log.warn("*** Exception outside of constructor ***");
            Log.warn("Constructor deactivated: " + refConstr.getTargetConstructor());
            refConstr.deactivate();

            if (e instanceof IllegalArgumentException)
            {
                Log.warn("*** IllegalArgumentException ***");
                Log.warn("Constructor: " + refConstr.getTargetConstructor());
                Log.warn("Parameter classes: " + ArrayUtils.arrayToString(getClasses(params)));
                Log.warn("Parameters: " + ArrayUtils.arrayToString(params));
            }

            Log.warn("", e);
        }
    }

    private static Object[] getClasses(Object[] objs)
    {
        if (objs == null)
        {
            return new Class[0];
        }
        else
        {
            Class[] aclass = new Class[objs.length];

            for (int i = 0; i < aclass.length; i++)
            {
                Object object = objs[i];

                if (object != null)
                {
                    aclass[i] = object.getClass();
                }
            }

            return aclass;
        }
    }

    private static ReflectorField[] getReflectorFields(ReflectorClass parentClass, Class fieldType, int count)
    {
        ReflectorField[] areflectorfield = new ReflectorField[count];

        for (int i = 0; i < areflectorfield.length; i++)
        {
            areflectorfield[i] = new ReflectorField(parentClass, fieldType, i);
        }

        return areflectorfield;
    }

    private static boolean registerResolvable(final String str)
    {
        String s = str;
        IResolvable iresolvable = new IResolvable()
        {
            @Override
            public void resolve()
            {
                Reflector.LOGGER.info("[OptiFine] " + str);
            }
        };
        ReflectorResolver.register(iresolvable);
        return true;
    }
}
