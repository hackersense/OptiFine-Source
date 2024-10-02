package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.serialization.Codec;

public record TelemetryEventInstance(TelemetryEventType type, TelemetryPropertyMap properties)
{
    public static final Codec<TelemetryEventInstance> CODEC = TelemetryEventType.CODEC
            .dispatchStable(TelemetryEventInstance::type, TelemetryEventType::codec);
    public TelemetryEventInstance(TelemetryEventType type, TelemetryPropertyMap properties)
    {
        properties.propertySet().forEach(p_261699_ ->
        {
            if (!type.contains((TelemetryProperty<?>)p_261699_))
            {
                throw new IllegalArgumentException("Property '" + p_261699_.id() + "' not expected for event: '" + type.id() + "'");
            }
        });
        this.type = type;
        this.properties = properties;
    }
    public TelemetryEvent export(TelemetrySession p_261645_)
    {
        return this.type.export(p_261645_, this.properties);
    }
}
