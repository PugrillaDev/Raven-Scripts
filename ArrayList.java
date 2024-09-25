List<Map<String, Object>> mods = new ArrayList<>();
Map<String, Map<String, String>> customModuleData = new HashMap<>();
List<Map<String, Object>> customDataList = new ArrayList<>();

Color startColor, endColor, staticColor;
int animationDuration;
float moduleHeight;
int colorMode;
float waveSpeed;
int background, direction, animationMode, outlineMode, color;
float gap, lineGap, textScale;
float xOffset, yOffset;

void onLoad() {
    setDataArray("KillAura", "", "Targets", new String[]{"Single", "Single", "Switch"});
    setDataSlider("AntiKnockback", "Velocity", "%v1% %v2%", new String[]{"Horizontal", "Vertical"});
    setDataSlider("Velocity", "", "%v1% %v2%", new String[]{"Horizontal", "Vertical"});
    setDataSlider("FastMine", "", "%v1x", new String[]{"Break speed"});
    setDataArray("NoSlow", "", "Mode", new String[]{"Vanilla", "Float", "Interact", "Invalid", "Jump", "Sneak"});
    setDataArray("NoFall", "", "Mode", new String[]{"Spoof", "Single", "Extra", "NoGround A", "NoGround B", "Precision", "Position"});
    setDataArray("BedAura", "", "Break mode", new String[]{"Legit", "Instant", "Swap"});
    
    // Color settings
    modules.registerSlider("Color 1 - Red", "", 255, 0, 255, 1);
    modules.registerSlider("Color 1 - Green", "", 0, 0, 255, 1);
    modules.registerSlider("Color 1 - Blue", "", 0, 0, 255, 1);
    modules.registerSlider("Color 2 - Red", "", 255, 0, 255, 1);
    modules.registerSlider("Color 2 - Green", "", 255, 0, 255, 1);
    modules.registerSlider("Color 2 - Blue", "", 255, 0, 255, 1);
    modules.registerSlider("Background Opacity", "", 0, 0, 255, 1);

    modules.registerSlider("Mode", "Mode", 0, new String[]{"Static", util.color("&cR&6a&ei&an&bb&do&5w"), util.color("&4G&cr&5a&bd&3i&9e&1n&1t")});
    modules.registerSlider("Direction", "Direction", 1, new String[]{"Up", "Down"});
    modules.registerSlider("Wave Speed", "s", 5, 0.1, 10, 0.1);

    modules.registerSlider("Animations", "", 0, new String[]{"Scale Right", util.color("Scale Center &cWIP")});
    modules.registerSlider("Animation Speed", "ms", 250, 0, 2000, 10);

    modules.registerSlider("Gap", "", 1, 0, 5, 0.1);
    modules.registerSlider("Scale", "", 1, 0.5, 2, 0.1);
    modules.registerSlider("X-Offset", "", 1, 0, 20, 1);
    modules.registerSlider("Y-Offset", "", 1, 0, 20, 1);

    modules.registerSlider("Outline Mode", "", 0, new String[]{util.colorSymbol + "cDisabled", "Left", "Right", util.color("Full &cWIP")});
    modules.registerSlider("Line Gap", "", 2, 0, 5, 0.1);
    
    Map<String, List<String>> categories = modules.getCategories();
    for (String category : categories.keySet()) {
        if (category.equalsIgnoreCase("profiles") || category.equalsIgnoreCase("fun")) continue;

        List<String> modulesList = categories.get(category);
        for (String module : modulesList) {
            modules.registerButton(module, false);
            Map<String, Object> modData = new HashMap<>();
            modData.put("name", module);
            modData.put("visibility", false);
            modData.put("offset", 0);
            modData.put("scale", 0);
            modData.put("animating", false);
            modData.put("animatingUp", false);
            modData.put("animationStart", 0L);
            modData.put("animationProgress", 0);
            mods.add(modData);
        }
    }
    sortModules();
}

