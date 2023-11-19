package fr.antonins;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static fr.antonins.Spotify.authorizationCode;

public class CallbackHandler implements HttpHandler {

	private final CountDownLatch latch;

	public CallbackHandler(CountDownLatch latch) {

		this.latch = latch;
	}

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


		String response = "Authorization code received. You can close this window now.";
		exchange.sendResponseHeaders(200, response.length());
		exchange.getResponseBody().write(response.getBytes());
		exchange.getResponseBody().close();

		// Signal que le code d'autorisation a été récupéré
		latch.countDown();
	}
}
