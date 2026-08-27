package top.swordsman.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements Container, ContainerData {
    protected final NonNullList<ItemStack> items;
    protected final int fuelSlot;
    protected final int reagentSlot; // -1 = 无
    protected final int inputStart, inputCount, outputStart, outputCount;
    protected int burnTime = 0;
    protected int burnDuration = 1;
    protected int[] craftProgress;
    protected int[] craftDuration;

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         int fuelSlot, int reagentSlot, int inputCount, int outputCount) {
        super(type, pos, state);
        this.fuelSlot = fuelSlot;
        this.reagentSlot = reagentSlot;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.inputStart = inputCount > 0 ? (fuelSlot + 1) : 0;
        this.outputStart = inputStart + inputCount;
        this.items = NonNullList.withSize(outputStart + outputCount + (reagentSlot >= 0 ? 1 : 0), ItemStack.EMPTY);
        this.craftProgress = new int[inputCount];
        this.craftDuration = new int[inputCount];
    }

    /* ---- 打开配置 ---- */
    public abstract int getMachineType();          // 0=电解炉 1=工业熔炉 2=高级工业熔炉
    public abstract boolean isValidFuel(ItemStack stack);
    public abstract int getFuelTime(ItemStack stack);
    public abstract SmeltResult findRecipe(int inputIndex, ItemStack stack);
    /** 每燃烧 tick 的进度倍率 (电解镁=2, 缺冰晶粉铝=4) */
    public float progressPerBurnTick(int inputIndex, ItemStack input) { return 1.0f; }

    public record SmeltResult(ItemStack result, int ticks, float xp, int cryoliteCost, int inputCount) {}

    /* ---- 逻辑 ---- */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        boolean active = false;
        for (int i = 0; i < inputCount; i++) {
            ItemStack in = items.get(inputStart + i);
            if (in.isEmpty() || craftProgress[i] <= 0 && findRecipe(i, in) == null && !startRecipe(i, in)) continue;
            SmeltResult r = findRecipe(i, items.get(inputStart + i));
            if (r == null) { craftProgress[i] = 0; craftDuration[i] = 0; continue; }
            active = true;
            if (burnTime <= 0) continue;
            if (craftProgress[i] == 0 && craftDuration[i] == 0) {
                craftDuration[i] = r.ticks();
                craftProgress[i] = 1;
            }
            if (burnTime > 0 && hasRoom(i, r)) {
                burnTime--;
                float prog = progressPerBurnTick(i, items.get(inputStart + i));
                craftProgress[i] += Math.max(1, Math.round(prog));
                if (craftProgress[i] >= craftDuration[i]) {
                    finish(i, r);
                }
            }
        }
        // 燃料补充
        if (burnTime <= 0) {
            for (int i = 0; i < inputCount; i++) {
                if (isCrafting(i)) { burnTimerRefill(i); break; }
            }
        }
        syncData();
    }

    private boolean isCrafting(int i) {
        ItemStack in = items.get(inputStart + i);
        return !in.isEmpty() && ((craftProgress[i] > 0) || findRecipe(i, in) != null);
    }

    private void burnTimerRefill(int inputIdx) {
        ItemStack fuel = items.get(fuelSlot);
        if (!fuel.isEmpty() && isValidFuel(fuel)) {
            burnDuration = getFuelTime(fuel);
            burnTime = burnDuration;
            fuel.shrink(1);
        } else {
            burnDuration = 1; burnTime = 0;
        }
    }

    private boolean startRecipe(int idx, ItemStack in) {
        SmeltResult r = findRecipe(idx, in);
        if (r == null) return false;
        craftDuration[idx] = r.ticks();
        craftProgress[idx] = 1;
        return true;
    }

    private boolean hasRoom(int idx, SmeltResult r) {
        ItemStack out = items.get(outputStart);
        if (out.isEmpty()) return true;
        return out.is(r.result().getItem()) && out.getCount() + r.result().getCount() <= out.getMaxStackSize();
    }

    private void finish(int idx, SmeltResult r) {
        ItemStack in = items.get(inputStart + idx);
        int need = r.inputCount();
        for (int k = 0; k < need; k++) in.shrink(1);
        if (r.cryoliteCost() > 0 && reagentSlot >= 0) {
            ItemStack rea = items.get(reagentSlot);
            if (!rea.isEmpty()) rea.shrink(r.cryoliteCost());
        }
        ItemStack out = items.get(outputStart);
        if (out.isEmpty()) items.set(outputStart, r.result().copy());
        else out.grow(r.result().getCount());
        if (level instanceof ServerLevel sl && r.xp() > 0) {
            net.minecraft.world.entity.ExperienceOrb.award(sl, new net.minecraft.world.phys.Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5), (int) r.xp());
        }
        craftProgress[idx] = 0; craftDuration[idx] = 0;
    }

    protected void syncData() { setChanged(); }

    /* ---- Container ---- */
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int pSlot) { return items.get(pSlot); }
    @Override public ItemStack removeItem(int pSlot, int pAmount) { return ContainerHelper.removeItem(items, pSlot, pAmount); }
    @Override public ItemStack removeItemNoUpdate(int pSlot) { return ContainerHelper.takeItem(items, pSlot); }
    @Override public void setItem(int pSlot, ItemStack pStack) { items.set(pSlot, pStack); syncData(); }
    public void setChanged() { super.setChanged(); }
    @Override public boolean stillValid(Player p) { return p.level().getBlockEntity(worldPosition) == this && p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64; }
    @Override public void clearContent() { items.clear(); }

    /* ---- ContainerData ---- */
    @Override public int get(int id) {
        if (id == 0) return burnTime;
        if (id == 1) return burnDuration;
        if (id >= 2 && id < 2 + craftProgress.length) return craftProgress[id - 2];
        if (id >= 8 && id < 8 + craftDuration.length) return craftDuration[id - 8];
        return 0;
    }
    @Override public void set(int id, int v) {
        if (id == 0) burnTime = v;
        else if (id == 1) burnDuration = v;
        else if (id >= 2 && id < 2 + craftProgress.length) craftProgress[id - 2] = v;
        else if (id >= 8 && id < 8 + craftDuration.length) craftDuration[id - 8] = v;
    }
    @Override public int getCount() { return 16; }

    /* ---- 存取 ---- */
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnDuration", burnDuration);
        tag.putIntArray("craft", craftProgress);
        tag.putIntArray("craftDur", craftDuration);
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        burnTime = tag.getInt("burnTime");
        burnDuration = tag.getInt("burnDuration");
        int[] cp = tag.getIntArray("craft");
        int[] cd = tag.getIntArray("craftDur");
        if (cp.length == craftProgress.length) craftProgress = cp;
        if (cd.length == craftDuration.length) craftDuration = cd;
    }

    public int getFuelSlot() { return fuelSlot; }
    public int getReagentSlot() { return reagentSlot; }
    public int getInputStart() { return inputStart; }
    public int getInputCount() { return inputCount; }
    public int getOutputStart() { return outputStart; }
    public int getOutputCount() { return outputCount; }
}
