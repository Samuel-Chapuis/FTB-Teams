package dev.ftb.mods.ftbteams.client.entity;

import dev.ftb.mods.ftbteams.world.entity.MinionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** The renderer shrinks the whole player; these parts compensate to preserve the adult head size. */
public class MinionModel extends PlayerModel<MinionEntity> {
	public MinionModel(ModelPart root) {
		super(root, false);
	}

	@Override
	public void setupAnim(MinionEntity entity, float swing, float swingAmount, float age, float headYaw, float headPitch) {
		super.setupAnim(entity, swing, swingAmount, age, headYaw, headPitch);
		head.xScale = head.yScale = head.zScale = MinionEntity.BODY_DIVISOR;
		hat.xScale = hat.yScale = hat.zScale = MinionEntity.BODY_DIVISOR;
		if (entity.isResting()) {
			head.xRot = head.yRot = head.zRot = 0;
			hat.copyFrom(head);
		}
		if (entity.isSittingOnBench()) {
			rightArm.xRot = leftArm.xRot = -0.35F;
			rightLeg.xRot = leftLeg.xRot = -1.35F;
			rightLeg.yRot = 0.12F;
			leftLeg.yRot = -0.12F;
		}
	}
}
