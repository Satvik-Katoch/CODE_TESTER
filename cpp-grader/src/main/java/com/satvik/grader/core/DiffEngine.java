package com.satvik.grader.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Line diff producing {@code difflib.SequenceMatcher.get_opcodes()}-style op-codes, implemented with
 * the Myers O((N+M)D) algorithm (after trimming the common prefix/suffix).
 */
public final class DiffEngine {

    private DiffEngine() {
    }

    public enum Tag { EQUAL, DELETE, INSERT, REPLACE }

    /** Half-open ranges a[i1, i2) and b[j1, j2), exactly like difflib. */
    public record OpCode(Tag tag, int i1, int i2, int j1, int j2) {
    }

    /** Above this edit distance we stop searching and report the middle block as one replacement. */
    private static final int MAX_EDIT_DISTANCE = 3000;

    private static final int OP_EQUAL = 0;
    private static final int OP_DELETE = 1;
    private static final int OP_INSERT = 2;

    public static List<OpCode> opcodes(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();

        // Map lines to ints (trailing whitespace is ignored for matching, like the verdict).
        Map<String, Integer> ids = new HashMap<>();
        int[] x = new int[n];
        int[] y = new int[m];
        for (int i = 0; i < n; i++) {
            x[i] = ids.computeIfAbsent(a.get(i).stripTrailing(), k -> ids.size());
        }
        for (int j = 0; j < m; j++) {
            y[j] = ids.computeIfAbsent(b.get(j).stripTrailing(), k -> ids.size());
        }

        int prefix = 0;
        while (prefix < n && prefix < m && x[prefix] == y[prefix]) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < n - prefix && suffix < m - prefix && x[n - 1 - suffix] == y[m - 1 - suffix]) {
            suffix++;
        }

        List<OpCode> result = new ArrayList<>();
        if (prefix > 0) {
            result.add(new OpCode(Tag.EQUAL, 0, prefix, 0, prefix));
        }

        int aLen = n - prefix - suffix;
        int bLen = m - prefix - suffix;
        if (aLen > 0 || bLen > 0) {
            int[] ops = myers(x, prefix, aLen, y, prefix, bLen);
            if (ops == null) {
                result.add(change(prefix, prefix + aLen, prefix, prefix + bLen));
            } else {
                appendOpcodes(result, ops, prefix, prefix);
            }
        }

        if (suffix > 0) {
            result.add(new OpCode(Tag.EQUAL, n - suffix, n, m - suffix, m));
        }
        return result;
    }

    private static OpCode change(int i1, int i2, int j1, int j2) {
        Tag t = (i1 == i2) ? Tag.INSERT : (j1 == j2) ? Tag.DELETE : Tag.REPLACE;
        return new OpCode(t, i1, i2, j1, j2);
    }

    private static void appendOpcodes(List<OpCode> out, int[] ops, int aOff, int bOff) {
        int i = aOff;
        int j = bOff;
        int k = 0;
        while (k < ops.length) {
            if (ops[k] == OP_EQUAL) {
                int si = i;
                int sj = j;
                while (k < ops.length && ops[k] == OP_EQUAL) {
                    i++;
                    j++;
                    k++;
                }
                out.add(new OpCode(Tag.EQUAL, si, i, sj, j));
            } else {
                int si = i;
                int sj = j;
                while (k < ops.length && ops[k] != OP_EQUAL) {
                    if (ops[k] == OP_DELETE) {
                        i++;
                    } else {
                        j++;
                    }
                    k++;
                }
                out.add(change(si, i, sj, j));
            }
        }
    }

    /** Returns the edit script (EQUAL/DELETE/INSERT per step) or null if it is too expensive. */
    private static int[] myers(int[] a, int aOff, int n, int[] b, int bOff, int m) {
        int max = n + m;
        int off = max + 1;
        int[] v = new int[2 * max + 3];
        List<int[]> trace = new ArrayList<>();
        int found = -1;

        outer:
        for (int d = 0; d <= max; d++) {
            if (d > MAX_EDIT_DISTANCE) {
                return null;
            }
            // snapshot of v for k in [-d-1, d+1]
            int lo = -d - 1;
            int[] snap = new int[2 * d + 3];
            System.arraycopy(v, lo + off, snap, 0, snap.length);
            trace.add(snap);

            for (int k = -d; k <= d; k += 2) {
                int xx;
                if (k == -d || (k != d && v[k - 1 + off] < v[k + 1 + off])) {
                    xx = v[k + 1 + off];
                } else {
                    xx = v[k - 1 + off] + 1;
                }
                int yy = xx - k;
                while (xx < n && yy < m && a[aOff + xx] == b[bOff + yy]) {
                    xx++;
                    yy++;
                }
                v[k + off] = xx;
                if (xx >= n && yy >= m) {
                    found = d;
                    break outer;
                }
            }
        }
        if (found < 0) {
            return null;
        }

        // Backtrack
        int[] rev = new int[n + m];
        int len = 0;
        int xx = n;
        int yy = m;
        for (int d = found; d > 0; d--) {
            int[] snap = trace.get(d);
            int k = xx - yy;
            int prevK;
            if (k == -d || (k != d && at(snap, d, k - 1) < at(snap, d, k + 1))) {
                prevK = k + 1;
            } else {
                prevK = k - 1;
            }
            int prevX = at(snap, d, prevK);
            int prevY = prevX - prevK;
            while (xx > prevX && yy > prevY) {
                rev[len++] = OP_EQUAL;
                xx--;
                yy--;
            }
            rev[len++] = (xx == prevX) ? OP_INSERT : OP_DELETE;
            xx = prevX;
            yy = prevY;
        }
        while (xx > 0 && yy > 0) {
            rev[len++] = OP_EQUAL;
            xx--;
            yy--;
        }
        int[] ops = new int[len];
        for (int i = 0; i < len; i++) {
            ops[i] = rev[len - 1 - i];
        }
        return ops;
    }

    private static int at(int[] snap, int d, int k) {
        return snap[k + d + 1];
    }
}
