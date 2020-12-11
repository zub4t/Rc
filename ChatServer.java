import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.nio.charset.*;

public class ChatServer implements Runnable {
    private final int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(256);
    public static List<Group> rooms = new ArrayList<>();
    public static List<User> userNotAssociatedWithGroup = new ArrayList<>();
    public static List<User> allUserLoggedIn = new ArrayList<>();
    public static Map<String, Group> map_group = new TreeMap<>();
    public static Map<String, User> map_user = new TreeMap<>();
    static private final Charset charset = Charset.forName("ISO_8859_1");
    static private final CharsetDecoder decoder = charset.newDecoder();

    ChatServer(int port) throws IOException {
        this.port = port;
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(port));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();
        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Server starting on port " + this.port);
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.ssc.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isAcceptable())
                        this.handleAccept(key);
                    if (key.isReadable()) {
                        this.handleRead(key);
                    }

                }
            }
        } catch (IOException e) {
            System.out.println("IOException, server of port " + this.port + " terminating. Stack trace:");
            e.printStackTrace();
        }
    }

    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("".getBytes(StandardCharsets.ISO_8859_1));

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        String address = (new StringBuilder(sc.socket().getInetAddress().toString())).append(":")
                .append(sc.socket().getPort()).toString();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, address);
        sc.write(welcomeBuf);
        welcomeBuf.rewind();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        buf.clear();
        try {
            int bytesRead = ch.read(buf);
            if (bytesRead != -1) {
                buf.flip();
                String message = "";
                while (buf.hasRemaining()) {
                    byte array[] = new byte[1];
                    array[0] = buf.get();
                    char c = (char) array[0];
                    ByteBuffer teste = ByteBuffer.wrap(array);
                    message += new String(array, StandardCharsets.ISO_8859_1);

                    if (c == '\n') {
                        System.out.println(message);
                        sendM(message, ch);
                        message = "";
                    }
                }

                while (message.length() - 1 >= 0 && message.charAt(message.length() - 1) != '\n') {
                    buf.clear();
                    bytesRead = ch.read(buf);
                    buf.flip();
                    while (buf.hasRemaining()) {
                        byte array[] = new byte[1];
                        array[0] = buf.get();
                        char c = (char) array[0];
                        ByteBuffer teste = ByteBuffer.wrap(array);
                        message += new String(array, StandardCharsets.ISO_8859_1);
                    }

                }
                if (!message.equals(""))
                    sendM(message, ch);
                buf.clear();
            }

        } catch (Exception e) {
            User u = getUser(ch);
            if (u != null) {
                Group grupo = ChatServer.map_group.get(u.getRoom());
                if (grupo != null) {
                    try {
                        grupo.users.remove(u);
                        UtilC.sendMessageToGroup(grupo, "LEFT " + u.getNick() + "\n");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
                ChatServer.allUserLoggedIn.remove(u);

            }
        }

    }

    public void sendM(String message, SocketChannel ch) {
        String usr_nick = "annonymous";
        String usr_room = "";
        User u = getUser(ch);
        if (u != null) {
            usr_nick = u.getNick();
            usr_room = u.getRoom();
        }
        Message m = UtilC.makeMessage(message, usr_nick, usr_room);
        try {
            UtilC.message_handler(m, ch);
            message = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User getUser(SocketChannel ch) {
        User u = null;
        for (User user : allUserLoggedIn) {
            if (user.getCh().equals(ch)) {
                u = user;
                break;
            }

        }
        return u;
    }

    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(Integer.parseInt(args[0]));
        // ChatServer server = new ChatServer(8000);
        (new Thread(server)).start();
    }
}

/// -----------------------------------------
/*
 * classes custom para melhorar na compreensão
 */
/// -----------------------------------------

class UtilC {

    final static String regex_join = "^(\\/join)( )+(.+)";
    final static String regex_priv = "^(\\/priv)( )+(.+)( )+(.+)+";
    final static String regex_nick = "^(\\/nick)( )+(.+)";
    final static String regex_leave = "^(\\/leave)";
    final static String regex_bye = "^(\\/bye)";
    final static String regex_listu = "^(\\/listu)";
    final static String regex_listr = "^(\\/listr)";
    final static DateFormat dateFormat = new SimpleDateFormat("hh:mm");

    public static void message_handler(Message message, SocketChannel ch) throws Exception {
        if (message.getBody() != null)
            message.setBody(message.getBody().replaceAll("\n", ""));
        User user;
        Group grupo;
        switch (message.getHeader().getType()) {
            case NICK:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                boolean isValid = true;
                for (User usr : ChatServer.allUserLoggedIn) {
                    if (usr.getNick().equals(message.getBody())) {
                        isValid = false;
                        break;
                    }
                }
                if (isValid) {
                    ch.write(ByteBuffer.wrap(("OK\n").getBytes(StandardCharsets.ISO_8859_1)));
                    Thread.sleep(10);
                    if (user != null) {
                        grupo = ChatServer.map_group.get(user.getRoom());
                        if (grupo != null) {
                            sendMessageToGroup(grupo, "NEWNICK " + user.getNick() + " " + message.getBody() + "\n");
                        }
                        ChatServer.map_user.remove(user.getNick());
                        user.setNick(message.getBody());
                        ChatServer.map_user.put(user.getNick(), user);

                    } else {
                        User usr = new User();
                        usr.setNick(message.getBody());
                        usr.setCh(ch);
                        usr.setRoom("");
                        ChatServer.userNotAssociatedWithGroup.add(usr);
                        ChatServer.allUserLoggedIn.add(usr);
                        ChatServer.map_user.put(usr.getNick(), usr);

                    }
                } else {
                    ch.write(ByteBuffer.wrap(("ERROR\n").getBytes(StandardCharsets.ISO_8859_1)));
                }
                break;
            case LISTU:
                String usrs = "";
                for (User usr : ChatServer.allUserLoggedIn) {
                    usrs += usr.getNick() + " - " + usr.getRoom() + "\n";
                }
                ch.write(ByteBuffer.wrap(("List of all users\n" + usrs).getBytes(StandardCharsets.ISO_8859_1)));
                break;
            case LISTR:
                String grps = "";
                for (Group g : ChatServer.rooms) {
                    grps += g.getNme() + " - " + g.users.size() + "\n";
                }
                ch.write(ByteBuffer.wrap(("List of all groups\n" + grps).getBytes(StandardCharsets.ISO_8859_1)));
                break;
            case BYE:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                if (user != null) {
                    grupo = ChatServer.map_group.get(user.getRoom());
                    if (grupo != null) {
                        grupo.users.remove(user);
                        sendMessageToGroup(grupo, "LEFT " + user.getNick() + "\n");
                    }
                    ChatServer.allUserLoggedIn.remove(user);

                }
                Thread.sleep(10);
                ch.write(ByteBuffer.wrap(("BYE\n").getBytes(StandardCharsets.ISO_8859_1)));
                Thread.sleep(10);
                user.getCh().close();
                break;

            case JOIN:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                java.util.List<Group> grupos = ChatServer.rooms;
                Group aux_g = null;
                for (Group grp : grupos) {
                    if (grp.getNme().equals(message.getBody())) {
                        aux_g = grp;
                    }
                }
                if (aux_g == null) {
                    Group grp = new Group(message.getBody());
                    ChatServer.rooms.add(grp);
                    ChatServer.map_group.put(grp.getNme(), grp);
                    aux_g = grp;
                }
                if (user != null) {
                    grupo = ChatServer.map_group.get(user.getRoom());
                    if (grupo != null) {
                        grupo.users.remove(user);
                        sendMessageToGroup(grupo, "LEFT " + user.getNick() + "\n");
                        Thread.sleep(10);
                        user.setRoom(aux_g.getNme());

                    } else {
                        user.setRoom(aux_g.getNme());
                        ChatServer.userNotAssociatedWithGroup.remove(user);
                    }
                    ch.write(ByteBuffer.wrap(("OK\n").getBytes(StandardCharsets.ISO_8859_1)));
                    Thread.sleep(10);
                    sendMessageToGroup(aux_g, "JOINED " + user.getNick() + "\n");
                    aux_g.addUser(user);

                } else {
                    ch.write(ByteBuffer.wrap(("ERROR\n").getBytes(StandardCharsets.ISO_8859_1)));

                }

                break;
            case LEAVE:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                grupo = ChatServer.map_group.get(user.getRoom());
                ch.write(ByteBuffer.wrap(("OK\n".getBytes(StandardCharsets.ISO_8859_1))));
                if (grupo != null) {
                    grupo.users.remove(user);
                    ChatServer.userNotAssociatedWithGroup.add(user);
                    sendMessageToGroup(grupo, "LEFT " + user.getNick() + "\n");
                    user.setRoom("");
                }
                break;
            case PRIV:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                User priv_u = ChatServer.map_user.get(message.getHeader().getReceiver());
                if (user != null && priv_u != null) {
                    ch.write(ByteBuffer.wrap(("OK\n").getBytes(StandardCharsets.ISO_8859_1)));
                    Thread.sleep(10);
                    priv_u.getCh().write(ByteBuffer.wrap(("PRIVATE " + user.getNick() + " " + message.getBody() + "\n")
                            .getBytes(StandardCharsets.ISO_8859_1)));
                } else {
                    ch.write(ByteBuffer.wrap(("ERROR\n").getBytes(StandardCharsets.ISO_8859_1)));

                }
                break;

            case NORMAL:
                grupo = ChatServer.map_group.get(message.getHeader().getReceiver());
                user = ChatServer.map_user.get(message.getHeader().getSender());
                if (grupo != null) {
                    sendMessageToGroup(grupo, "MESSAGE " + user.getNick() + " " + message.getBody() + "\n");

                } else {
                    ch.write(ByteBuffer.wrap(("ERROR\n").getBytes(StandardCharsets.ISO_8859_1)));

                }
                break;
        }

    }

    public static void sendMessageToGroup(Group grupo, String message) throws Exception {
        for (User usr : grupo.getUsers()) {
            SocketChannel ch = usr.getCh();
            ch.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.ISO_8859_1)));

        }
    }

    public boolean isTaskType(Message message) {
        boolean flag = false;
        if (message.getHeader().getType() == MessageType.JOIN || message.getHeader().getType() == MessageType.BYE
                || message.getHeader().getType() == MessageType.LEAVE
                || message.getHeader().getType() == MessageType.NICK)
            flag = true;

        return flag;
    }

    public static Message makeMessage(String m, String sender, String receiver) {
        Pattern pattern = null;
        Matcher matcher = null;
        Message message = new Message();
        MessageHeader header = new MessageHeader();
        header.setSender(sender);
        header.setReceiver(receiver);
        header.setTimestamp(System.currentTimeMillis());
        pattern = Pattern.compile(regex_nick, Pattern.MULTILINE);
        matcher = pattern.matcher(m);
        if (matcher.find()) {
            header.setType(MessageType.NICK);
            message.setBody(matcher.group(3));

        } else {

            pattern = Pattern.compile(regex_join, Pattern.MULTILINE);
            matcher = pattern.matcher(m);
            if (matcher.find()) {
                header.setType(MessageType.JOIN);
                message.setBody(matcher.group(3));
            } else {
                pattern = Pattern.compile(regex_leave, Pattern.MULTILINE);
                matcher = pattern.matcher(m);
                if (matcher.find()) {
                    header.setType(MessageType.LEAVE);
                } else {
                    pattern = Pattern.compile(regex_bye, Pattern.MULTILINE);
                    matcher = pattern.matcher(m);
                    if (matcher.find()) {
                        header.setType(MessageType.BYE);

                    } else {
                        pattern = Pattern.compile(regex_listu, Pattern.MULTILINE);
                        matcher = pattern.matcher(m);
                        if (matcher.find()) {
                            header.setType(MessageType.LISTU);

                        } else {
                            pattern = Pattern.compile(regex_listr, Pattern.MULTILINE);
                            matcher = pattern.matcher(m);
                            if (matcher.find()) {
                                header.setType(MessageType.LISTR);

                            } else {

                                pattern = Pattern.compile(regex_priv, Pattern.MULTILINE);
                                matcher = pattern.matcher(m);
                                if (matcher.find()) {
                                    header.setType(MessageType.PRIV);
                                    message.setBody(matcher.group(5));
                                    header.setReceiver(matcher.group(3));

                                } else {
                                    header.setType(MessageType.NORMAL);
                                    m = m.replaceFirst("/", "");
                                    message.setBody(m);
                                }

                            }

                        }
                    }
                }
            }

        }
        message.setHeader(header);
        return message;
    }

}

class User {
    private SocketChannel ch;
    private String nick;
    private String room;
    private String buffer = "";

    public String getBuffer() {
        return this.buffer;
    }

    public void setBuffer(String buffer) {
        this.buffer = buffer;
    }

    public User() {

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

class Group {
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

enum MessageType {
    NORMAL(3, "Chat individual"), JOIN(4, "JOIN"), NICK(5, "NICK"), LEAVE(6, "LEAVE"), BYE(7, "BYE"), LISTR(8, "LISTR"),
    LISTU(9, "LISTU"), PRIV(10, "PRIV");

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

class MessageHeader {

    private String sender;
    private String receiver;
    private MessageType type;
    private Long timestamp;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}

class Message {
    private MessageHeader header;
    private String body;

    public MessageHeader getHeader() {
        return header;
    }

    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
