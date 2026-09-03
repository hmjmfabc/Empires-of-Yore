package top.swordsman.empire.init;

import top.swordsman.empire.EmpiresOfYoreMod;

import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;

public class SwordsmanFuels {
 public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == EmpiresOfYoreMod.POWER_STONE.get())
			event.setBurnTime(1600);
	}
}
