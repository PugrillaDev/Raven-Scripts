/* old duels stats script which is now patched */

String team_name = util.colorSymbol + "7" + util.colorSymbol + "k";
String myName = "";
final String chromeUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
final String chatPrefix = "&7[&dR&7]&r";
int received = 0;
int status = 0;
String colorSymbol = util.colorSymbol;
Object errorString = "&4E";
String[] colorCodes = {"c", "4", "9", "1", "a", "2", "b", "5", "e", "6", "d", "f", "3", "7", "8", "0"};
Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
Map<String, String> nameToUuid = new ConcurrentHashMap<>();
int currentInGame = 0, currentMax = 0;

final static String starKey = "star",
                    fkdrKey = "fkdr",
                    winstreakKey = "winstreaks",
                    sessionKey = "session",
                    playerKey = "player",
                    encountersKey = "seen",
                    tagsKey = "tags";

final static String starValue = "starvalue",
                    fkdrValue = "fkdrvalue",
                    indexValue = "indexvalue",
                    winstreakValue = "winstreakvalue",
                    encountersValue = "seenvalue",
                    joinValue = "joinvalue";

String[] modes = {
    "overall", "classic_duel", "sw_duel", "op_duel", "uhc_duel", "combo_duel",
    "potion_duel", "blitz_duel", "sumo_duel", "mw_duel", "bridge_duel"
};

String[] displayModes = {
    "Overall", "Classic", "SkyWars", "OP", "UHC", "Combo",
    "Nodebuff", "Blitz", "Sumo", "MegaWalls", "Bridge"
};

void onLoad() {
    modules.registerSlider("Key Color", "", 11, new String[] { "Red", "Dark Red", "Blue", "Dark Blue", "Green", "Dark Green", "Aqua", "Purple", "Yellow", "Orange", "Pink", "White", "Cyan", "Light Gray", "Gray", "Black" });
    modules.registerSlider("Select Mode", "", 0, displayModes);
    modules.registerButton("Show Bedwars", true);
    modules.registerButton("Show Skywars", true);
    modules.registerButton("Show Accuracy", true);
    modules.registerButton("Show Session Time", true);
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    if (player.getNetworkPlayer() != null) {
        myName = player.getNetworkPlayer().getName();
    } else {
        myName = player.getName();
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (msg.startsWith("  ") && msg.contains("Opponent") && msg.contains(": ")) {
        received = 0;
    }
    if (getDuelsStatus() != 2) return true;
    if (msg.endsWith(")!") && msg.contains(" has joined (") && !msg.contains(":")) {
        String[] parts = msg.split("\\(")[1].split("/");
        currentInGame = Integer.parseInt(parts[0]);
        currentMax = Integer.parseInt(parts[1].split("\\)")[0]);
        return false;
    }
    return true;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        String message = c01.message;
        if (!message.startsWith("/sc ")) return true;
        String ign = message.split(" ")[1];
        String uuid = nameToUuid.getOrDefault(ign.toLowerCase(), ign.toLowerCase());
        if (uuid.equals("nicked")) {
            client.print(chatPrefix + " &3" + ign + " &eis nicked.");
            return false;
        }
        handlePlayerStats(uuid);
        return false;
    }
    return true;
}

boolean onPacketReceived(SPacket packet) {
    if (!(packet instanceof S3E)) return true;

    S3E s3e = (S3E) packet;
    if (!s3e.name.equals(team_name) || s3e.playerList.isEmpty()) return true;

    for (String s : s3e.playerList) {
        client.print((received + 1) % 2);
        if (++received % 2 != 0 || s.equals(client.getPlayer().getName())) continue;

        client.async(() -> {
            client.sleep(150);
            client.print("&7" + s + " &ehas joined (&b" + currentInGame + "&e/&b" + currentMax + "&e)!");
        });
        
        if (s.equals(myName) || s.equals(client.getPlayer().getName())) continue;

        String uuid = nameToUuid.getOrDefault(s.toLowerCase(), s.toLowerCase());
        if (uuid.equals("nicked")) {
            client.print(chatPrefix + " &3" + s + " &eis nicked.");
            break;
        }
        handlePlayerStats(uuid);
        break;
    }

    return true;
}

