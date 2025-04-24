/* 
    overlay that i never finished because i quit and then game died
*/

String hypixelKey;
String pugKey;
String urchinKey;

final String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

List<Map<String, Object>> alerts = new ArrayList<>();
Map<String, Map<String, Object>> statsCache = new ConcurrentHashMap<>();
Map<String, Map<String, Object>> pingCache = new ConcurrentHashMap<>();

final static String starKey = "star",
                    fkdrKey = "fkdr",
                    winstreakKey = "winstreaks",
                    sessionKey = "session",
                    pingKey = "ping";

final static String starValue = "starvalue",
                    fkdrValue = "fkdrvalue",
                    indexValue = "indexvalue",
                    pingValue = "pingvalue",
                    sessionValue = "sessionvalue",
                    winstreakValue = "winstreakvalue";

void onLoad() {

    /* 
    - add your columns here
    - addColumn(Button Display Name, Column Title, Stats Key); 
    */

    addColumn("Encounters", "E", encountersKey);
    addColumn("Player Heads", "H", headsKey);
    addColumn("Username", "Player", playerKey);
    modules.registerButton("Show Ranks", true);
    addColumn("Star", "Star", starKey);
    addColumn("FKDR", "FK/D", fkdrKey);
    addColumn("Winstreaks", "WS", winstreakKey);
    modules.registerSlider("Winstreak Mode", "", 0, new String[] { "Overall", "Solos", "Doubles", "Threes", "Fours", "4v4" });
    addColumn("Tags", "Tags", tagsKey);
    addColumn("Session", "Online", sessionKey);
    addColumn("Ping", "Ping", pingKey);

    /*
    - add sorting options here
    - if you want to sort a custom value, create a value key to use for it and store it as a double/int under that key
    */

    addSortingOption("Encounters", encountersValue);
    addSortingOption("Star", starValue);
    addSortingOption("FKDR", fkdrValue);
    addSortingOption("Index", indexValue);
    addSortingOption("Winstreak", winstreakValue);
    addSortingOption("Join Time", joinValue);
    addSortingOption("Ping", pingValue);

    /* 
    - add tags in the order which they will be displayed 
    - store your tag string under the key you registered the tag with
    - addTag("example");
    - then, you would store your tag you want to render under "example" when you parse
    */

    addTag("nofinaldeaths");
    addTag("language");
    addTag("bl");
    addTag("ubl");
    
    registerDefaultButtons(); // don't touch
}

void onPlayerAdd(String uuid) {
    handlePlayerStats(uuid, getLobbyId());
    handlePlayerPing(uuid, getLobbyId());
    handlePlayerBlacklist(uuid, getLobbyId());
    handleUrchin(uuid, getLobbyId());
}

void onManualPlayerAdd(String uuid) {
    statsCache.remove(uuid);
    pingCache.remove(uuid);
    handlePlayerStats(uuid, getLobbyId());
    handlePlayerPing(uuid, getLobbyId());
    handlePlayerBlacklist(uuid, getLobbyId());
    handleUrchin(uuid, getLobbyId());
}

void onWorldSwap() {

}

void handlePlayerStats(String uuid, String lobby) {
    if (hypixelKey == null || hypixelKey.isEmpty()) return;
    Map<String, Object> cachedStats = statsCache.get(uuid);
    if (cachedStats == null || client.time() > (long) cachedStats.get("cachetime")) {
        if (cachedStats != null) statsCache.remove(uuid);
        client.async(() -> {
            Map<String, Object> playerStats = new ConcurrentHashMap<>();
            try {
                String url = "https://api.hypixel.net/v2/player?key=" + hypixelKey + "&uuid=" + uuid;
                Object[] playerStatsRequest = get(url, 3000);
                if ((int)playerStatsRequest[1] == 200) {
                    playerStats = parseStats((Json)playerStatsRequest[0], uuid);
                } else {
                    client.print(getChatPrefix() + "&eHTTP Error &3" + playerStatsRequest[1] + " &ewhile getting stats.");
                    client.log("HTTP Error " + playerStatsRequest[1] + " getting stats on " + uuid);
                    playerStats.put("error", true);
                }
            } catch (Exception e) {
                client.print(getChatPrefix() + "&eRuntime error while getting stats.");
                client.log("Runtime error getting stats on " + uuid + ": " + e);
                playerStats.put("error", true);
            }

            if (isInOverlay(uuid) && !hasChangedLobby(lobby)) addToOverlay(uuid, playerStats);
        });
    } else {
        Map<String, Object> dereference = new ConcurrentHashMap<>(cachedStats);

        long lastLogin = ((Number) dereference.get("login")).longValue();
        long lastLogout = ((Number) dereference.get("logout")).longValue();
        boolean statusOn = lastLogin != 0;
        String coloredSession = util.colorSymbol + "cAPI";
        if (statusOn) {
            if (lastLogin - lastLogout > -2000 && client.time() - lastLogin < 43200000) {
                lastLogout = client.time();
                coloredSession = calculateRelativeTimestamp(lastLogin, lastLogout);
                coloredSession = getSessionColor(lastLogin, lastLogout, coloredSession);
            } else {
                coloredSession = util.colorSymbol + "cOFFLINE";
            }
        }
        
        dereference.put(sessionKey, coloredSession);
        addToOverlay(uuid, dereference);
    }
}

Map<String, Object> parseStats(Json jsonData, String uuid) {
    Map<String, Object> stats = new ConcurrentHashMap<>();
    try {
        Json playerNode = jsonData.get("player");
        if (playerNode.type() != Json.Type.OBJECT) {
            stats.put("nicked", true);
            return stats;
        }
        Json player = playerNode;

        String username = player.has("displayname") ? player.get("displayname").asString() : "";
        String formattedRank = getFormattedRank(jsonData);
        String rankColor = formattedRank.substring(0, 2);
        String formattedUsername = formattedRank.length() == 2 ? formattedRank + username : formattedRank + " " + username;
        stats.put("usernamewithrank", formattedUsername);
        stats.put("usernamewithrankcolor", rankColor + username);

        String language = player.has("userLanguage") ? player.get("userLanguage").asString() : "ENGLISH";
        if (!"ENGLISH".equals(language)) {
            stats.put("language", util.colorSymbol + "3L");
        }

        Json statsObj = player.get("stats");
        Json bw = statsObj.type() == Json.Type.OBJECT && statsObj.get("Bedwars").type() == Json.Type.OBJECT ? statsObj.get("Bedwars") : Json.object();

        int experience = bw.has("Experience") ? bw.get("Experience").asInt() : 0;
        int star = (int) Math.floor(expToStars(experience));
        stats.put(starKey, getPrestigeColor(star));
        stats.put(starValue, star);

        double fk = bw.has("final_kills_bedwars") ? Double.parseDouble(bw.get("final_kills_bedwars").asString()) : 0;
        double fd = bw.has("final_deaths_bedwars") ? Double.parseDouble(bw.get("final_deaths_bedwars").asString()) : 0;
        if (fd == 0) stats.put("nofinaldeaths", util.colorSymbol + "5Z");
        double rawKdr = (fd == 0 ? fk : fk / fd);
        double kdr = rawKdr < 10 ? util.round(rawKdr, 2) : util.round(rawKdr, 1);
        stats.put(fkdrKey, getFkdrColor(formatDoubleStr(kdr)));
        stats.put(fkdrValue, kdr);

        double index = star * Math.pow(kdr, 2);
        stats.put(indexValue, index);

        long lastLogin = player.has("lastLogin") ? player.get("lastLogin").asLong() : 0L;
        long lastLogout = player.has("lastLogout") ? player.get("lastLogout").asLong() : 0L;
        stats.put("login", lastLogin);
        stats.put("logout", lastLogout);
        boolean online = lastLogin != 0;
        String sessionColor = util.colorSymbol + "cAPI";
        if (online) {
            if (lastLogin - lastLogout > -2000) {
                lastLogout = client.time();
                String rel = calculateRelativeTimestamp(lastLogin, lastLogout);
                sessionColor = getSessionColor(lastLogin, lastLogout, rel);
            } else {
                sessionColor = util.colorSymbol + "cOFFLINE";
            }
        }
        stats.put(sessionKey, sessionColor);
        stats.put(sessionValue, lastLogin * -1);

        String mode = parseWinstreakMode((int) modules.getSlider(scriptName, "Winstreak Mode"));
        boolean disabled = bw.has("games_played_bedwars_1") && bw.get("games_played_bedwars_1").asInt() > 0 && !bw.has(mode + "winstreak");
        int ws = disabled ? 0 : bw.has(mode + "winstreak") ? bw.get(mode + "winstreak").asInt() : 0;
        stats.put(winstreakKey, getWinstreakColor(String.valueOf(ws)));
        stats.put(winstreakValue, ws);

        boolean highWs = ws > 50;
        long duration = highWs ? 600000L : Math.max(300, Math.min(86400, 60 * (60 * ((int) fd / 120)))) * 1000L;
        stats.put("cachetime", client.time() + duration);
        statsCache.put(uuid, stats);
    } catch (Exception e) {
        client.log("Error in parseStats on " + uuid + ": " + e);
        client.print(getChatPrefix() + "&eError detected. Please check &3latest.log&e.");
        stats.put("error", true);
    }
    return stats;
}

