package com.websarva.wings.android.chatmap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;



    public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
        private List<Friend> friendList;
        private OnFriendClickListener listener;
        private Context context;

        // コールバック用のインターフェース
        public interface OnFriendClickListener {
            void onFriendClick(Friend friend);
        }


        public FriendAdapter(Context context, List<Friend> friendList, OnFriendClickListener listener) {
            this.friendList = friendList;
            this.listener = listener;
            this.context = context;
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView nameText;
            public CompoundButton checkBox;

            public ViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.textFriendName);
                checkBox = view.findViewById(R.id.checkBox);
            }
        }

        @NonNull
        @Override
        public FriendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Friend friend = friendList.get(position);

            holder.checkBox.setChecked(friend.isChecked());
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                friend.setChecked(isChecked);
            });


            holder.nameText.setText(friend.getName());
            Log.d("FriendAdapter", "バインド中: " + friend.getName());

            // 🔽 クリックされたときの処理だけここにまとめる
            holder.itemView.setOnClickListener(v -> {
                String friendId = friend.getUid();
                String friendName = friend.getName();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // チャットルームIDを生成
                String chatRoomId = generateChatRoomId(currentUserId, friendId);

                Log.d("FriendAdapter", "生成されたchatRoomId: " + chatRoomId);

                // ChatActivityへ画面遷移
                this.context = holder.itemView.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("EXTRA_CHAT_ROOM_ID", chatRoomId);
                intent.putExtra("friendId", friendId);
                intent.putExtra("friendName", friendName);
                intent.putExtra(ChatActivity.EXTRA_USER_NAME, friendName);
                context.startActivity(intent);

                // 🔽 ここでクリック通知（もし使ってるなら）
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            });

            }

        private String generateChatRoomId(String userId1, String userId2) {
            if (userId1.compareTo(userId2) < 0) {
                return userId1 + "_" + userId2;
            } else {
                return userId2 + "_" + userId1;
            }
        }


        @Override
        public int getItemCount() {
            return friendList.size();
        }
    }


