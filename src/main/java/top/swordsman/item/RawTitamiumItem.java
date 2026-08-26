package top.swordsman.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class RawTitamiumItem extends Item {
	public RawTitamiumItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
	}
}