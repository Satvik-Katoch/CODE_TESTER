package com.satvik.grader.core;

import java.util.ArrayList;
import java.util.List;

/** Splits a flags string like {@code -O2 -DNAME="a b" '-Wl,--stack=1'} into arguments, honouring quotes. */
public final class ArgTokenizer {

    private ArgTokenizer() {
    }

    public static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        StringBuilder cur = new StringBuilder();
        boolean inToken = false;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    cur.append(c);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
                inToken = true;
            } else if (Character.isWhitespace(c)) {
                if (inToken) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    inToken = false;
                }
            } else {
                cur.append(c);
                inToken = true;
            }
        }
        if (inToken) {
            out.add(cur.toString());
        }
        return out;
    }
}
