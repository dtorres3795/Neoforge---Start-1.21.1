package net.tge11.firstmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tge11.firstmod.FirstMod;

public record BlightDashPayload() implements CustomPacketPayload {
    public static final Type<BlightDashPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, "blight_dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlightDashPayload> STREAM_CODEC = StreamCodec.unit(new BlightDashPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}