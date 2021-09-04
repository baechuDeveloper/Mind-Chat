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
			
			question[0] = "호랑이";
			question[1] = "강아지";
			question[2] = "꽃집";
			question[3] = "카페";
			question[4] = "수학";
			
			question[5] = "코카콜라";
			question[6] = "바나나";
			question[7] = "컴퓨터";
			question[8] = "키보드";
			question[9] = "텀블러";
			
			question[10] = "아파트";
			question[11] = "충전기";
			question[12] = "선인장";
			question[13] = "사슴";
			question[14] = "비몽사몽";
			
			question[15] = "밥 아저씨";
			question[16] = "두루미";
			question[17] = "늑대";
			question[18] = "소화기";
			question[19] = "갤럭시S10";
			
		}
	}
	/*------------------------------------------------------------------*/

}
