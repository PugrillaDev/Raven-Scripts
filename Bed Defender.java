int i;
boolean started;
boolean sneaked;
boolean onlyTopBeds;
boolean pendingPlace;
String lockedDirection = "";
String face;
Vec3 origin = null;
Vec3 hitPos;
Vec3 placePos;
Vec3 renderTarget;
float intervalYaw;
float intervalPitch;
float serverYaw;
float serverPitch;
int delayAfterSwap;
int delayAfterAim;
int swapTicks;
int aimTicks;
int sneakHoldTicks;
int sneakHoldTicksRemaining;
Map<String, Integer> inventoryCache = new HashMap<>();
List<Object[]> defense;
List<Object[]>[] defenses;
String[] defenseNames;

@SuppressWarnings("unchecked")
void onLoad() {
    String defsJson = config.get("bed_defenses");
    Json allDefs = null;
    if (defsJson != null) {
        try {
            Json parsed = Json.parse(defsJson);
            if (parsed.type() == Json.Type.OBJECT) {
                allDefs = parsed;
            }
        } catch (Exception ignored) {}
    }
    if (allDefs == null) {
        allDefs = Json.object();
    }

    if (allDefs.keys().isEmpty()) {
        String url = "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/bed_defenses.json";
        try {
            Request r = new Request("GET", url);
            r.setConnectTimeout(3000);
            r.setReadTimeout(5000);
            Response resp = r.fetch();
            if (resp.code() == 200) {
                Json defaults = resp.json();
                if (defaults.type() == Json.Type.OBJECT) {
                    allDefs = defaults;
                    config.set("bed_defenses", allDefs.toString());
                }
            }
        } catch (Exception ignored) {}
    }

    if (allDefs.keys().isEmpty()) {
        defenseNames = new String[]{ util.color("&cNo Defenses Found") };
        defenses = new List[0];
        modules.registerDescription("You may need to enable");
        modules.registerDescription("HTTP requests in order");
        modules.registerDescription("to see default defenses");
    } else {
        Set<String> namesSet = allDefs.keys();
        defenseNames = namesSet.toArray(new String[0]);
        List<Object[]>[] loadedDefs = new List[defenseNames.length];
        for (int i = 0; i < defenseNames.length; i++) {
            Json arr = allDefs.get(defenseNames[i]);
            List<Object[]> list = new ArrayList<>();
            if (arr != null && arr.type() == Json.Type.ARRAY) {
                for (Json entry : arr.asArray()) {
                    if (entry != null && entry.type() == Json.Type.OBJECT) {
                        String block = entry.get("block").asString();
                        int x = entry.get("x").asInt();
                        int y = entry.get("y").asInt();
                        int z = entry.get("z").asInt();
                        list.add(new Object[]{ block, new Vec3(x, y, z) });
                    }
                }
            }
            loadedDefs[i] = list;
        }
        defenses = loadedDefs;

        modules.registerButton("Only Top Beds", true);
        modules.registerSlider("Delay After Swap", " ticks", 0, 0, 10, 1);
        modules.registerSlider("Delay After Aiming", " ticks", 0, 0, 10, 1);
        modules.registerSlider("Sneak Hold Ticks", " ticks", 5, 0, 20, 1);
        modules.registerSlider("FOV", "", 180, 0, 180, 1);
        modules.registerSlider("Defense", "", 0, defenseNames);
    }
}

void onEnable() {
    defense = defenses[(int)modules.getSlider(scriptName, "Defense")];
    i = 0;
    renderTarget = null;
    started = sneaked = false;
    lockedDirection = "";
    aimTicks = (int) modules.getSlider(scriptName, "Delay After Aiming");
    serverYaw = client.getPlayer().getYaw();
    serverPitch = client.getPlayer().getPitch();
    inventoryCache.clear();
}

void onDisable() {
    if (sneaked) {
        keybinds.setPressed("sneak", false);
    }
}

