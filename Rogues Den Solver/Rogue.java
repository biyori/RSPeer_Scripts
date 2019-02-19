import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.Pickable;
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
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Pickables;
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
import java.util.List;

@ScriptMeta(name = "Rogues Den Minigame", desc = "Tries to farm Rogue pieces", developer = "Koko", version = 1.0, category = ScriptCategory.MINIGAME)
public class Rogue extends Script implements RenderListener, ChatMessageListener {

    private boolean takeScreenie = false, completedMaze = false;
    private String status;
    private long startTime, lastAnim;
    private int startThieveXP, startAgilityXP, startThieveLvl, startAgilitylvl, maze = 0, rogueCrates = 0;
    private RandomHandler randomEvent = new RandomHandler();
    private int lastAnimDelay = random(8000, 12000);

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        startThieveXP = Skills.getExperience(Skill.THIEVING);
        startThieveLvl = Skills.getLevelAt(startThieveXP);
        startAgilityXP = Skills.getExperience(Skill.AGILITY);
        startAgilitylvl = Skills.getLevelAt(startAgilityXP);

        status = "Loading up!";
    }

    @Override
    public int loop() {
        Player Local = Players.getLocal();

        if (Local.isAnimating() || Local.isMoving())
            lastAnim = System.currentTimeMillis();

        long msSinceAnim = System.currentTimeMillis() - lastAnim;

        randomEvent.checkLamp();
        if (randomEvent.findNearbyRandoms()) {
            Time.sleep(1000);
        }
        if (randomEvent.doWeHaveRandom()) {
            takeScreenie = true;
            randomEvent.handleRandom();
        } else if (!Inventory.contains("Mystic jewel") && Movement.getRunEnergy() < 85 && Inventory.contains(item -> item.getName().startsWith("Super energy"))) {
            status = "Restoring energy";
            if (Inventory.contains(item -> item.getName().startsWith("Super energy"))) {
                Inventory.getFirst(item -> item.getName().startsWith("Super energy")).interact("Drink");
                Time.sleepUntil(Local::isAnimating, 500, 5000);
                Time.sleep(random(400, 600));
            }
        } else if (!Inventory.contains("Mystic jewel") && (!Inventory.isEmpty() || Movement.getRunEnergy() < 85)) {
            status = "Banking and restoring energy";
            if (!Bank.isOpen()) {
                if (SceneObjects.getNearest("Bank chest") != null) {
                    SceneObjects.getNearest("Bank chest").interact("Use");
                    Time.sleepUntil(() -> Bank.isOpen() || Interfaces.isOpen(213), 500, 5000);
                } else {
                    Area bankArea = Area.rectangular(3043, 4974, 3047, 4969, 1);
                    Movement.walkTo(bankArea.getTiles().get(random(0, bankArea.getTiles().size() - 1)));
                    Time.sleepUntil(() -> bankArea.contains(Local), 200, 5000);
                }
            }
            if (Interfaces.isOpen(213)) {
                status = "Waiting for Bank PIN entry";
            }
            if (Bank.isOpen()) {
                if (Movement.getRunEnergy() < 85) {
                    Bank.withdraw(item -> item.getName().startsWith("Super energy"), 1);
                    Time.sleepUntil(() -> Inventory.contains(item -> item.getName().startsWith("Super energy")), 200, 5000);
                    Bank.close();
                    Time.sleep(random(300, 400));
                } else {
                    Bank.depositInventory();
                    Time.sleepUntil(Inventory::isEmpty, 500, 5000);
                    Bank.close();
                    Time.sleep(random(300, 400));

                    if (rogueCrates == 5) {
                        Log.info("We have 5 crates! Stopping now");
                        setStopping(true);
                    }
                }
            }
        } else if (!Inventory.contains("Mystic jewel")) {
            status = "Entering minigame";
            completedMaze = false;
            SceneObject door = SceneObjects.getNearest(7256);//Doorway
            if (door != null) {
                door.interact("Open");
                Time.sleepUntil(() -> Inventory.contains("Mystic jewel"), 500, 5000);
            }
        } else if (Local.getPosition().equals(new Position(3056, 4992, 1)) || Local.getPosition().equals(new Position(3056, 4993, 1))) {
            forceRun();

            if (!Local.getPosition().equals(new Position(3056, 4993, 1))) {
                traverse(new Position(3056, 4993, 1));
                Time.sleep(random(400, 700));
            } else {
                traverse(new Position(3056, 4995, 1));
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3056, 4995, 1)) || Local.getPosition().equals(new Position(3050, 4997, 1))) {
            SceneObject contortBars = SceneObjects.getNearest(7251);
            if (contortBars != null) {
                contortBars.interact("Enter");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3048, 4997, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3048, 4997, 1))) {
            status = "Trying tricky walk";
            List<Position> walr = Area.rectangular(3040, 4999, 3040, 4997, 1).getTiles();
            Position dis = walr.get(random(0, walr.size() - 1));

            Log.info("Trying to walk to " + dis + " Is it walkable? " + dis.isPositionWalkable());

            //Bad path
            if (dis.equals(new Position(3040, 4998, 1)))
                return 100;

            Movement.walkTo(dis);
            // Movement.walkTo(dis);
            Time.sleepUntil(() -> Local.getAnimation() == 1115, 500, 5000);
            Time.sleep(random(1500, 2000));
        } else if (Area.rectangular(3038, 4999, 3040, 4997, 1).contains(Local)) {
            status = "Crossing 3";
            traverse(new Position(3037, 4999, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3037, 4999, 1))) {
            status = "Crossing 3 now!";
            traverse(new Position(3033, 4999, 1));
            Time.sleep(random(200, 400));
        } else if (Local.getPosition().equals(new Position(3033, 4999, 1))) {
            status = "Moving up 1 tile";
            traverse(new Position(3032, 4999, 1));
            Time.sleep(random(200, 400));
        } else if (Local.getPosition().equals(new Position(3032, 4999, 1))) {
            status = "Finishing cross 3!";
            traverse(new Position(3027, 4999, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3027, 4999, 1))) {
            SceneObject grill = SceneObjects.getNearest(7255);
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3023, 5001, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3023, 5001, 1))) {
            status = "Running to diagANAL";
            traverse(new Position(3014, 5003, 1));
            Time.sleep(random(300, 500));
        } else if (Local.getPosition().equals(new Position(3014, 5003, 1))) {
            traverse(new Position(3012, 5001, 1));
            Time.sleep(random(300, 500));
        } else if (Local.getPosition().equals(new Position(3012, 5001, 1))) {
            traverse(new Position(3009, 5003, 1));
            Time.sleep(random(300, 500));
        } else if (Local.getPosition().equals(new Position(3009, 5003, 1))) {
            status = "Getting on edge";
            forceWalk();

            SceneObject ledge = SceneObjects.getNearest(7240);
            if (ledge != null) {
                ledge.interact("Climb");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2988, 5004, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2988, 5004, 1))) {
            status = "Getting to blades!";
            Area bladez = Area.rectangular(2970, 5019, 2972, 5016, 1);
            for (int i = 0; i < 5; i++) {
                Log.info("Loop " + i);
                Movement.walkTo(bladez.getTiles().get(random(0, bladez.getTiles().size() - 1)));
                Time.sleep(random(2000, 3000));
                if (bladez.contains(Local))
                    break;
            }
        } else if (Area.rectangular(2970, 5019, 2972, 5016, 1).contains(Local)) {
            status = "Crossing blades";
            //TODO: LOOKS BOTLIKE
            List<Position> bladez = Area.rectangular(2969, 5019, 2969, 5016, 1).getTiles();
            Position dis = bladez.get(random(0, bladez.size() - 1));
            Log.info("Trying to walk to " + dis + " Is it walkable? " + dis.isPositionWalkable());
            Movement.walkTo(dis);
            Time.sleepUntil(() -> Area.rectangular(2967, 5019, 2967, 5016, 1).contains(Local), 500, 5000);
            // Time.sleep(random(400, 700));
        } else if (Area.rectangular(2967, 5019, 2967, 5016, 1).contains(Local)) {
            status = "Crossing pendulums";
            List<Position> pendulumz = Area.rectangular(2958, 5026, 2964, 5025, 1).getTiles();
            Position dis = pendulumz.get(random(0, pendulumz.size() - 1));
            if (dis.getX() == 2963 || dis.getX() == 2960)
                return 100;

            Log.info("Trying to walk to " + dis + " Is it walkable? " + dis.isPositionWalkable());
            Movement.walkTo(dis);
            Time.sleepUntil(() -> Area.rectangular(2964, 5028, 2954, 5028, 1).contains(Local), 500, 5000);
            Time.sleep(random(400, 700));
        } else if (Area.rectangular(2964, 5028, 2954, 5028, 1).contains(Local)) {
            status = "Crossing ledge";
            SceneObject bridge = SceneObjects.getNearest(7239);
            if (bridge != null) {
                bridge.interact("Climb");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2958, 5035, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2958, 5035, 1))) {
            status = "Walking closer to trap";
            forceRun();
            for (int i = 0; i < 5; i++) {
                Log.info("Loop " + i);
                Movement.walkTo(new Position(2963, 5050, 1));
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2963, 5050, 1)), 500, random(2000, 3000));
                if (Local.getPosition().equals(new Position(2963, 5050, 1)))
                    break;
            }
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2963, 5050, 1))) {
            status = "Disabling trap";
            SceneObject trap = SceneObjects.getNearest(7227);
            if (trap != null) {
                trap.interact("Search");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2963, 5051, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2963, 5051, 1))) {
            status = "Moving plus 3! (NEW SPEED)";
            traverse(new Position(2963, 5055, 1));
            Time.sleep(random(150, 200));
        } else if (Local.getPosition().equals(new Position(2963, 5055, 1))) {
            status = "Entering passageway";
            forceWalk();
            SceneObject passage = SceneObjects.getNearest(7219);
            if (passage != null) {
                passage.interact("Enter");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2957, 5072, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2957, 5072, 1))) {
            status = "Crossing blade";
            Movement.walkTo(new Position(2957, 5074, 1));
            Time.sleepUntil(() -> Local.getPosition().equals(new Position(2957, 5076, 1)), 500, 5000);
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2957, 5076, 1))) {
            status = "Getting to next passageway";
            Area beforePassage = Area.rectangular(2954, 5091, 2955, 5089, 1);
            Movement.walkTo(beforePassage.getTiles().get(random(0, beforePassage.getTiles().size() - 1)));
            Time.sleep(random(400, 700));
        } else if (Area.rectangular(2954, 5091, 2955, 5089, 1).contains(Local)) {
            status = "Entering passageway";
            SceneObject passage = SceneObjects.getNearest(7219);
            if (passage != null) {
                passage.interact("Enter");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2955, 5098, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2955, 5098, 1))) {
            status = "Getting to next passageway (LAG?)";
            Area beforePassage = Area.rectangular(2970, 5100, 2971, 5098, 1);
            Movement.walkTo(beforePassage.getTiles().get(random(0, beforePassage.getTiles().size() - 1)));
            Time.sleep(random(400, 700));
        } else if (Area.rectangular(2970, 5100, 2971, 5098, 1).contains(Local)) {
            status = "Entering passageway";
            SceneObject passage = SceneObjects.getNearest(7219);
            if (passage != null) {
                passage.interact("Enter");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2972, 5094, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2972, 5094, 1))) {
            status = "Opening grill";
            SceneObject grl = SceneObjects.getNearest(7255);
            if (grl != null) {
                grl.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2972, 5093, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2972, 5093, 1))) {
            status = "Running away from spinning blades";
            forceRun();
            Area awayFromBlade = Area.rectangular(2972, 5087, 2976, 5086, 1);
            Movement.walkTo(awayFromBlade.getTiles().get(random(0, awayFromBlade.getTiles().size() - 1)));
            Time.sleep(random(400, 700));
        } else if (Area.rectangular(2972, 5087, 2976, 5086, 1).contains(Local)) {
            SceneObject ledge = SceneObjects.getNearest(s -> s.getId() == 7240 && s.getPosition().getY() == 5087);
            if (ledge != null) {
                ledge.interact("Climb");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2991, 5087, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2991, 5087, 1))) {
            status = "Disabling trap";
            SceneObject trap = SceneObjects.getNearest(trp -> trp.getId() == 7249 && trp.getPosition().equals(new Position(2993, 5087, 1)));
            if (trap != null) {
                Time.sleep(random(300, 400));//TODO: FIXED BROKEN BIT?
                trap.interact("Search");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(2993, 5088, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(2993, 5088, 1))) {
            status = "Running to blade";
            traverse(new Position(3000, 5087, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3000, 5087, 1))) {
            status = "Crossing blade";
            Movement.walkTo(new Position(3001, 5087, 1));
            Time.sleepUntil(() -> Local.getPosition().equals(new Position(3003, 5087, 1)), 500, 8000);
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3003, 5087, 1))) {
            status = "Picking up tile";
            Area tileRoom = Area.rectangular(3019, 5084, 3015, 5082, 1);
            Movement.walkTo(tileRoom.getTiles().get(random(0, tileRoom.getTiles().size() - 1)));
            Time.sleep(random(400, 700));
        } else if (Area.rectangular(3018, 5085, 3015, 5082, 1).contains(Local)) {
            status = "Picking up tile";
            Pickable tile = Pickables.getNearest(5568);
            if (tile != null) {
                tile.interact("Take");
                Time.sleepUntil(() -> Inventory.contains(5568), 500, 8000);
            }
            Time.sleep(random(400, 700));
        } else if (Inventory.contains(5568)) {
            status = "Putting tile into door";
            SceneObject door = SceneObjects.getNearest(7234);
            if (door != null && !Interfaces.isOpen(293)) {
                door.interact("Open");
                Time.sleepUntil(() -> Interfaces.isOpen(293), 500, 8000);
            }
            if (Interfaces.isOpen(293)) {
                InterfaceComponent key = Interfaces.getComponent(293, 3);
                if (key != null) {
                    Time.sleep(random(300, 700));
                    key.interact("Ok");
                    Time.sleepUntil(() -> Local.getPosition().equals(new Position(3024, 5082, 1)), 500, 8000);
                }
            }
        } else if (Local.getPosition().equals(new Position(3024, 5082, 1))) {
            status = "Time for a maze!";
            //forceWalk();
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3030, 5079, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3031, 5079, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3031, 5079, 1))) {
            status = "Maze 1";//TODO: Make maze more human like
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3032, 5078, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3032, 5077, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3032, 5077, 1))) {
            status = "Maze 2";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3036, 5076, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3037, 5076, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3037, 5076, 1))) {
            status = "Maze 3";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3039, 5079, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3040, 5079, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3040, 5079, 1))) {
            status = "Maze 4";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3042, 5076, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3043, 5076, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3043, 5076, 1))) {
            status = "Maze 5";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3044, 5069, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3044, 5068, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3044, 5068, 1))) {
            status = "Maze 6";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3041, 5068, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3041, 5069, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3041, 5069, 1))) {
            status = "Maze 7";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3040, 5070, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3039, 5070, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3039, 5070, 1))) {
            status = "Maze 9";
            SceneObject grill = SceneObjects.getNearest(id -> id.getId() == 7255 && id.getPosition().equals(new Position(3038, 5069, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3038, 5068, 1)), 500, 8000);
                Time.sleep(random(200, 400));
            }
        } else if (Local.getPosition().equals(new Position(3038, 5068, 1))) {
            status = "Finished maze, moving on!";
            Area toNextTrap = Area.rectangular(3038, 5039, 3040, 5034, 1);
            for (int i = 0; i < 20; i++) {
                Movement.walkTo(toNextTrap.getTiles().get(random(0, toNextTrap.getTiles().size() - 1)));
                Time.sleepUntil(() -> toNextTrap.contains(Local), 200, random(3000, 4000));
                if (toNextTrap.contains(Local))
                    break;
            }
        } else if (Area.rectangular(3038, 5039, 3040, 5034, 1).contains(Local)) {
            status = "Disabling trap";
            forceRun();
            SceneObject trap = SceneObjects.getNearest(trp -> trp.getId() == 7249 && trp.getPosition().equals(new Position(3027, 5032, 1)));
            if (trap != null) {
                trap.interact("Search");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3027, 5033, 1)), 500, 5000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3027, 5033, 1))) {
            status = "Opening grill";
            SceneObject grill = SceneObjects.getNearest(d -> d.getId() == 7255 && d.getPosition().equals(new Position(3015, 5033, 1)));
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3014, 5033, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3014, 5033, 1))) {
            status = "Skipping 3 traps";
            traverse(new Position(3010, 5033, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3010, 5033, 1))) {
            status = "Opening grill";
            SceneObject grill = SceneObjects.getNearest(7255);
            if (grill != null) {
                grill.interact("Open");
                Time.sleepUntil(() -> Local.getPosition().equals(new Position(3009, 5033, 1)), 500, 8000);
                Time.sleep(random(400, 700));
            }
        } else if (Local.getPosition().equals(new Position(3009, 5033, 1))) {
            status = "Getting closer to traps";
            traverse(new Position(3008, 5033, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3008, 5033, 1))) {
            status = "Skipping 3 traps";
            traverse(new Position(3004, 5033, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3004, 5033, 1))) {
            status = "Passing pendulum";
            Movement.walkTo(new Position(3003, 5034, 1));
            Time.sleepUntil(() -> Local.getPosition().equals(new Position(3000, 5034, 1)), 500, 8000);
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(3000, 5034, 1))) {
            status = "Walking closer to traps";
            Area nearTrap = Area.rectangular(2995, 5044, 2998, 5042, 1);
            Movement.walkTo(nearTrap.getTiles().get(random(0, nearTrap.getTiles().size() - 1)));
            Time.sleepUntil(() -> nearTrap.contains(Local), 500, 8000);
           // Time.sleep(random(200, 500));//TODO FIX BOT LIKE ACTIVITY HERE
        } else if (Area.rectangular(2995, 5044, 2998, 5042, 1).contains(Local)) {
            status = "Walking even closer!";
            traverse(new Position(2992, 5045, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2992, 5045, 1))) {
            status = "Passing through 3";
            traverse(new Position(2992, 5049, 1));
            Time.sleep(random(300, 500));
        } else if (Local.getPosition().equals(new Position(2992, 5049, 1))) {
            status = "Running to next trap";
            Area newTrap = Area.rectangular(2991, 5064, 2992, 5059, 1);
            Movement.walkTo(newTrap.getTiles().get(random(0, newTrap.getTiles().size() - 1)));
            Time.sleepUntil(() -> newTrap.contains(Local), 500, 8000);
            Time.sleep(random(200, 500));
        } else if (Area.rectangular(2991, 5066, 2992, 5059, 1).contains(Local)) {
            status = "Getting into position";
            traverse(new Position(2992, 5067, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2992, 5067, 1))) {
            status = "Skipping 3 traps";
            traverse(new Position(2992, 5071, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2992, 5071, 1))) {
            status = "Skipping 3 traps";
            traverse(new Position(2992, 5075, 1));
            Time.sleep(random(400, 700));
        } else if (Local.getPosition().equals(new Position(2992, 5075, 1))) {
            status = "Running to stun room";
            Area stunner = Area.rectangular(3000, 5068, 3006, 5066, 1);
            Movement.walkTo(stunner.getTiles().get(random(0, stunner.getTiles().size() - 1)));
            Time.sleepUntil(() -> stunner.contains(Local), 500, 8000);
            Time.sleep(random(200, 500));
        } else if (Area.rectangular(3000, 5068, 3006, 5066, 1).contains(Local)) {
            status = "Picking up powder";
            Pickable stunner = Pickables.getNearest(stun -> stun.getId() == 5559 && stun.getPosition().getY() == 5063);
            if (stunner != null) {
                stunner.interact("Take");
                Time.sleepUntil(() -> Inventory.contains(5559), 500, 8000);
                Time.sleep(random(200, 500));
            }
        } else if (Inventory.contains(5559) && Inventory.getCount(true, 5559) == 5) {
            status = "Stunning guard";
            if (!Inventory.isItemSelected()) {
                Inventory.getFirst(5559).interact("Use");
                Time.sleepUntil(Inventory::isItemSelected, 300, 8000);
            }
            if (Inventory.isItemSelected()) {
                Npc guard = Npcs.getNearest(3191);
                if (guard != null) {
                    guard.interact("Use");
                    Time.sleepUntil(Local::isAnimating, 500, 8000);
                }
            }
        } else if (Inventory.contains(5559) && Inventory.getCount(true, 5559) == 4 && !completedMaze) {
            status = "Running past guard";
            Area safeZone = Area.rectangular(3028, 5057, 3030, 5055, 1);
            Movement.walkTo(safeZone.getTiles().get(random(0, safeZone.getTiles().size() - 1)));
            Time.sleepUntil(() -> safeZone.contains(Local), 500, random(1000, 2000));
            Time.sleep(random(200, 500));
            if (safeZone.contains(Local)) {
                completedMaze = true;
            }
        } else if (completedMaze && Area.rectangular(3028, 5057, 3030, 5055, 1).contains(Local)) {
            status = "Getting through pendulum";
            Movement.walkTo(new Position(3028, 5054, 1));
            Time.sleepUntil(() -> Local.getPosition().equals(new Position(3028, 5051, 1)), 500, 8000);
            Time.sleep(random(200, 500));
        } else if (Local.getPosition().equals(new Position(3028, 5051, 1))) {
            status = "Getting through last pendulum";
            Movement.walkTo(new Position(3028, 5050, 1));
            Time.sleepUntil(() -> Local.getPosition().equals(new Position(3028, 5047, 1)), 500, 8000);
            Time.sleep(random(200, 500));
        } else if (Local.getPosition().equals(new Position(3028, 5047, 1))) {
            SceneObject[] safe = SceneObjects.getLoaded(d -> d.getId() == 7237);
            Log.info("There is currently " + safe.length + " safes available for cracking!");
            SceneObject crack = safe[random(0, safe.length - 1)];
            Log.info("Cracking safe at " + crack.getPosition());
            crack.interact("Crack");
            Time.sleepUntil(Dialog::isViewingChat, 500, 8000);
            Time.sleep(random(200, 500));
        } else {
            if (!Local.isAnimating() && msSinceAnim > lastAnimDelay) {
                Log.info("WE ARE STUCK!");
                takeScreenie = true;
                setStopping(true);
            }
        }
        return random(200, 300);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        int nextThieveLvl = Skills.getExperienceToNextLevel(Skill.THIEVING);
        int nextAgilityLvl = Skills.getExperienceToNextLevel(Skill.AGILITY);
        int gainedThieveXP = Skills.getExperience(Skill.THIEVING) - startThieveXP;
        int gainedAgilityXP = Skills.getExperience(Skill.AGILITY) - startAgilityXP;
        double ttl = (nextThieveLvl / (getPerHour(gainedThieveXP) / 60.0 / 60.0 / 1000.0));
        double ttl2 = (nextAgilityLvl / (getPerHour(gainedAgilityXP) / 60.0 / 60.0 / 1000.0));
        if (gainedAgilityXP == 0) {
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

        String thieveLvlGained = (Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) - startThieveLvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.HUNTER)) - startThieveLvl) + ")" : "";
        String agilityLvlGained = (Skills.getLevelAt(Skills.getExperience(Skill.AGILITY)) - startAgilitylvl) > 0 ? " (+" + (Skills.getLevelAt(Skills.getExperience(Skill.MAGIC)) - startAgilitylvl) + ")" : "";
        String crateString = rogueCrates == 0 ? "None" : String.valueOf(rogueCrates);

        g2.setColor(Color.WHITE);
        g2.drawString("Rogue crates: ", x, y);
        int width = metrics.stringWidth("Rogue crates: ");
        g2.setColor(Color.GREEN);
        g2.drawString(crateString, x + width, y);

        g2.setColor(Color.WHITE);
        g2.drawString("Laps: ", x, y += 15);
        width = metrics.stringWidth("Laps: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(maze), x + width, y);
        width = metrics.stringWidth("Laps: " + formatter.format(maze));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(maze)) + "/hr)", x + width, y);

        g2.drawString("Agility lvl: ", x, y += 15);
        g2.setColor(Color.YELLOW);
        width = metrics.stringWidth("Agility lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.AGILITY)) + agilityLvlGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Agility lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.AGILITY)) + agilityLvlGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl2).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedAgilityXP), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedAgilityXP));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedAgilityXP)) + "/hr)", x + width, y);

        g2.drawString("Thieving lvl: ", x, y += 15);
        g2.setColor(Color.ORANGE);
        width = metrics.stringWidth("Thieving lvl: ");
        g2.drawString(Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + thieveLvlGained, x + width, y);
        g2.setColor(Color.WHITE);
        width = metrics.stringWidth("Thieving lvl: " + Skills.getLevelAt(Skills.getExperience(Skill.THIEVING)) + thieveLvlGained);
        g2.drawString(" (TTL: " + formatTime(Double.valueOf(ttl).longValue()) + ")", x + width, y);

        g2.drawString("XP Gained: ", x, y += 15);
        width = metrics.stringWidth("XP Gained: ");
        g2.setColor(Color.YELLOW);
        g2.drawString(formatter.format(gainedThieveXP), x + width, y);
        width = metrics.stringWidth("XP Gained: " + formatter.format(gainedThieveXP));
        g2.setColor(Color.WHITE);
        g2.drawString(" (" + formatter.format(getPerHour(gainedThieveXP)) + "/hr)", x + width, y);

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
        if (event.getType().equals(ChatMessageType.FILTERED)) {
            if (m.endsWith("cracking the safe."))
                maze++;
            if (m.endsWith("crate of equipment."))
                rogueCrates++;
        }
    }

    @Override
    public void onStop() {
        Log.info("Hope you had fun at Rogues Den! Gained " + (Skills.getExperience(Skill.THIEVING) - startThieveXP) + " thieving experience and " + (Skills.getExperience(Skill.AGILITY) - startAgilityXP) + " agility experience this session. We also completed the maze " + maze + "x");
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }

    private void traverse(Position pos) {
        Movement.walkTo(pos);
        Time.sleepUntil(() -> Players.getLocal().getPosition().equals(pos), 500, 5000);
    }


    private void TakeScreenshot(Image img) {
        Log.info("Attempting to take a screenshot");
        BufferedImage buffered = (BufferedImage) img;

        //Get the current date to save in the screenshot folder
        long now = Instant.now().getEpochSecond();

        try {
            //Create a folder with my forum name to tell the user which script created this folder
            if (!new File(getDataDirectory() + "\\Koko\\Rogue\\Screenshots").exists()) {
                new File(getDataDirectory() + "\\Koko\\Rogue\\Screenshots").mkdirs();
            }
            //save the image to the folder and rename all player spaces with underscores
            try {
                if (ImageIO.write(
                        buffered,
                        "png",
                        new File(getDataDirectory() + "\\Koko\\Rogue\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png"))) {
                    Log.info("Saved " + getDataDirectory() + "\\Koko\\Rogue\\Screenshots\\" + now + "-" + Players.getLocal().getName().replaceAll("\\u00a0", "_") + ".png");
                }
            } catch (IOException e) {
                Log.info("Error! " + e.getMessage());
            }
        } catch (Exception e) {
            Log.info("Error! " + e.getMessage());
        }
    }

    private void forceRun() {
        if (!Movement.isRunEnabled())
            Movement.toggleRun(true);
    }

    private void forceWalk() {
        if (Movement.isRunEnabled())
            Movement.toggleRun(false);
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