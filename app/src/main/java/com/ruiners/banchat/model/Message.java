package com.ruiners.banchat.model;

import java.util.Date;

public class Message {
    private final long id;
    private final String message;
    private final long room;
    private final long timestamp;

    public Message(String message, long room) {
        this.id = 0;
        this.message = message;
        this.room = room;
        this.timestamp = new Date().getTime();
    }

    public Message(long id, String message, long room, long timestamp) {
        this.id = id;
        this.message = message;
        this.room = room;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public long getRoom() {
        return room;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return message;
    }
}