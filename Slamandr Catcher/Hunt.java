import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Pickables;
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
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;

@ScriptMeta(name = "Salamander Kong", desc = "Destroys the natural environment of Salamanders", developer = "Koko", version = 1.0, category = ScriptCategory.HUNTER)
public class Hunt extends Script implements RenderListener, ChatMessageListener {

    private int startMageXP, startHuntlvl, startHuntXP, startMagiclvl, salamanders = 0, runPercent;
    private boolean takeScreenie = false, nearbyPLAY = false;
    private String status;
    private long startTime;
    private Area workArea = Area.rectangular(2446, 3228, 2451, 3222);
    private Area workArea60 = Area.rectangular(2446, 3228, 2453, 3219);
    private Area walkArea = Area.rectangular(2447, 3228, 2449, 3222);
    private Area walkArea60 = Area.rectangular(2447, 3228, 2452, 3221);
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startHuntXP = Skills.getExperience(Skill.HUNTER);
        startHuntlvl = Skills.getLevelAt(startHuntXP);
        startMageXP = Skills.getExperience(Skill.MAGIC);
        startMagiclvl = Skills.getLevelAt(startMageXP);

        if (startHuntlvl >= 60) {
            workArea = workArea60;
            walkArea = walkArea60;
        }
        runPercent = random(20, 40);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        Player[] nearbyPlayers = Players.getLoaded();
        if (nearbyPlayers.length > 1) {
            if (!nearbyPLAY) {
                java.awt.EventQueue.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane("NEARBY PLAYER!", JOptionPane.WARNING_MESSAGE);
                    JDialog dialog = optionPane.createDialog("Warning!");
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                });
                nearbyPLAY = true;
            }
            for (Player nearbyPlayer : nearbyPlayers) {
                if (nearbyPlayer != Local)
                    Log.info("Local player: " + nearbyPlayer.getName() + " (Lvl " + nearbyPlayer.getCombatLevel() + ") is nearby!");
            }
            Time.sleep(random(2000, 3000));
        } else {
            nearbyPLAY = false;
        }

        if (!Movement.isRunEnabled() && Movement.getRunEnergy() > runPercent) {
            Movement.toggleRun(true);
            runPercent = random(20, 40);
            Log.info("When we run out of energy we will run again at " + runPercent + "%");
        }


        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (trapOnGround()) {
            status = "Picking up traps";
            Pickable rope = Pickables.getNearest("Rope");
            Pickable net = Pickables.getNearest("Small fishing net");

            if (rope != null) {
                Log.info("Picking up rope at " + rope.getPosition());
                rope.interact("Take");
                int invSpace = Inventory.getCount();
                Time.sleepUntil(() -> Inventory.getCount() > invSpace, 500, 5000);
            }
            if (net != null) {
                Log.info("Picking up net at " + net.getPosition());
                net.interact("Take");
                int invSpace = Inventory.getCount();
                Time.sleepUntil(() -> Inventory.getCount() > invSpace, 500, 5000);
            }
        } else if (SceneObjects.getNearest(trap -> workArea.contains(trap) && trap.getName().equals("Net trap") && trap.containsAction("Check")) != null) {
            status = "Checking traps!";
            SceneObject trap = SceneObjects.getNearest(s -> workArea.contains(s) && s.getName().equals("Net trap") && s.containsAction("Check"));
            if (trap != null) {
                Log.info("Checking trap at " + trap.getPosition());
                trap.interact("Check");
                int invSpace = Inventory.getCount();
                Time.sleepUntil(() -> Inventory.getCount() > invSpace, 500, 5000);
                Time.sleep(random(500, 1000));
            }
        } else if (SceneObjects.getNearest(tree -> workArea.contains(tree) && tree.getName().equals("Young tree") && tree.containsAction("Set-trap")) != null) {
            status = "Setting up traps";
            SceneObject tree = SceneObjects.getNearest(s -> workArea.contains(s) && s.getName().equals("Young tree") && s.containsAction("Set-trap"));
            if (tree != null) {
                Log.info("Setting up trap at " + tree.getPosition());
                tree.interact("Set-trap");
                Time.sleepUntil(() -> Local.getAnimation() == 5215, 500, 5000);
                Time.sleep(random(1200, 2100));
            }
        } /*else if (outOfWater()) {
            status = "Restoring waterskins";
            Log.info("Casting humidify");
            Magic.cast(Spell.Lunar.HUMIDIFY);
            Time.sleepUntil(() -> Inventory.contains("Waterskin(4)"), 500, 10000);
            Time.sleep(random(3000, 4000));
        } */ else {
            status = "Idle";
            Movement.walkTo(walkArea.getCenter().randomize(2));

            if (Inventory.contains("Red salamander")) {
                status = "Dropping salamanders";
                Log.info("Dropping salamander");
                Item[] salamandr = Inventory.getItems();
                for (Item item : salamandr) {
                    if (item.getName().equals("Red salamander")) {
                        item.interact("Release");
                        Time.sleep(random(300, 500));
                    }
                }
            }

            int sleep = random(5000, 15000);
            int counter = sleep / 1000;
            for (int i = 0; i < sleep / 1000; i++) {
                status = "Sleeping for " + counter + "s";
                Time.sleep(1000);
                counter--;
                if (SceneObjects.getNearest(trap -> workArea.contains(trap) && trap.getId() == 8986) != null || trapOnGround()) {
                    status = "Sleep interrupted!";
                    return random(500, 800);//is it okay to return out of a loop?
                }
            }
            status = "Sleeping for " + sleep % 1000 + "ms";
            Time.sleep(sleep % 1000);
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextHuntLvl = Skills.getExperienceToNextLevel(Skill.HUNTER);
        int nextMageLvl = Skills.getExperienceToNextLevel(Skill.MAGIC);
        int gainedHuntXp = Skills.getExperience(Skill.HUNTER) - startHuntXP;
        int gainedMageXp = Skills.getExperience(Skill.MAGIC) - startMageXP;
        double ttl = (nextHuntLvl / (getPerHour(gainedHuntXp) / 60.0 / 60.0 / 1000.0));
        double ttl2 = (nextMageLvl / (getPerHour(gainedMageXp) / 60.0 / 60.0 / 1000.0));
        if (gainedHuntXp == 0) {
            ttl = 0;
            ttl2 = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 220, 235, 125);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 220, 235, 125);

        int x = 25;
        int y = 235;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        String huntLvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.HUNTER)) - startHuntlvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.HUNTER)) - startHuntlvl) + ")" : "";
        String mageLvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) - startMagiclvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) - startMagiclvl) + ")" : "";


        g2.setColor(Color.WHITE);
        g2.drawString("Salamanders caught: ", x, y);
        int width = metrics.stringWidth("Salamanders caught: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(salamanders), x + width, y);
        width = metrics.stringWidth("Salamanders caught: " + formatter.format(salamanders));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(salamanders)) + "/hr)", x + width, y);

        g2.drawString("Hunter lvl: ", x, y += 15);
        g2.setColor(Color.ORANGE);
        width = metrics.stringWidth("Hunter lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.HUNTER)) + huntLvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Hunter lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.HUNTER)) + huntLvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedHuntXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedHuntXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedHuntXp)) + "/hr)", x + width, y);

        g2.drawString("Magic lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Magic lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) + mageLvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Magic lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) + mageLvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl2).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedMageXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedMageXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedMageXp)) + "/hr)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime), x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Waterskin capacity: ", x, y += 15);
        width = metrics.stringWidth("Waterskin capacity: ");
        g2.setColor(Color.CYAN);
        g2.drawString(String.valueOf(waterSkinVals()), x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Status: ", x, y += 15);
        width = metrics.stringWidth("Status: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(status, x + width, y);


        //Hide username
        if (Players.getLocal() != null) {
            Color tanColor = new Color(204, 187, 154);
            g2.setColor(tanColor);
            g2.fillRect(9, 459, 91, 15);
        }

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }
    }

    @Override
    public void notify(ChatMessageEvent event) {
        String m = event.getMessage().toLowerCase();
        if (event.getType().equals(ChatMessageType.FILTERED) && (m.endsWith("caught a red salamander."))) {
            salamanders++;
        }
    }

    @Override
    public void onStop() {
        Log.info("Hope you had fun hunting! Gained " + (Skills.getExperience(Skill.HUNTER) - startHuntXP) + " hunter experience and " + (Skills.getExperience(Skill.MAGIC) - startMageXP) + " magic experience this session");
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private boolean trapOnGround() {
        return Pickables.getNearest("Rope", "Small fishing net") != null && workArea.contains(Pickables.getNearest("Rope", "Small fishing net"));
    }

    private boolean outOfWater() {
        Item[] items = Inventory.getItems();
        int skin = 3;
        for (Item item : items) {
            if (item.getName().equals("Waterskin(0)"))
                skin--;
        }
        return skin == 0;
    }

    private int waterSkinVals() {
        Item[] items = Inventory.getItems();
        int capacity = 0;
        for (Item item : items) {
            if (item.getName().contains("Waterskin"))
                capacity += Integer.parseInt(item.getName().substring(10, 11));
        }
        return capacity;
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Hunter\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
        }
    }

    private String formatTime(final long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }
}