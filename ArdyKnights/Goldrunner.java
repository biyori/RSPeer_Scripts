import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.EquipmentSlot;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Npcs;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;

@ScriptMeta(name = "Ardy Knight Milker", desc = "Milks the fuk out of Ardy Knights", developer = "Koko", version = 1.0, category = ScriptCategory.THIEVING)
public class Goldrunner extends Script implements RenderListener, ChatMessageListener {

    private int startThieveXP, startThieveLvl, steal = 0, fail = 0, eatPercent, openPouch;
    private boolean takeScreenie = false;
    private String status;
    private long startTime, lastSteal = 0;
    private RandomHandler randomEvent = new RandomHandler();
    private Area westBank = Area.rectangular(2649, 3287, 2655, 3280);

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startThieveXP = Skills.getExperience(Skill.THIEVING);
        startThieveLvl = Skills.getLevelAt(startThieveXP);
        status = "Loading up!";
        eatPercent = random(40, 60);
        openPouch = random(0, 28);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();
        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (Health.getPercent() > eatPercent) {
            status = "Looking for trouble";
            if (Inventory.getFirst("Coin pouch") != null) {
                if (Inventory.getCount(true, "Coin pouch") == 28) {
                    Inventory.getFirst("Coin pouch").interact(x -> true);
                    Time.sleepUntil(() -> Inventory.getFirst("Coin pouch") == null, 1250, 10000);
                }
            }
            Npc knight = Npcs.getNearest(id -> id.getId() == 3108 && westBank.contains(id));
            if (knight != null) {
                if (Local.getAnimation() == 424) {
                    status = "Stunned";
                    if (Inventory.getFirst("Coin pouch") != null) {
                        if (Inventory.getCount(true, "Coin pouch") > openPouch) {
                            Inventory.getFirst("Coin pouch").interact(x -> true);
                            Time.sleepUntil(() -> Inventory.getFirst("Coin pouch") == null, 1250, 10000);
                            openPouch = random(0, 25);
                        }
                    }
                    Time.sleep(random(4000, 5000));
                    if (EquipmentSlot.NECK.getItem() == null) {
                        if (Inventory.contains("Dodgy necklace")) {
                            Inventory.getFirst("Dodgy necklace").interact("Wear");
                            Time.sleepUntil(() -> EquipmentSlot.NECK.getItem() != null, random(200, 300), 3000);
                        }
                    }
                } else {
                    status = "LOOT! Last action(" + (System.currentTimeMillis() - lastSteal) + "ms)";
                    knight.interact("Pickpocket");
                    lastSteal = System.currentTimeMillis();
                    Log.info("Stealing from knight at " + knight.getPosition());
                    Time.sleep(random(150, 210));
                }
            } else {
                Log.info("No Knight no gold!");
                setStopping(true);
            }
        } else {
            if (Inventory.contains(item -> item.containsAction("Drink"))) {
                int eat = random(2, 5);
                eatPercent = random(40, 60);
                for (int i = 0; i < eat; i++) {
                    if (Inventory.contains(item -> item.containsAction("Drink"))) {
                        Inventory.getFirst(item -> item.containsAction("Drink")).interact("Drink");
                        Time.sleepUntil(Local::isAnimating, 1250, 4000);
                        Time.sleep(random(500, 600));
                    } else
                        break;
                }
                for (int i = 0; i < 10; i++) {
                    if (Inventory.contains("Jug")) {
                        Inventory.getFirst("Jug").interact("Drop");
                        Time.sleep(random(500, 600));
                    } else
                        break;
                }
            } else {
                status = "Getting more food!";
                SceneObject booth = SceneObjects.getNearest("Bank booth");
                if (booth != null) {
                    if (!Bank.isOpen()) {
                        booth.interact("Bank");
                        Time.sleepUntil(() -> Bank.isOpen() || Interfaces.isOpen(213), 500, 5000);
                    }
                    if (Interfaces.isOpen(213)) {
                        status = "Waiting for Bank PIN entry";
                    }
                    if (Bank.isOpen()) {
                        if (Bank.contains("Jug of wine")) {
                            Bank.withdraw("Jug of wine", 20);
                            Time.sleepUntil(() -> Inventory.contains("Jug of wine"), 500, 5000);
                            Time.sleep(random(500, 750));
                        } else {
                            Log.info("Ran out of food!");
                            setStopping(true);
                        }
                        if (Bank.contains("Dodgy necklace")) {
                            Bank.withdrawAll("Dodgy necklace");
                            Time.sleepUntil(() -> Inventory.contains("Dodgy necklace"), 400, 4000);
                        }
                        Bank.close();
                    }
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.THIEVING);
        int gainedXp = Skills.getExperience(Skill.THIEVING) - startThieveXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (nextLvlXp == 0) {
            ttl = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 235, 200, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 200, 95);

        int x = 25;
        int y = 250;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        double thieveRate = ((double) steal / (double) (steal + fail)) * 100;
        String lvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) + ")" : "";

        g2.setColor(Color.WHITE);
        g2.drawString("Thieving lvl: ", x, y);
        g2.setColor(new Color(238, 130, 238));
        int width = metrics.stringWidth("Thieving lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Thieving lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Gold Stolen: ", x, y += 15);
        width = metrics.stringWidth("Gold Stolen: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(steal * 50 * 2), x + width, y);
        width = metrics.stringWidth("Gold Stolen: " + formatter.format(steal * 50 * 2));
        g2.drawString(" (" + formatter.format(getPerHour(steal * 50 * 2)) + "/hr)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Stunner: ", x, y += 15);
        width = metrics.stringWidth("Stunner: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(fail) + "/" + formatter.format(steal + fail), x + width, y);
        width = metrics.stringWidth("Stunner: " + formatter.format(fail) + "/" + formatter.format(steal + fail));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(thieveRate) + "% success)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime), x + width, y);

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

        //Hide username
        if (Players.getLocal() != null) {
            Color tanColor = new Color(204, 187, 154);
            g2.setColor(tanColor);
            g2.fillRect(9, 459, 91, 15);
        }
    }

    @Override
    public void notify(ChatMessageEvent event) {
        String m = event.getMessage().toLowerCase();
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if ((m.contains("you pick the") && (m.endsWith("pocket.")))) {
                steal++;
            }
            if (m.contains("you fail to pick")) {
                fail++;
            }
        }
    }

    @Override
    public void onStop() {
        int lvls = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl);
        Log.info("Hope you had fun stealing " + (steal * 50 * 2) + " gold! Gained " + (Skills.getExperience(Skill.THIEVING) - startThieveXP) + " thieving XP (+" + (lvls > 0 ? lvls : "0") + " levels).");
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\ArdyKnights\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\ArdyKnights\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\ArdyKnights\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\ArdyKnights\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
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