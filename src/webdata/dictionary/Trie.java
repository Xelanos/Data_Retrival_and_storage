package webdata.dictionary;

import java.io.Serializable;

public class Trie<T> implements  Serializable {

    private TrieNode<T> root;
    private int size = 0;

    public Trie() {
        this.root = new TrieNode<>();
    }

    public void add(String token, T data){
        TrieNode<T> current = root;
        for (int i = 0; i < token.length(); i++) {
            current = current.getSons()
                    .computeIfAbsent(token.charAt(i), son -> new TrieNode<>());
        }
        current.setIsWord(true);
        current.setData(data);
        size++;
    }

    public TrieNode<T> find(String word) {
        TrieNode<T> current = root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode<T> node = current.getSons().get(ch);
            if (node == null) {
                return node;
            }
            current = node;
        }
        return current.isWord()? current : null;
    }

    }
