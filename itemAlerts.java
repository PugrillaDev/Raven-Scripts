/* 
    Made by pug

    Steps to add a new item: 
    1. Add a display tag using the addDisplayColor method with the item name/displayName in the onLoad event.
    2. Register a button for the item using the registerButton method in the onLoad event.
    3. Add the item as a name/displayName/armorPiece using the addName/addDisplayName/addArmorPiece methods in the setupItems method.

    Tips:
    All variables are updated every 5 seconds because raven FPS is dogshit. Refer to this line: if (playerEntity.getTicksExisted() % 100 == 0).
    Use the debug option to print the name/displayName of a player's held item in the chat when attacking them.
    Check your latest.log file for specific color codes in the displayName.
    Display names are the names display for the item, names are the raw minecraft names; they are different.
    Items in your inventory may have different displayName values than other players. (ex: Dream Defenders vs Machine Gun Bow) 
    Don't be stupid, look at the code as a reference to add items.

    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/itemAlerts.java"
*/

Map<String, Map<String, Object>> playerItems = new HashMap<>();
HashSet<String> teamUpgrades = new HashSet<>();
Map<String, String> itemDisplayColors = new HashMap<>();
HashSet<String> armorPieceNames = new HashSet<>();
HashSet<String> heldItemNames = new HashSet<>();
HashSet<String> heldItemDisplayNames = new HashSet<>();
HashSet<String> checkedPlayers = new HashSet<>();
List<Map<String, Object>> alerts = new ArrayList<>();

boolean showDistance = true, showTeammates = false, debug = false, showSharpness = false, showProtection = false;
String myName, myTeam = "", chatPrefix = "&7[&dR&7]&d&r ";
String[] colorKeysArray = {"c", "9", "a", "e", "b", "f", "d", "8"};
int status = -1, DELAY_INTERVAL = 15000;

void onLoad() {
    addDisplayColor("chainmail_leggings", "&fChainmail Armor");
    addDisplayColor("iron_leggings", "&fIron Armor");
    addDisplayColor("diamond_leggings", "&bDiamond Armor");
    addDisplayColor("iron_sword", "&fIron Sword");
    addDisplayColor("diamond_sword", "&bDiamond Sword");
    addDisplayColor("diamond_pickaxe", "&bDiamond Pickaxe");
    addDisplayColor("ender_pearl", "&3Ender Pearl");
    addDisplayColor("egg", "&eBridge Egg");
    addDisplayColor("fire_charge", "&6Fireball");
    addDisplayColor("Bow", "&2Bow");
    addDisplayColor("obsidian", "&5Obsidian");
    addDisplayColor("tnt", "&cT&fN&cT");
    addDisplayColor("prismarine_shard", "&3Block Zapper");
    addDisplayColor("Speed II Potion (45 seconds)", "&bSpeed Potion");
    addDisplayColor("Jump V Potion (45 seconds)", "&aJump Boost Potion");
    addDisplayColor("Invisibility Potion (30 seconds)", "&fInvisibility Potion");
    addDisplayColor("&cDream Defender", "&fIron Golem");
    addDisplayColor("Machine Gun Bow", "&4Machine Gun Bow");
    addDisplayColor("Charlie the Unicorn", "&dCharlie the Unicorn");
    addDisplayColor("Ice Bridge", "&bIce Bridge");
    addDisplayColor("Sleeping Dust", "&cSleeping Dust");
    addDisplayColor("Unstable Teleportation Device", "&eUnstable Teleportation Device");
    addDisplayColor("Devastator Bow", "&2Devastator Bow");
    addDisplayColor("Miracle of the Stars", "&eMiracle of the Stars");
    addDisplayColor("Mystic Mirror", "&dMystic Mirror");

    modules.registerButton("Chat Alerts", true);
    modules.registerButton("HUD Alerts", true);
    modules.registerSlider("Hud Alerts Duration", "", 5, 0, 10, 0.1);

    modules.registerButton(util.color("&bSharpness"), true);
    modules.registerButton(util.color("&bProtection"), true);

    registerButton("chainmail_leggings");
    registerButton("iron_leggings");
    registerButton("diamond_leggings");
    registerButton("iron_sword");
    registerButton("diamond_sword");
    registerButton("diamond_pickaxe");
    registerButton("ender_pearl");
    registerButton("obsidian");
    registerButton("egg");
    registerButton("fire_charge");
    registerButton("tnt");
    registerButton("Bow");
    registerButton("prismarine_shard");
    registerButton("Ice Bridge");
    registerButton("Sleeping Dust");
    registerButton("Machine Gun Bow");
    registerButton("Charlie the Unicorn");
    registerButton("Unstable Teleportation Device");
    registerButton("Miracle of the Stars");
    registerButton("Mystic Mirror");
    registerButton("Devastator Bow");
    registerButton("Speed II Potion (45 seconds)");
    registerButton("Jump V Potion (45 seconds)");
    registerButton("Invisibility Potion (30 seconds)");
    registerButton("&cDream Defender");

    modules.registerButton("Show Teammates", false);
    modules.registerButton("Show Distance", true);
    modules.registerSlider("Delay (s)", "", 15, 0, 60, 1);
    modules.registerButton("Debug", false);

    setupItems();
}

