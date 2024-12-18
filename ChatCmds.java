/* 
    chat commands starting with '.' for toggling/editing modules
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/ChatCmds.java"
*/

String chatPrefix = "&7[&dR&7]&r ";

boolean onPacketSent(CPacket packet) {
    if (!(packet instanceof C01)) return true;

    C01 c01 = (C01) packet;
    String message = c01.message.toLowerCase();
    if (!message.startsWith(".")) return true;

    String[] parts = message.split(" ");
    if (parts.length < 1 || parts.length > 3) return false;

    String input = parts[0].substring(1).replace(" ", "-").toLowerCase();
    if (input.isEmpty()) return false;

    if (parts[0].equals(".help") || parts[0].equals(".list")) {
        Map<String, List<String>> categories = new HashMap<>(modules.getCategories());
        categories.remove("profiles");
        categories.remove("fun");
        List<String> modulesList = new ArrayList<>();

        for (List<String> mods : categories.values()) {
            modulesList.addAll(mods);
        }
        Collections.sort(modulesList, String.CASE_INSENSITIVE_ORDER);

        int page = 1;
        int items = 10;

        if (parts.length > 1) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                client.print(chatPrefix + "&eInvalid page number.");
                return false;
            }
        }

        int total = modulesList.size();
        int pages = (total + items - 1) / items;

        if (page < 1 || page > pages) {
            client.print(chatPrefix + "&ePage out of range.");
            return false;
        }

        int start = (page - 1) * items;
        int end = Math.min(start + items, total);

        client.print(chatPrefix + "&eModules List (&3" + page + "&7/&3" + pages + "&e)");
        for (int i = start; i < end; i++) {
            String name = modulesList.get(i);
            boolean enabled = modules.isEnabled(name);
            client.print(chatPrefix + "&7" + name + ": " + (enabled ? "&atrue" : "&cfalse"));
        }
        return false;
    }

    Map<String, List<String>> categories = modules.getCategories();
    String module = null;

    for (List<String> modulesList : categories.values()) {
        for (String name : modulesList) {
            if (name.toLowerCase().replace(" ", "-").equals(input)) {
                module = name;
                break;
            }
        }
        if (module != null) break;
    }

    if (module == null) {
        client.print(chatPrefix + "&eModule not found.");
        return false;
    }

    if (parts.length == 2 && (parts[1].equalsIgnoreCase("list") || parts[1].equalsIgnoreCase("help"))) {
        Map<String, Object> settings = modules.getSettings(module);

        client.print(chatPrefix + "&b" + module + " settings:");
        for (Map.Entry<String, Object> setting : settings.entrySet()) {
            String key = setting.getKey().toLowerCase().replace(" ", "-");
            Object value = setting.getValue();
            if (value == null) continue;
            String color;

            if (value instanceof Boolean) {
                color = ((Boolean) value) ? "&a" : "&c";
            } else if (value instanceof Double) {
                color = "&e";
                value = formatDoubleStr((Double) value);
            } else {
                color = "&7";
            }

            client.print(chatPrefix + "&7" + key + ": " + color + value);
        }
        return false;
    }

    if (parts.length == 1) {
        boolean enabled = modules.isEnabled(module);
        if (enabled) {
            modules.disable(module);
            client.print(chatPrefix + "&7Disabled &c" + module);
        } else {
            modules.enable(module);
            client.print(chatPrefix + "&7Enabled &a" + module);
        }
        return false;
    }

    if (parts.length == 3) {
        String setting = parts[1].replace(" ", "-").toLowerCase();
        String value = parts[2];
        Map<String, Object> settings = modules.getSettings(module);

        String match = null;

        for (String key : settings.keySet()) {
            if (key.toLowerCase().replace(" ", "-").equals(setting)) {
                match = key;
                break;
            }
        }

        if (match == null) {
            client.print(chatPrefix + "&eInvalid setting name.");
            return false;
        }

        Object currentValue = settings.get(match);
        if (currentValue instanceof Boolean) {
            boolean newValue = value.equalsIgnoreCase("true");
            modules.setButton(module, match, newValue);
            client.print(chatPrefix + "&7Set " + match + " to " + (newValue ? "&atrue" : "&cfalse"));
        } else if (currentValue instanceof Double) {
            double newValue = Double.parseDouble(value);
            modules.setSlider(module, match, newValue);
            client.print(chatPrefix + "&7Set " + match + " to " + "&e" + formatDoubleStr(newValue));
        } else {
            client.print(chatPrefix + "&eInvalid value type.");
        }
    }

    return false;
}

String formatDoubleStr(double val) {
    return val == (long) val ? Long.toString((long) val) : Double.toString(val);
}