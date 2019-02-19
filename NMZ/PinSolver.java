import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.ui.Log;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

class PinSolver {

    void enterPin(int pin) {
        HashMap<Integer, InterfaceComponent> pinCombination = new HashMap<>();
        char[] combination = String.valueOf(pin).toCharArray();
        int index = 0;
        int[] pinVals = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

        if (Interfaces.isOpen(213)) {

            for (int i = 16; i < 35; i += 2) {
                if (Interfaces.getComponent(213, i, 1) != null) {
                    InterfaceComponent j = Interfaces.getComponent(213, i, 1);
                    InterfaceComponent actionJ = Interfaces.getComponent(213, i, 0);
                    if (!j.getText().equals("")) {
                        pinCombination.put(Integer.parseInt(j.getText()), actionJ);
                        pinVals[index] = Integer.parseInt(j.getText());
                        index++;
                    } else {
                        pinCombination.put(-1, actionJ);
                        pinVals[index] = -1;
                        index++;
                    }
                }
            }
            int replace = getMissingNo(pinVals);
            //replace missing val
            if (pinCombination.containsKey(-1)) {
                Log.info("Changing -1 to " + replace);
                pinCombination.get(-1);
                pinCombination.put(replace, pinCombination.get(-1));
            }

            if (Interfaces.getComponent(213, 3).getText().equals("?")) {
                Log.info("Entering stage 1");
                if (pinCombination.containsKey(Integer.parseInt(String.valueOf(combination[0])))) {
                    Log.info("Entering " + combination[0]);
                    if (pinCombination.get(Integer.parseInt(String.valueOf(combination[0]))).interact("Select")) {
                        Time.sleepUntil(() -> Interfaces.getComponent(213, 3).getText().equals("*"), random(300, 500), 5000);
                    }
                    enterPin(pin);
                }
            } else if (Interfaces.getComponent(213, 4).getText().equals("?")) {
                Log.info("Entering stage 2");
                if (pinCombination.containsKey(Integer.parseInt(String.valueOf(combination[1])))) {
                    Log.info("Entering " + combination[1]);
                    if (pinCombination.get(Integer.parseInt(String.valueOf(combination[1]))).interact("Select")) {
                        Time.sleepUntil(() -> Interfaces.getComponent(213, 4).getText().equals("*"), random(300, 500), 5000);
                    }
                    enterPin(pin);
                }
            } else if (Interfaces.getComponent(213, 5).getText().equals("?")) {
                Log.info("Entering stage 3");
                if (pinCombination.containsKey(Integer.parseInt(String.valueOf(combination[2])))) {
                    Log.info("Entering " + combination[2]);
                    if (pinCombination.get(Integer.parseInt(String.valueOf(combination[2]))).interact("Select")) {
                        Time.sleepUntil(() -> Interfaces.getComponent(213, 5).getText().equals("*"), random(300, 500), 5000);
                    }
                    enterPin(pin);
                }
            } else if (Interfaces.getComponent(213, 6).getText().equals("?")) {
                Log.info("Entering stage 4");
                if (pinCombination.containsKey(Integer.parseInt(String.valueOf(combination[3])))) {
                    Log.info("Entering " + combination[3]);
                    if (pinCombination.get(Integer.parseInt(String.valueOf(combination[3]))).interact("Select")) {
                        Time.sleepUntil(() -> Interfaces.getComponent(213, 6).getText().equals("*"), random(300, 500), 5000);
                    }
                }
            }
        }
    }

    private int getMissingNo(int[] pins) {
        ArrayList<Integer> missing = new ArrayList<>();
        Arrays.sort(pins);

        for (int i = 1; i < pins.length; i++) {
            for (int j = pins[i - 1] + 1; j < pins[i]; j++) {
                missing.add(j);
            }
        }
        for (int i = pins[pins.length - 1] + 1; i <= pins[pins.length - 1]; i++) {
            if (i > -1)
                missing.add(i);
        }
        return missing.size() > 0 ? missing.get(0) : -1;
    }

    private int random(int min, int max) {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(max - min + 1) + min);
    }
}
