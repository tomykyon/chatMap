package com.websarva.wings.android.chatmap;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.os.Looper;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;



public class GroupChatActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";
    private static final String TAG = "GroupChatActivity";
    public static final String EXTRA_GROUP_NAME = "EXTRA_GROUP_NAME";


    private String groupId;
    private String groupName;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    private TextView textViewGroupInfo;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isLocationEnabled = true;
    private Switch switchLocation;
    private Object longitude;
    private Object latitude;
    private double currentLatitude;
    private double currentLongitude;
    private Object currentUserId;
    private Friend documentReference;
    private ResourceBundle snapshot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);
        Log.d("GroupChatActivity", "受け取ったgroupId: " + getIntent().getStringExtra(EXTRA_GROUP_ID));

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        TextView textViewGroupName = findViewById(R.id.textViewGroupName);
        ImageButton buttonEditGroupName = findViewById(R.id.buttonEditGroupName);

        db = FirebaseFirestore.getInstance();
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);

// Firestoreからグループ名取得
        db.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("name");
                        textViewGroupName.setText(name != null ? name : "グループ名未設定");
                    }
                });

// 編集ボタン処理
        buttonEditGroupName.setOnClickListener(v -> {
            EditText editText = new EditText(this);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("グループ名を変更")
                    .setView(editText)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newName = editText.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            db.collection("groups").document(groupId)
                                    .update("name", newName)
                                    .addOnSuccessListener(aVoid -> {
                                        textViewGroupName.setText(newName);
                                        Toast.makeText(this, "グループ名を更新しました", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "更新に失敗しました", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
        });


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        String currentUserId = mAuth.getCurrentUser().getUid();

        switchLocation = findViewById(R.id.switch_location);
        isLocationEnabled = switchLocation.isChecked();
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isLocationEnabled = isChecked;
            Log.d(TAG, "位置情報送信: " + isChecked);
        });

        TextView roomCodeTextView = findViewById(R.id.textViewRoomCode); // 表示用TextView

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        String groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME); // 任意：UIから取得する場合も可
        if (groupName == null) groupName = "新しいグループ";

        setTitle(groupName);

        if (groupId != null) {
            // 🔁 既存のグループに参加
            db.collection("groups").document(groupId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String roomCode = documentSnapshot.getString("roomCode");
                            roomCodeTextView.setText("ルームコード: " + roomCode);
                            listenForGroupMessages();
                        }
                    });
        }else{
            String generatedRoomCode = generateRoomCode();

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("roomCode", generatedRoomCode);
            groupData.put("createdAt", FieldValue.serverTimestamp());
            groupData.put("createdBy", currentUserId);

            db.collection("groups").document(groupId)
                    .set(groupData)
                    .addOnSuccessListener(unused -> {
                        roomCodeTextView.setText("ルームコード: " + generatedRoomCode);
                        listenForGroupMessages();
                        Log.d(TAG, "新しいグループとルームコードを作成しました");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "グループ作成に失敗しました", e);
                    });
        }

        // 以下はチャット画面の初期化（そのままでOK）
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                getCurrentLocationAndSendMessage(messageText);
            }
        });

    }

    private void createNewGroup(List<String> memberIds, String groupName) {
        String roomCode = generateRoomCode(); // 4桁のコードを生成
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("members", memberIds);
        groupData.put("name", groupName);
        groupData.put("roomCode", roomCode); // 🔴← これをFirestoreに保存！

        FirebaseFirestore.getInstance().collection("groups")
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("GroupChatActivity", "グループ作成成功: " + documentReference.getId());
                    String newGroupId = documentReference.getId();

                    // グループチャット画面へ遷移
                    Intent intent = new Intent(GroupChatActivity.this, GroupChatActivity.class);
                    intent.putExtra("groupId", newGroupId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupChatActivity", "グループ作成失敗", e);
                    Toast.makeText(this, "グループ作成に失敗しました", Toast.LENGTH_SHORT).show();
                });
    }



    private String generateRoomCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 1000〜9999の4桁コード
        return String.valueOf(code);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // パーミッションが許可されたら再度位置取得
                String messageText = editTextMessage.getText().toString().trim();
                if (!messageText.isEmpty()) {
                    getCurrentLocationAndSendMessage(messageText);
                }
            } else {
                Toast.makeText(this, "位置情報の権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // メッセージをFirestoreに保存するよ

    // Firestoreからメッセージを受け取ってリストに追加するよ
    private void listenForGroupMessages() {

        db.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "メッセージがまだありません");
                        return;
                    }
                    messageList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String sender = doc.getString("senderId");
                        String text = doc.getString("text");

                        Object timestampObj = doc.get("timestamp");
                        long ts = 0;

                        if (timestampObj instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) timestampObj;
                            ts = timestamp.toDate().getTime();
                        } else if (timestampObj instanceof Long) {
                            ts = (Long) timestampObj;
                        } else {
                            // timestampがnullか型不明なら現在時刻や0を入れるなど対処
                            ts = 0;
                        }
                        // 位置情報の取得
                        double latitude = doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0.0;
                        double longitude = doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0.0;

                        messageList.add(new Message(text, sender, ts, latitude, longitude));


                    }
                    messageAdapter.notifyDataSetChanged();
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                });
    }

    private String getChatRoomId(String userId1, String userId2) {
        // 並び順を固定（辞書順でソート）
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();

        if (text.isEmpty() && currentLatitude == 0.0 && currentLongitude == 0.0) {
            Toast.makeText(this, "メッセージまたは位置情報を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long timestampMillis = System.currentTimeMillis();

        Message message = new Message(text, senderId, timestampMillis, currentLatitude, currentLongitude);

        db.collection("groups")
                .document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Chat", "メッセージ送信成功");
                    editTextMessage.setText(""); // 成功したら入力欄をクリア
                    // 位置情報もクリアしたい場合はここで
                })
                .addOnFailureListener(e -> {
                    Log.e("Chat", "メッセージ送信失敗", e);
                });
    }


    private void getCurrentLocationAndSendMessage(String messageText) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(1);

        if (!isLocationEnabled) {
            sendMessageWithLocation(messageText, 0.0, 0.0);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    sendMessageWithLocation(messageText, location.getLatitude(), location.getLongitude());
                } else {
                    sendMessageWithLocation(messageText, 0.0, 0.0);
                }
            }
        }, Looper.getMainLooper());
    }
    private void sendMessageWithLocation(String messageText, double latitude, double longitude) {
        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();


        Map<String, Object> message = new HashMap<>();
        message.put("senderId", userId);
        message.put("text", messageText);
        message.put("timestamp", FieldValue.serverTimestamp());
        message.put("latitude", latitude);
        message.put("longitude", longitude);
        Log.d(TAG, "送信する位置情報: lat=" + latitude + ", lng=" + longitude);



        db.collection("groups")
                .document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "メッセージ送信成功: " + doc.getId());
                    editTextMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "メッセージ送信失敗", e);
                    Toast.makeText(this, "送信失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();



                });
    }





}