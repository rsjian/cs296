package com.rsjian2.cs296;

import android.app.ListActivity;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.content.Intent;
import com.rsjian2.cs296.R;

public class Main extends ListActivity {
    TextView content;
    String[] values = new String[] {"Host Game", "Join Game"};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, values);

        // Assign adapter to List
        setListAdapter(adapter);
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) { 
		super.onListItemClick(l, v, position, id);
		// ListView Clicked item index
		if(position == 0) {
			Intent intent = new Intent(this, ServerActivity.class);
			startActivity(intent);
		} else if(position == 1) {
			Intent intent = new Intent(this, ClientActivity.class);
			startActivity(intent);
		}
	}
    
    static {
        System.loadLibrary("cs296");
    }
}
