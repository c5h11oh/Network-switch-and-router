package edu.wisc.cs.sdn.vnet.rt;

import java.nio.ByteBuffer;
import java.util.*;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/* TODO: Handle packets                                             */
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
			return;
		
		
		IPv4 packet = (IPv4)etherPacket.getPayload();
		
		// Checksum
		int hLen = packet.getHeaderLength();
		byte[] b = packet.serialize();
		ByteBuffer bb = ByteBuffer.wrap(b);
		// short lengthInShort = (short)(hLen*2);
		// int sum = 0;
		// for(int i = 0; i < 5; ++i)
		// 	sum += (int)bb.getShort(i);
		// // The (i == 5)-th short is Checksum
		// for(int i = 6; i < lengthInShort; ++i)
		// 	sum += (int)bb.getShort(i);
		// sum = ~sum & 0xFFFF;
		
		// if((short)sum != packet.getChecksum()){
		//  String s = String.format("sum = %x, checksum = %x",(short)sum, packet.getChecksum());
		//  System.out.println(s);
		// 	return;
		// }

		bb.rewind();
        int accumulation = 0;
        for (int i = 0; i < hLen * 2; ++i) {
            accumulation += 0xffff & bb.getShort();
            }
        accumulation = ((accumulation >> 16) & 0xffff) + (accumulation & 0xffff);
        short sum = (short) (~accumulation & 0xffff);
		if(sum !=0){
			String s =  String.format("sum = %x, checksum = %x", sum, packet.getChecksum());
			System.out.println(s);
			return; 
		}

		System.out.println("Correct checksum >>>>>>>>>>>>>>>>>>");
		// decrement TTL
		if(packet.getTtl() <= 1) return;
		packet.setTtl((byte)(packet.getTtl() - 1));

		// Check if the packet is sent to the router itself - if yes, drop
		for(Map.Entry<String, Iface> e : this.interfaces.entrySet()){
			if(e.getValue().getIpAddress() == packet.getDestinationAddress()) return;
		}

		// Forward
		packet.setChecksum((short)0);
		b = packet.serialize(); // recalculate checksum
		packet.deserialize(b, 0, b.length);		
		System.out.println("Forwarding packet >>>>>>>>>>>>>>>>>>");
		RouteEntry IPEntry = routeTable.lookup(packet.getDestinationAddress());
		if(IPEntry == null){
			System.err.println("Failure: No entry in IP routeTable. Aborting.");
			return;
		}
		System.out.println("Printing RouteEntry IPEntry: \n" + IPEntry.toString());
		
		ArpEntry arpEntry = arpCache.lookup(packet.getDestinationAddress());
		if(arpEntry == null){
			System.err.println("Abnormal: No entry in ARP. Aborting.");
			return;
		}
		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
		etherPacket.setSourceMACAddress(IPEntry.getInterface().getMacAddress().toBytes());
		etherPacket.setPayload(packet);
		sendPacket(etherPacket, IPEntry.getInterface());
		return;
	}
}
