package net.tge11.firstmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tge11.firstmod.FirstMod;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FirstMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BACKROOMS_MUSIC = SOUNDS.register("music.backrooms",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, "music.backrooms")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}