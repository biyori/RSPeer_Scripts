import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.InterfaceAddress;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.EquipmentSlot;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
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
import java.time.Instant;
import java.time.LocalDateTime;

@ScriptMeta(name = "BaLawLoon", desc = "Crafts law runes via balloon transportation", developer = "Koko", version = 2.0, category = ScriptCategory.RUNECRAFTING)
public class Law extends Script implements RenderListener, ChatMessageListener {

    private int bankRunes = 0, invRunes = 0, pureEssence = 0, startXP, startRunes = 0, trips = 0;
    private boolean FILLED_POUCH = true, FLEW_TO_ENTRANA = false, startLogging = false, takeScreenie = false;
    private Area BALLOON_AREA = Area.rectangular(2455, 3110, 2461, 3102, 0);
    private Area RUIN_AREA = Area.rectangular(2855, 3379, 2861, 3372, 0);
    private String status;
    private long startTime;
    private int rounds = 0, task = 0, energy, hard = 0, medium = 0, easy = 0;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.RUNECRAFTING);
        setTask(1);
        setEnergyRNG(random(20, 65));
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        }

        if (Inventory.contains(item -> item.getName().endsWith("pouch"))) {
            switch (getTask()) {
                case 1:
                    if (!canLeave()) {
                        setTask(4);
                        break;
                    }
                    if (!Dialog.isViewingChat()) {
                        Log.info("[#1] Ballon area does not have us and we have not entered entrana yet!");
                        status = "(" + getTask() + ") Getting to Balloon Transport";

                        rounds++;
                        InterfaceAddress Balloon_Map = new InterfaceAddress(() -> Interfaces.getFirst(469, i -> i.getText().equals("Entrana")));
                        if (Npcs.getNearest("Assistant Marrow") != null && Balloon_Map.resolve() == null) {//map no open
                            Npcs.getNearest("Assistant Marrow").interact("Fly");
                            //TODO: Just use basket as well
                            Time.sleepUntil(() -> Balloon_Map.resolve() != null, 1250, 10000);//sleep till map is open
                            Log.info("(" + getTask() + ") Map is open!");
                        } else {
                            if (Npcs.getNearest("Auguste") != null) {
                                Time.sleep(random(900, 1200));
                                status = "(" + getTask() + ") Switching to phase 2!";
                                setTask(2);
                                break;
                            }
                            Log.info("(" + getTask() + ") Walking to Balloon!");
                            if (Local.distance(BALLOON_AREA.getCenter()) > 20) {
                                Movement.walkToRandomized(BALLOON_AREA.getCenter().randomize(4));
                                Time.sleep(random(300, 3500));
                            }
                            Log.info("Distance from BALLOON_AREA: " + Local.distance(BALLOON_AREA.getCenter()));
                        }
                        if (Balloon_Map.resolve() != null && Balloon_Map.resolve().isVisible()) {
                            Log.info("Map is open and Entrana has been found!");
                            Interfaces.getComponent(469, 16).interact("Travel");//use the map to Entrana
                            Time.sleepUntil(() -> FLEW_TO_ENTRANA || Dialog.canContinue(), 4000, 10000);
                            Time.sleep(random(500, 1000));
                        } else {
                            Log.info("Map is not open");
                        }
                        Log.info("Walking to the BALLOON_AREA! Round " + rounds);
                    } else {
                        if (Dialog.canContinue()) {
                            Log.info("Clicking dialog");
                            if (Dialog.canContinue()) {
                                Dialog.processContinue();
                                Time.sleepUntil(Dialog::canContinue, 300, 10000);
                            }
                        }
                    }
                    break;
                case 2:
                    SceneObject Ruins = SceneObjects.getNearest("Mysterious ruins");
                    if (Ruins != null) {
                        status = "(" + getTask() + ") Entering ruins";
                        Log.info("Distance from RUIN AREA: " + Local.distance(RUIN_AREA.getCenter()));
                        Ruins.interact("Enter");
                        Time.sleepUntil(() -> SceneObjects.getNearest(sceneObject -> sceneObject.containsAction("Craft-rune") && sceneObject.getName().equals("Altar")) != null, 500, 20000);
                        Time.sleep(random(500, 1000));
                    } else {
                        status = "(" + getTask() + ") Walking to ruins";
                        if (Local.distance(RUIN_AREA.getCenter()) > 25 && Local.distance(RUIN_AREA.getCenter()) < 80) {
                            Movement.walkToRandomized(RUIN_AREA.getCenter().randomize(4));
                            Time.sleep(random(300, 3500));
                        }
                    }
                    if (SceneObjects.getNearest(sceneObject -> sceneObject.getName().equals("Altar") && sceneObject.containsAction("Craft-rune")) != null) {
                        status = "(" + getTask() + ") Switching to phase 3!";
                        Log.info("Distance from RUIN AREA: " + Local.distance(RUIN_AREA.getCenter()));
                        Time.sleep(random(1150, 1600));
                        setTask(3);
                        break;
                    }
                    break;
                case 3:
                    status = "(" + getTask() + ") Crafting runes!";
                    Player[] nearbyPlayers = Players.getLoaded();
                    if (nearbyPlayers.length > 0) {
                        for (Player nearbyPlayer : nearbyPlayers)
                            Log.info("Nearby player: lvl " + nearbyPlayer.getCombatLevel() + " [" + nearbyPlayer.getName() + "]");
                        //Game.logout();
                        //setStopping(true);

                        //start logging chat there are nearby players
                        startLogging = true;
                    }
                    int lawRunes = Inventory.getCount(true, "Law rune");
                    SceneObject Altar = SceneObjects.getNearest("Altar");
                    if (Altar != null && lawRunes < 30) {
                        Altar.interact("Craft-rune");
                        Log.info("Interacting! Now sleeping until we animate");
                        Time.sleepUntil(() -> Local.getAnimation() == 791, 750, 10000);
                        Log.info("Sleeping Finished!");
                        Time.sleep(random(2019, 3000));
                        status = "(" + getTask() + ") Emptying pouches";
                        for (Item pouch : Inventory.getItems(item -> item.getName().endsWith("pouch"))) {
                            pouch.interact("Empty");
                            Time.sleep(random(310, 320));
                        }
                        FILLED_POUCH = false;
                        status = "(" + getTask() + ") Finalizing craft";
                        Altar.interact("Craft-rune");
                        Time.sleepUntil(() -> Local.getAnimation() == 791, 750, 10000);
                        Time.sleep(random(1600, 2019));
                    } else if (lawRunes > 30) {
                        Log.info("Time to tele to bank");
                        status = "(" + getTask() + ") Time to bank!";
                        if (EquipmentSlot.RING.getItem() != null) {
                            Log.info("Using ring to Duel Arena");
                            Time.sleep(700, 1100);
                            EquipmentSlot.RING.interact("Castle Wars");//TODO: Check if animating before attempting or trust previous sleepUntil
                            Time.sleepUntil(() -> SceneObjects.getNearest("Bank chest") != null, 1000, 10000);
                            Time.sleep(random(200, 300));
                        }
                        if (SceneObjects.getNearest("Bank chest") != null) {
                            status = "(" + getTask() + ") Switching to phase 4!";

                            //stop logging chat
                            startLogging = false;

                            Time.sleep(random(400, 750));
                            setTask(4);
                            break;
                        }
                    }
                    break;
                case 4:
                    status = "Banking + Restocking";
                    SceneObject Bank_Chest = SceneObjects.getNearest("Bank chest");
                    Log.info("(" + getTask() + ") Opening bank");
                    if (Bank_Chest != null && !Bank.isOpen()) {
                        status = "(" + getTask() + ") Opening bank";
                        Time.sleep(random(300, 500));
                        Bank_Chest.interact("Use");
                        Time.sleepUntil(Bank::isOpen, 1200, 10000);
                    } else if (Bank.isOpen()) {
                        status = "(" + getTask() + ") Bank is open";
                        Log.info("(" + getTask() + ") Bank is open!");
                        if (Inventory.contains("Law rune")) {
                            startedRunes();
                            Bank.depositAll("Law rune");
                            Time.sleep(random(500, 1000));
                        }

                        Log.info("Updating rune & pure essence counts");
                        updateCounter();

                        if (EquipmentSlot.RING.getItem() == null) {
                            status = "(" + getTask() + ") Need new ring of dueling";
                            Log.info("We aren't wearing a ring! Getting a new Ring of dueling...");
                            if (!Bank.contains(item -> item.getName().startsWith("Ring of duel"))) {
                                Log.info("We are out of ring's of dueling we cannot continue!");
                                setStopping(true);
                                break;
                            }
                            Bank.withdraw(item -> item.getName().startsWith("Ring of duel"), 1);
                            Time.sleepUntil(() -> Inventory.contains(item -> item.getName().startsWith("Ring of")), 1000, 10000);
                            Bank.close();
                            Time.sleep(random(500, 1000));

                            if (!Bank.isOpen()) {
                                if (Inventory.contains(item -> item.getName().startsWith("Ring of"))) {
                                    Log.info("Equipping new ring of dueling");
                                    Item ring = Inventory.getFirst(item -> item.getName().contains("Ring of dueling"));

                                    ring.interact("Wear");
                                    Time.sleepUntil(() -> EquipmentSlot.RING.getItem() != null, 1000, 10000);
                                    Time.sleep(random(200, 300));
                                }
                            }
                            break;
                        }

                        if (!FILLED_POUCH) {
                            status = "(" + getTask() + ") Filling pouches";
                            Log.info("We need to fill pouches! Getting essence...");
                            Bank.withdrawAll("Pure essence");
                            Time.sleepUntil(Inventory::isFull, 1500, 10000);
                            Time.sleep(random(500, 1000));

                            if (Inventory.isFull()) {
                                Bank.close();
                                Time.sleepUntil(() -> !Bank.isOpen(), 2000, 5000);

                            } else {
                                Log.info("FAILED TO FILL INVENTORY");
                                break;
                            }
                            Time.sleep(random(300, 700));
                            if (!Bank.isOpen()) {
                                for (Item pouch : Inventory.getItems(item -> item.getName().endsWith("pouch"))) {
                                    Log.info("Filling " + pouch.getName());
                                    pouch.interact("Fill");
                                    Time.sleep(random(310, 315));
                                }
                                FILLED_POUCH = true;
                            }
                            break;
                        }

                        if (!Inventory.isFull()) {
                            status = "(" + getTask() + ") Need to fill inventory up with more essence!";
                            Bank.withdrawAll("Pure essence");
                            Time.sleepUntil(Inventory::isFull, 1500, 10000);
                            Bank.close();
                            Time.sleep(random(500, 1000));
                        }

                        if (canLeave()) {
                            Log.info("We can finally restart to phase 1!");
                            status = "(" + getTask() + ") Ready for phase 1!";
                            trips++;
                            setTask(1);
                            break;
                        }
                    }
                    break;
            }
        }

        if (random(1, 5000) == 1024) {
            Log.info("LETS DO SOME ANTI BAN!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            hard++;
        }
        if (random(0, 512) == 256) {
            Log.info("LETS DO SOME MORE ANTI BAN!!!!!!!!!!!!!!!!!!!!!");
            medium++;
        }
        if (random(0, 255) == 22) {
            Log.info("MORE ANTI BAN WHICH ONE IS BEST?!?!?!");
            easy++;
        }
        if (!Movement.isRunEnabled() && Movement.getRunEnergy() >= getEnergyRNG() && !Bank.isOpen()) {
            Log.info("We have energy, let's run");
            Movement.toggleRun(true);
            int runNextAt = random(20, 65);
            Log.info("Next time we'll run at " + runNextAt);
            setEnergyRNG(runNextAt);
        }

        if (Inventory.contains("Law rune")) {
            invRunes = Inventory.getCount(true, "Law rune");
        }

        return random(200, 300);
    }

    @Override
    public void notify(ChatMessageEvent event) {
        if (event.getType().equals(ChatMessageType.SERVER)) {
            if (event.getMessage().endsWith("fly to Entrana.")) {
                FLEW_TO_ENTRANA = true;
            }
        }
        if (startLogging) {
            if (event.getType().equals(ChatMessageType.PUBLIC)) {
                Log.info(event.getSource() + ": " + event.getMessage());
                logChat(event.getSource() + ": " + event.getMessage(), 1);
            } else {
                logChat("[" + event.getType() + "] " + event.getSource() + ": " + event.getMessage(), 0);
            }
        } else {
            //Log everything else
            logChat("[" + event.getType() + "] " + event.getSource() + ": " + event.getMessage(), 0);
        }
      /*  if (event.getType().equals(ChatMessageType.GAME)) {
            Log.info("WORKS [Game]: " + event.getMessage());
        } else {
            Log.info("WHAT'S THIS? [" + event.getType() + "]" + event.getMessage());
        }*/

        //TODO: MULE?
        /*if (event.getType().equals(ChatMessageType.TRADE)) {
            String name = event.getMessage().replaceAll(" wishes to trade with you.", "");
            Player toTrade = Players.getNearest(name);
            if (toTrade != null) {
                toTrade.interact("Trade with");
            }
        }*/
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        double ETA = (pureEssence / getPerHour((bankRunes + invRunes))) * 60 * 1000;//60 minutes in an hour, 60 seconds in 1 minute, 1000ms in 1 second
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.RUNECRAFTING);//500
        int gainedXp = Skills.getExperience(Skill.RUNECRAFTING) - startXP;//75
        double ttl = (nextLvlXp / getPerHour(gainedXp)) * 60 * 1000;
        if (gainedXp == 0)
            ttl = 0;
        if (pureEssence == 0)
            ETA = 0;

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }

        Graphics g = renderEvent.getSource();
        g.drawString("Status: " + status, 30, 270);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime) + " Trips: " + trips + " (" + String.format("%.2f", getPerHour((trips))) + "/hr)", 30, 285);
        g.drawString("Runecrafting lvl: " + Skills.getCurrentLevel(Skill.RUNECRAFTING) + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 30, 300);
        g.drawString("Runes crafted: " + (bankRunes + invRunes) + " (" + String.format("%.2f", getPerHour((bankRunes + invRunes))) + "/hr)", 30, 315);
        g.drawString("Essence in bank: " + pureEssence + " (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", 30, 330);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for BaLawLooning! Anti-ban stats [hard: " + hard + "x] [medium: " + medium + "x] [easy: " + easy + "x]");
        if (isStopping())
            Game.logout();
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private int getTask() {
        return task;
    }

    private void setTask(int id) {
        task = id;
    }

    private int getEnergyRNG() {
        return energy;
    }

    private void setEnergyRNG(int val) {
        energy = val;
    }

    private String formatTime(final long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Lawl\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Lawl\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Lawl\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Lawl\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
        }
    }

    private void logChat(String text, int a) {
        LocalDateTime timestamp = LocalDateTime.now();
        if (a == 1) {
            try (FileWriter fw = new FileWriter("C:\\Users\\SS_Ve\\Documents\\RSPeer\\cache\\chatlog.txt", true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(timestamp + "> " + text);
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        } else {
            try (FileWriter fw = new FileWriter("C:\\Users\\SS_Ve\\Documents\\RSPeer\\cache\\other.txt", true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(timestamp + "> " + text);
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        }
    }

    private boolean canLeave() {
        if (!Inventory.isFull()) {
            return false;
        } else if (!FILLED_POUCH) {
            return false;
        } else if (EquipmentSlot.RING.getItem() == null) {
            return false;
        }
        return true;
    }

    private int startedRunes() {
        if (startRunes == 0) {
            if (Bank.contains("Law rune"))
                startRunes = Bank.getCount("Law rune") + invRunes;
        }
        return startRunes;
    }

    private void updateCounter() {
        if (Bank.isOpen()) {
            if (Bank.contains("Law rune"))
                bankRunes = Bank.getCount("Law rune") - startedRunes();
            if (Bank.contains("Pure essence"))
                pureEssence = Bank.getCount("Pure essence");
        }
    }

    private double getPerHour(double value) {
        if ((System.currentTimeMillis() - startTime) > 0) {
            return value * 3600000d / (System.currentTimeMillis() - startTime);
        } else {
            return 0;
        }
    }
}