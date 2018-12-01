package schema;

import java.util.HashMap;

public class HoldBack {
    private static HoldBack instance;
    private int lastSequence = 1;

    private HashMap<Integer, String> holdBack = new HashMap<>();

    private HoldBack() { }

    public static HoldBack getInstance() {
        if (instance == null)
            instance = new HoldBack();
        return instance;
    }

    public void incrementLastSequence() {
        lastSequence++;
    }

    public boolean isThereIsNext() {
        return (this.holdBack.containsKey(lastSequence));
    }

    public void addToQueue(int sequence, String content) {
        this.holdBack.put(sequence, content);
    }

    public void removeFromQueue() {
        this.holdBack.remove(lastSequence);
    }

    public String getNextRequest() {
        return this.holdBack.get(lastSequence);
    }
}
