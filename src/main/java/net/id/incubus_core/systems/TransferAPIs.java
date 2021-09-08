package net.id.incubus_core.systems;

import net.id.incubus_core.IncubusCore;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class TransferAPIs {
    public static final BlockApiLookup<HeatIo, @NotNull Direction> HEAT =
            BlockApiLookup.get(IncubusCore.id("heat_system"), HeatIo.class, Direction.class);

    public static final BlockApiLookup<KineticIo, @NotNull Direction> KINETIC =
            BlockApiLookup.get(IncubusCore.id("kinetic_system"), KineticIo.class, Direction.class);

    public static final BlockApiLookup<PressureIo, @NotNull Direction> PRESSURE =
            BlockApiLookup.get(IncubusCore.id("pressure_system"), PressureIo.class, Direction.class);

    public static final BlockApiLookup<PulseIo, @NotNull Direction> PULSE =
            BlockApiLookup.get(IncubusCore.id("pulse_system"), PulseIo.class, Direction.class);

    public static final BlockApiLookup<MaterialProvider, @Nullable Void> MATERIAL =
            BlockApiLookup.get(IncubusCore.id("material_lookup"), MaterialProvider.class, Void.class);



    public static long pressureInverse(double pressure) {
        if(pressure <= 0) {
            return 0;
        }
        return (long) Math.max(0, (Math.sqrt(pressure) * 36586.544243));
    }

    public static double accelerationFromPressureGradient(double self, double other) {
        return Math.sqrt(Math.abs(self - other)) * ((self < other) ? -1 : 1);
    }
}