/* 
    allows you to buy items from the bedwars quickbuy/diamond upgrades with keybinds
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/Quickbuy.java"
*/

void setupItems() {
    addItem("wool"); // Wool
    addItem("golden_apple"); // Gapple
    addItem("upg iron_sword"); // Sharpness Upgrade
    addItem("upg iron_chestplate"); // Protection Upgrade
    addItem("upg iron_pickaxe"); // Mining Fatigue Trap
    addItem("fire_charge"); // Fireball
}

HashSet<List<Object>> kbs = new HashSet<>();
Map<String, List<Object>> quickKeys = new HashMap<>();
Map<String, Integer> locations = new HashMap<>();
List<String> items = new ArrayList<>();
List<Integer[]> clickList = new ArrayList<>();
boolean sentinvPacket = false;
Map<String, Long> upgrades = new HashMap<>();

String[] keyNames = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    "BACK", "CAPITAL", "COMMA", "DELETE", "DOWN", "END", "ESCAPE", "F1", "F2", "F3", "F4", "F5",
    "F6", "F7", "HOME", "INSERT", "LBRACKET", "LCONTROL", "LMENU", "LMETA", "LSHIFT", "MINUS",
    "NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5", "NUMPAD6", "NUMPAD7",
    "NUMPAD8", "NUMPAD9", "PERIOD", "RETURN", "RCONTROL", "RSHIFT", "RBRACKET", "SEMICOLON",
    "SLASH", "SPACE", "TAB", "GRAVE"
};
String[] slotNames = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
HashSet<String> pickaxeTypes = new HashSet<>(Arrays.asList("wooden_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe"));
HashSet<String> axeTypes = new HashSet<>(Arrays.asList("wooden_axe", "stone_axe", "iron_axe", "diamond_axe"));

void addItem(String item) {
    items.add(item);
}

void onLoad() {
    setupItems();
    for (String item : items) {
        kbs.add(new ArrayList<>(Arrays.asList(false, item)));
    }

    List<String> itemNames = new ArrayList<>();
    for (List<Object> value : kbs) {
        itemNames.add(value.get(1).toString());
    }

    for (String itemName : itemNames) {
        if (itemName.startsWith("upg ")) {
            modules.registerSlider(itemName + " keybind", "", 16, keyNames);
        } else {
            modules.registerSlider(itemName + " keybind", "", 16, keyNames);
            modules.registerSlider(itemName + " quickslot", "", 0, slotNames);
            modules.registerButton(itemName + " turbo", false);
        }
    }

    refreshkbs();
}

void onGuiUpdate(String name, boolean opened) {
    if (!opened) {
        clickList.clear();
        return;
    }

    if (!name.equals("GuiChest")) return;

    refreshkbs();
    Map<Integer, ItemStack> inv = createCustomInventory();
    int chestSize = inventory.getChestSize();
    boolean isQuickBuy = inventory.getChest().equals("Quick Buy");
    boolean isUpgrades = inventory.getChest().equals("Upgrades & Traps");

    if (!isQuickBuy && !isUpgrades) return;

    for (List<Object> value : kbs) {
        String itemName = value.get(1).toString();
        String fullItemName = itemName;

        if (isQuickBuy && itemName.startsWith("upg ")) continue;
        if (isUpgrades) {
            if (!itemName.startsWith("upg ")) continue;
            itemName = itemName.substring("upg ".length());
        }

        HashSet<String> itemTypes = null;
        if (isQuickBuy) {
            itemTypes = itemName.equals("pickaxe") ? pickaxeTypes :
                        itemName.equals("axe") ? axeTypes : null;
        }

        int start = isQuickBuy ? 18 : 9;
        int end = isQuickBuy ? chestSize - 9 : 27;

        for (int i = start; i < end; i++) {
            ItemStack item = inv.get(i);
            if (item == null || (itemTypes != null && !itemTypes.contains(item.name)) || 
                (itemTypes == null && !item.name.equals(itemName))) continue;
            locations.put(fullItemName, i);
            break;
        }
    }
}

