package com.rsjian2.cs296;

import java.net.InetAddress;
import android.widget.LinearLayout;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.rsjian2.cs296.R;

public class ServerActivity extends Activity {
	private Button newGameButton;
	private EditText serverPortEditText;
	private EditText serverNameEditText;
	private EditText komiEditText;
	private EditText boardSizeEditText;
	private int port;
	private int komi;
	private int boardSize;
	int color;
	ScrollView gameInfoScrollView;
	TextView gameInfoTextView;
	Button startButton;
	String ip;
	
	Game game;
	LinearLayout serverLayout;
	BoardView boardView;
    LinearLayout gameLayout;
    TextView colorTextView;
    Button passButton;
    Button resignButton;
    
    class OnMoveCallback implements Callback {
    	@Override
    	public void run(Object o) {
    		onMovePlayed(o);
    	}
    }
	
    public void onMovePlayed(Object o) {
    	Integer idx = (Integer)o;
    	HashSet<Integer> changes = (HashSet<Integer>)game.setStone(idx);
    	// move valid
    	if(changes.size() != 0) {
    		boardView.setBoard(game.getBoard());
    		notifyMovePlayed(idx);
    	}
    	boardView.locked = true;
    }
    
    public void playMove(int idx) {
    	Log.d("asdf", "playMove");
    	if(idx == -1) { 
    		game.passTurn();
    	} else {
    		game.setStone(idx);
    	}
    	boardView.setBoard(game.getBoard());
    	boardView.locked = false;
    }
    
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("asdf", ex.toString());
	    }
	    return null;
	}
	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_layout);
        newGameButton = (Button)findViewById(R.id.newGameButton);
        serverPortEditText = (EditText)findViewById(R.id.serverPortEditText);
        komiEditText = (EditText)findViewById(R.id.komiEditText);
        boardSizeEditText = (EditText)findViewById(R.id.boardSizeEditText);
        serverNameEditText = (EditText)findViewById(R.id.serverNameEditText);
        gameInfoScrollView = (ScrollView)findViewById(R.id.serverGameInfoScrollView);
        gameInfoTextView = (TextView)findViewById(R.id.serverGameInfoTextView);
        startButton = (Button)findViewById(R.id.startButton);
        
        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int _port = Integer.parseInt(serverPortEditText.getText().toString());
                int _komi = Integer.parseInt(komiEditText.getText().toString());
                int _boardSize = Integer.parseInt(boardSizeEditText.getText().toString());
                port = _port;
                komi = _komi;
                boardSize = _boardSize;
                
                // get IP address that server is listening on
    			WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
    			ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    			
    			Log.d("asdf", ip);
            	Log.d("asdf", getLocalIpAddress());
            	runOnUiThread(new Runnable() {
        	    	public void run() {
        	        	gameInfoTextView.append("Listening on " + ip + ", port " + port + "\n");
        	        	gameInfoScrollView.fullScroll(View.FOCUS_DOWN);
        	    	}
            	});
                startServerThread(port);
            }
        });
        
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                confirmGame();
            }
        });
        startButton.setEnabled(false);

    }
    
    class ViewSwitch implements Runnable {
    	ViewGroup v1;
    	ViewGroup v2;
    	ViewSwitch(ViewGroup v1, ViewGroup v2) { this.v1 = v1; this.v2 = v2; } 
    	public void run() {
    		ViewGroup parent = (ViewGroup) v1.getParent();
    		parent.removeView(v1);
    		parent.addView(v2);
    	}
    }
    
    public void startGame(int color) {
    	/*
    	Intent intent = new Intent(this, GameActivity.class);
    	startActivity(intent);	
    	*/
    	Log.d("asdf", "server start game!!");
    	this.color = color;
    	
    	game = new Game(Game.POSITIONAL, false, boardSize);
    	
    	serverLayout = (LinearLayout)findViewById(R.id.serverLayout);

		boardView = new BoardView(this, game.getBoard(), new OnMoveCallback(), color == 1);
        gameLayout = (LinearLayout)View.inflate(ServerActivity.this, R.layout.game_layout, null);
    	gameLayout.addView(boardView, 0);
    	
    	colorTextView = new TextView(ServerActivity.this);
    	colorTextView.setText("You are: " + ((color == 1) ? "WHITE":"BLACK"));
    	gameLayout.addView(colorTextView, 0);
        runOnUiThread(new ViewSwitch(serverLayout, gameLayout)); // still not sure why runOnUiThread is needed
    	passButton = (Button)gameLayout.findViewById(R.id.passButton);
    	//resignButton = (Button)gameLayout.findViewById(R.id.resignButton);
    	passButton.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
            	game.passTurn();
            	boardView.setBoard(game.getBoard());
            	notifyMovePlayed(-1);
            	boardView.locked = true;
            	Toast.makeText(ServerActivity.this, "Passed", 5000).show();
    		}
    	});
    	/*
    	resignButton.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			boardView.locked = true;
    			Toast.makeText(ServerActivity.this, ((ServerActivity.this.color == 1) ? "White" : "Black") + " wins by resignation", 5000).show();
    			notifyMovePlayed(-2);
    		}
    	});
    	*/
    	
    }
    
    public void enableStart() {
    	runOnUiThread(new Runnable() {
	    	public void run() {
	    		startButton.setEnabled(true);
	    		gameInfoTextView.append("Received game request\n");
	        	gameInfoScrollView.fullScroll(View.FOCUS_DOWN);
	    	}
    	});
    }
    
    public void endGame() {
    	Log.d("asdf", "endGame()");

    	int blackScore = game.getBoard().score(Game.BLACK);
    	int whiteScore = game.getBoard().score(Game.WHITE) + komi;
  
    	int diff = blackScore - whiteScore;
    	String output = "Black: " + blackScore + "\n" + "White: " + whiteScore + "\n";
    	if(diff > 0) {
    		output += "Black wins by " + diff + "\n";
    	} else {
    		output += "White wins by " + (-diff) + "\n";
    	}
    	final String toast = output;
    	Log.d("asdf", "toast");
    	runOnUiThread(new Runnable() {
    		public void run() {
    			Toast.makeText(ServerActivity.this, toast, 5000).show();
    		}
    	});
    	Log.d("asdf", output);
    	boardView.locked = true;
    }
    
    private native void startServerThread(int port);
    private native void confirmGame();
    private native void notifyMovePlayed(int idx);
}
