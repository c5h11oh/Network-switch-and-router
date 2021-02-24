package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.*;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
	}

	/*
	 * So apparently we would need a forward table with a timeout function
	 */
	HashMap<MACAddress, ForwardTableEntry> ForwardTable;

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/* TODO: Handle packets                                             
		 * 
		 * 1. Add/refresh the forward table
		 * 2. Lookup the forward table for the designated MAC address.
		 * 	2.1 If found, check if it is valid. (timeout = 15 sec)
		 *		2.1.1 Valid: send the frame to such interface and RETURN
		 * 		2.1.2 過期: delete this entry in ForwardTable and continue
		 *  2.2 If not found, continue
		 * 3. Broadcast the frame to all the interface except the origin. RETURN
		 */
		
		// 1.
		ForwardTable.put(
			etherPacket.getSourceMAC(), 
			new ForwardTableEntry(inIface, System.currentTimeMillis())
		);
		// 2.
		MACAddress destKey = etherPacket.getDestinationMAC();
		if(ForwardTable.containsKey(destKey)){ // 2.1
			ForwardTableEntry entry = ForwardTable.get(destKey);
			long durSecond = ( System.currentTimeMillis() - entry.getCreatedAt() );
			if(durSecond < 15000){ // 2.1.1
				this.sendPacket(etherPacket, entry.getIface());
				return;
			}
			else{ // 2.1.2
				ForwardTable.remove(destKey);
				// Go on. Do not return yet.
			}
		}
		
		// 3. (fall from 2.2 or 2.1.2)
		for(Map.Entry<String, Iface> e : this.interfaces.entrySet()){
			if(e.getValue().equals(inIface)) continue;
			this.sendPacket(etherPacket, e.getValue());
		}
		return;
	}
}
