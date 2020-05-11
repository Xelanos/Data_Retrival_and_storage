package webdata.dictionary;

import java.io.Serializable;
import java.util.HashMap;

class TrieNode<T> implements Serializable {

    boolean isWord;
    T data;
    private HashMap<Character, TrieNode<T>> sons;


    public TrieNode() {
        this.sons = new HashMap<>();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isWord() {
        return isWord;
    }

    public void setIsWord(boolean word) {
        isWord = word;
    }


    public HashMap<Character, TrieNode<T>> getSons() {
        return sons;
    }

    public void setSons(HashMap<Character, TrieNode<T>> sons) {
        this.sons = sons;
    }
}
