package top.swordsman.empire.enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import top.swordsman.empire.EmpiresOfYoreMod;

/** 自定义附魔的引用与等级查询。 */
public final class EmpireEnchantments {

    public static final ResourceKey<Enchantment> SATIATED_MENDING = key("satiated_mending");
    public static final ResourceKey<Enchantment> VIGOR = key("vigor");
    public static final ResourceKey<Enchantment> LIANG_WENFENG_BLESSING = key("liang_wenfeng_blessing");
    public static final ResourceKey<Enchantment> HEAVY_BLADE = key("heavy_blade");
    public static final ResourceKey<Enchantment> FROST_STRIDE = key("frost_stride");

    private EmpireEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(EmpiresOfYoreMod.MODID, name));
    }

    /** 读取物品上该附魔的等级(无则 0)。 */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(enchantment).orElse(null);
        if (holder == null) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
}
