package Cliente;

import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import Util.*;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ChatClient {
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    private Selector selector;
    private SocketChannel clientChannel;
    private ByteBuffer buf;
    private TextField tfText;
    private TextArea taContent;
    private String username;
    private boolean isLogin = false;
    private boolean isConnected = false;
    private int port;
    private String host;
    public static Charset charset = StandardCharsets.UTF_8;
    public String myNick = "anonymous";
    public String myCurrentRoom = "";
    public Color myColor;

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextPane chatArea = new JTextPane();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    /*
     * public void printMessage(final String message) { chatArea.append(message); }
     */

    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocus();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        this.port = port;
        this.host = server;

    }

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        if (message.contains("/")) {
            // chatArea.setText(chatArea.getText() == null ? " " : chatArea.getText() + "\n"
            // + message);
            appendToPane(chatArea, message, Color.black, Font.BOLD);

        }
        clientChannel.write(ByteBuffer.wrap(UtilC.serialize(message, myNick, myCurrentRoom)));

    }

    // Método principal do objecto
    public void run() throws IOException {
        selector = Selector.open();
        clientChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", this.port));
        // Defina o cliente para o modo sem bloqueio
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        buf = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        // login();
        isConnected = true;

        Iterator<SelectionKey> iter;
        SelectionKey key;
        while (clientChannel.isOpen()) {
            selector.select();
            iter = this.selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                key = iter.next();
                iter.remove();
                if (key.isReadable())
                    this.handleRead(key);
            }
        }

    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();

        buf.clear();
        int read = 0;
        while ((read = ch.read(buf)) > 0) {

            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }
        String msg;
        msg = sb.toString();

        if (msg.contains("@nick:")) {
            myNick = msg.split(":")[1];
        } else if (msg.contains("@room:")) {
            myCurrentRoom = msg.split(":")[1];
        } else if (msg.contains("@color:")) {
            String aux[] = (msg.split(":")[1]).split(",");
            myColor = new Color(Integer.parseInt(aux[0]), Integer.parseInt(aux[1]), Integer.parseInt(aux[2]));
        } else {
            // chatArea.setText(chatArea.getText() == null ? " " : chatArea.getText() + "\n"
            // + msg);
            String aux[] = msg.split(";;;");
            String aux1[] = aux[1].split(",");
            Color c = new Color(Integer.parseInt(aux1[0]), Integer.parseInt(aux1[1]), Integer.parseInt(aux1[2]));
            appendToPane(chatArea, aux[0], c, Font.PLAIN);
        }

    }

    private void appendToPane(JTextPane tp, String msg, Color c, final int f) {
        Font font = new Font("Serif", f, 18);
        tp.setFont(font);
        StyledDocument doc = tp.getStyledDocument();
        Style style = tp.addStyle("", null);
        if (c == null) {
            c = Color.black;
        }
        StyleConstants.setForeground(style, c);
        StyleConstants.setBackground(style, Color.white);
        try {
            doc.insertString(doc.getLength(), "\n" + msg, style);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void login() {
        String username = JOptionPane.showInputDialog("Username");
        String password = JOptionPane.showInputDialog("Password");
        Message message = new Message();
        MessageHeader header = new MessageHeader();
        header.setSender(username);
        header.setType(MessageType.LOGIN);
        header.setTimestamp(System.currentTimeMillis());
        message.setHeader(header);
        message.setBody(password);

        try {
            clientChannel.write(ByteBuffer.wrap(UtilC.serialize(message)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username = username;
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        // ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        ChatClient client = new ChatClient("localhost", 10523);
        client.run();
    }

}