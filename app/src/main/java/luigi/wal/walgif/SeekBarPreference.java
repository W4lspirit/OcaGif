package luigi.wal.walgif;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private int mProgress;
    private TextView mTextView;

    public SeekBarPreference(Context context) {
        this(context, null, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_seekbar);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;
        mProgress = progress;
        mTextView.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setValue(mProgress);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }
        notifyChanged();

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSeekBar = view.findViewById(R.id.seekbar);
        mTextView = view.findViewById(R.id.summary);
        mSeekBar.setProgress(mProgress);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }
}