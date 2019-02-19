import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.component.Dialog;
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
import java.time.Instant;

@ScriptMeta(name = "Blackjack", desc = "Uses clubbing to get thieving XP", developer = "Koko", version = 1.0, category = ScriptCategory.THIEVING)
public class Steal extends Script implements RenderListener, ChatMessageListener {

    private int startThieveXP, startThieveLvl, knockout = 0, steal = 0, failKnockout = 0, eatPercent, stealCnt = 0;
    private boolean takeScreenie = false, timeToEat = false, canSteal = false, unconscious = false;
    private String status;
    private long startTime;
    private RandomHandler randomEvent = new RandomHandler();
    private Area room = Area.rectangular(3363, 3003, 3365, 3000);

    @Override
    public void onStart() {
        //Prefetch combat stats + exp
        //Get exp to handle overload potions increasing strength display
        startTime = System.currentTimeMillis();
        startThieveXP = Skills.getExperience(Skill.THIEVING);
        startThieveLvl = Skills.getLevelAt(startThieveXP);
        status = "Loading up!";
        eatPercent = random(40, 60);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();
        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (!timeToEat) {
            status = "Looking for trouble";
            if (Inventory.getFirst("Coin pouch") != null) {
                //Log.info("We have " + Inventory.getCount(true, "Coin pouch") + " pouches!");
                if (Inventory.getCount(true, "Coin pouch") == 28) {
                    Inventory.getFirst("Coin pouch").interact(x -> true);
                    Time.sleepUntil(() -> Inventory.getFirst("Coin pouch") == null, 1250, 10000);
                }
            }

            Npc bandit = Npcs.getNearest(id -> id.getId() == 735 && room.contains(id));
            if (bandit != null) {
                if (Local.getAnimation() == 403) {
                    status = "Stunned";
                    if (Inventory.getFirst("Coin pouch") != null) {
                        //Log.info("We have " + Inventory.getCount(true, "Coin pouch") + " pouches!");
                        if (Inventory.getCount(true, "Coin pouch") > random(0, 2)) {
                            Inventory.getFirst("Coin pouch").interact(x -> true);
                            Time.sleepUntil(() -> Inventory.getFirst("Coin pouch") == null, 1250, 10000);
                        }
                    }
                    Time.sleep(random(4000, 5000));
                }
                if (bandit.getAnimation() == 838 || (bandit.getOverheadText() != null && bandit.getOverheadText().contains("zzz"))) {
                    status = "LOOT";

                    if (room.contains(Local) && SceneObjects.getNearest("Curtain").containsAction("Close")) {
                        SceneObjects.getNearest("Curtain").interact("Close");
                        Time.sleepUntil(() -> SceneObjects.getNearest("Curtain").containsAction("Open"), 300, 5000);
                        Time.sleep(250, 500);
                    }

                    if (Health.getPercent() < eatPercent) {
                        status = "Time to eat!";
                        timeToEat = true;
                        return random(3000, 4000);
                    }
                    if (canSteal) {
                        bandit.interact("Pickpocket");
                    }
                }
                if (bandit.getAnimation() == -1 || bandit.getAnimation() == 395 || (bandit.getOverheadText() != null && !bandit.getOverheadText().contains("zzz"))) {
                    status = "Knocking dis bitch out";
                    if (unconscious) {
                        unconscious = false;
                        return random(1000, 2000);
                    }
                    bandit.interact("Knock-Out");
                    Time.sleep(200, 300);
                }
            }
        } else {
            if (Inventory.contains(item -> item.containsAction("Drink"))) {
                int eat = random(2, 5);
                eatPercent = random(40, 60);
                for (int i = 0; i < eat; i++) {
                    if (Inventory.contains(item -> item.containsAction("Drink"))) {
                        Inventory.getFirst(item -> item.containsAction("Drink")).interact("Drink");
                        Time.sleepUntil(Local::isAnimating, 1250, 10000);
                        Time.sleep(random(350, 475));
                    } else
                        break;
                }
                for (int i = 0; i < 10; i++) {
                    if (Inventory.contains("Jug")) {
                        Inventory.getFirst("Jug").interact("Drop");
                        Time.sleep(random(300, 500));
                    } else
                        break;
                }

                timeToEat = false;
            } else {
                status = "Getting more food!";
                SceneObject curtain = SceneObjects.getNearest("Curtain");
                if (curtain != null && curtain.containsAction("Open")) {
                    curtain.interact("Open");
                    Time.sleepUntil(() -> curtain.containsAction("Close"), 300, 5000);
                    Time.sleep(250, 500);
                }

                Npc bankNoter = Npcs.getNearest(1615);
                if (bankNoter != null) {
                    if (Inventory.contains(1994)) {
                        if (!Inventory.isItemSelected() && !Dialog.isViewingChatOptions()) {
                            Inventory.getFirst(1994).interact("Use");
                            Time.sleep(random(300, 700));
                        }
                        if (Inventory.isItemSelected() && !Dialog.isViewingChatOptions()) {
                            bankNoter.interact("Use");
                            Time.sleepUntil(Dialog::isViewingChatOptions, 1250, 5000);
                        }

                        if (Dialog.isViewingChatOptions()) {
                            if (Inventory.getFirst(1994).getStackSize() <= 5) //Exchange 5
                                Dialog.process(1);
                            else //exchange All
                                Dialog.process(2);
                            Time.sleepUntil(() -> Inventory.isFull() || Inventory.contains(333), 1250, 5000);
                            Time.sleep(random(200, 500));
                        }
                    } else {
                        Log.info("Ran out of food!");
                        setStopping(true);
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
        g2.fillRect(20, 235, 180, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 180, 95);

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }

        int x = 25;
        int y = 250;
        double thieveRate = ((double) steal / (double) (steal + failKnockout)) * 100;
        String lvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) + ")" : "";
        g2.drawString("Thieve lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x, y);
        g2.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", x, y += 15);
        g2.drawString("Gold Stolen: " + (steal * 50) + " @ (" + String.format("%.02f", thieveRate) + "%) [" + Math.ceil(getPerHour(steal * 50)) + "/hr]", x, y += 15);
        g2.drawString("Stunned: " + failKnockout + ". Knockouts: " + (knockout), x, y += 15);
        g2.drawString("Time Ran: " + formatTime(System.currentTimeMillis() - startTime), x, y += 15);
        g2.drawString("Status: " + status, x, y += 15);

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
            if ((m.contains("you smack the bandit") && (m.endsWith("unconscious.")))) {
                knockout++;
                canSteal = true;
            }
            if ((m.contains("you pick the") && (m.endsWith("pocket.")))) {
                steal++;
            }
            if ((m.contains("your blow only glances off") && (m.endsWith("head.")))) {
                failKnockout++;
            }
            if (m.contains("you fail to pick")) {
                failKnockout++;
            }

            if ((m.contains("they're") && (m.endsWith("unconscious.")))) {
                unconscious = true;
            }
        }
    }

    @Override
    public void onStop() {
        int lvls = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl);
        Log.info("Hope you had fun stealing! Gained " + (Skills.getExperience(Skill.THIEVING) - startThieveXP) + " thieving XP (+" + (lvls > 0 ? lvls : "0") + " levels).");
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
            if (!new File(getDataDirectory() + "\\Koko\\NMZ\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\NMZ\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\NMZ\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\NMZ\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
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