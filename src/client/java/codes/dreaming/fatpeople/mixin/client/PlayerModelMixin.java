package codes.dreaming.fatpeople.mixin.client;

import codes.dreaming.fatpeople.FatPeople;
import codes.dreaming.fatpeople.PrimeEnergyEffect;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void makeFat(T entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        MobEffectInstance primeEffect = entity.getEffect(FatPeople.PRIME_ENERGY_EFFECT);
        
        if (primeEffect == null) {
            this.body.xScale = 1.0F;
            this.body.yScale = 1.0F; 
            this.body.zScale = 1.0F;
            
            this.leftArm.xScale = 1.0F;
            this.leftArm.zScale = 1.0F;
            this.rightArm.xScale = 1.0F;
            this.rightArm.zScale = 1.0F;
            this.leftLeg.xScale = 1.0F;
            this.leftLeg.zScale = 1.0F;
            this.rightLeg.xScale = 1.0F;
            this.rightLeg.zScale = 1.0F;
            return;
        }
        
        float fatnessMultiplier = PrimeEnergyEffect.getFatnessMultiplier(primeEffect.getAmplifier());
        
        this.body.xScale = fatnessMultiplier;
        this.body.yScale = 1.0F; 
        this.body.zScale = fatnessMultiplier;
        
        this.leftArm.xScale = fatnessMultiplier;
        this.leftArm.zScale = fatnessMultiplier;
        this.rightArm.xScale = fatnessMultiplier;
        this.rightArm.zScale = fatnessMultiplier;
        this.leftLeg.xScale = fatnessMultiplier;
        this.leftLeg.zScale = fatnessMultiplier;
        this.rightLeg.xScale = fatnessMultiplier;
        this.rightLeg.zScale = fatnessMultiplier;
    }
}
