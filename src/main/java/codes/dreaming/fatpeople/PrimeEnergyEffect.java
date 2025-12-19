package codes.dreaming.fatpeople;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PrimeEnergyEffect extends MobEffect {
    
    private static final int EFFECT_COLOR = 0x00FF00;
    private static final float FATNESS_BASE = 1.0f;
    private static final float FATNESS_PER_LEVEL = 0.2f;
    private static final float SPEED_BASE = 1.0f;
    private static final float SPEED_PER_LEVEL = 0.3f;
    private static final float WALL_RETENTION_BASE = 0.7f;
    private static final float WALL_RETENTION_BONUS = 0.15f;
    private static final float WALL_RETENTION_MAX = 0.98f;
    private static final int WALL_TICKS_BASE = 3;
    private static final float WIDTH_BASE = 1.0f;
    private static final float WIDTH_PER_LEVEL = 0.5f;
    
    public PrimeEnergyEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
    
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    
    public static float getFatnessMultiplier(int amplifier) {
        return FATNESS_BASE + (FATNESS_PER_LEVEL * (amplifier + 1));
    }
    
    public static float getSpeedMultiplier(int amplifier) {
        return SPEED_BASE + (SPEED_PER_LEVEL * (amplifier + 1));
    }
    
    public static float getWallSpeedRetention(int amplifier) {
        return Math.min(WALL_RETENTION_MAX, WALL_RETENTION_BASE + (WALL_RETENTION_BONUS * (amplifier + 1)) / (amplifier + 2));
    }
    
    public static int getWallCollisionTicks(int amplifier) {
        return WALL_TICKS_BASE + (3 + amplifier) * (amplifier + 1);
    }
    
    public static float getWidthMultiplier(int amplifier) {
        return WIDTH_BASE + (WIDTH_PER_LEVEL * (amplifier + 1));
    }
}
