package top.swordsman.empire.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import top.swordsman.empire.EmpiresOfYoreMod;

/**
 * ⑯ 自定义药水的"任何宝箱小概率产出"：
 * 所有战利品表 id 形如 {@code <ns>:chests/...} 的箱子(含原版与模组)都有 12% 概率多出一瓶随机自定义药水。
 */
public class ChestPotionLootModifier extends LootModifier {

    public static final MapCodec<ChestPotionLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, ChestPotionLootModifier::new));

    private static final float CHANCE = 0.12f;

    public ChestPotionLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> doApply(
            it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        String path = context.getQueriedLootTableId().getPath();
        if (!path.startsWith("chests/")) {
            return generatedLoot;
        }
        if (context.getRandom().nextFloat() >= CHANCE) {
            return generatedLoot;
        }
        generatedLoot.add(randomPotion(context));
        return generatedLoot;
    }

    private static ItemStack randomPotion(LootContext context) {
        var potions = new net.neoforged.neoforge.registries.DeferredHolder[]{
                EmpiresOfYoreMod.POTION_ANGER, EmpiresOfYoreMod.POTION_CALM, EmpiresOfYoreMod.POTION_DIVINITY,
                EmpiresOfYoreMod.POTION_INCORPOREAL, EmpiresOfYoreMod.POTION_UNDEAD_FORM, EmpiresOfYoreMod.POTION_ENDER_FORM,
                EmpiresOfYoreMod.POTION_ARTIFACT, EmpiresOfYoreMod.POTION_CUT_RATIONS, EmpiresOfYoreMod.POTION_INDULGENCE,
                EmpiresOfYoreMod.POTION_LIGHTNING
        };
        var holder = (net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>)
                potions[context.getRandom().nextInt(potions.length)];
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new PotionContents(holder));
        return stack;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
