package com.ighook.kakaobot.service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LostArkService {
    private static final String TAG = "LostArkService";
    private static final String API_URL = "https://developer-lostark.game.onstove.com/characters";
    private static final String API_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IktYMk40TkRDSTJ5NTA5NWpjTWk5TllqY2lyZyIsImtpZCI6IktYMk40TkRDSTJ5NTA5NWpjTWk5TllqY2lyZyJ9.eyJpc3MiOiJodHRwczovL2x1ZHkuZ2FtZS5vbnN0b3ZlLmNvbSIsImF1ZCI6Imh0dHBzOi8vbHVkeS5nYW1lLm9uc3RvdmUuY29tL3Jlc291cmNlcyIsImNsaWVudF9pZCI6IjEwMDAwMDAwMDAzOTYxOTQifQ.Db6r_tbiMvCB8qiHqs0Lff7nvrzaiQZtQHVXmmkn7zBat_vm9gk38BlO4UvtGV3xL894dbixmeMw7jmU9T7SkWNY4GTVmUgsINVCGdzoNUt-E5lK-dUFq7PPosFDr-XZPFJpISyvJoX4h0VyGAS-cd-rAnGWcLisrcmSjoyIRivUlwB6ygOvJiaFn4CL_PgxPghUVPWLkZU4PfYWbYCOEKO4YewaddtRbqO9LsuYC8AUs1U36Ibj6e9j0yajeKx9DTBrihyjY9K9888k85mEGOduAKb1i1P8mMscPbLn_ydY_V9mg48dy5jBNlAQQS8jUpuJgaTEiqqFcBriPQeeNw";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void getCharacterList(String characterName, LostArkCallback callback) {
        executorService.submit(() -> {
            try {
                String encodedCharName = URLEncoder.encode(characterName, "UTF-8");
                String urlString = API_URL + "/" + encodedCharName + "/siblings";

                URL url = new URL(urlString);
                Log.d(TAG, "URL: " + urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // JSON 데이터 파싱
                    JSONArray jsonArray = new JSONArray(response.toString());
                    StringBuilder result = new StringBuilder();
                    Log.d(TAG, "Response: " + response.toString());

                    List<JSONObject> characterList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        characterList.add(jsonArray.getJSONObject(i));
                    }

                    // itemAvgLevel 기준으로 내림차순 정렬
                    Collections.sort(characterList, new Comparator<JSONObject>() {
                        public int compare(JSONObject a, JSONObject b) {
                            try {
                                String valA = a.getString("ItemAvgLevel").replace(",", "");
                                String valB = b.getString("ItemAvgLevel").replace(",", "");
                                return Double.compare(Double.parseDouble(valB), Double.parseDouble(valA));
                            } catch (Exception e) {
                                return 0;
                            }
                        }
                    });

                    // 상위 6개 캐릭터 정보 추출 및 포맷팅
                    int count = Math.min(characterList.size(), 6); // 리스트 크기가 6보다 작을 경우를 대비
                    for (int i = 0; i < count; i++) {
                        JSONObject character = characterList.get(i);
                        String itemAvgLevel = character.getString("ItemAvgLevel");
                        String characterClassName = character.getString("CharacterClassName");
                        String charName = character.getString("CharacterName");

                        result.append(itemAvgLevel).append(" / ").append(characterClassName).append(" / ").append(charName).append("\n");
                    }
                    result.append("6개의 캐릭터만 표시합니다.");
                    callback.onSuccess(result.toString());
                } else if(responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    String newUrl = conn.getHeaderField("Location");  // 리다이렉트 URL 읽기
                    Log.d(TAG, "Redirected to: " + newUrl);
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();  // 새 URL로 연결
                    conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestMethod("GET");
                    responseCode = conn.getResponseCode();
                }
            } catch (Exception e) {
                callback.onFailure(e);
            }
        });

    }

    public interface LostArkCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}
