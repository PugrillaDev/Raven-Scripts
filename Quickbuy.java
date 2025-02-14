/* 
    Allows you to buy items from the Bedwars quickbuy/diamond upgrades with keybinds
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/Quickbuy.java"
*/

Map<String, String> itemDisplayNames = new HashMap<>();
Map<String, Integer> locations = new HashMap<>();
Map<String, Long> purchases = new HashMap<>();
Map<String, Boolean> keyStates = new HashMap<>();
List<String> items = new ArrayList<>();
List<Integer[]> clickList = new ArrayList<>();
boolean sentinvPacket = false;

String[] slotNames = {util.color("&cDisabled"), "1", "2", "3", "4", "5", "6", "7", "8", "9"};
HashSet<String> pickaxeTypes = new HashSet<>(Arrays.asList("wooden_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe"));
HashSet<String> axeTypes = new HashSet<>(Arrays.asList("wooden_axe", "stone_axe", "iron_axe", "diamond_axe"));

void setupItems() {
    registerItem("wool", "Wool");
    registerItem("stone_sword", "Stone Sword");
    registerItem("iron_sword", "Iron Sword");
    registerItem("golden_apple", "Golden Apple");
    registerItem("fire_charge", "Fireball");
    registerItem("tnt", "TNT");
    registerItem("ender_pearl", "Ender Pearl");

    registerItem("pickaxe", "Pickaxe");
    registerItem("axe", "Axe");
    registerItem("shears", "Shears");
    registerItem("chainmail_boots", "Chainmail Armor");
    registerItem("iron_boots", "Iron Armor");

    registerItem("upg iron_sword", "Sharpness");
    registerItem("upg iron_chestplate", "Protection");
    registerItem("upg iron_pickaxe", "Mining Fatigue");
    registerItem("upg golden_pickaxe", "Haste");
    registerItem("upg diamond_boots", "Feather Falling");

    registerItem("diamond_sword", "Diamond Sword");
    registerItem("stick", "Knockback Stick");
    registerItem("arrow", "Arrows");
    registerItem("diamond_boots", "Diamond Armor");
}

void registerItem(String item, String displayName) {
    itemDisplayNames.put(item, displayName);
    items.add(item);
}

void onLoad() {
    setupItems();
    for (String item : items) {
        String displayName = itemDisplayNames.get(item);
        modules.registerGroup(displayName);
        if (!item.startsWith("upg ")) {
            modules.registerSlider(displayName, displayName + " Quickslot", "", 0, slotNames);
            modules.registerButton(displayName, displayName + " Turbo", false);
        }
        modules.registerKey(displayName, displayName + " Keybind", 0);
    }
}

void onPreUpdate() {
    if (!client.getScreen().equals("GuiChest")) return;
    locations.clear();
    
    Map<Integer, ItemStack> inv = createCustomInventory();
    int chestSize = inventory.getChestSize();
    boolean isQuickBuy = inventory.getChest().equals("Quick Buy");
    boolean isUpgrades = inventory.getChest().equals("Upgrades & Traps");

    if (!isQuickBuy && !isUpgrades) return;

    for (String itemName : items) {
        String fullItemName = itemName;
        if (isQuickBuy && itemName.startsWith("upg ")) continue;
        if (isUpgrades) {
            if (!itemName.startsWith("upg ")) continue;
            itemName = itemName.substring(4);
        }

        HashSet<String> itemTypes = isQuickBuy
            ? itemName.equals("pickaxe") ? pickaxeTypes
            : itemName.equals("axe") ? axeTypes : null
            : null;

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
    if (!client.getScreen().equals("GuiChest")) return;
    String chestName = inventory.getChest();
    boolean isQuickBuy = chestName.equals("Quick Buy");
    boolean isUpgrades = chestName.equals("Upgrades & Traps");
    if (!isQuickBuy && !isUpgrades) return;

    long now = client.time();
    for (String item : items) {
        if (isQuickBuy && item.startsWith("upg ")) continue;
        if (isUpgrades && !item.startsWith("upg ")) continue;

        String displayName = itemDisplayNames.get(item);
        boolean keyDown = modules.getKeyPressed(scriptName, displayName + " Keybind");
        boolean lastKeyState = keyStates.getOrDefault(item, false);

        if (!keyDown) {
            keyStates.put(item, false);
            continue;
        }

        int hotbarSlot = (int) modules.getSlider(scriptName, displayName + " Quickslot") - 1;
        boolean turbo = !item.startsWith("upg ") && modules.getButton(scriptName, displayName + " Turbo");
        long cooldown = item.startsWith("upg ") ? 300 : 90;
        long lastTime = purchases.getOrDefault(item, 0L);

        if (!turbo && lastKeyState) continue;
        if (now - lastTime < cooldown) continue;

        purchases.put(item, now);
        keyStates.put(item, true);
        clickSpecifiedItem(item, hotbarSlot);
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

Map<Integer, ItemStack> createCustomInventory() {
    Map<Integer, ItemStack> inv = new HashMap<>();
    String screen = client.getScreen();
    int inventorySize = inventory.getSize() - 4, slot = 0;

    if (screen.equals("GuiInventory")) {
        for (int i = 0; i < 5; i++) inv.put(slot++, null);
        Entity player = client.getPlayer();
        for (int i = 3; i >= 0; i--) inv.put(slot++, player.getArmorInSlot(i));
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