void setDataStatic(String moduleName, String alias, String overrideValue) {
    Map<String, Object> customData = new HashMap<>();
    customData.put("moduleName", moduleName);
    customData.put("alias", alias);
    customData.put("overrideValue", overrideValue);
    customData.put("type", "fixed");
    customDataList.add(customData);

    updateCustomData(customData);
}

void setDataSlider(String moduleName, String alias, String displayString, String[] placeholders) {
    Map<String, Object> customData = new HashMap<>();
    customData.put("moduleName", moduleName);
    if (!alias.isEmpty()) customData.put("alias", alias);
    customData.put("displayString", displayString);
    customData.put("placeholders", placeholders);
    customData.put("type", "placeholders");
    customDataList.add(customData);

    updateCustomData(customData);
}

void setDataArray(String moduleName, String alias, String setting, String[] possibleValues) {
    Map<String, Object> customData = new HashMap<>();
    customData.put("moduleName", moduleName);
    if (!alias.isEmpty()) customData.put("alias", alias);
    customData.put("setting", setting);
    customData.put("possibleValues", possibleValues);
    customData.put("type", "strings");
    customDataList.add(customData);

    updateCustomData(customData);
}

void updateCustomData(Map<String, Object> customData) {
    String moduleName = (String) customData.get("moduleName");
    String alias = moduleName;

    if (customData.containsKey("alias")) {
        String customAlias = (String) customData.get("alias");
        if (customAlias != null && !customAlias.isEmpty()) {
            alias = customAlias;
        }
    }

    String overrideValue = "";

    switch ((String) customData.get("type")) {
        case "fixed":
            overrideValue = (String) customData.get("overrideValue");
            break;

        case "placeholders":
            String displayString = (String) customData.get("displayString");
            String[] placeholders = (String[]) customData.get("placeholders");

            for (int i = 0; i < placeholders.length; i++) {
                String placeholder = "%v" + (i + 1);
                String sliderValue = formatDoubleStr(modules.getSlider(moduleName, placeholders[i]));
                displayString = displayString.replace(placeholder, sliderValue);
            }

            overrideValue = displayString;
            break;

        case "strings":
            String setting = (String) customData.get("setting");
            String[] possibleValues = (String[]) customData.get("possibleValues");

            int index = (int) modules.getSlider(moduleName, setting);
            overrideValue = possibleValues[Math.min(index, possibleValues.length - 1)];
            break;
    }

    Map<String, String> data = new HashMap<>();
    data.put("alias", alias);
    data.put("overrideValue", overrideValue);
    customModuleData.put(moduleName, data);
}

void onEnable() {
    updateButtonStates();
    updateSliders();
    sortModules();
}

void onPreUpdate() {
    int ticks = client.getPlayer().getTicksExisted();
    updateEnabledModules();
    gap = (float) modules.getSlider(scriptName, "Gap");
    lineGap = (float) modules.getSlider(scriptName, "Line Gap");
    moduleHeight = (float) render.getFontHeight() + gap;
    xOffset = (float) modules.getSlider(scriptName, "X-Offset");
    yOffset = (float) modules.getSlider(scriptName, "Y-Offset");

    if (ticks % 5 == 0) {
        updateSliders();
    }
}

long color1Edit = 0;
long color2Edit = 0;

