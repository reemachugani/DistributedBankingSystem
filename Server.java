import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

public class Server {

	static int lastAccNum;
	static ArrayList<Account> accArr = new ArrayList<Account>();
	static boolean flag = false;
	static long duration;
	static InetAddress flaggedIP;
	static int flaggedPort;
	static HashMap<String, String> map = new HashMap<String, String>();
	static HashMap<String, Long> monitorMap = new HashMap<String, Long>();

	public static void main(String args[]) throws Exception {
		readAccountNumFile();
		loadAccountObjects();

		Timer timer = new Timer();
		final DatagramSocket serverSocket = new DatagramSocket(2222);
		byte[] receiveData = new byte[2048];
		byte[] sendData = new byte[2048];
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			serverSocket.receive(receivePacket);
			String str = new String(receivePacket.getData());
			System.out.println("Received data : " + str);
			receiveData = new byte[2048];

			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();

			// create account
			if (str.startsWith("1")) {
				int accNum = createAccount(str);
				sendData = ("1|" + accNum + "|").getBytes(Charset
						.forName("UTF-8"));
			}

			// delete account
			if (str.startsWith("2")) {
				sendData = deleteAccount(str)
						.getBytes(Charset.forName("UTF-8"));
				writeAccountsToFile();
			}

			// update account
			if (str.startsWith("3")) {
				sendData = updateAccount(str, IPAddress.toString(), port)
						.getBytes(Charset.forName("UTF-8"));
				writeAccountsToFile();
			}

			// add all monitoring clients to the monitorMap
			if (str.startsWith("4")) {
				duration = Long.parseLong(str.split("\\|")[1]) * 1000;
				monitorMap.put(IPAddress.toString().split("\\/")[1] + "|"
						+ port, System.currentTimeMillis() + duration);
				sendData = ("4|Monitoring|").getBytes(Charset.forName("UTF-8"));

			}

			// check balance
			if (str.startsWith("5")) {
				sendData = (checkBalance(Integer.parseInt(str.split("\\|")[1]
						.trim()))).getBytes(Charset.forName("UTF-8"));
			}

			// fund transfer
			if (str.startsWith("6")) {
				sendData = fundTransfer(str, IPAddress.toString(), port)
						.getBytes(Charset.forName("UTF-8"));
				writeAccountsToFile();
			}

			// validate user
			if (str.startsWith("7")) {
				sendData = validateUser(str).getBytes(Charset.forName("UTF-8"));
			}

			// Send response to the client request
			sendDataPacket(serverSocket, sendData, IPAddress, port);

			// Iterate through all monitoring clients, send data, and remove the
			// ones timed-out
			Iterator<Entry<String, Long>> it = monitorMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Long> pairs = (Map.Entry<String, Long>) it
						.next();
				if (System.currentTimeMillis() <= pairs.getValue()) {
					InetAddress IP = InetAddress.getByName(pairs.getKey()
							.split("\\|")[0]);
					int po = Integer.parseInt(pairs.getKey().split("\\|")[1]);
					sendDataPacket(serverSocket, sendData, IP, po);
				} else
					it.remove();
			}
		}
	}

	public static void sendDataPacket(DatagramSocket serverSocket,
			byte[] sendData, InetAddress IPAddress, int port) throws Exception {
		// Thread.sleep(10000);
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, port);
		System.out.println(new String(sendData) + " ''' " + IPAddress + " '' " + port);
		serverSocket.send(sendPacket);
	}

	public static String validateUser(String str) {
		String[] elem = str.split("\\|");
		String name = elem[1].trim();
		char[] pwd = elem[2].trim().toCharArray();
		int accountNum = Integer.parseInt(elem[3].trim());

		for (Account obj : accArr) {
			if (obj.accNumber == accountNum) {
				if (obj.name.equals(name)) {
					if (Arrays.equals(pwd, obj.password))
						return "7|Valid user|";
					else
						return "0|Incorrect password|";
				} else
					return "0|Incorrect user name for this account|";
			}
		}
		return "0|Account No. doesn't exist|";
	}

	public static String checkBalance(int accNum) {
		for (Account obj : accArr) {
			if (obj.accNumber == accNum) {
				System.out.println(obj.balance);
				return "5|" + obj.name + "|" + obj.accNumber + "|"
						+ obj.balance + "|";
			}
		}
		return "0|Something went wrong while checking balance|";
	}

	public static String deleteAccount(String str) {
		int accNum = Integer.parseInt(str.split("\\|")[1].trim());
		boolean del = false;
		for (Account obj : accArr) {
			if (accNum == obj.accNumber) {
				accArr.remove(obj);
				break;
			}
		}
		return "2|" + accNum + "|";
	}

	public static String updateAccount(String str, String ipAdd, int port) {
		if (map.get(ipAdd + port) != null) {
			String[] arr = map.get(ipAdd + port).split("\\|\\|");
			if (arr[0].equals(str)) {
				System.out.println("executed before");
				return arr[1];
			}
		}
		String msg;
		String[] elem = str.split("\\|");

		for (Account obj : accArr) {
			if (obj.accNumber == Integer.parseInt(elem[1])) {
				if (!elem[2].equals(obj.currency.name())) {
					msg = "0|Currency mismatch|";
					map.put(ipAdd + port, str + "||" + msg);
					return msg;
				}
				double amt = Double.parseDouble(elem[3]);
				if (amt < 0) {
					if (obj.getBalance() < Math.abs(amt)) {
						msg = "0|Not enough balance|";
						map.put(ipAdd + port, str + "||" + msg);
						return msg;
					}
				}
				double curBal = obj.getBalance() + amt;
				obj.setBalance(curBal);
				msg = "3|" + obj.name + "|" + obj.accNumber + "|" + curBal
						+ "|";
				map.put(ipAdd + port, str + "||" + msg);
				return msg;
			}
		}
		msg = "0|Something went wrong during balance update|";
		map.put(ipAdd + port, str + "||" + msg);
		return msg;
	}

	public static String fundTransfer(String str, String ipAdd, int port) {
		// accNum, curr, amt, curBal, transferAccNum
		if (map.get(ipAdd + port) != null) {
			String[] arr = map.get(ipAdd + port).split("\\|\\|");
			if (arr[0].equals(str)) {
				System.out.println("executed before");
				return arr[1];
			}
		}
		String msg;
		String[] elem = str.split("\\|");

		for (Account obj : accArr) {
			if (obj.accNumber == Integer.parseInt(elem[5].trim())) {
				if (!elem[2].equals(obj.currency.name())) {
					msg = "0|Different currency. Transfer not possible!|";
					map.put(ipAdd + port, str + "||" + msg);
					return msg;
				}
				double amt = Double.parseDouble(elem[3]);

				if (Double.parseDouble(elem[4].trim()) < amt) {
					msg = "0|Not enough balance|";
					map.put(ipAdd + port, str + "||" + msg);
					return msg;
				}

				String sender = updateBalance(Integer.parseInt(elem[1].trim()),
						amt * -1);
				String receiver = updateBalance(
						Integer.parseInt(elem[5].trim()), amt);

				Double curBal = Double.parseDouble(elem[4].trim())
						- Double.parseDouble(elem[3].trim());
				String amtDisp = obj.currency.name() + curBal + "|"
						+ obj.currency.name() + elem[3];
				msg = "6|" + sender + "|" + elem[1] + "|" + elem[5] + "|"
						+ amtDisp + "|";
				// msg = "6|" + obj.currency.name()+ ". " + curBal;
				// System.out.println(msg);
				map.put(ipAdd + port, str + "||" + msg);
				return msg;
			}
		}
		msg = "0|Recepient account does not exist!";
		map.put(ipAdd + port, str + "||" + msg);
		return msg;
	}

	public static String updateBalance(int accNum, double amount) {
		for (Account obj : accArr) {
			if (obj.accNumber == accNum) {
				double curBal = obj.getBalance() + amount;
				obj.setBalance(curBal);
				return obj.name;
			}
		}
		return "";
	}

	public static int createAccount(String str) throws IOException {
		// System.out.println(str);
		String[] elem = str.split("\\|");
		String name = elem[1];
		String cur = elem[2];
		char[] pwd = elem[3].toCharArray();
		// System.out.println(elem[1] + ":" + elem[2] + ":" + elem[3] + ":" +
		// elem[4]);
		double initialAmt = Double.parseDouble(elem[4]);
		int accNum = ++lastAccNum;
		appendAccNum(accNum);
		Account obj = new Account(name, cur, pwd, initialAmt, accNum);
		appendAccountObj(obj);
		accArr.add(obj);
		System.out.println(accNum);
		return accNum;
	}

	public static void appendAccNum(int accNum) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
				"AccNum", true)));
		out.println(accNum);
		out.close();
	}

	public static void appendAccountObj(Account obj) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
				"AccountObjects", true)));
		out.println(obj.name);
		out.println(obj.currency.name());
		out.println(obj.password);
		out.println(obj.balance);
		out.println(obj.accNumber);
		out.close();
	}

	public static void readAccountNumFile() throws IOException {
		// create file if it doesn't exist
		File file = new File("AccNum");
		if (!file.exists()) {
			file.createNewFile();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		String line;
		lastAccNum = 0;
		while ((line = br.readLine()) != null && !line.equals("")) {
			lastAccNum = Integer.parseInt(line);
		}
		br.close();
	}

	public static void loadAccountObjects() throws IOException {
		// create file if it doesn't exist
		File file = new File("AccountObjects");
		if (!file.exists()) {
			file.createNewFile();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		String line;
		while ((line = br.readLine()) != null && !line.equals("")) {
			String cur = br.readLine().trim();
			char[] pwd = br.readLine().trim().toCharArray();
			double amt = Double.parseDouble(br.readLine().trim());
			int accNum = Integer.parseInt(br.readLine().trim());
			Account obj = new Account(line, cur, pwd, amt, accNum);
			accArr.add(obj);
		}
		br.close();
	}

	public static void writeAccountsToFile() throws IOException {
		PrintWriter writer = new PrintWriter("AccountObjects");
		for (Account obj : accArr) {
			appendAccountObj(obj);
		}
		writer.close();
	}
}
