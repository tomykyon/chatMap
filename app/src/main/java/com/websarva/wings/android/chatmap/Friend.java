package com.websarva.wings.android.chatmap;

public class Friend {
    private String uid;
    private String id;
    private String name;
    private boolean isChecked;
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }



    public Friend(String uid, String id,String name) {
        this.uid = uid;
        this.id = id;
        this.name = name;
        }

    public String getUid() {
        return uid;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
