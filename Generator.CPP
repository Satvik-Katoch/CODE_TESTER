#include <iostream>
#include <vector>
#include <random>
#include <algorithm>
#include <string>
#include <set>
#include <numeric>

using namespace std;

// =========================================================================
// MINI-TESTLIB (Self-contained, no external files needed)
// Mimics the most useful features of the official testlib.h
// =========================================================================
struct RandomGenerator {
    mt19937_64 rng;

    RandomGenerator(int argc, char* argv[]) {
        unsigned int seed = 3982541; // Default random seed
        if (argc > 1) {
            // Use the iteration number passed by the GUI as the seed
            // This ensures Test Case #5 is always the same!
            seed = (unsigned int)stoi(argv[1]);
        }
        rng.seed(seed);
    }

    // Returns integer in [0, n-1]
    long long next(long long n) {
        return uniform_int_distribution<long long>(0, n - 1)(rng);
    }

    // Returns integer in [l, r] inclusive
    long long next(long long l, long long r) {
        return uniform_int_distribution<long long>(l, r)(rng);
    }
    
    // Returns string of given length with chars from pattern
    string next_string(int length, string pattern = "abcdefghijklmnopqrstuvwxyz") {
        string res = "";
        for(int i=0; i<length; ++i) res += pattern[next(pattern.size())];
        return res;
    }

    // Returns a vector of size n with values in [l, r]
    vector<long long> next_array(int n, long long l, long long r) {
        vector<long long> v(n);
        for(int i=0; i<n; ++i) v[i] = next(l, r);
        return v;
    }
    
    // Returns a random permutation of 0..n-1
    vector<int> next_perm(int n) {
        vector<int> p(n);
        iota(p.begin(), p.end(), 0);
        shuffle(p.begin(), p.end(), rng);
        return p;
    }

    // Weighted Next: Good for stress testing edge cases!
    // w > 0: Result skewed towards max (returns max of w+1 trials)
    // w < 0: Result skewed towards min (returns min of |w|+1 trials)
    // w = 0: Uniform random
    long long wnext(long long n, int w) {
        if (w == 0) return next(n);
        long long res = next(n);
        for (int i = 0; i < abs(w); ++i) {
            long long tmp = next(n);
            if (w > 0) res = max(res, tmp);
            else res = min(res, tmp);
        }
        return res;
    }

    // Helper to print a vector cleanly
    template<typename T>
    void println(const vector<T>& v) {
        for (size_t i = 0; i < v.size(); ++i) {
            cout << v[i] << (i == v.size() - 1 ? "" : " ");
        }
        cout << "\n";
    }
};

// =========================================================================
// MAIN GENERATOR LOGIC
// =========================================================================

int main(int argc, char* argv[]) {
    // 1. Initialize Generator with command line args
    RandomGenerator rnd(argc, argv);

    // ---------------------------------------------------------
    // WRITE YOUR GENERATOR LOGIC HERE
    // ---------------------------------------------------------

    // Example: Generate N (1 to 10)
    int n = rnd.next(1, 10); 
    cout << n << "\n";

    // Example: Generate Array of size N with values between -100 and 100
    // Using wnext to skew numbers towards the boundaries (more likely to find bugs!)
    vector<long long> arr(n);
    for(int i=0; i<n; ++i) {
        // Randomly decide to pick a normal number or a skewed number
        if (rnd.next(0, 1) == 0) {
            arr[i] = rnd.next(-100, 100); 
        } else {
            // Skew towards max (100)
            arr[i] = rnd.wnext(201, 10) - 100; 
        }
    }

    rnd.println(arr);

    return 0;
}