package com.websarva.wings.android.chatmap;

import static com.websarva.wings.android.chatmap.GroupChatActivity.EXTRA_GROUP_ID;
import static com.websarva.wings.android.chatmap.GroupChatActivity.EXTRA_GROUP_NAME;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondActivity extends AppCompatActivity {

    private EditText editUserId;
    private TextView textResult;
    private Button buttonAddFriend;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String foundUserId = null;
    private View fab;
    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private List<Friend> friendList = new ArrayList<>();
    private Friend docRef;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        editUserId = findViewById(R.id.editUserId);
        textResult = findViewById(R.id.textResult);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new FriendAdapter(this, friendList, friend -> {
            if (friend != null && friend.getId() != null && friend.getName() != null) {
                if (auth.getCurrentUser() == null) {
                    Toast.makeText(this, "ログイン情報が取得できません", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentUserId = auth.getCurrentUser().getUid();
                String chatRoomId = getChatRoomId(currentUserId, friend.getUid());
                Intent intent = new Intent(SecondActivity.this, ChatActivity.class);
                intent.putExtra("friendId", friend.getUid());
                intent.putExtra("friendName", friend.getName());
                intent.putExtra("chatRoomId", chatRoomId);
                startActivity(intent);
                Log.d("ChatRoomID生成", "currentUserId: " + currentUserId + ", friendId: " + friend.getUid());
                Log.d("ChatRoomID生成", "生成されたchatRoomId: " + chatRoomId);
            } else {
                Toast.makeText(this, "フレンド情報に不備があります", Toast.LENGTH_SHORT).show();
            }
        });

        // フレンド検索ボタン
        findViewById(R.id.buttonSearch).setOnClickListener(v -> {
            String input = editUserId.getText().toString().trim();
            if (!input.isEmpty()) {
                searchUserById(input);
            }
        });

        // フレンド追加ボタン
        buttonAddFriend.setOnClickListener(v -> {
            if (foundUserId != null) {
                addFriend(foundUserId);
            }
        });

        // ✅ グループ作成ボタン（roomCode生成してFirestoreに保存）
        findViewById(R.id.buttonCreateGroup).setOnClickListener(v -> {
            List<String> selectedIds = new ArrayList<>();
            for (Friend f : friendList) {
                if (f != null && f.isChecked() && f.getUid() != null) {
                    selectedIds.add(f.getUid());
                }
            }

            if (selectedIds.size() >= 1) {
                selectedIds.add(auth.getCurrentUser().getUid()); // 自分も追加

                String roomCode = String.format("%04d", new java.util.Random().nextInt(10000));

                Map<String, Object> group = new HashMap<>();
                group.put("members", selectedIds);
                group.put("roomCode", roomCode);
                group.put("createdAt", FieldValue.serverTimestamp());
                group.put("name", "新しいグループ");


                db.collection("groups")
                        .add(group)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "部屋コード: " + roomCode, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(this, GroupChatActivity.class);
                            intent.putExtra(EXTRA_GROUP_ID, docRef.getId());
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "部屋の作成に失敗しました", Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(this, "フレンドを1人以上選んでください", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ コードで参加ボタン（JoinGroupActivityに遷移）
        findViewById(R.id.buttonJoinByCode).setOnClickListener(v -> {
            Intent intent = new Intent(this, JoinGroupActivity.class);
            startActivity(intent);
        });

        // RecyclerView設定
        recyclerView = findViewById(R.id.recyclerFriendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // フレンド一覧を読み込む
        loadFriendList();
    }

    private void createGroup(List<String> memberIds) {
        Map<String, Object> group = new HashMap<>();
        group.put("members", memberIds);
        group.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();
                    Intent intent = new Intent(this, GroupChatActivity.class);
                    intent.putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "グループ作成に失敗", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriendList() {
        String currentUserId = auth.getCurrentUser().getUid();
        friendList.clear();

        db.collection("friends").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> friendIds = (List<String>) documentSnapshot.get("friendIds");
                        Log.d("FriendList", "取得したfriendIds: " + friendIds);
                        if (friendIds != null && !friendIds.isEmpty()) {
                            // 🔸一時的なリストを用意！
                            List<Friend> tempList = new ArrayList<>();

                            // 🔸カウンター（何人読み終えたか）
                            final int[] count = {0};
                            Log.d("FriendList完成", "tempList.size() = " + tempList.size());
                            int total = friendIds.size();
                            for (String friendId : friendIds) {
                                db.collection("users").document(friendId)
                                        .get()
                                        .addOnSuccessListener(userSnap -> {
                                            Log.d("FriendList", "ユーザーデータ: " + userSnap.getData());
                                            String name = userSnap.getString("name");
                                            Log.d("FriendList", "フレンド取得成功: " + name);
                                            tempList.add(new Friend(friendId, friendId, name));
                                            count[0]++;

                                            if (count[0] == total) {
                                                Log.d("FriendList完成", "全員分完了。tempList.size() = " + tempList.size());
                                                friendList.clear();
                                                friendList.addAll(tempList);
                                                adapter.notifyDataSetChanged();
                                                for (Friend f : tempList) {
                                                    Log.d("FriendList確認", "追加する: " + f.getName() + " / id: " + f.getUid());
                                                }

                                                Log.d("FriendList", "name取得: " + name + " / id: " + friendId);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FriendList", "ユーザーデータ取得失敗: friendId=" + friendId, e);
                                            count[0]++;
                                            if (count[0] == total) {
                                                friendList.clear();
                                                friendList.addAll(tempList);
                                                for (Friend f : tempList) {
                                                    Log.d("FriendList", "追加予定: uid=" + f.getUid() + ", id=" + f.getId() + ", name=" + f.getName());
                                                }

                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                            }
                        }else {
                            Log.d("FriendList", "フレンドリストが空です");
                        }
                    }else {
                        Log.d("FriendList", "ドキュメントが存在しません");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendList", "読み込み失敗", e);
                });
    }
    private void searchUserById(String userIdInput) {
        db.collection("users")
                .whereEqualTo("email", userIdInput) // または "name" でも可
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        foundUserId = document.getId();
                        String name = document.getString("name");
                        textResult.setText("見つかりました: " + name);
                        buttonAddFriend.setVisibility(View.VISIBLE);
                    } else {
                        textResult.setText("ユーザーが見つかりません");
                        buttonAddFriend.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    textResult.setText("検索失敗: " + e.getMessage());
                });
    }

    private void addFriend(String friendUserId) {
        String currentUserId = auth.getCurrentUser().getUid();

        // 自分のフレンドリストに相手を追加
        db.collection("friends").document(currentUserId)
                .update("friendIds", FieldValue.arrayUnion(friendUserId))
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "フレンド追加しました", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // まだドキュメントが無ければ新規作成
                    Map<String, Object> data = new HashMap<>();
                    data.put("friendIds", Arrays.asList(friendUserId));
                    db.collection("friends").document(currentUserId).set(data);
                    Toast.makeText(this, "フレンド追加しました（初回）", Toast.LENGTH_SHORT).show();
                });
    }

    public void onBackButtonClick(View view) {

        Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);
        startActivity(intent);
        finish();
    }
    private String getChatRoomId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) return null; // 安全対策
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }


    private class SearchUserActivity {
    }
}

