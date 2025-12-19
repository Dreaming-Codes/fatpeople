package codes.dreaming.fatpeople.mixin;

import codes.dreaming.fatpeople.FatPeople;
import codes.dreaming.fatpeople.PrimeEnergyEffect;
import codes.dreaming.fatpeople.RollingAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements RollingAccessor {

    @Unique
    private static final EntityDataAccessor<Boolean> ROLLING = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Float> STRUGGLE_PROGRESS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> ROLLING_SPEED = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> ROLL_ANGLE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> STRUGGLE_VELOCITY = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);

    @Unique private static final float INITIAL_ROLLING_SPEED = 0.1f;
    @Unique private static final float BASE_ROLL_CHANCE = 0.15f;
    @Unique private static final float ROLL_CHANCE_PER_LEVEL = 0.10f;
    @Unique private static final float ROLLING_MAX_UP_STEP = 1.2f;
    @Unique private static final float DEFAULT_MAX_UP_STEP = 0.6f;
    @Unique private static final float BASE_MAX_ROLLING_SPEED = 0.8f;
    @Unique private static final float ROLLING_ACCELERATION = 0.01f;
    @Unique private static final float ROLL_ANGLE_SPEED_MULTIPLIER = 20.0f;
    @Unique private static final double WATER_DIP_DEPTH = 2.5;
    @Unique private static final float WATER_BOUNCE_STRENGTH = 1.2f;
    @Unique private static final double WATER_IMPACT_SLOWDOWN = 0.5;
    @Unique private static final double WATER_SINK_DAMPING = 0.85;
    @Unique private static final double WATER_SINK_ACCELERATION = 0.02;
    @Unique private static final double WATER_MAX_SINK_SPEED = 0.5;
    @Unique private static final double WATER_SURFACE_PUSH = 0.2;
    @Unique private static final double FALLING_THRESHOLD = 0.3;
    @Unique private static final float MAX_BOUNCE_STRENGTH = 1.2f;
    @Unique private static final float BOUNCE_DAMPING = 0.7f;
    @Unique private static final float STRUGGLE_GRAVITY = 0.008f;
    @Unique private static final float STRUGGLE_BOUNCE_DAMPING = 0.4f;
    @Unique private static final float STRUGGLE_BOUNCE_THRESHOLD = 0.01f;
    @Unique private static final float FALL_STRUGGLE_PENALTY = 0.5f;
    @Unique private static final double FALL_BREAK_THRESHOLD = 0.5;
    @Unique private static final double LARGE_RADIUS_THRESHOLD = 1.0;
    @Unique private static final float BLOCK_DESTROY_CHANCE_MULTIPLIER = 0.8f;
    @Unique private static final float MAX_BLOCK_HARDNESS = 50.0f;
    @Unique private static final float SHOCKWAVE_UPWARD_VELOCITY_SCALE = 0.25f;
    @Unique private static final double SHOCKWAVE_OUTWARD_PUSH = 0.08;
    @Unique private static final float SMASH_SPEED_THRESHOLD_RATIO = 0.5f;
    @Unique private static final float HARDNESS_SPEED_PENALTY_MULTIPLIER = 0.05f;
    @Unique private static final float MIN_SPEED_RETENTION = 0.3f;
    @Unique private static final float MAX_HARDNESS_PENALTY = 0.5f;
    @Unique private static final double ENTITY_HITBOX_INFLATION = 0.5;
    @Unique private static final float ROLLING_ENTITY_DAMAGE_MULTIPLIER = 5.0f;
    @Unique private static final double ROLLING_KNOCKBACK_STRENGTH = 5.0;
    @Unique private static final double ROLLING_KNOCKBACK_UPWARD = 1.0;
    @Unique private static final double CHECK_DISTANCE_OFFSET = 0.3;
    @Unique private static final float DEFAULT_SPEED_RETENTION = 0.7f;
    @Unique private static final int DEFAULT_MAX_COLLISION_TICKS = 3;

    @Unique private boolean wasFalling = false;
    @Unique private double lastYVelocity = 0;
    @Unique private int horizontalCollisionTicks = 0;
    @Unique private boolean waterBouncing = false;
    @Unique private double waterEntryY = 0;
    @Unique private boolean sinkingInWater = false;
    @Unique private float lastWidthMultiplier = 1.0f;

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    @Unique
    private float fatpeople$getWidthMultiplier() {
        int amp = fatpeople$getPrimeEnergyAmplifier();
        return amp >= 0 ? PrimeEnergyEffect.getWidthMultiplier(amp) : 1.0f;
    }
    
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        int amp = fatpeople$getPrimeEnergyAmplifier();
        if (amp >= 0) {
            EntityDimensions original = cir.getReturnValue();
            float widthMultiplier = PrimeEnergyEffect.getWidthMultiplier(amp);
            cir.setReturnValue(EntityDimensions.scalable(original.width * widthMultiplier, original.height));
        }
    }
    
    @Override
    public float getRollingSpeed() {
        return this.entityData.get(ROLLING_SPEED);
    }

    @Override
    public void setRollingSpeed(float speed) {
        this.entityData.set(ROLLING_SPEED, speed);
    }
    
    @Override
    public float getRollAngle() {
        return this.entityData.get(ROLL_ANGLE);
    }

    @Override
    public void setRollAngle(float angle) {
        this.entityData.set(ROLL_ANGLE, angle);
    }
    
    @Override
    public float getStruggleVelocity() {
        return this.entityData.get(STRUGGLE_VELOCITY);
    }

    @Override
    public void setStruggleVelocity(float velocity) {
        this.entityData.set(STRUGGLE_VELOCITY, velocity);
    }

    @Override
    public boolean isRolling() {
        return this.entityData.get(ROLLING);
    }

    @Override
    public void setRolling(boolean rolling) {
        this.entityData.set(ROLLING, rolling);
        if (rolling) {
            this.setStruggleProgress(0.0f);
            this.setStruggleVelocity(0.0f);
            this.setRollingSpeed(INITIAL_ROLLING_SPEED);
            
            if (!this.level.isClientSide && (Object)this instanceof ServerPlayer serverPlayer) {
                Advancement advancement = serverPlayer.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("fat-people", "human_bowling"));
                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "start_rolling");
                }
            }
        } else {
            this.setRollingSpeed(0.0f);
            this.setStruggleVelocity(0.0f);
        }
    }
    
    @Override
    public float getStruggleProgress() {
        return this.entityData.get(STRUGGLE_PROGRESS);
    }

    @Override
    public void setStruggleProgress(float progress) {
        this.entityData.set(STRUGGLE_PROGRESS, Math.max(0.0f, Math.min(1.0f, progress)));
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void defineRollingData(CallbackInfo ci) {
        this.entityData.define(ROLLING, false);
        this.entityData.define(STRUGGLE_PROGRESS, 0.0f);
        this.entityData.define(STRUGGLE_VELOCITY, 0.0f);
        this.entityData.define(ROLLING_SPEED, 0.0f);
        this.entityData.define(ROLL_ANGLE, 0.0f);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void preventJump(CallbackInfo ci) {
        if (this.isRolling()) {
            ci.cancel();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void onJump(CallbackInfo ci) {
        if (!this.level.isClientSide) {
            int primeAmp = fatpeople$getPrimeEnergyAmplifier();
            if (primeAmp >= 0 && !this.isRolling()) {
                float rollChance = BASE_ROLL_CHANCE + (ROLL_CHANCE_PER_LEVEL * (primeAmp + 1));
                if (this.random.nextFloat() < rollChance) {
                    this.setRolling(true);
                }
            }
        }
    }
    
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
         if (this.isRolling()) {
            this.jumping = false;
        }
    }
    
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void preventFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.isRolling()) {
            cir.setReturnValue(false);
        }
    }
    
    @Unique
    private int fatpeople$getPrimeEnergyAmplifier() {
        MobEffectInstance effect = this.getEffect(FatPeople.PRIME_ENERGY_EFFECT);
        return effect != null ? effect.getAmplifier() : -1;
    }
    
    @Unique
    private boolean fatpeople$hasPrimeEnergy() {
        return this.hasEffect(FatPeople.PRIME_ENERGY_EFFECT);
    }
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        float currentWidthMultiplier = fatpeople$getWidthMultiplier();
        if (currentWidthMultiplier != this.lastWidthMultiplier) {
            this.lastWidthMultiplier = currentWidthMultiplier;
            this.refreshDimensions();
        }
        
        if (this.isRolling()) {
            this.maxUpStep = ROLLING_MAX_UP_STEP;
            
            int primeAmplifier = fatpeople$getPrimeEnergyAmplifier();
            float speedMultiplier = primeAmplifier >= 0 ? PrimeEnergyEffect.getSpeedMultiplier(primeAmplifier) : 1.0f;
            
            float maxSpeed = BASE_MAX_ROLLING_SPEED * speedMultiplier;
            float acceleration = ROLLING_ACCELERATION * speedMultiplier;
            float currentSpeed = this.getRollingSpeed();
            
            if (currentSpeed < maxSpeed) {
                currentSpeed = Math.min(maxSpeed, currentSpeed + acceleration);
                this.setRollingSpeed(currentSpeed);
            }
            
            float rotationSpeed = currentSpeed * ROLL_ANGLE_SPEED_MULTIPLIER; 
            this.setRollAngle(this.getRollAngle() + rotationSpeed);
            
            net.minecraft.world.phys.Vec3 forward = net.minecraft.world.phys.Vec3.directionFromRotation(0, this.getYRot());
            net.minecraft.world.phys.Vec3 currentVel = this.getDeltaMovement();
            this.setDeltaMovement(forward.x * currentSpeed, currentVel.y, forward.z * currentSpeed);
            
            net.minecraft.core.BlockPos posAt = this.blockPosition();
            boolean inWater = this.level.getBlockState(posAt).getMaterial() == net.minecraft.world.level.material.Material.WATER;
            boolean isFalling = this.getDeltaMovement().y < -FALLING_THRESHOLD;
            
            if (this.sinkingInWater) {
                double depthSunk = this.waterEntryY - this.getY();
                
                if (depthSunk >= WATER_DIP_DEPTH) {
                    this.sinkingInWater = false;
                    this.waterBouncing = true;
                    this.setDeltaMovement(this.getDeltaMovement().x, WATER_BOUNCE_STRENGTH, this.getDeltaMovement().z);
                } else {
                    double currentY = this.getDeltaMovement().y;
                    double slowedY = Math.max(-WATER_MAX_SINK_SPEED, currentY * WATER_SINK_DAMPING - WATER_SINK_ACCELERATION);
                    this.setDeltaMovement(this.getDeltaMovement().x, slowedY, this.getDeltaMovement().z);
                }
            } else if (this.waterBouncing) {
                if (!inWater && this.getY() > this.waterEntryY) {
                    this.waterBouncing = false;
                }
            } else if (inWater && isFalling) {
                this.sinkingInWater = true;
                this.waterEntryY = this.getY();
                double slowedY = this.getDeltaMovement().y * WATER_IMPACT_SLOWDOWN;
                this.setDeltaMovement(this.getDeltaMovement().x, slowedY, this.getDeltaMovement().z);
            } else if (inWater && !this.sinkingInWater && !this.waterBouncing) {
                this.setDeltaMovement(this.getDeltaMovement().x, WATER_SURFACE_PUSH, this.getDeltaMovement().z);
            }
            
            boolean justLanded = this.wasFalling && this.onGround;
            
            if (justLanded) {
                float bounceStrength = (float) Math.min(MAX_BOUNCE_STRENGTH, Math.abs(this.lastYVelocity) * BOUNCE_DAMPING);
                this.setDeltaMovement(this.getDeltaMovement().x, bounceStrength, this.getDeltaMovement().z);
            }
            
            this.lastYVelocity = this.getDeltaMovement().y;
            this.wasFalling = isFalling;
        } else {
            this.maxUpStep = DEFAULT_MAX_UP_STEP;
            this.sinkingInWater = false;
            this.waterBouncing = false;
        }

        if (!this.level.isClientSide) {
            if (this.isRolling()) {
                float velocity = this.getStruggleVelocity();
                float progress = this.getStruggleProgress();
                
                velocity -= STRUGGLE_GRAVITY;
                progress += velocity;
                
                if (progress <= 0) {
                    progress = 0;
                    if (velocity < -STRUGGLE_BOUNCE_THRESHOLD) {
                        velocity = -velocity * STRUGGLE_BOUNCE_DAMPING;
                    } else {
                        velocity = 0;
                    }
                }
                
                progress = Math.max(0.0f, Math.min(1.0f, progress));
                
                this.setStruggleVelocity(velocity);
                this.setStruggleProgress(progress);
                
                boolean justLandedServer = this.onGround && Math.abs(this.lastYVelocity) > FALLING_THRESHOLD;
                
                if (justLandedServer && !this.wasFalling) {
                    this.setStruggleProgress(this.getStruggleProgress() * FALL_STRUGGLE_PENALTY);
                    this.setStruggleVelocity(0);
                    
                    double fallSpeed = Math.abs(this.lastYVelocity);
                    if (fallSpeed > FALL_BREAK_THRESHOLD) {
                        int radius = fallSpeed > LARGE_RADIUS_THRESHOLD ? 2 : 1;
                        float destroyChance = (float) Math.min(1.0, fallSpeed * BLOCK_DESTROY_CHANCE_MULTIPLIER);
                        
                        net.minecraft.core.BlockPos playerPos = this.blockPosition();
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                net.minecraft.core.BlockPos breakPos = playerPos.offset(dx, -1, dz);
                                net.minecraft.world.level.block.state.BlockState state = this.level.getBlockState(breakPos);
                                
                                if (!state.isAir() && state.getDestroySpeed(this.level, breakPos) >= 0 
                                    && state.getDestroySpeed(this.level, breakPos) < MAX_BLOCK_HARDNESS
                                    && this.random.nextFloat() < destroyChance) {
                                    this.level.destroyBlock(breakPos, true, (Player)(Object)this);
                                }
                            }
                        }
                    }
                    
                    int primeAmpLanding = fatpeople$getPrimeEnergyAmplifier();
                    if (primeAmpLanding >= 0) {
                        float waveStrength = PrimeEnergyEffect.getWidthMultiplier(primeAmpLanding);
                        int waveRadius = (int) Math.ceil(waveStrength + 1);
                        net.minecraft.core.BlockPos landPos = this.blockPosition();
                        
                        for (int dx = -waveRadius; dx <= waveRadius; dx++) {
                            for (int dz = -waveRadius; dz <= waveRadius; dz++) {
                                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                                
                                double dist = Math.sqrt(dx * dx + dz * dz);
                                if (dist > waveRadius) continue;
                                
                                net.minecraft.core.BlockPos wavePos = landPos.offset(dx, -1, dz);
                                net.minecraft.world.level.block.state.BlockState waveState = this.level.getBlockState(wavePos);
                                
                                if (!waveState.isAir() && !waveState.getMaterial().isLiquid() 
                                    && waveState.getDestroySpeed(this.level, wavePos) >= 0 
                                    && waveState.getDestroySpeed(this.level, wavePos) < MAX_BLOCK_HARDNESS) {
                                    
                                    this.level.setBlock(wavePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                                    
                                    net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = 
                                        new net.minecraft.world.entity.item.FallingBlockEntity(
                                            net.minecraft.world.entity.EntityType.FALLING_BLOCK,
                                            this.level
                                        );
                                    
                                    fallingBlock.setPos(wavePos.getX() + 0.5, wavePos.getY() + 0.5, wavePos.getZ() + 0.5);
                                    ((FallingBlockEntityAccessor) fallingBlock).setBlockState(waveState);
                                    
                                    double upwardVelocity = (waveRadius - dist + 1) * SHOCKWAVE_UPWARD_VELOCITY_SCALE * waveStrength;
                                    double outwardX = dx / dist * SHOCKWAVE_OUTWARD_PUSH;
                                    double outwardZ = dz / dist * SHOCKWAVE_OUTWARD_PUSH;
                                    
                                    fallingBlock.setDeltaMovement(outwardX, upwardVelocity, outwardZ);
                                    fallingBlock.time = 1;
                                    fallingBlock.dropItem = true;
                                    
                                    this.level.addFreshEntity(fallingBlock);
                                }
                            }
                        }
                    }
                }
                
                net.minecraft.world.phys.Vec3 forward = net.minecraft.world.phys.Vec3.directionFromRotation(0, this.getYRot());
                net.minecraft.core.BlockPos playerPos = this.blockPosition();
                
                int primeAmp = fatpeople$getPrimeEnergyAmplifier();
                float baseSpeedRetention = primeAmp >= 0 ? PrimeEnergyEffect.getWallSpeedRetention(primeAmp) : DEFAULT_SPEED_RETENTION;
                int maxCollisionTicks = primeAmp >= 0 ? PrimeEnergyEffect.getWallCollisionTicks(primeAmp) : DEFAULT_MAX_COLLISION_TICKS;
                float primeSpeedMult = primeAmp >= 0 ? PrimeEnergyEffect.getSpeedMultiplier(primeAmp) : 1.0f;
                float widthMultiplier = primeAmp >= 0 ? PrimeEnergyEffect.getWidthMultiplier(primeAmp) : 1.0f;
                
                float currentSpeed = this.getRollingSpeed();
                float maxSpeed = BASE_MAX_ROLLING_SPEED * primeSpeedMult;
                float smashThreshold = maxSpeed * SMASH_SPEED_THRESHOLD_RATIO;
                
                float playerWidth = this.getBbWidth();
                float halfWidth = playerWidth / 2.0f;
                double checkDistance = halfWidth + CHECK_DISTANCE_OFFSET;
                
                if (currentSpeed >= smashThreshold) {
                    float maxHardness = 0;
                    boolean brokeAny = false;
                    
                    net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(-forward.z, 0, forward.x);
                    
                    int widthBlocks = Math.max(1, (int) Math.ceil(playerWidth));
                    int halfWidthBlocks = widthBlocks / 2;
                    
                    boolean hasWallAcrossWidth = false;
                    for (int w = -halfWidthBlocks; w <= halfWidthBlocks; w++) {
                        double offsetX = this.getX() + forward.x * checkDistance + right.x * w;
                        double offsetZ = this.getZ() + forward.z * checkDistance + right.z * w;
                        
                        int blockX = (int) Math.floor(offsetX);
                        int blockZ = (int) Math.floor(offsetZ);
                        
                        net.minecraft.core.BlockPos headPos = new net.minecraft.core.BlockPos(blockX, playerPos.getY() + 1, blockZ);
                        net.minecraft.world.level.block.state.BlockState headState = this.level.getBlockState(headPos);
                        
                        boolean headSolid = !headState.isAir() && !headState.getMaterial().isLiquid() && !headState.getMaterial().isReplaceable();
                        if (headSolid) {
                            hasWallAcrossWidth = true;
                            break;
                        }
                    }
                    
                    if (hasWallAcrossWidth) {
                        for (int w = -halfWidthBlocks; w <= halfWidthBlocks; w++) {
                            double offsetX = this.getX() + forward.x * checkDistance + right.x * w;
                            double offsetZ = this.getZ() + forward.z * checkDistance + right.z * w;
                            
                            int blockX = (int) Math.floor(offsetX);
                            int blockZ = (int) Math.floor(offsetZ);
                            
                            net.minecraft.core.BlockPos feetPos = new net.minecraft.core.BlockPos(blockX, playerPos.getY(), blockZ);
                            net.minecraft.core.BlockPos headPos = new net.minecraft.core.BlockPos(blockX, playerPos.getY() + 1, blockZ);
                            net.minecraft.world.level.block.state.BlockState feetState = this.level.getBlockState(feetPos);
                            net.minecraft.world.level.block.state.BlockState headState = this.level.getBlockState(headPos);
                            
                            boolean feetSolid = !feetState.isAir() && !feetState.getMaterial().isLiquid() && !feetState.getMaterial().isReplaceable();
                            boolean headSolid = !headState.isAir() && !headState.getMaterial().isLiquid() && !headState.getMaterial().isReplaceable();
                            
                            if (headSolid) {
                                float headHardness = headState.getDestroySpeed(this.level, headPos);
                                if (headHardness >= 0 && headHardness < MAX_BLOCK_HARDNESS) {
                                    maxHardness = Math.max(maxHardness, headHardness);
                                    this.level.destroyBlock(headPos, true, (Player)(Object)this);
                                    brokeAny = true;
                                }
                            }
                            
                            if (feetSolid) {
                                float feetHardness = feetState.getDestroySpeed(this.level, feetPos);
                                if (feetHardness >= 0 && feetHardness < MAX_BLOCK_HARDNESS) {
                                    maxHardness = Math.max(maxHardness, feetHardness);
                                    this.level.destroyBlock(feetPos, true, (Player)(Object)this);
                                    brokeAny = true;
                                }
                            }
                        }
                    }
                    
                    if (brokeAny) {
                        float hardnessSpeedPenalty = Math.min(MAX_HARDNESS_PENALTY, maxHardness * HARDNESS_SPEED_PENALTY_MULTIPLIER);
                        float speedRetention = Math.max(MIN_SPEED_RETENTION, baseSpeedRetention - hardnessSpeedPenalty);
                        this.setRollingSpeed(currentSpeed * speedRetention);
                        currentSpeed = this.getRollingSpeed();
                        this.horizontalCollisionTicks = 0;
                    }
                }
                
                if (this.horizontalCollision && this.onGround) {
                    this.horizontalCollisionTicks++;
                    if (this.horizontalCollisionTicks > maxCollisionTicks) {
                        this.setRolling(false);
                        this.horizontalCollisionTicks = 0;
                    }
                } else {
                    this.horizontalCollisionTicks = 0;
                }
                
                net.minecraft.world.phys.AABB box = this.getBoundingBox().inflate(ENTITY_HITBOX_INFLATION, ENTITY_HITBOX_INFLATION, ENTITY_HITBOX_INFLATION);
                this.level.getEntities(this, box, e -> e instanceof LivingEntity).forEach(e -> {
                    e.hurt(net.minecraft.world.damagesource.DamageSource.playerAttack((Player)(Object)this), ROLLING_ENTITY_DAMAGE_MULTIPLIER * this.getRollingSpeed());
                    net.minecraft.world.phys.Vec3 knockback = e.position().subtract(this.position()).normalize().scale(ROLLING_KNOCKBACK_STRENGTH); 
                    e.setDeltaMovement(knockback.x, ROLLING_KNOCKBACK_UPWARD, knockback.z); 
                });
            }
        }
    }
}
