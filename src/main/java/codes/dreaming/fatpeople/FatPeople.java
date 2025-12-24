package codes.dreaming.fatpeople;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FatPeople implements ModInitializer {
	public static final String MOD_ID = "fat-people";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation STRUGGLE_JUMP_PACKET = new ResourceLocation(MOD_ID, "struggle_jump");
	public static final MobEffect PRIME_ENERGY_EFFECT = new PrimeEnergyEffect();
	public static final Item PRIME_ENERGY_ITEM = new PrimeEnergyItem(
		new FabricItemSettings()
			.group(CreativeModeTab.TAB_FOOD)
			.maxCount(16)
	);
	public static final EntityType<HangingBodyEntity> HANGING_BODY_ENTITY = FabricEntityTypeBuilder
		.<HangingBodyEntity>create(MobCategory.MISC, HangingBodyEntity::new)
		.dimensions(EntityDimensions.fixed(0.6F, 1.95F))  // Normal player size (1.95 blocks tall)
		.trackRangeBlocks(64)
		.trackedUpdateRate(1)
		.build();
	public static final ResourceKey<Biome> SUICIDE_FOREST_KEY = ResourceKey.create(
		Registry.BIOME_REGISTRY,
		new ResourceLocation(MOD_ID, "suicide_forest")
	);
	public static final SoundEvent SUICIDE_FOREST_AMBIENCE = new SoundEvent(
		new ResourceLocation(MOD_ID, "suicide_forest_ambience")
	);

	// Base spawn chance per chunk (21%) - mathematically grounded would be 0.0021f (0.21%)
	private static final float BASE_SPAWN_CHANCE = 0.21f;
	// Perlin noise clustering amplifies spawn chance in certain areas (up to 8x in cluster centers)
	private static final float CLUSTER_AMPLIFIER = 8.0f;
	private static final double NOISE_SCALE = 0.005; // Controls cluster size (lower = larger clusters)
	private static final double CLUSTER_THRESHOLD = 0.2; // Noise value threshold for clustering (lower = more frequent clusters)
	
	private static final int REQUIRED_AIR_SPACE = 6;
	private static final int MIN_TREE_HEIGHT = 8;
	private static final int MAX_SPAWN_ATTEMPTS = 5;
	private static final float STRUGGLE_JUMP_VELOCITY = 0.04f;
	
	// Queue of chunks pending processing (to avoid processing during world gen)
	private static final Queue<PendingChunk> pendingChunks = new ConcurrentLinkedQueue<>();
	// Track which chunks we've already processed to avoid duplicates
	private static final Set<Long> processedChunks = new HashSet<>();
	// Maximum chunks to process per tick to avoid lag spikes
	private static final int MAX_CHUNKS_PER_TICK = 3;
	
	// Sound cooldown tracking (34 second sound duration in milliseconds)
	private static final long AMBIENCE_SOUND_DURATION_MS = 35000L;
	private static long lastAmbienceSoundTime = 0L;
	
	public static boolean canPlayAmbienceSound() {
		long currentTime = System.currentTimeMillis();
		return (currentTime - lastAmbienceSoundTime) >= AMBIENCE_SOUND_DURATION_MS;
	}
	
	public static void markAmbienceSoundPlayed() {
		lastAmbienceSoundTime = System.currentTimeMillis();
	}
	
	private record PendingChunk(ServerLevel world, ChunkPos pos) {}

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		
		Registry.register(Registry.MOB_EFFECT, new ResourceLocation(MOD_ID, "prime_energy"), PRIME_ENERGY_EFFECT);
		Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, "prime_energy"), PRIME_ENERGY_ITEM);
		Registry.register(Registry.ENTITY_TYPE, new ResourceLocation(MOD_ID, "hanging_body"), HANGING_BODY_ENTITY);
		Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MOD_ID, "suicide_forest_ambience"), SUICIDE_FOREST_AMBIENCE);
		
		// Queue chunks for processing instead of processing immediately
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			long chunkKey = chunk.getPos().toLong();
			if (!processedChunks.contains(chunkKey)) {
				pendingChunks.offer(new PendingChunk(world, chunk.getPos()));
			}
		});
		
		// Process queued chunks on server tick (safe, after world gen)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int processed = 0;
			while (processed < MAX_CHUNKS_PER_TICK && !pendingChunks.isEmpty()) {
				PendingChunk pending = pendingChunks.poll();
				if (pending == null) break;
				
				long chunkKey = pending.pos().toLong();
				if (processedChunks.contains(chunkKey)) continue;
				
				// Get the chunk if it's still loaded
				LevelChunk chunk = pending.world().getChunkSource().getChunkNow(pending.pos().x, pending.pos().z);
				if (chunk != null) {
					processedChunks.add(chunkKey);
					processChunkSpawns(pending.world(), chunk);
					processed++;
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(STRUGGLE_JUMP_PACKET, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if (player instanceof RollingAccessor accessor && accessor.isRolling()) {
					accessor.setStruggleVelocity(accessor.getStruggleVelocity() + STRUGGLE_JUMP_VELOCITY);
					if (accessor.getStruggleProgress() >= 1.0f) {
						accessor.setRolling(false);
					}
				}
			});
		});
		
		// Register /findbody command to teleport to nearest hanging body
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("findbody")
				.requires(source -> source.hasPermission(2))
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					ServerLevel world = player.getLevel();
					
					// Search in a large area around the player
					double searchRadius = 1000.0;
					AABB searchBox = new AABB(
						player.getX() - searchRadius, world.getMinBuildHeight(), player.getZ() - searchRadius,
						player.getX() + searchRadius, world.getMaxBuildHeight(), player.getZ() + searchRadius
					);
					
					var bodies = world.getEntitiesOfClass(HangingBodyEntity.class, searchBox);
					
					if (bodies.isEmpty()) {
						context.getSource().sendFailure(Component.literal("No hanging bodies found within " + (int)searchRadius + " blocks"));
						return 0;
					}
					
					// Find the nearest one
					HangingBodyEntity nearest = null;
					double nearestDistSq = Double.MAX_VALUE;
					for (HangingBodyEntity body : bodies) {
						double distSq = player.distanceToSqr(body);
						if (distSq < nearestDistSq) {
							nearestDistSq = distSq;
							nearest = body;
						}
					}
					
					if (nearest != null) {
						player.teleportTo(nearest.getX(), nearest.getY(), nearest.getZ());
						context.getSource().sendSuccess(Component.literal("Teleported to hanging body at " + 
							(int)nearest.getX() + ", " + (int)nearest.getY() + ", " + (int)nearest.getZ() + 
							" (skin: " + nearest.getSkinUsername() + ")"), true);
						return 1;
					}
					
					return 0;
				})
			);
		});
	}
	
	/**
	 * Process spawns for a chunk. Called from server tick, safe to modify world.
	 */
	private static void processChunkSpawns(ServerLevel world, LevelChunk chunk) {
		RandomSource random = world.getRandom();
		
		int chunkX = chunk.getPos().getMinBlockX();
		int chunkZ = chunk.getPos().getMinBlockZ();
		
		BlockPos chunkCenter = new BlockPos(chunkX + 8, 64, chunkZ + 8);
		var biome = chunk.getNoiseBiome(
			chunkCenter.getX() >> 2, 
			chunkCenter.getY() >> 2, 
			chunkCenter.getZ() >> 2
		);
		
		if (!biome.is(SUICIDE_FOREST_KEY)) {
			return;
		}
		
		// Calculate Perlin noise-based cluster weight for this chunk
		double noiseValue = samplePerlinNoise(chunkX, chunkZ, world.getSeed());
		float clusterMultiplier = 1.0f;
		if (noiseValue > CLUSTER_THRESHOLD) {
			// In a cluster zone - amplify spawn chance
			clusterMultiplier = 1.0f + (float)((noiseValue - CLUSTER_THRESHOLD) / (1.0 - CLUSTER_THRESHOLD)) * (CLUSTER_AMPLIFIER - 1.0f);
		}
		
		float effectiveSpawnChance = BASE_SPAWN_CHANCE * clusterMultiplier;
		
		// Check if body should spawn (0.21% base, up to ~0.63% in cluster centers)
		if (random.nextFloat() > effectiveSpawnChance) {
			return;
		}
		
		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
			int localX = random.nextInt(16);
			int localZ = random.nextInt(16);
			int x = chunkX + localX;
			int z = chunkZ + localZ;
			
			int surfaceY = chunk.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 
				localX, localZ
			);
			
			int groundY = -1;
			for (int y = surfaceY; y > world.getMinBuildHeight(); y--) {
				BlockPos pos = new BlockPos(x, y, z);
				var state = chunk.getBlockState(pos);
				if (!state.isAir() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
					groundY = y;
					break;
				}
			}
			
			if (groundY == -1) continue;
			
			for (int y = surfaceY; y > groundY + MIN_TREE_HEIGHT; y--) {
				BlockPos checkPos = new BlockPos(x, y, z);
				var blockState = chunk.getBlockState(checkPos);
				
				if (blockState.is(BlockTags.LOGS)) {
					boolean hasEnoughSpace = true;
					for (int airCheck = 1; airCheck <= REQUIRED_AIR_SPACE; airCheck++) {
						BlockPos airPos = checkPos.below(airCheck);
						if (!chunk.getBlockState(airPos).isAir()) {
							hasEnoughSpace = false;
							break;
						}
					}
					
					if (hasEnoughSpace) {
						BlockPos bodyPos = checkPos.below(HangingBodyEntity.ROPE_LENGTH + 1);
						HangingBodyEntity hangingBody = new HangingBodyEntity(HANGING_BODY_ENTITY, world);
						hangingBody.setPos(bodyPos.getX() + 0.5, bodyPos.getY(), bodyPos.getZ() + 0.5);
						world.addFreshEntity(hangingBody);
						
						if (clusterMultiplier > 1.0f) {
							LOGGER.info("Spawned hanging body in cluster at {} (multiplier: {})", bodyPos, clusterMultiplier);
						} else {
							LOGGER.debug("Spawned hanging body at {}", bodyPos);
						}
						return;
					}
				}
			}
		}
	}
	
	/**
	 * Simple Perlin-like noise sampling using Minecraft's built-in utilities.
	 * Creates cluster zones where bodies are more likely to spawn.
	 */
	private static double samplePerlinNoise(int x, int z, long seed) {
		// Use seed to offset the noise pattern per world
		double offsetX = (seed & 0xFFFF) * 0.1;
		double offsetZ = ((seed >> 16) & 0xFFFF) * 0.1;
		
		double nx = (x * NOISE_SCALE) + offsetX;
		double nz = (z * NOISE_SCALE) + offsetZ;
		
		// Simple gradient noise approximation using sine waves
		// This creates smooth, organic-looking cluster patterns
		double noise = 0.0;
		noise += Math.sin(nx * 1.0 + nz * 0.5) * 0.5;
		noise += Math.sin(nx * 0.5 + nz * 1.0) * 0.5;
		noise += Math.sin(nx * 2.0 - nz * 1.5) * 0.25;
		noise += Math.sin(nx * 1.5 + nz * 2.0) * 0.25;
		
		// Normalize to 0-1 range
		return (noise / 1.5 + 1.0) * 0.5;
	}
}