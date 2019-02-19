import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.movement.Movement;
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

import java.awt.*;
import java.security.SecureRandom;

@ScriptMeta(name = "Blow Master", desc = "Blows shit", developer = "Koko", version = 1.0, category = ScriptCategory.CRAFTING)
public class Blow extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay;
    private int blow = 0, needsBlowing = 0;

    private String status;
    private long startTime, lastAnim;

    @Override
    public void onStart() {

        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.CRAFTING);
        startLvl = Skills.getCurrentLevel(Skill.CRAFTING);
        lastAnimDelay = random(3000, 5000);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);

        if (Inventory.contains(item -> item.getName().equals("Molten glass"))) {
            status = "Blowing!";
            if (Local.isAnimating())
                lastAnim = System.currentTimeMillis();

            long msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                Log.info("Not animating! Starting new blowjob session.");

                if (Production.isOpen()) {
                    Log.info("Production ready!");
                    Production.initiate(5);//unPowered Orb
                    lastAnimDelay = random(3000, 5000);
                    Time.sleep(random(1000, 1500));
                } else {
                    if (!Inventory.isItemSelected()) {
                        Log.info("Pipe selected");
                        Inventory.getFirst("Glassblowing pipe").interact("Use");
                        Time.sleep(random(300, 700));
                    }
                    if (Inventory.isItemSelected()) {
                        Log.info("Using pipe on molten glass");
                        Inventory.getFirst(item -> item.getName().equals("Molten glass")).interact("Use");
                    }
                    Time.sleepUntil(Production::isOpen, 500, 10000);
                }
            }
        } else {
            //Bank
            Log.info("We finished blowing! Banking for more molten glass.");
            status = "Finished blowing!";
            SceneObject bankChest = SceneObjects.getNearest("Chest");
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
                    status = "Depositing orbs";
                    Log.info("Depositing orbs");
                    Bank.depositAllExcept("Glassblowing pipe");
                    Time.sleep(random(400, 600));
                    Log.info("Getting more molten glass to blow");
                    status = "Getting more molten glass to blow";
                    if (Bank.contains("Molten glass")) {
                        Bank.withdrawAll("Molten glass");
                        needsBlowing = Bank.getCount("Molten glass");
                        Time.sleepUntil(Inventory::isFull, 1000, 10000);
                    } else {
                        Log.info("We ran out of molten glass!");
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
            if (event.getMessage().startsWith("You make an")) {
                blow++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        double ETA = getETA();
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.CRAFTING);//500
        int gainedXp = Skills.getExperience(Skill.CRAFTING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        g.setColor(Color.YELLOW);

        String lvlsGained = (Skills.getCurrentLevel(Skill.CRAFTING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.CRAFTING) - startLvl) + ")" : "";

        g.drawString("Status: " + status, 25, 280);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime) + " (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", 25, 295);
        g.drawString("Crafting lvl: " + Skills.getCurrentLevel(Skill.CRAFTING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 25, 310);
        g.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", 25, 325);
        g.drawString("Orbs: " + blow + " (" + String.format("%.2f", getPerHour(blow)) + "/hr)", 25, 340);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for blowing! " + blow + "/" + needsBlowing + " unpowered orbs blew this session");
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
        return needsBlowing / (getPerHour(blow) / 60.0 / 60.0 / 1000.0);
    }
}