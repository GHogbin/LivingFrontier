package dev.livingfrontier.puzzle;

import dev.livingfrontier.FrontierItems;
import dev.livingfrontier.LivingFrontier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PuzzleBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LivingFrontier.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LivingFrontier.MOD_ID);
    public static final RegistryObject<Block> SUN_RUNE = rune("sun_rune", RuneSymbol.SUN);
    public static final RegistryObject<Block> LEAF_RUNE = rune("leaf_rune", RuneSymbol.LEAF);
    public static final RegistryObject<Block> WAVE_RUNE = rune("wave_rune", RuneSymbol.WAVE);
    public static final RegistryObject<Block> SEAL_CONTROLLER = BLOCKS.register("seal_controller",
            () -> new RuneBlock(null, true));
    public static final RegistryObject<Block> SEAL_BARRIER = BLOCKS.register("seal_barrier", () -> new Block(
            BlockBehaviour.Properties.of().strength(-1, 3600000).sound(SoundType.METAL).noLootTable().noOcclusion()));
    public static final RegistryObject<BlockEntityType<RuneStoneBlockEntity>> RUNE_ENTITY = BLOCK_ENTITIES.register("rune_stone",
            () -> BlockEntityType.Builder.of(RuneStoneBlockEntity::new, SUN_RUNE.get(), LEAF_RUNE.get(), WAVE_RUNE.get()).build(null));
    public static final RegistryObject<BlockEntityType<SealControllerBlockEntity>> CONTROLLER_ENTITY = BLOCK_ENTITIES.register("seal_controller",
            () -> BlockEntityType.Builder.of(SealControllerBlockEntity::new, SEAL_CONTROLLER.get()).build(null));

    static {
        registerItem("sun_rune", SUN_RUNE);
        registerItem("leaf_rune", LEAF_RUNE);
        registerItem("wave_rune", WAVE_RUNE);
        registerItem("seal_controller", SEAL_CONTROLLER);
        registerItem("seal_barrier", SEAL_BARRIER);
    }

    private PuzzleBlocks() {
    }

    private static RegistryObject<Block> rune(String name, RuneSymbol symbol) {
        return BLOCKS.register(name, () -> new RuneBlock(symbol, false));
    }

    private static void registerItem(String name, RegistryObject<Block> block) {
        FrontierItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(SUN_RUNE.get());
            event.accept(LEAF_RUNE.get());
            event.accept(WAVE_RUNE.get());
            event.accept(SEAL_CONTROLLER.get());
            event.accept(SEAL_BARRIER.get());
        }
    }
}
