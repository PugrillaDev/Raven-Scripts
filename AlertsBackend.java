/*
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/AlertsBackend.java"
    Example Usage

    Renders an alert with the title "Title" and a "Hello World" message for 10 seconds. 
    When clicked, it will send "/bedwars" in the chat.
    This script is designed to be a resource for you to utilize, so you must run this code from another script.

    List<Map<String, Object>> alerts = new ArrayList<>();

    void addAlert(String title, String message, int duration, String command) {
        Map<String, Object> alert = new HashMap<>(4);
        alert.put("title", title);
        alert.put("message", message);
        alert.put("duration", duration);
        alert.put("command", command);
        alerts.add(alert);
    }

    void onPreUpdate() {
        if (!alerts.isEmpty() && !bridge.has("pugalert")) {
            bridge.add("pugalert", alerts.remove(0));
        }
    }

    void onEnable() {
        addAlert("Title", "Hello World", 10000, "/bedwars");
    }


    Additionally, you could send an alert with a distinct phrase such as "5749387394743989". 
    Then simply look for that message in an outgoing C01 packet to trigger another function instead of sending a message in chat.
    Now when the alert is clicked, it will call the trigger function in a separate script.

    Script 1:

    void onEnable() {
        addAlert("Title", "Hello World", 10000, "5749387394743989");
    }

    Script 2:

    void trigger() {
        client.chat("/bedwars");
    }

    boolean onPacketSent(CPacket packet) {
        if (packet instanceof C01) {
            C01 c01 = (C01) packet;
            if (c01.message.equals("5749387394743989")) {
                trigger();
                return false;
            }
        }
    }
*/

List<Map<String, Object>> alerts = new ArrayList<>();
int themeColor;
float scale = 1;
float animateIn = 100;
float animateOut = 100;
float animateClick = 25;
float animateHover = 100;
float lineWidth = 2.5f;
boolean clicking = false;
boolean lastclicking = false;
int[] displaySize;
float[] mousePosition;
float[] lastClickPosition;

void onLoad() {
    modules.registerSlider("Theme", "", 197, 0, 360, 1);
    modules.registerSlider("Scale", "", 1.0, 0.5, 1.5, 0.1);
    modules.registerSlider("Line Width", "", 2.5, 0.1, 5, 0.1);
    modules.registerSlider("Animate In", "ms", 100, 0, 2000, 10);
    modules.registerSlider("Animate Out", "ms", 100, 0, 2000, 10);
    bridge.remove("pugalert");
}

