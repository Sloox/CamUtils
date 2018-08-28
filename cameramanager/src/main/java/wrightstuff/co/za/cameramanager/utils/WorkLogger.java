package wrightstuff.co.za.cameramanager.utils;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

/**
 * A utility class to help log timings splits throughout a method call.
 * A clone of TimingLogger but with custom disabled flag settable
 * <p>
 * <pre>
 *     TimingLogger timings = new TimingLogger(TAG, "methodA");
 *     // ... do some work A ...
 *     timings.addSplit("work A");
 *     // ... do some work B ...
 *     timings.addSplit("work B");
 *     // ... do some work C ...
 *     timings.addSplit("work C");
 *     timings.dumpToLog();
 * </pre>
 * <p>
 * <p>The dumpToLog call would add the following to the log:</p>
 * <p>
 * <pre>
 *     D/TAG     ( 3459): methodA: begin
 *     D/TAG     ( 3459): methodA:      9 ms, work A
 *     D/TAG     ( 3459): methodA:      1 ms, work B
 *     D/TAG     ( 3459): methodA:      6 ms, work C
 *     D/TAG     ( 3459): methodA: end, 16 ms
 * </pre>
 */
public class WorkLogger {

    /**
     * Stores the time of each split.
     */
    ArrayList<Long> mSplits;
    /**
     * Stores the labels for each split.
     */
    ArrayList<String> mSplitLabels;
    /**
     * The Log tag to use for checking Log.isLoggable and for
     * logging the timings.
     */
    private String mTag;
    /**
     * A label to be included in every log.
     */
    private String mLabel;
    /**
     * Used to track whether Log.isLoggable was enabled at reset time.
     */
    private boolean mDisabled;

    public WorkLogger(String tag, String label, boolean disabled) {
        reset(tag, label, disabled);
    }


    public void reset(String tag, String label, boolean disabled) {
        mTag = tag;
        mLabel = label;
        mDisabled = disabled;
        reset();
    }


    public void reset() {
        if (mDisabled) return;
        if (mSplits == null) {
            mSplits = new ArrayList<Long>();
            mSplitLabels = new ArrayList<String>();
        } else {
            mSplits.clear();
            mSplitLabels.clear();
        }
        addSplit(null);
    }


    public void addSplit(String splitLabel) {
        if (mDisabled) return;
        long now = SystemClock.elapsedRealtime();
        mSplits.add(now);
        mSplitLabels.add(splitLabel);
    }


    public void dumpToLog() {
        if (mDisabled) return;
        Log.d(mTag, mLabel + ": begin");
        final long first = mSplits.get(0);
        long now = first;
        for (int i = 1; i < mSplits.size(); i++) {
            now = mSplits.get(i);
            final String splitLabel = mSplitLabels.get(i);
            final long prev = mSplits.get(i - 1);

            Log.d(mTag, mLabel + ":      " + (now - prev) + " ms, " + splitLabel);
        }
        Log.d(mTag, mLabel + ": end, " + (now - first) + " ms");
    }
}
