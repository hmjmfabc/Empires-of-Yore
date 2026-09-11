package top.swordsman.empire.item;

import top.swordsman.empire.EmpiresOfYoreMod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;

public class TitaniteSwordItem extends SwordItem {
	private static final Tier TOOL_TIER = new Tier() {
		@Override
		public int getUses() {
			return 3939;
		}

		@Override
		public float getSpeed() {
			return 4f;
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
			return 27;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(new ItemStack(EmpiresOfYoreMod.TITANITE_INGOT.get()));
		}
	};

	public TitaniteSwordItem() {
		super(TOOL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(TOOL_TIER, 12f, 1f)).fireResistant());
	}
}