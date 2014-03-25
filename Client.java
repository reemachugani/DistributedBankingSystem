import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	private static InetAddress IPAddress;
	private static int portNumber;
	private static BufferedReader inFromUser = new BufferedReader(
			new InputStreamReader(System.in));
	private static HashSet<String> validCurrency = new HashSet<String>();
	private static DatagramSocket clientSocket;
	public static int timeoutVal;
	public static boolean monitoring = false;

	public static void main(String args[]) throws Exception {

		openConnection();

		validCurrency.add("SGD");
		validCurrency.add("USD");
		validCurrency.add("INR");
		validCurrency.add("CNY");

		timeoutVal = 5;
		int choice;
		boolean exit = false;

		do {

			System.out.println("1. Open Bank account");
			System.out.println("2. Close Bank account");
			System.out.println("3. Withdraw/Deposit money");
			System.out.println("4. Monitor updates");
			System.out.println("5. Check Balance");
			System.out.println("6. Transfer Funds");
			System.out.println("To exit, enter 0");
			System.out.println();

			String name;
			String currency;
			String password;
			double amount;
			Double initialBalance;
			int accn;

			choice = Integer.parseInt(inFromUser.readLine());
			switch (choice) {

			case 1:
				System.out.println("Enter your name");
				name = inFromUser.readLine();

				System.out.println("Enter the currency for your account");
				currency = getValidCurrency();

				System.out.println("Enter an 8 letter password");
				password = getValidPassword();

				System.out.println("Enter the initial balance in your account");
				initialBalance = Double.parseDouble(inFromUser.readLine());

				createAccount(name, currency, password, initialBalance);
				break;

			case 2:
				System.out.println("Enter your name");
				name = inFromUser.readLine();

				System.out.println("Enter your password");
				password = getValidPassword();

				System.out.println("Enter you account number");
				accn = Integer.parseInt(inFromUser.readLine());

				while(!isValidUserInput(name, password, accn)){
					System.out.println("Enter your name");
					name = inFromUser.readLine();

					System.out.println("Enter your password");
					password = getValidPassword();

					System.out.println("Enter you account number");
					accn = Integer.parseInt(inFromUser.readLine());
				}

				closeAccount(accn);
				break;

			case 3:
				System.out.println("Enter your name");
				name = inFromUser.readLine();

				System.out.println("Enter your password");
				password = getValidPassword();

				System.out.println("Enter you account number");
				accn = Integer.parseInt(inFromUser.readLine());

				while(!isValidUserInput(name, password, accn)){
					System.out.println("Enter your name");
					name = inFromUser.readLine();

					System.out.println("Enter your password");
					password = getValidPassword();

					System.out.println("Enter you account number");
					accn = Integer.parseInt(inFromUser.readLine());
				}

				System.out
						.println("Enter the amount (Positive for deposit/Negative for withdrawal)");
				amount = Double.parseDouble(inFromUser.readLine());

				System.out.println("Enter the currency for your account");
				currency = getValidCurrency();

				updateBalance(accn, amount, currency);
				break;

			case 4:
				System.out.println("Enter monitoring time in seconds");
				int time = Integer.parseInt(inFromUser.readLine());
				monitorUpdates(time);
				break;

			case 5:
				System.out.println("Enter your name");
				name = inFromUser.readLine();

				System.out.println("Enter your password");
				password = getValidPassword();

				System.out.println("Enter you account number");
				accn = Integer.parseInt(inFromUser.readLine());

				while(!isValidUserInput(name, password, accn)){
					System.out.println("Enter your name");
					name = inFromUser.readLine();

					System.out.println("Enter your password");
					password = getValidPassword();

					System.out.println("Enter you account number");
					accn = Integer.parseInt(inFromUser.readLine());
				}
				checkBalanceUser(accn);
				break;

			case 6:
				System.out.println("Enter your name");
				name = inFromUser.readLine();

				System.out.println("Enter your password");
				password = getValidPassword();

				System.out.println("Enter you account number");
				accn = Integer.parseInt(inFromUser.readLine());

				while(!isValidUserInput(name, password, accn)){
					System.out.println("Enter your name");
					name = inFromUser.readLine();

					System.out.println("Enter your password");
					password = getValidPassword();

					System.out.println("Enter you account number");
					accn = Integer.parseInt(inFromUser.readLine());
				}

				System.out.println("Enter the amount to be transferred");
				amount = Double.parseDouble(inFromUser.readLine());

				System.out.println("Enter the currency for your account");
				currency = getValidCurrency();

				System.out.println("Transfer to which account number?");
				Integer transferAccn = Integer.parseInt(inFromUser.readLine());

				transferAmount(accn, amount, currency, transferAccn);
				break;

			case 0:
				exit = true;
				break;

			default:
				System.out.println("Not a valid choice");
				break;
			}

			System.out.println();
		} while (!exit);

		closeConnection();

	}

	public static void transferAmount(int accn, double amount, String currency,
			int transferAccn) throws Exception {
		
		double currBalance = checkBalanceState(accn);

		while(amount < 0){
				System.out.println("Amount to be transferred cannot be negative, Please enter again");
				amount = Double.parseDouble(inFromUser.readLine());
		} 


		if (currBalance == -1) {
			System.out.println("Unexpected Error");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("6|" + accn + "|" + currency + "|" + amount + "|"
				+ currBalance + "|" + transferAccn);

		System.out.println(sb.toString());

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);
	}

	public static void updateBalance(int accn, double amount, String currency)
			throws Exception {

		double currBalance = checkBalanceState(accn);

		if (currBalance == -1) {
			System.out.println("Unexpected Error");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("3|" + accn + "|" + currency + "|" + amount + "|"
				+ currBalance);

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}

	public static void closeAccount(int accn) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("2|" + accn);

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}

	public static void checkBalanceUser(int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("5|" + accn);

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}

	public static double checkBalanceState(int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("5|" + accn);

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);

		String[] returnedArr = receive.split("\\|");

		Integer returnCode = Integer.parseInt(returnedArr[0]);

		if (returnCode == 5) {
			return Double.parseDouble(returnedArr[3]);
		}

		else {
			return -1;
		}
	}

	public static boolean isValidUserInput(String name, String password,
			int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("7|" + name + "|" + password + "|" + accn);
		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);
		String[] returnedArr = receive.split("\\|");
		int returnCode = Integer.parseInt(returnedArr[0]);
		if(returnCode != 7) {
			return false;
		}
		else{ 
			return true;
		}
		
	}

	public static void createAccount(String name, String currency,
			String password, double initalBalance) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("1|" + name + "|" + currency + "|" + password + "|"
				+ initalBalance);

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);

		printOutputString(receive);
	}

	public static void monitorUpdates(int time) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("4|" + time + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

		long t = System.currentTimeMillis();
		long end = t + time * 1000;

		// when monitoring, we don't want the system to timeout if no operation
		// is performed
		monitoring = true;

		clientSocket.setSoTimeout((time + 1) * 1000);
		while (System.currentTimeMillis() < end) {
			receive = receiveData();
			printOutputString(receive);
		}

		monitoring = false;
	}

	public static String sendAndReceiveWithTimeout(String data, int timeout)
			throws Exception {
		clientSocket.setSoTimeout(timeout * 1000);
		String receive;

		while (true) {
			sendData(data);

			try {
				receive = receiveData();
				break;
			} catch (SocketTimeoutException e) {
				System.out.println("Timeout!");
				continue;
			}
		}

		return receive;
	}

	public static String receiveData() throws Exception {

		byte[] receiveData = new byte[2048];
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);

		clientSocket.receive(receivePacket);

		String receive = new String(receivePacket.getData());

		return receive;

	}

	public static void printOutputString(String receive) {

		String[] returnedArr = receive.split("\\|");

		int returnCode = Integer.parseInt(returnedArr[0]);

		switch (returnCode) {
		case 1:
			if(monitoring)
			System.out.println("Account " + returnedArr[1] + " created");
			break;

		case 2:
			System.out.println("Account " + returnedArr[1] + " deleted");
			break;

		case 3:
			System.out.println("Name: " + returnedArr[1]);
			System.out.println("Account Number: " + returnedArr[2]);
			System.out.println("Updated Balance: " + returnedArr[3]);
			break;

		case 4:
			System.out.println(returnedArr[1]);
			break;

		case 5:
			if(monitoring){
				System.out.println("Checking Balance");
			}	

			System.out.println("Name: " + returnedArr[1]);
			System.out.println("Account Number: " + returnedArr[2]);
			System.out.println("Current Balance: " + returnedArr[3]);
			break;

		case 6:
			if(monitoring){
				System.out.println("Transferring amount");
			}	

			System.out.println("Balance remaining after transfer: " + returnedArr[1]);
			//System.out.println("Name: " + returnedArr[1]);
			//System.out.println("Account Number: " + returnedArr[2]);
			//System.out.println("Balance remaining after transfer: " + returnedArr[4]);
			//System.out.println("Amount " + returnedArr[5] +  " transferred to Account no. " + returnedArr[3]);
			break;

		case 7:
			if(!monitoring){
				System.out.println(returnedArr[1]);
			}	
			break;

		default:
			//not printing errors for monitoring condition
			if(!monitoring){
				System.out.println(returnedArr[1]);
			}
		}

		System.out.println();
	}

	public static void sendData(String data) throws Exception {
		byte[] sendData = new byte[2048];
		sendData = data.getBytes();

		// System.out.println(new String(sendData));
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, portNumber);
		clientSocket.send(sendPacket);
	}

	public static void openConnection() throws Exception {
		// Assuming valid input
		/*
		 * System.out.println("Please enter IP address of server"); String IP =
		 * inFromUser.readLine(); IPAddress = InetAddress.getByName(IP);
		 * 
		 * System.out.println("Please enter port number used by server");
		 * portNumber = Integer.parseInt(inFromUser.readLine());
		 */
		clientSocket = new DatagramSocket();
		IPAddress = InetAddress.getByName("172.22.73.79");
		portNumber = 2222;

	}

	public static void closeConnection() {
		clientSocket.close();
	}

	public static String getValidPassword() throws Exception {
		String pass = null;
		do {
			pass = inFromUser.readLine();
		} while (!isValidPassword(pass));

		return pass;
	}

	public static String getValidCurrency() throws Exception {
		String cur = null;
		do {
			cur = inFromUser.readLine();
			cur = cur.toUpperCase();
		} while (!isValidCurrency(cur));

		return cur;
	}

	public static boolean isValidPassword(String password) {

		if (password.length() != 8) {
			System.out.println("Invalid password! Enter an 8 letter password");
			return false;
		}

		return true;
	}

	public static boolean isValidCurrency(String currency) {

		if (!validCurrency.contains(currency)) {
			System.out.println("Invalid currency! Enter from the list below");
			System.out.println(validCurrency.toString());
			return false;
		}

		return true;
	}
}
