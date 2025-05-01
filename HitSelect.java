boolean set;
double val;
boolean sprintState;

void onLoad() {
    modules.registerSlider("Mode", "", 0, new String[]{"Prioritize Second Hit", "Prioritize Critical Hits", "Prioritize WTap Hits"});
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C0B) {
        C0B c0b = (C0B) packet;
        if (c0b.action.equals("START_SPRINTING")) {
            sprintState = true;
        } else if (c0b.action.equals("STOP_SPRINTING")) {
            sprintState = false;
        }
    }

    if (!(packet instanceof C02)) return true;
    
    C02 c02 = (C02) packet;
    if (c02.action == null || !c02.action.equals("ATTACK")) return true;
    
    Entity target = c02.entity;
    if (target == null || target.type == null || target.type.equals("EntityLargeFireball")) return true;
    Entity player = client.getPlayer();
    
    int mode = (int) modules.getSlider(scriptName, "Mode");
    boolean allowAttack = true;
    switch (mode) {
        case 0:
            allowAttack = prioritizeSecondHit(player, target);
            break;
        case 1:
            allowAttack = prioritizeCriticalHits(player);
            break;
        case 2:
            allowAttack = prioritizeWTapHits(player, sprintState);
        default:
            break;
    }
    
    return allowAttack;
}

boolean prioritizeSecondHit(Entity player, Entity target) {
    if (target.getHurtTime() != 0) return true;
    if (player.getHurtTime() <= player.getMaxHurtTime() - 1) return true;

    double distance = player.getPosition().distanceTo(target.getPosition());
    if (distance < 2.5) return true;

    if (!isMovingTowards(target, player, 60)) return true;
    if (!isMovingTowards(player, target, 60)) return true;

    fixMotion();
    return false;
}

boolean prioritizeCriticalHits(Entity player) {
    if (player.onGround() || player.getHurtTime() != 0) return true;
    
    double fallDistance = player.getFallDistance();
    if (fallDistance > 0) return true;
    
    fixMotion();
    return false;
}

boolean prioritizeWTapHits(Entity player, boolean sprinting) {
    if (player.isCollidedHorizontally()) return true;
    if (client.getForward() != 1) return true;
    if (sprinting) return true;

    fixMotion();
    return false;
}

void fixMotion() {
    if (set) return;

    val = modules.getSlider("KeepSprint", "Slow %");
    set = true;
    modules.enable("KeepSprint");
    modules.setSlider("KeepSprint", "Slow %", 0);
}

void resetMotion() {
    if (set) {
        modules.setSlider("KeepSprint", "Slow %", val);
        modules.disable("KeepSprint");
        set = false;
        val = 0;
    }
}

void onPostMotion() {
    resetMotion();
}

boolean isMovingTowards(Entity source, Entity target, double angleThreshold) {
    Vec3 sourceCurrent = source.getPosition();
    Vec3 sourceLast = source.getLastPosition();
    Vec3 targetPosition = target.getPosition();

    double motionX = sourceCurrent.x - sourceLast.x;
    double motionZ = sourceCurrent.z - sourceLast.z;

    double motionLength = Math.sqrt(motionX * motionX + motionZ * motionZ);
    if (motionLength == 0) return false;
    motionX /= motionLength;
    motionZ /= motionLength;

    double toTargetX = targetPosition.x - sourceCurrent.x;
    double toTargetZ = targetPosition.z - sourceCurrent.z;

    double toTargetLength = Math.sqrt(toTargetX * toTargetX + toTargetZ * toTargetZ);
    if (toTargetLength == 0) return false;
    toTargetX /= toTargetLength;
    toTargetZ /= toTargetLength;

    double dotProduct = (motionX * toTargetX) + (motionZ * toTargetZ);
    return dotProduct >= Math.cos(Math.toRadians(angleThreshold));
}