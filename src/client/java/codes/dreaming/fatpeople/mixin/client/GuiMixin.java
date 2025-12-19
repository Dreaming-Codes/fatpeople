package codes.dreaming.fatpeople.mixin.client;

import codes.dreaming.fatpeople.RollingAccessor;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    private static final int STRUGGLE_BAR_WIDTH = 100;
    private static final int STRUGGLE_BAR_HEIGHT = 10;
    private static final int STRUGGLE_BAR_Y_OFFSET = 50;
    private static final int BAR_BACKGROUND_COLOR = 0xFF000000;
    private static final int BAR_PROGRESS_COLOR = 0xFF00FF00;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderStruggleBar(PoseStack poseStack, float f, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof RollingAccessor accessor && accessor.isRolling()) {
            float progress = accessor.getStruggleProgress();
            
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            
            int x = width / 2 - STRUGGLE_BAR_WIDTH / 2;
            int y = height - STRUGGLE_BAR_Y_OFFSET;
            
            Gui.fill(poseStack, x, y, x + STRUGGLE_BAR_WIDTH, y + STRUGGLE_BAR_HEIGHT, BAR_BACKGROUND_COLOR);
            int progressWidth = (int) (STRUGGLE_BAR_WIDTH * progress);
            Gui.fill(poseStack, x, y, x + progressWidth, y + STRUGGLE_BAR_HEIGHT, BAR_PROGRESS_COLOR);
        }
    }
}
