void onLoad() {
    modules.registerDescription("Lobby Messages");
    modules.registerButton("Disable lobby joins", true);
    modules.registerButton("Disable ticket machine", true);
    //modules.registerButton("Claim Daily Rewards", true); Needs to be fixed with something that is not in scripts at the moment...
}

boolean onChat(String msg) {
    String stripped = util.strip(msg);
    String text = stripped.toLowerCase();

    if (modules.getButton(scriptName, "Disable lobby joins") && filterLobbyJoin(stripped)) {
        return false;
    }

    if (modules.getButton(scriptName, "Disable ticket machine") && filterTicketMachine(stripped)) {
        return false;
    }

    if (modules.getButton(scriptName, "Claim Daily Rewards")) {
        handleReward(text);
    }

    return true;
}

boolean filterLobbyJoin(String msg) {
    return (msg.startsWith(" >>>") && msg.endsWith("lobby! <<<")) || (msg.startsWith("[") && msg.endsWith("lobby!"));
}

boolean filterTicketMachine(String msg) {
    return msg.contains(" has found a ") && (msg.contains(" COMMON ") || msg.contains(" RARE ") || msg.contains(" EPIC ") || msg.contains(" LEGENDARY "));
}

void handleReward(String text) {
    text = text.toLowerCase();
    int idx = text.indexOf("hypixel.net/claim-reward/");
    if (idx == -1) return;

    int start = text.lastIndexOf("http", idx);
    int end = text.indexOf(" ", idx);
    if (start == -1) return;
    if (end == -1) end = text.length();

    String rewardLink = text.substring(start, end).replace("https://hypixel.net/", "https://rewards.hypixel.net/");

    int codeStart = rewardLink.lastIndexOf("/") + 1;
    String code = rewardLink.substring(codeStart);
    if (code.length() != 8) return;

    if (!rewardLink.startsWith("https://rewards.hypixel.net/claim-reward/")) return;

    client.async(() -> {
        try {
            long now = client.time();
            client.print("&8[&cReward&8] &7Found reward link. Claiming...");
            Request req1 = new Request("GET", rewardLink);
            req1.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36");
            Response res1 = req1.fetch();
            String html = res1.string();

            String token = extractBetween(html, "window.securityToken = \"", "\"");
            String data = extractBetween(html, "window.appData = '", "';").replace("\\'", "'");
            if (token.isEmpty() || data.isEmpty()) {
                client.print("&8[&cReward&8] &7Reward was already claimed or is invalid.");
                return;
            }

            Json appData = Json.parse(data);
            List<Json> cards = appData.get("rewards").asArray();

            Map<String, Integer> rarityBonus = new HashMap<>();
            rarityBonus.put("COMMON", 0);
            rarityBonus.put("RARE", 50);
            rarityBonus.put("EPIC", 150);
            rarityBonus.put("LEGENDARY", 500);

            String best = "";
            double bestScore = -1;
            int index = 0;

            client.print("&8[&cReward&8] &7Rewards offered:");
            for (int i = 0; i < cards.size(); i++) {
                Json c = cards.get(i);
                String gameType = c.has("gameType") ? c.get("gameType").asString() : "";
                String type = c.has("reward") ? c.get("reward").asString() : "Unknown";
                String rarity = c.has("rarity") ? c.get("rarity").asString() : "COMMON";
                int amount = c.has("amount") ? c.get("amount").asInt() : 0;

                double score = 1;
                if (type.equals("adsense_token")) score = amount * 250;
                else if (type.equals("experience")) score = amount * 0.002;
                else if (type.equals("dust")) score = amount * 4;
                else if (type.equals("coins") || type.equals("tokens")) score = amount * 0.001;
                else if (type.equals("housing_package")) score = 10;

                if (!type.equals("coins") && !type.equals("tokens")) {
                    score += rarityBonus.getOrDefault(rarity, 0);
                }

                String name = prettyReward(type);
                if (type.equals("housing_package")) name = prettyHousingBlock(c.get("package").asString()) + " " + name;
                if (!gameType.isEmpty()) name = prettyGameType(gameType) + " " + name;
                String displayRarity = prettyRarity(rarity);
                String msg = rarityColor(rarity) + displayRarity + "&7" + (amount > 0 ? " " + amount + "x " : " ") + name;

                client.print("&8[&cReward&8] " + msg);

                if (score > bestScore) {
                    bestScore = score;
                    best = msg;
                    index = i;
                }
            }

            String claim = "https://rewards.hypixel.net/claim-reward/claim?id=" + appData.get("id").asString() + "&option=" + index + "&activeAd=" + appData.get("activeAd").asString() + "&_csrf=" + token;
            
            Request req2 = new Request("POST", claim);
            req2.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36");
            Response res2 = req2.fetch();
            long finished = client.time();

            if (res2 == null) {
                client.print("&8[&cReward&8] &7Failed to claim reward. Request timed out.");
                return;
            }

            int status = res2.code();

            if (status == 200) {
                client.print("&8[&cReward&8] &7Claimed " + best + " &8(&c" + (finished - now) + "ms&8)");
            } else if (status == 400) {
                client.print("&8[&cReward&8] &7Reward was already claimed. &8(&c" + (finished - now) + "ms&8)");
            } else {
                client.print("&8[&cReward&8] &7Failed to claim reward. Status &c" + status);
            }
        } catch (Exception e) {
            client.print("&8[&cReward&8] &7Error while claiming reward: &c" + e);
        }
    });
}

