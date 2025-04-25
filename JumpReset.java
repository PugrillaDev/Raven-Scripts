/* 
    1.8 velocity bypass
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/JumpReset.java"
*/

boolean setJump, ignoreNext, aiming;
int lastHurtTime;
double lastFallDistance;

void onLoad() {
    modules.registerSlider("Chance", "%", 100, 0, 100, 1);
    modules.registerButton("Mouse down", false);
    modules.registerButton("Moving forward", true);
    modules.registerButton("Aiming on player", true);
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    int hurtTime = player.getHurtTime();
    boolean onGround = player.onGround();

    if (onGround && lastFallDistance > 3 && !client.allowFlying()) ignoreNext = true;

    if (hurtTime > lastHurtTime) {
        boolean mouseDown = keybinds.isMouseDown(0) || !modules.getButton(scriptName, "Mouse down");
        boolean aimingAt = aiming || !modules.getButton(scriptName, "Aiming on player");
        boolean forward = keybinds.isKeyDown(keybinds.getKeyCode("forward")) || !modules.getButton(scriptName, "Moving forward");
        if (!ignoreNext && onGround && aimingAt && forward && mouseDown && !player.isBurning() && util.randomDouble(0, 100) < modules.getSlider(scriptName, "Chance") && !hasBadEffect()) {
            keybinds.setPressed("jump", setJump = true);
        }
        ignoreNext = false;
    }

    lastHurtTime = hurtTime;
    lastFallDistance = player.getFallDistance();
}

void onPostMotion() {
    if (setJump && !keybinds.isKeyDown(keybinds.getKeyCode("jump"))) {
        keybinds.setPressed("jump", setJump = false);
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C03) {
        C03 c03 = (C03) packet;
        if (c03.name.startsWith("C05") || c03.name.startsWith("C06")) {
            Object[] hit = client.raycastEntity(5, c03.yaw, c03.pitch);
            aiming = hit != null && "EntityOtherPlayerMP".equals(((Entity) hit[0]).type);
        }
    }
    return true;
}

boolean hasBadEffect() {
    for (Object[] effect : client.getPlayer().getPotionEffects()) {
        String name = (String) effect[1];
        return "potion.jump".equals(name) || "potion.poison".equals(name) || "potion.wither".equals(name);
    }
    return false;
}