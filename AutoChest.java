/* 
    automatically stores/takes resources from your chest/enderchest in bedwars 
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/AutoChest.java"
*/

int status = -1, delay = 0, delayTicks = 0, inChestDelay = 1;
HashSet<String> items = new HashSet<>();
boolean automatic = false, foundItemsInInventory = false, foundItemsInChest = false, foundAllItemsInInventory = false, foundAllItemsInChest = false, doIron = false, doGold = false, doDiamonds = false, doEmeralds = false, stopWhenDone = true;

String[] keyNames = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    "BACK", "CAPITAL", "COMMA", "DELETE", "DOWN", "END", "ESCAPE", "F1", "F2", "F3", "F4", "F5",
    "F6", "F7", "HOME", "INSERT", "LBRACKET", "LCONTROL", "LMENU", "LMETA", "LSHIFT", "MINUS",
    "NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5", "NUMPAD6", "NUMPAD7",
    "NUMPAD8", "NUMPAD9", "PERIOD", "RETURN", "RCONTROL", "RSHIFT", "RBRACKET", "SEMICOLON",
    "SLASH", "SPACE", "TAB", "GRAVE",
};

void onLoad() {
    modules.registerButton("Automatic", false);
    modules.registerButton("Stop When Finished", true);
    modules.registerSlider("Delay", "", 1, 0, 20, 1);
    modules.registerButton("Iron", true);
    modules.registerButton("Gold", true);
    modules.registerButton("Diamonds", true);
    modules.registerButton("Emeralds", true);

    modules.registerSlider("Inventory To Chest", "", 47, keyNames);
    modules.registerSlider("Chest To Inventory", "", 34, keyNames);
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

        if ((automatic && !foundItemsInChest && !foundAllItemsInInventory) || (keybinds.isKeyDown(getKeyCode(keyNames[(int) modules.getSlider(scriptName, "Inventory To Chest")])) && delayTicks == 0)) {
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

        if ((automatic && !foundItemsInInventory && !foundAllItemsInChest) || (keybinds.isKeyDown(getKeyCode(keyNames[(int) modules.getSlider(scriptName, "Chest To Inventory")])) && delayTicks == 0)) {
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
    List<String> sidebar = client.getWorld().getScoreboard();
    if (sidebar == null) {
        if (client.getWorld().getDimension().equals("The End")) {
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

int getKeyCode(String keyName) {
    switch (keyName) {
        case "0": return 11;
        case "1": return 2;
        case "2": return 3;
        case "3": return 4;
        case "4": return 5;
        case "5": return 6;
        case "6": return 7;
        case "7": return 8;
        case "8": return 9;
        case "9": return 10;
        case "A": return 30;
        case "B": return 48;
        case "C": return 46;
        case "D": return 32;
        case "E": return 18;
        case "F": return 33;
        case "G": return 34;
        case "H": return 35;
        case "I": return 23;
        case "J": return 36;
        case "K": return 37;
        case "L": return 38;
        case "M": return 50;
        case "N": return 49;
        case "O": return 24;
        case "P": return 25;
        case "Q": return 16;
        case "R": return 19;
        case "S": return 31;
        case "T": return 20;
        case "U": return 22;
        case "V": return 47;
        case "W": return 17;
        case "X": return 45;
        case "Y": return 21;
        case "Z": return 44;
        case "BACK": return 14;
        case "CAPITAL": return 58;
        case "COMMA": return 51;
        case "DELETE": return 211;
        case "DOWN": return 208;
        case "END": return 207;
        case "ESCAPE": return 1;
        case "F1": return 59;
        case "F2": return 60;
        case "F3": return 61;
        case "F4": return 62;
        case "F5": return 63;
        case "F6": return 64;
        case "F7": return 65;
        case "HOME": return 199;
        case "INSERT": return 210;
        case "LBRACKET": return 26;
        case "LCONTROL": return 29;
        case "LMENU": return 56;
        case "LMETA": return 219;
        case "LSHIFT": return 42;
        case "MINUS": return 12;
        case "NUMPAD0": return 82;
        case "NUMPAD1": return 79;
        case "NUMPAD2": return 80;
        case "NUMPAD3": return 81;
        case "NUMPAD4": return 75;
        case "NUMPAD5": return 76;
        case "NUMPAD6": return 77;
        case "NUMPAD7": return 71;
        case "NUMPAD8": return 72;
        case "NUMPAD9": return 73;
        case "PERIOD": return 52;
        case "RETURN": return 28;
        case "RCONTROL": return 157;
        case "RSHIFT": return 54;
        case "RBRACKET": return 27;
        case "SEMICOLON": return 39;
        case "SLASH": return 53;
        case "SPACE": return 57;
        case "TAB": return 15;
        case "GRAVE": return 41;
        default: return -1;
    }
}