import java.io.*;
import java.net.*;
import java.security.*;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import java.rmi.Naming;


public class MindCatchServer implements Runnable {

	String runRoot= "C:/Users/Gram/eclipse-workspace/MindChat/bin/";  // ����ڴ��� ���缭 SSLSocketServerKey�� ������ּ���
	String ksName = runRoot+".keystore/SSLSocketServerKey";

	KeyStore kStore = null;
	KeyManagerFactory kmFactory = null; 
	char keyStorePass[] = "123456".toCharArray();
	char keyPass[] = "123456".toCharArray();

	SSLContext sslContext = null;
	SSLServerSocketFactory sslFactory = null;
	SSLServerSocket sslservSock = null;

	private ServerRunnable clients[] = new ServerRunnable[6];
	public int clientCount = 0;
	public Game_func RMI_func = null;
	private int Port = -1;
	//---------------------------------------------------------------

	public MindCatchServer (int port) {
		this.Port = port;

	}
	//---------------------------------------------------------------

	public void run() {
		try {
			System.out.println(clients.length+"���� ������ �� �ֽ��ϴ�.");
			RMI_func = new Game_Impl(clients);		 
			Naming.rebind("rmi://"+"127.0.0.1"+":1099/"+"ser", RMI_func); 	 //�������� C��ü�� ���� rmi������ ���� �־���.  

			kStore = KeyStore.getInstance("JKS");
			kStore.load(new FileInputStream(ksName), keyStorePass);

			kmFactory = KeyManagerFactory.getInstance("SunX509");
			kmFactory.init(kStore, keyPass);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmFactory.getKeyManagers(), null, null);

			sslFactory = sslContext.getServerSocketFactory();
			sslservSock = (SSLServerSocket) sslFactory.createServerSocket(Port);
		
			System.out.println ("Server started: socket created on " + Port);

			System.out.println("���ε�ĳġ ���� �����߽��ϴ�.");

			while (true) {
				addClient(sslservSock);
			}

		} catch (SSLException e) {
			System.out.println("SSL���� ������ �߻��߽��ϴ�. ���Ḧ �մϴ�.");
		} catch (BindException e) {
			System.out.println("���� "+Port+" ��Ʈ ��ȣ�� bind�� ���� ���մϴ�.");
		} catch (IOException e) {
			System.out.println(e);
		} catch (Exception e) {
			System.out.println("�׿ܿ� ������ �߻��߽��ϴ�.");
			System.out.println(e);

		} finally {
			try {
				if (sslservSock != null) {
					sslservSock.close();
				}	
			} catch (Exception i) {
				System.out.println(i);
			}
		}
	}
	//---------------------------------------------------------------

	public int whoClient(int clientID) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() == clientID)
				return i;
		return -1;
	}
	//---------------------------------------------------------------

	public void addClient(SSLServerSocket serverSocket) {
		SSLSocket clientSocket = null;
		try {
			clientSocket = (SSLSocket)serverSocket.accept(); //������ �صξ����  �߰��� ���� ���ϵ��� �� ������ �����Ҽ��ִ�.

			if (clientCount < clients.length) { 
				clients[clientCount] = new ServerRunnable(this, clientSocket);
				new Thread(clients[clientCount]).start();
				clientCount++;
				System.out.println ("Client connected: " + clientSocket.getPort() +", CurrentClient: " + clientCount);
			} else {
				//SSLSocket dummySocket = (SSLSocket)serverSocket.accept(); �̷��� �ϸ� ���߿� ���� ���Ͽ� ���ؼ� ����� ó���� ���Ѵ�.
				ServerRunnable dummyRunnable = new ServerRunnable(this, clientSocket);
				new Thread(dummyRunnable);
				dummyRunnable.out.println(clientSocket.getPort() + "���� �ο���"+clientCount+"���� ��� �� �� �����ϴ�. ");
				System.out.println("Client refused: maximum connection " + clients.length + " reached.");
				dummyRunnable.close();
			}
		}
		catch(IOException i) {
			System.out.println ("Accept() fail: "+i);
		}

	}
	//---------------------------------------------------------------

	public synchronized void delClient(int clientID) {
		int pos = whoClient(clientID);
		String name = null;
		
		for(int i = 0; i < clientCount; i++) 
			if(clients[i].getClientID() == clientID) 
				name = clients[i].clientName;
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getClientID() != clientID) {
				clients[i].out.println("000#"+name);
			}
		
		ServerRunnable endClient = null;
		if (pos >= 0) {
			endClient = clients[pos];
			if (pos < clientCount-1)
				for (int i = pos+1; i < clientCount; i++)
					clients[i-1] = clients[i];
			clientCount--;
			System.out.println("Client removed: " + clientID + " at clients[" + pos +"], CurrentClient: " + clientCount);
			endClient.close();
		}
	}
	//---------------------------------------------------------------
	
	public void putClient(int clientID, String inputLine, int ver) {
		String name = null;

		switch(ver) {

		case 1:	//�Ϲ� ��ȭ
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("100#"+"<"+name+"> :"+inputLine);
					}
				break;
		
		case 2:	// ���� �޽���
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("200#"+name+"*"+inputLine);
					}
				break;

		case 3:	// ���� �޽���
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("300#"+name);
					}
				break;
			
		case 4:	// ������ �ٲ� �޽���
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("400#"+name);
					}
				break;
			
		case 5:	// �׸��� �׷��ִ� �޽���
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("500#"+inputLine);
					}
				break;
				
		case 6:	// ���찳�� ������ �˷���
				for (int i = 0; i < clientCount; i++)
						clients[i].out.println("600#"+inputLine);
					
				break;
			
		case 7:	// ���� ������ �˷���
				for (int i = 0; i < clientCount; i++)
						clients[i].out.println("700#"+inputLine);
					
				break;
				
		case 8:	// ���� �������� �˷���
				int whonum = Integer.parseInt(inputLine);
				
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == whonum) 
						name = clients[i].clientName;
					
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() == whonum) 
						clients[i].out.println("800#"+ name);
					else 
						clients[i].out.println("900#"+ name);
					
				break;		
		
		/*case 9:	// �ٸ� ����� �ϱ⸦ �ٶ�
				int whoturn = Integer.parseInt(inputLine);
			
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == whoturn) 
						name = clients[i].clientName;
				
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() == whoturn) 
						clients[i].out.println("800#"+ name);
					else 
						clients[i].out.println("900#"+ name);
				
				break;*/
		
		}
		
	}
	//---------------------------------------------------------------

	
	
	//main �Լ�
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: Classname ServerPort");
			System.exit(1);
		}
		int Port = Integer.parseInt(args[0]);

		new Thread(new MindCatchServer(Port)).start();
	}
}
/*-----------------------------------------------------------------------------------------*/



