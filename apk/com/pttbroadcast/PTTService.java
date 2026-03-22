package com.pttbroadcast;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;

public class PTTService extends Service {
    private static final String TAG = "PTTBroadcast";
    private static final String INPUT_DEVICE = "/dev/input/event1";  // mtk-kpd设备
    private static final int BTN_9_SC = 0x109;  // BTN_9的scancode
    private static final int EV_KEY = 0x01;

    private Thread inputThread;
    private volatile boolean running = true;
    private volatile boolean pttPressed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PTT Service created");
        startInputListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PTT Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (inputThread != null) {
            inputThread.interrupt();
        }
        Log.d(TAG, "PTT Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startInputListener() {
        inputThread = new Thread(() -> {
            Log.d(TAG, "Starting input listener on " + INPUT_DEVICE);

            File inputFile = new File(INPUT_DEVICE);
            if (!inputFile.exists()) {
                Log.e(TAG, "Input device not found: " + INPUT_DEVICE);
                // 尝试其他设备
                tryOtherDevices();
                return;
            }

            // 设置读取权限
            try {
                Runtime.getRuntime().exec("chmod 666 " + INPUT_DEVICE).waitFor();
            } catch (Exception e) {
                Log.e(TAG, "Failed to chmod: " + e.getMessage());
            }

            try (RandomAccessFile reader = new RandomAccessFile(inputFile, "r")) {
                reader.seek(0);
                byte[] event = new byte[24];

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesRead = reader.read(event);
                        if (bytesRead == 24) {
                            processEvent(event);
                        }
                    } catch (Exception e) {
                        if (running) {
                            Log.e(TAG, "Error reading input: " + e.getMessage());
                            SystemClock.sleep(100);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to open input device: " + e.getMessage());
            }

            Log.d(TAG, "Input listener thread exiting");
        });

        inputThread.setPriority(Thread.MAX_PRIORITY);
        inputThread.start();
    }

    private void tryOtherDevices() {
        // 尝试event0-event9
        for (int i = 0; i < 10; i++) {
            String device = "/dev/input/event" + i;
            File f = new File(device);
            if (f.exists()) {
                Log.d(TAG, "Trying device: " + device);
                // 继续监听此设备
            }
        }
    }

    private void processEvent(byte[] event) {
        // input_event结构（小端序）
        // offset 16: type (2 bytes)
        // offset 18: code (2 bytes)
        // offset 20: value (4 bytes)
        int type = (event[16] & 0xFF) | ((event[17] & 0xFF) << 8);
        int code = (event[18] & 0xFF) | ((event[19] & 0xFF) << 8);
        int value = (event[20] & 0xFF) | ((event[21] & 0xFF) << 8)
                   | ((event[22] & 0xFF) << 16) | ((event[23] & 0xFF) << 24);

        if (type == EV_KEY && code == BTN_9_SC) {
            if (value == 1 && !pttPressed) {
                pttPressed = true;
                Log.d(TAG, "PTT DOWN");
                sendPTTDown();
            } else if (value == 0 && pttPressed) {
                pttPressed = false;
                Log.d(TAG, "PTT UP");
                sendPTTUp();
            }
        }
    }

    private void sendPTTDown() {
        try {
            Intent intent = new Intent("android.intent.action.PTT.down");
            intent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            sendBroadcast(intent);
            Log.d(TAG, "PTT.down broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send PTT.down: " + e.getMessage());
        }
    }

    private void sendPTTUp() {
        try {
            Intent intent = new Intent("android.intent.action.PTT.up");
            intent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            sendBroadcast(intent);
            Log.d(TAG, "PTT.up broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send PTT.up: " + e.getMessage());
        }
    }
}
