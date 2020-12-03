package Util;

import java.util.*;

public class Group {
    public List<User> users;
    String nme;

    public Group(String nme) {
        users = new ArrayList<>();
        this.nme = nme;
    }

    public void addUser(User usr) {
        users.add(usr);
    }

    public List<User> getUsers() {
        return this.users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getNme() {
        return this.nme;
    }

    public void setNme(String nme) {
        this.nme = nme;
    }

}
