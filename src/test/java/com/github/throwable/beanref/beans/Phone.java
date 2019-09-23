package com.github.throwable.beanref.beans;

public class Phone {
    public enum Type {
        mobile, home, work
    }
    private Type type;
    private String phone;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
