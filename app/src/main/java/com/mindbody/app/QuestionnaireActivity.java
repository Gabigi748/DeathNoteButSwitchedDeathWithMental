package com.mindbody.app;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.mindbody.app.fragment.QuestionFragment;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionnaireActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnPrev, btnNext;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private ApiService apiService;

    private String type; // "PHQ9" or "GAD7"
    private String[] questions;
    private int[] answers;

    // PHQ-9 Questions
    private static final String[] PHQ9_QUESTIONS = {
            "做事時提不起勁或沒有樂趣",
            "感到心情低落、沮喪或絕望",
            "入睡困難、睡不安穩或睡眠過多",
            "感覺疲倦或沒有活力",
            "食慾不振或吃太多",
            "覺得自己很糟、是個失敗者，或讓自己或家人失望",
            "對事物專注有困難，例如閱讀報紙或看電視",
            "動作或說話速度緩慢到別人可以察覺，或正好相反，煩躁或坐立不安",
            "有不如死掉或用某種方式傷害自己的念頭"
    };

    // GAD-7 Questions
    private static final String[] GAD7_QUESTIONS = {
            "感覺緊張、焦慮或煩躁",
            "不能夠停止或控制擔憂",
            "對各種各樣的事情擔憂過多",
            "很難放鬆下來",
            "由於不安而無法靜坐",
            "變得容易煩惱或急躁",
            "感到似乎將有可怕的事情發生而害怕"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        apiService = RetrofitClient.getInstance(this).getApiService();

        type = getIntent().getStringExtra("type");
        if (type == null) type = "PHQ9";

        questions = type.equals("PHQ9") ? PHQ9_QUESTIONS : GAD7_QUESTIONS;
        answers = new int[questions.length];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = -1; // Not answered
        }

        viewPager = findViewById(R.id.view_pager);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar_questionnaire);

        setupViewPager();
        updateProgress(0);

        btnPrev.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < questions.length - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                // Last question - submit
                submitQuestionnaire();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateProgress(position);
            }
        });

        // Disable swipe (navigate only via buttons)
        viewPager.setUserInputEnabled(false);
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return questions.length;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return QuestionFragment.newInstance(position, questions[position]);
            }
        });
    }

    private void updateProgress(int position) {
        tvProgress.setText(getString(R.string.question_progress, position + 1, questions.length));
        progressBar.setMax(questions.length);
        progressBar.setProgress(position + 1);

        btnPrev.setEnabled(position > 0);
        if (position == questions.length - 1) {
            btnNext.setText(R.string.btn_submit);
        } else {
            btnNext.setText(R.string.btn_next);
        }
    }

    public void setAnswer(int questionIndex, int answer) {
        if (questionIndex >= 0 && questionIndex < answers.length) {
            answers[questionIndex] = answer;
        }
    }

    private void submitQuestionnaire() {
        // Check all answered
        for (int i = 0; i < answers.length; i++) {
            if (answers[i] == -1) {
                Toast.makeText(this, "請完成第 " + (i + 1) + " 題", Toast.LENGTH_SHORT).show();
                viewPager.setCurrentItem(i);
                return;
            }
        }

        // Calculate total score
        int totalScore = 0;
        for (int a : answers) {
            totalScore += a;
        }

        // Show result first
        String severity = getSeverity(totalScore);
        showResult(totalScore, severity);

        // Submit to server
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        List<Integer> answerList = new ArrayList<>();
        for (int a : answers) {
            answerList.add(a);
        }
        body.put("answers", answerList);

        apiService.submitQuestionnaire(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Result already shown
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(QuestionnaireActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSeverity(int score) {
        if (type.equals("PHQ9")) {
            if (score <= 4) return "正常（0-4 分）";
            if (score <= 9) return "輕度憂鬱（5-9 分）";
            if (score <= 14) return "中度憂鬱（10-14 分）";
            if (score <= 19) return "中重度憂鬱（15-19 分）";
            return "重度憂鬱（20-27 分）";
        } else {
            if (score <= 4) return "正常（0-4 分）";
            if (score <= 9) return "輕度焦慮（5-9 分）";
            if (score <= 14) return "中度焦慮（10-14 分）";
            return "重度焦慮（15-21 分）";
        }
    }

    private void showResult(int score, String severity) {
        new AlertDialog.Builder(this)
                .setTitle("量表結果")
                .setMessage(getString(R.string.score_result, score) + "\n\n程度：" + severity
                        + "\n\n⚠️ 此結果僅供參考，不構成醫療診斷。如有需要，請諮詢專業醫療人員。")
                .setPositiveButton("確定", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
