package net.id.incubus_core.condition.base;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.id.incubus_core.IncubusCore;
import net.id.incubus_core.condition.IncubusCondition;
import net.id.incubus_core.condition.api.Condition;
import net.id.incubus_core.condition.api.ConditionManager;
import net.id.incubus_core.condition.api.Persistence;
import net.id.incubus_core.condition.api.Severity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ConditionCommand {

    public static final ConditionSuggester CONDITION_SUGGESTER = new ConditionSuggester();
    public static final SeveritySuggester SEVERITY_SUGGESTER = new SeveritySuggester();
    public static final PersistenceSuggester PERSISTENCE_SUGGESTER = new PersistenceSuggester();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal(IncubusCore.MODID + ":condition")
                        .requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("query")
                                .executes(context -> printCondition(context.getSource(), null, null))
                                .then(argument("target", EntityArgumentType.entities())
                                        .executes(context -> printCondition(context.getSource(), EntityArgumentType.getEntities(context, "target"), null))
                                        .then(argument("condition", IdentifierArgumentType.identifier()).suggests(CONDITION_SUGGESTER)
                                                .executes((context -> printCondition(context.getSource(), EntityArgumentType.getEntities(context, "target"), IdentifierArgumentType.getIdentifier(context, "condition")))))))
                        .then(literal("assign")
                                .then(argument("target", EntityArgumentType.entities())
                                        .then(argument("condition", IdentifierArgumentType.identifier()).suggests(CONDITION_SUGGESTER)
                                                .then(argument("persistence", StringArgumentType.word()).suggests(PERSISTENCE_SUGGESTER)
                                                        .then(argument("value", FloatArgumentType.floatArg()).suggests(SEVERITY_SUGGESTER)
                                                                .executes(context -> setCondition(context.getSource(), EntityArgumentType.getEntity(context, "target"), IdentifierArgumentType.getIdentifier(context, "condition"), FloatArgumentType.getFloat(context, "value"), StringArgumentType.getString(context, "persistence"))))))))
                        .then(literal("clear")
                                .executes(context -> clearCondition(context.getSource(), null, null))
                                .then(argument("target", EntityArgumentType.entities())
                                        .executes(context -> clearCondition(context.getSource(), EntityArgumentType.getEntities(context, "target"), null))
                                        .then(argument("condition", IdentifierArgumentType.identifier()).suggests(CONDITION_SUGGESTER)
                                                .executes(context -> clearCondition(context.getSource(), EntityArgumentType.getEntities(context, "target"), IdentifierArgumentType.getIdentifier(context, "condition"))))))
        );
    }

    private static int clearCondition(ServerCommandSource source, Collection<? extends Entity> targets, Identifier attributeId) {
        for (var entity : resolveTargets(source, targets)) {
            if (!(entity instanceof LivingEntity target)) {
                errorImmune(source, entity, null);
                continue;
            }

            var conditions = resolveConditions(source, attributeId, target);
            if (conditions.isEmpty()) continue;

            ConditionManager manager = target.getConditionManager();
            for (Condition condition : conditions) {
                manager.set(condition, Persistence.TEMPORARY, 0);
                manager.set(condition, Persistence.CHRONIC, 0);
            }
            manager.trySync();

            source.sendFeedback(
                    new TranslatableText(
                            "commands.incubus_core.condition.success.clear.individual",
                            conditions.size(),
                            entity.getDisplayName()
                    ),
                    true
            );
        }

        source.sendFeedback(new TranslatableText("commands.incubus_core.condition.success.clear"), true);
        return 1;
    }

    private static int printCondition(ServerCommandSource source, @Nullable Collection<? extends Entity> entities, Identifier attributeId) {
        for (Entity entity : resolveTargets(source, entities)) {
            if (!(entity instanceof LivingEntity target)) {
                errorImmune(source, entity, null);
                continue;
            }

            var conditions = resolveConditions(source, attributeId, target);
            for (Condition condition : conditions) {
                float rawSeverity = target.getConditionManager().getScaledSeverity(condition);
                Severity severity = Severity.getSeverity(rawSeverity);

                if (!condition.isApplicableTo(target)) {
                    errorImmune(source, target, condition);
                    continue;
                }

                source.sendFeedback(
                        new TranslatableText(
                                "commands.incubus_core.condition.success.query",
                                entity.getName(),
                                new TranslatableText(condition.getTranslationKey()),
                                new TranslatableText(severity.getTranslationKey()),
                                rawSeverity), false);
            }
        }
        return 1;
    }

    private static int setCondition(ServerCommandSource source, Entity entity, Identifier attributeId, float value, String persistenceString) {
        Optional<Condition> conditionOptional = Condition.getOrEmpty(attributeId);

        if (conditionOptional.isEmpty()) {
            source.sendError(new TranslatableText("commands.incubus_core.condition.failure.get_condition", attributeId));
            return 1;
        }

        Condition condition = conditionOptional.get();

        if (!(entity instanceof LivingEntity target))
            return errorImmune(source, entity, condition);

        Persistence persistence;

        try {
            persistence = Persistence.valueOf(persistenceString);
        } catch (IllegalArgumentException e) {
            source.sendError(
                    new TranslatableText(
                            "commands.incubus_core.condition.failure.get_persistence",
                            persistenceString));
            return 1;
        }
        if (!condition.isApplicableTo(target))
            return errorImmune(source, target, condition);

        var manager = target.getConditionManager();

        if(manager.set(condition, persistence, value)) {
            float rawSeverity = manager.getScaledSeverity(condition);
            Severity severity = Severity.getSeverity(rawSeverity);

            source.sendFeedback(
                    new TranslatableText("commands.incubus_core.condition.success.assign",
                            target.getName(),
                            new TranslatableText(condition.getTranslationKey()),
                            new TranslatableText(severity.getTranslationKey()),
                            rawSeverity), false);
            manager.trySync();
        } else {
            source.sendError(new TranslatableText("commands.incubus_core.condition.failure.assign"));
        }
        return 1;
    }

    private static int errorImmune(ServerCommandSource source, Entity target, Condition condition) {
        source.sendError(
                new TranslatableText(
                        "commands.incubus_core.condition.failure.query",
                        target.getName(),
                        condition == null ? "that condition!" :
                                new TranslatableText(condition.getTranslationKey()))
        );
        return 1;
    }

    private static Collection<? extends Entity> resolveTargets(ServerCommandSource source, Collection<? extends Entity> entities){
        if (entities == null) {
            // Apply to self if unspecified
            Entity sourceEntity = source.getEntity();
            if (sourceEntity != null) {
                return List.of(sourceEntity);
            }
            // Nothing can be targeted
            source.sendError(new TranslatableText("commands.incubus_core.condition.failure.get_entity"));
            return List.of();
        }

        return entities;
    }

    private static Collection<Condition> resolveConditions(ServerCommandSource source, Identifier attributeId, LivingEntity target){
        if (attributeId == null) {
            // If none specified, return all valid conditions.
            return Condition.getValidConditions(target.getType());
        }

        Optional<Condition> condition = Condition.getOrEmpty(attributeId);

        if (condition.isEmpty()) {
            // Invalid condition
            source.sendError(new TranslatableText("commands.incubus_core.condition.failure.get_condition", attributeId));
            return List.of();
        }

        return List.of(condition.get());
    }

    public static class ConditionSuggester implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            IncubusCondition.CONDITION_REGISTRY.getIds().forEach(id -> builder.suggest(id.toString()));
            return builder.buildFuture();
        }
    }

    public static class SeveritySuggester implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            Optional<Condition> conditionOptional = Condition.getOrEmpty(IdentifierArgumentType.getIdentifier(context, "condition"));

            if (conditionOptional.isEmpty()) {
                return builder.suggest(0).buildFuture();
            }

            Condition condition = conditionOptional.get();

            float max = condition.scalingValue;

            Arrays.stream(Severity.values()).sorted().forEach((severity) -> builder.suggest(Float.toString(Math.round(max * severity.triggerPercent))));
            builder.suggest(Float.toString(max));

            return builder.buildFuture();
        }
    }

    public static class PersistenceSuggester implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            builder.suggest(Persistence.TEMPORARY.name());
            builder.suggest(Persistence.CHRONIC.name());
            return builder.buildFuture();
        }
    }
}
