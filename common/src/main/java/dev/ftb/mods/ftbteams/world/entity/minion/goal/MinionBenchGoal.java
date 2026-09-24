package dev.ftb.mods.ftbteams.world.entity.minion.goal;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionBenches;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionJobs;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Lets an off-duty minion walk to an available bench and sit for a short period. */
public final class MinionBenchGoal extends Goal {
	private final MinionEntity minion;
	private BlockPos seat;
	private int searchCooldown;
	private int repathDelay;
	private int sittingTicks;

	public MinionBenchGoal(MinionEntity minion) {
		this.minion = minion;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!canRelax() || --searchCooldown > 0 || minion.getRandom().nextInt(4) != 0) {
			return false;
		}
		searchCooldown = 100;
		seat = MinionBenches.findSeat(minion);
		return seat != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canRelax() && seat != null && minion.level() instanceof ServerLevel level
				&& MinionBenches.isFree(level, seat, minion) && (!minion.isSittingOnBench() || sittingTicks > 0);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		repathDelay = 0;
		sittingTicks = 100 + minion.getRandom().nextInt(201);
	}

	@Override
	public void stop() {
		minion.getNavigation().stop();
		minion.standFromBench();
		seat = null;
	}

	@Override
	public void tick() {
		if (seat == null) {
			return;
		}
		Vec3 destination = Vec3.atBottomCenterOf(seat).add(0, 0.45, 0);
		if (minion.position().distanceToSqr(destination) < 1.5) {
			minion.sitOnBench(seat);
			sittingTicks--;
		} else if (--repathDelay <= 0) {
			repathDelay = 20;
			minion.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.8);
		}
	}

	private boolean canRelax() {
		long time = minion.level().getDayTime();
		return minion.getFood() >= MinionEntity.MAX_FOOD && !minion.isResting()
				&& !MinionSchedule.isSleepTime(time) && !MinionJobs.isAssignedWorkTime(minion);
	}
}
