package net.tge11.firstmod.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tge11.firstmod.network.BlightDashPayload;

public class BlightDashClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyMappings.BLIGHT_DASH.consumeClick()) {
            PacketDistributor.sendToServer(new BlightDashPayload());
        }
    }
}