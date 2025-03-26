/* 
    hacker detector designed for hypixel but works elsewhere
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/CustomAC.java"
*/

Map<Entity, Map<String, Object>> anticheatPlayers = new HashMap<>();
Map<String, Object> debugData = null;
Entity debugEntity = null;
String team = "";
String[] checks = {"NoSlowA", "AutoBlockA", "SprintA", "VelocityA", "RotationA", "ScaffoldA", /* "ScaffoldB",  */"ScaffoldC"};

void onLoad() {
    modules.registerDescription("Unreliable in replays");
    modules.registerButton("Ignore Teammates", true);

    for (String check : checks) {
        modules.registerGroup(check);
        modules.registerButton(check, check + " enabled", true);
        modules.registerButton(check, check + " Alerts", true);
        modules.registerSlider(check, check + " Cooldown", "s", 1, 0, 20, 1);
        modules.registerSlider(check, check + " VL", "", 10, 1, 50, 1);
    }

    modules.registerButton("Debug Mode", false);
}

@SuppressWarnings("unchecked")
void updatePlayerData(Entity p, Map<String, Object> playerData) {
    int currentTick = p.getTicksExisted();

    boolean lastCrouching = (Boolean) playerData.getOrDefault("crouching", false);
    boolean lastSprinting = (Boolean) playerData.getOrDefault("sprinting", false);
    boolean lastUsing = (Boolean) playerData.getOrDefault("using", false);
    boolean lastOnEdge = (Boolean) playerData.getOrDefault("onEdge", false);
    int lastSwing = (Integer) playerData.getOrDefault("swingProgress", 0);
    boolean lastOnGround = (Boolean) playerData.getOrDefault("onGround", false);
    ItemStack lastHeldItem = (ItemStack) playerData.getOrDefault("heldItem", null);
    double lastDeltaY = (Double) playerData.getOrDefault("lastDeltaY", 0d);

    boolean isCrouching = p.isSneaking();
    boolean isSprinting = p.isSprinting();
    boolean isUsing = p.isUsingItem();
    int swingProgress = p.getSwingProgress();
    boolean isCollided = p.isCollided();
    boolean isDead = p.isDead();
    boolean isBurning = p.isBurning();
    boolean onEdge = p.isOnEdge();

    Vec3 currentPosition = p.getPosition();
    Vec3 lastPosition = p.getLastPosition();
    double deltaX = currentPosition.x - lastPosition.x;
    double deltaY = currentPosition.y - lastPosition.y;
    double deltaZ = currentPosition.z - lastPosition.z;

    float rotationYaw = p.getYaw();
    float moveYaw = getMoveYaw(deltaX, deltaZ, rotationYaw);
    float pitch = p.getPitch();
    float prevYaw = p.getPrevYaw();
    float prevPitch = p.getPrevPitch();

    boolean onGround = p.onGround();
    String name = p.getName();
    String displayName = p.getDisplayName();
    int hurtTime = p.getHurtTime();
    int maxHurtTime = p.getMaxHurtTime();
    Entity riding = p.getRidingEntity();
    ItemStack heldItem = p.getHeldItem();
    String blockOn = world.getBlockAt((int) Math.floor(currentPosition.x), (int) Math.floor(currentPosition.y) - 1, (int) Math.floor(currentPosition.z)).name;

    playerData.put("lastCrouching", lastCrouching);
    playerData.put("lastSprinting", lastSprinting);
    playerData.put("lastUsing", lastUsing);
    playerData.put("lastOnEdge", lastOnEdge);
    playerData.put("lastSwinging", lastSwing);
    playerData.put("lastBlockOn", playerData.getOrDefault("blockOn", "air"));
    playerData.put("lastOnGround", lastOnGround);
    playerData.put("lastHeldItem", lastHeldItem);
    playerData.put("lastDeltaY", deltaY);

    playerData.put("name", name);
    playerData.put("displayName", displayName);
    playerData.put("hurtTime", hurtTime);
    playerData.put("maxHurtTime", maxHurtTime);
    playerData.put("ticksExisted", currentTick);
    playerData.put("position", currentPosition);
    playerData.put("lastPosition", lastPosition);
    playerData.put("yaw", rotationYaw);
    playerData.put("pitch", pitch);
    playerData.put("previousYaw", prevYaw);
    playerData.put("previousPitch", prevPitch);
    playerData.put("swingProgress", swingProgress);
    playerData.put("riding", riding);
    playerData.put("dead", isDead);
    playerData.put("onGround", onGround);
    playerData.put("heldItem", heldItem);
    playerData.put("collided", isCollided);
    playerData.put("burning", isBurning);
    playerData.put("moveYaw", moveYaw);
    playerData.put("onEdge", onEdge);
    playerData.put("blockOn", blockOn);
    playerData.put("crouching", isCrouching);
    playerData.put("sprinting", isSprinting);
    playerData.put("using", isUsing);

    List<Vec3> previousPositions = (List<Vec3>) playerData.get("previousPositions");
    if (previousPositions == null) {
        previousPositions = new ArrayList<>(20);
        playerData.put("previousPositions", previousPositions);
    } else if (previousPositions.size() >= 20) {
        previousPositions.remove(previousPositions.size() - 1);
    }
    previousPositions.add(0, currentPosition);

    if (isCrouching && !lastCrouching) {
        playerData.put("lastCrouchedTick", currentTick);
    }
    if (!isCrouching && lastCrouching) {
        playerData.put("lastStopCrouchingTick", currentTick);
    }
    if (isSprinting && !lastSprinting) {
        playerData.put("lastSprintingTick", currentTick);
    }
    if (isUsing && !lastUsing) {
        playerData.put("lastUsingTick", currentTick);
    }
    if (!isUsing && lastUsing) {
        playerData.put("lastStopUsingTick", currentTick);
        playerData.put("lastStopUsingItem", lastHeldItem);
    }
    if (lastSwing == 0 && swingProgress == 1) {
        playerData.put("lastSwingTick", currentTick);
        playerData.put("lastSwingItem", heldItem);
    }
    if (!onEdge && lastOnEdge) {
        playerData.put("lastEdgeTick", currentTick);
    }
    if ((heldItem == null && lastHeldItem != null) || (heldItem != null && lastHeldItem == null) || (heldItem != null && lastHeldItem != null && (heldItem.meta != lastHeldItem.meta || !heldItem.name.equals(lastHeldItem.name)))) {
        playerData.put("lastItemChangeTick", currentTick);
    }
    if (blockOn.equals("air")) {
        playerData.put("lastOnAir", currentTick);
    }
    if (deltaY < -0.1 && lastDeltaY >= -0.1) {
        playerData.put("lastStartFallingTick", currentTick);
        playerData.put("lastStartFallingPosition", currentPosition);
    }
    if (onGround && !lastOnGround) {
        playerData.put("lastStopFallingTick", currentTick);
        playerData.put("lastStopFallingPosition", currentPosition);
    }
    if (!onGround && lastOnGround) {
        playerData.put("lastOnGroundTick", currentTick);
    }
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    boolean ignoreTeammates = modules.getButton(scriptName, "Ignore Teammates");

    if (ignoreTeammates && !client.allowFlying() && !player.isInvisible()) {
        NetworkPlayer nwp = player.getNetworkPlayer();
        String name = nwp != null ? nwp.getDisplayName() : player.getDisplayName();
        if (name.length() >= 2 && name.startsWith(util.colorSymbol)) {
            team = name.substring(0, 2);
        }
    }

    for (Entity p : world.getPlayerEntities()) {
        if (p == player || p.isDead()) continue;

        Map<String, Object> playerData = anticheatPlayers.computeIfAbsent(p, k -> new HashMap<>());
        updatePlayerData(p, playerData);

        if (ignoreTeammates) {
            String displayName = p.getDisplayName();
            if (!team.isEmpty() && displayName.startsWith(team)) continue;
        }

        for (String check : checks) {
            if (!modules.getButton(scriptName, check + " enabled")) continue;

            int cooldown = (int) modules.getSlider(scriptName, check + " Cooldown") * 1000;
            int vlThreshold = (int) modules.getSlider(scriptName, check + " VL");
            boolean alerts = modules.getButton(scriptName, check + " Alerts");
            
            switch (check) {
                case "AutoBlockA": AutoBlockA(playerData, cooldown, vlThreshold, alerts); break;
                case "NoSlowA": NoSlowA(playerData, cooldown, vlThreshold, alerts); break;
                case "SprintA": SprintA(playerData, cooldown, vlThreshold, alerts); break;
                case "VelocityA": VelocityA(playerData, cooldown, vlThreshold, alerts); break;
                case "RotationA": RotationA(playerData, cooldown, vlThreshold, alerts); break;
                case "ScaffoldA": ScaffoldA(playerData, cooldown, vlThreshold, alerts); break;
                case "ScaffoldB": ScaffoldB(playerData, cooldown, vlThreshold, alerts); break;
                case "ScaffoldC": ScaffoldC(playerData, cooldown, vlThreshold, alerts); break;
            }
        }
    }

    if (!modules.getButton(scriptName, "Debug Mode")) {
        debugData = null;
        debugEntity = null;
    }
}

