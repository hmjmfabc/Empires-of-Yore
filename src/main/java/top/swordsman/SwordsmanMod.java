package top.swordsman;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import top.swordsman.block.AluminumOreBlock;
import top.swordsman.block.PowerStoneOreBlock;
import top.swordsman.block.TitaniumOreBlock;
import top.swordsman.feature.DeepslateLayerFeature;
import top.swordsman.init.SwordsmanFuels;
import top.swordsman.init.SwordsmanTrades;
import top.swordsman.item.AluminumIngotItem;
import top.swordsman.item.CrystalCurrencyItem;
import top.swordsman.item.MikuItem;
import top.swordsman.item.PowerStoneItem;
import top.swordsman.item.RawAluminumItem;
import top.swordsman.item.RawTitamiumItem;
import top.swordsman.item.TitaniteHoeItem;
import top.swordsman.item.TitaniteIngotItem;
import top.swordsman.item.TitaniteItem;
import top.swordsman.item.TitanitePickaxeItem;
import top.swordsman.item.TitaniteScrapItem;
import top.swordsman.item.TitaniteStickItem;
import top.swordsman.item.TitaniteSwordItem;
import top.swordsman.item.TitaniumDustItem;
import top.swordsman.item.UpgradeToolItem;

/**
 * 剑客群组服 (Swordsman Group Server) 定制模组。
 * <p>
 * 内容：剑客群组服定制玩法（战利品表/进度/自定义维度 swordsman:mining、石英矿石），
 * 以及原 HCE 服务器科技模组全部内容（能源石、钛、铝合金、钛合金装备、初音未来护甲、
 * 外星结构等），均已合并至 swordsman 命名空间。
 */
@Mod(SwordsmanMod.MODID)
public final class SwordsmanMod {

