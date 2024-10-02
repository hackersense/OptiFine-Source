package net.optifine.config;

import net.minecraft.world.item.enchantment.Enchantment;
import net.optifine.util.EnchantmentUtils;

public class ParserEnchantmentId implements IParserInt
{
    @Override
    public int parse(String str, int defVal)
    {
        Enchantment enchantment = EnchantmentUtils.getEnchantment(str);
        return enchantment == null ? defVal : EnchantmentUtils.getId(enchantment);
    }
}
