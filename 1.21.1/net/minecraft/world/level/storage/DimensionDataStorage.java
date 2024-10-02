package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class DimensionDataStorage
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, SavedData> cache = Maps.newHashMap();
    private final DataFixer fixerUpper;
    private final HolderLookup.Provider registries;
    private final File dataFolder;

    public DimensionDataStorage(File p_78149_, DataFixer p_78150_, HolderLookup.Provider p_336063_)
    {
        this.fixerUpper = p_78150_;
        this.dataFolder = p_78149_;
        this.registries = p_336063_;
    }

    private File getDataFile(String p_78157_)
    {
        return new File(this.dataFolder, p_78157_ + ".dat");
    }

    public <T extends SavedData> T computeIfAbsent(SavedData.Factory<T> p_297495_, String p_164864_)
    {
        T t = this.get(p_297495_, p_164864_);

        if (t != null)
        {
            return t;
        }
        else
        {
            T t1 = (T)p_297495_.constructor().get();
            this.set(p_164864_, t1);
            return t1;
        }
    }

    @Nullable
    public <T extends SavedData> T get(SavedData.Factory<T> p_297465_, String p_164860_)
    {
        SavedData saveddata = this.cache.get(p_164860_);

        if (saveddata == null && !this.cache.containsKey(p_164860_))
        {
            saveddata = this.readSavedData(p_297465_.deserializer(), p_297465_.type(), p_164860_);
            this.cache.put(p_164860_, saveddata);
        }

        return (T)saveddata;
    }

    @Nullable
    private <T extends SavedData> T readSavedData(BiFunction<CompoundTag, HolderLookup.Provider, T> p_335409_, DataFixTypes p_300231_, String p_164870_)
    {
        try
        {
            File file1 = this.getDataFile(p_164870_);

            if (file1.exists())
            {
                CompoundTag compoundtag = this.readTagFromDisk(p_164870_, p_300231_, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                return p_335409_.apply(compoundtag.getCompound("data"), this.registries);
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Error loading saved data: {}", p_164870_, exception);
        }

        return null;
    }

    public void set(String p_164856_, SavedData p_164857_)
    {
        this.cache.put(p_164856_, p_164857_);
    }

    public CompoundTag readTagFromDisk(String p_78159_, DataFixTypes p_301060_, int p_78160_) throws IOException
    {
        File file1 = this.getDataFile(p_78159_);
        CompoundTag compoundtag1;

        try (
                InputStream inputstream = new FileInputStream(file1);
                PushbackInputStream pushbackinputstream = new PushbackInputStream(new FastBufferedInputStream(inputstream), 2);
            )
        {
            CompoundTag compoundtag;

            if (this.isGzip(pushbackinputstream))
            {
                compoundtag = NbtIo.readCompressed(pushbackinputstream, NbtAccounter.unlimitedHeap());
            }
            else
            {
                try (DataInputStream datainputstream = new DataInputStream(pushbackinputstream))
                {
                    compoundtag = NbtIo.read(datainputstream);
                }
            }

            int i = NbtUtils.getDataVersion(compoundtag, 1343);
            compoundtag1 = p_301060_.update(this.fixerUpper, compoundtag, i, p_78160_);
        }

        return compoundtag1;
    }

    private boolean isGzip(PushbackInputStream p_78155_) throws IOException
    {
        byte[] abyte = new byte[2];
        boolean flag = false;
        int i = p_78155_.read(abyte, 0, 2);

        if (i == 2)
        {
            int j = (abyte[1] & 255) << 8 | abyte[0] & 255;

            if (j == 35615)
            {
                flag = true;
            }
        }

        if (i != 0)
        {
            p_78155_.unread(abyte, 0, i);
        }

        return flag;
    }

    public void save()
    {
        this.cache.forEach((p_327534_, p_327535_) ->
        {
            if (p_327535_ != null)
            {
                p_327535_.save(this.getDataFile(p_327534_), this.registries);
            }
        });
    }
}