boolean onPacketSent(CPacket packet) {
    if (!(packet instanceof C02)) return true;
    C02 c02 = (C02) packet;
    Entity attackedEntity = c02.entity;
    if (attackedEntity != null && attackedEntity.type.equals("EntityOtherPlayerMP") && anticheatPlayers.containsKey(attackedEntity) && modules.getButton(scriptName, "Debug Mode")) {
        debugEntity = attackedEntity;
        debugData = anticheatPlayers.get(attackedEntity);
    }
    return true;
}

void onRenderTick(float partialTicks) {
    if (debugData == null) return;
    int startX = 10;
    int startY = 10;
    int lineHeight = render.getFontHeight();
    render.text2d(util.color("&cDebug Information:"), startX, startY, 1, 0xFF000000, true);
    int line = 1;
    List<Map.Entry<String, Object>> sortedEntries = new ArrayList<>(debugData.entrySet());
    sortedEntries.sort(Comparator.comparing(entry -> entry.getKey().toLowerCase()));
    for (Map.Entry<String, Object> entry : sortedEntries) {
        String key = entry.getKey();
        Object value = entry.getValue();
        String text = util.color("&a" + key + ": &f" + (value != null ? value.toString() : "null"));
        render.text2d(text, startX, startY + (line++ * lineHeight), 1, 0xFF000000, true);
    }
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        anticheatPlayers.clear();
        debugData = null;
        debugEntity = null;
    }
}

