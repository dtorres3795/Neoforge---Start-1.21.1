package net.tge11.firstmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tge11.firstmod.FirstMod;
import net.tge11.firstmod.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FirstMod.MODID);

    public static final Supplier<CreativeModeTab> DBD_ITEMS_TAB = CREATIVE_MODE_TAB.register("dbd_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BLIGHT.get()))
                    .title(Component.translatable("creativetab.firstmod.dbd_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.BLIGHT);
                        output.accept(ModItems.SCREAM_MASK);
                    }).build());

    public static final Supplier<CreativeModeTab> UNORGANIZED_BLOCKS_TAB = CREATIVE_MODE_TAB.register("unorganized_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.BLIGHT_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, "dbd_items_tab"))
                    .title(Component.translatable("creativetab.firstmod.unorganized_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.BLIGHT_BLOCK);
                        output.accept(ModBlocks.HOLLOW);
                        output.accept(ModBlocks.OPAL_BLOCK);
                        output.accept(ModBlocks.OPAL_ORE);
                        output.accept(ModBlocks.LIMINAL_PLANKS);
                    }).build());

    public static final Supplier<CreativeModeTab> OTHER_TAB = CREATIVE_MODE_TAB.register("other_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.OPAL.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, "unorganized_blocks_tab"))
                    .title(Component.translatable("creativetab.firstmod.other"))
                    .displayItems((itemsDisplayParameters, output) -> {
                        output.accept(ModItems.OPAL);
                    }).build());


    public static void register (IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
