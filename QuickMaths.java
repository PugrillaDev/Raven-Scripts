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
        String answer = solveExpression(msg);
        client.print("&7[&aQM&7]&e " + msg.split(": ")[1] + " = &a" + answer);
        client.async(() -> {
            client.sleep((int) modules.getSlider(scriptName, "Delay"));
            a = answer;
        });
        return false;
    }
    return true;
}

String solveExpression(String expression) {
    expression = expression.replace("QUICK MATHS! Solve: ", "").replace("x", "*");
    return formatAndRound(parseExpression(expression));
}

double parseExpression(String expr) {
    char[] chars = expr.toCharArray();
    int[] pos = { -1 };
    int[] ch = { nextChar(chars, pos) };
    return parse(chars, pos, ch);
}

double parse(char[] chars, int[] pos, int[] ch) {
    double result = parseExpressionInternal(chars, pos, ch);
    if (pos[0] < chars.length) {
        return 0;
    }
    return result;
}

double parseExpressionInternal(char[] chars, int[] pos, int[] ch) {
    double x = parseTerm(chars, pos, ch);
    while (true) {
        if (eat(chars, pos, ch, '+')) x += parseTerm(chars, pos, ch);
        else if (eat(chars, pos, ch, '-')) x -= parseTerm(chars, pos, ch);
        else return x;
    }
}

double parseTerm(char[] chars, int[] pos, int[] ch) {
    double x = parseFactor(chars, pos, ch);
    while (true) {
        if (eat(chars, pos, ch, '*')) x *= parseFactor(chars, pos, ch);
        else if (eat(chars, pos, ch, '/')) x /= parseFactor(chars, pos, ch);
        else return x;
    }
}

double parseFactor(char[] chars, int[] pos, int[] ch) {
    if (eat(chars, pos, ch, '+')) return parseFactor(chars, pos, ch);
    if (eat(chars, pos, ch, '-')) return -parseFactor(chars, pos, ch);

    double x;
    int startPos = pos[0];
    if (eat(chars, pos, ch, '(')) {
        x = parseExpressionInternal(chars, pos, ch);
        eat(chars, pos, ch, ')');
    } else if ((ch[0] >= '0' && ch[0] <= '9') || ch[0] == '.') {
        while ((ch[0] >= '0' && ch[0] <= '9') || ch[0] == '.') {
            ch[0] = nextChar(chars, pos);
        }
        x = Double.parseDouble(new String(chars, startPos, pos[0] - startPos));
    } else {
        return 0;
    }

    if (eat(chars, pos, ch, '^')) x = Math.pow(x, parseFactor(chars, pos, ch));
    return x;
}

boolean eat(char[] chars, int[] pos, int[] ch, int charToEat) {
    while (ch[0] == ' ') ch[0] = nextChar(chars, pos);
    if (ch[0] == charToEat) {
        ch[0] = nextChar(chars, pos);
        return true;
    }
    return false;
}

int nextChar(char[] chars, int[] pos) {
    pos[0]++;
    return (pos[0] < chars.length) ? chars[pos[0]] : -1;
}

String formatAndRound(double num) {
    double roundedVal = Math.round(num * 100.0) / 100.0;
    return (roundedVal % 1 == 0) ? String.valueOf((int) roundedVal) : String.valueOf(roundedVal);
}