package com.rsjian2.cs296;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;  
import android.app.Activity;  
import android.view.Menu;  
import android.content.Context;  
import android.graphics.Canvas;  
import android.graphics.Color;  
import android.graphics.Paint;  
import android.view.View;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.util.Log;

public class BoardView extends View {
	private Paint paint;
	private int width;
	private int height;
	private Board board;
	//private GestureDetector gestureDetector;
	
	private int ccx, ccy;
	private boolean hasCC = false;
	public boolean locked;
	private float radius;
	
	Callback onMovePlayed;
	
	public BoardView(Context context, Board board, Callback onMovePlayed, boolean locked) {
		super(context);
		this.board = board;
		this.onMovePlayed = onMovePlayed;
		this.locked = locked;
        setWillNotDraw(false);
		paint = new Paint();
		//gestureDetector = new GestureDetector(getContext(), new BoardGestureDetector());
	}
	
	public void setBoard(Board board) { 
		Log.d("asdf", "setBoard");
		this.board = board;
		postInvalidate();
	}
	
	 @Override
	 protected void onDraw(Canvas canvas) {
		 //Log.d("asdf3", "in onDraw()");
         super.onDraw(canvas);
         
         // custom drawing code here 
         paint.setStyle(Paint.Style.FILL);  

         // make the entire canvas white  
         paint.setColor(Color.WHITE);  
         canvas.drawPaint(paint);  

         // draw green circle with anti aliasing turned on  
         paint.setAntiAlias(true);
        
         
         //draw grid
         paint.setColor(Color.BLACK);
         for(int i = 0; i < board.getBoardSize(); ++i) {
        	 canvas.drawLine(radius+i*2*radius, radius, radius+i*2*radius, width - radius, paint);
        	 canvas.drawLine(radius, radius+i*2*radius, height - radius, radius+i*2*radius, paint);
         }
         
         //draw star points
         if(board.getBoardSize() == 19) {
             paint.setColor(Color.BLACK);
             canvas.drawCircle(radius + 3*2*radius, radius + 3*2*radius, radius/4, paint);
             canvas.drawCircle(width - (radius + 3*2*radius), radius + 3*2*radius, radius/4, paint);
             canvas.drawCircle(radius + 3*2*radius, width - (radius + 3*2*radius), radius/4, paint);
             canvas.drawCircle(width - (radius + 3*2*radius), width - (radius + 3*2*radius), radius/4, paint);
         }
         
         //draw stones
         float cx, cy;
         for(int i = 0; i < board.getBoardSize(); ++i) {
        	 for(int j = 0; j < board.getBoardSize(); ++j) {
        		 cx = radius + i*2*radius;
        		 cy = radius + j*2*radius;
    			 paint.setStyle(Paint.Style.FILL);
        		 if(board.getColor(i, j) == Game.BLACK) {
        			 paint.setColor(Color.BLACK);
        			 canvas.drawCircle(cx, cy, radius, paint);
        		 } else if(board.getColor(i, j) == Game.WHITE) {
        			 paint.setColor(Color.WHITE);
        			 canvas.drawCircle(cx, cy, radius, paint);
        			 
        			 paint.setColor(Color.BLACK);
        			 paint.setStyle(Paint.Style.STROKE);
        			 canvas.drawCircle(cx, cy, radius, paint);
        		 }
        	 }
         }
         
         //draw last played stone
         
         //draw cross cursor
         
         if(hasCC && !locked) {
        	cx = radius + ccx*2*radius;
        	cy = radius + ccy*2*radius;
        	paint.setColor(Color.GREEN);
        	paint.setStyle(Paint.Style.FILL);
        	paint.setStrokeWidth(2);
        	canvas.drawLine(cx, 0, cx, width, paint);
        	canvas.drawLine(0, cy, width, cy, paint);
        	 
        	canvas.drawCircle(cx, cy, radius, paint);
        	paint.setStrokeWidth(0);
         }
     }
	 
	 
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x, y;
		x = e.getX();
		y = e.getY();
		ccx = (int)((x - radius) / (2*radius) + 0.5);
		ccy = (int)((y - radius) / (2*radius) + 0.5);
		switch (e.getAction()) {
			case MotionEvent.ACTION_MOVE:
				//Log.d("asdf2", "ACTION MOVE");
				//Log.d("asdf2", "" + x + " " + y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				//Log.d("asdf2", "ACTION_UP");
				if(hasCC && !locked) {
					if(board.getColor(ccx, ccy) == Game.EMPTY) {
						//notify game engine
						onMovePlayed.run(ccy*board.getBoardSize() + ccx);
					}
				}
				hasCC = false;
				invalidate();
				break;
			case MotionEvent.ACTION_DOWN:
				//Log.d("asdf2", "Action_DOWN");
				hasCC = true;

				invalidate();
				break;
		}
		return true;
	}
	 
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    this.width = w;
	    this.height = h;
	    super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    int size = 0;
	    int width = getMeasuredWidth();
	    int height = getMeasuredHeight();

	    size = Math.min(width, height);
	    setMeasuredDimension(size, size);
        radius = ((float)size)/((float)board.getBoardSize())/2;
	}
}
