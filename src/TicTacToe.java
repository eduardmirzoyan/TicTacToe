package src;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class TicTacToe implements Runnable, Serializable {

    // Ip address to connect with,
    private String ip = "";
    // port to connect on, for online connection must be port forwarded and used public?
    private int port = 22222;
    // For GUI
    private transient JFrame frame;
    // Size of game
    private final int WIDTH = 506; // Original 506
    private final int HEIGHT = 600; // Original 527

    // ?
    private transient Thread thread;

    // Paints the components?
    private transient Painter painter;
    // Client?
    private transient Socket socket;
    // outputs data to users
    private transient ObjectOutputStream dos;
    // Inputs data from users
    private transient ObjectInputStream dis;

    // Server?
    private transient ServerSocket serverSocket;

    // Pictures for the game
    private transient BufferedImage board;
    private transient BufferedImage redX;
    private transient BufferedImage blueX;
    private transient BufferedImage redCircle;
    private transient BufferedImage blueCircle;

    // The array for the board from 0-8
    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean unableToCommunicateWithOpponent = false;
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean tie = false;

    private boolean requested = false;
    private boolean confirmRestart = false;


    private int lengthOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;

    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    // Different messages throughout the game
    private String waitingString = "Waiting for another player";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
    private String wonString = "You won!";
    private String enemyWonString = "Opponent won!";
    private String tieString = "Game ended in a tie.";
    private String consoleText = "";

    // Possible win combinations
    private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

    /**
     * <pre>
     * 0, 1, 2
     * 3, 4, 5
     * 6, 7, 8
     * </pre>
     */

    public TicTacToe() {


		ip = JOptionPane.showInputDialog("Please input the IP: ");
		String portStr = JOptionPane.showInputDialog("Please input the port: ");
		port = Integer.parseInt(portStr);
		while (port < 1 || port > 65535) {
			portStr = JOptionPane.showInputDialog("The port you entered was invalid, please input another port: ");
			port = Integer.parseInt(portStr);
		}



        loadImages(); // Loads pictures

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        if (!connect()) initializeServer();

        frame = new JFrame();
        frame.setTitle("Tic-Tac-Toe");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "TicTacToe");
        thread.start();
    }

    public void run() {
        while (true) {
            tick();
            painter.repaint();

            if (!circle && !accepted) {
                listenForServerRequest();
            }

        }
    }

    private void render(Graphics g) {
        g.drawImage(board, 0, 0, null);
        Graphics2D g2 = (Graphics2D) g;

        if (unableToCommunicateWithOpponent) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            consoleText = unableToCommunicateWithOpponentString;
            return;
        }

        if (accepted) {
            for (int i = 0; i < spaces.length; i++) {
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                           g.drawImage(blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (won || enemyWon) {
                //Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);


                if (won) {
                    consoleText = wonString;
                } else if (enemyWon) {
                    consoleText = enemyWonString;
                }
            } else {
                if (tie) {
                    consoleText = tieString;
                }
            }

        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            consoleText = waitingString;

        }
        // User output box
        g2.setStroke(new BasicStroke(5));
        g.setFont(smallerFont);
        g.setColor(Color.BLACK);
        g.drawRect(0, 500, 489, 60);
        g.drawString(consoleText, 10, 540);

    }

    private void tick() {
        if (errors >= 10) unableToCommunicateWithOpponent = true; //If there are too many erros, end the program


        if (!unableToCommunicateWithOpponent) {
                if (!yourTurn) {
                    try {
                        Data userData = (Data) dis.readObject();

                        // Code to deal with requests
                        boolean requested = userData.isRequest();
                        if (requested) {
                            //JOptionPane.showMessageDialog(frame, "You have been requested to restart the game.");
                            // Yes = 0, No = 1
                            int choice = JOptionPane.showConfirmDialog(frame, "Restart the game?", "Alert", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                dos.writeObject(new Data('a'));
                                dos.flush();
                                restartGame();
                                //confirmRestart = false;
                            }
                            else {
                                dos.writeObject(new Data('b'));
                                dos.flush();
                            }
                        }

                        // End of code

                        System.out.println("DATA WAS RECIEVED");
                        int space = userData.getPos();
                        if (space >= 0) {
                            if (circle) spaces[space] = "X";
                            else spaces[space] = "O";
                            checkForEnemyWin();
                            checkForTie();
                            yourTurn = true;
                            consoleText = "It is your turn";
                        }
                    }
                    catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        errors++;
                    }
                }
                else {
                    if(dis != null && confirmRestart) {
                        try {
                            Data userData = (Data)dis.readObject();
                            char confirmed = userData.getMsg();

                            if(confirmed == 'a') {
                                JOptionPane.showMessageDialog(frame, "Your opponent has accepted your request.");
                                // Restart here
                                restartGame();
                            }
                            else {
                                JOptionPane.showMessageDialog(frame, "Your opponent has declined your request.");
                                confirmRestart = false;
                            }

                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            errors++;
                        }
                    }

                }
        } // end of if(!unable...
    } // end of method



    private void checkForWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            }
        }
    }

    private void checkForEnemyWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            }
        }
    }

    private void checkForTie() {
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        tie = true;
    }

    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new ObjectOutputStream(socket.getOutputStream());
            dis = new ObjectInputStream(socket.getInputStream());
            accepted = true;

            if(yourTurn) consoleText = "You are going first.";
            System.out.println(consoleText);

            System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(ip, port);
            dos = new ObjectOutputStream(socket.getOutputStream());
            dis = new ObjectInputStream(socket.getInputStream());
            accepted = true;
            consoleText = "You are going second.";

        } catch (IOException e) {
            System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
            return false;
        }
        System.out.println("Successfully connected to the server.");
        return true;
    }



    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        circle = false;
    }

    private void loadImages() {
        try {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
            redX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
            redCircle = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
            blueX = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
            blueCircle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        TicTacToe ticTacToe = new TicTacToe();
    }

    public void restartGame() {
        for (int i = 0; i < spaces.length; i++) {
            spaces[i] = null;
        }
        won = false;
        enemyWon = false;
        tie = false;
        confirmRestart = false;
        if(yourTurn) consoleText = "You are going first.";
        else consoleText = "You are going second.";
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    public class Data implements Serializable {
        private int pos;
        private boolean request = false;
        private char msg;
        private static final long serialVersionUID = 1L;

        public Data(int pos) {
            this.pos = pos;
        }

        public Data(boolean request) {
            this.request = request;
            this.pos = -1;
        }

        public Data(char temp) {
            msg = temp;
            this.pos = -1;
        }

        public char getMsg() {
            return msg;
        }

        public Data(int pos, boolean request) {
            this.pos = pos;
            this.request = request;
        }

        public int getPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public boolean isRequest() {
            return request;
        }

        public void setRequest(boolean request) {
            this.request = request;
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



    private class Painter extends JPanel implements MouseListener, KeyListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
            addKeyListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            render(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
                    int x = e.getX() / lengthOfSpace;
                    int y = e.getY() / lengthOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        consoleText = "It is your opponents turn."; // ----------------------------------
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            dos.writeObject(new Data(position));
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("DATA WAS SENT");
                        checkForWin();
                        checkForTie();

                    }
                }
            }
        }

        public void keyPressed(KeyEvent e) {

            if (accepted && yourTurn && !unableToCommunicateWithOpponent) {
                int keyCode = e.getKeyCode();
                if(keyCode == KeyEvent.VK_R) {
                    System.out.println("Test");
                    JOptionPane.showMessageDialog(frame, "You have asked your opponent to restart the game.");
                    confirmRestart = true;
                    try {
                        dos.writeObject(new Data(true));
                        dos.flush();
                    } catch (IOException ioException) {
                        errors++;
                        ioException.printStackTrace();
                    }
                }
            }

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {

        }

        @Override
        public void keyTyped(KeyEvent e) {

        }
    }

}
