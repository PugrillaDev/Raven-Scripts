Set<String> blocks = new HashSet<>(Arrays.asList("wool", "planks", "log", "end_stone", "hardened_clay", "glass", "stained_hardened_clay", "stained_glass", "obsidian", "ladder", "sponge"));
int lastSwapTick = -20;
int lastSwapSlot = -1;
int lastPlaceSlot = -1;
ItemStack lastBlock;
boolean placing;

void onPreUpdate() {
    Entity player = client.getPlayer();
    int tick = player.getTicksExisted();
    int currSlot = inventory.getSlot();
    ItemStack held = player.getHeldItem();

    if (!placing || held != null || currSlot != lastPlaceSlot || lastBlock == null || !blocks.contains(lastBlock.name)) return;

    for (int slot = 8; slot >= 0; --slot) {
        if (slot == lastSwapSlot && tick - lastSwapTick <= 5) continue;
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack != null && stack.name.equals(lastBlock.name)) {
            inventory.setSlot(slot);
            lastSwapTick = tick;
            lastSwapSlot = slot;
            break;
        }
    }
}

void onPostMotion() {
    placing = keybinds.isMouseDown(1);
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C08) {
        C08 c08 = (C08) packet;
        if (c08.direction != 255 && c08.itemStack != null) {
            placing = true;
            lastBlock = c08.itemStack;
            lastPlaceSlot = inventory.getSlot();
        }
    }
    return true;
}