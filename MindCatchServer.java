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

	String runRoot= "C:/Users/Gram/eclipse-workspace/MindChat/bin/";  // 사용자님의 맟춰서 SSLSocketServerKey를 만들어주세요
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
			System.out.println(clients.length+"명이 참가할 수 있습니다.");
			RMI_func = new Game_Impl(clients);		 
			Naming.rebind("rmi://"+"127.0.0.1"+":1099/"+"ser", RMI_func); 	 //서버에서 C객체를 만들어서 rmi영역에 던져 주었다.  

			kStore = KeyStore.getInstance("JKS");
			kStore.load(new FileInputStream(ksName), keyStorePass);

			kmFactory = KeyManagerFactory.getInstance("SunX509");
			kmFactory.init(kStore, keyPass);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmFactory.getKeyManagers(), null, null);

			sslFactory = sslContext.getServerSocketFactory();
			sslservSock = (SSLServerSocket) sslFactory.createServerSocket(Port);
		
			System.out.println ("Server started: socket created on " + Port);

			System.out.println("마인드캐치 서버 동작했습니다.");

			while (true) {
				addClient(sslservSock);
			}

		} catch (SSLException e) {
			System.out.println("SSL에서 문제가 발생했습니다. 종료를 합니다.");
		} catch (BindException e) {
			System.out.println("현재 "+Port+" 포트 번호에 bind를 하지 못합니다.");
		} catch (IOException e) {
			System.out.println(e);
		} catch (Exception e) {
			System.out.println("그외에 문제가 발생했습니다.");
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
			clientSocket = (SSLSocket)serverSocket.accept(); //밖으로 해두어야지  중간에 나간 소켓들을 더 온전히 관리할수있다.

			if (clientCount < clients.length) { 
				clients[clientCount] = new ServerRunnable(this, clientSocket);
				new Thread(clients[clientCount]).start();
				clientCount++;
				System.out.println ("Client connected: " + clientSocket.getPort() +", CurrentClient: " + clientCount);
			} else {
				//SSLSocket dummySocket = (SSLSocket)serverSocket.accept(); 이렇게 하면 도중에 나간 소켓에 대해서 제대로 처리를 못한다.
				ServerRunnable dummyRunnable = new ServerRunnable(this, clientSocket);
				new Thread(dummyRunnable);
				dummyRunnable.out.println(clientSocket.getPort() + "지금 인원수"+clientCount+"명은 들어 올 수 없습니다. ");
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

		case 1:	//일반 대화
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("100#"+"<"+name+"> :"+inputLine);
					}
				break;
		
		case 2:	// 정답 메시지
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("200#"+name+"*"+inputLine);
					}
				break;

		case 3:	// 오답 메시지
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("300#"+name);
					}
				break;
			
		case 4:	// 문제를 바꾼 메시지
				for(int i = 0; i < clientCount; i++) 
					if(clients[i].getClientID() == clientID) 
						name = clients[i].clientName;
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("400#"+name);
					}
				break;
			
		case 5:	// 그림을 그려주는 메시지
				for (int i = 0; i < clientCount; i++)
					if (clients[i].getClientID() != clientID) {
						clients[i].out.println("500#"+inputLine);
					}
				break;
				
		case 6:	// 지우개를 썼음을 알려줌
				for (int i = 0; i < clientCount; i++)
						clients[i].out.println("600#"+inputLine);
					
				break;
			
		case 7:	// 펜을 썼음을 알려줌
				for (int i = 0; i < clientCount; i++)
						clients[i].out.println("700#"+inputLine);
					
				break;
				
		case 8:	// 누가 시작인지 알려줌
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
		
		/*case 9:	// 다른 사람이 하기를 바람
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

	
	
	//main 함수
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

