package dev.ftb.mods.ftbteams.world.entity.minion;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Per-dimension assignments survive entity/chunk unloads, preventing repeated checks from spawning duplicates. */
public class MinionPopulationData extends SavedData {
	private static final Factory<MinionPopulationData> FACTORY = new Factory<>(MinionPopulationData::new, MinionPopulationData::load, null);
	private final MinionAssignments assignments = new MinionAssignments();

	public static MinionPopulationData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, "ftbteams_minion_homes");
	}

	public MinionAssignments.Resident resident(BlockPos home) {
		return assignments.get(home.asLong());
	}

	public boolean claim(BlockPos home, UUID minion, UUID owner, UUID faction) {
		boolean changed = assignments.claim(home.asLong(), minion, owner, faction);
		if (changed) {
			setDirty();
		}
		return changed;
	}

	public void release(BlockPos home, UUID minion) {
		if (assignments.release(home.asLong(), minion)) {
			setDirty();
		}
	}

	public void removeBed(ServerLevel level, BlockPos home) {
		var resident = resident(home);
		if (resident != null) {
			release(home, resident.minion());
			if (level.getEntity(resident.minion()) instanceof MinionEntity minion) {
				minion.discard();
			}
			// An unloaded minion will see the revoked assignment and discard itself when loaded.
		}
	}

	private static MinionPopulationData load(CompoundTag tag, HolderLookup.Provider registries) {
		MinionPopulationData data = new MinionPopulationData();
		ListTag homes = tag.getList("Homes", Tag.TAG_COMPOUND);
		for (int i = 0; i < homes.size(); i++) {
			CompoundTag home = homes.getCompound(i);
			if (home.hasUUID("Minion") && home.hasUUID("Faction")) {
				// Worlds created before explicit ownership retain their resident assignment.
				UUID owner = home.hasUUID("Owner") ? home.getUUID("Owner") : Util.NIL_UUID;
				data.assignments.claim(home.getLong("Pos"), home.getUUID("Minion"), owner, home.getUUID("Faction"));
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag homes = new ListTag();
		assignments.entries().forEach((pos, resident) -> {
			CompoundTag home = new CompoundTag();
			home.putLong("Pos", pos);
			home.putUUID("Minion", resident.minion());
			home.putUUID("Owner", resident.owner());
			home.putUUID("Faction", resident.faction());
			homes.add(home);
		});
		tag.put("Homes", homes);
		return tag;
	}
}
