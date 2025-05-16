import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.awt.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;



abstract class Object{
    public static final int tile_size = 45;
    public int size = 45;
    public int x, y; // オブジェクトの位置
    public int shape;
    public Color c;
    
    Object(int xx, int yy, int s, Color col){
        x = xx * tile_size;
        y = yy * tile_size;
        shape = s;
        c = col;
    }
    
    void draw(Graphics g)
    {
        g.setColor(c);
        if (shape == 1){
            g.fillOval(x + (tile_size - size) / 2, y + (tile_size - size) / 2, size, size);
        } else {
            g.fillRect(x + (tile_size - size) / 2, y + (tile_size - size) / 2, size, size);
        }
    }
}

class Ground extends Object {
    Ground(int xx, int yy) {
        super(xx, yy, 0, Color.LIGHT_GRAY);
    }
}

class Burned extends Object {
    Burned(int xx, int yy) {
        super(xx, yy, 0, Color.ORANGE);
    }
}

class Wall extends Object {
    Wall(int xx, int yy) {
        super(xx, yy, 0, Color.DARK_GRAY);
    }
}

class Player extends Object{
    public static final Color[] color_list = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
    public int life;
    Player(int xx, int yy, int i) {
        super(xx, yy, 0, color_list[i]);
        size = size * 2 / 3;
    }
    @Override
    void draw(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, tile_size, tile_size);
        super.draw(g);
    }
}

abstract class Bomb extends Object {
    public boolean fired;
    public int fire_size;
    Bomb(int xx, int yy, Color col, boolean f) {
        super(xx, yy, 1, col);
        size = size * 2 / 3;
        fire_size = size / 3;
        fired = f;
    }
    @Override
    void draw(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, tile_size, tile_size);
        super.draw(g);
        if (fired) {
            g.setColor(Color.RED);
            g.fillOval(x + (tile_size - fire_size) / 2, y + (tile_size - fire_size) / 2, fire_size, fire_size);
        }
    }
}

class NomBomb extends Bomb {
    NomBomb(int xx, int yy, boolean f) {
        super(xx, yy, Color.BLACK, f);
    }
}

class FireBomb extends Bomb {
    FireBomb(int xx, int yy, boolean f) {
        super(xx, yy, Color.PINK, f);
    }
}

class FastBomb extends Bomb {
    FastBomb(int xx, int yy, boolean f) {
        super(xx, yy, Color.BLUE, f);
    }
}

public class MyClient_gui {
    public static int N = 10;
    final static private int size    = Object.tile_size * N;  // 動画を描画する領域の縦横サイズ
    final static private int XOFFSET = 20;   // 左右の縁の余裕
    final static private int YOFFSET = 80;
    private Image offscreen = null;

    public static void main(String[] args) {
        MyClient_gui client = new MyClient_gui();
        client.start();
    }

    public void start() {
        final String SERVER_IP = "localhost";
        final int PORT = 50505;

        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            System.out.println("loading...");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            MyFrame frame = new MyFrame();
            frame.setVisible(true);
            frame.requestFocus();

            java.util.Timer sendTimer = new java.util.Timer();
            sendTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String dir = frame.getDirection();
                    out.println(dir);
                }
            }, 0, 10);

            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        frame.showBoard(line);
                    }
                } catch (IOException e) {
                    System.err.println("error: " + e.getMessage());
                }
            });
            receiveThread.start();
            receiveThread.join();
            socket.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private class MyFrame extends JFrame {
        private final Set<String> pressedKeys = new HashSet<>();

        public MyFrame() {
            setTitle("Game");
            setBounds(0, 0, XOFFSET * 2 + size, YOFFSET * 2 + size);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                            pressedKeys.add("bomb");
                            break;
                        case KeyEvent.VK_RIGHT:
                            pressedKeys.add("right");
                            break;
                        case KeyEvent.VK_LEFT:
                            pressedKeys.add("left");
                            break;
                        case KeyEvent.VK_UP:
                            pressedKeys.add("up");
                            break;
                        case KeyEvent.VK_DOWN:
                            pressedKeys.add("down");
                            break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                            pressedKeys.remove("bomb");
                            break;
                        case KeyEvent.VK_RIGHT:
                            pressedKeys.remove("right");
                            break;
                        case KeyEvent.VK_LEFT:
                            pressedKeys.remove("left");
                            break;
                        case KeyEvent.VK_UP:
                            pressedKeys.remove("up");
                            break;
                        case KeyEvent.VK_DOWN:
                            pressedKeys.remove("down");
                            break;
                    }
                }
            });
        }

        public String getDirection() {
            if (pressedKeys.isEmpty()) return "stopped";
            if (pressedKeys.contains("bomb")) return "bomb";
            if (pressedKeys.contains("up")) return "up";
            if (pressedKeys.contains("down")) return "down";
            if (pressedKeys.contains("left")) return "left";
            if (pressedKeys.contains("right")) return "right";
            return "stopped";
        }

        public void showBoard(String boardData){
            if(offscreen == null) {
                offscreen = this.createImage(size,size);
            }
            Graphics g = offscreen.getGraphics();
            g.clearRect(0, 0, size, size);
            Object[][] objs = new Object[N][N];
            String[] data = boardData.split(",");
            boolean inj = false;
            boolean fire = false;
            for (int i = 0; i < objs.length; i++) {
                for (int j = 0; j < objs[0].length; j++) {
                    int s = Integer.parseInt(data[i * objs.length + j]);
                    switch (s) {
                        case 0:
                            objs[j][i] = new Ground(j, i);
                            break;
                        case 91:
                            fire = true;
                        case 21:
                            objs[j][i] = new NomBomb(j, i, fire);
                            fire = false;
                            break;
                        case 92:
                            fire = true;
                        case 22:
                            objs[j][i] = new FireBomb(j, i, fire);
                            fire = false;
                            break;
                        case 93:
                            fire = true;
                        case 23:
                            objs[j][i] = new FastBomb(j, i, fire);
                            fire = false;
                            break;
                        default:
                            if (s < 0) {
                                objs[j][i] = new Burned(j, i);
                            } else {
                                objs[j][i] = new Player(j, i, s % 4);
                            }
                            break;
                    }
                    objs[j][i].draw(g);
                }
            }
            Graphics currentg = this.getGraphics();
            currentg.drawImage(offscreen, XOFFSET, YOFFSET, this);
            
            // for (int i = 0; i < data.length - board.length * board.length; i++) {
            //     cells[i][N].setText(data[board.length * board.length + i]);
            //     switch(Integer.parseInt(data[board.length * board.length + i])){
            //         case 1:
            //             cells[i][N].setForeground(java.awt.Color.GRAY);
            //             break;
            //         case 2:
            //             cells[i][N].setForeground(java.awt.Color.RED);
            //             break;
            //         case 3:
            //             cells[i][N].setForeground(java.awt.Color.BLUE);
            //             break;
            //         default:
            //             cells[i][N].setForeground(java.awt.Color.BLACK);
            //             break;
            //     }
            // }

        }
    }
}
