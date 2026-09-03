package top.swordsman.empire.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import top.swordsman.empire.EmpiresOfYoreMod;

public class MachineBlocks {

    private static BlockBehaviour.Properties props(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(4.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<MachineBlock> INDUSTRIAL_OVEN = EmpiresOfYoreMod.BLOCKS.register("industrial_oven",
            () -> new MachineBlock(() -> MachineBlocks.INDUSTRIAL_OVEN_BE.get(),
                    Component.translatable("block.swordsman.industrial_oven"), props(MapColor.TERRACOTTA_ORANGE)));
    public static final DeferredBlock<MachineBlock> ADVANCED_INDUSTRIAL_OVEN = EmpiresOfYoreMod.BLOCKS.register("advanced_industrial_oven",
            () -> new MachineBlock(() -> MachineBlocks.ADVANCED_INDUSTRIAL_OVEN_BE.get(),
                    Component.translatable("block.swordsman.advanced_industrial_oven"), props(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<MachineBlock> ELECTROLYTIC_CELL = EmpiresOfYoreMod.BLOCKS.register("electrolytic_cell",
            () -> new MachineBlock(() -> MachineBlocks.ELECTROLYTIC_CELL_BE.get(),
                    Component.translatable("block.swordsman.electrolytic_cell"), props(MapColor.COLOR_GRAY)));

    public static final net.neoforged.neoforge.registries.DeferredItem<BlockItem> INDUSTRIAL_OVEN_ITEM = EmpiresOfYoreMod.ITEMS.registerSimpleBlockItem("industrial_oven", INDUSTRIAL_OVEN);
    public static final net.neoforged.neoforge.registries.DeferredItem<BlockItem> ADVANCED_INDUSTRIAL_OVEN_ITEM = EmpiresOfYoreMod.ITEMS.registerSimpleBlockItem("advanced_industrial_oven", ADVANCED_INDUSTRIAL_OVEN);
    public static final net.neoforged.neoforge.registries.DeferredItem<BlockItem> ELECTROLYTIC_CELL_ITEM = EmpiresOfYoreMod.ITEMS.registerSimpleBlockItem("electrolytic_cell", ELECTROLYTIC_CELL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialOvenBlockEntity>> INDUSTRIAL_OVEN_BE =
            EmpiresOfYoreMod.BLOCK_ENTITIES.register("industrial_oven",
                    () -> BlockEntityType.Builder.of(IndustrialOvenBlockEntity::new, INDUSTRIAL_OVEN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedIndustrialOvenBlockEntity>> ADVANCED_INDUSTRIAL_OVEN_BE =
            EmpiresOfYoreMod.BLOCK_ENTITIES.register("advanced_industrial_oven",
                    () -> BlockEntityType.Builder.of(AdvancedIndustrialOvenBlockEntity::new, ADVANCED_INDUSTRIAL_OVEN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyticCellBlockEntity>> ELECTROLYTIC_CELL_BE =
            EmpiresOfYoreMod.BLOCK_ENTITIES.register("electrolytic_cell",
                    () -> BlockEntityType.Builder.of(ElectrolyticCellBlockEntity::new, ELECTROLYTIC_CELL.get()).build(null));
}
