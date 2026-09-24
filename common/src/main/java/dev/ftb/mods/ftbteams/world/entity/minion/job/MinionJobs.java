package dev.ftb.mods.ftbteams.world.entity.minion.job;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/** Registry and lookup service for the available minion professions. */
public final class MinionJobs {
	private static final List<MinionJob> JOBS = List.of(CashierJob.INSTANCE);

	private MinionJobs() {
	}

	/** Resolves the job associated with the minion's retained workstation. */
	public static Optional<Assignment> assignedJob(MinionEntity minion) {
		return minion.getWorkstation().flatMap(pos -> jobAt(minion, pos).map(job -> new Assignment(job, pos)));
	}

	/** Finds the first on-duty job with a compatible free workstation. */
	public static Optional<Assignment> findAvailableJob(MinionEntity minion) {
		long dayTime = minion.level().getDayTime();
		for (MinionJob job : JOBS) {
			if (!job.isWorkTime(dayTime)) {
				continue;
			}
			BlockPos workstation = job.findWorkstation(minion);
			if (workstation != null) {
				return Optional.of(new Assignment(job, workstation));
			}
		}
		return Optional.empty();
	}

	/** Returns whether a retained workstation still represents a valid, exclusive job assignment. */
	public static boolean isValidAssignment(MinionEntity minion, BlockPos pos) {
		return jobAt(minion, pos).filter(job -> job.isAvailable(minion, pos)).isPresent();
	}

	/** Returns whether the minion's assigned profession should currently suppress leisure behavior. */
	public static boolean isAssignedWorkTime(MinionEntity minion) {
		return assignedJob(minion).filter(assignment -> assignment.job().isWorkTime(minion.level().getDayTime())).isPresent();
	}

	private static Optional<MinionJob> jobAt(MinionEntity minion, BlockPos pos) {
		if (!minion.level().hasChunkAt(pos)) {
			return Optional.empty();
		}
		var state = minion.level().getBlockState(pos);
		return JOBS.stream().filter(job -> job.supports(state)).findFirst();
	}

	/** A resolved profession and the workstation reserved for it. */
	public record Assignment(MinionJob job, BlockPos workstation) {
	}
}
