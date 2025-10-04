long won = 0;

boolean onChat(String message) {

    if (message.charAt(0) == ' ' && message.contains("1st Killer - ")) {
        sendAutoGG();
    }

    return true;
}

void sendAutoGG() {
    if (client.time() - won > 10000) {
        client.chat("/ac gg");
    }
    won = client.time();
}