package dev.ftb.mods.ftbteams.world.entity.minion.job;

import dev.ftb.mods.ftbteams.world.block.CashRegisterBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Cash-register profession working every day from 08:00 until 17:00. */
public final class CashierJob implements MinionJob {
	public static final CashierJob INSTANCE = new CashierJob();
	public static final int WORK_START = MinionSchedule.tickAtHour(8);
	public static final int WORK_END = MinionSchedule.tickAtHour(17);

	private CashierJob() {
	}

	@Override
	public boolean supports(BlockState state) {
		return state.getBlock() instanceof CashRegisterBlock;
	}

	@Override
	public boolean isWorkTime(long dayTime) {
		return MinionSchedule.isBetween(dayTime, WORK_START, WORK_END);
	}
}
