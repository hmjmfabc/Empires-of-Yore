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

    /**
     * ⑫ 批量配方: {输入规格[[槽位, 物品id, 数量]...], 结果id, 数量, tick, xp}
     * 2 远古残骸 + 3 焦炭 → 3 下界合金碎片(3 分钟)
     * 3 龙骸 + 8 焦炭 → 4 末影合金碎片(6 分钟)
     */
    private static final Object[][] BATCH = {
        {new Object[][]{{0, "minecraft:ancient_debris", 2}, {1, "empire:coke", 3}},
         "minecraft:netherite_scrap", 3, 3600, 180f},
        {new Object[][]{{0, "empire:dragon_remains", 3}, {1, "empire:coke", 8}},
         "empire:endite_scrap", 4, 7200, 360f},
    };

    private int batchProgress = 0;

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
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        Object[] batch = matchBatch();
        if (batch != null) {
            if (burnTime > 0) {
                burnTime--;
                batchProgress++;
                if (batchProgress >= (Integer) batch[3]) {
                    ItemStack result = byId((String) batch[1]);
                    result.setCount((Integer) batch[2]);
                    if (!fitsOutput(result)) {
                        batchProgress = (Integer) batch[3] - 1;
                        syncData();
                        return;
                    }
                    for (Object[] spec : (Object[][]) batch[0]) {
                        getItem(inputStart + (Integer) spec[0]).shrink((Integer) spec[2]);
                    }
                    ItemStack out = getItem(outputStart);
                    if (out.isEmpty()) {
                        setItem(outputStart, result);
                    } else {
                        out.grow(result.getCount());
                    }
                    if (level instanceof net.minecraft.server.level.ServerLevel sl && (Float) batch[4] > 0) {
                        net.minecraft.world.entity.ExperienceOrb.award(sl,
                                new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5),
                                (int) (float) (Float) batch[4]);
                    }
                    batchProgress = 0;
                }
            } else {
                refuel();
            }
            syncData();
            return;
        }
        batchProgress = 0;
        super.serverTick();
    }

    private Object[] matchBatch() {
        for (Object[] rec : BATCH) {
            boolean ok = true;
            for (Object[] spec : (Object[][]) rec[0]) {
                ItemStack stack = getItem(inputStart + (Integer) spec[0]);
                if (stack.getCount() < (Integer) spec[2]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return rec;
            }
        }
        return null;
    }

    private boolean fitsOutput(ItemStack result) {
        ItemStack out = getItem(outputStart);
        return out.isEmpty() || (out.is(result.getItem()) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private void refuel() {
        ItemStack fuel = getItem(fuelSlot);
        if (!fuel.isEmpty() && isValidFuel(fuel)) {
            burnDuration = getFuelTime(fuel);
            burnTime = burnDuration;
            fuel.shrink(1);
        } else {
            burnDuration = 1;
            burnTime = 0;
        }
    }

    private static ItemStack byId(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)));
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
