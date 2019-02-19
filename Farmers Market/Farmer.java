import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Player;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.ItemTables;
import org.rspeer.runetek.api.component.WorldHopper;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.local.Health;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ScriptMeta(name = "Farmers Market", desc = "Milks the fuk out of master farmers", developer = "Koko", version = 1.0, category = ScriptCategory.THIEVING)
public class Farmer extends Script implements RenderListener, ChatMessageListener {

    private int startThieveXP, startThieveLvl, steal = 0, fail = 0, eatPercent, seeds = 0, ranarr = 0, ranarrPrice, snapdragon = 0, snapPrice, torstol = 0, torstolPrice, hops = 0;
    private boolean takeScreenie = false, prefetchPrices;
    private String status;
    private long startTime, lastSteal = 0, hopTimer;
    private RandomHandler randomEvent = new RandomHandler();
    private boolean ardougne = false;


    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startThieveXP = Skills.getExperience(Skill.THIEVING);
        startThieveLvl = Skills.getLevelAt(startThieveXP);
        status = "Loading up!";
        eatPercent = random(40, 60);
        prefetchPrices = true;
        hopTimer = System.currentTimeMillis() + random(600000, 1800000);//10-30 minute hops
        Log.info("Next hop will happen at " + ((hopTimer - System.currentTimeMillis()) / 1000.0) / 60.0 + " mins");
    }

    @Override
    public int loop() {
        if (prefetchPrices) {
            status = "Prefetching prices...";
            new Thread(() -> {
                ranarrPrice = getExchangeInfo(5295);
                snapPrice = getExchangeInfo(5300);
                torstolPrice = getExchangeInfo(5304);
            }).run();
            prefetchPrices = false;
            status = "DONE!";
        }

        Player Local = Players.getLocal();
        randomEvent.checkLamp();
        randomEvent.findNearbyRandoms();
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (Health.getPercent() > eatPercent && !Inventory.isFull()) {
            status = "Looking for trouble";

            Npc masterFarmer = Npcs.getNearest(3257);
            if (masterFarmer != null) {
                if (Local.getAnimation() == 424) {
                    status = "Stunned";

                    if (ardougne) {
                        status = "Dropping seeds";
                        dropSeeds();
                        Time.sleep(random(2000, 3000));
                    } else
                        Time.sleep(random(4000, 5000));
                    if (EquipmentSlot.NECK.getItem() == null) {
                        if (Inventory.contains("Dodgy necklace")) {
                            Inventory.getFirst("Dodgy necklace").interact("Wear");
                            Time.sleepUntil(() -> EquipmentSlot.NECK.getItem() != null, random(200, 300), 3000);
                        }
                    }
                    if (hopTimer < System.currentTimeMillis()) {
                        Log.info("Time to hop!");
                        Time.sleepUntil(() -> !Local.isHealthBarVisible(), 500, 5000);
                        if (WorldHopper.randomHopInP2p()) {
                            hopTimer = System.currentTimeMillis() + random(600000, 1800000);//10-30 minute hops
                            hops++;
                            Time.sleepUntil(() -> Game.getState() == 25, random(400, 500), 5000);
                        }
                    }
                } else {
                    status = "LOOT! Last action(" + (lastSteal > 0 ? (System.currentTimeMillis() - lastSteal) : 0) + "ms)";
                    if (masterFarmer.interact("Pickpocket")) {
                        if (masterFarmer.distance(Local) > 3)
                            Time.sleepUntil(() -> masterFarmer.distance(Local) < 3, 100, 5000);
                        lastSteal = System.currentTimeMillis();
                        Log.info("Stealing from master farmer at " + masterFarmer.getPosition());
                        Time.sleep(random(150, 210));
                    }
                }
            } else {
                if (ardougne) {
                    Area ardyFarm = Area.rectangular(2635, 3368, 2640, 3359);
                    Position walk1 = ardyFarm.getTiles().get(random(0, ardyFarm.getTiles().size() - 1));
                    if (!walk1.isPositionWalkable())
                        return 100;//find a new spot
                    if (Movement.walkTo(walk1))
                        Time.sleepUntil(() -> Npcs.getNearest(3257) != null, random(200, 300), 3000);
                } else {
                    Log.info("Walking closer to farmer");
                    Area farm = Area.rectangular(3077, 3252, 3085, 3247);
                    Position walk2 = farm.getTiles().get(random(0, farm.getTiles().size() - 1));
                    if (!walk2.isPositionWalkable())
                        return 100;//find a new spot
                    if (Movement.walkTo(walk2))
                        Time.sleepUntil(() -> Npcs.getNearest(3257) != null, random(200, 300), 3000);
                }
            }
        } else {
            if (Inventory.contains(item -> item.containsAction("Drink")) && !Inventory.isFull()) {
                if (Tabs.getOpen() != Tab.INVENTORY)
                    Tabs.open(Tab.INVENTORY);

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
            } else {
                status = "Banking!";
                SceneObject booth = SceneObjects.getNearest("Bank booth");
                if (booth != null) {
                    if (!Bank.isOpen() && !Interfaces.isOpen(213)) {
                        booth.interact("Bank");
                        Time.sleepUntil(() -> Bank.isOpen() || Interfaces.isOpen(213), 500, 5000);
                    }
                    if (Interfaces.isOpen(213)) {
                        status = "Entering PIN";
                        PinSolver pin = new PinSolver();
                        pin.enterPin(1024);
                    }
                    if (Bank.isOpen()) {
                        if (Bank.depositInventory())
                            Time.sleepUntil(Inventory::isEmpty, 500, 5000);

                        if (ItemTables.contains(ItemTables.BANK, s -> s.equals("Jug of wine"))) {
                            Bank.withdraw("Jug of wine", 10);
                            Time.sleepUntil(() -> Inventory.contains("Jug of wine"), 500, 5000);
                            Time.sleep(random(500, 750));
                        } else {
                            Log.info("Ran out of food!");
                            setStopping(true);
                        }
                        if (ItemTables.contains(ItemTables.BANK, s -> s.equals("Dodgy necklace"))) {
                            Bank.withdraw("Dodgy necklace", 4);
                            Time.sleepUntil(() -> Inventory.contains("Dodgy necklace"), 400, 4000);
                        }
                        if (Inventory.contains("Jug of wine"))
                            Bank.close();
                    }
                } else {
                    status = "Walking closer to bank";
                    if (ardougne) {
                        Area ardyBank = Area.rectangular(2615, 3334, 2619, 3332);
                        if (Movement.walkTo(ardyBank.getCenter().randomize(2)))
                            Time.sleepUntil(() -> SceneObjects.getNearest("Bank booth") != null, random(200, 300), 3000);
                    }
                }
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextLvlXp = Skills.getExperienceToNextLevel(Skill.THIEVING);
        int gainedXp = Skills.getExperience(Skill.THIEVING) - startThieveXP;
        double ttl = (nextLvlXp / (getPerHour(gainedXp) / 60.0 / 60.0 / 1000.0));
        if (nextLvlXp == 0) {
            ttl = 0;
        }
        Graphics g = renderEvent.getSource();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(20, 200, 209, 140);
        g2.setColor(Color.WHITE);
        g2.drawRect(20, 200, 209, 140);

        int x = 25;
        int y = 215;
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        FontMetrics metrics = g2.getFontMetrics();

        double thieveRate = ((double) steal / (double) (steal + fail)) * 100;
        String lvlsGained = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) + ")" : "";

        g2.setColor(Color.WHITE);
        g2.drawString("Thieving Lvl: ", x, y);
        g2.setColor(new Color(238, 130, 238));
        int width = metrics.stringWidth("Thieving Lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + lvlsGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Thieving Lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + lvlsGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedXp), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedXp));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedXp)) + "/hr)", x + width, y);

        g2.drawString("Seeds: ", x, y += 15);
        width = metrics.stringWidth("Seeds: ");
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString(formatter.format(seeds * 2), x + width, y);
        width = metrics.stringWidth("Seeds: " + formatter.format(seeds * 2));
        g2.drawString(" (" + formatter.format(getPerHour(seeds * 2)) + "/hr)", x + width, y);
        g2.setColor(Color.WHITE);

        g2.drawString("Ranarr: ", x, y += 15);
        width = metrics.stringWidth("Ranarr: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(ranarr * 2), x + width, y);
        width = metrics.stringWidth("Ranarr: " + formatter.format(ranarr * 2));
        g2.drawString(" (" + formatter.format(getPerHour(ranarr * 2)) + "/hr)", x + width, y);
        g2.setColor(Color.WHITE);

        g2.drawString("Snapdragon: ", x, y += 15);
        width = metrics.stringWidth("Snapdragon: ");
        g2.setColor(Color.ORANGE);
        g2.drawString(formatter.format(snapdragon * 2), x + width, y);
        width = metrics.stringWidth("Snapdragon: " + formatter.format(snapdragon * 2));
        g2.drawString(" (" + formatter.format(getPerHour(snapdragon * 2)) + "/hr)", x + width, y);
        g2.setColor(Color.WHITE);

        g2.drawString("Profit: ", x, y += 15);//5295
        width = metrics.stringWidth("Profit: ");
        g2.setColor(Color.GREEN);
        g2.drawString(formatter.format(snapdragon * 2 * snapPrice + ranarr * 2 * ranarrPrice + torstol * 2 * torstolPrice), x + width, y);
        width = metrics.stringWidth("Profit: " + formatter.format(snapdragon * 2 * snapPrice + ranarr * 2 * ranarrPrice));
        g2.drawString(" (" + formatter.format(getPerHour(snapdragon * 2 * snapPrice + ranarr * 2 * ranarrPrice)) + "/hr)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Stunner: ", x, y += 15);
        width = metrics.stringWidth("Stunner: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(fail) + "/" + formatter.format(steal + fail), x + width, y);
        width = metrics.stringWidth("Stunner: " + formatter.format(fail) + "/" + formatter.format(steal + fail));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(thieveRate) + "% success)", x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Elapsed Time: ", x, y += 15);
        width = metrics.stringWidth("Elapsed Time: ");
        g2.setColor(Color.PINK);
        g2.drawString(formatTime(System.currentTimeMillis() - startTime) + " Hop" + (hops > 0 ? " " + hops : "") + ": " + formatter.format((hopTimer - System.currentTimeMillis()) / 1000.0), x + width, y);

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
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if ((m.contains("you pick the") && (m.endsWith("pocket.")))) {
                steal++;
            }
            if (m.contains("you fail to pick")) {
                fail++;
            }
            if (m.contains("you steal")) {
                seeds += Integer.parseInt(m.substring(10, 11));
            }
            if (m.contains("ranarr")) {
                ranarr += Integer.parseInt(m.substring(10, 11));
            }
            if (m.contains("snapdragon")) {
                snapdragon += Integer.parseInt(m.substring(10, 11));
            }
            if (m.contains("torstol")) {
                torstol += Integer.parseInt(m.substring(10, 11));
            }
        }
    }

    @Override
    public void onStop() {
        int lvls = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl);
        Log.info("Hope you had fun stealing " + (snapdragon * 2 * snapPrice + ranarr * 2 * ranarrPrice + torstol * 2 * torstolPrice) + "gp of seeds! Gained " + (Skills.getExperience(Skill.THIEVING) - startThieveXP) + " thieving experience and " + (lvls > 0 ? "+" + lvls : "0") + " levels.");
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

    private void dropSeeds() {
        final Set<String> DROP_SEEDS = new HashSet<>(Arrays.asList("Ranarr seed", "Snapdragon seed", "Torstol seed", "Lantadyme seed", "Cadantine seed", "Avantoe seed", "Dwarf weed seed", "Snape grass seed", "Dodgy necklace", "Jug of wine"));
        for (Item drop : Inventory.getItems(item -> !DROP_SEEDS.contains(item.getName()))) {
            if (Inventory.getItemAt(drop.getIndex()) != null) {
                Inventory.getItemAt(drop.getIndex()).interact("Drop");
                Time.sleep(random(200, 300));
            }
        }
    }

    private int getExchangeInfo(int id) {
        int price = 0, multiplier = 1;

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
                if (today[0].contains("k")) {
                    today[0] = today[0].replaceAll("k", "");
                    multiplier = 1000;
                }
                price = (int) (Double.parseDouble(today[0]) * multiplier);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        Log.info("Price of " + id + " is " + price);
        return price;
    }
}