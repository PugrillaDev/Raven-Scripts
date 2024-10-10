/* Auto weapon swap macro for hypixel zombies */

Map<String, Map<String, Object>> weapons = new HashMap<>();
boolean reviving = false;

void onLoad() {
    modules.registerButton("Use Slot 1", true);
    modules.registerButton("Use Slot 2", true);
    modules.registerButton("Use Slot 3", true);
}

void onEnable() {
    client.print("&7[&dZ&7] &eZombies Helper: &aEnabled&e.");
}

void onDisable() {
    client.print("&7[&dZ&7] &eZombies Helper: &cDisabled&e.");
}

void onPreUpdate() {
    long now = client.time();
    refreshWeapons();

    if (!client.getScreen().isEmpty() || !keybinds.isMouseDown(1) || (inventory.getSlot() < 1 || inventory.getSlot() > 3)) return;

    Map<String, Object> bestWeaponStats = null;
    Map<String, Object> lowestFireRateWeaponStats = null;
    double highestDamage = 0.0;
    long lowestFireRateTime = Long.MAX_VALUE;
    int bestSlot = 1;
    int lowestFireRateSlot = 1;

    for (int slot = 1; slot <= 3; slot++) {
        if (!modules.getButton(scriptName, "Use Slot " + slot)) continue;
        ItemStack itemStack = inventory.getStackInSlot(slot);
        if (itemStack == null || itemStack.name.equals("dye")) continue;

        String itemName = util.strip(itemStack.displayName);
        if (!weapons.containsKey(itemName)) continue;

        Map<String, Object> weaponStats = weapons.get(itemName);
        double damage = (double) weaponStats.get("Damage");
        long fireRateDelay = Long.parseLong(weaponStats.getOrDefault("FireRateDelay", "0").toString());
        long reloadDelay = Long.parseLong(weaponStats.getOrDefault("ReloadDelay", "0").toString());
        int reloadTicks = Integer.parseInt(weaponStats.getOrDefault("ReloadTicks", "0").toString());
        int durability = itemStack.durability;
        int maxDurability = itemStack.maxDurability;
        int stackSize = itemStack.stackSize;

        if (durability <= 1 && durability != maxDurability && stackSize < 2 && (now > reloadDelay || reloadTicks == 1)) {
            if (inventory.getSlot() != slot) inventory.setSlot(slot);
            client.swing();
            double reloadTime = (double) weaponStats.get("Reload");
            weaponStats.put("ReloadDelay", now + (long) ((reloadTime * 1000) / 2));
            weaponStats.put("ReloadTicks", reloadTicks == 1 ? 0 : 1);
            return;
        }

        if (damage > highestDamage && now >= fireRateDelay && (durability == maxDurability || stackSize > 1)) {
            highestDamage = damage;
            bestWeaponStats = weaponStats;
            bestSlot = slot;
        }

        if (fireRateDelay < lowestFireRateTime && (durability == maxDurability || stackSize > 1)) {
            lowestFireRateTime = fireRateDelay;
            lowestFireRateWeaponStats = weaponStats;
            lowestFireRateSlot = slot;
        }
    }

    if (bestWeaponStats != null) {
        if (inventory.getSlot() != bestSlot) inventory.setSlot(bestSlot);
        client.sendPacketNoEvent(new C08(client.getPlayer().getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0.0, 0.0, 0.0)));

        double fireRate = (double) bestWeaponStats.get("Fire Rate");
        bestWeaponStats.put("FireRateDelay", now + (long) (fireRate * 1000));
    } else if (lowestFireRateWeaponStats != null) {
        if (inventory.getSlot() != lowestFireRateSlot) inventory.setSlot(lowestFireRateSlot);
        client.sendPacketNoEvent(new C08(client.getPlayer().getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0.0, 0.0, 0.0)));
    }
}

void onPostPlayerInput() {
    Entity player = client.getPlayer();
    Vec3 m = player.getPosition();
    double closest = Double.MAX_VALUE;
    Entity knockedPlayer = null;
    for (Entity e : client.getWorld().getEntities()) {
        String name = util.strip(e.getDisplayName());
        if (!name.contains("HOLD SNEAK TO REVIVE!") && !name.contains("REVIVING...")) continue;
        double dist = e.getPosition().distanceToSq(m);
        if (dist < closest) {
            closest = dist;
            knockedPlayer = e;
        }
    }

    double sqrtDist = Math.sqrt(closest);
    if (knockedPlayer == null || sqrtDist > 3.5) {
        if (reviving && player.isSneaking()) {
            keybinds.setPressed("sneak", false);
            reviving = false;
        }
        return;
    }

    if (sqrtDist < 3.5) {
        keybinds.setPressed("sneak", true);
        reviving = true;
    }
}

void refreshWeapons() {
    for (int slot = 1; slot <= 3; slot++) {
        ItemStack itemStack = inventory.getStackInSlot(slot);
        if (itemStack == null || itemStack.name.equals("dye")) continue;

        String itemName = util.strip(itemStack.displayName);
        if (weapons.containsKey(itemName)) continue;
        HashMap<String, Object> weaponStats = getWeaponStats(itemStack);
        if (weaponStats.size() == 0) continue;
        
        weapons.put(itemName, weaponStats);
    }
}

HashMap<String, Object> getWeaponStats(ItemStack itemStack) {
    List<String> tooltips = itemStack.getTooltip();
    HashMap<String, Object> weaponStats = new HashMap<>();

    for (String tip : tooltips) {
        String strippedTip = util.strip(tip);
        String[] parts = strippedTip.split(" ");
        if (strippedTip.contains("Damage:") && !strippedTip.contains("Bonus Damage")) {
            weaponStats.put("Damage", Double.parseDouble(parts[parts.length - 2]));
        } else if (strippedTip.contains("Clip Ammo:")) {
            weaponStats.put("Clip Ammo", Double.parseDouble(parts[parts.length - 1]));
        } else if (strippedTip.contains("Ammo:")) {
            weaponStats.put("Ammo", Double.parseDouble(parts[parts.length - 1]));
        } else if (strippedTip.contains("Fire Rate:")) {
            weaponStats.put("Fire Rate", Double.parseDouble(parts[parts.length - 1].replace("s", "")));
        } else if (strippedTip.contains("Reload:")) {
            weaponStats.put("Reload", Double.parseDouble(parts[parts.length - 1].replace("s", "")));
        }
    }

    return weaponStats;
}