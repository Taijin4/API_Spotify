package fr.antonins;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException {
		Spotify spotify = new Spotify();
		spotify.getNbPlaylist();
	}

}