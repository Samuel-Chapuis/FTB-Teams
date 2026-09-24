package dev.ftb.mods.ftbteams.world.entity.minion.job;

import net.minecraft.world.level.block.state.BlockState;

/** Display-only fallback profession used while a minion has no workstation. */
public final class UnemployedJob implements MinionJob {
	public static final UnemployedJob INSTANCE = new UnemployedJob();
	public static final String TRANSLATION_KEY = "ftbteams.minion.job.unemployed";

	private UnemployedJob() {
	}

	@Override
	public boolean supports(BlockState state) {
		return false;
	}

	@Override
	public boolean isWorkTime(long dayTime) {
		return false;
	}
}
