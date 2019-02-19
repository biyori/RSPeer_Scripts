import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.commons.math.Distance;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.HashMap;

@ScriptMeta(name = "Tabby", desc = "Crafts tabs inside your house and friends", developer = "Koko", version = 3.5, category = ScriptCategory.MAGIC)
public class Tab extends Script implements RenderListener, ChatMessageListener {

    private int startXP, startLvl, lastAnimDelay, runPercent;
    private int tabs = 0, tabPrice = 0;
    private int startTabs = 0;
    private int lawRunes = 0, natureRunes = 0;
    private int sleepMin = 0, sleepMax = 0;

    private String POH, craftTab, whichLectern;


    private boolean waitForGUI, antiBan, userOwnHouse, usePoolForEnergy;

    private String status;
    private long startTime, lastAnim;
    private GUI gui;
    private RandomHandler randomEvent = new RandomHandler();

    @Override
    public void onStart() {
        gui = new GUI();
        waitForGUI = true;
        usePoolForEnergy = false;

        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.MAGIC);
        startLvl = Skills.getCurrentLevel(Skill.MAGIC);
        startTabs = Inventory.getCount(true, item -> item.containsAction("Break") &&
                (item.getName().endsWith("teleport")
                        || item.getName().endsWith("bananas")
                        || item.getName().endsWith("peaches")
                        || item.getName().endsWith("house")));

        lastAnimDelay = random(3500, 6200);//Sometimes when RS lags in world 330 this needs to be higher
        runPercent = random(20, 40);
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
            POH = gui.getHostUsername();
            craftTab = gui.getTab();
            whichLectern = gui.getLectern();
            antiBan = gui.useAntiban();
            userOwnHouse = gui.useOwnHouse();
            sleepMin = gui.getAntibanSleepMin();
            sleepMax = gui.getAntibanSleepMax();


            Log.info("Making " + craftTab + " at the " + whichLectern.toLowerCase() + " lectern.");

            if (antiBan) {
                Log.info("Using anti-ban");
                Log.info("Will sleep between " + sleepMin + "s to " + sleepMax + "s");
            }
            if (!POH.equals(""))
                Log.info("POH host: " + POH);
            if (userOwnHouse)
                Log.info("Using our own house");
            if (tabPrice == 0)
                tabPrice = tabPrices();

