import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// =========================================================================
// MINI-TESTLIB (Self-contained, no external files needed)
// Mimics the most useful features of the official testlib.h
// =========================================================================
public class Generator {
    static class RandomGenerator {
        private final Random rng;

        public RandomGenerator(String[] args) {
            long seed = 3982541L; // Default random seed
            if (args.length > 0) {
                try {
                    // Use the iteration number passed by the GUI as the seed
                    // This ensures Test Case #5 is always the same!
                    seed = Long.parseLong(args[0]);
                } catch (NumberFormatException ignored) {}
            }
            rng = new Random(seed);
        }

        // Returns integer in [0, n-1]
        public long next(long n) {
            return (Math.abs(rng.nextLong()) % n);
        }

        // Returns integer in [l, r] inclusive
        public long next(long l, long r) {
            return l + next(r - l + 1);
        }
        
        // Returns integer in [0, n-1]
        public int nextInt(int n) {
            return rng.nextInt(n);
        }

        // Returns integer in [l, r] inclusive
        public int nextInt(int l, int r) {
            return l + rng.nextInt(r - l + 1);
        }

        // Returns string of given length with chars from pattern
        public String nextString(int length, String pattern) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; ++i) {
                sb.append(pattern.charAt(nextInt(pattern.length())));
            }
            return sb.toString();
        }

        public String nextString(int length) {
            return nextString(length, "abcdefghijklmnopqrstuvwxyz");
        }

        // Returns an array of size n with values in [l, r]
        public long[] nextArray(int n, long l, long r) {
            long[] v = new long[n];
            for (int i = 0; i < n; ++i) v[i] = next(l, r);
            return v;
        }

        // Returns a random permutation of 0..n-1
        public int[] nextPerm(int n) {
            List<Integer> p = new ArrayList<>();
            for (int i = 0; i < n; i++) p.add(i);
            Collections.shuffle(p, rng);
            int[] res = new int[n];
            for (int i = 0; i < n; i++) res[i] = p.get(i);
            return res;
        }

        // Weighted Next: Good for stress testing edge cases!
        // w > 0: Result skewed towards max (returns max of w+1 trials)
        // w < 0: Result skewed towards min (returns min of |w|+1 trials)
        // w = 0: Uniform random
        public long wnext(long n, int w) {
            if (w == 0) return next(n);
            long res = next(n);
            for (int i = 0; i < Math.abs(w); ++i) {
                long tmp = next(n);
                if (w > 0) res = Math.max(res, tmp);
                else res = Math.min(res, tmp);
            }
            return res;
        }

        // Helper to print an array cleanly
        public void println(long[] v) {
            for (int i = 0; i < v.length; ++i) {
                System.out.print(v[i] + (i == v.length - 1 ? "" : " "));
            }
            System.out.println();
        }
        
        public void println(int[] v) {
            for (int i = 0; i < v.length; ++i) {
                System.out.print(v[i] + (i == v.length - 1 ? "" : " "));
            }
            System.out.println();
        }
    }

    // =========================================================================
    // MAIN GENERATOR LOGIC
    // =========================================================================
    public static void main(String[] args) {
        // 1. Initialize Generator with command line args
        RandomGenerator rnd = new RandomGenerator(args);

        // ---------------------------------------------------------
        // WRITE YOUR GENERATOR LOGIC HERE
        // ---------------------------------------------------------

        // Example: Generate N (1 to 10)
        int n = rnd.nextInt(1, 10);
        System.out.println(n);

        // Example: Generate Array of size N with values between -100 and 100
        // Using wnext to skew numbers towards the boundaries (more likely to find bugs!)
        long[] arr = new long[n];
        for (int i = 0; i < n; ++i) {
            // Randomly decide to pick a normal number or a skewed number
            if (rnd.nextInt(0, 1) == 0) {
                arr[i] = rnd.next(1, 1000000);
            } else {
                // Skew towards max (100)
                arr[i] = rnd.wnext(201, 10) - 100;
            }
        }

        rnd.println(arr);
    }
}
