/* 
    aimbot/anti recoil for hypixel cops n crims
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/cops.java"
*/

List<Entity> targets = new ArrayList<>();
int targetColor = new Color(255, 0, 0).getRGB(), hitcolor = new Color(0, 255, 0).getRGB();
int predictionTicks = 1;
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
    "planks",
    "coal_ore",
    "emerald_ore",
    "wooden_slab",
    "iron_ore",
    "stained_glass_pane",
    "diamond_ore",
    "iron_bars",
    "spruce_stairs",
    "fire"
));
Map<String, List<Double[]>> recoilValues = new HashMap<>();
int recoil = 0, aim = 0, lastStackSize = 1, lastDurability = 100;
String teamColor = "";

double hitboxScale = 1.0;
double width = 0.6 * hitboxScale;
double height = 1.8 * hitboxScale;
double halfWidth = width / 2d;
double halfHeight = height / 2d;

void onLoad() {
    modules.registerButton("Hold Right Click", true);
    modules.registerButton("Spin Bot", false);
    modules.registerButton("Silent Aim", true);
    modules.registerButton("No Recoil", true);
    modules.registerSlider("Prediction Ticks", "", 3, 0, 10, 1);

    recoilValues.put("wooden_pickaxe", Arrays.asList(
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.33, 12d},
        new Double[]{0.29, 12d},
        new Double[]{0.27, 12d},
        new Double[]{0.25, 12d}
    ));

    recoilValues.put("stone_hoe", Arrays.asList(
        new Double[]{1.2, 5d},
        new Double[]{1.2, 5d}
    ));

    recoilValues.put("iron_axe", Arrays.asList(
        new Double[]{1.53, 3d},
        new Double[]{1.53, 3d}
    ));

    recoilValues.put("golden_pickaxe", Arrays.asList(
        new Double[]{0.82, 7d},
        new Double[]{0.82, 7d},
        new Double[]{0.82, 7d},
        new Double[]{0.82, 7d},
        new Double[]{0.82, 7d},
        new Double[]{0.72, 7d}
    ));

    recoilValues.put("golden_shovel", Arrays.asList(
        new Double[]{1.0, 7d},
        new Double[]{1.4, 7d},
        new Double[]{1.2, 7d},
        new Double[]{1.1, 7d},
        new Double[]{0.62, 7d}
    ));
}

void onPreMotion(PlayerState s) {
    String targetName = target != null ? target.getName() : "";
    target = null;
    aimPoint = null;
    Entity player = client.getPlayer();
    ItemStack heldItem = player.getHeldItem();
    String itemName = heldItem != null ? heldItem.name : "";
    int stackSize = heldItem != null ? heldItem.stackSize : 1;
    int durability = heldItem != null ? heldItem.durability : 1;
    int maxDurability = heldItem != null ? heldItem.maxDurability : 1;
    int ticksExisted = player.getTicksExisted();
    predictionTicks = (int) modules.getSlider(scriptName, "Prediction Ticks") + 1;

    if (ticksExisted % 20 == 0) {
        getTeamColor();
    }

    if (!recoilValues.containsKey(itemName) || !player.onGround()) {
        recoil = aim = 0;
        return;
    }

    if (stackSize < lastStackSize || durability < lastDurability) {
        recoil++;
    }

    if (durability != lastDurability && durability > lastDurability) {
        recoil = 0;
    }

    lastStackSize = stackSize;
    lastDurability = durability;

    boolean manual = modules.getButton(scriptName, "Hold Right Click");
    boolean doAiming = !manual || (manual && keybinds.isMouseDown(1));

    if (doAiming) {
        List<Entity> closest = getClosestEntities(3);
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
            if (target == null) recoil = 0;
        }

        boolean silentAim = target != null && modules.getButton(scriptName, "Silent Aim");
        float[] rotations = new float[]{ player.getYaw(), player.getPitch() };
        if (silentAim) rotations = getRotations(aimPoint);
        
        if (modules.getButton(scriptName, "No Recoil")) {
            List<Double[]> recoilList = recoilValues.getOrDefault(itemName, new ArrayList<>());
            int index = Math.min(recoilList.size(), recoil);
            
            if (index > 0) {
                Double[] recoilVals = recoilList.get(index - 1);
                rotations[1] += recoilVals[0].floatValue() * (float)Math.min(recoilVals[1].intValue(), recoil);
                //client.print("Value: " + recoilVals[0] + " Recoil: " + recoil);
            }
        }

        int scaleFactor = (int) Math.floor(serverYaw / 360);
        float unwrappedYaw = rotations[0] + 360 * scaleFactor;
        if (unwrappedYaw < serverYaw - 180) {
            unwrappedYaw += 360;
        } else if (unwrappedYaw > serverYaw + 180) {
            unwrappedYaw -= 360;
        }

        float deltaYaw = unwrappedYaw - serverYaw;
        float deltaPitch = rotations[1] - serverPitch;
        
        if (silentAim) {
            s.yaw = serverYaw + (Math.abs(deltaYaw) >= 0.1 ? deltaYaw : 0);
            s.pitch = serverPitch + (Math.abs(deltaPitch) >= 0.1 ? deltaPitch : 0);
        }

        if (target == null && modules.getButton(scriptName, "Spin Bot")) {
            s.yaw = (float) util.randomDouble(-180, 180);
            s.pitch = (float) util.randomDouble(-90, 90);
        }

        if (target != null && aim++ > 0 && !sentAlready && durability == maxDurability) {
            client.sendPacketNoEvent(new C08(heldItem, new Vec3(-1, -1, -1), 255, new Vec3(0.0, 0.0, 0.0)));
        }
    } else {
        recoil = aim = 0;
    }
}

