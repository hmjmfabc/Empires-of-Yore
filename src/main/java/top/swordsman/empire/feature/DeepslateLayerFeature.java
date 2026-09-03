package top.swordsman.empire.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 将指定阈值(y)以下的所有石头替换为深板岩，模拟原版 "y<0 以下出现深板岩" 的规则，
 * 但在 empire:mining 维度中阈值为 256 层。
 */
public class DeepslateLayerFeature extends Feature<DeepslateLayerFeature.Config> {

    public DeepslateLayerFeature(MapCodec<Config> codec) {
        super(codec.codec());
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();

        int chunkMinX = context.origin().getX() & ~15;
        int chunkMinZ = context.origin().getZ() & ~15;
        int minY = Math.max(level.getMinBuildHeight(), 0);
        int maxY = Math.min(config.thresholdY(), level.getMinBuildHeight() + level.getHeight());
        if (minY >= maxY) {
            return false;
        }

        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = chunkMinX; x < chunkMinX + 16; x++) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.STONE)) {
                        level.setBlock(pos, deepslate, 2);
                    }
                }
            }
        }
        return true;
    }

    /** 特性配置：threshold_y 以下(不含)的石头替换为深板岩。 */
    public record Config(int thresholdY) implements FeatureConfiguration {
        public static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(
                builder -> builder.group(
                        Codec.INT.fieldOf("threshold_y").forGetter(Config::thresholdY)
                ).apply(builder, Config::new));
    }
}
