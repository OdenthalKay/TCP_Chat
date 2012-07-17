import java.util.Scanner;


public class TestClient {
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Bitte Nickname eingeben: ");
		String nickname = sc.next();
		Client client = new Client(nickname, "Kay", "Odenthal", "Kay.Odenthal@gmx.de");
		client.connect("localhost", 1111);
	}
}
