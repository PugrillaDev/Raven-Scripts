/* 
    renders the blocks covering a bed defense
    designed specifically for bedwars may not work properly in other gamemodes
    loadstring: load - "https://raw.githubusercontent.com/PugrillaDev/Raven-Scripts/refs/heads/main/BedPlates.java"
*/

Map<String, Map<String, Object>> bedPositions = new ConcurrentHashMap<>();
Map<String, Boolean> searchedBlocks = new ConcurrentHashMap<>();
Map<String, ItemStack> stacks = new HashMap<>();
HashSet<Integer> yLevels = new HashSet<>();
boolean keybindPressed;
boolean lastPressed;
boolean display = true;
int keybind;
int mode;
HashSet<String> invalid = new HashSet<>(Arrays.asList(
    "leaves", "water", "lava", 
    "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves", // Leaves
    "torch", "redstone_torch", // Torches
    "wooden_slab", "stone_slab", "stone_slab2", "double_wooden_slab", "double_stone_slab", // Wooden slabs
    "fire",
    "bed", // Beds
    "piston", "sticky_piston", "piston_extension", // Pistons
    "log", "log2",
    "oak_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "acacia_stairs", "dark_oak_stairs", "stone_stairs", "cobblestone_stairs", "brick_stairs", "stone_brick_stairs", "sandstone_stairs", "nether_brick_stairs", "quartz_stairs", "red_sandstone_stairs", // Stairs
    "redstone_wire", "daylight_sensor", // Redstone
    "wheat", "carrots", "potatoes", "beetroots", // Crops
    "farmland",
    "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door", // Doors
    "rails", "activator_rail", "detector_rail", "powered_rail", // Rails
    "ladder", "furnace", "chest", "trapped_chest", // Ladders and containers
    "sign", // Signs
    "dispenser", "dropper", // Redstone components
    "hopper", "lever", "pressure_plate", "button", // Redstone components
    "snow", // Snow
    "cactus", "sugar_cane", // Plants
    "jukebox", // Blocks
    "pumpkin", "jack_o_lantern", // Pumpkins
    "cake",
    "redstone_repeater", "redstone_comparator", // Redstone devices
    "trapdoor",
    "monster_egg", "stone_bricks", // Blocks
    "prismarine", "prismarine_bricks", "dark_prismarine", // Prismarine
    "sponge", 
    "brown_mushroom_block", "red_mushroom_block", // Mushroom blocks
    "cobblestone_wall", // Walls
    "flower_pot", // Decoration
    "skull", // Heads
    "quartz_block", "quartz_pillar", "chiseled_quartz_block", // Quartz
    "anvil" // Utility
));

String[] keyNames = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    "BACK", "CAPITAL", "COMMA", "DELETE", "DOWN", "END", "ESCAPE", "F1", "F2", "F3", "F4", "F5",
    "F6", "F7", "HOME", "INSERT", "LBRACKET", "LCONTROL", "LMENU", "LMETA", "LSHIFT", "MINUS",
    "NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5", "NUMPAD6", "NUMPAD7",
    "NUMPAD8", "NUMPAD9", "PERIOD", "RETURN", "RCONTROL", "RSHIFT", "RBRACKET", "SEMICOLON",
    "SLASH", "SPACE", "TAB", "GRAVE"
};

int backgroundColor = new Color(37, 37, 43).getRGB();
int borderColor = new Color(52, 54, 59).getRGB();
double verticalOffset = 1;
int maxDistance = 100;
boolean autoScale;
float scale = 1f;
boolean alignTop;

void onLoad() {
    modules.registerButton("Align Top Center", false);
    modules.registerButton("Auto Scale", true);
    modules.registerSlider("Scale", "", 0.8, 0.1, 1.5, 0.1);
    modules.registerSlider("Render Distance", " blocks", 150, 10, 200, 1);
    modules.registerSlider("Y Offset", " blocks", 1, -10, 10, 0.5);
    modules.registerSlider("Mode", "", 0, new String[]{ "Static", "Toggle", "Hold" });
    modules.registerSlider("Keybind", "", 0, keyNames);
}

