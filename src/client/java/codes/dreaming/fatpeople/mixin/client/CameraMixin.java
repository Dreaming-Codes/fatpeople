package codes.dreaming.fatpeople.mixin.client;

import codes.dreaming.fatpeople.RollingAccessor;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final float CAMERA_ROLL_SPEED = 20.0F;

    @Shadow protected abstract void setRotation(float f, float g);
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void rotateCamera(net.minecraft.world.level.BlockGetter blockGetter, Entity entity, boolean isDetached, boolean bl2, float partialTicks, CallbackInfo ci) {
        if (!isDetached && entity instanceof RollingAccessor accessor && accessor.isRolling()) {
            float time = entity.tickCount + partialTicks;
            float rollAngle = time * CAMERA_ROLL_SPEED;
            this.setRotation(this.getYRot(), this.getXRot() + rollAngle);
        }
    }
}
