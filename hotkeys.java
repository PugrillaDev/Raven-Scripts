/* 
    simple chat hotkeys
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/hotkeys.java"
*/

void onLoad() {
    modules.registerButton("Show Alerts", true);
    registerHotkey("Bedwars Lobby", "/bedwars");
    registerHotkey("Lobby", "/l");
    registerHotkey("Warp", "/p warp");
    registerHotkey("Fours", "/play bedwars_four_four");
}

Map<String, String> hotkeys = new HashMap<>();
Map<String, Boolean> lastPressed = new HashMap<>();

void onEnable() {
    for (String key : hotkeys.keySet()) {
        lastPressed.put(key, false);
    }
}

void onPreUpdate() {
    for (String name : hotkeys.keySet()) {
        boolean pressed = modules.getKeyPressed(scriptName, name);
        boolean wasPressed = lastPressed.getOrDefault(name, false);
        if (client.getScreen().isEmpty() && !wasPressed && pressed) {
            activate(name);
        }
        lastPressed.put(name, pressed);
    }
}

void activate(String name) {
    if (modules.getButton(scriptName, "Show Alerts")) {
        client.print("&7[&dR&7] &7Activated keybind &d" + name + "&7.");
    }
    client.chat(hotkeys.get(name));
}

void registerHotkey(String name, String text) {
    hotkeys.put(name, text);
    modules.registerKey(name, 0);
    lastPressed.put(name, false);
}