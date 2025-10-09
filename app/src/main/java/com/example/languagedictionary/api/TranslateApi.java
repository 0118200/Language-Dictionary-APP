package com.example.languagedictionary.api;

import com.example.languagedictionary.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TranslateApi {
    @GET("get")
    Call<TranslateResponse> translate(
            @Query("q") String text,
            @Query("langpair") String langPair
    );
}