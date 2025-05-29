Set<String> blocks = new HashSet<>(Arrays.asList("wool", "planks", "log", "end_stone", "hardened_clay", "glass", "stained_hardened_clay", "stained_glass", "obsidian", "ladder", "sponge"));
int lastSwapSlot = -1;
int lastPlaceSlot = -1;
ItemStack lastBlock;
boolean placing;
long lastSwap;

void onPreUpdate() {
    if (!placing || inventory.getSlot() != lastPlaceSlot || lastBlock == null || !blocks.contains(lastBlock.name) || client.getPlayer().getHeldItem() != null) return;

    long now = client.time();

    for (int slot = 8; slot >= 0; --slot) {
        if (slot == lastSwapSlot && now - lastSwap < 300) {
            continue;
        }
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack != null && stack.name.equals(lastBlock.name)) {
            inventory.setSlot(slot);
            lastSwap = now;
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