void printFlag(Map<String, Object> anticheatPlayer, String flag, int vl) {
    String displayName = (String) anticheatPlayer.getOrDefault("displayName", "&7Unknown");
    String nameColor = displayName.length() >= 2 && displayName.startsWith(util.colorSymbol) ? displayName.substring(0, 2) : util.colorSymbol + "7";
    String playerName = (String) anticheatPlayer.getOrDefault("name", "Unknown");

    Message msg = new Message(util.color("&8[&cAntiCheat&8] "));
    msg.appendStyle("CLICK", "RUN_COMMAND", "/wdr " + util.strip(playerName), nameColor + playerName);
    msg.append(util.color(" &7flagged &c" + flag + "&7. &8(&cVL: " + vl + "&8)"));

    client.print(msg);
}

float getMoveYaw(double deltaX, double deltaZ, float playerYaw) {
    if (Math.abs(deltaX) < 1e-8 && Math.abs(deltaZ) < 1e-8) {
        return 0f;
    }

    float moveAngle = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
    float relativeAngle = moveAngle - playerYaw;
    relativeAngle = (relativeAngle % 360 + 360) % 360;
    if (relativeAngle > 180) {
        relativeAngle -= 360;
    }

    return relativeAngle;
}

boolean checkSurroundingBlocks(Vec3 position) {
    double[][] offsets = {{0.5, 0}, {-0.5, 0}, {0, 0.5}, {0, -0.5}};
    int legHeight = (int) Math.floor(position.y);
    int torsoHeight = legHeight + 1;

    for (double[] offset : offsets) {
        double offsetX = offset[0];
        double offsetZ = offset[1];
        String blockLeg = world.getBlockAt((int) Math.floor(position.x + offsetX), legHeight, (int) Math.floor(position.z + offsetZ)).name;
        String blockTorso = world.getBlockAt((int) Math.floor(position.x + offsetX), torsoHeight, (int) Math.floor(position.z + offsetZ)).name;
        if (!blockLeg.equals("air") || !blockTorso.equals("air")) {
            return true;
        }
    }

    return false;
}

// CHECKS

