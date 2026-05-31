package com.mindbody.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mindbody.app.QuestionnaireActivity;
import com.mindbody.app.R;

public class QuestionFragment extends Fragment {

    private static final String ARG_INDEX = "index";
    private static final String ARG_QUESTION = "question";

    public static QuestionFragment newInstance(int index, String question) {
        QuestionFragment fragment = new QuestionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INDEX, index);
        args.putString(ARG_QUESTION, question);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_question, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int index = getArguments() != null ? getArguments().getInt(ARG_INDEX, 0) : 0;
        String question = getArguments() != null ? getArguments().getString(ARG_QUESTION, "") : "";

        TextView tvQuestion = view.findViewById(R.id.tv_question);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        RadioButton rb0 = view.findViewById(R.id.rb_option_0);
        RadioButton rb1 = view.findViewById(R.id.rb_option_1);
        RadioButton rb2 = view.findViewById(R.id.rb_option_2);
        RadioButton rb3 = view.findViewById(R.id.rb_option_3);

        tvQuestion.setText(question);
        rb0.setText(getString(R.string.phq_option_0) + "（0 分）");
        rb1.setText(getString(R.string.phq_option_1) + "（1 分）");
        rb2.setText(getString(R.string.phq_option_2) + "（2 分）");
        rb3.setText(getString(R.string.phq_option_3) + "（3 分）");

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int answer = -1;
            if (checkedId == R.id.rb_option_0) answer = 0;
            else if (checkedId == R.id.rb_option_1) answer = 1;
            else if (checkedId == R.id.rb_option_2) answer = 2;
            else if (checkedId == R.id.rb_option_3) answer = 3;

            if (answer >= 0 && getActivity() instanceof QuestionnaireActivity) {
                ((QuestionnaireActivity) getActivity()).setAnswer(index, answer);
            }
        });
    }
}
