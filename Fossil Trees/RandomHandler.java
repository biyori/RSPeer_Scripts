import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.script.Script;
import org.rspeer.ui.Log;

import javax.swing.*;
import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;

class RandomHandler {
    double SCRIPT_VERSION = 1.0;

    private boolean showPopup = false, showSecondPopup = false;

    void checkLamp() {
        if (Inventory.contains("Lamp")) {
            if (!Interfaces.isOpen(134)) {
                Inventory.getFirst("Lamp").interact("Rub");
                Time.sleepUntil(() -> Interfaces.isOpen(134), 500, 10000);
            }
            if (Interfaces.isOpen(134)) {
                InterfaceComponent prayer = Interfaces.getComponent(134, 9);
                InterfaceComponent confirm = Interfaces.getComponent(134, 26);
                prayer.interact("Advance Prayer");
                Time.sleep(random(1000, 1500));
                confirm.interact("Ok");
                Time.sleepUntil(() -> !Interfaces.isOpen(134), 500, 10000);
            }
            if (Dialog.canContinue()) {
                Dialog.processContinue();
                Time.sleep(random(200, 500));
            }
        }
    }

    boolean findNearbyRandoms() {
        if (Npcs.getNearest(npc -> npc.containsAction("Dismiss")) != null) {
            if (!showPopup) {
                java.awt.EventQueue.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane("RANDOM EVENT DETECTED!", JOptionPane.WARNING_MESSAGE);
                    JDialog dialog = optionPane.createDialog("Warning!");
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                });
                showPopup = true;
            }
            Log.info("Detected random event!");
            Npc event = Npcs.getNearest(npc -> npc.containsAction("Dismiss"));

            logChat("We have detected a random event [" + event.getName() + "] Actions: [" + Arrays.toString(event.getActions()) + "]"
                    + " NPC Target: " + (event.getTarget() != null ? event.getTarget().getName() : "N/A")
                    + " Message: " + (event.getOverheadText() != null ? event.getOverheadText() : "N/A"));
            return true;
        }
        showPopup = false;
        return false;
    }

    boolean doWeHaveRandom() {
        if (Npcs.getNearest(npc -> npc.containsAction("Dismiss") && npc.getTarget() == Players.getLocal() && npc.isPositionWalkable()) != null) {
            if (!showSecondPopup) {
                java.awt.EventQueue.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane("WE HAVE A RANDOM EVENT!", JOptionPane.WARNING_MESSAGE);
                    JDialog dialog = optionPane.createDialog("Warning!!");
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                });
                showSecondPopup = true;
            }
            return true;
        }
        showSecondPopup = false;
        return false;
    }

    void handleRandom() {
        Npc event = Npcs.getNearest(npc -> npc.containsAction("Dismiss") && npc.getTarget() == Players.getLocal());

        if ((event.getName().equals("Genie") || event.getName().equals("Drunken Dwarf") || event.getName().equals("Dr Jekyll") || event.getName().equals("Rick Turpentine")) && event.isPositionWalkable()) {
            if (!Dialog.isOpen()) {
                event.interact(x -> true);
                Time.sleepUntil(Dialog::isOpen, 1000, 10000);
            }
            if (Dialog.canContinue()) {
                Dialog.processContinue();
                Time.sleep(random(200, 500));
            }

            //NEEDS TESTING
        } else if (event.getName().equals("Frog")) {
            Npc frogPrince = Npcs.getNearest(npc -> !npc.containsAction("Dismiss") && npc.getName().equals("Frog") && npc.containsAction("Talk-to"));
            if (!Dialog.isOpen()) {
                frogPrince.interact(x -> true);
                Time.sleepUntil(Dialog::isOpen, 1000, 10000);
            }
            if (Dialog.canContinue()) {
                Dialog.processContinue();
                Time.sleep(random(200, 500));
            }
            if (Dialog.isViewingChatOptions()) {
                Dialog.process("I suppose so, sure.");
                //  Dialog.process(opt->opt.contains("sure"));
                //Dialog.process(0);//Yes?
                Time.sleep(random(200, 500));
            }
        } else if (event.getName().equals("Sergeant Damien")) {
            Log.info("We got sergeant lets do it!");
            Time.sleep(random(500, 1000));
        } else {
            Time.sleep(random(200, 500));
            event.interact("Dismiss");
            Time.sleep(random(300, 500));
        }

    }

    private void logChat(String text) {
        LocalDateTime timestamp = LocalDateTime.now();
        if (!new File(Script.getDataDirectory() + "\\Koko\\Logs\\Screenshots").exists()) {
            new File(Script.getDataDirectory() + "\\Koko\\Logs\\Screenshots").mkdirs();
        }

        try (FileWriter fw = new FileWriter(Script.getDataDirectory() + "\\Koko\\Logs\\Randoms.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(timestamp + "> " + text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }
}