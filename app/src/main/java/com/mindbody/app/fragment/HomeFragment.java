package com.mindbody.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mindbody.app.MainActivity;
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

        btnStartCheckin.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCheckin();
            }
        });
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
