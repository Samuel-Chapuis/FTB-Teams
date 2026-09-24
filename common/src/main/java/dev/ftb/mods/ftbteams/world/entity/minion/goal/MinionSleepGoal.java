package dev.ftb.mods.ftbteams.world.entity.minion.goal;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionBeds;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Moves a minion to its assigned pillow and maintains its sleeping pose overnight. */
public final class MinionSleepGoal extends Goal {
	private final MinionEntity minion;
	private int repathDelay;

	public MinionSleepGoal(MinionEntity minion) {
		this.minion = minion;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return MinionSchedule.isSleepTime(minion.level().getDayTime())
				&& minion.getHome().filter(pos -> MinionBeds.isValid(minion.level(), pos)).isPresent();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		minion.standFromBench();
		repathDelay = 0;
		minion.getNavigation().stop();
	}

	@Override
	public void stop() {
		minion.getNavigation().stop();
		minion.wakeFromBed();
	}

	@Override
	public void tick() {
		if (minion.isResting()) {
			return;
		}
		BlockPos home = minion.getHome().orElseThrow();
		Vec3 pillow = Vec3.atCenterOf(home).add(0, 0.15, 0);
		if (canReachPillow(pillow)) {
			minion.restAt(home);
		} else if (--repathDelay <= 0) {
			repathDelay = 20;
			minion.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, 1);
		}
	}

	private boolean canReachPillow(Vec3 pillow) {
		return minion.position().distanceToSqr(pillow) < 2.25 && minion.level().clip(new ClipContext(minion.getEyePosition(), pillow,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minion)).getType() == HitResult.Type.MISS;
	}
}