void onPostPlayerInput() {
    String chestName = inventory.getChest();
    if (!client.getScreen().equals("GuiChest") || (!chestName.equals("Quick Buy") && !chestName.equals("Upgrades & Traps"))) return;
    
    boolean isQuickBuy = chestName.equals("Quick Buy");
    boolean isUpgrades = chestName.equals("Upgrades & Traps");

    if (!isQuickBuy && !isUpgrades) return;

    int ticksExisted = client.getPlayer().getTicksExisted();
    long currentTime = client.time();

    for (List<Object> value : kbs) {
        String item = value.get(1).toString();
        if (isQuickBuy && item.startsWith("upg ")) continue;
        else if (isUpgrades && !item.startsWith("upg ")) continue;
        List<Object> keyAndSlot = quickKeys.get(item);
        int key = (int) keyAndSlot.get(0);
        boolean turbo = (Boolean) keyAndSlot.get(2);

        boolean down = keybinds.isKeyDown(key);
        boolean lastDown = (Boolean) value.get(0);

        if (down && ((turbo && ticksExisted % 2 == 0) || !lastDown)) {
            int hotbarSlot = (int) keyAndSlot.get(1);

            if (isUpgrades) {
                Long lastUpgradeTime = upgrades.get(item);
                if (lastUpgradeTime == null || (currentTime - lastUpgradeTime) > 300) {
                    clickSpecifiedItem(item, hotbarSlot);
                    upgrades.put(item, currentTime);
                }
            } else {
                clickSpecifiedItem(item, hotbarSlot);
            }
        }

        if (down != lastDown) {
            value.set(0, down);
        }
    }

    if (!sentinvPacket && !clickList.isEmpty()) {
        Integer[] click = clickList.remove(0);
        int slot = click[0];
        int hotbarSlot = click[1];

        if (hotbarSlot >= 0) {
            inventory.click(slot, hotbarSlot, 2);
        } else {
            inventory.click(slot, 0, 0);
        }
    }
}

void clickSpecifiedItem(String itemName, int hotbarSlot) {
    Integer slot = locations.get(itemName);
    if (slot != null) {
        clickList.add(new Integer[]{slot, hotbarSlot});
    }
}

void refreshkbs() {
    quickKeys.clear();
    for (List<Object> value : kbs) {
        String itemName = value.get(1).toString();

        Integer key = keybinds.getKeyCode(keyNames[(int) modules.getSlider(scriptName, itemName + " keybind")]);
        Integer slot = (itemName.startsWith("upg ")) ? -1 : (int) modules.getSlider(scriptName, itemName + " quickslot") - 1;
        boolean turbo = itemName.startsWith("upg ") ? false : modules.getButton(scriptName, itemName + " turbo");

        quickKeys.put(itemName, new ArrayList<>(Arrays.asList(key, slot, turbo)));
    }
}

Map<Integer, ItemStack> createCustomInventory() {
    Map<Integer, ItemStack> inv = new HashMap<>();
    String screen = client.getScreen();
    int inventorySize = inventory.getSize() - 4, slot = 0;

    if (screen.equals("GuiInventory")) {
        for (int i = 0; i < 5; i++) {
            inv.put(slot++, null);
        }

        Entity player = client.getPlayer();
        for (int i = 3; i >= 0; i--) {
            inv.put(slot++, player.getArmorInSlot(i));
        }
    } else if (screen.equals("GuiChest") && !inventory.getChest().isEmpty()) {
        int chestSize = inventory.getChestSize();
        for (int i = 0; i < chestSize; i++) {
            inv.put(slot++, inventory.getStackInChestSlot(i));
        }
    }

    for (int i = 9; i < inventorySize + 9; i++) {
        inv.put(slot++, inventory.getStackInSlot(i % inventorySize));
    }

    return inv;
}

void onPostMotion() {
    sentinvPacket = false;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C0E) {
        sentinvPacket = true;
    }
    return true;
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        locations.clear();
    }
}