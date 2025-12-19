package codes.dreaming.fatpeople.mixin.client;

import codes.dreaming.fatpeople.RollingAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    
    private static final float ROLL_ANGLE_SPEED_MULTIPLIER = 20.0f;

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> entityModel, float f) {
        super(context, entityModel, f);
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    protected void applyRollingRotation(AbstractClientPlayer entity, PoseStack poseStack, float f, float g, float partialTicks, CallbackInfo ci) {
        if (entity instanceof RollingAccessor accessor && accessor.isRolling()) {
            float angle = accessor.getRollAngle();
            float speed = accessor.getRollingSpeed() * ROLL_ANGLE_SPEED_MULTIPLIER;
            float smoothAngle = angle + (speed * partialTicks);
            
            poseStack.translate(0.0D, 1.0D, 0.0D); 
            poseStack.mulPose(Vector3f.XP.rotationDegrees(-smoothAngle));
            poseStack.translate(0.0D, -1.0D, 0.0D); 
        }
    }
}
