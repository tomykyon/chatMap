package com.websarva.wings.android.chatmap;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import androidx.navigation.ui.AppBarConfiguration;
import com.google.firebase.firestore.FirebaseFirestore;
import com.websarva.wings.android.chatmap.databinding.ActivityMainBinding;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        mAuth = FirebaseAuth.getInstance(); // Firebase Auth インスタンスを取得

        emailEditText = findViewById(R.id.textEmail);
        passwordEditText = findViewById(R.id.editTextNumberPassword);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "メールとパスワードを入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(MainActivity.this, "ログイン成功: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "ログイン成功しましたがユーザー情報を取得できません", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "ログイン失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });


        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "メールとパスワードを入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {                          // ユーザー登録成功
                            FirebaseUser user = task.getResult().getUser(); // ユーザー情報を取得
                            if (user == null) {
                                Toast.makeText(MainActivity.this, "ユーザー情報が取得できませんでした", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Firestore に保存
                            String uid = user.getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("email", email);
                            userMap.put("name", "たろう");

                            db.collection("users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "ユーザー情報を保存しました！");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "保存失敗", e);
                                    });

                            // メッセージと画面遷移
                            Toast.makeText(MainActivity.this, "登録成功: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            // 登録失敗
                            Toast.makeText(MainActivity.this, "登録失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
        @Override
    public void onStart() {
        super.onStart();
        // アプリ起動時にすでにログインしているか確認
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}