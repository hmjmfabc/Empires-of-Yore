package top.swordsman.empire.item;

import top.swordsman.empire.EmpiresOfYoreMod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.*;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

public class TitaniteHoeItem extends HoeItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 23999;
		}

		@Override
		public float getSpeed() {
			return 72f;
		}

		@Override
		public float getAttackDamageBonus() {
			return 5f;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "incorrect_for_titanite_tool"));
		}

		@Override
		public int getEnchantmentValue() {
			return 32;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(EmpiresOfYoreMod.TITANITE_INGOT.get()));
		}
	};

	public TitaniteHoeItem() {
		super(TOOL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TOOL_TIER, 3f, -3f)).rarity(Rarity.RARE).fireResistant());
	}
}