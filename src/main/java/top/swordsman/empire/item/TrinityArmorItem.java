package top.swordsman.empire.item;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import top.swordsman.empire.EmpiresOfYoreMod;

/**
 * ⑰ 三界钛合金护甲：数值 = 钛合金护甲 × 1.1（取整）。
 * 防御 8/14/18 → 9/15/20，韧性 4→4.4，击退抗性 0.3→0.33，附魔性 28→31，耐久系数 68→75。
 */
public abstract class TrinityArmorItem extends ArmorItem {

    public static Holder<ArmorMaterial> ARMOR_MATERIAL = null;

    public static void registerArmorMaterial(RegisterEvent event) {
        event.register(Registries.ARMOR_MATERIAL, registerHelper -> {
            ArmorMaterial material = new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 9);
                        map.put(ArmorItem.Type.LEGGINGS, 15);
                        map.put(ArmorItem.Type.CHESTPLATE, 20);
                        map.put(ArmorItem.Type.HELMET, 9);
                        map.put(ArmorItem.Type.BODY, 20);
                    }),
                    31,
                    DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_netherite")),
                    () -> Ingredient.of(new ItemStack(EmpiresOfYoreMod.TRINITY_INGOT.get())),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(EmpiresOfYoreMod.MODID, "trinity"))),
                    4.4f, 0.33f);
            registerHelper.register(ResourceLocation.fromNamespaceAndPath(EmpiresOfYoreMod.MODID, "trinity"), material);
            ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(material);
        });
    }

    /** 高等级护甲装备时不再触发"被攻击"以外的特殊逻辑，仅提供数值。 */
    protected TrinityArmorItem(ArmorItem.Type type, Item.Properties properties) {
        super(ARMOR_MATERIAL, type, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static class Helmet extends TrinityArmorItem {
        public Helmet() {
            super(ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(75)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        }
    }

    public static class Chestplate extends TrinityArmorItem {
        public Chestplate() {
            super(ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(75)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        }
    }

    public static class Leggings extends TrinityArmorItem {
        public Leggings() {
            super(ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(75)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        }
    }

    public static class Boots extends TrinityArmorItem {
        public Boots() {
            super(ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(75)).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        }
    }
}
