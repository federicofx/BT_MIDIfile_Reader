package fretx.fretxapp;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by AsmodeusStudio on 07/12/2015.
 */
public class ObservableVideoView extends VideoView{

    private IVideoViewActionListener mVideoViewListener;
    private boolean mIsOnPauseMode = false;

    public interface IVideoViewActionListener
    {
        void onPause();
        void onResume();
        void onTimeBarSeekChanged(int currentTime);
    }

    public void setVideoViewListener(IVideoViewActionListener listener)
    {
        mVideoViewListener = listener;
    }

    @Override
    public void pause()
    {
        super.pause();

        if (mVideoViewListener != null)
        {
            mVideoViewListener.onPause();
        }

        mIsOnPauseMode = true;
    }

    @Override
    public void start()
    {
        super.start();

        if (mIsOnPauseMode)
        {
            if (mVideoViewListener != null)
            {
                mVideoViewListener.onResume();
            }

            mIsOnPauseMode = false;
        }
    }

    @Override
    public void seekTo(int msec)
    {
        super.seekTo(msec);

        if (mVideoViewListener != null)
        {
            mVideoViewListener.onTimeBarSeekChanged(msec);
        }
    }

    public ObservableVideoView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ObservableVideoView(Context context)
    {
        super(context);
    }

    public ObservableVideoView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }
}