void handlePlayerPing(String uuid, String lobby) {
    if (pugKey == null || pugKey.isEmpty()) return;
    Map<String, Object> cachedPing = pingCache.get(uuid);
    if (cachedPing == null || client.time() > (long) cachedPing.get("cachetime")) {
        if (cachedPing != null) pingCache.remove(uuid);
        client.async(() -> {
            Map<String, Object> playerPing = new ConcurrentHashMap<>();
            try {
                String url = "https://privatemethod.xyz/api/lobby/ping?key=" + pugKey + "&uuid=" + uuid;
                Object[] playerPingRequest = get(url, 3000);
                if ((int)playerPingRequest[1] == 200) {
                    playerPing = parsePing((Json)playerPingRequest[0], uuid);
                } else {
                    client.print(getChatPrefix() + " &eHTTP Error &3" + playerPingRequest[1] + " &ewhile getting ping.");
                    client.log("HTTP Error " + playerPingRequest[1] + " getting ping on " + uuid);
                    playerPing.put("error", true);
                }

            } catch (Exception e) {
                client.print(getChatPrefix() + " &eRuntime error while getting ping.");
                client.log("Runtime error getting ping on " + uuid + ": " + e);
                playerPing.put("error", true);
            }

            if (isInOverlay(uuid) && !hasChangedLobby(lobby)) addToOverlay(uuid, playerPing);
        });
    } else {
        addToOverlay(uuid, cachedPing);
    }
}

void handleUrchin(String uuid, String lobby) {
    if (urchinKey == null || urchinKey.isEmpty()) return;
    client.async(() -> {
        Map<String, Object> playerData = new ConcurrentHashMap<>();
        try {
            String url = "http://urchin.ws/cubelify?id=" + toDashedUUID(uuid) + "&name=placeholder&sources=GAME&key=" + urchinKey;
            Object[] playerDataRequest = get(url, 10000);
            if ((int)playerDataRequest[1] == 200) {
                playerData = parseUrchin((Json)playerDataRequest[0], uuid);
            } else {
                client.print(getChatPrefix() + "&eHTTP Error &3" + playerDataRequest[1] + " &ewhile getting urchin data.");
                client.log("HTTP Error " + playerDataRequest[1] + " getting urchin data on " + uuid);
                playerData.put("error", true);
            }
        } catch (Exception e) {
            client.print(getChatPrefix() + "&eRuntime error while retrieving urchin data.");
            client.log("Runtime error getting urchin data on " + uuid + ": " + e);
            playerData.put("error", true);
        }

        if (isInOverlay(uuid) && !hasChangedLobby(lobby)) addToOverlay(uuid, playerData);
    });
}

Map<String, Object> parseUrchin(Json jsonData, String uuid) {
    Map<String, Object> urchinData = new ConcurrentHashMap<>();

    try {
        Json tagsNode = jsonData.get("tags");
        if (tagsNode.type() != Json.Type.ARRAY) {
            return urchinData;
        }

        List<Json> tags = tagsNode.asArray();
        if (tags.isEmpty()) {
            return urchinData;
        }

        Map<String, Object> playerData = overlayPlayers.get(uuid);
        if (playerData == null) {
            return urchinData;
        }

        String username = util.color(playerData.get("username").toString() + "&r");

        for (Json tag : tags) {
            String icon = tag.has("icon") ? tag.get("icon").asString() : "";
            if (!blacklistIcons.contains(icon)) {
                continue;
            }

            String tooltip = tag.has("tooltip") ? tag.get("tooltip").asString() : "Unknown reason";
            tooltip = tooltip.replace("&", "and").replace("\n", " ");
            String parsedTooltip = parseTooltip(tooltip);

            client.print(chatPrefix + "&3" + username + "&7: " + parsedTooltip);

            String ubl = parsedTooltip.length() >= 3 ? parsedTooltip.substring(0, 3) : parsedTooltip;
            urchinData.put("ubl", util.color(ubl));
            urchinData.put(tagsKey, "");
            break;
        }

    } catch (Exception e) {
        client.log("Error in parseUrchin: " + e);
        client.print(chatPrefix + " &eError detected. Please check &3latest.log&e.");
        client.log(jsonData.toString());
    }

    return urchinData;
}

Map<String, String> LABEL_COLORS = new HashMap<>();
{
    LABEL_COLORS.put("Sniper", "&c");
    LABEL_COLORS.put("Possible Sniper", "&c");
    LABEL_COLORS.put("Legit Sniper", "&c");
    LABEL_COLORS.put("Confirmed Cheater", "&4");
    LABEL_COLORS.put("Blatant Cheater", "&6");
    LABEL_COLORS.put("Closet Cheater", "&6");
    LABEL_COLORS.put("Caution", "&6");
    LABEL_COLORS.put("Account", "&6");
    LABEL_COLORS.put("Info", "&f");
}
        
HashSet<String> blacklistIcons = new HashSet<>(Arrays.asList(
    "mdi-alert-octagram",
    "mdi-alert-octagram-outline",
    "mdi-alert-rhombus-outline",     
    "mdi-information-outline",   
    "mdi-target-variant",           
    "mdi-alert-decagram",  
    "mdi-alert-decagram-outline", 
    "mdi-account-alert"       
));

String parseTooltip(String tooltip) {
    int open = tooltip.indexOf('(');
    int close = (open != -1) ? tooltip.indexOf(')', open) : -1;
    String mainLabel = tooltip, username = "", time = "", reason = "";
    
    if (open != -1 && close > open) {
        mainLabel = tooltip.substring(0, open).trim();
        String parenthetical = tooltip.substring(open + 1, close).trim();
        String extra = tooltip.substring(close + 1).trim();
        if (extra.startsWith("-")) {
            extra = extra.substring(1).trim();
        }
        
        if (parenthetical.startsWith("Added by ")) {
            String[] parts = parenthetical.substring(9).trim().split("\\s+");
            if (parts.length >= 3 && "ago".equalsIgnoreCase(parts[parts.length - 1])) {
                username = parts[0];
                time = parts[1];
            }
        } else if (parenthetical.startsWith("Added ")) {
            String content = parenthetical.substring(6).trim();
            if (content.contains(" - ")) {
                String[] splitParts = content.split(" - ", 2);
                String[] tokens = splitParts[0].trim().split("\\s+");
                if (tokens.length >= 2 && "ago".equalsIgnoreCase(tokens[1])) {
                    time = tokens[0];
                }
                reason = splitParts[1].trim();
            } else {
                String[] tokens = content.split("\\s+");
                if (tokens.length >= 2 && "ago".equalsIgnoreCase(tokens[1])) {
                    time = tokens[0];
                }
            }
        }
        
        if (!extra.isEmpty()) {
            reason = reason.isEmpty() ? extra : reason + " - " + extra;
        }
    }
    
    String color = LABEL_COLORS.getOrDefault(mainLabel, "&7");
    return new StringBuilder()
           .append(color).append(mainLabel).append("&r")
           .append(username.isEmpty() ? "" : " &eby&3 " + username)
           .append(reason.isEmpty() ? "" : " &e-&7 " + reason)
           .append(time.isEmpty() ? "" : " &e(&d" + time + "&e)")
           .toString().trim();
}

String toDashedUUID(String uuidStr) {
    return uuidStr.substring(0, 8) + "-" +
           uuidStr.substring(8, 12) + "-" +
           uuidStr.substring(12, 16) + "-" +
           uuidStr.substring(16, 20) + "-" +
           uuidStr.substring(20, 32);
}

Map<String, Object> parsePing(Json jsonData, String uuid) {
    Map<String, Object> pingdata = new ConcurrentHashMap<>();

    try {
        List<Json> array = jsonData.get("data").asArray();

        if (array.isEmpty()) {
            pingdata.put("ping", "");
            return pingdata;
        }

        int count = Math.min(array.size(), 7);
        int sum = 0;

        for (int i = 0; i < count; i++) {
            Json entry = array.get(i);
            try {
                sum += entry.get("avg").asInt();
            } catch (Exception e) {
                count--;
            }
        }

        int ping = (count > 0 ? sum / count : 0);

        String coloredPing = getPingColor(String.valueOf(ping));
        pingdata.put(pingValue, ping);
        pingdata.put(pingKey, coloredPing);
        pingdata.put("cachetime", client.time() + 1800000L);
        pingCache.put(uuid, pingdata);

    } catch (Exception e) {
        client.log("Error in parsePing: " + e);
        client.print("&eError detected. Please check &3latest.log&e.");
    }

    return pingdata;
}

void handlePlayerBlacklist(String uuid, String lobby) {
    if (pugKey == null || pugKey.isEmpty()) return;
    else return;
    client.async(() -> {
        Map<String, Object> playerBlacklist = new ConcurrentHashMap<>();
        try {
            String url = "https://privatemethod.xyz/api/blacklist/check?key=" + pugKey + "&uuid=" + uuid;
            Object[] playerBlacklistRequest = get(url, 3000);
            if ((int)playerBlacklistRequest[1] == 200) {
                playerBlacklist = parseBlacklist((Json)playerBlacklistRequest[0], uuid);
            } else {
                client.print(getChatPrefix() + "&eHTTP Error &3" + playerBlacklistRequest[1] + " &ewhile getting blacklist.");
                client.log("HTTP Error " + playerBlacklistRequest[1] + " getting blacklist on " + uuid);
                playerBlacklist.put("error", true);
            }
        } catch (Exception e) {
            client.print(getChatPrefix() + "&eRuntime error while getting blacklist.");
            client.log("Runtime error getting blacklist on " + uuid + ": " + e);
            playerBlacklist.put("error", true);
        }

        if (isInOverlay(uuid) && !hasChangedLobby(lobby)) addToOverlay(uuid, playerBlacklist);
    });
}

