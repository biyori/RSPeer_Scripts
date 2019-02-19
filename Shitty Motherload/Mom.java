import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.DepositBox;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.ChatMessageListener;
import org.rspeer.runetek.event.listeners.RenderListener;
import org.rspeer.runetek.event.types.ChatMessageEvent;
import org.rspeer.runetek.event.types.ChatMessageType;
import org.rspeer.runetek.event.types.RenderEvent;
import org.rspeer.script.Script;
import org.rspeer.script.ScriptCategory;
import org.rspeer.script.ScriptMeta;
import org.rspeer.ui.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;

@ScriptMeta(name = "Motherload RSP", desc = "Motherload all day, all night!", developer = "Koko", version = 1.0, category = ScriptCategory.MINING)
public class Mom extends Script implements RenderListener, ChatMessageListener {
    private int startXP, startLvl;
    private long startTime = 0;
    private String status = "Booting up!";
    private boolean sackFull = false;
    private Area veinWorkArea = Area.rectangular(3765, 5686, 3773, 5671, 0);
    private int stage = 1, paydirt = 0, nuggets = 0;
    private boolean takeScreenie = false, showWarning = false;
    private RandomHandler randomEvent = new RandomHandler();

    private Area[] onlyMineTheseRocks = {
            Area.rectangular(3765, 5686, 3770, 5680),
            Area.rectangular(3767, 5677, 3773, 5676),
            Area.rectangular(3764, 5672, 3769, 5671)
    };

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.MINING);
        startLvl = Skills.getCurrentLevel(Skill.MINING);
        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        }

        Log.info("Current Stage: " + getStage());
        switch (getStage()) {
            case 1:
                if (veinWorkArea.contains(Local) && !Local.isAnimating() && !Inventory.isFull()) {
                    status = "[#" + getStage() + "] Mining veins";
                    mineVeins();
                } else if (Local.getAnimation() == 335) {
                    goAFK();
                } else if (!veinWorkArea.contains(Local)) {
                    status = "[#" + getStage() + "] Entering tunnel";
                    enterTunnel();
                    Time.sleep(random(1200, 1500));
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() >= 0) {
                    status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                    setStage(2);
                }
                break;
            case 2:
                if (Inventory.isFull() && sackSum() == 0) {
                    status = "[#" + getStage() + "] Loading hopper";
                    loadHopper();
                } else if (!veinWorkArea.contains(Local)) {
                    status = "[#" + getStage() + "] Entering tunnel";
                    enterTunnel();
                    Time.sleep(1000, 1500);
                } else if (veinWorkArea.contains(Local) && !Local.isAnimating() && !Inventory.isFull()) {
                    status = "[#" + getStage() + "] Mining veins";
                    mineVeins();
                } else if (Local.getAnimation() == 335) {
                    goAFK();
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 28) {
                    for (int i = 0; i < 4; i++) {
                        status = "[#" + getStage() + "] Dropping " + (4 - i) + " dirt";
                        Inventory.getFirst("Pay-dirt").interact("Drop");
                        Time.sleep(random(700, 1000));
                    }
                    if (countDirt() == 24) {
                        status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                        setStage(3);
                    }
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 52) {
                    status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                    setStage(4);
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 80) {
                    status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                    setStage(5);
                }
                break;
            case 3:
                if (sackSum() == 28 && countDirt() == 24) { //maybe this will work?
                    status = "[#" + getStage() + "] Loading hopper";
                    loadHopper();
                } else if (!veinWorkArea.contains(Local)) {
                    status = "[#" + getStage() + "] Entering tunnel";
                    enterTunnel();
                    Time.sleep(1000, 1500);
                } else if (veinWorkArea.contains(Local) && !Local.isAnimating() && !Inventory.isFull()) {
                    status = "[#" + getStage() + "] Mining veins";
                    mineVeins();
                } else if (Local.getAnimation() == 335) {
                    goAFK();
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 52) {
                    status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                    setStage(4);
                }
                break;
            case 4:
                if (Inventory.isFull() && sackSum() == 52) {
                    status = "[#" + getStage() + "] Loading hopper";
                    loadHopper();
                } else if (!veinWorkArea.contains(Local)) {
                    status = "[#" + getStage() + "] Entering tunnel";
                    enterTunnel();
                    Time.sleep(1000, 1500);
                } else if (veinWorkArea.contains(Local) && !Local.isAnimating() && !Inventory.isFull()) {
                    status = "[#" + getStage() + "] Mining veins";
                    mineVeins();
                } else if (Local.getAnimation() == 335) {
                    goAFK();
                } else if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 80) {
                    status = "[#" + getStage() + "] Going to stage " + (getStage() + 1) + "!";
                    setStage(5);
                }
                break;
            case 5:
                if (Inventory.isFull() && Inventory.contains("Pay-dirt") && sackSum() == 80) {
                    status = "[#" + getStage() + "] Loading hopper";
                    loadHopper();
                    Time.sleep(1000, 2000);
                    sackFull = true;
                } else if (sackFull && !Inventory.isFull() && sackSum() > 0) {
                    status = "[#" + getStage() + "] Emptying ore sack";
                    emptySack();
                } else if (sackFull && Inventory.contains(item -> item.getName().endsWith("ore") || item.getName().equals("Coal"))) {
                    status = "[#" + getStage() + "] Depositing ore";
                    sumGoldenNuggets();
                    useDepositBox();
                } else if (sackSum() == 0 && Inventory.isEmpty()) {
                    status = "[#" + getStage() + "] Resetting to Stage 1!";
                    sackFull = false;
                    setStage(1);
                }
                break;
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.MINING);//500
        int gainedXp = Skills.getExperience(Skill.MINING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }

        if (showWarning) {
            java.awt.EventQueue.invokeLater(() -> {
                JOptionPane optionPane = new JOptionPane("RANDOM EVENT DETECTED!", JOptionPane.WARNING_MESSAGE);
                JDialog dialog = optionPane.createDialog("Warning!");
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
            });
            showWarning = false;
        }

        g2.setColor(Color.YELLOW);
        int x = 25;
        int y = 250;
        String lvlsGained = (Skills.getCurrentLevel(Skill.MINING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.MINING) - startLvl) + ")" : "";
        g2.drawString("Mining lvl: " + Skills.getCurrentLevel(Skill.MINING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x, y);
        g2.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", x, y += 15);
        g2.drawString("Mined Dirt: " + paydirt + " [" + String.format("%.2f", getPerHour(paydirt)) + "/hr]", x, y += 15);
        g2.drawString("Nuggets: " + nuggets + " [" + String.format("%.2f", getPerHour(nuggets)) + "/hr]", x, y += 15);
        g2.drawString("Time Ran: " + formatTime(System.currentTimeMillis() - startTime), x, y += 15);
        g2.drawString("Status: " + status, 25, y += 15);
    }

    @Override
    public void notify(ChatMessageEvent c) {
        if (c.getType().equals(ChatMessageType.FILTERED) && (c.getMessage().startsWith("You manage to mine"))) {
            paydirt++;
        }
    }

    @Override
    public void onStop() {
        Log.info("Ran for " + formatTime(System.currentTimeMillis() - startTime) + " and mined " + paydirt + " Pay-dirt's while earning " + (Skills.getExperience(Skill.MINING) - startXP) + " mining xp this session");
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private String formatTime(final long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void enterTunnel() {
        SceneObject tunnel = SceneObjects.getNearest("Dark tunnel");
        if (tunnel != null) {
            Time.sleep(random(500, 1000));
            tunnel.interact(x -> true);
            Time.sleepUntil(() -> veinWorkArea.contains(Players.getLocal()), random(400, 500), 10000);
        }
    }

    private void mineVeins() {
        if (Inventory.contains(item -> item.getName().startsWith("Uncut"))) {
            Inventory.getFirst(item -> item.getName().startsWith("Uncut")).interact("Drop");
            Time.sleep(random(400, 600));
        }
        SceneObject rock = SceneObjects.getNearest(vein -> vein.getName().equals("Ore vein") && vein.containsAction("Mine") && isRockinArea(vein));
        if (rock != null) {
            Position rockPlus = new Position(rock.getPosition().getX() + 1, rock.getPosition().getY());
            Position rockMinus = new Position(rock.getPosition().getX() - 1, rock.getPosition().getY());
            if (rockPlus.isPositionWalkable() || rockMinus.isPositionWalkable()) {
                status = "Mining " + rock.getName();
                if (SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")) != null && Players.getLocal().distance(SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")).getPosition()) < 2)
                    clearRockFall();

                rock.interact(x -> true);
                Log.info("Clicked: " + rock.getName() + " action(s): " + Arrays.toString(rock.getActions()));
                Time.sleepUntil(() -> Players.getLocal().getAnimation() == 335 || (Players.getLocal().distance(SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")).getPosition()) < 2), random(400, 500), 5000);
            } else {
                Log.info("Clearing rocks to access vein");
                clearRockFall();
                Time.sleep(random(300, 500));
            }
        } else {
            Log.info("Cannot find any veins to mine!");
            int sleep = random(5000, 10000);
            Log.info("Sleeping for " + sleep + " till new rocks spawn");
            Time.sleep(sleep);
        }
    }

    private void clearRockFall() {
        SceneObject rockfall = SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall") && veinWorkArea.contains(blockage));
        if (rockfall != null) {
            Log.info("Clearing rock at " + rockfall.getPosition());
            rockfall.interact(x -> true);
            Time.sleepUntil(() -> Players.getLocal().isAnimating() || SceneObjects.getAt(rockfall.getPosition())[0] == null, random(200, 500), random(950, 1000));
        }
    }

    private void setStage(int n) {
        stage = n;
    }

    private int getStage() {
        return stage;
    }

    private int countDirt() {
        if (!Inventory.contains("Pay-dirt")) {
            return 0;
        } else {
            Item[] paydirt = Inventory.getItems();
            int count = 0;
            for (Item dirt : paydirt) {
                if (dirt != null && dirt.getName().equals("Pay-dirt")) {
                    count++;
                }
            }
            return count;
        }
    }

    private int sackSum() {
        int sum = 0;
        InterfaceComponent sackVal = Interfaces.getComponent(382, 4, 2);
        if (sackVal != null) {
            sum = Integer.parseInt(sackVal.getText());
        }
        Log.info("Sack sum: " + sum);
        return sum;
    }

    private void sumGoldenNuggets() {
        int nug = Inventory.getCount(true, "Golden nugget");
        if (nug > 0) {
            nuggets += nug;
        }
    }

    private void emptySack() {
        SceneObject sack = SceneObjects.getNearest("Sack");
        if (sack != null) {
            sack.interact(x -> true);
            Time.sleepUntil(() -> Inventory.isFull() || Inventory.contains(item -> item.getName().endsWith("ore") || item.getName().startsWith("Coal")), random(400, 500), 10000);
        }
    }

    private boolean isRockinArea(SceneObject rock) {
        for (Area onlyMineTheseRock : onlyMineTheseRocks) {
            if (onlyMineTheseRock.contains(rock))
                return true;
        }
        return false;
    }

    private void useDepositBox() {
        SceneObject depositBox = SceneObjects.getNearest("Bank deposit box");
        if (depositBox != null) {
            if (!DepositBox.isOpen()) {
                Log.info("Deposit box is NOT open!");
                depositBox.interact("Deposit");
                Time.sleepUntil(DepositBox::isOpen, random(400, 500), 10000);
                useDepositBox();
            } else {
                Log.info("Deposit box is ready to deposit!");
                InterfaceComponent deposit = Interfaces.getComponent(192, 4);
                if (deposit != null) {
                    deposit.interact("Deposit inventory");
                    Time.sleepUntil(Inventory::isEmpty, random(400, 500), 10000);
                }
                if (Inventory.isEmpty()) {
                    DepositBox.close();
                    Time.sleep(random(200, 500));
                }
            }
        }
    }

    private void loadHopper() {
        if (veinWorkArea.contains(Players.getLocal())) {
            SceneObject tunnel = SceneObjects.getNearest("Dark tunnel");
            if (tunnel != null) {
                tunnel.interact("Enter");
                Time.sleepUntil(() -> !veinWorkArea.contains(Players.getLocal()) || Players.getLocal().distance(SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")).getPosition()) < 2, random(400, 500), 5000);

                if (SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")) != null && Players.getLocal().distance(SceneObjects.getNearest(blockage -> blockage.getName().equals("Rockfall")).getPosition()) < 2)
                    clearRockFall();
            }
        } else {
            SceneObject hopper = SceneObjects.getNearest("Hopper");
            if (hopper != null) {
                Time.sleep(random(1000, 2000));
                hopper.interact(x -> true);
                Time.sleepUntil(() -> Inventory.isEmpty() || !Players.getLocal().isMoving(), random(400, 500), 10000);
            }
        }
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Mother\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Mother\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Mother\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Mother\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
        }
    }

    private void goAFK() {
        status = "[#" + getStage() + "] Going AFK";
        int sleepN = random(10000, 20000);
        int timer = sleepN / 1000;
        Log.info("[#" + getStage() + "] Sleeping for " + timer + "." + (sleepN % 1000) + "s");

        for (int i = 0; i < (sleepN / 1000); i++) {
            status = "[#" + getStage() + "] Sleeping for " + timer + "s";
            timer--;
            Time.sleep(1000);
        }

        status = "[#" + getStage() + "] Sleeping for " + (sleepN % 1000) + "ms";
        Time.sleep(sleepN % 1000);
    }

    private void logChat(String text) {
        LocalDateTime timestamp = LocalDateTime.now();

        if (!new File(getDataDirectory() + "\\Koko\\Mother\\Screenshots").exists()) {
            new File(getDataDirectory() + "\\Koko\\Mother\\Screenshots").mkdirs();
        }

        try (FileWriter fw = new FileWriter(getDataDirectory() + "\\Koko\\Mother\\Randoms.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(timestamp + "> " + text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }
}