package com.websarva.wings.android.chatmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JoinGroupActivity extends AppCompatActivity {
    private EditText editTextRoomCode;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Object currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        editTextRoomCode = findViewById(R.id.editTextRoomCode);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        findViewById(R.id.buttonJoinGroup).setOnClickListener(v -> {
            String roomCode = editTextRoomCode.getText().toString().trim();

            if (roomCode.length() != 4) {
                Toast.makeText(this, "4桁のコードを入力してね", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("groups")
                    .whereEqualTo("roomCode", roomCode)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            DocumentSnapshot groupDoc = query.getDocuments().get(0);
                            String groupId = groupDoc.getId();
                            String currentUserId = auth.getCurrentUser().getUid();

                            // すでにmembersに含まれているかチェックしてから追加（オプション）
                            db.collection("groups").document(groupId)
                                    .update("members", FieldValue.arrayUnion(currentUserId))
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "入室しました！", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, GroupChatActivity.class);
                                        intent.putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId);
                                        startActivity(intent);
                                        finish();
                                    });

                        } else {
                            Toast.makeText(this, "そのコードの部屋は見つかりませんでした", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "検索に失敗しました", Toast.LENGTH_SHORT).show();
                    });
        });

    }
}
