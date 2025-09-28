package com.microsoft.xbox.xle.app.adapter;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.microsoft.xboxtcui.R;
import com.microsoft.xbox.telemetry.helpers.UTCReportUser;
import com.microsoft.xbox.telemetry.helpers.UTCTelemetry;
import com.microsoft.xbox.toolkit.XLEAssert;
import com.microsoft.xbox.toolkit.ui.CustomTypefaceTextView;
import com.microsoft.xbox.toolkit.ui.XLEButton;
import com.microsoft.xbox.xle.viewmodel.AdapterBase;
import com.microsoft.xbox.xle.viewmodel.ReportUserScreenViewModel;
import com.microsoft.xboxtcui.XboxTcuiSdk;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class ReportUserScreenAdapter extends AdapterBase {
    private final XLEButton cancelButton = ((XLEButton) findViewById(R.id.report_user_cancel));
    private final Spinner reasonSpinner = ((Spinner) findViewById(R.id.report_user_reason));
    private final XLEButton submitButton = ((XLEButton) findViewById(R.id.report_user_submit));
    private final CustomTypefaceTextView titleTextView = ((CustomTypefaceTextView) findViewById(R.id.report_user_title));
    public EditText optionalText = ((EditText) findViewById(R.id.report_user_text));
    public ReportUserScreenViewModel viewModel;
    private ArrayAdapter<String> reasonSpinnerAdapter;

    public ReportUserScreenAdapter(ReportUserScreenViewModel reportUserScreenViewModel) {
        super(reportUserScreenViewModel);
        this.viewModel = reportUserScreenViewModel;
        XLEAssert.assertNotNull(this.titleTextView);
        XLEAssert.assertNotNull(this.reasonSpinner);
        XLEAssert.assertNotNull(this.optionalText);
        XLEAssert.assertNotNull(this.cancelButton);
        XLEAssert.assertNotNull(this.submitButton);
    }

    public void onStart() {
        super.onStart();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(XboxTcuiSdk.getActivity(), R.layout.report_spinner_item, this.viewModel.getReasonTitles());
        this.reasonSpinnerAdapter = arrayAdapter;
        arrayAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        this.reasonSpinner.setAdapter(this.reasonSpinnerAdapter);
        if (Build.VERSION.SDK_INT >= 16) {
            this.reasonSpinner.setPopupBackgroundDrawable(new ColorDrawable(this.viewModel.getPreferredColor()));
        }
        this.reasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                ReportUserScreenAdapter.this.viewModel.setReason(i);
            }
        });
        this.cancelButton.setOnClickListener(view -> ReportUserScreenAdapter.this.viewModel.onBackButtonPressed());
        this.submitButton.setOnClickListener(view -> {
            UTCReportUser.trackReportDialogOK(ReportUserScreenAdapter.this.viewModel.getReason() == null ? UTCTelemetry.UNKNOWNPAGE : ReportUserScreenAdapter.this.viewModel.getReason().toString());
            ReportUserScreenAdapter.this.viewModel.submitReport(ReportUserScreenAdapter.this.optionalText.getText().toString());
        });
    }

    public void updateViewOverride() {
        CustomTypefaceTextView customTypefaceTextView = this.titleTextView;
        if (customTypefaceTextView != null) {
            customTypefaceTextView.setText(this.viewModel.getTitle());
        }
        XLEButton xLEButton = this.submitButton;
        if (xLEButton != null) {
            xLEButton.setEnabled(this.viewModel.validReasonSelected());
        }
    }
}
