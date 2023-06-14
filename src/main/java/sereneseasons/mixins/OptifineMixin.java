package sereneseasons.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import sereneseasons.season.SeasonASMHelper;
import shadersmod.uniform.ShaderParameterFloat;

@Mixin(value = ShaderParameterFloat.class)
public class OptifineMixin {
	@Redirect(
			method = "eval",
			at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;getBiomeGenForCoords(II)Lnet/minecraft/world/biome/BiomeGenBase;"))
	public BiomeGenBase getBiomeSerene(World world, int x, int z) {
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if (SeasonASMHelper.getFloatTemperature(world, biome, x, 50, z) < 0.15) {
			biome = BiomeGenBase.coldTaiga;
		}
		return biome;
	}
}
