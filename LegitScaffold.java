/*
This legit scaffold is better than others because it uses a player simulator before Minecraft handles your movement
so it knows if you're about to walk past the edge offset before you move. That means with max vanilla offset (0.3) you
won't crouch unless you're actually going to fall. Most other legit scaffolds like Vape or Myau just check if there is air 
under you after you have already moved, which is slower and less reliable.
*/

double HW = 0.3;
double[][] CORNERS = {{ -HW, -HW }, { HW, -HW }, { -HW, HW }, { HW, HW }};
boolean sneakingFromScript;
boolean placed;
boolean forceRelease;
int sneakJumpDelayTicks;
int sneakJumpStartTick = -1;
int unsneakDelayTicks;
int unsneakStartTick = -1;

void onLoad() {
    modules.registerSlider("Edge offset", " blocks", 0, 0, 0.3, 0.01);
    modules.registerSlider("Unsneak delay", "ms", 50, 50, 300, 5);
    modules.registerSlider("Sneak on jump", "ms", 0, 0, 500, 5);
    modules.registerButton("Sneak key pressed", false);
    modules.registerButton("Holding blocks", false);
    modules.registerButton("Looking down", false);
    modules.registerButton("Not moving forward", false);
}

void onEnable() {
    unsneakStartTick = sneakJumpStartTick = -1;
}

void onPrePlayerInput(MovementInput m) {
    boolean manualSneak = isManualSneak();
    boolean requireSneak = modules.getButton(scriptName, "Sneak key pressed");

    if (manualSneak && !requireSneak) {
        resetUnsneak();
        return;
    }

    if (requireSneak && (!manualSneak || (m.forward == 0 && m.strafe == 0))) {
        if (!manualSneak) resetUnsneak();
        repressSneak(m);
        return;
    }

    Entity player = client.getPlayer();
    if (modules.getButton(scriptName, "Not moving forward") && client.getForward() > 0 ||
        modules.getButton(scriptName, "Looking down") && player.getPitch() < 70 ||
        modules.getButton(scriptName, "Holding blocks") && !player.isHoldingBlock()) {
        if (requireSneak) {
            repressSneak(m);
        }
        return;
    }

    if (m.jump && player.onGround() && (m.forward != 0 || m.strafe != 0) && modules.getSlider(scriptName, "Sneak on jump") > 0) {
        if (!requireSneak || forceRelease) {
            sneakJumpStartTick = player.getTicksExisted();
            double raw = modules.getSlider(scriptName, "Sneak on jump") / 50;
            int base = (int) raw;
            sneakJumpDelayTicks = base + (util.randomDouble(0, 1) < (raw - base) ? 1 : 0);
            pressSneak(m, true);
            return;
        }
    }

    Vec3 position = player.getPosition();
    Simulation sim = Simulation.create();
    if (client.isSneak()) {
        sim.setForward(m.forward / 0.3f);
        sim.setStrafe(m.strafe / 0.3f);
        sim.setSneak(false);
    }
    sim.tick();
    Vec3 simPosition = sim.getPosition();

    double edgeOffset = computeEdgeOffset(simPosition, position);
    if (Double.isNaN(edgeOffset)) {
        if (sneakingFromScript) tryReleaseSneak(m, true);
        return;
    }

    boolean shouldSneak = edgeOffset > modules.getSlider(scriptName, "Edge offset");
    boolean shouldRelease = sneakingFromScript;
    
    if (shouldSneak) {
        pressSneak(m, true);
    } else if (shouldRelease) {
        tryReleaseSneak(m, true);
    }
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C08) {
        C08 c08 = (C08) packet;
        if (c08.direction != 255 && sneakingFromScript && modules.getButton(scriptName, "Sneak key pressed")) {
            placed = true;
        }
    }
    return true;
}

void repressSneak(MovementInput m) {
    if (forceRelease && isManualSneak()) {
        keybinds.setPressed("sneak", true);
        m.sneak = true;
    }
    forceRelease = false;
}

void pressSneak(MovementInput m, boolean resetDelay) {
    m.sneak = true;
    sneakingFromScript = true;
    if (resetDelay) {
        unsneakStartTick = -1;
    }
    repressSneak(m);
}

void tryReleaseSneak(MovementInput m, boolean resetDelay) {
    int existed = client.getPlayer().getTicksExisted();
    if (unsneakStartTick == -1 && sneakJumpStartTick == -1) {
        unsneakStartTick = existed;
        double raw = (modules.getSlider(scriptName, "Unsneak delay") - 50) / 50;
        int base = (int) raw;
        unsneakDelayTicks = base + (util.randomDouble(0, 1) < (raw - base) ? 1 : 0);
    }

    if (existed - sneakJumpStartTick < sneakJumpDelayTicks) {
        pressSneak(m, false);
        return;
    }
    if (existed - unsneakStartTick < unsneakDelayTicks) {
        pressSneak(m, false);
        return;
    }

    releaseSneak(m, resetDelay);
}

void releaseSneak(MovementInput m, boolean resetDelay) {
    if (!modules.getButton(scriptName, "Sneak key pressed")) {
        m.sneak = false;
    }
    else if (sneakingFromScript && isManualSneak() && (placed || !client.getPlayer().onGround())) {
        keybinds.setPressed("sneak", false);
        m.sneak = false;
        forceRelease = true;
    }
    else if (forceRelease) {
        m.sneak = false;
    }

    sneakingFromScript = placed = false;
    if (resetDelay) {
        resetUnsneak();
    }
}

void resetUnsneak() {
    unsneakStartTick = sneakJumpStartTick = -1;
}

boolean isManualSneak() {
    return keybinds.isKeyDown(keybinds.getKeyCode("sneak"));
}

double computeEdgeOffset(Vec3 pos1, Vec3 pos2) {
    int floorY = (int)(pos1.y - 0.01);
    double best = Double.NaN;

    for (double[] c : CORNERS) {
        int bx = (int)Math.floor(pos2.x + c[0]);
        int bz = (int)Math.floor(pos2.z + c[1]);
        if (world.getBlockAt(bx, floorY, bz).name.equals("air")) continue;

        double offX = Math.abs(pos1.x - (bx + (pos1.x < bx + 0.5 ? 0 : 1)));
        double offZ = Math.abs(pos1.z - (bz + (pos1.z < bz + 0.5 ? 0 : 1)));
        boolean xDiff = (int)Math.floor(pos1.x) != bx;
        boolean zDiff = (int)Math.floor(pos1.z) != bz;

        double cornerDist;
        if (xDiff) {
            cornerDist = zDiff ? Math.max(offX, offZ) : offX;
        } else {
            cornerDist = zDiff ? offZ : 0;
        }

        best = Double.isNaN(best) ? cornerDist : Math.min(best, cornerDist);
    }

    return best;
}