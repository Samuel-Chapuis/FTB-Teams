package dev.ftb.mods.ftbteams.world.entity.minion.job;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionWorkstations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Defines one minion profession, its compatible workstation, and its daily working hours. */
public interface MinionJob {
	/** Returns whether this job can use the supplied workstation block. */
	boolean supports(BlockState state);

	/** Returns whether the job is currently on duty. */
	boolean isWorkTime(long dayTime);

	/** Finds the nearest compatible and unreserved workstation. */
	default BlockPos findWorkstation(MinionEntity minion) {
		return MinionWorkstations.findNearest(minion, this::supports);
	}

	/** Validates faction ownership, job compatibility, and exclusive assignment. */
	default boolean isAvailable(MinionEntity minion, BlockPos pos) {
		return MinionWorkstations.isAvailable(minion, pos, this::supports);
	}
}
