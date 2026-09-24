package dev.ftb.mods.ftbteams.world.block;

import dev.architectury.registry.menu.MenuRegistry;
import dev.ftb.mods.ftbteams.world.block.enclosure.EnclosureScanner;
import dev.ftb.mods.ftbteams.world.block.entity.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.faction.FactionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Shared interaction, storage and enclosure rules for building controller blocks. */
public abstract class EnclosureBlock extends BaseEntityBlock {
	public static final int SEARCH_RADIUS = 15;
	/** Slots 0-19: inputs (the four upper rows). */
	public static final int INPUT_SLOTS = 20;
	/** Slots 20-24: extract-only outputs (the separate bottom row). */
	public static final int OUTPUT_SLOTS = 5;
	public static final int STORAGE_SIZE = INPUT_SLOTS + OUTPUT_SLOTS;

	protected EnclosureBlock(Properties properties) {
		super(properties);
	}

	/** Override to disable storage for a building type; its enclosure menu remains available. */
	public boolean hasInventory() {
		return true;
	}

	public BlockPos getControllerPos(BlockState state, BlockPos pos) {
		return pos;
	}

	/** Interior air from which to start searching. Multi-part blocks may supply multiple seeds. */
	public List<BlockPos> getInteriorSeeds(BlockState state, BlockPos pos) {
		return List.of(pos.above());
	}

	/**
	 * Called after a manual enclosure scan. Subclasses use this hook for building-specific effects
	 * such as assigning a resident to a Pop Bed.
	 */
	public void onEnclosureChecked(EnclosureBlockEntity enclosure, ServerPlayer player, EnclosureScanner.Result result) {
	}

	/** Returns the number displayed by building interfaces that expose a worker slot. */
	public int getWorkerCount(ServerLevel level, BlockPos pos) {
		return 0;
	}

	/** Selects the worker-specific status panel without coupling the block entity to a concrete block. */
	public boolean showsWorkerStatus() {
		return false;
	}

	/** Returns the party faction currently associated with a player, if they belong to one. */
	public static Optional<UUID> getPlayerFaction(ServerPlayer player) {
		return FactionAccess.findPlayerFaction(player);
	}

	/** Building types share faction access; the controller stores the persistent owner and faction IDs. */
	public boolean canAccess(EnclosureBlockEntity enclosure, ServerPlayer player) {
		return enclosure.canAccess(player);
	}

	/** Claims an unowned sealed building for a faction player. Existing faction members retain access. */
	public boolean claimOwnership(EnclosureBlockEntity enclosure, ServerPlayer player) {
		return enclosure.claimOwnership(player, getPlayerFaction(player).orElse(null));
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(getControllerPos(state, pos)) instanceof EnclosureBlockEntity enclosure ? enclosure : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(getControllerPos(state, pos)) instanceof EnclosureBlockEntity enclosure
				&& enclosure.canOpen(player) && canAccess(enclosure, serverPlayer)) {
			MenuRegistry.openExtendedMenu(serverPlayer, enclosure);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnclosureBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock())) {
			if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnclosureBlockEntity enclosure) {
				Containers.dropContents(level, pos, enclosure);
				level.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, level, pos, newState, moving);
		}
	}
}
