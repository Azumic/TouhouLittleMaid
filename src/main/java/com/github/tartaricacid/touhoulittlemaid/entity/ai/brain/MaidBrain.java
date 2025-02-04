package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFollowOwnerVehicleTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ride.MaidRideBegTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

public final class MaidBrain {
    public static ImmutableList<MemoryModuleType<?>> getMemoryTypes() {
        List<MemoryModuleType<?>> defaultTypes = Lists.newArrayList(
                MemoryModuleType.PATH,
                MemoryModuleType.DOORS_TO_CLOSE,
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.NEAREST_HOSTILE,
                MemoryModuleType.HURT_BY,
                MemoryModuleType.HURT_BY_ENTITY,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.ATTACK_TARGET,
                MemoryModuleType.ATTACK_COOLING_DOWN,
                InitEntities.TARGET_POS.get(),
                InitEntities.CURRENT_ITEMSTACK.get()
        );
        ExtraMaidBrainManager.EXTRA_MAID_BRAINS.forEach(extra -> defaultTypes.addAll(extra.getExtraMemoryTypes()));
        return ImmutableList.copyOf(defaultTypes);
    }

    public static ImmutableList<SensorType<? extends Sensor<? super EntityMaid>>> getSensorTypes() {
        List<SensorType<? extends Sensor<? super EntityMaid>>> defaultTypes = Lists.newArrayList(
                InitEntities.MAID_NEAREST_LIVING_ENTITY_SENSOR.get(),
                SensorType.HURT_BY,
                InitEntities.MAID_HOSTILES_SENSOR.get(),
                InitEntities.MAID_PICKUP_ENTITIES_SENSOR.get()
        );
        ExtraMaidBrainManager.EXTRA_MAID_BRAINS.forEach(extra -> defaultTypes.addAll(extra.getExtraSensorTypes()));
        return ImmutableList.copyOf(defaultTypes);
    }

    public static void registerBrainGoals(Brain<EntityMaid> brain, EntityMaid maid) {
        registerSchedule(brain, maid);
        registerCoreGoals(brain);
        registerPanicGoals(brain);

        registerRideIdleGoals(brain);
        registerRideWorkGoals(brain, maid);
        registerRideRestGoals(brain);

        registerIdleGoals(brain);
        registerWorkGoals(brain, maid);
        registerRestGoals(brain);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        MaidUpdateActivityFromSchedule.updateActivityFromSchedule(maid, brain);
    }

    private static void registerSchedule(Brain<EntityMaid> brain, EntityMaid maid) {
        switch (maid.getSchedule()) {
            case ALL:
                brain.setSchedule(InitEntities.MAID_ALL_DAY_SCHEDULES.get());
                break;
            case NIGHT:
                brain.setSchedule(InitEntities.MAID_NIGHT_SHIFT_SCHEDULES.get());
                break;
            case DAY:
            default:
                brain.setSchedule(InitEntities.MAID_DAY_SHIFT_SCHEDULES.get());
                break;
        }
    }

    private static void registerCoreGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> swimJump = Pair.of(0, new MaidSwimJumpTask(0.8f));
        Pair<Integer, BehaviorControl<? super EntityMaid>> breathAirEaten = Pair.of(0, new MaidBreathAirEatenTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> climb = Pair.of(0, new MaidClimbTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> look = Pair.of(0, new LookAtTargetSink(45, 90));
        Pair<Integer, BehaviorControl<? super EntityMaid>> maidPanic = Pair.of(1, new MaidPanicTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> maidAwait = Pair.of(1, new MaidAwaitTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> interactWithDoor = Pair.of(2, MaidInteractWithDoor.create());
        Pair<Integer, BehaviorControl<? super EntityMaid>> walkToTarget = Pair.of(2, new MoveToTargetSink());
        Pair<Integer, BehaviorControl<? super EntityMaid>> followOwner = Pair.of(3, new MaidFollowOwnerTask(0.5f, 2));
        Pair<Integer, BehaviorControl<? super EntityMaid>> followOwnerVehicle = Pair.of(3, new MaidFollowOwnerVehicleTask(0.5f, 2));
        Pair<Integer, BehaviorControl<? super EntityMaid>> healSelf = Pair.of(3, new MaidHealSelfTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> pickupItem = Pair.of(10, new MaidPickupEntitiesTask(EntityMaid::isPickup, 0.6f));
        Pair<Integer, BehaviorControl<? super EntityMaid>> clearSleep = Pair.of(99, new MaidClearSleepTask());

        brain.addActivity(Activity.CORE, ImmutableList.of(swimJump, climb, breathAirEaten, look, maidPanic, maidAwait, interactWithDoor,
                walkToTarget, followOwner, followOwnerVehicle, healSelf, pickupItem, clearSleep));
    }

    private static void registerIdleGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> beg = Pair.of(5, new MaidBegTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> homeMeal = Pair.of(6, new MaidFindHomeMealTask(0.6f, 2));
        Pair<Integer, BehaviorControl<? super EntityMaid>> joy = Pair.of(7, new MaidJoyTask(0.6f, 2));
        Pair<Integer, BehaviorControl<? super EntityMaid>> supplemented = Pair.of(20, getLookAndRandomWalk());
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());

        brain.addActivity(Activity.IDLE, ImmutableList.of(beg, homeMeal, joy, supplemented, updateActivity));
    }

    private static void registerWorkGoals(Brain<EntityMaid> brain, EntityMaid maid) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());
        IMaidTask task = maid.getTask();
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> pairMaidList = task.createBrainTasks(maid);
        if (pairMaidList.isEmpty()) {
            pairMaidList = Lists.newArrayList(updateActivity);
        } else {
            pairMaidList.add(updateActivity);
        }
        pairMaidList.add(Pair.of(6, new MaidBegTask()));
        pairMaidList.add(Pair.of(7, new MaidWorkMealTask()));
        if (task.enableLookAndRandomWalk(maid)) {
            pairMaidList.add(Pair.of(20, getLookAndRandomWalk()));
        }
        brain.addActivity(Activity.WORK, ImmutableList.copyOf(pairMaidList));
    }

    private static void registerRestGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> bed = Pair.of(5, new MaidBedTask(0.6f, 2));
        Pair<Integer, BehaviorControl<? super EntityMaid>> supplemented = Pair.of(20, getLookAndRandomWalk());
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());

