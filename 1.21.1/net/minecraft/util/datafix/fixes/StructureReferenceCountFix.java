package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class StructureReferenceCountFix extends DataFix
{
    public StructureReferenceCountFix(Schema p_16961_, boolean p_16962_)
    {
        super(p_16961_, p_16962_);
    }

    @Override
    protected TypeRewriteRule makeRule()
    {
        Type<?> type = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
        return this.fixTypeEverywhereTyped(
                   "Structure Reference Fix", type, p_16964_ -> p_16964_.update(DSL.remainderFinder(), StructureReferenceCountFix::setCountToAtLeastOne)
               );
    }

    private static <T> Dynamic<T> setCountToAtLeastOne(Dynamic<T> p_16966_)
    {
        return p_16966_.update(
                   "references", p_326645_ -> p_326645_.createInt(p_326645_.asNumber().map(Number::intValue).result().filter(p_145724_ -> p_145724_ > 0).orElse(1))
               );
    }
}
