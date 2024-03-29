package TcpServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import LockManager.DeadlockException;
import LockManager.LockManager;

public class MwImpl extends Thread implements Mw {
	// protected RMHashtable m_itemHT = new RMHashtable();
	ObjectInputStream iis;
	DataOutputStream os;
	ObjectOutputStream oos;
	Socket clientSocket;
	String rmF;
	int rmFPort;
	String rmC;
	int rmCPort;
	String rmH;
	int rmHPort;
	Socket fSocket;
	DataInputStream fIs;
	ObjectOutputStream fOs;
	Socket cSocket;
	DataInputStream cIs;
	ObjectOutputStream cOs;
	Socket hSocket;
	DataInputStream hIs;
	ObjectOutputStream hOs;
	ArrayList<Object> array;
	ArrayList<Object> outArray;
	LockManager lock = new LockManager();
	int myId;

	public MwImpl(Socket client, String rmF, int rmFPort, String rmC,
			int rmCPort, String rmH, int rmHPort) {
		clientSocket = client;
		this.rmF = rmF;
		this.rmFPort = rmFPort;
		this.rmC = rmC;
		this.rmCPort = rmCPort;
		this.rmH = rmH;
		this.rmHPort = rmHPort;
	}

	public void connectToRm() {
		try {
			fSocket = new Socket(rmF, rmFPort);
			fOs = new ObjectOutputStream(fSocket.getOutputStream());
			fIs = new DataInputStream(fSocket.getInputStream());
			cSocket = new Socket(rmC, rmCPort);
			cOs = new ObjectOutputStream(cSocket.getOutputStream());
			cIs = new DataInputStream(cSocket.getInputStream());
			hSocket = new Socket(rmH, rmHPort);
			hOs = new ObjectOutputStream(hSocket.getOutputStream());
			hIs = new DataInputStream(hSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to host");
		}
		if (fSocket != null && fOs != null && fIs != null && cSocket != null
				&& cOs != null && cIs != null && hSocket != null && hOs != null
				&& hIs != null) {

		} else {
			System.out
			.println("failed at rmSocket != null && os != null && is != null");
			System.exit(0);
		}
	}

	public void run() {
		connectToRm();
		try {
			iis = new ObjectInputStream(clientSocket.getInputStream());
			os = new DataOutputStream(clientSocket.getOutputStream());
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			ArrayList<Object> array = new ArrayList<Object>();
			try {
				array = (ArrayList<Object>) iis.readObject();
			} catch (Exception e) {
			}
			if (array != null) {
				if (((String) array.get(0)).compareToIgnoreCase("quit") == 0) {
					System.out.println("Client disconnected");

					break;
				}
				try {
					boolean ret = reflector(array, os);
					if (ret) {
						System.out.println(myId + "'s operation executed");
					} else {
						System.out.println(myId + "'s operation failed");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// Reads a data item
	private RMItem readData(int id, String key) throws DeadlockException{
		try{
			if(	lock.Lock(myId, key, LockManager.READ)){

				//	System.out.println(MwHashTable.readData(id, key));
				System.out.println("putting a read lock on mw hash table");
				return MwHashTable.readData(myId, key);
			} 
		}catch(DeadlockException e){
			System.out.println("deadlock in mw-readdata");
			throw e;
		}
		return null;
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value) throws DeadlockException {
		try{
			if(lock.Lock(myId, key, LockManager.WRITE)){
				System.out.println("putting a write lock on mw hash table");
				MwHashTable.writeData(myId, key, value);
			}
		}
		catch(DeadlockException e){
			System.out.println("deadlock in mw-writedata");
			throw e;
		}
	}

	// Remove the item out of storage
	protected RMItem removeData(int id, String key) throws DeadlockException{
		try{
			if(lock.Lock(myId, key, LockManager.WRITE)){
				System.out.println("putting a write lock on mw hash table");
				return MwHashTable.removeData(myId, key);
			}
		}
		catch(DeadlockException e){
			System.out.println("deadlock in mw-removedata");
			throw e;
		}

		return null;
	}

	//unlocks locks related to transaction
	public void unlock(int id){
		if (lock.UnlockAll (id) == true)
			System.out.println("Unlocked in the Mw");
		else
			System.out.println("Unlock failed in the Mw");
	}

	// reserve an item
	protected boolean reserveItem(int id, int customerID, String key,
			String location) throws IOException, DeadlockException {
		Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", "
				+ key + ", " + location + " ) called");
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key
					+ ", " + location + ")  failed--customer doesn't exist");
			return false;
		}
		int price;
		array = new ArrayList<Object>();
		String method = "reserveItemHelper";
		array.add(method);
		array.add(id);
		array.add(customerID);
		array.add(key);
		array.add(location);
		if (key.charAt(0) == 'f') {
			fOs.writeObject(array);
			if(!fIs.readBoolean())
				throw new DeadlockException(myId,"");
			price = fIs.readInt();
		} else if (key.charAt(0) == 'c') {
			cOs.writeObject(array);
			if(!cIs.readBoolean())
				throw new DeadlockException(myId,"");
			price = cIs.readInt();
		} else {
			hOs.writeObject(array);
			if(!hIs.readBoolean())
				throw new DeadlockException(myId,"");
			price = hIs.readInt();
		}
		if (price == -1) {
			return false;
		} else {
			writeData(id, cust.getKey(), cust);
			cust.reserve(key, location, price);
			Trace.info("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") succeeded");
			return true;
		}
	}

	public int reserveItemHelper(int id, int customerID, String key,
			String location) throws IOException {
		return 0;
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
	// current price
	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws IOException, DeadlockException {
		Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $"
				+ flightPrice + ", " + flightSeats + ") called on middleware");
		array = new ArrayList<Object>();
		String method = "addFlight";
		array.add(method);
		array.add(id);
		array.add(flightNum);
		array.add(flightSeats);
		array.add(flightPrice);
		fOs.writeObject(array);
		if(!fIs.readBoolean())
			throw new DeadlockException(myId,"");
		return fIs.readBoolean();
	}

	public boolean deleteFlight(int id, int flightNum) throws IOException,DeadlockException {
		Trace.info("RM::deleteFlight(" + id + ", " + flightNum
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "deleteFlight";
		array.add(method);
		array.add(id);
		array.add(flightNum);
		fOs.writeObject(array);
		if(!fIs.readBoolean())
			throw new DeadlockException(myId,"");
		return fIs.readBoolean();
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains
	// its current price
	public boolean addRooms(int id, String location, int count, int price)
			throws IOException, DeadlockException {
		Trace.info("RM::addRooms(" + id + ", " + location + ", " + count
				+ ", $" + price + ") called");
		array = new ArrayList<Object>();
		String method = "addRoom";
		array.add(method);
		array.add(id);
		array.add(location);
		array.add(count);
		array.add(price);
		hOs.writeObject(array);
		if(!hIs.readBoolean())
			throw new DeadlockException(myId,"");
		return hIs.readBoolean();
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::deleteRooms(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "deleteRoom";
		array.add(method);
		array.add(id);
		array.add(location);
		hOs.writeObject(array);
		if(!hIs.readBoolean())
			throw new DeadlockException(myId,"");
		return hIs.readBoolean();
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its
	// current price
	public boolean addCars(int id, String location, int count, int price)
			throws IOException, DeadlockException {
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $"
				+ price + ") called");
		array = new ArrayList<Object>();
		String method = "addCar";
		array.add(method);
		array.add(id);
		array.add(location);
		array.add(count);
		array.add(price);
		cOs.writeObject(array);
		if(!cIs.readBoolean())
			throw new DeadlockException(myId,"");
		return cIs.readBoolean();
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::deleteCars(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "deleteCar";
		array.add(method);
		array.add(id);
		array.add(location);
		cOs.writeObject(array);
		if(!cIs.readBoolean())
			throw new DeadlockException(myId,"");
		return cIs.readBoolean();
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws IOException, DeadlockException {
		Trace.info("RM::queryFlight(" + id + ", " + flightNum
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryFlight";
		array.add(method);
		array.add(id);
		array.add(flightNum);
		fOs.writeObject(array);
		if(!fIs.readBoolean())
			throw new DeadlockException(myId,"");
		return fIs.readInt();
	}

	// Returns the number of reservations for this flight.
	// public int queryFlightReservations(int id, int flightNum)
	// throws IOException
	// {
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum +
	// ") called" );
	// RMInteger numReservations = (RMInteger) readData( id,
	// Flight.getNumReservationsKey(flightNum) );
	// if( numReservations == null ) {
	// numReservations = new RMInteger(0);
	// } // if
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum +
	// ") returns " + numReservations );
	// return numReservations.getValue();
	// }

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum) throws IOException, DeadlockException {
		Trace.info("RM::queryFlightPrice(" + id + ", " + flightNum
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryFlightPrice";
		array.add(method);
		array.add(id);
		array.add(flightNum);
		fOs.writeObject(array);
		if(!fIs.readBoolean())
			throw new DeadlockException(myId,"");
		return fIs.readInt();
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::queryRooms(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryRoom";
		array.add(method);
		array.add(id);
		array.add(location);
		hOs.writeObject(array);
		if(!hIs.readBoolean())
			throw new DeadlockException(myId,"");
		return hIs.readInt();
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::queryRoomPrice(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryRoomPrice";
		array.add(method);
		array.add(id);
		array.add(location);
		hOs.writeObject(array);
		if(!hIs.readBoolean())
			throw new DeadlockException(myId,"");
		return hIs.readInt();
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::queryCars(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryCar";
		array.add(method);
		array.add(id);
		array.add(location);
		cOs.writeObject(array);
		if(!cIs.readBoolean())
			throw new DeadlockException(myId,"");
		return cIs.readInt();
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws IOException, DeadlockException {
		Trace.info("RM::queryCarPrice(" + id + ", " + location
				+ ") called on middleware");
		array = new ArrayList<Object>();
		String method = "queryCarPrice";
		array.add(method);
		array.add(id);
		array.add(location);
		cOs.writeObject(array);
		if(!cIs.readBoolean())
			throw new DeadlockException(myId,"");
		return cIs.readInt();
	}

	// Returns data structure containing customer reservation info. Returns null
	// if the
	// customer doesn't exist. Returns empty RMHashtable if customer exists but
	// has no
	// reservations.
	public RMHashtable getCustomerReservations(int id, int customerID)
			throws IOException, DeadlockException {
		Trace.info("RM::getCustomerReservations(" + id + ", " + customerID
				+ ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::getCustomerReservations failed(" + id + ", "
					+ customerID + ") failed--customer doesn't exist");
			return null;
		} else {
			return cust.getReservations();
		}
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID) throws IOException, DeadlockException {
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
				+ ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID
					+ ") failed--customer doesn't exist");
			return ""; // NOTE: don't change this--WC counts on this value
			// indicating a customer does not exist...
		} else {
			String s = cust.printBill();
			Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
					+ "), bill follows...");
			System.out.println(s);
			return s;
		}
	}

	// customer functions
	// new customer just returns a unique customer identifier
	public int newCustomer(int id) throws IOException, DeadlockException {
		Trace.info("INFO: RM::newCustomer(" + id + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(id)
				+ String.valueOf(Calendar.getInstance().get(
						Calendar.MILLISECOND))
						+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer cust = new Customer(cid);
		writeData(id, cust.getKey(), cust);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int id, int customerID) throws DeadlockException{
		Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
				+ ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			cust = new Customer(customerID);
			writeData(id, cust.getKey(), cust);
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
					+ ") created a new customer");
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
					+ ") failed--customer already exists");
			return false;
		} // else
	}

	// Deletes customer from the database.
	public boolean deleteCustomer(int id, int customerID) throws IOException, DeadlockException {
		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::deleteCustomer(" + id + ", " + customerID
					+ ") failed--customer doesn't exist");
			return false;
		} else {
			// Increase the reserved numbers of all reservable items which the
			// customer reserved.
			RMHashtable reservationHT = cust.getReservations();

			for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {
				String reservedkey = (String) (e.nextElement());
				ReservedItem reserveditem = cust.getReservedItem(reservedkey);
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID
						+ ") has reserved " + reserveditem.getKey() + " "
						+ reserveditem.getCount() + " times");
				boolean temp;
				if (reserveditem.getKey().charAt(0) == 'f') {
					array = new ArrayList<Object>();
					String method = "removeReservation";
					array.add(method);
					array.add(id);
					array.add(reserveditem.getKey());
					array.add(reserveditem.getCount());
					fOs.writeObject(array);
					if(!fIs.readBoolean())
						throw new DeadlockException(myId,"");
					temp = fIs.readBoolean();
				} else if (reserveditem.getKey().charAt(0) == 'c') {
					array = new ArrayList<Object>();
					String method = "removeReservation";
					array.add(method);
					array.add(id);
					array.add(reserveditem.getKey());
					array.add(reserveditem.getCount());
					cOs.writeObject(array);
					if(!cIs.readBoolean())
						throw new DeadlockException(myId,"");
					temp = cIs.readBoolean();
				} else {
					array = new ArrayList<Object>();
					String method = "removeReservation";
					array.add(method);
					array.add(id);
					array.add(reserveditem.getKey());
					array.add(reserveditem.getCount());
					hOs.writeObject(array);
					if(!hIs.readBoolean())
						throw new DeadlockException(myId,"");
					temp = hIs.readBoolean();
				}
				if (!temp) {
					return false;
				}
			}
			removeData(id, cust.getKey());
			Trace.info("RM::deleteCustomer(" + id + ", " + customerID
					+ ") succeeded");
			return true;
		}
	}

	public boolean removeReservation(int id, String key, int count)
			throws IOException {
		return true;
	}

	// Frees flight reservation record. Flight reservation records help us make
	// sure we
	// don't delete a flight if one or more customers are holding reservations
	// public boolean freeFlightReservation(int id, int flightNum)
	// throws IOException
	// {
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum +
	// ") called" );
	// RMInteger numReservations = (RMInteger) readData( id,
	// Flight.getNumReservationsKey(flightNum) );
	// if( numReservations != null ) {
	// numReservations = new RMInteger( Math.max( 0,
	// numReservations.getValue()-1) );
	// } // if
	// writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum +
	// ") succeeded, this flight now has "
	// + numReservations + " reservations" );
	// return true;
	// }
	//

	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location)
			throws IOException, DeadlockException {
		return reserveItem(id, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer.
	public boolean reserveRoom(int id, int customerID, String location)
			throws IOException, DeadlockException {
		return reserveItem(id, customerID, Hotel.getKey(location), location);
	}

	// Adds flight reservation to this customer.
	public boolean reserveFlight(int id, int customerID, int flightNum)
			throws IOException, DeadlockException {
		return reserveItem(id, customerID, Flight.getKey(flightNum),
				String.valueOf(flightNum));
	}

	/* reserve an itinerary */
	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean car, boolean Room) throws IOException, DeadlockException {
		Customer cust = (Customer) readData(id, Customer.getKey(customer));
		if (cust == null) {
			Trace.warn("RM::itinerary( " + id + ", " + customer + ", "
					+ location + ")  failed--customer doesn't exist");
			return false;
		}
		if (car) {
			array = new ArrayList<Object>();
			String method = "reserveItemHelper";
			array.add(method);
			array.add(id);
			array.add(customer);
			array.add(Car.getKey(location));
			array.add(location);
			cOs.writeObject(array);
			if(!cIs.readBoolean())
				throw new DeadlockException(myId,"");
			int carPrice = cIs.readInt();
			if (carPrice == -1) {
				return false;
			}
			writeData(id, cust.getKey(), cust);
			cust.reserve(Car.getKey(location), location, carPrice);
		}
		if (Room) {
			array = new ArrayList<Object>();
			String method = "reserveItemHelper";
			array.add(method);
			array.add(id);
			array.add(customer);
			array.add(Hotel.getKey(location));
			array.add(location);
			hOs.writeObject(array);
			if(!hIs.readBoolean())
				throw new DeadlockException(myId,"");
			int roomPrice = hIs.readInt();
			if (roomPrice == -1) {
				return false;
			}
			writeData(id, cust.getKey(), cust);
			cust.reserve(Hotel.getKey(location), location, roomPrice);
		}
		Iterator iterator = flightNumbers.iterator();
		int flightPrice;
		while (iterator.hasNext()) 
		{
			int flightNum = Integer.parseInt(iterator.next().toString());
			array = new ArrayList<Object>();
			String method = "reserveItemHelper";
			array.add(method);
			array.add(id);
			array.add(customer);
			array.add(Flight.getKey(flightNum));
			array.add(String.valueOf(flightNum));
			fOs.writeObject(array);
			if(!fIs.readBoolean())
				throw new DeadlockException(myId,"");
			flightPrice = fIs.readInt();
			if (flightPrice == -1) {
				return false;
			}
			writeData(id, cust.getKey(), cust);
			cust.reserve(Flight.getKey(flightNum), String.valueOf(flightNum), flightPrice);
		}
		Trace.info("RM::Reserve Itinerary(" + id + ", " + customer
				+ ") Succeded");
		return true;
	}

	public boolean abort() throws IOException
	{
		Vector ret = TransactionManager.abort(myId);
		if(ret != null)
		{
			int size = ret.size();
			boolean ret1= false;
	
			ArrayList<Object> array1 = new ArrayList<Object>();
			array1.add("abort");
	
			for(int i =0; i < size; i++){
				if(((String)ret.elementAt(i)).equals("f")){ //flight
					int code = 0;
					array1.add(code);
					fOs.writeObject(array1);
					array1.remove(1);
					ret1 = fIs.readBoolean();
				}
				else if(((String)ret.elementAt(i)).equals("h")){ //hotel
					int code = 1;
					array1.add(code);
					hOs.writeObject(array1);
					array1.remove(1);
					ret1 = hIs.readBoolean();
				}
				else if(((String)ret.elementAt(i)).equals("c")){ //car
					int code = 2;
					array1.add(code);
					cOs.writeObject(array1);
					array1.remove(1);
					ret1 = cIs.readBoolean();
				}
				else{
					System.out.println("a problem in abort() mw");
					ret1 = false;
				}
	
			}
	
			unlock(myId);
			return true;
		}
		else
		{
			return true;
		}

	}

	public boolean reflector(ArrayList<Object> array, DataOutputStream os)
			throws IOException {

		Object[] argument = array.toArray();
		if (((String) argument[0]).equals("start")) {
			int id = TransactionManager.start();
			myId = id;
			System.out.println("New transaction started with id: " + myId);
			ArrayList<Object> array1 = new ArrayList<Object>();
			array1.add("start");
			array1.add(myId);
			cOs.writeObject(array1);
			fOs.writeObject(array1);
			hOs.writeObject(array1);

			cIs.readBoolean();
			fIs.readBoolean();
			hIs.readBoolean();

			os.writeInt(id);
			return true;
		} else if (((String) argument[0]).equals("commit")) {

			Vector ret = TransactionManager.commit(myId);
			if(ret != null)
			{
				int size = ret.size();
				boolean ret1= false;
	
				ArrayList<Object> array1 = new ArrayList<Object>();
				array1.add("commit");
	
				for(int i =0; i < size; i++)
				{
					if(((String)ret.elementAt(i)).equals("f")){ //flight
						int code = 0;
						array1.add(code);
						fOs.writeObject(array1);
						array1.remove(1);
						ret1 = fIs.readBoolean();
					}
					else if(((String)ret.elementAt(i)).equals("h")){ //hotel
						int code = 1;
						array1.add(code);
						hOs.writeObject(array1);
						array1.remove(1);
						ret1 = hIs.readBoolean();
					}
					else if(((String)ret.elementAt(i)).equals("c")){ //car
						int code = 2;
						array1.add(code);
						cOs.writeObject(array1);
						array1.remove(1);
						ret1 = cIs.readBoolean();
					}
					else{
						System.out.println("a problem in commit() mw");
						ret1 = false;
					}
	
				}
				os.writeBoolean(true);
				unlock(myId);
				return true;
			}
			else
			{
				os.writeBoolean(false);
				return true;
			}
			
			
		} else if (((String) argument[0]).equals("abort")) {
			abort();
			os.writeBoolean(true);
			return true;
		}
		else if(((String) argument[0]).equals("shutdown")){

			if(!TransactionManager.transactionsLeft())
			{
				try {
					ArrayList<Object> array1 = new ArrayList<Object>();
					String shutdown = "shutdown";
					array1.add(shutdown);
					fOs.writeObject(array1);
					cOs.writeObject(array1);
					hOs.writeObject(array1);
					os.writeBoolean(true);
					try {
						Thread.sleep(1000);
					} catch (Exception e) {

					}
					iis.close();
					os.close();
					oos.close();
					fOs.close();
					cOs.close();
					hOs.close();
					fIs.close();
					cIs.close();
					hIs.close();
					fSocket.close();
					cSocket.close();
					hSocket.close();
					clientSocket.close();
				} catch (UnknownHostException e) {
					System.err.println("Trying to connect to unknown host: "
							+ e);
				} catch (IOException e) {
					System.err.println("IOException: " + e);
				}
				System.out.println("Quitting middleware.");
				System.exit(1);
			}
			else
			{
				os.writeBoolean(false);
			}
			return true;
		}
		else if (((String) argument[0]).equals("addFlight")) {
			boolean ret;
			TransactionManager.enlist(myId, "f");
			try{
				ret = addFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("addCar")) {
			TransactionManager.enlist(myId, "c");
			boolean ret;
			try{
				ret = addCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("addRoom")) {
			TransactionManager.enlist(myId, "h");
			boolean ret;
			try{
				ret = addRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("newCustomer")) {
			int ret;
			try{
				ret = newCustomer(((Integer) argument[1]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 

		else if (((String) argument[0]).equals("newCustomerId")) {
			boolean ret;
			try{
				ret = newCustomer(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;

		} else if (((String) argument[0]).equals("deleteFlight")) {
			boolean ret;
			TransactionManager.enlist(myId, "f");
			try{
				ret = deleteFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}

			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("deleteCar")) {
			TransactionManager.enlist(myId, "c");
			boolean ret;
			try{
				ret = deleteCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());

			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("deleteRoom")) {
			TransactionManager.enlist(myId, "h");
			boolean ret;
			try{
				ret = deleteRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		else if (((String) argument[0]).equals("deleteCustomer")) {
			boolean ret;
			TransactionManager.enlist(myId, "f");
			TransactionManager.enlist(myId, "c");
			TransactionManager.enlist(myId, "h");
			try{
				ret= deleteCustomer(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		}
		else if (((String) argument[0]).equals("queryFlight")) {
			int ret;
			TransactionManager.enlist(myId, "f");
			try{
				ret = queryFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("queryCar")) {
			TransactionManager.enlist(myId, "c");

			int ret;
			try{
				ret = queryCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("queryRoom")) {
			TransactionManager.enlist(myId, "h");

			int ret;
			try{
				ret = queryRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("queryCustomer")) {
			String ret;
			try{
				ret = queryCustomerInfo(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			oos.writeObject(ret);
			return true;
		} else if (((String) argument[0]).equals("queryFlightPrice")) {
			TransactionManager.enlist(myId, "f");
			int ret;
			try{
				ret = queryFlightPrice(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("queryCarPrice")) {
			TransactionManager.enlist(myId, "c");
			int ret;
			try{
				ret = queryCarsPrice(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("queryRoomPrice")) {
			TransactionManager.enlist(myId, "h");
			int ret;
			try{
				ret = queryRoomsPrice(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} else if (((String) argument[0]).equals("reserveFlight")) {
			boolean ret;
			TransactionManager.enlist(myId, "f");
			try{
				ret = reserveFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((Integer) argument[3]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				TransactionManager.abort(myId);
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("reserveCar")) {
			TransactionManager.enlist(myId, "c");
			boolean ret;
			try{
				ret = reserveCar(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((String) argument[3]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("reserveRoom")) {
			TransactionManager.enlist(myId, "h");
			boolean ret;
			try{
				ret = reserveRoom(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((String) argument[3]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				abort();
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else if (((String) argument[0]).equals("itinerary")) {
			TransactionManager.enlist(myId, "f");
			TransactionManager.enlist(myId, "c");
			TransactionManager.enlist(myId, "h");
			boolean ret;
			try{
				ret = itinerary(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((Vector<Integer>) argument[3]),
						((String) argument[4]).toString(),
						((Boolean) argument[5]).booleanValue(),
						((Boolean) argument[6]).booleanValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock, aborting");
				TransactionManager.abort(myId);
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} else {
			System.out.println("Method desired was not found");
			return false;
		}




	}

}
