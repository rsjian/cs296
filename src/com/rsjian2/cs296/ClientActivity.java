package com.rsjian2.cs296;


import java.util.HashSet;

import com.rsjian2.cs296.R;
import com.rsjian2.cs296.ServerActivity.OnMoveCallback;
import com.rsjian2.cs296.ServerActivity.ViewSwitch;

import android.net.wifi.WifiManager;
import android.widget.ScrollView;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.widget.LinearLayout;
import android.content.Context;
import android.content.Intent;

public class ClientActivity extends Activity {
	LinearLayout clientLayout;
	private Button connectButton;
	private EditText clientPortEditText;
	private EditText clientNameEditText;
	private EditText clientIPEditText;
	private int port;
	private String ip;
	
	int boardSize;
	int komi;
	int color;
	ScrollView gameInfoScrollView;
	TextView gameInfoTextView;
	Button acceptButton;
	
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
    
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_layout);
        
        clientLayout = (LinearLayout)findViewById(R.id.clientLayout);
        connectButton = (Button)findViewById(R.id.connectButton);
        clientPortEditText = (EditText)findViewById(R.id.clientPortEditText);
        clientIPEditText = (EditText)findViewById(R.id.clientIPEditText);
        clientNameEditText = (EditText)findViewById(R.id.clientNameEditText);
        gameInfoScrollView = (ScrollView)findViewById(R.id.gameInfoScrollView);
        gameInfoTextView = (TextView)findViewById(R.id.gameInfoTextView);
    	acceptButton = (Button)findViewById(R.id.acceptButton);
    	
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ip = clientIPEditText.getText().toString();
                int _port = Integer.parseInt(clientPortEditText.getText().toString());
                port = _port;
                startClientThread(ip, port);
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	runOnUiThread(new Runnable() {
        	    	public void run() {
        	        	gameInfoTextView.append("Sent game acceptance to server\n");
        	        	gameInfoScrollView.fullScroll(View.FOCUS_DOWN);
        	        	requestGame();
        	    	}
            	});
            }
        });
        acceptButton.setEnabled(false);
    }
    private native void startClientThread(String ip, int port);
    
    public void loadGameInfo(int boardSize, int komi) {
    	this.boardSize = boardSize;
    	this.komi = komi;
    	Log.d("asdf", "!!" + boardSize + " " + komi);
    	
    	final int _komi = komi;
    	final int _boardSize = boardSize;
    	runOnUiThread(new Runnable() {
	    	public void run() {
	        	gameInfoTextView.append("Board Size: " + _boardSize + "\n");
	        	gameInfoTextView.append("Komi: " + _komi + "\n");
	        	gameInfoScrollView.fullScroll(View.FOCUS_DOWN);
	        	acceptButton.setEnabled(true);
	    	}
    	});
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
    	
    	serverLayout = (LinearLayout)findViewById(R.id.clientLayout);

		boardView = new BoardView(this, game.getBoard(), new OnMoveCallback(), color == 1);
        gameLayout = (LinearLayout)View.inflate(ClientActivity.this, R.layout.game_layout, null);
    	gameLayout.addView(boardView, 0);
    	
    	colorTextView = new TextView(ClientActivity.this);
    	colorTextView.setText("You are: " + ((color == 1) ? "WHITE":"BLACK"));
    	gameLayout.addView(colorTextView, 0);

        runOnUiThread(new ViewSwitch(clientLayout, gameLayout)); // still not sure why runOnUiThread is needed
    	passButton = (Button)gameLayout.findViewById(R.id.passButton);
    	//resignButton = (Button)gameLayout.findViewById(R.id.resignButton);
    	passButton.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			game.passTurn();
            	boardView.setBoard(game.getBoard());
            	notifyMovePlayed(-1);
            	boardView.locked = true;
            	Toast.makeText(ClientActivity.this, "Passed", 5000).show();
    		}
    	});
    	/*
    	resignButton.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			boardView.locked = true;
    			Toast.makeText(ClientActivity.this, ((ClientActivity.this.color == 1) ? "White" : "Black") + " wins by resignation", 5000).show();
				notifyMovePlayed(-2);
    		}
    	});
    	*/
    	
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
    			Toast.makeText(ClientActivity.this, toast, 5000).show();
    		}
    	});
    	Log.d("asdf", output);
    	boardView.locked = true;
    }
    
    private native void notifyMovePlayed(int idx);
    private native void requestGame();
}
