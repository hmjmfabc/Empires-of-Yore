package top.swordsman.empire.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;

public class TitaniumOreBlock extends DropExperienceBlock {
	public TitaniumOreBlock() {
		super(UniformInt.of(78, 91), BlockBehaviour.Properties.of().strength(13f, 10f).lightLevel(blockstate -> 7).requiresCorrectToolForDrops());
	}
}