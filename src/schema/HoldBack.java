package schema;

import java.util.HashMap;

public class HoldBack {
    private static HoldBack instance;
    private int currentSequenceNumber = 1;

    private HashMap<Integer, String> holdBack = new HashMap<>();

    private HoldBack() { }

    public static HoldBack getInstance() {
        if (instance == null)
            instance = new HoldBack();
        return instance;
    }

    public void incrementLastSequence() {
    	currentSequenceNumber++;
    }

    public boolean isThereIsNext() {
        return (this.holdBack.containsKey(currentSequenceNumber));
    }

    public void addToQueue(int sequence, String content) {
        this.holdBack.put(sequence, content);
    }

    public void removeFromQueue() {
        this.holdBack.remove(currentSequenceNumber);
    }

    public String getNextRequest() {
        return this.holdBack.get(currentSequenceNumber);
    }
}
