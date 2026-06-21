package me.velc.devqs;

import static me.velc.devqs.Utils.execute;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import rikka.shizuku.Shizuku;

public class ShizukuActivity extends Activity
		implements Shizuku.OnBinderReceivedListener,
		           Shizuku.OnBinderDeadListener,
		           Shizuku.OnRequestPermissionResultListener {

	private static final int REQ_SHIZUKU = 748;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Shizuku.addBinderReceivedListenerSticky(this);
		Shizuku.addBinderDeadListener(this);
		Shizuku.addRequestPermissionResultListener(this);
	}

	@Override
	public void onBinderReceived() {
		Shizuku.requestPermission(REQ_SHIZUKU);
	}

	@Override
	public void onBinderDead() {
		finish();
	}

	@Override
	public void onRequestPermissionResult(int requestCode, int grantResult) {
		if (requestCode == REQ_SHIZUKU && grantResult == PackageManager.PERMISSION_GRANTED) {
			execute("pm", "grant", getPackageName(), Manifest.permission.WRITE_SECURE_SETTINGS);
		}
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Shizuku.removeBinderReceivedListener(this);
		Shizuku.removeBinderDeadListener(this);
		Shizuku.removeRequestPermissionResultListener(this);
	}
}