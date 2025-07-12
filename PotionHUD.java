String CFG_KEY = "pug_potion_hud";
int formatMode;
int sortMode;
int alignH;
int alignV;
int prevAlignH = -1;
int prevAlignV = -1;
float relAX, relAY;
boolean dragging, prevDown, permanent;
float dragDX, dragDY;
List<Object[]> rows = new ArrayList<>();
int lineHeight, stackHeight, maxWidth;
float boundingBoxLeft, boundingBoxTop, boundingBoxRight, boundingBoxBottom;
Map<String, Map<String, Object>> potions = new HashMap<>();

void onLoad() {
    modules.registerSlider("Time Format", "", formatMode, new String[]{ "1m20s", "01:20" });
    modules.registerSlider("Sort By", "", sortMode, new String[]{ "Duration", "Length" });
    modules.registerSlider("Alignment", "", alignH, new String[]{ "Left", "Center", "Right" });
    modules.registerSlider("Direction", "", alignV, new String[]{ "Descending", "Ascending" });
    modules.registerButton("Exclude Permanent", false);
    modules.registerButton("Reset Position", false);

    String pos = config.get(CFG_KEY);
    if (pos != null && !pos.isEmpty()) {
        try {
            String[] p = pos.split(",");
            relAX = Float.parseFloat(p[0]);
            relAY = Float.parseFloat(p[1]);
            return;
        } catch (Exception ignore) { }
    }
    relAX = relAY = .5f;
}

void savePos() {
    config.set(CFG_KEY, relAX + "," + relAY);
}

String fmtTime(int ticks) {
    int s = (ticks + 19) / 20;
    int m = s / 60;
    s %= 60;
    return formatMode == 0 ? (m > 0 ? m + "m" : "") + s + "s" : String.format("%02d:%02d", m, s);
}

Object[] buildEntry(Map<String, Object> d) {
    String n = (String) d.get("name");
    int lv = ((Integer) d.get("level")) + 1;
    int dur = (Integer) d.get("duration");
    int col = (Integer) d.get("color");
    String left = n + (lv > 1 ? " " + lv : "");
    String right = fmtTime(dur);
    int w = render.getFontWidth(left + " " + right);
    return new Object[] { left, right, dur, col, w };
}

void refreshSettings() {
    formatMode = (int) modules.getSlider(scriptName, "Time Format");
    sortMode = (int) modules.getSlider(scriptName, "Sort By");
    alignH = (int) modules.getSlider(scriptName, "Alignment");
    alignV = (int) modules.getSlider(scriptName, "Direction");
    permanent = modules.getButton(scriptName, "Exclude Permanent");
}

void onPreUpdate() {
    refreshSettings();
    if (prevAlignH == -1 || prevAlignV == -1) {
        prevAlignH = alignH;
        prevAlignV = alignV;
    }

    if (modules.getButton(scriptName, "Reset Position")) {
        relAX = relAY = .5f;
        modules.setButton(scriptName, "Reset Position", false);
        savePos();
    }

    potions.clear();
    List<Object[]> raw = client.getPlayer().getPotionEffects();
    if (raw != null)
        for (Object[] e : raw) {
            int dur = (Integer) e[3];
            if (permanent && dur > 32000) continue;
            int id = (Integer) e[0];
            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("name", getPotionName(id));
            m.put("level", (Integer) e[2]);
            m.put("duration", (Integer) e[3]);
            m.put("color", getPotionColor(id));
            potions.put((String) m.get("name"), m);
        }

    rows.clear();
    if (!potions.isEmpty()) {
        for (Map<String, Object> v : potions.values()) rows.add(buildEntry(v));
        if (sortMode == 0)
            rows.sort((a, b) -> ((Integer) b[2]).compareTo((Integer) a[2]));
        else if (sortMode == 1)
            rows.sort((a, b) -> ((Integer) b[4]).compareTo((Integer) a[4]));
    }

    lineHeight = render.getFontHeight() + 1;
    stackHeight = rows.size() * lineHeight;
    maxWidth = 0;
    for (Object[] r : rows) maxWidth = Math.max(maxWidth, (Integer) r[4]);
}

