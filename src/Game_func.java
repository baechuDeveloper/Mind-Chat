import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Game_func extends Remote {

	// ������ �������� ���빰�� ����ֽ��ϴ�. �������� ���ӿ� ���� ������ ��µ� �־ �ʿ��Ѱ� ����Ҽ� �ֵ��� �صξ����ϴ�.
	public int Gamer_Num() throws RemoteException;
	public int TurnPass() throws RemoteException;
	public String SetAns() throws RemoteException;
	public boolean isAns(String ans) throws RemoteException;
	public int WhoStart() throws RemoteException;
	public int Start() throws RemoteException;
}
