package com.example.ampg08.sync;

import android.os.Handler;
import android.os.Looper;

import com.example.ampg08.firebase.RoomService;

public class PositionSyncManager {
    private static final long SYNC_INTERVAL_MS = 100L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    public interface PositionProvider {
        float getX();
        float getY();
    }

    public void start(String roomId, String uid, PositionProvider provider, boolean offline) {
        if (offline) return;
        running = true;
        handler.post(new Runnable() {
            @Override public void run() {
                if (!running) return;
                RoomService.getInstance().updatePlayerPosition(roomId, uid, provider.getX(), provider.getY());
                handler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        });
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }
}