package fr.antonins;



import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import se.michaelthelin.spotify.SpotifyApi;
import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import static fr.antonins.Credentials.*;

public class Spotify {

	private static final CountDownLatch latch = new CountDownLatch(1);
	public static String authorizationCode;
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
		server.createContext("/callback", new CallbackHandler(latch));
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

			// Après avoir obtenu la réponse JSON contenant l'access token
			JSONObject jsonResponse = new JSONObject(responseJson);

			// Extrait l'access token du JSON
			String accessToken = jsonResponse.getString("access_token");

			// Affiche l'access token
			spotifyApi.setAccessToken(accessToken);


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void getNbPlaylist() {
		spotifyApi.getPlaylist("6WKFe2uQo4SblWVPsGMDYL");

		System.out.println("Nombre de playlists");
	}

}
