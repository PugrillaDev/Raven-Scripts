/* 
    simple chat hotkeys
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/hotkeys.java"
*/

void onLoad() {
    modules.registerButton("Show Alerts", true);

    hotkey("Lobby", "/l");
    hotkey("Warp", "/p warp");
    hotkey("Fours", "/play bedwars_four_four");
}

boolean alerts;
Map<String, Map<String, Object>> hotkeys = new HashMap<>();
String[] keys = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    "BACK", "CAPITAL", "COMMA", "DELETE", "DOWN", "END", "ESCAPE", "F1", "F2", "F3", "F4", "F5",
    "F6", "F7", "HOME", "INSERT", "LBRACKET", "LCONTROL", "LMENU", "LMETA", "LSHIFT", "MINUS",
    "NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5", "NUMPAD6", "NUMPAD7",
    "NUMPAD8", "NUMPAD9", "PERIOD", "RETURN", "RCONTROL", "RSHIFT", "RBRACKET", "SEMICOLON",
    "SLASH", "SPACE", "TAB", "GRAVE"
};

void onEnable() {
    updateHotkeys();
}

void onPreUpdate() {
    updateHotkeys();
}

void activate(String name) {
    if (alerts) {
        client.print("&7[&dR&7] &7Activated keybind &d" + name + "&7.");
    }
    client.chat((String)hotkeys.get(name).get("text"));
}

void hotkey(String name, String text) {
    Map<String, Object> keybind = new HashMap<>();
    keybind.put("text", text);
    keybind.put("keycode", null);
    keybind.put("pressed", false);
    
    hotkeys.put(name, keybind);

    modules.registerSlider(name, text, 0, keys);
}

void updateHotkeys() {
    if (!client.getScreen().isEmpty()) return;
    alerts = modules.getButton(scriptName, "Show Alerts");
    for (Map.Entry<String, Map<String, Object>> entry : hotkeys.entrySet()) {
        String name = entry.getKey();
        Map<String, Object> keybind = entry.getValue();

        Integer keycode = (Integer) keybind.get("keycode");
        boolean pressed = (boolean) keybind.get("pressed");

        int key = keybinds.getKeyIndex(keys[(int)modules.getSlider(scriptName, name)]);
        if (keycode == null || key != keycode) {
            keybind.put("keycode", key);
            keycode = key;
        }

        boolean down = keybinds.isKeyDown(keycode);
        if (down && !pressed) {
            activate(name);
            keybind.put("pressed", true);
        } else if (!down) {
            keybind.put("pressed", false);
        }
    }
}