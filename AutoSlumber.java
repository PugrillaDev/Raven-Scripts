boolean finished = true;

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        modules.disable(scriptName);
    }
}

void onPreUpdate() {
    Entity player = client.getPlayer();
    if (player.getTicksExisted() % 5 != 0) return;
    int tickets = 0, maxTickets = 0;

    List<String> scoreboard = world.getScoreboard();
    if (scoreboard == null) return;

    for (String line : scoreboard) {
        line = util.strip(line);
        if (!line.startsWith("Tickets")) continue;
        String[] parts = line.replaceAll("Tickets: ", "").split("/");
        tickets = Integer.parseInt(parts[0].replaceAll(",", ""));
        maxTickets = Integer.parseInt(parts[1].replaceAll(",", ""));
    }

    if (client.getPlayer().getHeldItem() != null) return;
    
    Object[] raycastResult = client.raycastEntity(3);
    if (raycastResult == null) return;
    Entity raycastEntity = (Entity)raycastResult[0];

    if (finished && client.getScreen().isEmpty() && (raycastEntity.type.equals("EntityArmorStand") || raycastEntity.type.equals("EntityOtherPlayerMP"))) {
        keybinds.leftClick();
    } else if (client.getScreen().equals("GuiChest")) {
        int slotToClick = -1;
        switch (inventory.getChest()) {
            case "Ticket Machine":
                if (tickets < 75) {
                    client.print("ran out of tickets");
                    modules.disable(scriptName);
                }

                int xpDoublers = -1;
                ItemStack stack27 = inventory.getStackInChestSlot(15);

                if (stack27 != null) {
                    for (String raw : stack27.getTooltip()) {
                        String line = util.strip(raw);
                        if (line.startsWith("Your Doublers")) {
                            try {
                                String afterColon = line.split(": ")[1];
                                xpDoublers = Integer.parseInt(afterColon.split("/")[0].replace(",", ""));
                            } catch (Exception ignored) { }
                            break;
                        }
                    }
                }

                if (xpDoublers >= 9999) {
                    client.print("max doublers reached (" + xpDoublers + ")");
                    modules.disable(scriptName);
                    break;
                }
                
                slotToClick = 11;
                finished = false;
                break;
            case "Item Submission":
                if (tickets >= maxTickets) {
                    client.print("reached max tickets");
                    modules.disable(scriptName);
                }
                slotToClick = 11;
                break;

        }
        if (slotToClick != -1) {
            if (tickets >= 7500) {
                inventory.click(slotToClick, 1, 1);
            } else if (tickets >= 750) {
                inventory.click(slotToClick, 1, 0);
            } else {
                inventory.click(slotToClick, 0, 0);
            }
        }
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (msg.equals("You do not have enough Slumber Tickets for this!")) {
        modules.disable(scriptName);
    } else if (msg.startsWith("TICKET REWARD! ")) {
        finished = true;
    }
    return true;
}