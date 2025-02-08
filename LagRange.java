int disableTicks;
int lastAttack;
double closest;
boolean toggle;
double startFallHeight;
Vec3 lastPosition;
boolean fell;

void onLoad() {
    modules.registerSlider("Latency", "ms", 300, 0, 500, 50);
    modules.registerSlider("Activation Distance", " blocks", 13, 0, 20, 1);
    modules.registerSlider("Hurttime", "", 0, 0, 10, 1);
    modules.registerButton("Check Team", true);
    modules.registerButton("Check Ping", true);
    modules.registerButton("Check Weapon", true);
    modules.registerButton("Check Distance", true);
    modules.registerButton("Release on damage", true);
    modules.registerButton("Release on fall", true);
    modules.registerButton("Release on swap", false);
    modules.registerButton("Release when breaking", false);
    modules.registerButton("Swords", true);
    modules.registerButton("Stick", true);
    modules.registerButton("Fishing Rod", false);
}

void onPreUpdate() {
    disableTicks--;
    double boxSize = modules.getSlider(scriptName, "Activation Distance");
    double latency = modules.getSlider(scriptName, "Latency");
    if (latency != modules.getSlider("Fake Lag", "Packet delay")) {
        disableTicks = 10;
        modules.disable("Fake Lag");
        modules.setSlider("Fake Lag", "Packet delay", latency);
    }

    Entity player = client.getPlayer();
    Vec3 myPosition = player.getPosition();
    boolean onGround = player.onGround();

    if (player.getTicksExisted() % 5 == 0) {
        closest = -1;
        boolean checkTeams = modules.getButton(scriptName, "Check Team");
        String team = "";
        if (checkTeams) {
            String name = player.getNetworkPlayer() != null ? player.getNetworkPlayer().getDisplayName() : player.getDisplayName(); // Get nick on hypixel
            if (name.length() >= 2 && name.startsWith(util.colorSymbol)) {
                team = name.substring(0, 2);
            }
        }

        boolean checkPing = modules.getButton(scriptName, "Check Ping");
        for (Entity p : world.getPlayerEntities()) {
            if (p == player) continue;

            NetworkPlayer nwp = p.getNetworkPlayer();
            if (nwp == null || (checkPing && nwp.getPing() != 1)) continue;

            char uv = p.getUUID().charAt(14);
            if (uv != '4' && uv != '1') continue;

            if (checkTeams) {
                String displayName = p.getDisplayName();
                if (!team.isEmpty() && displayName.startsWith(team)) continue; // If player's name has the same team color as our name
                if (p.isInvisible() && displayName.startsWith(util.colorSymbol + "c") && !displayName.contains(" ")) continue; // Watchdog Bot
            }

            Vec3 position = p.getPosition();
            if (Math.abs(position.x - myPosition.x) > boxSize || Math.abs(position.z - myPosition.z) > boxSize || Math.abs(position.y - myPosition.y) > boxSize) continue; // More efficient than pythagorean distance check

            double distanceSq = position.distanceToSq(myPosition);
            if (closest == -1 || distanceSq < closest) {
                closest = distanceSq;
            }
        }
    }

    ItemStack held = player.getHeldItem();
    boolean correctHeldItem = !modules.getButton(scriptName, "Check Weapon");
    if (!correctHeldItem) {
        boolean holdingWeapon = false;
        if (held != null) {
            holdingWeapon = held.type.equals("ItemSword") && modules.getButton(scriptName, "Swords") ||
                            held.name.equals("stick") && modules.getButton(scriptName, "Stick") || 
                            held.name.equals("fishing_rod") && modules.getButton(scriptName, "Fishing Rod");
        }
        correctHeldItem = holdingWeapon;
    }

    toggle = correctHeldItem && disableTicks <= 0 && client.getScreen().isEmpty() && closest != -1 && closest < boxSize * boxSize;

    if (player.getHurtTime() == player.getMaxHurtTime() - 1 && modules.getButton(scriptName, "Release on damage")) {
        disableTicks = 10;
        toggle = false;
    }

    if (lastPosition != null && !onGround && lastPosition.y > myPosition.y && myPosition.y > startFallHeight) {
        startFallHeight = myPosition.y;
    } else if (onGround && myPosition.y < startFallHeight) {
        if (startFallHeight - myPosition.y > 3 && modules.getButton(scriptName, "Release on fall") && !client.allowFlying()) {
            disableTicks = 6;
            toggle = false;
        }
        startFallHeight = -Double.MAX_VALUE;
    }
    lastPosition = myPosition;
}

void onPostMotion() {
    if (toggle) {
        modules.enable("Fake Lag");
    } else {
        modules.disable("Fake Lag");
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C02) {
        C02 c02 = (C02) packet;
        Entity entity = c02.entity;
        if (entity == null || c02.action == null || entity.type == null || entity.type.equals("EntityLargeFireball")) return true;

        if (c02.action.equals("ATTACK")) {
            if (entity.getHurtTime() <= modules.getSlider(scriptName, "Hurttime")) {
                disableTicks = 2;
                toggle = false;
            }
            if (modules.getButton(scriptName, "Check Distance") && entity.getPosition().distanceToSq(client.getPlayer().getPosition()) < 2.25) {
                disableTicks = 10;
                toggle = false;
            }
        } else {
            if (entity.getHurtTime() == 0) {
                disableTicks = 2;
                toggle = false;
            }
        }
    } else if (packet instanceof C09) {
        if (modules.getButton(scriptName, "Release on swap")) {
            disableTicks = 2;
            toggle = false;
        }
    } else if (packet instanceof C07) {
        if (modules.getButton(scriptName, "Release when breaking")) {
            C07 c07 = (C07) packet;
            if (c07.status.equals("ABORT_DESTROY_BLOCK") || c07.status.equals("STOP_DESTROY_BLOCK") || c07.status.equals("START_DESTROY_BLOCK")) {
                disableTicks = 2;
                toggle = false;
            }
        }
    }

    return true;
}