package net.tge11.firstmod.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tge11.firstmod.FirstMod;
import net.tge11.firstmod.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FirstMod.MODID);

    public static final DeferredBlock<Block> BLIGHT_BLOCK = registerBlock("blight_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.MOSS)));

    public static final DeferredBlock<Block> HOLLOW = registerBlock("hollow",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of()
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.SCULK)));

    public static final DeferredBlock<Block> OPAL_BLOCK = registerBlock("opal_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> OPAL_ORE = registerBlock("opal_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of()
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> LIMINAL_PLANKS = registerBlock("liminal_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.WOOD)));

    public static final DeferredBlock<Block> LIMINAL_BLOCK = registerBlock("liminal_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.WOOD)));

    public static final DeferredBlock<StairBlock> LIMINAL_STAIRS = registerBlock("liminal_stairs",
            () -> new StairBlock(LIMINAL_PLANKS.get().defaultBlockState(), liminalWoodProperties()));

    public static final DeferredBlock<SlabBlock> LIMINAL_SLAB = registerBlock("liminal_slab",
            () -> new SlabBlock(liminalWoodProperties()));

    public static final DeferredBlock<FenceBlock> LIMINAL_FENCE = registerBlock("liminal_fence",
            () -> new FenceBlock(liminalWoodProperties()));

    public static final DeferredBlock<FenceGateBlock> LIMINAL_FENCE_GATE = registerBlock("liminal_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, liminalWoodProperties()));

    public static final DeferredBlock<DoorBlock> LIMINAL_DOOR = registerBlock("liminal_door",
            () -> new DoorBlock(BlockSetType.OAK, liminalDoorProperties()));

    public static final DeferredBlock<TrapDoorBlock> LIMINAL_TRAPDOOR = registerBlock("liminal_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, liminalTrapdoorProperties()));

    public static final DeferredBlock<PressurePlateBlock> LIMINAL_PRESSURE_PLATE = registerBlock("liminal_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, liminalPressurePlateProperties()));

    public static final DeferredBlock<ButtonBlock> LIMINAL_BUTTON = registerBlock("liminal_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, liminalButtonProperties()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
