/* 
    automatically stores/takes resources from your chest/enderchest in bedwars 
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/AutoChest.java"
*/

int delayTicks, inChestDelay;
boolean foundAllItemsInInventory, foundAllItemsInChest, foundItemsInChest, foundItemsInInventory;

void onLoad() {
    modules.registerButton("Automatic", false);
    modules.registerButton("Stop When Finished", true);
    modules.registerSlider("Delay", "ms", 50, 0, 1000, 50);
    modules.registerButton("Iron", true);
    modules.registerButton("Gold", true);
    modules.registerButton("Diamonds", true);
    modules.registerButton("Emeralds", true);

    modules.registerKey("Inventory To Chest", 0);
    modules.registerKey("Chest To Inventory", 0);
}

void onPreUpdate() {
    if (delayTicks > 0) delayTicks--;

    if (!client.getScreen().equals("GuiChest") || !inventory.getChest().endsWith("Chest") || getBedwarsStatus() != 3) {
        inChestDelay = 2;
        foundItemsInInventory = foundItemsInChest = foundAllItemsInChest = foundAllItemsInInventory = false;
        return;
    }

    if (inChestDelay > 0 && --inChestDelay != 0) return;

    boolean automatic = modules.getButton(scriptName, "Automatic");
    boolean stopWhenDone = modules.getButton(scriptName, "Stop When Finished");
    int delay = (int) (modules.getSlider(scriptName, "Delay") / 50);

    HashSet<String> items = new HashSet<>();
    if (modules.getButton(scriptName, "Iron")) items.add("iron_ingot");
    if (modules.getButton(scriptName, "Gold")) items.add("gold_ingot");
    if (modules.getButton(scriptName, "Diamonds")) items.add("diamond");
    if (modules.getButton(scriptName, "Emeralds")) items.add("emerald");

    Map<Integer, ItemStack> inv = createCustomInventory();
    int chestSize = inventory.getChestSize();
    boolean clicked = false;

    boolean invToChestKey = modules.getKeyPressed(scriptName, "Inventory To Chest");
    boolean chestToInvKey = modules.getKeyPressed(scriptName, "Chest To Inventory");

    if ((automatic && !foundItemsInChest && !foundAllItemsInInventory) || (invToChestKey && delayTicks == 0)) {
        for (int i = chestSize; i < inv.size(); i++) {
            ItemStack item = inv.get(i);
            if (item == null || !items.contains(item.name)) continue;
            inventory.click(i, 0, 1);
            clicked = true;
            delayTicks = delay;
            foundItemsInInventory = true;
            if (delay > 0) break;
        }

        if (stopWhenDone && !clicked && foundItemsInInventory) {
            foundAllItemsInInventory = true;
        }
    }

    clicked = false;

    if ((automatic && !foundItemsInInventory && !foundAllItemsInChest) || (chestToInvKey && delayTicks == 0)) {
        for (int i = 0; i < chestSize; i++) {
            ItemStack item = inv.get(i);
            if (item == null || !items.contains(item.name)) continue;
            inventory.click(i, 1, 1);
            clicked = true;
            delayTicks = delay;
            foundItemsInChest = true;
            if (delay > 0) break;
        }

        if (stopWhenDone && !clicked && foundItemsInChest) {
            foundAllItemsInChest = true;
        }
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
    }

    if (screen.equals("GuiChest") && !inventory.getChest().isEmpty()) {
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
    if (lobbyId.endsWith("]")) {
        lobbyId = lobbyId.split(" ")[0];
    }

    if (lobbyId.startsWith("L")) {
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