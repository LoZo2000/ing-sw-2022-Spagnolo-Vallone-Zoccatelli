package it.polimi.ingsw.view;

import it.polimi.ingsw.messages.AddMeMessage;
import it.polimi.ingsw.messages.Message;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class GUIEntry{
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    private OutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    private JFrame window = new JFrame("Eriantys"); //Main window
    private JTextField textField;
    private JSlider slider;
    private JButton ruleButton = new JButton(); //To change rules
    private JButton creditButton = new JButton(); //To open credits dialog
    private JButton playButton = new JButton(); //To join a match
    private boolean completeRules = false;
    private GameReport report;
    private boolean requestSent = false; //To avoid a client joins more than one match at the same time
    private JLabel connDesc, uncDesc; //To display the status of the research
    private JLabel connLabel, uncLabel; //To display the status of the research
    private String nickname;

    private final Object lockWrite;

    public GUIEntry(Socket socket, Object lockWrite) throws IOException{
        System.setProperty("sun.java2d.uiScale", "1.0");

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        this.lockWrite = lockWrite;

        createGUI();
    }

    public GameReport openGUI(){
        window.setVisible(true);

        try {
            objectInputStream = new ObjectInputStream(inputStream);
            report = (GameReport) objectInputStream.readObject();
        }catch(Exception e){
            e.printStackTrace();
        }
        while(report.getError() != null && report.getError().equals("This nickname is already taken")){
            connDesc.setVisible(false);
            connLabel.setVisible(false);
            uncDesc.setVisible(true);
            uncLabel.setVisible(true);
            requestSent = false;
            JOptionPane.showMessageDialog(null, "<html>This nickname was already taken!<br/>Please choose another one...</html>","Eriantys - Error", JOptionPane.ERROR_MESSAGE);
            try {
                objectInputStream = new ObjectInputStream(inputStream);
                report = (GameReport) objectInputStream.readObject();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        while(report.getTurnOf() == null){
            try {
                objectInputStream = new ObjectInputStream(inputStream);
                report = (GameReport) objectInputStream.readObject();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return report;
    }

    public void hideGUI(){
        window.setVisible(false);
    }

    public void showGUI(){
        window.setVisible(true);
    }



    private void createGUI(){

        //Background image:
        ImageIcon bgIcon = new ImageIcon(this.getClass().getResource("/Lobby_bg.png"));
        Image bgImage = bgIcon.getImage();
        Image newImg = bgImage.getScaledInstance(700, 400,  Image.SCALE_SMOOTH);
        bgIcon = new ImageIcon(newImg);
        JLabel bgLabel = new JLabel(bgIcon);
        bgLabel.setSize(700,400);

        //Main window:
        window.add(bgLabel);
        window.setSize(700, 435);
        window.setResizable(false);
        window.setLayout(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);

        //Show help Buttons:
        JButton creditButton = new JButton();
        ImageIcon creditIcon = new ImageIcon(this.getClass().getResource("/Credits.png"));
        Image creditImage = creditIcon.getImage();
        newImg = creditImage.getScaledInstance(35, 35,  Image.SCALE_SMOOTH);
        creditIcon = new ImageIcon(newImg);
        creditButton.setIcon(creditIcon);
        creditButton.setContentAreaFilled(false);
        creditButton.setBorderPainted(false);
        creditButton.setBounds(10,10,  35, 35);
        creditButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        CreditsDialog credits = new CreditsDialog(window);
        creditButton.addActionListener(e->{
            credits.showDialog();
        });
        bgLabel.add(creditButton);

        JButton ruleButton = new JButton();
        ImageIcon ruleIcon = new ImageIcon(this.getClass().getResource("/Book.png"));
        Image ruleImage = ruleIcon.getImage();
        newImg = ruleImage.getScaledInstance(35, 35,  Image.SCALE_SMOOTH);
        ruleIcon = new ImageIcon(newImg);
        ruleButton.setIcon(ruleIcon);
        ruleButton.setContentAreaFilled(false);
        ruleButton.setBorderPainted(false);
        ruleButton.setBounds(10,55,  35, 35);
        ruleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bgLabel.add(ruleButton);
        RulesDialog rulesDialog = new RulesDialog(window);
        ruleButton.addActionListener(e->{
            rulesDialog.showDialog();
        });

        //Label containing the setting menu:
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(100, 200, 500, 155);
        panel.setOpaque(true);
        Color colorLabel = new Color(128,128,128,255);
        panel.setBackground(colorLabel);
        Border border = BorderFactory.createLineBorder(Color.BLACK);
        panel.setBorder(border);
        bgLabel.add(panel);

        //Title of the label:
        JLabel title = new JLabel();
        title.setText("Find your next match, now!!!");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setVerticalAlignment(SwingConstants.CENTER);
        title.setFont(new Font("MV Boli", Font.BOLD, 18));
        title.setBounds(100,5,300,20);
        panel.add(title);

        //Text to state the requirements for the nickname:
        JLabel nick = new JLabel();
        nick.setText("Enter your nickname");
        nick.setHorizontalAlignment(SwingConstants.CENTER);
        nick.setVerticalAlignment(SwingConstants.CENTER);
        nick.setBounds(25,30,125,20);
        panel.add(nick);
        JLabel req = new JLabel();
        req.setText("(must be different from other players'ones)");
        req.setHorizontalAlignment(SwingConstants.CENTER);
        req.setVerticalAlignment(SwingConstants.CENTER);
        req.setBounds(150, 30, 200, 20);
        req.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        panel.add(req);

        //TextField to get player's nickname
        textField = new JTextField(16);
        textField.setBounds(350,30,125,20);
        panel.add(textField);

        //Text to insert number of players:
        JLabel pla = new JLabel();
        pla.setText("Set number of players");
        pla.setHorizontalAlignment(SwingConstants.CENTER);
        pla.setVerticalAlignment(SwingConstants.CENTER);
        pla.setBounds(25,55,125,30);
        panel.add(pla);

        //Slider to select the number of players:
        slider = new JSlider(JSlider.HORIZONTAL,2,4,2);
        slider.setMajorTickSpacing(1);
        slider.setPaintLabels(true);
        slider.setOpaque(false);
        slider.setBounds(150,55,75,30);
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(slider);

        //Text to choose the rule type:
        JLabel rul = new JLabel();
        rul.setText("Set of rules:");
        rul.setHorizontalAlignment(SwingConstants.CENTER);
        rul.setVerticalAlignment(SwingConstants.CENTER);
        rul.setBounds(260,55,75,30);
        panel.add(rul);
        JRadioButton option1 = new JRadioButton("Simple",true);
        option1.setOpaque(false);
        option1.setBounds(335,55,65,30);
        panel.add(option1);
        JRadioButton option2 = new JRadioButton("Complete",false);
        option2.setOpaque(false);
        option2.setBounds(400,55,100,30);
        panel.add(option2);
        ButtonGroup group = new ButtonGroup();
        group.add(option1);
        group.add(option2);
        option1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        option2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        //Play button:
        ImageIcon playIcon = new ImageIcon(this.getClass().getResource("/Play.png"));
        Image playImage = playIcon.getImage();
        newImg = playImage.getScaledInstance(60, 60,  Image.SCALE_SMOOTH);
        playIcon = new ImageIcon(newImg);
        playButton.setIcon(playIcon);
        playButton.setBounds(220, 90, 60, 60);
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e->{
            if(requestSent){
                JOptionPane.showMessageDialog(null, "<html>You are waiting to join a match!<br/>Please hold on for a little bit...</html>","Eriantys - Error", JOptionPane.ERROR_MESSAGE);
            }
            else{
                requestSent = true;
                nickname = textField.getText();
                Message message = new AddMeMessage(nickname, option2.isSelected(), slider.getValue());
                try {
                    synchronized (lockWrite) {
                        objectOutputStream = new ObjectOutputStream(outputStream);
                        objectOutputStream.writeObject(message);
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                uncDesc.setVisible(false);
                uncLabel.setVisible(false);
                connDesc.setVisible(true);
                connLabel.setVisible(true);
            }
        });
        panel.add(playButton);

        //Status bar:
        JPanel status = new JPanel();
        status.setLayout(null);
        status.setBounds(283, 126, 212, 24);
        status.setOpaque(true);
        status.setBackground(colorLabel);
        status.setBorder(border);
        panel.add(status);
        JLabel sta = new JLabel();
        sta.setText("Status");
        sta.setHorizontalAlignment(SwingConstants.CENTER);
        sta.setVerticalAlignment(SwingConstants.CENTER);
        sta.setBounds(2,4,40,16);
        status.add(sta);
        uncDesc = new JLabel();
        uncDesc.setText("Not playing: find a game!");
        uncDesc.setFont(new Font("Times New Roman", Font.PLAIN, 9));
        uncDesc.setHorizontalAlignment(SwingConstants.CENTER);
        uncDesc.setVerticalAlignment(SwingConstants.CENTER);
        uncDesc.setBounds(42,4,146,16);
        status.add(uncDesc);
        connDesc = new JLabel();
        connDesc.setText("Match created: wait for other players...");
        connDesc.setFont(new Font("Times New Roman", Font.PLAIN, 9));
        connDesc.setHorizontalAlignment(SwingConstants.CENTER);
        connDesc.setVerticalAlignment(SwingConstants.CENTER);
        connDesc.setBounds(42,4,146,16);
        connDesc.setVisible(false);
        status.add(connDesc);
        ImageIcon connIcon = new ImageIcon(this.getClass().getResource("/Connected.png"));
        Image connImage = connIcon.getImage();
        newImg = connImage.getScaledInstance(20, 20,  Image.SCALE_SMOOTH);
        connIcon = new ImageIcon(newImg);
        connLabel = new JLabel(connIcon);
        connLabel.setBounds(190,2,20,20);
        connLabel.setVisible(false);
        status.add(connLabel);
        ImageIcon uncIcon = new ImageIcon(this.getClass().getResource("/Unconnected.png"));
        Image uncImage = uncIcon.getImage();
        newImg = uncImage.getScaledInstance(20, 20,  Image.SCALE_SMOOTH);
        uncIcon = new ImageIcon(newImg);
        uncLabel = new JLabel(uncIcon);
        uncLabel.setBounds(190,2,20,20);
        status.add(uncLabel);
    }
}