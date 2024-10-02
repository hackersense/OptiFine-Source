package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;

public class LiquidBlockRenderer
{
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
    private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
    private TextureAtlasSprite waterOverlay;

    protected void setupSprites()
    {
        this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
        this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
        this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
        this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
        this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
    }

    private static boolean isNeighborSameFluid(FluidState p_203186_, FluidState p_203187_)
    {
        return p_203187_.getType().isSame(p_203186_.getType());
    }

    private static boolean isFaceOccludedByState(BlockGetter p_110979_, Direction p_110980_, float p_110981_, BlockPos p_110982_, BlockState p_110983_)
    {
        if (p_110983_.canOcclude())
        {
            VoxelShape voxelshape = Shapes.box(0.0, 0.0, 0.0, 1.0, (double)p_110981_, 1.0);
            VoxelShape voxelshape1 = p_110983_.getOcclusionShape(p_110979_, p_110982_);
            return Shapes.blockOccudes(voxelshape, voxelshape1, p_110980_);
        }
        else
        {
            return false;
        }
    }

    private static boolean isFaceOccludedByNeighbor(BlockGetter p_203180_, BlockPos p_203181_, Direction p_203182_, float p_203183_, BlockState p_203184_)
    {
        return isFaceOccludedByState(p_203180_, p_203182_, p_203183_, p_203181_.relative(p_203182_), p_203184_);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter p_110960_, BlockPos p_110961_, BlockState p_110962_, Direction p_110963_)
    {
        return isFaceOccludedByState(p_110960_, p_110963_.getOpposite(), 1.0F, p_110961_, p_110962_);
    }

    public static boolean shouldRenderFace(
        BlockAndTintGetter p_203167_, BlockPos p_203168_, FluidState p_203169_, BlockState p_203170_, Direction p_203171_, FluidState p_203172_
    )
    {
        return !isFaceOccludedBySelf(p_203167_, p_203168_, p_203170_, p_203171_) && !isNeighborSameFluid(p_203169_, p_203172_);
    }