void updateSliders() {
    colorMode = (int) modules.getSlider(scriptName, "Mode");
    waveSpeed = (float) modules.getSlider(scriptName, "Wave Speed");
    direction = (int) modules.getSlider(scriptName, "Direction");
    textScale = (float) modules.getSlider(scriptName, "Scale");
    animationDuration = (int) modules.getSlider(scriptName, "Animation Speed");
    animationMode = (int) modules.getSlider(scriptName, "Animations");
    background = new Color(0, 0, 0, (int) Math.floor(modules.getSlider(scriptName, "Background Opacity"))).getRGB();
    outlineMode = (int) modules.getSlider(scriptName, "Outline Mode");

    int color1Red = (int) modules.getSlider(scriptName, "Color 1 - Red");
    int color1Green = (int) modules.getSlider(scriptName, "Color 1 - Green");
    int color1Blue = (int) modules.getSlider(scriptName, "Color 1 - Blue");
    int color2Red = (int) modules.getSlider(scriptName, "Color 2 - Red");
    int color2Green = (int) modules.getSlider(scriptName, "Color 2 - Green");
    int color2Blue = (int) modules.getSlider(scriptName, "Color 2 - Blue");

    if (staticColor == null || color1Red != staticColor.getRed() || color1Green != staticColor.getGreen() || color1Blue != staticColor.getBlue()) {
        if (staticColor != null) color1Edit = client.time() + 5000;
        staticColor = new Color(color1Red, color1Green, color1Blue);
    }

    if (colorMode == 2) {
        startColor = staticColor;
        if (endColor == null || color2Red != endColor.getRed() || color2Green != endColor.getGreen() || color2Blue != endColor.getBlue()) {
            if (endColor != null) color2Edit = client.time() + 5000;
            endColor = new Color(color2Red, color2Green, color2Blue);
        }
    }

    for (Map<String, Object> customData : customDataList) {
        updateCustomData(customData);
    }

    sortModules();
}

