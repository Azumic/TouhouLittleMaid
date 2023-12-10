package com.github.tartaricacid.touhoulittlemaid.entity.favorability;

import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

public class FavorabilityManager {
    private static final String TAG_NAME = "FavorabilityManagerCounter";
    private final Map<String, Time> counter;

    public FavorabilityManager() {
        this.counter = Maps.newHashMap();
    }

    public void tick() {
        counter.values().forEach(Time::tick);
    }

    public void addCooldown(String type, int tickCount) {
        this.counter.put(type, new Time(tickCount));
    }

    public boolean canAdd(String type) {
        if (this.counter.containsKey(type)) {
            return this.counter.get(type).isZero();
        }
        return true;
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        CompoundTag data = new CompoundTag();
        this.counter.forEach((name, time) -> data.putInt(name, time.getTickCount()));
        compound.put(TAG_NAME, data);
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains(TAG_NAME, Tag.TAG_COMPOUND)) {
            CompoundTag data = compound.getCompound(TAG_NAME);
            for (String name : data.getAllKeys()) {
                this.counter.put(name, new Time(data.getInt(name)));
            }
        }
    }

    public static class Time {
        private int tickCount;

        public Time(int tickCount) {
            this.tickCount = tickCount;
        }

        public int getTickCount() {
            return tickCount;
        }

        public void setTickCount(int tickCount) {
            this.tickCount = tickCount;
        }

        public void tick() {
            if (tickCount > 0) {
                tickCount--;
            }
        }

        public boolean isZero() {
            return this.tickCount <= 0;
        }
    }
}
