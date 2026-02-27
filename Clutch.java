Map<String,Integer> BLOCK_SCORE = new HashMap<>();
{
    BLOCK_SCORE.put("obsidian", 0);
    BLOCK_SCORE.put("end_stone", 1);
    BLOCK_SCORE.put("planks", 2);
    BLOCK_SCORE.put("log", 2);
    BLOCK_SCORE.put("log2", 2);
    BLOCK_SCORE.put("glass", 3);
    BLOCK_SCORE.put("stained_glass", 3);
    BLOCK_SCORE.put("hardened_clay", 4);
    BLOCK_SCORE.put("stained_hardened_clay", 4);
    BLOCK_SCORE.put("stone", 5);
    BLOCK_SCORE.put("wool", 5);
}

Set<String> placeThrough = new HashSet<>(Arrays.asList(
    "air",
    "water",
    "lava",
    "fire"
));

double HW = 0.3;
double[][] CORNERS = {{ -HW, -HW }, { HW, -HW }, { -HW, HW }, { HW, HW }};

float serverYaw, serverPitch;
Vec3 placeAt, hitAt;
String hitSide = "";
boolean placeQueued;
boolean placing;
boolean slotWasSwapped;
boolean autoClickerWasOn;
int prevSlot = -1;
int plannedSlot = -1;
float aimYaw, aimPitch;
Vec3 targetHitPos;
String targetSide;
boolean hasAim;
boolean resetting;
int lastPlacedX = -999, lastPlacedY = -999, lastPlacedZ = -999;
int clutchBlocksPlaced = 0;

void onLoad() {
    modules.registerSlider("Reach", " blocks", 4.5, 0.5, 4.5, 0.1);
    modules.registerSlider("Speed", "", 8, 0, 100, 1);
    modules.registerSlider("Snapback Speed", "", 12, 0, 100, 1);
    modules.registerSlider("Max distance", " blocks", 10, 0, 20, 1);
    modules.registerSlider("Rotation Tolerance", "\u00B0", 25, 20, 100, 1);
    modules.registerButton("Simulate future position", true);
    modules.registerKey("Select Keybind", 0);
}

void onEnable() {
    serverYaw = client.getPlayer().getYaw();
    serverPitch = client.getPlayer().getPitch();
    hasAim = false;
    resetting = false;
    clutchBlocksPlaced = 0;
}

void onPrePlayerInteract() {
    boolean pressed = modules.getKeyPressed(scriptName, "Select Keybind");
    if (!pressed || !client.getScreen().isEmpty()) {
        clearAim();
        disablePlacing();
        return;
    }

    Entity me = client.getPlayer();
    Vec3 pos = me.getPosition();

    if (me.onGround()) clutchBlocksPlaced = 0;

    boolean needsClutch = canPlaceThrough(world.getBlockAt(pos.floor().offset(0, -1, 0)).name);

    if (!needsClutch) {
        disablePlacing();
        return;
    }

    int weakSlot = pickBlockSlot();
    if (weakSlot == -1) {
        disablePlacing();
        return;
    }

    plannedSlot = weakSlot;

    Object[] tgt = clutchAim();
    if (tgt != null) {
        Object[] ray = (Object[]) tgt[0];
        targetHitPos = (Vec3) ray[0];
        targetSide = (String) ray[2];
        aimYaw = (float) tgt[1];
        aimPitch = (float) tgt[2];
        hasAim = true;
        resetting = false;
    }

    if (hasAim && !placing) enablePlacing();

    if (placing) {
        keybinds.setPressed("attack", false);
        keybinds.setPressed("use", false);
        equipPlannedSlot();
    }
}

Float[] getRotations() {
    if (!client.getScreen().isEmpty()) disablePlacing();

    if (resetting) {
        aimYaw = client.getPlayer().getYaw();
        aimPitch = client.getPlayer().getPitch();
        Float[] sm = getRotationsSmoothed(aimYaw, aimPitch, true);
        if (Math.abs(sm[0] - aimYaw) < 0.5f && Math.abs(sm[1] - aimPitch) < 0.5f) {
            resetting = false;
            return null;
        }
        client.enableMovementFix();
        return sm;
    }

    if (!hasAim) return null;

    Float[] sm = getRotationsSmoothed(aimYaw, aimPitch, false);

    if (placing && targetHitPos != null) {
        double reach = modules.getSlider(scriptName, "Reach");
        Object[] chk = client.raycastBlock(reach, sm[0], sm[1]);

        if (chk != null) {
            Vec3 hit = (Vec3) chk[0];
            String side = (String) chk[2];
            if (hit.equals(targetHitPos) && side.equals(targetSide)) {
                int maxBlocks = (int) modules.getSlider(scriptName, "Max distance");
                if (maxBlocks == 0 || clutchBlocksPlaced < maxBlocks) {
                    double tol = modules.getSlider(scriptName, "Rotation Tolerance");
                    if (Math.abs(sm[0] - serverYaw) <= tol && Math.abs(sm[1] - serverPitch) <= tol) {
                        hitAt = hit;
                        hitSide = side;
                        placeAt = ((Vec3) chk[1]).offset(hit.x, hit.y, hit.z);
                        placeQueued = true;
                    }
                }
            }
        }
    }

    client.enableMovementFix();
    return sm;
}

