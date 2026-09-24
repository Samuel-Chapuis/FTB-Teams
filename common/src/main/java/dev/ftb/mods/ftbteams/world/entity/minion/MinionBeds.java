package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.block.PopBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Validates the two-block Pop Bed assigned to a minion. */
public final class MinionBeds {
	private MinionBeds() {
	}

	public static boolean isValid(Level level, BlockPos head) {
		if (!level.hasChunkAt(head)) {
			return false;
		}
		var state = level.getBlockState(head);
		if (!(state.getBlock() instanceof PopBedBlock) || state.getValue(PopBedBlock.PART) != BedPart.HEAD) {
			return false;
		}
		BlockPos foot = head.relative(state.getValue(PopBedBlock.FACING).getOpposite());
		if (!level.hasChunkAt(foot)) {
			return true;
		}
		var footState = level.getBlockState(foot);
		return footState.is(state.getBlock()) && footState.getValue(PopBedBlock.PART) == BedPart.FOOT
				&& footState.getValue(PopBedBlock.FACING) == state.getValue(PopBedBlock.FACING);
	}
}
