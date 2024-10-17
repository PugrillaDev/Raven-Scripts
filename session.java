/* 
    shows your current bedwars session
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/session.java"
*/

Map<String, Map<String, Integer>> session = new HashMap<>();
HashSet<String> killKeywords = new HashSet<>(Arrays.asList("against", "by", "fighting", "for", "from", "meet", "of", "seeing", "to", "was", "with"));

String[] colorsArray = {"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"};
String[] colorKeysArray = {"c", "9", "a", "e", "b", "f", "d", "8"};
String[] colorCodes = {"c", "4", "9", "1", "a", "2", "b", "5", "e", "6", "d", "f", "3", "7", "8", "0"};
String[] renderStatList;
String[] startMessages = {
    "Protect your bed and destroy the enemy beds.",
    "All generators are maxed! Your bed has three",
    "Collect Lucky Blocks from resource generators",
    "Select an ultimate in the store! They will",
    "Players swap teams at random intervals! Players",
    "Become a Soul Collector and trade in your"
};

List<String> rows = new ArrayList<>();
List<Long> gameTimes = new ArrayList<>();

float startX, endX, startY, endY;
float lineHeight = render.getFontHeight() + 2, scale = 1;

int gameFinals = 0, gameBeds = 0, gameKills = 0, finalKills = 0, finalDeaths, bedsBroken = 0, bedsLost = 0, kills = 0, deaths = 0, wins = 0, losses = 0, winstreak = 0, sessionGames = 0;
int background = new Color(0, 0, 0, 100).getRGB(), yellow = new Color(255, 255, 0).getRGB();
int status = -1;

boolean debug, shadow;

long sessionStart = client.time(), gameStart = 0L, gameEnd = 0L;

String myTeam = "", myName = client.getPlayer().getName();
String currentUUID, lastUUID;

