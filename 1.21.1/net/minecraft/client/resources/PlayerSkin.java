package net.minecraft.client.resources;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkin(
    ResourceLocation texture,
    @Nullable String textureUrl,
    @Nullable ResourceLocation capeTexture,
    @Nullable ResourceLocation elytraTexture,
    PlayerSkin.Model model,
    boolean secure
)
{
    public static enum Model
    {
        SLIM("slim"),
        WIDE("default");

        private final String id;

        private Model(final String p_300061_)
        {
            this.id = p_300061_;
        }

        public static PlayerSkin.Model byName(@Nullable String p_299354_)
        {
            if (p_299354_ == null)
            {
                return WIDE;
            }
            else
            {
                byte b0 = -1;

                switch (p_299354_.hashCode())
                {
                    case 3533117:
                        if (p_299354_.equals("slim"))
                        {
                            b0 = 0;
                        }

                    default:
                        return switch (b0)
                        {
                            case 0 -> SLIM;

                            default -> WIDE;
                        };
                }
            }
        }

        public String id()
        {
            return this.id;
        }
    }
}
