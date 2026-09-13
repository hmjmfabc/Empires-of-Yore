package top.swordsman.empire.init;

import top.swordsman.empire.EmpiresOfYoreMod;

import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;

public class EmpireFuels {
 public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == EmpiresOfYoreMod.POWER_STONE.get())
			event.setBurnTime(1600);
		else if (itemstack.getItem() == EmpiresOfYoreMod.ADVANCED_ENERGY_STONE.get())
			event.setBurnTime(3200);
	}
}