void onLoad() {
    modules.registerSlider("Header Color", "", 8, new String[] { "Red", "Dark Red", "Blue", "Dark Blue", "Green", "Dark Green", "Aqua", "Purple", "Yellow", "Orange", "Pink", "White", "Cyan", "Light Gray", "Gray", "Black" });
    modules.registerSlider("Key Color", "", 11, new String[] { "Red", "Dark Red", "Blue", "Dark Blue", "Green", "Dark Green", "Aqua", "Purple", "Yellow", "Orange", "Pink", "White", "Cyan", "Light Gray", "Gray", "Black" });
    modules.registerSlider("Value Color", "", 4, new String[] { "Red", "Dark Red", "Blue", "Dark Blue", "Green", "Dark Green", "Aqua", "Purple", "Yellow", "Orange", "Pink", "White", "Cyan", "Light Gray", "Gray", "Black" });
    modules.registerSlider("Background Opacity", "", 100, 0, 255, 1);
    modules.registerSlider("Scale", "", 1, 0.5, 1.5, 0.05);
    modules.registerButton("Text Shadow", false);
    modules.registerButton("Game Stats", true);
    modules.registerButton("Final Kills", true);
    modules.registerButton("Beds", true);
    modules.registerButton("Kills", true);
    modules.registerButton("Wins", true);
    modules.registerButton("Winstreak", true);
    modules.registerButton("Session Games", true);
    modules.registerButton("Game Time", true);
    modules.registerButton("Avg Game Time", true);
    modules.registerButton("Session Time", true);
    modules.registerButton("Debug", false);

    modules.registerSlider("X Position", "", client.getDisplaySize()[0], 0, client.getDisplaySize()[0], 0.1);
    modules.registerSlider("Y Position", "", client.getDisplaySize()[1], 0, client.getDisplaySize()[1], 0.1);
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    status = getBedwarsStatus();
    scale = (float) modules.getSlider(scriptName, "Scale");
    lineHeight = (render.getFontHeight() + 2) * scale;
    shadow = modules.getButton(scriptName, "Text Shadow");
    lastUUID = currentUUID;
    currentUUID = player.getUUID();

    if (!currentUUID.equals(lastUUID)) {
        Map<String, Integer> currentStats = session.getOrDefault(currentUUID, new HashMap<>());

        finalKills = currentStats.getOrDefault("Final Kills", 0);
        bedsBroken = currentStats.getOrDefault("Beds", 0);
        kills = currentStats.getOrDefault("Kills", 0);
        wins = currentStats.getOrDefault("Wins", 0);
        winstreak = currentStats.getOrDefault("Winstreak", 0);
        sessionStart = currentStats.getOrDefault("Session Time", (int) (client.time() / 1000L)) * 1000L;
        finalDeaths = currentStats.getOrDefault("Final Deaths", 0);
        bedsLost = currentStats.getOrDefault("Bed Losses", 0);
        losses = currentStats.getOrDefault("Losses", 0);
        sessionGames = currentStats.getOrDefault("Session Games", 0);
    }

    Map<String, Integer> playerStats = new HashMap<>();
    playerStats.put("Final Kills", finalKills);
    playerStats.put("Beds", bedsBroken);
    playerStats.put("Kills", kills);
    playerStats.put("Wins", wins);
    playerStats.put("Winstreak", winstreak);
    playerStats.put("Session Time", (int) (sessionStart / 1000));
    playerStats.put("Final Deaths", finalDeaths);
    playerStats.put("Bed Losses", bedsLost);
    playerStats.put("Losses", losses);
    playerStats.put("Session Games", sessionGames);
    session.put(currentUUID, playerStats);

    if (status == 3 && player.getNetworkPlayer() != null) {
        myName = player.getNetworkPlayer().getName();
    }
    if (player.getTicksExisted() % 20 == 0) {
        debug = modules.getButton(scriptName, "Debug");
        if (status == 3) {
            refreshTeams(myName);
        } else if (status == 1) { for (Entity en : client.getWorld().getEntities()) {
            if (!en.type.equals("EntityArmorStand")) continue;
            String name = util.strip(en.getName());
            if (!name.startsWith("Current Winstreak: ")) continue;
            winstreak = Integer.parseInt(name.split(": ")[1].replaceAll(",", ""));
            break;
        }}
    }

    background = new Color(0, 0, 0, (int) modules.getSlider(scriptName, "Background Opacity")).getRGB();
    String headerColor = colorCodes[(int)modules.getSlider(scriptName, "Header Color")];
    String keyColor = colorCodes[(int)modules.getSlider(scriptName, "Key Color")];
    String valueColor = colorCodes[(int)modules.getSlider(scriptName, "Value Color")];
    String sessionString = util.color("&" + headerColor + "&lSession  ");
    String gameString = util.color("&" + headerColor + "&lGame  ");

    int tempFDS = finalDeaths == 0 ? 1 : finalDeaths, 
        tempBLS = bedsLost == 0 ? 1 : bedsLost, 
        tempDS = deaths == 0 ? 1 : deaths, 
        tempLS = losses == 0 ? 1 : losses;

    rows.clear();
    boolean anyGameStats = false;
    boolean anySessionStats = false;
    boolean anyLowerStats = false;
    boolean sessionHeaderAdded = false;
    boolean gap = false;

    if (modules.getButton(scriptName, "Game Stats")) {
        rows.add(gameString);
        rows.add(util.color("&" + keyColor + "Finals: &" + valueColor + gameFinals));
        rows.add(util.color("&" + keyColor + "Beds: &" + valueColor + gameBeds));
        rows.add(util.color("&" + keyColor + "Kills: &" + valueColor + gameKills));
        anyGameStats = true;
    }

    if (modules.getButton(scriptName, "Final Kills")) {
        if (!sessionHeaderAdded) {
            rows.add(sessionString);
            sessionHeaderAdded = true;
            anySessionStats = true;
        }
        rows.add(util.color("&" + keyColor + "Finals: &" + valueColor + formatNumber(finalKills) + "&7 / &" + keyColor + "FKDR: &" + valueColor + formatDoubleStr(util.round((double)finalKills / (double)tempFDS, 1))));
    }
    if (modules.getButton(scriptName, "Beds")) {
        if (!sessionHeaderAdded) {
            rows.add(sessionString);
            sessionHeaderAdded = true;
            anySessionStats = true;
        }
        rows.add(util.color("&" + keyColor + "Beds: &" + valueColor + formatNumber(bedsBroken) + "&7 / &" + keyColor + "BBLR: &" + valueColor + formatDoubleStr(util.round((double)bedsBroken / (double)tempBLS, 1))));
    }
    if (modules.getButton(scriptName, "Kills")) {
        if (!sessionHeaderAdded) {
            rows.add(sessionString);
            sessionHeaderAdded = true;
            anySessionStats = true;
        }
        rows.add(util.color("&" + keyColor + "Kills: &" + valueColor + formatNumber(kills) + "&7 / &" + keyColor + "KDR: &" + valueColor + formatDoubleStr(util.round((double)kills / (double)tempDS, 1))));
    }
    if (modules.getButton(scriptName, "Wins")) {
        if (!sessionHeaderAdded) {
            rows.add(sessionString);
            sessionHeaderAdded = true;
            anySessionStats = true;
        }
        rows.add(util.color("&" + keyColor + "Wins: &" + valueColor + formatNumber(wins) + "&7 / &" + keyColor + "WLR: &" + valueColor + formatDoubleStr(util.round((double)wins / (double)tempLS, 1))));
    }

    if (modules.getButton(scriptName, "Winstreak")) {
        if (!gap && (anyGameStats || anySessionStats)) {
            rows.add(" ");
            gap = true;
        }
        rows.add(util.color("&" + keyColor + "Winstreak: &" + valueColor + winstreak));
        anyLowerStats = true;
    }
    if (modules.getButton(scriptName, "Session Games")) {
        if (!gap && (anyGameStats || anySessionStats)) {
            rows.add(" ");
            gap = true;
        }
        rows.add(util.color("&" + keyColor + "Session Games: &" + valueColor + sessionGames));
        anyLowerStats = true;
    }
    if (modules.getButton(scriptName, "Game Time")) {
        if (!gap && (anyGameStats || anySessionStats)) {
            rows.add(" ");
            gap = true;
        }

        String gameTime;
        if (gameStart == 0L && gameEnd == 0L) {
            gameTime = "00:00";
        } else {
            long endTime = (gameStart != 0L && gameEnd == 0L) ? client.time() : gameEnd;
            gameTime = calculateRelativeTimestamp(endTime - gameStart);
        }

        rows.add(util.color("&" + keyColor + "Game Time: &" + valueColor + gameTime));
        anyLowerStats = true;
    }
    if (modules.getButton(scriptName, "Avg Game Time")) {
        if (!gap && (anyGameStats || anySessionStats)) {
            rows.add(" ");
            gap = true;
        }

        long totalGameTime = 0L;
        int games = gameTimes.size();

        for (long time : gameTimes) {
            totalGameTime += time;
        }

        if (gameStart != 0L && gameEnd == 0L) {
            totalGameTime += client.time() - gameStart;
            games++;
        }

        long averageGameTime = games == 0 ? 0L : totalGameTime / games;
        String displayTime = averageGameTime < 500 ? "00:00" : calculateRelativeTimestamp(averageGameTime);

        rows.add(util.color("&" + keyColor + "Avg Time: &" + valueColor + displayTime));
        anyLowerStats = true;
    }
    if (modules.getButton(scriptName, "Session Time")) {
        if (!gap && (anyGameStats || anySessionStats)) {
            rows.add(" ");
            gap = true;
        }
        rows.add(util.color("&" + keyColor + "Session Time: &" + valueColor + calculateRelativeTimestamp(client.time() - sessionStart)));
        anyLowerStats = true;
    }

    int longestString = 0;
    for (String row : rows) {
        int length = render.getFontWidth(row);
        if (length > longestString) longestString = length;
    }
    longestString *= scale;

    int[] displaySize = client.getDisplaySize();
    endX = (float)modules.getSlider(scriptName, "X Position");
    endY = (float)modules.getSlider(scriptName, "Y Position");
    startX = endX - longestString - 4f * scale;
    startY = endY - (rows.size() * lineHeight);
}

