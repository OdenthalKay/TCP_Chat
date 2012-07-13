import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
	private Socket socket;
	private String nickname = "Gast";
	private String vorname = "John";
	private String nachname = "Doe";
	private String email = "-";
	private Thread messageListenerThread = null;
	PrintWriter writer = null;
	private List<Client> ignoreList;
	

	/**
	 * Der MessageListener läuft in einem sepparaten Thread. Er prüft rund um
	 * die Uhr, ob der Server Nachrichten schickt. Die Nachrichten gibt er
	 * Anschließend in der Konsole aus.
	 */
	private class MessageListener implements Runnable {

		@Override
		public void run() {
			BufferedReader bufferedReader = null; // Nachrichten vom Server lesen

			try {
				bufferedReader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				// horchen, ob Server Nachrichten sendet
				String message = "";
				while ((message = bufferedReader.readLine()) != null) {
					
					// Nachricht auf der Client Konsole ausgeben
					System.out.println(message);
					
					// dem Server meine privaten Informationen zusenden
					if (message.equals("send_infos_to_server")) {
						System.out.println("Sende Server meine infos: " + nickname);
						writer.println(nickname);
						writer.println(vorname);
						writer.println(nachname);
						writer.println(email);
					}
				}

			} catch (IOException e) {
				System.out
						.println("Problem beim Empfang einer Server Nachricht: "
								+ e.getMessage());
			}

			// Verbindungen schließen
			finally {
				
				try {
					
					// sicherstellen, dass Socket auch wirklich geschlossen ist
					if (socket != null) {
						socket.close();
					}

					bufferedReader.close();
					
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	public Client(String nick, String vname, String nname, String email) {
		this.nickname = nick;
		this.vorname = vname;
		this.nachname = nname;
		this.email = email;
		this.ignoreList = new ArrayList<Client>();
	}

	public void connect(String hostname, int port) {
		BufferedReader bufferedReader = null;

		try {
			// mit dem server verbinden
			socket = new Socket(hostname, port);

			writer = new PrintWriter(socket.getOutputStream(), true);

			// MessageListener als neuen Thread starten
			messageListenerThread = new Thread(new MessageListener());
			messageListenerThread.start();

			// Reader zum eingeben von Nachrichten
			bufferedReader = new BufferedReader(
					new InputStreamReader(System.in));

			// Nachrichten an den Server senden
			String message = "";
			while ((message = bufferedReader.readLine()) != null) {
				writer.println(message);
			
				// Subthreads stoppen, damit Prozess terminiert
				if (message.equals("disconnect")) {
					messageListenerThread.interrupt();
					break;
				} 
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		// Verbindungen schließen
		finally {

			try {
				// sicherstellen, dass Socket auch wirklich geschlossen ist
				if (socket != null)
					socket.close();

				writer.close();
				bufferedReader.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			
		}
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
	public String getVorname() {
		return vorname;
	}
	
	public void setVorname(String vorname) {
		this.vorname = vorname;
	}
	
	public String getNachname() {
		return nachname;
	}
	
	public void setNachname(String nachname) {
		this.nachname = nachname;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public List<Client> getIgnoreList() {
		return ignoreList;
	}
	
	
	
}
