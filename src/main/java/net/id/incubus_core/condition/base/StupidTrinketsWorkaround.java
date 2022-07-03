package net.id.incubus_core.condition.base;

import dev.emi.trinkets.api.TrinketsApi;
import net.id.incubus_core.condition.api.ConditionModifier;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.stream.Collectors;

// TODO this is stupid. It works, probably, but it's stupid.
// This class only gets loaded if trinkets exists
public final class StupidTrinketsWorkaround {
    public static List<ItemStack> getTrinketStuffs(Entity player) {
        return TrinketsApi.TRINKET_COMPONENT.get(player)
                .getEquipped(stack -> stack.getItem() instanceof ConditionModifier)
                .stream().map(Pair::getRight).collect(Collectors.toList());
    }
}
