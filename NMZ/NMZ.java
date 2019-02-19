import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Varps;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Prayers;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
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

@ScriptMeta(name = "NMZ", desc = "Trains combat at NMZ n shit", developer = "Koko", version = 1.0, category = ScriptCategory.COMBAT)
public class NMZ extends Script implements RenderListener, ChatMessageListener {

    private int startStrXP, startHpXP, startRangeXP, startMageXP, startDefXP, startAtkXP, startStrLvl, startDefLvl, startAtkLvl, startHpLvl, startRangelvl, startMagiclvl;
    private boolean drinkOverload = false, needPots = false, takeScreenie = false, waitForGUI;
    private int nmzPoints = 0, dreamConfig = 999999999, afkHealthVal;
    private long nextFlick = System.currentTimeMillis();
    private String status, combatStyle;
    private long startTime;
    private GUI gui;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        Log.info("Loading GUI");
        gui = new GUI();
        gui.isVisible();
        gui.setVisible(true);
        waitForGUI = true;

        //Prefetch combat stats + exp
        //Get exp to handle overload potions increasing strength display
        startTime = System.currentTimeMillis();
        startStrXP = Skills.getExperience(Skill.STRENGTH);
        startDefXP = Skills.getExperience(Skill.DEFENCE);
        startAtkXP = Skills.getExperience(Skill.ATTACK);
        startHpXP = Skills.getExperience(Skill.HITPOINTS);
        startRangeXP = Skills.getExperience(Skill.RANGED);
        startMageXP = Skills.getExperience(Skill.MAGIC);
        startStrLvl = Skills.getLevelAt(startStrXP);
        startDefLvl = Skills.getLevelAt(startDefXP);
        startAtkLvl = Skills.getLevelAt(startAtkXP);
        startHpLvl = Skills.getLevelAt(startHpXP);
        startRangelvl = Skills.getLevelAt(startRangeXP);
        startMagiclvl = Skills.getLevelAt(startMageXP);
        status = "Loading up!";
    }

    @Override
    public int loop() {
        //Wait for GUI vars
        if (gui.isVisible()) {
            Log.info("Waiting for GUI");
            return 1000;
        }
        if (gui.isStartedClicked() && waitForGUI) {
            Log.info("Script started!");

            afkHealthVal = gui.getAfkHpVal();
            combatStyle = gui.getCombatStyle();
            Log.info("Training " + combatStyle + " and will AFK up to " + afkHealthVal + " health.");
            waitForGUI = false;
        }

        if (combatStyle == null) {
            Log.info("Something went wrong with setting GUI variables");
            setStopping(true);
            return 1000;
        }
        Player Local = Players.getLocal();
        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (Local.getFloorLevel() == 3) {
            if (Inventory.isFull() && Health.getCurrent() > 50) {
                status = "Getting ready for battle!";

                //Reset boolean just incase it was set from a previous instance
                drinkOverload = false;

                if (!Local.isMoving()) {
                    Position moveTo = new Position((Local.getX()) + random(-10, 12), (Local.getY()) + random(7, 16), Local.getFloorLevel());
                    Log.info("Walking to " + moveTo.toString());
                    Movement.walkTo(moveTo);
                }

                Inventory.getFirst(item -> item.getName().startsWith("Overload")).interact("Drink");
                Time.sleepUntil(Local::isAnimating, 1250, 10000);
                Time.sleep(random(1000, 2000));

                for (int i = 0; i < random(30, 70); i++) {
                    if (currentAbsorption() == 1000)
                        break;
                    Inventory.getFirst(item -> item.getName().startsWith("Absorption")).interact("Drink");
                    Time.sleep(random(100, 200));
                }
            } else if (drinkOverload && Inventory.contains(item -> item.getName().startsWith("Overload"))) {
                int sleepN = random(1000, 15000);
                int timer = sleepN / 1000;
                Log.info("Overload wore off! Sleeping for " + timer + "." + (sleepN % 1000) + "s till next dose");
                status = "Overload wore off! Sleeping for " + timer + "." + (sleepN % 1000) + "s";

                for (int i = 0; i < (sleepN / 1000); i++) {
                    status = "Overload wore off! Sleeping for " + timer + "." + (sleepN % 1000) + "s";
                    timer--;
                    Time.sleep(1000);
                }
                status = "Overload wore off! Sleeping for " + (sleepN % 1000) + "ms";
                Time.sleep(sleepN % 1000);

                Inventory.getFirst(item -> item.getName().startsWith("Overload")).interact("Drink");
                Time.sleepUntil(Local::isAnimating, 1250, 10000);
                Time.sleep(random(8000, 10000));
                drinkOverload = false;
            } else if (Health.getCurrent() > 1 && currentAbsorption() > 100) {
                if (Health.getCurrent() > 50 && Inventory.contains(item -> item.getName().startsWith("Overload"))) {
                    Log.info("We almost didn't drink an overload!!!!!!!!");
                    takeScreenie = true;
                    return random(1000, 2000);
                }
                status = "Lowering HP";
                //Item cake = Inventory.getFirst("Dwarven rock cake");
                Item orb = Inventory.getFirst("Locator orb");
                if (timeToFlick() && Health.getCurrent() <= afkHealthVal) {
                    orb.interact("Feel");
                    Time.sleep(random(500, 800));
                } else if (Health.getCurrent() >= afkHealthVal + 1) {//Prevent script from getting out of sync from timeToFlick() regen
                    orb.interact("Feel");
                    Time.sleep(random(200, 400));
                } else {
                    status = "AFK";
                    Time.sleep(random(500, 800));
                }
                status = status + " [" + (nextFlick - System.currentTimeMillis()) / 1000 + "s]";
            } else if (Inventory.contains(item -> item.getName().startsWith("Absorption")) && currentAbsorption() < 400) {
                status = "Restoring absorption";
                for (int i = 0; i < random(8, 25); i++) {
                    if (Inventory.contains(item -> item.getName().startsWith("Absorption"))) {
                        status = "Drinking absorptions";
                        if (currentAbsorption() > 900)
                            break;
                        Inventory.getFirst(item -> item.getName().startsWith("Absorption")).interact("Drink");
                        Time.sleep(random(100, 300));
                    }
                }
            } else if (timeToFlick()) {
                status = "Time to flick!";
                Prayers.toggleQuickPrayer(true);
                Time.sleep(random(300, 350));
                Prayers.toggleQuickPrayer(false);
                Time.sleep(random(300, 350));
                setNextFlick();
                Time.sleep(random(500, 1000));
                if (Prayers.isQuickPrayerActive()) {
                    Prayers.toggleQuickPrayer(false);
                    Time.sleepUntil(() -> !Prayers.isQuickPrayerActive(), 1250, 10000);
                }
            } else {
                int sleep = random(10000, 30000);
                goAFK(sleep);
            }
        } else if (Npcs.getNearest("Dominic Onion") != null) {
            if (canDream() && Varps.get(1058) < dreamConfig) {
                status = "Getting into NMZ";

                Npc dom = Npcs.getNearest("Dominic Onion");
                if (dom != null) {
                    if (!Dialog.isViewingChatOptions()) {
                        dom.interact("Dream");
                        Time.sleepUntil(Dialog::isOpen, 500, 10000);
                    }
                    if (Dialog.isViewingChatOptions() && !Dialog.canContinue()) {
                        if (Dialog.getChatOption(c -> c.startsWith("Previous")) != null) {
                            Dialog.process(3);//previous customisable
                            Time.sleepUntil(Dialog::canContinue, 500, 10000);
                        } else if (Dialog.getChatOption(c -> c.equals("Yes")) != null) {
                            Dialog.process(0);
                            Time.sleepUntil(Dialog::canContinue, 500, 10000);
                            dreamConfig = Varps.get(1058);
                        }
                    }
                    if (Dialog.canContinue()) {
                        Dialog.processContinue();
                        Time.sleepUntil(Dialog::isProcessing, 500, 10000);
                        Time.sleep(random(500, 1000));
                    }
                }
            } else if (canDream() && Varps.get(1058) == dreamConfig) {
                SceneObject coffer = SceneObjects.getNearest("Dominic's coffer");
                if (coffer != null && coffer.containsAction("Unlock")) {
                    if (!Interfaces.isOpen(213)) {
                        coffer.interact("Unlock");
                        Time.sleepUntil(() -> Interfaces.isOpen(213), 500, 10000);
                    }
                    if (Interfaces.isOpen(213)) {
                        status = "Entering PIN";
                        PinSolver pin = new PinSolver();
                        pin.enterPin(1024);
                    }
                } else {
                    status = "Coffer unlocked";
                    SceneObject potion = SceneObjects.getAt(new Position(2605, 3117, 0))[0];
                    InterfaceComponent dream = Interfaces.getComponent(129, 6);

                    if (potion != null && dream == null) {
                        Log.info("Drinking potion to get into NMZ");
                        potion.interact("Drink");
                        Time.sleepUntil(() -> Interfaces.isOpen(129), 500, 10000);
                    } else {
                        Log.info("Time to dream!");
                        dream.interact("Continue");
                        Time.sleepUntil(() -> Local.getFloorLevel() == 3, 500, 10000);
                    }
                }
            } else {
                status = "Getting supplies for NMZ";
                //if (!Inventory.contains("Dwarven rock cake")) {
                if (!Inventory.contains("Locator orb")) {
                    Log.info("We can't dream without our locator orb!");
                    setStopping(true);
                } else if (needPots) {
                    status = "Buying pots";
                    SceneObject chest = SceneObjects.getNearest("Rewards chest");
                    if (chest != null && !Interfaces.isOpen(206) && !Interfaces.isOpen(213)) {
                        Log.info("Opening chest");
                        chest.interact("Search");
                        Time.sleepUntil(() -> Interfaces.isOpen(206) || Interfaces.isOpen(213), 500, 7000);
                    } else if (Interfaces.isOpen(213)) {
                        status = "Entering PIN";
                        PinSolver pin = new PinSolver();
                        pin.enterPin(1024);
                    } else if (Interfaces.getComponent(206, 2, 4).containsAction("Benefits")) {
                        Log.info("Chest is open! Switching to benefits tab...");
                        InterfaceComponent benefits = Interfaces.getComponent(206, 2, 4);
                        benefits.interact("Benefits");
                        Time.sleepUntil(() -> Interfaces.getComponent(206, 6, 9).isVisible(), 500, 10000);
                    } else if (Interfaces.isVisible(206, 6, 9)) {
                        Log.info("Ready to buy shit!");
                        InterfaceComponent absorption = Interfaces.getComponent(206, 6, 11);
                        InterfaceComponent absorbShop = Interfaces.getComponent(206, 6, 9);
                        InterfaceComponent overload = Interfaces.getComponent(206, 6, 8);
                        InterfaceComponent overloadShop = Interfaces.getComponent(206, 6, 6);
                        if (absorption != null && overload != null) {
                            if (!absorption.getText().equalsIgnoreCase("(255)")) {
                                absorbShop.interact("Buy-X");
                                Time.sleep(random(1000, 2000));
                                Keyboard.sendText("255");
                                Time.sleep(random(250, 500));
                                Keyboard.pressEnter();
                                Time.sleepUntil(() -> absorption.getText().equals("(255)"), 500, 5000);
                            } else if (!overload.getText().equalsIgnoreCase("(255)")) {
                                overloadShop.interact("Buy-X");
                                Time.sleep(random(1000, 2000));
                                Keyboard.sendText("255");
                                Time.sleep(random(250, 500));
                                Keyboard.pressEnter();
                                Time.sleepUntil(() -> overload.getText().equals("(255)"), 500, 5000);
                            } else {
                                needPots = false;
                            }
                        }
                    }
                } else if (!Inventory.contains(item -> item.getName().startsWith("Overload"))) {
                    Log.info("We can't dream yet without our overload pots!");
                    //get overlords
                    SceneObject overloads = SceneObjects.getNearest("Overload potion");
                    if (overloads != null) {
                        if (!Interfaces.isVisible(162, 44)) {
                            overloads.interact("Take");
                            Time.sleepUntil(() -> Interfaces.isVisible(162, 44), 500, 10000);
                        } else if (Interfaces.isVisible(162, 44)) {
                            Time.sleep(random(400, 600));
                            Keyboard.sendText("20");
                            Time.sleep(random(250, 500));
                            Keyboard.pressEnter();
                            Time.sleep(random(1000, 2000));
                        }
                    }
                } else if (!Inventory.isFull()) {
                    Log.info("We can't dream yet because our inventory is not completely full!");
                    //Count overloads
                    int count = countOverloads();

                    Log.info("Overload count: " + count);
                    if (count < 5) {
                        status = "Buying overloads";
                        SceneObject overloads = SceneObjects.getNearest("Overload potion");
                        if (overloads != null) {
                            if (!Interfaces.isVisible(162, 44)) {
                                overloads.interact("Take");
                                Time.sleepUntil(() -> Interfaces.isVisible(162, 44), 500, 10000);
                            } else if (Interfaces.isVisible(162, 44)) {
                                int takeOut = (5 - count) * 4;
                                Log.info("We need to take out " + takeOut + " doses of overload");
                                Time.sleep(random(400, 600));
                                Keyboard.sendText(String.valueOf(takeOut));
                                Time.sleep(random(250, 500));
                                Keyboard.pressEnter();
                                Time.sleep(random(1000, 2000));
                            }
                        }
                    } else {
                        SceneObject absorption = SceneObjects.getNearest("Absorption potion");
                        if (absorption != null) {
                            status = "Buying absorptions";
                            if (!Interfaces.isVisible(162, 44)) {
                                absorption.interact("Take");
                                Time.sleepUntil(() -> Interfaces.isVisible(162, 44), 500, 10000);
                            } else if (Interfaces.isVisible(162, 44)) {
                                Time.sleep(random(400, 600));
                                Keyboard.sendText("222");
                                Time.sleep(random(250, 500));
                                Keyboard.pressEnter();
                                Time.sleep(random(1000, 2000));
                            }
                        }
                    }
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(getCombatSkill());
        int nextHpLvlXp = Skills.getExperienceToNextLevel(Skill.HITPOINTS);
        int gainedXp = Skills.getExperience(getCombatSkill()) - getStartXP();
        int gainedHpXp = Skills.getExperience(Skill.HITPOINTS) - startHpXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        double ttlHp = (nextHpLvlXp / (getPerHour(gainedHpXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0) {
            ttl = 0;
            ttlHp = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        InterfaceComponent points = Interfaces.getComponent(202, 8, 3);
        if (points != null) {
            String msg = points.getText();
            msg = msg.replaceAll("Points:<br>", "");
            nmzPoints = Integer.parseInt(msg.replaceAll(",", ""));
        }

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 235, 235, 110);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 235, 110);

        int x = 25;
        int y = 250;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        String lvlsGained = (Skills.getLevelAt(Skills.getExperience(getCombatSkill())) - getStartLvl()) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(getCombatSkill())) - getStartLvl()) + ")" : "";
        String hplvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.HITPOINTS)) - startHpLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.HITPOINTS)) - startHpLvl) + ")" : "";

        g2.setColor(Color.WHITE);
        g2.drawString("NMZ Points: ", x, y);
        int width = metrics.stringWidth("NMZ Points: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(nmzPoints), x + width, y);
        width = metrics.stringWidth("NMZ Points: " + formatter.format(nmzPoints));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(nmzPoints)) + "/hr)", x + width, y);

        g2.drawString(combatStyle + " lvl: ", x, y += 15);
        g2.setColor(Color.GREEN);
        width = metrics.stringWidth(combatStyle + " lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(getCombatSkill())) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth(combatStyle + " lvl: " + Skills.getLevelAt(Skills.getExperience(getCombatSkill())) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("HP lvl: ", x, y += 15);
        g2.setColor(Color.RED);
        width = metrics.stringWidth("HP lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.HITPOINTS)) + hplvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("HP lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.HITPOINTS)) + hplvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttlHp).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedHpXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedHpXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedHpXp)) + "/hr)", x + width, y);

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
    }

    @Override
    public void notify(ChatMessageEvent event) {
        String m = event.getMessage().toLowerCase();
        if (event.getType().equals(ChatMessageType.SERVER) && (m.contains("overload have worn off") && (m.endsWith("again.")))) {
            Log.info("We need to drink an overload!");
            drinkOverload = true;
        } else if (event.getType().equals(ChatMessageType.SERVER) && (m.contains("barrel is") && (m.endsWith("empty.")))) {
            Log.info("We need pots!");
            needPots = true;
        }
    }

    @Override
    public void onStop() {
        if (gui != null) {
            gui.setVisible(false);
            gui.dispose();
        }
        Log.info("Hope you had fun training! Gained " + (Skills.getExperience(getCombatSkill()) - getStartXP()) + " " + getCombatSkill().toString().toLowerCase() + " XP and " + (Skills.getExperience(Skill.HITPOINTS) - startHpXP) + " hitpoint XP this session");
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private int currentAbsorption() {
        InterfaceComponent absorption = Interfaces.getComponent(202, 3, 5);
        if (absorption != null && !absorption.getText().isEmpty()) {
            return Integer.parseInt(absorption.getText().replace(",", ""));
        }
        return 0;
    }

    private void goAFK(int sleepN) {
        status = "Going AFK";
        int timer = sleepN / 1000;
        Log.info("Fighting NMZ (Next flick: " + (nextFlick - System.currentTimeMillis()) / 1000 + "s) Sleeping for " + timer + "." + (sleepN % 1000) + "s");

        for (int i = 0; i < (sleepN / 1000); i++) {
            status = "Fighting NMZ (Next flick: " + (nextFlick - System.currentTimeMillis()) / 1000 + "s) Sleeping for " + timer + "s";
            timer--;
            Time.sleep(1000);
        }

        status = "Fighting NMZ (Next flick: " + (nextFlick - System.currentTimeMillis()) / 1000 + "s) Sleeping for " + (sleepN % 1000) + "ms";
        Time.sleep(sleepN % 1000);
    }

    private int countOverloads() {
        Item[] overload = Inventory.getItems();
        int count = 0;
        for (Item over : overload) {
            if (over != null && over.getName().startsWith("Overload")) {
                count++;
            }
        }
        return count;
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

    private void logChat(String text) {
        LocalDateTime timestamp = LocalDateTime.now();

        if (!new File(getDataDirectory() + "\\Koko\\NMZ\\Screenshots").exists()) {
            new File(getDataDirectory() + "\\Koko\\NMZ\\Screenshots").mkdirs();
        }

        try (FileWriter fw = new FileWriter(getDataDirectory() + "\\Koko\\Sears\\Randoms.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(timestamp + "> " + text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private boolean canDream() {
        //if (!Inventory.contains("Dwarven rock cake")) {
        if (!Inventory.contains("Locator orb")) {
            Log.info("We can't dream without our locator orb!");
            return false;
        } else if (!Inventory.contains("Overload (4)")) {
            Log.info("We can't dream yet without our overlord pots!");
            return false;
        } else if (!Inventory.contains("Absorption (4)")) {
            Log.info("We can't dream yet without our absorption pots!");
            return false;
        } else if (!Inventory.isFull()) {
            Log.info("We can't dream yet because our inventory is not completely full!");
            return false;
        } else if (needPots) {
            Log.info("We can't dream yet because we need pots!");
            return false;
        }
        return true;
    }

    private boolean timeToFlick() {
        return (nextFlick - System.currentTimeMillis()) < 0;
    }

    private void setNextFlick() {
        nextFlick = System.currentTimeMillis() + random(30000, afkHealthVal * 60 * 1000) + random(0, 5000);//5hp = 5*60 for hp regen (+1 per min), * 1000 to get MS
    }

    private Skill getCombatSkill() {
        if (combatStyle != null) {
            switch (combatStyle) {
                case "Ranged":
                    return Skill.RANGED;
                case "Magic":
                    return Skill.MAGIC;
                case "Strength":
                    return Skill.STRENGTH;
                case "Attack":
                    return Skill.ATTACK;
                case "Defence":
                    return Skill.DEFENCE;
            }
            return getCombatSkill();
        }
        return Skill.HITPOINTS;
    }

    private int getStartXP() {
        if (combatStyle != null) {
            switch (combatStyle) {
                case "Ranged":
                    return startRangeXP;
                case "Magic":
                    return startMageXP;
                case "Strength":
                    return startStrXP;
                case "Attack":
                    return startAtkXP;
                case "Defence":
                    return startDefXP;
            }
            return getStartXP();
        }
        return startHpXP;
    }

    private int getStartLvl() {
        if (combatStyle != null) {
            switch (combatStyle) {
                case "Ranged":
                    return Skills.getLevelAt(startRangeXP);
                case "Magic":
                    return Skills.getLevelAt(startMageXP);
                case "Strength":
                    return Skills.getLevelAt(startStrXP);
                case "Attack":
                    return Skills.getLevelAt(startAtkXP);
                case "Defence":
                    return Skills.getLevelAt(startDefXP);
            }
            return getStartXP();
        }
        return Skills.getLevelAt(startHpXP);
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