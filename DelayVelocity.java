List<Map<String, Object>> packets = new ArrayList<>();
boolean delaying, aiming, conditionals, teleported;

void onLoad() {
    modules.registerSlider("Delay", "ms", 400, 0, 1000, 50);
    modules.registerButton("Looking at player", false);
    modules.registerButton("Mouse down", false);
    modules.registerButton("Holding weapon", false);
}

void onEnable() {
    packets.clear();
    delaying = aiming = conditionals = teleported = false;
}

void onDisable() {
    flushAll();
}

boolean onPacketReceived(SPacket packet) {
    if (packet instanceof S08) {
        teleported = true;
    } else if (packet instanceof S12) {
        S12 s12 = (S12) packet;
        if (conditionals && s12.entityId == client.getPlayer().entityId) {
            delaying = true;
        }
    }
    if (!delaying) return true;
    Map<String, Object> entry = new HashMap<>();
    entry.put("packet", packet);
    entry.put("time", client.time());
    synchronized (packets) {
        packets.add(entry);
    }
    return false;
}

void onPostMotion() {
    conditionals = conditionals();
    if (packets.isEmpty()) return;

    if (teleported || !conditionals || !containsVelocity()) {
        flushAll();
    }

    long now = client.time();
    long delay = (long) modules.getSlider(scriptName, "Delay");

    while (!packets.isEmpty()) {
        long timestamp = (Long) packets.get(0).get("time");
        if (now - timestamp < delay) break;
        flushOne();
    }

    teleported = false;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C03) {
        C03 c03 = (C03) packet;
        if (c03.name.startsWith("C05") || c03.name.startsWith("C06")) {
            Object[] hit = client.raycastEntity(5, c03.yaw, c03.pitch);
            aiming = hit != null && "EntityOtherPlayerMP".equals(((Entity) hit[0]).type);
        }
    }
    return true;
}

boolean conditionals() {
    if (client.isFlying()) return false;
    Entity player = client.getPlayer();
    if (player.isCollidedHorizontally()) return false;
    if (player.onGround()) return false;
    if (modules.getButton(scriptName, "Looking at player") && !aiming) return false;
    if (modules.getButton(scriptName, "Mouse down") && !keybinds.isMouseDown(0)) return false;
    if (modules.getButton(scriptName, "Holding weapon") && !holdingWeapon()) return false;
    return true;
}

boolean holdingWeapon() {
    ItemStack held = client.getPlayer().getHeldItem();
    if (held == null) return false;
    String name = held.name;
    return name.endsWith("_sword") || name.endsWith("_axe") || name.equals("stick");
}

void flushOne() {
    synchronized (packets) {
        Map<String, Object> entry = packets.remove(0);
        client.processPacketNoEvent((SPacket) entry.get("packet"));
    }
}

void flushAll() {
    while (!packets.isEmpty()) {
        flushOne();
    }
    delaying = false;
}

boolean containsVelocity() {
    synchronized(packets) {
        int id = client.getPlayer().entityId;
        for (Map<String, Object> entry : packets) {
            SPacket p = (SPacket) entry.get("packet");
            if (p instanceof S12 && ((S12) p).entityId == id) return true;
        }
    }
    return false;
}