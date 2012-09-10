package jp.co.tis.stc.julius;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class JuliusActivity extends Activity {
	private static final String TAG = "Julius JulisuActivity";
	private static final String CONTINUOUS_JCONF = "/julius/fast-android.jconf";
	private static final String GRAMMAR_JCONF = "/julius/demo-grammar-android.jconf";
	
	private static final String WAVE_PATH = "/julius/voice.wav";
	private static final int SAMPLING_RATE = 22050;
	static {
		System.loadLibrary("julius_arm");
	}
	private native boolean initJulius(String jconfpath);
	private native void recognize(String wavpath);
	private native void terminateJulius();
	private boolean isInitialized = false;
	
	private AudioRecord audioRec = null;
	private int bufSize = 0;
	private boolean isRecording = false;
	private String resultStr = "";
	private RadioGroup radioGroup;
	private TextView resultText;
	private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_julius);
        resultText = (TextView)findViewById(R.id.result_text);
        
        button = (Button)findViewById(R.id.speech_button);
        button.setEnabled(false);
        button.setOnClickListener(onClickListener);
        
        radioGroup = (RadioGroup)findViewById(R.id.radiogroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				new JuliusInitializer(JuliusActivity.this).execute(checkedId);
			}
		});
        
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;
        audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC, 
        		SAMPLING_RATE,
        		AudioFormat.CHANNEL_CONFIGURATION_MONO,
        		AudioFormat.ENCODING_PCM_16BIT,
        		bufSize);
    }
	
	@Override
	protected void onDestroy() {
		if (isInitialized) {
			terminateJulius();
			isInitialized = false;
		}
		super.onDestroy();
	}

	private class JuliusInitializer extends AsyncTask<Integer, Void, Boolean> {
		private ProgressDialog progressDialog;
		Context context;
		
		public JuliusInitializer(Context context) {
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			Log.d(TAG, "JuliusInitializer:onPreExecute");
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage(JuliusActivity.this.getString(R.string.initializing_message));
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Integer... params) {
			if (isInitialized) {
				terminateJulius();
			}
			String conf;
			int checkedId = params[0];
			if (checkedId == R.id.continuous) {
				Log.d(TAG, "JuliusInitializer:doInBackground:conf is continuous");
				conf = CONTINUOUS_JCONF;
			}
			else if (checkedId == R.id.grammer) {
				Log.d(TAG, "JuliusInitializer:doInBackground:conf is grammer");
				conf = GRAMMAR_JCONF;
			}
			else {
				Log.d(TAG, "JuliusInitializer:doInBackground:invalid conf");
				return false;
			}
			
			if (initJulius(Environment.getExternalStorageDirectory() + conf)) {
				Log.d(TAG, "JuliusInitializer:doInBackground:init julius success");
				return true;
			}
			else {
				Log.e(TAG, "JuliusInitializer:doInBackground:init julius error");
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(TAG, "JuliusInitializer:onPostExecute");
			progressDialog.dismiss();
			if(result) {
				isInitialized = true;
				button.setEnabled(true);
			}
			else {
				isInitialized = false;
				button.setEnabled(false);
				Toast.makeText(context, "initJulius Error", Toast.LENGTH_LONG).show();
			}
		}
	}
    
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (isRecording) {
				Log.d(TAG, "call recognize");
				isRecording = false;
				button.setText(R.string.recogninzing);
				button.setEnabled(false);
				new JuliusRecognizer(JuliusActivity.this).execute(Environment.getExternalStorageDirectory() + WAVE_PATH);
			}
			else {
				Log.d(TAG, "start recording");
				isRecording = true;
				button.setText(R.string.recording);
				resultText.setText(JuliusActivity.this.getString(R.string.init_text));
				File recFile = new File(Environment.getExternalStorageDirectory() + WAVE_PATH);
				try {
					recFile.createNewFile();
					FileOutputStream fout =  new FileOutputStream(recFile);
					final DataOutputStream dout = new DataOutputStream(fout);
					audioRec.startRecording();
					new Thread(new Runnable(){
						@Override
						public void run() {
							android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
							short buf[] = new short[bufSize];
							while(isRecording) {
								audioRec.read(buf, 0, buf.length);
								try {
									for (short s : buf) {
										dout.writeShort(Short.reverseBytes(s));
									}
								} catch (IOException e) {
									Log.e(TAG, e.toString());
								}
							}
							audioRec.stop();
							Log.d(TAG, "end recording");
						}}).start();
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				}
			}
		}
    };
    
	private class JuliusRecognizer extends AsyncTask<String, Void, Void> {
		private ProgressDialog progressDialog;
		Context context;
		
		public JuliusRecognizer(Context context) {
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			Log.d(TAG, "JuliusRecognizer:onPreExecute");
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage(JuliusActivity.this.getString(R.string.recognizing_message));
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(String... params) {			
			String wavepath = params[0];
			recognize(wavepath);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(TAG, "JuliusRecognizer:onPostExecute");
			progressDialog.dismiss();
	    	TextView resultView = (TextView)findViewById(R.id.result_text);
	    	resultView.setText(resultStr);
	    	button.setText(R.string.speech);
			button.setEnabled(true);
		}
	}
    
    public void callback(byte[] result) {
    	Log.d(TAG, "callbacked");
    	StringBuilder bld = new StringBuilder();
    	for (byte b : result) {
    		bld.append(String.format("%02x ", b));
		}
    	Log.d(TAG, "result:" + bld.toString());
    	
    	try {
			resultStr = new String(result, "Shift_JIS");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.toString());
		}
    	Log.d(TAG, "callbacked " + resultStr);
    }
}
