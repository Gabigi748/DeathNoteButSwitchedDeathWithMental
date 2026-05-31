package com.mindbody.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mindbody.app.LoginActivity;
import com.mindbody.app.QuestionnaireActivity;
import com.mindbody.app.R;
import com.mindbody.app.model.User;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;
import com.mindbody.app.util.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvNickname, tvEmail, tvGender, tvBirthday;
    private MaterialButton btnPhq9, btnGad7, btnLogout;
    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNickname = view.findViewById(R.id.tv_nickname);
        tvEmail = view.findViewById(R.id.tv_email);
        tvGender = view.findViewById(R.id.tv_gender);
        tvBirthday = view.findViewById(R.id.tv_birthday);
        btnPhq9 = view.findViewById(R.id.btn_phq9);
        btnGad7 = view.findViewById(R.id.btn_gad7);
        btnLogout = view.findViewById(R.id.btn_logout);

        prefManager = new SharedPrefManager(requireContext());
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();

        loadProfile();

        btnPhq9.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QuestionnaireActivity.class);
            intent.putExtra("type", "PHQ9");
            startActivity(intent);
        });

        btnGad7.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QuestionnaireActivity.class);
            intent.putExtra("type", "GAD7");
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadProfile() {
        apiService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvNickname.setText(user.getNickname());
                    tvEmail.setText(user.getEmail());
                    tvGender.setText(user.getGender() != null ? user.getGender() : "-");
                    tvBirthday.setText(user.getBirthday() != null ? user.getBirthday() : "-");

                    // Update local cache
                    if (user.getNickname() != null) {
                        prefManager.saveNickname(user.getNickname());
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Show cached data
                tvNickname.setText(prefManager.getNickname());
                tvEmail.setText(prefManager.getEmail());
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    prefManager.clear();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
