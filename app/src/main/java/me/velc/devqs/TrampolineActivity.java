package me.velc.devqs;

import static me.velc.devqs.Utils.execute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

public class TrampolineActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finish();

		if (showWifiDebuggingScreen(getIntent())) {
			try {
				final var pkgName = "com.android.settings";
				var res = getPackageManager().getResourcesForApplication(pkgName);

				@SuppressLint("DiscouragedApi")
				var titleResId = res.getIdentifier("adb_wireless_settings", "string", pkgName);
				execute("am", "start",
				        "-n", pkgName + '/' + pkgName + ".SubSettings",
				        "--es", ":settings:show_fragment",
				        pkgName + ".development.WirelessDebuggingFragment",
				        "--ei", ":settings:show_fragment_title_resid",
				        Integer.toString(titleResId),
				        "--activity-clear-task",
				        "--activity-task-on-home");
				return;
			} catch (PackageManager.NameNotFoundException ignored) {
			}
		}

		if ("1".equals(Settings.Global.getString(
				getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED))) {
			startActivity(makeIntent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
			return;
		}

		try {
			var intent = makeIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.setData(Uri.parse("package:" + getPackageName()));
			startActivity(intent);
			return;
		} catch (ActivityNotFoundException ignored) {
		}

		try {
			startActivity(makeIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
			return;
		} catch (ActivityNotFoundException ignored) {
		}

		try {
			startActivity(makeIntent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
			return;
		} catch (ActivityNotFoundException ignored) {
		}
	}

	private static final String WIRELESS_DEBUGGING =
			BaseDevQsTileService.WirelessDebugging.class.getName();

	private static Intent makeIntent(String action) {
		var intent = new Intent(action);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		return intent;
	}

	private static boolean showWifiDebuggingScreen(Intent intent) {
		ComponentName cName = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
		return cName != null &&
		       cName.getClassName().equals(WIRELESS_DEBUGGING) &&
		       Sui.isSui() &&
		       Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
	}
}