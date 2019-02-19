import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Combat;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;

@ScriptMeta(name = "Teaky Mon", desc = "Chops Teaks n shit on fossil island patches", developer = "Koko", version = 1.0, category = ScriptCategory.WOODCUTTING)
public class Teaky extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl;
    private int teaks = 0, nestCnt = 0, lastAnimDelay;
    private String status;
    private long startTime, lastAnim;
    private boolean takeScreenie = false;
    private RandomHandler randomEvent = new RandomHandler();
    private Skill training = Skill.WOODCUTTING;

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(training);
        startLvl = Skills.getLevelAt(startXP);
        status = "Loading up!";
        lastAnimDelay = random(1000, 15000);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            status = "Handling random event";
            Time.sleep(random(3000, 6000));
            takeScreenie = true;
            randomEvent.handleRandom();
            lastAnimDelay = 0;
        } else if (Inventory.isFull()) {
            int sleep = random(1000, 20000);
            int timer = sleep / 1000;
            for (int i = 0; i < sleep / 1000; i++) {
                status = "Sleeping for " + timer + "s";
                Time.sleep(1000);
                timer--;
            }
            status = "Sleeping for " + (sleep % 1000) + "ms";
            Time.sleep(sleep % 1000);

            status = "Dropping logs";
            int[] indexes = dropOrder();
            for (int index : indexes) {
                Item log = Inventory.getItemAt(index);
                if (log != null && log.getName().equals("Teak logs")) {
                    log.interact("Drop");
                    Time.sleep(random(200, 300));
                }
            }
            lastAnimDelay = 0;
        } else if (SceneObjects.getNearest("Teak Tree") == null) {
            status = "No trees to chop! Waiting...";
            Time.sleep(random(5000, 1000));
        } else {
            if (Local.isAnimating())
                lastAnim = System.currentTimeMillis();

            long msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                if (Combat.getSpecialEnergy() == 100) {
                    Combat.toggleSpecial(true);
                    Time.sleep(random(200, 300));
                }
                SceneObject tree = SceneObjects.getNearest("Teak Tree");
                if (tree != null) {
                    Log.info("Time to chop some teaks!");
                    status = "Ready to chop";
                    tree.interact("Chop down");
                    Time.sleepUntil(Local::isAnimating, 1000, 10000);
                    lastAnimDelay = random(1000, 20000);
                }
            } else {
                status = "Chop chop!";
                if (msSinceAnim > 0)
                    status += " " + (lastAnimDelay - msSinceAnim) + "ms";
                Time.sleep(random(300, 1000));

                if (nestSpotted()) {
                    Time.sleep(random(3000, 5000));
                    if (Inventory.isFull()) {
                        Inventory.getFirst("Teak log").interact("Drop");
                        Time.sleep(random(500, 1000));
                    }
                    lootNest();
                    lastAnimDelay = 0;
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(training);//500
        int gainedXp = Skills.getExperience(training) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 235, 196, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 196, 95);

        int x = 25;
        int y = 250;
        String wcLvlsGained = (Skills.getLevelAt(Skills.getExperience(training)) - startLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(training)) - startLvl) + ")" : "";

        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        g2.setColor(Color.WHITE);
        g2.drawString("Status: ", x, y);
        int width = metrics.stringWidth("Status: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(status, x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime), x + width, y);
        width = metrics.stringWidth("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime));
        g2.setColor(Color.WHITE);

        g2.drawString("Woodcut lvl: ", x, y += 15);
        g2.setColor(Color.GREEN);
        width = metrics.stringWidth("Woodcut lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(training)) + wcLvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Woodcut lvl: " + Skills.getLevelAt(Skills.getExperience(training)) + wcLvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Teaks cut: ", x, y += 15);
        width = metrics.stringWidth("Teaks cut: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(teaks), x + width, y);
        width = metrics.stringWidth("Teaks cut: " + formatter.format(teaks));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(teaks)) + "/hr)", x + width, y);

        g2.drawString("Nests looted: ", x, y += 15);
        width = metrics.stringWidth("Nests looted: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(nestCnt), x + width, y);
        width = metrics.stringWidth("Nests looted: " + formatter.format(nestCnt));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(nestCnt)) + "/hr)", x + width, y);

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
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (event.getMessage().endsWith("teak logs.")) {
                teaks++;
            }
        }
    }

    @Override
    public void onStop() {
        Log.info("Hope you had fun chopping teaks! Chopped " + teaks + " earning " + (Skills.getExperience(training) - startXP) + " " + training.name().toLowerCase() + " xp and looting " + nestCnt + " bird nests.");
    }

    private boolean nestSpotted() {
        return Pickables.getNearest("Bird nest") != null && Pickables.getNearest("Bird nest").isPositionWalkable();
    }

    private void lootNest() {
        Pickable nest = Pickables.getNearest("Bird nest");
        if (nest != null) {
            nest.interact("Take");
            Time.sleepUntil(() -> !nestSpotted(), 500, 10000);
            nestCnt++;
        }
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private int[] dropOrder() {
        int[] drop1 = new int[]{0, 4, 8, 12, 16, 20, 24, 25, 21, 17, 13, 9, 5, 1, 2, 6, 10, 14, 18, 22, 26, 27, 23, 19, 15, 11, 7, 3};
        int[] drop2 = new int[]{0, 1, 4, 5, 8, 9, 12, 13, 16, 17, 20, 21, 24, 25, 26, 27, 22, 23, 18, 19, 14, 15, 10, 11, 6, 7, 2, 3};
        int[] drop3 = new int[]{0, 1, 2, 3, 7, 6, 5, 4, 8, 9, 10, 11, 15, 14, 13, 12, 16, 17, 18, 19, 23, 22, 21, 20, 24, 25, 26, 27};

        switch (random(1, 3)) {
            case 1:
                Log.info("Drop 1");
                return drop1;
            case 2:
                Log.info("Drop 2");
                return drop2;
            case 3:
                Log.info("Drop 3");
                return drop3;
        }
        return dropOrder();
    }

    private String formatTime(final long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Teak\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Teak\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Teak\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Teak\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
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