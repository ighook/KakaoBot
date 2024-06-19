package com.ighook.kakaobot.service;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GPTService extends Thread {

    private static final String API_KEY = "sk-proj-R90Dc43iWZihdDDXKSz7T3BlbkFJkhOran9KsAxfD5JuYfrW";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static GPTService instance;
    private final OkHttpClient client;

    private GPTService() {
        this.client = new OkHttpClient();
    }

    public static GPTService getInstance() {
        if (instance == null) {
            instance = new GPTService();
        }
        return instance;
    }

    public void getReply(String content, GPTCallback callback) {
        new GetReplyTask(callback).execute(content);
    }

    public interface GPTCallback {
        void onReplyReceived(String reply);

        void onGPTResponse(String response);
        void onError(Exception e);
    }

    private static class GetReplyTask extends AsyncTask<String, Void, String> {
        private final GPTCallback callback;
        private Exception error;

        GetReplyTask(GPTCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            String content = params[0];
            JSONObject json = new JSONObject();
            try {
                json.put("model", "gpt-4");  // 사용할 모델 지정
                JSONArray messages = new JSONArray();
                JSONObject messageObject = new JSONObject();
                messageObject.put("role", "user");
                messageObject.put("content", content);
                messages.put(messageObject);
                json.put("messages", messages);
            } catch (JSONException e) {
                error = e;
                return null;
            }

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Log.d("afdsafsdafsafs", body.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            try (Response response = new OkHttpClient().newCall(request).execute()) {
                Log.d("response1: ", response.toString());
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                JSONObject responseBody = new JSONObject(response.body().string());
                JSONArray choices = responseBody.getJSONArray("choices");
                if (choices.length() > 0) {
                    return choices.getJSONObject(0).getJSONObject("message").getString("content");
                } else {
                    return "No reply from GPT.";
                }
            } catch (IOException | JSONException e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onGPTResponse(result);
            }
        }
    }
}
