package dev.ftb.mods.ftbteams.world.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import dev.ftb.mods.ftbteams.world.block.PopBedBlock;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionBeds;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionFoodTracker;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionGoals;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionPersistence;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionPopulationData;
import dev.ftb.mods.ftbteams.world.entity.minion.MinionWorkstations;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionJobs;
import dev.ftb.mods.ftbteams.world.entity.minion.job.MinionSchedule;
import dev.ftb.mods.ftbteams.world.faction.FactionAccess;
import dev.ftb.mods.ftbteams.world.inventory.MinionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** A persistent faction resident: daytime strolls and a nightly walk back to its own Pop Bed. */
public class MinionEntity extends PathfinderMob implements ExtendedMenuProvider {
	public static final float BODY_DIVISOR = 1.75F;
	public static final int MAX_FOOD = 3;
	public static final int INITIAL_FOOD = 1;
	public static final int SKIN_COUNT = 9;
	private static final EntityDataAccessor<Optional<BlockPos>> HOME = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Optional<BlockPos>> WORKSTATION = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<String> PROFESSION = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> FOOD = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> CUSTOM_SKIN = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> CUSTOM_SKIN_UUID = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.STRING);
	private final MinionFoodTracker foodTracker = new MinionFoodTracker();
	private BlockPos activeWorkstation;
	private UUID faction;
	private UUID owner;
	private boolean skinAssigned;

	public MinionEntity(EntityType<? extends MinionEntity> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		if (getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanOpenDoors(true);
			navigation.setCanPassDoors(true);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.23)
				.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HOME, Optional.empty());
		builder.define(WORKSTATION, Optional.empty());
		builder.define(PROFESSION, "");
		builder.define(FOOD, INITIAL_FOOD);
		builder.define(SKIN, 0);
		builder.define(CUSTOM_SKIN, "");
		builder.define(CUSTOM_SKIN_UUID, "");
	}

	public void assignHome(BlockPos home, UUID owner, UUID faction) {
		if (!skinAssigned) {
			entityData.set(SKIN, level().getRandom().nextInt(SKIN_COUNT));
			skinAssigned = true;
		}
		entityData.set(HOME, Optional.of(home.immutable()));
		this.owner = owner;
		this.faction = faction;
		restrictTo(home, 24);
	}

	public Optional<BlockPos> getHome() {
		return entityData.get(HOME);
	}

	public Optional<BlockPos> getWorkstation() {
		return entityData.get(WORKSTATION);
	}

	public boolean isWorkingAt(BlockPos pos) {
		return pos.equals(activeWorkstation);
	}

	/** Returns whether this minion owns the workstation, including outside working hours. */
	public boolean isAssignedTo(BlockPos pos) {
		return getWorkstation().filter(pos::equals).isPresent();
	}

	public String getProfession() {
		return entityData.get(PROFESSION);
	}

	public ItemStack getProfessionStack() {
		return MinionWorkstations.icon(this);
	}

	public void clearProfession() {
		assignWorkstation(null);
		entityData.set(PROFESSION, "");
	}

	/** Assigns or releases the workstation retained across work, leisure, and sleep periods. */
	public void assignWorkstation(BlockPos pos) {
		entityData.set(WORKSTATION, Optional.ofNullable(pos == null ? null : pos.immutable()));
		if (pos == null) {
			stopWorking();
		}
	}

	/** Reserves the workstation and derives the displayed profession from its block. */
	public void startWorkingAt(BlockPos pos) {
		assignWorkstation(pos);
		activeWorkstation = pos.immutable();
		entityData.set(PROFESSION, level().getBlockState(pos).getBlock().getDescriptionId());
	}

	/** Leaves the active work state while retaining the assigned profession and workstation. */
	public void stopWorking() {
		activeWorkstation = null;
	}

	public UUID getFactionId() {
		return faction;
	}

	public UUID getOwnerId() {
		return owner;
	}

	public int getFood() {
		return entityData.get(FOOD);
	}

	public int getSkinId() {
		return entityData.get(SKIN);
	}

	public String getCustomSkinId() {
		return entityData.get(CUSTOM_SKIN);
	}

	public String getCustomSkinUuid() {
		return entityData.get(CUSTOM_SKIN_UUID);
	}

	public void setCustomSkinId(String id) {
		entityData.set(CUSTOM_SKIN, id == null ? "" : id);
	}

	public void setCustomSkin(String name, UUID uuid) {
		entityData.set(CUSTOM_SKIN, name == null ? "" : name);
		entityData.set(CUSTOM_SKIN_UUID, uuid == null ? "" : uuid.toString());
	}

	/** Restores the persisted vanilla skin index before assigning a home. */
	public void restoreSkin(int skin) {
		entityData.set(SKIN, Math.floorMod(skin, SKIN_COUNT));
		skinAssigned = true;
	}

	/** Restores custom skin identifiers, including legacy UUID-only saves. */
	public void restoreCustomSkin(String name, String uuid) {
		entityData.set(CUSTOM_SKIN, name == null ? "" : name);
		entityData.set(CUSTOM_SKIN_UUID, uuid == null ? "" : uuid);
	}

	/** Restores the translation key retained for a previously used workstation. */
	public void restoreProfession(String profession) {
		entityData.set(PROFESSION, profession == null ? "" : profession);
	}

	/** Reserved for future food/workstation rules; the current cap is intentionally three points. */
	public void setFood(int food) {
		entityData.set(FOOD, Math.clamp(food, 0, MAX_FOOD));
	}

	/** The owner and every current member of the owner's faction can interact with this minion. */
	public boolean canAccess(ServerPlayer player) {
		return FactionAccess.canAccess(owner, faction, player);
	}

	public String getFactionName() {
		return getFactionProfile().name();
	}

	public int getFactionColor() {
		return getFactionProfile().color();
	}

	public String getFactionLogo() {
		return getFactionProfile().logo();
	}

	/** Resolves all display properties with one faction-manager lookup. */
	public FactionAccess.Profile getFactionProfile() {
		return FactionAccess.resolve(faction);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("entity.ftbteams.minion");
	}

	@Override
	public void saveExtraData(FriendlyByteBuf buf) {
		FactionAccess.Profile profile = getFactionProfile();
		buf.writeVarInt(getId());
		buf.writeUtf(profile.name(), 96);
		buf.writeInt(profile.color());
		buf.writeUtf(profile.logo(), 8192);
		buf.writeUtf(getProfession(), 256);
		buf.writeBoolean(getHome().isPresent());
		getHome().ifPresent(buf::writeBlockPos);
		buf.writeBoolean(getWorkstation().isPresent());
		getWorkstation().ifPresent(buf::writeBlockPos);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MinionMenu(id, inventory, this);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			if (!canAccess(serverPlayer)) {
				player.displayClientMessage(Component.translatable("ftbteams.minion.access_denied"), true);
				return InteractionResult.FAIL;
			}
			MenuRegistry.openExtendedMenu(serverPlayer, this);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	public static boolean isHomeBed(Level level, BlockPos pos) {
		return MinionBeds.isValid(level, pos);
	}

	@Override
	protected void registerGoals() {
		MinionGoals.register(goalSelector, this);
	}

	/** Uses the synced sleeping pose, without vanilla's BedBlock-only sleeping-position checks. */
	public boolean isResting() {
		return hasPose(Pose.SLEEPING);
	}

	/** Enters the sleeping pose at the assigned pillow. */
	public void restAt(BlockPos home) {
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		setPose(Pose.SLEEPING);
		setNoGravity(true);
		setPos(home.getX() + 0.5, home.getY() + 0.5625, home.getZ() + 0.5);
	}

	/** Leaves the sleeping pose. Safe to call while already awake. */
	public void wakeFromBed() {
		setPose(Pose.STANDING);
		setNoGravity(false);
	}

	@Override
	public Direction getBedOrientation() {
		return getHome().filter(pos -> level().getBlockState(pos).getBlock() instanceof PopBedBlock)
				.map(pos -> level().getBlockState(pos).getValue(PopBedBlock.FACING)).orElse(Direction.NORTH);
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel server) {
			foodTracker.tick(this, server);
			if (getWorkstation().filter(pos -> server.hasChunkAt(pos) && !MinionJobs.isValidAssignment(this, pos)).isPresent()) {
				assignWorkstation(null);
			}
			if (MinionSchedule.isSleepTime(server.getDayTime())) {
				stopWorking();
			}
			if (tickCount % 20 == 0 && !hasValidHomeAssignment(server)) {
				discard();
				return;
			}
			if (isResting() && (!MinionSchedule.isSleepTime(server.getDayTime()) || isInWaterOrBubble())) {
				wakeFromBed();
			}
		}
		super.tick();
		if (!level().isClientSide && isAlive() && isResting()) {
			getHome().ifPresent(this::restAt);
		}
	}

	private boolean hasValidHomeAssignment(ServerLevel level) {
		if (getHome().isEmpty()) {
			return true;
		}
		BlockPos home = getHome().orElseThrow();
		var assignment = MinionPopulationData.get(level).resident(home);
		return assignment != null && assignment.minion().equals(getUUID())
				&& (!level.hasChunkAt(home) || isHomeBed(level, home));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!level().isClientSide && isResting()) {
			wakeFromBed();
		}
		return super.hurt(source, amount);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
			getHome().ifPresent(home -> MinionPopulationData.get(server).release(home, getUUID()));
		}
		super.remove(reason);
	}

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		MinionPersistence.save(this, foodTracker, tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		MinionPersistence.load(this, foodTracker, tag);
	}

}
