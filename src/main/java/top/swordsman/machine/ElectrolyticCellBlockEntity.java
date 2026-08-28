package top.swordsman.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import top.swordsman.SwordsmanMod;

/** 电解炉: 仅能源石/高级能源石燃料; 冰晶石粉试剂槽; 铝/镁电解 */
public class ElectrolyticCellBlockEntity extends AbstractMachineBlockEntity {

    public static final int FUEL = 0;
    public static final int REAGENT = 1;
    public static final int INPUT = 2;
    public static final int OUTPUT = 3;

    public ElectrolyticCellBlockEntity(BlockPos pos, BlockState state) {
        super(top.swordsman.machine.MachineBlocks.ELECTROLYTIC_CELL_BE.get(), pos, state, FUEL, REAGENT, 1, 1);
    }

    @Override public int getMachineType() { return 0; }

    @Override public boolean isValidFuel(ItemStack s) {
        return s.is(SwordsmanMod.POWER_STONE.get()) || s.is(SwordsmanMod.ADVANCED_ENERGY_STONE.get());
    }

    @Override public int getFuelTime(ItemStack s) {
        if (s.is(SwordsmanMod.POWER_STONE.get())) return 1600;
        if (s.is(SwordsmanMod.ADVANCED_ENERGY_STONE.get())) return 3200;
        return 0;
    }

    @Override
    public SmeltResult findRecipe(int inputIndex, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(SwordsmanMod.BAUXITE_POWDER.get()))
            return new SmeltResult(new ItemStack(SwordsmanMod.ALUMINUM_INGOT.get()), 1800, 90f, 1, 3);
        if (stack.is(SwordsmanMod.RAW_MAGNESIUM.get()))
            return new SmeltResult(new ItemStack(SwordsmanMod.MAGNESIUM_INGOT.get()), 2400, 120f, 0, 1);
        return null;
    }

    @Override
    public float fuelCostPerTick(int inputIndex, ItemStack input) {
        if (input.is(SwordsmanMod.BAUXITE_POWDER.get())) {
            return hasCryolite() ? 1.0f : 4.0f;      // 无冰晶粉 → 能量4倍消耗
        }
        if (input.is(SwordsmanMod.RAW_MAGNESIUM.get())) return 2.0f; // 镁 → 能量2倍消耗
        return 1.0f;
    }

    private boolean hasCryolite() {
        ItemStack r = getItem(reagentSlot);
        return !r.isEmpty() && r.is(SwordsmanMod.CRYOLITE_POWDER.get());
    }
}
