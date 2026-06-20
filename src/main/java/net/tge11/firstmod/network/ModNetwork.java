package net.tge11.firstmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tge11.firstmod.event.BlightDashHandler;

public class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BlightDashPayload.TYPE, BlightDashPayload.STREAM_CODEC, ModNetwork::handleBlightDash);
    }

    private static void handleBlightDash(BlightDashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlightDashHandler.tryDash(player);
            }
        });
    }
}