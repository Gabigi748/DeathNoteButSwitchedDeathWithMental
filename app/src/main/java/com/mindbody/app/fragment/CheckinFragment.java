package com.mindbody.app.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.mindbody.app.R;
import com.mindbody.app.adapter.SymptomExpandableAdapter;
import com.mindbody.app.model.CheckinRequest;
import com.mindbody.app.model.Symptom;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckinFragment extends Fragment {

    private ImageButton btnMood1, btnMood2, btnMood3, btnMood4, btnMood5;
    private ExpandableListView elvSymptoms;
    private EditText etDiary;
    private TextView tvWordCount;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private LinearLayout layoutWarning;
    private ApiService apiService;

    private int selectedMood = 0;
    private SymptomExpandableAdapter symptomAdapter;

    // Symptom data
    private final LinkedHashMap<String, List<String>> symptomData = new LinkedHashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getInstance(requireContext()).getApiService();

        btnMood1 = view.findViewById(R.id.btn_mood_1);
        btnMood2 = view.findViewById(R.id.btn_mood_2);
        btnMood3 = view.findViewById(R.id.btn_mood_3);
        btnMood4 = view.findViewById(R.id.btn_mood_4);
        btnMood5 = view.findViewById(R.id.btn_mood_5);
        elvSymptoms = view.findViewById(R.id.elv_symptoms);
        etDiary = view.findViewById(R.id.et_diary);
        tvWordCount = view.findViewById(R.id.tv_word_count);
        btnSubmit = view.findViewById(R.id.btn_submit_checkin);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutWarning = view.findViewById(R.id.layout_warning);

        setupMoodButtons();
        setupSymptoms();
        setupDiary();

        btnSubmit.setOnClickListener(v -> submitCheckin());
    }

    private void setupMoodButtons() {
        ImageButton[] buttons = {btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};

        for (int i = 0; i < buttons.length; i++) {
            final int mood = i + 1;
            buttons[i].setOnClickListener(v -> {
                selectedMood = mood;
                updateMoodUI(buttons);
            });
        }
    }

    private void updateMoodUI(ImageButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            if (i + 1 == selectedMood) {
                buttons[i].setBackgroundResource(R.drawable.bg_mood_selected);
            } else {
                buttons[i].setBackgroundResource(R.drawable.bg_mood_normal);
            }
        }
    }

    private void setupSymptoms() {
        symptomData.put("頭部", Arrays.asList("頭痛", "頭暈", "耳鳴", "視力模糊"));
        symptomData.put("胸部", Arrays.asList("胸悶", "心悸", "呼吸困難", "咳嗽"));
        symptomData.put("腹部", Arrays.asList("胃痛", "噁心", "腹瀉", "便秘", "食慾不振"));
        symptomData.put("四肢", Arrays.asList("肌肉痠痛", "關節痛", "手腳麻木", "疲勞無力"));
        symptomData.put("心理", Arrays.asList("焦慮", "憂鬱", "失眠", "注意力不集中", "情緒低落"));
        symptomData.put("其他", Arrays.asList("其他症狀"));

        List<String> groups = new ArrayList<>(symptomData.keySet());
        symptomAdapter = new SymptomExpandableAdapter(requireContext(), groups, symptomData);
        elvSymptoms.setAdapter(symptomAdapter);
    }

    private void setupDiary() {
        etDiary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvWordCount.setText(String.format("%d/500", s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void submitCheckin() {
        if (selectedMood == 0) {
            Toast.makeText(requireContext(), "請選擇今天的心情", Toast.LENGTH_SHORT).show();
            return;
        }

        String diary = etDiary.getText().toString().trim();
        List<Symptom> symptoms = symptomAdapter.getSelectedSymptoms();

        // Check for severe symptoms
        boolean hasSevere = false;
        for (Symptom s : symptoms) {
            if ("重度".equals(s.getSeverity())) {
                hasSevere = true;
                break;
            }
        }

        if (hasSevere) {
            layoutWarning.setVisibility(View.VISIBLE);
        }

        setLoading(true);

        CheckinRequest request = new CheckinRequest(selectedMood, symptoms, diary);
        apiService.submitCheckin(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.success_checkin, Toast.LENGTH_SHORT).show();
                    showAiResult(response.body());
                    resetForm();
                } else {
                    Toast.makeText(requireContext(), "打卡失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAiResult(Map<String, Object> responseBody) {
        String aiText = "";
        if (responseBody != null && responseBody.containsKey("ai_response")) {
            aiText = (String) responseBody.get("ai_response");
        }

        if (aiText == null || aiText.isEmpty()) {
            aiText = "打卡已記錄，AI 分析將在稍後生成。";
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ai_result, null);
        TextView tvResult = sheetView.findViewById(R.id.tv_ai_result);
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);

        tvResult.setText(aiText);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void resetForm() {
        selectedMood = 0;
        ImageButton[] buttons = {btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};
        for (ImageButton btn : buttons) {
            btn.setBackgroundResource(R.drawable.bg_mood_normal);
        }
        etDiary.setText("");
        layoutWarning.setVisibility(View.GONE);
        symptomAdapter.clearSelections();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}
