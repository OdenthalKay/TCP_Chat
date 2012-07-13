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
		 * Kommandos: (1) /w username some text here (2) /infos username (3)
		 * /ignore username (4) /unignore username
		 */
		private void handleCommandResponse(String command) {
			String header = "";
			String body = "";

			// '/' entfernen
			header = command.substring(1);

			if (header.startsWith("infos")) {
				body = header.substring("infos".length() + 1);
				
				for (Client client : clients) {
					
					if (client.getNickname().equals(body)) {
						String infoString = (client.getNickname()+"\n"+client.getVorname()+" "+client.getNachname()+"\n"+client.getEmail()+"\n");
						
						// nachricht an den user senden, der den request geschickt hat
						writers.get(this.client).println(infoString);
					}
				}
				
			}
			
			if (header.startsWith("w")) {
				body = header.substring("w".length() + 1);
				
				// username und message extrahieren
				String username = "";
				String message = "";
				for (int i = 0; i < body.length(); i++) {
					
					if (body.charAt(i) == ' ') {
						username = body.substring(0, i);
						message = body.substring(i+1);
						break;
					}
				}
				
				// message an adressierten user senden
				for (Client client : clients) {

					if (client.getNickname().equals(username) && !client.getIgnoreList().contains(this.client)) 
						writers.get(client).println(message);
				}
			}

			if (header.startsWith("ignore")) {
				body = header.substring("ignore".length() + 1);

				for (Client client : clients) {

					if (client.getNickname().equals(body)) {
						List<Client> ignoreList = this.client.getIgnoreList();

						if (!ignoreList.contains(client))
							ignoreList.add(client);
					}
				}
			}

			if (header.startsWith("unignore")) {
				body = header.substring("unignore".length() + 1);

				for (Client client : clients) {

					if (client.getNickname().equals(body)) {
						List<Client> ignoreList = this.client.getIgnoreList();

						if (ignoreList.contains(client))
							ignoreList.remove(client);
					}
				}
			}

			//writers.get(client).println("Dieses Kommando gibt es nicht...");
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
						System.out
								.println("Habe Disconnect Nachricht erhalten: "
										+ client.getNickname());
						break;
					}

					// Command-Nachrichten sepparat behandeln und versenden
					if (message.charAt(0) == '/') {
						handleCommandResponse(message);
					} else {

						// Textnachrichte an alle anderen clients weitersenden
						for (Client otherClient : clients) {

							if (!otherClient.equals(client)) {

								if (!otherClient.getIgnoreList().contains(
										client)) {

									writers.get(otherClient).println(
											prefix + message);
								}
							}
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

				// Jeder Client bekommt einen eigenen writer, über den ihm der
				// server
				// Nachrichten zusenden kann
				PrintWriter writer = new PrintWriter(client.getSocket()
						.getOutputStream(), true);
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
