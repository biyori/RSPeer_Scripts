import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.StopWatch;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.ItemTables;
import org.rspeer.runetek.api.component.tab.*;
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
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;

@ScriptMeta(name = "Spinner", desc = "Spins flax right round baby", developer = "Koko", version = 1.0, category = ScriptCategory.CRAFTING)
public class Panties extends Script implements RenderListener {

    private int startMageXP, startCraftXP, startMagicLvl, startCraftLvl, startFlax = 0, startAstrals = 0, astrals = -1;
    private boolean takeScreenie = false, nearbyPLAY = false;
    private String status;
    private long startTime;
    private RandomHandler randomEvent = new RandomHandler();
    private StopWatch timer;

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startCraftXP = Skills.getExperience(Skill.CRAFTING);
        startCraftLvl = Skills.getLevelAt(startCraftXP);
        startMageXP = Skills.getExperience(Skill.MAGIC);
        startMagicLvl = Skills.getLevelAt(startMageXP);
        timer = StopWatch.start();
        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

    /*    Player[] nearbyPlayers = Players.getLoaded();
        if (nearbyPlayers.length > 1) {
            if (!nearbyPLAY) {
                java.awt.EventQueue.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane("NEARBY PLAYER!", JOptionPane.WARNING_MESSAGE);
                    JDialog dialog = optionPane.createDialog("Warning!");
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                });
                nearbyPLAY = true;
            }
            for (Player nearbyPlayer : nearbyPlayers) {
                if (nearbyPlayer != Local)
                    Log.info("Local player: " + nearbyPlayer.getName() + " (Lvl " + nearbyPlayer.getCombatLevel() + ") is nearby!");
            }
            Time.sleep(random(2000, 3000));
        } else {
            nearbyPLAY = false;
        }*/
        if (startAstrals == 0 && Inventory.contains("Astral rune"))
            startAstrals = Inventory.getCount(true, "Astral rune");


        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (Inventory.contains("Flax") && Inventory.contains("Astral rune")) {
            if (Dialog.canContinue()) {
                Time.sleep(random(1000, 1200));
                Log.info("Level up!");
                Dialog.processContinue();
                return random(1000, 1500);
            }

            status = "Casting spell";
            Log.info("Casting spell");
            timer.reset();
            int xpDrop = Skills.getExperience(Skill.MAGIC);
            if (Magic.cast(Spell.Lunar.SPIN_FLAX)) {
                Time.sleepUntil(() -> Skills.getExperience(Skill.MAGIC) - xpDrop == 75, 500, 5000);

                if (Inventory.contains("Astral rune")) {
                    astrals = Inventory.getCount(true, "Astral rune");
                } else {
                    //last cast fix?
                    astrals = 1;
                }
                status = "G-String!";
                if (Inventory.contains("Flax"))
                    Time.sleep(random(1869, 2100));
            }
        } else {
            //Bank
            Log.info("Banking for more thongs");
            status = "Finished strings!";
            SceneObject bankChest = SceneObjects.getNearest(obj -> obj.getName().equals("Chest") || obj.getName().equals("Bank booth") || obj.getName().equals("Bank chest") || obj.getName().equals("Grand Exchange booth"));
            if (bankChest != null) {
                if (!Bank.isOpen()) {
                    int sleep = random(500, 1000);
                    status = "Time to bank!";
                    Time.sleep(sleep);

                    Log.info("Opening bank");
                    status = "Opening bank";
                    bankChest.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 500, 10000);
                    return random(700, 1200);
                } else {
                    Time.sleep(random(500, 1000));
                    if (Inventory.contains("Bow string")) {
                        status = "Depositing string";
                        Log.info("Depositing string");
                        Time.sleep(random(0, 500));
                        Bank.depositAll("Bow string");
                        Time.sleepUntil(() -> !Inventory.contains("Bow string"), random(400, 600), 5000);
                    }
                    if (ItemTables.contains(ItemTables.BANK, x -> x.equals("Flax"))) {
                        Log.info("Getting flax");
                        status = "Getting more flax";
                        Time.sleep(random(400, 600));
                        if (startFlax == 0) {
                            startFlax = Bank.getCount("Flax");
                        }
                        Bank.withdrawAll("Flax");
                        Time.sleepUntil(() -> Inventory.contains("Flax"), 1000, 10000);
                    } else {
                        Log.info("We ran out of flax!");
                        setStopping(true);
                    }
                    if (!Inventory.contains("Astral rune")) {
                        if (Bank.contains("Astral rune")) {
                            Bank.withdrawAll("Astral rune");
                            Time.sleepUntil(() -> Inventory.contains("Astral rune"), random(900, 1000), 5000);
                        } else {
                            Log.info("Out of astral runes! Stopping script...");
                            setStopping(true);
                        }
                    }
                    if (!Inventory.contains("Nature rune")) {
                        if (Bank.contains("Nature rune")) {
                            Bank.withdrawAll("Nature rune");
                            Time.sleepUntil(() -> Inventory.contains("Nature rune"), random(900, 1000), 5000);
                        } else {
                            Log.info("Out of nature runes! Stopping script...");
                            setStopping(true);
                        }
                    }
                    if (Inventory.contains("Flax")) {
                        Bank.close();
                        Time.sleep(random(300, 500));
                    }
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextCraftLvl = Skills.getExperienceToNextLevel(Skill.CRAFTING);
        int nextMageLvl = Skills.getExperienceToNextLevel(Skill.MAGIC);
        int gainedCraftXP = Skills.getExperience(Skill.CRAFTING) - startCraftXP;
        int gainedMageXp = Skills.getExperience(Skill.MAGIC) - startMageXP;
        double ttl = (nextCraftLvl / (getPerHour(gainedCraftXP) / 60.0 / 60.0 / 1000.0));
        double ttl2 = (nextMageLvl / (getPerHour(gainedMageXp) / 60.0 / 60.0 / 1000.0));
        if (gainedCraftXP == 0) {
            ttl = 0;
            ttl2 = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 220, 235, 125);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 220, 235, 125);

        int x = 25;
        int y = 235;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        String craftLvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.CRAFTING)) - startCraftLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.CRAFTING)) - startCraftLvl) + ")" : "";
        String mageLvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) - startMagicLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) - startMagicLvl) + ")" : "";


        g2.setColor(Color.WHITE);
        g2.drawString("G-strings: ", x, y);
        int width = metrics.stringWidth("G-strings: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(astralsCasted() * 5), x + width, y);
        width = metrics.stringWidth("G-strings: " + formatter.format(astralsCasted() * 5));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(astralsCasted() * 5)) + "/hr)", x + width, y);

        g2.drawString("Craft lvl: ", x, y += 15);
        g2.setColor(Color.ORANGE);
        width = metrics.stringWidth("Craft lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.CRAFTING)) + craftLvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Craft lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.CRAFTING)) + craftLvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedCraftXP), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedCraftXP));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedCraftXP)) + "/hr)", x + width, y);

        g2.drawString("Magic lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Magic lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) + mageLvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Magic lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) + mageLvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl2).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedMageXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedMageXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedMageXp)) + "/hr)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime), x + width, y);
        width = metrics.stringWidth("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime));
        g2.setColor(Color.WHITE);
        g2.drawString(" (ETA: " + formatTime(Double.valueOf(getETA()).longValue()) + ")", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Spell timer: ", x, y += 15);
        width = metrics.stringWidth("Spell timer: ");
        g2.setColor(Color.CYAN);
        g2.drawString(timer.toElapsedString(), x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Status: ", x, y += 15);
        width = metrics.stringWidth("Status: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(status, x + width, y);


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
        Log.info("We pulled " + astralsCasted() * 5 + " g-strings! Gained " + (Skills.getExperience(Skill.MAGIC) - startMageXP) + " magic experience and " + (Skills.getExperience(Skill.CRAFTING) - startCraftXP) + " crafting experience this session.");
        Game.logout();
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
            if (!new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Hunter\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Hunter\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
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

    private double getETA() {
        //60 minutes in an hour, 60 seconds in 1 minute, 1000ms in 1 second
        double flax = startFlax - (astralsCasted() * 5);
        int bowStrings = astralsCasted() * 5;
        return bowStrings > 0 ? flax / (getPerHour(bowStrings) / 60.0 / 60.0 / 1000.0) : 0;
    }

    private int astralsCasted() {
        return astrals > -1 ? startAstrals - astrals : 0;
    }

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }
}