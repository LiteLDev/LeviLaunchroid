package org.levimc.pojavcontrols;

import java.util.Map;

final class ExpressionEvaluator {
    private final String expression;
    private int position;

    private ExpressionEvaluator(String expression) {
        this.expression = expression;
    }

    static float evaluate(String source, Map<String, Float> variables, float fallback) {
        if (source == null) return fallback;
        String expanded = source;
        for (Map.Entry<String, Float> entry : variables.entrySet()) {
            expanded = expanded.replace("${" + entry.getKey() + "}", Float.toString(entry.getValue()));
        }
        try {
            ExpressionEvaluator evaluator = new ExpressionEvaluator(expanded);
            double value = evaluator.parseExpression();
            evaluator.skipSpaces();
            if (evaluator.position != evaluator.expression.length() || !Double.isFinite(value)) return fallback;
            return (float) value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            if (take('+')) value += parseTerm();
            else if (take('-')) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            skipSpaces();
            if (take('*')) value *= parseFactor();
            else if (take('/')) value /= parseFactor();
            else return value;
        }
    }

    private double parseFactor() {
        skipSpaces();
        if (take('+')) return parseFactor();
        if (take('-')) return -parseFactor();
        if (take('(')) {
            double value = parseExpression();
            if (!take(')')) throw new IllegalArgumentException();
            return value;
        }
        int start = position;
        while (position < expression.length()) {
            char c = expression.charAt(position);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                if ((c == '+' || c == '-') && position > start && expression.charAt(position - 1) != 'e'
                        && expression.charAt(position - 1) != 'E') break;
                position++;
            } else break;
        }
        if (start == position) throw new IllegalArgumentException();
        return Double.parseDouble(expression.substring(start, position));
    }

    private void skipSpaces() {
        while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) position++;
    }

    private boolean take(char expected) {
        skipSpaces();
        if (position < expression.length() && expression.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }
}
