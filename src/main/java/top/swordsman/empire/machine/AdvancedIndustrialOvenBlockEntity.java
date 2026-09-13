package top.swordsman.empire.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import top.swordsman.empire.EmpiresOfYoreMod;

/** 高级工业熔炉: 4 槽批次配方 + 单槽兼容所有熔炼配方 */
public class AdvancedIndustrialOvenBlockEntity extends AbstractMachineBlockEntity {

    public static final int FUEL = 0;
    public static final int INPUT0 = 1;

    /** 批次配方: {槽位数组[{槽索引, 物品id, 数量}], 结果id, tick, xp} */
    private static final Object[][] BATCH = {
        {new Object[][]{{0, "empire:magnesium_ingot", 2}, {1, "empire:aluminum_ingot", 3},
                        {2, "empire:chromium_ingot", 1}, {3, "empire:coke", 1}},
         "empire:duraalumin_ingot", 12000, 600f},
        {new Object[][]{{0, "empire:titanium_ingot", 2}, {1, "empire:duraalumin_ingot", 3},
                        {2, "empire:coke", 4}},
         "empire:titanite_ingot", 12000, 600f},
        {new Object[][]{{0, "minecraft:iron_ingot", 1}, {1, "empire:coke", 1}},
         "empire:carbon_steel_ingot", 4800, 480f},
        {new Object[][]{{0, "empire:raw_titamium", 1}},
         "empire:titanium_ingot", 7200, 360f},
        // ⑫ 工业熔炉及以上: 2 远古残骸 + 3 焦炭 → 3 下界合金碎片(3 分钟)
        {new Object[][]{{0, "minecraft:ancient_debris", 2}, {1, "empire:coke", 3}},
         "minecraft:netherite_scrap", 3, 3600, 180f},
        // ⑫ 3 龙骸 + 8 焦炭 → 4 末影合金碎片(6 分钟)
        {new Object[][]{{0, "empire:dragon_remains", 3}, {1, "empire:coke", 8}},
         "empire:endite_scrap", 4, 7200, 360f},
        // ⑰ 三界钛合金锭: 钛合金锭/下界合金锭/末影合金锭/焦炭 = 4:4:4:9, 10 分钟, 600 EXP
        {new Object[][]{{0, "empire:titanite_ingot", 4}, {1, "minecraft:netherite_ingot", 4},
                        {2, "empire:endite_ingot", 4}, {3, "empire:coke", 9}},
         "empire:trinity_titanite_ingot", 1, 12000, 600f},
    };

    private int batchProgress = 0;
    private static final int BATCH_NONE = 0;

    public AdvancedIndustrialOvenBlockEntity(BlockPos pos, BlockState state) {
        super(top.swordsman.empire.machine.MachineBlocks.ADVANCED_INDUSTRIAL_OVEN_BE.get(), pos, state, FUEL, -1, 4, 1);
    }

    @Override public int getMachineType() { return 2; }
    @Override public boolean isValidFuel(ItemStack s) { return getFuelTime(s) > 0; }

    @Override public int getFuelTime(ItemStack s) {
        if (s.is(EmpiresOfYoreMod.ADVANCED_ENERGY_STONE.get())) return 3200;
        if (s.is(EmpiresOfYoreMod.POWER_STONE.get())) return 1600;
        try {
            Integer v = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().get(s.getItem());
            return v == null ? 0 : v;
        } catch (Throwable t) { return 0; }
    }

    private Object[] matchBatch() {
        for (Object[] rec : BATCH) {
            boolean ok = true;
            for (Object[] spec : (Object[][]) rec[0]) {
                ItemStack st = getItem(inputStart + (Integer) spec[0]);
                int need = (Integer) spec[2];
                if (st.getCount() < need) { ok = false; break; }
            }
            if (ok) return rec;
        }
        return null;
    }

