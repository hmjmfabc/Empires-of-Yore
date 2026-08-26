package top.swordsman.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class TitaniteStickItem extends Item {
	public TitaniteStickItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.RARE));
	}
}