Float[] getRotations() {
    onlyTopBeds   = modules.getButton(scriptName, "Only Top Beds");
    delayAfterSwap = (int) modules.getSlider(scriptName, "Delay After Swap");
    delayAfterAim  = (int) modules.getSlider(scriptName, "Delay After Aiming");
    sneakHoldTicks = (int) modules.getSlider(scriptName, "Sneak Hold Ticks");

    float fov = (float) modules.getSlider(scriptName, "FOV");
    float maxYaw   = fov;
    float maxPitch = Math.min(fov, 90f);

    if (!started) {
        Vec3 bh = findBed(8);
        if (bh == null) return null;
        origin = bh;
        switch (world.getBlockAt(bh).variant) {
            case 10: case 0: lockedDirection = "north"; break;
            case 8:  case 2: lockedDirection = "south"; break;
            case 9:  case 3: lockedDirection = "west";  break;
            case 11: case 1: lockedDirection = "east";  break;
            default: lockedDirection = ""; break;
        }
        started = true;
    }

    while (i < defense.size()) {
        Vec3 off = rotateOffset((Vec3) defense.get(i)[1], lockedDirection);
        Vec3 tgt = origin.offset(off.x, off.y, off.z);
        if (world.getBlockAt((int) tgt.x, (int) tgt.y, (int) tgt.z).name.equals("air")) break;
        i++;
    }
    if (i >= defense.size()) { modules.disable(scriptName); return null; }

    Vec3 off = rotateOffset((Vec3) defense.get(i)[1], lockedDirection);
    Vec3 target = origin.offset(off.x, off.y, off.z);
    renderTarget = target;

    float baseYaw   = serverYaw;
    float basePitch = serverPitch;

    Float[] res0 = attemptPlace(baseYaw, basePitch, target);
    if (res0 != null) {
        if (res0[1] == -999f) return new Float[] { baseYaw, basePitch };
        return res0;
    }

    Entity me = client.getPlayer();

    String[] opp = { "DOWN", "UP", "SOUTH", "NORTH", "WEST", "EAST" };
    int[] dx = { 0, 0, 0, 0, 1, -1 };
    int[] dy = { 1, -1, 0, 0, 0, 0 };
    int[] dz = { 0, 0, -1, 1, 0, 0 };

    Vec3 eye = me.getPosition().offset(0, me.getEyeHeight(), 0);
    float curYawW = normYaw(serverYaw);
    float curPit  = serverPitch;
    float cliYawW = normYaw(me.getYaw());
    float cliPit  = me.getPitch();

    double INSET = 0.05, STEP = 0.2, JIT = 0.2, insetTop = 1 - INSET - 1e-3, insetBot = INSET + 1e-3;
    int GRID = (int) Math.round(1 / STEP);

    ArrayList<Object[]> cands = new ArrayList<>((GRID + 1) * (GRID + 1) * 6);

    for (int fi = 0; fi < 6; fi++) {
        String face = opp[fi];
        Vec3 support = new Vec3(target.x + dx[fi], target.y + dy[fi], target.z + dz[fi]);

        String supportName = world.getBlockAt(support).name;
        if (supportName.equals("air")) continue;
        if (onlyTopBeds && supportName.equals("bed") && !"UP".equals(face)) continue;

        for (int rr = 0; rr <= GRID; rr++) {
            boolean ltr = (rr & 1) == 0;
            double v = rr * STEP + util.randomDouble(-STEP * JIT, STEP * JIT);
            if (v < 0) v = 0; else if (v > 1) v = 1;

            for (int cc = 0; cc <= GRID; cc++) {
                double cu = cc * STEP + util.randomDouble(-STEP * JIT, STEP * JIT);
                if (cu < 0) cu = 0; else if (cu > 1) cu = 1;
                double u = ltr ? cu : 1 - cu;

                double px, py, pz;
                if (fi < 2) {
                    px = support.x + u; pz = support.z + v;
                    py = support.y + (fi == 1 ? insetTop : insetBot);
                } else if (fi < 4) {
                    px = support.x + u; py = support.y + v;
                    pz = support.z + (fi == 2 ? insetTop : insetBot);
                } else {
                    pz = support.z + u; py = support.y + v;
                    px = support.x + (fi == 5 ? insetTop : insetBot);
                }

                float[] rotW = getRotationsWrapped(eye, px, py, pz);
                float yawW = rotW[0], pit = rotW[1];

                if (Math.abs(wrapYawDelta(cliYawW, yawW)) > maxYaw) continue;
                if (Math.abs(pit - cliPit) > maxPitch) continue;
                if (Math.abs(pit) > 90f) continue;

                double cost = Math.abs((double) wrapYawDelta(curYawW, yawW)) + Math.abs((double) (pit - curPit)) + ("UP".equals(face) ? -0.25 : 0);

                cands.add(new Object[]{ cost, Float.valueOf(yawW), Float.valueOf(pit) });
            }
        }
    }

    if (cands.isEmpty()) { if (i >= defense.size()) modules.disable(scriptName); return null; }

    cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

    for (int ci = 0; ci < cands.size(); ci++) {
        float yawW = (Float) cands.get(ci)[1];
        float pit  = (Float) cands.get(ci)[2];
        float yawUnwrapped = unwrapYaw(yawW, serverYaw);

        Float[] res = attemptPlace(yawUnwrapped, pit, target);
        if (res != null) return (res[1] == -999f) ? new Float[]{ yawUnwrapped, pit } : res;
    }

    if (i >= defense.size()) modules.disable(scriptName);
    return null;
}

