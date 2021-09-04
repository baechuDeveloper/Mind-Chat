import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Game_func extends Remote {

	// 게임의 전반적인 내용물을 담고있습니다. 유저들이 게임에 대한 정보를 얻는데 있어서 필요한걸 사용할수 있도록 해두었습니다.
	public int Gamer_Num() throws RemoteException;
	public int TurnPass() throws RemoteException;
	public String SetAns() throws RemoteException;
	public boolean isAns(String ans) throws RemoteException;
	public int WhoStart() throws RemoteException;
	public int Start() throws RemoteException;
}
