package codes.dreaming.fatpeople;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HangingBodyRenderer extends EntityRenderer<HangingBodyEntity> {
    
    private static final ResourceLocation STEVE_TEXTURE = new ResourceLocation("textures/entity/steve.png");
    private static final float ROPE_LENGTH = (float) HangingBodyEntity.ROPE_LENGTH;
    private static final int ROPE_SEGMENTS = 16;
    private static final float ROPE_WIDTH = 0.03F;
    private static final float ROPE_COLOR_R = 0.55F;
    private static final float ROPE_COLOR_G = 0.35F;
    private static final float ROPE_COLOR_B = 0.22F;
    private static final float SWING_SPEED = 0.1F;
    private static final float SWING_AMPLITUDE = 8.0F;
    private static final float ARM_SPREAD = 0.1F;
    private static final float HEAD_DROOP = 0.4F;
    private static final float HEAD_TILT = 0.1F;
    
    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SKIN_LOADING = new ConcurrentHashMap<>();
    
    private final PlayerModel<Player> model;
    
    public HangingBodyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    }
    
    @Override
    public void render(HangingBodyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, 
                       MultiBufferSource buffer, int packedLight) {
        
        BlockPos entityPos = entity.blockPosition();
        int blockLight = entity.level.getBrightness(LightLayer.BLOCK, entityPos);
        int skyLight = entity.level.getBrightness(LightLayer.SKY, entityPos);
        int properLight = LightTexture.pack(blockLight, skyLight);
        
        float swingAngle = (float) Math.sin((entity.tickCount + partialTick) * SWING_SPEED) * SWING_AMPLITUDE;
        
        poseStack.pushPose();
        
        // Move to top of rope (attachment point on log)
        poseStack.translate(0.0D, ROPE_LENGTH + 1.95D, 0.0D);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(swingAngle));
        
        renderRope(poseStack, buffer, properLight);
        
        // Move down to where the body hangs (at entity position)
        poseStack.translate(0.0D, -ROPE_LENGTH - 1.95D, 0.0D);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        
        // Reset model state
        this.model.young = false;
        this.model.crouching = false;
        this.model.riding = false;
        this.model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        this.model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        
        // Arms hanging down with slight spread
        this.model.leftArm.xRot = 0.1F;
        this.model.rightArm.xRot = 0.1F;
        this.model.leftArm.zRot = ARM_SPREAD;
        this.model.rightArm.zRot = -ARM_SPREAD;
        this.model.leftArm.yRot = 0.0F;
        this.model.rightArm.yRot = 0.0F;
        
        // Legs hanging straight
        this.model.leftLeg.xRot = 0.0F;
        this.model.rightLeg.xRot = 0.0F;
        this.model.leftLeg.yRot = 0.0F;
        this.model.rightLeg.yRot = 0.0F;
        this.model.leftLeg.zRot = 0.0F;
        this.model.rightLeg.zRot = 0.0F;
        
        // Head drooped forward and tilted
        this.model.head.xRot = HEAD_DROOP;
        this.model.head.yRot = 0.0F;
        this.model.head.zRot = HEAD_TILT;
        this.model.hat.copyFrom(this.model.head);
        
        // Body straight
        this.model.body.xRot = 0.0F;
        this.model.body.yRot = 0.0F;
        this.model.body.zRot = 0.0F;
        
        ResourceLocation skinTexture = getSkinTexture(entity.getSkinUsername());
        var vertexConsumer = buffer.getBuffer(this.model.renderType(skinTexture));
        this.model.renderToBuffer(poseStack, vertexConsumer, properLight, OverlayTexture.NO_OVERLAY, 
                                   1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
    
    private void renderRope(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        
        float topY = 0.0F;
        float bottomY = -ROPE_LENGTH;
        
        for (int i = 0; i < ROPE_SEGMENTS; i++) {
            float t1 = (float) i / ROPE_SEGMENTS;
            float t2 = (float) (i + 1) / ROPE_SEGMENTS;
            
            float y1 = topY + (bottomY - topY) * t1;
            float y2 = topY + (bottomY - topY) * t2;
            
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y1, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y1, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y2, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y2, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y1, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y1, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y2, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y2, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y1, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y1, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y2, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, -ROPE_WIDTH, y2, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y1, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y1, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y2, ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
            vertexConsumer.vertex(matrix, ROPE_WIDTH, y2, -ROPE_WIDTH).color(ROPE_COLOR_R, ROPE_COLOR_G, ROPE_COLOR_B, 1.0F).uv2(packedLight).endVertex();
        }
    }
    
    @Override
    public ResourceLocation getTextureLocation(HangingBodyEntity entity) {
        return getSkinTexture(entity.getSkinUsername());
    }
    
    private ResourceLocation getSkinTexture(String username) {
        // Check cache first
        ResourceLocation cached = SKIN_CACHE.get(username);
        if (cached != null) {
            return cached;
        }
        
        // Start loading if not already loading
        if (!SKIN_LOADING.containsKey(username)) {
            SKIN_LOADING.put(username, true);
            loadSkinAsync(username);
        }
        
        // Return steve texture while loading
        return STEVE_TEXTURE;
    }
    
    private void loadSkinAsync(String username) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Create an initial GameProfile with just the username
        GameProfile profile = new GameProfile(null, username);
        
        // Fill the profile asynchronously to get the correct UUID and skin data
        CompletableFuture.supplyAsync(() -> {
            try {
                return minecraft.getMinecraftSessionService().fillProfileProperties(profile, true);
            } catch (Exception e) {
                return profile;
            }
        }).thenAccept(filledProfile -> {
            // Use Minecraft's skin manager to fetch the skin with the filled profile
            minecraft.getSkinManager().registerSkins(filledProfile, (type, location, texture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    SKIN_CACHE.put(username, location);
                }
            }, true);
        });
    }
}
