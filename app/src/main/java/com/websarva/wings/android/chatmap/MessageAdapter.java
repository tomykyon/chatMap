package com.websarva.wings.android.chatmap;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth; // 現在のユーザーID取得のため

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>  {

    // メッセージのビュータイプを定義
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;      // 送信メッセージ用レイアウト
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2; // 受信メッセージ用レイアウト

    private List<Message> messageList = new ArrayList<>();
    private String currentUserId; // 現在ログインしている// ユーザーのUID

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        // アダプター作成時に現在のユーザーIDを取得しておく
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            // エラーハンドリング: ユーザーがログインしていない場合
            //this.currentUserId = ""; // または適切なデフォルト値
            throw new IllegalStateException("MessageAdapter: FirebaseAuth.getCurrentUser() is null.ユーザーがログインしていません。");
        }
    }

    public MessageAdapter(List<Message> messageList, String uid) {
        this.messageList = messageList;
        this.currentUserId = uid;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        String senderId = message.getSenderId();

        Log.d("MessageAdapter", "Message SenderId: " + senderId+", currentUserId: "+currentUserId);
        // メッセージの送信者IDが現在のユーザーIDと一致すれば「送信」、そうでなければ「受信」

        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (message.getText() != null && !message.getText().isEmpty()) {
            holder.messageText.setText(message.getText());
            holder.messageText.setVisibility(View.VISIBLE);
        } else {
            holder.messageText.setVisibility(View.GONE);
        }

        double lat = message.getLatitude();
        double lng = message.getLongitude();

        // 緯度経度が有効な場合だけ「現在地を表示」リンクを出す
        if (lat != 0 && lng != 0) {
            String mapUrl = "https://www.google.com/maps?q=" + lat + "," + lng;

            holder.locationTextView.setVisibility(View.VISIBLE);
            holder.locationTextView.setText("現在地を表示");
            holder.locationTextView.setTextColor(android.graphics.Color.BLUE);
            holder.locationTextView.setPaintFlags(
                    holder.locationTextView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG
            );

            holder.locationTextView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mapUrl));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.locationTextView.setVisibility(View.GONE); // 緯度経度がなければ非表示
        }

        // タイムスタンプを整形して表示
        if (message.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.timestamp.setText(sdf.format(message.getTimestamp()));
        } else {
            holder.timestamp.setText(""); // タイムスタンプがない場合
        }
        String senderId = message.getSenderId();
        if (senderId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String senderName = documentSnapshot.getString("name");
                            holder.senderNameTextView.setText(senderName != null ? senderName : "名無し");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MessageAdapter", "名前取得失敗: " + e.getMessage());
                        holder.senderNameTextView.setText("取得失敗");
                    });
        }
    }

    @Override
    public int getItemCount() {
        return(messageList != null) ? messageList.size():0;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public  TextView locationTextView;
        public TextView messageText;
        public TextView timestamp;
        public TextView senderNameTextView;


        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textViewMessageText);
            timestamp = itemView.findViewById(R.id.textViewTimestamp);
            locationTextView = itemView.findViewById(R.id.textViewLocation);
            senderNameTextView = itemView.findViewById(R.id.text_sender_name);
        }
    }

    // メッセージリスト全体を更新するメソッド (リアルタイムリスナーで使う)
    public void setMessages(List<Message> messages) {
        this.messageList = messages;
        notifyDataSetChanged(); // データ全体が変更されたことをアダプターに通知し、再描画を促す
    }





}