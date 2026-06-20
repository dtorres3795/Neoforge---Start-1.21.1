package net.tge11.firstmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class ModKeyMappings {
    public static final KeyMapping BLIGHT_DASH = new KeyMapping(
            "key.firstmod.blight_dash",
            InputConstants.KEY_T,
            "key.categories.firstmod"
    );

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BLIGHT_DASH);
    }
}