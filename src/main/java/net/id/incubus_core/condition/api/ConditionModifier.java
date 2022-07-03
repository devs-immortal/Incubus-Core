package net.id.incubus_core.condition.api;

import net.id.incubus_core.condition.Condition;

@SuppressWarnings("unused")
public interface ConditionModifier {
    default float getDecayMultiplier(Condition condition) {
        return 1;
    }

    default float getScalingMultiplier(Condition condition) {
        return 1;
    }

    default float getScalingOffset(Condition condition) {
        return 0;
    }

    default float getSeverityMultiplier(Condition condition) {
        return 1;
    }

    default float getConstantCondition(Condition condition) {
        return 0;
    }
}