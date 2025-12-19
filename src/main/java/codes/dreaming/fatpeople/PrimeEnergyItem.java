package codes.dreaming.fatpeople;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PrimeEnergyItem extends Item {
    
    private static final int DRINK_DURATION_TICKS = 32;
    private static final int BASE_EFFECT_DURATION_TICKS = 20 * 60 * 3;
    private static final int MAX_AMPLIFIER = 4;
    
    public PrimeEnergyItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
                serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            }
            
            if (!level.isClientSide) {
                MobEffectInstance existingEffect = player.getEffect(FatPeople.PRIME_ENERGY_EFFECT);
                int newAmplifier = 0;
                int duration = BASE_EFFECT_DURATION_TICKS;
                
                if (existingEffect != null) {
                    newAmplifier = Math.min(existingEffect.getAmplifier() + 1, MAX_AMPLIFIER);
                    duration = existingEffect.getDuration() + duration;
                }
                
                player.addEffect(new MobEffectInstance(
                    FatPeople.PRIME_ENERGY_EFFECT,
                    duration,
                    newAmplifier,
                    false,
                    true,
                    true
                ));
            }
            
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return stack;
    }
    
    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION_TICKS;
    }
    
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
