package net.id.incubus_core.condition.api;

import net.id.incubus_core.condition.Condition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * An API intended to aid use of {@code Condition}s. <br>
 * <br>
 * ~ Jack
 * @author AzazelTheDemonLord
 */
@SuppressWarnings("unused")
@Deprecated(since = "1.7.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval
public final class ConditionAPI {
    /**
     * @param type The {@code EntityType} to test
     * @return A list of all conditions the given entity is not immune to.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static List<Condition> getValidConditions(EntityType<?> type) {
        return new ArrayList<>(Condition.getValidConditions(type));
    }

    /**
     * @deprecated
     * Use {@link Condition#getOrThrow(Identifier)} instead.
     * @param id The unique {@code Identifier} of the {@code Condition}.
     * @return The {@code Condition} corresponding to the given {@code Identifier}
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static Condition getOrThrow(Identifier id) {
        return Condition.getOrThrow(id);
    }

    /**
     * @deprecated
     * Use {@link ConditionManager#isVisible(Condition)} instead
     * @param condition The {@code Condition} to test
     * @param entity The entity to test
     * @return Whether the given {@code Condition} is currently outwardly
     * visible on the given entity.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static boolean isVisible(Condition condition, LivingEntity entity) {
        return entity.getConditionManager().isVisible(condition);
    }

    /**
     * @deprecated
     * Use {@link LivingEntity#getConditionManager()}
     * @param entity The entity you want to get the {@code ConditionManager} of.
     * @return The {@code ConditionManager} of the given entity.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static ConditionManager getConditionManager(LivingEntity entity) {
        return entity.getConditionManager();
    }

    /**
     * @deprecated
     * Use {@link ConditionManager#trySync()} instead. <br>
     * Syncs a given entity's {@code ConditionManager}.
     * @param entity The entity whose {@code ConditionManager} you wish to sync.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static void trySync(LivingEntity entity) {
        entity.getConditionManager().trySync();
    }

    /**
     * @deprecated
     * Use {@link Condition#getTranslationKey()} instead
     * @param condition The {@code Condition} you want the translation string of
     * @return The translation string of the given {@code Condition}
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public static String getTranslationString(Condition condition) {
        return condition.getTranslationKey();
    }
}
