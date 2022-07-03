package net.id.incubus_core.condition.base;

import net.id.incubus_core.condition.IncubusCondition;
import net.id.incubus_core.condition.api.ConditionManager;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface IncubusConditionEntityExtensions {
    /**
     * @return The condition manager of this entity
     */
    // Should probably have "incubusCore$" at the beginning, but also, uh, I like this better. Sue me
    default @NotNull ConditionManager getConditionManager() {
        return IncubusCondition.CONDITION_MANAGER_KEY.get(this);
    }
}
