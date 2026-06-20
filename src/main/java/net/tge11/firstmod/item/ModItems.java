package net.tge11.firstmod.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tge11.firstmod.FirstMod;
import net.tge11.firstmod.item.custom.BlightSyringe;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FirstMod.MODID);

    public static final DeferredItem<Item> BLIGHT = ITEMS.register("blight",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SCREAM_MASK = ITEMS.register("scream_mask",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OPAL = ITEMS.register("opal",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLIGHT_SYRINGE = ITEMS.register("blight_syringe",
            () -> new BlightSyringe(new Item.Properties().stacksTo(1)));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
