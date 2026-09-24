package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Reads and writes the complete custom state owned by a {@link MinionEntity}. */
public final class MinionPersistence {
	private MinionPersistence() {
	}

	/** Writes entity state without duplicating synced-data ownership in this serializer. */
	public static void save(MinionEntity minion, MinionFoodTracker foodTracker, CompoundTag tag) {
		minion.getHome().ifPresent(home -> tag.putLong("MinionHome", home.asLong()));
		minion.getWorkstation().ifPresent(workstation -> tag.putLong("MinionWorkstation", workstation.asLong()));
		if (minion.getOwnerId() != null) {
			tag.putUUID("MinionOwner", minion.getOwnerId());
		}
		if (minion.getFactionId() != null) {
			tag.putUUID("MinionFaction", minion.getFactionId());
		}
		tag.putInt("MinionFood", minion.getFood());
		tag.putInt("MinionSkin", minion.getSkinId());
		putNonBlank(tag, "MinionCustomSkin", minion.getCustomSkinId());
		putNonBlank(tag, "MinionCustomSkinUuid", minion.getCustomSkinUuid());
		putNonBlank(tag, "MinionProfession", minion.getProfession());
		tag.putBoolean("MinionResting", minion.isResting());
		foodTracker.save(tag);
	}

	/** Restores state in dependency order so a saved skin is never replaced by home assignment. */
	public static void load(MinionEntity minion, MinionFoodTracker foodTracker, CompoundTag tag) {
		minion.setFood(tag.contains("MinionFood") ? tag.getInt("MinionFood") : MinionEntity.INITIAL_FOOD);
		if (tag.contains("MinionSkin")) {
			minion.restoreSkin(tag.getInt("MinionSkin"));
		}
		minion.restoreCustomSkin(tag.getString("MinionCustomSkin"), legacySkinUuid(tag));
		foodTracker.load(tag);
		restoreHome(minion, tag);
		if (tag.contains("MinionWorkstation")) {
			minion.assignWorkstation(BlockPos.of(tag.getLong("MinionWorkstation")));
		}
		if (tag.contains("MinionProfession")) {
			minion.restoreProfession(tag.getString("MinionProfession"));
		}
	}

	private static void restoreHome(MinionEntity minion, CompoundTag tag) {
		if (!tag.contains("MinionHome") || !tag.hasUUID("MinionFaction")) {
			return;
		}
		BlockPos home = BlockPos.of(tag.getLong("MinionHome"));
		minion.assignHome(home, tag.hasUUID("MinionOwner") ? tag.getUUID("MinionOwner") : null,
				tag.getUUID("MinionFaction"));
		if (tag.getBoolean("MinionResting")) {
			minion.restAt(home);
		}
	}

	private static String legacySkinUuid(CompoundTag tag) {
		String savedUuid = tag.getString("MinionCustomSkinUuid");
		String savedName = tag.getString("MinionCustomSkin");
		return savedUuid.isBlank() && savedName.matches("[0-9a-fA-F-]{36}") ? savedName : savedUuid;
	}

	private static void putNonBlank(CompoundTag tag, String key, String value) {
		if (!value.isBlank()) {
			tag.putString(key, value);
		}
	}
}