void onRenderTick(float partialTicks) {
    int[] displaySize = client.getDisplaySize();
    long now = client.time();

    if (client.getScreen().equals("GuiRaven")) {
        int screenWidth = displaySize[0], rectY = 20, rectSize = 50;
        if ((colorMode == 0 || colorMode == 2) && now < color1Edit) {
            render.roundedRect(screenWidth / 2 - rectSize - 10, rectY, screenWidth / 2 - 10, rectY + rectSize, 10, staticColor.getRGB());
        }
        if (colorMode == 2 && now < color2Edit) {
            render.roundedRect(screenWidth / 2 + 10, rectY, screenWidth / 2 + rectSize + 10, rectY + rectSize, 10, endColor.getRGB());
        }
    }

    float x = xOffset;
    float y = yOffset;
    float displayWidth = displaySize[0] - x;

    updateAnimations();

    long index = 0;
    float previousWidth = 0;
    float prevX = displayWidth;
    float prevY = yOffset;
    float firstY = y;

    for (Map<String, Object> mod : mods) {
        boolean animating = (boolean) mod.get("animating");
        if (!(boolean) mod.get("visibility") && !animating) {
            continue;
        }

        String moduleName = (String) mod.get("name");
        String displayName = moduleName;
        String displayValue = "";

        if (customModuleData.containsKey(moduleName)) {
            Map<String, String> customData = customModuleData.get(moduleName);
            displayName = customData.getOrDefault("alias", moduleName);
            displayValue = customData.getOrDefault("overrideValue", "");
        }

        float scale = (float) mod.get("scale") * textScale;
        String textToDisplay = displayName + (displayValue.isEmpty() ? "" : " " + util.colorSymbol + "7" + displayValue);

        float textWidth = render.getFontWidth(textToDisplay) * textScale;
        float scaledTextWidth = textWidth * scale;
        float finalXPosition;

        switch (animationMode) {
            case 1: // Scale Center (Need formula to work with scaling)
                /* finalXPosition = displayWidth - scaledTextWidth / 2 - x - textWidth / 2;
                break; */
            case 0: // Scale Right
                finalXPosition = displayWidth - (scaledTextWidth / textScale) - x;
                break;
            default:
                finalXPosition = displayWidth - scaledTextWidth - x;
                break;
        }

        float backgroundHeight = render.getFontHeight() * scale;
        float backgroundOffset = index != 0 ? (gap + 1) * textScale : textScale;

        float val1 = y - backgroundOffset;
        float val2 = y + backgroundHeight - scale - (gap + 1) * (textScale - scale);
        /* if (scale != textScale) {
            client.print(val1 + " " + val2);
        } */
        render.rect(finalXPosition - scale, val1, finalXPosition + (scaledTextWidth / textScale), val2, background);
        
        switch (colorMode) {
            case 0: // Static
                color = staticColor.getRGB();
                break;
            case 1: // Rainbow
                color = getRainbow(waveSpeed, 1f, 1f, index);
                break;
            case 2: // Rolling Gradient
                double ratio = getWaveRatio(waveSpeed, index);
                color = blendColors(startColor, endColor, ratio).getRGB();
                break;
            default:
                color = 0xFFFFFF;
        }

        if (index == 0) firstY = y;
        float ty = y - (moduleHeight * scale - render.getFontHeight() * scale);
        float tyoff = (gap + 2) * (1 - scale);
        render.text(textToDisplay, finalXPosition, ty - tyoff, scale, color, true);

        float lineOffset = lineGap * scale;

        switch(outlineMode) {
            case 1: // Left
                render.line2D(finalXPosition - lineOffset, y, finalXPosition - lineOffset, y + backgroundHeight - 2 * scale, 2.5f * scale, color);
                break;
            case 3: // Full
                /* if (index == 0) {
                    render.line2D(finalXPosition - lineOffset, y - backgroundOffset - lineOffset, finalXPosition - lineOffset, y + backgroundHeight - scale + lineOffset, 2.5f * scale, color); //Left
                    render.line2D(finalXPosition - lineOffset, y - backgroundOffset - lineOffset, finalXPosition + (scaledTextWidth / textScale) + lineOffset, y - backgroundOffset - lineOffset, 2.5f * scale, color); //TOP
                } else {
                    render.line2D(finalXPosition - lineOffset, y - backgroundOffset + lineOffset, finalXPosition - lineOffset, y + backgroundHeight - scale + lineOffset, 2.5f * scale, color); //up
                    render.line2D(prevX, y - backgroundOffset + lineOffset, finalXPosition - lineOffset, y - backgroundOffset + lineOffset, 2.5f * scale, color); //down
                }
                break;   */
        }
        prevX = finalXPosition - lineOffset;
        prevY = y + backgroundHeight * scale;
        y += moduleHeight * scale;
        previousWidth = scaledTextWidth / textScale;
        index += (direction == 0) ? 100 * scale : -100 * scale;
    }
    
    switch(outlineMode) {
        case 2: // Right
            float posX = displayWidth - xOffset + lineGap * textScale;
            render.line2D( // Right
                posX, // x1
                yOffset - 1 * textScale, // y1
                posX, // x2
                y - textScale - gap * textScale, // y2
                2.5f * textScale,
                color);
            break;
        case 3: // Full
            /* render.line2D(prevX, y - 2 * textScale + lineGap * textScale, prevX + previousWidth + 1 * textScale, y - 2 * textScale + lineGap * textScale, 2.5f * textScale, color); // Bottom
            posX = displayWidth - xOffset;
            render.line2D( // Right
                posX, // x1
                yOffset - 1 * textScale, // y1
                posX, // x2
                y - textScale - gap * textScale, // y2
                2.5f * textScale,
                color);
            break; */
    }
}

int getRainbow(float seconds, float saturation, float brightness, long index) {
    float hue = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    return Color.HSBtoRGB(hue, saturation, brightness);
}

double getWaveRatio(float seconds, long index) {
    float time = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    double waveRatio = (time <= 0.5) ? (time * 2) : (2 - time * 2);
    return waveRatio;
}

Color blendColors(Color color1, Color color2, double ratio) {
    int r = clamp((int) (color1.getRed() * ratio + color2.getRed() * (1 - ratio)), 0, 255);
    int g = clamp((int) (color1.getGreen() * ratio + color2.getGreen() * (1 - ratio)), 0, 255);
    int b = clamp((int) (color1.getBlue() * ratio + color2.getBlue() * (1 - ratio)), 0, 255);
    return new Color(r, g, b);
}

