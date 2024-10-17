/* 
    renders candy in hypixel halloween simulator
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/HalloweenSim.java"
*/

List<Vec3> skullList = new ArrayList<>();
int orange = new Color(255, 100, 0).getRGB(),
    green =  Color.GREEN.getRGB(),
    yellow = Color.YELLOW.getRGB(),
    red = Color.RED.getRGB();
boolean sentPacket;
double range;
boolean looking = false;
float serverYaw = client.getPlayer().getYaw(), serverPitch = client.getPlayer().getPitch();

void onLoad() {
    modules.registerButton("aura", false);
    modules.registerButton("rotations", false);
    modules.registerSlider("range", "", 5, 1, 10, 0.5);
}

void onPreMotion(PlayerState state) {
    if (!checkStatus()) return;

    Entity player = client.getPlayer();
    double threshold = modules.getSlider(scriptName, "range") * 1.5;
    double reach = Math.pow(modules.getSlider(scriptName, "range"), 2);
    range = modules.getSlider(scriptName, "range");

    Vec3 playerPos = player.getPosition();
    playerPos.y += 1.62;
    sentPacket = false;
    skullList.clear();
    boolean shouldRotate = modules.getButton(scriptName, "rotations");

    for (TileEntity tileEntity : client.getWorld().getTileEntities()) {
        if (!tileEntity.name.equals("skull")) continue;

        Object[] skullData = tileEntity.getSkullData();
        if (skullData == null || skullData[2] == null) continue;

        Vec3 skullPosition = tileEntity.getPosition();
        double distanceSq = playerPos.distanceToSq(skullPosition);

        if (modules.getButton(scriptName, "aura") && !sentPacket && distanceSq < reach) {
            if (Math.abs(playerPos.x - skullPosition.x) < threshold 
                && Math.abs(playerPos.y - skullPosition.y) < threshold 
                && Math.abs(playerPos.z - skullPosition.z) < threshold) {

                if (shouldRotate) {
                    float[] rotations = getRotations(skullPosition);
                    float unwrappedYaw = unwrapYaw(rotations[0]);

                    float deltaYaw = unwrappedYaw - serverYaw;
                    float deltaPitch = rotations[1] - serverPitch;

                    state.yaw = serverYaw + (Math.abs(deltaYaw) >= 0.1 ? deltaYaw : 0);
                    state.pitch = serverPitch + (Math.abs(deltaPitch) >= 0.1 ? deltaPitch : 0);

                    if (!looking) {
                        client.sendPacketNoEvent(new C08(player.getHeldItem(), skullPosition, 1, new Vec3(0.5, 0.5, 0.5)));
                        looking = true;
                    } else {
                        looking = false;
                    }
                } else {
                    client.sendPacketNoEvent(new C08(player.getHeldItem(), skullPosition, 1, new Vec3(0.5, 0.5, 0.5)));
                }

                sentPacket = true;
            }
        }

        synchronized (skullList) {
            skullList.add(skullPosition);
        }
    }
}

void onRenderWorld(float partialTicks) {
    Vec3 playerPosition = client.getPlayer().getPosition();

    synchronized (skullList) {
        for (Vec3 skullPosition : skullList) {
            int diffX = (int) Math.abs(skullPosition.x - playerPosition.x);
            int diffY = (int) Math.abs(skullPosition.y - playerPosition.y);
            int diffZ = (int) Math.abs(skullPosition.z - playerPosition.z);

            int color;
            if (diffX > 40 || diffY > 40 || diffZ > 40) {
                color = red;
            } else if (diffX > 20 || diffY > 20 || diffZ > 20) {
                color = orange;
            } else if (diffX > range || diffY > range || diffZ > range) {
                color = yellow;
            } else {
                color = green;
            }

            render.block(skullPosition, color, false, true);
        }
    }
}

void onWorldJoin(Entity entity) {
    if (entity == client.getPlayer()) {
        skullList.clear();
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet.name.startsWith("C05") || packet.name.startsWith("C06")) {
        C03 c03 = (C03) packet;
        serverYaw = c03.yaw;
        serverPitch = c03.pitch;
    }
    return true;
}

void onDisable() {
    skullList.clear();
}

boolean checkStatus() {
    List<String> sidebar = client.getWorld().getScoreboard();
    return sidebar != null && util.strip(sidebar.get(0)).startsWith("HALLOWEEN SIMULATOR");
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
    return new float[]{ yaw, pitch };
}

float unwrapYaw(float yaw) {
    int scaleFactor = (int) Math.floor(serverYaw / 360);
    float unwrappedYaw = yaw + 360 * scaleFactor;

    if (unwrappedYaw < serverYaw - 180) {
        unwrappedYaw += 360;
    } else if (unwrappedYaw > serverYaw + 180) {
        unwrappedYaw -= 360;
    }
    return unwrappedYaw;
}