Map<String, Object> parseBlacklist(Json jsonData, String uuid) {
    Map<String, Object> blacklistData = new ConcurrentHashMap<>();
    try {
        Json blField = jsonData.get("blacklisted");
        boolean blacklisted = blField.type() == Json.Type.BOOLEAN && blField.asBoolean();
        if (!blacklisted) {
            return blacklistData;
        }

        List<Json> entries = jsonData.get("data").asArray();
        if (entries.isEmpty()) {
            return blacklistData;
        }

        Json highest = entries.get(0);
        for (Json entry : entries) {
            int threat = entry.has("threat") ? entry.get("threat").asInt() : 0;
            int highestThreat = highest.has("threat") ? highest.get("threat").asInt() : 0;
            if (threat > highestThreat) {
                highest = entry;
            } else if (threat == highestThreat) {
                long ts = entry.has("timestamp") ? entry.get("timestamp").asLong() : Long.MAX_VALUE;
                long highestTs = highest.has("timestamp") ? highest.get("timestamp").asLong() : Long.MAX_VALUE;
                if (ts < highestTs) {
                    highest = entry;
                }
            }
        }

        int threat = highest.get("threat").asInt();
        String blacklister = highest.get("discord_username").asString();
        long timeAdded = highest.get("timestamp").asLong();
        String timeAgo = calculateRelativeTimestamp(timeAdded, client.time());
        String type = convertType(threat);
        String color = getBlacklistColor(threat);
        if (color == null || type == null) {
            return blacklistData;
        }

        Map<String, Object> playerData = overlayPlayers.get(uuid);
        if (playerData == null) {
            return blacklistData;
        }
        String username = util.color(playerData.get("username").toString() + "&r");

        String reason = highest.has("reason") ? highest.get("reason").asString().trim() : "";
        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        String payload = new StringBuilder()
            .append(color).append(type).append("&r")
            .append(" &eby &3").append(blacklister)
            .append(" &e- &7").append(reason.isEmpty() ? "No reason provided" : reason)
            .append(" &e(&d").append(timeAgo).append("&e)")
            .toString();

        client.print(getChatPrefix() + "&3" + username + "&7: " + payload);

        addAlert(util.color("&lLazify Overlay"), util.color(username + " &7blacklisted for " + color + type + "&7."), 8000, "");
        if (threat >= 3) {
            client.ping();
        }

        blacklistData.put("blacklisted", true);
        blacklistData.put("bl", color + type.substring(0, 1).toUpperCase());
        blacklistData.put(tagsKey, "");
    } catch (Exception e) {
        client.log("Error in parseBlacklist: " + e);
        client.print(getChatPrefix() + " &eError detected. Please check &3latest.log&e.");
    }
    return blacklistData;
}

String convertType(int type) {
    switch (type) {
        case 6: return "sniper";
        case 5: return "cheater";
        case 4: return "beamed";
        case 3: return "risky";
        case 2: return "bot";
        case 1: return "annoying";
        default: return null;
    }
}

String getBlacklistColor(int type) {
    switch (type) {
        case 6: return util.colorSymbol + "4";
        case 5: return util.colorSymbol + "c";
        case 4: return util.colorSymbol + "6";
        case 3: return util.colorSymbol + "6";
        case 2: return util.colorSymbol + "e";
        case 1: return util.colorSymbol + "e";
        default: return null;
    }
}

String getPingColor(String ping) {
    if (ping.isEmpty()) {
        return "";
    }

    int realping = Integer.parseInt(ping);
    
    if (realping > 300) {
        return util.colorSymbol + '4' + realping;
    } else if (realping > 200) {
        return util.colorSymbol + 'c' + realping;
    } else if (realping > 170) {
        return util.colorSymbol + 'e' + realping;
    } else if (realping > 120) {
        return util.colorSymbol + '2' + realping;
    } else {
        return util.colorSymbol + 'a' + realping;
    }
}

String parseWinstreakMode(int i) {
    switch (i) {
        case 0: return "";
        case 1: return "eight_one_";
        case 2: return "eight_two_";
        case 3: return "four_three_";
        case 4: return "four_four_";
        case 5: return "two_four_";
        default: return "";
    }
}

double expToStars(int exp) {
    int levelBase = (exp / 487000) * 100;
    int expMod = exp % 487000;
    int[][] levels = {
        {7000, 4, 5000},
        {3500, 3, 3500},
        {1500, 2, 2000},
        {500, 1, 1000},
        {0, 0, 500}
    };
    
    for (int[] lvl : levels) {
        if (expMod < lvl[0]) continue;
        double result = levelBase + lvl[1] + ((double) (expMod - lvl[0]) / lvl[2]);
        return result;
    }
    
    return 0;
}

String getRank(Json playerData) {
    if (playerData == null) {
        return null;
    }
    Json player = playerData.get("player");
    if (player.type() != Json.Type.OBJECT) {
        return null;
    }

    if (player.has("prefix")) {
        String prefix = player.get("prefix").asString();
        if (!prefix.isEmpty()) {
            return prefix.replace("§", util.colorSymbol);
        }
    }

    if (player.has("rank")) {
        String rank = player.get("rank").asString();
        if ("STAFF".equals(rank)) {
            return "\u12DE";
        }
        if ("YOUTUBER".equals(rank)) {
            return "YOUTUBE";
        }
        if (!"NORMAL".equals(rank)) {
            return rank;
        }
    }

    String pkg = null;
    if (player.has("newPackageRank") && !player.get("newPackageRank").asString().isEmpty()) {
        pkg = player.get("newPackageRank").asString();
    } else if (player.has("packageRank")) {
        pkg = player.get("packageRank").asString();
    }
    if (pkg == null || "NONE".equals(pkg)) {
        return null;
    }
    if (pkg.startsWith("MVP")) {
        if (player.has("monthlyPackageRank") && "SUPERSTAR".equals(player.get("monthlyPackageRank").asString())) {
            return "MVP++";
        }
        return pkg.length() == 3 ? pkg : "MVP+";
    }
    if (pkg.startsWith("VIP")) {
        return pkg.length() == 3 ? pkg : "VIP+";
    }

    return null;
}

String getFormattedRank(Json playerData) {
    String defaultColor = util.color("&7");
    if (playerData == null) {
        return defaultColor;
    }
    String rank = getRank(playerData);
    if (rank == null) {
        return defaultColor;
    }
    Json player = playerData.get("player");
    String plusColor = player.has("rankPlusColor") ? player.get("rankPlusColor").asString() : "RED";

    String colorCode;
    switch (plusColor) {
        case "BLACK": colorCode = "&0"; break;
        case "DARK_BLUE": colorCode = "&1"; break;
        case "DARK_GREEN": colorCode = "&2"; break;
        case "DARK_AQUA": colorCode = "&3"; break;
        case "DARK_RED": colorCode = "&4"; break;
        case "DARK_PURPLE": colorCode = "&5"; break;
        case "GOLD": colorCode = "&6"; break;
        case "GRAY": colorCode = "&7"; break;
        case "DARK_GRAY": colorCode = "&8"; break;
        case "BLUE": colorCode = "&9"; break;
        case "GREEN": colorCode = "&a"; break;
        case "AQUA": colorCode = "&b"; break;
        case "RED": colorCode = "&c"; break;
        case "LIGHT_PURPLE": colorCode = "&d"; break;
        case "YELLOW": colorCode = "&e"; break;
        case "WHITE": colorCode = "&f"; break;
        default: colorCode = "&7";
    }

    switch (rank) {
        case "VIP":
            return util.color("&a[VIP]");
        case "VIP+":
            return util.color("&a[VIP&6+&a]");
        case "MVP":
            return util.color("&b[MVP]");
        case "MVP+":
            return util.color("&b[MVP" + colorCode + "+" + "&b]");
        case "MVP++":
            return util.color("&6[MVP" + colorCode + "++&6]");
        case "YOUTUBE":
            return util.color("&c[&fYOUTUBE&c]");
        case "\u12DE":
            return util.color("&c[&6\u12DE&c]");
        default:
            return rank;
    }
}

String getWinstreakColor(String winstreak) {
    if (winstreak.isEmpty()) {
        return "";
    }

    int realwinstreak = Integer.parseInt(winstreak);
    if (realwinstreak == 0) {
        return "";
    }

    if (realwinstreak >= 1000) {
        return util.color("&5") + realwinstreak;
    } else if (realwinstreak >= 500) {
        return util.color("&d") + realwinstreak;
    } else if (realwinstreak >= 300) {
        return util.color("&4") + realwinstreak;
    } else if (realwinstreak >= 150) {
        return util.color("&c") + realwinstreak;
    } else if (realwinstreak >= 100) {
        return util.color("&6") + realwinstreak;
    } else if (realwinstreak >= 75) {
        return util.color("&e") + realwinstreak;
    } else if (realwinstreak >= 50) {
        return util.color("&2") + realwinstreak;
    } else if (realwinstreak >= 25) {
        return util.color("&a") + realwinstreak;
    } else {
        return util.color("&7") + realwinstreak;
    }
}

String getSessionColor(long lastLogin, long lastLogout, String sessionFormatted) {
    long session = lastLogout - lastLogin;

    if (session > 21600000) {
        return util.color("&4") + sessionFormatted;
    } else if (session > 14400000) {
        return util.color("&c") + sessionFormatted;
    } else if (session > 9000000) {
        return util.color("&6") + sessionFormatted;
    } else if (session > 7200000) {
        return util.color("&e") + sessionFormatted;
    } else if (session > 1200000) {
        return util.color("&a") + sessionFormatted;
    } else if (session > 600000) {
        return util.color("&e") + sessionFormatted;
    } else if (session > 300000) {
        return util.color("&e") + sessionFormatted;
    } else if (session > 150000) {
        return util.color("&c") + sessionFormatted;
    } else {
        return util.color("&4") + sessionFormatted;
    }
}

