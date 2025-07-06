package com.websarva.wings.android.chatmap;


import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;


public class Message {


    private String text;
    private String senderId;
    @ServerTimestamp
    private Date timestamp;
    private double latitude;
    private double longitude;





    public Message() {
    }


    public Message(String text, String senderId, Date timestamp, double latitude, double longitude) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public Message(String text, String senderId, long timestampMillis, double latitude, double longitude) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = new Date(timestampMillis);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getText() {
        return text;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @ServerTimestamp
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    // 既存のコードの下に追加
    public Message(String senderId, String text, long timestampMillis) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = new Date(timestampMillis);
        this.latitude = 0.0;
        this.longitude = 0.0;
    }


}
