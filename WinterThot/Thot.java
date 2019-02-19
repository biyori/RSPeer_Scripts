import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;

@ScriptMeta(name = "Winter Thot", desc = "Does the Winterthot minigame", developer = "Koko", version = 1.0, category = ScriptCategory.MINIGAME)
public class Thot extends Script implements RenderListener, ChatMessageListener {

    private int startFireXP, startFireLvl, crates = 0, fail = 0, eatPercent, lastAnimDelay;
    private boolean takeScreenie = false, fletching = false, burning = false, chopping = true;
    private String status;
    private long startTime, lastAnim, msSinceAnim;
    private RandomHandler randomEvent = new RandomHandler();
    private Area toMinigame = Area.rectangular(1628, 3963, 1632, 3957);
    private Area toExitGame = Area.rectangular(1628, 3979, 1632, 3968);
    private Area bankChest = Area.rectangular(1638, 3945, 1640, 3944);
    private Area treeArea = Area.rectangular(1638, 3989, 1638, 3988);

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startFireXP = Skills.getExperience(Skill.FIREMAKING);
        startFireLvl = Skills.getLevelAt(startFireXP);
        status = "Loading up!";
        eatPercent = random(40, 60);
        lastAnim = random(3000, 4000);
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();
        randomEvent.checkLamp();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (insideThot() && (!gameEnded() || readyForGame())) {
            if (Local.isAnimating())
                lastAnim = System.currentTimeMillis();

            msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (Health.getPercent() < eatPercent) {
                status = "Eating to " + eatPercent + "%";
                int eat = random(2, 5);
                eatPercent = random(40, 60);
                for (int i = 0; i < eat; i++) {
                    if (Inventory.contains(item -> item.containsAction("Drink"))) {
                        Inventory.getFirst(item -> item.containsAction("Drink")).interact("Drink");
                        Time.sleepUntil(Local::isAnimating, 1250, 4000);
                        Time.sleep(random(500, 600));
                        if (Health.getPercent() > 90)
                            break;
                    } else
                        break;
                }

                for (int i = 0; i < 27; i++) {
                    if (Inventory.contains("Jug")) {
                        if (Inventory.getItemAt(i) != null && Inventory.getItemAt(i).getName().equals("Jug")) {
                            Inventory.getItemAt(i).interact("Drop");
                            Time.sleep(random(200, 300));
                        }
                    } else {
                        break;
                    }
                }
                msSinceAnim = 10000;//force animate
            }
            if (chopping && !Inventory.isFull()) {
                if (readyForGame() && Interfaces.getComponent(396, 21) != null && Interfaces.getComponent(396, 21).getText().equals("Wintertodt's Energy: 0%")) {
                    status = "Waiting for game to start";
                    if (!treeArea.contains(Local)) {
                        status = "Moving closer to tree area";
                        Log.info("Moving closer to tree area");
                        Movement.walkTo(treeArea.getTiles().get(random(0, 1)));
                        Time.sleepUntil(() -> Local.distance(treeArea.getCenter()) < 7, random(300, 500), 4000);
                    }
                    return random(500, 1000);
                }

                if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                    status = "Restarting chop session!";
                    lastAnimDelay = random(3000, 4000);
                    SceneObject root = SceneObjects.getNearest(t -> t.getName().equals("Bruma roots") && t.getPosition().equals(new Position(1639, 3988, 0)));
                    if (root != null) {
                        Log.info("Chopping root!");
                        if (root.interact("Chop"))
                            Time.sleepUntil(Local::isAnimating, 500, 5000);
                    }
                } else {
                    status = "Chopping!";
                }
                if (!treeArea.contains(Local)) {
                    status = "Moving to root area";
                    Log.info("Moving to root area");
                    Movement.walkTo(treeArea.getTiles().get(random(0, 1)));
                    Time.sleepUntil(() -> treeArea.contains(Local), random(500, 1000), 4000);
                    msSinceAnim = 10000;//force cut
                }
            } else if (chopping) {
                chopping = false;
                msSinceAnim = 10000;
                if (getPoints() == 0) {
                    fletching = true;
                } else {
                    burning = true;
                }
            } else if (fletching && Inventory.contains(20695)) {//roots
                int fletched = Inventory.getCount(20696);
                if (Local.isHealthBarVisible() || (!Local.isAnimating() && msSinceAnim > lastAnimDelay)) {
                    lastAnimDelay = random(3000, 4000);
                    status = "Restarting fletch session!";
                    if (!Inventory.isItemSelected()) {
                        Log.info("Knife selected");
                        Inventory.getFirst("knife").interact("Use");
                        Time.sleep(random(300, 700));
                    }
                    if (Inventory.isItemSelected()) {
                        Log.info("Using knife on logs");

                        if (Inventory.getFirst(20695).interact("Use"))
                            Time.sleepUntil(() -> Inventory.getCount(20696) > fletched, 500, 5000);
                    }
                } else {
                    status = "Fletching";
                }
            } else if (fletching && !Inventory.contains(20695)) {
                burning = true;
                fletching = false;
            } else if (burning && Inventory.contains(20696, 20695)) {//logs and fletched
                if (Local.isHealthBarVisible() || (!Local.isAnimating() && msSinceAnim > lastAnimDelay)) {
                    lastAnimDelay = random(3000, 4000);
                    int invSize = Inventory.getFreeSlots();
                    status = "Restarting burn session";
                    SceneObject burningBrazier = SceneObjects.getNearest(b -> b.getName().equals("Burning brazier") && b.getPosition().equals(new Position(1638, 3997, 0)));
                    SceneObject brazier = SceneObjects.getNearest(c -> c.getName().equals("Brazier") && c.getPosition().equals(new Position(1638, 3997, 0)));
                    if (burningBrazier != null) {
                        Log.info("Feeding brazier");
                        burningBrazier.interact("Feed");
                        Time.sleepUntil(() -> Inventory.getFreeSlots() > invSize, 500, 5000);
                    }
                    if (brazier != null) {
                        status = "Lighting brazier!";
                        if (brazier.interact("Light"))
                            Time.sleepUntil(() -> burningBrazier != null, 500, 2000);
                    }
                } else {
                    status = "Burning";
                    SceneObject brazier = SceneObjects.getNearest(c -> c.getName().equals("Brazier") && c.getPosition().equals(new Position(1638, 3997, 0)));
                    if (brazier != null) {
                        msSinceAnim = 10000;
                        status = "Lighting brazier!";
                        if (brazier.interact("Light"))
                            Time.sleepUntil(() -> SceneObjects.getNearest(b -> b.getName().equals("Burning brazier") && b.getPosition().equals(new Position(1638, 3997, 0))) != null, 500, 2000);
                    }
                }
            } else {
                chopping = true;
                burning = false;
            }
        } else if ((gameEnded() && insideThot()) || (!insideThot() && !readyForGame())) {
            status = "Banking!";
            if (insideThot()) {
                status = "Pulling out of thot";
                if (Inventory.contains("Jug of wine")) {
                    for (int i = 0; i < 10; i++) {
                        if (Inventory.contains(item -> item.containsAction("Drink"))) {
                            Inventory.getFirst(item -> item.containsAction("Drink")).interact("Drink");
                            Time.sleepUntil(Local::isAnimating, 1250, 4000);
                            Time.sleep(random(500, 600));
                            if (Health.getPercent() > 90)
                                break;
                        } else
                            break;
                    }
                }

                if (SceneObjects.getNearest("Doors of Dinh") != null) {
                    SceneObjects.getNearest("Doors of Dinh").interact("Enter");
                    Time.sleepUntil(() -> !insideThot(), 500, 9000);
                } else {
                    Movement.walkTo(toExitGame.getCenter().randomize(3));
                    Time.sleep(random(500, 2000));
                }
            } else {
                status = "Banking";
                Time.sleep(random(2000, 4000));
                SceneObject chest = SceneObjects.getNearest("Bank chest");
                if (chest != null) {
                    if (!Bank.isOpen()) {
                        chest.interact("Bank");
                        Time.sleepUntil(() -> Bank.isOpen() || Interfaces.isOpen(213), 500, 5000);
                    }
                    if (Interfaces.isOpen(213)) {
                        status = "Waiting for Bank PIN entry";
                    }
                    if (Bank.isOpen()) {
                        if (Bank.depositInventory())
                            Time.sleepUntil(Inventory::isEmpty, 500, 5000);

                        if (Bank.contains("Tinderbox")) {
                            Bank.withdraw("Tinderbox", 1);
                            Time.sleepUntil(() -> Inventory.contains("Tinderbox"), 500, 5000);
                            Time.sleep(random(200, 500));
                        }
                        if (Bank.contains("Knife")) {
                            Bank.withdraw("Knife", 1);
                            Time.sleepUntil(() -> Inventory.contains("Knife"), 500, 5000);
                            Time.sleep(random(500, 750));
                        }
                        if (Bank.contains("Jug of wine")) {
                            Bank.withdraw("Jug of wine", 10);
                            Time.sleepUntil(() -> Inventory.contains("Jug of wine"), 500, 4000);
                        }
                        if (Inventory.contains("Jug of wine"))
                            Bank.close();
                    }
                } else {
                    status = "Walking closer to chest!";
                    Movement.walkTo(bankChest.getCenter().randomize(3));
                    Time.sleepUntil(() -> SceneObjects.getNearest("Bank chest") != null, random(300, 500), random(2000, 4000));
                }
            }
        } else if (readyForGame()) {
            status = "Ready for next game!";
            if (SceneObjects.getNearest("Doors of Dinh") != null) {
                SceneObjects.getNearest("Doors of Dinh").interact("Enter");
                Time.sleepUntil(this::insideThot, random(1000, 2000), 8000);
                burning = false;
                fletching = false;
                chopping = true;
            } else {
                Movement.walkTo(toMinigame.getCenter().randomize(3));
                Time.sleepUntil(() -> SceneObjects.getNearest("Doors of Dinh") != null, random(300, 500), random(2000, 4000));
            }
        } else {
            status = "WHAT THE FUCK NEXT?";
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.FIREMAKING);
        int gainedXp = Skills.getExperience(Skill.FIREMAKING) - startFireXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0) {
            ttl = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 240, 209, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 240, 209, 95);

        int x = 25;
        int y = 255;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        String lvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.FIREMAKING)) - startFireLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.FIREMAKING)) - startFireLvl) + ")" : "";

        g2.setColor(Color.WHITE);
        g2.drawString("Firemaking Lvl: ", x, y);
        g2.setColor(new Color(238, 133, 74));
        int width = metrics.stringWidth("Firemaking Lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.FIREMAKING)) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Firemaking Lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.FIREMAKING)) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);


        g2.drawString("Crates: ", x, y += 15);
        width = metrics.stringWidth("Crates: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(crates), x + width, y);
        width = metrics.stringWidth("Crates: " + formatter.format(crates));
        g2.drawString(" (" + formatter.format(getPerHour(crates)) + "/hr)", x + width, y);
        g2.setColor(Color.WHITE);

        g2.setColor(Color.WHITE);
        g2.drawString("Failed games: ", x, y += 15);
        width = metrics.stringWidth("Failed games: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(fail) + "/" + formatter.format(fail), x + width, y);
        width = metrics.stringWidth("Failed games: " + formatter.format(fail) + "/" + formatter.format(fail + crates));

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime), x + width, y);

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
        if (event.getType().equals(ChatMessageType.SERVER)) {
            if (m.endsWith("supply crate!")) {
                crates++;
            }
            if (m.contains("worthy of a gift")) {
                fail++;
            }
        }
    }

    @Override
    public void onStop() {
        int lvls = (Skills.getLevelAt(Skills.getExperience(Skill.FIREMAKING)) - startFireLvl);
        Log.info("Hope you had fun playing with " + (crates + fail) + " thots! Found " + crates + " crates and gained " + (Skills.getExperience(Skill.FIREMAKING) - startFireXP) + " firemaking experience " + (lvls > 0 ? "(+" + lvls : "(0") + " levels).");
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
            if (!new File(getDataDirectory() + "\\Koko\\FarmersMarket\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\FarmersMarket\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\FarmersMarket\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\FarmersMarket\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
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
            return value * 3600000.0 / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }

    private int getPoints() {
        InterfaceComponent pts = Interfaces.getComponent(396, 7);
        if (pts != null) {
            String p = pts.getText();
            p = p.replace("Points<br>", "");
            return Integer.parseInt(p);
        }
        return 0;
    }

    private boolean gameEnded() {
        InterfaceComponent gameEnd = Interfaces.getComponent(396, 3);
        if (gameEnd != null) {
            return gameEnd.getText().contains("returns in:");
        }
        return false;
    }

    private boolean readyForGame() {
        return Inventory.contains("Jug of wine") && Inventory.getCount("Jug of wine") == 10;

    }

    private boolean insideThot() {
        InterfaceComponent pts = Interfaces.getComponent(396, 7);
        if (pts != null) {
            return pts.isVisible();
        }
        return false;
    }

}