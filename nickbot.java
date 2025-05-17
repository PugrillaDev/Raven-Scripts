/* 
    bot that auto claims nicks based on specified parameters
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/nickbot.java"
*/

static final String NICK_CLAIM_COMMAND = "/nick actuallyset ";
final String chatPrefix = "&7[&dN&7]&r ";
long lastSentLimbo = client.time();
long lastSentNick = client.time();
boolean enabled;
boolean startup;
boolean wait;
int INTERVAL = 20;
int counter = 0;
String currentNick = "";
List<String> containsList = new ArrayList<>();
List<String> startsWithList = new ArrayList<>();
List<String> endsWithList = new ArrayList<>();
String currentLobby = "";
String[] lobbies = { "mw", "blitz", "sw", "bw", "mm", "bb", "duels", "s", "classic", "arcade", "uhc", "tnt", "ww", "prototype" };
HashSet<Character> allowedRepeating = new HashSet<>(Arrays.asList(
    'a',
    'e',
    'i',
    'o',
    'u',
    'y',
    'A',
    'E',
    'I',
    'O',
    'U',
    'Y'
));

void onLoad() {
    modules.registerSlider("Nick Interval", "", 15, 1, 50, 1);
    modules.registerButton("Vowel Repeaters", true);
    modules.registerButton("4 Letters", true);
    modules.registerButton("N Word", true);
}

void onEnable() {
    enabled = false;
    if (!startup) {
        client.print(chatPrefix + "&eRun &3/nb &efor custom nickbot commands.");
        startup = true;
    }   
}

