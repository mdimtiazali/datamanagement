package com.cnh.pf.android.data.management.misc;


import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.Toast;

public class DeleteButton extends ImageButton {
    private PopContentProvider popContentProvider;

    public interface PopContentProvider{
        int getStringResourceId();
    }

    public DeleteButton(Context context) {
        super(context);
    }

    public DeleteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeleteButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPopContentProvider(PopContentProvider popContentProvider) {
        this.popContentProvider = popContentProvider;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled() && MotionEvent.ACTION_UP == event.getAction() && popContentProvider != null){
            Toast toast = Toast.makeText(getContext(),popContentProvider.getStringResourceId(),Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP,0,48);
            toast.show();
        }
        return super.onTouchEvent(event);
    }
}