import java.io.IOException;


public class TestServer {
	
	public static void main(String[] args) {
		
		try {
			Server server = new Server(6666);
			server.run();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			
		}
	}
}
