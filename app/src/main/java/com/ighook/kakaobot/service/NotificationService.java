package com.ighook.kakaobot.service;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ighook.kakaobot.model.Quotes;

import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class NotificationService extends NotificationListenerService {

    private static final String TAG = "푸시 알림";
    private static final String REPLY_KEY = "reply_message";
    private static final String[] RESPONSES = {"싫어 떼 쓸거야 빨리 고쳐줘", "목줄까진 몰라도 목조르기는 좀...", "뭘 봐 팍씨 참치캔 내놔", "내가 목줄한거 봤어요?", "내가 나서면 그건 싸움이 아닌 학살이 되니까...", "ㄹㅇ 허접이네", "어정쩡한 허접이군요"};

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        android.app.Notification notification = sbn.getNotification();
        Bundle extras = sbn.getNotification().extras;

        String sender = extras.getString(android.app.Notification.EXTRA_TITLE);
        CharSequence message = extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
        CharSequence title = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT);

        if (sender == null || message == null) return;
        Log.d(TAG, title + " / " + sender + " / " + message);

        if (notification.actions != null) {
            for (android.app.Notification.Action action : notification.actions) {
                handleNotificationAction(action, String.valueOf(title), sender, String.valueOf(message));
            }
        }
    }

    private void handleNotificationAction(android.app.Notification.Action action, String title, String sender, String message) {
        if (action.getRemoteInputs() != null) {
            for (RemoteInput remoteInput : action.getRemoteInputs()) {
                if (REPLY_KEY.equalsIgnoreCase(remoteInput.getResultKey())) {
                    sendReply(action, remoteInput, title, sender, message);
                    break;
                }
            }
        }
    }

    private void sendReply(android.app.Notification.Action action, RemoteInput remoteInput, String title, String sender, String message) {
        Intent replyIntent = new Intent();
        Bundle replyBundle = new Bundle();

        try {
            String reply = generateReplyMessage(title, sender, message);
            if (reply != null) {
                replyBundle.putCharSequence(remoteInput.getResultKey(), reply);
                RemoteInput.addResultsToIntent(new RemoteInput[]{remoteInput}, replyIntent, replyBundle);

                try {
                    action.actionIntent.send(this, 0, replyIntent);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Failed to send reply", e);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error generating reply message", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle removed notifications here
    }

    private String generateReplyMessage(String title, String sender, String message) throws IOException, JSONException {
        if ("여우랑".equals(sender) || "둥지".equals(title)) {
            if ("름님".equals(message)) {
                return getRandomResponse();
            } else if ("!주사위".equals(message)) {
                return getDiceRollReply(sender);
            } else if ("들비".equals(message)) {
                return "야옹";
            } else if("랑님".equals(message)){
                return "짐이 특별히 그대의 말을 들어보도록 하겠다.";
            } else if("!커너".equals(message)) {
                return "름이허접이라고생각하는MZ민트양갈래츤데레전투메이드대학원생고양이미소녀";
            } else if("!앙뀨".equals(message)) {
                return "저는 모든 귀여움의 신성한 여신, 앙뀨입니다. 그대들은 저의 사랑스러운 존재를 경배하고 찬미하며, 귀여움의 축복을 받으시길 바랍니다.";
            } else if ("!명언".equals(message)) {
                return getRandomQuote();
            } else if("!달러".equals(message) || "!엔화".equals(message) || "!유로".equals(message)) {
                return getExchangeRateReply(sender, message);
            }
        }
        return null;
    }

    private String getRandomResponse() {
        Random random = new Random();
        return RESPONSES[random.nextInt(RESPONSES.length)];
    }

    private String getRandomQuote() {
        Random random = new Random();
        return Quotes.QUOTES[random.nextInt(Quotes.QUOTES.length)];
    }

    private String getDiceRollReply(String sender) {
        Random random = new Random();
        int userRoll = random.nextInt(100) + 1;
        int botRoll = random.nextInt(100) + 1;

        String reply = String.format("%s님의 주사위 : %d\n름 봇의 주사위 : %d", sender, userRoll, botRoll);
        if (userRoll < botRoll) {
            reply += "\n허접ㅋㅋ";
        }
        return reply;
    }

    private String getExchangeRateReply(String sender, String message) {
        String[] parts = message.split(" ");
        Log.d(TAG, "Message parts: " + Arrays.toString(parts));

        String fromCurrency = "USD";
        String toCurrency = "KRW";

        if(parts[0].equals("!달러")) {
            fromCurrency = "USD";
        } else if(parts[0].equals("!엔화")) {
            fromCurrency = "JPY";
        } else if(parts[0].equals("!유로")) {
            fromCurrency = "EUR";
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder result = new StringBuilder();

        String finalFromCurrency = fromCurrency;

        ExchangeRateService.getExchangeRate(fromCurrency, toCurrency, new ExchangeRateService.ExchangeRateCallback() {
            @Override
            public void onSuccess(double rate) {
                if (finalFromCurrency.equals("JPY")) {
                    rate *= 100;
                }

                result.append(String.format("%s -> %s 환율은 %.2f 입니다.", finalFromCurrency, toCurrency, rate));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching exchange rate", e);
                result.append("환율 정보를 가져오는 도중 오류가 발생했습니다. 오류: ").append(e.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await(); // 콜백이 호출될 때까지 대기
            Log.d(TAG, "Exchange rate result: " + result.toString());
            return result.toString();
        } catch (InterruptedException e) {
            Log.e(TAG, "Latch interrupted", e);
            return "환율 정보를 가져오는 도중 인터럽트가 발생했습니다.";
        }
    }
}