HashSet<String> getTeamsMapPlayers() {
    HashSet<String> players = new HashSet<>(); 

    Map<String, List<String>> teamMap = client.getWorld().getTeams();
    for (String key : teamMap.keySet()) {
        List<String> list = teamMap.get(key);
        for (String ign : list) {
            players.add(ign);
        }
    }

    return players;
}

void handlePlayerStats(String uuid) {
    Map<String, Object> cachedStats = cache.get(uuid);
    if (cachedStats == null || client.time() > (long) cachedStats.get("cachetime")) {
        if (cachedStats != null) cache.remove(uuid);
        client.async(() -> {
            Map<String, Object> playerStats = new ConcurrentHashMap<>();
            try {
                if (!uuid.equals("nicked")) {
                    String url = "https://api.antisniper.net/v2/hypixel/player?key=6ca3ac79-2046-4016-8754-c7f91a2d0228&raw=true&cache=true&player=" + uuid;
                    Object[] playerStatsRequest = get(url, 6000);
                    if ((int)playerStatsRequest[1] == 200) {
                        playerStats = parseStats((Json)playerStatsRequest[0], uuid);
                    } else {
                        client.print(chatPrefix + " &eHTTP Error &3" + playerStatsRequest[1] + " &ewhile getting stats.");
                        client.log("HTTP Error " + playerStatsRequest[1] + " getting stats on " + uuid);
                        playerStats.put("error", true);
                    }
                } else {
                    playerStats.put("nicked", true);
                }
            } catch (Exception e) {
                client.print(chatPrefix + " &eRuntime error while getting stats.");
                client.log("Runtime error getting stats on " + uuid + ": " + e);
                playerStats.put("error", true);
            }

            printStats(playerStats);
        });
    } else {
        printStats(cachedStats);
    }
}

void printStats(Map<String, Object> stats) {

    String name = stats.getOrDefault(playerKey, errorString).toString();
    boolean nicked = Boolean.parseBoolean(stats.getOrDefault("nicked", false).toString());
    if (nicked) {
        client.print(chatPrefix + " &3" + name + " &eis nicked.");
        return;
    }

    String ses = stats.getOrDefault("sessiontime", errorString).toString();
    String star = stats.getOrDefault(starKey, errorString).toString();
    String swStar = stats.getOrDefault("swstar", errorString).toString();
    String swKdr = stats.getOrDefault("swkdr", errorString).toString();
    String fkdr = stats.getOrDefault(fkdrKey, errorString).toString();
    int duelsWins = (int) Double.parseDouble(stats.getOrDefault("dwins", "0").toString());
    int duelsLosses = (int) Double.parseDouble(stats.getOrDefault("dlosses", "0").toString());
    String wlr = stats.getOrDefault("wlr", errorString).toString();
    String kdr = stats.getOrDefault("kdr", errorString).toString();
    String hmr = stats.getOrDefault("hmr", errorString).toString();
    String bhr = stats.getOrDefault("bhr", errorString).toString();

    String color = util.colorSymbol + "" + colorCodes[(int)modules.getSlider(scriptName, "Key Color")];

    client.print("");
    client.print(chatPrefix + " " + name);
    if (modules.getButton(scriptName, "Show Bedwars")) {
        client.print(chatPrefix + " " + color + "BW Star: " + star + " " + color + "FKDR: " + fkdr);
    }

    if (modules.getButton(scriptName, "Show Skywars")) {
        client.print(chatPrefix + " " + color + "SW Star: " + swStar + " " + color + "KDR: " + swKdr);
    }

    client.print(chatPrefix + " " + color + "WLR: " + wlr + " " + color + "KDR: " + kdr + " " + color + "(&d" + formatNumber(duelsWins) + color + ")");

    if (modules.getButton(scriptName, "Show Accuracy")) {
        client.print(chatPrefix + " " + color + "HMR: " + hmr + " " + color + "BHR: " + bhr);
    }

    if (modules.getButton(scriptName, "Show Session Time")) {
        client.print(chatPrefix + " " + color + "Session: " + ses);
    }
}

String formatNumber(int number) {
    if (Math.abs(number) < 1000) return Integer.toString(number);
    String numberStr = Integer.toString(number);
    StringBuilder formattedNumber = new StringBuilder(numberStr);

    int insertPosition = formattedNumber.length() - 3;
    while (insertPosition > 0) {
        formattedNumber.insert(insertPosition, ",");
        insertPosition -= 3;
    }

    return formattedNumber.toString();
}

