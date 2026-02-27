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
    BLOCK_SCORE.put("wool", 5);
}

Set<String> placeThrough = new HashSet<>(Arrays.asList(
    "air",
    "water",
    "lava",
    "fire"
));

float serverYaw, serverPitch;
float fillCount;
Vec3 placeAt, hitAt;
String hitSide = "";
boolean placeQueued;
boolean placing;
boolean slotWasSwapped;
boolean autoClickerWasOn;
int prevSlot = -1;
int plannedSlot = -1;
float circleProgress = 0f;
float animStartProgress = 0f;
float animTargetProgress = 0f;
long animStartTime = 0L;
float lastFillCount = -1;
float aimYaw, aimPitch;
Vec3 targetHitPos;
String targetSide;
int[][] DIRS = { {1,0,0}, {0,0,1}, {-1,0,0}, {0,0,-1} };
String[] SIDE_FACES = { "DOWN", "UP", "NORTH", "SOUTH", "EAST", "WEST" };

void onLoad() {
    modules.registerSlider("Reach", " blocks", 4.5, 0.5, 4.5, 0.1);
    modules.registerSlider("Speed", "", 8, 0, 100, 1);
    modules.registerSlider("Rotation Tolerance", "\u00B0", 25, 20, 100, 1);
    modules.registerKey("Select Keybind", 0);
}

void onEnable() {
    serverYaw = client.getPlayer().getYaw();
    serverPitch = client.getPlayer().getPitch();
}

void onPrePlayerInteract() {
    clearAim();

    boolean pressed = modules.getKeyPressed(scriptName, "Select Keybind");
    if (!pressed || !client.getScreen().isEmpty()) {
        disablePlacing();
        circleProgress = 0f;
        return;
    }

    int strongSlot = pickBlockSlot(true);
    int weakSlot = pickBlockSlot(false);
    if (strongSlot == -1 && weakSlot == -1) {
        disablePlacing();
        return;
    }

    plannedSlot = (strongSlot != -1 ? strongSlot : weakSlot);

    Object[] tgt = getTarget();
    if (tgt == null) {
        disablePlacing();
        return;
    }

    boolean adjacent = (Boolean) tgt[0];
    if (adjacent) plannedSlot = (strongSlot != -1 ? strongSlot : weakSlot);
    else plannedSlot = (weakSlot != -1 ? weakSlot : strongSlot);

    if (!placing) enablePlacing();

    if (keybinds.isPressed("attack") || keybinds.isPressed("use")) {
        clearAim();
    }

    keybinds.setPressed("attack", false);
    keybinds.setPressed("use", false);
    equipPlannedSlot();
}

