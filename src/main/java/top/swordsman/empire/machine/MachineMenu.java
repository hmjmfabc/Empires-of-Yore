package top.swordsman.empire.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import top.swordsman.empire.EmpiresOfYoreMod;

public class MachineMenu extends AbstractContainerMenu {
    private final AbstractMachineBlockEntity be;
    private final int machineType;

    public MachineMenu(int id, Inventory inv, AbstractMachineBlockEntity be) {
        super(EmpiresOfYoreMod.MACHINE_MENU, id);
        this.be = be;
        this.machineType = be.getMachineType();
        addDataSlots(be);
        int idx = 0;
        addSlot(new Slot(be, be.getFuelSlot(), 62, 53)); idx++;
        if (be.getReagentSlot() >= 0) addSlot(new Slot(be, be.getReagentSlot(), 40, 36)); idx++;
        for (int i = 0; i < be.getInputCount(); i++) {
            int x = 71 + i * 22 - (be.getInputCount() > 1 ? (be.getInputCount() - 1) * 11 : 0);
            addSlot(new Slot(be, be.getInputStart() + i, x, 26));
        }
        for (int i = 0; i < be.getOutputCount(); i++) {
            addSlot(new Slot(be, be.getOutputStart() + i, 116, 35));
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack s = slot.getItem();
            stack = s.copy();
            if (index < be.getContainerSize()) {
                if (!moveItemStackTo(s, be.getContainerSize(), slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(s, 0, be.getContainerSize(), false)) return ItemStack.EMPTY;
            if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }

    @Override public boolean stillValid(Player player) { return be.stillValid(player); }

    public AbstractMachineBlockEntity getBlockEntity() { return be; }
    public int getMachineType() { return machineType; }
}
