package com.baidu.android.voicedemo;

/**
 * Created by win on 2017/7/5.
 */

public class responseBean {
    String code;

    String text ;

    public responseBean(String code,String text) {
        this.code=code;
        this.text=text;

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "responseBean{" +
                "code='" + code + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