int clamp(int val, int min, int max) {
    if (val < min) return min;
    if (val > max) return max;
    return val;
}

void updateEnabledModules() {
    long now = client.time();
    List<String> previousEnabledModules = new ArrayList<>();
    int ex = client.getPlayer().getTicksExisted();

    if (ex < 60 || ex % 20 == 0) {
        updateButtonStates();
    }

    for (Map<String, Object> mod : mods) {
        String moduleName = (String) mod.get("name");
        boolean currentlyVisible = (boolean) mod.get("visibility");
        boolean shouldBeVisible = (boolean) mod.getOrDefault("buttonEnabled", false) && modules.isEnabled(moduleName);

        if (currentlyVisible) {
            previousEnabledModules.add(moduleName);
        }

        if (shouldBeVisible != currentlyVisible) {
            mod.put("visibility", shouldBeVisible);
            mod.put("animating", true);
            mod.put("animatingUp", !shouldBeVisible);

            float animationProgress = ((Number) mod.get("animationProgress")).floatValue();
            animationProgress = (animationProgress >= 1f) ? 0f : (animationProgress > 0f ? 1f - animationProgress : animationProgress);

            long adjustedStartTime = now - (long) (animationDuration * animationProgress);
            mod.put("animationStart", adjustedStartTime);
        }
    }
}

void updateAnimations() {
    long currentTime = client.time();

    for (Map<String, Object> mod : mods) {
        if ((boolean) mod.get("animating")) {
            long startTime = (long) mod.get("animationStart");
            float elapsed = (float) (currentTime - startTime) / (float) animationDuration;

            if (elapsed >= 1f) {
                elapsed = 1f;
                mod.put("animating", false);
            }

            mod.put("animationProgress", elapsed);

            float easedOffset = quadInOut(elapsed) * moduleHeight;
            float easedScale = quadInOut(elapsed);
            if ((boolean) mod.get("animatingUp")) {
                mod.put("offset", moduleHeight - easedOffset);
                mod.put("scale", 1f - easedScale);
            } else {
                mod.put("offset", easedOffset);
                mod.put("scale", easedScale);
            }
        }
    }
}

float quadInOut(float t) {
    if (t < 0.5f) {
        return 2 * t * t;
    } else {
        return -1 + (4 - 2 * t) * t;
    }
}

void updateButtonStates() {
    for (Map<String, Object> mod : mods) {
        String moduleName = (String) mod.get("name");
        boolean isButtonEnabled = modules.getButton(scriptName, moduleName);
        mod.put("buttonEnabled", isButtonEnabled);
    }

    sortModules();
}

void sortModules() {
    mods.sort((a, b) -> {
        String aName = (String) a.get("name");
        String bName = (String) b.get("name");

        String aDisplayName = aName;
        String bDisplayName = bName;

        if (customModuleData.containsKey(aName)) {
            Map<String, String> customDataA = customModuleData.get(aName);
            aDisplayName = customDataA.getOrDefault("alias", aName) + (customDataA.containsKey("overrideValue") ? ": " + customDataA.get("overrideValue") : "");
        }

        if (customModuleData.containsKey(bName)) {
            Map<String, String> customDataB = customModuleData.get(bName);
            bDisplayName = customDataB.getOrDefault("alias", bName) + (customDataB.containsKey("overrideValue") ? ": " + customDataB.get("overrideValue") : "");
        }

        int widthA = render.getFontWidth(aDisplayName);
        int widthB = render.getFontWidth(bDisplayName);

        return Integer.compare(widthB, widthA);
    });
}

String formatDoubleStr(double val) {
    String str;
    if (val % 1 == 0) {
        str = String.valueOf((int) val);
    } else {
        str = String.valueOf(val);
    }
    return str;
}