import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
/*
 * Created by JFormDesigner on Mon Jan 28 04:23:18 PST 2019
 */



/**
 * @author James Thomas
 */
class GUI extends JFrame {
    private boolean startedScript = false;
    GUI() {
        initComponents();
    }

    private void startButtonMouseClicked(MouseEvent e) {
        setVisible(false);
        startedScript = true;
        dispose();
    }

    private void initComponents() {
        panel1 = new JPanel();
        panel2 = new JPanel();
        combatStyle = new JComboBox<>();
        label1 = new JLabel();
        hpVal = new JSpinner();
        label2 = new JLabel();
        startButton = new JButton();

        //======== this ========
        setTitle("NMZ");
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] {0, 0, 0};
        ((GridBagLayout)contentPane.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
        ((GridBagLayout)contentPane.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
        ((GridBagLayout)contentPane.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

        //======== panel1 ========
        {
            panel1.setBorder(new TitledBorder("NMZ"));

            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
            ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
            ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

            //======== panel2 ========
            {
                panel2.setBorder(new TitledBorder("Combat Options"));
                panel2.setLayout(new GridBagLayout());
                ((GridBagLayout)panel2.getLayout()).columnWidths = new int[] {35, 24, 29, 0};
                ((GridBagLayout)panel2.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                ((GridBagLayout)panel2.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout)panel2.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                //---- comboBox1 ----
                combatStyle.setModel(new DefaultComboBoxModel<>(new String[] {
                        "Strength",
                        "Attack",
                        "Defence",
                        "Ranged",
                        "Magic"
                }));
                panel2.add(combatStyle, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));

                //---- label1 ----
                label1.setText("AFK to");
                panel2.add(label1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //---- hpVal ----
                hpVal.setModel(new SpinnerNumberModel(2, 0, null, 1));
                panel2.add(hpVal, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 5), 0, 0));

                //---- label2 ----
                label2.setText("HP");
                panel2.add(label2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 5, 0), 0, 0));
            }
            panel1.add(panel2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- startButton ----
            startButton.setText("Start");
            startButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    startButtonMouseClicked(e);
                }
            });
            panel1.add(startButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));
        }
        contentPane.add(panel1, new GridBagConstraints(0, 0, 2, 3, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        pack();
        setLocationRelativeTo(getOwner());
    }

    boolean isStartedClicked() {
        return startedScript;
    }

    String getCombatStyle() {
        return combatStyle.getItemAt(combatStyle.getSelectedIndex());
    }

    int getAfkHpVal() {
        return (Integer) hpVal.getValue();
    }

    private JPanel panel1;
    private JPanel panel2;
    private JComboBox<String> combatStyle;
    private JLabel label1;
    private JSpinner hpVal;
    private JLabel label2;
    private JButton startButton;
}