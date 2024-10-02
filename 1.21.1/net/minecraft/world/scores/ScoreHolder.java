package net.minecraft.world.scores;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public interface ScoreHolder
{
    String WILDCARD_NAME = "*";
    ScoreHolder WILDCARD = new ScoreHolder()
    {
        @Override
        public String getScoreboardName()
        {
            return "*";
        }
    };

    String getScoreboardName();

    @Nullable

default Component getDisplayName()
    {
        return null;
    }

default Component getFeedbackDisplayName()
    {
        Component component = this.getDisplayName();
        return component != null
               ? component.copy().withStyle(p_312428_ -> p_312428_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getScoreboardName()))))
               : Component.literal(this.getScoreboardName());
    }

    static ScoreHolder forNameOnly(final String p_312707_)
    {
        if (p_312707_.equals("*"))
        {
            return WILDCARD;
        }
        else
        {
            final Component component = Component.literal(p_312707_);
            return new ScoreHolder()
            {
                @Override
                public String getScoreboardName()
                {
                    return p_312707_;
                }
                @Override
                public Component getFeedbackDisplayName()
                {
                    return component;
                }
            };
        }
    }

    static ScoreHolder fromGameProfile(GameProfile p_311927_)
    {
        final String s = p_311927_.getName();
        return new ScoreHolder()
        {
            @Override
            public String getScoreboardName()
            {
                return s;
            }
        };
    }
}
