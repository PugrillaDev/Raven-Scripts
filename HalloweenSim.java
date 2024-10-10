List<Vec3> skullList = new ArrayList<>();
int orange = new Color(255, 100, 0).getRGB(),
    green =  Color.GREEN.getRGB(),
    yellow = Color.YELLOW.getRGB(),
    red = Color.RED.getRGB();
boolean sentPacket;
double range;
boolean looking = false;

void onLoad() {
    modules.registerButton("aura", false);
    modules.registerButton("rotations", false);
    modules.registerSlider("range", "", 5, 1, 10, 0.5);
}

void onPreMotion(PlayerState state) {
    if (!grinching()) return;
    Entity player = client.getPlayer();
    double threshold = modules.getSlider(scriptName, "range") * 1.5;
    double reach = Math.pow(modules.getSlider(scriptName, "range"), 2);
    range = modules.getSlider(scriptName, "range");
    Vec3 plapos = player.getPosition();
    plapos.y += 1.62;
    sentPacket = false;
    skullList.clear();
    boolean shouldRotate = modules.getButton(scriptName, "rotations");
    for (TileEntity tileEntity : client.getWorld().getTileEntities()) {
        if (!tileEntity.name.equals("skull")) continue;
        Object[] skullData = tileEntity.getSkullData();
        if (skullData == null || skullData[2] == null) continue;
        Vec3 skullPosition = tileEntity.getPosition();
        if (modules.getButton(scriptName, "aura")) {
            if (!sentPacket && isWithinThreshold(plapos, skullPosition, threshold)) {
                if (plapos.distanceToSq(skullPosition) < reach) {
                    if (shouldRotate) {
                        if (looking) {
                            client.sendPacketNoEvent(new C08(player.getHeldItem(), skullPosition, 1, new Vec3(0.5, 0.5, 0.5)));
                            float[] rots = client.getRotations(skullPosition);
                            state.yaw = rots[0];
                            state.pitch = rots[1];
                            looking = false;
                        } else {
                            float[] rots = client.getRotations(skullPosition);
                            state.yaw = rots[0];
                            state.pitch = rots[1];
                            looking = true;
                        }
                    } else {
                        client.sendPacketNoEvent(new C08(player.getHeldItem(), skullPosition, 1, new Vec3(0.5, 0.5, 0.5)));
                    }
                    sentPacket = true;
                }
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

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        skullList.clear();
    }
}

void onDisable() {
    skullList.clear();
}

boolean isWithinThreshold(Vec3 playerPos, Vec3 skullPos, double threshold) {
    return Math.abs(playerPos.x - skullPos.x) < threshold 
        && Math.abs(playerPos.z - skullPos.z) < threshold 
        && Math.abs(playerPos.y - skullPos.y) < threshold;
}

boolean grinching() {
    List<String> sidebar = client.getWorld().getScoreboard();
    return sidebar != null && util.strip(sidebar.get(0)).startsWith("HALLOWEEN SIMULATOR");
}