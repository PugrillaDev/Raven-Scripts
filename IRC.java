WebSocket w;
List<String> q = new ArrayList<>();
long l = 0, lr = 0;
boolean rc = false;
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

void onEnable(){
    k = config.get("irc_key");
    if(k == null || k.isEmpty()){
        client.print(P + " &7No API key found. Use &6/irc key <key> &7to set it.");
        return;
    }
    if(w != null && w.isOpen()) return;
    c();
}

void c(){
    if(w != null){ w.close(false); w = null; }
    w = new WebSocket("wss://privatemethod.xyz/api/irc?key="+k){
        public void onOpen(short s, String m){ rc = false; q.clear(); }
        public void onMessage(String m){ if(modules.isEnabled(scriptName)) mP(m); }
        public void onClose(int c, String rs, boolean rem){
            client.print(P + " &7Disconnected.");
            rc = true; lr = client.time();
        }
    };
    w.connect(false);
}

boolean onPacketSent(CPacket p){
    if(p instanceof C01){
        C01 c = (C01)p;
        
        if(c.message.startsWith(prefix)){
            q.add(c.message.substring(prefix.length()));
            return false;
        }
        
        if(!c.message.startsWith("/irc")) return true;
        String[] a = c.message.split(" ", 3);
        
        if(a.length <= 1){
            String t = " &6IRC Commands &7", f = " &6Made by Pug &7";
            String[] m = {
                "&6/irc key <key>&7: Sets your IRC API key.",
                "&6/irc name <name>&7: Sets your IRC username.",
                "&6/irc prefix <symbol>&7: Sets the IRC message prefix."
            };
            int mx = 0;
            String st = t.replaceAll("&[0-9a-fk-or]", ""), sf = f.replaceAll("&[0-9a-fk-or]", "");
            for(String s : m) mx = Math.max(mx, render.getFontWidth(s.replaceAll("&[0-9a-fk-or]", "")));
            mx = Math.max(mx, render.getFontWidth(st));
            mx = Math.max(mx, render.getFontWidth(sf));
            int ex = render.getFontWidth("  "), hw = mx + ex;
            int tp = hw - render.getFontWidth(st), fp = hw - render.getFontWidth(sf);
            int tsp = tp / 2, fsp = fp / 2;
            String hdr = "&7" + genPad('-', tsp) + t + genPad('-', tsp);
            if(tp % 2 != 0) hdr += "-";
            client.print(P + " " + hdr);
            for(String s : m){
                int lp = (mx - render.getFontWidth(s.replaceAll("&[0-9a-fk-or]", ""))) / 2;
                client.print(P + " " + genPad(' ', lp) + s);
            }
            String ftr = "&7" + genPad('-', fsp) + f + genPad('-', fsp);
            if(fp % 2 != 0) ftr += "-";
            client.print(P + " " + ftr);
            return false;
        }

        String sub = a[1].trim().toLowerCase();
        if(sub.equals("key")){
            if(a.length < 3 || a[2].trim().isEmpty()){
                client.print(P + " &7 Usage: &6/irc key <key>");
                return false;
            }
            k = a[2].trim();
            if(config.set("irc_key", k)){
                client.print(P + " &7API key updated. Connecting...");
                c();
            } else client.print(P + " &7Failed to save API key.");
            return false;
        } else if(sub.equals("name")){
            if(a.length < 3 || a[2].trim().isEmpty()){
                client.print(P + " &7Usage: &6/irc name <name>");
                return false;
            }
            sendIRC(1, a[2].trim());
            return false;
        } else if(sub.equals("prefix")){
            if(a.length < 3 || a[2].trim().isEmpty()){
                client.print(P + " &7Usage: &6/irc prefix <symbol>");
                return false;
            }
            String newPrefix = a[2].trim();
            
            if(newPrefix.length() > 1){
                client.print(P + " &7Invalid prefix.");
                return false;
            }

            prefix = newPrefix;
            config.set("irc_prefix", prefix);
            client.print(P + " &7Prefix updated to: &6" + prefix);
            return false;
        } else {
            client.print(P + " &7Unknown subcommand: " + sub);
            return false;
        }
    }
    return true;
}

String genPad(char ch, int px){
    StringBuilder sb = new StringBuilder();
    int cw = render.getFontWidth(String.valueOf(ch));
    int n = px / cw;
    for(int i = 0; i < n; i++) sb.append(ch);
    return sb.toString();
}

void sendIRC(int id, String d){
    if(w != null && w.isOpen()){
        w.send("{\"id\":"+id+",\"data\":\""+e(d)+"\"}");
    }
}

void mP(String m){
    Json j = new Json(m);
    if(!j.exists()){
        client.print(P + " &cInvalid JSON: " + m);
        return;
    }
    int id = Integer.parseInt(j.get("id", "-1"));
    String d = j.get("data", "");
    if(id == 0) {
        Message ms = new Message(d);
        client.print(ms);
    }
}

void onPreUpdate(){
    long t = client.time();
    if(w != null && w.isOpen() && t - l > 1000 && !q.isEmpty()){
        sendIRC(0, q.remove(0));
        l = t;
    }
    if(rc && t - lr > 5000){
        client.print(P + " &7Attempting to reconnect...");
        lr = t; c();
    }
}

String e(String s){ return s.replace("\\", "\\\\").replace("\"", "\\\""); }