package codes.dreaming.fatpeople;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.network.FriendlyByteBuf;

public class FatPeopleClient implements ClientModInitializer {
	private boolean lastJumping = false;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(FatPeople.HANGING_BODY_ENTITY, HangingBodyRenderer::new);
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player instanceof RollingAccessor accessor && accessor.isRolling()) {
				boolean isJumpPressed = client.options.keyJump.isDown();
				
				boolean isRisingEdge = isJumpPressed && !lastJumping;
				if (isRisingEdge) {
					ClientPlayNetworking.send(FatPeople.STRUGGLE_JUMP_PACKET, new FriendlyByteBuf(Unpooled.buffer()));
				}
				
				lastJumping = isJumpPressed;
			} else {
				lastJumping = false;
			}
		});
	}
}