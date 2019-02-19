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

@ScriptMeta(name = "Lumbridge Cook", desc = "Cooks at the Lumbridge range", developer = "Koko", version = 1.0, category = ScriptCategory.COOKING)
public class Cook extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay;
    private int cooked = 0, burnt = 0, needsCooking = 0;

    private String status;
    private long startTime, lastAnim;

    @Override
    public void onStart() {

        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.COOKING);
        startLvl = Skills.getCurrentLevel(Skill.COOKING);
        lastAnimDelay = random(3000, 5000);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);

        if (Inventory.contains(item -> item.getName().startsWith("Raw"))) {
            //COOK
            SceneObject range = SceneObjects.getNearest("Cooking range");
            if (range != null) {
                status = "Cooking!";
                //Cook
                if (Local.isAnimating())
                    lastAnim = System.currentTimeMillis();

                long msSinceAnim = System.currentTimeMillis() - lastAnim;

                if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                    Log.info("Not animating! Starting new cook session.");
                    range.interact("Cook");
                    lastAnimDelay = random(3000, 5000);
                    Time.sleepUntil(Production::isOpen, 500, 10000);
                    if (Production.isOpen()) {
                        Production.initiate();
                        Time.sleep(random(1000, 1500));
                    }
                }
            } else {
                Log.info("We have raw food but we can't find the range! Climbing ladder...");
                status = "Getting to range";
                SceneObject Bank = SceneObjects.getNearest("Chest");
                SceneObject Ladder = SceneObjects.getNearest("Ladder");
                if (Bank != null && Ladder != null) {
                    Ladder.interact("Climb-up");
                    Time.sleepUntil(() -> SceneObjects.getNearest("Cooking range") != null, 1000, 10000);
                    Time.sleep(random(500, 1200));
                }
            }
            //Last anim
        } else {
            //Bank
            Log.info("We finished cooking! Banking for more raw stuff.");
            status = "Finished cooking!";
            SceneObject bankChest = SceneObjects.getNearest("Chest");
            if (bankChest != null) {
                if (!Bank.isOpen()) {
                    Log.info("Opening bank");
                    status = "Opening bank";
                    bankChest.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 1000, 10000);
                    Time.sleep(random(500, 1200));
                } else {
                    status = "Depositing cooked food";
                    Log.info("Depositing cooked food");
                    Bank.depositInventory();
                    Time.sleep(random(400, 600));
                    Log.info("Getting more shit to cook");
                    status = "Getting more stuff to cook";
                    if(Bank.contains(item -> item.getName().startsWith("Raw"))) {
                        Bank.withdrawAll(item -> item.getName().startsWith("Raw"));
                        needsCooking = Bank.getCount(item -> item.getName().startsWith("Raw"));
                        Time.sleepUntil(Inventory::isFull, 1000, 10000);
                    } else {
                        Log.info("We ran out of things to cook! Stopping script...");
                        setStopping(true);
                    }
                }
            } else {
                int sleep = random(3000, 12000);
                status = "Time to bank! Sleeping for " + (sleep / 1000) + "s.";
                Time.sleep(sleep);
                Log.info("Climbing down trapdoor");
                SceneObject trapDoor = SceneObjects.getNearest("Trapdoor");
                if (trapDoor != null) {
                    trapDoor.interact("Climb-down");
                    Time.sleepUntil(() -> SceneObjects.getNearest("Chest") != null, 1000, 10000);
                    Time.sleep(random(500, 750));
                }
            }
        }


        SceneObject Portal = SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Portal") && SceneObject.containsAction("Lock") && SceneObject.containsAction("Enter"));
        if (Portal != null) {
            Portal.interact("Enter");
            Time.sleepUntil(() -> Npcs.getNearest("Phials") != null, 1000, 10000);
        }

        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (event.getMessage().startsWith("You accidentally")) {
                burnt++;
            } else if (event.getMessage().startsWith("You successfully") || event.getMessage().startsWith("You manage") || event.getMessage().startsWith("You roast")) {
                cooked++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        double ETA = getETA();
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.COOKING);//500
        int gainedXp = Skills.getExperience(Skill.COOKING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        g.setColor(Color.YELLOW);

        String lvlsGained = (Skills.getCurrentLevel(Skill.COOKING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.COOKING) - startLvl) + ")" : "";

        g.drawString("Status: " + status, 25, 265);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime) + " (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", 25, 280);
        g.drawString("Cooking lvl: " + Skills.getCurrentLevel(Skill.COOKING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 25, 295);
        g.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", 25, 310);
        g.drawString("Fish cooked: " + cooked + " (" + String.format("%.2f", getPerHour(cooked)) + "/hr)", 25, 325);
        g.drawString("Fish burn: " + burnt + " (" + String.format("%.2f", getPerHour(burnt)) + "/hr)", 25, 340);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for cooking! Yield: " + cooked + "/" + (cooked + burnt) + " cooked this session");
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
        return needsCooking / (getPerHour(cooked + burnt) / 60.0 / 60.0 / 1000.0);
    }
}