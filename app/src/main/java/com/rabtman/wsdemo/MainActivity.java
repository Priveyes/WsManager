package com.rabtman.wsdemo;

import android.content.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.format.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.*;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.core.content.*;
import androidx.core.view.*;

import com.rabtman.wsmanager.*;
import com.rabtman.wsmanager.listener.*;

import java.util.concurrent.*;

import okhttp3.*;
import okio.*;

public class MainActivity extends AppCompatActivity {

	private final static String TAG = "MainActivity";
	private WsManager wsManager;
	private TextView btn_send, btn_clear, tv_content;
	private Button btn_connect, btn_disconnect;
	private EditText edit_url, edit_content;
	private ScrollingView scrollingView;
	private WsStatusListener wsStatusListener = new WsStatusListener() {
		@Override
		public void onOpen(Response response) {
			Log.d(TAG, "WsManager-----onOpen");
			tv_content.append(Spanny.spanText("Server connection succeeded\n\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))));
		}

		@Override
		public void onMessage(String text) {
			Log.d(TAG, "WsManager-----onMessage");
			tv_content.append(Spanny.spanText("Server " + DateUtils.formatDateTime(getBaseContext(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME) + "\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))));
			tv_content.append(fromHtmlText(text) + "\n\n");
		}

		@Override
		public void onMessage(ByteString bytes) {
			Log.d(TAG, "WsManager-----onMessage");
		}

		@Override
		public void onReconnect() {
			Log.d(TAG, "WsManager-----onReconnect");
			tv_content.append(Spanny.spanText("Server reconnection...\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_red_light))));
		}

		@Override
		public void onClosing(int code, String reason) {
			Log.d(TAG, "WsManager-----onClosing");
			tv_content.append(Spanny.spanText("Server connection is down...\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_red_light))));
		}

		@Override
		public void onClosed(int code, String reason) {
			Log.d(TAG, "WsManager-----onClosed");
			tv_content.append(Spanny.spanText("Server connection is down\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_red_light))));
		}

		@Override
		public void onFailure(Throwable t, Response response) {
			Log.d(TAG, "WsManager-----onFailure");
			tv_content.append(Spanny.spanText("Server connection failed\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_red_light))));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btn_send = (TextView) findViewById(R.id.btn_send);
		btn_clear = (TextView) findViewById(R.id.btn_clear);
		tv_content = (TextView) findViewById(R.id.tv_content);
		btn_connect = (Button) findViewById(R.id.btn_connect);
		btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
		edit_url = (EditText) findViewById(R.id.edit_url);
		edit_content = (EditText) findViewById(R.id.edit_content);
		btn_connect.setOnClickListener(v -> {
			String url = edit_url.getText().toString();
			if (!TextUtils.isEmpty(url) && url.contains("ws")) {
				if (wsManager != null) {
					wsManager.stopConnect();
					wsManager = null;
				}
				wsManager = new WsManager.Builder(getBaseContext()).client(new OkHttpClient().newBuilder().pingInterval(15, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()).needReconnect(true).wsUrl(url).build();
				wsManager.setWsStatusListener(wsStatusListener);
				wsManager.startConnect();
			} else {
				Toast.makeText(getBaseContext(), "Please fill in the address you need to link", Toast.LENGTH_SHORT).show();
			}
		});

		btn_disconnect.setOnClickListener(v -> {
			if (wsManager != null) {
				wsManager.stopConnect();
				wsManager = null;
			}
		});

		btn_send.setOnClickListener(v -> {
			String content = edit_content.getText().toString();
			if (!TextUtils.isEmpty(content)) {
				if (wsManager != null && wsManager.isWsConnected()) {
					boolean isSend = wsManager.sendMessage(content);
					if (isSend) {
						tv_content.append(Spanny.spanText("Myself " + DateUtils.formatDateTime(getBaseContext(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME) + "\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_green_light))));
						tv_content.append(content + "\n\n");
					} else {
						tv_content.append(Spanny.spanText("Message failed to be sent\n", new ForegroundColorSpan(ContextCompat.getColor(getBaseContext(), android.R.color.holo_red_light))));
					}
					showOrHideInputMethod();
					edit_content.setText("");
				} else {
					Toast.makeText(getBaseContext(), "Please connect to the server first.", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(getBaseContext(), "Please fill in the content you need to send.", Toast.LENGTH_SHORT).show();
			}
		});

		btn_clear.setOnClickListener(v -> tv_content.setText(""));
	}

	@Override
	protected void onDestroy() {
		if (wsManager != null) {
			wsManager.stopConnect();
			wsManager = null;
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_source:
				//Bring up the browser update app
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri url = Uri.parse("https://github.com/Rabtman/WsManager");
				intent.setData(url);
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private Spanned fromHtmlText(String s) {
		Spanned result;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(s);
		}
		return result;
	}

	private void showOrHideInputMethod() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
	}

}
