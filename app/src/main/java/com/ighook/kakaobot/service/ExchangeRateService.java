package com.ighook.kakaobot.service;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeRateService {
    private static final String TAG = "ExchangeRateService";
    private static final String API_KEY = "bf0a0cfe59123c3fa11dd25a"; // 발급받은 API 키를 입력하세요
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void getExchangeRate(String fromCurrency, String toCurrency, ExchangeRateCallback callback) {
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Getting exchange rate from " + fromCurrency + " to " + toCurrency);
                String urlString = BASE_URL + API_KEY + "/latest/" + fromCurrency;
                Log.d(TAG, "URL: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == 200) { // 성공적인 응답
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String inputLine;
                        StringBuilder response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }

                        Log.d(TAG, "Response: " + response.toString());

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        JSONObject conversionRates = jsonResponse.getJSONObject("conversion_rates");

                        if (conversionRates.has(toCurrency)) {
                            double rate = conversionRates.getDouble(toCurrency);
                            callback.onSuccess(rate);
                        } else {
                            throw new Exception("Invalid toCurrency code: " + toCurrency);
                        }
                    }
                } else {
                    throw new Exception("Failed to get response from ExchangeRate-API: Response code " + responseCode);
                }
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public interface ExchangeRateCallback {
        void onSuccess(double rate);
        void onFailure(Exception e);
    }
}
