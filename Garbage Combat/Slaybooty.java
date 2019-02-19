import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Prayers;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
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

import java.awt.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;

@ScriptMeta(name = "Slayer", desc = "Shitty combat script for slayer", developer = "Koko", version = 1.0, category = ScriptCategory.SLAYER)
public class Slaybooty extends Script implements RenderListener, ChatMessageListener {

    private int startSlayXP, startRangeXP, startSlayLvl, startRangeLvl, lastAnimDelay;

    private String status;
    private long startTime, lastAnim;
    //  private Position fireGiantSpot = new Position(2568, 9893, 0);
    private RandomHandler randomEvent = new RandomHandler();
    // private Area giantz = Area.rectangular(2563, 9893, 2570, 9885);
    private int oldSplat = 0;
    private String[] loot = {"Tooth half of key", "Grimy ranarr weed", "Blood rune", "Chaos rune", "Fire rune", "Rune scimitar", "Brimstone key"};

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startSlayXP = Skills.getExperience(Skill.SLAYER);
        startSlayLvl = Skills.getCurrentLevel(Skill.SLAYER);
        startRangeXP = Skills.getExperience(Skill.RANGED);
        startRangeLvl = Skills.getCurrentLevel(Skill.RANGED);

        lastAnimDelay = random(2000, 3700);
        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();
        if (Local.isAnimating())
            lastAnim = System.currentTimeMillis();


        if (Health.getPercent() < 60)
            Inventory.getFirst("Tuna").interact("Eat");

        if (Prayers.getPoints() < 10)
            Inventory.getFirst(item -> item.getName().startsWith("Prayer")).interact("Drink");

        long msSinceAnim = System.currentTimeMillis() - lastAnim;

        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            randomEvent.handleRandom();
        } else if ((!Local.isAnimating() && msSinceAnim > lastAnimDelay)) {
            status = "Attacking " + msSinceAnim + "ms reset";
            //Prayers.toggleQuickPrayer(false);
            status = "Getting new target";
            lastAnimDelay = random(2000, 3700);
            Log.info("Attacking now!");
            Npc slay = Npcs.getNearest(task -> task.getName().equals("Hellhound") && (!task.isHealthBarVisible() || (task.getTarget() != null && task.getTarget().equals(Local))));
            if (Local.getTarget() == null) {
                slay.interact("Attack");
                Time.sleepUntil(() -> slay.isHealthBarVisible() && Local.isAnimating() || Local.isHealthBarVisible(), 10000);
                Time.sleep(300, 500);
            }
        } else if (Local.isHealthBarVisible()) {
            status = "Flick pray";
            if (Local.getHitsplatCycles().length > 0 && Local.getHitsplatCycles()[0] > oldSplat) {
                oldSplat = Local.getHitsplatCycles()[0];
                Prayers.toggleQuickPrayer(false);
            } else {
                Time.sleep(500);
                Npc slay = Npcs.getNearest(task -> task.getName().equals("Hellhound") && (task.getTarget() != null && task.getTarget().equals(Local)));
                if (slay != null)
                    Log.info("Index: " + slay.getIndex() + " Health " + slay.getHealthPercent() + "%");
                //TEST IF INDEX MATCHING FIXES DED
                if (slay != null && slay.getHealthPercent() == 0 || (slay != null && Npcs.getNearest(booty -> booty.getIndex() == slay.getIndex()) == null)) {
                    Log.info("DED");
                    if (Prayers.isQuickPrayerActive())
                        Prayers.toggleQuickPrayer(true);
                } else
                    Prayers.toggleQuickPrayer(true);
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (!event.getType().equals(ChatMessageType.PUBLIC) && !event.getType().equals(ChatMessageType.PRIVATE_RECEIVED) && !event.getType().equals(ChatMessageType.CLAN_CHANNEL)) {
            if (event.getMessage().endsWith("privacy mode enabled.") || event.getMessage().endsWith("do not seem to be at home.")) {
                Log.info("House is broken!");
                setStopping(true);
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextSlayLvlXp = Skills.getExperienceToNextLevel(Skill.SLAYER);//500
        int gainedSlayXp = Skills.getExperience(Skill.SLAYER) - startSlayXP;//75

        int nextRangeLvlXp = Skills.getExperienceToNextLevel(Skill.RANGED);//500
        int gainedRangeXp = Skills.getExperience(Skill.RANGED) - startRangeXP;//75

        double ttl1 = (nextSlayLvlXp / (getPerHour(gainedSlayXp) / 60.0 / 60.0 / 1000.0));
        double ttl2 = (nextRangeLvlXp / (getPerHour(gainedRangeXp) / 60.0 / 60.0 / 1000.0));
        if (gainedSlayXp == 0)
            ttl1 = 0;
        if (gainedRangeXp == 0)
            ttl2 = 0;

        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 235, 227, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 227, 95);

        int x = 25;
        int y = 250;
        String slaylvlsGained = (Skills.getCurrentLevel(Skill.RANGED) - startSlayLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.SLAYER) - startSlayLvl) + ")" : "";
        String rangelvlsGained = (Skills.getCurrentLevel(Skill.RANGED) - startRangeLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.RANGED) - startRangeLvl) + ")" : "";

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

        g2.drawString("Slayer lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Slayer lvl: ");
        g2.drawString(Skills.getCurrentLevel(Skill.SLAYER) + slaylvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Slayer lvl: " + Skills.getCurrentLevel(Skill.SLAYER) + slaylvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl1).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedSlayXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedSlayXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedSlayXp)) + "/hr)", x + width, y);

        g2.drawString("Range lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Range lvl: ");
        g2.drawString(Skills.getCurrentLevel(Skill.RANGED) + rangelvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Slayer lvl: " + Skills.getCurrentLevel(Skill.RANGED) + rangelvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl2).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedRangeXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedRangeXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedRangeXp)) + "/hr)", x + width, y);

        //Hide username
        if (Players.getLocal() != null) {
            Color tanColor = new Color(204, 187, 154);
            g2.setColor(tanColor);
            g2.fillRect(9, 459, 91, 15);
        }
    }

    @Override
    public void onStop() {
        Log.info("Thanks for slayin the booty!");
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
}