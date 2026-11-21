package com.abilitybooks.ability;

import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Ability {
    @FunctionalInterface
    public interface Action {
        /** @return true, если способность сработала (и надо запустить перезарядку) */
        boolean use(ServerPlayerEntity player);
    }

    public final String id;
    public final String name;
    public final int cooldownTicks;
    /** null = способность открыта с самого начала */
    public final Item requirementItem;
    public final int requirementCount;
    public final String requirementText;
    public final Action action;

    public Ability(String id, String name, int cooldownTicks,
                   Item requirementItem, int requirementCount, String requirementText, Action action) {
        this.id = id;
        this.name = name;
        this.cooldownTicks = cooldownTicks;
        this.requirementItem = requirementItem;
        this.requirementCount = requirementCount;
        this.requirementText = requirementText;
        this.action = action;
    }

    public boolean isFree() {
        return requirementItem == null;
    }
}
