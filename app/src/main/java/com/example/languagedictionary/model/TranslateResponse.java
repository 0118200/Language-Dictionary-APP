package com.example.languagedictionary.model;

public class TranslateResponse {
    private ResponseData responseData;

    public ResponseData getResponseData() {
        return responseData;
    }

    public static class ResponseData {
        private String translatedText;

        public String getTranslatedText() {
            return translatedText;
        }
    }
}