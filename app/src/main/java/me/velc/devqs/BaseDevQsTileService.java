package me.velc.devqs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.util.Locale;
import java.util.Objects;

public abstract class BaseDevQsTileService extends TileService {

	private static final String TAG = "DevQS";

	private final String settingName;

	protected BaseDevQsTileService(String settingName) {
		this.settingName = settingName;
	}

	protected boolean isSettingEnabled() {
		return "1".equals(Settings.Global.getString(getContentResolver(), settingName));
	}

	protected boolean hasPermission() {
		int result = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
		return result == PackageManager.PERMISSION_GRANTED;
	}

	private final ContentObserver observer = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			var tile = getQsTile();
			int state = isLocked()
					? Tile.STATE_UNAVAILABLE
					: isSettingEnabled()
							? Tile.STATE_ACTIVE
							: Tile.STATE_INACTIVE;
			onUpdateTile(tile, tile.getLabel(), state);
		}
	};

	@Override
	public void onStartListening() {
		var uri = Settings.Global.getUriFor(settingName);
		observer.onChange(true, uri);

		getContentResolver().registerContentObserver(uri, false, observer);
	}

	@Override
	public void onClick() {
		if (hasPermission()) {
			var tile = getQsTile();
			if (tile.getState() == Tile.STATE_ACTIVE) {
				Settings.Global.putString(getContentResolver(), settingName, "0");
				onUpdateTile(tile, tile.getLabel(), Tile.STATE_INACTIVE);
			} else if (canEnableSetting()) {
				Settings.Global.putString(getContentResolver(), settingName, "1");
				onUpdateTile(tile, tile.getLabel(), Tile.STATE_ACTIVE);
			} else {
				showCannotEnableSettingReason();
			}
		} else {
			showPermissionRequestDialog();
		}
	}

	protected void onUpdateTile(Tile tile, CharSequence label, int state) {
		if (!tile.getLabel().equals(label) || tile.getState() != state) {
			tile.setLabel(label);
			tile.setState(state);
			tile.updateTile();
		}
	}

	protected boolean canEnableSetting() {
		return true;
	}

	protected void showCannotEnableSettingReason() {}

	private void showPermissionRequestDialog() {
		var command = String.format(
				Locale.ROOT,
				"adb shell pm grant %s %s",
				getPackageName(),
				Manifest.permission.WRITE_SECURE_SETTINGS);

		var view = LayoutInflater.from(this).inflate(R.layout.permission_request_body, null);
		view.<TextView>findViewById(R.id.command).setText(command);

		var dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setView(view)
				.setPositiveButton(android.R.string.ok, null)
				.setNeutralButton(android.R.string.copy,
				                  (di, which) -> copyCommandToClipboard(command));
		showDialog(dialog.create());
	}

	private void copyCommandToClipboard(String command) {
		var manager = getSystemService(ClipboardManager.class);
		var data = ClipData.newPlainText(null, command);
		manager.setPrimaryClip(data);

		Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStopListening() {
		getContentResolver().unregisterContentObserver(observer);
	}

	public static class DeveloperOptions extends BaseDevQsTileService {

		public DeveloperOptions() {
			super(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
		}
	}

	public static class UsbDebugging extends BaseDevQsTileService {

		public UsbDebugging() {
			super(Settings.Global.ADB_ENABLED);
		}
	}

	public static class WirelessDebugging extends BaseDevQsTileService
			implements NsdManager.DiscoveryListener {

		public WirelessDebugging() {
			super("adb_wifi_enabled");
		}

		@Override
		public void onStartListening() {
			super.onStartListening();
			var nsdm = getSystemService(NsdManager.class);
			if (nsdm != null) {
				nsdm.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, this);
			}
		}

		@Override
		public void onDiscoveryStarted(String serviceType) {
			log("onDiscoveryStarted(%s)", serviceType);
		}

		@Override
		public void onStartDiscoveryFailed(String serviceType, int errorCode) {
			log("onStartDiscoveryFailed(%s, %d)", serviceType, errorCode);
		}

		@Override
		public void onServiceFound(NsdServiceInfo serviceInfo) {
			if (!isSettingEnabled()) return;
			log("onServiceFound(%s)", serviceInfo);

			var nsdm = getSystemService(NsdManager.class);
			nsdm.resolveService(serviceInfo, new NsdManager.ResolveListener() {
				@Override
				public void onServiceResolved(NsdServiceInfo serviceInfo) {
					WirelessDebugging.this.onServiceResolved(serviceInfo);
				}

				@Override
				public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
					WirelessDebugging.this.onResolveFailed(serviceInfo, errorCode);
				}
			});
		}

		void onServiceResolved(NsdServiceInfo serviceInfo) {
			if (!isSettingEnabled()) return;
			log("onServiceResolved(%s)", serviceInfo);

			var serviceAddr = serviceInfo.getHost().getHostAddress();
			log("Service Address: %s", serviceAddr);
			int servicePort = serviceInfo.getPort();
			log("Service Port: %d", servicePort);

			var ipv4Addr = getIpv4Address(this);
			log("IPv4 Address: %s", ipv4Addr);

			if (Objects.equals(serviceAddr, ipv4Addr)) {
				var label = String.format(Locale.ROOT, "%s : %d", serviceAddr, servicePort);
				int state = isLocked() ? Tile.STATE_UNAVAILABLE : Tile.STATE_ACTIVE;
				onUpdateTile(getQsTile(), label, state);
			}
		}

		void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
			log("onResolveFailed(%s, %d)", serviceInfo, errorCode);
		}

		@Override
		public void onServiceLost(NsdServiceInfo serviceInfo) {
			log("onServiceLost(%s)", serviceInfo);
		}

		@Override
		public void onDiscoveryStopped(String serviceType) {
			log("onDiscoveryStopped(%s)", serviceType);
		}

		@Override
		public void onStopDiscoveryFailed(String serviceType, int errorCode) {
			log("onStopDiscoveryFailed(%s, %d)", serviceType, errorCode);
		}

		@Override
		protected void onUpdateTile(Tile tile, CharSequence label, int state) {
			var newLabel = isSettingEnabled() ? label : getText(R.string.label_wireless_debugging);
			super.onUpdateTile(tile, newLabel, state);
		}

		@Override
		protected boolean canEnableSetting() {
			return isWifiConnected(this);
		}

		@Override
		protected void showCannotEnableSettingReason() {
			var dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.app_name)
					.setMessage(R.string.msg_network_connect)
					.setPositiveButton(android.R.string.ok, null);
			showDialog(dialog.create());
		}

		@Override
		public void onStopListening() {
			try {
				var nsdm = getSystemService(NsdManager.class);
				if (nsdm != null) {
					nsdm.stopServiceDiscovery(this);
				}
			} catch (IllegalArgumentException ignored) {
			}
			super.onStopListening();
		}

		private static String getIpv4Address(Context context) {
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

		private static boolean isWifiConnected(Context context) {
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

		private static void log(String format, Object... args) {
			// Log.d(TAG, String.format(Locale.ROOT, format, args));
		}
	}
}