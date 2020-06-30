package com.basecamp.turbolinks.demo;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class PushNotifications extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        Log.d("Push", "Create PushNotification Service");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("Push", "From: " + remoteMessage.getFrom());

        Map<String, String> data = this.getData(remoteMessage);

         //Get an instance of NotificationManager//
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setColor(getResources().getColor(R.color.colorLogo))
                        .setContentTitle(data.get("title"))
                        .setContentText(data.get("body"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        if (data.get("location") != null) {
            // Create an explicit intent for an Activity in your app
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Cause onNewIntent to fire instead of creating new task
            intent.putExtra(MainActivity.INTENT_URL, data.get("location"));
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 4, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        // Gets an instance of the NotificationManager service//
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // When you issue multiple notifications about the same type of event,
        // it’s best practice for your app to try to update an existing notification
        notificationManager.notify(1, builder.build());
    }

    public Map<String, String> getData(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d("Push", "Data Received");
//            remoteMessage.getData().forEach((k,v) -> Log.d("Push",k + " - " + v));
            return remoteMessage.getData();
        } else {
            Log.d("Push", "Notification Received");
            HashMap<String, String> data = new HashMap<>();
            data.put("title", remoteMessage.getNotification().getTitle());
            data.put("body", remoteMessage.getNotification().getBody());
            return data;
        }
    }
}
