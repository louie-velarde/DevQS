package me.velc.devqs;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class TrampolineActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finish();

		if ("1".equals(Settings.Global.getString(
				getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED))) {
			startActivity(makeIntent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));

		} else {
			try {
				var intent = makeIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				intent.setData(Uri.parse("package:" + getPackageName()));
				startActivity(intent);
				return;
			} catch (ActivityNotFoundException ignored) {}

			try {
				startActivity(makeIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
				return;
			} catch (ActivityNotFoundException ignored) {}

			try {
				startActivity(makeIntent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
				return;
			} catch (ActivityNotFoundException ignored) {}
		}
	}

	private static Intent makeIntent(String action) {
		var intent = new Intent(action);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		return intent;
	}
}