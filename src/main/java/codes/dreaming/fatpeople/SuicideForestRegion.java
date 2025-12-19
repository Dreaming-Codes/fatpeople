package codes.dreaming.fatpeople;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class SuicideForestRegion extends Region {
    
    private static final float TEMPERATURE_MIN = 0.2f;
    private static final float TEMPERATURE_MAX = 1.0f;
    private static final float HUMIDITY_MIN = 0.3f;
    private static final float HUMIDITY_MAX = 1.0f;
    private static final float CONTINENTALNESS_MIN = -0.2f;
    private static final float CONTINENTALNESS_MAX = 0.2f;
    private static final float EROSION_MIN = -0.5f;
    private static final float EROSION_MAX = 0.5f;
    private static final float SURFACE_DEPTH = 0.0f;
    private static final float WEIRDNESS_MIN = -0.5f;
    private static final float WEIRDNESS_MAX = 0.5f;
    
    public SuicideForestRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }
    
    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addBiome(mapper,
            Climate.parameters(
                Climate.Parameter.span(TEMPERATURE_MIN, TEMPERATURE_MAX),
                Climate.Parameter.span(HUMIDITY_MIN, HUMIDITY_MAX),
                Climate.Parameter.span(CONTINENTALNESS_MIN, CONTINENTALNESS_MAX),
                Climate.Parameter.span(EROSION_MIN, EROSION_MAX),
                Climate.Parameter.point(SURFACE_DEPTH),
                Climate.Parameter.span(WEIRDNESS_MIN, WEIRDNESS_MAX),
                0L
            ),
            FatPeople.SUICIDE_FOREST_KEY
        );
    }
}
