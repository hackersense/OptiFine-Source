package net.minecraft.client.main;

import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.DisplayData;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.util.StringUtil;

public class GameConfig
{
    public final GameConfig.UserData user;
    public final DisplayData display;
    public final GameConfig.FolderData location;
    public final GameConfig.GameData game;
    public final GameConfig.QuickPlayData quickPlay;

    public GameConfig(
        GameConfig.UserData p_279448_,
        DisplayData p_279368_,
        GameConfig.FolderData p_279174_,
        GameConfig.GameData p_279138_,
        GameConfig.QuickPlayData p_279425_
    )
    {
        this.user = p_279448_;
        this.display = p_279368_;
        this.location = p_279174_;
        this.game = p_279138_;
        this.quickPlay = p_279425_;
    }

    public static class FolderData
    {
        public final File gameDirectory;
        public final File resourcePackDirectory;
        public final File assetDirectory;
        @Nullable
        public final String assetIndex;

        public FolderData(File p_101921_, File p_101922_, File p_101923_, @Nullable String p_101924_)
        {
            this.gameDirectory = p_101921_;
            this.resourcePackDirectory = p_101922_;
            this.assetDirectory = p_101923_;
            this.assetIndex = p_101924_;
        }

        public Path getExternalAssetSource()
        {
            return this.assetIndex == null ? this.assetDirectory.toPath() : IndexedAssetSource.createIndexFs(this.assetDirectory.toPath(), this.assetIndex);
        }
    }

    public static class GameData
    {
        public final boolean demo;
        public final String launchVersion;
        public final String versionType;
        public final boolean disableMultiplayer;
        public final boolean disableChat;

        public GameData(boolean p_101932_, String p_101933_, String p_101934_, boolean p_101935_, boolean p_101936_)
        {
            this.demo = p_101932_;
            this.launchVersion = p_101933_;
            this.versionType = p_101934_;
            this.disableMultiplayer = p_101935_;
            this.disableChat = p_101936_;
        }
    }

    public static record QuickPlayData(@Nullable String path, @Nullable String singleplayer, @Nullable String multiplayer, @Nullable String realms)
    {
        public boolean isEnabled()
        {
            return !StringUtil.isBlank(this.singleplayer) || !StringUtil.isBlank(this.multiplayer) || !StringUtil.isBlank(this.realms);
        }
    }

    public static class UserData
    {
        public final User user;
        public final PropertyMap userProperties;
        public final PropertyMap profileProperties;
        public final Proxy proxy;

        public UserData(User p_101947_, PropertyMap p_101948_, PropertyMap p_101949_, Proxy p_101950_)
        {
            this.user = p_101947_;
            this.userProperties = p_101948_;
            this.profileProperties = p_101949_;
            this.proxy = p_101950_;
        }
    }
}
