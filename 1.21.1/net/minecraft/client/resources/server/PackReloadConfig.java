package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface PackReloadConfig
{
    void scheduleReload(PackReloadConfig.Callbacks p_312695_);

    public interface Callbacks
    {
        void onSuccess();

        void onFailure(boolean p_313000_);

        List<PackReloadConfig.IdAndPath> packsToLoad();
    }

    public static record IdAndPath(UUID id, Path path)
    {
    }
}
