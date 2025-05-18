int status = -1;
int step = 0;
int tpAimingTicks = 0;
int attackingTicks = 0;
int chestTicks = 0;
int lastFavorite = 0;
int stopLookingNOW = 0;
float serverYaw, serverPitch;
boolean clickedMap = false;
boolean waiting = false;
long lastUpdate = client.time();
long lastAttack = client.time();
Vec3 spawnPoint = new Vec3(-38, 72, 0);
Map<String, Vec3> queueingCache = new HashMap<>();

void onLoad() {
    modules.registerSlider("Select Mode", "", 2, new String[] { "Solos", "Doubles", "Threes", "Fours" });
    modules.registerSlider("Aim Delay", "ms", 250, 0, 3000, 50);
}

void onEnable() {
    Entity en = findQueueingNPC();
    step = 0;
    clickedMap = false;
    serverYaw = client.getPlayer().getYaw();
    serverPitch = client.getPlayer().getPitch();
}

void onPostPlayerInput() {
    client.setForward(0);
    client.setStrafe(0);
}

void onPreMotion(PlayerState s) {
    long now = client.time();
    status = getBedwarsStatus();
    Entity player = client.getPlayer();
    Vec3 myPosition = player.getPosition();
    chestTicks++;

    String chest = inventory.getChest();
    if (chest.equals("Play Bed Wars")) {
        step = 2;
        lastUpdate = now;
    } else if (chest.equals("Bed Wars " + getArmorStandName())) {
        step = 3;
        lastUpdate = now;
    } else if (step > 1 && client.getScreen().isEmpty()) {
        client.print("wtf is this");
        step = 1;
        lastUpdate = now;
    }

    if (step == 0) {
        lastAttack = 0;
        if (status >= 2) {
            for (int i = 0; i < 100; i++) {
                client.chat("/");
            }
        } else if (status == 1) {
            client.chat("/stuck");
        } else {
            client.chat("/bedwars");
        }
        step++;
        waiting = true;
        lastUpdate = now;
    }
    else if (step == 1) {
        if (waiting) {
            if (now - lastUpdate > 3000) {
                step--;
                waiting = false;
                return;
            } else if (myPosition.distanceTo(spawnPoint) > 3) {
                return;
            }
            waiting = false;
            tpAimingTicks = 0;
        }

        ItemStack item = player.getHeldItem();
        if (item == null || !item.name.equals("blaze_rod")) {
            if (inventory.getStackInSlot(5) != null && !inventory.getStackInSlot(5).name.equals("blaze_rod")) {
                return;
            }
            inventory.setSlot(5);
        }

        Entity npc = findQueueingNPC();
        if (npc == null) return;

        float[] rotations = getRotations(npc.getPosition());

        int scaleFactor = (int) Math.floor(serverYaw / 360);
        float unwrappedYaw = rotations[0] + 360 * scaleFactor;
        if (unwrappedYaw < serverYaw - 180) {
            unwrappedYaw += 360;
        } else if (unwrappedYaw > serverYaw + 180) {
            unwrappedYaw -= 360;
        }

        float deltaYaw = unwrappedYaw - serverYaw;
        float deltaPitch = rotations[1] - serverPitch;

        if (--stopLookingNOW <= 0) {
            s.yaw = serverYaw + (Math.abs(deltaYaw) > 0.1 ? deltaYaw : 0); 
            s.pitch = serverPitch + (Math.abs(deltaPitch) > 0.1 ? deltaPitch : 0); 
        } else {
            attackingTicks = 0;
            tpAimingTicks = 0;
            return;
        }

        if (npc.getPosition().distanceToSq(myPosition) < 9) {
            tpAimingTicks = 0;

            if (++attackingTicks > 3 && now - lastAttack > 2000) {
                client.attack(npc);
                lastAttack = now;
            }
        } else {
            attackingTicks = 0;
            if (++tpAimingTicks > (int)(modules.getSlider(scriptName, "Aim Delay") / 50)) {
                client.sendPacketNoEvent(new C08(player.getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0.0, 0.0, 0.0)));
            }
        }
    }
    else if (step == 2) {
        if (now - lastUpdate > 1000) {
            step = 1;
        } 

        if (!client.getScreen().equals("GuiChest") || chestTicks < 4) return;

        for (int i = 0; i < inventory.getChestSize(); i++) {
            ItemStack chestItem = inventory.getStackInChestSlot(i);
            if (chestItem == null || !chestItem.displayName.startsWith(util.colorSymbol + "aMap Selector")) continue;
            inventory.click(i, 0, 0);
            chestTicks = 0;
            break;
        }
    } else if (step == 3) {

        if (now - lastUpdate > 2000) {
            step = 1;
        } 

        if (!client.getScreen().equals("GuiChest") || chestTicks < 5) return;

        List<Integer> favorites = new ArrayList<>();

        int generalFavorite = 0;
        for (int i = 0; i < inventory.getChestSize(); i++) {
            ItemStack chestItem = inventory.getStackInChestSlot(i);
            if (chestItem == null) continue;
            if (chestItem.displayName.startsWith(util.colorSymbol + "b")) {
                favorites.add(i);
            } else if (chestItem.displayName.startsWith(util.colorSymbol + "aRandom Map")) {
                generalFavorite = i;
            }
        }

        if (favorites.isEmpty()) {
            inventory.click(generalFavorite, 0, 0);
            closeScreen();
            step = 1;
            lastUpdate = now;
            clickedMap = true;
            return;
        }

        int slot = favorites.get((favorites.indexOf(lastFavorite) + 1) % favorites.size());
        if (favorites.size() == 1) slot = favorites.get(0);
        lastFavorite = slot;
        inventory.click(slot, 0, 0);
        clickedMap = true;
        closeScreen();
        step = 1;
        lastUpdate = now;
    }
}

