package schema;

import java.io.Serializable;

public class UdpPacket implements Serializable {

    private int operation;

    private UdpBody body;

    public UdpPacket(int operation, UdpBody body) {
        this.operation = operation;
        this.body = body;
    }
    public int getOperation() {
        return operation;
    }

    public UdpBody getBody() {
        return body;
    }
}
