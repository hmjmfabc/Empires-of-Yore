package top.swordsman.empire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class PowerStoneOreBlock extends DropExperienceBlock {

	public PowerStoneOreBlock() {
		super(UniformInt.of(6, 24), BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(1.1f, 10f)
				.requiresCorrectToolForDrops()
				.instrument(NoteBlockInstrument.HAT));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}
}
