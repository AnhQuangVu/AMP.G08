package com.example.ampg08.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.ampg08.firebase.RoomService;

/**
 * Đồng bộ vị trí bóng lên Firestore mỗi 100ms.
 * Có cơ chế retry khi gặp lỗi (NF02: tối đa 200ms delay).
 */
public class PositionSyncManager {

    private static final String TAG              = "PositionSyncManager";
    private static final long   SYNC_INTERVAL_MS = 100L;
    private static final long   RETRY_DELAY_MS   = 200L;  // retry sau 200ms nếu lỗi
    private static final int    MAX_CONSECUTIVE_ERRORS = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running       = false;
    private          int     errorCount    = 0;

    public interface PositionProvider {
        float getX();
        float getY();
        float getMapX();
        float getMapY();
    }

    public void start(String roomId, String uid, PositionProvider provider, boolean offline) {
        if (offline) return;
        if (roomId == null || uid == null) return;

        running    = true;
        errorCount = 0;

        scheduleSync(roomId, uid, provider);
    }

    private void scheduleSync(String roomId, String uid, PositionProvider provider) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!running) return;

                try {
                    RoomService.getInstance().updatePlayerPosition(
                            roomId, uid,
                            provider.getX(), provider.getY(),
                            provider.getMapX(), provider.getMapY());
                    errorCount = 0; // reset khi thành công
                } catch (Exception e) {
                    errorCount++;
                    Log.w(TAG, "Sync lỗi lần " + errorCount + ": " + e.getMessage());

                    if (errorCount >= MAX_CONSECUTIVE_ERRORS) {
                        // Mất kết nối kéo dài — retry chậm hơn
                        Log.e(TAG, "Mất kết nối liên tục, retry sau " + RETRY_DELAY_MS + "ms");
                        handler.postDelayed(this, RETRY_DELAY_MS);
                        return;
                    }
                }

                handler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        });
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isRunning() {
        return running;
    }
}