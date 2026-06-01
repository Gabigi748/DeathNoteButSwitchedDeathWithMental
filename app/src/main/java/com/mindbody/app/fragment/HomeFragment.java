package com.mindbody.app.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mindbody.app.MainActivity;
import com.mindbody.app.QuestionnaireActivity;
import com.mindbody.app.R;
import com.mindbody.app.adapter.CalendarAdapter;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;
import com.mindbody.app.util.SharedPrefManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvStreak, tvAiSuggestion, tvMonthTitle;
    private RecyclerView rvCalendar;
    private MaterialButton btnStartCheckin;
    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvAiSuggestion = view.findViewById(R.id.tv_ai_suggestion);
        tvMonthTitle = view.findViewById(R.id.tv_month_title);
        rvCalendar = view.findViewById(R.id.rv_calendar);
        btnStartCheckin = view.findViewById(R.id.btn_start_checkin);

        prefManager = new SharedPrefManager(requireContext());
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();

        setupGreeting();
        setupCalendar();
        loadStreak();
        loadAiSuggestion();
        loadCheckinHistory();
        checkQuestionnaireReminder(view);

        btnStartCheckin.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCheckin();
            }
        });
    }

    /**
     * 14 天沒填問卷或從未填過 → 顯示首頁提醒卡片
     */
    private void checkQuestionnaireReminder(View root) {
        final CardView card = root.findViewById(R.id.card_questionnaire_reminder);
        final TextView tvText = root.findViewById(R.id.tv_reminder_text);
        final MaterialButton btnGo = root.findViewById(R.id.btn_reminder_go);
        final MaterialButton btnDismiss = root.findViewById(R.id.btn_reminder_dismiss);
        if (card == null) return;

        // 1 天內按過「稍後再說」就不再彈
        SharedPreferences prefs = requireContext().getSharedPreferences("mindbody_reminder", 0);
        long dismissedAt = prefs.getLong("home_dismissed_at", 0L);
        if (System.currentTimeMillis() - dismissedAt < 24L * 3600L * 1000L) {
            card.setVisibility(View.GONE);
            return;
        }

        apiService.getLatestQuestionnaire().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                Object q = response.body().get("questionnaire");

                boolean show;
                String text;
                if (q == null) {
                    show = true;
                    text = "你還沒填過 PHQ-9 / GAD-7 量表，花 3 分鐘檢視一下身心狀態吧";
                } else if (q instanceof Map) {
                    Map<?, ?> qm = (Map<?, ?>) q;
                    Object createdAt = qm.get("created_at");
                    long days = createdAt != null ? daysSince(createdAt.toString()) : 0;
                    if (days >= 14) {
                        show = true;
                        text = String.format("距離上次填問卷已過 %d 天，建議再評估一次", days);
                    } else {
                        show = false;
                        text = "";
                    }
                } else {
                    show = false;
                    text = "";
                }

                if (show) {
                    tvText.setText(text);
                    card.setVisibility(View.VISIBLE);
                } else {
                    card.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                card.setVisibility(View.GONE);
            }
        });

        btnGo.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), QuestionnaireActivity.class));
            card.setVisibility(View.GONE);
        });
        btnDismiss.setOnClickListener(v -> {
            prefs.edit().putLong("home_dismissed_at", System.currentTimeMillis()).apply();
            card.setVisibility(View.GONE);
        });
    }

    private long daysSince(String iso) {
        try {
            String norm = iso.replace(" ", "T");
            if (!norm.endsWith("Z") && !norm.contains("+")) norm += "Z";
            long t = java.time.Instant.parse(norm).toEpochMilli();
            return (System.currentTimeMillis() - t) / (24L * 3600L * 1000L);
        } catch (Exception e) {
            return 0;
        }
    }

    private void setupGreeting() {
        String nickname = prefManager.getNickname();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = getString(R.string.greeting_morning, nickname);
        } else if (hour < 18) {
            greeting = getString(R.string.greeting_afternoon, nickname);
        } else {
            greeting = getString(R.string.greeting_evening, nickname);
        }
        tvGreeting.setText(greeting);
    }

    private void setupCalendar() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        tvMonthTitle.setText(String.format("%d 年 %d 月", year, month));

        rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
    }

    private void loadStreak() {
        apiService.getStreak().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object streakObj = response.body().get("streak_days");
                    int streak = 0;
                    if (streakObj instanceof Double) {
                        streak = ((Double) streakObj).intValue();
                    }
                    tvStreak.setText(getString(R.string.streak_text, streak));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvStreak.setText(getString(R.string.streak_text, 0));
            }
        });
    }

    private void loadAiSuggestion() {
        apiService.getLatestAi().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object analysisObj = response.body().get("analysis");
                    if (analysisObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> analysis = (Map<String, Object>) analysisObj;
                        Object aiTextObj = analysis.get("ai_response");
                        if (aiTextObj != null && !aiTextObj.toString().isEmpty()) {
                            tvAiSuggestion.setText(aiTextObj.toString());
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Keep default text
            }
        });
    }

    private void loadCheckinHistory() {
        apiService.getCheckinHistory(30).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Set<Integer> checkedDays = new HashSet<>();
                if (response.isSuccessful() && response.body() != null) {
                    Object listObj = response.body().get("checkins");
                    if (listObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> checkins = (List<Map<String, Object>>) listObj;
                        for (Map<String, Object> item : checkins) {
                            Object dateObj = item.get("checkin_date");
                            if (dateObj != null) {
                                String dateStr = dateObj.toString();
                                // Handle ISO format "2026-06-01T00:00:00.000Z" and "2026-06-01"
                                try {
                                    if (dateStr.length() >= 10) {
                                        String ymd = dateStr.substring(0, 10);
                                        String[] parts = ymd.split("-");
                                        if (parts.length == 3) {
                                            int day = Integer.parseInt(parts[2]);
                                            int month = Integer.parseInt(parts[1]);
                                            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
                                            if (month == currentMonth) {
                                                checkedDays.add(day);
                                            }
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    // Skip invalid dates
                                }
                            }
                        }
                    }
                }
                updateCalendar(checkedDays);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                updateCalendar(new HashSet<>());
            }
        });
    }

    private void updateCalendar(Set<Integer> checkedDays) {
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarAdapter.DayItem> days = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(new CalendarAdapter.DayItem(i, checkedDays.contains(i)));
        }

        CalendarAdapter adapter = new CalendarAdapter(days);
        rvCalendar.setAdapter(adapter);
    }
}