Map<String, Object> parseStats(Json jsonData, String uuid) {
    Map<String, Object> stats = new ConcurrentHashMap<>();
    
    try {
        Json data = jsonData.object();
        try {
            data.object("player").exists();
        } catch (ClassCastException e) {
            stats.put("nicked", true);
            stats.put(playerKey, uuid);
            nameToUuid.put(uuid, "nicked");
            return stats;
        }

        Json playerObject = data.object("player");
        String username = playerObject.get("displayname", "");
        if (username.isEmpty()) {
            client.log("Failed to parse displayname for " + uuid);
            return stats;
        }
        String realuuid = playerObject.get("uuid", "");
        if (!realuuid.isEmpty()) {
            uuid = realuuid;
            nameToUuid.put(username.toLowerCase(), realuuid);
        }
        
        String rank = getRank(jsonData);
        String coloredUsername = getRankColor(rank) + username;
        if (!rank.isEmpty()) {
            coloredUsername = getFormattedRank(jsonData) + " " + coloredUsername;
        }

        long lastLogin = Long.parseLong(playerObject.get("lastLogin", "0"));
        long lastLogout = Long.parseLong(playerObject.get("lastLogout", "0"));
        boolean statusOn = lastLogin != 0;
        String coloredSession = util.colorSymbol + "cAPI";
        if (statusOn) {
            if (lastLogin - lastLogout > -10000) {
                lastLogout = client.time();
                coloredSession = calculateRelativeTimestamp(lastLogin, lastLogout);
                coloredSession = getSessionColor(lastLogin, lastLogout, coloredSession);
            } else {
                coloredSession = util.colorSymbol + "cOFFLINE";
            }
        }

        Json bedwarsObject = playerObject.object("stats").object("Bedwars");

        int star = (int) Math.floor(expToStars((int)Double.parseDouble(bedwarsObject.get("Experience", "0"))));
        String coloredStar = getPrestigeColor(star);

        double finalKills = Double.parseDouble(bedwarsObject.get("final_kills_bedwars", "0"));
        double finalDeaths = Double.parseDouble(bedwarsObject.get("final_deaths_bedwars", "0"));

        double fkdr = finalDeaths == 0 ? finalKills : finalKills / finalDeaths < 10 ? util.round(finalKills / finalDeaths, 2) : util.round(finalKills / finalDeaths, 1);
        String coloredFkdr = getFkdrColor(formatDoubleStr(fkdr));

        String selectedMode = modes[(int) modules.getSlider(scriptName, "Select Mode")];

        String winsKey = selectedMode.equals("overall") ? "wins" : selectedMode + "_wins";
        String lossesKey = selectedMode.equals("overall") ? "losses" : selectedMode + "_losses";
        String killsKey = selectedMode.equals("overall") ? "kills" : selectedMode + "_kills";
        String deathsKey = selectedMode.equals("overall") ? "deaths" : selectedMode + "_deaths";
        String meleeHitsKey = selectedMode.equals("overall") ? "melee_hits" : selectedMode + "_melee_hits";
        String meleeSwingsKey = selectedMode.equals("overall") ? "melee_swings" : selectedMode + "_melee_swings";
        String bowHitsKey = selectedMode.equals("overall") ? "bow_hits" : selectedMode + "_bow_hits";
        String bowShotsKey = selectedMode.equals("overall") ? "bow_shots" : selectedMode + "_bow_shots";
        
        Json duelsObject = playerObject.object("stats").object("Duels");

        double duelsWins = Double.parseDouble(duelsObject.get(winsKey, "0"));
        double duelsLosses = Double.parseDouble(duelsObject.get(lossesKey, "0"));
        double duelsWlr = duelsLosses == 0 ? duelsWins : duelsWins / duelsLosses < 10 ? util.round(duelsWins / duelsLosses, 2) : util.round(duelsWins / duelsLosses, 1);
        String coloredWlr = getWlrColor(formatDoubleStr(duelsWlr));

        double bridgeKills = Double.parseDouble(duelsObject.get(killsKey, "0"));
        double bridgeDeaths = Double.parseDouble(duelsObject.get(deathsKey, "0"));
        double bridgeKdr = bridgeDeaths == 0 ? bridgeKills : bridgeKills / bridgeDeaths < 10 ? util.round(bridgeKills / bridgeDeaths, 2) : util.round(bridgeKills / bridgeDeaths, 1);
        String coloredKdr = getKdrColor(formatDoubleStr(bridgeKdr));

        double bridgeHits = Double.parseDouble(duelsObject.get(meleeHitsKey, "0"));
        double bridgeSwings = Double.parseDouble(duelsObject.get(meleeSwingsKey, "0"));
        double bridgeHmr = bridgeSwings == 0 ? 0 : (int)((bridgeHits / bridgeSwings) * 100);
        String coloredHmr = getHmrColor(formatDoubleStr(bridgeHmr)) + "%";

        double bridgeBowHits = Double.parseDouble(duelsObject.get(bowHitsKey, "0"));
        double bridgeBowShots = Double.parseDouble(duelsObject.get(bowShotsKey, "0"));
        double bridgeBhr = bridgeBowShots == 0 ? 0 : (int)((bridgeBowHits / bridgeBowShots) * 100);
        String coloredBhr = getBhrColor(formatDoubleStr(bridgeBhr)) + "%";

        Json skywarsObject = playerObject.object("stats").object("SkyWars");

        double swkills = Double.parseDouble(skywarsObject.get("kills", "0"));
        double swDeaths = Double.parseDouble(skywarsObject.get("deaths", "0"));
        double swKdr = swDeaths == 0 ? swkills : swkills / swDeaths < 10 ? util.round(swkills / swDeaths, 2) : util.round(swkills / swDeaths, 1);
        String coloredSwKdr = getFkdrColor(formatDoubleStr(swKdr));
        String swStar = skywarsObject.get("levelFormatted", util.colorSymbol + "71\u22C6");

        String title = getTitle((int)duelsWins);
        if (!title.isEmpty()) {
            coloredUsername = title + "&r " + coloredUsername;
        }

        stats.put(playerKey, coloredUsername);
        stats.put(starKey, coloredStar);
        stats.put(fkdrKey, coloredFkdr);
        stats.put("sessiontime", coloredSession);
        stats.put("dwins", duelsWins);
        stats.put("dlosses", duelsLosses);
        stats.put("wlr", coloredWlr);
        stats.put("kdr", coloredKdr);
        stats.put("hmr", coloredHmr);
        stats.put("bhr", coloredBhr);
        stats.put("swkdr", coloredSwKdr);
        stats.put("swstar", swStar);
    } catch (Exception e) {
        client.log("Error in parseStats function on " + uuid + ": " + e);
        client.print(chatPrefix + " &eError detected. Please check &3latest.log&e.");
        stats.put("error", true);
    }

    stats.put("cachetime", client.time() + 60000);
    cache.put(uuid, stats);
    return stats;
}

