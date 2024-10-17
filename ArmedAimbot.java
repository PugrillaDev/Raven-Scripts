/* 
    aimbot for armed bedwars dreams mode
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/ArmedAimbot.java"
*/

int targetColor = new Color(255, 0, 0).getRGB();
int predictionTicks = 1;
int aim = 0;
float serverYaw = client.getPlayer().getYaw(), serverPitch = client.getPlayer().getPitch();
Entity target = null;
Vec3 aimPoint = null;
boolean sentAlready = false;
HashSet ignoreBlocks = new HashSet<>(Arrays.asList(
    "wall_sign",
    "torch",
    "air",
    "ladder",
    "wheat",
    "fire",
    "water",
    "grass",
    "double_plant",
    "tallgrass"
));
HashSet<String> guns = new HashSet<>(Arrays.asList(
    "golden_hoe",
    "stone_hoe",
    "diamond_hoe",
    "flint_and_steel",
    "iron_hoe",
    "wooden_hoe"
));

double hitboxScale = 1.3;
double width = 0.6 * hitboxScale;
double height = 1.8 * hitboxScale;
double halfWidth = width / 2d;
double halfHeight = height / 2d;

void onLoad() {
    modules.registerButton("Hold Right Click", true);
    modules.registerButton("Spin Bot", false);
    modules.registerSlider("Prediction", " ticks", 3, 0, 10, 1);
}

void onPreMotion(PlayerState s) {
    if (!client.getScreen().isEmpty() || getBedwarsStatus() != 3) return;
    String targetName = target != null ? target.getName() : "";
    target = null;
    aimPoint = null;
    Entity player = client.getPlayer();
    ItemStack heldItem = player.getHeldItem();
    String itemName = heldItem != null ? heldItem.name : "";
    int durability = heldItem != null ? heldItem.durability : 1;
    int maxDurability = heldItem != null ? heldItem.maxDurability : 1;
    int ticksExisted = player.getTicksExisted();
    predictionTicks = (int) modules.getSlider(scriptName, "Prediction") + 1;

    boolean manual = modules.getButton(scriptName, "Hold Right Click");
    boolean doAiming = durability == maxDurability && guns.contains(itemName) && (!manual || (manual && keybinds.isMouseDown(1)));

    if (!doAiming) {
        aim = 0;
        return;
    }

    List<Entity> closest = getClosestEntities(4);
    for (Entity p : closest) {
        Object[] hit = canHitEntity(p);
        boolean successful = Boolean.parseBoolean(hit[0].toString());
        if (!successful) continue;
        target = p;
        aimPoint = (Vec3) hit[1];
        break;
    }

    String newTargetName = target != null ? target.getName() : "";
    if (target == null || !targetName.equals(newTargetName)) {
        aim = 0;
    }

    float[] rotations = new float[]{ player.getYaw(), player.getPitch() };
    if (target != null) rotations = getRotations(aimPoint);

    int scaleFactor = (int) Math.floor(serverYaw / 360);
    float unwrappedYaw = rotations[0] + 360 * scaleFactor;
    if (unwrappedYaw < serverYaw - 180) {
        unwrappedYaw += 360;
    } else if (unwrappedYaw > serverYaw + 180) {
        unwrappedYaw -= 360;
    }

    float deltaYaw = unwrappedYaw - serverYaw;
    float deltaPitch = rotations[1] - serverPitch;
    
    if (target != null) {
        s.yaw = serverYaw + (Math.abs(deltaYaw) >= 0.1 ? deltaYaw : 0);
        s.pitch = serverPitch + (Math.abs(deltaPitch) >= 0.1 ? deltaPitch : 0);

        if (aim++ > 0 && !sentAlready && durability == maxDurability) {
            client.sendPacketNoEvent(new C08(heldItem, new Vec3(-1, -1, -1), 255, new Vec3(0.0, 0.0, 0.0)));
        }
    } else if (modules.getButton(scriptName, "Spin Bot")) {
        s.yaw = (float) util.randomDouble(-180, 180);
        s.pitch = (float) util.randomDouble(-90, 90);
    }
}

boolean onMouse(int button, boolean state) {
    if (!modules.getButton(scriptName, "Hold Right Click")) return true;
    if (button != 1 || !state) return true;

    Entity player = client.getPlayer();
    ItemStack heldItem = player.getHeldItem();
    String itemName = heldItem != null ? heldItem.name : "";

    return !guns.contains(itemName);
}

void onRenderTick(float partialTicks) {
    if (target == null || !render.isInView(target)) return;
    double scale = partialTicks;
    int size = client.getDisplaySize()[2];

    Vec3 screen = render.worldToScreen(aimPoint.x, aimPoint.y, aimPoint.z, size, partialTicks);

    if (screen.z >= 0 && screen.z < 1.0003684d) {
        double crosshairSize = 3;
        double startX = screen.x - crosshairSize;
        double endX = screen.x + crosshairSize;
        double startY = screen.y - crosshairSize;
        double endY = screen.y + crosshairSize;

        render.line2D(startX, screen.y, endX, screen.y, 3.0f, targetColor);
        render.line2D(screen.x, startY, screen.x, endY, 3.0f, targetColor);
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet.name.startsWith("C05") || packet.name.startsWith("C06")) {
        C03 c03 = (C03) packet;
        serverYaw = c03.yaw;
        serverPitch = c03.pitch;
    } else if (packet instanceof C08) {
        sentAlready = true;
    }
    return true;
}

void onPostMotion() {
    sentAlready = false;
}

