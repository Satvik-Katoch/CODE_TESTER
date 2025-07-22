#include <bits/stdc++.h>
using namespace std;

// ====================
// BASIC TYPEDEFS
// ====================
typedef long long ll;
typedef unsigned long long ull;
typedef long double ld;
typedef pair<int, int> pii;
typedef pair<ll, ll> pll;
typedef vector<int> vi;
typedef vector<ll> vll;
typedef vector<pii> vpii;
typedef vector<pll> vpll;
typedef vector<string> vs;
typedef vector<vi> vvi;
typedef vector<vll> vvll;
typedef map<int, int> mii;
typedef map<ll, ll> mll;
typedef set<int> si;
typedef set<ll> sll;

// ====================
// COMMONLY USED MACROS
// ====================
#define pb push_back
#define mp make_pair
#define fi first
#define se second
#define sz(x) ((int)(x).size())
#define all(x) (x).begin(), (x).end()
#define rall(x) (x).rbegin(), (x).rend()
#define rep(i, a, b) for (int i = (a); i < (b); i++)
#define per(i, a, b) for (int i = (a); i >= (b); i--)
#define trav(x, v) for (auto &x : v)

// ====================
// FAST I/O MACROS
// ====================
#define fast_io ios_base::sync_with_stdio(false); cin.tie(NULL); cout.tie(NULL)
#define endl '\n'

// ====================
// DEBUGGING MACROS
// ====================
#ifdef LOCAL
#define debug(x) cerr << #x << " = " << x << endl
#define debug2(x, y) cerr << #x << " = " << x << ", " << #y << " = " << y << endl
#define debug_arr(arr, n) cerr << #arr << " = "; rep(i, 0, n) cerr << arr[i] << " "; cerr << endl
#define debug_vec(v) cerr << #v << " = "; trav(x, v) cerr << x << " "; cerr << endl
#else
#define debug(x)
#define debug2(x, y)
#define debug_arr(arr, n)
#define debug_vec(v)
#endif

// ====================
// MATHEMATICAL CONSTANTS
// ====================
const int MOD = 1e9 + 7;
const int MOD2 = 998244353;
const ll INF = 1e18;
const int MAXN = 2e5 + 5;
const double EPS = 1e-9;
const double PI = acos(-1.0);

// ====================
// UTILITY FUNCTIONS
// ====================

// Fast power function with modular arithmetic
ll power(ll a, ll b, ll mod = MOD) {
    ll result = 1;
    a %= mod;
    while (b > 0) {
        if (b & 1) result = (result * a) % mod;
        a = (a * a) % mod;
        b >>= 1;
    }
    return result;
}

// Modular inverse using Fermat's little theorem
ll mod_inverse(ll a, ll mod = MOD) {
    return power(a, mod - 2, mod);
}

// GCD function (already available in C++14+ as __gcd or std::gcd in C++17)
ll gcd(ll a, ll b) {
    return b == 0 ? a : gcd(b, a % b);
}

// LCM function
ll lcm(ll a, ll b) {
    return (a / gcd(a, b)) * b;
}

// Check if a number is prime
bool is_prime(ll n) {
    if (n <= 1) return false;
    if (n <= 3) return true;
    if (n % 2 == 0 || n % 3 == 0) return false;
    for (ll i = 5; i * i <= n; i += 6) {
        if (n % i == 0 || n % (i + 2) == 0) return false;
    }
    return true;
}

// Binary search functions
template<typename T>
int lower_bound_index(vector<T>& arr, T val) {
    return lower_bound(all(arr), val) - arr.begin();
}

template<typename T>
int upper_bound_index(vector<T>& arr, T val) {
    return upper_bound(all(arr), val) - arr.begin();
}

// ====================
// DIRECTION ARRAYS
// ====================
const int dx[] = {-1, 1, 0, 0};           // 4-directional movement
const int dy[] = {0, 0, -1, 1};
const int dx8[] = {-1, -1, -1, 0, 0, 1, 1, 1};  // 8-directional movement
const int dy8[] = {-1, 0, 1, -1, 1, -1, 0, 1};

// ====================
// MAIN FUNCTION TEMPLATE
// ====================
void solve() {
    // Your solution code goes here
    
}

int main() {
    fast_io;
    
    int t = 1;
    // cin >> t;  // Uncomment for multiple test cases
    
    while (t--) {
        solve();
    }
    
    return 0;
}

// ====================
// ADDITIONAL UTILITY TEMPLATES
// ====================

/*
// Segment Tree Template (uncomment when needed)
class SegmentTree {
private:
    vector<ll> tree;
    int n;
    
    void build(vector<ll>& arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2*node, start, mid);
            build(arr, 2*node+1, mid+1, end);
            tree[node] = tree[2*node] + tree[2*node+1];
        }
    }
    
    void update(int node, int start, int end, int idx, ll val) {
        if (start == end) {
            tree[node] = val;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                update(2*node, start, mid, idx, val);
            } else {
                update(2*node+1, mid+1, end, idx, val);
            }
            tree[node] = tree[2*node] + tree[2*node+1];
        }
    }
    
    ll query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) return 0;
        if (l <= start && end <= r) return tree[node];
        int mid = (start + end) / 2;
        return query(2*node, start, mid, l, r) + 
               query(2*node+1, mid+1, end, l, r);
    }
    
public:
    SegmentTree(vector<ll>& arr) {
        n = arr.size();
        tree.resize(4 * n);
        build(arr, 1, 0, n-1);
    }
    
    void update(int idx, ll val) { update(1, 0, n-1, idx, val); }
    ll query(int l, int r) { return query(1, 0, n-1, l, r); }
};
*/

/*
// Union-Find (Disjoint Set Union) Template
class UnionFind {
private:
    vector<int> parent, rank;
    
public:
    UnionFind(int n) {
        parent.resize(n);
        rank.resize(n, 0);
        rep(i, 0, n) parent[i] = i;
    }
    
    int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }
    
    bool unite(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        if (rank[px] < rank[py]) swap(px, py);
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }
    
    bool same(int x, int y) { return find(x) == find(y); }
};
*/
