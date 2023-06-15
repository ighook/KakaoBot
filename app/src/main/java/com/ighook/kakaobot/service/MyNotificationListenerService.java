package com.ighook.kakaobot.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MyNotificationListenerService extends NotificationListenerService {

    private static String TAG = "푸시 알림";
    private String[] fma = {
//            "싫어 떼 쓸거야 빨리 고쳐줘",
//            "목줄까진 몰라도 목조르기는 좀...",
//            "뭘 봐 팍씨 참치캔 내놔",
//            "내가 목줄한거 봤어요?",
//            "내가 나서면 그건 싸움이 아닌 학살이 되니까...",
//            "ㄹㅇ 허접이네", "어정쩡한 허접이군요",
//            "몰?름은 가짜에요",
            "ㅗ",
            "네?"
//            "안됩니다"
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Notification notification = sbn.getNotification();
        Bundle extras = sbn.getNotification().extras;
        String sender = extras.getString(Notification.EXTRA_TITLE);
        CharSequence message = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence title = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        if(sender == null || message == null) return;
        Log.d(TAG, title + " / " + sender + " / " + message);

        try {
            if(notification.actions == null) return;
            for (Notification.Action action : notification.actions) {
                if (action.getRemoteInputs() != null) {
                    for (RemoteInput remoteInput : action.getRemoteInputs()) {
                        if (remoteInput.getResultKey().equalsIgnoreCase("reply_message")) {
                            Intent replyIntent = new Intent();
                            Bundle replyBundle = new Bundle();

                            String reply = getReplyMessage(String.valueOf(title), sender, String.valueOf(message));
                            if(reply != null) {
                                replyBundle.putCharSequence(remoteInput.getResultKey(), reply);
                                RemoteInput.addResultsToIntent(new RemoteInput[]{remoteInput}, replyIntent, replyBundle);

                                try {
                                    action.actionIntent.send(this, 0, replyIntent);
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle removed notifications here
    }

    public String getReplyMessage(String title, String sender, String message) throws IOException {
        String reply = null;

        if(title.equals("게와글의 둥지")) {
            if(message.equals("름님")) {
                Random random = new Random();
                int randomNumber1 = random.nextInt(fma.length);
                reply = fma[randomNumber1];
            } else if(message.equals("!주사위")) {
                Random random = new Random();
                int randomNumber1 = random.nextInt(100) + 1;

                if(sender.equals("솦랑")) randomNumber1 = 999999;
                int randomNumber2 = random.nextInt(100) + 1;

                reply = sender + "의 주사위 : " + randomNumber1 + "\n름 봇의 주사위 : " + randomNumber2;

                if(randomNumber1 < randomNumber2) {
                    reply += "\n\n허~접ㅋㅋ";
                } else {
                    reply += "\n\n름은 허접이에요...";
                }
            } else if(message.startsWith("!날씨")) {
                try {
                    String city = message.split(" ")[1];
                    Log.d(TAG, city);
                    if (city == null) return null;

                    CountDownLatch latch = new CountDownLatch(1);
                    final String[] result = new String[1];

                    Weather weatherThread = new Weather(city, new Weather.WeatherCallback() {
                        @Override
                        public void onWeatherInfoReceived(String info) {
                            result[0] = info;
                            latch.countDown(); // Decrease the count, allowing main thread to proceed
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            latch.countDown(); // Decrease the count even in case of error
                        }
                    });
                    weatherThread.start();

                    try {
                        latch.await(); // This makes the main thread wait until the latch count is decreased to 0
                        Log.d(TAG, result[0]);
                        reply = result[0];
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    reply = "도시 이름은 영어로 써줘";
                }
            } else if(message.equals("젤다")) {
                reply = "젤다는 업무 중 이에요";
            } else if(message.equals("망고")) {
                reply = "난 예뻐";
            }
        }
        return reply;
    }
}