ItemStack getStackFromName(String name) {
    return stacks.computeIfAbsent(name, key -> {
        try {
            if (name.equals("water")) return new ItemStack("water_bucket");
            if (name.equals("lava")) return new ItemStack("lava_bucket");
            if (name.equals("fire")) return new ItemStack("flint_and_steel");
            return new ItemStack(key);
        } catch (Exception e) {
            return new ItemStack("barrier");
        }
    });
}

void onPreUpdate() {
    scale = (float) modules.getSlider(scriptName, "Scale");
    maxDistance = (int) modules.getSlider(scriptName, "Render Distance");
    verticalOffset = modules.getSlider(scriptName, "Y Offset");
    autoScale = modules.getButton(scriptName, "Auto Scale");
    alignTop = modules.getButton(scriptName, "Align Top Center");
    mode = (int) modules.getSlider(scriptName, "Mode");
    keybind = /* keybinds. */keybinds.getKeyIndex(keyNames[(int)modules.getSlider(scriptName, "Keybind")]);

    Entity player = client.getPlayer();
    int ticks = player.getTicksExisted();
    
    if (ticks % 20 == 0) {
        searchForBeds();
    }

    if (ticks % 3 == 0) {
        findYLevels();
    }

    if (ticks % 300 == 0) {
        searchedBlocks.clear();
    }

    updateBeds();
}

@SuppressWarnings("unchecked")
void onRenderWorld(float partialTicks) {
    if (bedPositions.isEmpty()) return;

    if (mode != 0) {
        boolean noGui = client.getScreen().isEmpty();
        boolean keybindPressed = keybinds.isKeyDown(keybind);

        if (mode == 1) {
            if (keybindPressed && !lastPressed) {
                display = !display;
            }
        } else {
            display = noGui && keybindPressed;
        }

        lastPressed = keybindPressed;

        if (!display) return;
    }

    List<Map<String, Object>> sortedBedPositions = new ArrayList<>(bedPositions.values());
    Collections.sort(sortedBedPositions, Comparator.comparingDouble(bedData -> (double) bedData.get("distance")));
    Collections.reverse(sortedBedPositions);

    int size = client.getDisplaySize()[2];

    gl.alpha(false);
    for (Map<String, Object> bedData : sortedBedPositions) {
        double distance = (double) bedData.get("distance");
        double lastDistance = (double) bedData.getOrDefault("lastdistance", distance);
        
        double interpolatedDistance = lastDistance + (distance - lastDistance) * partialTicks;
        if (interpolatedDistance > maxDistance) continue;

        boolean visible = (boolean) bedData.get("visible");
        if (!visible) continue;

        List<String> layers = (List<String>) bedData.getOrDefault("layers", new ArrayList<>());
        if (layers.isEmpty()) continue;
        
        Vec3 position1 = (Vec3) bedData.get("position1");
        Vec3 position2 = (Vec3) bedData.get("position2");

        Vec3 bedPos = position1.offset(
            (position2.x - position1.x) / 2 + 0.5,
            verticalOffset,
            (position2.z - position1.z) / 2 + 0.5
        );

        Vec3 screenPos = render.worldToScreen(bedPos.x, bedPos.y, bedPos.z, size, partialTicks);
        if (screenPos.z < 0 || screenPos.z >= 1.0003684d) continue;

        float currentScale = scale;
        if (autoScale) {
            currentScale = (float) Math.max(0, scale * (1 - interpolatedDistance / maxDistance));
        }

        float itemSize = 16 * currentScale;
        float itemPadding = currentScale * 2;
        float boxSize = itemSize + itemPadding;
        float rectWidth = layers.size() * boxSize;
        float rectHeight = boxSize;

        float startX = (float) screenPos.x - rectWidth / 2f;
        float startY;
        if (alignTop) {
            startY = (float) screenPos.y;
        } else {
            startY = (float) screenPos.y - rectHeight;
        }

        float borderThickness = currentScale * 3;

        render.roundedRect(
            startX - borderThickness, 
            startY - borderThickness, 
            startX + rectWidth + borderThickness, 
            startY + rectHeight + borderThickness, 
            borderThickness, 
            backgroundColor
        );

        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.get(i);
            ItemStack itemToRender = getStackFromName(layer);

            float itemX = startX + i * boxSize;
            float itemY = startY;

            render.roundedRect(
                itemX + itemPadding / 4f, 
                itemY + itemPadding / 4f, 
                itemX + boxSize - itemPadding / 4f,
                itemY + boxSize - itemPadding / 4f,
                currentScale * 2,
                borderColor
            );

            render.item(itemToRender, itemX + itemPadding / 2f, itemY + itemPadding / 2f, currentScale);
        }
    }
    gl.alpha(true);
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        yLevels.clear();
        bedPositions.clear();
        searchedBlocks.clear();
    }
}

