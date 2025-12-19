package codes.dreaming.fatpeople;

public interface RollingAccessor {
    boolean isRolling();
    void setRolling(boolean rolling);
    float getStruggleProgress();
    void setStruggleProgress(float progress);
    float getStruggleVelocity();
    void setStruggleVelocity(float velocity);
    float getRollingSpeed();
    void setRollingSpeed(float speed);
    float getRollAngle();
    void setRollAngle(float angle);
}
