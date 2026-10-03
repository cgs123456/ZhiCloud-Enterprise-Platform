package cn.zhicloud.server;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向 clean baseline 的保守 SQL 脚本切分器。
 *
 * <p>JDBC {@link java.sql.Statement#execute(String)} 一次只接受一条语句。这里不追求覆盖所有数据库方言，
 * 只覆盖 MySQL baseline 中出现的语法，并对未闭合字符串/注释 fail-closed，避免把半个脚本交给数据库执行。</p>
 */
final class SqlScriptSplitter {

    private SqlScriptSplitter() {
    }

    static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null || script.isEmpty()) {
            return statements;
        }

        StringBuilder current = new StringBuilder();
        State state = State.NORMAL;
        boolean hasSqlToken = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            switch (state) {
                case NORMAL -> {
                    if (ch == '\'' || ch == '"' || ch == '`') {
                        state = stateForQuote(ch);
                        hasSqlToken = true;
                        current.append(ch);
                    } else if (ch == '-' && next == '-') {
                        state = State.LINE_COMMENT;
                        current.append(ch).append(next);
                        i++;
                    } else if (ch == '#') {
                        state = State.LINE_COMMENT;
                        current.append(ch);
                    } else if (ch == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        current.append(ch).append(next);
                        i++;
                    } else if (ch == ';') {
                        addStatement(statements, current, hasSqlToken);
                        current.setLength(0);
                        hasSqlToken = false;
                    } else {
                        if (!Character.isWhitespace(ch)) {
                            hasSqlToken = true;
                        }
                        current.append(ch);
                    }
                }
                case SINGLE_QUOTE, DOUBLE_QUOTE, BACKTICK -> {
                    current.append(ch);
                    char quote = state.quote();
                    if (ch == '\\' && i + 1 < script.length()) {
                        current.append(next);
                        i++;
                    } else if (ch == quote) {
                        if (next == quote) {
                            current.append(next);
                            i++;
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }
                case LINE_COMMENT -> {
                    current.append(ch);
                    if (ch == '\n' || ch == '\r') {
                        state = State.NORMAL;
                    }
                }
                case BLOCK_COMMENT -> {
                    current.append(ch);
                    if (ch == '*' && next == '/') {
                        current.append(next);
                        i++;
                        state = State.NORMAL;
                    }
                }
            }
        }

        if (state == State.SINGLE_QUOTE || state == State.DOUBLE_QUOTE
                || state == State.BACKTICK || state == State.BLOCK_COMMENT) {
            throw new IllegalArgumentException("SQL 脚本存在未闭合的字符串或块注释");
        }
        addStatement(statements, current, hasSqlToken);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current, boolean hasSqlToken) {
        if (hasSqlToken) {
            String statement = current.toString().strip();
            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }
    }

    private static State stateForQuote(char quote) {
        return switch (quote) {
            case '\'' -> State.SINGLE_QUOTE;
            case '"' -> State.DOUBLE_QUOTE;
            case '`' -> State.BACKTICK;
            default -> throw new IllegalArgumentException("不支持的引号: " + quote);
        };
    }

    private enum State {
        NORMAL('\0'),
        SINGLE_QUOTE('\''),
        DOUBLE_QUOTE('"'),
        BACKTICK('`'),
        LINE_COMMENT('\0'),
        BLOCK_COMMENT('\0');

        private final char quote;

        State(char quote) {
            this.quote = quote;
        }

        char quote() {
            return quote;
        }
    }
}
