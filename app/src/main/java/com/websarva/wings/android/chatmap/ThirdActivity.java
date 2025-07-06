package com.websarva.wings.android.chatmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;

public class ThirdActivity extends AppCompatActivity {


    private View logout;
    private FirebaseAuth mAuth;
    private Intent intent;
    private FirebaseFirestore db;
    private int position;
    private ChatItem chat;
    private String friendId;
    private String friendName;
    private Friend friend;
    private Object friendMap;
    private boolean isGroup;
    private static final int REQUEST_IMAGE_PERMISSION = 200;
    private ImageView userIcon;
    private ActivityResultLauncher<Intent> imagePickLauncher;
    private static final int REQUEST_IMAGE_PICK = 1002;





    public class ChatItem {
        private final boolean isGroup;
        private int userIcon;
        private String userName;
        private String lastMessage;
        private String timestamp;
        private int unreadCount;
        private String friendId;

        private FirebaseAuth mAuth; // FirebaseAuthインスタンスを保持する変数
        private Button logoutButton; // ログアウトボタンの変数
        private String chatRoomId;


        public ChatItem(int userIcon, String userName, String lastMessage, String timestamp, int unreadCount, String chatRoomId, String friendId,boolean isGroup) {
            this.userIcon = userIcon;
            this.userName = userName;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.unreadCount = unreadCount;
            this.chatRoomId = chatRoomId;
            this.friendId = friendId;
            this.isGroup = isGroup;

        }
        public boolean isGroup() {
            return isGroup;
        }


        public String getFriendName() {
            return userName;
        }

        public String getFriendId() {
            return friendId;
        }

        public String getChatRoomId() {
            return chatRoomId;
        }

        // Getterメソッド
        public int getUserIcon() {
            return userIcon;
        }

        public String getUserName() {
            return userName;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getUnreadCount() {
            return unreadCount;
        }

        // Setterメソッド (必要に応じて)
        public void setUserIcon(int userIcon) {
            this.userIcon = userIcon;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setLastMessage(String lastMessage) {
            this.lastMessage = lastMessage;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setUnreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
        }
    }
    public class GroupItem {
        private String groupId;
        private String groupName;

        public GroupItem(String groupId, String groupName) {
            this.groupId = groupId;
            this.groupName = groupName;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getGroupName() {
            return groupName;
        }
    }


    public class ChatListAdapter extends BaseAdapter {


        private Context context;
        private List<ChatItem> chatItems;
        private LayoutInflater inflater;
        private RecyclerView groupRecyclerView;
        private List<GroupItem> groupList = new ArrayList<>();


        public ChatListAdapter(Context context, List<ChatItem> chatItems) {
            this.context = context;
            this.chatItems = chatItems;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return chatItems.size();
        }

        @Override
        public Object getItem(int position) {
            return chatItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;


            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_chat, parent, false);
                holder = new ViewHolder();
                holder.userIcon = convertView.findViewById(R.id.imageViewUserIcon);
                holder.userName = convertView.findViewById(R.id.textViewUserName);
                holder.lastMessage = convertView.findViewById(R.id.textViewLastMessage);
                holder.timestamp = convertView.findViewById(R.id.textViewTimestamp);
                holder.unreadCount = convertView.findViewById(R.id.textViewUnreadCount);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ChatItem currentItem = chatItems.get(position);

            holder.userIcon.setImageResource(currentItem.getUserIcon());
            holder.userName.setText(currentItem.getUserName());
            holder.lastMessage.setText(currentItem.getLastMessage());
            holder.timestamp.setText(currentItem.getTimestamp());

            if (currentItem.getUnreadCount() > 0) {
                holder.unreadCount.setText(String.valueOf(currentItem.getUnreadCount()));
                holder.unreadCount.setVisibility(View.VISIBLE);
            } else {
                holder.unreadCount.setVisibility(View.GONE);
            }

            return convertView;

        }


        // ViewHolderパターン
        private class ViewHolder {
            ImageView userIcon;
            TextView userName;
            TextView lastMessage;
            TextView timestamp;
            TextView unreadCount;
        }

    }


    private ListView listViewChats;
    private ChatListAdapter chatListAdapter;
    private List<ChatItem> chatItemList;

    private ImageView imageViewZoom;
    private String chatRoomId;




    private String generateChatRoomId(String userId1, String userId2) {
        return (userId1.compareTo(userId2) < 0) ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }
    private void loadFriendData(String friendId, String currentUserId) {
        db.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener(friendDoc -> {
                    String friendName = friendDoc.getString("name");
                    String chatRoomId = generateChatRoomId(currentUserId, friendId);

                    db.collection("chats")
                            .document(chatRoomId)
                            .collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(messageSnapshots -> {
                                String lastMessage = "";
                                String timestamp = "";
                                if (!messageSnapshots.isEmpty()) {
                                    DocumentSnapshot lastMsg = messageSnapshots.getDocuments().get(0);
                                    lastMessage = lastMsg.getString("lastMessage");
                                    Timestamp ts = lastMsg.getTimestamp("timestamp");
                                    if (ts != null) {
                                        timestamp = ts.toDate().toString();
                                    }
                                }

                                int unreadCount = 0;

                                ChatItem item = new ChatItem(
                                        R.drawable.man,
                                        friendName,
                                        lastMessage,
                                        timestamp,
                                        unreadCount,
                                        chatRoomId,
                                        friendId,
                                        false
                                );

                                chatItemList.add(item);
                                chatListAdapter.notifyDataSetChanged();
                            });
                });
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

        chatItemList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(this, chatItemList);
        listViewChats = findViewById(R.id.listViewChats);
        listViewChats.setAdapter(chatListAdapter);

        userIcon = findViewById(R.id.userIcon);
        imageViewZoom = findViewById(R.id.imageViewZoom);
        imageViewZoom = findViewById(R.id.imageViewZoom);
        userIcon = findViewById(R.id.userIcon);
        userIcon.setOnClickListener(v -> {
            imageViewZoom.setImageDrawable(userIcon.getDrawable());
            imageViewZoom.setVisibility(View.VISIBLE);
        });
        imageViewZoom.setOnClickListener(v -> {
            imageViewZoom.setVisibility(View.GONE);
        });
        imagePickLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            showImagePreviewDialog(selectedImageUri);
                        }
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }




