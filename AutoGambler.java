/* Auto Slumber Hotel Gambler I made ages ago, should work but its very scuffed. */

boolean gamble = false;
final String chatPrefix = "&7[&dS&7]&r ";
String slumberStatus = "", lastSlumberStatus = "";
int lobbyOverride = 0;
int tickets = 0, maxTickets = 0;
int waitTicks = 0;
int tpStickHold = 0;
int gambles = 0, startXP = 0;
int delayTicks = 0;
long lastAttack = client.time();
long lastTeleport = client.time();
long lastStatusChange = client.time();
long startTime = client.time(), endTime = client.time();
String lastGui = "";

void closeScreen() {
    if (!client.getScreen().isEmpty()) {
        client.closeScreen();
    }
}

void onEnable() {
    gamble = false;
}

void onPreUpdate() {
    Entity player = client.getPlayer();

    String gui = client.getScreen();
    if (!gui.equals(lastGui)) {
        guiUpdate(gui, !gui.isEmpty());
    }
    lastGui = gui;

    if (!gamble) return;

    if (!slumberStatus.equals(lastSlumberStatus)) {
        lastStatusChange = client.time();
    }
    lastSlumberStatus = slumberStatus;

    updateTickets();

    if (client.time() - lastStatusChange > 10000) {
        client.chat("/bedwars");
    }

    if (tickets < 75) {
        client.print(chatPrefix + "&eFinished gambling.");
        endTime = client.time();
        gamble = false;
        printResults();
        return;
    }

    switch (slumberStatus) {
        case "SWAPLOBBY":
            closeScreen();
            slumberStatus = "SWINGARM";
            break;
        case "SWINGARM":
            ItemStack item = player.getHeldItem();
            if (item == null || !item.name.equals("nether_star")) {
                inventory.setSlot(8);
                break;
            }

            attack(null, false);
            break;
        case "FINDTOURGUIDE":
            client.setForward(0);
            if (findTourGuide() != null) {
                inventory.setSlot(3);
                slumberStatus = "WALKTOURGUIDE";
            }
            break;
        case "WALKTOURGUIDE":
            closeScreen();
            Entity tourGuide = findTourGuide();
            if (tourGuide == null) {
                slumberStatus = "FINDTOURGUIDE";
                break;
            }
            client.setForward(1);
            double distance = player.getPosition().distanceTo(tourGuide.getPosition());
            boolean tpStickExists = inventory.getStackInSlot(5) != null && inventory.getStackInSlot(5).name.equals("blaze_rod");
            boolean holdingTpStick = tpStickExists && player.getHeldItem() != null && player.getHeldItem().name.equals("blaze_rod");
            boolean canSeeBlock = false;
            Vec3 tpOffset = holdingTpStick && distance > 3.5 ? new Vec3(1, 0, 1) : new Vec3(0, 1, 0);
            Vec3 offset = offset(tourGuide.getPosition(), tpOffset.x, tpOffset.y, tpOffset.z);

            rotateLegit(offset(tourGuide.getPosition(), tpOffset.x, tpOffset.y, tpOffset.z));

            Object[] block = client.raycastBlock(20);
            if (block != null) {
                Vec3 blockPosition = (Vec3)block[0];
                if (Math.abs(blockPosition.x - offset.x) < 2 && Math.abs(blockPosition.y - offset.y) < 2 && Math.abs(blockPosition.z - offset.z) < 2) {
                    canSeeBlock = true;
                }
            }

            if (tpStickExists && distance > 5 && (client.time() - lastTeleport > 1000)) {
                if (!holdingTpStick) {
                    inventory.setSlot(5);
                    break;
                } else if (holdingTpStick && canSeeBlock && ++tpStickHold > 2) {
                    keybinds.rightClick();
                    lastTeleport = client.time();
                }
            } else {
                tpStickHold = 0;
            }

            if (client.isFlying() && distance < 9) client.setFlying(false);
            if (distance > 3) {
                if (player.onGround()) client.jump();
                break;
            }

            client.setForward(0);
            inventory.setSlot(3);
            attack(tourGuide, true);
            break;
        case "TPGAMBLER":
            if (atTicketMachine()) {
                slumberStatus = "ATGAMBLER";
            }
            break;
        case "ATGAMBLER":
            if (!atTicketMachine()) {
                slumberStatus = "TPGAMBLER";
                break;
            }

            if (inventory.getSlot() != 3) {
                inventory.setSlot(3);
                break;
            }

            attack(null, false);
            break;
        case "ROLL":
            if (!client.getScreen().equals("GuiChest") || !inventory.getChest().equals("Ticket Machine")) {
                slumberStatus = "SWAPLOBBY";
                closeScreen();
                break;
            }

            for (int i = 0; i < inventory.getChestSize(); i++) {
                ItemStack chestItem = inventory.getStackInChestSlot(i);
                if (chestItem == null) continue;
                if (chestItem.displayName.equals(util.colorSymbol + "aClick to try your luck!")) {
                    inventory.click(i, 0, 0);
                    closeScreen();
                    slumberStatus = "SWAPLOBBY";
                }
            }

            break;
        case "CLICKGAMBLE":
            delayTicks = 0;
            if (!client.getScreen().equals("GuiChest") || !inventory.getChest().equals("Slumber Locations")) {
                slumberStatus = "FINDTOURGUIDE";
                closeScreen();
                break;
            }

            for (int i = 0; i < inventory.getChestSize(); i++) {
                ItemStack chestItem = inventory.getStackInChestSlot(i);
                if (chestItem == null) continue;
                if (chestItem.displayName.equals(util.colorSymbol + "6Teleport to the Ticket Machine")) {
                    inventory.click(i, 0, 0);
                    closeScreen();
                    slumberStatus = "TPGAMBLER";
                }
            }

            break;
        case "CLICKLOBBY":
            if (!client.getScreen().equals("GuiChest") || !inventory.getChest().equals("Bed Wars Lobby Selector")) {
                slumberStatus = "SWAPLOBBY";
                closeScreen();
                break;
            }

            int chestSize = inventory.getChestSize();

            for (int i = 0; i < chestSize; i++) {
                ItemStack chestItem = inventory.getStackInChestSlot(i);
                if (chestItem == null) continue;
                if (chestItem.displayName.startsWith(util.colorSymbol + "cBed Wars Lobby #")) {
                    int swap = i + ++lobbyOverride;
                    if (swap > chestSize - 1) swap = 0;
                    inventory.click(swap, 0, 0);
                    closeScreen();
                }
            }

            break;
        default:
            client.print("how did we get here? " + slumberStatus);
            break;
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (msg.equals("Ticket Machine now rolling...")) {
        gambles++;
    }
    return true;
}

void guiUpdate(String name, boolean opened) {
    if (!gamble || !opened || !name.equals("GuiChest")) return;

    switch (inventory.getChest()) {
        case "Bed Wars Lobby Selector":
            slumberStatus = "CLICKLOBBY";
            break;
        case "Slumber Locations":
            slumberStatus = "CLICKGAMBLE";
            break;
        case "Ticket Machine":
            slumberStatus = "ROLL";
            break;
        default:
            client.print("not right selector: " + inventory.getChest());
            break;
    }
}

void onWorldJoin(Entity en) {
    if (!gamble) return;
    if (en == client.getPlayer()) {
        slumberStatus = "FINDTOURGUIDE";
        lobbyOverride = 0;
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        if (!c01.message.startsWith("/as")) return true;
        String[] parts = c01.message.split(" ");
        if (parts.length <= 1) {
            client.print(chatPrefix + "&eAutoSlumber");
            client.print(chatPrefix + "&3/as gamble &e: Automatically gambles for you.");
            return false;
        }

        String command = parts[1].toLowerCase();

        switch (command) {
            case "gamble":
                if (!gamble) {
                    client.print(chatPrefix + "&eCommencing gambling.");
                    gamble = true;
                    slumberStatus = "SWAPLOBBY";
                    lastStatusChange = startTime = client.time();
                    gambles = 0;
                    client.async(() -> {
                        //startXP = getPlayerXP();
                    });
                } else {
                    gamble = false;
                    client.print(chatPrefix + "&cStopped gambing.");
                    slumberStatus = "";
                    endTime = client.time();
                    printResults();
                }
                break;
        }
        return false;
    }
    return true;
}

void onDisable() {
    if (gamble) {
        endTime = client.time();
        printResults();
    }
}

void printResults() {
    client.async(() -> {
        int endXP = /* getPlayerXP() */0;
        int experienceGained = endXP - startXP;
        double startStar = expToStars(startXP), endStar = expToStars(endXP);
        double starsGained = endStar - startStar;
        long timeElapsed = endTime - startTime + 1;
        double hours = timeElapsed / 1000d / 3600d;
        double seconds = timeElapsed / 1000d;
        client.sleep(7000);

        client.print(chatPrefix + "&eGambling Results:");
        client.print(chatPrefix + "&eTotal Stars Gained: &3" + formatDoubleStr(util.round(startStar, 2)) + " &e -> &3" + formatDoubleStr(util.round(endStar, 2)) + " &e(&d" + formatDoubleStr(util.round(starsGained, 2)) + "&e)");
        client.print(chatPrefix + "&eTotal Experience Gained: &3" + experienceGained);
        client.print(chatPrefix + "&eTotal Gambles: &3" + gambles);
        client.print(chatPrefix + "&eTickets Gambled: &3" + gambles * 75);

        client.print(chatPrefix + "&eStars/Hour: &3" + formatDoubleStr(util.round(starsGained / hours, 2)));
        client.print(chatPrefix + "&eExperience/Hour: &3" + formatDoubleStr(util.round(experienceGained / hours, 2)));
        client.print(chatPrefix + "&eRolls/Hour: &3" + formatDoubleStr(util.round(gambles / hours, 2)));
        client.print(chatPrefix + "&eSeconds/Roll: &3" + formatDoubleStr(util.round(seconds / gambles, 3)));
    });
}

/* int getPlayerXP() {
    return 0;
    final String uuid = client.getPlayer().getUUID().replace("-", "");
    try {
        Request request = new Request("GET", "" + uuid + "&cache=false");
        Response response = request.fetch();

        int code = response != null ? response.code() : 404;

        if (code != 200) {
            client.print(chatPrefix + "&eError &3" + code + "&e requesting stats.");
            return -1;
        }
        
        String dataString = response.string();
        Json jayson = response.json();
        Json data = jayson.object();
        Json playerObject = data.object("player");
        if (playerObject == null || dataString.equals("{\"success\":true,\"player\":null}")) {
            client.print(chatPrefix + "&eError: Player object is null");
            return -1;
        }

        Json bedwarsObject = playerObject.object("stats").object("Bedwars");
        int xp = Integer.parseInt(bedwarsObject.get("Experience", "0"));
        return xp;
    } catch (Exception e) {
        client.print(chatPrefix + "&eError in player function: &3" + e);
        return -1;
    }
} */

double expToStars(int exp) {
    int levelBase = (exp / 487000) * 100;
    int expMod = exp % 487000;
    int[][] levels = {
        {7000, 4, 5000},
        {3500, 3, 3500},
        {1500, 2, 2000},
        {500, 1, 1000},
        {0, 0, 500}
    };
    
    for (int[] lvl : levels) {
        if (expMod < lvl[0]) continue;

        double result = levelBase + lvl[1] + ((double) (expMod - lvl[0]) / lvl[2]);
        return result;
    }
    return 0;
}

String formatDoubleStr(double val) {
    String str;
    if (val % 1 == 0) {
        str = String.valueOf((int) val);
    } else {
        str = String.valueOf(val);
    }
    return str;
}

void attack(Entity en, boolean attack) {
    Entity player = client.getPlayer();

    int delay = 250;
    if (slumberStatus.equals("SWINGARM")) {
        delay = 500;
    }

    long now = client.time();

    if (now - lastAttack > delay) {
        if (attack) client.attack(en);
        else client.swing();
        lastAttack = now;
    }
}

boolean atTicketMachine() {
    Vec3 ticketMachinePositon = new Vec3(33.81, 69.5, 14.59);
    return client.getPlayer().getPosition().distanceToSq(ticketMachinePositon) < 9;
}

Entity findTourGuide() {
    Entity player = client.getPlayer();
    List<Entity> entities = world.getEntities();

    for (Entity en : entities) {
        if (!en.getDisplayName().equals(util.colorSymbol + "eSlumber Tour Guide")) continue;
        Vec3 tourGuidePos = en.getPosition();
        List<Entity> entities2 = world.getPlayerEntities();
        for (Entity pla : entities2) {
            Vec3 plaPos = pla.getPosition();
            if (plaPos.x != tourGuidePos.x) continue;
            if (plaPos.z != tourGuidePos.z) continue;
            return pla;
        }
    }

    return null;
}

void updateTickets() {
    List<String> scoreboard = world.getScoreboard();
    if (scoreboard == null) return;
    for (String line : scoreboard) {
        line = util.strip(line);
        if (!line.contains("Tickets: ")) continue;
        String[] parts = line.replaceAll("Tickets: ", "").split("/");
        tickets = Integer.parseInt(parts[0].replaceAll(",", ""));
        maxTickets = Integer.parseInt(parts[1].replaceAll(",", ""));
        break;
    }
}

void rotateLegit(Vec3 position) {
    Entity player = client.getPlayer();
    double currentYaw = player.getYaw();
    double currentPitch = player.getPitch();

    float[] rots = getRotations(position);
    float customYaw = rots[0];
    float customPitch = rots[1];

    double dYaw = deltaYaw(customYaw, currentYaw) * 1;
    double dPitch = (customPitch - currentPitch) * 1;

    double yawSpeed = 1000;
    double pitchSpeed = 1000;

    double maxDeltaYaw = yawSpeed + (yawSpeed * Math.random());
    double maxDeltaPitch = pitchSpeed + (pitchSpeed * Math.random());

    double changeYaw = clamp(dYaw, -maxDeltaYaw, maxDeltaYaw);
    double changePitch = clamp(dPitch, -maxDeltaPitch, maxDeltaPitch);

    float f = 0.15F * 0.6F + 0.2F;
    float gcd = f * f * f * 1.2F;
    changeYaw -= changeYaw % gcd;
    changePitch -= changePitch % gcd;

    double newYaw = currentYaw + changeYaw;
    double newPitch = currentPitch + changePitch;

    player.setYaw((float) newYaw);
    player.setPitch((float) newPitch);
}

double deltaYaw(double yaw1, double yaw2) {
    yaw1 = minecraftYawToStandard(yaw1);
    yaw2 = minecraftYawToStandard(yaw2);

    double dYaw = (yaw1 - yaw2) % 360;
    if (dYaw < -180) dYaw += 360;
    else if (dYaw > 180) dYaw -= 360;

    return dYaw;
}

double minecraftYawToStandard(double yaw) {
    return (yaw < 0) ? 360 + yaw : yaw;
}

double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(value, max));
}

Vec3 offset(Vec3 vec, double dx, double dy, double dz) {
    return new Vec3(vec.x + dx, vec.y + dy, vec.z + dz);
}

Vec3 subtract(Vec3 a, Vec3 b) {
    return new Vec3(a.x - b.x, a.y - b.y, a.z - b.z);
}

float[] getRotations(Vec3 point) {
    Entity player = client.getPlayer();
    Vec3 playerPosition = player.getPosition();
    
    Vec3 playerEyePosition = offset(playerPosition, 0, (double)player.getHeight(), 0);

    Vec3 delta = subtract(point, playerEyePosition);

    double x = delta.x;
    double y = delta.y;
    double z = delta.z;
    double dist = Math.sqrt(x * x + z * z);

    float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
    float pitch = (float) Math.toDegrees(-Math.atan2(y, dist));

    return new float[]{yaw, pitch};
}