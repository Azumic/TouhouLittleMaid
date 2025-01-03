package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class MaidAttackTridentTask extends Behavior<EntityMaid> {
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public MaidAttackTridentTask() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT),
                1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        return this.hasTrident(owner) &&
                owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                        .filter(Entity::isAlive)
                        .filter(e -> owner.isWithinRestriction(e.blockPosition()))
                        .isPresent();
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            int maxAttackDistance = 30;
            double distance = owner.distanceTo(target);

            // 如果在最大攻击距离之内，而且看见的时长足够长
            if (distance < maxAttackDistance) {
                ++this.strafingTime;
            } else {
                this.strafingTime = -1;
            }

            // 如果攻击时间也足够长，随机对走位方向和前后走位进行反转
            // 原版10tick就可以投出去
            if (this.strafingTime >= 10) {
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            // 如果攻击时间大于 -1
            if (this.strafingTime > -1) {
                boolean isSafeArea = !(!owner.isUnderWater() && EnchantmentHelper.hasChanneling(owner.getMainHandItem())
                        && owner.level.isThundering() && owner.closerThan(target, Math.max(target.getBbWidth(), target.getBbHeight()) + 1.5));
                // 依据距离远近决定是否前后走位
                if (distance > maxAttackDistance * 0.5) {
                    this.strafingBackwards = false;
                } else if (distance < maxAttackDistance * 0.2 || !isSafeArea) {
                    this.strafingBackwards = true;
                }

                // 应用走位
                owner.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                owner.setYRot(Mth.rotateIfNecessary(owner.getYRot(), owner.yHeadRot, 0.0F));
                BehaviorUtils.lookAtEntity(owner, target);
            } else {
                // 否则只朝向攻击目标
                BehaviorUtils.lookAtEntity(owner, target);
            }
        });
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(true);
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(false);
        entityIn.getMoveControl().strafe(0, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        return this.checkExtraStartConditions(worldIn, entityIn);
    }

    // 必须要有忠诚附魔，硬性条件(虽说代码上可不用强制要求...)
    // 不然女仆有时miss了，三叉戟飞的太远...
    // 捡不回来，玩家：我三叉戟呢:(
    private boolean hasTrident(EntityMaid maid) {
        return maid.getMainHandItem().getItem() instanceof TridentItem && EnchantmentHelper.getLoyalty(maid.getMainHandItem()) > 0;
    }
}
