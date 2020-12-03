package Util;

import java.nio.channels.*;
import java.awt.*;

public class User {
    private SocketChannel ch;
    private String nick;
    private String room;
    private String color;

    public User() {

    }

    public String getColor() {
        return this.color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public SocketChannel getCh() {
        return this.ch;
    }

    public void setCh(SocketChannel ch) {
        this.ch = ch;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getRoom() {
        return this.room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

}
