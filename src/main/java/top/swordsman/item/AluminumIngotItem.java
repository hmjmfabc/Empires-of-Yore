package top.swordsman.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class AluminumIngotItem extends Item {
	public AluminumIngotItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
	}
}