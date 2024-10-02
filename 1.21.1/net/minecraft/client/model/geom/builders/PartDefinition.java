package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public class PartDefinition
{
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();
    private String name;

    PartDefinition(List<CubeDefinition> p_171581_, PartPose p_171582_)
    {
        this.cubes = p_171581_;
        this.partPose = p_171582_;
    }

    public PartDefinition addOrReplaceChild(String p_171600_, CubeListBuilder p_171601_, PartPose p_171602_)
    {
        PartDefinition partdefinition = new PartDefinition(p_171601_.getCubes(), p_171602_);
        partdefinition.setName(p_171600_);
        PartDefinition partdefinition1 = this.children.put(p_171600_, partdefinition);

        if (partdefinition1 != null)
        {
            partdefinition.children.putAll(partdefinition1.children);
        }

        return partdefinition;
    }

    public ModelPart bake(int p_171584_, int p_171585_)
    {
        Object2ObjectArrayMap<String, ModelPart> object2objectarraymap = this.children
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        p_171590_2_ -> p_171590_2_.getValue().bake(p_171584_, p_171585_),
                        (p_171594_0_, p_171594_1_) -> p_171594_0_,
                        Object2ObjectArrayMap::new
                    )
                );
        List<ModelPart.Cube> list = this.cubes
                                    .stream()
                                    .map(p_171586_2_ -> p_171586_2_.bake(p_171584_, p_171585_))
                                    .collect(ImmutableList.toImmutableList());
        ModelPart modelpart = new ModelPart(list, object2objectarraymap);
        modelpart.setInitialPose(this.partPose);
        modelpart.loadPose(this.partPose);
        modelpart.setName(this.name);
        return modelpart;
    }

    public PartDefinition getChild(String p_171598_)
    {
        return this.children.get(p_171598_);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
