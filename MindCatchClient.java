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
			//chatSocket.startHandshake(); 이미 한 번의 세션이 있기때문에 추가적으로 하지 않았습니다.

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

			System.out.println("서버에 접속했습니다.");

		} catch (BindException b) {
			System.out.println("현재 "+Port+" 포트 번호에 bind를 하지 못합니다.");
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

	private Container 	container;  // MindCatch 본인을 지칭 하도록 만든 컨테이너.

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
		setTitle("마인드캐치");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		container = getContentPane(); 
		container.setBackground(Color.darkGray);
		container.setLayout(null);	
		setSize(600,900);

		grimPanel = new GrimPane();
		grimPanel.setBackground(Color.WHITE);
		grimPanel.setBounds(20, 35, 540, 355);
		container.add(grimPanel);

		JButton sendButton = new JButton("보내기");
		sendButton.setBounds(450, 700, 75, 27);
		sendButton.addActionListener(new applyButton());
		container.add(sendButton);

		JButton eraseButton = new JButton("지우개");
		eraseButton.setBounds(30, 740, 75, 27);
		eraseButton.addActionListener(new eraseButton());
		container.add(eraseButton);

		JButton penButton = new JButton("펜");
		penButton.setBounds(110, 740, 75, 27);
		penButton.addActionListener(new penButton());
		container.add(penButton);

		JButton startButton = new JButton("게임 시작");
		startButton.setBounds(220, 740, 90, 27);
		startButton.addActionListener(new startButton());
		container.add(startButton);

		JButton passButton = new JButton("차례 넘기기");
		passButton.setBounds(220, 775, 110, 27);
		passButton.addActionListener(new passButton());
		container.add(passButton);

		JButton queButton = new JButton("다른 문제");
		queButton.setBounds(315, 740, 90, 27);
		queButton.addActionListener(new queButton());
		container.add(queButton);

		JButton answerButton = new JButton("정 답!");
		answerButton.setBounds(450, 740, 75, 27);
		answerButton.addActionListener(new answerButton());
		container.add(answerButton);

		textField = new JTextField();
		textField.setBounds(30, 700, 405, 24);
		textField.setColumns(10);
		container.add(textField);

		chatPanel = new JTextPane();
		chatPanel.setText("이름을 입력해주세요");
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
		System.out.println("마인드채팅 UI 나타났습니다.");

	}//----------------------------------------------------------------

	/*------------------------------ 내부 클래스들 ------------------------------*/

	public class GrimPane extends JPanel {

		int x,y,q,w;
		Dohwagi canvas;
		boolean clear = false;

		GrimPane() {
			canvas = new Dohwagi();
			canvas.setBounds(25, 40, 530, 345);
			canvas.setBackground(Color.white); // 도화지 배경색 주기
			canvas.addMouseMotionListener(new MyHandler());
			add(canvas);
		}

		// 이벤트 핸들러
		class MyHandler implements MouseMotionListener {

			public void mouseDragged(MouseEvent e){

				if(icandraw == true) {
					//마우스를 드래그한 지점의 x좌표,y좌표를 얻어와서 canvas의 x,y 좌표값에 전달한다.
					int X=e.getX(); int Y=e.getY();
					//System.out.println("x="+X+", y="+Y);
					canvas.x=X; canvas.y=Y;
					canvas.repaint();//paint()는 JVM이 호출해주는 메소드으로 변경x, repaint을 써서 재사용하자

					String text = new String("500#"+X+"^"+Y+"^"+canvas.w+"^"+canvas.h);
					out.println(text);
					out.flush();
				}
			}

			public void mouseMoved(MouseEvent e) { }
		}
		// Grimpane의 내부 클래스
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
					g.fillOval(x, y, w, h); // x, y 지점에 원 그리기
				}
			}
			public void update(Graphics g) {
				paint(g);
			}
		}

	}//----------------------------------------------------------------

	// 이벤트 핸들러 클래스
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


	// 이벤트 핸들러 클래스
	class eraseButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			grimPanel.canvas.cr = Color.WHITE;
			grimPanel.canvas.w=13;
			grimPanel.canvas.h=13;
			out.println("600#"+"erase");
			out.flush();
		}

	}//----------------------------------------------------------------

	// 이벤트 핸들러 클래스
	class penButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			grimPanel.canvas.cr = Color.BLACK;
			grimPanel.canvas.w=10;
			grimPanel.canvas.h=10;
			out.println("700#"+"pen");
			out.flush();
		}

	}//----------------------------------------------------------------

	// 이벤트 핸들러 클래스
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
							allText.append("      [슬프게도 아직은 못할것 같군요. 유저들을 기다려주세요.]\n");
							chatPanel.setText(allText.toString());
						}
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}
				}
				else {
					chatPanel.setText("아직 이름을 정하지 않으셨어요! 이름을 정하고 게임을 해주세요.");
				}
			}
			else {
				allText.append("[게임을 다시 시작할 방법은 문제를 맞추거나, 방장이 pass했을때만 가능합니다.]\n");
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
						allText.append("      [다른 사람에게 넘겨주었습니다 ]\n");
						chatPanel.setText(allText.toString());
						out.println("800#"+ clientID);
						out.flush();
					}
					else {
						allText.append("      [슬프게도 아직은 못할것 같군요. 유저들을 기다려주세요.]\n");
						chatPanel.setText(allText.toString());
					}
				}
				catch (RemoteException e1) {
					e1.printStackTrace();
				}
			}
			else {
				allText.append("      [당신은 방장이 아닙니다!]\n");
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
						allText.append("	  >>>>새로운 정답은 ["+ans+"]"+"입니다<<<<\n");
						chatPanel.setText(allText.toString());
						out.println("400#"+"정답을 바꿈");
						out.flush();

						grimPanel.clear = true;
						grimPanel.canvas.x = 0;
						grimPanel.canvas.y = 0;
						grimPanel.canvas.w = 570;
						grimPanel.canvas.h = 440;
						grimPanel.canvas.repaint();
					}
					else {
						allText.append("      [슬프게도 아직은 못할것 같군요. 유저들을 기다려주세요.]\n");
						chatPanel.setText(allText.toString());
					}
				} catch (RemoteException e1) {
					e1.printStackTrace();
				}

				chatPanel.setText(allText.toString());
			}
			else {
				allText.append("      [당신의 차례가 아직 아니라 불가능합니다.]\n");
				chatPanel.setText(allText.toString());
			}
		}

	}//----------------------------------------------------------------

	class answerButton implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if(MyName.equals("")) {
				chatPanel.setText("아직 이름을 정하지 않으셨어요! 이름을 정하고 게임을 해주세요.");
				return;
			}
			if(gameRunning == true && icandraw == false) {
				inputText = textField.getText();
				allText.append("["+inputText+"] 라고 적어주셨습니다.\n");
				try {
					if(RMI_func.isAns(inputText)) {
						allText.append("        !!!!정 답 입 니 다!!!!\n");
						chatPanel.setText(allText.toString());
						gameRunning = false;
						icandraw = true;
						out.println("200#"+inputText);
						out.flush();
					}
					else {
						allText.append("        ....오  답  입  니  다....\n");
						chatPanel.setText(allText.toString());
						out.println("300#"+inputText);
						out.flush();
					}
				} catch (RemoteException e1) {

					e1.printStackTrace();
				}
			}else {
				allText.append("        <당신은 정답을 맞출수 없습니다.>\n");
				chatPanel.setText(allText.toString());
			}
		}

	}//----------------------------------------------------------------


	/*-----------------------------------------------------------------------------------------*/
	//내부 쓰레드로 돌아가며 받는 함수
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

						if(temp.equalsIgnoreCase("100")) {		// 일반적 대화
							allText.append(message+"\n");
							chatPanel.setText(allText.toString());
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("200")) {	// 누군가 정답을 맞추었을 때 
							Token = new StringTokenizer(message, "*");
							String name = Token.nextToken();
							String ans = Token.nextToken();

							allText.append("      ["+name+"님이 정답을 맞추셨습니다!]\n");
							chatPanel.setText(allText.toString());
							allText.append("     [정답은  \""+ans+"\" 입니다.]\n");
							chatPanel.setText(allText.toString());

							gameRunning = false;
							icandraw = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("300")) { // 누군가 오답을 냈을 때 
							String name = message;
							allText.append("      ["+name+"님이 오답을 내셨습니다~]\n");
							chatPanel.setText(allText.toString());
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("400")) {	// question 다른 문제를 원할때

							String name = message;
							allText.append("    ["+name+"님이 문제를 바꾸셨습니다.]\n");
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
						else if(temp.equalsIgnoreCase("500")) {	// 그림 좌표 보내기
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
						else if(temp.equalsIgnoreCase("600")) {	// 지우개
							grimPanel.clear = false;
							grimPanel.canvas.cr = Color.WHITE;
							grimPanel.canvas.w=11;
							grimPanel.canvas.h=11;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("700")) {	// 펜 
							grimPanel.clear = false;
							grimPanel.canvas.cr = Color.BLACK;
							grimPanel.canvas.w=7;
							grimPanel.canvas.h=7;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("800")) {	// 나의 차례 !!
							icandraw = true;

							grimPanel.clear = true;
							grimPanel.canvas.x = 0;
							grimPanel.canvas.y = 0;
							grimPanel.canvas.w = 570;
							grimPanel.canvas.h = 440;
							grimPanel.canvas.repaint();

							allText.append("    [당신이  그림을 그리겠습니다. '펜 버튼'을 눌러서 그려주세요.]\n");
							chatPanel.setText(allText.toString());

							try {
								String ans = RMI_func.SetAns();
								allText.append("	  >>>>>>정답은 ["+ans+"]"+"입니다<<<<<<\n");
							} catch (RemoteException e1) {
								e1.printStackTrace();
							}

							chatPanel.setText(allText.toString());

							gameRunning = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("900")) {	 // 상대 차례 ㅜㅜ
							icandraw = false;

							grimPanel.clear = true;
							grimPanel.canvas.x = 0;
							grimPanel.canvas.y = 0;
							grimPanel.canvas.w = 570;
							grimPanel.canvas.h = 440;
							grimPanel.canvas.repaint();

							allText.append("   ["+message+" 님이 그림을 그리겠습니다. 그림을 봐주세요.]\n");
							chatPanel.setText(allText.toString());

							gameRunning = true;
							Token = null;
							continue;
						}
						else if(temp.equalsIgnoreCase("000")) {	 // 누군가 종료를 했습니다.

							allText.append("   ["+message+" 님이 종료하셨습니다.]\n");
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
					System.out.println("연결에 문제가 생겼습니다.");
					System.exit(1);
				}
			}
		}
	}
	/*-----------------------------------------------------------------------------------------*/
}
/*-----------------------------------------------------------------------------------------*/
