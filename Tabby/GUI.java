import java.awt.*;
import java.awt.event.*;
import java.security.SecureRandom;
import javax.swing.*;
import javax.swing.border.*;
/*
 * Created by JFormDesigner on Mon Jan 21 01:05:13 PST 2019
 */

class GUI extends JFrame {
    final Object lock = new Object();
    private boolean startedScript = false;

    GUI() {
        initComponents();
    }

    private void startButtonClicked(MouseEvent e) {
        startedScript = true;

        synchronized (lock) {
            lock.notify();
        }
        setVisible(false);
    }

    private void cancelButtonMouseClicked(MouseEvent e) {
        setVisible(false);
        dispose();
    }

    private void initComponents() {
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        selectedTab = new JComboBox<>();
        panel4 = new JPanel();
        whichLectern = new JComboBox<>();
        useAntiban = new JCheckBox();
        useOwnHouse = new JCheckBox();
        panel3 = new JPanel();
        hostUsername = new JTextField();
        panel1 = new JPanel();
        panel2 = new JPanel();
        label3 = new JLabel();
        minVal = new JSpinner();
        label5 = new JLabel();
        label4 = new JLabel();
        maxVal = new JSpinner();
        label6 = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle("Tabby");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(5, 5, 5, 5));

            // JFormDesigner evaluation mark
          /*  dialogPane.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
                            "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                            javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
                            java.awt.Color.red), dialogPane.getBorder()));
            dialogPane.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    if ("border".equals(e.getPropertyName())) throw new RuntimeException();
                }
            });*/

            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setBorder(new TitledBorder("Rimmington Tab Creation"));
                contentPanel.setToolTipText("Tabby");
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout) contentPanel.getLayout()).columnWidths = new int[]{0, 0, 0, 0};
                ((GridBagLayout) contentPanel.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout) contentPanel.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout) contentPanel.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                //---- selectedTab ----
                selectedTab.setModel(new DefaultComboBoxModel<>(new String[]{
                        "Lumbridge teleport",
                        "Falador teleport",
                        "Varrock teleport",
                        "Camelot teleport",
                        "Ardougne teleport",
                        "Watchtower teleport",
                        "Teleport to house",
                        "Bones to bananas",
                        "Bones to peaches"
                }));
                selectedTab.setSelectedIndex(3);
                selectedTab.setToolTipText("What tab are we making?");
                contentPanel.add(selectedTab, new GridBagConstraints(0, 0, 2, 2, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //======== panel4 ========
                {
                    panel4.setBorder(new TitledBorder("Use which study lectern"));
                    panel4.setLayout(new GridBagLayout());
                    ((GridBagLayout) panel4.getLayout()).columnWidths = new int[]{0, 0, 0};
                    ((GridBagLayout) panel4.getLayout()).rowHeights = new int[]{0, 0, 0, 0};
                    ((GridBagLayout) panel4.getLayout()).columnWeights = new double[]{0.0, 0.0, 1.0E-4};
                    ((GridBagLayout) panel4.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};

                    //---- whichLectern ----
                    whichLectern.setModel(new DefaultComboBoxModel<>(new String[]{
                            "First closest",
                            "Second closest"
                    }));
                    whichLectern.setSelectedIndex(0);
                    whichLectern.setToolTipText("Which lectern are we going to use?");
                    panel4.add(whichLectern, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 0), 0, 0));
                }
                contentPanel.add(panel4, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //---- useAntiban ----
                useAntiban.setText("Anti-ban");
                useAntiban.setSelected(true);
                useAntiban.setToolTipText("Simulate a human player");
                contentPanel.add(useAntiban, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //---- useOwnHouse ----
                useOwnHouse.setText("Use own house");
                useOwnHouse.setToolTipText("Are we using our own house?");
                contentPanel.add(useOwnHouse, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //======== panel3 ========
                {
                    panel3.setBorder(new TitledBorder("Host username"));
                    panel3.setToolTipText("Player owned house");
                    panel3.setLayout(new GridBagLayout());
                    ((GridBagLayout) panel3.getLayout()).columnWidths = new int[]{0, 131, 0};
                    ((GridBagLayout) panel3.getLayout()).rowHeights = new int[]{0, 0, 0, 0};
                    ((GridBagLayout) panel3.getLayout()).columnWeights = new double[]{0.0, 0.0, 1.0E-4};
                    ((GridBagLayout) panel3.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};

                    //---- hostUsername ----
                    hostUsername.setToolTipText("What is the host's username?");
                    panel3.add(hostUsername, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 0), 0, 0));
                }
                contentPanel.add(panel3, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.NORTH);

            //======== panel1 ========
            {
                panel1.setBorder(new TitledBorder("Anti-ban settings"));
                panel1.setToolTipText("Simulate a real player");
                panel1.setLayout(new GridBagLayout());
                ((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0, 114, 0};
                ((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 0, 0};
                ((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};

                //======== panel2 ========
                {
                    panel2.setBorder(new TitledBorder("Sleep"));
                    panel2.setToolTipText("How long should we sleep for?");
                    panel2.setLayout(new GridBagLayout());
                    ((GridBagLayout) panel2.getLayout()).columnWidths = new int[]{40, 0, 59, 24, 0};
                    ((GridBagLayout) panel2.getLayout()).rowHeights = new int[]{0, 0, 0, 0};
                    ((GridBagLayout) panel2.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};
                    ((GridBagLayout) panel2.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};

                    //---- label3 ----
                    label3.setText("Min");
                    label3.setLabelFor(minVal);
                    label3.setToolTipText("Minimum sleep time in seconds");
                    panel2.add(label3, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));
                    minVal.setModel(new SpinnerNumberModel(0, 0, null, 1));
                    panel2.add(minVal, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));

                    //---- label5 ----
                    label5.setText("(s)");
                    panel2.add(label5, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 0), 0, 0));

                    //---- label4 ----
                    label4.setText("Max");
                    label4.setLabelFor(maxVal);
                    label4.setToolTipText("Maximum sleep time in seconds");
                    panel2.add(label4, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));
                    maxVal.setModel(new SpinnerNumberModel(0, 0, null, 1));
                    panel2.add(maxVal, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));

                    //---- label6 ----
                    label6.setText("(s)");
                    panel2.add(label6, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 0), 0, 0));
                }
                panel1.add(panel2, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));
            }
            dialogPane.add(panel1, BorderLayout.CENTER);


            panel1.setVisible(true);

            useAntiban.addActionListener(e -> {
                if (useAntiban.isSelected()) {
                    minVal.setValue(random(0, 5));
                    maxVal.setValue(random(6, 30));
                    panel1.setVisible(true);
                    pack();
                } else if (!useAntiban.isSelected()) {
                    panel1.setVisible(false);
                    minVal.setValue(0);
                    maxVal.setValue(0);
                    pack();
                }
            });

            useOwnHouse.addActionListener(e -> {
                if (useOwnHouse.isSelected()) {
                    hostUsername.setEditable(false);
                    pack();
                } else if (!useOwnHouse.isSelected()) {
                    hostUsername.setEditable(true);
                    pack();
                }
            });
            minVal.setValue(random(0, 5));
            maxVal.setValue(random(6, 30));

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("Start");
                okButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        startButtonClicked(e);
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        cancelButtonMouseClicked(e);
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.NORTH);
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setVisible(true);
    }

    String getTab() {
        return selectedTab.getItemAt(selectedTab.getSelectedIndex());
    }

    int getAntibanSleepMax() {
        return (Integer) maxVal.getValue();
    }

    int getAntibanSleepMin() {
        return (Integer) minVal.getValue();
    }

    boolean useOwnHouse() {
        return useOwnHouse.isSelected();
    }

    boolean useAntiban() {
        return useAntiban.isSelected();
    }

    String getHostUsername() {
        return hostUsername.getText().trim();
    }

    String getLectern() {
        return whichLectern.getItemAt(whichLectern.getSelectedIndex());
    }

    boolean isStartedClicked() {
        return startedScript;
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private JPanel dialogPane;
    private JPanel contentPanel;
    private JComboBox<String> selectedTab;
    private JPanel panel4;
    private JComboBox<String> whichLectern;
    private JCheckBox useAntiban;
    private JCheckBox useOwnHouse;
    private JPanel panel3;
    private JTextField hostUsername;
    private JPanel panel1;
    private JPanel panel2;
    private JLabel label3;
    private JSpinner minVal;
    private JLabel label5;
    private JLabel label4;
    private JSpinner maxVal;
    private JLabel label6;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
}