List<Entity> getClosestEntities(int amount) {
    Entity player = client.getPlayer();
    Vec3 p = player.getPosition();
    String myTeam = player.getNetworkPlayer() != null ? player.getNetworkPlayer().getDisplayName().substring(0, 2) : player.getDisplayName().substring(0, 2);

    List<Object[]> entityDistances = new ArrayList<>();
    for (Entity entity : client.getWorld().getPlayerEntities()) {
        String d = entity.getDisplayName();
        char u = entity.getUUID().charAt(14);
        if (entity == player || entity.getNetworkPlayer() == null || (u != '4' && u != '1') || d.startsWith(myTeam) || (entity.isInvisible() && d.startsWith(util.colorSymbol + "c") && !d.contains(" "))) continue;
        double distanceSq = p.distanceToSq(entity.getPosition());
        entityDistances.add(new Object[]{entity, distanceSq});
    }

    entityDistances.sort(Comparator.comparingDouble(o -> (double) o[1]));

    List<Entity> closestEntities = new ArrayList<>();
    for (int i = 0; i < Math.min(amount, entityDistances.size()); i++) {
        closestEntities.add((Entity) entityDistances.get(i)[0]);
    }

    return closestEntities;
}

double interpolate(double current, double old, double scale) {
    return old + (current - old) * scale;
}

Object[] canHitEntity(Entity p) {
    double range = getCurrentWeaponRange();
    if (range == 0 || p.getPosition().distanceTo(client.getPlayer().getPosition()) > range) return new Object[] { false };

    World world = client.getWorld();
    Vec3[] boundingBox = getPredictedBoundingBox(p);
    Vec3 pos = boundingBox[0].offset(halfWidth, 0, halfWidth);

    Vec3[] offsets = {
        new Vec3(0, 2, 0),
        new Vec3(halfWidth, halfHeight, 0),
        new Vec3(0, halfHeight, halfWidth),
        new Vec3(-halfWidth, halfHeight, 0),
        new Vec3(0, halfHeight, -halfWidth),
        new Vec3(0, height, 0),
        new Vec3(0, 0, 0)
    };

    for (Vec3 offset : offsets) {
        Vec3 targetPos = pos.offset(offset.x, offset.y, offset.z);
        if (isRaycastHit(targetPos, boundingBox, range)) {
            return new Object[] { true, targetPos };
        }
    }

    return new Object[] { false };
}

boolean isRaycastHit(Vec3 targetPos, Vec3[] boundingBox, double range) {
    World world = client.getWorld();
    float[] rots = getRotations(targetPos);
    List<Vec3> path = raycastPath(range, rots[0], rots[1], 0.2);
    for (Vec3 pos : path) {
        Block block = world.getBlockAt(pos);
        if (isWithinBoundingBox(pos, boundingBox)) return true;
        if (ignoreBlocks.contains(block.name)) continue;
        return false;
    }
    return false;
}

double getCurrentWeaponRange() {
    ItemStack item = client.getPlayer().getHeldItem();
    if (item == null || !guns.contains(item.name)) return 0;

    for (String t : item.getTooltip()) {
        String ts = util.strip(t);
        if (!ts.contains("Range: ")) continue;
        double range = Double.parseDouble(ts.split("Range: ")[1]);
        return range;
    }

    return 0;
}

Vec3[] getPredictedBoundingBox(Entity p) {
    Vec3 position = p.getPosition();
    Vec3 lposition = p.getLastPosition();
    Vec3 motion = position.offset(-lposition.x, -lposition.y, -lposition.z);

    boolean inAir = client.getWorld().getBlockAt(position).name.equals("air");
    if (inAir) {
        motion.x *= 0.91;
        motion.z *= 0.91;
    }

    Vec3 predictedPosition = position.offset(motion.x * predictionTicks, motion.y * (predictionTicks / 4), motion.z * predictionTicks);
    Vec3[] boundingBox = { predictedPosition.offset(-halfWidth, 0, -halfWidth), predictedPosition.offset(halfWidth, height, halfWidth) };
    return boundingBox;
}

List<Vec3> raycastPath(double distance, float yaw, float pitch, double step) {
    List<Vec3> positions = new ArrayList<>();

    double yawRad = Math.toRadians(yaw);
    double pitchRad = Math.toRadians(pitch);

    double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
    double dirY = -Math.sin(pitchRad);
    double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

    Entity player = client.getPlayer();
    Vec3 startPos = player.getPosition().offset(0, player.getEyeHeight(), 0);

    for (double i = 0; i <= distance; i += step) {
        positions.add(new Vec3(startPos.x + dirX * i, startPos.y + dirY * i, startPos.z + dirZ * i));
    }

    return positions;
}

boolean isWithinBoundingBox(Vec3 position, Vec3[] boundingBox) {
    Vec3 min = boundingBox[0];
    Vec3 max = boundingBox[1];

    return (position.x >= min.x && position.x <= max.x) &&
           (position.y >= min.y && position.y <= max.y) &&
           (position.z >= min.z && position.z <= max.z);
}

float[] getRotations(Vec3 point) {
    Entity player = client.getPlayer();
    Vec3 pos = player.getPosition().offset(0, player.getEyeHeight(), 0);
    double x = point.x - pos.x;
    double y = point.y - pos.y;
    double z = point.z - pos.z;
    double dist = Math.sqrt(x * x + z * z);
    float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
    float pitch = (float) Math.toDegrees(-Math.atan2(y, dist));

    yaw = ((yaw % 360) + 360) % 360;
    if (yaw > 180) {
        yaw -= 360;
    }
    
    return new float[]{ yaw, pitch };
}

int getBedwarsStatus() {
    World world = client.getWorld();
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
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
    if (lobbyId.charAt(lobbyId.length() - 1) == ']') {
        lobbyId = lobbyId.split(" ")[0];
    }

    if (lobbyId.charAt(0) == 'L') {
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