package dev.ftb.mods.ftbteams.world.entity.minion.goal;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionJob;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionJobs;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Resolves the minion's profession and keeps it at its workstation during that job's working hours. */
public final class MinionWorkGoal extends Goal {
	private final MinionEntity minion;
	private MinionJob job;
	private BlockPos target;
	private int searchCooldown;
	private int repathDelay;

	public MinionWorkGoal(MinionEntity minion) {
		this.minion = minion;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (MinionSchedule.isSleepTime(minion.level().getDayTime()) || minion.isResting() || --searchCooldown > 0) {
			return false;
		}
		searchCooldown = 40;
		var assignment = MinionJobs.assignedJob(minion)
				.filter(value -> value.job().isWorkTime(minion.level().getDayTime())
						&& value.job().isAvailable(minion, value.workstation()))
				.or(() -> MinionJobs.findAvailableJob(minion));
		if (assignment.isEmpty()) {
			return false;
		}
		job = assignment.get().job();
		target = assignment.get().workstation();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return !minion.isResting() && job != null && target != null && job.isWorkTime(minion.level().getDayTime())
				&& job.isAvailable(minion, target);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		repathDelay = 0;
		minion.assignWorkstation(target);
	}

	@Override
	public void stop() {
		minion.getNavigation().stop();
		minion.stopWorking();
		job = null;
		target = null;
	}

	@Override
	public void tick() {
		if (job == null || target == null || !job.isAvailable(minion, target)) {
			stop();
			return;
		}
		Vec3 destination = Vec3.atBottomCenterOf(target).add(0, 1, 0);
		if (minion.position().distanceToSqr(destination) < 4) {
			minion.getNavigation().stop();
			minion.setDeltaMovement(Vec3.ZERO);
			minion.startWorkingAt(target);
		} else if (--repathDelay <= 0) {
			repathDelay = 20;
			minion.getNavigation().moveTo(destination.x, destination.y, destination.z, 1);
		}
	}
}