void updateBeds() {
    if (bedPositions.isEmpty()) return;

    Entity player = client.getPlayer();
    Vec3 pos = render.getPosition();
    Vec3 lastPos = render.getPosition();

    client.async(() -> {
        for (Map<String, Object> bedData : bedPositions.values()) {
            Vec3 bedPos1 = (Vec3) bedData.get("position1");
            Vec3 bedPos2 = (Vec3) bedData.get("position2");

            double distance = pos.distanceTo(bedPos1);
            double lastDistance = lastPos.distanceTo(bedPos1);

            bedData.put("distance", distance);
            bedData.put("lastdistance", lastDistance);

            Block bedBlock = world.getBlockAt((int) bedPos1.x, (int) bedPos1.y, (int) bedPos1.z);
            boolean visible = bedBlock.name.equals("bed");
            bedData.put("visible", visible);

            if (visible) {
                int delay = getDelay(distance);
                long lastCheck = (long) bedData.getOrDefault("lastcheck", 0L);

                if (client.time() > lastCheck + delay) {
                    List<String> layers = getBedDefenseLayers(bedPos1, bedPos2);
                    bedData.put("layers", layers);
                    bedData.put("lastcheck", client.time());
                }
            }
        }
    });
}

int getDelay(double distance) {
    if (distance > 100) {
        return 4000;
    } else if (distance > 50) {
        return 3000;
    } else if (distance > 25) {
        return 2000;
    } else if (distance > 15) {
        return 1000;
    } else {
        return 1000;
    }
} 

void searchForBeds() {
    if (yLevels.isEmpty()) return;
    List<Entity> players = world.getPlayerEntities();
    Vec3 myPos = client.getPlayer().getPosition();

    client.async(() -> {
        for (Entity player : players) {
            if (player.getNetworkPlayer() == null) continue;

            Vec3 playerPos = player.getBlockPosition();
            int startX = (int) playerPos.x - 20;
            int endX = (int) playerPos.x + 20;
            int startZ = (int) playerPos.z - 20;
            int endZ = (int) playerPos.z + 20;

            for (int yLevel : yLevels) { for (int x = startX; x <= endX; x++) { for (int z = startZ; z <= endZ; z++) {
                String blockKey = "1" + x + "," + yLevel + "," + z;
                if (searchedBlocks.containsKey(blockKey)) continue;

                Block block = world.getBlockAt(x, yLevel, z);
                if (!block.name.equals("bed")) continue;

                Block bedXPlus = world.getBlockAt(x + 1, yLevel, z);
                Block bedZPlus = world.getBlockAt(x, yLevel, z + 1);
                if (bedXPlus.name.equals("bed") || bedZPlus.name.equals("bed")) continue;

                Vec3 position1 = new Vec3(x, yLevel, z);
                Vec3 position2 = null;

                Block bedXMinus = world.getBlockAt(x - 1, yLevel, z);
                if (bedXMinus.name.equals("bed")) {
                    position2 = new Vec3(x - 1, yLevel, z);
                } else {
                    Block bedZMinus = world.getBlockAt(x, yLevel, z - 1);
                    if (bedZMinus.name.equals("bed")) {
                        position2 = new Vec3(x, yLevel, z - 1);
                    }
                }

                Map<String, Object> bedData = new ConcurrentHashMap<>();
                bedData.put("visible", true);
                bedData.put("distance", myPos.distanceTo(position1));
                bedData.put("position1", position1);
                bedData.put("position2", position2);

                List<String> layers = getBedDefenseLayers(position1, position2);
                bedData.put("layers", layers);
                bedData.put("lastcheck", client.time());

                bedPositions.put(blockKey, bedData);
                searchedBlocks.put(blockKey, true);
            }}}
        }
    });
}

