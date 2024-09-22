package me.velc.devqs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public abstract class BaseDevQsTileService extends TileService {

	private final String settingName;

	protected BaseDevQsTileService(String settingName) {
		this.settingName = settingName;
	}

	protected boolean isActive() {
		return "1".equals(Settings.Global.getString(getContentResolver(), settingName));
	}

	protected boolean hasPermission() {
		int result = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
		return result == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public void onStartListening() {
		var tile = getQsTile();
		if (isLocked()) {
			tile.setState(Tile.STATE_UNAVAILABLE);
		} else if (isActive()) {
			tile.setState(Tile.STATE_ACTIVE);
		} else {
			tile.setState(Tile.STATE_INACTIVE);
		}
		tile.updateTile();
	}

	@Override
	public void onClick() {
		if (hasPermission()) {
			var tile = getQsTile();
			if (isActive()) {
				Settings.Global.putString(getContentResolver(), settingName, "0");
				tile.setState(Tile.STATE_INACTIVE);
			} else {
				Settings.Global.putString(getContentResolver(), settingName, "1");
				tile.setState(Tile.STATE_ACTIVE);
			}
			tile.updateTile();
		} else {
			showPermissionRequestDialog();
		}
	}

	protected void showPermissionRequestDialog() {
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

	public static class WirelessDebugging extends BaseDevQsTileService {

		public WirelessDebugging() {
			super("adb_wifi_enabled");
		}

		@Override
		public void onClick() {
			if (hasPermission() && !isActive() && !isWifiConnected(this)) {
				var dialog = new AlertDialog.Builder(this)
						.setTitle(R.string.app_name)
						.setMessage(R.string.msg_network_connect)
						.setPositiveButton(android.R.string.ok, null);
				showDialog(dialog.create());
			} else {
				super.onClick();
			}
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
	}
}