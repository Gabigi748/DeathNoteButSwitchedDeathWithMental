package com.mindbody.app.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mindbody.app.MainActivity;
import com.mindbody.app.QuestionnaireActivity;
import com.mindbody.app.R;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;
import com.mindbody.app.util.SharedPrefManager;

import java.util.Map;
import retrofit2.Response;

/**
 * 每日檢查上次問卷距今是否 >= 14 天，是則發系統通知提醒做 PHQ-9 / GAD-7
 */
public class QuestionnaireReminderWorker extends Worker {

    private static final String CHANNEL_ID = "mindbody_questionnaire_reminder";
    private static final int NOTIF_ID = 2001;
    private static final String PREF_NAME = "mindbody_reminder";
    private static final String KEY_LAST_REMINDED = "last_reminded_at";
    private static final long DAYS_INTERVAL = 14L; // 14 天
    private static final long REMIND_COOLDOWN_MS = 24L * 3600L * 1000L; // 1 天內不重複提醒

    public QuestionnaireReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1) 24 小時內提醒過就跳過
        long lastReminded = prefs.getLong(KEY_LAST_REMINDED, 0L);
        if (System.currentTimeMillis() - lastReminded < REMIND_COOLDOWN_MS) {
            return Result.success();
        }

        // 2) 確認有登入（沒 token 就無法查 latest，跳過）
        SharedPrefManager spm = new SharedPrefManager(ctx);
        String token = spm.getToken();
        if (token == null || token.isEmpty()) {
            return Result.success();
        }
        ApiService api = RetrofitClient.getInstance(ctx).getApiService();

        // 3) 查最近一次問卷
        try {
            Response<Map<String, Object>> resp = api.getLatestQuestionnaire().execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                return Result.retry();
            }

            Object q = resp.body().get("questionnaire");
            boolean shouldRemind;
            if (q == null) {
                // 從未做過問卷 → 提醒
                shouldRemind = true;
            } else if (q instanceof Map) {
                Map<?, ?> qm = (Map<?, ?>) q;
                String createdAt = qm.get("created_at") != null ? qm.get("created_at").toString() : null;
                shouldRemind = isOlderThanDays(createdAt, DAYS_INTERVAL);
            } else {
                shouldRemind = false;
            }

            if (shouldRemind) {
                showNotification(ctx);
                prefs.edit().putLong(KEY_LAST_REMINDED, System.currentTimeMillis()).apply();
            }
        } catch (Exception e) {
            return Result.retry();
        }

        return Result.success();
    }

    private boolean isOlderThanDays(String isoDate, long days) {
        if (isoDate == null) return true;
        try {
            // ISO 字串格式：2026-06-01T08:30:00.000Z 或 2026-06-01 08:30:00
            String norm = isoDate.replace(" ", "T");
            if (!norm.endsWith("Z") && !norm.contains("+")) norm += "Z";
            long t = java.time.Instant.parse(norm).toEpochMilli();
            return (System.currentTimeMillis() - t) >= days * 24L * 3600L * 1000L;
        } catch (Exception e) {
            return false;
        }
    }

    private void showNotification(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "問卷提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("提醒定期完成 PHQ-9 / GAD-7 量表");
            nm.createNotificationChannel(ch);
        }

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("該做身心評估了")
            .setContentText("已超過兩週沒填 PHQ-9 / GAD-7 量表，花 3 分鐘檢視一下身心狀態吧")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(
                "已超過兩週沒填 PHQ-9 / GAD-7 量表，花 3 分鐘檢視一下身心狀態，記錄會幫助 AI 給你更精準的回應。"
            ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true);

        nm.notify(NOTIF_ID, builder.build());
    }
}
