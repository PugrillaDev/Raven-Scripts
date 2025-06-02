long firstDropTime = -1;

void onLoad() {
    modules.registerSlider("Double Tap Delay", "ms", 350, 0, 500, 10);

    modules.registerButton("Wooden Sword", true);
    modules.registerButton("Stone Sword", true);
    modules.registerButton("Iron Sword", true);
    modules.registerButton("Golden Sword", true);
    modules.registerButton("Diamond Sword", true);
}

boolean onPacketSent(CPacket packet) {
    if (!(packet instanceof C07)) return true;

    C07 c07 = (C07) packet;
    if (!c07.status.startsWith("DROP_")) return true;

    ItemStack item = client.getPlayer().getHeldItem();
    if (item == null) return true;

    String type = item.name;

    if (!type.endsWith("_sword") || !shouldBlock(type)) return true;

    long now = client.time();
    int delay = (int) modules.getSlider(scriptName, "Double Tap Delay");

    if (firstDropTime == -1 || now - firstDropTime > delay) {
        firstDropTime = now;
        return false;
    }

    firstDropTime = -1;
    return true;
}

boolean shouldBlock(String type) {
    switch (type) {
        case "wooden_sword":
            return modules.getButton(scriptName, "Wooden Sword");
        case "stone_sword":
            return modules.getButton(scriptName, "Stone Sword");
        case "iron_sword":
            return modules.getButton(scriptName, "Iron Sword");
        case "golden_sword":
            return modules.getButton(scriptName, "Golden Sword");
        case "diamond_sword":
            return modules.getButton(scriptName, "Diamond Sword");
        default:
            return false;
    }
}