Float[] attemptPlace(float yaw, float pitch, Vec3 target) {
    if (i >= defense.size()) return null;

    Object[] ray = client.raycastBlock(4.5, yaw, pitch);
    if (ray == null) return null;

    Vec3 hit = (Vec3) ray[0];
    String _face = (String) ray[2];
    Vec3 _place = offsetByFace(hit, _face);
    if (!_place.equals(target)) return null;

    String targetBlock = world.getBlockAt((int) hit.x, (int) hit.y, (int) hit.z).name;
    if (onlyTopBeds && !_face.equals("UP") && targetBlock.equals("bed")) return null;

    Object[] cur = defense.get(i);
    String block = (String) cur[0];

    int curSlot = inventory.getSlot();
    int wantSlot = getMatchingInventorySlot(block);
    if (wantSlot == -1) { modules.disable(scriptName); return null; }

    if (curSlot != wantSlot) {
        inventory.setSlot(wantSlot);
        swapTicks = delayAfterSwap;
    }

    if (client.canPlaceBlock(client.getPlayer().getHeldItem(), hit, _face)) {
        client.enableMovementFix();

        if (swapTicks-- > 0) return new Float[]{ -999f, -999f };

        if (!keybinds.isPressed("sneak") && targetBlock.equals("bed")) {
            keybinds.setPressed("sneak", true);
            sneaked = true;
            sneakHoldTicksRemaining = sneakHoldTicks;
            return new Float[]{ -999f, -999f };
        }

        if (aimTicks-- > 0 || Math.abs(yaw - serverYaw) > 25 || Math.abs(pitch - serverPitch) > 25)
            return new Float[]{ yaw, pitch };

        aimTicks = delayAfterAim;

        hitPos = hit;
        face = _face;
        placePos = ((Vec3) ray[1]).offset(hit.x, hit.y, hit.z);
        pendingPlace = true;

        return new Float[]{ yaw, pitch };
    }
    return null;
}

float normYaw(float yaw) { 
    yaw = ((yaw % 360f) + 360f) % 360f; 
    return (yaw > 180f) ? (yaw - 360f) : yaw; 
}
float wrapYawDelta(float base, float target) { 
    float d = target - base; 
    while (d <= -180f) d += 360f; 
    while (d > 180f) d -= 360f; 
    return d; 
}
float unwrapYaw(float yaw, float prevYaw) {
    return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
}
float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
    double dx = tx - eye.x, dy = ty - eye.y, dz = tz - eye.z;
    double hd = Math.sqrt(dx*dx + dz*dz);
    float yawWrapped = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
    yawWrapped = normYaw(yawWrapped);
    float pitch = (float) Math.toDegrees(-Math.atan2(dy, hd));
    return new float[]{ yawWrapped, pitch };
}

void onPreUpdate() {
    if (sneaked) {
        if (sneakHoldTicksRemaining > 0) {
            sneakHoldTicksRemaining--;
        } else {
            keybinds.setPressed("sneak", false);
            sneaked = false;
        }
    }

    if (!pendingPlace) return;
    pendingPlace = false;
    if (client.placeBlock(hitPos, face, placePos)) {
        client.swing();
        i++;
    }
}

