/*
 * Copyright 2009 Andrew Shu
 *
 * This file is part of "reddit is fun".
 *
 * "reddit is fun" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "reddit is fun" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "reddit is fun".  If not, see <http://www.gnu.org/licenses/>.
 */

package com.andrewshu.android.reddit;

import android.app.ProgressDialog;
import android.content.Context;

public class AutoResetProgressDialog extends ProgressDialog {

	public AutoResetProgressDialog(Context context) {
		super(context);
	}
	public AutoResetProgressDialog(Context context, int theme) {
		super(context, theme);
	}

	@Override
	public void onStart() {
		super.onStart();
		setProgress(0);
	}
}
