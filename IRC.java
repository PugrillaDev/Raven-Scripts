WebSocket w;
List<String> q = new ArrayList<>();
long lastSend = 0, lastReconnect = 0;
long lastHeartbeat = 0;
long HEARTBEAT_INTERVAL = 30000;
boolean reconnect = false;
String P = "&6&lIRC&r", k, prefix;

void onLoad() {
    modules.registerDescription("https://discord.gg/Y2BRdNcns2");
    modules.registerDescription("Use /api-key with Dave H bot");
    modules.registerDescription("Try /irc");

    prefix = config.get("irc_prefix");
    if (prefix == null || prefix.isEmpty() || prefix.length() > 1) {
        prefix = "-";
        config.set("irc_prefix", prefix);
    }
}

void onEnable() {
    k = config.get("irc_key");
    if (k == null || k.isEmpty()) {
        client.print(P + " &7No API key found. Use &6/irc key <key> &7to set it.");
        return;
    }
    if (w != null && w.isOpen()) return;
    connect();
}

void connect() {
    if (w != null) { w.close(false); w = null; }

    w = new WebSocket("wss://privatemethod.xyz/api/irc?key=" + k) {
        public void onOpen(short s, String m) {
            reconnect = false;
            q.clear();

            lastHeartbeat = client.time();
        }

        public void onMessage(String m) {
            if (modules.isEnabled(scriptName)) handleMessage(m);
        }

        public void onClose(int c, String rs, boolean rem) {
            client.print(P + " &7Disconnected.");
            reconnect = true;
            lastReconnect = client.time();
        }
    };

    w.connect(false);
}

boolean onPacketSent(CPacket p) {
    if (!(p instanceof C01)) return true;
    C01 c = (C01) p;

    if (c.message.startsWith(prefix)) {
        String msg = c.message.substring(prefix.length());
        if (!msg.isEmpty()) q.add(msg);
        return false;
    }

    if (!c.message.startsWith("/irc")) return true;

    String[] a = c.message.split(" ", 3);
    if (a.length <= 1) { printHelp(); return false; }

    String sub = a[1].trim().toLowerCase();
    String arg = (a.length >= 3) ? a[2].trim() : "";

    if (sub.equals("key")) {
        if (arg.isEmpty()) { client.print(P + " &7Usage: &6/irc key <key>"); return false; }
        k = arg;
        if (config.set("irc_key", k)) {
            client.print(P + " &7API key updated. Connecting...");
            connect();
        } else client.print(P + " &7Failed to save API key.");
        return false;
    }

    if (sub.equals("name")) {
        if (arg.isEmpty()) { client.print(P + " &7Usage: &6/irc name <name>"); return false; }
        sendIRC(1, arg);
        return false;
    }

    if (sub.equals("prefix")) {
        if (arg.isEmpty()) { client.print(P + " &7Usage: &6/irc prefix <symbol>"); return false; }
        if (arg.length() > 1) { client.print(P + " &7Invalid prefix."); return false; }
        prefix = arg;
        config.set("irc_prefix", prefix);
        client.print(P + " &7Prefix updated to: &6" + prefix);
        return false;
    }

    client.print(P + " &7Unknown subcommand: " + sub);
    return false;
}

void printHelp() {
    String t = " &6IRC Commands &7", f = " &6Made by Pug &7";
    String[] m = {
        "&6/irc key <key>&7: Sets your IRC API key.",
        "&6/irc name <name>&7: Sets your IRC username.",
        "&6/irc prefix <symbol>&7: Sets the IRC message prefix."
    };

    int mx = 0;
    String st = t.replaceAll("&[0-9a-fk-or]", "");
    String sf = f.replaceAll("&[0-9a-fk-or]", "");

    mx = Math.max(mx, render.getFontWidth(st));
    mx = Math.max(mx, render.getFontWidth(sf));
    for (String s : m) mx = Math.max(mx, render.getFontWidth(s.replaceAll("&[0-9a-fk-or]", "")));

    int ex = render.getFontWidth("  ");
    int hw = mx + ex;

    int tp = hw - render.getFontWidth(st);
    int fp = hw - render.getFontWidth(sf);

    String hdr = "&7" + genPad('-', tp / 2) + t + genPad('-', tp / 2) + ((tp % 2 != 0) ? "-" : "");
    client.print(P + " " + hdr);

    for (String s : m) {
        int lp = (mx - render.getFontWidth(s.replaceAll("&[0-9a-fk-or]", ""))) / 2;
        client.print(P + " " + genPad(' ', lp) + s);
    }

    String ftr = "&7" + genPad('-', fp / 2) + f + genPad('-', fp / 2) + ((fp % 2 != 0) ? "-" : "");
    client.print(P + " " + ftr);
}

String genPad(char ch, int px) {
    int cw = render.getFontWidth(String.valueOf(ch));
    if (cw <= 0 || px <= 0) return "";
    int n = px / cw;
    if (n <= 0) return "";
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(ch);
    return sb.toString();
}

void sendIRC(int id, String d) {
    if (w == null || !w.isOpen()) return;
    Json o = Json.object();
    o.add("id", id);
    o.add("data", d);
    w.send(o.toString());
}

void handleMessage(String m) {
    Json j;
    try { j = Json.parse(m); }
    catch (Exception ex) {
        client.print(P + " &cInvalid JSON: " + m);
        return;
    }

    if (j == null || j.type() != Json.Type.OBJECT) {
        client.print(P + " &cInvalid JSON: " + m);
        return;
    }

    Json jid = j.get("id");
    if (jid == null || jid.type() == Json.Type.NULL) {
        client.print(P + " &cInvalid JSON (missing id): " + m);
        return;
    }

    int id = -1;
    if (jid.type() == Json.Type.NUMBER) id = jid.asInt();
    else if (jid.type() == Json.Type.STRING) {
        try { id = Integer.parseInt(jid.asString()); } catch (Exception ignored) {}
    }

    Json jd = j.get("data");
    String d = "";
    if (jd != null && jd.type() != Json.Type.NULL) {
        if (jd.type() == Json.Type.STRING) d = jd.asString();
        else d = jd.toString();
    }

    if (id == 0) client.print(new Message(d));
}

void onPreUpdate() {
    long t = client.time();

    if (w != null && w.isOpen() && !q.isEmpty() && t - lastSend > 1000) {
        sendIRC(0, q.remove(0));
        lastSend = t;
    }

    if (w != null && w.isOpen() && q.isEmpty() && t - lastHeartbeat > HEARTBEAT_INTERVAL) {
        sendIRC(9, "ping");
        lastHeartbeat = t;
    }

    if (reconnect && t - lastReconnect > 5000) {
        client.print(P + " &7Attempting to reconnect...");
        lastReconnect = t;
        connect();
    }
}

void onDisconnect() {
    q.clear();
    try {
        if (w != null) {
            w.close(false);
            w = null;
        }
    } catch (Exception ignored) { }
}