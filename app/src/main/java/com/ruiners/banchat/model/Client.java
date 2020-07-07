package com.ruiners.banchat.model;

import com.github.nkzawa.socketio.client.Socket;

public class Client {
    private long room;
    private Socket socket;

    public Client(long room) {
        this.room = room;
    }

    public long getRoom() {
        return room;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setRoom(long room) {
        this.room = room;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
