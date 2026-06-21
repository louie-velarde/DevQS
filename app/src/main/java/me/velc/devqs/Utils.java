package me.velc.devqs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.net.Inet4Address;

import rikka.shizuku.Shizuku;

public final class Utils {

	private static final String TAG = "DevQS";

	public static void execute(String... commands) {
		if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
			try {
				//noinspection deprecation
				Shizuku.newProcess(commands, null, null).waitFor();
			} catch (InterruptedException | RuntimeException e) {
				Log.e(TAG, "Unable to execute command", e);
			}
		}
	}

	public static String getIpv4Address(Context context) {
		var cm = context.getSystemService(ConnectivityManager.class);
		if (cm == null) return null;

		for (var network : cm.getAllNetworks()) {
			var nc = cm.getNetworkCapabilities(network);
			if (nc == null || !nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue;

			var lp = cm.getLinkProperties(network);
			if (lp == null) continue;

			for (var linkAddress : lp.getLinkAddresses()) {
				var inetAddress = linkAddress.getAddress();
				if (inetAddress instanceof Inet4Address) {
					return inetAddress.getHostAddress();
				}
			}
		}
		return null;
	}

	public static boolean isWifiConnected(Context context) {
		var cm = context.getSystemService(ConnectivityManager.class);
		if (cm == null) return false;

		for (var network : cm.getAllNetworks()) {
			var nc = cm.getNetworkCapabilities(network);
			if (nc == null) continue;

			if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
				return true;
			}
		}
		return false;
	}

	public static void log(String format, Object... args) {
		// Log.d(TAG, String.format(Locale.ROOT, format, args));
	}

	private Utils() {}
}