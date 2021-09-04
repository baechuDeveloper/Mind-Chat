import java.io.*;
import java.util.StringTokenizer;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.BindException;
import java.rmi.Naming;
import java.rmi.RemoteException;



public class MindCatchClient {

	static String Server = "";
	static int Port = 0000;

	static SSLSocket chatSocket = null;
	static SSLSocketFactory sslFactory = null;

	static Game_func RMI_func = null;

	static private Client_UI UI;

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: Classname ServerName ServerPort");
			System.exit(1);
		}
		Server = args[0];
		Port = Integer.parseInt(args[1]);

		try {
			System.setProperty("javax.net.ssl.trustStore", "trustedcerts");
			System.setProperty("javax.net.ssl.trustStorePassword", "123456");

			sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			chatSocket = (SSLSocket) sslFactory.createSocket(Server, Port);

			String[] supported = chatSocket.getSupportedCipherSuites();
			chatSocket.setEnabledCipherSuites(supported);

			RMI_func = (Game_func)Naming.lookup("rmi://"+"127.0.0.1"+"/"+"ser");
			//chatSocket.startHandshake(); �̹� �� ���� ������ �ֱ⶧���� �߰������� ���� �ʾҽ��ϴ�.

			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						UI = new Client_UI(chatSocket, RMI_func);
						UI.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			System.out.println("������ �����߽��ϴ�.");

		} catch (BindException b) {
			System.out.println("���� "+Port+" ��Ʈ ��ȣ�� bind�� ���� ���մϴ�.");
			System.exit(1);
		} catch (IOException i) {
			System.out.println(i);
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
/*-----------------------------------------------------------------------------------------*/


/*-----------------------------------------------------------------------------------------*/

class Client_UI extends JFrame{

	private Container 	container;  // MindCatch ������ ��Ī �ϵ��� ���� �����̳�.

	private GrimPane 	grimPanel;
	private JTextField 	textField;
	private JTextPane 	chatPanel;

	private Game_func 	RMI_func;
	private SSLSocket 	chatSocket;
	private StringBuilder 	allText;

	private String inputText = "";
	private String MyName = "";
	private PrintWriter out = null; 
	private boolean icandraw = true;
	private boolean gameRunning = false;

	public Client_UI(SSLSocket chatSocket ,Game_func RMI_func) {
		this.chatSocket = chatSocket;
		this.RMI_func = RMI_func;
		initialize();
	}

	private void initialize() {
		setTitle("���ε�ĳġ");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		container = getContentPane(); 
		container.setBackground(Color.darkGray);
		container.setLayout(null);	
		setSize(600,900);

		grimPanel = new GrimPane();
		grimPanel.setBackground(Color.WHITE);
		grimPanel.setBounds(20, 35, 540, 355);
		container.add(grimPanel);

		JButton sendButton = new JButton("������");
		sendButton.setBounds(450, 700, 75, 27);
		sendButton.addActionListener(new applyButton());
		container.add(sendButton);

		JButton eraseButton = new JButton("���찳");
		eraseButton.setBounds(30, 740, 75, 27);
		eraseButton.addActionListener(new eraseButton());
		container.add(eraseButton);

		JButton penButton = new JButton("��");
		penButton.setBounds(110, 740, 75, 27);
		penButton.addActionListener(new penButton());
		container.add(penButton);

		JButton startButton = new JButton("���� ����");
		startButton.setBounds(220, 740, 90, 27);
		startButton.addActionListener(new startButton());
		container.add(startButton);

		JButton passButton = new JButton("���� �ѱ��");
		passButton.setBounds(220, 775, 110, 27);
		passButton.addActionListener(new passButton());
		container.add(passButton);

		JButton queButton = new JButton("�ٸ� ����");
		queButton.setBounds(315, 740, 90, 27);
		queButton.addActionListener(new queButton());
		container.add(queButton);

		JButton answerButton = new JButton("�� ��!");
		answerButton.setBounds(450, 740, 75, 27);
		answerButton.addActionListener(new answerButton());
		container.add(answerButton);

		textField = new JTextField();
		textField.setBounds(30, 700, 405, 24);
		textField.setColumns(10);
		container.add(textField);

		chatPanel = new JTextPane();
		chatPanel.setText("�̸��� �Է����ּ���");
		chatPanel.setBounds(25, 405, 535, 270);
		chatPanel.setFont(new Font("",Font.BOLD,18));
		chatPanel.setEditable(false);
		allText = new StringBuilder("");

		JScrollPane scrollPane = new JScrollPane(chatPanel);
		scrollPane.setBounds(25, 405, 535, 270);
		container.add(scrollPane);


		new Thread(new Receiver(chatSocket)).start();


		try{
			out = new PrintWriter(chatSocket.getOutputStream(), true);
		}
		catch(IOException e) {
			if(out != null) 
				out.close();
		} 
		System.out.println("���ε�ä�� UI ��Ÿ�����ϴ�.");

	}//----------------------------------------------------------------

	/*------------------------------ ���� Ŭ������ ------------------------------*/

	public class GrimPane extends JPanel {

		int x,y,q,w;
		Dohwagi canvas;
		boolean clear = false;

		GrimPane() {
			canvas = new Dohwagi();
			canvas.setBounds(25, 40, 530, 345);
			canvas.setBackground(Color.white); // ��ȭ�� ���� �ֱ�
			canvas.addMouseMotionListener(new MyHandler());
			add(canvas);
		}

		// �̺�Ʈ �ڵ鷯
		class MyHandler implements MouseMotionListener {

			public void mouseDragged(MouseEvent e){

				if(icandraw == true) {
					//���콺�� �巡���� ������ x��ǥ,y��ǥ�� ���ͼ� canvas�� x,y ��ǥ���� �����Ѵ�.
					int X=e.getX(); int Y=e.getY();
					//System.out.println("x="+X+", y="+Y);
					canvas.x=X; canvas.y=Y;
					canvas.repaint();//paint()�� JVM�� ȣ�����ִ� �޼ҵ����� ����x, repaint�� �Ἥ ��������

					String text = new String("500#"+X+"^"+Y+"^"+canvas.w+"^"+canvas.h);
					out.println(text);
					out.flush();
				}
			}

			public void mouseMoved(MouseEvent e) { }
		}
		// Grimpane�� ���� Ŭ����
		class Dohwagi extends Canvas{

			int x=-50, y=-50, w=7, h=7;
			Color cr=Color.black;
			public void paint(Graphics g) {
				if(clear == true) {
					g.setColor(Color.WHITE);
					g.fillRect(x, y, w, h);
				}
				else {
					g.setColor(cr);
					g.fillOval(x, y, w, h); // x, y ������ �� �׸���
				}
			}
			public void update(Graphics g) {
				paint(g);
			}
		}

	}//----------------------------------------------------------------

	// �̺�Ʈ �ڵ鷯 Ŭ����
	class applyButton implements ActionListener{

		public void actionPerformed(ActionEvent e) {
			if(MyName.equals("")) {
				MyName = textField.getText();
				chatPanel.setText("");
				textField.setText(null);
				out.println("100#"+MyName);
				out.flush();
				return;
			}
			inputText = textField.getText();
			allText.append("<"+MyName+"> :"+inputText+"\n");
			chatPanel.setText(allText.toString());
			textField.setText(null);
			out.println("100#"+inputText);
			out.flush();
		}

	}//----------------------------------------------------------------


	// �̺�Ʈ �ڵ鷯 Ŭ����
	class eraseButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			grimPanel.canvas.cr = Color.WHITE;
			grimPanel.canvas.w=13;
			grimPanel.canvas.h=13;
			out.println("600#"+"erase");
			out.flush();
		}

	}//----------------------------------------------------------------

	// �̺�Ʈ �ڵ鷯 Ŭ����
	class penButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			grimPanel.canvas.cr = Color.BLACK;
			grimPanel.canvas.w=10;
			grimPanel.canvas.h=10;
			out.println("700#"+"pen");
			out.flush();
		}

	}//----------------------------------------------------------------

	// �̺�Ʈ �ڵ鷯 Ŭ����
	class startButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {

			if(gameRunning == false && icandraw == true) {
				if(!MyName.equals("")) {
					try {
						if(RMI_func.Gamer_Num() > 1) {
							int clientID = RMI_func.Start();
							gameRunning = true;
							System.out.println(clientID);
							out.println("800#"+clientID);
							out.flush();
						}
						else {
							allText.append("      [�����Ե� ������ ���Ұ� ������. �������� ��ٷ��ּ���.]\n");
							chatPanel.setText(allText.toString());
						}
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}
				}
				else {
					chatPanel.setText("���� �̸��� ������ �����̾��! �̸��� ���ϰ� ������ ���ּ���.");
				}
			}
			else {
				allText.append("[������ �ٽ� ������ ����� ������ ���߰ų�, ������ pass�������� �����մϴ�.]\n");
				chatPanel.setText(allText.toString());
			}
		}

	}//----------------------------------------------------------------

	class passButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {

			if(gameRunning == true && icandraw == true) {
				try {
					if(RMI_func.Gamer_Num() > 1) {
						int clientID = RMI_func.TurnPass();
						icandraw = false;
						allText.append("      [�ٸ� ������� �Ѱ��־����ϴ� ]\n");
						chatPanel.setText(allText.toString());
						out.println("800#"+ clientID);
						out.flush();
					}
					else {
						allText.append("      [�����Ե� ������ ���Ұ� ������. �������� ��ٷ��ּ���.]\n");
						chatPanel.setText(allText.toString());
					}
				}
				catch (RemoteException e1) {
					e1.printStackTrace();
				}
			}
			else {
				allText.append("      [����� ������ �ƴմϴ�!]\n");
				chatPanel.setText(allText.toString());
			}

		}

	}//----------------------------------------------------------------

	class queButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if(gameRunning == true && icandraw == true) {
				try {
					if(RMI_func.Gamer_Num() > 1) {
						String ans = RMI_func.SetAns();
						allText.append("	  >>>>���ο� ������ ["+ans+"]"+"�Դϴ�<<<<\n");
						chatPanel.setText(allText.toString());
						out.println("400#"+"������ �ٲ�");
						out.flush();

						grimPanel.clear = true;
						grimPanel.canvas.x = 0;
						grimPanel.canvas.y = 0;
						grimPanel.canvas.w = 570;
						grimPanel.canvas.h = 440;
						grimPanel.canvas.repaint();
					}
					else {
						allText.append("      [�����Ե� ������ ���Ұ� ������. �������� ��ٷ��ּ���.]\n");
						chatPanel.setText(allText.toString());
					}
				} catch (RemoteException e1) {
					e1.printStackTrace();
				}

				chatPanel.setText(allText.toString());
			}
			else {
				allText.append("      [����� ���ʰ� ���� �ƴ϶� �Ұ����մϴ�.]\n");
				chatPanel.setText(allText.toString());
			}
		}

	}//----------------------------------------------------------------

	class answerButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if(MyName.equals("")) {
				chatPanel.setText("���� �̸��� ������ �����̾��! �̸��� ���ϰ� ������ ���ּ���.");
				return;
			}
			if(gameRunning == true && icandraw == false) {
				inputText = textField.getText();
				allText.append("["+inputText+"] ��� �����ּ̽��ϴ�.\n");
				try {
					if(RMI_func.isAns(inputText)) {
						allText.append("        !!!!�� �� �� �� ��!!!!\n");
						chatPanel.setText(allText.toString());
						gameRunning = false;
						icandraw = true;
						out.println("200#"+inputText);
						out.flush();
					}
					else {
						allText.append("        ....��  ��  ��  ��  ��....\n");
						chatPanel.setText(allText.toString());
						out.println("300#"+inputText);
						out.flush();
					}
				} catch (RemoteException e1) {

					e1.printStackTrace();
				}
			}else {
				allText.append("        <����� ������ ����� �����ϴ�.>\n");
				chatPanel.setText(allText.toString());
			}
		}

	}//----------------------------------------------------------------


	/*-----------------------------------------------------------------------------------------*/
	//���� ������� ���ư��� �޴� �Լ�
	class Receiver implements Runnable {

		private SSLSocket chatSocket = null;

		private StringTokenizer Token;

		Receiver(SSLSocket socket){
			this.chatSocket = socket;
		}

		public void run() {

			while(chatSocket.isConnected()) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
					String readSome = null;

					while((readSome = in.readLine())!=null) {
						Token = new StringTokenizer(readSome, "#");
						String temp = Token.nextToken();
						String message = Token.nextToken();

						if(temp.equalsIgnoreCase("100")) {		// �Ϲ��� ��ȭ
							allText.append(message+"\n");
							chatPanel.setText(allText.toString());
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("200")) {	// ������ ������ ���߾��� �� 
							Token = new StringTokenizer(message, "*");
							String name = Token.nextToken();
							String ans = Token.nextToken();

							allText.append("      ["+name+"���� ������ ���߼̽��ϴ�!]\n");
							chatPanel.setText(allText.toString());
							allText.append("     [������  \""+ans+"\" �Դϴ�.]\n");
							chatPanel.setText(allText.toString());

							gameRunning = false;
							icandraw = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("300")) { // ������ ������ ���� �� 
							String name = message;
							allText.append("      ["+name+"���� ������ ���̽��ϴ�~]\n");
							chatPanel.setText(allText.toString());
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("400")) {	// question �ٸ� ������ ���Ҷ�

							String name = message;
							allText.append("    ["+name+"���� ������ �ٲټ̽��ϴ�.]\n");
							chatPanel.setText(allText.toString());

							grimPanel.clear = true;
							grimPanel.canvas.x = 0;
							grimPanel.canvas.y = 0;
							grimPanel.canvas.w = 570;
							grimPanel.canvas.h = 440;
							grimPanel.canvas.repaint();

							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("500")) {	// �׸� ��ǥ ������
							Token = new StringTokenizer(message, "^");
							int x,y,w,h;
							x=  Integer.parseInt(Token.nextToken());
							y=  Integer.parseInt(Token.nextToken());
							w=  Integer.parseInt(Token.nextToken());
							h=  Integer.parseInt(Token.nextToken());
							grimPanel.canvas.x = x;
							grimPanel.canvas.y = y;
							grimPanel.canvas.repaint();
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("600")) {	// ���찳
							grimPanel.clear = false;
							grimPanel.canvas.cr = Color.WHITE;
							grimPanel.canvas.w=11;
							grimPanel.canvas.h=11;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("700")) {	// �� 
							grimPanel.clear = false;
							grimPanel.canvas.cr = Color.BLACK;
							grimPanel.canvas.w=7;
							grimPanel.canvas.h=7;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("800")) {	// ���� ���� !!
							icandraw = true;

							grimPanel.clear = true;
							grimPanel.canvas.x = 0;
							grimPanel.canvas.y = 0;
							grimPanel.canvas.w = 570;
							grimPanel.canvas.h = 440;
							grimPanel.canvas.repaint();

							allText.append("    [�����  �׸��� �׸��ڽ��ϴ�. '�� ��ư'�� ������ �׷��ּ���.]\n");
							chatPanel.setText(allText.toString());

							try {
								String ans = RMI_func.SetAns();
								allText.append("	  >>>>>>������ ["+ans+"]"+"�Դϴ�<<<<<<\n");
							} catch (RemoteException e1) {
								e1.printStackTrace();
							}

							chatPanel.setText(allText.toString());

							gameRunning = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("900")) {	 // ��� ���� �̤�
							icandraw = false;

							grimPanel.clear = true;
							grimPanel.canvas.x = 0;
							grimPanel.canvas.y = 0;
							grimPanel.canvas.w = 570;
							grimPanel.canvas.h = 440;
							grimPanel.canvas.repaint();

							allText.append("   ["+message+" ���� �׸��� �׸��ڽ��ϴ�. �׸��� ���ּ���.]\n");
							chatPanel.setText(allText.toString());

							gameRunning = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("000")) {	 // ������ ���Ḧ �߽��ϴ�.

							allText.append("   ["+message+" ���� �����ϼ̽��ϴ�.]\n");
							chatPanel.setText(allText.toString());

							Token = null;
							continue;
						}

					}
					in.close();
					chatSocket.close();

				}catch (IOException i) {
					try {
						if(in !=null) in.close();
						if(chatSocket != null) chatSocket.close();
					}catch (IOException e) {
					}
					System.out.println("���ῡ ������ ������ϴ�.");
					System.exit(1);
				}
			}
		}
	}
	/*-----------------------------------------------------------------------------------------*/
}
/*-----------------------------------------------------------------------------------------*/
