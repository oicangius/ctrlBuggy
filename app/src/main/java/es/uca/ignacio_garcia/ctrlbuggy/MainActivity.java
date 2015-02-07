package es.uca.ignacio_garcia.ctrlbuggy;

//import es.uca.ignacio_garcia.contolbluetooth.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Build;
import android.preference.PreferenceManager;

public class MainActivity extends Activity implements OnTouchListener {
	ImageView iv;
	RelativeLayout rl;
	TextView tv1,tv2,tv3;
	Button bt1,bt2;
	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    volatile boolean stopWorker;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    String cad1,cad2,cad3;
    
    String device,password;
    boolean reverse;
    int reverseValue,minAngle,minSpeed,margin;
    
    int[] sensorsV;

    boolean conectado=false;

    long tultimocambio=0;
    
    void pintaTV1(String ss) {
		cad1 = ss;
		new Thread(new Runnable() {
			public void run() {
				tv1.post(new Runnable() {
					public void run() {
						tv1.setText(cad1);
					}
				});
			}
		}).start();
	}
    void pintaTV2(String ss) {
		cad2 = ss;
		new Thread(new Runnable() {
			public void run() {
				tv2.post(new Runnable() {
					public void run() {
						tv2.setText(cad2);
					}
				});
			}
		}).start();
	}
    void pintaTV3(String ss) {
		cad3 = ss;
		new Thread(new Runnable() {
			public void run() {
				tv3.post(new Runnable() {
					public void run() {
						tv3.setText(cad3);
					}
				});
			}
		}).start();
	}


    private void displaySharedPreferences() {
    	   SharedPreferences prefs = PreferenceManager
    	    .getDefaultSharedPreferences(this);
    	 
    	   device = prefs.getString("device", "Meteo");
    	   password = prefs.getString("password", "1234");
    	   reverse = prefs.getBoolean("reverse", false);
    	   
    	   reverseValue = -Integer.parseInt(prefs.getString("reverseValue", "120"));
    	   minAngle = Integer.parseInt(prefs.getString("minAngle", "10"));
    	   minSpeed = Integer.parseInt(prefs.getString("minSpeed", "110"));
    	   margin = Integer.parseInt(prefs.getString("margin", "10"));
    	 
    	   //StringBuilder builder = new StringBuilder();
    	   /*
    	   Log.d("Device: " + device,"<<<");
    	   Log.d("Password: " + password,"<<<");
    	   Log.d("Keep me logged in: " + String.valueOf(reverse),"<<<");
    	   
    	   Log.d("reverseValue: " + reverseValue,"<<<");
    	   Log.d("minAngle: " + minAngle,"<<<");
    	   Log.d("minSpeed: " + minSpeed,"<<<");
    	   Log.d("margin: " + margin,"<<<");
			*/
    	 
    	   //textView.setText(builder.toString());
    	  //Log.d(""+builder.toString(),"<<<");
    	   this.sensorsV=new int[5];
    }
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		iv=(ImageView)findViewById(R.id.imageView1);
		tv1=(TextView)findViewById(R.id.textView1);
		tv2=(TextView)findViewById(R.id.textView2);
		tv3=(TextView)findViewById(R.id.textView3);
		//bt1=(Button)findViewById(R.id.button1);
		//bt2=(Button)findViewById(R.id.button2);		

		iv.setOnTouchListener(this);
		
		Log.d("dntro de onCreate","<<<");
		
		displaySharedPreferences();
		
