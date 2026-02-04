#include <iostream>
#include <unordered_map>
#include <string>

using namespace std;

struct TrieNode {
    // Maps a character to the next node
    unordered_map<char, TrieNode*> children;
    bool isEndOfWord;

    TrieNode() : isEndOfWord(false) {}
};

class Trie {
private:
    TrieNode* root;

    // Helper for recursive deletion
    bool deleteHelper(TrieNode* curr, const string& word, int depth) {
        if (!curr) return false;

        // Base case: reached the end of the word
        if (depth == word.length()) {
            if (!curr->isEndOfWord) return false; // Word doesn't exist
            
            curr->isEndOfWord = false; // Unmark end of word
            
            // If node has no other children, it can be deleted
            return curr->children.empty();
        }

        char c = word[depth];
        if (curr->children.find(c) == curr->children.end()) return false;

        bool shouldDeleteChild = deleteHelper(curr->children[c], word, depth + 1);

        if (shouldDeleteChild) {
            delete curr->children[c];
            curr->children.erase(c);
            
            // Return true if current node is now empty and not another word's end
            return curr->children.empty() && !curr->isEndOfWord;
        }

        return false;
    }

public:
    Trie() {
        root = new TrieNode();
    }

    void insert(string word) {
        TrieNode* curr = root;
        for (char c : word) {
            if (curr->children.find(c) == curr->children.end()) {
                curr->children[c] = new TrieNode();
            }
            curr = curr->children[c];
        }
        curr->isEndOfWord = true;
    }

    bool search(string word) {
        TrieNode* curr = root;
        for (char c : word) {
            if (curr->children.find(c) == curr->children.end()) return false;
            curr = curr->children[c];
        }
        return curr->isEndOfWord;
    }

    void remove(string word) {
        deleteHelper(root, word, 0);
    }
};