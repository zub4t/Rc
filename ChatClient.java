import java.net.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    private Selector selector;
    private SocketChannel clientChannel;
    private ByteBuffer buf;
    private int port;
    private String host;

    public String myNick = "anonymous";
    public String myCurrentRoom = "";
    public Color myColor;

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

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

        /*
         * frame.addWindowListener(new WindowAdapter() { public void
         * windowClosing(WindowEvent e) { try { clientChannel.close();
         * 
         * } catch (Exception ex) { ex.printStackTrace(); } } });
         */
        this.port = port;
        this.host = server;

    }

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor

        if (message.charAt(0) == '/') {

            String aux[] = message.split(" ");
            switch (aux[0]) {
                case "/join":
                case "/nick":
                case "/leave":
                case "/bye":
                case "/priv":
                    printMessage(message + "\n");
                    break;
                default:
                    message = "/" + message;
                    break;
            }

        }
        message += "\n";
        clientChannel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.ISO_8859_1)));

    }

    // Método principal do objecto
    public void run() throws IOException {
        selector = Selector.open();
        clientChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", this.port));
        // Defina o cliente para o modo sem bloqueio
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        buf = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
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
        buf.clear();
        String msg = "";
        int bytesRead = ch.read(buf);
        if (bytesRead != -1) {
            buf.flip();
            while (buf.hasRemaining()) {
                byte array[] = new byte[1];
                array[0] = buf.get();
                msg += new String(array, StandardCharsets.ISO_8859_1);

            }
        }
        msg = msg.replaceAll("\n", "");
        String aux[] = msg.split(" ");
        String m = "";
        switch (aux[0]) {
            case "MESSAGE":
                m = "";
                for (int i = 2; i < aux.length; i++) {
                    m += " " + aux[i];
                }
                msg = aux[1] + ":" + m;
                break;
            case "PRIVATE":
                m = "";
                for (int i = 2; i < aux.length; i++) {
                    m += " " + aux[i];
                }
                msg = aux[1] + " sussurou:" + m;
                break;

            case "NEWNICK":
                msg = aux[1] + " mudou de nome para " + aux[2];
                break;
            case "LEFT":
                msg = "usuário saiu -> " + aux[1];
                break;
            case "BYE":
                msg = " até outra hora ";
                break;
            case "OK":
                msg = " comando OK ";
                break;
            case "ERROR":
                msg = " algo correu mal ERROR";
                break;
            case "JOINED":
                msg = "usuário entrou -> " + aux[1];
                break;

            default:
                break;
        }

        printMessage(msg + "\n");

    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        // ChatClient client = new ChatClient("localhost", 8000);

        client.run();
    }

}