String getFkdrColor(String fkdr) {
    double realfkdr = Double.parseDouble(fkdr);
    if (realfkdr > 1000) {
        return util.color("&5") + fkdr;
    } else if (realfkdr > 100) {
        return util.color("&4") + fkdr;
    } else if (realfkdr > 10) {
        return util.color("&c") + fkdr;
    } else if (realfkdr > 5) {
        return util.color("&6") + fkdr;
    } else if (realfkdr > 2.4) {
        return util.color("&e") + fkdr;
    } else if (realfkdr > 1.4) {
        return util.color("&f") + fkdr;
    } else {
        return util.color("&7") + fkdr;
    }
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

String calculateRelativeTimestamp(long lastLogin, long lastLogout) {
    long timeSince = (lastLogout - lastLogin) / 1000L;
    long remainingTime = timeSince;

    long years = remainingTime / 31557600L;
    remainingTime %= 31557600L;
    long months = remainingTime / 2629800L;
    remainingTime %= 2629800L;
    long days = remainingTime / 86400L;
    remainingTime %= 86400L;
    long hours = remainingTime / 3600L;
    remainingTime %= 3600L;
    long minutes = remainingTime / 60L;
    long seconds = remainingTime % 60L;

    StringBuilder msg = new StringBuilder();
    int componentsAdded = 0;

    if (years > 0 && componentsAdded < 2) {
        msg.append(years).append("y");
        componentsAdded++;
    }
    if (months > 0 && componentsAdded < 2) {
        msg.append(months).append("mo");
        componentsAdded++;
    }
    if (days > 0 && componentsAdded < 2) {
        msg.append(days).append("d");
        componentsAdded++;
    }
    if (hours > 0 && componentsAdded < 2) {
        msg.append(hours).append("h");
        componentsAdded++;
    }
    if (minutes > 0 && componentsAdded < 2) {
        msg.append(minutes).append("m");
        componentsAdded++;
    }
    if ((seconds > 0 && componentsAdded < 2) || timeSince == 0) {
        msg.append(seconds).append("s");
        componentsAdded++;
    }

    return msg.toString();
}

String getPrestigeColor(int number) {
    int prestige = (number / 100) * 100;
    String nums = "", result;
    if (prestige >= 1000) {
        nums = String.format("%04d", number);
    }
    switch (prestige) {
        case 0:
            result = "&7[" + number + "\u272B]";
            break;
        case 100:
            result = "&f[" + number + "\u272B]";
            break;
        case 200:
            result = "&6[" + number + "\u272B]";
            break;
        case 300:
            result = "&b[" + number + "\u272B]";
            break;
        case 400:
            result = "&2[" + number + "\u272B]";
            break;
        case 500:
            result = "&3[" + number + "\u272B]";
            break;
        case 600:
            result = "&4[" + number + "\u272B]";
            break;
        case 700:
            result = "&d[" + number + "\u272B]";
            break;
        case 800:
            result = "&9[" + number + "\u272B]";
            break;
        case 900:
            result = "&5[" + number + "\u272B]";
            break;
        case 1000: {
            result = "&c[&6" + nums.charAt(0) + "&e" + nums.charAt(1)
                   + "&a" + nums.charAt(2) + "&b" + nums.charAt(3)
                   + "&d\u272B&5]";
            break;
        }
        case 1100:
            result = "&7[&f" + number + "&7\u272A]";
            break;
        case 1200:
            result = "&7[&e" + number + "&6\u272A&7]";
            break;
        case 1300:
            result = "&7[&b" + number + "&3\u272A&7]";
            break;
        case 1400:
            result = "&7[&a" + number + "&2\u272A&7]";
            break;
        case 1500:
            result = "&7[&3" + number + "&9\u272A&7]";
            break;
        case 1600:
            result = "&7[&c" + number + "&4\u272A&7]";
            break;
        case 1700:
            result = "&7[&d" + number + "&5\u272A&7]";
            break;
        case 1800:
            result = "&7[&9" + number + "&1\u272A&7]";
            break;
        case 1900:
            result = "&7[&5" + number + "&8\u272A&7]";
            break;
        case 2000: {
            result = "&8[&7" + nums.charAt(0) + "&f" + nums.charAt(1)
                   + nums.charAt(2) + "&7" + nums.charAt(3) + "\u272A&8]";
            break;
        }
        case 2100: {
            result = "&f[" + nums.charAt(0) + "&e" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&l\u269D&r&6]";
            break;
        }
        case 2200: {
            result = "&6[" + nums.charAt(0) + "&f" + nums.charAt(1)
                   + nums.charAt(2) + "&b" + nums.charAt(3) + "&3&l\u269D&r&3]";
            break;
        }
        case 2300: {
            result = "&5[" + nums.charAt(0) + "&d" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&e&l\u269D&r&e]";
            break;
        }
        case 2400: {
            result = "&b[" + nums.charAt(0) + "&f" + nums.charAt(1)
                   + nums.charAt(2) + "&7" + nums.charAt(3) + "&l\u269D&r&8]";
            break;
        }
        case 2500: {
            result = "&f[" + nums.charAt(0) + "&a" + nums.charAt(1)
                   + nums.charAt(2) + "&2" + nums.charAt(3) + "&l\u269D&r&2]";
            break;
        }
        case 2600: {
            result = "&4[" + nums.charAt(0) + "&c" + nums.charAt(1)
                   + nums.charAt(2) + "&d" + nums.charAt(3) + "&l\u269D&r&d]";
            break;
        }
        case 2700: {
            result = "&e[" + nums.charAt(0) + "&f" + nums.charAt(1)
                   + nums.charAt(2) + "&8" + nums.charAt(3) + "&l\u269D&r&8]";
            break;
        }
        case 2800: {
            result = "&a[" + nums.charAt(0) + "&2" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&l\u269D&r&e]";
            break;
        }
        case 2900: {
            result = "&b[" + nums.charAt(0) + "&3" + nums.charAt(1)
                   + nums.charAt(2) + "&9" + nums.charAt(3) + "&l\u269D&r&1]";
            break;
        }
        case 3000: {
            result = "&e[" + nums.charAt(0) + "&6" + nums.charAt(1)
                   + nums.charAt(2) + "&c" + nums.charAt(3) + "&l\u269D&r&4]";
            break;
        }
        case 3100: {
            result = "&9[" + nums.charAt(0) + "&2" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&l\u2725&r&e]";
            break;
        }
        case 3200: {
            result = "&c[&4" + nums.charAt(0) + "&7" + nums.charAt(1)
                   + nums.charAt(2) + "&4" + nums.charAt(3) + "&c&l\u2725&r&c]";
            break;
        }
        case 3300: {
            result = "&9[" + nums.charAt(0) + "" + nums.charAt(1)
                   + "&d" + nums.charAt(2) + "&c" + nums.charAt(3) + "&l\u2725&r&4]";
            break;
        }
        case 3400: {
            result = "&2[&a" + nums.charAt(0) + "&d" + nums.charAt(1)
                   + nums.charAt(2) + "&5" + nums.charAt(3) + "&l\u2725&r&2]";
            break;
        }
        case 3500: {
            result = "&c[" + nums.charAt(0) + "&4" + nums.charAt(1)
                   + nums.charAt(2) + "&2" + nums.charAt(3) + "&a&l\u2725&r&a]";
            break;
        }
        case 3600: {
            result = "&a[" + nums.charAt(0) + "" + nums.charAt(1)
                   + "&b" + nums.charAt(2) + "&9" + nums.charAt(3) + "&l\u2725&r&1]";
            break;
        }
        case 3700: {
            result = "&4[" + nums.charAt(0) + "&c" + nums.charAt(1)
                   + nums.charAt(2) + "&b" + nums.charAt(3) + "&3&l\u2725&r&3]";
            break;
        }
        case 3800: {
            result = "&1[" + nums.charAt(0) + "&9" + nums.charAt(1)
                   + "&5" + nums.charAt(2) + nums.charAt(3) + "&d&l\u2725&r&1]";
            break;
        }
        case 3900: {
            result = "&c[" + nums.charAt(0) + "&a" + nums.charAt(1)
                   + nums.charAt(2) + "&3" + nums.charAt(3) + "&9&l\u2725&r&9]";
            break;
        }
        case 4000: {
            result = "&5[" + nums.charAt(0) + "&c" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&l\u2725&r&e]";
            break;
        }
        case 4100: {
            result = "&e[" + nums.charAt(0) + "&6" + nums.charAt(1)
                   + nums.charAt(2) + "&d" + nums.charAt(3) + "&l\u2725&r&5]";
            break;
        }
        case 4200: {
            result = "&1[&9" + nums.charAt(0) + "&3" + nums.charAt(1)
                   + nums.charAt(2) + "&b" + nums.charAt(3) + "&7&l\u2725&r&7]";
            break;
        }
        case 4300: {
            result = "&0[&5" + nums.charAt(0) + "&8" + nums.charAt(1)
                   + nums.charAt(2) + "&5" + nums.charAt(3) + "&l\u2725&r&0]";
            break;
        }
        case 4400: {
            result = "&2[" + nums.charAt(0) + "&a" + nums.charAt(1)
                   + nums.charAt(2) + "&e" + nums.charAt(3) + "&5&l\u2725&r&d]";
            break;
        }
        case 4500: {
            result = "&f[" + nums.charAt(0) + "&b" + nums.charAt(1)
                   + nums.charAt(2) + "&2" + nums.charAt(3) + "&l\u2725&r&2]";
            break;
        }
        case 4600: {
            result = "&2[&b" + nums.charAt(0) + "&e" + nums.charAt(1)
                   + nums.charAt(2) + "&6" + nums.charAt(3) + "&l\u2725&r&5]";
            break;
        }
        case 4700: {
            result = "&f[" + nums.charAt(0) + "&c" + nums.charAt(1)
                   + nums.charAt(2) + "&9" + nums.charAt(3) + "&1&l\u2725&r&9]";
            break;
        }
        case 4800: {
            result = "&5[" + nums.charAt(0) + "&c" + nums.charAt(1)
                   + "&6" + nums.charAt(2) + "&e" + nums.charAt(3) + "&b&l\u2725&r&3]";
            break;
        }
        case 4900: {
            result = "&2[&a" + nums.charAt(0) + "&f" + nums.charAt(1)
                   + nums.charAt(2) + "&a" + nums.charAt(3) + "&l\u2725&r&2]";
            break;
        }
        case 5000: {
            result = "&4[" + nums.charAt(0) + "&5" + nums.charAt(1)
                   + "&9" + nums.charAt(2) + nums.charAt(3)
                   + "&1&l\u2725&r&0]";
            break;
        }
        default:
            if (prestige > 5000) {
                result = "&4[" + nums.charAt(0) + "&5" + nums.charAt(1)
                        + "&9" + nums.charAt(2) + nums.charAt(3)
                        + "&1&l\u2725&r&0]";
                break;
            }
            result = "&7[" + number + "\u272B]";
    }
    return util.color(result);
}






















