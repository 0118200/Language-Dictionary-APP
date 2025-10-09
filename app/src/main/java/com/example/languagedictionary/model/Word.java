package com.example.languagedictionary.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class Word {
    @SerializedName("word")
    private String word;

    @SerializedName("phonetic")
    private String phonetic;

    @SerializedName("meanings")
    private List<Meaning> meanings;

    public String getWord() { return word; }
    public String getPhonetic() { return phonetic; }
    public List<Meaning> getMeanings() { return meanings; }
}