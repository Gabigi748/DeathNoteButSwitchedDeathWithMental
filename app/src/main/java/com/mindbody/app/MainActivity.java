package com.mindbody.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mindbody.app.fragment.CheckinFragment;
import com.mindbody.app.fragment.HomeFragment;
import com.mindbody.app.fragment.ProfileFragment;
import com.mindbody.app.fragment.StatsFragment;
import com.mindbody.app.worker.QuestionnaireReminderWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String REMINDER_WORK_TAG = "questionnaire_reminder_periodic";
    private static final int REQ_NOTIF_PERM = 1001;

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_checkin) {
                fragment = new CheckinFragment();
            } else if (itemId == R.id.nav_stats) {
                fragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });

        // 排程「每日問卷提醒」Worker（冪等：KEEP 不會重複註冊）
        scheduleQuestionnaireReminder();

        // Android 13+ 需要動態請求通知權限
        ensureNotificationPermission();
    }

    private void scheduleQuestionnaireReminder() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                QuestionnaireReminderWorker.class,
                1, TimeUnit.DAYS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        REMINDER_WORK_TAG,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                );
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF_PERM
                );
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Navigate to checkin tab programmatically
     */
    public void navigateToCheckin() {
        bottomNav.setSelectedItemId(R.id.nav_checkin);
    }
}