Object[] get(String url, int timeout) {
    Json jsonData = new Json("{}");
    try {
        Request request = new Request("GET", url);
        request.setConnectTimeout(timeout);
        request.setReadTimeout(timeout);
        request.setUserAgent(chromeUserAgent);
        request.addHeader("Reason", "Just need hypixel data for developing something");
        Response response = request.fetch();

        int code = response != null ? response.code() : 404;

        if (code == 200) jsonData = response.json();
        return new Object[] { jsonData, code };
    } catch (Exception e) {
        client.log("Error in get function: " + e);
        client.print(chatPrefix + " &eError detected. Please check &3latest.log&e.");
        return new Object[] { jsonData, 500 };
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
    if (playerData == null) return null;
    if (!playerData.object("player").exists()) {
        return null;
    }

    Json player = playerData.object("player");
    
    String prefix = player.get("prefix", null);
    if (prefix != null) {
        return prefix.replace("§", util.colorSymbol);
    }

    String rank = player.get("rank", null);
    if (rank != null) {
        if (rank.equals("GAME_MASTER")) return "GM";
        if (rank.equals("YOUTUBER")) return "YOUTUBE";
        if (!rank.equals("NORMAL")) return rank;
    }

    String packageRank = player.get("newPackageRank", player.get("packageRank", null));

    if (packageRank == null || packageRank.equals("NONE")) return null;
    if (packageRank.startsWith("MVP")) {
        return player.get("monthlyPackageRank", "").equals("SUPERSTAR") ? "MVP++" : packageRank.length() == 3 ? packageRank : "MVP+";
    }
    if (packageRank.startsWith("VIP")) {
        return packageRank.length() == 3 ? packageRank : "VIP+";
    }

    return null;
}

String getFormattedRank(Json playerData) {
    String colorCode = util.colorSymbol + "7";
    if (playerData == null) return colorCode;
    String rank = getRank(playerData);

    if (rank == null) return colorCode;

    Json player = playerData.object("player");
    String plusColor = player.get("rankPlusColor", "RED");

    switch (plusColor) {
        case "BLACK": colorCode = util.colorSymbol + "0"; break;
        case "DARK_BLUE": colorCode = util.colorSymbol + "1"; break;
        case "DARK_GREEN": colorCode = util.colorSymbol + "2"; break;
        case "DARK_AQUA": colorCode = util.colorSymbol + "3"; break;
        case "DARK_RED": colorCode = util.colorSymbol + "4"; break;
        case "DARK_PURPLE": colorCode = util.colorSymbol + "5"; break;
        case "GOLD": colorCode = util.colorSymbol + "6"; break;
        case "GRAY": colorCode = util.colorSymbol + "7"; break;
        case "DARK_GRAY": colorCode = util.colorSymbol + "8"; break;
        case "BLUE": colorCode = util.colorSymbol + "9"; break;
        case "GREEN": colorCode = util.colorSymbol + "a"; break;
        case "AQUA": colorCode = util.colorSymbol + "b"; break;
        case "RED": colorCode = util.colorSymbol + "c"; break;
        case "LIGHT_PURPLE": colorCode = util.colorSymbol + "d"; break;
        case "YELLOW": colorCode = util.colorSymbol + "e"; break;
        case "WHITE": colorCode = util.colorSymbol + "f"; break;
    }

    switch (rank) {
        case "VIP": return util.colorSymbol + "a[VIP]";
        case "VIP+": return util.colorSymbol + "a[VIP" + util.colorSymbol + "6+" + util.colorSymbol + "a]";
        case "MVP": return util.colorSymbol + "b[MVP]";
        case "MVP+": return util.colorSymbol + "b[MVP" + colorCode + "+" + util.colorSymbol + "b]";
        case "MVP++": return util.colorSymbol + "6[MVP" + colorCode + "++" + util.colorSymbol + "6]";
        case "GM": return util.colorSymbol + "2[GM]";
        case "YOUTUBE": return util.colorSymbol + "c[" + util.colorSymbol + "fYOUTUBE" + util.colorSymbol + "c]";
        case "ADMIN": return util.colorSymbol + "c[ADMIN]";
        default: return rank;
    }
}

String getTitle(int wins) {
    boolean overall = modules.getSlider(scriptName, "Select Mode") == 0;
    String selectedMode = displayModes[(int) modules.getSlider(scriptName, "Select Mode")];
    Object[][] titles = {
        {0, 0, "None", "7", false, -1},
        {50, 10, "Rookie", "8", false, -1},
        {100, 30, "Iron", "f", false, -1},
        {250, 50, "Gold", "6", false, -1},
        {500, 100, "Diamond", "3", false, -1},
        {1000, 200, "Master", "2", false, -1},
        {2000, 600, "Legend", "4", true, -1},
        {5000, 1000, "Grandmaster", "e", true, -1},
        {10000, 3000, "Godlike", "5", true, -1},
        {25000, 5000, "CELESTIAL", "b", true, -1},
        {50000, 10000, "DIVINE", "d", true, -1},
        {100000, 10000, "ASCENDED", "c", true, 50}
    };

    String title = "None";
    String color = "7";
    boolean bold = false;
    int maxWins = 0;
    int inc = 0;
    int maxLevel = -1;

    for (Object[] data : titles) {
        int req = (int) data[0];
        int increment = (int) data[1];
        if (overall) {
            req *= 2;
            increment *= 2;
        }
        if (wins >= req) {
            title = (String) data[2];
            color = (String) data[3];
            bold = (boolean) data[4];
            maxWins = req;
            inc = increment;
            maxLevel = (int) data[5];
        }
    }

    if (wins >= maxWins && inc > 0) {
        int additionalLevel = (wins - maxWins) / inc + 1;
        if (maxLevel != -1 && additionalLevel > maxLevel) {
            additionalLevel = maxLevel;
        }
        if (additionalLevel > 1) title += " " + toRomanNumeral(additionalLevel);
    }

    String formattedTitle = util.colorSymbol + color + (bold ? util.colorSymbol + "l" : "") + (!selectedMode.equals("Overall") ? selectedMode + " " : "") + title;
    return !formattedTitle.equals(util.colorSymbol + "7None") ? formattedTitle : "";
}

String toRomanNumeral(int number) {
    String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
    return number > 0 && number < numerals.length ? numerals[number] : "";
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

String getSessionColor(long lastLogin, long lastLogout, String sessionFormatted) {
    long session = lastLogout - lastLogin;

    if (session > 21600000) {
        return util.colorSymbol + '4' + sessionFormatted;
    } else if (session > 14400000) {
        return util.colorSymbol + 'c' + sessionFormatted;
    } else if (session > 9000000) {
        return util.colorSymbol + '6' + sessionFormatted;
    } else if (session > 7200000) {
        return util.colorSymbol + 'e' + sessionFormatted;
    } else if (session > 1200000) {
        return util.colorSymbol + 'a' + sessionFormatted;
    } else if (session > 600000) {
        return util.colorSymbol + 'e' + sessionFormatted;
    } else if (session > 300000) {
        return util.colorSymbol + 'e' + sessionFormatted;
    } else if (session > 150000) {
        return util.colorSymbol + 'c' + sessionFormatted;
    } else {
        return util.colorSymbol + '4' + sessionFormatted;
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
        return colorSymbol + '5' + realwinstreak;
    } else if (realwinstreak >= 500) {
        return colorSymbol + 'd' + realwinstreak;
    } else if (realwinstreak >= 300) {
        return colorSymbol + '4' + realwinstreak;
    } else if (realwinstreak >= 150) {
        return colorSymbol + 'c' + realwinstreak;
    } else if (realwinstreak >= 100) {
        return colorSymbol + '6' + realwinstreak;
    } else if (realwinstreak >= 75) {
        return colorSymbol + 'e' + realwinstreak;
    } else if (realwinstreak >= 50) {
        return colorSymbol + '2' + realwinstreak;
    } else if (realwinstreak >= 25) {
        return colorSymbol + 'a' + realwinstreak;
    } else {
        return colorSymbol + '7' + realwinstreak;
    }
}

String getFkdrColor(String fkdr) {
    double realfkdr = Double.parseDouble(fkdr);
    if (realfkdr > 1000) {
        return colorSymbol + '5' + fkdr;
    } else if (realfkdr > 100) {
        return colorSymbol + '4' + fkdr;
    } else if (realfkdr > 10) {
        return colorSymbol + 'c' + fkdr;
    } else if (realfkdr > 5) {
        return colorSymbol + '6' + fkdr;
    } else if (realfkdr > 2.4) {
        return colorSymbol + 'e' + fkdr;
    } else if (realfkdr > 1.4) {
        return colorSymbol + 'f' + fkdr;
    } else {
        return colorSymbol + '7' + fkdr;
    }
}

String getWlrColor(String wlr) {
    double real = Double.parseDouble(wlr);
    if (real > 100) {
        return colorSymbol + '5' + wlr;
    } else if (real > 25) {
        return colorSymbol + '4' + wlr;
    } else if (real > 10) {
        return colorSymbol + 'c' + wlr;
    } else if (real > 4) {
        return colorSymbol + '6' + wlr;
    } else if (real > 2.4) {
        return colorSymbol + 'e' + wlr;
    } else if (real > 1.4) {
        return colorSymbol + 'f' + wlr;
    } else {
        return colorSymbol + '7' + wlr;
    }
}

String getKdrColor(String wlr) {
    double real = Double.parseDouble(wlr);
    if (real > 5) {
        return colorSymbol + '5' + wlr;
    } else if (real > 3) {
        return colorSymbol + '4' + wlr;
    } else if (real > 2) {
        return colorSymbol + 'c' + wlr;
    } else if (real > 1.7) {
        return colorSymbol + '6' + wlr;
    } else if (real > 1.3) {
        return colorSymbol + 'e' + wlr;
    } else if (real > 1) {
        return colorSymbol + 'f' + wlr;
    } else {
        return colorSymbol + '7' + wlr;
    }
}

String getHmrColor(String wlr) {
    double real = Double.parseDouble(wlr);
    if (real > 90) {
        return colorSymbol + '5' + wlr;
    } else if (real > 80) {
        return colorSymbol + '4' + wlr;
    } else if (real > 70) {
        return colorSymbol + 'c' + wlr;
    } else if (real > 60) {
        return colorSymbol + '6' + wlr;
    } else if (real > 40) {
        return colorSymbol + 'e' + wlr;
    } else if (real > 30) {
        return colorSymbol + 'f' + wlr;
    } else {
        return colorSymbol + '7' + wlr;
    }
}

String getBhrColor(String wlr) {
    double real = Double.parseDouble(wlr);
    if (real > 90) {
        return colorSymbol + '5' + wlr;
    } else if (real > 80) {
        return colorSymbol + '4' + wlr;
    } else if (real > 70) {
        return colorSymbol + 'c' + wlr;
    } else if (real > 60) {
        return colorSymbol + '6' + wlr;
    } else if (real > 40) {
        return colorSymbol + 'e' + wlr;
    } else if (real > 30) {
        return colorSymbol + 'f' + wlr;
    } else {
        return colorSymbol + '7' + wlr;
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

String getRankColor(String rank) {
    switch (rank) {
        case "VIP":
        case "VIP+":
            return colorSymbol + "a";
        case "MVP":
        case "MVP+":
            return colorSymbol + "b";
        case "MVP++":
            return colorSymbol + "6";
        case "YOUTUBE":
        case "ADMIN":
        case "OWNER":
            return colorSymbol + "c";
        case "GM":
            return colorSymbol + "2";
        default:
            return colorSymbol + "7";
    }
}

String getPrestigeColor(int number) {
    int prestige = number - number % 100;
    String nums = String.format("%04d", number);

    if (prestige >= 5000) {
        return util.color("&4" + nums.charAt(0) + "&5" + nums.charAt(1) + "&9" + nums.charAt(2) + nums.charAt(3));
    }

    switch (prestige) {
        case 5000:
            return util.color("&4" + nums.charAt(0) + "&5" + nums.charAt(1) + "&9" + nums.charAt(2) + nums.charAt(3));
        case 4900:
            return util.color("&a" + nums.charAt(0) + "&f" + nums.charAt(1) + nums.charAt(2) + "&a" + nums.charAt(3));
        case 4800:
            return util.color("&5" + nums.charAt(0) + "&c" + nums.charAt(1) + "&6" + nums.charAt(2) + "&e" + nums.charAt(3));
        case 4700:
            return util.color("&f" + nums.charAt(0) + "&4" + nums.charAt(1) + nums.charAt(2) + "&9" + nums.charAt(3));
        case 4600:
            return util.color("&2" + nums.charAt(0) + "&b" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 4500:
            return util.color("&f" + nums.charAt(0) + "&b" + nums.charAt(1) + nums.charAt(2) + "&2" + nums.charAt(3));
        case 4400:
            return util.color("&2" + nums.charAt(0) + "&a" + nums.charAt(1) + "&e" + nums.charAt(2) + "&6" + nums.charAt(3));
        case 4300:
            return util.color("&0" + nums.charAt(0) + "&5" + nums.charAt(1) + nums.charAt(2) + "&5" + nums.charAt(3));
        case 4200:
            return util.color("&1" + nums.charAt(0) + "&9" + nums.charAt(1) + "&3" + nums.charAt(2) + "&f" + nums.charAt(3));
        case 4100:
            return util.color("&e" + nums.charAt(0) + "&6" + nums.charAt(1) + "&c" + nums.charAt(2) + "&d" + nums.charAt(3));
        case 4000:
            return util.color("&5" + nums.charAt(0) + "&c" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 3900:
            return util.color("&c" + nums.charAt(0) + "&a" + nums.charAt(1) + nums.charAt(2) + "&3" + nums.charAt(3));
        case 3800:
            return util.color("&1" + nums.charAt(0) + "&9" + nums.charAt(1) + nums.charAt(2) + "&5" + nums.charAt(3));
        case 3700:
            return util.color("&4" + nums.charAt(0) + "&c" + nums.charAt(1) + nums.charAt(2) + "&b" + nums.charAt(3));
        case 3600:
            return util.color("&a" + nums.charAt(0) + nums.charAt(1) + "&b" + nums.charAt(2) + "&9" + nums.charAt(3));
        case 3500:
            return util.color("&c" + nums.charAt(0) + "&4" + nums.charAt(1) + nums.charAt(2) + "&2" + nums.charAt(3));
        case 3400:
            return util.color("&2" + nums.charAt(0) + "&a" + nums.charAt(1) + nums.charAt(2) + "&5" + nums.charAt(3));
        case 3300:
            return util.color("&9" + nums.charAt(0) + nums.charAt(1) + "&d" + nums.charAt(2) + "&c" + nums.charAt(3));
        case 3200:
            return util.color("&c" + nums.charAt(0) + "&7" + nums.charAt(1) + nums.charAt(2) + "&4" + nums.charAt(3));
        case 3100:
            return util.color("&9" + nums.charAt(0) + "&2" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 3000:
            return util.color("&e" + nums.charAt(0) + "&6" + nums.charAt(1) + "&c" + nums.charAt(2) + "&4" + nums.charAt(3));
        case 2900:
            return util.color("&b" + nums.charAt(0) + "&3" + nums.charAt(1) + nums.charAt(2) + "&9" + nums.charAt(3));
        case 2800:
            return util.color("&a" + nums.charAt(0) + "&2" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 2700:
            return util.color("&e" + nums.charAt(0) + "&f" + nums.charAt(1) + nums.charAt(2) + "&8" + nums.charAt(3));
        case 2600:
            return util.color("&4" + nums.charAt(0) + "&c" + nums.charAt(1) + nums.charAt(2) + "&d" + nums.charAt(3));
        case 2500:
            return util.color("&f" + nums.charAt(0) + "&a" + nums.charAt(1) + nums.charAt(2) + "&2" + nums.charAt(3));
        case 2400:
            return util.color("&b" + nums.charAt(0) + "&f" + nums.charAt(1) + nums.charAt(2) + "&7" + nums.charAt(3));
        case 2300:
            return util.color("&5" + nums.charAt(0) + "&d" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 2200:
            return util.color("&6" + nums.charAt(0) + "&f" + nums.charAt(1) + nums.charAt(2) + "&b" + nums.charAt(3));
        case 2100:
            return util.color("&f" + nums.charAt(0) + "&e" + nums.charAt(1) + nums.charAt(2) + "&6" + nums.charAt(3));
        case 2000:
            return util.color("&7" + nums.charAt(0) + "&f" + nums.charAt(1) + nums.charAt(2) + "&7" + nums.charAt(3));
        case 1000:
            return util.color("&c" + nums.charAt(0) + "&6" + nums.charAt(1) + "&e" + nums.charAt(2) + "&a" + nums.charAt(3));
        case 1900:
        case 900:
            return util.color("&5" + number);
        case 1800:
        case 800:
            return util.color("&9" + number);
        case 1700:
        case 700:
            return util.color("&d" + number);
        case 1600:
            return util.color("&c" + number);
        case 600:
            return util.color("&4" + number);
        case 1500:
        case 500:
            return util.color("&3" + number);
        case 1400:
        case 400:
            return util.color("&2" + number);
        case 1300:
        case 300:
            return util.color("&b" + number);
        case 1200:
        case 200:
            return util.color("&6" + number);
        case 1100:
        case 100:
            return util.color("&f" + number);
        default:
            return util.color("&7" + number);
    }
}


int getDuelsStatus() {
    List<String> sidebar = client.getWorld().getScoreboard();
    if (sidebar == null) {
        if (client.getWorld().getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    if (sidebar.size() > 2 && util.strip(sidebar.get(2)).equals("You can challenge other")) {
        received = 0;
        return 1;
    }

    if (sidebar.size() > 3 && util.strip(sidebar.get(3)).startsWith("Time Left: ")) {
        return 3;
    }
    
    if (sidebar.size() > 7) {
        String six = util.strip(sidebar.get(6));
        if (six.equals("Waiting...") || six.startsWith("Starting in")) {
            return 2;
        }
    }

    return -1;
}