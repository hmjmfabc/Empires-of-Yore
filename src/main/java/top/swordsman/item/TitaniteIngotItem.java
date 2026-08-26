package top.swordsman.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class TitaniteIngotItem extends Item {
	public TitaniteIngotItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.RARE));
	}
}