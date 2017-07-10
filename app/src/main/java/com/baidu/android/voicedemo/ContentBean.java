package com.baidu.android.voicedemo;

/**
 * Created by win on 2017/7/5.
 */

public class ContentBean {
    String key;
    String info;
    String loc;
    String userid;

//    String CreateTime ;

    public ContentBean(String key,String info,String loc,String userid) {
        this.key=key;
        this.info=info;
        this.loc=loc;
        this.userid=userid;

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @Override
    public String toString() {
        return "ContentBean{" +
                "key='" + key + '\'' +
                ", info='" + info + '\'' +
                ", loc='" + loc + '\'' +
                ", userid='" + userid + '\'' +
                '}';
    }
}
