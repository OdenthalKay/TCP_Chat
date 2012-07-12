import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
	private ServerSocket serverSocket;
	private List<Client> clients;
	
	// der server hat für jeden client genau einen writer, über den
	// er den clients nachrichten senden kann
	private Map<Client, PrintWriter> writers = null;


	private class ConnectionHandler implements Runnable {
		private Client client;
		private PrintWriter writer;

		
		/**
		 * Ein Kommando kann maximal aus drei Teilen bestehen:
		 * prefix: muss
		 * suffix: muss
		 * potfix: optional
		 * 
		 * Beispiele:
		 * (1) /w username hallo
		 * (2) /infos username
		 */
		private void handleCommandResponse(String command) {
			String string = "";
			
			// '/' entfernen
			command = command.substring(1);
			
			// String weiter aufteilen [prefix|suffix|postfix]
			String[] parts = command.split(" ");
			String prefix = parts[0];
			
			String suffix = "";
			if (parts.length == 1) {
				writers.get(client).println("Das Kommando ist zu Kurz. Versuch es erneut.");
				return;
			}
			else
				suffix = parts[1];
			
			// optional 
			String postfix = ""; 
			if (parts.length > 2)
				postfix = parts[2];
			
			if (command.equals("infos")) { // show all infos about a user
				string = ("Infos:\n"+client.getNickname()+"\n"+client.getVorname()+" "+client.getNachname()+"\n"+client.getEmail());
				writers.get(client).println(string);
				return;
			}
			
			// zur Zeit nur ein zusammenhänges wort (ohne leerzeichen)
			if (prefix.equals("w")) { // whisper
				
				for (Client client : clients) {
					
					if (client.getNickname().equals(suffix)) {
						
						writers.get(client).println(client.getNickname() + " flüstert: " + postfix);
						return;
					}	
				}
			}
			
			writers.get(client).println("Dieses Kommando gibt es nicht...");
		}
		
		public ConnectionHandler(Client client, PrintWriter writer) {
			this.client = client;
			this.writer = writer;
		}

		@Override
		public void run() {
			handleConversation(client);
		}

		public void handleConversation(Client client) {
			BufferedReader bufferedReader = null;

			try {
				// Reader initialisieren um Nachrichten vom Client auslesen zu
				// können
				bufferedReader = new BufferedReader(new InputStreamReader(
						client.getSocket().getInputStream()));
				
				// Blockieren, bis Client alle Infos rübergeschickt hat
				client.setNickname(bufferedReader.readLine());
				client.setVorname(bufferedReader.readLine());
				client.setNachname(bufferedReader.readLine());
				client.setEmail(bufferedReader.readLine());

				// Horchen, ob Client etwas sendet
				String prefix = (client.getNickname() + ": ");
				String message = "";
				while ((message = bufferedReader.readLine()) != null) {

					// Falls user manuell eine "disconnect" Nachricht sendet
					// wird er entfernt
					if (message.equals("disconnect")) {
						System.out.println("Habe Disconnect Nachricht erhalten: " + client.getNickname());
						break;
					}

					// Command-Nachrichten sepparat behandeln und versenden
					if (message.charAt(0) == '/') {
						handleCommandResponse(message);
					} else {
						
						// Textnachrichte an alle anderen clients weitersenden
						for (Client otherClient : clients) {

							if (!otherClient.equals(client))
								writers.get(otherClient).println(prefix + message);
						}	
					}
					
					// Nachricht auf der Konsole des Servers ausgeben
					System.out.println(prefix + message);
				}

			} catch (IOException e) {
				System.out.println("Fehler beim abhorchen des clients: "
						+ e.getMessage());
			} finally {

				// Verbindungen schließen und client vom Server löschen
				try {
					clients.get(clients.indexOf(client)).getSocket().close();
					clients.remove(client);
					System.out.println("User wurde entfernt. Anzahl: "
							+ clients.size());

					// Die Reader / Writer müssen UNBEDINGT nach dem socket
					// geschlossen werden,
					// da sie die sockets sonst frühzeitig SCHLIESSEN (=>
					// Exception)
					writers.get(client).close();
					writers.remove(writer);
					bufferedReader.close();

				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	public Server(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
		System.out.println("Server Socket wurde erfolgreich geöffnet.");
		this.clients = new ArrayList<Client>();
		this.writers = new HashMap<Client, PrintWriter>();
	}

	public void run() {
		System.out.println("Warte darauf, dass Clients connecten...");

		while (true) {

			try {
				Client client = new Client("", "", "", "");
				Socket socket = serverSocket.accept();
				client.setSocket(socket);
				clients.add(client);

				// Jeder Client bekommt einen eigenen writer, über den ihm der server 
				// Nachrichten zusenden kann
				PrintWriter writer = new PrintWriter(client.getSocket().getOutputStream(),
						true);
				writers.put(client, writer);
				writer.println("Der Server hat dich hinzugefuegt.");
				writer.println("send_infos_to_server");

				System.out.println("Neuer Client connected. Anzahl: "
						+ clients.size());

				// für jede Verbindung einen eigenen Thread eröffnen
				new Thread(new ConnectionHandler(client, writer)).start();
			} catch (IOException e) {
				System.out.println("Fehler beim accepten.");
			}
		}
	}
}
