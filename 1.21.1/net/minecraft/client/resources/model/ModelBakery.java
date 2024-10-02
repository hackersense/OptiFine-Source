package net.minecraft.client.resources.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class ModelBakery
{
    public static final Material FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/fire_0"));
    public static final Material FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/fire_1"));
    public static final Material LAVA_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/lava_flow"));
    public static final Material WATER_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/water_flow"));
    public static final Material WATER_OVERLAY = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/water_overlay"));
    public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, ResourceLocation.withDefaultNamespace("entity/banner_base"));
    public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base"));
    public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base_nopattern"));
    public static final int DESTROY_STAGE_COUNT = 10;
    public static final List<ResourceLocation> DESTROY_STAGES = IntStream.range(0, 10)
            .mapToObj(indexIn -> ResourceLocation.withDefaultNamespace("block/destroy_stage_" + indexIn))
            .collect(Collectors.toList());
    public static final List<ResourceLocation> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
            .map(locationIn -> locationIn.withPath(pathIn -> "textures/" + pathIn + ".png"))
            .collect(Collectors.toList());
    public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderType::crumbling).collect(Collectors.toList());
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BUILTIN_SLASH = "builtin/";
    private static final String BUILTIN_SLASH_GENERATED = "builtin/generated";
    private static final String BUILTIN_BLOCK_ENTITY = "builtin/entity";
    private static final String MISSING_MODEL_NAME = "missing";
    public static final ResourceLocation MISSING_MODEL_LOCATION = ResourceLocation.withDefaultNamespace("builtin/missing");
    public static final ModelResourceLocation MISSING_MODEL_VARIANT = new ModelResourceLocation(MISSING_MODEL_LOCATION, "missing");
    public static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    @VisibleForTesting
    public static final String MISSING_MODEL_MESH = ("{    'textures': {       'particle': '"
                                            + MissingTextureAtlasSprite.getLocation().getPath()
                                            + "',       'missingno': '"
                                            + MissingTextureAtlasSprite.getLocation().getPath()
                                            + "'    },    'elements': [         {  'from': [ 0, 0, 0 ],            'to': [ 16, 16, 16 ],            'faces': {                'down':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'down',  'texture': '#missingno' },                'up':    { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'up',    'texture': '#missingno' },                'north': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'north', 'texture': '#missingno' },                'south': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'south', 'texture': '#missingno' },                'west':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'west',  'texture': '#missingno' },                'east':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'east',  'texture': '#missingno' }            }        }    ]}")
                                           .replace('\'', '"');
    private static final Map<String, String> BUILTIN_MODELS = Map.of("missing", MISSING_MODEL_MESH);
    public static final BlockModel GENERATION_MARKER = Util.make(
                BlockModel.fromString("{\"gui_light\": \"front\"}"), modelIn -> modelIn.name = "generation marker"
            );
    public static final BlockModel BLOCK_ENTITY_MARKER = Util.make(
                BlockModel.fromString("{\"gui_light\": \"side\"}"), modelIn -> modelIn.name = "block entity marker"
            );
    static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private final Map<ResourceLocation, BlockModel> modelResources;
    private final Set<ResourceLocation> loadingStack = new HashSet<>();
    private final Map<ResourceLocation, UnbakedModel> unbakedCache = new HashMap<>();
    final Map<ModelBakery.BakedCacheKey, BakedModel> bakedCache = new HashMap<>();
    private final Map<ModelResourceLocation, UnbakedModel> topLevelModels = new HashMap<>();
    private final Map<ModelResourceLocation, BakedModel> bakedTopLevelModels = new HashMap<>();
    private final UnbakedModel missingModel;
    private final Object2IntMap<BlockState> modelGroups;
    public Map<ResourceLocation, UnbakedModel> mapUnbakedModels;

    public ModelBakery(
        BlockColors p_249183_,
        ProfilerFiller p_252014_,
        Map<ResourceLocation, BlockModel> p_251087_,
        Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> p_250416_
    )
    {
        this.modelResources = new HashMap<>(p_251087_);
        p_252014_.push("missing_model");

        try
        {
            this.missingModel = this.loadBlockModel(MISSING_MODEL_LOCATION);
            this.registerModel(MISSING_MODEL_VARIANT, this.missingModel);
        }
        catch (IOException ioexception)
        {
            LOGGER.error("Error loading missing model, should never happen :(", (Throwable)ioexception);
            throw new RuntimeException(ioexception);
        }

        BlockStateModelLoader blockstatemodelloader = new BlockStateModelLoader(p_250416_, p_252014_, this.missingModel, p_249183_, this::registerModelAndLoadDependencies);
        blockstatemodelloader.loadAllBlockStates();
        this.modelGroups = blockstatemodelloader.getModelGroups();
        p_252014_.popPush("items");

        for (ResourceLocation resourcelocation : BuiltInRegistries.ITEM.keySet())
        {
            this.loadItemModelAndDependencies(resourcelocation);
        }

        p_252014_.popPush("special");
        this.loadSpecialItemModelAndDependencies(ItemRenderer.TRIDENT_IN_HAND_MODEL);
        this.loadSpecialItemModelAndDependencies(ItemRenderer.SPYGLASS_IN_HAND_MODEL);
        Set<ModelResourceLocation> set = Sets.newHashSet();
        Reflector.ForgeHooksClient_onRegisterAdditionalModels.call(set);

        for (ModelResourceLocation modelresourcelocation : set)
        {
            this.registerModel(modelresourcelocation, this.getModel(modelresourcelocation.id()));
        }

        this.mapUnbakedModels = this.unbakedCache;
        TextureUtils.registerCustomModels(this);
        this.topLevelModels.values().forEach(modelIn -> modelIn.resolveParents(this::getModel));
        p_252014_.pop();
    }

    public void bakeModels(ModelBakery.TextureGetter p_343407_)
    {
        this.topLevelModels.forEach((locIn, modelIn) ->
        {
            BakedModel bakedmodel = null;

            try {
                bakedmodel = new ModelBakery.ModelBakerImpl(p_343407_, locIn).bakeUncached(modelIn, BlockModelRotation.X0_Y0);
            }
            catch (Exception exception)
            {
                LOGGER.warn("Unable to bake model: '{}': {}", locIn, exception);
            }

            if (bakedmodel != null)
            {
                this.bakedTopLevelModels.put(locIn, bakedmodel);
            }
        });
    }

    public UnbakedModel getModel(ResourceLocation p_119342_)
    {
        if (this.unbakedCache.containsKey(p_119342_))
        {
            return this.unbakedCache.get(p_119342_);
        }
        else if (this.loadingStack.contains(p_119342_))
        {
            throw new IllegalStateException("Circular reference while loading " + p_119342_);
        }
        else
        {
            this.loadingStack.add(p_119342_);

            while (!this.loadingStack.isEmpty())
            {
                ResourceLocation resourcelocation = this.loadingStack.iterator().next();

                try
                {
                    if (!this.unbakedCache.containsKey(resourcelocation))
                    {
                        UnbakedModel unbakedmodel = this.loadBlockModel(resourcelocation);
                        this.unbakedCache.put(resourcelocation, unbakedmodel);
                        this.loadingStack.addAll(unbakedmodel.getDependencies());
                    }
                }
                catch (Exception exception1)
                {
                    LOGGER.warn("Unable to load model: '{}' referenced from: {}", resourcelocation, p_119342_);
                    LOGGER.warn(exception1.getClass().getName() + ": " + exception1.getMessage());
                    this.unbakedCache.put(resourcelocation, this.missingModel);
                }
                finally
                {
                    this.loadingStack.remove(resourcelocation);
                }
            }

            return this.unbakedCache.getOrDefault(p_119342_, this.missingModel);
        }
    }

    public void loadItemModelAndDependencies(ResourceLocation p_344766_)
    {
        ModelResourceLocation modelresourcelocation = ModelResourceLocation.inventory(p_344766_);
        ResourceLocation resourcelocation = p_344766_.withPrefix("item/");
        String s = p_344766_.getPath();

        if (s.startsWith("optifine/") || s.startsWith("item/"))
        {
            resourcelocation = p_344766_.withPrefix("");
        }

        UnbakedModel unbakedmodel = this.getModel(resourcelocation);
        this.registerModelAndLoadDependencies(modelresourcelocation, unbakedmodel);
    }

    private void loadSpecialItemModelAndDependencies(ModelResourceLocation p_342811_)
    {
        ResourceLocation resourcelocation = p_342811_.id().withPrefix("item/");
        UnbakedModel unbakedmodel = this.getModel(resourcelocation);
        this.registerModelAndLoadDependencies(p_342811_, unbakedmodel);
    }

    private void registerModelAndLoadDependencies(ModelResourceLocation p_342218_, UnbakedModel p_344824_)
    {
        for (ResourceLocation resourcelocation : p_344824_.getDependencies())
        {
            this.getModel(resourcelocation);
        }

        this.registerModel(p_342218_, p_344824_);
    }

    private void registerModel(ModelResourceLocation p_342222_, UnbakedModel p_344405_)
    {
        this.topLevelModels.put(p_342222_, p_344405_);
    }

    private BlockModel loadBlockModel(ResourceLocation p_119365_) throws IOException
    {
        String s = p_119365_.getPath();

        if ("builtin/generated".equals(s))
        {
            return GENERATION_MARKER;
        }
        else if ("builtin/entity".equals(s))
        {
            return BLOCK_ENTITY_MARKER;
        }
        else if (s.startsWith("builtin/"))
        {
            String s2 = s.substring("builtin/".length());
            String s3 = BUILTIN_MODELS.get(s2);

            if (s3 == null)
            {
                throw new FileNotFoundException(p_119365_.toString());
            }
            else
            {
                Reader reader = new StringReader(s3);
                BlockModel blockmodel1 = BlockModel.fromStream(reader);
                blockmodel1.name = p_119365_.toString();
                return blockmodel1;
            }
        }
        else
        {
            ResourceLocation resourcelocation = this.getModelLocation(p_119365_);
            BlockModel blockmodel = this.modelResources.get(resourcelocation);

            if (blockmodel == null)
            {
                blockmodel = this.loadCustomModel(resourcelocation);

                if (blockmodel != null)
                {
                    this.modelResources.put(resourcelocation, blockmodel);
                }
            }

            if (blockmodel == null)
            {
                throw new FileNotFoundException(resourcelocation.toString());
            }
            else
            {
                blockmodel.name = p_119365_.toString();
                String s1 = TextureUtils.getBasePath(resourcelocation.getPath());
                fixModelLocations(blockmodel, s1);
                return blockmodel;
            }
        }
    }

    public Map<ModelResourceLocation, BakedModel> getBakedTopLevelModels()
    {
        return this.bakedTopLevelModels;
    }

    public Object2IntMap<BlockState> getModelGroups()
    {
        return this.modelGroups;
    }

    private ResourceLocation getModelLocation(ResourceLocation location)
    {
        String s = location.getPath();

        if (s.startsWith("optifine/"))
        {
            if (!s.endsWith(".json"))
            {
                location = new ResourceLocation(location.getNamespace(), s + ".json");
            }

            return location;
        }
        else
        {
            return MODEL_LISTER.idToFile(location);
        }
    }

    public static void fixModelLocations(BlockModel modelBlock, String basePath)
    {
        ResourceLocation resourcelocation = fixModelLocation(FaceBakery.getParentLocation(modelBlock), basePath);

        if (resourcelocation != FaceBakery.getParentLocation(modelBlock))
        {
            FaceBakery.setParentLocation(modelBlock, resourcelocation);
        }

        if (FaceBakery.getTextures(modelBlock) != null)
        {
            for (Entry<String, Either<Material, String>> entry : FaceBakery.getTextures(modelBlock).entrySet())
            {
                Either<Material, String> either = entry.getValue();
                Optional<Material> optional = either.left();

                if (optional.isPresent())
                {
                    Material material = optional.get();
                    ResourceLocation resourcelocation1 = material.texture();
                    String s = resourcelocation1.getPath();
                    String s1 = fixResourcePath(s, basePath);

                    if (!s1.equals(s))
                    {
                        ResourceLocation resourcelocation2 = new ResourceLocation(resourcelocation1.getNamespace(), s1);
                        Material material1 = new Material(material.atlasLocation(), resourcelocation2);
                        Either<Material, String> either1 = Either.left(material1);
                        entry.setValue(either1);
                    }
                }
            }
        }
    }

    public static ResourceLocation fixModelLocation(ResourceLocation loc, String basePath)
    {
        if (loc != null && basePath != null)
        {
            if (!loc.getNamespace().equals("minecraft"))
            {
                return loc;
            }
            else
            {
                String s = loc.getPath();
                String s1 = fixResourcePath(s, basePath);

                if (s1 != s)
                {
                    loc = new ResourceLocation(loc.getNamespace(), s1);
                }

                return loc;
            }
        }
        else
        {
            return loc;
        }
    }

    private static String fixResourcePath(String path, String basePath)
    {
        path = TextureUtils.fixResourcePath(path, basePath);
        path = StrUtils.removeSuffix(path, ".json");
        return StrUtils.removeSuffix(path, ".png");
    }

    public BlockModel loadCustomModel(ResourceLocation locJson)
    {
        try
        {
            Resource resource = Config.getResource(locJson);
            Reader reader = resource.openAsReader();
            return BlockModel.fromStream(reader);
        }
        catch (Exception exception)
        {
            Config.warn("Error loading model: " + locJson);
            Config.warn(exception.getClass().getName() + ": " + exception.getMessage());
            return null;
        }
    }

    static record BakedCacheKey(ResourceLocation id, Transformation transformation, boolean isUvLocked)
    {
    }

    class ModelBakerImpl implements ModelBaker
    {
        private final Function<Material, TextureAtlasSprite> modelTextureGetter;

        ModelBakerImpl(final ModelBakery.TextureGetter p_342310_, final ModelResourceLocation p_344289_)
        {
            this.modelTextureGetter = materialIn -> p_342310_.get(p_344289_, materialIn);
        }

        @Override
        public UnbakedModel getModel(ResourceLocation p_248568_)
        {
            return ModelBakery.this.getModel(p_248568_);
        }

        @Override
        public Function<Material, TextureAtlasSprite> getModelTextureGetter()
        {
            return this.modelTextureGetter;
        }

        @Override
        public BakedModel bake(ResourceLocation p_252176_, ModelState p_249765_)
        {
            return this.bake(p_252176_, p_249765_, this.modelTextureGetter);
        }

        @Override
        public BakedModel bake(ResourceLocation locIn, ModelState stateIn, Function<Material, TextureAtlasSprite> sprites)
        {
            ModelBakery.BakedCacheKey modelbakery$bakedcachekey = new ModelBakery.BakedCacheKey(locIn, stateIn.getRotation(), stateIn.isUvLocked());
            BakedModel bakedmodel = ModelBakery.this.bakedCache.get(modelbakery$bakedcachekey);

            if (bakedmodel != null)
            {
                return bakedmodel;
            }
            else
            {
                UnbakedModel unbakedmodel = this.getModel(locIn);
                BakedModel bakedmodel1 = this.bakeUncached(unbakedmodel, stateIn, sprites);
                ModelBakery.this.bakedCache.put(modelbakery$bakedcachekey, bakedmodel1);
                return bakedmodel1;
            }
        }

        @Nullable
        BakedModel bakeUncached(UnbakedModel p_343761_, ModelState p_342939_)
        {
            return this.bakeUncached(p_343761_, p_342939_, this.modelTextureGetter);
        }

        BakedModel bakeUncached(UnbakedModel modelIn, ModelState stateIn, Function<Material, TextureAtlasSprite> sprites)
        {
            if (modelIn instanceof BlockModel blockmodel && blockmodel.getRootModel() == ModelBakery.GENERATION_MARKER)
            {
                return ModelBakery.ITEM_MODEL_GENERATOR.generateBlockModel(sprites, blockmodel).bake(this, blockmodel, sprites, stateIn, false);
            }

            return modelIn.bake(this, this.modelTextureGetter, stateIn);
        }
    }

    @FunctionalInterface
    public interface TextureGetter
    {
        TextureAtlasSprite get(ModelResourceLocation p_343839_, Material p_345409_);
    }
}