void setupItems() {
    heldItemNames.clear();
    heldItemDisplayNames.clear();
    armorPieceNames.clear();

    addName("iron_sword");
    addName("diamond_sword");
    addName("diamond_pickaxe");
    addName("ender_pearl");
    addName("egg");
    addName("fire_charge");
    addName("tnt");
    addName("prismarine_shard");
    addName("obsidian");

    addDisplayName("Bow");
    addDisplayName("Speed II Potion (45 seconds)");
    addDisplayName("Jump V Potion (45 seconds)");
    addDisplayName("Invisibility Potion (30 seconds)");
    addDisplayName("&cDream Defender");
    addDisplayName("Machine Gun Bow");
    addDisplayName("Charlie the Unicorn");
    addDisplayName("Ice Bridge");
    addDisplayName("Sleeping Dust");
    addDisplayName("Unstable Teleportation Device");
    addDisplayName("Devastator Bow");
    addDisplayName("Miracle of the Stars");
    addDisplayName("Mystic Mirror");

    addArmorPiece("chainmail_leggings");
    addArmorPiece("iron_leggings");
    addArmorPiece("diamond_leggings");

    showDistance = modules.getButton(scriptName, "Show Distance");
    showTeammates = modules.getButton(scriptName, "Show Teammates");
    DELAY_INTERVAL = (int) modules.getSlider(scriptName, "Delay (s)") * 1000;
    debug = modules.getButton(scriptName, "Debug");
    showSharpness = modules.getButton(scriptName, util.color("&bSharpness"));
    showProtection = modules.getButton(scriptName, util.color("&bProtection"));
}

void onPreUpdate() {
    Entity playerEntity = client.getPlayer();
    if (playerEntity.getTicksExisted() % 100 == 0) {
        status = getBedwarsStatus();
        myName = playerEntity.getNetworkPlayer() == null ? playerEntity.getName() : playerEntity.getNetworkPlayer().getName();
        refreshTeams();
        setupItems();
    }

    if (!alerts.isEmpty() && !bridge.has("pugalert")) {
        bridge.add("pugalert", alerts.remove(0));
    }
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        playerItems.clear();
        teamUpgrades.clear();
        status = getBedwarsStatus();
    }
}

boolean onPacketReceived(SPacket packet) {
    if (packet instanceof S04) {
        S04 s04 = (S04) packet;
        ItemStack item = s04.item;
        int entityId = s04.entityId;
        int slot = s04.slot;
        Entity entity = world.getEntityById(entityId);
        doAlerts(entity, item, slot);
    }
    return true;
}

