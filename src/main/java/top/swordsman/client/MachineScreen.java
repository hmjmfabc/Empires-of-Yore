package top.swordsman.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import top.swordsman.machine.MachineMenu;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int type = menu.getMachineType();
        // 燃料火焰
        int burn = menu.getBlockEntity().get(0);
        int burnDur = menu.getBlockEntity().get(1);
        if (burnDur > 0 && burn > 0) {
            int f = Math.min(12, burn * 12 / burnDur);
            g.fill(relX + 57, relY + 55 - f, relX + 65, relY + 55, 0xFFFF8C00);
        }
        // 进度箭头
        for (int i = 0; i < menu.getBlockEntity().getInputCount(); i++) {
            int prog = menu.getBlockEntity().get(2 + i);
            int dur = menu.getBlockEntity().get(8 + i);
            int pct = dur <= 0 ? 0 : prog * 24 / dur;
            int x = relX + 71 + i * 22 - (menu.getBlockEntity().getInputCount() > 1 ? (menu.getBlockEntity().getInputCount() - 1) * 11 : 0) + 14;
            g.fill(x - 12, relY + 32, x, relY + 38, 0xFF8C2F2F);
            g.fill(x - 12, relY + 32, x - 12 + pct, relY + 38, 0xFFFF5040);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