void onPreUpdate() {
    if (!enabled) return;

    int status = getLobbyStatus();
    if (status != 1) {
        if (status == 0 && client.time() - lastSentLimbo > 5000) {
            client.chat("/l " + lobbies[util.randomInt(0, lobbies.length - 1)]);
            lastSentLimbo = client.time();
        }
        return;
    }

    if (client.time() - lastSentNick > INTERVAL * 50) {
        INTERVAL = (int) modules.getSlider(scriptName, "Nick Interval");
        lastSentNick = client.time();
    
        client.chat("/nick help setrandom");
    }

    if (client.getScreen().equals("GuiScreenBook")) {
        List<String> pages = inventory.getBookContents();
        if (pages != null) {
            String nick = "";

            for (int i = 0; i < pages.size(); i++) {
                String page = pages.get(i);
                if (!page.equals("you:")) continue;
                nick = util.strip(pages.get(i + 1));
                break;
            }

            if (nick.isEmpty()) return;

            boolean isGood = isGood(nick);

            if (isGood) {
                client.chat(NICK_CLAIM_COMMAND + nick);
                client.print(chatPrefix + "&eClaimed nick &3" + nick + "&e!");
                enabled = false;
            } else {
                client.print(chatPrefix + "&eNew nick #" + (++counter) + ": &3" + nick + "&e.");
                client.closeScreen();
            }
        }
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (wait && msg.startsWith("You are now nicked as ")) {
        currentNick = msg.substring(22, msg.length() - 1);
        wait = false;
        enabled = true;
        client.print(chatPrefix + "&eCurrent nick set to &3" + currentNick + "&e.");
        client.print(chatPrefix + "&eNickbot has been " + (enabled ? "&aenabled" : "&cdisabled") + "&e.");
        return false;
    } else if (wait && msg.equals("Processing request. Please wait...")) {
        return false;
    } else if (msg.equals("Generating a unique random name. Please wait...")) {
        return false;
    }
    return true;
}

boolean isGood(String nick) {
    String lower = nick.toLowerCase();

    if (currentNick.toLowerCase().equals(lower)) return false;
    if (modules.getButton(scriptName, "4 Letters") && nick.length() == 4) return true;
    if (modules.getButton(scriptName, "N Word") && lower.contains("nickherr")) return true;

    if (modules.getButton(scriptName, "Vowel Repeaters") && Character.isUpperCase(nick.charAt(0))) {
        char lastChar = ' ';
        int repeating = 1;
        for (int i = 0; i < nick.length(); i++) {
            char currentChar = nick.charAt(i);
            if (allowedRepeating.contains(currentChar) && currentChar == lastChar) {
                repeating++;
            } else {
                lastChar = currentChar;
                repeating = 1;
            }

            if (repeating >= 3) {
                if (nick.length() > 9 && lower.contains("free")) return false;
                if (nick.length() >= 11) return false;
                return true;
            }
        }
    }

    for (String pattern : startsWithList)
        if (lower.startsWith(pattern)) return true;

    for (String pattern : endsWithList)
        if (lower.endsWith(pattern)) return true;

    for (String pattern : containsList)
        if (lower.contains(pattern)) return true;

    return false;
}

int getLobbyStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }

    ItemStack item = inventory.getStackInSlot(4);

    if (item == null || !item.name.equals("trapped_chest")) return -1;
    else return 1;
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        if (!c01.message.startsWith("/nb")) return true;
        String[] parts = c01.message.split(" ");
        if (parts.length <= 1) {
            String title = " &eNick Bot &7";
            String footerText = " &eMade by Pug &7";
            String[] messages = {
                "&3/nb toggle&e: Toggles the nickbot &aon&e/&coff&e.",
                "&3/nb test [nick]&e: Checks if a nick would be claimed or not.",
                "",
                "&eTypes of checks: &3startsWith, endsWith, contains&e.",
                "&3/nb <check> [pattern]&e: Checks if the nick matches a pattern.",
                "&3/nb <check>&e: Shows the contents of a check.",
                "&3/nb <check> clear&e: Clears contents of specified check.",
                "",
                "&eCheck the module settings as well!"
            };

            int maxPixelWidth = 0;
            for (String message : messages) {
                int messageWidth = render.getFontWidth(message);
                if (messageWidth > maxPixelWidth) {
                    maxPixelWidth = messageWidth;
                }
            }
            int titleWidth = render.getFontWidth(title);
            int footerTextWidth = render.getFontWidth(footerText);
            if (titleWidth > maxPixelWidth) maxPixelWidth = titleWidth;
            if (footerTextWidth > maxPixelWidth) maxPixelWidth = footerTextWidth;

            int headerFooterWidth = maxPixelWidth + render.getFontWidth("  ");
            int titlePaddingTotal = headerFooterWidth - titleWidth;
            int titlePaddingSides = titlePaddingTotal / 2;

            String header = "&7" + generatePadding('-', titlePaddingSides) + title + generatePadding('-', titlePaddingSides);
            if (titlePaddingTotal % 2 != 0) header += "-";
            client.print(chatPrefix + header);

            for (String message : messages) {
                int messagePixelWidth = render.getFontWidth(message);
                int totalPaddingWidth = maxPixelWidth - messagePixelWidth;
                int paddingLeftWidth = totalPaddingWidth / 2;
                String paddedMessage = generatePadding(' ', paddingLeftWidth) + message;
                client.print(chatPrefix + paddedMessage);
            }

            int footerPaddingTotal = headerFooterWidth - footerTextWidth;
            int footerPaddingSides = footerPaddingTotal / 2;
            String footer = "&7" + generatePadding('-', footerPaddingSides) + footerText + generatePadding('-', footerPaddingSides);
            if (footerPaddingTotal % 2 != 0) footer += "-";
            client.print(chatPrefix + footer);

            return false;
        }

        String command = parts[1];

        if (command.equalsIgnoreCase("toggle")) {
            if (enabled) {
                enabled = false;
                client.print(chatPrefix + "&eNickbot has been " + (enabled ? "&aenabled" : "&cdisabled") + "&e.");
            } else {
                wait = true;
                counter = 0;
                client.chat("/nick reuse");
                client.print(chatPrefix + "&eFetching current nick...");
            }
            return false;
        } else if (command.equalsIgnoreCase("test")) {
            if (parts.length < 3) {
                client.print(chatPrefix + "&eInvalid syntax. Use &3/nb test [nick]&e.");
                return false;
            }

            String nick = parts[2];

            boolean isGood = isGood(nick);

            client.print(chatPrefix + "&eThe nick &3" + nick + " &eis " + (isGood ? "&agood" : "&cbad") + "&e.");

            return false;
        } else if (command.equalsIgnoreCase("contains")) {
            if (parts.length < 3) {
                if (containsList.size() > 0) {
                    String msg = chatPrefix + "&eList of &3" + containsList.size() + " &econtains pattern";
                    if (containsList.size() != 1) msg += "s";
                    msg += ":";
                    client.print(msg);
                    for (String pattern : containsList) {
                        client.print(chatPrefix + "&e\"&3" + pattern + "&e\"");
                    }
                } else {
                    client.print(chatPrefix + "&eYou aren't using any contains patterns!");
                }
                return false;
            }

            String pattern = parts[2];

            if (pattern.equalsIgnoreCase("clear")) {
                String msg = chatPrefix + "&eCleared &3" + containsList.size() + " &econtains pattern";
                if (containsList.size() != 1) msg += "s";
                msg += ".";
                containsList.clear();
                client.print(msg);
                return false;
            }

            containsList.add(pattern);
            client.print(chatPrefix + "&eAdded &3" + pattern + " &eto the contains list!");

            return false;
        } else if (command.equalsIgnoreCase("startswith")) {
            if (parts.length < 3) {
                if (startsWithList.size() > 0) {
                    String msg = chatPrefix + "&eList of &3" + startsWithList.size() + " &estartsWith pattern";
                    if (startsWithList.size() != 1) msg += "s";
                    msg += ":";
                    client.print(msg);
                    for (String pattern : startsWithList) {
                        client.print(chatPrefix + "&e\"&3" + pattern + "&e\"");
                    }
                } else {
                    client.print(chatPrefix + "&eYou aren't using any startsWith patterns!");
                }
                return false;
            }

            String pattern = parts[2];

            if (pattern.equalsIgnoreCase("clear")) {
                String msg = chatPrefix + "&eCleared &3" + startsWithList.size() + " &estartsWith pattern";
                if (startsWithList.size() != 1) msg += "s";
                msg += ".";
                startsWithList.clear();
                client.print(msg);
                return false;
            }

            startsWithList.add(pattern);
            client.print(chatPrefix + "&eAdded &3" + pattern + " &eto the startsWith list!");

            return false;
        } else if (command.equalsIgnoreCase("endswith")) {
            if (parts.length < 3) {
                if (endsWithList.size() > 0) {
                    String msg = chatPrefix + "&eList of &3" + endsWithList.size() + " &eendsWith pattern";
                    if (endsWithList.size() != 1) msg += "s";
                    msg += ":";
                    client.print(msg);
                    for (String pattern : endsWithList) {
                        client.print(chatPrefix + "&e\"&3" + pattern + "&e\"");
                    }
                } else {
                    client.print(chatPrefix + "&eYou aren't using any endsWith patterns!");
                }
                return false;
            }

            String pattern = parts[2];

            if (pattern.equalsIgnoreCase("clear")) {
                String msg = chatPrefix + "&eCleared &3" + endsWithList.size() + " &eendsWith pattern";
                if (endsWithList.size() != 1) msg += "s";
                msg += ".";
                endsWithList.clear();
                client.print(msg);
                return false;
            }

            endsWithList.add(pattern);
            client.print(chatPrefix + "&eAdded &3" + pattern + " &eto the endsWith list!");

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