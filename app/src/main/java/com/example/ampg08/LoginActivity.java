package com.example.ampg08;

import android.os.Bundle;
import com.example.ampg08.databinding.ActivityLoginBinding;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnLogin.setOnClickListener(v -> {
            // TODO: Implement authentication
            finish();
        });

        binding.btnGuest.setOnClickListener(v -> finish());
    }
}