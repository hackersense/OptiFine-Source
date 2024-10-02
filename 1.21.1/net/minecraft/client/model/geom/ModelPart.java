package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.RandomEntities;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.entity.model.anim.ModelUpdater;
import net.optifine.model.Attachment;
import net.optifine.model.AttachmentPath;
import net.optifine.model.AttachmentPaths;
import net.optifine.model.AttachmentType;
import net.optifine.model.ModelSprite;
import net.optifine.render.BoxVertexPositions;
import net.optifine.render.RenderPositions;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ModelPart
{
    public static final float DEFAULT_SCALE = 1.0F;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    public final List<ModelPart.Cube> cubes;
    public final Map<String, ModelPart> children;
    private String name;
    public List<ModelPart> childModelsList;
    public List<ModelSprite> spriteList = new ArrayList<>();
    public boolean mirrorV = false;
    private ResourceLocation textureLocation = null;
    private String id = null;
    private ModelUpdater modelUpdater;
    private LevelRenderer renderGlobal = Config.getRenderGlobal();
    private boolean custom;
    private Attachment[] attachments;
    private AttachmentPaths attachmentPaths;
    private boolean attachmentPathsChecked;
    private ModelPart parent;
    public float textureWidth = 64.0F;
    public float textureHeight = 32.0F;
    public float textureOffsetX;
    public float textureOffsetY;
    public boolean mirror;
    public static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);
    private PartPose initialPose = PartPose.ZERO;

    public ModelPart setTextureOffset(float x, float y)
    {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
        return this;
    }

    public ModelPart setTextureSize(int textureWidthIn, int textureHeightIn)
    {
        this.textureWidth = (float)textureWidthIn;
        this.textureHeight = (float)textureHeightIn;
        return this;
    }

    public ModelPart(List<ModelPart.Cube> p_171306_, Map<String, ModelPart> p_171307_)
    {
        if (p_171306_ instanceof ImmutableList)
        {
            p_171306_ = new ArrayList<>(p_171306_);
        }

        this.cubes = p_171306_;
        this.children = p_171307_;
        this.childModelsList = new ArrayList<>(this.children.values());

        for (ModelPart modelpart : this.childModelsList)
        {
            modelpart.setParent(this);
        }
    }

    public PartPose storePose()
    {
        return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
    }

    public PartPose getInitialPose()
    {
        return this.initialPose;
    }

    public void setInitialPose(PartPose p_233561_)
    {
        this.initialPose = p_233561_;
    }

    public void resetPose()
    {
        this.loadPose(this.initialPose);
    }

    public void loadPose(PartPose p_171323_)
    {
        if (!this.custom)
        {
            this.x = p_171323_.x;
            this.y = p_171323_.y;
            this.z = p_171323_.z;
            this.xRot = p_171323_.xRot;
            this.yRot = p_171323_.yRot;
            this.zRot = p_171323_.zRot;
            this.xScale = 1.0F;
            this.yScale = 1.0F;
            this.zScale = 1.0F;
        }
    }

    public void copyFrom(ModelPart p_104316_)
    {
        this.xScale = p_104316_.xScale;
        this.yScale = p_104316_.yScale;
        this.zScale = p_104316_.zScale;
        this.xRot = p_104316_.xRot;
        this.yRot = p_104316_.yRot;
        this.zRot = p_104316_.zRot;
        this.x = p_104316_.x;
        this.y = p_104316_.y;
        this.z = p_104316_.z;
    }

    public boolean hasChild(String p_233563_)
    {
        return this.children.containsKey(p_233563_);
    }

    public ModelPart getChild(String p_171325_)
    {
        ModelPart modelpart = this.children.get(p_171325_);

        if (modelpart == null)
        {
            throw new NoSuchElementException("Can't find part " + p_171325_);
        }
        else
        {
            return modelpart;
        }
    }

    public void setPos(float p_104228_, float p_104229_, float p_104230_)
    {
        this.x = p_104228_;
        this.y = p_104229_;
        this.z = p_104230_;
    }

    public void setRotation(float p_171328_, float p_171329_, float p_171330_)
    {
        this.xRot = p_171328_;
        this.yRot = p_171329_;
        this.zRot = p_171330_;
    }

    public void render(PoseStack p_104302_, VertexConsumer p_104303_, int p_104304_, int p_104305_)
    {
        this.render(p_104302_, p_104303_, p_104304_, p_104305_, -1);
    }

    public void render(PoseStack p_104307_, VertexConsumer p_104308_, int p_104309_, int p_104310_, int p_343158_)
    {
        this.render(p_104307_, p_104308_, p_104309_, p_104310_, p_343158_, true);
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int colorIn, boolean updateModel)
    {
        if (this.visible && (!this.cubes.isEmpty() || !this.children.isEmpty() || !this.spriteList.isEmpty()))
        {
            RenderType rendertype = null;
            BufferBuilder bufferbuilder = null;
            MultiBufferSource.BufferSource multibuffersource$buffersource = null;

            if (this.textureLocation != null)
            {
                if (this.renderGlobal.renderOverlayEyes)
                {
                    return;
                }

                multibuffersource$buffersource = bufferIn.getRenderTypeBuffer();

                if (multibuffersource$buffersource != null)
                {
                    VertexConsumer vertexconsumer = bufferIn.getSecondaryBuilder();
                    rendertype = multibuffersource$buffersource.getLastRenderType();
                    bufferbuilder = multibuffersource$buffersource.getStartedBuffer(rendertype);
                    bufferIn = multibuffersource$buffersource.getBuffer(this.textureLocation, bufferIn);

                    if (vertexconsumer != null)
                    {
                        bufferIn = VertexMultiConsumer.create(vertexconsumer, bufferIn);
                    }
                }
            }

            if (updateModel && CustomEntityModels.isActive())
            {
                this.updateModel();
            }

            matrixStackIn.pushPose();
            this.translateAndRotate(matrixStackIn);

            if (!this.skipDraw)
            {
                this.compile(matrixStackIn.last(), bufferIn, packedLightIn, packedOverlayIn, colorIn);
            }

            int j = this.childModelsList.size();

            for (int i = 0; i < j; i++)
            {
                ModelPart modelpart = this.childModelsList.get(i);
                modelpart.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, colorIn, false);
            }

            int k = this.spriteList.size();

            for (int l = 0; l < k; l++)
            {
                ModelSprite modelsprite = this.spriteList.get(l);
                modelsprite.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, colorIn);
            }

            matrixStackIn.popPose();

            if (multibuffersource$buffersource != null)
            {
                multibuffersource$buffersource.restoreRenderState(rendertype, bufferbuilder);
            }
        }
    }

    public void visit(PoseStack p_171310_, ModelPart.Visitor p_171311_)
    {
        this.visit(p_171310_, p_171311_, "");
    }

    private void visit(PoseStack p_171313_, ModelPart.Visitor p_171314_, String p_171315_)
    {
        if (!this.cubes.isEmpty() || !this.children.isEmpty())
        {
            p_171313_.pushPose();
            this.translateAndRotate(p_171313_);
            PoseStack.Pose posestack$pose = p_171313_.last();

            for (int i = 0; i < this.cubes.size(); i++)
            {
                p_171314_.visit(posestack$pose, p_171315_, i, this.cubes.get(i));
            }

            String s = p_171315_ + "/";
            this.children.forEach((nameIn, partIn) -> partIn.visit(p_171313_, p_171314_, s + nameIn));
            p_171313_.popPose();
        }
    }

    public void translateAndRotate(PoseStack p_104300_)
    {
        p_104300_.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);

        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F)
        {
            p_104300_.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F)
        {
            p_104300_.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    private void compile(PoseStack.Pose p_104291_, VertexConsumer p_104292_, int p_104293_, int p_104294_, int p_343687_)
    {
        boolean flag = Config.isShaders() && Shaders.useVelocityAttrib && Config.isMinecraftThread();
        int i = this.cubes.size();

        for (int j = 0; j < i; j++)
        {
            ModelPart.Cube modelpart$cube = this.cubes.get(j);
            VertexPosition[][] avertexposition = null;

            if (flag)
            {
                IRandomEntity irandomentity = RandomEntities.getRandomEntityRendered();

                if (irandomentity != null)
                {
                    avertexposition = modelpart$cube.getBoxVertexPositions(irandomentity.getId());
                }
            }

            modelpart$cube.compile(p_104291_, p_104292_, p_104293_, p_104294_, p_343687_, avertexposition);
        }
    }

    public ModelPart.Cube getRandomCube(RandomSource p_233559_)
    {
        return this.cubes.get(p_233559_.nextInt(this.cubes.size()));
    }

    public boolean isEmpty()
    {
        return this.cubes.isEmpty();
    }

    public void offsetPos(Vector3f p_253873_)
    {
        this.x = this.x + p_253873_.x();
        this.y = this.y + p_253873_.y();
        this.z = this.z + p_253873_.z();
    }

    public void offsetRotation(Vector3f p_253983_)
    {
        this.xRot = this.xRot + p_253983_.x();
        this.yRot = this.yRot + p_253983_.y();
        this.zRot = this.zRot + p_253983_.z();
    }

    public void offsetScale(Vector3f p_253957_)
    {
        this.xScale = this.xScale + p_253957_.x();
        this.yScale = this.yScale + p_253957_.y();
        this.zScale = this.zScale + p_253957_.z();
    }

    public Stream<ModelPart> getAllParts()
    {
        return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
    }

    public void addSprite(float posX, float posY, float posZ, int sizeX, int sizeY, int sizeZ, float sizeAdd)
    {
        this.spriteList.add(new ModelSprite(this, this.textureOffsetX, this.textureOffsetY, posX, posY, posZ, sizeX, sizeY, sizeZ, sizeAdd));
    }

    public ResourceLocation getTextureLocation()
    {
        return this.textureLocation;
    }

    public void setTextureLocation(ResourceLocation textureLocation)
    {
        this.textureLocation = textureLocation;
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addBox(float[][] faceUvs, float x, float y, float z, float dx, float dy, float dz, float delta)
    {
        this.cubes.add(new ModelPart.Cube(faceUvs, x, y, z, dx, dy, dz, delta, delta, delta, this.mirror, this.textureWidth, this.textureHeight));
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float delta)
    {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, delta, delta, delta, this.mirror, false);
    }

    private void addBox(
        float texOffX,
        float texOffY,
        float x,
        float y,
        float z,
        float width,
        float height,
        float depth,
        float deltaX,
        float deltaY,
        float deltaZ,
        boolean mirror,
        boolean dummyIn
    )
    {
        this.cubes
        .add(
            new ModelPart.Cube(
                texOffX, texOffY, x, y, z, width, height, depth, deltaX, deltaY, deltaZ, mirror, this.textureWidth, this.textureHeight, ALL_VISIBLE
            )
        );
    }

    public ModelPart getChildModelDeep(String name)
    {
        if (name == null)
        {
            return null;
        }
        else if (this.children.containsKey(name))
        {
            return this.getChild(name);
        }
        else
        {
            if (this.children != null)
            {
                for (String s : this.children.keySet())
                {
                    ModelPart modelpart = this.children.get(s);
                    ModelPart modelpart1 = modelpart.getChildModelDeep(name);

                    if (modelpart1 != null)
                    {
                        return modelpart1;
                    }
                }
            }

            return null;
        }
    }

    public ModelPart getChildById(String id)
    {
        if (id == null)
        {
            return null;
        }
        else
        {
            if (this.children != null)
            {
                for (String s : this.children.keySet())
                {
                    ModelPart modelpart = this.children.get(s);

                    if (id.equals(modelpart.getId()))
                    {
                        return modelpart;
                    }
                }
            }

            return null;
        }
    }

    public ModelPart getChildDeepById(String id)
    {
        if (id == null)
        {
            return null;
        }
        else
        {
            ModelPart modelpart = this.getChildById(id);

            if (modelpart != null)
            {
                return modelpart;
            }
            else
            {
                if (this.children != null)
                {
                    for (String s : this.children.keySet())
                    {
                        ModelPart modelpart1 = this.children.get(s);
                        ModelPart modelpart2 = modelpart1.getChildDeepById(id);

                        if (modelpart2 != null)
                        {
                            return modelpart2;
                        }
                    }
                }

                return null;
            }
        }
    }

    public ModelUpdater getModelUpdater()
    {
        return this.modelUpdater;
    }

    public void setModelUpdater(ModelUpdater modelUpdater)
    {
        this.modelUpdater = modelUpdater;
    }

    public void addChildModel(String name, ModelPart part)
    {
        if (part != null)
        {
            this.children.put(name, part);
            this.childModelsList = new ArrayList<>(this.children.values());
            part.setParent(this);

            if (part.getName() == null)
            {
                part.setName(name);
            }
        }
    }

    public String getUniqueChildModelName(String name)
    {
        String s = name;

        for (int i = 2; this.children.containsKey(name); i++)
        {
            name = s + "-" + i;
        }

        return name;
    }

    private void updateModel()
    {
        if (this.modelUpdater != null)
        {
            this.modelUpdater.update();
        }
        else
        {
            int i = this.childModelsList.size();

            for (int j = 0; j < i; j++)
            {
                ModelPart modelpart = this.childModelsList.get(j);
                modelpart.updateModel();
            }
        }
    }

    public boolean isCustom()
    {
        return this.custom;
    }

    public void setCustom(boolean custom)
    {
        this.custom = custom;
    }

    public ModelPart getParent()
    {
        return this.parent;
    }

    public void setParent(ModelPart parent)
    {
        this.parent = parent;
    }

    public Attachment[] getAttachments()
    {
        return this.attachments;
    }

    public void setAttachments(Attachment[] attachments)
    {
        this.attachments = attachments;
    }

    public boolean applyAttachmentTransform(AttachmentType typeIn, PoseStack matrixStackIn)
    {
        if (this.attachmentPathsChecked && this.attachmentPaths == null)
        {
            return false;
        }
        else
        {
            AttachmentPath attachmentpath = this.getAttachmentPath(typeIn);

            if (attachmentpath == null)
            {
                return false;
            }
            else
            {
                attachmentpath.applyTransform(matrixStackIn);
                return true;
            }
        }
    }

    private AttachmentPath getAttachmentPath(AttachmentType typeIn)
    {
        if (!this.attachmentPathsChecked)
        {
            this.attachmentPathsChecked = true;
            this.attachmentPaths = new AttachmentPaths();
            this.collectAttachmentPaths(new ArrayList<>(), this.attachmentPaths);

            if (this.attachmentPaths.isEmpty())
            {
                this.attachmentPaths = null;
            }
        }

        return this.attachmentPaths == null ? null : this.attachmentPaths.getVisiblePath(typeIn);
    }

    private void collectAttachmentPaths(List<ModelPart> parents, AttachmentPaths paths)
    {
        parents.add(this);

        if (this.attachments != null)
        {
            paths.addPaths(parents, this.attachments);
        }

        for (ModelPart modelpart : this.childModelsList)
        {
            modelpart.collectAttachmentPaths(parents, paths);
        }

        parents.remove(parents.size() - 1);
    }

    @Override
    public String toString()
    {
        return "name: "
               + this.name
               + ", id: "
               + this.id
               + ", boxes: "
               + (this.cubes != null ? this.cubes.size() : null)
               + ", submodels: "
               + (this.children != null ? this.children.size() : null)
               + ", custom: "
               + this.custom;
    }

    public static class Cube
    {
        private final ModelPart.Polygon[] polygons;
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;
        private BoxVertexPositions boxVertexPositions;
        private RenderPositions[] renderPositions;

        public Cube(
            int p_273701_,
            int p_273034_,
            float p_272824_,
            float p_273777_,
            float p_273748_,
            float p_273722_,
            float p_273763_,
            float p_272823_,
            float p_272945_,
            float p_272790_,
            float p_272870_,
            boolean p_273589_,
            float p_273591_,
            float p_273313_,
            Set<Direction> p_273291_
        )
        {
            this(
                (float)p_273701_,
                (float)p_273034_,
                p_272824_,
                p_273777_,
                p_273748_,
                p_273722_,
                p_273763_,
                p_272823_,
                p_272945_,
                p_272790_,
                p_272870_,
                p_273589_,
                p_273591_,
                p_273313_,
                p_273291_
            );
        }

        public Cube(
            float texOffX,
            float texOffY,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float deltaX,
            float deltaY,
            float deltaZ,
            boolean mirror,
            float texWidth,
            float texHeight,
            Set<Direction> directionsIn
        )
        {
            this.minX = x;
            this.minY = y;
            this.minZ = z;
            this.maxX = x + width;
            this.maxY = y + height;
            this.maxZ = z + depth;
            this.polygons = new ModelPart.Polygon[directionsIn.size()];
            float f = x + width;
            float f1 = y + height;
            float f2 = z + depth;
            x -= deltaX;
            y -= deltaY;
            z -= deltaZ;
            f += deltaX;
            f1 += deltaY;
            f2 += deltaZ;

            if (mirror)
            {
                float f3 = f;
                f = x;
                x = f3;
            }

            ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, z, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(x, f1, z, 8.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(x, y, f2, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, y, f2, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(x, f1, f2, 8.0F, 0.0F);
            float f4 = texOffX + depth;
            float f5 = texOffX + depth + width;
            float f6 = texOffX + depth + width + width;
            float f7 = texOffX + depth + width + depth;
            float f8 = texOffX + depth + width + depth + width;
            float f9 = texOffY + depth;
            float f10 = texOffY + depth + height;
            int i = 0;

            if (directionsIn.contains(Direction.DOWN))
            {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
                    f4,
                    texOffY,
                    f5,
                    f9,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.DOWN
                );
            }

            if (directionsIn.contains(Direction.UP))
            {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
                    f5,
                    f9,
                    f6,
                    texOffY,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.UP
                );
            }

            if (directionsIn.contains(Direction.WEST))
            {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
                    texOffX,
                    f9,
                    f4,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.WEST
                );
            }

            if (directionsIn.contains(Direction.NORTH))
            {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
                    f4,
                    f9,
                    f5,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.NORTH
                );
            }

            if (directionsIn.contains(Direction.EAST))
            {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
                    f5,
                    f9,
                    f7,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.EAST
                );
            }

            if (directionsIn.contains(Direction.SOUTH))
            {
                this.polygons[i] = new ModelPart.Polygon(
                    new ModelPart.Vertex[] {modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
                    f7,
                    f9,
                    f8,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.SOUTH
                );
            }

            this.renderPositions = collectRenderPositions(this.polygons);
        }

        public Cube(
            float[][] faceUvs,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float deltaX,
            float deltaY,
            float deltaZ,
            boolean mirorIn,
            float texWidth,
            float texHeight
        )
        {
            this.minX = x;
            this.minY = y;
            this.minZ = z;
            this.maxX = x + width;
            this.maxY = y + height;
            this.maxZ = z + depth;
            this.polygons = new ModelPart.Polygon[6];
            float f = x + width;
            float f1 = y + height;
            float f2 = z + depth;
            x -= deltaX;
            y -= deltaY;
            z -= deltaZ;
            f += deltaX;
            f1 += deltaY;
            f2 += deltaZ;

            if (mirorIn)
            {
                float f3 = f;
                f = x;
                x = f3;
            }

            ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, z, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(x, f1, z, 8.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(x, y, f2, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, y, f2, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(x, f1, f2, 8.0F, 0.0F);
            this.polygons[2] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
                                    faceUvs[1],
                                    true,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.DOWN
                                );
            this.polygons[3] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
                                    faceUvs[0],
                                    true,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.UP
                                );
            this.polygons[1] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
                                    faceUvs[5],
                                    false,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.WEST
                                );
            this.polygons[4] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
                                    faceUvs[2],
                                    false,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.NORTH
                                );
            this.polygons[0] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
                                    faceUvs[4],
                                    false,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.EAST
                                );
            this.polygons[5] = this.makeTexturedQuad(
                                    new ModelPart.Vertex[] {modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
                                    faceUvs[3],
                                    false,
                                    texWidth,
                                    texHeight,
                                    mirorIn,
                                    Direction.SOUTH
                                );
            this.renderPositions = collectRenderPositions(this.polygons);
        }

        private static RenderPositions[] collectRenderPositions(ModelPart.Polygon[] quads)
        {
            Map<Vector3f, RenderPositions> map = new LinkedHashMap<>();

            for (int i = 0; i < quads.length; i++)
            {
                ModelPart.Polygon modelpart$polygon = quads[i];

                if (modelpart$polygon != null)
                {
                    for (int j = 0; j < modelpart$polygon.vertices.length; j++)
                    {
                        ModelPart.Vertex modelpart$vertex = modelpart$polygon.vertices[j];
                        RenderPositions renderpositions = map.get(modelpart$vertex.pos);

                        if (renderpositions == null)
                        {
                            renderpositions = new RenderPositions(modelpart$vertex.pos);
                            map.put(modelpart$vertex.pos, renderpositions);
                        }

                        modelpart$vertex.renderPositions = renderpositions;
                    }
                }
            }

            return map.values().toArray(new RenderPositions[map.size()]);
        }

        private ModelPart.Polygon makeTexturedQuad(
            ModelPart.Vertex[] positionTextureVertexs,
            float[] faceUvs,
            boolean reverseUV,
            float textureWidth,
            float textureHeight,
            boolean mirrorIn,
            Direction directionIn
        )
        {
            if (faceUvs == null)
            {
                return null;
            }
            else
            {
                return reverseUV
                       ? new ModelPart.Polygon(
                           positionTextureVertexs, faceUvs[2], faceUvs[3], faceUvs[0], faceUvs[1], textureWidth, textureHeight, mirrorIn, directionIn
                       )
                       : new ModelPart.Polygon(
                           positionTextureVertexs, faceUvs[0], faceUvs[1], faceUvs[2], faceUvs[3], textureWidth, textureHeight, mirrorIn, directionIn
                       );
            }
        }

        public VertexPosition[][] getBoxVertexPositions(int key)
        {
            if (this.boxVertexPositions == null)
            {
                this.boxVertexPositions = new BoxVertexPositions();
            }

            return this.boxVertexPositions.get(key);
        }

        public void compile(PoseStack.Pose p_171333_, VertexConsumer p_171334_, int p_171335_, int p_171336_, int p_344599_)
        {
            this.compile(p_171333_, p_171334_, p_171335_, p_171336_, p_344599_, null);
        }

        public void compile(
            PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int colorIn, VertexPosition[][] boxPos
        )
        {
            Matrix4f matrix4f = matrixEntryIn.pose();
            Vector3f vector3f = bufferIn.getTempVec3f();

            for (RenderPositions renderpositions : this.renderPositions)
            {
                MathUtils.transform(matrix4f, renderpositions.getPositionDiv16(), renderpositions.getPositionRender());
            }

            boolean flag = bufferIn.canAddVertexFast();
            int i = this.polygons.length;

            for (int j = 0; j < i; j++)
            {
                ModelPart.Polygon modelpart$polygon = this.polygons[j];

                if (modelpart$polygon != null)
                {
                    if (boxPos != null)
                    {
                        bufferIn.setQuadVertexPositions(boxPos[j]);
                    }

                    Vector3f vector3f1 = matrixEntryIn.transformNormal(modelpart$polygon.normal, vector3f);
                    float f = vector3f1.x();
                    float f1 = vector3f1.y();
                    float f2 = vector3f1.z();

                    if (flag)
                    {
                        int k = colorIn;
                        byte b0 = BufferBuilder.normalIntValue(f);
                        byte b1 = BufferBuilder.normalIntValue(f1);
                        byte b2 = BufferBuilder.normalIntValue(f2);
                        int l = (b2 & 255) << 16 | (b1 & 255) << 8 | b0 & 255;

                        for (ModelPart.Vertex modelpart$vertex1 : modelpart$polygon.vertices)
                        {
                            Vector3f vector3f3 = modelpart$vertex1.renderPositions.getPositionRender();
                            bufferIn.addVertexFast(
                                vector3f3.x,
                                vector3f3.y,
                                vector3f3.z,
                                k,
                                modelpart$vertex1.u,
                                modelpart$vertex1.v,
                                packedOverlayIn,
                                packedLightIn,
                                l
                            );
                        }
                    }
                    else
                    {
                        for (ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices)
                        {
                            Vector3f vector3f2 = modelpart$vertex.renderPositions.getPositionRender();
                            bufferIn.addVertex(
                                vector3f2.x,
                                vector3f2.y,
                                vector3f2.z,
                                colorIn,
                                modelpart$vertex.u,
                                modelpart$vertex.v,
                                packedOverlayIn,
                                packedLightIn,
                                f,
                                f1,
                                f2
                            );
                        }
                    }
                }
            }
        }
    }

    static class Polygon
    {
        public final ModelPart.Vertex[] vertices;
        public final Vector3f normal;

        public Polygon(
            ModelPart.Vertex[] p_104362_,
            float p_104363_,
            float p_104364_,
            float p_104365_,
            float p_104366_,
            float p_104367_,
            float p_104368_,
            boolean p_104369_,
            Direction p_104370_
        )
        {
            this.vertices = p_104362_;
            float f = 0.0F / p_104367_;
            float f1 = 0.0F / p_104368_;

            if (Config.isAntialiasing())
            {
                f = 0.05F / p_104367_;
                f1 = 0.05F / p_104368_;

                if (p_104365_ < p_104363_)
                {
                    f = -f;
                }

                if (p_104366_ < p_104364_)
                {
                    f1 = -f1;
                }
            }

            p_104362_[0] = p_104362_[0].remap(p_104365_ / p_104367_ - f, p_104364_ / p_104368_ + f1);
            p_104362_[1] = p_104362_[1].remap(p_104363_ / p_104367_ + f, p_104364_ / p_104368_ + f1);
            p_104362_[2] = p_104362_[2].remap(p_104363_ / p_104367_ + f, p_104366_ / p_104368_ - f1);
            p_104362_[3] = p_104362_[3].remap(p_104365_ / p_104367_ - f, p_104366_ / p_104368_ - f1);

            if (p_104369_)
            {
                int i = p_104362_.length;

                for (int j = 0; j < i / 2; j++)
                {
                    ModelPart.Vertex modelpart$vertex = p_104362_[j];
                    p_104362_[j] = p_104362_[i - 1 - j];
                    p_104362_[i - 1 - j] = modelpart$vertex;
                }
            }

            this.normal = p_104370_.step();

            if (p_104369_)
            {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
        }
    }

    static class Vertex
    {
        public final Vector3f pos;
        public final float u;
        public final float v;
        public RenderPositions renderPositions;

        public Vertex(float p_104375_, float p_104376_, float p_104377_, float p_104378_, float p_104379_)
        {
            this(new Vector3f(p_104375_, p_104376_, p_104377_), p_104378_, p_104379_);
        }

        public ModelPart.Vertex remap(float p_104385_, float p_104386_)
        {
            return new ModelPart.Vertex(this.pos, p_104385_, p_104386_);
        }

        public Vertex(Vector3f p_253667_, float p_253662_, float p_254308_)
        {
            this.pos = p_253667_;
            this.u = p_253662_;
            this.v = p_254308_;
        }
    }

    @FunctionalInterface
    public interface Visitor
    {
        void visit(PoseStack.Pose p_171342_, String p_171343_, int p_171344_, ModelPart.Cube p_171345_);
    }
}
