package TcpServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

public class clientAuto extends Thread {
	static String message = "blank";
	String [] args;
	int threadNum;
	
	public clientAuto(String args[], int i)
	{
		this.args = args;
		threadNum = i;
	}
	
	public void run() {
		clientAuto obj = new clientAuto(args,0);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(
				System.in));
		String command = "";
		Vector arguments = new Vector();
		int Id, Cid, transactionID;
		int flightNum;
		int flightPrice;
		int flightSeats;
		boolean Room;
		boolean Car;
		int price;
		int numRooms;
		int numCars;
		String location;
		int numberOfTransactions = 0;
		Socket clientSocket = null;
		DataInputStream is = null;
		ObjectOutputStream oos = null;
		ObjectInputStream iis = null;
		BufferedReader in = null;
		long totalResponseTime = 0;
		long totalAverage = 0;
		try {
			clientSocket = new Socket((args[0]), Integer.parseInt(args[1]));
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			is = new DataInputStream(clientSocket.getInputStream());
			iis = new ObjectInputStream(clientSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to host");
		}
		if (clientSocket != null && oos != null && is != null) {
		} else {
			System.out
			.println("failed at clientSocket != null && os != null && is != null");
			System.exit(0);
		}
		
		
		String [] commandList1 = {"newcustomer"}; //1 parameter
		String [] commandList2 = {"queryflight", "querycar", "queryroom", "newcustomerid",
				"deleteflight", "deletecar", "deleteroom", "deletecustomer", 
				"queryflightprice", "querycarprice", "queryroomprice", "querycustomer"}; //2 parameters
		String [] commandList3 = {"reserveflight", "reserveroom", "reservecar"}; //3 parameters
		String [] commandList4 = {"newflight","newcar","newroom"}; //4 parameters
		String [] commandList5 = {"itinerary"}; //6 parameters
	
		int incr = 0;
		long globaltime = System.currentTimeMillis();
		long waitTime=1000;
		
		while (numberOfTransactions < Integer.parseInt(args[2])) 
		{
			if(System.currentTimeMillis()-globaltime > waitTime){
			try{
				Thread.sleep(waitTime-(System.currentTimeMillis()-globaltime));}
			catch(Exception e){}
			}
			globaltime = System.currentTimeMillis();
			
			String tempstr2 = "";
		//	String tempstr2 = commandList4[incr] + tempstr + tempstr + tempstr + tempstr;
			
			Random rand = new Random();
			int randNumber = rand.nextInt(19);
			
			if(randNumber == 0){ //1 parameter
				tempstr2 = tempstr2 + commandList1[randNumber%1];
				tempstr2= tempstr2 + "," + Integer.toString(threadNum);
				
			}
			else if(randNumber >0 && randNumber < 13){ //2 parameters
				tempstr2 = tempstr2 + commandList2[randNumber%12];
				for( int k = 0; k < 2;k++)
					tempstr2 = tempstr2 + "," + Integer.toString(threadNum);
			}
			else if(randNumber >= 13 && randNumber < 16){ //3 parameters
				tempstr2 = tempstr2 + commandList3[randNumber%3];
				for( int k = 0; k < 3;k++)
					tempstr2 = tempstr2 + "," + Integer.toString(threadNum);
			}
			else if(randNumber >=16 && randNumber < 19){ //4 parameters
				tempstr2 = tempstr2 + commandList4[randNumber%3];
				for( int k = 0; k < 4;k++)
					tempstr2 = tempstr2 + "," + Integer.toString(threadNum);
				
			}
			else if(randNumber ==19){  //6 parameters
				tempstr2 = tempstr2 + commandList5[randNumber%1];
				for( int k = 0; k < 6;k++)
					tempstr2 = tempstr2 + "," + Integer.toString(threadNum);
			}
			
			command = tempstr2;
			incr++;
			//System.out.println(command);
				
			if(numberOfTransactions % 5 == 0)
			{
				command = "start";
				totalResponseTime = 0;
				incr = 0;
			}
			if((numberOfTransactions + 1) % 5 == 0)
			{
				command = "commit";
				System.out.println("average response time: " + totalResponseTime/(long)(3) + "ms");
				totalAverage += totalResponseTime/(long)(3);
				incr = 0;
			}
			if(numberOfTransactions == Integer.parseInt(args[2]) - 1)
			{
				command = "commit";
				long totAvgRespTime = totalAverage/(long)(Integer.parseInt(args[2]) / 5);
				System.out.println("--\n--\n--\n--\n--\n--\n--\n--\n--\n--\n--\n--\n TOTAL AVG RESPONSE TIME: " + totAvgRespTime + "ms");
				clientC.incrRespTime(totAvgRespTime);
				incr = 0;
			}
			numberOfTransactions++;
			// remove heading and trailing white space
			command = command.trim();
			arguments = obj.parse(command);
			System.out.println("the command entered was " + (String) arguments.elementAt(0));
			// decide which of the commands this was
			
			try{
				String str = "bogus";
				ArrayList<Object> bogus = new ArrayList<Object>();
				bogus.add(str);
				oos.writeObject(bogus);
				Boolean check =is.readBoolean();
				if(check==false && arguments.size()>=1 &&
						 !(   
								(((String)arguments.elementAt(0)).equalsIgnoreCase("start"))   ||
								(((String)arguments.elementAt(0)).equalsIgnoreCase("quit")) ||
								(((String)arguments.elementAt(0)).equalsIgnoreCase("shutdown"))
						   )
				  )
				{
					System.out.println("there is no active transaction");
					continue;
				}
				}catch(Exception e){}

			switch (obj.findChoice((String) arguments.elementAt(0))) {
			case 1: // help section
				if (arguments.size() == 1) // command was "help"
					obj.listCommands();
				else if (arguments.size() == 2) // command was
					// "help <commandname>"
					obj.listSpecific((String) arguments.elementAt(1));
				else
					// wrong use of help command
					System.out
					.println("Improper use of help command. Type help or help, <commandname>");
				break;

			case 2: // new flight
				if (arguments.size() != 5) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Adding a new Flight using id: "
						+ arguments.elementAt(1));
				System.out.println("Flight number: " + arguments.elementAt(2));
				System.out.println("Add Flight Seats: "
						+ arguments.elementAt(3));
				System.out.println("Set Flight Price: "
						+ arguments.elementAt(4));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					flightNum = obj.getInt(arguments.elementAt(2));
					flightSeats = obj.getInt(arguments.elementAt(3));
					flightPrice = obj.getInt(arguments.elementAt(4));

					String method = "addFlight";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(flightNum);
					array.add(flightSeats);
					array.add(flightPrice);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					if (is.readBoolean()) {
						System.out.println("Flights were added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newflight response time: " + responseTime + "ms");
					} else {
						System.out.println("Flights could not be added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newflight response time: " + responseTime + "ms");
					}
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 3: // new Car
				if (arguments.size() != 5) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Adding a new Car using id: "
						+ arguments.elementAt(1));
				System.out.println("Car Location: " + arguments.elementAt(2));
				System.out.println("Add Number of Cars: "
						+ arguments.elementAt(3));
				System.out.println("Set Price: " + arguments.elementAt(4));
				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));
					numCars = obj.getInt(arguments.elementAt(3));
					price = obj.getInt(arguments.elementAt(4));
					String method = "addCar";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					array.add(numCars);
					array.add(price);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Cars added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newcar response time: " + responseTime + "ms");
					} else {
						System.out.println("Cars could not be added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newcar response time: " + responseTime + "ms");
					}

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 4: // new Room
				if (arguments.size() != 5) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Adding a new Room using id: "
						+ arguments.elementAt(1));
				System.out.println("Room Location: " + arguments.elementAt(2));
				System.out.println("Add Number of Rooms: "
						+ arguments.elementAt(3));
				System.out.println("Set Price: " + arguments.elementAt(4));
				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));
					numRooms = obj.getInt(arguments.elementAt(3));
					price = obj.getInt(arguments.elementAt(4));
					String method = "addRoom";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					array.add(numRooms);
					array.add(price);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Rooms added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newroom response time: " + responseTime + "ms");
					} else {
						System.out.println("Rooms could not be added");
						long responseTime = (System.currentTimeMillis() - time1);
						totalResponseTime += responseTime;
						System.out.println("Newroom response time: " + responseTime + "ms");
						
					}
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 5: // new Customer
				if (arguments.size() != 2) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Adding a new Customer using id:"
						+ arguments.elementAt(1));

				try {
					Id = obj.getInt(arguments.elementAt(1));

					String method = "newCustomer";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
					System.out.println("Newcustomer response time: " + responseTime + "ms");
					System.out.println("new customer id: " + is.readInt());

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 6: // delete Flight
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Deleting a flight using id: "
						+ arguments.elementAt(1));
				System.out.println("Flight Number: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					flightNum = obj.getInt(arguments.elementAt(2));

					String method = "deleteFlight";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(flightNum);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					if (is.readBoolean()) {
						System.out.println("Flight deleted");
						
					} else {
						System.out.println("Flight could not be deleted");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 7: // delete Car
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out
				.println("Deleting the cars from a particular location using id: "
						+ arguments.elementAt(1));
				System.out.println("Car Location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "deleteCar";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Cars deleted");
					} else {
						System.out.println("Cars could not be deleted");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 8: // delete Room
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out
				.println("Deleting all rooms from a particular location using id: "
						+ arguments.elementAt(1));
				System.out.println("Room Location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "deleteRoom";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Rooms deleted");
					} else {
						System.out.println("Rooms could not be deleted");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 9: // delete Customer
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out
				.println("Deleting a customer from the database using id: "
						+ arguments.elementAt(1));
				System.out.println("Customer id: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));

					String method = "deleteCustomer";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					
					if(!is.readBoolean()){
						deadLock();
						continue;
					}

					if (is.readBoolean()) {
						System.out.println("Customer Deleted");
					} else
						System.out.println("Customer could not be deleted");
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 10: // querying a flight
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a flight using id: "
						+ arguments.elementAt(1));
				System.out.println("Flight number: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					flightNum = obj.getInt(arguments.elementAt(2));

					String method = "queryFlight";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(flightNum);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);

					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					System.out.println("Number of seats available: "
							+ is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 11: // querying a Car Location
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a car location using id: "
						+ arguments.elementAt(1));
				System.out.println("Car location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "queryCar";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}

					System.out.println("number of Cars at this location: "
							+ is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 12: // querying a Room location
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a room location using id: "
						+ arguments.elementAt(1));
				System.out.println("Room location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "queryRoom";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}

					System.out.println("number of Rooms at this location: "
							+ is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 13: // querying Customer Information
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying Customer information using id: "
						+ arguments.elementAt(1));
				System.out.println("Customer id: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));

					String method = "queryCustomer";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					System.out.println("Customer info:"
							+ (String) iis.readObject());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 14: // querying a flight Price
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a flight Price using id: "
						+ arguments.elementAt(1));
				System.out.println("Flight number: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					flightNum = obj.getInt(arguments.elementAt(2));

					String method = "queryFlightPrice";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(flightNum);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					System.out.println("Price of a seat: " + is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 15: // querying a Car Price
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a car price using id: "
						+ arguments.elementAt(1));
				System.out.println("Car location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "queryCarPrice";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}

					System.out.println("Price of a car at this location: "
							+ is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 16: // querying a Room price
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Querying a room price using id: "
						+ arguments.elementAt(1));
				System.out.println("Room Location: " + arguments.elementAt(2));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					location = obj.getString(arguments.elementAt(2));

					String method = "queryRoomPrice";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}

					System.out.println("Price of Rooms at this location: "
							+ is.readInt());
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 17: // reserve a flight
				if (arguments.size() != 4) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Reserving a seat on a flight using id: "
						+ arguments.elementAt(1));
				System.out.println("Customer id: " + arguments.elementAt(2));
				System.out.println("Flight number: " + arguments.elementAt(3));
				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));
					flightNum = obj.getInt(arguments.elementAt(3));
					String method = "reserveFlight";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					array.add(flightNum);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					
					if (is.readBoolean()) {
						System.out.println("Flight reserved");
					} else {
						System.out.println("Flight could not be reserved");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 18: // reserve a car
				if (arguments.size() != 4) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Reserving a car at a location using id: "
						+ arguments.elementAt(1));
				System.out.println("Customer id: " + arguments.elementAt(2));
				System.out.println("Location: " + arguments.elementAt(3));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));
					location = obj.getString(arguments.elementAt(3));
					String method = "reserveCar";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Car reserved");
					} else {
						System.out.println("Car could not be reserved");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 19: // reserve a room
				if (arguments.size() != 4) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Reserving a room at a location using id: "
						+ arguments.elementAt(1));
				System.out.println("Customer id: " + arguments.elementAt(2));
				System.out.println("Location: " + arguments.elementAt(3));

				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));
					location = obj.getString(arguments.elementAt(3));
					String method = "reserveRoom";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					array.add(location);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("Room reserved");
					} else {
						System.out.println("Room could not be reserved");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;
				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				break;

			case 20: // reserve an Itinerary
				if (arguments.size() < 7) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Reserving an Itinerary using id:"
						+ arguments.elementAt(1));
				System.out.println("Customer id:" + arguments.elementAt(2));
				for (int i = 0; i < arguments.size() - 6; i++)
					System.out.println("Flight number"
							+ arguments.elementAt(3 + i));
				System.out.println("Location for Car/Room booking:"
						+ arguments.elementAt(arguments.size() - 3));
				System.out.println("Car to book?:"
						+ arguments.elementAt(arguments.size() - 2));
				System.out.println("Room to book?:"
						+ arguments.elementAt(arguments.size() - 1));
				try {
					Id = obj.getInt(arguments.elementAt(1));
					int customer = obj.getInt(arguments.elementAt(2));
					Vector flightNumbers = new Vector();
					for (int i = 0; i < arguments.size() - 6; i++)
						flightNumbers.addElement(arguments.elementAt(3 + i));
					location = obj.getString(arguments.elementAt(arguments
							.size() - 3));
					Car = obj
							.getBoolean(arguments.elementAt(arguments.size() - 2));
					Room = obj
							.getBoolean(arguments.elementAt(arguments.size() - 1));

					String method = "itinerary";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(customer);
					array.add(flightNumbers);
					array.add(location);
					array.add(Car);
					array.add(Room);
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) 
					{
						System.out.println("Itinerary Reserved");
					} 
					else 
					{
						try
						{
							System.out.println("Itinerary could not be reserved");
							ArrayList<Object> array1 = new ArrayList<Object>();
							String start = "abort";
							array1.add(start);
							oos.writeObject(array1);
							if (is.readBoolean()) 
							{
								System.out.println("ABORTING because itinerary failed");
							} 
							else 
							{
								System.out.println("itinerary failed, aborting failed");
							}
						}
						catch (Exception e) 
						{
							System.out.println("EXCEPTION:");
							System.out.println(e.getMessage());
							e.printStackTrace();
						}
					}

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 21: // quit the client
				if (arguments.size() != 1) {
					obj.wrongNumber();
					break;
				}
				try {
					ArrayList<Object> array = new ArrayList<Object>();
					String quit = "quit";
					array.add(quit);
					oos.writeObject(array);
					try {
						Thread.sleep(1000);
					} catch (Exception e) {

					}
					is.close();
					clientSocket.close();
				} catch (UnknownHostException e) {
					System.err.println("Trying to connect to unknown host: "
							+ e);
				} catch (IOException e) {
					System.err.println("IOException: " + e);
				}
				System.out.println("Quitting client.");
				System.exit(1);

			case 22: // new Customer given id
				if (arguments.size() != 3) {
					obj.wrongNumber();
					break;
				}
				System.out.println("Adding a new Customer using id:"
						+ arguments.elementAt(1) + " and cid "
						+ arguments.elementAt(2));
				try {
					Id = obj.getInt(arguments.elementAt(1));
					Cid = obj.getInt(arguments.elementAt(2));
					String method = "newCustomerId";
					ArrayList<Object> array = new ArrayList<Object>();
					array.add(method);
					array.add(Id);
					array.add(Cid);
					long time1 = System.currentTimeMillis();
					oos.writeObject(array);
					if(!is.readBoolean()){
						deadLock();
						continue;
					}
					if (is.readBoolean()) {
						System.out.println("new customer id:" + Cid);
					} else {
						System.out.println("Customer could not be created");
					}
					long responseTime = (System.currentTimeMillis() - time1);
					totalResponseTime += responseTime;

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 23: // start()
				if (arguments.size() != 1) {
					System.out.println("Wrong number of arguments");
					obj.wrongNumber();
					break;
				}
				try {
					ArrayList<Object> array = new ArrayList<Object>();
					String start = "start";
					array.add(start);
					oos.writeObject(array);
					System.out.println("Sending start");
					transactionID = is.readInt();
					System.out.println("Transaction id:" + transactionID);

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 24: // commit()
				if (arguments.size() != 1) {
					obj.wrongNumber();
					break;
				}
				try {
					ArrayList<Object> array = new ArrayList<Object>();
					String start = "commit";
					array.add(start);
					oos.writeObject(array);

					if (is.readBoolean()) {
						System.out.println("commit successful");
					} else {
						System.out.println("commit not successful");
					}

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 25: // abort()
				if (arguments.size() != 1) {
					obj.wrongNumber();
					break;
				}
				try {
					ArrayList<Object> array = new ArrayList<Object>();
					String start = "abort";
					array.add(start);
					oos.writeObject(array);

					if (is.readBoolean()) {
						System.out.println("abort successful");
					} else {
						System.out.println("abort not successful");
					}

				} catch (Exception e) {
					System.out.println("EXCEPTION:");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;

			case 26: //shutdown()
				if (arguments.size() != 1) {
					obj.wrongNumber();
					break;
				}
				try 
				{
					ArrayList<Object> array = new ArrayList<Object>();
					String quit = "shutdown";
					array.add(quit);
					oos.writeObject(array);
					if (is.readBoolean()) 
					{
						System.out.println("MW shutdown successful");
						try 
						{
							Thread.sleep(1000);
						} 
						catch (Exception e) 
						{
						}
						is.close();
						clientSocket.close();
						oos.close();
						System.out.println("Quitting client.");
						System.exit(1);
					}
					else 
					{
						System.out.println("Mw shutdown not successful (Transactions still running)");
					}
				}
				catch (UnknownHostException e) 
				{
					System.err.println("Trying to connect to unknown host: "
							+ e);
				} 
				catch (IOException e) 
				{
					System.err.println("IOException: " + e);
				}
				break;
				
				default:
					System.out
					.println("The interface does not support this command.");
					break;
				}// end of switch
			}// end of while(true)
		}
	
		public static void deadLock()
		{
			System.out.println("Deadlock detected\n Transaction was ABORTED!");	
		}

		public Vector parse(String command) {
			Vector arguments = new Vector();
			StringTokenizer tokenizer = new StringTokenizer(command, ",");
			String argument = "";
			while (tokenizer.hasMoreTokens()) {
				argument = tokenizer.nextToken();
				argument = argument.trim();
				arguments.add(argument);
			}
			return arguments;
		}

		public int findChoice(String argument) {
			if (argument.compareToIgnoreCase("help") == 0)
				return 1;
			else if (argument.compareToIgnoreCase("newflight") == 0)
				return 2;
			else if (argument.compareToIgnoreCase("newcar") == 0)
				return 3;
			else if (argument.compareToIgnoreCase("newroom") == 0)
				return 4;
			else if (argument.compareToIgnoreCase("newcustomer") == 0)
				return 5;
			else if (argument.compareToIgnoreCase("deleteflight") == 0)
				return 6;
			else if (argument.compareToIgnoreCase("deletecar") == 0)
				return 7;
			else if (argument.compareToIgnoreCase("deleteroom") == 0)
				return 8;
			else if (argument.compareToIgnoreCase("deletecustomer") == 0)
				return 9;
			else if (argument.compareToIgnoreCase("queryflight") == 0)
				return 10;
			else if (argument.compareToIgnoreCase("querycar") == 0)
				return 11;
			else if (argument.compareToIgnoreCase("queryroom") == 0)
				return 12;
			else if (argument.compareToIgnoreCase("querycustomer") == 0)
				return 13;
			else if (argument.compareToIgnoreCase("queryflightprice") == 0)
				return 14;
			else if (argument.compareToIgnoreCase("querycarprice") == 0)
				return 15;
			else if (argument.compareToIgnoreCase("queryroomprice") == 0)
				return 16;
			else if (argument.compareToIgnoreCase("reserveflight") == 0)
				return 17;
			else if (argument.compareToIgnoreCase("reservecar") == 0)
				return 18;
			else if (argument.compareToIgnoreCase("reserveroom") == 0)
				return 19;
			else if (argument.compareToIgnoreCase("itinerary") == 0)
				return 20;
			else if (argument.compareToIgnoreCase("quit") == 0)
				return 21;
			else if (argument.compareToIgnoreCase("newcustomerid") == 0)
				return 22;
			else if (argument.compareToIgnoreCase("start") == 0)
				return 23;
			else if (argument.compareToIgnoreCase("commit") == 0)
				return 24;
			else if (argument.compareToIgnoreCase("abort") == 0)
				return 25;
			else if (argument.compareToIgnoreCase("shutdown") == 0)
				return 26;
			else
				return 666;

		}

		public void listCommands() {
			System.out
			.println("\nWelcome to the client interface provided to test your project.");
			System.out.println("Commands accepted by the interface are:");
			System.out.println("help");
			System.out
			.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar\ndeleteroom");
			System.out
			.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
			System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
			System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
			System.out.println("nquit");
			System.out
			.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
		}

		public void listSpecific(String command) {
			System.out.print("Help on: ");
			switch (findChoice(command)) {
			case 1:
				System.out.println("Help");
				System.out
				.println("\nTyping help on the prompt gives a list of all the commands available.");
				System.out
				.println("Typing help, <commandname> gives details on how to use the particular command.");
				break;

			case 2: // new flight
				System.out.println("Adding a new Flight.");
				System.out.println("Purpose:");
				System.out.println("\tAdd information about a new flight.");
				System.out.println("\nUsage:");
				System.out
				.println("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
				break;

			case 3: // new Car
				System.out.println("Adding a new Car.");
				System.out.println("Purpose:");
				System.out.println("\tAdd information about a new car location.");
				System.out.println("\nUsage:");
				System.out
				.println("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
				break;

			case 4: // new Room
				System.out.println("Adding a new Room.");
				System.out.println("Purpose:");
				System.out.println("\tAdd information about a new room location.");
				System.out.println("\nUsage:");
				System.out
				.println("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
				break;

			case 5: // new Customer
				System.out.println("Adding a new Customer.");
				System.out.println("Purpose:");
				System.out
				.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
				System.out.println("\nUsage:");
				System.out.println("\tnewcustomer,<id>");
				break;

			case 6: // delete Flight
				System.out.println("Deleting a flight");
				System.out.println("Purpose:");
				System.out.println("\tDelete a flight's information.");
				System.out.println("\nUsage:");
				System.out.println("\tdeleteflight,<id>,<flightnumber>");
				break;

			case 7: // delete Car
				System.out.println("Deleting a Car");
				System.out.println("Purpose:");
				System.out.println("\tDelete all cars from a location.");
				System.out.println("\nUsage:");
				System.out.println("\tdeletecar,<id>,<location>,<numCars>");
				break;

			case 8: // delete Room
				System.out.println("Deleting a Room");
				System.out.println("\nPurpose:");
				System.out.println("\tDelete all rooms from a location.");
				System.out.println("Usage:");
				System.out.println("\tdeleteroom,<id>,<location>,<numRooms>");
				break;

			case 9: // delete Customer
				System.out.println("Deleting a Customer");
				System.out.println("Purpose:");
				System.out.println("\tRemove a customer from the database.");
				System.out.println("\nUsage:");
				System.out.println("\tdeletecustomer,<id>,<customerid>");
				break;

			case 10: // querying a flight
				System.out.println("Querying flight.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain Seat information about a certain flight.");
				System.out.println("\nUsage:");
				System.out.println("\tqueryflight,<id>,<flightnumber>");
				break;

			case 11: // querying a Car Location
				System.out.println("Querying a Car location.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain number of cars at a certain car location.");
				System.out.println("\nUsage:");
				System.out.println("\tquerycar,<id>,<location>");
				break;

			case 12: // querying a Room location
				System.out.println("Querying a Room Location.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain number of rooms at a certain room location.");
				System.out.println("\nUsage:");
				System.out.println("\tqueryroom,<id>,<location>");
				break;

			case 13: // querying Customer Information
				System.out.println("Querying Customer Information.");
				System.out.println("Purpose:");
				System.out.println("\tObtain information about a customer.");
				System.out.println("\nUsage:");
				System.out.println("\tquerycustomer,<id>,<customerid>");
				break;

			case 14: // querying a flight for price
				System.out.println("Querying flight.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain price information about a certain flight.");
				System.out.println("\nUsage:");
				System.out.println("\tqueryflightprice,<id>,<flightnumber>");
				break;

			case 15: // querying a Car Location for price
				System.out.println("Querying a Car location.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain price information about a certain car location.");
				System.out.println("\nUsage:");
				System.out.println("\tquerycarprice,<id>,<location>");
				break;

			case 16: // querying a Room location for price
				System.out.println("Querying a Room Location.");
				System.out.println("Purpose:");
				System.out
				.println("\tObtain price information about a certain room location.");
				System.out.println("\nUsage:");
				System.out.println("\tqueryroomprice,<id>,<location>");
				break;

			case 17: // reserve a flight
				System.out.println("Reserving a flight.");
				System.out.println("Purpose:");
				System.out.println("\tReserve a flight for a customer.");
				System.out.println("\nUsage:");
				System.out
				.println("\treserveflight,<id>,<customerid>,<flightnumber>");
				break;

			case 18: // reserve a car
				System.out.println("Reserving a Car.");
				System.out.println("Purpose:");
				System.out
				.println("\tReserve a given number of cars for a customer at a particular location.");
				System.out.println("\nUsage:");
				System.out
				.println("\treservecar,<id>,<customerid>,<location>,<nummberofCars>");
				break;

			case 19: // reserve a room
				System.out.println("Reserving a Room.");
				System.out.println("Purpose:");
				System.out
				.println("\tReserve a given number of rooms for a customer at a particular location.");
				System.out.println("\nUsage:");
				System.out
				.println("\treserveroom,<id>,<customerid>,<location>,<nummberofRooms>");
				break;

			case 20: // reserve an Itinerary
				System.out.println("Reserving an Itinerary.");
				System.out.println("Purpose:");
				System.out
				.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
				System.out.println("\nUsage:");
				System.out
				.println("\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>,<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
				break;

			case 21: // quit the client
				System.out.println("Quitting client.");
				System.out.println("Purpose:");
				System.out.println("\tExit the client application.");
				System.out.println("\nUsage:");
				System.out.println("\tquit");
				break;

			case 22: // new customer with id
				System.out.println("Create new customer providing an id");
				System.out.println("Purpose:");
				System.out.println("\tCreates a new customer with the id provided");
				System.out.println("\nUsage:");
				System.out.println("\tnewcustomerid, <id>, <customerid>");
				break;

			default:
				System.out.println(command);
				System.out.println("The interface does not support this command.");
				break;
			}
		}

		public void wrongNumber() {
			System.out
			.println("The number of arguments provided in this command are wrong.");
			System.out
			.println("Type help, <commandname> to check usage of this command.");
		}

		public int getInt(Object temp) throws Exception {
			try {
				return (new Integer((String) temp)).intValue();
			} catch (Exception e) {
				throw e;
			}
		}

		public boolean getBoolean(Object temp) throws Exception {
			try {
				return (new Boolean((String) temp)).booleanValue();
			} catch (Exception e) {
				throw e;
			}
		}

		public String getString(Object temp) throws Exception {
			try {
				return (String) temp;
			} catch (Exception e) {
				throw e;
			}
		}

	}

