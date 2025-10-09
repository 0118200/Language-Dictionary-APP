package com.example.languagedictionary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.languagedictionary.api.DictionaryApi;
import com.example.languagedictionary.api.TranslateApi;
import com.example.languagedictionary.model.Definition;
import com.example.languagedictionary.model.Meaning;
import com.example.languagedictionary.model.TranslateResponse;
import com.example.languagedictionary.model.Word;

import java.util.Locale;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText etSearchWord;
    private Button btnSearch, btnSpeak;
    private TextView tvWord, tvError;
    private ProgressBar pbLoading;
    private Spinner spinnerLanguage;

    private String selectedLanguage = "Bahasa Inggris → Indonesia";
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinner();
        btnSearch.setOnClickListener(v -> handleSearch());
        btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(v -> ucapkanKata(etSearchWord.getText().toString().trim()));
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //default bhs inggris
                    int result = textToSpeech.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "Bahasa tidak didukung", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Gagal inisialisasi TTS", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void initViews() {
        etSearchWord = findViewById(R.id.etSearchWord);
        btnSearch = findViewById(R.id.btnSearch);
        tvWord = findViewById(R.id.tvWord);
        tvError = findViewById(R.id.tvError);
        pbLoading = findViewById(R.id.pbLoading);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void handleSearch() {
        String word = etSearchWord.getText().toString().trim();
        if (word.isEmpty()) {
            Toast.makeText(this, "Masukkan kata terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSpeak.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        hideAllResults();
        tvError.setVisibility(View.GONE);

        if (selectedLanguage.equals("Bahasa Inggris → Indonesia")) {
            cariDanTerjemahkanLengkap(word);
        } else if (selectedLanguage.equals("Bahasa Indonesia → Inggris")) {
            terjemahkanKataLaluCariDefinisi(word);
        }
    }

    private void cariDanTerjemahkanLengkap(String word) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.dictionaryapi.dev/api/v2/entries/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DictionaryApi api = retrofit.create(DictionaryApi.class);
        Call<List<Word>> call = api.getWordDefinitions(word);

        call.enqueue(new Callback<List<Word>>() {
            @Override
            public void onResponse(Call<List<Word>> call, Response<List<Word>> response) {
                pbLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Word wordObj = response.body().get(0);
                    StringBuilder result = new StringBuilder();

                    if (wordObj.getPhonetic() != null && !wordObj.getPhonetic().isEmpty()) {
                        ucapkanKata(wordObj.getWord());
                    }

                    result.append("🔤 Kata: ").append(wordObj.getWord()).append("\n\n");

                    if (wordObj.getPhonetic() != null && !wordObj.getPhonetic().trim().isEmpty()) {
                        terjemahkanDanTambah("🔊 Pelafalan: " + wordObj.getPhonetic(), result, "\n\n");
                    }

                    for (Meaning meaning : wordObj.getMeanings()) {
                        if (meaning.getPartOfSpeech() != null) {
                            terjemahkanDanTambah("📌 Jenis Kata: " + meaning.getPartOfSpeech(), result, "\n\n");
                        }

                        for (Definition def : meaning.getDefinitions()) {
                            terjemahkanDanTambah(" • Definisi: " + def.getDefinition(), result, "\n");

                            if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                terjemahkanDanTambah("   → Contoh: " + def.getExample(), result, "\n");
                            }

                            if (def.getSynonyms() != null && !def.getSynonyms().isEmpty()) {
                                String synList = String.join(", ", def.getSynonyms());
                                terjemahkanDanTambah("   💡 Sinonim: " + synList, result, "\n");
                            }

                            if (def.getAntonyms() != null && !def.getAntonyms().isEmpty()) {
                                String antList = String.join(", ", def.getAntonyms());
                                terjemahkanDanTambah("   ❌ Antonim: " + antList, result, "\n");
                            }

                            result.append("\n");
                        }
                    }

                    mainHandler.postDelayed(() -> {
                        tampilkanHasil(word, result.toString());
                    }, 3500);

                } else {
                    String pesan = "🔍 Kata \"" + word + "\" tidak ditemukan di kamus.";
                    tampilkanHasil(word, pesan);
                }
            }

            @Override
            public void onFailure(Call<List<Word>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                String pesan = "⚠️ Gagal terhubung ke server.";
                tampilkanHasil(word, pesan);
            }
        });
    }

    private void terjemahkanDanTambah(String text, StringBuilder sb, String suffix) {
        terjemahkanTeks(text, "en|id", translated -> {
            mainHandler.post(() -> {
                if (translated == null || translated.trim().isEmpty() ||
                        translated.contains("Gagal") || translated.equals(text)) {
                    sb.append(text).append(suffix);
                } else {
                    sb.append(translated).append(suffix);
                }
            });
        });
    }

    private void ucapkanKata(String word) {
        if (word.isEmpty()) return;

        if (textToSpeech != null) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    private void terjemahkanKataLaluCariDefinisi(String word) {
        terjemahkanTeks(word, "id|en", kataInggris -> {
            if (kataInggris == null || kataInggris.contains("Gagal") || kataInggris.trim().isEmpty()) {
                pbLoading.setVisibility(View.GONE);
                showError("Tidak bisa menerjemahkan kata ini.");
                return;
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.dictionaryapi.dev/api/v2/entries/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            DictionaryApi api = retrofit.create(DictionaryApi.class);
            Call<List<Word>> call = api.getWordDefinitions(kataInggris);

            call.enqueue(new Callback<List<Word>>() {
                @Override
                public void onResponse(Call<List<Word>> call, Response<List<Word>> response) {
                    pbLoading.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Word wordObj = response.body().get(0);
                        StringBuilder result = new StringBuilder();

                        result.append("🔤 Kata: ").append(word).append(" (").append(kataInggris).append(")\n\n");

                        if (wordObj.getPhonetic() != null) {
                            result.append("🔊 Pelafalan: ").append(wordObj.getPhonetic()).append("\n\n");
                        }

                        for (Meaning meaning : wordObj.getMeanings()) {
                            result.append("📌 Jenis Kata: ").append(meaning.getPartOfSpeech()).append("\n\n");
                            for (Definition def : meaning.getDefinitions()) {
                                result.append(" • Definisi: ").append(def.getDefinition()).append("\n");
                                if (def.getExample() != null) {
                                    result.append("   → Contoh: ").append(def.getExample()).append("\n");
                                }
                                if (def.getSynonyms() != null && !def.getSynonyms().isEmpty()) {
                                    result.append("   💡 Sinonim: ").append(String.join(", ", def.getSynonyms())).append("\n");
                                }
                                if (def.getAntonyms() != null && !def.getAntonyms().isEmpty()) {
                                    result.append("   ❌ Antonim: ").append(String.join(", ", def.getAntonyms())).append("\n");
                                }
                                result.append("\n");
                            }
                            result.append("────────────────\n\n");
                        }

                        tampilkanHasil(word, result.toString());

                    } else {
                        showError("Kata \"" + kataInggris + "\" tidak ditemukan di kamus Inggris.");
                    }
                }

                @Override
                public void onFailure(Call<List<Word>> call, Throwable t) {
                    pbLoading.setVisibility(View.GONE);
                    showError("Gagal terhubung ke server: " + t.getMessage());
                }
            });
        });
    }

    private void terjemahkanTeks(String text, String langPair, TranslationCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.mymemory.translated.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TranslateApi api = retrofit.create(TranslateApi.class);
        Call<TranslateResponse> call = api.translate(text, langPair);

        call.enqueue(new Callback<TranslateResponse>() {
            @Override
            public void onResponse(Call<TranslateResponse> call, Response<TranslateResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getResponseData() != null) {
                    String translated = response.body().getResponseData().getTranslatedText();
                    callback.onSuccess(translated);
                } else {
                    callback.onSuccess(text + " (terjemahan gagal)");
                }
            }

            @Override
            public void onFailure(Call<TranslateResponse> call, Throwable t) {
                callback.onSuccess(text + " (error jaringan)");
            }
        });
    }

    private void tampilkanHasil(String word, String hasil) {
        tvWord.setText(hasil);
        tvWord.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideAllResults() {
        tvWord.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    interface TranslationCallback {
        void onSuccess(String translatedText);
    }
}