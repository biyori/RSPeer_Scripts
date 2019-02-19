import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Pickables;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.RenderListener;
import org.rspeer.runetek.event.types.RenderEvent;
import org.rspeer.script.Script;
import org.rspeer.script.ScriptCategory;
import org.rspeer.script.ScriptMeta;
import org.rspeer.ui.Log;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;

@ScriptMeta(name = "Sears Agility", desc = "Does the rooftop agility course at Seers", developer = "Koko", version = 1.0, category = ScriptCategory.AGILITY)
public class Sears extends Script implements RenderListener {

    private int startXP, startLvl;
    private int marks = 0, laps = 0, eatPercent, runPercent;
    private Area afterRope = Area.rectangular(2710, 3481, 2715, 3477, 2);
    private String status;
    private long startTime;
    private Skill training = Skill.AGILITY;
    private boolean takeScreenie = false;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();

        startXP = Skills.getExperience(training);
        startLvl = Skills.getCurrentLevel(training);
        status = "Loading up!";
        eatPercent = random(40, 80);
        runPercent = random(20, 40);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled() && Movement.getRunEnergy() > runPercent) {
            Movement.toggleRun(true);
            runPercent = random(20, 40);
            Log.info("When we run out of energy we will run again at " + runPercent + "%");
        }

        if (Local.isHealthBarVisible()) {
            Log.info("We fell! Restart course!");
            Time.sleep(random(1000, 2000));
        }

        if (Skills.getCurrentLevel(training) == 76)
            setStopping(true);

        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (Health.getPercent() <= eatPercent && SceneObjects.getNearest(11373) != null) {
            SceneObject bank = SceneObjects.getNearest("Bank booth");
            if (bank != null && !Inventory.contains("Jug of wine")) {
                if (!Bank.isOpen()) {
                    bank.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 500, 10000);
                } else {
                    if (Inventory.contains("Jug")) {
                        Bank.depositInventory();
                        Time.sleepUntil(Inventory::isEmpty, 500, 10000);
                    }
                    Bank.withdraw("Jug of wine", 5);
                    Time.sleepUntil(() -> Inventory.contains("Jug of wine"), 500, 10000);
                }
            } else if (Inventory.contains("Jug of wine")) {
                Inventory.getFirst("Jug of wine").interact("Drink");
                Time.sleepUntil(Local::isAnimating, 500, 10000);
                eatPercent = random(40, 80);
            }

        } else {
            if (SceneObjects.getNearest(11373) != null) { //start
                Log.info("Health: " + Health.getPercent() + "% we will eat at " + eatPercent + "%");
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Starting the course!";
                    SceneObject start = SceneObjects.getNearest(11373);
                    start.interact("Climb-up");
                    Time.sleepUntil(() -> SceneObjects.getNearest(11374) != null, 500, 10000);
                    Time.sleep(random(300, 500));
                }
            } else if (SceneObjects.getNearest(11374) != null && Local.distance(SceneObjects.getNearest(11374)) < 20) {
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Jumping";
                    SceneObject jump = SceneObjects.getNearest(11374);
                    jump.interact("Jump");
                    Time.sleepUntil(() -> Local.isHealthBarVisible() || SceneObjects.getNearest(11378) != null, 500, 10000);
                    Time.sleep(random(1300, 1575));
                }
            } else if (SceneObjects.getNearest(11378) != null && !afterRope.contains(Local) && Local.distance(SceneObjects.getNearest(11377)) > 20) {
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Crossing the scary rope";
                    SceneObject crossRope = SceneObjects.getNearest(11378);
                    crossRope.interact("Cross");
                    Time.sleepUntil(() -> Local.isHealthBarVisible() || afterRope.contains(Local), 500, 10000);
                    Time.sleep(random(400, 600));
                }
            } else if (SceneObjects.getNearest(11375) != null && Local.distance(SceneObjects.getNearest(11375)) < 21 && Local.distance(SceneObjects.getNearest(11377)) > 20) {
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Jumping";
                    SceneObject jump = SceneObjects.getNearest(11375);
                    jump.interact("Jump");
                    Time.sleepUntil(() -> Local.isHealthBarVisible() || SceneObjects.getNearest(11376) != null, 500, 10000);
                    Time.sleep(random(700, 900));
                }
            } else if (SceneObjects.getNearest(11376) != null) {
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Jumping";
                    SceneObject jumpAgain = SceneObjects.getNearest(11376);
                    jumpAgain.interact("Jump");
                    Time.sleepUntil(() -> Local.isHealthBarVisible() || SceneObjects.getNearest(11377) != null, 500, 10000);
                    Time.sleep(random(400, 600));
                }
            } else if (SceneObjects.getNearest(11377) != null && Local.distance(SceneObjects.getNearest(11375)) > 1) {
                if (markofGraceSpotted()) {
                    status = "Looting mark of grace";
                    lootGrace();
                } else {
                    status = "Finishing the course";
                    SceneObject finish = SceneObjects.getNearest(11377);
                    finish.interact("Jump");
                    Time.sleepUntil(() -> Local.isHealthBarVisible() || SceneObjects.getNearest(11373) != null, 500, 10000);
                    Time.sleep(random(300, 500));
                    laps++;
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
        g2.fillRect(20, 250, 235, 80);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 250, 235, 80);

        int x = 25;
        int y = 265;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        String Grace = marks == 0 ? "None" : marks + " (" + String.format("%.2f", getPerHour(marks)) + "/hr)";
        String lvlsGained = (Skills.getCurrentLevel(training) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(training) - startLvl) + ")" : "";


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
        g2.drawString(" (Laps: " + laps + " [" + String.format("%.2f", getPerHour(laps)) + "/hr])", x + width, y);

        g2.drawString("Agility lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Agility lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(training)) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Agility lvl: " + Skills.getLevelAt(Skills.getExperience(training)) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Marks of Grace: ", x, y += 15);
        width = metrics.stringWidth("Marks of Grace: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(Grace, x + width, y);
        g2.setColor(Color.WHITE);

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }
    }

    @Override
    public void onStop() {
        Log.info("Hope you had fun running around! Collected " + marks + " marks of grace and earned " + (Skills.getExperience(Skill.AGILITY) - startXP) + " agility xp this session");
        Game.logout();
    }

    private boolean markofGraceSpotted() {
        return Pickables.getNearest("Mark of grace") != null && Pickables.getNearest("Mark of grace").isPositionWalkable();
    }

    private void lootGrace() {
        Pickable MOG = Pickables.getNearest("Mark of grace");
        if (MOG != null) {
            MOG.interact("Take");
            Time.sleepUntil(() -> !markofGraceSpotted(), 500, 10000);
            marks++;
        }
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

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Sears\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Sears\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Sears\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Sears\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
        }
    }

    private void logChat(String text) {
        LocalDateTime timestamp = LocalDateTime.now();

        if (!new File(getDataDirectory() + "\\Koko\\Sears\\Screenshots").exists()) {
            new File(getDataDirectory() + "\\Koko\\Sears\\Screenshots").mkdirs();
        }

        try (FileWriter fw = new FileWriter(getDataDirectory() + "\\Koko\\Sears\\Randoms.txt", true);
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