boolean onMouse(int button, boolean state) {
    if (!modules.getButton(scriptName, "Hold Right Click")) return true;
    if (button != 1 || !state) return true;

    Entity player = client.getPlayer();
    ItemStack heldItem = player.getHeldItem();
    String itemName = heldItem != null ? heldItem.name : "";

    return !recoilValues.containsKey(itemName);
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

void onPostMotion() {
    sentAlready = false;
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

void getTeamColor() {
    List<String> lines = client.getWorld().getScoreboard();
    if (lines.size() < 13) return;
    String line = lines.get(12);
    if (!line.contains("Team: ")) return;
    teamColor = line.split("Team: ")[1].substring(0, 2);
}

List<Entity> getClosestEntities(int numberOfEntities) {
    Entity player = client.getPlayer();

    List<Object[]> entityDistances = new ArrayList<>();
    for (Entity entity : client.getWorld().getPlayerEntities()) {
        if ((entity.getUUID().charAt(14) != '4' && entity.getUUID().charAt(14) != '1') || entity.isInvisible() || entity.getNetworkPlayer() == null || entity == player || entity.getDisplayName().startsWith(teamColor)) continue;
        double distanceSq = player.getPosition().distanceToSq(entity.getPosition());
        entityDistances.add(new Object[]{entity, distanceSq});
    }

    for (int i = 0; i < entityDistances.size() - 1; i++) {
        for (int j = 0; j < entityDistances.size() - i - 1; j++) {
            if ((double) entityDistances.get(j)[1] > (double) entityDistances.get(j + 1)[1]) {
                Object[] temp = entityDistances.get(j);
                entityDistances.set(j, entityDistances.get(j + 1));
                entityDistances.set(j + 1, temp);
            }
        }
    }

    List<Entity> closestEntities = new ArrayList<>();
    for (int i = 0; i < Math.min(numberOfEntities, entityDistances.size()); i++) {
        closestEntities.add((Entity) entityDistances.get(i)[0]);
    }

    return closestEntities;
}

double interpolate(double current, double old, double scale) {
    return old + (current - old) * scale;
}

Object[] canHitEntity(Entity p) {
    World world = client.getWorld();
    Vec3[] boundingBox = getPredictedBoundingBox(p);
    Vec3 pos = boundingBox[0].offset(halfWidth, 0, halfWidth);

    Vec3[] offsets = {
        new Vec3(0, p.getEyeHeight(), 0),
        new Vec3(halfWidth, halfHeight, 0),
        new Vec3(0, halfHeight, halfWidth),
        new Vec3(-halfWidth, halfHeight, 0),
        new Vec3(0, halfHeight, -halfWidth),
        new Vec3(0, height, 0),
        new Vec3(0, 0, 0)
    };

    for (Vec3 offset : offsets) {
        Vec3 targetPos = pos.offset(offset.x, offset.y, offset.z);
        if (isRaycastHit(targetPos, boundingBox)) {
            return new Object[] { true, targetPos };
        }
    }

    return new Object[] { false };
}

boolean isRaycastHit(Vec3 targetPos, Vec3[] boundingBox) {
    World world = client.getWorld();
    float[] rots = getRotations(targetPos);
    List<Vec3> path = raycastPath(100, rots[0], rots[1], 0.2);
    for (Vec3 pos : path) {
        Block block = world.getBlockAt(pos);
        if (isWithinBoundingBox(pos, boundingBox)) return true;
        if (ignoreBlocks.contains(block.name)) continue;
        return false;
    }
    return false;
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