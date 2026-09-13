package top.swordsman.empire.item;

import java.util.function.Supplier;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 通用自定义材质(工具等级)。
 * 注意: 原版 TieredItem 会用 {@code tier.getUses()} 覆盖物品属性里的耐久,
 * 因此"每件装备不同耐久"必须使用独立的 Tier 实例(青铜五件套即如此)。
 */
public record EmpireTier(int uses, float speed, float damageBonus, int enchantValue,
                         TagKey<Block> incorrectBlocksForDrops, Supplier<Item> repairItem) implements Tier {

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocksForDrops;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairItem == null ? Ingredient.of() : Ingredient.of(repairItem.get());
    }
}