void onWorldJoin(Entity en) {
    if (clickedMap && en == client.getPlayer()) {
        modules.disable(scriptName);
    }
}

void onGuiUpdate(String name, boolean opened) {
    if (opened) {
        chestTicks = 0;
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet.name.startsWith("C05") || packet.name.startsWith("C05")) {
        C03 c03 = (C03) packet;
        serverYaw = c03.yaw;
        serverPitch = c03.pitch;
    }
    return true;
}

Entity findQueueingNPC() {
    Entity player = client.getPlayer();
    String armorStandName = getArmorStandName();
    Vec3 cachedPosition = queueingCache.get(armorStandName);
    if (cachedPosition != null) {
        for (Entity p : world.getPlayerEntities()) {
            if (p == player) continue;
            if (p.getPosition().distanceToSq(cachedPosition) <= 2) {
                queueingCache.put(armorStandName, p.getPosition());
                return p;
            }
        }
    }

    Entity armorStand = null;
    for (Entity as : world.getEntities()) {
        if (as.getName().contains(armorStandName)) {
            armorStand = as;
            break;
        }
    }

    if (armorStand == null) return null;
    Vec3 armorStandPosition = armorStand.getPosition();

    for (Entity p : world.getPlayerEntities()) {
        if (p == player) continue;
        if (p.getPosition().distanceToSq(armorStandPosition) <= 2) {
            queueingCache.put(armorStandName, p.getPosition());
            return p;
        }
    }

    return null;
}

String getArmorStandName() {
    int mode = (int) modules.getSlider(scriptName, "Select Mode");

    switch (mode) {
        case 0: return "Solo";
        case 1: return "Doubles";
        case 2: return "3v3v3v3";
        case 3: return "4v4v4v4";
    }

    return "3v3v3v3";
}

void closeScreen() {
    if (!client.getScreen().isEmpty()) {
        client.closeScreen();
    }
}

float[] getRotations(Vec3 point) {
    Entity player = client.getPlayer();
    Vec3 pos = player.getPosition().offset(0, player.getEyeHeight(), 0);
    double x = point.x - pos.x;
    double y = point.y - pos.y;
    double z = point.z - pos.z;
    double dist = Math.sqrt(x * x + z * z);
    float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
    float pitch = (float) Math.toDegrees(-Math.atan2(y, dist));

    yaw = ((yaw % 360) + 360) % 360;
    if (yaw > 180) {
        yaw -= 360;
    }
    
    return new float[]{ yaw, pitch };
}

int getBedwarsStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    int size = sidebar.size();
    if (size < 7) return -1;

    if (!util.strip(sidebar.get(0)).startsWith("BED WARS")) {
        return -1;
    }

    String lobbyId = util.strip(sidebar.get(1)).split("  ")[1];
    if (lobbyId.charAt(lobbyId.length() - 1) == ']') {
        lobbyId = lobbyId.split(" ")[0];
    }

    if (lobbyId.charAt(0) == 'L') {
        return 1;
    }

    if (util.strip(sidebar.get(5)).startsWith("R Red:") && util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }

    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }

    return -1;
}