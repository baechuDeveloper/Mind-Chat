import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class Game_Impl extends UnicastRemoteObject implements Game_func {
	
	private static final long serialVersionUID = 1L;

	private Answer answer; 
	private Random random;
	private int now_answer_num = 0;
	private int now_who_turn = 0;
	private ServerRunnable[] Game_User;
	
	
	public Game_Impl(ServerRunnable[] client) throws RemoteException{
		super();
		answer = new Answer();
		random = new Random();
		this.Game_User = client;
	}//--------------------------------------------------------
	
	
	public int Gamer_Num() {
		int num = 0;
		
		for(int i=0; i<Game_User.length; i++) {
			if(Game_User[i] != null) 
				if(Game_User[i].clientName != null) 
					num++;
		}
		return num;
	}
	
	
	public int TurnPass () {
		int temp;
		
		while(true) {
			temp = random.nextInt(Game_User.length);
			
			if(temp == now_who_turn) {
				continue;
			}
			else if(Game_User[temp] != null) {
				if(Game_User[temp].clientName != null) {
					now_who_turn = temp;
					
					return Game_User[now_who_turn].clientID;
				}
			}	
		}
		
	}//--------------------------------------------------------
	
	public String SetAns() {
		now_answer_num = random.nextInt(20);
		String que = answer.question[now_answer_num];
		return que;
	}//--------------------------------------------------------
	
	public boolean isAns(String ans) {

		if(answer.question[now_answer_num].equalsIgnoreCase(ans)) {
			return true;
		}	
		return false;
	}//--------------------------------------------------------
	
	public int WhoStart() {

		return now_who_turn;
	}//--------------------------------------------------------
	
	public int Start() {
		
		int temp = random.nextInt(Game_User.length);
		 
		while(true) {
			if(Game_User[temp] != null) {
				if(Game_User[temp].clientName != null) {
					now_who_turn = temp;
					
					return Game_User[now_who_turn].clientID;
				}
			}
			temp = random.nextInt(Game_User.length);
		}
		
	}//--------------------------------------------------------
	
	
	/*------------------------------------------------------------------*/
	private class Answer {
		String[] question;
		Answer(){
			question = new String[20];
			
			question[0] = "ȣ����";
			question[1] = "������";
			question[2] = "����";
			question[3] = "ī��";
			question[4] = "����";
			
			question[5] = "��ī�ݶ�";
			question[6] = "�ٳ���";
			question[7] = "��ǻ��";
			question[8] = "Ű����";
			question[9] = "�Һ�";
			
			question[10] = "����Ʈ";
			question[11] = "������";
			question[12] = "������";
			question[13] = "�罿";
			question[14] = "������";
			
			question[15] = "�� ������";
			question[16] = "�η��";
			question[17] = "����";
			question[18] = "��ȭ��";
			question[19] = "������S10";
			
		}
	}
	/*------------------------------------------------------------------*/

}
