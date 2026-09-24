package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import dev.ftb.mods.ftbteams.world.entity.minion.goal.MinionSleepGoal;
import dev.ftb.mods.ftbteams.world.entity.minion.goal.MinionEatGoal;
import dev.ftb.mods.ftbteams.world.entity.minion.goal.MinionBenchGoal;
import dev.ftb.mods.ftbteams.world.entity.minion.goal.MinionWorkGoal;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionJobs;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;

/** Builds the ordered behavior set used by every minion. */
public final class MinionGoals {
	private MinionGoals() {
	}

	/** Registers safety, sleep, food, work, and leisure behaviors in descending priority. */
	public static void register(GoalSelector selector, MinionEntity minion) {
		selector.addGoal(0, new FloatGoal(minion));
		selector.addGoal(1, new MinionSleepGoal(minion));
		selector.addGoal(2, new MinionEatGoal(minion));
		selector.addGoal(3, new MinionWorkGoal(minion));
		selector.addGoal(4, new OpenDoorGoal(minion, true));
		selector.addGoal(5, new MoveTowardsRestrictionGoal(minion, 1));
		selector.addGoal(6, new MinionBenchGoal(minion));
		selector.addGoal(7, leisureStroll(minion));
		selector.addGoal(8, awakePlayerLook(minion));
		selector.addGoal(9, awakeRandomLook(minion));
	}

	private static WaterAvoidingRandomStrollGoal leisureStroll(MinionEntity minion) {
		return new WaterAvoidingRandomStrollGoal(minion, 0.8) {
			@Override
			public boolean canUse() {
				return canWander(minion) && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return canWander(minion) && super.canContinueToUse();
			}
		};
	}

	private static LookAtPlayerGoal awakePlayerLook(MinionEntity minion) {
		return new LookAtPlayerGoal(minion, Player.class, 6) {
			@Override
			public boolean canUse() {
				return !minion.isResting() && super.canUse();
			}
		};
	}

	private static RandomLookAroundGoal awakeRandomLook(MinionEntity minion) {
		return new RandomLookAroundGoal(minion) {
			@Override
			public boolean canUse() {
				return !minion.isResting() && super.canUse();
			}
		};
	}

	private static boolean canWander(MinionEntity minion) {
		return !MinionSchedule.isSleepTime(minion.level().getDayTime()) && !MinionJobs.isAssignedWorkTime(minion)
				&& !minion.isResting() && !minion.isSittingOnBench();
	}
}
