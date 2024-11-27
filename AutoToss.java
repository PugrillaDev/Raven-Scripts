/* 
    automatically tosses bedwars resources when enabled
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/AutoToss.java"
*/

int open = 0;
int delayTicks = 0;
int delay = 0;

void onLoad() {
    modules.registerSlider("Delay", " ticks", 0, 0, 10, 1);

    modules.registerButton("Iron", true);
    modules.registerButton("Gold", true);
    modules.registerButton("Diamonds", true);
    modules.registerButton("Emeralds", true);
}

void onEnable() {
    open = 0;
    delayTicks = 0;
    delay = (int) modules.getSlider(scriptName, "Delay");
    inventory.open();
}

void onPreUpdate() {
    if (open++ >= 3) modules.disable(scriptName);
    if (!client.getScreen().equals("GuiInventory")) return;
    open = 0;
    if (--delayTicks > 0) return;

    Map<Integer, ItemStack> inv = createCustomInventory();
    HashSet<String> tempitems = new HashSet<>();
    if (modules.getButton(scriptName, "Iron")) tempitems.add("iron_ingot");
    if (modules.getButton(scriptName, "Gold")) tempitems.add("gold_ingot");
    if (modules.getButton(scriptName, "Diamonds")) tempitems.add("diamond");
    if (modules.getButton(scriptName, "Emeralds")) tempitems.add("emerald");

    for (Map.Entry<Integer, ItemStack> entry : inv.entrySet()) {
        ItemStack item = entry.getValue();
        if (item == null || !tempitems.contains(item.name)) continue;

        inventory.click(entry.getKey(), 1, 4);
        delayTicks = delay;
        if (delayTicks > 0) return;
    }

    modules.disable(scriptName);
    client.closeScreen();
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