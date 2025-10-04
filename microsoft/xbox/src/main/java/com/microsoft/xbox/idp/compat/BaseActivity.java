package com.microsoft.xbox.idp.compat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public abstract class BaseActivity extends AppCompatActivity {
    public boolean hasFragment(int i) {
        return getSupportFragmentManager().findFragmentById(i) != null; // Изменено на getSupportFragmentManager()
    }

    public void addFragment(int i, BaseFragment baseFragment) {
        getSupportFragmentManager().beginTransaction().add(i, baseFragment).commit();
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setOrientation();
    }

    @SuppressLint("WrongConstant")
    public void setOrientation() {
        if ((getApplicationContext().getResources().getConfiguration().screenLayout & 15) < 3) {
            setRequestedOrientation(1);
        }
    }
}
