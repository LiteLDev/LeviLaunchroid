package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;

import java.util.regex.Pattern;

public class VersionRenameDialog extends Dialog {
    
    public interface Callback {
        void onRenameClicked(String newName);
        void onCancelled();
    }
    
    private GameVersion version;
    private Callback callback;
    private EditText editVersionName;
    private TextView textVersionError;
    private Button buttonRename;
    private Button buttonCancel;
    
    public VersionRenameDialog(@NonNull Context context) {
        super(context);
    }
    
    public VersionRenameDialog setVersion(GameVersion version) {
        this.version = version;
        return this;
    }
    
    public VersionRenameDialog setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_version_rename);
        
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        initViews();
        setupListeners();
        
        if (version != null) {
            String currentName = extractDisplayNameFromVersion(version);
            editVersionName.setText(currentName);
            editVersionName.setSelection(currentName.length());
        }
    }
    
    private void initViews() {
        editVersionName = findViewById(R.id.edit_version_name);
        textVersionError = findViewById(R.id.text_version_error);
        buttonRename = findViewById(R.id.button_rename);
        buttonCancel = findViewById(R.id.button_cancel);
    }
    
    private void setupListeners() {
        editVersionName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateVersionName(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        buttonRename.setOnClickListener(v -> {
            String newName = editVersionName.getText().toString().trim();
            if (isValidVersionName(newName) && callback != null) {
                callback.onRenameClicked(newName);
                dismiss();
            }
        });
        
        buttonCancel.setOnClickListener(v -> {
            if (callback != null) {
                callback.onCancelled();
            }
            dismiss();
        });
    }
    
    private void validateVersionName(String name) {
        if (isValidVersionName(name)) {
            textVersionError.setVisibility(View.GONE);
            buttonRename.setEnabled(true);
        } else {
            textVersionError.setVisibility(View.VISIBLE);
            buttonRename.setEnabled(false);
        }
    }
    
    private boolean isValidVersionName(String name) {
        if (name == null || name.isEmpty() || name.length() > 40) return false;
        String regex = "^[a-zA-Z0-9._-]+$";
        return Pattern.compile(regex).matcher(name).matches();
    }

    private String extractDisplayNameFromVersion(GameVersion version) {
        if (version == null) return "";

        String displayName = version.displayName;
        if (displayName == null) return version.directoryName;

        int lastParenIndex = displayName.lastIndexOf(" (");
        if (lastParenIndex > 0) {
            return displayName.substring(0, lastParenIndex);
        }

        return version.directoryName;
    }
}
