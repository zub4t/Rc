package Util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Cliente.ChatClient;
import Server.ChatServer;
import java.nio.*;
import java.awt.*;
import java.nio.channels.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class UtilC {

    final static String regex_join = "^(\\/join)( )+(\\w+)";
    final static String regex_nick = "^(\\/nick)( )+(\\w+)";
    final static String regex_leave = "^(\\/leave)";
    final static String regex_bye = "^(\\/bye)";
    final static String regex_listu = "^(\\/listu)";
    final static String regex_listr = "^(\\/listr)";
    final static DateFormat dateFormat = new SimpleDateFormat("hh:mm");
    public final static String serverMessageColor = "255,0,0";

    public static byte[] serialize(Message message) {

        StringBuilder str = new StringBuilder();
        str.append(";" + message.getHeader().getSender() + ";");
        str.append(message.getHeader().getReceiver() + ";");
        str.append(message.getHeader().getType() + ";");
        str.append(message.getHeader().getTimestamp() + ";");
        str.append(message.getBody() + ";");
        return str.toString().getBytes(ChatClient.charset);
    }

    public static byte[] serialize(String m, String nick, String receiver) {
        return serialize(makeMessage(m, nick, receiver));

    }

    public static Message deserialize(String m) {
        Message message = new Message();
        MessageHeader header = new MessageHeader();
        String[] aux = m.split(";");
        header.setSender(aux[1]);
        header.setReceiver(aux[2]);

        switch (aux[3]) {

            case "NORMAL":
                header.setType(MessageType.NORMAL);
                break;
            case "JOIN":
                header.setType(MessageType.JOIN);
                break;
            case "NICK":
                header.setType(MessageType.NICK);
                break;
            case "LEAVE":
                header.setType(MessageType.LEAVE);
                break;
            case "BYE":
                header.setType(MessageType.BYE);
                break;
            case "LISTR":
                header.setType(MessageType.LISTR);
                break;
            case "LISTU":
                header.setType(MessageType.LISTU);
                break;

        }
        header.setTimestamp(Long.parseLong(aux[4]));
        message.setHeader(header);
        message.setBody(aux[5]);
        return message;
    }

    public static void message_handler(Message message, SocketChannel ch) throws Exception {
        Date date = new Date(message.getHeader().getTimestamp());
        User user;
        Group grupo;

        switch (message.getHeader().getType()) {

            case NICK:
                user = ChatServer.map_user.get(message.getHeader().getSender());

                // verificar se o nick está disponivel
                boolean isValid = true;
                for (User usr : ChatServer.allUserLoggedIn) {
                    if (usr.getNick().equals(message.getBody())) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    User usr = new User();
                    usr.setNick(message.getBody());
                    usr.setCh(ch);
                    usr.setColor(generateRandomColor());
                    ChatServer.userNotAssociatedWithGroup.add(usr);
                    ChatServer.allUserLoggedIn.add(usr);
                    ChatServer.map_user.put(usr.getNick(), usr);
                    ch.write(ByteBuffer.wrap(("OK" + ";;;" + serverMessageColor).getBytes()));
                    Thread.sleep(10);
                    ch.write(ByteBuffer.wrap(("@nick:" + usr.getNick()).getBytes()));
                    Thread.sleep(10);
                    ch.write(ByteBuffer.wrap(("@color:" + usr.getColor()).getBytes()));
                    if (user != null) {

                        // user já estava logado com nick
                        grupo = ChatServer.map_group.get(user.getRoom());
                        if (grupo != null) {
                            usr.setRoom(user.getRoom());
                            // enviar messagem para os menbros da sala avisando q o maluco troco de nick
                            sendMessageToGroup(grupo,
                                    dateFormat.format(date) + " - " + user.getNick() + " mudou o nick para"
                                            + message.getBody() + "  : \n \t" + ";;;" + serverMessageColor);
                        } else {
                            ch.write(ByteBuffer.wrap((message.getBody() + " mudou o nick para" + user.getNick() + ";;;"
                                    + serverMessageColor).getBytes()));

                        }

                        ChatServer.allUserLoggedIn.remove(user);
                    }
                } else {
                    ch.write(ByteBuffer.wrap(("ERROR\n" + ";;;" + serverMessageColor).getBytes()));

                }
                break;
            case LISTU:
                String usrs = "";
                for (User usr : ChatServer.allUserLoggedIn) {
                    usrs += usr.getNick() + " - " + usr.getRoom() + "\n";
                }
                ch.write(ByteBuffer.wrap(("List of all users\n" + usrs + ";;;" + serverMessageColor).getBytes()));
                break;
            case LISTR:
                String grps = "";
                for (Group g : ChatServer.rooms) {
                    grps += g.getNme() + " - " + g.users.size() + "\n";
                }
                ch.write(ByteBuffer.wrap(("List of all groups\n" + grps + ";;;" + serverMessageColor).getBytes()));
                break;
            case BYE:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                grupo = ChatServer.map_group.get(user.getRoom());
                if (grupo != null) {
                    grupo.users.remove(user);
                }
                ChatServer.allUserLoggedIn.remove(user);

                user.getCh().close();
                break;

            case JOIN:
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

                User aux_u = null;
                for (User usr : ChatServer.userNotAssociatedWithGroup) {
                    if (usr.getCh().equals(ch)) {
                        aux_u = usr;
                    }
                }
                aux_u.setRoom(aux_g.getNme());
                ChatServer.userNotAssociatedWithGroup.remove(aux_u);
                aux_g.addUser(aux_u);
                sendMessageToGroup(aux_g,
                        dateFormat.format(date) + " - " + aux_u.getNick() + " JOINED  !!" + ";;;" + serverMessageColor);
                Thread.sleep(10);
                ch.write(ByteBuffer.wrap(("@room:" + aux_g.getNme()).getBytes()));

                break;
            case LEAVE:
                user = ChatServer.map_user.get(message.getHeader().getSender());
                grupo = ChatServer.map_group.get(user.getRoom());
                grupo.users.remove(user);
                ChatServer.userNotAssociatedWithGroup.add(user);
                ch.write(ByteBuffer
                        .wrap(("LEAVING chat room " + user.getRoom() + ";;;" + serverMessageColor).getBytes()));
                break;
            case NORMAL:
                grupo = ChatServer.map_group.get(message.getHeader().getReceiver());

                user = ChatServer.map_user.get(message.getHeader().getSender());

                if (grupo != null) {
                    sendMessageToGroup(grupo, dateFormat.format(date) + " - " + user.getNick() + " say MESSAGE : \n \t"
                            + message.getBody() + ";;;" + user.getColor());

                }

                break;
        }

    }

    public static void sendMessageToGroup(Group grupo, String message) throws Exception {
        for (User usr : grupo.getUsers()) {
            SocketChannel ch = usr.getCh();
            ch.write(ByteBuffer.wrap(message.getBytes()));

        }
    }

    public boolean isTaskType(Message message) {
        boolean flag = false;
        if (message.getHeader().getType() == MessageType.JOIN || message.getHeader().getType() == MessageType.BYE
                || message.getHeader().getType() == MessageType.LEAVE
                || message.getHeader().getType() == MessageType.LOGIN
                || message.getHeader().getType() == MessageType.LOGOUT
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
            // é commando de nick
            System.out.println("nick");
            header.setType(MessageType.NICK);
            message.setBody(matcher.group(3));

        } else {

            pattern = Pattern.compile(regex_join, Pattern.MULTILINE);
            matcher = pattern.matcher(m);
            if (matcher.find()) {
                // é commando de join
                System.out.println("join");

                header.setType(MessageType.JOIN);
                message.setBody(matcher.group(3));

            } else {

                pattern = Pattern.compile(regex_leave, Pattern.MULTILINE);
                matcher = pattern.matcher(m);
                if (matcher.find()) {
                    // é commando de leave
                    System.out.println("leave");

                    header.setType(MessageType.LEAVE);

                } else {

                    pattern = Pattern.compile(regex_bye, Pattern.MULTILINE);
                    matcher = pattern.matcher(m);
                    if (matcher.find()) {
                        // é commando de bye
                        System.out.println("bye");

                        header.setType(MessageType.BYE);

                    } else {
                        pattern = Pattern.compile(regex_listu, Pattern.MULTILINE);
                        matcher = pattern.matcher(m);
                        if (matcher.find()) {
                            // é commando de listu
                            System.out.println("listu");

                            header.setType(MessageType.LISTU);

                        } else {
                            pattern = Pattern.compile(regex_listr, Pattern.MULTILINE);
                            matcher = pattern.matcher(m);
                            if (matcher.find()) {
                                // é commando de listr
                                System.out.println("listr");

                                header.setType(MessageType.LISTR);

                            } else {
                                // é um messagem normal
                                System.out.println("normal");
                                header.setType(MessageType.NORMAL);
                                message.setBody(m);
                            }

                        }
                    }
                }
            }

        }
        message.setHeader(header);
        return message;
    }

    public static String generateRandomColor() {
        Random random = new Random();
        int red = random.nextInt(40);
        int green = random.nextInt(20);
        int blue = random.nextInt(30);

        int n = ChatServer.allUserLoggedIn.size() + random.nextInt(30);

        return (n * red > 255 ? red : n * red) + "," + (n * green > 255 ? green : n * green) + ","
                + (n * blue > 255 ? blue : n * blue);
    }
}