void onRenderTick(float partialTicks) {
    if (!client.getScreen().isEmpty() && !client.getScreen().startsWith("GuiChat")) return;

    render.rect(startX, startY, endX, endY, background);

    float y = startY + 2f * scale;
    for (String row : rows) {
        if (row.isEmpty()) {
            y += lineHeight;
            continue;
        }
        float rowWidth = render.getFontWidth(row) * scale;
        float x = endX - rowWidth - 2f * scale;
        render.text(row, x, y, scale, yellow, shadow);
        y += lineHeight;
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (!msg.contains(":") && !msg.contains("/")) {
        if (msg.startsWith(" ") && Arrays.stream(startMessages).anyMatch(msg::contains)) {
            sessionGames++;
            gameStart = client.time();
            gameEnd = 0L;
            if (debug) client.print("&5Game started!");
            else client.log(myTeam + " " + myName + " " + "&5Game started!");
        }

        if (status == 3) {
            if (msg.startsWith("+") && msg.contains(" tokens!") && msg.endsWith(" (Win)")) {
                if (debug) client.print("&eYou won!");
                else client.log(myTeam + " " + myName + " " + "&eYou won!");
                wins++;
                winstreak++;
            }
            else if (msg.startsWith("+") && msg.contains(" tokens!") && msg.endsWith(" (Bed Destroyed)")) {
                if (debug) client.print("&dYou broke a bed!");
                else client.log(myTeam + " " + myName + " " + "&dYou broke a bed!");
                bedsBroken++;
                gameBeds++;
            }
            else if (msg.startsWith("+") && msg.contains(" tokens!") && msg.endsWith(" (Final Kill)")) {
                if (debug) client.print("&bYou got a final kill!");
                else client.log(myTeam + " " + myName + " " + "&bYou got a final kill!");
                finalKills++;
                gameFinals++;
            }
            else if (msg.startsWith("BED DESTRUCTION > Your Bed")) {
                if (debug) client.print("&cYou lost your bed!");
                else client.log(myTeam + " " + myName + " " + "&cYou lost your bed!");
                bedsLost++;
            }
            else if (didPlayerGetKill(myName, msg)) {
                if (debug) client.print("&aYou got a kill!");
                else client.log(myTeam + " " + myName + " " + "&aYou got a kill!");
                kills++;
                gameKills++;
            }
            else if (msg.equals("You have respawned!")) {
                if (debug) client.print("&cYou died.");
                else client.log(myTeam + " " + myName + " " + "&cYou died.");
                deaths++;
            }
            else if (msg.equals("You have been eliminated!")) {
                if (debug) client.print("&4You final died.");
                else client.log(myTeam + " " + myName + " " + "&4You final died.");
                finalDeaths++;
            }
            else if (!myTeam.isEmpty() && msg.startsWith("TEAM ELIMINATED > " + myTeam)) {
                if (debug) client.print("&4You lost!");
                else client.log(myTeam + " " + myName + " " + "&4You lost!");
                losses++;
                winstreak = 0;
            }
            else if (msg.startsWith(" ") && msg.contains("1st Killer")) {
                if (debug) client.print("&cGame ended.");
                else client.log(myTeam + " " + myName + " " + "&cGame ended.");
                if (gameStart != 0L) {
                    gameEnd = client.time();
                    gameTimes.add(gameEnd - gameStart);
                }
            }
        }
    }
    return true;
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        gameFinals = 0;
        gameBeds = 0;
        gameKills = 0;
        if (gameEnd == 0L) gameStart = gameEnd = 0L;
    }
}