Float[] getRotations() {
    if (!client.getScreen().isEmpty()) disablePlacing();
    if (!placing || targetHitPos == null) return null;

    Float[] sm = getRotationsSmoothed(aimYaw, aimPitch);
    double reach = modules.getSlider(scriptName, "Reach");
    Object[] chk = client.raycastBlock(reach, sm[0], sm[1]);

    if (chk != null) {
        Vec3 hit = (Vec3) chk[0];
        String side = (String) chk[2];
        if (hit.equals(targetHitPos) && side.equals(targetSide)) {
            double tol = modules.getSlider(scriptName, "Rotation Tolerance");
            if (Math.abs(sm[0] - serverYaw) <= tol && Math.abs(sm[1] - serverPitch) <= tol) {
                hitAt = hit;
                hitSide = side;
                placeAt = ((Vec3) chk[1]).offset(hit.x, hit.y, hit.z);
                placeQueued = true;
            }
        }
    }

    client.enableMovementFix();
    return sm;
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

void onPreUpdate() {
    if (placeQueued) {
        placeQueued = false;
        if (client.placeBlock(hitAt, hitSide, placeAt)) client.swing();
    }

    fillCount = 0;
    if (modules.getKeyPressed(scriptName, "Select Keybind") && client.getScreen().isEmpty()) {
        Vec3 feet = client.getPlayer().getPosition().floor();

        if (!canPlaceThrough(world.getBlockAt(feet.offset(0, 2, 0)).name)) fillCount++;

        for (int[] d : DIRS) {
            Vec3 f = feet.offset(d[0], 0, d[2]);
            if (!canPlaceThrough(world.getBlockAt(f).name)) fillCount++;

            Vec3 h = feet.offset(d[0], 1, d[2]);
            if (!canPlaceThrough(world.getBlockAt(h).name)) fillCount++;
        }

        if (fillCount != lastFillCount) {
            animStartProgress = circleProgress;
            animTargetProgress = Math.max(0f, Math.min(1f, fillCount / 9f));
            animStartTime = client.time();
            lastFillCount = fillCount;
        }
    }
}

void onRenderTick(float pt) {
    if (fillCount <= 0) return;

    long elapsed = client.time() - animStartTime;
    if (elapsed < 50L) {
        float t = (float) elapsed / 50f;
        float eased = quadInOutEasing(t);
        circleProgress = lerp(animStartProgress, animTargetProgress, eased);
    } else {
        circleProgress = animTargetProgress;
    }

    float radius = 10f;
    float thickness = 3f;
    int[] sz = client.getDisplaySize();
    float cx = sz[0] / 2f - 1f;
    float cy = sz[1] / 2f;

    drawCircle(cx, cy, radius, 100, thickness, 0f, 0f, 0f, 0.5f);

    float startAngle = 90f;
    if (circleProgress >= 0.999f) {
        drawCircle(cx, cy, radius, 100, thickness, 0f, 1f, 0f, 1f);
        return;
    }

    float endAngle = startAngle + circleProgress * 360f + 0.5f;

    float ratio = circleProgress;
    if (ratio < 0f) ratio = 0f; else if (ratio > 1f) ratio = 1f;
    int r = (int)((1f - ratio) * 255f + 0.5f);
    int g = (int)(ratio * 255f + 0.5f);
    int curColor = rgba(r, g, 0, 255);

    drawCircleArc(cx, cy, radius, startAngle, endAngle, thickness, curColor);
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
}

int pickBlockSlot(boolean preferStrong) {
    int best = -1;
    int bestScore = preferStrong ? Integer.MAX_VALUE : Integer.MIN_VALUE;

    for (int slot = 8; slot >= 0; --slot) {
        ItemStack s = inventory.getStackInSlot(slot);
        if (s == null || s.stackSize == 0) continue;

        Integer score = BLOCK_SCORE.get(s.name);
        if (score == null) continue;

        if (preferStrong ? score < bestScore : score > bestScore) {
            bestScore = score;
            best = slot;
            if (preferStrong && score == 0) break;
        }
    }
    return best;
}

boolean isDirectAdjacentPlacement(Vec3 p) {
    Vec3 feet = client.getPlayer().getPosition().floor();
    int dx = (int)(p.x - feet.x), dy = (int)(p.y - feet.y), dz = (int)(p.z - feet.z);
    if (dx == 0 && dz == 0 && dy == 2) return true;
    if ((dy == 0 || dy == 1) && ((Math.abs(dx) == 1 && dz == 0) || (Math.abs(dz) == 1 && dx == 0))) return true;
    return false;
}

Object[] getTarget() {
    Object[] res = roofAim();
    if (res == null) res = sidesAim();
    if (res == null) return null;

    Object[] ray = (Object[]) res[0];
    Vec3 hit = (Vec3) ray[0];
    String side = (String) ray[2];
    Vec3 placed = offsetByFace(hit, side).floor();
    boolean adjacent = isDirectAdjacentPlacement(placed);

    targetHitPos = hit;
    targetSide = side;
    aimYaw = (float) res[1];
    aimPitch = (float) res[2];
    return new Object[]{ adjacent };
}

void equipPlannedSlot() {
    int cur = inventory.getSlot();
    if (plannedSlot != -1 && plannedSlot != cur) {
        inventory.setSlot(plannedSlot);
        slotWasSwapped = true;
    }
}

Float[] getRotationsSmoothed(float targetYaw, float targetPitch) {
    float curYaw = serverYaw;
    float curPitch = serverPitch;

    float dYaw = targetYaw - curYaw;
    float dPit = targetPitch - curPitch;

    if (Math.abs(dYaw) < 0.1f) curYaw = targetYaw;
    if (Math.abs(dPit) < 0.1f) curPitch = targetPitch;
    if (curYaw == targetYaw && curPitch == targetPitch)
        return new Float[] { curYaw, curPitch };

    float maxStep = (float) modules.getSlider(scriptName, "Speed");
    float random = 20;

    if (random > 0f) {
        float factor = 1f - (float) util.randomDouble(0, random / 100f);
        maxStep *= factor;
    }

    float stepYaw = Math.max(-maxStep, Math.min(maxStep, dYaw));
    float stepPit = Math.max(-maxStep, Math.min(maxStep, dPit));

    curYaw += stepYaw;
    curPitch += stepPit;

    if (Math.signum(targetYaw - curYaw) != Math.signum(dYaw)) curYaw = targetYaw;
    if (Math.signum(targetPitch - curPitch) != Math.signum(dPit)) curPitch = targetPitch;

    return new Float[] { curYaw, curPitch };
}

Object[] roofAim() {
    Entity me = client.getPlayer();
    Vec3 p = me.getPosition();
    if (!canPlaceThrough(world.getBlockAt((int)Math.floor(p.x), (int)Math.floor(p.y) + 2, (int)Math.floor(p.z)).name)) return null;

    ItemStack held = inventory.getStackInSlot(plannedSlot);
    double r = modules.getSlider(scriptName, "Reach");
    Vec3 eye = p.offset(0, me.getEyeHeight(), 0);
    double r2 = r * r, rp12 = (r + 1) * (r + 1);

    int minY = (int) Math.floor(eye.y) + 1, maxY = (int) Math.floor(eye.y + r);
    int minX = (int) Math.floor(eye.x - r), maxX = (int) Math.floor(eye.x + r);
    int minZ = (int) Math.floor(eye.z - r), maxZ = (int) Math.floor(eye.z + r);

    ArrayList<Object[]> cands = new ArrayList<>();
    for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
        double dx = (x + 0.5) - eye.x, dy = (y + 0.5) - eye.y, dz = (z + 0.5) - eye.z;
        if (dx*dx + dy*dy + dz*dz > rp12) continue;

        Block b = world.getBlockAt(x, y, z);
        if (canPlaceThrough(b.name)) continue;

        double d2 = dist2PointAABB(eye, b);
        if (d2 > r2) continue;

        cands.add(new Object[]{ d2, b });
    }

    cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

    for (int i = 0; i < cands.size(); i++) {
        Block b = (Block) cands.get(i)[1];
        Object[] res = getBestRotationsToBlock(held, b, eye, r, minY);
        if (res != null) return res;
    }
    return null;
}

