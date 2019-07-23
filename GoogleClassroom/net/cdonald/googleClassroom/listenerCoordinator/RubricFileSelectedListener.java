package net.cdonald.googleClassroom.listenerCoordinator;

public abstract class RubricFileSelectedListener {
	// The google sheet dialog passes in both the url and the sheet, we only need the url.
	public void fired(String url, String ignored) {fired(url);}
	public abstract void fired(String url);

}