    private static ItemStack byId(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)));
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        boolean wantCraft = false;

        Object[] batch = matchBatch();
        if (batch != null) {
            wantCraft = true;
            if (burnTime > 0) {
                burnTime--;
                batchProgress++;
                if (batchProgress >= (Integer) batch[2]) {
                    ItemStack result = byId((String) batch[1]);
                    result.setCount((Integer) batch[2]);
                    if (!fitsOutput(result)) {            // 修复: 输出槽类型/容量校验, 避免吞产物或错误堆叠
                        batchProgress = (Integer) batch[2] - 1;
                        syncData();
                        return;
                    }
                    for (Object[] spec : (Object[][]) batch[0]) {
                        getItem(inputStart + (Integer) spec[0]).shrink((Integer) spec[2]);
                    }
                    ItemStack out = getItem(outputStart);
                    if (out.isEmpty()) setItem(outputStart, result);
                    else out.grow(result.getCount());
                    if (level instanceof ServerLevel sl && (Float) batch[3] > 0) {
                        net.minecraft.world.entity.ExperienceOrb.award(sl, new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5), (int) (float) (Float) batch[3]);
                    }
                    batchProgress = 0;
                }
            } else {
                refuel();
            }
        } else {
            batchProgress = 0;
            boolean any = false;
            // 单槽(槽0)兼容: 常规工业配方 / 原版熔炉/高炉配方
            ItemStack in = getItem(inputStart);
            SmeltResult r = findRecipe(0, in);
            if (r != null && !in.isEmpty()) {
                any = true;
                if (burnTime > 0 && hasRoomFor(r)) {
                    if (craftProgress[0] <= 0) craftDuration[0] = r.ticks();   // 修复: 此前从未设置 -> 每tick瞬间产出
                    burnTime--;
                    craftProgress[0]++;
                    if (craftProgress[0] >= craftDuration[0]) {
                        if (r.cryoliteCost() == 0) in.shrink(r.inputCount());
                        ItemStack out = getItem(outputStart);
                        if (out.isEmpty()) setItem(outputStart, r.result().copy());
                        else out.grow(r.result().getCount());
                        if (level instanceof ServerLevel sl && r.xp() > 0)
                            net.minecraft.world.entity.ExperienceOrb.award(sl, new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5), (int) r.xp());
                        craftProgress[0] = 0; craftDuration[0] = 0;
                    }
                } else if (burnTime <= 0) {
                    refuel();
                }
            } else {
                craftProgress[0] = 0; craftDuration[0] = 0;
            }
            if (!any && burnTime <= 0) { /* idle */ }
        }
        syncData();
    }

    /** 输出槽是否可以容纳该产物(空槽或同物品且不超堆叠上限) */
    private boolean fitsOutput(ItemStack result) {
        ItemStack out = getItem(outputStart);
        return out.isEmpty() || (out.is(result.getItem()) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private boolean hasRoomFor(SmeltResult r) {
        ItemStack out = getItem(outputStart);
        return out.isEmpty() || (out.is(r.result().getItem()) && out.getCount() + r.result().getCount() <= out.getMaxStackSize());
    }

    private void refuel() {
        ItemStack fuel = getItem(fuelSlot);
        if (!fuel.isEmpty() && isValidFuel(fuel)) {
            burnDuration = getFuelTime(fuel);
            burnTime = burnDuration;
            fuel.shrink(1);
        } else {
            burnDuration = 1; burnTime = 0;
        }
    }

    @Override
    public SmeltResult findRecipe(int inputIndex, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(EmpiresOfYoreMod.RAW_CHROMIUM.get())) return new SmeltResult(new ItemStack(EmpiresOfYoreMod.CHROMIUM_INGOT.get()), 360, 18f, 0, 1);
        if (stack.is(EmpiresOfYoreMod.DRAGON_REMAINS_ITEM.get())) return new SmeltResult(new ItemStack(EmpiresOfYoreMod.ENDITE_SCRAP.get()), 6000, 300f, 0, 1);
        if (stack.is(Items.COAL)) return new SmeltResult(new ItemStack(EmpiresOfYoreMod.COKE.get()), 200, 10f, 0, 1);
        if (level != null) {
            try {
                var opt1 = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level);
                if (opt1.isPresent() && opt1.get().value() instanceof AbstractCookingRecipe cr) {
                    return new SmeltResult(cr.getResultItem(RegistryAccess.EMPTY).copy(), cr.getCookingTime(), cr.getExperience(), 0, 1);
                }
                var opt2 = level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level);
                if (opt2.isPresent() && opt2.get().value() instanceof AbstractCookingRecipe cr) {
                    return new SmeltResult(cr.getResultItem(RegistryAccess.EMPTY).copy(), cr.getCookingTime(), cr.getExperience(), 0, 1);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

}
