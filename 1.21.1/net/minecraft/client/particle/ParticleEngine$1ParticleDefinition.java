package net.minecraft.client.particle;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

record ParticleEngine$1ParticleDefinition(ResourceLocation id, Optional<List<ResourceLocation>> sprites)
{
}
