/* 
    automatically stores/takes resources from your chest/enderchest in bedwars 
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/AutoChest.java"
*/

int status = -1, delay = 0, delayTicks = 0, inChestDelay = 1;
HashSet<String> items = new HashSet<>();
boolean automatic = false, foundItemsInInventory = false, foundItemsInChest = false, foundAllItemsInInventory = false, foundAllItemsInChest = false, doIron = false, doGold = false, doDiamonds = false, doEmeralds = false, stopWhenDone = true;

void onLoad() {
    modules.registerButton("Automatic", false);
    modules.registerButton("Stop When Finished", true);
    modules.registerSlider("Delay", "", 1, 0, 20, 1);
    modules.registerButton("Iron", true);
    modules.registerButton("Gold", true);
    modules.registerButton("Diamonds", true);
    modules.registerButton("Emeralds", true);

    modules.registerKey("Inventory To Chest", 0);
    modules.registerKey("Chest To Inventory", 0);
}

void onPreUpdate() {
    if (client.getPlayer().getTicksExisted() % 20 == 0) {
        status = getBedwarsStatus();
        delay = (int) modules.getSlider(scriptName, "Delay");
        automatic = modules.getButton(scriptName, "Automatic");
        stopWhenDone = modules.getButton(scriptName, "Stop When Finished");
        doIron = modules.getButton(scriptName, "Iron");
        doGold = modules.getButton(scriptName, "Gold");
        doDiamonds = modules.getButton(scriptName, "Diamonds");
        doEmeralds = modules.getButton(scriptName, "Emeralds");

        HashSet<String> tempitems = new HashSet<>();
        if (doIron) tempitems.add("iron_ingot");
        if (doGold) tempitems.add("gold_ingot");
        if (doDiamonds) tempitems.add("diamond");
        if (doEmeralds) tempitems.add("emerald");
        
        items = tempitems;
    }

    if (delayTicks > 0) delayTicks--;

    if (status == 3 && client.getScreen().equals("GuiChest") && inventory.getChest().endsWith("Chest")) {
        if (inChestDelay > 0 && --inChestDelay != 0) return;
        Map<Integer, ItemStack> inv = createCustomInventory();
        int chestSize = inventory.getChestSize();
        boolean clicked = false;

        if ((automatic && !foundItemsInChest && !foundAllItemsInInventory) || (modules.getKeyPressed(scriptName, "Inventory To Chest") && delayTicks == 0)) {
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

        if ((automatic && !foundItemsInInventory && !foundAllItemsInChest) || (modules.getKeyPressed(scriptName, "Chest To Inventory") && delayTicks == 0)) {
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
    } else {
        inChestDelay = 2;
        foundItemsInInventory = foundItemsInChest = foundAllItemsInChest = foundAllItemsInInventory = false;
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