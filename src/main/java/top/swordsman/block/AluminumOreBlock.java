package top.swordsman.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public class AluminumOreBlock extends Block {
	public AluminumOreBlock() {
		super(BlockBehaviour.Properties.of().strength(8f, 18f).lightLevel(blockstate -> 3).requiresCorrectToolForDrops());
	}
}