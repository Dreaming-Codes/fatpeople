package codes.dreaming.fatpeople;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.phys.Vec3;

public class HangingBodyEntity extends Entity {
    
    public static final int ROPE_LENGTH = 4;
    private static final int MAX_RENDER_DISTANCE_SQ = 256 * 256;
    private static final EntityDataAccessor<Boolean> DATA_LOOTED = SynchedEntityData.defineId(
        HangingBodyEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> DATA_SKIN_INDEX = SynchedEntityData.defineId(
        HangingBodyEntity.class, EntityDataSerializers.INT
    );
    
    public static final List<String> SKIN_USERNAMES = List.of(
        "MarcusKron", "DreamingCodes", "Brodino96", "FabioTobi_", "ArgoSeven",
        "Kepp2", "Notch", "Capobastone", "BigBoss340", "JadexFire",
        "DOMOLOCO", "Jade", "Ryo7nix", "Tvnaka", "Tia05",
        "maryvern", "ziNSaNiTY_", "Pella97", "brimstone", "Player_GPT", "Puccilillo"
    );
    
    public static final Map<String, String> SKIN_UUIDS = Map.ofEntries(
        Map.entry("MarcusKron", "7f4b964d-7661-41bf-b71e-33ff01f3994d"),
        Map.entry("DreamingCodes", "77883b4e-effa-41c1-af43-38f4194c3933"),
        Map.entry("Brodino96", "9b79ce12-5ad1-4984-ab5e-9447df87af1e"),
        Map.entry("FabioTobi_", "fdd44537-e3c0-4d95-9a5a-4ef0ecd845e2"),
        Map.entry("ArgoSeven", "68d5b47f-a704-4182-bd77-3aa384322a63"),
        Map.entry("Kepp2", "265e7dcc-d8fb-47f2-a57b-11dd4911e75d"),
        Map.entry("Notch", "069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        Map.entry("Capobastone", "c1a0f1c6-15b3-438f-beea-ee2b9cf8c29c"),
        Map.entry("BigBoss340", "2ff2af20-29dc-48ef-8d33-262e44eaafc4"),
        Map.entry("JadexFire", "4e4e7934-63de-412b-b098-a7e51ae59a1f"),
        Map.entry("DOMOLOCO", "3d247909-b2dc-45e0-bf6c-4702fa5c3554"),
        Map.entry("Jade", "9ff71a76-3033-4acd-a3d9-c48f1866af5a"),
        Map.entry("Ryo7nix", "e15e2ca1-3123-4636-994c-8c5efdf0517d"),
        Map.entry("Tvnaka", "9b90db4d-b859-41b3-a97a-02ea04e8987e"),
        Map.entry("Tia05", "f8ec68fa-a706-44fe-a51a-1d34878898bf"),
        Map.entry("maryvern", "741c3dd9-410f-4eef-aed9-0a9dcaf151f1"),
        Map.entry("ziNSaNiTY_", "b4010360-2847-4637-aad6-dca0c00bccd6"),
        Map.entry("Pella97", "8a63da0d-3ec6-4f3b-8a80-2db7102e107a"),
        Map.entry("brimstone", "fe7f2c93-c0fc-4db1-9caf-a8764c84e922"),
        Map.entry("Player_GPT", "c89053a1-f8ad-4023-b2ad-191ab3672649"),
        Map.entry("Puccilillo", "8175325f-db77-42a3-8e30-7c5845b2cf55")
    );
    
    private static final Random RANDOM = new Random();
    
    public HangingBodyEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        if (!level.isClientSide) {
            this.setSkinIndex(RANDOM.nextInt(SKIN_USERNAMES.size()));
        }
    }
    
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LOOTED, false);
        this.entityData.define(DATA_SKIN_INDEX, 0);
    }
    
    public boolean isLooted() {
        return this.entityData.get(DATA_LOOTED);
    }
    
    public void setLooted(boolean looted) {
        this.entityData.set(DATA_LOOTED, looted);
    }
    
    public int getSkinIndex() {
        return this.entityData.get(DATA_SKIN_INDEX);
    }
    
    public void setSkinIndex(int index) {
        this.entityData.set(DATA_SKIN_INDEX, index);
    }
    
    public String getSkinUsername() {
        int index = getSkinIndex();
        if (index >= 0 && index < SKIN_USERNAMES.size()) {
            return SKIN_USERNAMES.get(index);
        }
        return SKIN_USERNAMES.get(0);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        
        if (!this.level.isClientSide) {
            BlockPos logPos = this.blockPosition().above(ROPE_LENGTH + 1);
            if (!this.level.getBlockState(logPos).is(BlockTags.LOGS)) {
                this.discard();
            }
        }
    }
    
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level.isClientSide && !this.isLooted()) {
            ItemStack primeEnergy = new ItemStack(FatPeople.PRIME_ENERGY_ITEM);
            
            if (!player.getInventory().add(primeEnergy)) {
                player.drop(primeEnergy, false);
            }
            
            // Only play sound if not already playing
            if (FatPeople.canPlayAmbienceSound()) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(), 
                    FatPeople.SUICIDE_FOREST_AMBIENCE, this.getSoundSource(), 1.0F, 1.0F);
                FatPeople.markAmbienceSoundPlayed();
            }
            
            this.setLooted(true);
            this.discard();
            
            return InteractionResult.SUCCESS;
        }
        
        return this.isLooted() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level.isClientSide && !this.isInvulnerableTo(source)) {
            this.discard();
            return true;
        }
        return false;
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setLooted(tag.getBoolean("Looted"));
        if (tag.contains("SkinIndex")) {
            this.setSkinIndex(tag.getInt("SkinIndex"));
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Looted", this.isLooted());
        tag.putInt("SkinIndex", this.getSkinIndex());
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < MAX_RENDER_DISTANCE_SQ;
    }
    
    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
