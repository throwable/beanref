package com.github.throwable.beanutil.beans;

import java.util.concurrent.atomic.AtomicReference;

public class Person {
    /** read-only */
    private String id;
    /** read-only */
    private String name;
    private int age;
    private Contact contact;
    /** wrapper type */
    private AtomicReference<Status> status = new AtomicReference<>();


    public Person() {
    }

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public AtomicReference<Status> getStatus() {
        return status;
    }
}
