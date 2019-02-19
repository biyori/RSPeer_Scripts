import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.commons.math.Distance;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Position;
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

@ScriptMeta(name = "Lovakengj", desc = "Mines volcanic sulphur and survives the poison", developer = "Koko", version = 1.0, category = ScriptCategory.MINING)
public class Mine extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay, sulphur = 0, eatAt;
    private String status;
    private long startTime, lastAnim;
    private double startFavour;

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.MINING);
        startLvl = Skills.getCurrentLevel(Skill.MINING);
        startFavour = Double.parseDouble(Interfaces.getComponent(245, 9, 8).getText().replace("%", ""));
        lastAnimDelay = random(2000, 4000);
        status = "Loading up!";
        eatAt = random(30, 40);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);

        if (Health.getCurrent() > eatAt) {
            if (Inventory.isFull()) {
                status = "Banking";
                SceneObject bank = SceneObjects.getNearest("Bank chest");
                if (bank != null) {
                    bank.interact("Use");
                    Time.sleepUntil(Bank::isOpen, 1000, 10000);
                }
                if (Bank.isOpen()) {
                    Bank.depositInventory();
                    Time.sleepUntil(Inventory::isEmpty, 1000, 10000);
                    Bank.close();
                }
            } else {
                status = "Gaining favour";
                SceneObject volcanicSulphur = SceneObjects.getNearest("Volcanic sulphur");
                //SceneObject volcanicSulphur = SceneObjects.getNearest(28596);red rocks
                SceneObject[] sorted = Distance.sort(SceneObjects.getLoaded(SceneObject -> SceneObject.getName().equals("Volcanic sulphur")));


                if (Local.isAnimating())
                    lastAnim = System.currentTimeMillis();

                long msSinceAnim = System.currentTimeMillis() - lastAnim;

                if (Local.isHealthBarVisible() && msSinceAnim > lastAnimDelay) {
                    Log.info("Need a new rock!");

                    if (sorted.length > 0) {
                        int index = sorted.length - 1;
                        index = random(0, index);
                        sorted[index].interact(x -> true);
                        Time.sleepUntil(Local::isAnimating, 1000, 10000);
                    }
                }

                if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                    if (volcanicSulphur != null) {
                        Position rock = volcanicSulphur.getPosition();
                        volcanicSulphur.interact(x -> true);
                        Time.sleepUntil(() -> SceneObjects.getAt(rock)[0].getId() == 7468, 1000, 15000);
                        lastAnimDelay = random(2000, 4000);
                    }
                } else {
                    if (msSinceAnim > 0)
                        status = "Tabby! " + msSinceAnim + "ms";
                }
            }
        } else {
            eatAt = random(30, 40);
            status = "Eating! Next eat at " + eatAt;
            Log.info("Next eat at " + eatAt);
            if (Inventory.contains(item -> item.containsAction("Eat"))) {
                for (int i = 0; i < 5; i++) {
                    if (Inventory.contains(item -> item.containsAction("Eat"))) {
                        Inventory.getFirst(item -> item.containsAction("Eat")).interact(x -> true);
                        Time.sleepUntil(Local::isAnimating, 500, 10000);
                    } else {
                        break;
                    }
                }
            } else {
                status = "Getting food";
                SceneObject bank = SceneObjects.getNearest("Bank chest");
                if (bank != null) {
                    bank.interact("Use");
                    Time.sleepUntil(Bank::isOpen, 1000, 10000);
                }
                if (Bank.isOpen()) {
                    Bank.depositInventory();
                    Time.sleepUntil(Inventory::isEmpty, 1000, 10000);
                    Bank.withdraw("Tuna", random(3, 5));
                    Time.sleepUntil(() -> Inventory.contains("Tuna"), 1000, 10000);
                    Bank.close();
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (!event.getType().equals(ChatMessageType.PUBLIC)) {//!= chatType.Public
            if (event.getMessage().startsWith("You manage to mine some")) {
                sulphur++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.MINING);//500
        int gainedXp = Skills.getExperience(Skill.MINING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        Graphics g = renderEvent.getSource();
        g.setColor(Color.YELLOW);

        String lvlsGained = (Skills.getCurrentLevel(Skill.MINING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.MINING) - startLvl) + ")" : "";
        g.drawString("Status: " + status, 25, 265);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime), 25, 280);
        g.drawString("Mining lvl: " + Skills.getCurrentLevel(Skill.MINING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 25, 295);
        g.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", 25, 310);
        g.drawString("Mined: " + sulphur + " (" + String.format("%.2f", getPerHour(sulphur)) + "/hr)", 25, 325);
        g.drawString("Favor: " + String.format("%.2f", getFavor()) + "% " + String.format("%.2f", (getPerHour(getFavor()))) + "%/hr]", 25, 340);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for gaining favor! Mined " + sulphur + " sulphur this session.");
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

    private double getFavor() {
		//Make sure to switch to favor tab and have this open for this function to work
		
        if (Interfaces.getComponent(245, 9, 8) != null) {
            String flavour = Interfaces.getComponent(245, 9, 8).getText();
            flavour = flavour.replace("%", "");
            return Double.parseDouble(flavour) - startFavour;
        }
        return 0;
    }

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }
}