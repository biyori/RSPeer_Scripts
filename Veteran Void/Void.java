import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
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
import java.io.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;

@ScriptMeta(name = "Void Grinder", desc = "Grind out void n shit", developer = "Koko", version = 1.0, category = ScriptCategory.MINIGAME)
public class Void extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay;
    private int voidPts = 0;
    private int runPercent;
    private Area insideBoat = Area.rectangular(2632, 2654, 2635, 2649);
    private String status;
    private long startTime, lastAnim = 0;
    private Skill training = Skill.RANGED;
    private boolean takeScreenie = false, deathWalk = false;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(training);
        startLvl = Skills.getLevelAt(startXP);
        status = "Loading up!";
        runPercent = random(20, 40);
        lastAnimDelay = random(1500, 2000);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();
        if (playerIsAttacking())
            lastAnim = System.currentTimeMillis();

        long msSinceAnim = System.currentTimeMillis() - lastAnim;

        //Run
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
        } else if (SceneObjects.getNearest("Gangplank") != null) {
            if (finishedGame())
                voidPts += 5;

            if (insideBoat.contains(Local)) {
                int timer = random(3000, 6523);
                status = "Waiting to get into a game... (" + timer + "ms)";
                Time.sleepUntil(() -> !insideBoat.contains(Local), 500, timer);
                Movement.walkToRandomized(insideBoat.getCenter());
            } else {
                SceneObject plank = SceneObjects.getNearest(25632);//Veteran gangplank
                if (plank != null) {
                    status = "Entering boat";
                    plank.interact("Cross");
                    Time.sleepUntil(() -> insideBoat.contains(Local), 500, 2000);
                    deathWalk = false;
                }
            }
        } else {
            Npc portal = Npcs.getNearest(port -> port.getName().equals("Portal") && port.containsAction("Attack"));
            Npc examinePortal = Npcs.getNearest(port -> port.getName().equals("Portal") && !port.containsAction("Attack"));

            //When health gets low lets turn on a prayer
            if (Health.getCurrent() <= 17 && !Prayers.isActive(Prayer.RETRIBUTION))
                Prayers.toggle(true, Prayer.RETRIBUTION);

            //Move after entering game
            if (/*Dialog.isViewingChat() && */Npcs.getNearest(squire -> squire.getName().equals("Squire") && squire.containsAction("Leave")) != null && Npcs.getNearest("Squire").getPosition().distance(Local) < 15) {
                status = "Walking closer to position!";
                Position currentPos = Local.getPosition();
                Position newPos = new Position(currentPos.getX() + random(-2, 2), currentPos.getY() - random(29, 35));
                for (int i = 0; i < 10; i++) {
                    Movement.walkTo(newPos);
                    Time.sleep(random(1000, 1100));
                    if (Local.getPosition().distance(newPos) < 2)
                        break;
                }
            } else if (Local.getAnimation() == 836 || deathWalk) {
                status = "We died!";
                Log.info("We ded");
                Time.sleepUntil(() -> Npcs.getNearest("Squire") != null, random(700, 1200), 2000);

                if (Npcs.getNearest("Squire") != null && Npcs.getNearest("Squire").getPosition().distance(Local) < 15) {
                    status = "We died! Walking closer to position!";
                    Position currentPos = Players.getLocal().getPosition();
                    Position newPos = new Position(currentPos.getX() + random(-2, 2), currentPos.getY() - random(29, 35));
                    for (int i = 0; i < 10; i++) {
                        Movement.walkTo(newPos);
                        Time.sleep(random(1000, 1100));
                        if (Players.getLocal().getPosition().distance(newPos) < 2)
                            break;
                    }
                    deathWalk = false;
                }
            } else if (portal != null) {
                status = "Attacking portal";
                if (msSinceAnim > lastAnimDelay) {
                    Log.info("Attacking portal!");
                    portal.interact("Attack");
                    Time.sleepUntil(this::playerIsAttacking, 500, 1000);
                    lastAnimDelay = random(1500, 2000);
                    if (!playerIsAttacking() && Npcs.getNearest("Brawler") != null) {
                        status = "Brawler is in the way!";
                        Npcs.getNearest("Brawler").interact("Attack");
                    }
                }
            } else if (examinePortal != null && examinePortal.getPosition().distance(Local) > 15) {
                status = "Walking closer to portal";
                Log.info("Walking closer to portal");
                Movement.walkTo(examinePortal.getPosition().randomize(4));
                Time.sleep(random(3000, 5000));
            } else if (Npcs.getNearest(mobs -> mobs.containsAction("Attack")) != null && (msSinceAnim > lastAnimDelay)) {
                status = "Attacking adds";

                Npc adds = Npcs.getNearest(mobs -> mobs.containsAction("Attack"));
                Npc underAttack = Npcs.getNearest(atk -> atk.containsAction("Attack") && (atk.getTarget() != null && atk.getTarget().equals(Local)));

                if (Combat.getSpecialEnergy() == 100 || Combat.getSpecialEnergy() == 50) {
                    Combat.toggleSpecial(true);
                    Time.sleep(random(200, 300));
                }

                if (underAttack != null) {
                    Log.info("Attacking " + underAttack.getName());
                    status = "Attacking " + underAttack.getName();
                    underAttack.interact("Attack");
                    Time.sleepUntil(this::playerIsAttacking, 500, 1000);
                } else {
                    Log.info("Attacking " + adds.getName());
                    status = "Attacking " + adds.getName();
                    adds.interact("Attack");
                    Time.sleepUntil(this::playerIsAttacking, 500, 1000);
                }
                if (playerIsAttacking())
                    lastAnimDelay = random(1500, 2000);
            } else {
                status = "Idle";
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

        String VoidPts = voidPts == 0 ? "None" : voidPts + " (" + String.format("%.2f", getPerHour(voidPts)) + "/hr)";
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
        int laps = 0;
        g2.drawString(" (Laps: " + laps + " [" + String.format("%.2f", getPerHour(laps)) + "/hr])", x + width, y);

        g2.drawString("Range lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Range lvl: ");
        g2.drawString(Skills.getCurrentLevel(training) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Range lvl: " + Skills.getCurrentLevel(training) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Void Points: ", x, y += 15);
        width = metrics.stringWidth("Void Points: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(VoidPts, x + width, y);
        g2.setColor(Color.WHITE);

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }
    }

    @Override
    public void notify(ChatMessageEvent messages) {
        if (messages.getType().equals(ChatMessageType.SERVER)) {
            if (messages.getMessage().endsWith("you are dead!")) {
                Log.info("We died!");
                deathWalk = true;
            }
        }
    }

    @Override
    public void onStop() {
        Log.info("Hope you had fun grinding void! Collected " + voidPts + " points and " + (Skills.getExperience(training) - startXP) + " range xp this session");
    }

    private boolean finishedGame() {
        return Players.getLocal().getPosition().equals(new Position(2638, 2653, 0)) && Dialog.isViewingChat();
    }

    private boolean playerIsAttacking() {
        return Players.getLocal().getAnimation() == 426 || Players.getLocal().getAnimation() == 1074;
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