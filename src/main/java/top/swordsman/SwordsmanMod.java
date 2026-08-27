package top.swordsman;

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import top.swordsman.feature.DeepslateLayerFeature;
import top.swordsman.init.SwordsmanFuels;
import top.swordsman.init.SwordsmanTrades;
import top.swordsman.machine.AbstractMachineBlockEntity;
import top.swordsman.machine.ElectrolyticCellBlockEntity;
import top.swordsman.machine.IndustrialOvenBlockEntity;
import top.swordsman.machine.AdvancedIndustrialOvenBlockEntity;
import top.swordsman.machine.MachineBlocks;
import top.swordsman.machine.MachineMenu;
import top.swordsman.item.CrystalCurrencyItem;
import top.swordsman.item.MikuItem;
import top.swordsman.item.PowerStoneItem;
import top.swordsman.item.TitaniteHoeItem;
import top.swordsman.item.TitaniteItem;
import top.swordsman.item.TitanitePickaxeItem;
import top.swordsman.item.TitaniteSwordItem;
import top.swordsman.item.UpgradeToolItem;

/**
 * 剑客群组服 (Swordsman) 主类 · 工业扩展版
 */
@Mod(SwordsmanMod.MODID)
public final class SwordsmanMod {

    public static final String MODID = "swordsman";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static Item.Properties base() { return new Item.Properties(); }
    private static BlockBehaviour.Properties blockProps(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(3.0f, 6.0f);
    }

