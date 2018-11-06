package com.cnh.pf.android.data.management.misc;


import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.Toast;
import com.cnh.android.pf.widget.controls.ToastMessageCustom;
import com.cnh.pf.android.data.management.R;

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
            ToastMessageCustom.makeToastMessageText(getContext(),getResources().getString(R.string.delete_select_notice), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
        }
        return super.onTouchEvent(event);
    }
}