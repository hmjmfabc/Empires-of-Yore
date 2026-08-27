package top.swordsman.init;

import top.swordsman.SwordsmanMod;

import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.VillagerProfession;

public class SwordsmanTrades {
 public static void registerTrades(VillagerTradesEvent event) {
		if (event.getType() == VillagerProfession.TOOLSMITH) {
			event.getTrades().get(2).add(new BasicItemListing(new ItemStack(SwordsmanMod.CRYSTAL_CURRENCY.get(), 4), new ItemStack(SwordsmanMod.UPGRADE_TOOL.get()), 10, 5, 0.05f));
		}
	}
}