void doAlerts(Entity player, ItemStack item, int slot) {
    if (player == null || !player.type.contains("EntityOtherPlayerMP") || status != 3) return;
    Entity playerEntity = client.getPlayer();
    long now = client.time();
    if (player == playerEntity || player.isDead()) return;
    NetworkPlayer nwp = player.getNetworkPlayer();
    String playerDisplay = player.getDisplayName();
    if (nwp == null || (!showTeammates && playerDisplay.startsWith(util.colorSymbol + myTeam)) || playerDisplay.startsWith(util.colorSymbol + "7")) return;

    String uuid = nwp.getUUID();
    String teamColor = playerDisplay.substring(1, 2);
    String team = getColoredTeam(teamColor);
    if (team == null) return;
    String itemName = item == null ? "" : item.name;
    String itemDisplayName = item != null ? item.displayName : "";
    Map<String, Object> existingData = playerItems.getOrDefault(uuid, new HashMap<>());

    if (item != null) {
        boolean heldRaw = heldItemNames.contains(itemName);
        boolean heldDisplay = heldItemDisplayNames.contains(itemDisplayName);
        boolean isWeapon = itemName.endsWith("sword");
        boolean hasEnchantments = item.getEnchantments() != null;

        if (slot == 0 && (heldRaw || heldDisplay)) {
            String lastItem = existingData.getOrDefault("lastitem", "").toString();
            String trackedItemName = heldRaw ? itemName : itemDisplayName;
            long lastTime = Long.parseLong(existingData.getOrDefault(trackedItemName, "0").toString());

            if (now > lastTime && !lastItem.equals(itemName)) {
                String coloredName = util.colorSymbol + teamColor + player.getName();
                String displayColor = itemDisplayColors.get(trackedItemName);
                String msg = chatPrefix + "&eAlert: " + coloredName + " &7is holding&r " + displayColor + "&r&7";

                if (showDistance) msg += " &7(&d" + (int) playerEntity.getPosition().distanceTo(player.getPosition()) + "m&7)";
                String alertmsg = util.color(coloredName + " &7has " + displayColor + "&7.");

                if (modules.getButton(scriptName, "HUD Alerts")) {
                    addAlert(util.color("&lItem Alerts"), alertmsg, (int) (modules.getSlider(scriptName, "Hud Alerts Duration") * 1000), "");
                }
                if (modules.getButton(scriptName, "Chat Alerts")) client.print(msg);

                existingData.put(trackedItemName, now + DELAY_INTERVAL);
            }
        }

        if (hasEnchantments && (slot == 2 || (slot == 0 && isWeapon))) {
            String upgradeKey = slot == 2 ? "protection" + teamColor : "sharpness" + teamColor;
            String upgradeName = slot == 2 ? "Reinforced Armor" : "Sharpened Swords";
            String upgradeAlert = "&b" + upgradeName;

            if (!teamUpgrades.contains(upgradeKey)) {
                teamUpgrades.add(upgradeKey);
                String msg = chatPrefix + "&eAlert: " + team + " &7purchased " + upgradeAlert;
                String alertmsg = util.color(team + " &7has " + upgradeAlert + "&7.");

                if (modules.getButton(scriptName, "HUD Alerts")) {
                    addAlert(util.color("&lItem Alerts"), alertmsg, (int) (modules.getSlider(scriptName, "Hud Alerts Duration") * 1000), "");
                }
                if (modules.getButton(scriptName, "Chat Alerts")) client.print(msg);
            }
        }

        if (slot == 2 && armorPieceNames.contains(itemName)) {
            String existingArmor = existingData.getOrDefault("armorpiece", "").toString();
            if (!existingArmor.isEmpty() && !existingArmor.equals(itemName)) {
                String coloredName = util.colorSymbol + teamColor + player.getName();
                String armorDisplayColor = itemDisplayColors.get(itemName);
                String msg = chatPrefix + "&eAlert: " + coloredName + " &7purchased&r " + armorDisplayColor + "&r&7";

                if (showDistance) msg += " &7(&d" + (int) playerEntity.getPosition().distanceTo(player.getPosition()) + "m&7)";
                String alertmsg = util.color(coloredName + " &7has " + armorDisplayColor + "&7.");

                if (modules.getButton(scriptName, "HUD Alerts")) {
                    addAlert(util.color("&lItem Alerts"), alertmsg, (int) (modules.getSlider(scriptName, "Hud Alerts Duration") * 1000), "");
                }
                if (modules.getButton(scriptName, "Chat Alerts")) client.print(msg);
            }
            existingData.put("armorpiece", itemName);
        }
    }

    if (slot == 0) existingData.put("lastitem", itemName);
    playerItems.put(uuid, existingData);
}

boolean onPacketSent(CPacket packet) {
    if (!debug) return true;
    if (packet instanceof C02) {
        C02 c02 = (C02) packet;
        Entity en = (Entity)c02.entity;
        client.print(en.getName() + " " + en.getDisplayName() + " " + en.type + " " + en.getHeight());
        if (!c02.action.equals("ATTACK") || !en.type.equals("EntityOtherPlayerMP")) return true;
        String msg = chatPrefix + en.getDisplayName().substring(0, 2) + en.getName() + "&7: ";
        ItemStack item = en.getHeldItem();
        if (item == null) {
            msg += "&r'null' / 'null'";
        } else {
            msg += "&r'" + item.name + "&r' &7/ &r'" + item.displayName + "&r' " + item.stackSize;
        }
        client.print(msg);
    }
    return true;
}

void addAlert(String title, String message, int duration, String command) {
    Map<String, Object> alert = new HashMap<>(4);
    alert.put("title", title);
    alert.put("message", message);
    alert.put("duration", duration);
    alert.put("command", command);
    alerts.add(alert);
}

void addDisplayColor(String key, String value) {
    itemDisplayColors.put(util.color(key), util.color(value));
}

void registerButton(String key) {
    modules.registerButton(itemDisplayColors.get(util.color(key)), true);
}

void addName(String key) {
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        heldItemNames.add(key);
    }
}

void addArmorPiece(String key) {
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        armorPieceNames.add(key);
    }
}

void addDisplayName(String key) {
    key = util.color(key);
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        heldItemDisplayNames.add(key);
    }
}

String getColoredTeam(String colorCode) {
    switch (colorCode) {
        case "c":
            return util.color("&cRed Team&r");
        case "9":
            return util.color("&9Blue Team&r");
        case "a":
            return util.color("&aGreen Team&r");
        case "e":
            return util.color("&eYellow Team&r");
        case "b":
            return util.color("&bAqua Team&r");
        case "f":
            return util.color("&fWhite Team&r");
        case "d":
            return util.color("&dPink Team&r");
        case "8":
            return util.color("&8Gray Team&r");
        default:
            return null;
    }
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
    if (util.strip(sidebar.get(5)).startsWith("R Red:") && util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }
    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }
    return -1;
}

void refreshTeams() {
    if (status != 3 || client.allowFlying()) return;
    for (NetworkPlayer pla : world.getNetworkPlayers()) { for (int i = 0; i < colorKeysArray.length; i++) {
        if (!pla.getName().equals(myName) || !pla.getDisplayName().startsWith(util.colorSymbol + colorKeysArray[i])) continue;
        myTeam = colorKeysArray[i];
    }}
}