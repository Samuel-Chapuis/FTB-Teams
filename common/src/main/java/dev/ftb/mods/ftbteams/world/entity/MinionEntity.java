package dev.ftb.mods.ftbteams.world.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlock;
import dev.ftb.mods.ftbteams.world.block.EnclosureBlockEntity;
import dev.ftb.mods.ftbteams.world.block.CashRegisterBlock;
import dev.ftb.mods.ftbteams.world.block.PopBedBlock;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
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
	private UUID faction;
	private UUID owner;
	private long lastFoodDay = Long.MIN_VALUE;
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

	public Optional<BlockPos> getWorkstation() { return entityData.get(WORKSTATION); }

	public boolean isWorkingAt(BlockPos pos) { return getWorkstation().filter(pos::equals).isPresent(); }

	public String getProfession() { return entityData.get(PROFESSION); }

	public ItemStack getProfessionStack() {
		return getWorkstation().map(pos -> {
			var block = level().getBlockState(pos).getBlock();
			return block.asItem() == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
		}).orElse(ItemStack.EMPTY);
	}

	public void clearProfession() {
		setWorkstation(null);
		entityData.set(PROFESSION, "");
	}

	private void setProfession(BlockState state) { entityData.set(PROFESSION, state.getBlock().getDescriptionId()); }

	private void setWorkstation(BlockPos pos) {
		entityData.set(WORKSTATION, Optional.ofNullable(pos == null ? null : pos.immutable()));
	}

	private boolean validWorkstation(BlockPos pos) {
		if (!(level() instanceof ServerLevel server) || !server.hasChunkAt(pos)
				|| !(server.getBlockState(pos).getBlock() instanceof CashRegisterBlock)
				|| !(server.getBlockEntity(pos) instanceof EnclosureBlockEntity enclosure)
				|| faction == null || !faction.equals(enclosure.getFactionId())) return false;
		return server.getEntitiesOfClass(MinionEntity.class, new AABB(pos).inflate(3.0), other -> other != this && other.isWorkingAt(pos)).isEmpty();
	}

	private BlockPos findWorkstation() {
		return getHome().flatMap(home -> BlockPos.findClosestMatch(home, 48, 16, this::validWorkstation)).orElse(null);
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

	public int getSkinId() { return entityData.get(SKIN); }

	public String getCustomSkinId() { return entityData.get(CUSTOM_SKIN); }
	public String getCustomSkinUuid() { return entityData.get(CUSTOM_SKIN_UUID); }

	public void setCustomSkinId(String id) {
		entityData.set(CUSTOM_SKIN, id == null ? "" : id);
	}

	public void setCustomSkin(String name, UUID uuid) {
		entityData.set(CUSTOM_SKIN, name == null ? "" : name);
		entityData.set(CUSTOM_SKIN_UUID, uuid == null ? "" : uuid.toString());
	}

	/** Reserved for future food/workstation rules; the current cap is intentionally three points. */
	public void setFood(int food) {
		entityData.set(FOOD, Math.clamp(food, 0, MAX_FOOD));
	}

	/** The owner and every current member of the owner's faction can interact with this minion. */
	public boolean canAccess(ServerPlayer player) {
		return owner == null || owner.equals(player.getUUID())
				|| (faction != null && EnclosureBlock.getPlayerFaction(player).filter(faction::equals).isPresent());
	}

	public String getFactionName() {
		return faction == null ? "" : FTBTeamsAPI.api().getManager().getTeamByID(faction)
				.map(team -> team.getName().getString()).orElse("");
	}

	public int getFactionColor() {
		return faction == null ? 0xFF333333 : FTBTeamsAPI.api().getManager().getTeamByID(faction)
				.map(team -> team.getProperty(TeamProperties.COLOR).rgb()).orElse(0xFF333333);
	}

	public String getFactionLogo() {
		return faction == null ? "" : FTBTeamsAPI.api().getManager().getTeamByID(faction)
				.map(team -> team.getProperty(TeamProperties.FACTION_LOGO)).orElse("");
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("entity.ftbteams.minion");
	}

	@Override
	public void saveExtraData(FriendlyByteBuf buf) {
		buf.writeVarInt(getId());
		buf.writeUtf(getFactionName(), 96);
		buf.writeInt(getFactionColor());
		buf.writeUtf(getFactionLogo(), 8192);
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
		if (!level.hasChunkAt(pos)) return false;
		var state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof PopBedBlock) || state.getValue(PopBedBlock.PART) != BedPart.HEAD) return false;
		BlockPos foot = pos.relative(state.getValue(PopBedBlock.FACING).getOpposite());
		// A temporarily unloaded second half is not evidence that the resident lost its home.
		if (!level.hasChunkAt(foot)) return true;
		var footState = level.getBlockState(foot);
		return footState.is(state.getBlock()) && footState.getValue(PopBedBlock.PART) == BedPart.FOOT
				&& footState.getValue(PopBedBlock.FACING) == state.getValue(PopBedBlock.FACING);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ReturnToBedGoal());
		goalSelector.addGoal(2, new WorkAtCashRegisterGoal());
		goalSelector.addGoal(3, new OpenDoorGoal(this, true));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override public boolean canUse() { return !level().isNight() && !isResting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !level().isNight() && !isResting() && super.canContinueToUse(); }
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6) {
			@Override public boolean canUse() { return !isResting() && super.canUse(); }
		});
		goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
			@Override public boolean canUse() { return !isResting() && super.canUse(); }
		});
	}

	/** Uses the synced sleeping pose, without vanilla's BedBlock-only sleeping-position checks. */
	public boolean isResting() {
		return hasPose(Pose.SLEEPING);
	}

	private void restInBed(BlockPos home) {
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		setPose(Pose.SLEEPING);
		setNoGravity(true);
		setPos(home.getX() + 0.5, home.getY() + 0.5625, home.getZ() + 0.5);
	}

	private void wakeUp() {
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
			updateFood(server);
			if (server.isNight() || getWorkstation().filter(pos -> !validWorkstation(pos)).isPresent()) setWorkstation(null);
			if (getHome().isPresent()) {
			BlockPos home = getHome().get();
			if (tickCount % 20 == 0) {
				var assignment = MinionPopulationData.get(server).resident(home);
				if (assignment == null || !assignment.minion().equals(getUUID())
						|| (server.hasChunkAt(home) && !isHomeBed(server, home))) {
					discard();
					return;
				}
			}
			if (isResting() && (!server.isNight() || isInWaterOrBubble())) wakeUp();
			}
		}
		super.tick();
		if (!level().isClientSide && isAlive() && isResting()) getHome().ifPresent(this::restInBed);
	}

	private void updateFood(ServerLevel level) {
		long day = level.getDayTime() / 24000L;
		if (lastFoodDay == Long.MIN_VALUE || day < lastFoodDay) {
			lastFoodDay = day;
			return;
		}
		if (day > lastFoodDay) {
			setFood(getFood() - (int) Math.min(day - lastFoodDay, MAX_FOOD));
			lastFoodDay = day;
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!level().isClientSide && isResting()) wakeUp();
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
		getHome().ifPresent(home -> tag.putLong("MinionHome", home.asLong()));
		getWorkstation().ifPresent(pos -> tag.putLong("MinionWorkstation", pos.asLong()));
		if (owner != null) tag.putUUID("MinionOwner", owner);
		if (faction != null) tag.putUUID("MinionFaction", faction);
		tag.putInt("MinionFood", getFood());
		tag.putInt("MinionSkin", getSkinId());
		if (!getCustomSkinId().isBlank()) tag.putString("MinionCustomSkin", getCustomSkinId());
		if (!getCustomSkinUuid().isBlank()) tag.putString("MinionCustomSkinUuid", getCustomSkinUuid());
		tag.putLong("MinionFoodDay", lastFoodDay);
		if (!getProfession().isBlank()) tag.putString("MinionProfession", getProfession());
		tag.putBoolean("MinionResting", isResting());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setFood(tag.contains("MinionFood") ? tag.getInt("MinionFood") : INITIAL_FOOD);
		skinAssigned = tag.contains("MinionSkin");
		if (skinAssigned) entityData.set(SKIN, Math.floorMod(tag.getInt("MinionSkin"), SKIN_COUNT));
		setCustomSkinId(tag.getString("MinionCustomSkin"));
		String savedSkinUuid = tag.getString("MinionCustomSkinUuid");
		if (savedSkinUuid.isBlank() && getCustomSkinId().matches("[0-9a-fA-F-]{36}")) savedSkinUuid = getCustomSkinId();
		entityData.set(CUSTOM_SKIN_UUID, savedSkinUuid);
		lastFoodDay = tag.contains("MinionFoodDay") ? tag.getLong("MinionFoodDay") : Long.MIN_VALUE;
		if (tag.contains("MinionHome") && tag.hasUUID("MinionFaction")) {
			assignHome(BlockPos.of(tag.getLong("MinionHome")), tag.hasUUID("MinionOwner") ? tag.getUUID("MinionOwner") : null,
					tag.getUUID("MinionFaction"));
			if (tag.getBoolean("MinionResting")) restInBed(getHome().orElseThrow());
		}
		if (tag.contains("MinionWorkstation")) setWorkstation(BlockPos.of(tag.getLong("MinionWorkstation")));
		if (tag.contains("MinionProfession")) entityData.set(PROFESSION, tag.getString("MinionProfession"));
	}

	private final class ReturnToBedGoal extends Goal {
		private int repathDelay;

		private ReturnToBedGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override public boolean canUse() {
			return level().isNight() && getHome().filter(pos -> isHomeBed(level(), pos)).isPresent();
		}
		@Override public boolean canContinueToUse() { return canUse(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void start() { repathDelay = 0; getNavigation().stop(); }
		@Override public void stop() { getNavigation().stop(); if (isResting()) wakeUp(); }

		@Override
		public void tick() {
			if (isResting()) return;
			BlockPos home = getHome().orElseThrow();
			Vec3 pillow = new Vec3(home.getX() + 0.5, home.getY() + 0.65, home.getZ() + 0.5);
			// Reaching the bed requires an unobstructed approach, not teleporting through the house wall.
			if (position().distanceToSqr(pillow) < 2.25 && level().clip(new ClipContext(getEyePosition(), pillow,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MinionEntity.this)).getType() == HitResult.Type.MISS) {
				restInBed(home);
			} else if (--repathDelay <= 0) {
				repathDelay = 20;
				getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, 1);
			}
		}
	}

	private final class WorkAtCashRegisterGoal extends Goal {
		private BlockPos workstation;
		private int searchCooldown;
		private int repathDelay;

		private WorkAtCashRegisterGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
		@Override public boolean canUse() {
			if (level().isNight() || isResting() || --searchCooldown > 0) return false;
			searchCooldown = 40;
			return (workstation = findWorkstation()) != null;
		}
		@Override public boolean canContinueToUse() { return !level().isNight() && !isResting() && workstation != null && (isWorkingAt(workstation) || validWorkstation(workstation)); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void start() { repathDelay = 0; setWorkstation(null); }
		@Override public void stop() { getNavigation().stop(); setWorkstation(null); workstation = null; }
		@Override public void tick() {
			if (workstation == null || !validWorkstation(workstation) && !isWorkingAt(workstation)) { stop(); return; }
			Vec3 target = new Vec3(workstation.getX() + 0.5, workstation.getY() + 1, workstation.getZ() + 0.5);
			if (position().distanceToSqr(target) < 4.0) {
				getNavigation().stop();
				setDeltaMovement(Vec3.ZERO);
				setWorkstation(workstation);
				setProfession(level().getBlockState(workstation));
			} else if (--repathDelay <= 0) {
				repathDelay = 20;
				setWorkstation(null);
				getNavigation().moveTo(target.x, target.y, target.z, 1);
			}
		}
	}
}
