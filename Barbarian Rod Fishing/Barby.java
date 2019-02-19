import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
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

@ScriptMeta(name = "Barbarian Rod Fishing", desc = "Barbarian fishes with the rod", developer = "Koko", version = 1.0, category = ScriptCategory.FISHING)
public class Barby extends Script implements RenderListener, ChatMessageListener {

    private int startXP, fish = 0, startLvl, lastAnimDelay, feathers = 0;
    private String status;
    private long startTime, nextSpaceTime = System.currentTimeMillis() / 1000, lastAnim;
    private boolean DROPPING, takeScreenie;
    private Position fishingHole;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.FISHING);
        startLvl = Skills.getCurrentLevel(Skill.FISHING);
        lastAnimDelay = random(3000, 5000);
        DROPPING = false;
        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);

        if (!Inventory.contains("Feather"))
            setStopping(true);
        else
            feathers = Inventory.getCount(true, "Feather");


        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            status = "Handling random event";
            Time.sleep(random(3000, 6000));
            takeScreenie = true;
            if (Inventory.isFull()) {
                Inventory.getFirst(item -> item.getName().startsWith("Leaping")).interact("Drop");
                Time.sleep(random(500, 1000));
            }
            randomEvent.handleRandom();
            lastAnimDelay = 0;
        } else if (!Inventory.isFull() && !DROPPING) {
            //Fish
            Npc fishingSpot = Npcs.getNearest(1542); //Use-rod

            if (fishingSpot == null) {
                Movement.walkTo(new Position(2502, 3498, 0));
                return random(200, 300);
            }

            if (local.isAnimating())
                lastAnim = System.currentTimeMillis();

            long msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (!local.isAnimating() && msSinceAnim > lastAnimDelay) {
                status = "Attempting to fish";
                fishingSpot.interact("Use-rod");
                recordFishingPos(fishingSpot.getPosition());//save location
                Time.sleepUntil(() -> local.isAnimating() || isFishingSpotDed(), 1200, 10000);
                Time.sleep(random(751, 3500));
                lastAnimDelay = random(3000, 5000);
            } else {
                if (isFishingSpotDed()) {
                    int sleepN = random(9000, 55000);
                    Log.info("Fishing spot is ded! Sleeping for " + (sleepN / 1000) + " seconds");

                    int timer = sleepN / 1000;
                    for (int i = 0; i < sleepN / 1000; i++) {
                        status = "Fishing spot ded. Sleeping for " + timer + "s";
                        Time.sleep(1000);
                        timer--;
                    }
                    status = "Fishing spot ded. Sleeping for " + (sleepN % 1000) + "ms";
                    Time.sleep(sleepN % 1000);
                } else {
                    int val = random(250, 1500);
                    status = "AFK fishing [" + (nextSpaceTime - System.currentTimeMillis() / 1000) + "s]";
                    Time.sleep(val);

                    if (nextSpaceTime < System.currentTimeMillis() / 1000) {
                        Log.info("Sending enter!");
                        Keyboard.pressEnter();
                        Time.sleep(random(500, 1000));
                        nextSpaceTime = System.currentTimeMillis() / 1000 + random(15, 44);
                        Log.info("Next enter will be in " + (nextSpaceTime - System.currentTimeMillis() / 1000) + "s");
                    }
                }
            }
        } else {
            if (Inventory.isFull()) {
                DROPPING = true;
                int sleepN = random(9000, 20000);

                Log.info("Inventory full. Sleeping for " + (sleepN / 1000) + " seconds");

                int timer = sleepN / 1000;
                for (int i = 0; i < sleepN / 1000; i++) {
                    status = "Inventory full. Sleeping for " + timer + "s";
                    Time.sleep(1000);
                    timer--;
                }
                status = "Inventory full. Sleeping for " + (sleepN % 1000) + "ms";
                Time.sleep(sleepN % 1000);

                //Drop one to get out of this if()
                Inventory.getFirst(item -> item.getName().startsWith("Leaping")).interact("Drop");
                Time.sleep(random(300, 400));
            }
            if (Inventory.contains(item -> item.getName().startsWith("Leaping"))) {
                int[] indexes = dropOrder();
                for (int index : indexes) {
                    Item fish = Inventory.getItemAt(index);
                    if (fish != null && fish.getName().startsWith("Leaping")) {
                        fish.interact("Drop");
                        Time.sleep(random(200, 300));
                    }
                }
            } else {
                DROPPING = false;
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (event.getMessage().startsWith("You catch a")) {
                Log.info("Caught a fish!");
                fish++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.FISHING);
        int gainedXp = Skills.getExperience(Skill.FISHING) - startXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        double ETA = feathers / (getPerHour(fish) / 60.0 / 60.0 / 1000.0);
        if (gainedXp == 0) {
            ttl = 0;
            ETA = 0;
        }
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

        String lvlsGained = (Skills.getCurrentLevel(Skill.FISHING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.FISHING) - startLvl) + ")" : "";

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
        g2.drawString(" (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", x + width, y);

        g2.drawString("Fishing lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Fishing lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.FISHING)) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Fishing lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.FISHING)) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Fish: ", x, y += 15);
        width = metrics.stringWidth("Fish: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(fish), x + width, y);
        width = metrics.stringWidth("Fish: " + formatter.format(fish));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(fish)) + "/hr)", x + width, y);

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
    public void onStop() {
        Log.info("Thanks for barbarian fishing! Caught " + fish + " fish this session.");
        //Game.logout();
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

    private void recordFishingPos(Position pos) {
        fishingHole = pos;
    }

    private Position getFishingHole() {
        return fishingHole;
    }

    private boolean isFishingSpotDed() {
        return Npcs.getNearest(npc -> npc.getPosition().equals(getFishingHole()) && npc.getId() == 1542) == null;
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