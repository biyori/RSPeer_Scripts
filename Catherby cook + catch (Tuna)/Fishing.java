import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
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

import java.awt.*;
import java.security.SecureRandom;

@ScriptMeta(name = "Barbarian Fishing", desc = "Fishes in Catherby via barbarian fishing", developer = "Koko", version = 2.0, category = ScriptCategory.FISHING)
public class Fishing extends Script implements RenderListener, ChatMessageListener {

    private int startXP, fish = 0;
    private double sum = 0;
    private String status;
    private long startTime, nextSpaceTime = System.currentTimeMillis() / 1000;
    private Area stove = Area.rectangular(2816, 3443, 2818, 3441);
    private Area fishingArea = Area.rectangular(2836, 3437, 2860, 3427);
    private boolean WORKING;
    private Position fishingHole;

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.FISHING);
        WORKING = false;
        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);


        if (!Inventory.isFull()) {
            //Fish
            Npc fishingSpot = Npcs.getNearest(1519); //Cage + Harpoon
            if (fishingSpot != null) {
                if (!WORKING) {
                    if (local.isAnimating()) {
                        status = "Fishing!";
                        WORKING = true;
                    } else {
                        status = "Attempting to fish";
                        fishingSpot.interact("Harpoon");
                        recordFishingPos(fishingSpot.getPosition());//save location
                        Time.sleepUntil(() -> local.isAnimating() || isFishingSpotDed(), 1200, 10000);
                    }
                    Time.sleep(random(751, 3500));
                } else {
                    if (isFishingSpotDed()) {
                        int sleepN = random(9000, 55000);
                        status = "Fishing spot ded. Sleeping for " + (sleepN / 1000) + " seconds";
                        Log.info("Fishing spot is ded! Sleeping for " + (sleepN / 1000) + " seconds");
                        Time.sleep(sleepN);
                        WORKING = false;
                    } else {
                        int val = random(250, 1500);
                        sum += val;
                        status = "AFK fishing [" + (nextSpaceTime - System.currentTimeMillis() / 1000) + "s]";
                        Time.sleep(val);

                        if (nextSpaceTime < System.currentTimeMillis() / 1000) {
                            Log.info("Sending enter!");
                            Keyboard.pressEnter();
                            Time.sleep(random(500, 1000));
                            nextSpaceTime = System.currentTimeMillis() / 1000 + random(15, 44);
                            Log.info("Next enter will be in " + (nextSpaceTime - System.currentTimeMillis() / 1000) + "s");
                        }
                        if (Inventory.isFull())
                            WORKING = false;
                        if (Dialog.isViewingChat()) {
                            Log.info("Level up or inventory is full!");
                            WORKING = false;
                            Time.sleep(random(1000, 5000));
                        }
                    }
                }
            } else {
                status = "Walking to fishing area";
                Position toFish = fishingArea.getCenter().randomize(2);
                //Movement.walkToRandomized(toFish);
                Movement.walkTo(toFish);//Something here is fucky
                Time.sleep(random(3000, 4000));
            }
        } else {
            Position toStove = stove.getCenter().randomize(2);
            Position rangePos = new Position(2817, 3444, 0);
            SceneObject Range = SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Range") && SceneObject.getPosition().equals(rangePos));
            if (Range != null && Inventory.contains("Raw tuna") && !WORKING) {
                //cook
                status = "Cooking fish!";
                if (!Production.isOpen()) {
                    status = "Preparing food";
                    Range.interact("Cook");
                    Time.sleepUntil(Production::isOpen, random(500, 750), 10000);
                } else {
                    status = "Cooking!";
                    Production.initiate();
                    Time.sleep(random(500, 1000));
                }
                if (local.isAnimating())
                    WORKING = true;
            } else if (Range == null) {
                status = "Walking closer to range";
                Movement.walkToRandomized(toStove);
                Time.sleep(random(300, 3500));
            } else if (!Inventory.contains("Raw tuna")) {
                WORKING = false;
                SceneObject BankB = SceneObjects.getNearest("Bank booth");
                Log.info("Opening bank");
                if (BankB != null && !Bank.isOpen()) {
                    int sleep = random(3000, 10000);
                    status = "Banking after sleeping for " + (sleep / 1000) + " seconds";
                    Time.sleep(sleep);

                    status = "Opening bank";
                    Time.sleep(random(300, 500));
                    BankB.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 1200, 10000);
                } else if (Bank.isOpen()) {
                    status = "Depositing fish";
                    Time.sleep(random(500, 1000));
                    Bank.depositInventory();
                    Time.sleepUntil(Inventory::isEmpty, random(500, 750), 10000);
                    //  Time.sleep(random(150, 200));
                }
            } else if (Dialog.isViewingChat() || Production.isOpen()) {
                Time.sleep(random(4000, 6000));
                WORKING = false;
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (event.getMessage().endsWith("catch a tuna.")) {
                Log.info("Caught a fish!");
                fish++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.FISHING);
        int gainedXp = Skills.getExperience(Skill.FISHING) - startXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));//TODO: FIX TTL convert hours to ms
        if (gainedXp == 0)
            ttl = 0;
        Graphics g = renderEvent.getSource();
        g.setColor(Color.WHITE);
        g.drawString("Status: " + status, 30, 285);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime), 30, 300);
        g.drawString("Fishin lvl: " + Skills.getCurrentLevel(Skill.FISHING) + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 30, 315);
        g.drawString("Fish caught: " + fish + " (" + String.format("%.2f", getPerHour(fish)) + "/hr)", 30, 330);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for barbarian fishing! Caught " + fish + " fish this session.");
        // Game.logout();
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private void recordFishingPos(Position pos) {
        fishingHole = pos;
    }

    private Position getFishingHole() {
        return fishingHole;
    }

    private boolean isFishingSpotDed() {
        return Npcs.getNearest(npc -> npc.getPosition().equals(getFishingHole()) && npc.getId() == 1519) == null;
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