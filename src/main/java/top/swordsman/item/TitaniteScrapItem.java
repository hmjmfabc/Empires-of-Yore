package top.swordsman.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class TitaniteScrapItem extends Item {
	public TitaniteScrapItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
	}
}