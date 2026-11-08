package __PKG__;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

public class MediaService extends Service {
    static final String CH = "mushaf_media";
    static final int ID = 4711;
    private MediaSession session;
    private String title = "";
    private String text = "";
    private boolean playing = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "التشغيل الصوتي", NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
        session = new MediaSession(this, "MushafMedia");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { MediaCtlPlugin.emit("play"); }
            @Override public void onPause() { MediaCtlPlugin.emit("pause"); }
            @Override public void onSkipToNext() { MediaCtlPlugin.emit("next"); }
            @Override public void onSkipToPrevious() { MediaCtlPlugin.emit("prev"); }
            @Override public void onStop() { MediaCtlPlugin.emit("stop"); }
        });
        session.setActive(true);
    }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        String a = in == null ? null : in.getAction();
        if (a != null && a.startsWith("act:")) {
            String act = a.substring(4);
            MediaCtlPlugin.emit(act);
            if ("stop".equals(act)) {
                stopForeground(true);
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        if (in != null) {
            String t = in.getStringExtra("title");
            String x = in.getStringExtra("text");
            title = t == null ? "" : t;
            text = x == null ? "" : x;
            playing = in.getBooleanExtra("playing", false);
        }
        show();
        return START_NOT_STICKY;
    }

    private Notification.Action act(int icon, String label, String a, int code) {
        Intent i = new Intent(this, MediaService.class).setAction("act:" + a);
        PendingIntent pi = PendingIntent.getService(this, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(icon, label, pi).build();
    }

    private void show() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build());
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, text)
                .build());

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_mushaf)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(playing)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch != null) {
            b.setContentIntent(PendingIntent.getActivity(this, 0, launch,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        b.addAction(act(android.R.drawable.ic_media_previous, "السابق", "prev", 1));
        if (playing) {
            b.addAction(act(android.R.drawable.ic_media_pause, "إيقاف مؤقت", "pause", 2));
        } else {
            b.addAction(act(android.R.drawable.ic_media_play, "تشغيل", "play", 2));
        }
        b.addAction(act(android.R.drawable.ic_media_next, "التالي", "next", 3));
        b.addAction(act(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", "stop", 4));
        b.setStyle(new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));
        Notification n = b.build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(ID, n);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.release();
        }
        super.onDestroy();
    }
}
