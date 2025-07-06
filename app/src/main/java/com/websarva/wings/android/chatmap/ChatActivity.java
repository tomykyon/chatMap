package com.websarva.wings.android.chatmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.LocationRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange; // リアルタイム更新の差分検知用
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // メッセージをソートするため
import com.google.firebase.firestore.ListenerRegistration; // リスナー解除用
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.view.View;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "com.example.chatapp.EXTRA_USER_NAME";
    private TextView textViewChattingWith;
    public static final String EXTRA_CHAT_ROOM_ID = "chat_room_id";
    private EditText editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String chatRoomId; // 現在のチャットルームのID

    private ListenerRegistration chatListenerRegistration; // Firestore リスナーの登録解除用
    private FusedLocationProviderClient fusedLocationClient;
    private Switch SwitchLocation;
    private boolean isLocationEnabled = true;
    private ArrayList<Message> messageList;
    private double longitude;
    private double latitude;



    private void getCurrentLocationAndSendMessage(String messageText) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



        if (!isLocationEnabled) {
            sendMessageWithLocation(messageText, 0.0, 0.0);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    Log.d("Location", "Lat: " + lat + ", Lon: " + lon);
                    // Firestoreに送る処理などここで
                    sendMessageWithLocation(messageText, lat, lon);
                }else {
                    Log.w("Location", "Failed to get location, sending with default 0.0, 0.0");
                    sendMessageWithLocation(messageText, 0.0, 0.0);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        buttonSend = findViewById(R.id.buttonSend);
        editTextMessage = findViewById(R.id.editTextMessage);
        textViewChattingWith = findViewById(R.id.textViewChattingWith);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        SwitchLocation = findViewById(R.id.switch_location);
        TextView textViewUserName = findViewById(R.id.textViewUserName);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // フレンド情報などを Intent から受け取る
        String friendId = getIntent().getStringExtra("friendId");
        String friendName = getIntent().getStringExtra("friendName");
        chatRoomId = getIntent().getStringExtra("chatRoomId");

        Log.d("ChatActivity", "chatRoomId: " + chatRoomId + ", friendId: " + friendId);
        Log.d("ChatActivity", "受け取った friendName = " + friendName);

        if (friendId == null || chatRoomId == null) {
            Toast.makeText(this, "チャット情報が取得できませんでした", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 位置情報スイッチ初期化
        SwitchLocation.setChecked(isLocationEnabled);
        SwitchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isLocationEnabled = isChecked;
            if (isChecked) {
                Toast.makeText(this, "位置情報送信ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "位置情報送信OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // チャット相手の名前表示
        String userName = friendName;

        if (userName != null) {
            textViewUserName.setText(userName);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(userName);
            }
            textViewChattingWith.setText("チャット相手: " + userName);
        } else {
            // friendName が null なら Firestore から名前を取得
            db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String retrievedName = documentSnapshot.getString("name");
                            if (retrievedName != null) {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(retrievedName);
                                }
                                textViewUserName.setText(retrievedName);
                                textViewChattingWith.setText("チャット相手: " + retrievedName);
                            } else {
                                textViewUserName.setText("名前がありません");
                                textViewChattingWith.setText("チャット相手: 名前がありません");
                            }
                        } else {
                            textViewUserName.setText("情報が見つかりません");
                            textViewChattingWith.setText("チャット相手: 情報が見つかりません");
                        }
                    })
                    .addOnFailureListener(e -> {
                        textViewUserName.setText("取得失敗");
                        textViewChattingWith.setText("チャット相手: 取得失敗");
                        Log.e("ChatActivity", "ユーザー情報の取得に失敗", e);
                    });
        }

        // メッセージ送信用のボタン
        buttonSend.setOnClickListener(v -> sendMessage());

        // RecyclerView（メッセージ一覧）設定
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 下から順に表示
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);

        // メッセージのリアルタイム受信設定
        listenForMessages();
    }


    public void onBackButtonClick(View view) {


        // 現在のユーザーがログインしているか確認
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "ログインしていません。ログイン画面に戻ります。", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        // チャットルームIDが渡されていない場合は、エラー処理や新規チャットルーム作成のロジックが必要
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            Toast.makeText(this, "チャットルームIDが指定されていません。", Toast.LENGTH_LONG).show();
            Log.e("ChatActivity", "チャットルームIDがnullまたは空です。このActivityを終了します。");
            return;
        }

        // アクションバーのタイトル設定 (オプション)
        String chatPartnerName = getIntent().getStringExtra("friendName");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
                actionBar.setTitle(chatPartnerName);


        }
        Intent intent = new Intent(ChatActivity.this, ThirdActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // Activityが終了する際に、Firestoreリスナーを解除する（重要！）
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove(); // リスナーを解除してメモリリークを防ぐ
        }
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim(); // 前後の空白を削除
        Message message = new Message(messageText, currentUserId, new Date(), latitude, longitude);

        if (messageText.isEmpty()) {
            Toast.makeText(this, "メッセージを入力してください。", Toast.LENGTH_SHORT).show();
            return;
        }
        getCurrentLocationAndSendMessage(messageText);
    }
    private void sendMessageWithLocation(String messageText, double latitude, double longitude) {
        Message message = new Message(messageText, currentUserId, new Date(), latitude, longitude);

        db.collection("chats").document(chatRoomId).set(
                new HashMap<String, Object>() {{
                    put("lastMessage", messageText);
                    put("lastMessageTimestamp", FieldValue.serverTimestamp());
                }},
                SetOptions.merge()
        );
        db.collection("chats").document(chatRoomId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    editTextMessage.setText("");
                    Map<String, Object> chatSummary = new HashMap<>();
                    chatSummary.put("lastMessage", messageText);
                    chatSummary.put("lastMessageTimestamp", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatRoomId)
                            .set(chatSummary, SetOptions.merge()); // ←ここがポイント！
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "送信失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendMessage(); // 再試行
            } else {
                Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void listenForMessages() {
        // chatRoomId の messages サブコレクションを購読
        // タイムスタンプで昇順にソートすることで、古いメッセージから順に表示される
        chatListenerRegistration = db.collection("chats").document(chatRoomId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ChatActivity", "メッセージ受信エラー", e);
                        return;
                    }

                    if (snapshots != null) {
                        // DocumentChange を利用して、追加/変更/削除されたメッセージのみを処理する
                        // これにより、全メッセージを毎回クリアして再構築するより効率的になる
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Message message = dc.getDocument().toObject(Message.class);
                            int oldIndex = dc.getOldIndex();
                            int newIndex = dc.getNewIndex();

                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d("ChatActivity", "SnapshotListener: Message ADDED. Text: " + message.getText() + ", New Index: " + newIndex);
                                    if (newIndex >= 0 && newIndex <= messageList.size()) {
                                    messageList.add(newIndex, message);
                                    messageAdapter.notifyItemInserted(newIndex);
                                    recyclerViewMessages.scrollToPosition(messageList.size() - 1); // 一番下までスクロール
                                        Log.d("ChatActivity", "SnapshotListener: notifyItemInserted at " + newIndex + ". List size: " + messageList.size());
                                    }else{Log.w("ChatActivity", "DEBUG: Deciding if newIndex is invalid. newIndex=" + newIndex + ", list.size() at that moment=" + messageList.size());
                                        messageList.add(message);
                                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                                        Log.w("ChatActivity", "Invalid newIndex for ADDED:"+ newIndex +"current list size: " + messageList.size() + "Appended to end." );
                                    }
                                    if (newIndex >= 0 && newIndex < messageList.size()) {
                                        recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                                        Log.d("ChatActivity", "SnapshotListener: Scrolled to position " + (messageAdapter.getItemCount() - 1));
                                    }
                                    break;
                                case MODIFIED:
                                    Log.d("ChatActivity", "SnapshotListener: Message MODIFIED. Text: " + message.getText()); // ★変更をログ出力
                                    if (oldIndex != -1 && newIndex != -1 && oldIndex < messageList.size()) { // oldIndexが有効か確認
                                        if (oldIndex != newIndex) {
                                            messageList.remove(oldIndex);
                                            if (newIndex < messageList.size()) { // newIndexの境界チェック
                                                messageList.add(newIndex, message);
                                            } else {
                                                messageList.add(message); // 末尾に追加
                                            }
                                            messageAdapter.notifyItemMoved(oldIndex, newIndex);
                                        } else {
                                            messageList.set(newIndex, message);
                                            messageAdapter.notifyItemChanged(newIndex);
                                        }
                                    } else {
                                        Log.w("ChatActivity", "Invalid index for MODIFIED: old=" + oldIndex + ", new=" + newIndex);
                                        // エラー回復のため、リストをクリアして再読み込みなども検討
                                    }
                                    break;
                                case REMOVED:
                                    Log.d("ChatActivity", "SnapshotListener: Message REMOVED. Text: " + message.getText()); // ★削除をログ出力
                                    if (oldIndex != -1 && oldIndex < messageList.size()) { // oldIndexが有効か確認
                                        messageList.remove(oldIndex);
                                        messageAdapter.notifyItemRemoved(oldIndex);
                                    } else {
                                        Log.w("ChatActivity", "Invalid oldIndex for REMOVED: " + oldIndex);
                                    }
                                    break;
                            }
                        }
                    }
                });
    }

    private String getChatRoomId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }




}