void onPreUpdate() {
    if (placeQueued) {
        placeQueued = false;
        if (client.placeBlock(hitAt, hitSide, placeAt)) {
            if (!"UP".equals(hitSide)) clutchBlocksPlaced++;
            lastPlacedX = (int) Math.floor(hitAt.x);
            lastPlacedY = (int) Math.floor(hitAt.y);
            lastPlacedZ = (int) Math.floor(hitAt.z);
            client.swing();
        }
    }
}

void enablePlacing() {
    if (placing) return;
    placing = true;
    slotWasSwapped = false;
    prevSlot = inventory.getSlot();
    autoClickerWasOn = modules.isEnabled("AutoClicker");
    if (autoClickerWasOn) modules.disable("AutoClicker");
}

void disablePlacing() {
    if (!placing) return;

    if (slotWasSwapped && prevSlot != -1 && prevSlot != inventory.getSlot()) {
        inventory.setSlot(prevSlot);
    }

    placing = false;
    slotWasSwapped = false;
    prevSlot = -1;
    plannedSlot = -1;

    if (client.getScreen().isEmpty()) {
        keybinds.setPressed("attack", keybinds.isMouseDown(0));
        keybinds.setPressed("use", keybinds.isMouseDown(1));
    }

    if (autoClickerWasOn) {
        modules.enable("AutoClicker");
        autoClickerWasOn = false;
    }
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
    if (placing && button > -1) return false;
    return true;
}

void clearAim() {
    targetHitPos = null;
    targetSide = "";
    lastPlacedX = lastPlacedY = lastPlacedZ = -999;
    clutchBlocksPlaced = 0;
    if (hasAim) resetting = true;
    hasAim = false;
}

Object[] clutchAim() {
    Entity me = client.getPlayer();
    Vec3 p = me.getPosition();
    Vec3 eye = p.offset(0, me.getEyeHeight(), 0);
    double reach = modules.getSlider(scriptName, "Reach");

    boolean simulateFuture = modules.getButton(scriptName, "Simulate future position");
    Vec3 futurePos = p;
    if (simulateFuture) {
        Simulation sim = Simulation.create();
        for (int t = 0; t < 20; t++) {
            sim.tick();
            Vec3 sp = sim.getPosition();
            if (sp.y < p.y - 2 || sim.onGround()) break;
        }
        futurePos = sim.getPosition();
    }

    int feetX = (int) Math.floor(p.x), feetZ = (int) Math.floor(p.z);
    int feetY = (int) Math.floor(p.y);
    int minX = feetX - 5, maxX = feetX + 4;
    int minZ = feetZ - 5, maxZ = feetZ + 4;
    int maxY = feetY - 1;
    int minY = feetY - 4;

    ArrayList<Object[]> cands = new ArrayList<>();
    for (int y = maxY; y >= minY; y--) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block b = world.getBlockAt(x, y, z);
                if (canPlaceThrough(b.name)) continue;

                double currentDist = dist2PointAABB(p, b);
                double futureDist = dist2PointAABB(futurePos, b);
                double score = simulateFuture ? (currentDist * 0.3 + futureDist * 0.7) : currentDist;
                if (b.x == lastPlacedX && b.y == lastPlacedY && b.z == lastPlacedZ) score *= 0.95;
                cands.add(new Object[]{ score, b });
            }
        }
    }

    cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

    ItemStack held = inventory.getStackInSlot(plannedSlot);
    for (int i = 0; i < cands.size(); i++) {
        Block b = (Block) cands.get(i)[1];
        boolean underPlayer = isBlockUnderPlayer(b, p);
        Object[] res = getBestRotationsToBlock(held, b, eye, reach, underPlayer);
        if (res != null) return res;
    }

    return null;
}

boolean isBlockUnderPlayer(Block b, Vec3 pos) {
    if (b.y >= (int) Math.floor(pos.y)) return false;
    for (double[] c : CORNERS) {
        int cx = (int) Math.floor(pos.x + c[0]);
        int cz = (int) Math.floor(pos.z + c[1]);
        if (b.x == cx && b.z == cz) return true;
    }
    return false;
}