boolean didPlayerGetKill(String playerName, String message) {
    if (message.endsWith("FINAL KILL!") || message.startsWith("BED DESTRUCTION > ")) return false;
    for (String keyword : killKeywords) {
        if (message.contains(keyword + " " + playerName)) {
            return true;
        }
    }
    return false;
}

void refreshTeams(String playerName) {
    if (client.allowFlying()) return;
    for (NetworkPlayer pla : client.getWorld().getNetworkPlayers()) { for (int i = 0; i < colorKeysArray.length; i++) {
        if (pla.getName().equals(myName) && pla.getDisplayName().startsWith(util.color("&" + colorKeysArray[i]))) {
            myTeam = colorsArray[i];
        }
    }}
}

int getBedwarsStatus() {
    World world = client.getWorld();
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

String calculateRelativeTimestamp(long timeSince) {

    long hours = timeSince / 3600000L;
    long minutes = (timeSince % 3600000L) / 60000L;
    long seconds = (timeSince % 60000L) / 1000L;

    if (hours > 0) {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    } else {
        return String.format("%02d:%02d", minutes, seconds);
    }
}

String formatDoubleStr(double val) {
    String str;
    if (val % 1 == 0) {
        str = String.valueOf((int)val);
    } else {
        str = String.valueOf(val);
    }
    return str;
}

String formatNumber(int number) {
    int absoluteNumber = Math.abs(number);
    if (absoluteNumber < 1000) return Integer.toString(number);

    String numberStr = Integer.toString(absoluteNumber);
    StringBuilder formattedNumber = new StringBuilder(numberStr);

    int insertPosition = formattedNumber.length() - 3;
    while (insertPosition > 0) {
        formattedNumber.insert(insertPosition, ",");
        insertPosition -= 3;
    }

    if (number < 0) {
        formattedNumber.insert(0, "-");
    }

    return formattedNumber.toString();
}