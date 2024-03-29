/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class UrlUtil {

	public static void openUrl(Context context, String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, "No browser installed", Toast.LENGTH_LONG).show();
		}
	}

	public static void openUrlInBrowserExcludingWalletApp(Context context, Uri url) {
		List<String> availableBrowserPackages = Arrays.asList("com.android.browser", "com.android.chrome", "com.huawei.browser");
		for (String browserPackage : availableBrowserPackages) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(url);
				intent.setPackage(browserPackage);
				context.startActivity(intent);
				return;
			} catch (ActivityNotFoundException ignored) {

			}
		}

		Toast.makeText(context, "No browser installed", Toast.LENGTH_LONG).show();
	}

	/**
	 * Attempts to open the provided store URLs, first trying the playUrl and as a fallback huaweiUrl
	 *
	 * @param context the application context
	 * @param playUrl the Google Play Store URL (market://details?id=packageName) to open
	 * @param huaweiUrl the Huawei App Gallery URL (appmarket://details?id=packageName) to open
	 */
	public static void openStoreUrl(Context context, String playUrl, String huaweiUrl) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		PlatformUtil.PlatformType platformType = PlatformUtil.Companion.getPlatformType(context);

		if (platformType == PlatformUtil.PlatformType.HUAWEI) {
			intent.setData(Uri.parse(huaweiUrl));
			try {
				context.startActivity(intent);
			} catch (ActivityNotFoundException e2) {
				Toast.makeText(context, "No browser installed", Toast.LENGTH_LONG).show();
			}
		} else {
			intent.setData(Uri.parse(playUrl));
			try {
				context.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(context, "No browser installed", Toast.LENGTH_LONG).show();
			}
		}
	}
}
