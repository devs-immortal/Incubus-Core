package net.id.incubus_core.condition.api;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent;
import dev.onyxstudios.cca.api.v3.entity.PlayerComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.id.incubus_core.condition.IncubusCondition;
import net.id.incubus_core.condition.base.StupidTrinketsWorkaround;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ConditionManager implements AutoSyncedComponent, CommonTickingComponent, PlayerComponent<ConditionManager> {

    private final LivingEntity target;
    private final Set<ConditionTracker> conditionTrackers = new HashSet<>();

    @ApiStatus.Internal
    public ConditionManager(LivingEntity target) {
        this.target = target;
        var conditions = Condition.getValidConditions(target.getType());
        conditions.forEach(condition -> conditionTrackers.add(new ConditionTracker(condition)));
    }

    @ApiStatus.Internal
    @Override
    public void tick() {
        conditionTrackers.forEach(tracker -> {
            var condition = tracker.getCondition();

            float rawSeverity = getScaledSeverity(condition);
            var severity = Severity.getSeverity(rawSeverity);

            if(target instanceof PlayerEntity player) {
                condition.tickPlayer(player.world, player, severity, rawSeverity);
            }
            else {
                condition.tick(target.world, target, severity, rawSeverity);
            }

            tracker.remove(Persistence.TEMPORARY, getScaledDecay(Persistence.TEMPORARY, condition));
            tracker.remove(Persistence.CHRONIC, getScaledDecay(Persistence.CHRONIC, condition));
        });
    }

    @ApiStatus.Internal
    @Override
    @Environment(EnvType.CLIENT)
    public void clientTick() {
        CommonTickingComponent.super.clientTick();
        conditionTrackers.forEach(tracker -> {
            var condition = tracker.getCondition();

            float rawSeverity = getScaledSeverity(condition);
            var severity = Severity.getSeverity(rawSeverity);

            condition.clientTick((ClientWorld) target.world, target, severity, rawSeverity);
        });
    }

    /**
     * Sets the persistence of the condition to the given value
     */
    public boolean set(Condition condition, Persistence persistence, float value) {
        return this.getConditionTracker(condition).map(tracker -> {
            switch (persistence) {
                case TEMPORARY -> tracker.tempVal = value;
                case CHRONIC -> tracker.chronVal = value;
                case CONSTANT -> throw new IllegalArgumentException("Constant condition values may not be directly edited");
            }
            this.trySync();
            return true;
        }).orElse(false);
    }

    /**
     * Adds the specified amount to the specified persistence.
     */
    public void add(Condition condition, Persistence persistence, float amount) {
        this.getConditionTracker(condition).ifPresent(tracker -> tracker.add(persistence, amount));
        this.trySync();
    }

    /**
     * Removes the specified amount from the specified persistence
     */
    public void remove(Condition condition, Persistence persistence, float amount) {
        this.getConditionTracker(condition).ifPresent(tracker -> tracker.remove(persistence, amount));
        this.trySync();
    }

    /**
     * Clears all conditions for this entity
     */
    public boolean removeAll(){
        return conditionTrackers.stream().allMatch((tracker) ->
                set(tracker.parent, Persistence.TEMPORARY, 0)
                        && set(tracker.parent, Persistence.CHRONIC, 0));
    }

    /**
     * Removes a specified percentage of the specified condition.
     * @param amount The percentage of the condition to remove. 0.00 means no change, 1.00 means remove all.
     */
    public void removeScaled(Condition condition, float amount) {
        this.getConditionTracker(condition).ifPresent(tracker -> {
            float partial = tracker.getPartialCondition();
            float tempPart = tracker.tempVal / partial;
            float chronPart = tracker.chronVal / partial;
            tracker.remove(Persistence.TEMPORARY, amount * tempPart);
            tracker.remove(Persistence.CHRONIC, amount * chronPart);
        });
    }

    /**
     * Syncs this with the server and client
     */
    public void trySync() {
        IncubusCondition.CONDITION_MANAGER_KEY.sync(this.target);
    }

    /**
     * @return Whether this entity is immune to the specified condition.
     */
    @ApiStatus.Experimental
    public boolean isImmuneTo(Condition condition) {
        // Should be equivalent to return Condition#isApplicableTo(this.target);
        return conditionTrackers.stream().noneMatch(tracker -> tracker.getCondition() == condition);
    }

    /**
     * @return Whether the effects of the given condition are visible on this entity.
     */
    public boolean isVisible(Condition condition) {
        if (!condition.isApplicableTo(this.target)) return false;
        return this.getScaledSeverity(condition) >= condition.visThreshold;
    }

    private Optional<ConditionTracker> getConditionTracker(Condition condition){
        for (var tracker : conditionTrackers) {
            if (tracker.getCondition() == condition){
                return Optional.of(tracker);
            }
        }
        return Optional.empty();
    }

    /**
     * Tries to apply the specified condition with the specified persistence and amount.
     * @return Whether the condition could be applied.
     */
    public boolean tryApply(Condition condition, Persistence persistence, float amount) {
        var tracker = this.getConditionTracker(condition);
        if(tracker.isPresent() && persistence != Persistence.CONSTANT && !isImmuneTo(condition)) {
            tracker.get().add(persistence, amount);
            return true;
        }
        return false;
    }

    public float getScaledDecay(Persistence persistence, Condition condition) {
        return switch (persistence) {
            case TEMPORARY -> condition.tempDecay;
            case CHRONIC -> condition.chronDecay;
            default -> 0;
        } * getDecayMultiplier(condition);
    }

    public float getDecayMultiplier(Condition condition) {
        var modifiers = getActiveModifiers();
        if(!modifiers.isEmpty()) {
            return (float) modifiers.stream()
                    .mapToDouble(mod -> mod.getDecayMultiplier(condition))
                    .average().orElse(1);
        }
        return 1;
    }

    public float getScaledSeverity(Condition condition) {
        return MathHelper.clamp((getRawCondition(condition) / getScalingValueForCondition(condition)) * getSeverityMultiplier(condition), 0, 1);
    }

    public float getSeverityMultiplier(Condition condition) {
        return (float) getActiveModifiers().stream().mapToDouble(mod -> mod.getSeverityMultiplier(condition)).average().orElse(1);
    }

    public float getScalingValueForCondition(Condition condition) {
        var modifiers = getActiveModifiers();
        float scalingValue = condition.scalingValue;
        scalingValue *= modifiers.stream().mapToDouble(mod -> mod.getScalingMultiplier(condition)).average().orElse(1);
        scalingValue += modifiers.stream().mapToDouble(mod -> mod.getScalingOffset(condition)).sum();
        return scalingValue;
    }

    public float getRawCondition(Condition condition) {
        return this.getConditionTracker(condition).map(tracker -> {
            float partial = tracker.getPartialCondition();
            partial += getActiveModifiers().stream().mapToDouble(mod -> mod.getConstantCondition(condition)).sum();
            return partial;
        }).orElseThrow(() -> new IllegalStateException("HOW in the FUCK do you get an invalid condition here: " + condition.getId()));
    }

    public List<ConditionModifier> getActiveModifiers() {
        List<ConditionModifier> modifiers = new ArrayList<>();
        if (target instanceof PlayerEntity player) {
            List<ItemStack> stacks;
            if (FabricLoader.getInstance().isModLoaded("trinkets")) {
                stacks = StupidTrinketsWorkaround.getTrinketStuffs(player);
            } else {
                stacks = new ArrayList<>();
            }

            player.getArmorItems().forEach(stack -> {
                if(stack.getItem() instanceof ConditionModifier)
                    stacks.add(stack);
            });

            if(player.getMainHandStack().getItem() instanceof ConditionModifier)
                stacks.add(player.getMainHandStack());

            if(player.getOffHandStack().getItem() instanceof ConditionModifier)
                stacks.add(player.getOffHandStack());

            stacks.forEach(stack -> modifiers.add((ConditionModifier) stack.getItem()));
        }

        target.getActiveStatusEffects().forEach((statusEffect, statusEffectInstance) -> {
            if(statusEffect instanceof ConditionModifier modifier) {
                modifiers.add(modifier);
            }
        });

        return modifiers;
    }

    @ApiStatus.Internal
    @Override
    public void readFromNbt(NbtCompound tag) {
        conditionTrackers.forEach(tracker -> {
            var condition = tracker.getCondition();
            if(tag.contains(condition.getId().toString())) {
                //noinspection ConstantConditions
                tracker.fromNbt((NbtCompound) tag.get(condition.getId().toString()));
            }
        });
    }

    @ApiStatus.Internal
    @Override
    public void writeToNbt(NbtCompound tag) {
        conditionTrackers.forEach(tracker -> {
            var nbt = new NbtCompound();
            tracker.writeToNbt(nbt);
            tag.put(tracker.getCondition().getId().toString(), nbt);
        });
    }

    @ApiStatus.Internal
    @Override
    public void copyFrom(ConditionManager other) {
        PlayerComponent.super.copyFrom(other);
    }

    @ApiStatus.Internal
    @Override
    public void copyForRespawn(ConditionManager original, boolean lossless, boolean keepInventory, boolean sameCharacter) {
        if(sameCharacter) {
            PlayerComponent.super.copyForRespawn(original, lossless, keepInventory, sameCharacter);
        }
    }

    @ApiStatus.Internal
    @Override
    public boolean shouldCopyForRespawn(boolean lossless, boolean keepInventory, boolean sameCharacter) {
        return false;
    }

    @ApiStatus.Internal
    private static class ConditionTracker {

        private final Condition parent;

        private float tempVal;
        private float chronVal;

        public ConditionTracker(Condition parent) {
            this.parent = parent;
        }

        public Condition getCondition(){
            return parent;
        }

        public void add(Persistence persistence, float amount) {
            switch (persistence) {
                case TEMPORARY -> tempVal = Math.min(parent.maxTemp, tempVal + amount);
                case CHRONIC -> chronVal = Math.min(parent.maxChron, chronVal + amount);
            }
        }

        public void remove(Persistence persistence, float amount) {
            switch (persistence) {
                case TEMPORARY -> tempVal = Math.max(0, tempVal - amount);
                case CHRONIC -> chronVal = Math.max(0, chronVal - amount);
            }
        }

        public float getPartialCondition() {
            return tempVal + chronVal;
        }

        public void fromNbt(NbtCompound nbt) {
            tempVal = nbt.getFloat("temporary");
            chronVal = nbt.getFloat("chronic");
        }

        public void writeToNbt(NbtCompound nbt) {
            nbt.putFloat("temporary", tempVal);
            nbt.putFloat("chronic", chronVal);
        }
    }
}
