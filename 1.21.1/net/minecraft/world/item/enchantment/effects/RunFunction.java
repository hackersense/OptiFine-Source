package net.minecraft.world.item.enchantment.effects;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public record RunFunction(ResourceLocation function) implements EnchantmentEntityEffect
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<RunFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_344153_ -> p_344153_.group(ResourceLocation.CODEC.fieldOf("function").forGetter(RunFunction::function)).apply(p_344153_, RunFunction::new)
    );

    @Override
    public void apply(ServerLevel p_344815_, int p_342426_, EnchantedItemInUse p_343415_, Entity p_344692_, Vec3 p_344054_)
    {
        MinecraftServer minecraftserver = p_344815_.getServer();
        ServerFunctionManager serverfunctionmanager = minecraftserver.getFunctions();
        Optional<CommandFunction<CommandSourceStack>> optional = serverfunctionmanager.get(this.function);

        if (optional.isPresent())
        {
            CommandSourceStack commandsourcestack = minecraftserver.createCommandSourceStack()
            .withPermission(2)
            .withSuppressedOutput()
            .withEntity(p_344692_)
            .withLevel(p_344815_)
            .withPosition(p_344054_)
            .withRotation(p_344692_.getRotationVector());
            serverfunctionmanager.execute(optional.get(), commandsourcestack);
        }
        else
        {
            LOGGER.error("Enchantment run_function effect failed for non-existent function {}", this.function);
        }
    }

    @Override
    public MapCodec<RunFunction> codec()
    {
        return CODEC;
    }
}
