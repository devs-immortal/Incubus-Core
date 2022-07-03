package net.id.incubus_core.dev;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.id.incubus_core.IncubusCore;
import net.id.incubus_core.condition.Condition;
import net.id.incubus_core.condition.api.Severity;
import net.id.incubus_core.dev.item.EntityDeathMessageTestItem;
import net.id.incubus_core.woodtypefactory.api.WoodSettingsFactory;
import net.id.incubus_core.woodtypefactory.api.WoodTypeFactory;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import static net.id.incubus_core.IncubusCore.*;
import static net.minecraft.item.Items.DIAMOND;

/**
 * Development only stuff.
 * <p>
 * Used for testing internal things without littering production environments.
 */
public final class DevInit {
    static {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new RuntimeException("Incubus Core development classes loaded in production, this should not happen!");
        }
    }
    
    private static final ItemGroup GROUP = FabricItemGroupBuilder.build(IncubusCore.locate("dev"), () -> new ItemStack(DIAMOND));
    private static final Item ENTITY_DEATH_MESSAGE_ITEM = new EntityDeathMessageTestItem(new FabricItemSettings().group(GROUP));
    private static final Condition TEST_CONDITION = new Condition(TagKey.of(Registry.ENTITY_TYPE_KEY, new Identifier("a")), 100, 100, 5, 5, 1, 50) {
        @Override
        public void tick(World world, LivingEntity entity, Severity severity, float rawSeverity) {
        }

        @Override
        public void tickPlayer(World world, PlayerEntity player, Severity severity, float rawSeverity) {
            if (severity.isAsOrMoreSevere(Severity.MILD)) {
                player.addStatusEffect(new StatusEffectInstance(ZONKED, 10));
            }
        }

        @Override
        public void clientTick(ClientWorld world, LivingEntity entity, Severity severity, float rawSeverity) {
        }
    };

    private static final WoodTypeFactory INCUBUS_WOOD =
            new WoodTypeFactory(
                    new WoodSettingsFactory(MapColor.BLACK, MapColor.BRIGHT_RED),
                    MODID, "incubus"
            );
    private static final PillarBlock INCUBUS_LOG = INCUBUS_WOOD.log();
    private static final Block INCUBUS_PLANKS = INCUBUS_WOOD.planks();
    private static final Block INCUBUS_CHEST = INCUBUS_WOOD.chestFactory().chest;
    private static final Block INCUBUS_SIGN = INCUBUS_WOOD.signFactory().signBlock;
    private static final Item INCUBUS_BOAT = INCUBUS_WOOD.boatFactory(GROUP).item;

    public static void commonInit() {
        registerItem("entity_death_message_item", ENTITY_DEATH_MESSAGE_ITEM);

        INCUBUS_WOOD.registerCreatedBlocksAndItems(GROUP, GROUP, GROUP, GROUP);
    }

    public static void clientInit() {
        INCUBUS_WOOD.registerBlockEntityRenderers();
        Registry.register(Condition.CONDITION_REGISTRY, IncubusCore.locate("test_condition"), TEST_CONDITION);
    }
}
