package com.sgsoftware.mpfcremote;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.os.Handler;
import android.graphics.Color;

public class MainActivity extends Activity 
	implements View.OnClickListener, AdapterView.OnItemClickListener, INotificationHandler {
	private static RemotePlayer m_player;
	
	private Handler m_handler;
	
	private boolean m_notificationsDisabled;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        m_notificationsDisabled = false;
        
        m_handler = new Handler();

		((Button)findViewById(R.id.pauseBtn)).setOnClickListener(this);
		((Button)findViewById(R.id.refreshBtn)).setOnClickListener(this);
		((Button)findViewById(R.id.nextBtn)).setOnClickListener(this);
		((Button)findViewById(R.id.prevBtn)).setOnClickListener(this);
		((Button)findViewById(R.id.backBtn)).setOnClickListener(this);
		((ListView)findViewById(R.id.playListView)).setOnItemClickListener(this);

		tryConnect();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {
			Intent intent = new Intent()
				.setClass(this, com.sgsoftware.mpfcremote.PrefActivity.class);
			this.startActivityForResult(intent, 0);
		}
		else if (item.getItemId() == R.id.menu_playlist) {
			m_notificationsDisabled = true;
			Intent intent = new Intent()
				.setClass(this, com.sgsoftware.mpfcremote.PlaylistActivity.class);
			this.startActivityForResult(intent, 0);
			m_notificationsDisabled = false;
		}
		else if (item.getItemId() == R.id.menu_reconnect) {
			tryConnect();
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		super.onActivityResult(reqCode, resCode, data);
	}

	@Override
	public void onClick(View target) {
		switch (target.getId()) {
		case R.id.pauseBtn:
			m_player.pause();
			break;
		case R.id.nextBtn:
			m_player.next();
			break;
		case R.id.prevBtn:
			m_player.prev();
			break;
		case R.id.backBtn:
			m_player.timeBack();
			break;
		case R.id.refreshBtn:
			if (m_player != null)
				m_player.refresh();
			refresh();
			break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
		if (m_player != null)
			m_player.play(position);
	}
	
	private void refresh() {
		// Enable/disable controls
		boolean enabled = (m_player != null);
		findViewById(R.id.nextBtn).setEnabled(enabled);
		findViewById(R.id.pauseBtn).setEnabled(enabled);
		findViewById(R.id.prevBtn).setEnabled(enabled);
		findViewById(R.id.backBtn).setEnabled(enabled);
		findViewById(R.id.refreshBtn).setEnabled(true);
		if (enabled) {
			RemotePlayer.CurSong curSong = m_player.getCurSong();
			((TextView)findViewById(R.id.curSongTextView)).setText(curSong == null ? "" :
				String.format("%d. %s", curSong.posInList + 1, curSong.title));
			((TextView)findViewById(R.id.curTimeTextView)).setText(curSong == null ? "" :
				String.format("%d:%02d / %d:%02d", curSong.curPos / 60, curSong.curPos % 60,
					curSong.length / 60, curSong.length % 60));

			ListView playList = (ListView)findViewById(R.id.playListView);
			playList.setAdapter(new MyAdapter(m_player));
		}
		else {
			((TextView)findViewById(R.id.curSongTextView)).setText("Not connected");
			((ListView)findViewById(R.id.playListView)).setAdapter(null);
		}
	}
	
	private void tryConnect() {
		m_player = null;

		SharedPreferences prefs = getSharedPreferences("com.sgsoftware.mpfcremote_preferences", 0);
		String remoteAddr = prefs.getString("RemoteAddr", "");
		String remotePort = prefs.getString("RemotePort", "19792");

		try {
			m_player = new RemotePlayer(remoteAddr, Integer.parseInt(remotePort), this);
		}
		catch (java.net.UnknownHostException e) {
			m_player = null;
		}
		catch (java.io.IOException e) {
			m_player = null;
		}

		refresh();
	}

	public void processNotification(String msg) {
		if (m_notificationsDisabled)
			return;
		
		m_handler.post(new Runnable() {
			@Override
			public void run() {
				if (m_player != null)
					m_player.refresh();
				refresh();
			}
		});
	}

	public static RemotePlayer getPlayer() {
		return m_player;
	}

	private class MyAdapter extends BaseAdapter {
		private RemotePlayer m_player;

		public MyAdapter(RemotePlayer player) {
			m_player = player;
		}

		@Override
		public int getCount() {
			return m_player.getPlayList().size();
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		@Override
		public Object getItem(int pos) {
			return null;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater =
				(LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup vg = (ViewGroup)inflater.inflate(
					R.layout.playlistrow, parent, false);

			TextView tv_title = (TextView)vg.findViewById(R.id.playListItemTitle);
			TextView tv_len = (TextView)vg.findViewById(R.id.playListItemLength);

			RemotePlayer.Song s = m_player.getPlayList().get(position);
			tv_title.setText(String.format("%d. %s", position + 1, s.name));
			tv_len.setText(String.format("%d:%02d", s.length / 60, s.length % 60));

			int col = (m_player.getCurSong() != null && 
					   position == m_player.getCurSong().posInList) ?
					Color.RED : Color.LTGRAY;
			tv_title.setTextColor(col);
			tv_len.setTextColor(col);
			return vg;
		}
	}
}
