package com.ighook.kakaobot.service;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

import java.io.IOException;



public class Weather extends Thread{
    String city;

    String api_key = "28955112192ebae876043c3eb30fae57";
    String base_url = "https://api.openweathermap.org/data/2.5/weather?q=";

    private final WeatherCallback callback;

    public Weather(String city, WeatherCallback callback) {
        this.city = city;
        this.callback = callback;
    }

    @Override
    public void run() {
        try{
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(base_url + city + "&units=metric&lang=kr&appid=" + api_key)
                    .build();

            Log.d("", String.valueOf(request.url()));

            try (Response response = client.newCall(request).execute()) {
                Log.d("test", String.valueOf(response));
                Gson gson = new Gson();
                WeatherResponse weatherResponse = gson.fromJson(response.body().string(), WeatherResponse.class);

                String result = "온도: " + (weatherResponse.main.temp) + "\n습도: " + weatherResponse.main.humidity + "\n기타: " + weatherResponse.weather[0].description;
                callback.onWeatherInfoReceived(result);
            } catch (IOException e) {
                callback.onError(e);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public interface WeatherCallback {
        void onWeatherInfoReceived(String info);
        void onError(Exception e);
    }

    private static class WeatherResponse {
        private Main main;
        private WeatherInfo[] weather;

        static class Main {
            private float temp;
            private float humidity;
        }

        static class WeatherInfo {
            private String description;
        }
    }
}