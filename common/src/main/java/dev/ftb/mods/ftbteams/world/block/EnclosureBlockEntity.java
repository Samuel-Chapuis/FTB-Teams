package dev.ftb.mods.ftbteams.world.block;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.world.inventory.EnclosureMenu;
import dev.ftb.mods.ftbteams.world.entity.MinionHousing;
import dev.ftb.mods.ftbteams.world.entity.MinionPopulationData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class EnclosureBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider {
	private NonNullList<ItemStack> items;
	private EnclosureScanner.Status status = EnclosureScanner.Status.UNCHECKED;
	private int volume;
	private MinionHousing.Status housingStatus = MinionHousing.Status.NONE;
	private EnclosurePreview preview = EnclosurePreview.EMPTY;
	private UUID owner;
	private UUID faction;
	private long nextCheckTick;
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> status.ordinal();
				case 1 -> volume;
				case 2 -> getHousingStatus().ordinal();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) {
				status = EnclosureScanner.Status.values()[value];
			} else if (index == 1) {
				volume = value;
			} else if (index == 2) {
				housingStatus = MinionHousing.Status.values()[value];
			}
		}

		@Override
		public int getCount() {
			return 3;
		}
	};

	public EnclosureBlockEntity(BlockPos pos, BlockState state) {
		this(FTBTeams.ENCLOSURE_BLOCK_ENTITY.get(), pos, state);
	}

	/** Other building types can reuse this entity with their own registered block entity type. */
	protected EnclosureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		EnclosureBlock block = (EnclosureBlock) state.getBlock();
		// Only the controller half owns items; the foot exists solely for rendering.
		items = NonNullList.withSize(block.hasInventory() && block.getControllerPos(state, pos).equals(pos)
				? EnclosureBlock.STORAGE_SIZE : 0, ItemStack.EMPTY);
	}

	public boolean checkEnclosure(ServerPlayer player) {
		if (level == null || level.isClientSide || level.getGameTime() < nextCheckTick) {
			return false;
		}
		nextCheckTick = level.getGameTime() + 10;
		EnclosureBlock block = (EnclosureBlock) getBlockState().getBlock();
		var seeds = block.getInteriorSeeds(getBlockState(), worldPosition).stream().map(EnclosureBlockEntity::position).toList();
		var result = EnclosureScanner.scan(position(worldPosition), seeds, EnclosureBlock.SEARCH_RADIUS, candidate -> {
			BlockPos pos = new BlockPos(candidate.x(), candidate.y(), candidate.z());
			if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) {
				return EnclosureScanner.Cell.UNAVAILABLE;
			}
			return level.getBlockState(pos).isAir() ? EnclosureScanner.Cell.AIR : EnclosureScanner.Cell.SOLID;
		});
		status = result.status();
		volume = result.volume();
		preview = EnclosurePreview.capture(level, worldPosition, result);
		if (status == EnclosureScanner.Status.SEALED) {
			block.claimOwnership(this, player);
		}
		if (getBlockState().getBlock() instanceof PopBedBlock) {
			housingStatus = MinionHousing.validate((ServerLevel) level, worldPosition, player, result);
		}
		return true;
	}

	private MinionHousing.Status getHousingStatus() {
		if (level instanceof ServerLevel server && (housingStatus == MinionHousing.Status.NONE || housingStatus == MinionHousing.Status.ASSIGNED)) {
			return MinionPopulationData.get(server).resident(worldPosition) != null ? MinionHousing.Status.ASSIGNED : MinionHousing.Status.NONE;
		}
		return housingStatus;
	}

	public EnclosurePreview getPreview() {
		return preview;
	}

	/** The player who first validated this sealed building. */
	public UUID getOwnerId() {
		return owner;
	}

	/** The faction of the owner when the building was claimed. */
	public UUID getFactionId() {
		return faction;
	}

	public boolean canAccess(ServerPlayer player) {
		return owner == null || owner.equals(player.getUUID())
				|| (faction != null && EnclosureBlock.getPlayerFaction(player).filter(faction::equals).isPresent());
	}

	boolean claimOwnership(ServerPlayer player, UUID playerFaction) {
		if (owner != null) return canAccess(player);
		if (playerFaction == null) return false;
		owner = player.getUUID();
		faction = playerFaction;
		setChanged();
		return true;
	}

	private static EnclosureScanner.Position position(BlockPos pos) {
		return new EnclosureScanner.Position(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void saveExtraData(FriendlyByteBuf buf) {
		buf.writeVarInt(getContainerSize());
		buf.writeBlockPos(worldPosition);
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new EnclosureMenu(id, inventory, this, data);
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
		if (owner != null) tag.putUUID("EnclosureOwner", owner);
		if (faction != null) tag.putUUID("EnclosureFaction", faction);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, registries);
		owner = tag.hasUUID("EnclosureOwner") ? tag.getUUID("EnclosureOwner") : null;
		faction = tag.hasUUID("EnclosureFaction") ? tag.getUUID("EnclosureFaction") : null;
		// A scan is a snapshot, not proof that the building is still closed after a reload.
		status = EnclosureScanner.Status.UNCHECKED;
		housingStatus = MinionHousing.Status.NONE;
		volume = 0;
		preview = EnclosurePreview.EMPTY;
	}
}