void findYLevels() {
    List<Entity> players = world.getPlayerEntities();

    client.async(() -> {
        for (Entity player : players) {
            if (player.getNetworkPlayer() == null || player.getSwingProgress() == 0 || !player.isHoldingBlock()) continue;

            Vec3 playerPos = player.getBlockPosition();

            int startX = (int) playerPos.x - 4;
            int endX = (int) playerPos.x + 4;
            int startY = (int) playerPos.y - 4;
            int endY = (int) playerPos.y + 4;
            int startZ = (int) playerPos.z - 4;
            int endZ = (int) playerPos.z + 4;

            for (int x = startX; x <= endX; x++) { for (int y = startY; y <= endY; y++) { for (int z = startZ; z <= endZ; z++) {
                String blockKey = "2" + x + "," + y + "," + z;
                if (searchedBlocks.containsKey(blockKey)) continue;

                Block block = world.getBlockAt(x, y, z);
                if (block.name.equals("bed")) {
                    yLevels.add((int) Math.floor(y));
                }

                searchedBlocks.put(blockKey, true);
            }}}
        }
    });
}

List<String> getBedDefenseLayers(Vec3 position1, Vec3 position2) {
    boolean facingZ = Math.abs(position2.z - position1.z) > Math.abs(position2.x - position1.x);

    List<String> finalLayers = new ArrayList<>();
    Vec3[] beds = { position1, position2 };
    HashSet<String> addedBlocks = new HashSet<>();

    int maxLayers = 5;
    int airLayersCount = 0;

    for (int layer = 1; layer <= maxLayers; layer++) {
        Map<String, Integer> blockCounts = new HashMap<>();
        Vec3 startPos;

        int totalBlocks = 0;
        int airBlocks = 0;

        for (int bedPart = 0; bedPart < beds.length; bedPart++) {
            Vec3 bed = beds[bedPart];
            int offset = bedPart == 0 ? layer : -layer;

            if (facingZ) {
                startPos = new Vec3(bed.x, bed.y, bed.z + offset);
            } else {
                startPos = new Vec3(bed.x + offset, bed.y, bed.z);
            }

            for (int step1 = 0; step1 <= layer; step1++) {
                int yOffset = 0;
                for (int step2 = step1; step2 >= 0; step2--) {
                    Vec3 pos1, pos2;

                    if (facingZ) {
                        pos1 = new Vec3(startPos.x - step2, startPos.y + yOffset, startPos.z - (bedPart == 0 ? step1 : -step1));
                        pos2 = new Vec3(startPos.x + step2, startPos.y + yOffset, startPos.z - (bedPart == 0 ? step1 : -step1));
                    } else {
                        pos1 = new Vec3(startPos.x - (bedPart == 0 ? step1 : -step1), startPos.y + yOffset, startPos.z - step2);
                        pos2 = new Vec3(startPos.x - (bedPart == 0 ? step1 : -step1), startPos.y + yOffset, startPos.z + step2);
                    }

                    String blockType1 = addBlockToCount(pos1, blockCounts);
                    if (blockType1.equals("air")) {
                        airBlocks++;
                    }
                    totalBlocks++;

                    if (!pos1.equals(pos2)) {
                        String blockType2 = addBlockToCount(pos2, blockCounts);
                        if (blockType2.equals("air")) {
                            airBlocks++;
                        }
                        totalBlocks++;
                    }

                    if (step2 > 0) {
                        yOffset++;
                    }
                }
            }
        }

        if (totalBlocks == 0 || ((float) airBlocks / totalBlocks) > 0.2) {
            airLayersCount++;
            if (airLayersCount >= 2) {
                break;
            }
            continue;
        }

        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            String blockType = entry.getKey();
            int count = entry.getValue();
            float blockFraction = (float) count / totalBlocks;

            if (blockFraction >= 0.2 && !blockType.equals("air") && addedBlocks.add(blockType)) {
                finalLayers.add(blockType);
            }
        }
    }

    return finalLayers;
}

String addBlockToCount(Vec3 pos, Map<String, Integer> blockCounts) {
    Block block = world.getBlockAt((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
    String blockType = block.name + (block.variant != 0 && !invalid.contains(block.name) ? ":" + block.variant : "");
    blockCounts.put(blockType, blockCounts.getOrDefault(blockType, 0) + 1);
    return blockType;
}