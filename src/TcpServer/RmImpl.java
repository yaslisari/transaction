package TcpServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;
import LockManager.*;

public class RmImpl extends Thread implements Rm {
	ObjectInputStream iis;
	DataOutputStream os;
	Socket clientSocket = null;
	LockManager lock = new LockManager();
	int myId;

	public RmImpl(Socket client) {
		clientSocket = client;
	}

	public void run() {
		try {
			iis = new ObjectInputStream(clientSocket.getInputStream());
			os = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			ArrayList<Object> array = new ArrayList<Object>();
			try 
			{
				array = (ArrayList<Object>) iis.readObject();
			} 
			catch (Exception e) 
			{
			}
			
			if (array != null) {
				if (((String) array.get(0)).compareToIgnoreCase("quit") == 0) {
					System.out.println("Client disconnected");
					break;
				}
				try {
					boolean ret=true;
					try{
						ret = reflector(array, os);
					}
					catch(DeadlockException e){}
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
		try {
			if (lock.Lock(myId, key, LockManager.READ)==true){
				if (key.charAt(0) == 'f') {
					System.out.println(myId + " putting a read lock on flight hash table, the key is " + key);
					return FlightHashTable.readData(myId, key);
				} else if (key.charAt(0) == 'c') {
					System.out.println(myId + " putting a read lock on car hash table, the key is " + key);
					return CarHashTable.readData(myId, key);
				} else {
					System.out.println(myId + " putting a read lock on hotel hash table, the key is " + key);
					return HotelHashTable.readData(myId, key);
				}
			}
		} catch (DeadlockException deadLock) {
			System.out.println("deadlock....");
			throw deadLock;
		}
		return null;
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value) throws DeadlockException{
		try {
			if (lock.Lock(myId, key, LockManager.WRITE)==true){
				if (key.charAt(0) == 'f') {
					System.out.println(myId + " putting a write lock on flight hash table, the key is " + key);
					FlightHashTable.writeData(myId, key, value);
				} else if (key.charAt(0) == 'c') {
					System.out.println(myId + " putting a write lock on Car hash table, the key is " + key);
					CarHashTable.writeData(myId, key, value);
				} else {
					System.out.println(myId + " putting a write lock on hotel hash table, the key is " + key);
					HotelHashTable.writeData(myId, key, value);
				}
			}
		} catch (DeadlockException deadLock) {
			System.out.println("deadlock....");
			throw deadLock;
		}
	}

	// Remove the item out of storage
	protected RMItem removeData(int id, String key) throws DeadlockException{
		try {
			if( lock.Lock(myId, key, LockManager.WRITE)==true){
				if (key.charAt(0) == 'f') {
					System.out.println(myId + " putting a write lock on flight hash table, the key is " + key);
					return FlightHashTable.removeData(myId, key);
				} else if (key.charAt(0) == 'c') {
					System.out.println(myId + " putting a write lock on car hash table, the key is " + key);
					return CarHashTable.removeData(myId, key);
				} else {
					System.out.println(myId + " putting a write lock on hotel hash table, the key is " + key);
					return HotelHashTable.removeData(myId, key);
				}
			}
		} catch (DeadlockException deadLock) {
			System.out.println("deadlock....");
			throw deadLock;
		}
		return null;
	}

	// deletes the entire item
	protected boolean deleteItem(int id, String key) throws DeadlockException{
		Trace.info("RM::deleteItem(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		// Check if there is such an item in the storage
		if (curObj == null) {
			Trace.warn("RM::deleteItem(" + id + ", " + key
					+ ") failed--item doesn't exist");
			return false;
		} else {
			if (curObj.getReserved() == 0) {
				removeData(id, curObj.getKey());
				Trace.info("RM::deleteItem(" + id + ", " + key
						+ ") item deleted");
				return true;
			} else {
				Trace.info("RM::deleteItem("
						+ id
						+ ", "
						+ key
						+ ") item can't be deleted because some customers reserved it");
				return false;
			}
		} // if
	}

	// query the number of available seats/rooms/cars
	protected int queryNum(int id, String key) throws DeadlockException{
		Trace.info("RM::queryNum(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getCount();
		} // else
		Trace.info("RM::queryNum(" + id + ", " + key + ") returns count="
				+ value);
		return value;
	}

	// query the price of an item
	protected int queryPrice(int id, String key) throws DeadlockException{
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getPrice();
		} // else
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$"
				+ value);
		return value;
	}

	// reserve an item
	protected int reserveItem(int id, int customerID, String key,
			String location) throws IOException, DeadlockException {
		Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", "
				+ key + ", " + location + " ) called");
		// Read customer object if it exists (and read lock it)
		/*
		 * Customer cust = (Customer) readData( id, Customer.getKey(customerID)
		 * ); if( cust == null ) { Trace.warn("RM::reserveCar( " + id + ", " +
		 * customerID + ", " + key +
		 * ", "+location+")  failed--customer doesn't exist" ); return false; }
		 */
		// check if the item is available
		ReservableItem item = (ReservableItem) readData(id, key);
		if (item == null) {
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") failed--item doesn't exist");
			return -1;
		} else if (item.getCount() == 0) {
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") failed--No more items");
			return -1;
		} else {
			// cust.reserve( key, location, item.getPrice());
			// writeData( id, cust.getKey(), cust );

			// decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);

			Trace.info("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") succeeded");
			return item.getPrice();
		}
	}

	// reserve an item
	public int reserveItemHelper(int id, int customerID, String key,
			String location) throws IOException, DeadlockException {
		Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", "
				+ key + ", " + location + " ) called");
		// Read customer object if it exists (and read lock it)
		/*
		 * Customer cust = (Customer) readData( id, Customer.getKey(customerID)
		 * ); if( cust == null ) { Trace.warn("RM::reserveCar( " + id + ", " +
		 * customerID + ", " + key +
		 * ", "+location+")  failed--customer doesn't exist" ); return false; }
		 */
		// check if the item is available
		ReservableItem item = (ReservableItem) readData(id, key);
		if (item == null) {
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") failed--item doesn't exist");
			return -1;
		} else if (item.getCount() == 0) {
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") failed--No more items");
			return -1;
		} else {
			// cust.reserve( key, location, item.getPrice());
			// writeData( id, cust.getKey(), cust );

			// decrease the number of available items in the storage
			writeData(id, item.getKey(), item);
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);

			Trace.info("RM::reserveItem( " + id + ", " + customerID + ", "
					+ key + ", " + location + ") succeeded");
			return item.getPrice();
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
	// current price
	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws IOException, DeadlockException {
		Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $"
				+ flightPrice + ", " + flightSeats + ") called");
		Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));
		if (curObj == null) {
			// doesn't exist...add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + id + ") created new flight "
					+ flightNum + ", seats=" + flightSeats + ", price=$"
					+ flightPrice);
		} else {
			// add seats to existing flight and update the price...
			writeData(id, curObj.getKey(), curObj);
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0) {
				curObj.setPrice(flightPrice);
			} // if
			Trace.info("RM::addFlight(" + id + ") modified existing flight "
					+ flightNum + ", seats=" + curObj.getCount() + ", price=$"
					+ flightPrice);
		} // else
		return (true);
	}

	public boolean deleteFlight(int id, int flightNum) throws IOException, DeadlockException {
		return deleteItem(id, Flight.getKey(flightNum));
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains
	// its current price
	public boolean addRooms(int id, String location, int count, int price)
			throws IOException, DeadlockException {
		Trace.info("RM::addRooms(" + id + ", " + location + ", " + count
				+ ", $" + price + ") called");
		Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));
		if (curObj == null) {
			// doesn't exist...add it
			Hotel newObj = new Hotel(location, count, price);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + id + ") created new room location "
					+ location + ", count=" + count + ", price=$" + price);
		} else {
			// add count to existing object and update price...
			writeData(id, curObj.getKey(), curObj);
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			} // if
			Trace.info("RM::addRooms(" + id + ") modified existing location "
					+ location + ", count=" + curObj.getCount() + ", price=$"
					+ price);
		} // else
		return (true);
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location) throws DeadlockException{
		return deleteItem(id, Hotel.getKey(location));
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its
	// current price
	public boolean addCars(int id, String location, int count, int price)
			throws IOException, DeadlockException {
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $"
				+ price + ") called");
		Car curObj = (Car) readData(id, Car.getKey(location));
		if (curObj == null) {
			// car location doesn't exist...add it
			Car newObj = new Car(location, count, price);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + id + ") created new location "
					+ location + ", count=" + count + ", price=$" + price);
		} else {
			// add count to existing car location and update price...
			writeData(id, curObj.getKey(), curObj);
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			} // if
			Trace.info("RM::addCars(" + id + ") modified existing location "
					+ location + ", count=" + curObj.getCount() + ", price=$"
					+ price);
		} // else
		return (true);
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws IOException, DeadlockException {
		return deleteItem(id, Car.getKey(location));
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws IOException, DeadlockException {
		return queryNum(id, Flight.getKey(flightNum));
	}

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum) throws IOException, DeadlockException {
		return queryPrice(id, Flight.getKey(flightNum));
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws IOException, DeadlockException {
		return queryNum(id, Hotel.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws IOException, DeadlockException {
		return queryPrice(id, Hotel.getKey(location));
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws IOException, DeadlockException {
		return queryNum(id, Car.getKey(location));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws IOException, DeadlockException {
		return queryPrice(id, Car.getKey(location));
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
		} // if
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
		} // if
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
	public boolean newCustomer(int id, int customerID) throws IOException, DeadlockException {
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

	//
	public boolean removeReservation(int id, String key, int count)
			throws IOException, DeadlockException {
		Trace.info("RM::removeReservation(" + id + ", " + key + ", " + count
				+ ") called");
		ReservableItem item = (ReservableItem) readData(id, key);
		// Trace.info("RM::deleteCustomer(" + id + ", " + customerID +
		// ") has reserved " + reserveditem.getKey() + "which is reserved" +
		// item.getReserved() + " times and is still available " +
		// item.getCount() + " times" );
		if (item == null) {
			Trace.warn("RM::removeReservation(" + id + ", " + key + ", "
					+ count + ") failed--item doesn't exist");
			return false;
		}
		writeData(id, item.getKey(), item);
		item.setReserved(item.getReserved() - count);
		item.setCount(item.getCount() + count);
		return true;
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
				ReservableItem item = (ReservableItem) readData(id,
						reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID
						+ ") has reserved " + reserveditem.getKey()
						+ "which is reserved" + item.getReserved()
						+ " times and is still available " + item.getCount()
						+ " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
			}

			// remove the customer from the storage
			removeData(id, cust.getKey());

			Trace.info("RM::deleteCustomer(" + id + ", " + customerID
					+ ") succeeded");
			return true;
		} // if
	}

	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location)
			throws IOException, DeadlockException {
		// return reserveItem(id, customerID, Car.getKey(location), location);
		return true;
	}

	// Adds room reservation to this customer.
	public boolean reserveRoom(int id, int customerID, String location)
			throws IOException, DeadlockException {
		// return reserveItem(id, customerID, Hotel.getKey(location), location);
		return true;
	}

	// Adds flight reservation to this customer.
	public boolean reserveFlight(int id, int customerID, int flightNum)
			throws IOException, DeadlockException {
		// return reserveItem(id, customerID, Flight.getKey(flightNum),
		// String.valueOf(flightNum));
		return true;
	}

	/* reserve an itinerary */
	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean Car, boolean Room) throws IOException, DeadlockException {
		return false;
	}


	public void unlock(int id){
		if (lock.UnlockAll (id) == true)
			System.out.println("Unlocked in the Rm");
		else
			System.out.println("Unlock failed in the Rm");
	}

	public boolean reflector(ArrayList<Object> array, DataOutputStream os) throws IOException, DeadlockException 
	{
		Object[] argument = array.toArray();
		
		if (((String) argument[0]).equals("addFlight")) {
			boolean ret;
			try{
				ret = addFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("Id")) {
			myId = (Integer) argument[1];
			System.out.println("New transaction started with transaction id: " + myId );
			return true;
		} 
		
		else if (((String) argument[0]).equals("addCar")) {
			boolean ret;
			try{
				ret = addCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("addRoom")) {
			boolean ret;
			try{
				ret = addRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString(),
						((Integer) argument[3]).intValue(),
						((Integer) argument[4]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("newcustomer")) {
			int ret = newCustomer(((Integer) argument[1]).intValue());
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("newcustomerid")) {
			boolean ret = newCustomer(((Integer) argument[1]).intValue(),
					((Integer) argument[2]).intValue());
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("deleteFlight")) {
			boolean ret;
			try{
				ret = deleteFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("deleteCar")) {
			boolean ret;
			try{
				ret = deleteCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("deleteRoom")) {
			boolean ret;
			try{
				ret = deleteRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("deleteCustomer")) {
			boolean ret;
			try{
				ret = deleteCustomer(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryFlight")) {
			int ret;
			try{	
				ret = queryFlight(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryCar")) {
			int ret;
			try{
				ret = queryCars(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryRoom")) {
			int ret;
			try{
				ret = queryRooms(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());
			}catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("removeReservation")) {
			boolean ret;
			try{
				ret = removeReservation(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString(),
						((Integer) argument[3]).intValue());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryFlightPrice")) {
			int ret;
			try{
				ret = queryFlightPrice(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryCarPrice")) {
			int ret;
			try{
				ret = queryCarsPrice(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("queryRoomPrice")) {
			int ret;
			try{
				ret = queryRoomsPrice(((Integer) argument[1]).intValue(),
						((String) argument[2]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("reserveItemHelper")) {
			int ret;
			try{
				ret = reserveItemHelper(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((String) argument[3]).toString(),
						((String) argument[4]).toString());
			}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeInt(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("reserveCar")) {
			boolean ret;
			try{
				ret = reserveCar(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((String) argument[3]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("reserveRoom")) {
			boolean ret;
			try{
				ret = reserveRoom(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((String) argument[3]).toString());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if (((String) argument[0]).equals("itinerary")) {
			boolean ret;

			try{
				ret = itinerary(((Integer) argument[1]).intValue(),
						((Integer) argument[2]).intValue(),
						((Vector<Integer>) argument[3]),
						((String) argument[4]).toString(),
						((Boolean) argument[5]).booleanValue(),
						((Boolean) argument[6]).booleanValue());}
			catch(DeadlockException e){
				os.writeBoolean(false);
				System.out.println("Deadlock in RM, aborting");
				return true;
			}
			os.writeBoolean(true);
			os.writeBoolean(ret);
			return true;
		} 
		
		else if  (((String) argument[0]).equals("commit")) {
			boolean ret;
			if(((Integer)argument[1]).intValue()==0){//flight
				System.out.println("committing flight Rm");
				ret = FlightHashTable.commit(myId);
			}
			else if(((Integer)argument[1]).intValue()==1){//room
				System.out.println("committing hotel Rm");
				HotelHashTable.commit(myId);
			}
			else{
				CarHashTable.commit(myId);
				System.out.println("committing car Rm");
			}
			unlock(myId);
			os.writeBoolean(true);
			return true;
		} 
		
		else if  (((String) argument[0]).equals("abort")) {
			if(((Integer)argument[1]).intValue()==0){//flight
				System.out.println("aborting flight Rm");
				FlightHashTable.abort(myId);
			}
			else if(((Integer)argument[1]).intValue()==1){//room
				System.out.println("aborting hotel Rm");
				HotelHashTable.abort(myId);
			}
			else{
				System.out.println("aborting car Rm");
				CarHashTable.abort(myId);
			}
			unlock(myId);
			os.writeBoolean(true);
			return true;
		} 

		else if (((String) argument[0]).equals("start")) {
			myId = ((Integer)argument[1]).intValue();
			System.out.println("New transaction started with id: " + myId );
			os.writeBoolean(true);
			return true;
		}

		else if(((String) argument[0]).equals("shutdown")) {
			iis.close();
			os.close();
			clientSocket.close();
			System.out.println("Shuting down rm.");
			System.exit(1);
			return true;
		}

		else {
			System.out.println("Method desired was not found");
			return false;
		}
	}
}
