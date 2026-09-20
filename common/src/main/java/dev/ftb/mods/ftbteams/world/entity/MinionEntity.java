package dev.ftb.mods.ftbteams.world.entity;

import dev.ftb.mods.ftbteams.world.block.PopBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/** A persistent faction resident: daytime strolls and a nightly walk back to its own Pop Bed. */
public class MinionEntity extends PathfinderMob {
	public static final float BODY_DIVISOR = 1.75F;
	private static final EntityDataAccessor<Optional<BlockPos>> HOME = SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private UUID faction;

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
	}

	public void assignHome(BlockPos home, UUID faction) {
		entityData.set(HOME, Optional.of(home.immutable()));
		this.faction = faction;
		restrictTo(home, 24);
	}

	public Optional<BlockPos> getHome() {
		return entityData.get(HOME);
	}

	public UUID getFactionId() {
		return faction;
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
		goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1));
		goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override public boolean canUse() { return !level().isNight() && !isResting() && super.canUse(); }
			@Override public boolean canContinueToUse() { return !level().isNight() && !isResting() && super.canContinueToUse(); }
		});
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6) {
			@Override public boolean canUse() { return !isResting() && super.canUse(); }
		});
		goalSelector.addGoal(6, new RandomLookAroundGoal(this) {
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
		if (level() instanceof ServerLevel server && getHome().isPresent()) {
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
		super.tick();
		if (!level().isClientSide && isAlive() && isResting()) getHome().ifPresent(this::restInBed);
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
		if (faction != null) tag.putUUID("MinionFaction", faction);
		tag.putBoolean("MinionResting", isResting());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("MinionHome") && tag.hasUUID("MinionFaction")) {
			assignHome(BlockPos.of(tag.getLong("MinionHome")), tag.getUUID("MinionFaction"));
			if (tag.getBoolean("MinionResting")) restInBed(getHome().orElseThrow());
		}
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
}
