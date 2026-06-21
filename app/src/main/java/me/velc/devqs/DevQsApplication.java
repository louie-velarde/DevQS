package me.velc.devqs;

import android.app.Application;

import rikka.sui.Sui;

public class DevQsApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		Sui.init(getPackageName());
	}
}