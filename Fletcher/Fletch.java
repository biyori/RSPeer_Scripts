import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.movement.Movement;
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

import java.awt.*;
import java.security.SecureRandom;

@ScriptMeta(name = "Fletch Master", desc = "Fletches shit", developer = "Koko", version = 1.0, category = ScriptCategory.FLETCHING)
public class Fletch extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay;
    private int fletched = 0, needsFletching = 0;

    private String status;
    private long startTime, lastAnim;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {

        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.FLETCHING);
        startLvl = Skills.getCurrentLevel(Skill.FLETCHING);
        lastAnimDelay = random(3000, 5000);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);


        randomEvent.checkLamp();
        if(randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            //we have a random
            if (Inventory.isFull()) {
                Inventory.getFirst(item -> item.getName().contains("Maple longbow")).interact("Drop");
                Time.sleep(random(500, 1000));
            }
            randomEvent.handleRandom();
        } else if (Inventory.contains(item -> item.getName().endsWith("logs"))) {
            //Fletch
            status = "Fletching!";
            if (Local.isAnimating())
                lastAnim = System.currentTimeMillis();

            long msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                Log.info("Not animating! Starting new fletch session.");

                if (Production.isOpen()) {
                    Log.info("Production ready!");
                    if (Skills.getCurrentLevel(Skill.FLETCHING) < 55)
                        Production.initiate(1);// 1 shortbow
                    else
                        Production.initiate(2);//2 longbow
                    lastAnimDelay = random(3000, 5000);
                    Time.sleep(random(1000, 1500));
                } else {
                    if (!Inventory.isItemSelected()) {
                        Log.info("Knife selected");
                        Inventory.getFirst("knife").interact("Use");
                        Time.sleep(random(300, 700));
                    }
                    if (Inventory.isItemSelected()) {
                        Log.info("Using knife on logs");
                        Inventory.getFirst(item -> item.getName().endsWith("logs")).interact("Use");
                    }
                    Time.sleepUntil(Production::isOpen, 500, 10000);
                }
            }
        } else {
            //Bank
            Log.info("We finished fletching! Banking for more logs.");
            status = "Finished fletching!";
            SceneObject bankChest = SceneObjects.getNearest(obj -> obj.getName().equals("Chest") || obj.getName().equals("Bank booth") || obj.getName().equals("Bank chest"));
            if (bankChest != null) {
                if (!Bank.isOpen()) {
                    int sleep = random(3000, 20000);
                    status = "Time to bank! Sleeping for " + (sleep / 1000) + "s.";
                    Time.sleep(sleep);

                    Log.info("Opening bank");
                    status = "Opening bank";
                    bankChest.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 1000, 10000);
                    Time.sleep(random(500, 1200));
                } else {
                    status = "Depositing bows";
                    Log.info("Depositing bows");
                    Bank.depositAllExcept("Knife");
                    Time.sleep(random(400, 600));
                    Log.info("Getting more logs to cut");
                    status = "Getting more logs";
                    if (Bank.contains("Maple logs")) {
                        Bank.withdrawAll("Maple logs");
                        needsFletching = Bank.getCount("Maple logs");
                        Time.sleepUntil(Inventory::isFull, 1000, 10000);
                    } else {
                        Log.info("We ran out of logs!");
                        setStopping(true);
                    }
                    Bank.close();
                    Time.sleep(random(300, 500));
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (event.getMessage().startsWith("You carefully cut")) {
                fletched++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        double ETA = getETA();
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.FLETCHING);//500
        int gainedXp = Skills.getExperience(Skill.FLETCHING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        g.setColor(Color.YELLOW);

        String lvlsGained = (Skills.getCurrentLevel(Skill.FLETCHING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.FLETCHING) - startLvl) + ")" : "";

        g.drawString("Status: " + status, 25, 280);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime) + " (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", 25, 295);
        g.drawString("Fletch lvl: " + Skills.getCurrentLevel(Skill.FLETCHING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 25, 310);
        g.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", 25, 325);
        g.drawString("Fletched: " + fletched + " (" + String.format("%.2f", getPerHour(fletched)) + "/hr)", 25, 340);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for fletching! Fletched " + fletched + "/" + needsFletching + " longbows this session");
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

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }

    private double getETA() {
        //60 minutes in an hour, 60 seconds in 1 minute, 1000ms in 1 second
        return needsFletching / (getPerHour(fletched) / 60.0 / 60.0 / 1000.0);
    }
}