    public void tesselate(BlockAndTintGetter p_234370_, BlockPos p_234371_, VertexConsumer p_234372_, BlockState p_234373_, FluidState p_234374_)
    {
        BlockState blockstate = p_234374_.createLegacyBlock();

        try
        {
            if (Config.isShaders())
            {
                SVertexBuilder.pushEntity(blockstate, p_234372_);
            }

            boolean flag = p_234374_.is(FluidTags.LAVA);
            TextureAtlasSprite[] atextureatlassprite = flag ? this.lavaIcons : this.waterIcons;

            if (Reflector.ForgeHooksClient_getFluidSprites.exists())
            {
                TextureAtlasSprite[] atextureatlassprite1 = (TextureAtlasSprite[])Reflector.call(
                            Reflector.ForgeHooksClient_getFluidSprites, p_234370_, p_234371_, p_234374_
                        );

                if (atextureatlassprite1 != null)
                {
                    atextureatlassprite = atextureatlassprite1;
                }
            }

            RenderEnv renderenv = p_234372_.getRenderEnv(blockstate, p_234371_);
            boolean flag1 = !flag && Minecraft.useAmbientOcclusion();
            int i = -1;
            float f = 1.0F;

            if (Reflector.ForgeHooksClient.exists())
            {
                i = IClientFluidTypeExtensions.of(p_234374_).getTintColor(p_234374_, p_234370_, p_234371_);
                f = (float)(i >> 24 & 0xFF) / 255.0F;
            }

            BlockState blockstate1 = p_234370_.getBlockState(p_234371_.relative(Direction.DOWN));
            FluidState fluidstate = blockstate1.getFluidState();
            BlockState blockstate2 = p_234370_.getBlockState(p_234371_.relative(Direction.UP));
            FluidState fluidstate1 = blockstate2.getFluidState();
            BlockState blockstate3 = p_234370_.getBlockState(p_234371_.relative(Direction.NORTH));
            FluidState fluidstate2 = blockstate3.getFluidState();
            BlockState blockstate4 = p_234370_.getBlockState(p_234371_.relative(Direction.SOUTH));
            FluidState fluidstate3 = blockstate4.getFluidState();
            BlockState blockstate5 = p_234370_.getBlockState(p_234371_.relative(Direction.WEST));
            FluidState fluidstate4 = blockstate5.getFluidState();
            BlockState blockstate6 = p_234370_.getBlockState(p_234371_.relative(Direction.EAST));
            FluidState fluidstate5 = blockstate6.getFluidState();
            boolean flag2 = !isNeighborSameFluid(p_234374_, fluidstate1);
            boolean flag3 = shouldRenderFace(p_234370_, p_234371_, p_234374_, p_234373_, Direction.DOWN, fluidstate)
                            && !isFaceOccludedByNeighbor(p_234370_, p_234371_, Direction.DOWN, 0.8888889F, blockstate1);
            boolean flag4 = shouldRenderFace(p_234370_, p_234371_, p_234374_, p_234373_, Direction.NORTH, fluidstate2);
            boolean flag5 = shouldRenderFace(p_234370_, p_234371_, p_234374_, p_234373_, Direction.SOUTH, fluidstate3);
            boolean flag6 = shouldRenderFace(p_234370_, p_234371_, p_234374_, p_234373_, Direction.WEST, fluidstate4);
            boolean flag7 = shouldRenderFace(p_234370_, p_234371_, p_234374_, p_234373_, Direction.EAST, fluidstate5);

            if (flag2 || flag3 || flag7 || flag6 || flag4 || flag5)
            {
                if (i < 0)
                {
                    i = CustomColors.getFluidColor(p_234370_, blockstate, p_234371_, renderenv);
                }

                float f1 = (float)(i >> 16 & 0xFF) / 255.0F;
                float f2 = (float)(i >> 8 & 0xFF) / 255.0F;
                float f3 = (float)(i & 0xFF) / 255.0F;
                float f4 = p_234370_.getShade(Direction.DOWN, true);
                float f5 = p_234370_.getShade(Direction.UP, true);
                float f6 = p_234370_.getShade(Direction.NORTH, true);
                float f7 = p_234370_.getShade(Direction.WEST, true);
                Fluid fluid = p_234374_.getType();
                float f8 = this.getHeight(p_234370_, fluid, p_234371_, p_234373_, p_234374_);
                float f9;
                float f10;
                float f11;
                float f12;

                if (f8 >= 1.0F)
                {
                    f9 = 1.0F;
                    f10 = 1.0F;
                    f11 = 1.0F;
                    f12 = 1.0F;
                }
                else
                {
                    float f13 = this.getHeight(p_234370_, fluid, p_234371_.north(), blockstate3, fluidstate2);
                    float f14 = this.getHeight(p_234370_, fluid, p_234371_.south(), blockstate4, fluidstate3);
                    float f15 = this.getHeight(p_234370_, fluid, p_234371_.east(), blockstate6, fluidstate5);
                    float f16 = this.getHeight(p_234370_, fluid, p_234371_.west(), blockstate5, fluidstate4);
                    f9 = this.calculateAverageHeight(p_234370_, fluid, f8, f13, f15, p_234371_.relative(Direction.NORTH).relative(Direction.EAST));
                    f10 = this.calculateAverageHeight(p_234370_, fluid, f8, f13, f16, p_234371_.relative(Direction.NORTH).relative(Direction.WEST));
                    f11 = this.calculateAverageHeight(p_234370_, fluid, f8, f14, f15, p_234371_.relative(Direction.SOUTH).relative(Direction.EAST));
                    f12 = this.calculateAverageHeight(p_234370_, fluid, f8, f14, f16, p_234371_.relative(Direction.SOUTH).relative(Direction.WEST));
                }

                float f24 = (float)(p_234371_.getX() & 15);
                float f25 = (float)(p_234371_.getY() & 15);
                float f26 = (float)(p_234371_.getZ() & 15);

                if (Config.isRenderRegions())
                {
                    int i5 = p_234371_.getX() >> 4 << 4;
                    int j = p_234371_.getY() >> 4 << 4;
                    int k = p_234371_.getZ() >> 4 << 4;
                    int l = 8;
                    int i1 = i5 >> l << l;
                    int j1 = k >> l << l;
                    int k1 = i5 - i1;
                    int l1 = k - j1;
                    f24 += (float)k1;
                    f25 += (float)j;
                    f26 += (float)l1;
                }

                if (Config.isShaders() && Shaders.useMidBlockAttrib)
                {
                    p_234372_.setMidBlock((float)((double)f24 + 0.5), (float)((double)f25 + 0.5), (float)((double)f26 + 0.5));
                }

                float f27 = 0.001F;
                float f28 = flag3 ? 0.001F : 0.0F;

                if (flag2 && !isFaceOccludedByNeighbor(p_234370_, p_234371_, Direction.UP, Math.min(Math.min(f10, f12), Math.min(f11, f9)), blockstate2))
                {
                    f10 -= 0.001F;
                    f12 -= 0.001F;
                    f11 -= 0.001F;
                    f9 -= 0.001F;
                    Vec3 vec3 = p_234374_.getFlow(p_234370_, p_234371_);
                    float f17;
                    float f18;
                    float f19;
                    float f30;
                    float f32;
                    float f34;
                    float f37;
                    float f41;

                    if (vec3.x == 0.0 && vec3.z == 0.0)
                    {
                        TextureAtlasSprite textureatlassprite1 = atextureatlassprite[0];
                        p_234372_.setSprite(textureatlassprite1);
                        f30 = textureatlassprite1.getU(0.0F);
                        f17 = textureatlassprite1.getV(0.0F);
                        f32 = f30;
                        f41 = textureatlassprite1.getV(1.0F);
                        f34 = textureatlassprite1.getU(1.0F);
                        f18 = f41;
                        f37 = f34;
                        f19 = f17;
                    }
                    else
                    {
                        TextureAtlasSprite textureatlassprite = atextureatlassprite[1];
                        p_234372_.setSprite(textureatlassprite);
                        float f20 = (float)Mth.atan2(vec3.z, vec3.x) - (float)(Math.PI / 2);
                        float f21 = Mth.sin(f20) * 0.25F;
                        float f22 = Mth.cos(f20) * 0.25F;
                        float f23 = 0.5F;
                        f30 = textureatlassprite.getU(0.5F + (-f22 - f21));
                        f17 = textureatlassprite.getV(0.5F + -f22 + f21);
                        f32 = textureatlassprite.getU(0.5F + -f22 + f21);
                        f41 = textureatlassprite.getV(0.5F + f22 + f21);
                        f34 = textureatlassprite.getU(0.5F + f22 + f21);
                        f18 = textureatlassprite.getV(0.5F + (f22 - f21));
                        f37 = textureatlassprite.getU(0.5F + (f22 - f21));
                        f19 = textureatlassprite.getV(0.5F + (-f22 - f21));
                    }

                    float f48 = (f30 + f32 + f34 + f37) / 4.0F;
                    float f49 = (f17 + f41 + f18 + f19) / 4.0F;
                    float f50 = atextureatlassprite[0].uvShrinkRatio();
                    f30 = Mth.lerp(f50, f30, f48);
                    f32 = Mth.lerp(f50, f32, f48);
                    f34 = Mth.lerp(f50, f34, f48);
                    f37 = Mth.lerp(f50, f37, f48);
                    f17 = Mth.lerp(f50, f17, f49);
                    f41 = Mth.lerp(f50, f41, f49);
                    f18 = Mth.lerp(f50, f18, f49);
                    f19 = Mth.lerp(f50, f19, f49);
                    int l5 = this.getLightColor(p_234370_, p_234371_);
                    int i2 = l5;
                    int j2 = l5;
                    int k2 = l5;
                    int l2 = l5;

                    if (flag1)
                    {
                        BlockPos blockpos = p_234371_.north();
                        BlockPos blockpos1 = p_234371_.south();
                        BlockPos blockpos2 = p_234371_.east();
                        BlockPos blockpos3 = p_234371_.west();
                        int i3 = this.getLightColor(p_234370_, blockpos);
                        int j3 = this.getLightColor(p_234370_, blockpos1);
                        int k3 = this.getLightColor(p_234370_, blockpos2);
                        int l3 = this.getLightColor(p_234370_, blockpos3);
                        int i4 = this.getLightColor(p_234370_, blockpos.west());
                        int j4 = this.getLightColor(p_234370_, blockpos1.west());
                        int k4 = this.getLightColor(p_234370_, blockpos1.east());
                        int l4 = this.getLightColor(p_234370_, blockpos.east());
                        i2 = ModelBlockRenderer.AmbientOcclusionFace.blend(i3, i4, l3, l5);
                        j2 = ModelBlockRenderer.AmbientOcclusionFace.blend(j3, j4, l3, l5);
                        k2 = ModelBlockRenderer.AmbientOcclusionFace.blend(j3, k4, k3, l5);
                        l2 = ModelBlockRenderer.AmbientOcclusionFace.blend(i3, l4, k3, l5);
                    }

                    float f56 = f5 * f1;
                    float f58 = f5 * f2;
                    float f60 = f5 * f3;
                    this.vertexVanilla(p_234372_, f24 + 0.0F, f25 + f10, f26 + 0.0F, f56, f58, f60, f30, f17, i2, f);
                    this.vertexVanilla(p_234372_, f24 + 0.0F, f25 + f12, f26 + 1.0F, f56, f58, f60, f32, f41, j2, f);
                    this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f11, f26 + 1.0F, f56, f58, f60, f34, f18, k2, f);
                    this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f9, f26 + 0.0F, f56, f58, f60, f37, f19, l2, f);

                    if (p_234374_.shouldRenderBackwardUpFace(p_234370_, p_234371_.above()))
                    {
                        this.vertexVanilla(p_234372_, f24 + 0.0F, f25 + f10, f26 + 0.0F, f56, f58, f60, f30, f17, i2, f);
                        this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f9, f26 + 0.0F, f56, f58, f60, f37, f19, l2, f);
                        this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f11, f26 + 1.0F, f56, f58, f60, f34, f18, k2, f);
                        this.vertexVanilla(p_234372_, f24 + 0.0F, f25 + f12, f26 + 1.0F, f56, f58, f60, f32, f41, j2, f);
                    }
                }

                if (flag3)
                {
                    p_234372_.setSprite(atextureatlassprite[0]);
                    float f29 = atextureatlassprite[0].getU0();
                    float f31 = atextureatlassprite[0].getU1();
                    float f33 = atextureatlassprite[0].getV0();
                    float f35 = atextureatlassprite[0].getV1();
                    int k5 = this.getLightColor(p_234370_, p_234371_.below());
                    float f39 = p_234370_.getShade(Direction.DOWN, true);
                    float f42 = f39 * f1;
                    float f44 = f39 * f2;
                    float f46 = f39 * f3;
                    this.vertexVanilla(p_234372_, f24, f25 + f28, f26 + 1.0F, f42, f44, f46, f29, f35, k5, f);
                    this.vertexVanilla(p_234372_, f24, f25 + f28, f26, f42, f44, f46, f29, f33, k5, f);
                    this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f28, f26, f42, f44, f46, f31, f33, k5, f);
                    this.vertexVanilla(p_234372_, f24 + 1.0F, f25 + f28, f26 + 1.0F, f42, f44, f46, f31, f35, k5, f);
                }

                int j5 = this.getLightColor(p_234370_, p_234371_);

                for (Direction direction : Direction.Plane.HORIZONTAL)
                {
                    float f36;
                    float f38;
                    float f40;
                    float f43;
                    float f45;
                    float f47;
                    boolean flag8;

                    switch (direction)
                    {
                        case NORTH:
                            f36 = f10;
                            f38 = f9;
                            f40 = f24;
                            f45 = f24 + 1.0F;
                            f43 = f26 + 0.001F;
                            f47 = f26 + 0.001F;
                            flag8 = flag4;
                            break;

                        case SOUTH:
                            f36 = f11;
                            f38 = f12;
                            f40 = f24 + 1.0F;
                            f45 = f24;
                            f43 = f26 + 1.0F - 0.001F;
                            f47 = f26 + 1.0F - 0.001F;
                            flag8 = flag5;
                            break;

                        case WEST:
                            f36 = f12;
                            f38 = f10;
                            f40 = f24 + 0.001F;
                            f45 = f24 + 0.001F;
                            f43 = f26 + 1.0F;
                            f47 = f26;
                            flag8 = flag6;
                            break;

                        default:
                            f36 = f9;
                            f38 = f11;
                            f40 = f24 + 1.0F - 0.001F;
                            f45 = f24 + 1.0F - 0.001F;
                            f43 = f26;
                            f47 = f26 + 1.0F;
                            flag8 = flag7;
                    }

                    if (flag8 && !isFaceOccludedByNeighbor(p_234370_, p_234371_, direction, Math.max(f36, f38), p_234370_.getBlockState(p_234371_.relative(direction))))
                    {
                        BlockPos blockpos4 = p_234371_.relative(direction);
                        TextureAtlasSprite textureatlassprite2 = atextureatlassprite[1];
                        float f51 = 0.0F;
                        float f52 = 0.0F;
                        boolean flag9 = !flag;

                        if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists())
                        {
                            flag9 = atextureatlassprite[2] != null;
                        }

                        if (flag9)
                        {
                            BlockState blockstate7 = p_234370_.getBlockState(blockpos4);
                            Block block = blockstate7.getBlock();
                            boolean flag10 = false;

                            if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists())
                            {
                                flag10 = Reflector.callBoolean(
                                             blockstate7, Reflector.IForgeBlockState_shouldDisplayFluidOverlay, p_234370_, blockpos4, p_234374_
                                         );
                            }

                            if (flag10 || block instanceof HalfTransparentBlock || block instanceof LeavesBlock || block == Blocks.BEACON)
                            {
                                textureatlassprite2 = this.waterOverlay;
                            }

                            if (block == Blocks.FARMLAND || block == Blocks.DIRT_PATH)
                            {
                                f51 = 0.9375F;
                                f52 = 0.9375F;
                            }

                            if (block instanceof SlabBlock slabblock && blockstate7.getValue(SlabBlock.TYPE) == SlabType.BOTTOM)
                            {
                                f51 = 0.5F;
                                f52 = 0.5F;
                            }
                        }

                        p_234372_.setSprite(textureatlassprite2);

                        if (!(f36 <= f51) || !(f38 <= f52))
                        {
                            f51 = Math.min(f51, f36);
                            f52 = Math.min(f52, f38);

                            if (f51 > f27)
                            {
                                f51 -= f27;
                            }

                            if (f52 > f27)
                            {
                                f52 -= f27;
                            }

                            float f53 = textureatlassprite2.getV((1.0F - f51) * 0.5F);
                            float f54 = textureatlassprite2.getV((1.0F - f52) * 0.5F);
                            float f55 = textureatlassprite2.getU(0.0F);
                            float f57 = textureatlassprite2.getU(0.5F);
                            float f59 = textureatlassprite2.getV((1.0F - f36) * 0.5F);
                            float f61 = textureatlassprite2.getV((1.0F - f38) * 0.5F);
                            float f62 = textureatlassprite2.getV(0.5F);
                            float f63 = direction != Direction.NORTH && direction != Direction.SOUTH
                                        ? p_234370_.getShade(Direction.WEST, true)
                                        : p_234370_.getShade(Direction.NORTH, true);
                            float f64 = f5 * f63 * f1;
                            float f65 = f5 * f63 * f2;
                            float f66 = f5 * f63 * f3;
                            this.vertexVanilla(p_234372_, f40, f25 + f36, f43, f64, f65, f66, f55, f59, j5, f);
                            this.vertexVanilla(p_234372_, f45, f25 + f38, f47, f64, f65, f66, f57, f61, j5, f);
                            this.vertexVanilla(p_234372_, f45, f25 + f28, f47, f64, f65, f66, f57, f54, j5, f);
                            this.vertexVanilla(p_234372_, f40, f25 + f28, f43, f64, f65, f66, f55, f53, j5, f);

                            if (textureatlassprite2 != this.waterOverlay)
                            {
                                this.vertexVanilla(p_234372_, f40, f25 + f28, f43, f64, f65, f66, f55, f53, j5, f);
                                this.vertexVanilla(p_234372_, f45, f25 + f28, f47, f64, f65, f66, f57, f54, j5, f);
                                this.vertexVanilla(p_234372_, f45, f25 + f38, f47, f64, f65, f66, f57, f61, j5, f);
                                this.vertexVanilla(p_234372_, f40, f25 + f36, f43, f64, f65, f66, f55, f59, j5, f);
                            }
                        }
                    }
                }

                p_234372_.setSprite(null);
            }
        }
        finally
        {
            if (Config.isShaders())
            {
                SVertexBuilder.popEntity(p_234372_);
            }
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter p_203150_, Fluid p_203151_, float p_203152_, float p_203153_, float p_203154_, BlockPos p_203155_)
    {
        if (!(p_203154_ >= 1.0F) && !(p_203153_ >= 1.0F))
        {
            float[] afloat = new float[2];

            if (p_203154_ > 0.0F || p_203153_ > 0.0F)
            {
                float f = this.getHeight(p_203150_, p_203151_, p_203155_);

                if (f >= 1.0F)
                {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, p_203152_);
            this.addWeightedHeight(afloat, p_203154_);
            this.addWeightedHeight(afloat, p_203153_);
            return afloat[0] / afloat[1];
        }
        else
        {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] p_203189_, float p_203190_)
    {
        if (p_203190_ >= 0.8F)
        {
            p_203189_[0] += p_203190_ * 10.0F;
            p_203189_[1] += 10.0F;
        }
        else if (p_203190_ >= 0.0F)
        {
            p_203189_[0] += p_203190_;
            p_203189_[1]++;
        }
    }

    private float getHeight(BlockAndTintGetter p_203157_, Fluid p_203158_, BlockPos p_203159_)
    {
        BlockState blockstate = p_203157_.getBlockState(p_203159_);
        return this.getHeight(p_203157_, p_203158_, p_203159_, blockstate, blockstate.getFluidState());
    }

    private float getHeight(BlockAndTintGetter p_203161_, Fluid p_203162_, BlockPos p_203163_, BlockState p_203164_, FluidState p_203165_)
    {
        if (p_203162_.isSame(p_203165_.getType()))
        {
            BlockState blockstate = p_203161_.getBlockState(p_203163_.above());
            return p_203162_.isSame(blockstate.getFluidState().getType()) ? 1.0F : p_203165_.getOwnHeight();
        }
        else
        {
            return !p_203164_.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(
        VertexConsumer p_110985_,
        float p_110989_,
        float p_110990_,
        float p_110991_,
        float p_110992_,
        float p_110993_,
        float p_343128_,
        float p_344448_,
        float p_344284_,
        int p_110994_
    )
    {
        p_110985_.addVertex(p_110989_, p_110990_, p_110991_)
        .setColor(p_110992_, p_110993_, p_343128_, 1.0F)
        .setUv(p_344448_, p_344284_)
        .setLight(p_110994_)
        .setNormal(0.0F, 1.0F, 0.0F);
    }

    private void vertexVanilla(
        VertexConsumer buffer, float x, float y, float z, float red, float green, float blue, float u, float v, int combinedLight, float alpha
    )
    {
        buffer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(combinedLight).setNormal(0.0F, 1.0F, 0.0F);
    }

    private int getLightColor(BlockAndTintGetter p_110946_, BlockPos p_110947_)
    {
        int i = LevelRenderer.getLightColor(p_110946_, p_110947_);
        int j = LevelRenderer.getLightColor(p_110946_, p_110947_.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }
}
