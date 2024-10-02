package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

public class SmithingTransformRecipeBuilder
{
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final RecipeCategory category;
    private final Item result;
    private final Map < String, Criterion<? >> criteria = new LinkedHashMap<>();

    public SmithingTransformRecipeBuilder(Ingredient p_266973_, Ingredient p_267047_, Ingredient p_267009_, RecipeCategory p_266694_, Item p_267183_)
    {
        this.category = p_266694_;
        this.template = p_266973_;
        this.base = p_267047_;
        this.addition = p_267009_;
        this.result = p_267183_;
    }

    public static SmithingTransformRecipeBuilder smithing(
        Ingredient p_267071_, Ingredient p_266959_, Ingredient p_266803_, RecipeCategory p_266757_, Item p_267256_
    )
    {
        return new SmithingTransformRecipeBuilder(p_267071_, p_266959_, p_266803_, p_266757_, p_267256_);
    }

    public SmithingTransformRecipeBuilder unlocks(String p_266919_, Criterion<?> p_297342_)
    {
        this.criteria.put(p_266919_, p_297342_);
        return this;
    }

    public void save(RecipeOutput p_300964_, String p_267035_)
    {
        this.save(p_300964_, ResourceLocation.parse(p_267035_));
    }

    public void save(RecipeOutput p_301024_, ResourceLocation p_267287_)
    {
        this.ensureValid(p_267287_);
        Advancement.Builder advancement$builder = p_301024_.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_267287_))
                .rewards(AdvancementRewards.Builder.recipe(p_267287_))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        SmithingTransformRecipe smithingtransformrecipe = new SmithingTransformRecipe(
            this.template, this.base, this.addition, new ItemStack(this.result)
        );
        p_301024_.accept(
            p_267287_, smithingtransformrecipe, advancement$builder.build(p_267287_.withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceLocation p_267259_)
    {
        if (this.criteria.isEmpty())
        {
            throw new IllegalStateException("No way of obtaining recipe " + p_267259_);
        }
    }
}