/*-----------------------------------------------------------------------------------------*/
class ServerRunnable implements Runnable {
	protected MindCatchServer chatServer = null;
	protected SSLSocket clientSocket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;
	public int clientID = -1;
	public String clientName = null;

	public ServerRunnable (MindCatchServer server, SSLSocket socket) {
		this.chatServer = server;
		this.clientSocket = socket;
		clientID = clientSocket.getPort();
		try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream()));

		}catch(IOException i) {

		}
	}
	public void run() {
		try {
			String inputLine;
			StringTokenizer Token;
			while((inputLine = in.readLine())!=null) {
				Token = new StringTokenizer(inputLine, "#");
				String Signal=null, message=null;
				if(Token.hasMoreElements())
					Signal = Token.nextToken();
				if(Token.hasMoreElements())
					message = Token.nextToken();
				if(Signal==null || message==null) continue;

				if(Signal.equalsIgnoreCase("100")) {
					if(clientName == null) {
						clientName = message;
						continue;
					}
					chatServer.putClient(getClientID(), message, 1);
					continue;
				}
				else if(Signal.equalsIgnoreCase("200")) {
					chatServer.putClient(getClientID(), message, 2);
					continue;
				}
				else if(Signal.equalsIgnoreCase("300")) {
					chatServer.putClient(getClientID(), message, 3);
					continue;
				}
				else if(Signal.equalsIgnoreCase("400")) {
					chatServer.putClient(getClientID(), message, 4);
					continue;
				}
				else if(Signal.equalsIgnoreCase("500")) {
					chatServer.putClient(getClientID(), message, 5);
					continue;
				}
				else if(Signal.equalsIgnoreCase("600")) {
					chatServer.putClient(getClientID(), message, 6);
					continue;
				}
				else if(Signal.equalsIgnoreCase("700")) {
					chatServer.putClient(getClientID(), message, 7);
					continue;
				}
				else if(Signal.equalsIgnoreCase("800")) {
					chatServer.putClient(getClientID(), message, 8);
					continue;
				}
				

			}
			chatServer.delClient(getClientID());
		} catch(SocketTimeoutException ste) {
			System.out.println("Socket timeout Occurred, force close() :"+getClientID());
			chatServer.delClient(getClientID());
		} catch(IOException e) {
			chatServer.delClient(getClientID());
		}
	}
	public int getClientID() {
		return clientID;
	}

	public void close() {
		try {
			if(in != null) in.close();
			if(out != null) out.close();
			if(clientSocket != null) clientSocket.close();
		}catch(IOException i) {

		}
	}


}
/*-----------------------------------------------------------------------------------------*/

