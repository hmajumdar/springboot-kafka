package com.test.kafka.consumer.model;

public class Message {

    private static final int IMPOSSIBLE_ID = -1;

    private int id = IMPOSSIBLE_ID;
    private String identifier;
    private String description;

    public int getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
