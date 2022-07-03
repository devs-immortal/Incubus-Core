package net.id.incubus_core.condition.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.id.incubus_core.condition.IncubusCondition;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>   These are supposed to be flywheel objects ya nut.
 * <br>  No you don't get any further documentation, go nag jack.
 * <br>  [ - insert gigachad image - ]
 * <br>  ~ Azzy
 * <br>
 * <br>  Jack here, these are conditions.
 * <br>  {@link Condition#tick} is ticked for every not-exempt {@code LivingEntity} once every tick, as provided by {@link dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent}
 * <br>  {@link Condition#tickPlayer} is similarly ticked, but only if the entity is a {@code PlayerEntity}
 * <br>  {@link Condition#clientTick} is also similarly ticked, but only if the game is the client
 * <br>  All conditions, which extend this class, override these methods with their own effects.
 *
 * <br>  See the {@link Persistence} class for a description of {@link Persistence#TEMPORARY},
 *       {@link Persistence#CHRONIC}, and {@link Persistence#CONSTANT} persistences are.
 * <br>
 * <br>  Hope this helps!
 * <br>  ~ Jack. </p>
 *
 * @author AzazelTheDemonLord
 * @see Persistence
 */
public abstract class Condition {

    /**
     * @deprecated
     * This field will become private in a later version. <br>
     * A tag containing all {@code EntityType}s which cannot get this condition.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "1.7.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval
    public final TagKey<EntityType<?>> exempt;
    /**
     * The maximum value for the {@code Temporary} {@link Persistence}.
     */
    public final float maxTemp;
    /**
     * The maximum value for the {@code Chronic} {@link Persistence}.
     */
    public final float maxChron;
    /**
     * The decay rate of the {@code Temporary} {@link Persistence}.
     */
    public final float tempDecay;
    /**
     * The decay rate of the {@code Chronic} {@link Persistence}.
     */
    public final float chronDecay;
    /**
     * The value {@link Condition#maxTemp} and {@link Condition#maxChron} are normalized to.
     */
    public final float scalingValue;
    /**
     * The severity threshold after which the effects of the condition become visible.
     */
    public final float visThreshold;

    /**
     * @param exempt See {@link Condition#exempt}
     * @param maxTemp See {@link Condition#maxTemp}
     * @param maxChron See {@link Condition#maxChron}
     * @param tempDecay See {@link Condition#tempDecay}
     * @param chronDecay See {@link Condition#chronDecay}
     * @param scalingValue See {@link Condition#scalingValue}
     * @param visThreshold See {@link Condition#visThreshold}
     * @see Persistence
     */
    public Condition(TagKey<EntityType<?>> exempt, float maxTemp, float maxChron, float tempDecay, float chronDecay, float scalingValue, float visThreshold) {
        this.exempt = exempt;
        this.maxTemp = maxTemp;
        this.maxChron = maxChron;
        this.tempDecay = tempDecay;
        this.chronDecay = chronDecay;
        this.scalingValue = scalingValue;
        this.visThreshold = visThreshold;
    }

    /**
     * @return The {@link Identifier} for the condition, as registered in {@link IncubusCondition#CONDITION_REGISTRY}.
     */
    public final @Nullable Identifier getId(){
        return IncubusCondition.CONDITION_REGISTRY.getId(this);
    }

    /**
     * @deprecated
     * Use {@link #isApplicableTo(LivingEntity)}
     * @param entity A {@code LivingEntity} to be tested.
     * @return Whether the provided {@code LivingEntity} is exempt from the condition
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval
    public final boolean isExempt(LivingEntity entity) {
        return !isApplicableTo(entity);
    }

    /**
     * @return Whether this condition is applicable to the given entity
     */
    public final boolean isApplicableTo(LivingEntity entity) {
        return isApplicableTo(entity.getType());
    }

    /**
     * @return Whether this condition is applicable to the given entity type
     */
    public final boolean isApplicableTo(EntityType<?> entityType) {
        return !entityType.isIn(exempt);
    }

    /**
     * Processes the given entity, and applies any effects from the condition.
     * @param world The current {@code World}
     * @param entity The {@code LivingEntity} to process
     * @param severity The {@link Severity} of the {@code Condition}
     * @param rawSeverity The raw {@code float} value of the severity
     */
    public abstract void tick(World world, LivingEntity entity, Severity severity, float rawSeverity);

    /**
     * Processes the given player, and applies any effects from the condition.
     * @param world The current {@code World}
     * @param player The {@code PlayerEntity} to process
     * @param severity The {@link Severity} of the {@code Condition}
     * @param rawSeverity The raw {@code float} value of the severity
     * @see Condition#tick
     */
    public abstract void tickPlayer(World world, PlayerEntity player, Severity severity, float rawSeverity);

    /**
     * Processes the given entity, and applies any effects from the condition.
     * @param world The current {@code ClientWorld}
     * @param entity The {@code LivingEntity} to process
     * @param severity The {@link Severity} of the {@code Condition}
     * @param rawSeverity The raw {@code float} value of the severity
     * @see Condition#tick
     */
    @Environment(EnvType.CLIENT)
    public abstract void clientTick(ClientWorld world, LivingEntity entity, Severity severity, float rawSeverity);

    /**
     * @return The translation key of this condition
     */
    public final String getTranslationKey() {
        Identifier id = Objects.requireNonNull(this.getId(), this + " has not been registered.");
        return "condition." + id.getNamespace() + ".condition." + id.getPath();
    }

    /**
     * @deprecated It is recommended to use {@link #get(Identifier)} instead,
     * but this method isn't going anywhere.
     * @param id The unique {@code Identifier} of the desired {@code Condition}.
     * @return The {@code Condition} corresponding to the given {@code Identifier}
     * @throws NoSuchElementException if no condition is registered with the given id.
     */
    @Deprecated(since = "1.7.0")
    public static Condition getOrThrow(Identifier id) {
        return getOrEmpty(id).orElseThrow(() -> new NoSuchElementException("No Condition found registered for entry: " + id));
    }

    /**
     * @param id The unique {@code Identifier} of the desired {@code Condition}.
     * @return The {@code Condition} corresponding to the given {@code Identifier}
     */
    public static @Nullable Condition get(Identifier id) {
        return IncubusCondition.CONDITION_REGISTRY.get(id);
    }

    /**
     * @param id The unique {@code Identifier} of the desired {@code Condition}.
     * @return The {@code Condition} corresponding to the given {@code Identifier},
     *         contained within an {@code Optional}.
     */
    public static Optional<Condition> getOrEmpty(Identifier id) {
        return Optional.ofNullable(get(id));
    }

    /**
     * @param type The {@code EntityType} to test
     * @return A list of all conditions the given entity is not immune to.
     */
    public static Set<Condition> getValidConditions(EntityType<?> type) {
        return IncubusCondition.CONDITION_REGISTRY
                .stream()
                .filter(condition -> condition.isApplicableTo(type))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        var id = Objects.requireNonNullElse(this.getId(), "Unregistered");
        return this.getClass().getSimpleName() + "[" + id + "]";
    }
}