Image defaultHead = new Image("https://crafatar.com/avatars/c06f89064c8a49119c29ea1dbd1aab82?overlay=true&size=8", true);

float fontHeight = (float) render.getFontHeight();
float startX = 12;
float startY = 12;
float offsetY = 3;
float lineHeight = fontHeight + offsetY;
float borderWidth = 2.5f;
int background, borderColorRGB, columnTitles;
float textScale = 1;
float endY;
float endX;

final String chromeUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

final String chatPrefix = "&7[&dL&7]&r ";

boolean firstEnable = false;
boolean dowho = true;
boolean didwho = false;
boolean ascending = false;
boolean showYourself = false;
boolean showTeamPrefix = false;
boolean showRank;
boolean lastRank, lastTeam, lastTeamPrefix;
boolean showTeamColors = false;
boolean showHeads = false;
String sortBy;
int status = 0;
int overlayTicks = 5;
float headsSize = fontHeight - 1;

Map<String, Map<String, Object>> overlayPlayers = new ConcurrentHashMap<>();
Map<String, String> ignoredPlayers = new HashMap<>();
ArrayList<String> currentPlayers = new ArrayList<>();
String currentLobby = "";
String lastLobby = "";

List<Map<String, Object>> columns = new ArrayList<>();
List<String> tags = new ArrayList<>();
List<String> sortingOptions = new ArrayList<>();
Map<String, String> parseSortingMode = new HashMap<>();
Map<String, List<Object[]>> playerEncounters = new HashMap<>();
Map<String, String> teams = new HashMap<>();

final static String playerKey = "player",
                    encountersKey = "seen",
                    tagsKey = "tags",
                    headsKey = "playerhead";

final static String encountersValue = "seenvalue",
                    joinValue = "joinvalue";

void registerDefaultButtons() {
    modules.registerButton("Player Heads", true);
    modules.registerButton("Teams", true);
    modules.registerButton("Team Prefix", false);
    modules.registerButton("Show Yourself", false);
    modules.registerSlider("Encounters Timeout (mins)", "", 30, 1, 60, 1);
    modules.registerSlider("Sort By", "", 2, getSortingOptions());
    modules.registerSlider("Sort Mode", "", 0, new String[] { "Ascending", "Descending" });
    modules.registerSlider("Background Opacity", "", 170, 0, 255, 5);
    modules.registerSlider("Background Color", "", 0, 0, 360, 4);
    modules.registerSlider("Header Color", "", 290, 0, 360, 4);
    modules.registerSlider("Border Color", "", 360, 0, 360, 4);
    modules.registerSlider("Corner Radius", "", 5, 0.5, 8, 0.1);
    modules.registerSlider("Font Size", "px", 9, 4.5, 18, 0.5);
}

void defaultSettings() {
    showHeads = modules.getButton(scriptName, "Player Heads");
    showYourself = modules.getButton(scriptName, "Show Yourself");
    showTeamPrefix = modules.getButton(scriptName, "Team Prefix");
    showRank = modules.getButton(scriptName, "Show Ranks");
    showTeamColors = modules.getButton(scriptName, "Teams");
    if (overlayPlayers.size() > 1 && (modules.getSlider(scriptName, "Sort Mode") == 0 ? true : false) != ascending) {
        ascending = modules.getSlider(scriptName, "Sort Mode") == 0 ? true : false;
        sortOverlay();
    }
    ascending = modules.getSlider(scriptName, "Sort Mode") == 0 ? true : false;
    if (overlayPlayers.size() > 1 && !parseSortingMode.get(sortingOptions.get((int) modules.getSlider(scriptName, "Sort By"))).equals(sortBy)) {
        sortBy = parseSortingMode.get(sortingOptions.get((int) modules.getSlider(scriptName, "Sort By")));
        sortOverlay();
    }
    sortBy = parseSortingMode.get(sortingOptions.get((int) modules.getSlider(scriptName, "Sort By")));
    background = getHueRGB((float) modules.getSlider(scriptName, "Background Color"), (int) modules.getSlider(scriptName, "Background Opacity"));
    columnTitles = getHueRGB((float) modules.getSlider(scriptName, "Header Color"), 255);
    borderColorRGB = getHueRGB((float) modules.getSlider(scriptName, "Border Color"), 255);
    textScale = (float)modules.getSlider(scriptName, "Font Size") / (float)render.getFontHeight();
    fontHeight = (float)render.getFontHeight() * textScale;
    headsSize = ((float)render.getFontHeight() - 1f * textScale) * textScale;
    offsetY = 3f * textScale;
    lineHeight = fontHeight + offsetY;

    if (showRank != lastRank || showTeamColors != lastTeam || showTeamPrefix != lastTeamPrefix) {
        doColumns(false);
    }
    lastRank = showRank;
    lastTeam = showTeamColors;
    lastTeamPrefix = showTeamPrefix;
}

boolean isInOverlay(String uuid) {
    return overlayPlayers.containsKey(uuid);
}

String getChatPrefix() {
    return chatPrefix;
}

String getLobbyId() {
    return currentLobby;
}

boolean hasChangedLobby(String inputLobby) {
    return !inputLobby.equals(getLobbyId());
}

boolean hasTeamColor(String uuid) {
    return teams.containsKey(uuid);
}

int bedwarsStatus() {
    return status;
}

void addPlaceholderStats(String player, String username, boolean doName) {
    Map<String, Object> placeholderStats = new ConcurrentHashMap<>();

    for (Map<String, Object> column : columns) {
        boolean enabled = Boolean.TRUE.equals(column.get("enabled"));
        if (!enabled) continue;
        String key = column.get("key").toString();
        if (key.equals(encountersKey)) continue;

        if (key.equals(encountersKey)) {
            placeholderStats.put(key, getSeenColor(1));
            continue;
        } else if (key.equals(playerKey)) {
            if (doName) placeholderStats.put(key, util.color("&7") + username);
            continue;
        } else if (key.equals(headsKey)) {
            if (showHeads) {
                String url = "https://crafatar.com/avatars/" + player + "?overlay=true&size=8";
                Image head = new Image(url, true);
                placeholderStats.put(headsKey, head);
            }
            continue;
        }
        
        placeholderStats.put(key, util.colorSymbol + "7-");
    }

    placeholderStats.put(joinValue, (int) (client.time() / 1000) * -1);
    if (doName) overlayPlayers.put(player, placeholderStats);
    else addToOverlay(player, placeholderStats);
}

void onEnable() {
    if (!firstEnable) {
        client.print(getChatPrefix() + "&eWelcome to &3Lazify&e! Please run &3/ov&e for commands.");

        pugKey = config.get("lazify_pugkey");
        urchinKey = config.get("lazify_urchinkey");
        hypixelKey = config.get("lazify_hypixelkey");
        if (pugKey == null || pugKey.isEmpty()) {
            client.print(getChatPrefix() + "&cNo Pug API key detected.");
        }
        if (urchinKey == null || urchinKey.isEmpty()) {
            client.print(getChatPrefix() + "&cNo Urchin API key detected.");
        }
        if (hypixelKey == null || hypixelKey.isEmpty()) {
            client.print(getChatPrefix() + "&cNo Hypixel API key detected.");
        }

        firstEnable = true;
    }
    overlayTicks = 5;
    defaultSettings();
    updateStatus();
    doColumns(false);
}

