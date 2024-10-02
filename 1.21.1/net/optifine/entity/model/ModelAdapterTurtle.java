package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.TurtleRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Turtle;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterTurtle extends ModelAdapterQuadruped
{
    public ModelAdapterTurtle()
    {
        super(EntityType.TURTLE, "turtle", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new TurtleModel(bakeModelLayer(ModelLayers.TURTLE));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof QuadrupedModel))
        {
            return null;
        }
        else
        {
            TurtleModel turtlemodel = (TurtleModel)model;
            return modelPart.equals("body2") ? (ModelPart)Reflector.ModelTurtle_body2.getValue(turtlemodel) : super.getModelRenderer(model, modelPart);
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        String[] astring = super.getModelRendererNames();
        return (String[])Config.addObjectToArray(astring, "body2");
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        TurtleRenderer turtlerenderer = new TurtleRenderer(entityrenderdispatcher.getContext());
        turtlerenderer.model = (TurtleModel<Turtle>)modelBase;
        turtlerenderer.shadowRadius = shadowSize;
        return turtlerenderer;
    }
}
