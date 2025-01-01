/* Allows you to run the /rq command to instantly requeue 95% of hypixel gamemodes */

String mode = "";
int ticks = 0;
boolean scoreboard = false, check = false;

void onPreUpdate() {
    if (check && !scoreboard && world.getScoreboard() != null) scoreboard = true;
    if (scoreboard && ++ticks == 20) {
        client.chat("/locraw");
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (check && msg.startsWith("{")) {
        check = false;
        try {
            if (!msg.contains("REPLAY") && !msg.equals("{\"server\":\"limbo\"}")) mode = msg.split("mode\":\"")[1].split("\"")[0];
        } catch (Exception e) {}
        client.log(msg);
        client.log(mode);
        return false;
    }
    return true;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        if (!c01.message.equals("/rq")) return true;
        client.chat("/play " + mode);
        return false;
    }
    return true;
}

void onWorldJoin(Entity entity) {
    if (client.getPlayer() == entity) {
        scoreboard = false;
        check = true;
        ticks = 0;
    }
}

void onLoad() {
    scoreboard = false;
    check = true;
    ticks = 0;
}