Object[] getBestRotationsToBlock(ItemStack held, Block b, Vec3 eye, double reach, int minY) {
    double INSET = 0.05, STEP = 0.2, JIT = STEP * 0.1;
    boolean faceUP = Math.abs(eye.y - (b.y + 1)) < Math.abs(eye.y - b.y);
    boolean faceSOUTH = Math.abs(eye.z - (b.z + 1)) < Math.abs(eye.z - b.z);
    boolean faceEAST = Math.abs(eye.x - (b.x + 1)) < Math.abs(eye.x - b.x);
    float baseYaw = normYaw(serverYaw);
    float basePit = serverPitch;
    int n = (int) Math.round(1 / STEP);

    ArrayList<Object[]> cands = new ArrayList<>((n + 1) * (n + 1) * 3 + 1);
    cands.add(new Object[]{ 0D, baseYaw, basePit });

    for (int r = 0; r <= n; r++) {
        double v = r * STEP + util.randomDouble(-JIT, JIT);
        if (v < 0) v = 0; else if (v > 1) v = 1;

        for (int c = 0; c <= n; c++) {
            double u = c * STEP + util.randomDouble(-JIT, JIT);
            if (u < 0) u = 0; else if (u > 1) u = 1;

            float[] rV = getRotationsWrapped(eye, b.x + u, faceUP ? b.y + 1 - INSET : b.y + INSET, b.z + v);
            double costV = Math.abs((double) wrapYawDelta(baseYaw, rV[0])) + Math.abs((double) (rV[1] - basePit));
            cands.add(new Object[]{ costV, rV[0], rV[1] });

            float[] rZ = getRotationsWrapped(eye, b.x + u, b.y + v, faceSOUTH ? b.z + 1 - INSET : b.z + INSET);
            double costZ = Math.abs((double) wrapYawDelta(baseYaw, rZ[0])) + Math.abs((double) (rZ[1] - basePit));
            cands.add(new Object[]{ costZ, rZ[0], rZ[1] });

            float[] rX = getRotationsWrapped(eye, faceEAST ? b.x + 1 - INSET : b.x + INSET, b.y + v, b.z + u);
            double costX = Math.abs((double) wrapYawDelta(baseYaw, rX[0])) + Math.abs((double) (rX[1] - basePit));
            cands.add(new Object[]{ costX, rX[0], rX[1] });
        }
    }

    cands.sort((a, b2) -> Double.compare(((Number) a[0]).doubleValue(), ((Number) b2[0]).doubleValue()));

    Object[] best = null;
    float bestYaw = 0f, bestPit = 0f;
    Vec3 targetCell = new Vec3(b.x, b.y, b.z);

    for (int i = 0; i < cands.size(); i++) {
        float yawW = unwrapYaw(((Number) cands.get(i)[1]).floatValue(), serverYaw);
        float pit = ((Number) cands.get(i)[2]).floatValue();

        Object[] ray = client.raycastBlock(reach, yawW, pit);
        if (ray == null) continue;

        Vec3 hit = (Vec3) ray[0];
        String face = (String) ray[2];

        Vec3 hitCell = hit.floor();
        int hitY = (int) hitCell.y;
        int byY = (int) targetCell.y;

        if (hitCell.equals(targetCell) && hitY >= minY && !("DOWN".equals(face) && byY == minY) && client.canPlaceBlock(held, hit, face)) {
            best = ray; bestYaw = yawW; bestPit = pit;
            break;
        }
    }

    return best != null ? new Object[]{ best, bestYaw, bestPit } : null;
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

boolean canPlaceThrough(String name) {
    return placeThrough.contains(name);
}

boolean hasAirNeighborExceptPlayer(Vec3 pos, Vec3 feet, Vec3 head) {
    int px = (int)Math.floor(pos.x), py = (int)Math.floor(pos.y), pz = (int)Math.floor(pos.z);
    int fx = (int)Math.floor(feet.x), fy = (int)Math.floor(feet.y), fz = (int)Math.floor(feet.z);
    int hx = (int)Math.floor(head.x), hy = (int)Math.floor(head.y), hz = (int)Math.floor(head.z);
    int[] ox = { 1, -1, 0, 0, 0, 0 };
    int[] oy = { 0, 0, 1, -1, 0, 0 };
    int[] oz = { 0, 0, 0, 0, 1, -1 };
    for (int i = 0; i < 6; i++) {
        Vec3 n = new Vec3(px + ox[i], py + oy[i], pz + oz[i]);
        if (!"air".equals(world.getBlockAt(n).name)) continue;
        int nx = px + ox[i], ny = py + oy[i], nz = pz + oz[i];
        boolean isFeet = nx == fx && ny == fy && nz == fz;
        boolean isHead = nx == hx && ny == hy && nz == hz;
        if (!isFeet && !isHead) return true;
    }
    return false;
}

double sq(double v) { return v * v; }
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

Object[] sidesAim() {
    Entity me = client.getPlayer();
    Vec3 feet = me.getPosition().floor();
    Vec3 head = feet.offset(0, 1, 0);
    double reach = modules.getSlider(scriptName, "Reach");
    Vec3 eye = me.getPosition().offset(0, me.getEyeHeight(), 0);

    ArrayList<Vec3> baseline = new ArrayList<>(8);
    for (int[] d : DIRS) {
        baseline.add(feet.offset(d[0], 0, d[2]));
        baseline.add(head.offset(d[0], 0, d[2]));
    }

    ArrayList<Vec3> primaryGoals = new ArrayList<>(baseline.size());
    for (Vec3 pos : baseline) {
        if (!canPlaceThrough(world.getBlockAt(pos).name)) continue;
        if (!hasAirNeighborExceptPlayer(pos, feet, head)) continue;
        primaryGoals.add(pos);
    }
    if (primaryGoals.isEmpty()) return null;

    Vec3 enemyPos = getClosestOtherPlayerPos();
    if (enemyPos != null) {
        baseline.sort((a, b) -> {
            double da = sq(a.x + 0.5 - enemyPos.x) + sq(a.y + 0.5 - enemyPos.y) + sq(a.z + 0.5 - enemyPos.z);
            double db = sq(b.x + 0.5 - enemyPos.x) + sq(b.y + 0.5 - enemyPos.y) + sq(b.z + 0.5 - enemyPos.z);
            return Double.compare(da, db);
        });
        int picked = 0;
        for (int i = 0; i < baseline.size() && picked < 3; i++) {
            Vec3 pos = baseline.get(i);
            if (!canPlaceThrough(world.getBlockAt(pos).name)) continue;
            if (!hasAirNeighborExceptPlayer(pos, feet, head)) continue;
            Object[] rEnemy = findBestForGoals(Collections.singletonList(pos), reach, eye);
            if (rEnemy != null) return rEnemy;
            picked++;
        }
    }

    Object[] r = findBestForGoals(primaryGoals, reach, eye);
    if (r != null) return r;

    ArrayList<Vec3> frontier = new ArrayList<>(primaryGoals);
    HashSet<String> seen = new HashSet<>(frontier.size() * 8);
    for (Vec3 g : frontier) seen.add(key(g));

    for (int iter = 0; iter < 5; iter++) {
        if (frontier.isEmpty()) break;

        ArrayList<Vec3> layer = new ArrayList<>(frontier.size() * 3);
        for (Vec3 g : frontier) {
            for (String f : SIDE_FACES) {
                Vec3 s = offsetByFace(g, f);
                if (!canPlaceThrough(world.getBlockAt(s).name)) continue;
                String k = key(s);
                if (!seen.add(k)) continue;
                layer.add(s);
            }
        }

        if (!layer.isEmpty()) {
            Object[] rLayer = findBestForGoals(layer, reach, eye);
            if (rLayer != null) return rLayer;
        }
        frontier = layer;
    }

    return null;
}

Object[] findBestForGoals(List<Vec3> goals, double reach, Vec3 eye) {
    if (goals == null || goals.isEmpty()) return null;

    ItemStack held = inventory.getStackInSlot(plannedSlot);
    float curYawW = normYaw(serverYaw), curPitch = serverPitch;

    Object[] now = client.raycastBlock(reach, curYawW, curPitch);
    if (now != null) {
        Vec3 support = (Vec3) now[0];
        String faceHit = (String) now[2];

        if (!canPlaceThrough(world.getBlockAt(support).name) && client.canPlaceBlock(held, support, faceHit)) {
            for (int i = 0; i < goals.size(); i++) {
                Object[] ok = tryPlacement(reach, serverYaw, serverPitch, support, faceHit, goals.get(i));
                if (ok != null) return ok;
            }
        }
    }

    int[] dx = { 0, 0, 0, 0, 1, -1 };
    int[] dy = { 1, -1, 0, 0, 0, 0 };
    int[] dz = { 0, 0, -1, 1, 0, 0 };
    double INSET = 0.05, STEP = 0.2, JIT = 0.1, insetTop = 1 - INSET - 1e-3, insetBot = INSET + 1e-3;
    int GRID = (int) Math.round(1 / STEP);
    int cells = (GRID + 1) * (GRID + 1);
    ArrayList<Object[]> cands = new ArrayList<>(Math.max(16, goals.size() * 6 * cells));

    for (int gi = 0; gi < goals.size(); gi++) {
        Vec3 g = goals.get(gi);

        for (int i = 0; i < 6; i++) {
            Vec3 support = new Vec3(g.x + dx[i], g.y + dy[i], g.z + dz[i]);
            String face = SIDE_FACES[i];
            String supportName = world.getBlockAt(support).name;
            if (canPlaceThrough(supportName) || !client.canPlaceBlock(held, support, face)) continue;

            for (int rr = 0; rr <= GRID; rr++) {
                boolean ltr = (rr & 1) == 0;
                double v = rr * STEP + util.randomDouble(-STEP * JIT, STEP * JIT);
                if (v < 0) v = 0; else if (v > 1) v = 1;

                for (int cc = 0; cc <= GRID; cc++) {
                    double cu = cc * STEP + util.randomDouble(-STEP * JIT, STEP * JIT);
                    if (cu < 0) cu = 0; else if (cu > 1) cu = 1;
                    double u = ltr ? cu : 1 - cu;

                    double px, py, pz;
                    if (i < 2) {
                        px = support.x + u;
                        pz = support.z + v;
                        py = support.y + (i == 1 ? insetTop : insetBot);
                    } else if (i < 4) {
                        px = support.x + u;
                        py = support.y + v;
                        pz = support.z + (i == 2 ? insetTop : insetBot);
                    } else {
                        pz = support.z + u;
                        py = support.y + v;
                        px = support.x + (i == 5 ? insetTop : insetBot);
                    }

                    float[] rot = getRotationsWrapped(eye, px, py, pz);
                    float yawW = rot[0], pit = rot[1];

                    float dYaw = Math.abs(wrapYawDelta(curYawW, yawW));
                    float dPit = Math.abs(pit - curPitch);
                    if (dYaw < 0.1f && dPit < 0.1f) continue;

                    double cost = dYaw + dPit;
                    cands.add(new Object[]{ cost, yawW, pit, support, face, g });
                }
            }
        }
    }

    if (cands.isEmpty()) return null;

    cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

    for (int i = 0; i < cands.size(); i++) {
        float yawUnwrapped = unwrapYaw((Float) cands.get(i)[1], serverYaw);
        float pit = (Float) cands.get(i)[2];
        Vec3 support = (Vec3) cands.get(i)[3];
        String face = (String) cands.get(i)[4];
        Vec3 g = (Vec3) cands.get(i)[5];

        Object[] ok = tryPlacement(reach, yawUnwrapped, pit, support, face, g);
        if (ok != null) return ok;
    }

    return null;
}

Object[] tryPlacement(double reach, float yaw, float pit, Vec3 expectedSupport, String expectedFace, Vec3 goal) {
    Object[] ray = client.raycastBlock(reach, yaw, pit);
    if (ray == null) return null;
    Vec3 hitGrid = (Vec3) ray[0];
    String faceHit = (String) ray[2];
    if (!hitGrid.floor().equals(expectedSupport.floor())) return null;
    if (!expectedFace.equals(faceHit)) return null;
    Vec3 plc = offsetByFace(hitGrid, faceHit);
    if (!plc.floor().equals(goal.floor())) return null;
    return new Object[]{ ray, yaw, pit };
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

Vec3 getClosestOtherPlayerPos() {
    Entity me = client.getPlayer();
    Vec3 myPos = me.getPosition();
    double myX = myPos.x, myY = myPos.y, myZ = myPos.z;
    double boxSize = 10;
    Vec3 best = null;
    double bestD2 = Double.POSITIVE_INFINITY;

    for (Entity e : world.getPlayerEntities()) {
        if (e == me || e.getNetworkPlayer() == null) continue;

        Vec3 p = e.getPosition();

        double dx = p.x - myX;
        if (dx > boxSize || dx < -boxSize) continue;

        double dy = p.y - myY;
        if (dy > boxSize || dy < -boxSize) continue;

        double dz = p.z - myZ;
        if (dz > boxSize || dz < -boxSize) continue;

        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 < bestD2) {
            bestD2 = d2;
            best = p;
        }
    }

    return best;
}

void drawCircleArc(float centerX, float centerY, float radius, float startAngle, float endAngle, float lineWidth, int color) {
    float r = ((color >> 16) & 0xFF) / 255f;
    float g = ((color >> 8) & 0xFF) / 255f;
    float b = (color & 0xFF) / 255f;
    float a = ((color >> 24) & 0xFF) / 255f;

    gl.push();

    gl.enable(3042);
    gl.enable(2884);
    gl.blend(true);
    gl.texture2d(false);
    gl.lineSmooth(true);
    gl.color(r, g, b, a);
    gl.lineWidth(lineWidth);

    gl.begin(3);
    for (float angle = startAngle; angle <= endAngle; angle += 1) {
        double theta = Math.toRadians(angle + 180);
        float x = (float) (radius * Math.cos(theta)) + centerX;
        float y = (float) (radius * Math.sin(theta)) + centerY;
        gl.vertex2(x, y);
    }
    gl.end();

    gl.disable(3042);
    gl.disable(2884);
    gl.blend(false);
    gl.texture2d(true);
    gl.lineSmooth(false);
    gl.color(1, 1, 1, 1);
    gl.lineWidth(1);
    gl.pop();
}

void drawCircle(float centerX, float centerY, float radius, int segments, float lineWidth, float r, float g, float b, float a) {
    gl.push();

    gl.enable(3042);
    gl.enable(2884);
    gl.blend(true);
    gl.texture2d(false);
    gl.lineSmooth(true);
    gl.color(r, g, b, a);
    gl.lineWidth(lineWidth);

    gl.begin(2);
    for (int i = 0; i <= segments; i++) {
        double theta = 2 * Math.PI * i / segments;
        float x = (float) (radius * Math.cos(theta)) + centerX;
        float y = (float) (radius * Math.sin(theta)) + centerY;
        gl.vertex2(x, y);
    }
    gl.end();

    gl.disable(3042);
    gl.disable(2884);
    gl.blend(false);
    gl.texture2d(true);
    gl.lineSmooth(false);
    gl.color(1, 1, 1, 1);
    gl.lineWidth(1);
    gl.pop();
}

int rgba(int r, int g, int b, int a) {
    return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
}

float lerp(double start, double end, double t) {
    return (float) (start + (end - start) * t);
}

float quadInOutEasing(double t) {
    if (t < 0.5) return (float) (2 * t * t);
    return (float) (-1 + (4 - 2 * t) * t);
}

String key(Vec3 v) {
    int ix = (int) Math.floor(v.x);
    int iy = (int) Math.floor(v.y);
    int iz = (int) Math.floor(v.z);
    return ix + "|" + iy + "|" + iz;
}