void onRenderWorld(float partialTicks) {
    if (renderTarget != null) {
        render.text3d("", renderTarget, 1, false, false, false, 0); // Raven bS sucks so we need this line to setup rendering otherwise the block outline wont render
        render.block(renderTarget, 0x00FF00, true, true);
    }
}

int getMatchingInventorySlot(String blockName) {
    String key = blockName.toLowerCase();
    Integer slot = inventoryCache.get(key);
    if (slot != null) {
        ItemStack cachedItem = inventory.getStackInSlot(slot);
        if (cachedItem != null && cachedItem.name.equalsIgnoreCase(blockName)) {
            return slot;
        }
    }
    for (int s = 0; s < 9; s++) {
        ItemStack item = inventory.getStackInSlot(s);
        if (item != null && item.name.equalsIgnoreCase(blockName)) {
            inventoryCache.put(key, s);
            return s;
        }
    }
    return -1;
}

Vec3 rotateOffset(Vec3 offset, String direction) {
    int x = (int) offset.x, y = (int) offset.y, z = (int) offset.z;
    switch (direction) {
        case "north":
            return new Vec3(x, y, z);
        case "south":
            return new Vec3(-x, y, -z);
        case "east":
            return new Vec3(-z, y, x);
        case "west":
            return new Vec3(z, y, -x);
        default:
            return offset;
    }
}

Vec3 offsetByFace(Vec3 pos, String face) {
    switch (face) {
        case "UP":
            return pos.offset(0, 1, 0);
        case "DOWN":
            return pos.offset(0, -1, 0);
        case "NORTH":
            return pos.offset(0, 0, -1);
        case "SOUTH":
            return pos.offset(0, 0, 1);
        case "EAST":
            return pos.offset(1, 0, 0);
        case "WEST":
            return pos.offset(-1, 0, 0);
        default:
            return pos;
    }
}

Vec3 findBed(int range) {
    Vec3 playerPos = client.getPlayer().getBlockPosition();
    int centerX = (int) playerPos.x;
    int centerY = (int) playerPos.y;
    int centerZ = (int) playerPos.z;
    int startX = centerX - range, endX = centerX + range;
    int startY = centerY - range, endY = centerY + range;
    int startZ = centerZ - range, endZ = centerZ + range;
    
    for (int x = startX; x <= endX; x++) {
        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                Vec3 pos = new Vec3(x, y, z);
                Block block = world.getBlockAt(pos);
                if (!block.name.equalsIgnoreCase("bed")) continue;
                Vec3 head = null, foot = null;
                switch (block.variant) {
                    case 0: // South (foot)
                        foot = pos;
                        head = pos.offset(0, 0, 1);
                        break;
                    case 1: // West (foot)
                        foot = pos;
                        head = pos.offset(-1, 0, 0);
                        break;
                    case 2: // North (foot)
                        foot = pos;
                        head = pos.offset(0, 0, -1);
                        break;
                    case 3: // East (foot)
                        foot = pos;
                        head = pos.offset(1, 0, 0);
                        break;
                    case 8: // South (head)
                        head = pos;
                        foot = pos.offset(0, 0, -1);
                        break;
                    case 9: // West (head)
                        head = pos;
                        foot = pos.offset(1, 0, 0);
                        break;
                    case 10: // North (head)
                        head = pos;
                        foot = pos.offset(0, 0, 1);
                        break;
                    case 11: // East (head)
                        head = pos;
                        foot = pos.offset(-1, 0, 0);
                        break;
                }
                if (head != null && foot != null) {
                    double headDist = head.distanceToSq(playerPos);
                    double footDist = foot.distanceToSq(playerPos);
                    return headDist > footDist ? head : foot;
                }
            }
        }
    }
    return null;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C03) {
        C03 c03 = (C03) packet;
        if (c03.name.startsWith("C05") || c03.name.startsWith("C06")) {
            serverYaw = c03.yaw;
            serverPitch = c03.pitch;
        }
    }
    return true;
}

boolean onMouse(int button, boolean state) {
    if (button == 1 || button == 0) {
        return false;
    }
    return true;
}