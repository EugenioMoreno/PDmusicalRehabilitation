package com.androidhive.musicplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import libsvm.svm_model;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.supportVector.*;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class AndroidBuildingMusicPlayerActivity extends Activity implements OnCompletionListener, SeekBar.OnSeekBarChangeListener, SensorEventListener {

	private ImageButton btnPlay;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist;
	private ImageButton btnRepeat;
	private ImageButton btnShuffle;
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	// Media Player
	private  MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;
	private SongsManager songManager;
	private Utilities utils;
	private int seekForwardTime = 5000; // 5000 milliseconds
	private int seekBackwardTime = 5000; // 5000 milliseconds
	private int currentSongIndex = 0; 
	private boolean isShuffle = false;
	private boolean isRepeat = false;
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	
	//accelerometer
	private Sensor accelerometer;
	private SensorManager sm;
	static boolean comenzarSesion;
	static boolean finalizarSesion=false;
	public static String horaMedida;
	final float alpha = (float) 0.8;
	private static float [] gravity = new float[3];
	
	
	private float xAcc;
	private float yAcc;
	private float zAcc;
	private float moduloAcc;
	//Android device name
	String deviceName = android.os.Build.MODEL;
	
	//Buffers for data and songs which have been played
	public static List<String> songList;

	//Files where results will be written
	public  static File file;
	public  static File fileTrain;
	public static String fileName;
	public static OutputStreamWriter archivo;
	FileOutputStream  outputStream;
	
	//-------------Calendar---------Use static Calendar
	//For file name(avoid domain names problems)
	public static SimpleDateFormat formatterFile = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	//For measures, with more accuracy
	public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
	
	//Machine Learning
		private static int fs=50;
		public static int numeroMedidas=10000/fs;
		public static List<MedidaAcc> arraySesionMusica;
		private static float [] signal = new float[numeroMedidas];
		private static float [] features = new float[2];//0:RMS-1:Range-2
		private static final String TAG = AndroidBuildingMusicPlayerActivity.class.getSimpleName();
		private sesionRehabilitacionTask rehabilitacionAsyncTask;
		
		
		//Dropbox
		private final static String appKey="jhr4kw4ukiuvb52";
		private final static String appSecret="wey3q430q5c7hz2";
		final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
		private DropboxAPI<AndroidAuthSession> mDBApi;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		//Dropbox
		// And later in some initialization function:
				AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
				AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
				mDBApi = new DropboxAPI<AndroidAuthSession>(session);
				mDBApi.getSession().startAuthentication(AndroidBuildingMusicPlayerActivity.this);
		//Accelerometer sensor
		sm=(SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		//Initialize lists
		arraySesionMusica = new ArrayList<MedidaAcc>();//List to save the results of acc data
		songList= new ArrayList<String>();//to save song which have been played
		
		// All player buttons
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
		btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		
		// Mediaplayer
		mp = new MediaPlayer();
		songManager = new SongsManager();
		utils = new Utilities();
		
		// Listeners
		songProgressBar.setOnSeekBarChangeListener(this); // Important
		mp.setOnCompletionListener(this); // Important
		
		// Getting all songs list
		songsList = songManager.getPlayList();
		
		// By default play first song
		//playSong(0);
				
		/**
		 * Play button click event
		 * plays a song and changes button to pause image
		 * pauses a song and changes button to play image
		 * */
		
		
		//Here we star a musical session
		btnPlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				
				
				// check for already playing and in this case we will stop the session.
				if(mp.isPlaying()){
					if(mp!=null){
						mp.pause();
						// Changing button image to play button
						btnPlay.setImageResource(R.drawable.btn_play);
						finalizarSesion=true;
						System.out.println("Cancelamos el timer");
					}
				}else{
					// Resume song
					
					if(mp!=null){
						mp.start();
						// Changing button image to pause button
						btnPlay.setImageResource(R.drawable.btn_pause);
						finalizarSesion=false;
						arraySesionMusica.clear();//Clean the array
						
						//The session will star in 5 seconds
						Toast.makeText(getApplication(), R.string.timer1, Toast.LENGTH_SHORT).show();	

						new CountDownTimer(5000, 30000) //5 seconds Timer.
						{
							@Override
							public void onTick(long millisUntilFinished){
						
							}

							@Override
							//Lo que quiero hacer cuando acabe el timer, en este caso llamar al mainTimer
							public void onFinish() {
								Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
								// Vibrate for 1000 milliseconds at the beginning of the season.
								v.vibrate(1000);
								guardarResultados();

							};
						}.start();
					}
				}
				
			}
		});
		
		/**
		 * Forward button click event
		 * Forwards song specified seconds
		 * */
		btnForward.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// get current song position				
				int currentPosition = mp.getCurrentPosition();
				// check if seekForward time is lesser than song duration
				if(currentPosition + seekForwardTime <= mp.getDuration()){
					// forward song
					mp.seekTo(currentPosition + seekForwardTime);
				}else{
					// forward to end position
					mp.seekTo(mp.getDuration());
				}
			}
		});
		
		/**
		 * Backward button click event
		 * Backward song to specified seconds
		 * */
		btnBackward.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// get current song position				
				int currentPosition = mp.getCurrentPosition();
				// check if seekBackward time is greater than 0 sec
				if(currentPosition - seekBackwardTime >= 0){
					// forward song
					mp.seekTo(currentPosition - seekBackwardTime);
				}else{
					// backward to starting position
					mp.seekTo(0);
				}
				
			}
		});
		
		/**
		 * Next button click event
		 * Plays next song by taking currentSongIndex + 1
		 * */
		btnNext.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// check if next song is there or not
				if(currentSongIndex < (songsList.size() - 1)){
					playSong(currentSongIndex + 1);
					currentSongIndex = currentSongIndex + 1;
				}else{
					// play first song
					playSong(0);
					currentSongIndex = 0;
				}
				
			}
		});
		
		/**
		 * Back button click event
		 * Plays previous song by currentSongIndex - 1
		 * */
		btnPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(currentSongIndex > 0){
					playSong(currentSongIndex - 1);
					currentSongIndex = currentSongIndex - 1;
				}else{
					// play last song
					playSong(songsList.size() - 1);
					currentSongIndex = songsList.size() - 1;
				}
				
			}
		});
		
		/**
		 * Button Click event for Repeat button
		 * Enables repeat flag to true
		 * */
		btnRepeat.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(isRepeat){
					isRepeat = false;
					Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}else{
					// make repeat to true
					isRepeat = true;
					Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isShuffle = false;
					btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}	
			}
		});
		
		/**
		 * Button Click event for Shuffle button
		 * Enables shuffle flag to true
		 * */
		btnShuffle.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(isShuffle){
					isShuffle = false;
					Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}else{
					// make repeat to true
					isShuffle= true;
					Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isRepeat = false;
					btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}	
			}
		});
		
		/**
		 * Button Click event for Play list click event
		 * Launches list activity which displays list of songs
		 * */
		btnPlaylist.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
				startActivityForResult(i, 100);			
			}
		});
		
	}
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		 // Do something here if sensor accuracy changes.	
	}

	public void onSensorChanged(SensorEvent event) {
		
		 // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        
        //Valores del acelerómetro sin componente continua
        xAcc = event.values[0] - gravity[0];
        yAcc = event.values[1] - gravity[1];
        zAcc = event.values[2] - gravity[2];
		moduloAcc=(float) Math.sqrt(xAcc*xAcc+yAcc*yAcc+zAcc*zAcc);

	
	}
	
	/**
	 * Receiving song index from playlist view
	 * and play the song
	 * */
	@Override
    protected void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100){
         	 currentSongIndex = data.getExtras().getInt("songIndex");
         	 // play selected song
             playSong(currentSongIndex);
        }
 
    }
	
	/**
	 * Function to play a song
	 * @param songIndex - index of song
	 * */
	public void  playSong(int songIndex){
		// Play song
		try {
        	mp.reset();
			mp.setDataSource(songsList.get(songIndex).get("songPath"));
			mp.prepare();
			mp.start();
			// Displaying Song title
			String songTitle = songsList.get(songIndex).get("songTitle");
        	songTitleLabel.setText(songTitle);
			
        	// Changing Button Image to pause image
			btnPlay.setImageResource(R.drawable.btn_pause);
			
			// set Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);
			
			// Updating progress bar
			updateProgressBar();			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);        
    }	
	
	/**
	 * Background Runnable thread
	 * */
	private Runnable mUpdateTimeTask = new Runnable() {
		   public void run() {
			   long totalDuration = mp.getDuration();
			   long currentDuration = mp.getCurrentPosition();
			  
			   // Displaying Total Duration time
			   songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
			   // Displaying time completed playing
			   songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));
			   
			   // Updating progress bar
			   int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
			   //Log.d("Progress", ""+progress);
			   songProgressBar.setProgress(progress);
			   
			   // Running this thread after 100 milliseconds
		       mHandler.postDelayed(this, 100);
		   }
		};
		
	/**
	 * 
	 * */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		
	}

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
    }
	
	/**
	 * When user stops moving the progress hanlder
	 * */
	@Override
    public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = mp.getDuration();
		int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
		
		// forward or backward to certain seconds
		mp.seekTo(currentPosition);
		
		// update timer progress again
		updateProgressBar();
    }

	/**
	 * On Song Playing completed
	 * if repeat is ON play same song again
	 * if shuffle is ON play random song
	 * */
	@Override
	public void onCompletion(MediaPlayer arg0) {
		
		// check for repeat is ON or OFF
		if(isRepeat){
			// repeat is on play same song again
			playSong(currentSongIndex);
		} else if(isShuffle){
			// shuffle is on - play a random song
			Random rand = new Random();
			currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
			playSong(currentSongIndex);
		} else{
			// no repeat or shuffle ON - play next song
			if(currentSongIndex < (songsList.size() - 1)){
				playSong(currentSongIndex + 1);
				currentSongIndex = currentSongIndex + 1;
			}else{
				// play first song
				playSong(0);
				currentSongIndex = 0;
			}
		}
	}
	
	@Override
	 public void onDestroy(){
	 super.onDestroy();
	    mp.release();
	 }
	
	private void guardarResultados(){
		
		//Create a .txt file
		//Change the user name: device name+kind of session+ date
		
		String nombreUsuario= deviceName;
		fileName= nombreUsuario +"_"+ formatterFile.format(Calendar.getInstance().getTime())+ ".arff";
	
		//where we want to save the file:
		File sdCard = Environment.getExternalStorageDirectory();
		final File dir = new File(sdCard.getAbsolutePath() + "/PD/Results/");
		dir.mkdirs(); //create folders where write files
		file = new File(dir + "/" +  fileName);
		
		
		//Create the file and the object to write in it. Catch the exception
		try{
            file.createNewFile();			
            FileOutputStream fOut = new FileOutputStream(file);
            archivo = new OutputStreamWriter(fOut);
     
		}
		catch(Exception e){
           Log.v("MainActivity", e.toString());
        }
		//write the header for an .arff file
		String header="%Rango, RMS, SessionType"+"\r\n"+
"@RELATION 50ms"+"\r\n"+
"@ATTRIBUTE range  NUMERIC"+"\r\n"+
"@ATTRIBUTE rms  NUMERIC"+"\r\n"+
"@ATTRIBUTE class        {walking, resting, upStairs, downStairs}"+"\r\n"+
"@DATA"+"\r\n";
		try {
			archivo.append(header);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Star a new rehabilitation session
		rehabilitacionAsyncTask = new sesionRehabilitacionTask();
		rehabilitacionAsyncTask.execute();
		
	}

	
	private class sesionRehabilitacionTask extends AsyncTask<Void, Integer, Void>{

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			Log.v("MainActivity",  "comienza el timer");
			
			while(!finalizarSesion){
				horaMedida=formatter.format(Calendar.getInstance().getTime());//take a measure
				MedidaAcc nuevaMedida= new MedidaAcc(xAcc,yAcc,zAcc,moduloAcc,formatter.format(Calendar.getInstance().getTime()));
				arraySesionMusica.add(nuevaMedida);
				
				
				//after 10 seconds of taking measure, we calculate the features
				if(arraySesionMusica.size()==(10000/fs)){
					//take modules
					for (int i = 0; i < arraySesionMusica.size(); i++) {
						float modulo=arraySesionMusica.get(i).getModulo();
						signal[i]=modulo;
						
					}
					
					//Range
					float maxValue=signal[0];
					float minValue=signal[0];
					for (int i = 0; i < signal.length; i++) {
						if(signal[i]>maxValue){
							maxValue=signal[i];
						}
						if(signal[i]<minValue){
							minValue=signal[i];
						}
					}
					float range=maxValue-minValue;
					features[0]=range;
					
					//RMS
					float x=0;
					float n=0;
					for (int i = 0; i < signal.length; i++) {
						x=(float) (x + Math.pow(signal[i], 2));
						n++;
					}
					float rms=(float) Math.sqrt(x/n);
					features[1]=rms;
					
					
					
					//create a string to write the features in the arff file
					String stringFeatures= features[0] +"," +features[1]+ ","+"?" +"%"+ songTitleLabel.getText().toString() +"\r\n";//"\r\n for Windows"
					//Save the songs´titles
					songList.add(songTitleLabel.getText().toString());
					System.out.println(songList.toString());
					
					
					try {
						//write the string into the file
						archivo.append(stringFeatures);
						Log.v("cancion",  songTitleLabel.toString());
						Log.v("MainActivity",  "escribimos feature");
						System.out.println("wirte a feature");

					} 
					catch (Exception e) {
				           Log.v("MainActivity", e.toString());
					}
					//OVERLAP: 50%
					arraySesionMusica=arraySesionMusica.subList(numeroMedidas/2, arraySesionMusica.size());
					
				}
				

				try {
					Thread.sleep(fs);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return null;
		}
		protected void onPostExecute(Void unused) {
			//We the user ends the session, we have to closed the file
			
				try {
					archivo.close();
					System.out.println("File Closed");
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("END OF THE SESSION!");
				
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				// Vibrate for 2000 milliseconds at the end of the season.
				v.vibrate(2000);
			
				
				//we call the classifier, with the results we have taken
				try {
					classifier();
				} catch (Exception e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				
				//Upload to dropbox when we have closed the definitive file
				File file2= new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PD/"+fileName+"_"+"Result.arff");
				FileInputStream inputStream = null;
				try {
					inputStream = new FileInputStream(file2);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					com.dropbox.client2.DropboxAPI.Entry response=mDBApi.putFile(fileName, inputStream, file2.length(), null, null);
					//Log.i("DbExampleLog", "The uploaded file's rev is: " + newEntry.rev);
				} catch (DropboxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			
			
		}
		
	}
	
	protected void onResume() {
	    super.onResume();

	    if (mDBApi.getSession().authenticationSuccessful()) {
	        try {
	            // Required to complete auth, sets the access token on the session
	            mDBApi.getSession().finishAuthentication();

	            AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}
private void classifier() throws Exception{
	
	Log.v("Classifier",  "entramos en el clasificador");
		//Run the trainer
		BufferedReader breader=null;
		breader=new BufferedReader(new FileReader (Environment.getExternalStorageDirectory().getAbsolutePath() + "/PD/WEKA50msAPP.arff"));

		Instances train=new Instances (breader);
		train.setClassIndex(train.numAttributes()-1);
		
		//take the file which contains the results of the session
		breader=new BufferedReader(new FileReader(file));
		Instances test=new Instances (breader);
		test.setClassIndex(train.numAttributes()-1);
		breader.close();
		//Create a classifier
		//J48 tree = new J48();
		
		LibSVM tree=new LibSVM();
		tree.setCoef0(0.0);
		tree.setCost(12.5);
		tree.setDebug(false);
		tree.setDegree(3);
		tree.setDoNotReplaceMissingValues(false);
		tree.setEps(0.001);
		tree.setGamma(0.0);
		tree.setLoss(0.1);
		tree.setNormalize(false);
		tree.setNu(0.5);
		tree.setProbabilityEstimates(true);
		tree.setSeed(1);
		tree.setShrinking(true);
		tree.setWeights("0.75 0.4 0.9 0.99");
		
		tree.buildClassifier(train);
		Instances labeled = new Instances (test);
		for (int i = 0; i < test.numInstances(); i++) {
			double clsLabel=tree.classifyInstance(test.instance(i));
			labeled.instance(i).setClassValue(clsLabel);
			
		}
		
		BufferedWriter writer =new BufferedWriter (
				new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PD/"+fileName+"_"+"Result.arff"));
		String someTextLines=labeled.toString();
		//Split results in order to visualize them better
		String[] lines = someTextLines.split("[\\r\\n]+");
		for (int i = 0; i < lines.length; i++) {
			if(i>4){
				//write the results of the classifier with the song which was been playing
			writer.write(lines[i]+"%"+songList.get(i-5)+"\r\n");
			}else{
				writer.write(lines[i]+"\r\n");
			}
		
		}
			
		writer.close();
	
	}
}