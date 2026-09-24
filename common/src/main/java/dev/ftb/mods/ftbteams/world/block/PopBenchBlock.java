package dev.ftb.mods.ftbteams.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A lateral two-block bench whose two halves are placed and removed together. */
public final class PopBenchBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<PopBenchBlock> CODEC = simpleCodec(PopBenchBlock::new);
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	private static final VoxelShape NORTH_SHAPE = Shapes.or(Block.box(0, 0, 3, 16, 7, 16), Block.box(0, 7, 3, 16, 15, 6));
	private static final VoxelShape SOUTH_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 7, 13), Block.box(0, 7, 10, 16, 15, 13));
	private static final VoxelShape EAST_SHAPE = Shapes.or(Block.box(0, 0, 0, 13, 7, 16), Block.box(10, 7, 0, 13, 15, 16));
	private static final VoxelShape WEST_SHAPE = Shapes.or(Block.box(3, 0, 0, 16, 7, 16), Block.box(3, 7, 0, 6, 15, 16));

	public PopBenchBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LEFT));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	/** Returns the direction from the left half toward the right half. */
	public static Direction rightDirection(BlockState state) {
		return state.getValue(FACING).getCounterClockWise();
	}

	/** Returns the other half's position for either part of the bench. */
	public static BlockPos partnerPos(BlockState state, BlockPos pos) {
		Direction right = rightDirection(state);
		return pos.relative(state.getValue(PART) == Part.LEFT ? right : right.getOpposite());
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockState state = defaultBlockState().setValue(FACING, facing);
		BlockPos right = context.getClickedPos().relative(rightDirection(state));
		return context.getLevel().getWorldBorder().isWithinBounds(right)
				&& context.getLevel().getBlockState(right).canBeReplaced(context) ? state : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide) {
			level.setBlock(partnerPos(state, pos), state.setValue(PART, Part.RIGHT), UPDATE_ALL);
			level.blockUpdated(pos, Blocks.AIR);
			state.updateNeighbourShapes(level, pos, UPDATE_ALL);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		Direction partnerDirection = state.getValue(PART) == Part.LEFT ? rightDirection(state) : rightDirection(state).getOpposite();
		if (direction == partnerDirection) {
			return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
					&& neighbor.getValue(FACING) == state.getValue(FACING) ? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative() && state.getValue(PART) == Part.LEFT) {
			BlockPos partner = partnerPos(state, pos);
			BlockState partnerState = level.getBlockState(partner);
			if (partnerState.is(this)) {
				level.setBlock(partner, Blocks.AIR.defaultBlockState(), UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, partner, Block.getId(partnerState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> NORTH_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case EAST -> EAST_SHAPE;
			default -> WEST_SHAPE;
		};
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** Named lateral halves used by block states and loot conditions. */
	public enum Part implements StringRepresentable {
		LEFT("left"), RIGHT("right");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
