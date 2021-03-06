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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public final class PickSubredditActivity extends ListActivity {
	
	private static final String TAG = "PickSubredditActivity";
	
	// Group 1: inner
    private final Pattern MY_SUBREDDITS_OUTER = Pattern.compile("your front page reddits.*?<ul>(.*?)</ul>", Pattern.CASE_INSENSITIVE);
    // Group 3: subreddit name. Repeat the matcher.find() until it fails.
    private final Pattern MY_SUBREDDITS_INNER = Pattern.compile("<a(.*?)/r/(.*?)>(.+?)</a>");

	private RedditSettings mSettings = new RedditSettings();
	private DefaultHttpClient mClient = Common.getGzipHttpClient();
	
	private PickSubredditAdapter mAdapter;
	private EditText mEt;
	
    public static final String[] SUBREDDITS_MINUS_FRONTPAGE = {
    	"reddit.com",
    	"pics",
    	"politics",
    	"wtf",
    	"funny",
    	"technology",
    	"askreddit",
    	"science",
    	"programming",
    	"gaming",
    	"worldnews",
    	"comics",
    	"offbeat",
    	"videos",
    	"environment",
    	"iama",
    	"business",
    	"entertainment",
    	"bestof",
    	"economics",
    	"marijuana",
    	"todayilearned",
    	"linux",
    	"android"
    };
    
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
    	Common.loadRedditPreferences(this, mSettings, mClient);
    	setRequestedOrientation(mSettings.rotation);
    	setTheme(mSettings.theme);
    	
        setContentView(R.layout.pick_subreddit_view);
        
        // Set the EditText to do same thing as onListItemClick
        mEt = (EditText) findViewById(R.id.pick_subreddit_input);
		mEt.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		        	returnSubreddit(mEt.getText().toString());
		        	return true;
		        }
		        return false;
		    }
		});
        final Button goButton = (Button) findViewById(R.id.pick_subreddit_button);
        goButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		returnSubreddit(mEt.getText().toString());
        	}
        });

        new DownloadRedditsTask().execute();
    }
    
    /**
     * Hack to explicitly set background color whenever changing ListView.
     */
    public void setContentView(int layoutResID) {
    	super.setContentView(layoutResID);
    	// HACK: set background color directly for android 2.0
        if (mSettings.theme == R.style.Reddit_Light)
        	getListView().setBackgroundResource(R.color.white);
    }
        
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String item = mAdapter.getItem(position);
        returnSubreddit(item);
    }
    
    private void returnSubreddit(String subreddit) {
       	Bundle bundle = new Bundle();
       	bundle.putString(ThreadInfo.SUBREDDIT, subreddit.trim());
       	Intent mIntent = new Intent();
       	mIntent.putExtras(bundle);
       	setResult(RESULT_OK, mIntent);
       	finish();	
    }
    
    class DownloadRedditsTask extends AsyncTask<Void, Void, ArrayList<String>> {
    	@Override
    	public ArrayList<String> doInBackground(Void... voidz) {
    		ArrayList<String> reddits = new ArrayList<String>();
    		HttpEntity entity = null;
            try {
            	HttpGet request = new HttpGet("http://www.reddit.com/reddits");
            	// Set timeout to 15 seconds
                HttpParams params = request.getParams();
    	        HttpConnectionParams.setConnectionTimeout(params, 15000);
    	        HttpConnectionParams.setSoTimeout(params, 15000);
    	        
    	        HttpResponse response = mClient.execute(request);
            	entity = response.getEntity();
            	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                
                String line = in.readLine();
                in.close();
                entity.consumeContent();
                
                Matcher outer = MY_SUBREDDITS_OUTER.matcher(line);
                if (outer.find()) {
                	Matcher inner = MY_SUBREDDITS_INNER.matcher(outer.group(1));
                	while (inner.find()) {
                		reddits.add(inner.group(3));
                	}
                } else {
                	return null;
                }
                
                return reddits;
                
            } catch (Exception e) {
            	if (Constants.LOGGING) Log.e(TAG, "failed:" + e.getMessage());
                if (entity != null) {
	                try {
	                	entity.consumeContent();
	                } catch (Exception e2) {
	                	// Ignore.
	                }
                }
            }
            return null;
	    }
    	
    	@Override
    	public void onPreExecute() {
    		showDialog(Constants.DIALOG_LOADING_REDDITS_LIST);
    	}
    	
    	@Override
    	public void onPostExecute(ArrayList<String> reddits) {
    		dismissDialog(Constants.DIALOG_LOADING_REDDITS_LIST);
			ArrayList<String> items;
    		if (reddits == null || reddits.size() == 0) {
    			// Need to make a copy because Arrays.asList returns List backed by original array
    	        items = new ArrayList<String>();
    			items.addAll(Arrays.asList(SUBREDDITS_MINUS_FRONTPAGE));
    		} else {
    			items = reddits;
    		}
    	    // Insert front page into subreddits list, unless suppressed by Intent extras
    		Bundle extras = getIntent().getExtras();
    	    if (extras != null) {
	        	boolean shouldHideFrontpage = extras.getBoolean(Constants.EXTRA_HIDE_FRONTPAGE_STRING, false);
	        	if (!shouldHideFrontpage)
	        		items.add(0, Constants.FRONTPAGE_STRING);
	        } else {
	        	items.add(0, Constants.FRONTPAGE_STRING);
	        }
	        mAdapter = new PickSubredditAdapter(PickSubredditActivity.this, items);
	        getListView().setAdapter(mAdapter);
	        Common.updateListDrawables(PickSubredditActivity.this, mSettings.theme);
	        
	        // Enable EditText focus, but set focus to ListView, so soft keyboard doesn't pop up.
	        final EditText pickSubredditInput = (EditText) findViewById(R.id.pick_subreddit_input);
	        pickSubredditInput.setFocusableInTouchMode(true);
	        getListView().requestFocus();
    	}
    }

    private final class PickSubredditAdapter extends ArrayAdapter<String> {
    	private LayoutInflater mInflater;
        private boolean mLoading = true;
        private int mFrequentSeparatorPos = ListView.INVALID_POSITION;

        
        public PickSubredditAdapter(Context context, List<String> objects) {
            super(context, 0, objects);
            
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setLoading(boolean loading) {
            mLoading = loading;
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == mFrequentSeparatorPos) {
                // We don't want the separator view to be recycled.
                return IGNORE_ITEM_VIEW_TYPE;
            }
            return super.getItemViewType(position);
        }

        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            // Here view may be passed in for re-use, or we make a new one.
            if (convertView == null) {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            } else {
                view = convertView;
            }
                        
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText(mAdapter.getItem(position));
            
            return view;
        }
    }
    
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	ProgressDialog pdialog;
    	
    	switch (id) {
	    	// "Please wait"
		case Constants.DIALOG_LOADING_REDDITS_LIST:
			pdialog = new ProgressDialog(this);
			pdialog.setMessage("Loading your reddits...");
			pdialog.setIndeterminate(true);
			pdialog.setCancelable(false);
			dialog = pdialog;
			break;
		default:
			throw new IllegalArgumentException("Unexpected dialog id "+id);
    	}
    	return dialog;
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
        final int[] myDialogs = {
        	Constants.DIALOG_LOADING_REDDITS_LIST,
        };
        for (int dialog : myDialogs) {
	        try {
	        	dismissDialog(dialog);
		    } catch (IllegalArgumentException e) {
		    	// Ignore.
		    }
        }
    }
}
