package net.id.incubus_core.mixin.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.id.incubus_core.util.SeedSupplier;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GeneratorOptions.class)
public abstract class GeneratorOptionsMixin {
	// private static synthetic method_28606(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;
    @Redirect(
		method = "method_28606(Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;)Lcom/mojang/datafixers/kinds/App;",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/serialization/codecs/PrimitiveCodec;fieldOf(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;",
			ordinal = 0
		)
	)
    private static MapCodec<Long> giveUsRandomSeeds(PrimitiveCodec<Long> codec, final String name) {
        return codec.fieldOf(name).orElseGet(SeedSupplier::getSeed);
    }
}
