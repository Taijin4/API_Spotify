package fr.antonins;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import se.michaelthelin.spotify.SpotifyApi;

import javax.security.auth.callback.CallbackHandler;
import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

import static fr.antonins.Credentials.*;

public class Spotify {

	private static final CountDownLatch latch = new CountDownLatch(1);
	static SpotifyApi spotifyApi = new SpotifyApi.Builder()
			.setClientId(CLIENT_ID)
			.setClientSecret(CLIENT_SECRET)
			.setRedirectUri(URI.create("http://localhost"))
			.build();

	public Spotify() throws IOException {
		System.out.println("test");

		String authorizationUrl = "https://accounts.spotify.com/authorize" +
				"?response_type=code" +
				"&client_id=" + CLIENT_ID +
				"&redirect_uri=" + REDIRECT_URI +
				"&scope=user-read-private%20playlist-modify-public";

		// Ouvre l'URL d'autorisation dans le navigateur par défaut de l'utilisateur
		Desktop.getDesktop().browse(java.net.URI.create(authorizationUrl));

		// Créez et démarrez un serveur HTTP pour gérer la redirection
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		server.createContext("/callback", new CallbackHandler());
		server.start();

		// Attendez jusqu'à ce que le code d'autorisation soit récupéré
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Arrêtez le serveur HTTP
		server.stop(0);

		// Étape 3 : Demander un access token
		HttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://accounts.spotify.com/api/token");

		// Encodez le Client ID et le Client Secret en utilisant Base64
		String encodedCredentials = Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		// Configurez les paramètres de la requête
		StringEntity params = new StringEntity(
				"grant_type=authorization_code" +
						"&code=" + authorizationCode +
						"&redirect_uri=" + REDIRECT_URI
		);
		httpPost.setEntity(params);
		httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
		httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

		try {
			// Envoyez la requête POST et récupérez la réponse
			HttpResponse response = httpClient.execute(httpPost);
			String responseJson = EntityUtils.toString(response.getEntity());
			System.out.println(responseJson);

			// Après avoir obtenu la réponse JSON contenant l'access token
			JSONObject jsonResponse = new JSONObject(responseJson);

			// Extrait l'access token du JSON
			String accessToken = jsonResponse.getString("access_token");

			// Affiche l'access token
			System.out.println("Access Token: " + accessToken);
			spotifyApi.setAccessToken(accessToken);


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static class CallbackHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String query = exchange.getRequestURI().getQuery();
			String[] queryParams = query.split("&");
			for (String param : queryParams) {
				String[] keyValue = param.split("=");
				if (keyValue.length == 2 && keyValue[0].equals("code")) {
					authorizationCode = keyValue[1];
					break;
				}
			}

			System.out.println("Authorization code: " + authorizationCode);

			String response = "Authorization code received. You can close this window now.";
			exchange.sendResponseHeaders(200, response.length());
			exchange.getResponseBody().write(response.getBytes());
			exchange.getResponseBody().close();

			// Signal que le code d'autorisation a été récupéré
			latch.countDown();
		}
	}

}
