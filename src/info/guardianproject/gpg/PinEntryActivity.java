package info.guardianproject.gpg;

import java.io.UnsupportedEncodingException;

import info.guardianproject.gpg.pinentry.PinentryStruct;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PinEntryActivity extends Activity {

	static final String TAG = "PinEntryActivity";

	private PinentryStruct pinentry;
	private EditText pinEdit;
	private TextView description;
	private TextView title;
	private Button okButton;
	private Button cancelButton;

	private OnClickListener okClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			syncNotify();
			finish();
		}
	};

	private OnClickListener cancelClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			syncNotify();
			finish();
		}
	};

	static {
		System.load("/data/data/info.guardianproject.gpg/lib/libpinentry.so");
	}

//    private native void startPinentryLoop();
    private native void connectToGpgAgent();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pinentry);
		NativeHelper.setup(this);
		Log.d("PinEntryActivity", "PinEntryActivity::onCreate");

		description = (TextView) findViewById(R.id.description);
		title = (TextView) findViewById(R.id.title);
        okButton = (Button) findViewById(R.id.okButton);
        pinEdit = (EditText) findViewById(R.id.pinEdit);

        okButton.setOnClickListener(okClickListener);
//        cancelButton.setOnClickListener(cancelClickListener);

		new Thread( new Runnable() {

			@Override
			public void run() {
				connectToGpgAgent();
			}

		}).start();

	}

	private synchronized void syncNotify() {
		notify();
	}

	private synchronized void setPin() {
		if( pinentry == null) {
			Log.d(TAG, "setPin(): pinentry struct is null :(");
			return;
		}
		try {
			pinentry.pin = pinEdit.getText().toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException in setPin");
			e.printStackTrace();
		}
	}

	private synchronized void updateViews() {
		if( pinentry == null)
			Log.d(TAG, "pinentry struct is null :(");
		else {
			if( pinentry.title != null) {
				Log.d(TAG, "PinentryStruct.title: " + pinentry.title);
				title.setText(pinentry.title);
			} else {
				title.setText("");
			}
			if( pinentry.description != null) {
				Log.d(TAG, "PinentryStruct.description: " + pinentry.description);
				description.setText(pinentry.description);
			} else {
				title.setText("");
			}
		}
	}

	PinentryStruct setPinentryStruct(PinentryStruct s) {

		synchronized (this) {
			pinentry = s;
		}
		Log.d(TAG, "set pinentry, running update on UI thread");

		runOnUiThread(new Runnable() {
			public void run() {
				updateViews();
			}
		});

		synchronized (this) {
			Log.d(TAG, "waiting for user input");
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Log.d(TAG, "user input received, returning");
			return pinentry;
		}
	}
}