void NoSlowA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("NoSlowA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("NoSlowA_LastAlert", 0L);

    boolean isSprinting = Boolean.TRUE.equals(anticheatPlayer.get("sprinting"));
    boolean isUsingItem = Boolean.TRUE.equals(anticheatPlayer.get("using"));
    boolean isRiding = anticheatPlayer.get("riding") != null;
    int ticks = (int) anticheatPlayer.get("ticksExisted");
    int lastStartUsing = (int) anticheatPlayer.getOrDefault("lastUsingTick", 0);
    int lastItemSwap = (int) anticheatPlayer.getOrDefault("lastItemChangeTick", 0);

    if (isUsingItem && isSprinting && !isRiding && lastStartUsing - lastItemSwap > 1) {
        boolean isSameItem = true;
        int ticksNotUsing = (int) anticheatPlayer.getOrDefault("lastUsingTick", 0) - (int) anticheatPlayer.getOrDefault("lastStopUsingTick", 0);
        if (ticksNotUsing <= 5) {
            ItemStack heldItem = (ItemStack) anticheatPlayer.get("heldItem");
            ItemStack lastStopUsingItem = (ItemStack) anticheatPlayer.get("lastStopUsingItem");
            String heldItemKey = heldItem != null ? heldItem.name + ":" + heldItem.meta : null;
            String lastStopUsingItemKey = lastStopUsingItem != null ? lastStopUsingItem.name + ":" + lastStopUsingItem.meta : null;
            isSameItem = heldItemKey != null && heldItemKey.equals(lastStopUsingItemKey);
        }

        if (ticksNotUsing > 5 || !isSameItem) {
            vl++;
            if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
                anticheatPlayer.put("NoSlowA_LastAlert", client.time());
                printFlag(anticheatPlayer, "NoSlowA", vl);
                if (alerts) {
                    client.ping();
                }
            }
        }
    } else {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("NoSlowA_VL", vl);
}

void AutoBlockA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("AutoBlockA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("AutoBlockA_LastAlert", 0L);

    int swingProgress = (int) anticheatPlayer.get("swingProgress");
    boolean isUsingItem = Boolean.TRUE.equals(anticheatPlayer.get("using"));
    int ticksUsing = (int) anticheatPlayer.get("ticksExisted") - (int) anticheatPlayer.getOrDefault("lastUsingTick", 0);
    ItemStack heldItem = (ItemStack) anticheatPlayer.get("heldItem");

    if (isUsingItem && heldItem != null && heldItem.type.equals("ItemSword")) {
        if (swingProgress != 0) {
            vl++;
            if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
                anticheatPlayer.put("AutoBlockA_LastAlert", client.time());
                printFlag(anticheatPlayer, "AutoBlockA", vl);
                if (alerts) {
                    client.ping();
                }
            }
        }
    } else {
        vl = Math.max(vl - 4, 0);
    }

    anticheatPlayer.put("AutoBlockA_VL", vl);
}

void SprintA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("SprintA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("SprintA_LastAlert", 0L);

    boolean isSprinting = Boolean.TRUE.equals(anticheatPlayer.get("sprinting"));
    boolean isGroundCollision = Boolean.TRUE.equals(anticheatPlayer.get("onGround"));
    boolean isRiding = anticheatPlayer.get("riding") != null;
    float moveYaw = (float) anticheatPlayer.get("moveYaw");
    float rotationYaw = (float) anticheatPlayer.get("yaw");
    Vec3 current = (Vec3) anticheatPlayer.get("position");
    Vec3 previous = (Vec3) anticheatPlayer.get("lastPosition");
    double speed = Math.max(Math.abs(current.x - previous.x), Math.abs(current.z - previous.z));

    if (!isRiding && isSprinting && isGroundCollision && Math.abs(moveYaw) > 90 && speed >= 0.2) {
        vl++;

        if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
            anticheatPlayer.put("SprintA_LastAlert", client.time());
            printFlag(anticheatPlayer, "SprintA", vl);
            if (alerts) {
                client.ping();
            }
        }
    } else {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("SprintA_VL", vl);
}

void VelocityA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("VelocityA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("VelocityA_LastAlert", 0L);

    int hurtTime = (int) anticheatPlayer.get("hurtTime");
    int maxHurtTime = (int) anticheatPlayer.get("maxHurtTime");
    int ticksExisted = (int) anticheatPlayer.get("ticksExisted");
    int startFall = (int) anticheatPlayer.getOrDefault("lastStartFallingTick", 0);
    int stopFall = (int) anticheatPlayer.getOrDefault("lastStopFallingTick", 0);
    int ticksSinceFall = ticksExisted - stopFall;
    int fallTicks = Math.max(0, stopFall - startFall);
    boolean burning = Boolean.TRUE.equals(anticheatPlayer.get("burning"));
    Vec3 position = (Vec3) anticheatPlayer.get("position");
    Vec3 lastPosition = (Vec3) anticheatPlayer.get("lastPosition");
    boolean recentFall = fallTicks >= 6 && ticksSinceFall <= 6;
    double deltaXZ = Math.sqrt(Math.pow(position.z - lastPosition.z, 2) + Math.pow(position.z - lastPosition.z, 2));

    if (!burning && hurtTime > 0 && hurtTime < maxHurtTime && !recentFall && Math.abs(deltaXZ) < 1e-8) {
        boolean isCollided = checkSurroundingBlocks(position);
        if (!isCollided) {
            vl++;
            if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
                anticheatPlayer.put("VelocityA_LastAlert", client.time());
                printFlag(anticheatPlayer, "VelocityA", vl);
                if (alerts) {
                    client.ping();
                }
            }
        }
    } else {
        vl = Math.max(vl - 1, 0);
    }
    
    anticheatPlayer.put("VelocityA_VL", vl);
}