        // 拡大画像をタップ → 閉じる
        imageViewZoom.setOnClickListener(v -> imageViewZoom.setVisibility(View.GONE));

        // 長押し → 画像選択
        userIcon.setOnLongClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            openGallery(intent);  // intent を渡す
            return true;
        });

        logout = findViewById(R.id.logout);
        mAuth = FirebaseAuth.getInstance();

        FloatingActionButton fabQRScan = findViewById(R.id.fab);
        fabQRScan.setOnClickListener(v -> {
            Intent intent = new Intent(ThirdActivity.this, SecondActivity.class);
            startActivity(intent);
        });

        listViewChats.setOnItemClickListener((parent, view, position, id) -> {
            ChatItem selectedItem = chatItemList.get(position);
            if (selectedItem.isGroup()) {
                Intent groupIntent = new Intent(ThirdActivity.this, GroupChatActivity.class);
                groupIntent.putExtra("groupId", selectedItem.getChatRoomId());
                startActivity(groupIntent);
            } else {
                Intent chatIntent = new Intent(ThirdActivity.this, ChatActivity.class);
                chatIntent.putExtra("chatRoomId", selectedItem.getChatRoomId());
                chatIntent.putExtra("friendId", selectedItem.getFriendId());
                chatIntent.putExtra("friendName", selectedItem.getFriendName());
                startActivity(chatIntent);
            }
        });

        logout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ThirdActivity.this, "ログアウトしました", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ThirdActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        db = FirebaseFirestore.getInstance();
        final String currentUserId = mAuth.getCurrentUser().getUid();

        // フレンド一覧の取得
        db.collection("friends")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> friendIds = (List<String>) documentSnapshot.get("friendIds");
                        if (friendIds != null) {
                            for (String friendId : friendIds) {
                                if (friendId != null && !friendId.trim().isEmpty()) {
                                    loadFriendData(friendId, currentUserId);
                                } else {
                                    Log.w("ThirdActivity", "無効な friendId を検出（null または空）: " + friendId);
                                }
                            }
                        }
                    }
                });

        // グループチャット一覧の取得
        db.collection("groups")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot groupDoc : queryDocumentSnapshots.getDocuments()) {
                        List<String> members = (List<String>) groupDoc.get("members");

                        if (members != null && members.contains(currentUserId)) {
                            String roomCode = groupDoc.getString("roomCode");
                            String groupId = groupDoc.getId();
                            String groupName = groupDoc.getString("name");
                            String groupNameFromDb = groupDoc.getString("name");
                            String finalGroupName = (groupNameFromDb != null && !groupNameFromDb.isEmpty())
                                    ? groupNameFromDb
                                    : "グループ: " + roomCode;


                            db.collection("chats")
                                    .document(groupId)
                                    .collection("messages")
                                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(messageSnapshots -> {
                                        String lastMessage = "";
                                        String timestamp = "";

                                        if (!messageSnapshots.isEmpty()) {
                                            DocumentSnapshot lastMsg = messageSnapshots.getDocuments().get(0);
                                            lastMessage = lastMsg.getString("message"); // ← 修正点
                                            Timestamp ts = lastMsg.getTimestamp("timestamp");
                                            if (ts != null) {
                                                timestamp = ts.toDate().toString();
                                            }
                                        }

                                        ChatItem groupItem = new ChatItem(
                                                R.drawable.woman,
                                                finalGroupName, // ← 修正点
                                                lastMessage,
                                                timestamp,
                                                0,
                                                groupId,
                                                "",
                                                true
                                        );

                                        chatItemList.add(groupItem);
                                        chatListAdapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });





// userIcon をタップすると拡大表示
        userIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewZoom.setImageDrawable(userIcon.getDrawable()); // 現在の画像を使う
                imageViewZoom.setVisibility(View.VISIBLE);
            }
        });
        imageViewZoom.setOnLongClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("image/*");
            imagePickLauncher.launch(pickIntent);
            return true;
        });
    }

    private void openGallery(Intent intent) {
        imagePickLauncher.launch(intent);
    }



    private void checkAndRequestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_IMAGE_PERMISSION);
            } else {
                launchGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_PERMISSION);
            } else {
                launchGallery();
            }
        }
    }
    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickLauncher.launch(intent);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGallery(); // 許可されたらギャラリーを開く
            } else {
                Toast.makeText(this, "画像アクセスの許可が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private void showImagePreviewDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("画像を設定しますか？");

        ImageView imageView = new ImageView(this);
        imageView.setImageURI(imageUri);
        builder.setView(imageView);

        builder.setPositiveButton("設定", (dialog, which) -> {
            userIcon.setImageURI(imageUri); // 選んだ画像を反映
        });

        builder.setNegativeButton("キャンセル", null);

        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            userIcon.setImageURI(selectedImageUri);
            if (selectedImageUri != null) {
                // ImageView などに表示
                userIcon.setImageURI(selectedImageUri);
            }
        }


        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                showImagePreviewDialog(selectedImageUri);  // プレビュー付き確認ダイアログを表示
            }
        }
    }


    public void showZoomImage(int imageResId) {
        imageViewZoom.setImageResource(imageResId);
        imageViewZoom.setVisibility(View.VISIBLE);

    }
}

