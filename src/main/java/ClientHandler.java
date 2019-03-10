import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;

import javax.swing.*;
import java.awt.event.*;

import static java.lang.Thread.sleep;

public class ClientHandler extends JFrame {
    private JTabbedPane basicControlPane;
    private JPanel panel1;
    private JButton LoginButton;
    private JPanel textControl;
    private JPanel login;
    private JTextField presenceChanger;
    private JLabel presenceLabel;
    private JButton changePresence;
    private JLabel presence;
    private JComboBox activityTypeList;
    private JComboBox serverList;
    private JLabel serverLabel;
    private JComboBox channelList;
    private JLabel channelLabel;
    private JTextField messageText;
    private JButton messageButton;
    private IDiscordClient client;
    private RhoEventHandler handler;
    private boolean firstLogin = true;

    public ClientHandler() {
        client = RhoMain.getClient();
        handler = new RhoEventHandler();
        this.add(basicControlPane);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        LoginButton.setVisible(true);
        login.setVisible(false);
        basicControlPane.setVisible(true);
        basicControlPane.remove(textControl);
        client.getDispatcher().registerListener(handler);
        client.getDispatcher().registerListener(new GameServerEventHandler());
        LoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!client.isLoggedIn()) {
                    client.login();
                    if (firstLogin) {
                        while (!client.isReady()) {
                            try {
                                sleep(10);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                        initialize();
                    }
                    LoginButton.setText("Logout");
                    presence.setText("Playing: Type ]]help for a list of commands.");
                    basicControlPane.addTab("Say Something!", textControl);
                } else {
                    handler.logout();
                    client.logout();
                    LoginButton.setText("Login");
                    basicControlPane.remove(textControl);
                }
            }
        });
        changePresence.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newPresence = presenceChanger.getText();
                if (newPresence != "") {
                    ActivityType activity = (ActivityType) activityTypeList.getSelectedItem();
                    client.changePresence(StatusType.ONLINE, activity, newPresence);
                    presence.setText(activity + ": " + newPresence);
                }
            }
        });
        serverList.addFocusListener(new FocusAdapter() {
        });
        serverList.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    IGuild server = RhoMain.findGuild(e.getItem().toString());
                    channelList.removeAllItems();
                    for (IChannel channel : server.getChannels()) {
                        channelList.addItem(channel.getName());
                    }
                }
            }
        });
        messageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (messageText.getText() != "") {
                    RhoMain.sendMessage(RhoMain.findChannel(RhoMain.findGuild(serverList.getSelectedItem().toString()), channelList.getSelectedItem().toString()), messageText.getText());
                }
            }
        });
    }

    private void initialize() {
        activityTypeList.removeAllItems();
        for (ActivityType value : ActivityType.values()) {
            activityTypeList.addItem(value);
        }
        serverList.removeAllItems();
        channelList.removeAllItems();
        for (IGuild server : client.getGuilds()) {
            serverList.addItem(server.getName());
        }
        System.out.println(serverList.getItemCount());
        for (IChannel channel : RhoMain.findGuild(serverList.getItemAt(0).toString()).getChannels()) {
            channelList.addItem(channel.getName());
        }
    }

    public static void main(String[] args) {
        //Logs rhobot in. Might implement a GUI sometime later
        ClientHandler handler = new ClientHandler();
        handler.setVisible(true);
    }

}

