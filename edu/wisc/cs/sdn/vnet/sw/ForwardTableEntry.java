import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Iface;

public class ForwardTableEntry {
    // MACAddress macAddress;
    Iface iface;
    long createdAt;
    
    public ForwardTableEntry(Iface iface, long createdAt) {
        this.iface = iface;
        this.createdAt = System.currentTimeMillis();
    }

    public Iface getIface(){
        return this.iface;
    }

    public long getCreatedAt(){
        return this.createdAt;
    }
}