void onPreUpdate() {
    if (!alerts.isEmpty() && !bridge.has("pugalert")) {
        bridge.add("pugalert", alerts.remove(0));
    }

    updateStatus();

    if (overlayTicks++ % 5 != 0) { // no need to update every tick
        return;
    }

    defaultSettings();
    doColumns(true);
    
    if (status > 1) {
        HashSet<String> currentEntityUUIDs = new HashSet<>();
        final int threshold = (int) modules.getSlider(scriptName, "Encounters Timeout (mins)") * 60000;
        for (NetworkPlayer pla : world.getNetworkPlayers()) {
            final long currentTime = client.time();
            final String uuid = pla.getUUID().replace("-", "");
            final String displayName = pla.getDisplayName();
            if (displayName.startsWith(util.colorSymbol + "k")) continue;
            final String username = pla.getName();
            if (ignoredPlayers.containsKey(username.toLowerCase())) {
                if (isInOverlay(uuid)) {
                    overlayPlayers.remove(uuid);
                    currentPlayers.remove(uuid);
                }
                continue;
            }
            currentEntityUUIDs.add(uuid);
            if (isBot(pla)) continue;
            if (isInOverlay(uuid)) {
                if (status == 3 && !teams.containsKey(uuid) && displayName.contains(" ")) {
                    teams.put(uuid, displayName);
                    Map<String, Object> theTeamName = new HashMap<>();
                    theTeamName.put("displayprefix", displayName);
                    theTeamName.put("displaynoprefix", displayName.split(" ")[1]);
                    addToOverlay(uuid, theTeamName);
                }
                continue;
            }
            
            List<Object[]> encounters = uuid.charAt(12) == '4' ? playerEncounters.getOrDefault(uuid, new ArrayList<>()) : playerEncounters.getOrDefault(username, new ArrayList<>());
            if (!encounters.isEmpty()) {
                encounters.removeIf(encounter -> currentTime - (long) encounter[1] > threshold);
            }
            if (encounters.isEmpty() || !encounters.get(encounters.size() - 1)[0].equals(getLobbyId())) {
                encounters.add(new Object[]{getLobbyId(), currentTime});
            }
            if (uuid.charAt(12) == '4') playerEncounters.put(uuid, encounters);
            else playerEncounters.put(username, encounters);
            final String formattedencounter = getSeenColor(encounters.size());

            Map<String, Object> placeholderStats = new ConcurrentHashMap<>();

            placeholderStats.put(encountersKey, formattedencounter);
            placeholderStats.put(encountersValue, encounters.size());
            placeholderStats.put(playerKey, displayName);
            placeholderStats.put("username", username);

            if (uuid.charAt(12) != '4') {
                placeholderStats.put(joinValue, (int) (client.time() / 1000) * -1);
                placeholderStats.put("nicked", true);
                placeholderStats.put(playerKey, util.color("&e" + username));
                placeholderStats.put(headsKey, defaultHead);
                overlayPlayers.put(uuid, placeholderStats);
                sortOverlay();
                continue;
            }

            overlayPlayers.put(uuid, placeholderStats);
            
            addPlaceholderStats(uuid, displayName, false);
            onPlayerAdd(uuid);
        }
        
        if (status == 2) {
            Iterator<String> overlayIterator = overlayPlayers.keySet().iterator();
            while (overlayIterator.hasNext()) {
                String overlayUUID = overlayIterator.next();
                if (currentEntityUUIDs.contains(overlayUUID)) continue;
                if (overlayPlayers.get(overlayUUID).containsKey("manual")) continue;
                overlayIterator.remove();
                doColumns(false);
            }
        }
    }

    synchronized(currentPlayers) {
        if (status != 3) {
            Iterator<String> iterator = currentPlayers.iterator();
            while (iterator.hasNext()) {
                String uuid = iterator.next();
                if (isInOverlay(uuid)) continue;
                iterator.remove();
                doColumns(false);
            }
        }

        for (String uuid : overlayPlayers.keySet()) {
            if (currentPlayers.contains(uuid)) continue;
            currentPlayers.add(uuid.charAt(12) == '4' ? (ascending ? currentPlayers.size() : 0) : (ascending ? 0 : currentPlayers.size()), uuid);
            doColumns(false);
        }
    }
}

void onRenderTick(float partialTicks) {
    if ((!client.getScreen().isEmpty() && !client.getScreen().startsWith("GuiChat")) || overlayTicks < 5 || columns.size() == 0) return;

    float radius = (float)modules.getSlider(scriptName, "Corner Radius");

    /* render.bloom.prepare();
    render.roundedRect(startX + 1, startY + 1, endX - 1, endY - 1, radius, new Color(0, 0, 0).getRGB());
    render.bloom.apply(3, 2); */

    render.roundedRect(startX, startY, endX, endY, radius, new Color(43, 45, 49).getRGB());
    render.roundedRect(startX, startY, endX, startY + lineHeight + offsetY / 2f, radius, new Color(37, 37, 43).getRGB());
    if (currentPlayers.size() > 0) {
        render.rect(startX, startY + radius, endX, startY + lineHeight + offsetY / 2f, new Color(37, 37, 43).getRGB());
    }


    for (Map<String, Object> column : columns) {
        boolean enabled = Boolean.TRUE.equals(column.get("enabled"));
        if (!enabled) continue;

        String statKey = column.get("key").toString();
        String title = column.get("header").toString();
        float width = Float.parseFloat(column.get("width").toString()) * textScale;
        float maxWidth = Float.parseFloat(column.get("maxwidth").toString());
        float x = Float.parseFloat(column.get("position").toString());

        if (!statKey.equals(playerKey)) x += (maxWidth - width) / 2;
        render.text2d(title, x, startY + offsetY, textScale, columnTitles, true);
    }

    float y = startY + lineHeight + 5f * textScale;

    synchronized (currentPlayers) { for (String uuid : currentPlayers) {
        Map<String, Object> playerStats = overlayPlayers.get(uuid);
        if (playerStats == null) {
            overlayPlayers.remove(uuid);
            continue;
        }
        boolean isNicked = (Boolean) playerStats.getOrDefault("nicked", false);
        boolean isBlacklisted = (Boolean) playerStats.getOrDefault("blacklisted", false);
        boolean isError = (Boolean) playerStats.getOrDefault("error", false);
        
        for (Map<String, Object> column : columns) {
            boolean enabled = Boolean.TRUE.equals(column.get("enabled"));
            if (!enabled) continue;
            
            String statKey = column.get("key").toString();
            float maxWidth = Float.parseFloat(column.get("maxwidth").toString());
            Object statValue = playerStats.get(statKey);
            String stringStatValue = String.valueOf(statValue);

            float x = Float.parseFloat(column.get("position").toString());

            if (isNicked) {
                if (statKey.equals(headsKey)) {
                    statValue = defaultHead;
                } else if (!statKey.equals(playerKey) && !statKey.equals(encountersKey)) {
                    statValue = util.colorSymbol + "7-";
                } else if (!teams.containsKey(uuid) && statKey.equals(playerKey)) {
                    statValue = util.colorSymbol + 'e' + stringStatValue.replaceAll(util.colorSymbol + ".", "");
                }
            } else if (isError && (statValue == null || stringStatValue.isEmpty())) {
                statValue = util.colorSymbol + "4E";
            }

            switch (statKey) {
                case playerKey:
                    if (isNicked && !teams.containsKey(uuid)) {
                        statValue = util.colorSymbol + 'e' + stringStatValue.replaceAll(util.colorSymbol + ".", "");
                        break;
                    }
                    if (isError && (statValue == null || stringStatValue.isEmpty() || stringStatValue.equals(util.colorSymbol + "7-"))) {
                        statValue = util.colorSymbol + "4E";
                        break;
                    }
                    if (statValue == null || stringStatValue.isEmpty()) {
                        overlayPlayers.remove(uuid);
                        continue;
                    }
                    if (showTeamColors && status == 3) {
                        Object name = showTeamPrefix ? playerStats.get("displayprefix") : playerStats.get("displaynoprefix");
                        if (name != null) {
                            statValue = name;
                        }
                    } else if (showRank) {
                        Object name = playerStats.get("usernamewithrank");
                        if (name != null) {
                            statValue = name;
                        }
                    } else {
                        Object name = playerStats.get("usernamewithrankcolor");
                        if (name != null) {
                            statValue = name;
                        }
                    }
                    /* if (isBlacklisted) {
                        statValue = util.color("&4" + util.strip(statValue.toString()));
                    } */
                    break;
                case tagsKey:
                    if (stringStatValue.isEmpty()) {
                        StringBuilder statValueBuilder = new StringBuilder();
                        for (String tag : tags) {
                            if (!playerStats.containsKey(tag)) continue;
                            String realTag = String.valueOf(playerStats.get(tag));
                            if (!realTag.startsWith(util.colorSymbol)) continue;
                            statValueBuilder.append(realTag);
                        }
                        statValue = statValueBuilder.length() > 0 ? statValueBuilder.toString() : isNicked ? util.colorSymbol + "7-" : null;
                    }
                    break;
                case encountersKey:
                    if (statValue == null || stringStatValue.isEmpty()) {
                        statValue = util.colorSymbol + "a1";
                    }
                    break;
                case headsKey:
                    Image head = statValue != null ? (Image) statValue : null;
                    if (head != null && head.isLoaded()) {
                        render.image(head, x + (maxWidth - headsSize) / 2, y, headsSize, headsSize);
                    } else {
                        render.image(defaultHead, x + (maxWidth - headsSize) / 2, y, headsSize, headsSize);
                    }
                    continue;
            }

            String text = statValue != null ? statValue.toString() : "";
            float textWidth = (float) render.getFontWidth(text) * textScale;

            if (!statKey.equals(playerKey)) {
                x += (maxWidth - textWidth) / 2f;
            }

            render.text2d(text, x, y, textScale, -1, true);
        }
        y += lineHeight;
    }}
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (sortBy == joinValue) {
        if (dowho && ((msg.endsWith("!") && msg.contains("has joined")) || msg.startsWith("You will respawn in"))) {
            dowho = false;
            client.async(() -> {
                client.sleep(500);
                if (status > 1 && timeUntilStart() > 5) {
                    client.chat("/who");
                }
            });
            return true;
        } else if (msg.startsWith("ONLINE: ")) {
            String[] players = msg.substring(8).split(", ");
            Map<String, Integer> playerMap = new HashMap<>();
            for (int i = 0; i < players.length; i++) {
                playerMap.put(players[i], players.length - 1 - i);
            }

            overlayPlayers.forEach((uuid, overlayPlayer) -> {
                Object usernameObj = overlayPlayer.get(playerKey);
                if (usernameObj instanceof String) {
                    String username = util.strip((String) usernameObj);
                    if (username.contains(" ")) {
                        username = username.split(" ")[1];
                    }
                    Integer joinOrder = playerMap.get(username);
                    if (joinOrder != null) {
                        overlayPlayer.put(joinValue, joinOrder);
                        overlayPlayers.put(uuid, overlayPlayer);
                    }
                }
            });

            sortOverlay();

            if (!didwho) {
                didwho = true;
                client.log("[CHAT] " + msg);
                return false;
            }
        }
    }
    return true;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        if (!c01.message.startsWith("/ov")) return true;
        String[] parts = c01.message.split(" ");
        if (parts.length <= 1) {
            String title = " &eLazify Overlay &7";
            String footerText = " &eMade by Pug &7";
            String[] messages = {
                "&3/ov clearhidden&e: Clears the hidden player list.",
                "&3/ov hide [username]&e: Hides player from the overlay.",
                "&3/ov reload&e: Reloads the players displayed on the overlay.",
                "&3/ov sc [username]&e: Manually adds a player to the overlay.",
                "&3/ov hypixelkey [key]&e: Updates the hypixel api key.",
                "&3/ov pugkey [key]&e: Updates the pug api key.",
                "&3/ov urchinkey [key]&e: Updates the urchin api key."
            };

            int maxPixelWidth = 0;
            String strippedTitle = title.replaceAll("&[0-9a-fk-or]", "");
            String strippedFooterText = footerText.replaceAll("&[0-9a-fk-or]", "");
            for (String message : messages) {
                String strippedMessage = message.replaceAll("&[0-9a-fk-or]", "");
                int messageWidth = render.getFontWidth(strippedMessage);
                if (messageWidth > maxPixelWidth) {
                    maxPixelWidth = messageWidth;
                }
            }
            int titleWidth = render.getFontWidth(strippedTitle);
            int footerTextWidth = render.getFontWidth(strippedFooterText);
            if (titleWidth > maxPixelWidth) maxPixelWidth = titleWidth;
            if (footerTextWidth > maxPixelWidth) maxPixelWidth = footerTextWidth;

            int headerFooterWidth = maxPixelWidth + render.getFontWidth("  ");
            int titlePaddingTotal = headerFooterWidth - titleWidth;
            int footerPaddingTotal = headerFooterWidth - footerTextWidth;
            int titlePaddingSides = titlePaddingTotal / 2;
            int footerPaddingSides = footerPaddingTotal / 2;

            String header = "&7" + generatePadding('-', titlePaddingSides) + title + generatePadding('-', titlePaddingSides);
            if (titlePaddingTotal % 2 != 0) header += "-";
            client.print(getChatPrefix() + header);

            for (String message : messages) {
                String strippedMessage = message.replaceAll("&[0-9a-fk-or]", "");
                int messagePixelWidth = render.getFontWidth(strippedMessage);
                int totalPaddingWidth = maxPixelWidth - messagePixelWidth;
                int paddingLeftWidth = totalPaddingWidth / 2;

                String paddedMessage = generatePadding(' ', paddingLeftWidth) + message;
                client.print(getChatPrefix() + paddedMessage);
            }

            String footer = "&7" + generatePadding('-', footerPaddingSides) + footerText + generatePadding('-', footerPaddingSides);
            if (footerPaddingTotal % 2 != 0) footer += "-";
            client.print(getChatPrefix() + footer);

            return false;
        }

        String command = parts[1];

        if (command.equalsIgnoreCase("sc")) {
            if (parts.length < 3) {
                client.print(getChatPrefix() + "&eInvalid syntax. Use &3/ov sc [username]&e.");
                return false;
            }

            String player = parts[2];

            client.async(() -> { 
                String[] conversion = convertPlayer(player);
                String uuid = conversion[0];
                String username = conversion[1];

                if (uuid == null || uuid.isEmpty()) {
                    String[] conversion2 = convertPlayerPlayerdb(player);
                    uuid = conversion2[0];
                    username = conversion2[1];
                    if (uuid == null || uuid.isEmpty()) {
                        client.print(getChatPrefix() + "&eFailed to convert &3" + player + "&e.");
                        return;
                    }
                }

                synchronized(currentPlayers) {
                    overlayPlayers.remove(uuid);
                    currentPlayers.remove(uuid);
                    addPlaceholderStats(uuid, username, true);
                    addToPlayers(uuid);

                    Map<String, Object> manual = new ConcurrentHashMap<>();
                    manual.put("manual", true);
                    manual.put("username", username);
                    addToOverlay(uuid, manual);

                    onManualPlayerAdd(uuid);
                    client.print(getChatPrefix() + "&eAdded &3" + username + " &eto the overlay.");
                }
            });
            return false;
        } else if (command.equalsIgnoreCase("hide")) {
            if (parts.length > 2) {
                String ign = parts[2].toLowerCase();
                ignoredPlayers.put(ign, "");
                client.print(getChatPrefix() + "&3" + parts[2] + "&e is now hidden.");
            } else {
                client.print(getChatPrefix() + "&eInvalid syntax. Use &3/ov hide [username]&e.");
            }
            return false;
        } else if (command.equalsIgnoreCase("clearhidden")) {
            String msg = getChatPrefix() + "&eCleared &3" + ignoredPlayers.size() + "&e player";
            msg += ignoredPlayers.size() != 1 ? "s." : ".";
            ignoredPlayers.clear();
            client.print(msg);
            return false;
        } else if (command.equalsIgnoreCase("reload")) {
            Map<String, Map<String, Object>> players = new ConcurrentHashMap<>(overlayPlayers);
            clearMaps();
            for (String player : players.keySet()) {
                if (player.charAt(12) != '4') {
                    addToOverlay(player, players.get(player));
                    continue;
                }
                addPlaceholderStats(player, players.get(player).get(playerKey).toString(), true);
                addToPlayers(player);
                onPlayerAdd(player);
                Map<String, Object> fix = new ConcurrentHashMap<>();
                fix.put("username", players.get(player).get("username").toString());
                addToOverlay(player, fix);
            }
            overlayTicks = 5;
            String msg = getChatPrefix() + "&eReloaded &3" + players.size() + "&e player" + (players.size() != 1 ? "s" : "") + ".";
            client.print(msg);
            return false;
        } else if (command.equalsIgnoreCase("clear")) {
            Map<String, Map<String, Object>> players = new ConcurrentHashMap<>(overlayPlayers);
            clearMaps();
            overlayTicks = 5;
            String msg = getChatPrefix() + "&eCleared &3" + players.size() + "&e player" + (players.size() != 1 ? "s" : "") + ".";
            client.print(msg);
            return false;
        } else if (command.equals("pugkey") || command.equals("hypixelkey") || command.equals("urchinkey")) {
            if (parts.length < 3 || parts[2].trim().isEmpty()) {
                client.print(getChatPrefix() + "&eInvalid syntax. Use &3/ov " + command + " [key]&e.");
                return false;
            }

            String newKey = parts[2].trim();
            String cfgKey  = "lazify_" + command;

            switch (command) {
                case "pugkey": pugKey = newKey; break;
                case "hypixelkey": hypixelKey = newKey; break;
                case "urchinkey": urchinKey = newKey; break;
            }

            if (config.set(cfgKey, newKey)) {
                client.print(getChatPrefix() + "&a" + command + " updated successfully.");
            } else {
                client.print(getChatPrefix() + "&cFailed to save " + command + ".");
            }
            return false;
        }
    }
    return true;
}

