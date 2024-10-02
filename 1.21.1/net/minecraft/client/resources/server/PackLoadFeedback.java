package net.minecraft.client.resources.server;

import java.util.UUID;

public interface PackLoadFeedback
{
    void reportUpdate(UUID p_312796_, PackLoadFeedback.Update p_311319_);

    void reportFinalResult(UUID p_309920_, PackLoadFeedback.FinalResult p_312819_);

    public static enum FinalResult
    {
        DECLINED,
        APPLIED,
        DISCARDED,
        DOWNLOAD_FAILED,
        ACTIVATION_FAILED;
    }

    public static enum Update
    {
        ACCEPTED,
        DOWNLOADED;
    }
}
