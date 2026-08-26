package top.swordsman.init;

import top.swordsman.SwordsmanMod;

import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;

public class SwordsmanFuels {
	@SubscribeEvent
	public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == SwordsmanMod.POWER_STONE.get())
			event.setBurnTime(1600);
	}
}