            waitForGUI = false;
        }

        if (craftTab == null) {
            Log.info("Something went wrong with setting GUI variables");
            setStopping(true);
            return 1000;
        }

        Player Local = Players.getLocal();

        //If the house has a pool we tell it to use it here
        if (!Movement.isRunEnabled()) {
            usePoolForEnergy = true;
        }

        //If we drank from the pool this will get us back to running, otherwise we'll wait until we can run again
        if (!Movement.isRunEnabled() && Movement.getRunEnergy() > runPercent) {
            Movement.toggleRun(true);
            runPercent = random(20, 40);
            Log.info("When we run out of energy we will run again at " + runPercent + "%");
        }

        randomEvent.checkLamp();
        if (randomEvent.doWeHaveRandom()) {
            randomEvent.handleRandom();
        } else if (Inventory.contains(1761) && !needRunes()) {

            //Not inside a house, get inside
            if (!playerInsideHouse()) {
                Log.info("Ready to enter house");
                if (userOwnHouse)
                    status = "Entering our house";
                else
                    status = "Entering friend's house";

                SceneObject Portal = SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Portal") && SceneObject.containsAction("Friend's house"));

                if (Portal != null) {
                    if (userOwnHouse) {
                        Portal.interact("Home");
                        Time.sleepUntil(() -> SceneObjects.getNearest("Lectern") != null, 1000, 10000);
                    } else {
                        Portal.interact("Friend's house");
                        Time.sleepUntil(() -> Interfaces.isVisible(162, 44), random(400, 500), 10000);
                        Time.sleep(random(250, 500));
                    }
                }
                if (Interfaces.isVisible(162, 44)) {
                    if (Interfaces.getComponent(162, 40, 0) != null) {
                        String host = Interfaces.getComponent(162, 40, 0).getText();
                        if (!host.contains(POH)) {
                            //User changed hosts
                            Log.info("Typing " + POH);
                            Keyboard.sendText(POH);
                            Time.sleep(random(250, 500));
                            Keyboard.pressEnter();//TODO: Possibly sleep untiil interface 71 is open or not open, i71 is house welcome interface
                        } else {
                            Log.info("Entering the house!");
                            Keyboard.pressEnter();
                        }
                    } else {
                        Log.info("Typing " + POH);
                        Keyboard.sendText(POH);
                        Time.sleep(random(250, 500));
                        Keyboard.pressEnter();
                    }//TODO: DOES THIS FIX THE WRONG SELECTION INSIDE slu1 house?
                    Time.sleepUntil(() -> SceneObjects.getNearest("Lectern") != null && !Interfaces.isOpen(71), 1000, 10000);
                    Time.sleep(random(500, 1200));
                }
            } else {
                //Craft tabs
                SceneObject Study = SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Lectern") && SceneObject.containsAction("Study") /*&& Scroll.distance(SceneObject) < 10*/);
                SceneObject[] sorted = Distance.sort(SceneObjects.getLoaded(SceneObject -> SceneObject.getName().equals("Lectern") && SceneObject.containsAction("Study")));

                if (Local.isAnimating())
                    lastAnim = System.currentTimeMillis();

                long msSinceAnim = System.currentTimeMillis() - lastAnim;

                //Sometimes when entering a house this will remain visible even when already inside
                if (Interfaces.isVisible(162, 44)) {
                    Log.info("Somehow this is still visible!");
                    Keyboard.pressEnter();
                    Time.sleep(random(500, 1200));
                }

                if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                    if (Study != null) {
                        Log.info("Time to study teletabs");
                        status = "Processing studies";
                        //Is there two lecterns?
                        if (sorted.length > 1) {
                            if (Local.distance(sorted[0]) < Local.distance(sorted[1])) {
                                Log.info("Closest lectern: " + Local.distance(sorted[0]));
                                Log.info("Second closest: " + Local.distance(sorted[1]));
                            } else {
                                Log.info("Closest lectern: " + Local.distance(sorted[1]));
                                Log.info("Second closest: " + Local.distance(sorted[0]));
                            }
                        }
                        int index = getClosest(Local, sorted);
                        if (!Interfaces.isOpen(79)) {
                            sorted[index].interact("Study");
                            Time.sleepUntil(() -> Interfaces.isOpen(79), 500, 10000);
                            Time.sleep(random(1000, 1500));
                        }
                        if (Interfaces.isOpen(79)) {
                            status = "Studying tabs";
                            Time.sleep(random(200, 300));
                            if (getTabInterfaceForName(craftTab) != null)
                                getTabInterfaceForName(craftTab).interact("Make-All");
                            Time.sleepUntil(() -> Local.isAnimating() || !Inventory.contains(1761), 1000, 10000);
                            Log.info("Studying tabs");
                            lastAnimDelay = random(3500, 6200);
                        }
                    }
                } else {
                    status = "Tabby!";
                    if (msSinceAnim > 0)
                        status = "Tabby! " + msSinceAnim + "ms";
                    Time.sleep(random(300, 1000));
                }
            }
        } else if (needRunes()) { //out of runes
            status = "Out of runes. Stopping script";
            Log.info("Out of runes, stopping script!");
            setStopping(true);
        } else if (!Inventory.contains(1762) && !Inventory.contains(1761)) { //Out of both noted and un-noted soft clay
            status = "Out of materials, stopping script!";
            Log.info("Out of materials, stopping script!");
            setStopping(true);
        } else if (!Inventory.contains(1761)) { //Out of clay, get more
            status = "Getting more soft clay";
            if (playerInsideHouse()) {
                int sleep = random(sleepMin * 1000, sleepMax * 1000);
                if (sleepMin <= 0 && sleepMax <= 0) {
                    sleep = random(5000, 30000);
                }
                if (!antiBan) {
                    status = "Finished tabs.";
                } else {
                    int timer = sleep / 1000;
                    for (int i = 0; i < (sleep / 1000); i++) {
                        status = "Finished tabs. Sleeping for " + timer + "s";
                        timer--;
                        Time.sleep(1000);
                    }
                    status = "Finished tabs. Sleeping for " + (sleep % 1000) + "ms";
                    Time.sleep(sleep % 1000);
                }
                status = "Getting more soft clay";

                if (usePoolForEnergy) {
                    SceneObject pool = SceneObjects.getNearest(drank -> drank.getName().endsWith("pool") && drank.containsAction("Drink"));
                    if (pool != null) {
                        status = "Drinking from pool";
                        pool.interact(x -> true);
                        Time.sleepUntil(Local::isAnimating, 500, 10000);
                        Time.sleep(random(300, 500));
                        usePoolForEnergy = false;
                        status = "Getting more soft clay";
                    }
                }

                SceneObject Portal = SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Portal") && SceneObject.containsAction("Lock"));
                if (Portal != null) {
                    Portal.interact("Enter");
                    Time.sleepUntil(() -> Npcs.getNearest("Phials") != null, 1000, 10000);
                }
                if (Npcs.getNearest("Phials") != null) {
                    Time.sleep(random(500, 1000));
                }
            } else {
                if (Npcs.getNearest("Phials") != null) {
                    Log.info("Phials is here!");
                    if (Inventory.contains(1762)) {
                        if (!Inventory.isItemSelected() && !Dialog.isViewingChatOptions()) {
                            Inventory.getFirst("Soft clay").interact("Use");
                            Time.sleep(random(300, 700));
                        }
                        if (Inventory.isItemSelected() && !Dialog.isViewingChatOptions()) {
                            Npcs.getNearest("Phials").interact("Use");
                            Time.sleepUntil(Dialog::isViewingChatOptions, 1250, 10000);
                        }
                    }
                    if (Dialog.isViewingChatOptions()) {
                        if (Inventory.getFirst(1762).getStackSize() <= 5) //Exchange 5
                            Dialog.process(1);
                        else //exchange All
                            Dialog.process(2);
                        Time.sleepUntil(() -> Inventory.isFull() || Inventory.contains(1761), 1250, 10000);
                        Time.sleep(random(200, 500));
                    }
                }
            }
        }

        if (Inventory.contains("Law rune"))
            lawRunes = Inventory.getCount(true, "Law rune");
        if (Inventory.contains("Nature rune"))
            natureRunes = Inventory.getCount(true, "Nature rune");

        if (Inventory.contains(item -> item.containsAction("Break")))
            tabs = Inventory.getCount(true, item -> item.containsAction("Break") &&
                    (item.getName().endsWith("teleport")
                            || item.getName().endsWith("bananas")
                            || item.getName().endsWith("peaches")
                            || item.getName().endsWith("house")));

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
        double ETA = getETA();
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.MAGIC);//500
        int gainedXp = Skills.getExperience(Skill.MAGIC) - startXP;//75
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (gainedXp == 0)
            ttl = 0;
        if ((tabs - startTabs) <= 0) {
            ETA = 0;
            tabs = startTabs;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 235, 227, 95);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 235, 227, 95);

        int profit = (tabs - startTabs) * tabPrice;
        int x = 25;
        int y = 250;
        String lvlsGained = (Skills.getCurrentLevel(Skill.MAGIC) - startLvl) > 0 ? " (+" + (Skills.getCurrentLevel(Skill.MAGIC) - startLvl) + ")" : "";
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
        g2.drawString(" (ETA: " + formatTime(Double.valueOf(ETA).longValue()) + ")", x + width, y);


        g2.drawString("Magic lvl: ", x, y += 15);
        g2.setColor(Color.CYAN);
        width = metrics.stringWidth("Magic lvl: ");
        g2.drawString(Skills.getCurrentLevel(Skill.MAGIC) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Magic lvl: " + Skills.getCurrentLevel(Skill.MAGIC) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Tabs made: ", x, y += 15);
        width = metrics.stringWidth("Tabs made: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(tabs - startTabs), x + width, y);
        width = metrics.stringWidth("Tabs made: " + formatter.format(tabs - startTabs));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + String.format("%.2f", getPerHour((tabs - startTabs))) + "/hr)", x + width, y);

        g2.drawString("Profit: ", x, y += 15);
        width = metrics.stringWidth("Profit: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(profit), x + width, y);
        width = metrics.stringWidth("Profit: " + formatter.format(profit));
        g2.setColor(Color.WHITE);
        g2.drawString("gp (" + formatter.format(getPerHour(tabs - startTabs) * tabPrice) + "/hr)", x + width, y);

        //Hide username
        if (Players.getLocal() != null) {
            Color tanColor = new Color(204, 187, 154);
            g2.setColor(tanColor);
            g2.fillRect(9, 459, 91, 15);
        }
    }

    @Override
    public void onStop() {
        if (gui != null) {
            synchronized (gui.lock) {
                gui.lock.notify();
            }
            gui.setVisible(false);
            gui.dispose();
        }
        if (tabs - startTabs < 0)
            tabs = startTabs;
        if (craftTab == null)
            craftTab = "tab";
        Log.info("Thanks for making tabs! Crafted " + (tabs - startTabs) + " " + craftTab + "(s) in tabs this session.");
        //Game.logout();
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private boolean needRunes() {
        return !Inventory.contains("Law rune") && !Inventory.contains("Nature rune");
    }

    private InterfaceComponent getTabInterfaceForName(String tabName) {
        switch (tabName) {
            case "Lumbridge teleport":
                return Interfaces.getComponent(79, 12); //Lumbridge Teleport
            case "Falador teleport":
                return Interfaces.getComponent(79, 13); //Falador Teleport
            case "Varrock teleport":
                return Interfaces.getComponent(79, 11); //Varrock Teleport
            case "Camelot teleport":
                return Interfaces.getComponent(79, 14); //Camelot Teleport
            case "Ardougne teleport":
                return Interfaces.getComponent(79, 15); //Ardougne Teleport
            case "Watchtower teleport":
                return Interfaces.getComponent(79, 16); //Watchtower Teleport
            case "Teleport to house":
                return Interfaces.getComponent(79, 17); //House Teleport
            case "Bones to bananas":
                return Interfaces.getComponent(79, 5); //Bones to Bananas
            case "Bones to peaches":
                return Interfaces.getComponent(79, 10); //Bones to Peaches
        }
        return getTabInterfaceForName(tabName);
    }

    private int itemIdForTab() {
        switch (craftTab) {
            case "Lumbridge teleport":
                return 8008;//Lumbridge Teleport
            case "Falador teleport":
                return 8009; //Falador Teleport
            case "Varrock teleport":
                return 8007; //Varrock Teleport
            case "Camelot teleport":
                return 8010; //Camelot Teleport
            case "Ardougne teleport":
                return 8011; //Ardougne Teleport
            case "Watchtower teleport":
                return 8012; //Watchtower Teleport
            case "Teleport to house":
                return 8013; //House Teleport
            case "Bones to bananas":
                return 8014; //Bones to Bananas
            case "Bones to peaches":
                return 8015; //Bones to Peaches
        }
        return itemIdForTab();
    }

    private int getClosest(Player p, SceneObject[] list) {
        if (list.length > 1) {
            if (whichLectern.equals("First closest")) {
                if (p.distance(list[0]) < p.distance(list[1]))
                    return 0;
                return 1;
            } else {
                if (p.distance(list[0]) < p.distance(list[1]))
                    return 1;
                return 0;
            }
        }
        return 0;
    }

    private boolean playerInsideHouse() {
        return SceneObjects.getNearest(SceneObject -> SceneObject.getName().equals("Portal") && SceneObject.containsAction("Lock")) != null;
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
        double ETA = lawRunes / (getPerHour(tabs - startTabs) / 60.0 / 60.0 / 1000.0);

        //GUI variables not set yet
        if (craftTab != null) {
            switch (craftTab) {
                case "Ardougne teleport":
                case "Watchtower teleport":
                    ETA = (lawRunes / 2.0) / (getPerHour(tabs - startTabs) / 60.0 / 60.0 / 1000.0);
                    break;
                case "Bones to peaches":
                    ETA = (natureRunes / 2.0) / (getPerHour(tabs - startTabs) / 60.0 / 60.0 / 1000.0);
                    break;
                case "Bones to bananas":
                    ETA = natureRunes / (getPerHour(tabs - startTabs) / 60.0 / 60.0 / 1000.0);
                    break;
            }
        }
        return ETA;
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

    private int tabPrices() {
        HashMap<String, Integer> exchangeInfo = getExchangeInfo(itemIdForTab());
        if (exchangeInfo.get("current") != null && tabPrice == 0) {
            tabPrice = exchangeInfo.get("current");
            Log.info("Price of " + craftTab + " is " + tabPrice + "gp");
        }
        return tabPrice;
    }
}