void RotationA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("RotationA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("RotationA_LastAlert", 0L);

    float pitch = (float) anticheatPlayer.get("pitch"); // False flags in replays due to inaccurate pitch!

    if (Math.abs(pitch) > 90) {
        vl++;
        if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
            anticheatPlayer.put("RotationA_LastAlert", client.time());
            printFlag(anticheatPlayer, "RotationA", vl);
            if (alerts) {
                client.ping();
            }
        }
    } else {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("RotationA_VL", vl);
}

void ScaffoldA(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("ScaffoldA_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("ScaffoldA_LastAlert", 0L);

    int lastStopCrouch = (int) anticheatPlayer.getOrDefault("lastStopCrouchingTick", 0);
    int ticksExisted = (int) anticheatPlayer.get("ticksExisted");
    int lastSwing = (int) anticheatPlayer.getOrDefault("lastSwingTick", 0);
    int lastStartCrouch = (int) anticheatPlayer.getOrDefault("lastCrouchedTick", 0);
    Vec3 current = (Vec3) anticheatPlayer.get("position");
    boolean onGround = Boolean.TRUE.equals(anticheatPlayer.get("onGround")); // Does not flag in replays due to ground always being false for entities!
    float pitch = (float) anticheatPlayer.get("pitch");
    ItemStack lastSwingItem = (ItemStack) anticheatPlayer.get("lastSwingItem");
    boolean holdingBlocks = lastSwingItem == null ? false : lastSwingItem.type.startsWith("Block");
    boolean lookingDown = pitch >= 70f;

    if (lookingDown && onGround && holdingBlocks && lastSwing == ticksExisted && lastStopCrouch >= ticksExisted - 1 && lastStopCrouch - lastStartCrouch <= 2) {
        vl++;
        if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
            anticheatPlayer.put("ScaffoldA_LastAlert", client.time());
            printFlag(anticheatPlayer, "ScaffoldA", vl);
            if (alerts) {
                client.ping();
            }
        }
    } else if (!lookingDown || !holdingBlocks || ticksExisted - lastStopCrouch > 20 || ticksExisted - lastSwing > 20 || (onGround && lastSwing == ticksExisted && lastStopCrouch < ticksExisted - 1)) {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("ScaffoldA_VL", vl);
}

// Thought this would work well, but it does not. If anybody ends up improving, please lmk I cba
void ScaffoldB(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("ScaffoldB_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("ScaffoldB_LastAlert", 0L);
    
    int lastStopCrouch = (int) anticheatPlayer.getOrDefault("lastStopCrouchingTick", 0);
    int ticksExisted = (int) anticheatPlayer.get("ticksExisted");
    int lastSwing = (int) anticheatPlayer.getOrDefault("lastSwingTick", 0);
    int lastOnGround = (int) anticheatPlayer.getOrDefault("lastOnGroundTick", 0);
    float pitch = (float) anticheatPlayer.get("pitch");
    ItemStack lastSwingItem = (ItemStack) anticheatPlayer.get("lastSwingItem");
    boolean holdingBlocks = lastSwingItem != null && lastSwingItem.type.startsWith("Block");
    boolean lookingDown = pitch >= 70;
    
    if (lookingDown && holdingBlocks && ticksExisted - lastSwing <= 10 && lastStopCrouch == ticksExisted && ticksExisted == lastOnGround) {
        vl++;
        if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
            anticheatPlayer.put("ScaffoldB_LastAlert", client.time());
            printFlag(anticheatPlayer, "ScaffoldB", vl);
            if (alerts) {
                client.ping();
            }
        }
    } else if (!lookingDown || !holdingBlocks || ticksExisted - lastStopCrouch > 50 || ticksExisted - lastSwing > 50) {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("ScaffoldB_VL", vl);
}

@SuppressWarnings("unchecked")
void ScaffoldC(Map<String, Object> anticheatPlayer, int cooldown, int vlThreshold, boolean alerts) {
    int vl = (int) anticheatPlayer.getOrDefault("ScaffoldC_VL", 0);
    long lastAlert = (long) anticheatPlayer.getOrDefault("ScaffoldC_LastAlert", 0L);

    int lastStartCrouch = (int) anticheatPlayer.getOrDefault("lastCrouchedTick", 0);
    int lastStopCrouch = (int) anticheatPlayer.getOrDefault("lastStopCrouchingTick", 0);
    int ticksExisted = (int) anticheatPlayer.get("ticksExisted");
    int lastSwing = (int) anticheatPlayer.getOrDefault("lastSwingTick", 0);
    float moveYaw = (float) anticheatPlayer.get("moveYaw");
    float pitch = (float) anticheatPlayer.get("pitch");
    ItemStack lastSwingItem = (ItemStack) anticheatPlayer.get("lastSwingItem");
    List<Vec3> previousPositions = (List<Vec3>) anticheatPlayer.get("previousPositions");
    boolean holdingBlocks = lastSwingItem != null && lastSwingItem.type.startsWith("Block");
    boolean lookingDown = pitch >= 70; // Sometimes does not flag in replays due to inaccurate pitch!

    if (lookingDown && holdingBlocks && ticksExisted - lastSwing <= 10 && lastStopCrouch >= lastStartCrouch && ticksExisted - lastStopCrouch > 30 && Math.abs(moveYaw) >= 90 && previousPositions.size() >= 20) {
        double speed = 0;
        for (int i = 0; i < previousPositions.size() - 1; i++) {
            Vec3 current = previousPositions.get(i);
            Vec3 previous = previousPositions.get(i + 1);
            speed += Math.max(Math.abs(current.x - previous.x), Math.abs(current.z - previous.z));
        }

        double avgSpeed = speed / (previousPositions.size() - 1);
        Vec3 position = (Vec3) anticheatPlayer.get("position");
        Vec3 lastPosition = (Vec3) anticheatPlayer.get("lastPosition");
        double direction = Math.toRadians(Math.toDegrees(Math.atan2(position.z - lastPosition.z, position.x - lastPosition.x)) - 90);
        int baseY = (int) Math.floor(position.y);
        
        for (int i = 0; i < 3; i++) {
            String blockBelow = world.getBlockAt((int) Math.floor(lastPosition.x), baseY - i, (int) Math.floor(lastPosition.z)).name;
            if (!blockBelow.equals("air")) {
                baseY -= i;
                break;
            }
        }

        boolean onGround = Boolean.TRUE.equals(anticheatPlayer.get("onGround"));
        double totalDistance = previousPositions.get(previousPositions.size() - 1).distanceTo(previousPositions.get(0));
        String blockAhead = world.getBlockAt((int) Math.floor(position.x + (2 * Math.cos(direction))), baseY, (int) Math.floor(position.z + (2 * Math.sin(direction)))).name;
        boolean overAir = blockAhead.equals("air");
        String standingBlock = world.getBlockAt((int) Math.floor(lastPosition.x), baseY, (int) Math.floor(lastPosition.z)).name;
        boolean matchingBlock = standingBlock.equals(lastSwingItem.name);
        boolean unsupported = true;
        if (onGround) {
            String block1Below = world.getBlockAt((int) Math.floor(lastPosition.x), baseY - 1, (int) Math.floor(lastPosition.z)).name;
            String block2Below = world.getBlockAt((int) Math.floor(lastPosition.x), baseY - 2, (int) Math.floor(lastPosition.z)).name;
            unsupported = block1Below.equals("air") && block2Below.equals("air");
        }

        if (avgSpeed >= 0.14 && totalDistance > 3.4 && overAir && matchingBlock && unsupported) {
            vl++;
            if (vl >= vlThreshold && client.time() - lastAlert > cooldown) {
                anticheatPlayer.put("ScaffoldC_LastAlert", client.time());
                printFlag(anticheatPlayer, "ScaffoldC", vl);
                if (alerts) {
                    client.ping();
                }
            }
        } else {
            vl = Math.max(vl - 1, 0);
        }
    } else {
        vl = Math.max(vl - 1, 0);
    }

    anticheatPlayer.put("ScaffoldC_VL", vl);
}