String generatePadding(char character, int pixelWidth) {
    StringBuilder builder = new StringBuilder();
    int charWidth = render.getFontWidth(String.valueOf(character));
    int numChars = pixelWidth / charWidth;
    for (int i = 0; i < numChars; i++) {
        builder.append(character);
    }
    return builder.toString();
}

void addAlert(String title, String message, int duration, String command) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", title);
    alert.put("message", message);
    alert.put("duration", duration);
    alert.put("command", command);
    alerts.add(alert);
}

void onWorldJoin(Entity entity) {
    if (client.getPlayer() == entity) {
        dowho = true;
        didwho = false;
        overlayTicks = 0;
        clearMaps();
        onWorldSwap();
    }
}

void addToOverlay(String uuid, Map<String, Object> newData) {
    try {

        Map<String, Object> existingData = overlayPlayers.get(uuid);
        if (existingData == null) {
            return;
        }
        
        existingData.putAll(newData);
        overlayPlayers.put(uuid, existingData);
        doColumns(false);
        sortOverlay();
    } catch (Exception e) {
        client.log("Error in addToOverlay: " + e);
        client.print(getChatPrefix() + "&eError detected. Please check &3latest.log&e.");
    }
}

Comparator<String> comparator = (uuid1, uuid2) -> {
    try {
        Map<String, Object> stats1 = overlayPlayers.get(uuid1);
        Map<String, Object> stats2 = overlayPlayers.get(uuid2);

        boolean isNicked1 = stats1 != null && Boolean.TRUE.equals(stats1.get("nicked"));
        boolean isNicked2 = stats2 != null && Boolean.TRUE.equals(stats2.get("nicked"));

        if (!sortBy.equals(joinValue)) {
            if (isNicked1 && !isNicked2) {
                return ascending ? -1 : 1;
            } else if (!isNicked1 && isNicked2) {
                return ascending ? 1 : -1;
            }
        }

        String val1 = (stats1 != null && stats1.get(sortBy) != null) ? stats1.get(sortBy).toString() : "-";
        String val2 = (stats2 != null && stats2.get(sortBy) != null) ? stats2.get(sortBy).toString() : "-";

        val1 = val1.replaceAll(util.colorSymbol + ".", "");
        val2 = val2.replaceAll(util.colorSymbol + ".", "");

        boolean isVal1Numeric = containsDigit(val1);
        boolean isVal2Numeric = containsDigit(val2);

        if (!isVal1Numeric && !isVal2Numeric) {
            return 0;
        } else if (!isVal1Numeric) {
            return ascending ? 1 : -1;
        } else if (!isVal2Numeric) {
            return ascending ? -1 : 1;
        }

        double num1 = Double.parseDouble(val1);
        double num2 = Double.parseDouble(val2);
        return ascending ? Double.compare(num2, num1) : Double.compare(num1, num2);
    } catch (NumberFormatException e) {
        client.log("NumberFormatException for " + uuid1 + " or " + uuid2);
        client.print(getChatPrefix() + "&eError detected. Please check &3latest.log&e.");
        return ascending ? -1 : 1;
    }
};

boolean containsDigit(String s) {
    for (char c : s.toCharArray()) { if (Character.isDigit(c)) return true; }
    return false;
}

