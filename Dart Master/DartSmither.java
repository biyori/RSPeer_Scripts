import org.rspeer.RSPeer;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;

@ScriptMeta(name = "Dart Smither", desc = "Smiths all bars into darts", developer = "Koko", version = 1.0, category = ScriptCategory.SMITHING)
public class DartSmither extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay;
    private int darts = 0, needSmithing = 0, dartID = 822, barID = 2359, dartPrice = 0, barPrice = 0;
    private boolean takeScreenie = false;

    private String status;
    private long startTime, lastAnim;

    @Override
    public void onStart() {

        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.SMITHING);
        startLvl = Skills.getCurrentLevel(Skill.SMITHING);
        lastAnimDelay = random(3000, 5000);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        if (dartPrice == 0 && barPrice == 0) {
            dartPrice = prices(dartID);
            barPrice = prices(barID);
        }

        //Run
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);

        if (Npcs.getNearest(npc -> npc.containsAction("Dismiss")) != null) {
            Log.info("Detected random event!");
            Npc event = Npcs.getNearest(npc -> npc.containsAction("Dismiss"));
            StringBuilder actions = new StringBuilder();
            if (event.getActions().length > 0) {
                for (int i = 0; i < event.getActions().length; i++)
                    actions.append(event.getActions()[i]).append(",");
            }
            logChat("We have detected a random event [" + event.getName() + "] Actions: [" + actions + "] Index: " + event.getIndex()
                    + " Target index: " + event.getTargetIndex()
                    + " My index: " + Local.getIndex()
                    + " Event: " + event.toString()
                    + " Message: " + event.getOverheadText());
        }

        if (playerHasRandom(Local)) {
            takeScreenie = true;
            Npc event = Npcs.getNearest(npc -> npc.containsAction("Dismiss") && npc.getTargetIndex() == Local.getIndex());
            StringBuilder actions = new StringBuilder();
            if (event.getActions().length > 0) {
                for (int i = 0; i < event.getActions().length; i++)
                    actions.append(event.getActions()[i]).append(",");
            }
            logChat("We have detected a random event [" + event.getName() + "] Actions: [" + actions + "] Index: " + event.getIndex()
                    + " Target index: " + event.getTargetIndex()
                    + " My index: " + Local.getIndex()
                    + " Event: " + event.toString()
                    + " Message: " + event.getOverheadText());

            JOptionPane.showMessageDialog(RSPeer.getClient().getCanvas(), "We have a random even!");
            if (event.getName().equals("Genie")) {
                if (!Dialog.isOpen()) {
                    event.interact(x -> true);
                    Time.sleepUntil(Dialog::isOpen, 1000, 10000);
                }
                if (Dialog.canContinue()) {
                    Dialog.processContinue();
                    Time.sleep(random(200, 500));
                }
            } else {
                Log.info("Do nothing");
            }
        }

        if (Inventory.contains(item -> item.getName().endsWith("bar"))) {
            status = "Dart master!";
            if (Local.isAnimating())
                lastAnim = System.currentTimeMillis();

            long msSinceAnim = System.currentTimeMillis() - lastAnim;

            if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                Log.info("Not animating! Starting new session.");

                if (Interfaces.isOpen(312)) {
                    Log.info("Time to smith!");
                    Time.sleep(random(500, 1000));
                    Interfaces.getComponent(312, 23).interact("Smith All sets");//darts
                    lastAnimDelay = random(3000, 5000);
                    Time.sleep(random(1000, 1500));
                } else {
                    Log.info("Using anvil to open smithing window");
                    SceneObject anvil = SceneObjects.getNearest("Anvil");
                    if (anvil != null) {
                        anvil.interact("Smith");
                        Time.sleepUntil(() -> Interfaces.isOpen(312), 500, 10000);
                    }
                }
            }
        } else {
            if (playerHasRandom(Local))
                return random(200, 300);

            //Bank
            Log.info("We finished smithing! Banking for more bars.");
            status = "Finished smithing!";
            SceneObject booth = SceneObjects.getNearest("Bank booth");
            if (booth != null) {
                if (!Bank.isOpen()) {
                    if (Inventory.contains(item -> item.getName().endsWith("dart tip"))) {
                        int sleep = random(3000, 20000);
                        status = "Time to bank! Sleeping for " + (sleep / 1000) + "s.";
                        Log.info("Time to bank! Sleeping for " + (sleep / 1000) + "s.");
                        Time.sleep(sleep);
                    }
                    Log.info("Opening bank");
                    status = "Opening bank";
                    booth.interact("Bank");
                    Time.sleepUntil(Bank::isOpen, 1000, 10000);
                    Time.sleep(random(500, 1200));
                } else {
                    status = "Depositing darts";
                    Log.info("Depositing darts");
                    Bank.depositAllExcept("Hammer");
                    Time.sleepUntil(() -> !Inventory.contains(item -> item.getName().endsWith("dart tip")), 500, 10000);
                    Time.sleep(random(400, 600));
                    Log.info("Getting more bars");
                    status = "Getting more bars";
                    if (Bank.contains("Mithril bar")) {
                        Bank.withdrawAll("Mithril bar");
                        needSmithing = Bank.getCount("Mithril bar");
                        Time.sleepUntil(Inventory::isFull, 1000, 10000);
                    } else {
                        Log.info("We are out of bars and cannot continue!");
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
            if (event.getMessage().endsWith("ten dart tips.")) {
                darts++;
            }
        }
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        double ETA = getETA();
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.SMITHING);//500
        int gainedXp = Skills.getExperience(Skill.SMITHING) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;

        if (takeScreenie) {
            TakeScreenshot(renderEvent.getProvider().getImage());
            takeScreenie = false;
        }

        Graphics g = renderEvent.getSource();
        g.setColor(Color.YELLOW);

        String lvlsGained = (Skills.getCurrentLevel(Skill.SMITHING) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.SMITHING) - startLvl) + ")" : "";
        int profit = ((darts * 10) * dartPrice) - (darts * barPrice);
        g.drawString("Status: " + status, 25, 265);
        g.drawString("Elapsed Time: " + formatTime(System.currentTimeMillis() - startTime) + " (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", 25, 280);
        g.drawString("Smithing lvl: " + Skills.getCurrentLevel(Skill.SMITHING) + lvlsGained + " (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", 25, 295);
        g.drawString("XP Gained: " + gainedXp + " [" + String.format("%.2f", getPerHour(gainedXp)) + "/hr]", 25, 310);
        g.drawString("Darts made: " + (darts * 10) + " (" + String.format("%.2f", getPerHour(darts * 10)) + "/hr)", 25, 325);
        g.drawString("Profit: " + profit + " (" + String.format("%.2f", getPerHour(profit)) + "/hr)", 25, 340);
    }

    @Override
    public void onStop() {
        Log.info("Thanks for making darts! Made " + (darts * 10) + "/" + (needSmithing * 10) + " darts this session");
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

    private boolean playerHasRandom(Player p) {
        return Npcs.getNearest(npc -> npc.containsAction("Dismiss") && npc.getTargetIndex() == p.getIndex()) != null;
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
        return needSmithing / (getPerHour(darts) / 60.0 / 60.0 / 1000.0);
    }

    private HashMap<String, Integer> getExchangeInfo(int id) {

        HashMap<String, Integer> exchangeInfo = new HashMap<>();

        try {
            URL url = new URL("http://services.runescape.com/m=itemdb_oldschool/api/catalogue/detail.json?item=" + id);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
            con.setUseCaches(true);
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String json = br.readLine();
            br.close();
            json = json.replaceAll("[{}\"]", "");
            String[] items = json.split("price:");
            String[] today = items[1].split(",today");

            if (today.length > 0) {
                today[0] = today[0].replaceAll(",", "");
                exchangeInfo.put("current", Integer.parseInt(today[0]));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return exchangeInfo;
    }

    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Darts\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Darts\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Darts\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Darts\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
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
        try (FileWriter fw = new FileWriter(getDataDirectory() + "\\Koko\\Darts\\Randoms.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(timestamp + "> " + text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    private int prices(int id) {
        HashMap<String, Integer> exchangeInfo = getExchangeInfo(id);
        int price = 0;
        if (exchangeInfo.get("current") != null) {
            price = exchangeInfo.get("current");
            Log.info("Price of " + id + " is " + price + "gp");
        }
        return price;
    }
}