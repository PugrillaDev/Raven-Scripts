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
    "SLASH", "SPACE", "TAB"
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

        int key = getKeyCode(keys[(int)modules.getSlider(scriptName, name)]);
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

int getKeyCode(String keyName) {
    switch (keyName) {
        case "0": return 11;
        case "1": return 2;
        case "2": return 3;
        case "3": return 4;
        case "4": return 5;
        case "5": return 6;
        case "6": return 7;
        case "7": return 8;
        case "8": return 9;
        case "9": return 10;
        case "A": return 30;
        case "B": return 48;
        case "C": return 46;
        case "D": return 32;
        case "E": return 18;
        case "F": return 33;
        case "G": return 34;
        case "H": return 35;
        case "I": return 23;
        case "J": return 36;
        case "K": return 37;
        case "L": return 38;
        case "M": return 50;
        case "N": return 49;
        case "O": return 24;
        case "P": return 25;
        case "Q": return 16;
        case "R": return 19;
        case "S": return 31;
        case "T": return 20;
        case "U": return 22;
        case "V": return 47;
        case "W": return 17;
        case "X": return 45;
        case "Y": return 21;
        case "Z": return 44;
        case "BACK": return 14;
        case "CAPITAL": return 58;
        case "COMMA": return 51;
        case "DELETE": return 211;
        case "DOWN": return 208;
        case "END": return 207;
        case "ESCAPE": return 1;
        case "F1": return 59;
        case "F2": return 60;
        case "F3": return 61;
        case "F4": return 62;
        case "F5": return 63;
        case "F6": return 64;
        case "F7": return 65;
        case "HOME": return 199;
        case "INSERT": return 210;
        case "LBRACKET": return 26;
        case "LCONTROL": return 29;
        case "LMENU": return 56;
        case "LMETA": return 219;
        case "LSHIFT": return 42;
        case "MINUS": return 12;
        case "NUMPAD0": return 82;
        case "NUMPAD1": return 79;
        case "NUMPAD2": return 80;
        case "NUMPAD3": return 81;
        case "NUMPAD4": return 75;
        case "NUMPAD5": return 76;
        case "NUMPAD6": return 77;
        case "NUMPAD7": return 71;
        case "NUMPAD8": return 72;
        case "NUMPAD9": return 73;
        case "PERIOD": return 52;
        case "RETURN": return 28;
        case "RCONTROL": return 157;
        case "RSHIFT": return 54;
        case "RBRACKET": return 27;
        case "SEMICOLON": return 39;
        case "SLASH": return 53;
        case "SPACE": return 57;
        case "TAB": return 15;
        default: return -1;
    }
}