@SuppressWarnings("unchecked")
void onPostMotion() {
    long now = client.time();
    Color color = Color.getHSBColor((float) modules.getSlider(scriptName, "Theme") / 360f, 1f, 1f);
    themeColor = (255 << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    scale = (float) modules.getSlider(scriptName, "Scale");
    displaySize = client.getDisplaySize();
    animateIn = (float) modules.getSlider(scriptName, "Animate In");
    animateOut = (float) modules.getSlider(scriptName, "Animate Out");
    lineWidth = (float) modules.getSlider(scriptName, "Line Width") * scale;
    lastclicking = clicking;
    clicking = keybinds.isMouseDown(0);

    int[] size = client.getDisplaySize();
    float guiScale = size[2];
    int[] mp = keybinds.getMousePosition();
    mousePosition = new float[]{ mp[0] / guiScale, size[1] - (mp[1] / guiScale) };

    if (clicking && !lastclicking) {
        lastClickPosition = new float[]{ mousePosition[0], mousePosition[1] };
    }
    
    Map<String, Object> alert = (Map<String, Object>) bridge.get("pugalert");
    if (alert != null) {
        bridge.remove("pugalert");
        String title = (String) alert.get("title");
        String message = (String) alert.get("message");
        Integer duration = (Integer) alert.get("duration");

        if (title != null && message != null && duration != null) {
            String command = (String) alert.getOrDefault("command", "");
            
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("title", title);
            alertData.put("message", message);
            alertData.put("command", command);
            alertData.put("added", now);
            alertData.put("animationStart", now);
            alertData.put("duration", duration + (int) animateIn);
            alertData.put("originalDuration", duration + (int) animateIn);
            alertData.put("removing", false);
            alertData.put("hoverState", false);
            alertData.put("startHover", -1L);
            alertData.put("stopHover", -1L);
            alertData.put("clicking", false);
            alertData.put("clickStart", -1L);
            alerts.add(alertData);
        }
    }

    for (Map<String, Object> alertData : alerts) {
        boolean isHovered = (boolean) alertData.get("hoverState");
        boolean isClicking = (boolean) alertData.get("clicking");
        alertData.put("lastClicking", isClicking);
        if (isHovered && clicking && !isClicking) {
            alertData.put("clicking", true);
            alertData.put("clickStart", now);
        } else if (!clicking && isClicking) {
            alertData.put("clicking", false);
            alertData.put("clickStart", now);
        }
    }
}

void onRenderTick(float partialTicks) {
    if (alerts.isEmpty()) return;

    boolean empty = client.getScreen().isEmpty();
    long now = client.time();
    float offsetY = 0;
    float maxSlideOffsetY = 0;
    boolean foundRemoving = false;

    for (Iterator<Map<String, Object>> iterator = alerts.iterator(); iterator.hasNext();) {
        Map<String, Object> alertData = iterator.next();
        boolean removing = (boolean) alertData.get("removing");
        int alertDurationMillis = (int) alertData.get("duration");
        String command = (String) alertData.get("command");

        if (alertDurationMillis <= 0) {
            alertData.put("removing", true);
            if (!removing) alertData.put("animationStart", now);
            removing = true;
            foundRemoving = true;
        }

        long animationStart = (long) alertData.get("animationStart");
        float animationProgress;
        if (removing) {
            animationProgress = (now - animationStart) / animateOut;
            if (animationProgress >= 1f) {
                iterator.remove();
                continue;
            }
            animationProgress = 1f - animationProgress;
        } else {
            animationProgress = Math.min((now - animationStart) / animateIn, 1f);
        }

        float progress = animationProgress < 0.5
                ? 2 * animationProgress * animationProgress
                : -1 + (4 - 2 * animationProgress) * animationProgress;

        String title = (String) alertData.get("title");
        String message = (String) alertData.get("message");

        float maxWidth = (float) Math.max(render.getFontWidth(title), render.getFontWidth(message)) * scale;
        float scaledFontHeight = render.getFontHeight() * scale;
        float padding = 5 * scale;
        float width = maxWidth + padding * 2;
        float height = scaledFontHeight * 2 + padding * 2 + (3 * scale);

        float endX = displaySize[0] - width - padding;
        float currentX = (displaySize[0] + width) - ((displaySize[0] + width) - endX) * progress;
        float startY = displaySize[1] - height - padding - offsetY;
        float currentY = startY;

        if (removing) {
            float removeProgress = (1f - progress);
            currentX = endX + (width + padding) * removeProgress;
            currentY = startY + (displaySize[1] - startY - padding) * removeProgress;
            maxSlideOffsetY = Math.max(maxSlideOffsetY, (height + padding) * removeProgress);
        } else if (foundRemoving) {
            currentY += maxSlideOffsetY;
        }

        boolean isHovered = !empty && mousePosition[0] >= currentX && mousePosition[0] <= currentX + width &&
                            mousePosition[1] >= currentY && mousePosition[1] <= currentY + height;

        long lastCheck = (long) alertData.getOrDefault("lastCheck", now);
        long elapsed = now - lastCheck;
        if (!isHovered) alertDurationMillis -= elapsed;
        alertData.put("duration", alertDurationMillis);
        alertData.put("lastCheck", now);

        if (isHovered && !(boolean) alertData.get("hoverState")) {
            alertData.put("hoverState", true);
            alertData.put("startHover", now);
        } else if (!isHovered && (boolean) alertData.get("hoverState")) {
            alertData.put("hoverState", false);
            alertData.put("stopHover", now);
        }

        long startHover = (long) alertData.get("startHover");
        long stopHover = (long) alertData.get("stopHover");

        int lineColor;
        if (isHovered) {
            if (startHover > 0) {
                float hoverProgress = Math.min(1f, (now - startHover) / animateHover);
                lineColor = interpolateColor(0xFF000000, themeColor, hoverProgress);
            } else {
                lineColor = themeColor;
            }
        } else {
            if (stopHover > 0) {
                float hoverProgress = Math.min(1f, (now - stopHover) / animateHover);
                lineColor = interpolateColor(themeColor, 0xFF000000, hoverProgress);
            } else {
                lineColor = 0xFF000000;
            }
        }

        float clickScale = 1f;
        if (!command.isEmpty() && lastClickPosition != null &&
            lastClickPosition[0] >= currentX && lastClickPosition[0] <= currentX + width &&
            lastClickPosition[1] >= currentY && lastClickPosition[1] <= currentY + height) {
                
            boolean isClicking = (boolean) alertData.get("clicking");
            boolean lastClicking = (boolean) alertData.get("lastClicking");

            if (!isClicking && lastClicking && isHovered) {
                client.chat(command);
                alertData.put("clicking", false);
                alertData.put("lastClicking", false);
            }
            
            if (isClicking) {
                long clickStart = (long) alertData.get("clickStart");
                float clickProgress = Math.min(1f, (now - clickStart) / animateClick);
                clickScale = 1f - 0.02f * clickProgress;
            } else if (alertData.containsKey("clickStart") && (long) alertData.get("clickStart") > 0) {
                long clickStart = (long) alertData.get("clickStart");
                float clickProgress = Math.min(1f, (now - clickStart) / animateClick);
                clickScale = 0.98f + 0.02f * clickProgress;
            }
        }

        float adjustedScale = scale * clickScale;
        float adjustedLineWidth = lineWidth * clickScale;
        float adjustedWidth = width * clickScale;
        float adjustedHeight = height * clickScale;
        float adjustedX = currentX + (width - adjustedWidth) / 2;
        float adjustedY = currentY + (height - adjustedHeight) / 2;

        // background
        render.blur.prepare();
        render.rect(adjustedX - adjustedScale, adjustedY, adjustedX + adjustedWidth - adjustedScale, adjustedY + adjustedHeight, -1);
        render.blur.apply(2, 3);
        render.rect(adjustedX, adjustedY, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0x80000000);

        // progress bar
        float timeRemaining = Math.max(0, alertDurationMillis);
        float progressBarWidth = adjustedWidth * (timeRemaining / ((Number) alertData.get("originalDuration")).floatValue());
        render.line2D(adjustedX, adjustedY + adjustedHeight, adjustedX + progressBarWidth, adjustedY + adjustedHeight, adjustedLineWidth, themeColor);
        render.line2D(adjustedX + progressBarWidth, adjustedY + adjustedHeight, adjustedX + adjustedWidth, adjustedY + adjustedHeight, adjustedLineWidth, lineColor);

        // outlines
        render.line2D(adjustedX, adjustedY, adjustedX, adjustedY + adjustedHeight, adjustedLineWidth, lineColor);
        render.line2D(adjustedX + adjustedWidth, adjustedY, adjustedX + adjustedWidth, adjustedY + adjustedHeight, adjustedLineWidth, lineColor);
        render.line2D(adjustedX, adjustedY, adjustedX + adjustedWidth, adjustedY, adjustedLineWidth, lineColor);

        // text
        render.text(title, adjustedX + padding * clickScale, adjustedY + padding * clickScale, adjustedScale, themeColor, true);
        render.text(message, adjustedX + padding * clickScale, adjustedY + scaledFontHeight * clickScale + padding * 2 * clickScale, adjustedScale, themeColor, true);

        offsetY += height + padding;
    }
}

int interpolateColor(int startColor, int endColor, float progress) {
    int startA = (startColor >> 24) & 0xFF, startR = (startColor >> 16) & 0xFF, startG = (startColor >> 8) & 0xFF, startB = startColor & 0xFF;
    int endA = (endColor >> 24) & 0xFF, endR = (endColor >> 16) & 0xFF, endG = (endColor >> 8) & 0xFF, endB = endColor & 0xFF;
    int interpolatedA = (int) (startA + (endA - startA) * progress);
    int interpolatedR = (int) (startR + (endR - startR) * progress);
    int interpolatedG = (int) (startG + (endG - startG) * progress);
    int interpolatedB = (int) (startB + (endB - startB) * progress);
    return (interpolatedA << 24) | (interpolatedR << 16) | (interpolatedG << 8) | interpolatedB;
}