    public static final String MODID = "swordsman";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ---- 石英矿石（与下界石英矿石完全相同，仅贴图不同）----
    public static final DeferredBlock<Block> OVERWORLD_QUARTZ_ORE = BLOCKS.register(
            "overworld_quartz_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)));
    public static final DeferredItem<BlockItem> OVERWORLD_QUARTZ_ORE_ITEM = ITEMS
            .registerSimpleBlockItem("overworld_quartz_ore", OVERWORLD_QUARTZ_ORE);

    public static final DeferredBlock<Block> DEEPSLATE_QUARTZ_ORE = BLOCKS.register(
            "deepslate_quartz_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)));
    public static final DeferredItem<BlockItem> DEEPSLATE_QUARTZ_ORE_ITEM = ITEMS
            .registerSimpleBlockItem("deepslate_quartz_ore", DEEPSLATE_QUARTZ_ORE);

    // ---- 深板岩层替换特性（用于 swordsman:mining：256 层以下石→深板岩）----
    public static final DeferredHolder<Feature<?>, Feature<DeepslateLayerFeature.Config>>
            DEEPSLATE_LAYER = FEATURES.register(
                    "deepslate_layer",
                    () -> new DeepslateLayerFeature(DeepslateLayerFeature.Config.CODEC));

    // ---- 原 HCE 服务器科技模组内容（合并至 swordsman 命名空间）----
    public static final DeferredBlock<Block> POWER_STONE_ORE = BLOCKS.register(
            "power_stone_ore", PowerStoneOreBlock::new);
    public static final DeferredItem<BlockItem> POWER_STONE_ORE_ITEM = ITEMS
            .registerSimpleBlockItem("power_stone_ore", POWER_STONE_ORE);
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.register(
            "titanium_ore", TitaniumOreBlock::new);
    public static final DeferredItem<BlockItem> TITANIUM_ORE_ITEM = ITEMS
            .registerSimpleBlockItem("titanium_ore", TITANIUM_ORE,
                    new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredBlock<Block> ALUMINUM_ORE = BLOCKS.register(
            "aluminum_ore", AluminumOreBlock::new);
    public static final DeferredItem<BlockItem> ALUMINUM_ORE_ITEM = ITEMS
            .registerSimpleBlockItem("aluminum_ore", ALUMINUM_ORE);

    public static final DeferredItem<PowerStoneItem> POWER_STONE = ITEMS.register(
            "power_stone", PowerStoneItem::new);
    public static final DeferredItem<CrystalCurrencyItem> CRYSTAL_CURRENCY = ITEMS.register(
            "crystal_currency", CrystalCurrencyItem::new);
    public static final DeferredItem<TitanitePickaxeItem> TITANITE_PICKAXE = ITEMS.register(
            "titanite_pickaxe", TitanitePickaxeItem::new);
    public static final DeferredItem<TitaniteSwordItem> TITANITE_SWORD = ITEMS.register(
            "titanite_sword", TitaniteSwordItem::new);
    public static final DeferredItem<TitaniteHoeItem> TITANITE_HOE = ITEMS.register(
            "titanite_hoe", TitaniteHoeItem::new);
    public static final DeferredItem<TitaniteIngotItem> TITANITE_INGOT = ITEMS.register(
            "titanite_ingot", TitaniteIngotItem::new);
    public static final DeferredItem<TitaniteStickItem> TITANITE_STICK = ITEMS.register(
            "titanite_stick", TitaniteStickItem::new);
    public static final DeferredItem<TitaniteScrapItem> TITANITE_SCRAP = ITEMS.register(
            "titanite_scrap", TitaniteScrapItem::new);
    public static final DeferredItem<TitaniteItem.Helmet> TITANITE_HELMET = ITEMS.register(
            "titanite_helmet", TitaniteItem.Helmet::new);
    public static final DeferredItem<TitaniteItem.Chestplate> TITANITE_CHESTPLATE = ITEMS.register(
            "titanite_chestplate", TitaniteItem.Chestplate::new);
    public static final DeferredItem<TitaniteItem.Leggings> TITANITE_LEGGINGS = ITEMS.register(
            "titanite_leggings", TitaniteItem.Leggings::new);
    public static final DeferredItem<TitaniteItem.Boots> TITANITE_BOOTS = ITEMS.register(
            "titanite_boots", TitaniteItem.Boots::new);
    public static final DeferredItem<MikuItem.Helmet> MIKU_HELMET = ITEMS.register(
            "miku_helmet", MikuItem.Helmet::new);
    public static final DeferredItem<MikuItem.Chestplate> MIKU_CHESTPLATE = ITEMS.register(
            "miku_chestplate", MikuItem.Chestplate::new);
    public static final DeferredItem<MikuItem.Leggings> MIKU_LEGGINGS = ITEMS.register(
            "miku_leggings", MikuItem.Leggings::new);
    public static final DeferredItem<MikuItem.Boots> MIKU_BOOTS = ITEMS.register(
            "miku_boots", MikuItem.Boots::new);
    public static final DeferredItem<UpgradeToolItem> UPGRADE_TOOL = ITEMS.register(
            "upgrade_tool", UpgradeToolItem::new);
    public static final DeferredItem<TitaniumDustItem> TITANIUM_DUST = ITEMS.register(
            "titanium_dust", TitaniumDustItem::new);
    public static final DeferredItem<RawTitamiumItem> RAW_TITAMIUM = ITEMS.register(
            "raw_titamium", RawTitamiumItem::new);
    public static final DeferredItem<RawAluminumItem> RAW_ALUMINUM = ITEMS.register(
            "raw_aluminum", RawAluminumItem::new);
    public static final DeferredItem<AluminumIngotItem> ALUMINUM_INGOT = ITEMS.register(
            "aluminum_ingot", AluminumIngotItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SWORDSMAN_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("swordsman_items",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("item_group.swordsman.swordsman_items"))
                            .icon(() -> new ItemStack(TITANITE_PICKAXE.get()))
                            .displayItems((parameters, tabData) -> {
                                tabData.accept(POWER_STONE.get());
                                tabData.accept(POWER_STONE_ORE_ITEM.get());
                                tabData.accept(TITANITE_PICKAXE.get());
                                tabData.accept(CRYSTAL_CURRENCY.get());
                                tabData.accept(TITANITE_SWORD.get());
                                tabData.accept(TITANITE_INGOT.get());
                                tabData.accept(TITANITE_STICK.get());
                                tabData.accept(TITANITE_HELMET.get());
                                tabData.accept(TITANITE_CHESTPLATE.get());
                                tabData.accept(TITANITE_LEGGINGS.get());
                                tabData.accept(TITANITE_BOOTS.get());
                                tabData.accept(UPGRADE_TOOL.get());
                                tabData.accept(TITANIUM_ORE_ITEM.get());
                                tabData.accept(TITANIUM_DUST.get());
                                tabData.accept(RAW_TITAMIUM.get());
                                tabData.accept(MIKU_HELMET.get());
                                tabData.accept(MIKU_CHESTPLATE.get());
                                tabData.accept(MIKU_LEGGINGS.get());
                                tabData.accept(MIKU_BOOTS.get());
                                tabData.accept(RAW_ALUMINUM.get());
                                tabData.accept(ALUMINUM_INGOT.get());
                                tabData.accept(TITANITE_SCRAP.get());
                                tabData.accept(TITANITE_HOE.get());
                            })
                            .withSearchBar()
                            .build());

    public SwordsmanMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FEATURES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(SwordsmanMod::addCreative);
        modEventBus.addListener(TitaniteItem::registerArmorMaterial);
        modEventBus.addListener(MikuItem::registerArmorMaterial);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(SwordsmanFuels::furnaceFuelBurnTimeEvent);
        NeoForge.EVENT_BUS.addListener(SwordsmanTrades::registerTrades);
        LOGGER.info("[Swordsman] 剑客群组服(SGU) custom mod loaded.");
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(POWER_STONE.get());
            event.accept(TITANITE_INGOT.get());
            event.accept(TITANITE_STICK.get());
            event.accept(TITANIUM_DUST.get());
            event.accept(RAW_TITAMIUM.get());
            event.accept(RAW_ALUMINUM.get());
            event.accept(ALUMINUM_INGOT.get());
            event.accept(TITANITE_SCRAP.get());
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(POWER_STONE_ORE_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TITANITE_PICKAXE.get());
            event.accept(UPGRADE_TOOL.get());
            event.accept(TITANITE_HOE.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TITANITE_SWORD.get());
            event.accept(TITANITE_HELMET.get());
            event.accept(TITANITE_CHESTPLATE.get());
            event.accept(TITANITE_LEGGINGS.get());
            event.accept(TITANITE_BOOTS.get());
            event.accept(MIKU_HELMET.get());
            event.accept(MIKU_CHESTPLATE.get());
            event.accept(MIKU_LEGGINGS.get());
            event.accept(MIKU_BOOTS.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TITANIUM_ORE_ITEM.get());
        }
    }

    /** 判断指定世界是否为自定义采矿维度 swordsman:mining。 */
    public static boolean isMiningDimension(net.minecraft.world.level.Level level) {
        return level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath(MODID, "mining"));
    }

    // ---- 服务器 tick 延迟任务队列（保留自 HCE 科技模组内容）----
    private static final Queue<IntObjectPair<Runnable>> WORK_TO_BE_SCHEDULED = new ConcurrentLinkedQueue<>();
    private static final PriorityQueue<TickTask> WORK_QUEUE =
            new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

    public static void queueServerWork(int delay, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            WORK_TO_BE_SCHEDULED.add(new IntObjectImmutablePair<>(delay, action));
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int currentTick = event.getServer().getTickCount();
        IntObjectPair<Runnable> work;
        while ((work = WORK_TO_BE_SCHEDULED.poll()) != null) {
            WORK_QUEUE.add(new TickTask(currentTick + work.leftInt(), work.right()));
        }
        while (!WORK_QUEUE.isEmpty() && currentTick >= WORK_QUEUE.peek().getTick()) {
            WORK_QUEUE.poll().run();
        }
    }
}
