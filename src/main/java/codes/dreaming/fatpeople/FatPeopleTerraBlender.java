package codes.dreaming.fatpeople;

import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

public class FatPeopleTerraBlender implements TerraBlenderApi {
    private static final int BIOME_WEIGHT = 2;
    
    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new SuicideForestRegion(new ResourceLocation(FatPeople.MOD_ID, "overworld"), BIOME_WEIGHT));
    }
}