void onRenderTick(float pt) {
    String screen = client.getScreen();
    boolean editing = screen.equals("ClickGui");
    if (!editing) dragging = prevDown = false;

    if (!editing && !screen.isEmpty()) return;

    int[] size = client.getDisplaySize();
    float width = size[0], height = size[1], scale = size[2];
    int[] mp = keybinds.getMousePosition();
    float mouseX = mp[0] / scale, mouseY = height - mp[1] / scale;

    boolean hadPlaceholder = false;
    if (editing && rows.isEmpty()) {
        rows.add(new Object[]{ "No Potions Active", "", 0, 0xFFFFFFFF, render.getFontWidth("No Potions Active"), 18 });
        hadPlaceholder = true;
        stackHeight = lineHeight;
        maxWidth = (Integer) rows.get(0)[4];
    }

    if (alignH != prevAlignH) {
        float axPrev = relAX * width;
        float leftPrev = prevAlignH == 0 ? axPrev : prevAlignH == 2 ? axPrev - maxWidth : axPrev - maxWidth / 2f;
        float axNew = alignH == 0 ? leftPrev : alignH == 2 ? leftPrev + maxWidth : leftPrev + maxWidth / 2f;
        relAX = axNew / width;
    }
    prevAlignH = alignH;

    if (alignV != prevAlignV) {
        float ayPrev = relAY * height;
        float top = prevAlignV == 0 ? ayPrev : ayPrev - stackHeight;
        relAY = (alignV == 0 ? top : top + stackHeight) / height;
    }
    prevAlignV = alignV;

    float ax = relAX * width;
    float ay = relAY * height;
    boolean bottomUp = alignV == 1;
    float firstY = bottomUp ? ay - lineHeight : ay;

    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

    for (int i = 0; i < rows.size(); i++) {
        int rowW = (Integer) rows.get(i)[4];
        float y = firstY + i * lineHeight * (bottomUp ? -1 : 1);
        float x = alignH == 0 ? ax : alignH == 2 ? ax - rowW : ax - rowW / 2f;
        if (x < minX) minX = x;
        if (x + rowW > maxX) maxX = x + rowW;
        if (y < minY) minY = y;
        if (y + lineHeight > maxY) maxY = y + lineHeight;
    }
    boundingBoxLeft = minX; boundingBoxTop = minY; boundingBoxRight = maxX; boundingBoxBottom = maxY;

    boolean down = editing && keybinds.isMouseDown(0);
    if (!down && dragging) savePos();
    if (down && !prevDown && !dragging && mouseX >= boundingBoxLeft && mouseX <= boundingBoxRight && mouseY >= boundingBoxTop && mouseY <= boundingBoxBottom) {
        dragging = true;
        dragDX = mouseX - ax;
        dragDY = mouseY - ay;
    }
    if (!down) dragging = false;

    if (dragging) {
        ax = mouseX - dragDX;
        ay = mouseY - dragDY;
        relAX = ax / width;
        relAY = ay / height;

        firstY = bottomUp ? ay - lineHeight : ay;
        boundingBoxLeft = ax + (alignH == 0 ? 0 : alignH == 2 ? -maxWidth : -maxWidth / 2f);
        boundingBoxRight = boundingBoxLeft + maxWidth;
        boundingBoxTop = firstY + (bottomUp ? -(rows.size() - 1) * lineHeight : 0);
        boundingBoxBottom = boundingBoxTop + stackHeight;
    }
    prevDown = down;

    for (int i = 0; i < rows.size(); i++) {
        Object[] r = rows.get(i);
        String left = (String) r[0];
        String time = (String) r[1];
        int col = (Integer) r[3];
        int rowW = (Integer) r[4];
        float y = firstY + i * lineHeight * (bottomUp ? -1 : 1);
        float x = alignH == 0 ? ax : alignH == 2 ? ax - rowW : ax - rowW / 2f;
        render.text2d(left + " " + util.colorSymbol + "7" + time, x, y, 1f, col, true);
    }

    if (editing) {
        float pad = 1f, l = boundingBoxLeft - pad, r = boundingBoxRight + pad, t = boundingBoxTop - pad, b = boundingBoxBottom;
        int wht = 0xFFFFFFFF;
        render.line2D(l, t, r, t, 1f, wht);
        render.line2D(l, b, r, b, 1f, wht);
        render.line2D(l, t, l, b, 1f, wht);
        render.line2D(r, t, r, b, 1f, wht);
    }

    if (!editing && hadPlaceholder) rows.clear();
}

String getPotionName(int id) {
    switch (id) {
        case 1: return "Speed";
        case 2: return "Slowness";
        case 3: return "Haste";
        case 4: return "Mining Fatigue";
        case 5: return "Strength";
        case 6: return "Instant Health";
        case 7: return "Instant Damage";
        case 8: return "Jump Boost";
        case 9: return "Nausea";
        case 10: return "Regeneration";
        case 11: return "Resistance";
        case 12: return "Fire Resistance";
        case 13: return "Water Breathing";
        case 14: return "Invisibility";
        case 15: return "Blindness";
        case 16: return "Night Vision";
        case 17: return "Hunger";
        case 18: return "Weakness";
        case 19: return "Poison";
        case 20: return "Wither";
        case 21: return "Health Boost";
        case 22: return "Absorption";
        case 23: return "Saturation";
        case 24: return "Glowing";
        case 25: return "Levitation";
        case 26: return "Luck";
        case 27: return "Bad Luck";
        case 28: return "Slow Falling";
        case 29: return "Conduit Power";
        case 30: return "Dolphin's Grace";
        case 31: return "Bad Omen";
        case 32: return "Hero of the Village";
        default: return "Effect " + id;
    }
}

int getPotionColor(int id) {
    switch (id) {
        case 1: return 0x7CAFC6;
        case 2: return 0x5A6C81;
        case 3: return 0xD9C043;
        case 4: return 0x4A4217;
        case 5: return 0x932423;
        case 6: return 0xF82423;
        case 7: return 0x430A09;
        case 8: return 0x22FF4C;
        case 9: return 0x551D4A;
        case 10: return 0xCD5CAB;
        case 11: return 0x99453A;
        case 12: return 0xE49A3A;
        case 13: return 0x2E5299;
        case 14: return 0x7F8392;
        case 15: return 0x1F1F23;
        case 16: return 0x1F1FA1;
        case 17: return 0x587653;
        case 18: return 0x484D48;
        case 19: return 0x4E9331;
        case 20: return 0x352A27;
        case 21: return 0xF87D23;
        case 22: return 0xFFB653;
        case 23: return 0xF82423;
        case 24: return 0x94A3A3;
        case 25: return 0xCEFFFF;
        case 26: return 0x339900;
        case 27: return 0x555555;
        case 28: return 0xE0FFFF;
        case 29: return 0x2E5299;
        case 30: return 0x76DDF7;
        case 31: return 0x0E0B0B;
        case 32: return 0x44FF00;
        default: return 0xFFFFFF;
    }
}