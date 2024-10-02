package net.minecraft.client.gui.screens;

import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;

public class ReceivingLevelScreen extends Screen
{
    private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
    private static final long CHUNK_LOADING_START_WAIT_LIMIT_MS = 30000L;
    private final long createdAt;
    private final BooleanSupplier levelReceived;
    private final ReceivingLevelScreen.Reason reason;
    @Nullable
    private TextureAtlasSprite cachedNetherPortalSprite;
    private CustomLoadingScreen customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();

    public ReceivingLevelScreen(BooleanSupplier p_310110_, ReceivingLevelScreen.Reason p_336020_)
    {
        super(GameNarrator.NO_TITLE);
        this.levelReceived = p_310110_;
        this.reason = p_336020_;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation()
    {
        return false;
    }

    @Override
    public void render(GuiGraphics p_281489_, int p_282902_, int p_283018_, float p_281251_)
    {
        super.render(p_281489_, p_282902_, p_283018_, p_281251_);
        p_281489_.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, this.width / 2, this.height / 2 - 50, 16777215);
    }

    @Override
    public void renderBackground(GuiGraphics p_298240_, int p_297552_, int p_298125_, float p_297335_)
    {
        if (this.customLoadingScreen != null)
        {
            this.customLoadingScreen.drawBackground(this.width, this.height);
        }
        else
        {
            switch (this.reason)
            {
                case NETHER_PORTAL:
                    p_298240_.blit(0, 0, -90, p_298240_.guiWidth(), p_298240_.guiHeight(), this.getNetherPortalSprite());
                    break;

                case END_PORTAL:
                    p_298240_.fillRenderType(RenderType.endPortal(), 0, 0, this.width, this.height, 0);
                    break;

                case OTHER:
                    this.renderPanorama(p_298240_, p_297335_);
                    this.renderBlurredBackground(p_297335_);
                    this.renderMenuBackground(p_298240_);
            }
        }
    }

    private TextureAtlasSprite getNetherPortalSprite()
    {
        if (this.cachedNetherPortalSprite != null)
        {
            return this.cachedNetherPortalSprite;
        }
        else
        {
            this.cachedNetherPortalSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
            return this.cachedNetherPortalSprite;
        }
    }

    @Override
    public void tick()
    {
        if (this.levelReceived.getAsBoolean() || System.currentTimeMillis() > this.createdAt + 30000L)
        {
            this.onClose();
        }
    }

    @Override
    public void onClose()
    {
        this.minecraft.getNarrator().sayNow(Component.translatable("narrator.ready_to_play"));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    public static enum Reason
    {
        NETHER_PORTAL,
        END_PORTAL,
        OTHER;
    }
}