String[] convertPlayer(String player) {
    boolean isUUID =
        (player.length() == 32 && player.charAt(12) == '4') ||
        (player.length() == 36 && player.charAt(14) == '4');
    String url = isUUID
        ? "https://sessionserver.mojang.com/session/minecraft/profile/" + player
        : "https://api.mojang.com/users/profiles/minecraft/" + player;

    try {
        Object[] conversionResponse = get(url, 3000);
        int status = (int) conversionResponse[1];
        if (status == 200) {
            Json jsonData = (Json) conversionResponse[0];
            String uuid = jsonData.has("id")
                ? jsonData.get("id").asString()
                : "";
            String username2 = jsonData.has("name")
                ? jsonData.get("name").asString()
                : "";
            return new String[] { uuid, username2 };
        } else {
            client.log("HTTP Error " + status + " getting uuid on " + player);
            return new String[] { "", "" };
        }
    } catch (Exception e) {
        client.print(getChatPrefix() + "&eRuntime error while getting uuid.");
        client.log("Runtime error getting uuid on " + player + ": " + e);
        return new String[] { "", "" };
    }
}

String[] convertPlayerPlayerdb(String player) {
    String url = "https://playerdb.co/api/player/minecraft/" + player;
    try {
        Object[] conversionResponse = get(url, 3000);
        int status = (int) conversionResponse[1];
        if (status == 200) {
            Json jsonData = (Json) conversionResponse[0];
            Json playerJson = jsonData.get("data").get("player");
            String uuid = playerJson.has("raw_id")
                ? playerJson.get("raw_id").asString()
                : "";
            String username2 = playerJson.has("username")
                ? playerJson.get("username").asString()
                : "";
            return new String[] { uuid, username2 };
        } else {
            client.log("HTTP Error " + status + " getting uuid on " + player);
            return new String[] { "", "" };
        }
    } catch (Exception e) {
        client.print(getChatPrefix() + "&eRuntime error while getting uuid.");
        client.log("Runtime error getting uuid on " + player + ": " + e);
        return new String[] { "", "" };
    }
}

Object[] get(String url, int timeout) {
    Json jsonData = Json.object();
    try {
        Request request = new Request("GET", url);
        request.setConnectTimeout(timeout);
        request.setReadTimeout(timeout);
        request.setUserAgent(chromeUserAgent);
        Response response = request.fetch();

        int code = response != null ? response.code() : 404;

        if (code == 200) jsonData = response.json();
        return new Object[] { jsonData, code };
    } catch (Exception e) {
        client.log("Error in get function: " + e);
        client.print(getChatPrefix() + "&eError detected. Please check &3latest.log&e.");
        return new Object[] { jsonData, 500 };
    }
}

int getBedwarsStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    int size = sidebar.size();
    if (size < 7) return -1;

    if (!util.strip(sidebar.get(0)).startsWith("BED WARS")) {
        return -1;
    }

    String lobbyId = util.strip(sidebar.get(1)).split("  ")[1];
    if (lobbyId.charAt(lobbyId.length() - 1) == ']') {
        lobbyId = lobbyId.split(" ")[0];
    }
    currentLobby = lobbyId;

    if (lobbyId.charAt(0) == 'L') {
        return 1;
    }

    if (util.strip(sidebar.get(5)).startsWith("R Red:") && util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }

    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }

    return -1;
}

int getChroma(long speed, int alpha) {
    float hue = client.time() % (15000L / speed) / (15000.0f / speed);
    Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
    return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
}

void sortOverlay() {
    synchronized(currentPlayers) {
        currentPlayers.sort(comparator);
    }
}

int getHueRGB(float hue, int alpha) {
    if (hue == 0) {
        return new Color(0, 0, 0, alpha).getRGB();
    } else if (hue == 360) {
        return getChroma(1L, alpha);
    } else {
        Color color = Color.getHSBColor(hue / 360.0f, 1.0f, 1.0f);
        return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
}

boolean isBot(NetworkPlayer pla) {
    final String uuid = pla.getUUID();

    if (pla.getPing() > 1) {
        return true;
    }

    if (pla.getName().length() < 2) {
        return true;
    }
    
    if (uuid.charAt(14) != '4' && uuid.charAt(14) != '1') {
        return true;
    }

    if (overlayTicks < 80 && pla.getDisplayName().startsWith(util.colorSymbol + "c") && !pla.getDisplayName().contains(" ")) {
        return true;
    }
    
    if (!showYourself) {
        if (uuid.equals(client.getPlayer().getUUID())) {
            return true;
        }
    }

    if (status == 3 && !pla.getDisplayName().contains(" ")) {
        return true;
    }

    return false;
}

int timeUntilStart() {
    List<String> scoreboard = world.getScoreboard();
    if (scoreboard == null || scoreboard.size() < 7) return -1;
    String line = util.strip(scoreboard.get(6));
    if (!line.startsWith("Starting in ")) {
        if (line.equals("Waiting...")) return 20;
        return -1;
    }
    String[] parts = line.split(" ");
    String lastPart = parts[parts.length - 1];
    if (!lastPart.endsWith("s")) return -1;
    return Integer.parseInt(lastPart.substring(0, lastPart.length() - 1));
}

void clearMaps() {
    teams.clear();
    overlayPlayers.clear();
    synchronized(currentPlayers) {
        currentPlayers.clear();
    }
}

void addColumn(String display, String header, String key) {
    Map<String, Object> columnData = new HashMap<>();
    columnData.put("display", display);
    columnData.put("header", header);
    columnData.put("key", key);
    columnData.put("width", render.getFontWidth(header));
    columnData.put("maxwidth", render.getFontWidth(header));
    columnData.put("position", 0);
    columnData.put("enabled", false);

    columns.add(columnData);

    modules.registerButton(display, true);
}

void addSortingOption(String display, String key) {
    sortingOptions.add(display);
    parseSortingMode.put(display, key);
}

String[] getSortingOptions() {
    if (sortingOptions.size() == 0) {
        sortingOptions.add("Empty");
        parseSortingMode.put("Empty", "Empty");
    }

    String[] array = new String[sortingOptions.size()];
    return sortingOptions.toArray(array);
}

boolean isColumnEnabled(String key) {
    for (Map<String, Object> column : columns) {
        if (key.equals(column.get("key"))) {
            return Boolean.TRUE.equals(column.get("enabled"));
        }
    }
    return false;
}

void addTag(String newTag) {
    tags.add(newTag);
}

void doColumns(boolean updateEnabled) {
    float currentX = startX + 5f * textScale;

    for (Map<String, Object> column : columns) {
        float longest = Float.parseFloat(column.get("width").toString()) * textScale;
        String statKey = column.get("key").toString();
        
        boolean enabled;
        if (updateEnabled) {
            enabled = modules.getButton(scriptName, column.get("display").toString());
            column.put("enabled", enabled);
        } else {
            enabled = Boolean.TRUE.equals(column.get("enabled"));
        }

        if (!enabled) continue;

        synchronized (currentPlayers) { for (String uuid : currentPlayers) {
            if (uuid == null) continue;

            Map<String, Object> playerData = overlayPlayers.get(uuid);
            if (playerData == null) continue;

            Object statValueObj = playerData.get(statKey);
            if (statValueObj == null) continue;
            String statValue = "";

            if (statKey.equals(tagsKey)) {
                StringBuilder statValueBuilder = new StringBuilder();
                for (String tag : tags) {
                    Object tagObj = playerData.get(tag);
                    if (tagObj == null) continue;
                    statValueBuilder.append(tagObj.toString());
                }
                statValue = statValueBuilder.length() > 0 ? statValueBuilder.toString() : "";
            } else if (statKey.equals(playerKey)) {
                if (showTeamColors && status == 3) {
                    Object name = showTeamPrefix ? playerData.get("displayprefix") : playerData.get("displaynoprefix");
                    if (name != null) {
                        statValue = name.toString();
                    }
                } else if (showRank) {
                    Object name = playerData.get("usernamewithrank");
                    if (name != null) {
                        statValue = name.toString();
                    }
                } else {
                    Object name = playerData.get("usernamewithrankcolor");
                    if (name != null) {
                        statValue = name.toString();
                    }
                }
                if (statValue.isEmpty()) {
                    statValue = statValueObj.toString();
                }
            } else {
                statValue = statValueObj.toString();
            }

            float width = (float) render.getFontWidth(statValue) * textScale;
            if (statKey.equals(headsKey)) width = (float)headsSize * textScale;
            if (width > longest) {
                longest = width;
            }
        }};

        column.put("maxwidth", longest);
        column.put("position", currentX);
        currentX += longest + (5f * textScale);
    }

    endX = currentX;
    endY = startY + lineHeight + (currentPlayers.size() * lineHeight) + (currentPlayers.size() > 0 ? 5f * textScale : textScale);
}

void updateStatus() {
    lastLobby = getLobbyId();
    status = getBedwarsStatus();
    if (!lastLobby.equals(getLobbyId())) {
        clearMaps();
    }
}

void addToPlayers(String uuid) { synchronized(currentPlayers) {
    if (ascending) {
        if (uuid.charAt(12) == '4') currentPlayers.add(uuid);
        else currentPlayers.add(0, uuid);
    } else {
        if (uuid.charAt(12) == '4') currentPlayers.add(0, uuid);
        else currentPlayers.add(uuid);
    }
    doColumns(false);
}}

String getSeenColor(int encounters) {
    if (encounters > 5) {
        return util.colorSymbol + 'c' + String.valueOf(encounters);
    } else if (encounters > 3) {
        return util.colorSymbol + '6' + String.valueOf(encounters);
    } else if (encounters > 1) {
        return util.colorSymbol + 'e' + String.valueOf(encounters);
    } else {
        return util.colorSymbol + 'a' + String.valueOf(encounters);
    }
}