    /* ====== 矿石方块 ====== */
    public static final DeferredBlock<Block> CRYOLITE = BLOCKS.register("cryolite",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(2, 6),
                    blockProps(MapColor.SNOW).lightLevel(s -> 15).sound(SoundType.GLASS).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> CRYOLITE_ITEM = ITEMS.registerSimpleBlockItem("cryolite", CRYOLITE);

    public static final DeferredBlock<Block> DRAGON_REMAINS = BLOCKS.register("dragon_remains",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7),
                    blockProps(MapColor.QUARTZ).strength(9.0f, 12.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> DRAGON_REMAINS_ITEM = ITEMS.registerSimpleBlockItem("dragon_remains", DRAGON_REMAINS);

    public static final DeferredBlock<Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), blockProps(MapColor.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_BAUXITE_ORE = BLOCKS.register("deepslate_bauxite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), blockProps(MapColor.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> CHROMIUM_ORE = BLOCKS.register("chromium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_CHROMIUM_ORE = BLOCKS.register("deepslate_chromium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> MAGNESIUM_ORE = BLOCKS.register("magnesium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), blockProps(MapColor.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_MAGNESIUM_ORE = BLOCKS.register("deepslate_magnesium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), blockProps(MapColor.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> ADVANCED_ENERGY_ORE = BLOCKS.register("advanced_energy_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> DEEPSLATE_ADVANCED_ENERGY_ORE = BLOCKS.register("deepslate_advanced_energy_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> NETHER_ADVANCED_ENERGY_ORE = BLOCKS.register("nether_advanced_energy_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.NETHER).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> END_ADVANCED_ENERGY_ORE = BLOCKS.register("end_advanced_energy_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.SAND).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.register("titanium_ore",
            () -> new top.swordsman.block.TitaniumOreBlock());
    public static final DeferredBlock<Block> DEEPSLATE_TITANIUM_ORE = BLOCKS.register("deepslate_titanium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> NETHER_TITANIUM_ORE = BLOCKS.register("nether_titanium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.NETHER).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> ENDER_TITANIUM_ORE = BLOCKS.register("ender_titanium_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), blockProps(MapColor.SAND).requiresCorrectToolForDrops()));

    /* ====== 石英(既有) ====== */
    public static final DeferredBlock<Block> OVERWORLD_QUARTZ_ORE = BLOCKS.register("overworld_quartz_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
    public static final DeferredItem<BlockItem> OVERWORLD_QUARTZ_ORE_ITEM = ITEMS.registerSimpleBlockItem("overworld_quartz_ore", OVERWORLD_QUARTZ_ORE);
    public static final DeferredBlock<Block> DEEPSLATE_QUARTZ_ORE = BLOCKS.register("deepslate_quartz_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
    public static final DeferredItem<BlockItem> DEEPSLATE_QUARTZ_ORE_ITEM = ITEMS.registerSimpleBlockItem("deepslate_quartz_ore", DEEPSLATE_QUARTZ_ORE);

    /* ====== 金属块 ====== */
    public static final DeferredBlock<Block> ALUMINUM_BLOCK = BLOCKS.register("aluminum_block", () -> new Block(blockProps(MapColor.SNOW)));
    public static final DeferredBlock<Block> TITANIUM_BLOCK = BLOCKS.register("titanium_block", () -> new Block(blockProps(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredBlock<Block> TITANITE_BLOCK = BLOCKS.register("titanite_block", () -> new Block(blockProps(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<Block> CHROMIUM_BLOCK = BLOCKS.register("chromium_block", () -> new Block(blockProps(MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<Block> RAW_MAGNESIUM_BLOCK = BLOCKS.register("raw_magnesium_block", () -> new Block(blockProps(MapColor.COLOR_GREEN)));
    public static final DeferredBlock<Block> CARBON_STEEL_BLOCK = BLOCKS.register("carbon_steel_block", () -> new Block(blockProps(MapColor.COLOR_BLACK)));
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("aluminum_block", ALUMINUM_BLOCK);
    public static final DeferredItem<BlockItem> TITANIUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("titanium_block", TITANIUM_BLOCK);
    public static final DeferredItem<BlockItem> TITANITE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("titanite_block", TITANITE_BLOCK);
    public static final DeferredItem<BlockItem> CHROMIUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("chromium_block", CHROMIUM_BLOCK);
    public static final DeferredItem<BlockItem> RAW_MAGNESIUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("raw_magnesium_block", RAW_MAGNESIUM_BLOCK);
    public static final DeferredItem<BlockItem> CARBON_STEEL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("carbon_steel_block", CARBON_STEEL_BLOCK);

    /* ====== 机器 ====== */
    public static final DeferredBlock<top.swordsman.machine.MachineBlock> INDUSTRIAL_OVEN_BLOCK = MachineBlocks.INDUSTRIAL_OVEN;
    public static final DeferredBlock<top.swordsman.machine.MachineBlock> ADVANCED_INDUSTRIAL_OVEN_BLOCK = MachineBlocks.ADVANCED_INDUSTRIAL_OVEN;
    public static final DeferredBlock<top.swordsman.machine.MachineBlock> ELECTROLYTIC_CELL_BLOCK = MachineBlocks.ELECTROLYTIC_CELL;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialOvenBlockEntity>> INDUSTRIAL_OVEN_BE = MachineBlocks.INDUSTRIAL_OVEN_BE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedIndustrialOvenBlockEntity>> ADVANCED_INDUSTRIAL_OVEN_BE = MachineBlocks.ADVANCED_INDUSTRIAL_OVEN_BE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyticCellBlockEntity>> ELECTROLYTIC_CELL_BE = MachineBlocks.ELECTROLYTIC_CELL_BE;

    public static final MenuType<MachineMenu> MACHINE_MENU = IMenuTypeExtension.create(
            (windowId, inv, buf) -> {
                net.minecraft.core.BlockPos pos = buf.readBlockPos();
                net.minecraft.world.level.block.entity.BlockEntity be = inv.player.level().getBlockEntity(pos);
                return new MachineMenu(windowId, inv, (AbstractMachineBlockEntity) be);
            });

    /* ====== 物品 ====== */
    public static final DeferredItem<PowerStoneItem> POWER_STONE = ITEMS.register("power_stone", PowerStoneItem::new);
    public static final DeferredItem<CrystalCurrencyItem> CRYSTAL_CURRENCY = ITEMS.register("crystal_currency", CrystalCurrencyItem::new);
    public static final DeferredItem<UpgradeToolItem> UPGRADE_TOOL = ITEMS.register("upgrade_tool", UpgradeToolItem::new);
    public static final DeferredItem<Item> TITANITE_INGOT = ITEMS.register("titanite_ingot", () -> new Item(base().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TITANITE_STICK = ITEMS.register("titanite_stick", () -> new Item(base().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TITANITE_SCRAP = ITEMS.register("titanite_scrap", () -> new Item(base().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> TITANITE_PICKAXE = ITEMS.register("titanite_pickaxe", TitanitePickaxeItem::new);
    public static final DeferredItem<Item> TITANITE_SWORD = ITEMS.register("titanite_sword", TitaniteSwordItem::new);
    public static final DeferredItem<Item> TITANITE_HOE = ITEMS.register("titanite_hoe", TitaniteHoeItem::new);
    public static final DeferredItem<Item> TITANITE_HELMET = ITEMS.register("titanite_helmet", TitaniteItem.Helmet::new);
    public static final DeferredItem<Item> TITANITE_CHESTPLATE = ITEMS.register("titanite_chestplate", TitaniteItem.Chestplate::new);
    public static final DeferredItem<Item> TITANITE_LEGGINGS = ITEMS.register("titanite_leggings", TitaniteItem.Leggings::new);
    public static final DeferredItem<Item> TITANITE_BOOTS = ITEMS.register("titanite_boots", TitaniteItem.Boots::new);
    public static final DeferredItem<Item> PROFESSIONAL_UPGRADE_TOOLS = ITEMS.register("professional_upgrade_tools", () -> new Item(base().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> ADVANCED_ENERGY_STONE = ITEMS.register("advanced_energy_stone", () -> new Item(base().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> CRYOLITE_POWDER = ITEMS.register("cryolite_powder", () -> new Item(base()));
    public static final DeferredItem<Item> BAUXITE_POWDER = ITEMS.register("bauxite_powder", () -> new Item(base()));
    public static final DeferredItem<Item> COKE = ITEMS.register("coke", () -> new Item(base()));
    public static final DeferredItem<Item> RAW_CHROMIUM = ITEMS.register("raw_chromium", () -> new Item(base()));
    public static final DeferredItem<Item> RAW_MAGNESIUM = ITEMS.register("raw_magnesium", () -> new Item(base()));
    public static final DeferredItem<Item> RAW_TITAMIUM = ITEMS.register("raw_titamium", () -> new Item(base()));
    public static final DeferredItem<Item> ENDITE_SCRAP = ITEMS.register("endite_scrap", () -> new Item(base().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot", () -> new Item(base()));
    public static final DeferredItem<Item> MAGNESIUM_INGOT = ITEMS.register("magnesium_ingot", () -> new Item(base()));
    public static final DeferredItem<Item> CHROMIUM_INGOT = ITEMS.register("chromium_ingot", () -> new Item(base()));
    public static final DeferredItem<Item> DURAALUMIN_INGOT = ITEMS.register("duraalumin_ingot", () -> new Item(base().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot", () -> new Item(base()));
    public static final DeferredItem<Item> CARBON_STEEL_INGOT = ITEMS.register("carbon_steel_ingot", () -> new Item(base()));
    public static final DeferredItem<Item> ENDITE_INGOT = ITEMS.register("endite_ingot", () -> new Item(base().rarity(Rarity.EPIC)));

    /* ====== 层级 ====== */
    public static final Tier CARBON_STEEL_TIER = new Tier() {
        public int getUses() { return 4200; }
        public float getSpeed() { return 6.5f; }
        public float getAttackDamageBonus() { return 0; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
        public int getEnchantmentValue() { return 14; }
        public Ingredient getRepairIngredient() { return Ingredient.of(CARBON_STEEL_INGOT.get()); }
    };
    public static final Tier DURAALUMIN_TIER = new Tier() {
        public int getUses() { return 3000; }
        public float getSpeed() { return 10.5f; }
        public float getAttackDamageBonus() { return 0; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "incorrect_for_duraalumin_tool")); }
        public int getEnchantmentValue() { return 24; }
        public Ingredient getRepairIngredient() { return Ingredient.of(DURAALUMIN_INGOT.get()); }
    };
    public static final Tier ENDITE_TIER = new Tier() {
        public int getUses() { return 3500; }
        public float getSpeed() { return 17.0f; }
        public float getAttackDamageBonus() { return 0; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "incorrect_for_endite_tool")); }
        public int getEnchantmentValue() { return 26; }
        public Ingredient getRepairIngredient() { return Ingredient.of(ENDITE_INGOT.get()); }
    };
    public static final Tier TITANITE_TIER = new Tier() {
        public int getUses() { return 3939; }
        public float getSpeed() { return 24f; }
        public float getAttackDamageBonus() { return 0; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "incorrect_for_titanite_tool")); }
        public int getEnchantmentValue() { return 30; }
        public Ingredient getRepairIngredient() { return Ingredient.of(TITANITE_INGOT.get()); }
    };

    public static Holder<ArmorMaterial> DURAALUMIN_MATERIAL;
    public static Holder<ArmorMaterial> ENDITE_MATERIAL;

    public static void registerArmorMaterials(RegisterEvent event) {
        event.register(Registries.ARMOR_MATERIAL, helper -> {
            DURAALUMIN_MATERIAL = holderOf(helper, Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 4); m.put(ArmorItem.Type.LEGGINGS, 8);
                m.put(ArmorItem.Type.CHESTPLATE, 11); m.put(ArmorItem.Type.HELMET, 4); m.put(ArmorItem.Type.BODY, 11);
            }), 20, "item.armor.equip_iron", () -> Ingredient.of(DURAALUMIN_INGOT.get()), new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "duraalumin")), 3.5f, 0.1f, "duraalumin");
            ENDITE_MATERIAL = holderOf(helper, Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 6); m.put(ArmorItem.Type.LEGGINGS, 11);
                m.put(ArmorItem.Type.CHESTPLATE, 15); m.put(ArmorItem.Type.HELMET, 6); m.put(ArmorItem.Type.BODY, 15);
            }), 25, "item.armor.equip_netherite", () -> Ingredient.of(ENDITE_INGOT.get()), new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "endite")), 3.8f, 0.3f, "endite");
        });
    }

    interface ArmorHolderHelper {
        void register(ResourceLocation id, ArmorMaterial material);
    }

    private static Holder<ArmorMaterial> holderOf(RegisterEvent.RegisterHelper<ArmorMaterial> helper,
            EnumMap<ArmorItem.Type, Integer> def, int enchant, String sound, java.util.function.Supplier<Ingredient> repair,
            ArmorMaterial.Layer layer, float toughness, float kb, String id) {
        ArmorMaterial mat = new ArmorMaterial(def, enchant,
                DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse(sound)), repair, List.of(layer), toughness, kb);
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, id);
        helper.register(loc, mat);
        return net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(mat);
    }

    /* ====== 硬铝/末影/碳钢 工具与护甲 ====== */
    private static Item.Properties toolProps(Tier t, int dur, float dmg, float spd, boolean armor) {
        Item.Properties p = base().durability(dur);
        if (armor) return p;
        return p.attributes(DiggerItem.createAttributes(t, dmg, spd)).fireResistant();
    }

    public static final DeferredItem<Item> DURAALUMIN_PICKAXE = ITEMS.register("duraalumin_pickaxe", () -> new PickaxeItem(DURAALUMIN_TIER, toolProps(DURAALUMIN_TIER, 3000, 5f, -2.6f, false)));
    public static final DeferredItem<Item> DURAALUMIN_SWORD = ITEMS.register("duraalumin_sword", () -> new SwordItem(DURAALUMIN_TIER, base().durability(2600).attributes(SwordItem.createAttributes(DURAALUMIN_TIER, 5f, -2.4f)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_AXE = ITEMS.register("duraalumin_axe", () -> new AxeItem(DURAALUMIN_TIER, base().durability(2800).attributes(DiggerItem.createAttributes(DURAALUMIN_TIER, 7f, -2.8f)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_SHOVEL = ITEMS.register("duraalumin_shovel", () -> new ShovelItem(DURAALUMIN_TIER, base().durability(2800).attributes(DiggerItem.createAttributes(DURAALUMIN_TIER, 3f, -2.8f)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_HOE = ITEMS.register("duraalumin_hoe", () -> new HoeItem(DURAALUMIN_TIER, base().durability(3000).attributes(DiggerItem.createAttributes(DURAALUMIN_TIER, 2f, -2.5f)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_HELMET = ITEMS.register("duraalumin_helmet", () -> new ArmorItem(DURAALUMIN_MATERIAL, ArmorItem.Type.HELMET, base().durability(ArmorItem.Type.HELMET.getDurability(50)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_CHESTPLATE = ITEMS.register("duraalumin_chestplate", () -> new ArmorItem(DURAALUMIN_MATERIAL, ArmorItem.Type.CHESTPLATE, base().durability(ArmorItem.Type.CHESTPLATE.getDurability(50)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_LEGGINGS = ITEMS.register("duraalumin_leggings", () -> new ArmorItem(DURAALUMIN_MATERIAL, ArmorItem.Type.LEGGINGS, base().durability(ArmorItem.Type.LEGGINGS.getDurability(50)).fireResistant()));
    public static final DeferredItem<Item> DURAALUMIN_BOOTS = ITEMS.register("duraalumin_boots", () -> new ArmorItem(DURAALUMIN_MATERIAL, ArmorItem.Type.BOOTS, base().durability(ArmorItem.Type.BOOTS.getDurability(50)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_PICKAXE = ITEMS.register("endite_pickaxe", () -> new PickaxeItem(ENDITE_TIER, toolProps(ENDITE_TIER, 3500, 6f, -2.5f, false)));
    public static final DeferredItem<Item> ENDITE_SWORD = ITEMS.register("endite_sword", () -> new SwordItem(ENDITE_TIER, base().durability(3200).attributes(SwordItem.createAttributes(ENDITE_TIER, 6f, -2.3f)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_AXE = ITEMS.register("endite_axe", () -> new AxeItem(ENDITE_TIER, base().durability(3300).attributes(DiggerItem.createAttributes(ENDITE_TIER, 8f, -2.7f)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_SHOVEL = ITEMS.register("endite_shovel", () -> new ShovelItem(ENDITE_TIER, base().durability(3300).attributes(DiggerItem.createAttributes(ENDITE_TIER, 4f, -2.7f)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_HOE = ITEMS.register("endite_hoe", () -> new HoeItem(ENDITE_TIER, base().durability(3500).attributes(DiggerItem.createAttributes(ENDITE_TIER, 3f, -2.4f)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_HELMET = ITEMS.register("endite_helmet", () -> new ArmorItem(ENDITE_MATERIAL, ArmorItem.Type.HELMET, base().durability(ArmorItem.Type.HELMET.getDurability(58)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_CHESTPLATE = ITEMS.register("endite_chestplate", () -> new ArmorItem(ENDITE_MATERIAL, ArmorItem.Type.CHESTPLATE, base().durability(ArmorItem.Type.CHESTPLATE.getDurability(58)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_LEGGINGS = ITEMS.register("endite_leggings", () -> new ArmorItem(ENDITE_MATERIAL, ArmorItem.Type.LEGGINGS, base().durability(ArmorItem.Type.LEGGINGS.getDurability(58)).fireResistant()));
    public static final DeferredItem<Item> ENDITE_BOOTS = ITEMS.register("endite_boots", () -> new ArmorItem(ENDITE_MATERIAL, ArmorItem.Type.BOOTS, base().durability(ArmorItem.Type.BOOTS.getDurability(58)).fireResistant()));
    public static final DeferredItem<Item> CARBON_STEEL_PICKAXE = ITEMS.register("carbon_steel_pickaxe", () -> new PickaxeItem(CARBON_STEEL_TIER, base().durability(4200).attributes(DiggerItem.createAttributes(CARBON_STEEL_TIER, 3.0f, -2.8f))));
    public static final DeferredItem<Item> CARBON_STEEL_SWORD = ITEMS.register("carbon_steel_sword", () -> new SwordItem(CARBON_STEEL_TIER, base().durability(1499).attributes(SwordItem.createAttributes(CARBON_STEEL_TIER, 2f, -2.6f))));
    public static final DeferredItem<Item> CARBON_STEEL_AXE = ITEMS.register("carbon_steel_axe", () -> new AxeItem(CARBON_STEEL_TIER, base().durability(1299).attributes(DiggerItem.createAttributes(CARBON_STEEL_TIER, 6f, -3.4f))));
    public static final DeferredItem<Item> CARBON_STEEL_SHOVEL = ITEMS.register("carbon_steel_shovel", () -> new ShovelItem(CARBON_STEEL_TIER, base().durability(1899).attributes(DiggerItem.createAttributes(CARBON_STEEL_TIER, 1f, -3.0f))));
    public static final DeferredItem<Item> CARBON_STEEL_HOE = ITEMS.register("carbon_steel_hoe", () -> new HoeItem(CARBON_STEEL_TIER, base().durability(2699).attributes(DiggerItem.createAttributes(CARBON_STEEL_TIER, 0f, -3.2f))));
    public static final DeferredItem<Item> TITANITE_AXE = ITEMS.register("titanite_axe", () -> new AxeItem(TITANITE_TIER, base().durability(3939).attributes(DiggerItem.createAttributes(TITANITE_TIER, 9f, -2.6f)).fireResistant()));
    public static final DeferredItem<Item> TITANITE_SHOVEL = ITEMS.register("titanite_shovel", () -> new ShovelItem(TITANITE_TIER, base().durability(3939).attributes(DiggerItem.createAttributes(TITANITE_TIER, 5f, -2.6f)).fireResistant()));

    /* ====== 方块物品(矿石/金属块/机器) ====== */
    public static final DeferredBlock<Block> POWER_STONE_ORE = BLOCKS.register("power_stone_ore", top.swordsman.block.PowerStoneOreBlock::new);
    public static final DeferredItem<BlockItem> POWER_STONE_ORE_ITEM = ITEMS.registerSimpleBlockItem("power_stone_ore", POWER_STONE_ORE);
    public static final DeferredItem<BlockItem> TITANIUM_ORE_ITEM = ITEMS.registerSimpleBlockItem("titanium_ore", TITANIUM_ORE, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRYOLITE_BLOCK_ITEM = CRYOLITE_ITEM;
    public static final DeferredItem<BlockItem> DRAGON_BLOCK_ITEM = DRAGON_REMAINS_ITEM;

    /* ====== 特性(既有) ====== */
    public static final DeferredHolder<Feature<?>, Feature<DeepslateLayerFeature.Config>> DEEPSLATE_LAYER =
            FEATURES.register("deepslate_layer", () -> new DeepslateLayerFeature(DeepslateLayerFeature.Config.CODEC));

    /* ====== 构造 ====== */
    public SwordsmanMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FEATURES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(SwordsmanMod::addCreative);
        modEventBus.addListener(SwordsmanMod::registerArmorMaterials);
        modEventBus.addListener(TitaniteItem::registerArmorMaterial);
        modEventBus.addListener(MikuItem::registerArmorMaterial);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(SwordsmanFuels::furnaceFuelBurnTimeEvent);
        NeoForge.EVENT_BUS.addListener(SwordsmanTrades::registerTrades);
        LOGGER.info("[Swordsman] 剑客群组服(SGU) custom mod loaded.");
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (Item i : new Item[]{POWER_STONE.get(), ADVANCED_ENERGY_STONE.get(), CRYSTAL_CURRENCY.get(), COKE.get(),
                    RAW_CHROMIUM.get(), RAW_MAGNESIUM.get(), RAW_TITAMIUM.get(), CRYOLITE_POWDER.get(), BAUXITE_POWDER.get(),
                    ALUMINUM_INGOT.get(), MAGNESIUM_INGOT.get(), CHROMIUM_INGOT.get(), TITANIUM_INGOT.get(), CARBON_STEEL_INGOT.get(),
                    DURAALUMIN_INGOT.get(), ENDITE_INGOT.get(), TITANITE_INGOT.get(), ENDITE_SCRAP.get(), UPGRADE_TOOL.get(), PROFESSIONAL_UPGRADE_TOOLS.get()})
                event.accept(i);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            for (Block b : new Block[]{BAUXITE_ORE.get(), DEEPSLATE_BAUXITE_ORE.get(), CHROMIUM_ORE.get(), DEEPSLATE_CHROMIUM_ORE.get(),
                    MAGNESIUM_ORE.get(), DEEPSLATE_MAGNESIUM_ORE.get(), ADVANCED_ENERGY_ORE.get(), DEEPSLATE_ADVANCED_ENERGY_ORE.get(),
                    NETHER_ADVANCED_ENERGY_ORE.get(), END_ADVANCED_ENERGY_ORE.get(), CRYOLITE.get(), POWER_STONE_ORE.get(),
                    TITANIUM_ORE.get()})
                event.accept(b.asItem());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (Item i : new Item[]{TITANITE_PICKAXE.get(), TITANITE_AXE.get(), TITANITE_SHOVEL.get(), TITANITE_HOE.get(),
                    DURAALUMIN_PICKAXE.get(), DURAALUMIN_AXE.get(), DURAALUMIN_SHOVEL.get(), DURAALUMIN_HOE.get(),
                    ENDITE_PICKAXE.get(), ENDITE_AXE.get(), ENDITE_SHOVEL.get(), ENDITE_HOE.get(),
                    CARBON_STEEL_PICKAXE.get(), CARBON_STEEL_AXE.get(), CARBON_STEEL_SHOVEL.get(), CARBON_STEEL_HOE.get()})
                event.accept(i);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            for (Item i : new Item[]{TITANITE_SWORD.get(), DURAALUMIN_SWORD.get(), ENDITE_SWORD.get(), CARBON_STEEL_SWORD.get(),
                    TITANITE_HELMET.get(), TITANITE_CHESTPLATE.get(), TITANITE_LEGGINGS.get(), TITANITE_BOOTS.get(),
                    DURAALUMIN_HELMET.get(), DURAALUMIN_CHESTPLATE.get(), DURAALUMIN_LEGGINGS.get(), DURAALUMIN_BOOTS.get(),
                    ENDITE_HELMET.get(), ENDITE_CHESTPLATE.get(), ENDITE_LEGGINGS.get(), ENDITE_BOOTS.get()})
                event.accept(i);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (Block b : new Block[]{ALUMINUM_BLOCK.get(), TITANIUM_BLOCK.get(), TITANITE_BLOCK.get(), CHROMIUM_BLOCK.get(),
                    RAW_MAGNESIUM_BLOCK.get(), CARBON_STEEL_BLOCK.get(), INDUSTRIAL_OVEN_BLOCK.get(), ADVANCED_INDUSTRIAL_OVEN_BLOCK.get(), ELECTROLYTIC_CELL_BLOCK.get()})
                event.accept(b.asItem());
        }
    }

    public static boolean isMiningDimension(net.minecraft.world.level.Level level) {
        return level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath(MODID, "mining"));
    }

    /* ====== 事件: 龙骸 / 奉献恒心 ====== */
    @SubscribeEvent
    public void onDragonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || dragon.level().isClientSide) return;
        dragon.spawnAtLocation(DRAGON_REMAINS.get());
    }

    @SubscribeEvent
    public void onItemDestroyed(PlayerDestroyItemEvent event) {
        Player p = event.getEntity();
        if (p == null || p.level().isClientSide) return;
        ItemStack original = event.getOriginal();
        if (original != null && original.is(CARBON_STEEL_HOE.get()) && p instanceof net.minecraft.server.level.ServerPlayer sp) {
            var adv = sp.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("swordsman", "dedicated_heart"));
            if (adv != null) sp.getAdvancements().award(adv, "auto");
        }
    }

    /* ====== 服务器任务队列 ====== */
    private static final Queue<IntObjectPair<Runnable>> WORK_TO_BE_SCHEDULED = new ConcurrentLinkedQueue<>();
    private static final PriorityQueue<TickTask> WORK_QUEUE = new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

    public static void queueServerWork(int delay, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            WORK_TO_BE_SCHEDULED.add(new IntObjectImmutablePair<>(delay, action));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int currentTick = event.getServer().getTickCount();
        IntObjectPair<Runnable> work;
        while ((work = WORK_TO_BE_SCHEDULED.poll()) != null) WORK_QUEUE.add(new TickTask(currentTick + work.leftInt(), work.right()));
        while (!WORK_QUEUE.isEmpty() && currentTick >= WORK_QUEUE.peek().getTick()) WORK_QUEUE.poll().run();
    }
}
