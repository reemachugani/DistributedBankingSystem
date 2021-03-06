import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class Client {

	private static InetAddress IPAddress;
	private static int portNumber;
	private static BufferedReader inFromUser = new BufferedReader(
			new InputStreamReader(System.in));
	private static HashSet<String> validCurrency = new HashSet<String>();
	private static DatagramSocket clientSocket;
	public static int timeoutVal;
	public static boolean monitoring = false;
	public static String lastResponse = "";

	public static void main(String args[]) throws Exception {

		openConnection();

		validCurrency.add("SGD");
		validCurrency.add("USD");
		validCurrency.add("INR");
		validCurrency.add("CNY");

		timeoutVal = 5;
		int choice;
		boolean exit = false;

		//prints menu and asks user for input
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
				name = getName();

				System.out.println("Enter the currency for your account");
				System.out.println("Choose one from the following: "+ validCurrency.toString());

				currency = getValidCurrency();

				System.out.println("Enter an 8 letter password");
				password = getValidPassword();

				System.out.println("Enter the initial balance in your account");
				initialBalance = getValidAmount();

				while (initialBalance < 0) {
					System.out
							.println("Balance cannot be negative, Enter a positive value");
					initialBalance = getValidAmount();
				}

				createAccount(name, currency, password, initialBalance);
				break;

			case 2:
				accn = verifyUser();
				closeAccount(accn);
				break;

			case 3:
				accn = verifyUser();
				System.out
						.println("Enter the amount (Positive for deposit/Negative for withdrawal)");
				amount = getValidAmount();

				System.out.println("Enter the currency for your account");
				System.out.println("Choose one from the following: "+ validCurrency.toString());
				currency = getValidCurrency();

				updateBalance(accn, amount, currency);
				break;

			case 4:
				System.out.println("Enter monitoring time in seconds");
				int time = getValidTime();
				monitorUpdates(time);
				break;

			case 5:
				accn = verifyUser();
				checkBalanceUser(accn);
				break;

			case 6:
				accn = verifyUser();
				System.out.println("Enter the amount to be transferred");
				amount = getValidAmount();

				System.out.println("Enter the currency for your account");
				System.out.println("Choose one from the following: "+ validCurrency.toString());
				currency = getValidCurrency();

				System.out.println("Transfer to which account number?");
				int transferAccn = getValidAccountNumber();

				transferAmount(accn, amount, currency, transferAccn);
				break;

			case 0:
				exit = true;
				System.out.println("Bye!");
				break;

			default:
				System.out.println("Not a valid choice");
				break;
			}

			System.out.println();
		} while (!exit);

		closeConnection();

	}

	//sends the user input to create account to server and prints reply message
	public static void createAccount(String name, String currency,
			String password, double initalBalance) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("1|" + name + "|" + currency + "|" + password + "|"
				+ initalBalance + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);

		printOutputString(receive);
	}

	//requests server to close account with account number specified
	public static void closeAccount(int accn) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("2|" + accn + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}

	//sends a request to server to update balance in account by amount entered
	public static void updateBalance(int accn, double amount, String currency)
			throws Exception {

		double currBalance = checkBalanceState(accn);

		if (currBalance == -1) {
			System.out.println("Unexpected Error");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("3|" + accn + "|" + currency + "|" + amount + "|"
				+ currBalance + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}	

	//sends message to server to monitor updates of accounts for the time entered
	public static void monitorUpdates(int time) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("4|" + time + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

		long t = System.currentTimeMillis();
		long end = t + time * 1000;

		monitoring = true;
		clientSocket.setSoTimeout(time * 1000);

		while (System.currentTimeMillis() < end) {
			try {
				receive = receiveData();
				printOutputString(receive);
				long remainingTime = end - System.currentTimeMillis();
				//System.out.println((int) remainingTime);
				//updating time left
				clientSocket.setSoTimeout((int) remainingTime);
			} catch (SocketTimeoutException e) {
				System.out.println("Stopped monitoring");
				break;
			}

		}
		monitoring = false;
	}

	// Checks balance for account specified by user nad displays the received result
	public static void checkBalanceUser(int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("5|" + accn + "|");

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);

	}	

	//this method is used to maintain state for non-idempotent functions such as transfer funds
	// this does not print the balance only sends to server as the current state
	public static double checkBalanceState(int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("5|" + accn + "|");

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

	// performs errorc checks and sends a request to server to transfer amount to specified account
	public static void transferAmount(int accn, double amount, String currency,
			int transferAccn) throws Exception {

		double currBalance = checkBalanceState(accn);

		while (amount < 0) {
			System.out
					.println("Amount to be transferred cannot be negative, Please enter again");
			amount = Double.parseDouble(inFromUser.readLine());
		}

		while (transferAccn == accn) {
			System.out
					.println("You cannot tranfer to your own Account, Please enter a different Account No.");
			transferAccn = Integer.parseInt(inFromUser.readLine());
		}

		if (currBalance == -1) {
			System.out.println("Unexpected Error");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("6|" + accn + "|" + currency + "|" + amount + "|"
				+ currBalance + "|" + transferAccn + "|");

		// System.out.println(sb.toString());

		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);
	}

	// sends the String data to server by converting to bytes
	public static void sendData(String data) throws Exception {
		byte[] sendData = new byte[2048];
		sendData = data.getBytes(Charset.forName("UTF-8"));

		// System.out.println(new String(sendData));
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, portNumber);
		clientSocket.send(sendPacket);
	}


	//receives the data from server in bytes anc converts to string
	public static String receiveData() throws Exception {

		byte[] receiveData = new byte[2048];
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);

		clientSocket.receive(receivePacket);

		String receive = new String(receivePacket.getData());

		return receive;

	}

	//methods which ensures the at-least once semantics
	//sends the data until reply is received if timeout occurs
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
				System.out
						.println("Timeout! No reply from server. Trying again..");
				continue;
			}
		}

		return receive;
	}

	//formats and prints the responses received from the server
	public static void printOutputString(String receive) {

		if (lastResponse.equals(receive)) {
			return;
		}

		lastResponse = receive;

		System.out.println();

		String[] returnedArr = receive.split("\\|");

		int returnCode = Integer.parseInt(returnedArr[0]);

		switch (returnCode) {
		case 1:
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
			if (!monitoring) {
				System.out.println(returnedArr[1]);
			}
			break;

		case 5:
			if (monitoring) {
				System.out.println("Checking Balance");
			}

			System.out.println("Name: " + returnedArr[1]);
			System.out.println("Account Number: " + returnedArr[2]);
			System.out.println("Current Balance: " + returnedArr[3]);
			break;

		case 6:
			if (monitoring) {
				System.out.println("Transferring amount");
			}

			// System.out.println("Balance remaining after transfer: "+
			// returnedArr[1]);
			System.out.println("Name: " + returnedArr[1]);
			System.out.println("Account Number: " + returnedArr[2]);
			System.out.println("Balance remaining after transfer: "
					+ returnedArr[4]);
			System.out.println("Amount " + returnedArr[5]
					+ " transferred to Account no. " + returnedArr[3]);
			break;

		case 7:
			if (!monitoring) {
				System.out.println(returnedArr[1]);
			}
			break;

		default:
			// not printing errors for monitoring condition
			if (!monitoring) {
				System.out.println(returnedArr[1]);
			}
		}

		System.out.println();
	}

	//opens connection of client and initializes IP and port or server
	public static void openConnection() throws Exception {
		// Assuming valid input
		
		System.out.println("Please enter IP address of server"); String IP =
		inFromUser.readLine(); IPAddress = InetAddress.getByName(IP);
		  
		System.out.println("Please enter port number used by server");
		portNumber = Integer.parseInt(inFromUser.readLine());
		
		clientSocket = new DatagramSocket();
		//IPAddress = InetAddress.getByName("192.168.0.13");
		//portNumber = 2222;

	}

	public static void closeConnection() {
		clientSocket.close();
	}

	//gets the input for verification from user and returns the account number
	// account number is used to call the banks functions
	public static int  verifyUser() throws Exception{
		System.out.println("Enter your name");
		String name = getName();

		System.out.println("Enter your password");
		String password = getValidPassword();

		System.out.println("Enter your account number");
		int accn = getValidAccountNumber();

		while (!isValidUserInput(name, password, accn)) {
			System.out.println("Enter your name");
			name = getName();

			System.out.println("Enter your password");
			password = getValidPassword();

			System.out.println("Enter your account number");
			accn = getValidAccountNumber();
		}

		return accn;
	}

	//gets name of user and checks for validity
	public static String getName() throws Exception {
		String name = inFromUser.readLine();
		while (name.contains("|")) {
			System.out
					.println("Cannot use the '|' symbol in Name, Enter again");
			name = inFromUser.readLine();
		}

		name = name.toLowerCase();

		StringBuffer res = new StringBuffer();

		String[] strArr = name.split(" ");
		for (String str : strArr) {
			char[] stringArray = str.trim().toCharArray();
			stringArray[0] = Character.toUpperCase(stringArray[0]);
			str = new String(stringArray);

			res.append(str + " ");
		}

		return res.toString().trim();
	}

	// get password from user and checks it
	public static String getValidPassword() throws Exception {
		String pass = null;
		do {
			pass = inFromUser.readLine();
		} while (!isValidPassword(pass));

		return pass;
	}

	// get currency from user and checks it
	public static String getValidCurrency() throws Exception {
		String cur = null;
		do {
			cur = inFromUser.readLine();
			cur = cur.toUpperCase();
		} while (!isValidCurrency(cur));

		return cur;
	}

	// ensures time entered is valid
	public static int getValidTime() throws Exception {
		int time = Integer.MAX_VALUE;
		do {
			try {
				time = Integer.parseInt(inFromUser.readLine());
				if (time <= 0) {
					System.out.println("Time should be positive, Enter again");
				}
			} catch (Exception e) {
				System.out
						.println("Time should be less than 10000 seconds, Enter again");
			}
		} while (time > 10000 || time <= 0);

		return time;
	}

	// ensures amount entered is valid
	public static double getValidAmount() throws Exception {
		double amount = Double.MAX_VALUE;
		do {
			try {
				amount = Double.parseDouble(inFromUser.readLine());
			} catch (Exception e) {
				System.out
						.println("Enter a valid number, (Absolute value less than 1 million)");
			}
		} while (Math.abs(amount) > 1000000);

		return amount;
	}

	// ensures account number entered is valid
	public static int getValidAccountNumber() throws Exception {
		int accn = Integer.MAX_VALUE;
		do {
			try {
				accn = Integer.parseInt(inFromUser.readLine());
			} catch (Exception e) {
				System.out.println("Enter a valid Account Number");
			}
		} while (accn > 1000000);

		return accn;
	}

	//checks whether the user credentials are valid to access data in account
	public static boolean isValidUserInput(String name, String password,
			int accn) throws Exception {

		StringBuilder sb = new StringBuilder();
		sb.append("7|" + name + "|" + password + "|" + accn + "|");
		String receive = sendAndReceiveWithTimeout(sb.toString(), timeoutVal);
		printOutputString(receive);
		String[] returnedArr = receive.split("\\|");
		int returnCode = Integer.parseInt(returnedArr[0]);
		if (returnCode != 7) {
			return false;
		} else {
			return true;
		}

	}

	// ensures password entered is valid
	public static boolean isValidPassword(String password) {

		if (password.length() != 8 || password.contains("|")) {
			System.out.println("Invalid password! Enter an 8 letter password "
					+ "(Cannot contain the '|'symbol)");
			return false;
		}

		return true;
	}

	// ensures currency entered is valid
	public static boolean isValidCurrency(String currency) {

		if (!validCurrency.contains(currency)) {
			System.out.println("Invalid currency! Enter from the list below");
			System.out.println(validCurrency.toString());
			return false;
		}

		return true;
	}
}
