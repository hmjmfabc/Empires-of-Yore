package top.swordsman.empire.machine;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import top.swordsman.empire.EmpiresOfYoreMod;

/** 工业熔炉: 2 个冶炼槽, 支持所有燃料 */
public class IndustrialOvenBlockEntity extends AbstractMachineBlockEntity {

    public static final int FUEL = 0;
    public static final int INPUT0 = 1, INPUT1 = 2, OUTPUT = 3;

    public IndustrialOvenBlockEntity(BlockPos pos, BlockState state) {
        super(top.swordsman.empire.machine.MachineBlocks.INDUSTRIAL_OVEN_BE.get(), pos, state, FUEL, -1, 2, 1);
    }

    @Override public int getMachineType() { return 1; }

    @Override public boolean isValidFuel(ItemStack stack) {
        return getFuelTime(stack) > 0;
    }

    @Override public int getFuelTime(ItemStack stack) {
        if (stack.is(EmpiresOfYoreMod.ADVANCED_ENERGY_STONE.get())) return 3200;
        if (stack.is(EmpiresOfYoreMod.POWER_STONE.get())) return 1600;
        try {
            Integer v = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
            return v == null ? 0 : v;
        } catch (Throwable t) { return 0; }
    }

    private static SmeltResult single(ItemStack in, ItemStack out, int ticks, float xp, int count) {
        return new SmeltResult(out, ticks, xp, 0, count);
    }

    @Override
    public SmeltResult findRecipe(int inputIndex, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(EmpiresOfYoreMod.RAW_CHROMIUM.get())) return single(stack, new ItemStack(EmpiresOfYoreMod.CHROMIUM_INGOT.get()), 360, 18f, 1);
        if (stack.is(EmpiresOfYoreMod.DRAGON_REMAINS_ITEM.get())) return single(stack, new ItemStack(EmpiresOfYoreMod.ENDITE_SCRAP.get()), 6000, 300f, 1);
        if (stack.is(Items.COAL)) return single(stack, new ItemStack(EmpiresOfYoreMod.COKE.get()), 200, 10f, 1);
        // 向下兼容熔炉配方
        if (level != null) {
            try {
                var r = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                        new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level);
                if (r.isPresent()) {
                    SmeltingRecipe rec = r.get().value();
                    return new SmeltResult(rec.getResultItem(RegistryAccess.EMPTY).copy(), rec.getCookingTime(), rec.getExperience(), 0, 1);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
