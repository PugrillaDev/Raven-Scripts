/* Solves the quickmaths message automatically in lobby/pit */

void onLoad() {
    modules.registerSlider("Delay", "ms", 1500, 0, 5000, 100);
}

String a = "";
void onPreUpdate() {
    if (!a.isEmpty()) {
        client.chat(a);
        a = "";
    }
}

boolean onChat(String message) {
    String msg = util.strip(message);
    if (msg.startsWith("QUICK MATHS! Solve: ")) {
        client.print(message);
        String answer = solve(msg);
        client.print("&7[&aQM&7]&e " + msg.split(": ")[1] + " = &a" + answer);
        client.async(() -> {
            client.sleep((int)modules.getSlider(scriptName, "Delay"));
            a = answer;
        });
        return false;
    }

    return true;
}

String solve(String expression) {
    expression = expression.replace("QUICK MATHS! Solve: ", "").replace("x", "*");

    while (expression.contains("(")) {
        int start = expression.lastIndexOf('(');
        int end = expression.indexOf(')', start);
        String result = solve(expression.substring(start + 1, end));
        expression = expression.substring(0, start) + formatAndRound(result) + expression.substring(end + 1);
    }

    if (expression.contains("+")) {
        String[] parts = expression.split("\\+", 2);
        return formatAndRound(add(solve(parts[0]), solve(parts[1])));
    } else if (expression.contains("-")) {
        String[] parts = expression.split("-", 2);
        return formatAndRound(subtract(solve(parts[0]), solve(parts[1])));
    } else if (expression.contains("*")) {
        String[] parts = expression.split("\\*", 2);
        return formatAndRound(multiply(solve(parts[0]), solve(parts[1])));
    } else if (expression.contains("/")) {
        String[] parts = expression.split("/", 2);
        return formatAndRound(divide(solve(parts[0]), solve(parts[1])));
    } else if (expression.contains("^")) {
        String[] parts = expression.split("\\^", 2);
        return formatAndRound(power(solve(parts[0]), solve(parts[1])));
    } else {
        return formatAndRound(expression.trim());
    }
}

String formatAndRound(String val) {
    double num = Double.parseDouble(val);
    double roundedVal = util.round(num, 2);

    if (roundedVal % 1 == 0) {
        return String.valueOf((int)roundedVal);
    } else {
        return String.valueOf(roundedVal);
    }
}

String add(String a, String b) {
    return String.valueOf(Double.parseDouble(a) + Double.parseDouble(b));
}

String subtract(String a, String b) {
    return String.valueOf(Double.parseDouble(a) - Double.parseDouble(b));
}

String multiply(String a, String b) {
    return String.valueOf(Double.parseDouble(a) * Double.parseDouble(b));
}

String divide(String a, String b) {
    return String.valueOf(Double.parseDouble(a) / Double.parseDouble(b));
}

String power(String a, String b) {
    return String.valueOf(Math.pow(Double.parseDouble(a), Double.parseDouble(b)));
}