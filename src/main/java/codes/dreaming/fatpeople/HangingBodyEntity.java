package codes.dreaming.fatpeople;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import java.util.List;
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
            
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), 
                FatPeople.SUICIDE_FOREST_AMBIENCE, this.getSoundSource(), 1.0F, 1.0F);
            
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
