package dev.ftb.mods.ftbteams.world.entity.minion.goal;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.food.CashRegisterFoodSource;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Sends a hungry minion to a staffed cash register and feeds it from the input slots. */
public final class MinionEatGoal extends Goal {
	private final MinionEntity minion;
	private BlockPos target;
	private int searchCooldown;
	private int repathDelay;

	public MinionEatGoal(MinionEntity minion) {
		this.minion = minion;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (minion.getFood() >= MinionEntity.MAX_FOOD || minion.isResting()
				|| MinionSchedule.isSleepTime(minion.level().getDayTime()) || --searchCooldown > 0) {
			return false;
		}
		searchCooldown = 40;
		target = CashRegisterFoodSource.findNearest(minion);
		return target != null;
	}

	@Override
	public boolean canContinueToUse() {
		return target != null && minion.getFood() < MinionEntity.MAX_FOOD && !minion.isResting()
				&& !MinionSchedule.isSleepTime(minion.level().getDayTime())
				&& CashRegisterFoodSource.isAvailable(minion, target);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		minion.standFromBench();
		repathDelay = 0;
	}

	@Override
	public void stop() {
		minion.getNavigation().stop();
		target = null;
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}
		Vec3 destination = Vec3.atBottomCenterOf(target).add(0, 1, 0);
		if (minion.position().distanceToSqr(destination) < 4) {
			minion.getNavigation().stop();
			minion.setDeltaMovement(Vec3.ZERO);
			while (minion.getFood() < MinionEntity.MAX_FOOD) {
				int nutrition = CashRegisterFoodSource.takeOne(minion, target);
				if (nutrition <= 0) {
					break;
				}
				minion.consumeNutrition(nutrition);
			}
		} else if (--repathDelay <= 0) {
			repathDelay = 20;
			minion.getNavigation().moveTo(destination.x, destination.y, destination.z, 1);
		}
	}
}
