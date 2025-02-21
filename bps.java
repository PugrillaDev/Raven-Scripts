List<Vec3> positions = new ArrayList<>();
int tickRange = 20;
boolean showXZOnly = false;

void onLoad() {
    modules.registerSlider("Tick Range", "", 20, 2, 50, 1);
    modules.registerButton("Show XZ Only", false);
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    Vec3 currentPos = player.getPosition();
    positions.add(currentPos);
    tickRange = (int) modules.getSlider(scriptName, "Tick Range");
    showXZOnly = modules.getButton(scriptName, "Show XZ Only");
    while (positions.size() > tickRange) {
        positions.remove(0);
    }
}

void onRenderTick(float partialTicks) {
    if (positions.size() < 2 || !client.getScreen().isEmpty()) return;

    double totalDistanceXYZ = 0;
    double totalDistanceXZ = 0;

    for (int i = 1; i < positions.size(); i++) {
        Vec3 prev = positions.get(i - 1);
        Vec3 current = positions.get(i);

        totalDistanceXYZ += distanceXYZ(prev, current);
        totalDistanceXZ += distanceXZ(prev, current);
    }

    double averageBPS = totalDistanceXYZ / (positions.size() - 1) * 20;
    double horizontalBPS = totalDistanceXZ / (positions.size() - 1) * 20;
    int baseX = 1;
    int baseY = client.getDisplaySize()[1] - 1 - render.getFontHeight();

    render.text2d("bps: " + util.round(averageBPS, 1), baseX, baseY, 1, 0xFFFFFF, true);
    if (showXZOnly) {
        render.text2d("hbps: " + util.round(horizontalBPS, 1), baseX, baseY - render.getFontHeight() - 1, 1, 0xFFFFFF, true);
    }
}


double distanceXYZ(Vec3 a, Vec3 b) {
    return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2) + Math.pow(a.z - b.z, 2));
}

double distanceXZ(Vec3 a, Vec3 b) {
    return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.z - b.z, 2));
}
