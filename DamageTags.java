/* 
    damage indicators that float in the air
    broken in third person and shows in the gui, lazy to fix ask chef to add to the actual client dont ping me
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/DamageTags.java"
*/

Map<String, Float> playerHealth = new HashMap<>();
List<Map<String, Object>> objects = new ArrayList<>();
int green = new Color(0, 255, 0).getRGB(), red = new Color(255, 0, 0).getRGB(), mode, colorMode;
double yOffset, fadeDistance = 2.5, min = 0.7;
float baseScale;
long duration, fadeOutTime = 150;
boolean showHealing, showDamage;

void onLoad() {
    modules.registerButton("Show Healing", true);
    modules.registerButton("Show Damage", true);
    modules.registerSlider("Mode", "", 0, new String[]{ "Hearts", "Health Points"});
    modules.registerSlider("Color", "", 0, new String[]{ "RAG", "Team"});
    modules.registerSlider("Scale", "", 1, 0, 3, 0.1);
    modules.registerSlider("Duration", "s", 1.5, 0, 5, 0.1);
    modules.registerSlider("Y Offset", "", 1.8, -2, 3, 0.1);

    modules.registerButton("Depth", true);
    modules.registerButton("Box", true);
    modules.registerButton("Shadow", true);
}

void onPreUpdate() {
    yOffset = modules.getSlider(scriptName, "Y Offset");
    mode = (int) modules.getSlider(scriptName, "Mode");
    colorMode = (int) modules.getSlider(scriptName, "Color");
    showHealing = modules.getButton(scriptName, "Show Healing");
    showDamage = modules.getButton(scriptName, "Show Damage");
    Vec3 me = render.getPosition();
    long now = client.time();

    for (Entity p : world.getPlayerEntities()) {
        String entityId = String.valueOf(p.entityId) + p.getUUID();

        float hp = p.getHealth() + p.getAbsorption();
        float health = hp / (mode == 0 ? 2 : 1);
        float lastHp = playerHealth.getOrDefault(entityId, hp);
        float lastHealth = lastHp / (mode == 0 ? 2 : 1);

        if (p.isDead()) continue;
        playerHealth.put(entityId, hp);

        if (p.getTicksExisted() < 2 || health == lastHealth || (!showDamage && health < lastHealth) || (!showHealing && health > lastHealth)) continue;

        float difference = health - lastHealth;
        String renderHealth;
        int color = health > lastHealth ? green : red;

        if (colorMode == 1) {
            String teamColorCode = p.getDisplayName().substring(0, 2);
            renderHealth = teamColorCode + formatDoubleStr(util.round((double) difference, 1));
        } else {
            renderHealth = formatDoubleStr(util.round(Math.abs((double) difference), 1));
        }

        if (renderHealth.endsWith("0")) continue;

        Vec3 position = p.getPosition().offset(0, yOffset, 0);

        Map<String, Object> object = new HashMap<>();
        object.put("position", position);
        object.put("time", now);
        object.put("color", color);
        object.put("health", renderHealth);
        object.put("distance", position.distanceTo(me));
        object.put("lastdistance", object.get("distance"));
        objects.add(object);
    }

    if (!objects.isEmpty()) {
        duration = (long) (modules.getSlider(scriptName, "Duration") * 1000);
        baseScale = (float) modules.getSlider(scriptName, "Scale");

        for (Map<String, Object> object : objects) {
            object.put("lastdistance", object.get("distance"));
            Vec3 markerPosition = (Vec3) object.get("position");
            double distance = me.distanceTo(markerPosition);
            object.put("distance", distance);
        }

        objects.sort((a, b) -> Double.compare((double) b.get("distance"), (double) a.get("distance")));
    }
}

void onRenderWorld(float partialTicks) {
    long now = client.time();
    int size = client.getDisplaySize()[2];

    for (Iterator<Map<String, Object>> it = objects.iterator(); it.hasNext();) {
        Map<String, Object> object = it.next();
        long elapsed = now - (long) object.get("time");

        if (elapsed > duration + fadeOutTime) {
            it.remove();
            continue;
        }

        double distance = (double) object.get("distance");
        if (distance > 25) continue;

        double lastDistance = (double) object.get("lastdistance");
        double interpolatedDistance = lastDistance + (distance - lastDistance) * partialTicks;

        Vec3 position = (Vec3) object.get("position");
        int color = (int) object.get("color");
        String health = (String) object.get("health");

        int alpha = 255;
        if (elapsed > duration) {
            double fadeFactor = 1 - ((double) (elapsed - duration) / fadeOutTime);
            alpha = (int) (230 * fadeFactor + 25);
        }

        if (alpha < 26) {
            it.remove();
            continue;
        }

        if (interpolatedDistance < fadeDistance) {
            double scaledDistance = (interpolatedDistance - min) / (fadeDistance - min);
            int proximityAlpha = (int) (25 + (230 * Math.max(scaledDistance, 0)));
            alpha = Math.min(alpha, proximityAlpha);
        }

        color = (color & 0x00FFFFFF) | (alpha << 24);

        render.text3d(health, position, baseScale, modules.getButton(scriptName, "Shadow"), modules.getButton(scriptName, "Depth"), modules.getButton(scriptName, "Box"), color);
    }
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        objects.clear();
        playerHealth.clear();
    }
}

String formatDoubleStr(double val) {
    return val == (long) val ? Long.toString((long) val) : Double.toString(val);
}