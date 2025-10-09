package com.example.languagedictionary.api;

import com.example.languagedictionary.model.Word;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
public interface DictionaryApi {
    @GET("en/{word}")
    Call<List<Word>> getWordDefinitions(@Path("word") String word);
}