Object[] getBestRotationsToBlock(ItemStack held, Block b, Vec3 eye, double reach, boolean underPlayer) {
    double INSET = 0.05, STEP = 0.2, JIT = STEP * 0.1;
    boolean faceSOUTH = Math.abs(eye.z - (b.z + 1)) < Math.abs(eye.z - b.z);
    boolean faceEAST = Math.abs(eye.x - (b.x + 1)) < Math.abs(eye.x - b.x);
    float baseYaw = normYaw(serverYaw);
    float basePit = serverPitch;
    int n = (int) Math.round(1 / STEP);

    ArrayList<Object[]> cands = new ArrayList<>();
    cands.add(new Object[]{ 0D, baseYaw, basePit });

    for (int r = 0; r <= n; r++) {
        double v = r * STEP + util.randomDouble(-JIT, JIT);
        if (v < 0) v = 0; else if (v > 1) v = 1;

        for (int c = 0; c <= n; c++) {
            double u = c * STEP + util.randomDouble(-JIT, JIT);
            if (u < 0) u = 0; else if (u > 1) u = 1;

            if (underPlayer) {
                float[] rV = getRotationsWrapped(eye, b.x + u, b.y + 1 - INSET, b.z + v);
                double costV = Math.abs((double) wrapYawDelta(baseYaw, rV[0])) + Math.abs((double) (rV[1] - basePit));
                cands.add(new Object[]{ costV, rV[0], rV[1] });
            }

            float[] rZ = getRotationsWrapped(eye, b.x + u, b.y + v, faceSOUTH ? b.z + 1 - INSET : b.z + INSET);
            double costZ = Math.abs((double) wrapYawDelta(baseYaw, rZ[0])) + Math.abs((double) (rZ[1] - basePit));
            cands.add(new Object[]{ costZ, rZ[0], rZ[1] });

            float[] rX = getRotationsWrapped(eye, faceEAST ? b.x + 1 - INSET : b.x + INSET, b.y + v, b.z + u);
            double costX = Math.abs((double) wrapYawDelta(baseYaw, rX[0])) + Math.abs((double) (rX[1] - basePit));
            cands.add(new Object[]{ costX, rX[0], rX[1] });
        }
    }

    cands.sort((a, b2) -> Double.compare(((Number) a[0]).doubleValue(), ((Number) b2[0]).doubleValue()));

    Vec3 targetCell = new Vec3(b.x, b.y, b.z);
    for (int i = 0; i < cands.size(); i++) {
        float yawW = unwrapYaw(((Number) cands.get(i)[1]).floatValue(), serverYaw);
        float pit = ((Number) cands.get(i)[2]).floatValue();

        Object[] ray = client.raycastBlock(reach, yawW, pit);
        if (ray == null) continue;

        Vec3 hit = (Vec3) ray[0];
        String face = (String) ray[2];

        if ("DOWN".equals(face)) continue;
        if ("UP".equals(face) && !underPlayer) continue;
        if (!hit.floor().equals(targetCell)) continue;
        if (!client.canPlaceBlock(held, hit, face)) continue;

        return new Object[]{ ray, yawW, pit };
    }

    return null;
}

int pickBlockSlot() {
    int best = -1;
    int bestScore = Integer.MIN_VALUE;

    for (int slot = 8; slot >= 0; --slot) {
        ItemStack s = inventory.getStackInSlot(slot);
        if (s == null || s.stackSize == 0) continue;

        Integer score = BLOCK_SCORE.get(s.name);
        if (score == null) continue;

        if (score > bestScore) {
            bestScore = score;
            best = slot;
        }
    }
    return best;
}

void equipPlannedSlot() {
    int cur = inventory.getSlot();
    if (plannedSlot != -1 && plannedSlot != cur) {
        inventory.setSlot(plannedSlot);
        slotWasSwapped = true;
    }
}

Float[] getRotationsSmoothed(float targetYaw, float targetPitch, boolean snapback) {
    float curYaw = serverYaw;
    float curPitch = serverPitch;

    float dYaw = targetYaw - curYaw;
    float dPit = targetPitch - curPitch;

    if (Math.abs(dYaw) < 0.1f) curYaw = targetYaw;
    if (Math.abs(dPit) < 0.1f) curPitch = targetPitch;
    if (curYaw == targetYaw && curPitch == targetPitch)
        return new Float[] { curYaw, curPitch };

    float maxStep = (float) modules.getSlider(scriptName, snapback ? "Snapback Speed" : "Speed");
    float random = 20;

    if (random > 0f) {
        float factor = 1f - (float) util.randomDouble(0, random / 100f);
        maxStep *= factor;
    }

    float totalDelta = Math.abs(dYaw) + Math.abs(dPit);
    if (totalDelta <= maxStep) {
        curYaw = targetYaw;
        curPitch = targetPitch;
    } else {
        float scale = maxStep / totalDelta;
        curYaw += dYaw * scale;
        curPitch += dPit * scale;
    }

    return new Float[] { curYaw, curPitch };
}

boolean canPlaceThrough(String name) {
    return placeThrough.contains(name);
}

double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }

double dist2PointAABB(Vec3 p, Block b) {
    double minX = b.x, maxX = b.x + 1;
    double minY = b.y, maxY = b.y + 1;
    double minZ = b.z, maxZ = b.z + 1;
    double cx = clamp(p.x, minX, maxX);
    double cy = clamp(p.y, minY, maxY);
    double cz = clamp(p.z, minZ, maxZ);
    double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
    return dx*dx + dy*dy + dz*dz;
}

float normYaw(float yaw) { yaw = ((yaw % 360f) + 360f) % 360f; return (yaw > 180f) ? (yaw - 360f) : yaw; }
float wrapYawDelta(float base, float target) { float d = target - base; while (d <= -180f) d += 360f; while (d > 180f) d -= 360f; return d; }
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