        brain.addActivity(Activity.REST, ImmutableList.of(bed, supplemented, updateActivity));
    }

    private static void registerPanicGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> clearHurt = Pair.of(5, new MaidClearHurtTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> runAway = Pair.of(5, MaidRunAwayTask.entity(MemoryModuleType.NEAREST_HOSTILE, 0.7f, false));

        brain.addActivity(Activity.PANIC, ImmutableList.of(clearHurt, runAway));
    }

    private static void registerRideIdleGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> beg = Pair.of(4, new MaidRideBegTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> homeMeal = Pair.of(5, new MaidHomeMealTask());
        Pair<Integer, BehaviorControl<? super EntityMaid>> look = Pair.of(6, getLook());
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());

        brain.addActivity(InitEntities.RIDE_IDLE.get(), ImmutableList.of(beg, homeMeal, look, updateActivity));
    }

    private static void registerRideWorkGoals(Brain<EntityMaid> brain, EntityMaid maid) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());
        IMaidTask task = maid.getTask();
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> pairMaidList = task.createRideBrainTasks(maid);
        if (pairMaidList.isEmpty()) {
            pairMaidList = Lists.newArrayList(updateActivity);
        } else {
            pairMaidList.add(updateActivity);
        }
        pairMaidList.add(Pair.of(6, new MaidRideBegTask()));
        pairMaidList.add(Pair.of(7, new MaidWorkMealTask()));
        if (task.enableLookAndRandomWalk(maid)) {
            pairMaidList.add(Pair.of(20, getLook()));
        }
        brain.addActivity(InitEntities.RIDE_WORK.get(), ImmutableList.copyOf(pairMaidList));
    }

    private static void registerRideRestGoals(Brain<EntityMaid> brain) {
        Pair<Integer, BehaviorControl<? super EntityMaid>> updateActivity = Pair.of(99, new MaidUpdateActivityFromSchedule());
        brain.addActivity(InitEntities.RIDE_REST.get(), ImmutableList.of(updateActivity));
    }

    private static MaidRunOne getLookAndRandomWalk() {
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToPlayer = Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToMaid = Pair.of(SetEntityLookTarget.create(EntityMaid.TYPE, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToWolf = Pair.of(SetEntityLookTarget.create(EntityType.WOLF, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToCat = Pair.of(SetEntityLookTarget.create(EntityType.CAT, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToParrot = Pair.of(SetEntityLookTarget.create(EntityType.PARROT, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> walkRandomly = Pair.of(RandomStroll.stroll(0.3f, 5, 3), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> noLook = Pair.of(new DoNothing(30, 60), 2);
        return new MaidRunOne(ImmutableList.of(lookToPlayer, lookToMaid, lookToWolf, lookToCat, lookToParrot, walkRandomly, noLook));
    }

    private static MaidRunOne getLook() {
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToPlayer = Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToMaid = Pair.of(SetEntityLookTarget.create(EntityMaid.TYPE, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToWolf = Pair.of(SetEntityLookTarget.create(EntityType.WOLF, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToCat = Pair.of(SetEntityLookTarget.create(EntityType.CAT, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> lookToParrot = Pair.of(SetEntityLookTarget.create(EntityType.PARROT, 5), 1);
        Pair<BehaviorControl<? super EntityMaid>, Integer> noLook = Pair.of(new DoNothing(30, 60), 2);
        return new MaidRunOne(ImmutableList.of(lookToPlayer, lookToMaid, lookToWolf, lookToCat, lookToParrot, noLook));
    }
}
