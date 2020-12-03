package Util;

public enum MessageType {
    LOGIN(1, "Conecte-se"), LOGOUT(2, "Sair"), NORMAL(3, "Chat individual"), BROADCAST(4, "Grupo"), JOIN(4, "JOIN"),
    NICK(5, "NICK"), LEAVE(6, "LEAVE"), BYE(7, "BYE"), LISTR(8, "LISTR"), LISTU(9, "LISTU");

    private int code;
    private String desc;

    MessageType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}