String extractBetween(String src, String a, String b) {
    int start = src.indexOf(a);
    if (start == -1) return "";
    start += a.length();
    int end = src.indexOf(b, start);
    if (end == -1) return "";
    return src.substring(start, end);
}

String prettyRarity(String rarity) {
    if (rarity == null || rarity.isEmpty()) return "Common";
    return rarity.substring(0, 1).toUpperCase() + rarity.substring(1).toLowerCase();
}

String prettyReward(String raw) {
    Map<String, String> rewardsData = new HashMap<>();
    rewardsData.put("adsense_token", "Reward Token");
    rewardsData.put("housing_package", "Housing Block");
    rewardsData.put("dust", "Mystery Dust");
    if (rewardsData.containsKey(raw)) return rewardsData.get(raw);
    String[] parts = raw.replace('_', ' ').split(" ");
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
        if (part.length() > 0) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        sb.append(" ");
    }
    return sb.toString().trim();
}

String prettyHousingBlock(String raw) {
    Map<String, String> housingData = new HashMap<>();
    housingData.put("specialoccasion_reward_card_skull_red_treasure_chest", "Red Treasure Chest");
    housingData.put("specialoccasion_reward_card_skull_gold_nugget", "Gold Nugget");
    housingData.put("specialoccasion_reward_card_skull_pot_o'_gold", "Pot O' Gold");
    housingData.put("specialoccasion_reward_card_skull_rubik's_cube", "Rubik's Cube");
    housingData.put("specialoccasion_reward_card_skull_piggy_bank", "Piggy Bank");
    housingData.put("specialoccasion_reward_card_skull_health_potion", "Health Potion");
    housingData.put("specialoccasion_reward_card_skull_green_treasure_chest", "Green Treasure Chest");
    housingData.put("specialoccasion_reward_card_skull_coin_bag", "Coin Bag");
    housingData.put("specialoccasion_reward_card_skull_ornamental_helmet", "Ornamental Helmet");
    housingData.put("specialoccasion_reward_card_skull_pocket_galaxy", "Pocket Galaxy");
    housingData.put("specialoccasion_reward_card_skull_mystic_pearl", "Mystic Pearl");
    housingData.put("specialoccasion_reward_card_skull_agility_potion", "Agility Potion");
    housingData.put("specialoccasion_reward_card_skull_blue_treasure_chest", "Blue Treasure Chest");
    housingData.put("specialoccasion_reward_card_skull_golden_chalice", "Golden Chalice");
    housingData.put("specialoccasion_reward_card_skull_jewelery_box", "Jewelry Box");
    housingData.put("specialoccasion_reward_card_skull_crown", "Crown");
    housingData.put("specialoccasion_reward_card_skull_molten_core", "Molten Core");
    housingData.put("specialoccasion_reward_card_skull_mana_potion", "Mana Potion");
    if (housingData.containsKey(raw)) return housingData.get(raw);
    return raw.replace('_', ' ').toLowerCase();
}

String prettyGameType(String raw) {
    Map<String, String> gameTypes = new HashMap<>();
    gameTypes.put("walls3", "Mega Walls");
    gameTypes.put("quakecraft", "Quakecraft");
    gameTypes.put("walls", "Walls");
    gameTypes.put("paintball", "Paintball");
    gameTypes.put("survival_games", "Blitz SG");
    gameTypes.put("tntgames", "TNT Games");
    gameTypes.put("vampirez", "VampireZ");
    gameTypes.put("arcade", "Arcade");
    gameTypes.put("arena", "Arena");
    gameTypes.put("uhc", "UHC");
    gameTypes.put("mcgo", "Cops and Crims");
    gameTypes.put("battleground", "Warlords");
    gameTypes.put("super_smash", "Smash Heroes");
    gameTypes.put("gingerbread", "Turbo Kart Racers");
    gameTypes.put("skywars", "SkyWars");
    gameTypes.put("true_combat", "Crazy Walls");
    gameTypes.put("speeduhc", "Speed UHC");
    gameTypes.put("bedwars", "Bed Wars");
    gameTypes.put("build_battle", "Build Battle");
    gameTypes.put("murder_mystery", "Murder Mystery");
    gameTypes.put("duels", "Duels");
    gameTypes.put("legacy", "Classic");
    return gameTypes.getOrDefault(raw.toLowerCase(), raw.replace('_', ' ').toLowerCase());
}

String rarityColor(String rarity) {
    return rarity.equalsIgnoreCase("COMMON") ? "&f" : rarity.equalsIgnoreCase("RARE") ? "&9" : rarity.equalsIgnoreCase("EPIC") ? "&d" : rarity.equalsIgnoreCase("LEGENDARY") ? "&6" : "&f";
}