		//findBT();
		
		
	}

	void findBT()
	{
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
        	Log.d("No bluetooth adapter available","<<<");
        }
        
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
	        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
            	Log.d("Dispositivo:"+device.getName(),"<<<");
                if(device.getName().equals("Meteo")) 
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Log.d("Bluetooth Device Found","<<<");
	}
	
	void openBT() throws IOException
    {
		Log.d("1 openBT","<<<");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        Log.d("2 openBT","<<<");
        if(mmInputStream!=null)
        	Log.d("mmInputStream is not null","<<<");
        else 
        	Log.d("mmInputStream is null","<<<");
        beginListenForData();
        
        Log.d("Bluetooth Opened","<<<");
        
        this.conectado=true;
    }
	
	
	void beginListenForData()
    {
        final Handler handler = new Handler(); 
        final byte delimiter = 13; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
            	int bytesAvailable;
            	while(!Thread.currentThread().isInterrupted() && !stopWorker)
            	{
            		/*
            		try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
                    try 
                    {
                        bytesAvailable= mmInputStream.available();
                        
                        //Log.d("Dentro de run: "+bytesAvailable,"<<<");
                        if(bytesAvailable > 0)
                        {
                        	//Log.d("Dentro del if: "+bytesAvailable,"<<<");
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            
                            //Log.d("packetBytes:"+new String(packetBytes),"<<<");
                            
                            for(int i=0;i<bytesAvailable;i++)
                            {
                            	//Log.d("Dentro del for: "+bytesAvailable,"<<<");
                                byte b = packetBytes[i];
                            	//Log.d("b="+b+" "+" readBufferPosition="+readBufferPosition,"<<<");
                            	if(b==10){
                            		//Log.d("10 encontrado","<<<");
                            		continue;
                            	}
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes);//, "US-ASCII");
                                    if(data.charAt(0)=='#'){
                                    	int j;
                                    	String[] vs=data.split("-");
                                    	for(j=0;j<vs.length;j++){
                                    		Log.d("vs="+vs[j],"<<<");
                                    		if(j==0)
                                    			sensorsV[j]=Integer.parseInt(vs[j].substring(1));
                                    		else
                                    			sensorsV[j]=Integer.parseInt(vs[j]);
                                    	}
                                    	pintaTV1("SV="+sensorsV[0]+" "+sensorsV[1]+" "+sensorsV[2]+" "+sensorsV[3]+" "+sensorsV[4]+" ");
                                    }
                                    //Log.d(data+" ","<<<");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                    	public void run()
                                    	{
                                        	Log.d("Recivido por comm serie:"+data+"","<<<");
                                        	tv2.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                	readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    }
	
	void sendData() throws IOException
    {
        String msg = "hola";
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        Log.d("Data Sent","<<<");
    }
	
	void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d("Bluetooth Closed","<<<");
        this.conectado=false;
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.config:
			Log.d("config option","<<<");
			intent = new Intent(this, PrefsActivity.class);
			startActivity(intent);
			return true;
		case R.id.bt:
			Log.d("opcion 1, open find and open bt","<<<");
			try 
	        {
				findBT();
				Log.d("en menu antes de openBT","<<<");
	            openBT();
	        }
			catch (IOException ex) { }
			return true;
		case R.id.cutbt:
			Log.d("opcion 2, cortar bt","<<<");
			try 
	        {
	            closeBT();
	        }
	        catch (IOException ex) { }
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
		
		
		
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			Log.d("dentro de PlaceholderFragment.onCreateView","<<<");
			return rootView;
		}
	}
	
	public void clickPausa(View arg0) {
		Log.d("boton pausa pulsado","<<<");
	}

	public void clickContinuar(View arg0) {
		Log.d("boton continuar pulsado","<<<");
	}
	
	
	int calcX(int x){
		int marginR=320-margin;
		int maxAngle=180-minAngle;
		int y;
		// f(margin)=minAngle
		// f(marginR)=maxAngle
		y=Math.round((float)minAngle*(float)(x-marginR)/(float)(margin-marginR)+(float)maxAngle*(float)(x-margin)/(float)(marginR-margin));
		if(y<minAngle)
			y=minAngle;
		if(y>maxAngle)
			y=maxAngle;
		return y;
	}
	int calcY(int y){
		if(y<margin)
			return 255;
		if(margin<=y && y<=	(200-margin)){
			y=Math.round((float)255*(float)(y-(200-margin))/(float)(margin-(200-margin))+(float)minSpeed*(float)(y-margin)/(float)((200-margin)-margin));
			return y;
		}
		if((200-margin)<y && y<=200)
			return minSpeed;
		if(200<y && y<=275)
			return 0;
		if(275<=y)
			if(reverse)
				return reverseValue;
			else return 0;
		return y;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		int action = arg1.getAction();
	    int x = (int) arg1.getX();       
	    int y = (int) arg1.getY();
	    //Log.d("pos:"+x+"-"+y+" Action:"+action+" View:"+arg0.toString(),"<<<");
		//Log.d(iv.getWidth()+"***"+iv.getHeight(),"<<<");

	    x=calcX(x);
	    y=calcY(y);
	    String msg=y+"+"+x;
	    
	    Log.d("Mensaje:"+msg,"<<<");

    	long aux=System.currentTimeMillis();
    	long dt=aux-this.tultimocambio;
	    
	    if(MotionEvent.ACTION_OUTSIDE==action){
	    	Log.d("ACTION_OUTSIDE "+msg.toString(),"<<<");
	    	this.pintaTV3(msg);	    	
	    	return false;
	    }
	    if(MotionEvent.ACTION_MOVE==action){
	    	if(dt>200){
	    		this.tultimocambio=aux;
	    		this.pintaTV1(dt+"");
	    		Log.d("ACTION_MOVE "+msg.toString(),"<<<");
	    		this.pintaTV3(msg);
	    		if(conectado){
	    			try{
	    				mmOutputStream.write(msg.getBytes());
	    			}
	    			catch(IOException e){}
	    		}			
	    	}
	    	return true;
	    }
	    if(MotionEvent.ACTION_DOWN==action){
	    	if(dt>200){
	    		this.tultimocambio=aux;
	    		this.pintaTV1(dt+"");
	    		Log.d("ACTION_DOWN "+x+" "+y,"<<<");
	    		this.pintaTV3(msg);
	    		if(conectado){
	    			try{
	    				mmOutputStream.write(msg.getBytes());
	    				mmOutputStream.flush();
	    			}
	    			catch(IOException e){}
	    		}
	    	}
	    	return true;
	    }
	    if(MotionEvent.ACTION_UP==action){
	    	Log.d("ACTION_UP "+msg,"<<<");
	    	this.pintaTV3(msg);
	    	/*
	    	if(conectado){
	    		try{
	    			mmOutputStream.write(msg.getBytes());
	    		}
	    		catch(IOException e){}
	    	}*/
	    	return false;
	    }
	    return true;
	    
	}


}
