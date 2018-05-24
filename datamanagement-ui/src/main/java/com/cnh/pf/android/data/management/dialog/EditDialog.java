/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.widget.control.InputField;
import com.cnh.pf.android.data.management.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by f09953c on 9/13/2017.
 */
public class EditDialog extends DialogView {
    public interface UserSelectCallback {
        void inputStr(String name);
    }

    private static final Logger logger = LoggerFactory.getLogger(EditDialog.class);
    public static final int DIALOG_WIDTH = 480;
    public static final int DIALOG_HEIGHT = 120;
    private String imply;
    private EditText inputField;
    private TextView warning;
    private Drawable errorIndicator;
    private UserSelectCallback callback;
    private Set<String> names;

    public EditDialog(Context context, Set<String> names) {
        super(context);
        this.showThirdButton(false).setBodyView(R.layout.edit_dialog).setBodyHeight(DIALOG_HEIGHT).setDialogWidth(DIALOG_WIDTH);
        this.setContentPaddings(24, 18, 24, 24);
        this.names = names;
    }

    public void setUserSelectCallback(UserSelectCallback callback) {
        this.callback = callback;
    }

    public void setDefaultStr(String str) {
        imply = str;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
    }

    private void init() {
        initButtons();
        initInputField();
    }

    private void initButtons() {
        setFirstButtonEnabled(false);
        setTitle(getResources().getString(R.string.edit_dialog_header));
        setFirstButtonText(getResources().getString(R.string.edit_dialog_save));
        setSecondButtonText(getResources().getString(R.string.edit_dialog_cancel));

        mButtonFirst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.inputStr(inputField.getText().toString());
                    dismiss();
                }
            }
        });
    }

    private void initInputField() {
        if (inputField == null) {
            inputField = (EditText) findViewById(R.id.edit_dialog_name_input_field);
        }
        if(warning == null){
            warning = (TextView) findViewById(R.id.edit_name_warning);
        }
        if(errorIndicator == null){
            errorIndicator = getResources().getDrawable(R.drawable.invalid_input_field);
            errorIndicator.setBounds(new Rect(0, 0, errorIndicator.getIntrinsicHeight(), errorIndicator.getIntrinsicWidth()));
        }
        inputField.setText(imply);

        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() > 0 && !editable.toString().equals(imply)) {
                    if (names.contains(editable.toString())) {
                        inputField.setCompoundDrawables(null, null, errorIndicator,null);
                        warning.setVisibility(View.VISIBLE);
                        setFirstButtonEnabled(false);
                    } else {
                        inputField.setCompoundDrawables(null, null, null, null);
                        warning.setVisibility(View.INVISIBLE);
                        setFirstButtonEnabled(true);
                    }
                } else {
                    setFirstButtonEnabled(false);
                }
            }
        });

    }
}
