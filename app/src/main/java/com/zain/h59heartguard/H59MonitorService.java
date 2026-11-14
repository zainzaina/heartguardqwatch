// Developed by Zain Abuzaid
// H59 Heart Guard - direct H59 BLE monitoring
package com.zain.h59heartguard;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.ServiceCompat;

import java.util.Locale;
import java.util.UUID;

public class H59MonitorService extends Service {
    public static final String ACTION_START = "com.zain.h59heartguard.START";
    public static final String ACTION_STOP = "com.zain.h59heartguard.STOP";
    public static final String ACTION_ACKNOWLEDGE = "com.zain.h59heartguard.ACKNOWLEDGE";
    public static final String ACTION_STATE = "com.zain.h59heartguard.STATE";
    public static final String EXTRA_ALERT_ID = "alert_id";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_BPM = "bpm";
    public static final String EXTRA_WARNING = "warning";
    public static final String EXTRA_WATCH = "watch";
    public static final String EXTRA_LAST_ALERT_AT = "last_alert_at";

    public enum LinkState { STOPPED, CONNECTING, CONNECTED, DISCONNECTED, SIMULATING }

    private static final String DEVICE_NAME = "H59_F603";
    private static final String DEVICE_ADDRESS = "31:30:44:36:F6:03";
    private static final UUID SERVICE_UUID = UUID.fromString("6e40fff0-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID WRITE_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID NOTIFY_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final long SCAN_TIMEOUT_MS = 10_000L;
    private static final long RECONNECT_DELAY_MS = 3_000L;
    private static final long SIMULATION_INTERVAL_MS = 2_000L;
    private static final int[] SIMULATION_OFFSETS = {
            -35, -30, -25, -20, -12, -4, 3, 6, 9, 8, 4, 2, 5, 7, 6, 4, -2, -8, -14, -20, -25, -30
    };
    private static final long[] VIBRATE_HIGH = {0, 120, 100, 120};
    private static final long[] VIBRATE_LOW = {0, 500};

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable scanTimeout = () -> {
        stopScanning();
        if (running && gatt == null) {
            setLinkState(LinkState.DISCONNECTED);
            scheduleReconnect();
        }
    };
    private final Runnable reconnect = this::startScan;
    private final Runnable nextRepeat = this::playNextRepeat;
    private final Runnable simulationTick = this::simulateReading;

    private AppSettings settings;
    private AlertHistoryDb history;
    private AlertNotifier notifier;
    private AlertVoicePlayer voice;
    private Vibrator vibrator;
    private HeartRateAlertEngine engine = new HeartRateAlertEngine();

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeChar;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private String watchName = DEVICE_NAME;
    private LinkState linkState = LinkState.STOPPED;
    private boolean running;
    private int lastBpm;
    private long lastAlertAt;
    private int repeatsRemaining;
    private boolean repeatHigh;
    private int simulationStep;

    @Override public void onCreate() {
        super.onCreate();
        settings = new AppSettings(this);
        history = new AlertHistoryDb(this);
        notifier = new AlertNotifier(this);
        voice = new AlertVoicePlayer(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        lastAlertAt = latestAlertTime();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startMonitoring();
        } else if (ACTION_STOP.equals(action)) {
            stopMonitoring();
        } else if (ACTION_ACKNOWLEDGE.equals(action)) {
            acknowledgeAlert(intent.getLongExtra(EXTRA_ALERT_ID, -1L));
        }
        return START_NOT_STICKY;
    }

    private void startMonitoring() {
        if (running) return;
        running = true;
        engine = new HeartRateAlertEngine();
        lastBpm = 0;
        ServiceCompat.startForeground(
                this,
                AlertNotifier.MONITOR_NOTIFICATION_ID,
                notifier.monitorNotification(getString(R.string.status_connecting)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        if (settings.isSimulationMode()) {
            startSimulation();
        } else {
            startScan();
        }
    }

    private void stopMonitoring() {
        running = false;
        main.removeCallbacks(reconnect);
        stopScanning();
        stopSimulation();
        cancelVoiceRepeats();
        closeGatt();
        lastBpm = 0;
        setLinkState(LinkState.STOPPED);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void acknowledgeAlert(long alertId) {
        if (alertId >= 0) history.acknowledge(alertId);
        cancelVoiceRepeats();
        notifier.cancelAlert();
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void startScan() {
        if (!running || gatt != null || scanCallback != null) return;
        if (!hasBlePermissions()) {
            setLinkState(LinkState.DISCONNECTED);
            updateNotification(getString(R.string.status_permission_missing));
            return;
        }
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            setLinkState(LinkState.DISCONNECTED);
            updateNotification(getString(R.string.status_bt_off));
            scheduleReconnect();
            return;
        }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            setLinkState(LinkState.DISCONNECTED);
            updateNotification(getString(R.string.status_bt_off));
            return;
        }
        scanCallback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (!isH59(device)) return;
                main.post(() -> {
                    if (scanCallback != null) {
                        stopScanning();
                        connect(device);
                    }
                });
            }
        };
        setLinkState(LinkState.CONNECTING);
        scanner.startScan(scanCallback);
        main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS);
    }

    private void stopScanning() {
        main.removeCallbacks(scanTimeout);
        if (scanner != null && scanCallback != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (SecurityException ignored) {
            }
        }
        scanCallback = null;
    }

    private void scheduleReconnect() {
        if (!running) return;
        main.removeCallbacks(reconnect);
        main.postDelayed(reconnect, RECONNECT_DELAY_MS);
    }

    private void connect(BluetoothDevice device) {
        if (!hasBlePermissions()) return;
        String name = deviceName(device);
        watchName = name != null ? name : DEVICE_NAME;
        updateNotification(getString(R.string.status_connecting_to, watchName));
        gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private static boolean isH59(BluetoothDevice device) {
        if (DEVICE_ADDRESS.equalsIgnoreCase(device.getAddress())) return true;
        String name = deviceName(device);
        return name != null && name.toUpperCase(Locale.US).startsWith("H59");
    }

    private static String deviceName(BluetoothDevice device) {
        try {
            return device.getName();
        } catch (SecurityException e) {
            return null;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                main.post(() -> onLinked(g));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                main.post(H59MonitorService.this::onLinkLost);
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                main.post(() -> setupNotifications(g));
            }
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            onPacket(c.getValue());
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value) {
            onPacket(value);
        }
    };

    private void onLinked(BluetoothGatt g) {
        setLinkState(LinkState.CONNECTED);
        updateNotification(getString(R.string.status_connected));
        try {
            g.discoverServices();
        } catch (SecurityException ignored) {
        }
    }

    private void onLinkLost() {
        writeChar = null;
        closeGatt();
        lastBpm = 0;
        if (!running) return;
        setLinkState(LinkState.DISCONNECTED);
        updateNotification(getString(R.string.status_reconnecting));
        scheduleReconnect();
    }

    private void setupNotifications(BluetoothGatt g) {
        BluetoothGattService service = g.getService(SERVICE_UUID);
        if (service == null) {
            updateNotification(getString(R.string.status_service_missing));
            return;
        }
        writeChar = service.getCharacteristic(WRITE_UUID);
        BluetoothGattCharacteristic notify = service.getCharacteristic(NOTIFY_UUID);
        if (writeChar == null || notify == null) {
            updateNotification(getString(R.string.status_characteristics_missing));
            return;
        }
        try {
            g.setCharacteristicNotification(notify, true);
            BluetoothGattDescriptor d = notify.getDescriptor(CCC_UUID);
            if (d != null) {
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(d);
            }
        } catch (SecurityException ignored) {
        }
        // Start on-demand HR measurement, then realtime if needed.
        main.postDelayed(() -> sendCommand((byte) 0x69, (byte) 0x01), 600);
        main.postDelayed(() -> sendCommand((byte) 0x1E, (byte) 0x01), 13000);
    }

    private void sendCommand(byte opcode, byte payload) {
        if (gatt == null || writeChar == null) return;
        byte[] p = new byte[16];
        p[0] = opcode;
        p[1] = payload;
        int sum = 0;
        for (int i = 0; i < 15; i++) sum = (sum + (p[i] & 0xFF)) & 0xFF;
        p[15] = (byte) sum;
        try {
            writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            writeChar.setValue(p);
            gatt.writeCharacteristic(writeChar);
        } catch (SecurityException ignored) {
        }
    }

    private void onPacket(byte[] value) {
        if (value == null) return;
        byte[] copy = value.clone();
        main.post(() -> parsePacket(copy));
    }

    private void parsePacket(byte[] d) {
        if (d == null || d.length != 16) return;
        int sum = 0;
        for (int i = 0; i < 15; i++) sum = (sum + (d[i] & 0xFF)) & 0xFF;
        if ((d[15] & 0xFF) != sum) return;
        int op = d[0] & 0xFF;
        if (op == 0x69 && d.length >= 4) {
            int code = d[2] & 0xFF;
            int bpm = d[3] & 0xFF;
            if (code == 0 && bpm >= 30 && bpm <= 220) handleHeartRate(bpm);
        } else if (op == 0x1E && d.length >= 3) {
            int bpm = (d[1] & 0xFF) == 0 ? (d[2] & 0xFF) : (d[1] & 0xFF);
            if (bpm >= 30 && bpm <= 220) handleHeartRate(bpm);
        }
    }

    private void handleHeartRate(int bpm) {
        HeartRateAlertEngine.Config config = settings.alertConfig();
        long now = System.currentTimeMillis();
        lastBpm = bpm;
        HeartRateAlertEngine.Event event = engine.onReading(bpm, now, config);
        if (event != HeartRateAlertEngine.Event.NONE) {
            raiseAlert(event, bpm, config, now);
        }
        publishState();
        updateNotification(getString(R.string.status_live_bpm, bpm));
    }

    private void raiseAlert(HeartRateAlertEngine.Event event, int bpm, HeartRateAlertEngine.Config config, long now) {
        boolean high = event == HeartRateAlertEngine.Event.HIGH;
        int threshold = high ? config.highBpm : config.lowBpm;
        long alertId = history.insert(now, bpm, threshold, high ? AlertEvent.TYPE_HIGH : AlertEvent.TYPE_LOW);
        lastAlertAt = now;
        notifier.showAlert(alertId, high, bpm, settings.isMedicationWording());
        if (settings.isVibrationEnabled()) vibrate(high);
        if (settings.isSoundEnabled()) startVoiceRepeats(high);
    }

    private void vibrate(boolean high) {
        if (vibrator == null) return;
        vibrator.vibrate(VibrationEffect.createWaveform(high ? VIBRATE_HIGH : VIBRATE_LOW, -1));
    }

    private void startVoiceRepeats(boolean high) {
        cancelVoiceRepeats();
        repeatHigh = high;
        repeatsRemaining = settings.getRepeatCount();
        playNextRepeat();
    }

    private void playNextRepeat() {
        if (repeatsRemaining <= 0) return;
        repeatsRemaining--;
        boolean useCustomAudio = repeatHigh && settings.isMedicationWording();
        String customPath = useCustomAudio ? settings.getCustomAudioPath() : null;
        String message = AlertVoicePlayer.message(this, repeatHigh, settings.isMedicationWording());
        voice.play(customPath, message, () -> {
            if (repeatsRemaining > 0) {
                main.postDelayed(nextRepeat, settings.getRepeatDelaySec() * 1000L);
            }
        });
    }

    private void cancelVoiceRepeats() {
        repeatsRemaining = 0;
        main.removeCallbacks(nextRepeat);
        voice.stop();
    }

    private void startSimulation() {
        simulationStep = 0;
        setLinkState(LinkState.SIMULATING);
        updateNotification(getString(R.string.status_simulating));
        main.post(simulationTick);
    }

    private void stopSimulation() {
        main.removeCallbacks(simulationTick);
    }

    private void simulateReading() {
        if (!running) return;
        int offset = SIMULATION_OFFSETS[simulationStep];
        simulationStep = (simulationStep + 1) % SIMULATION_OFFSETS.length;
        handleHeartRate(Math.max(40, settings.getHighBpm() + offset));
        main.postDelayed(simulationTick, SIMULATION_INTERVAL_MS);
    }

    private void setLinkState(LinkState state) {
        linkState = state;
        publishState();
    }

    private void publishState() {
        HeartRateAlertEngine.Config config = settings.alertConfig();
        boolean warning = lastBpm > 0 && HeartRateAlertEngine.isOutOfRange(lastBpm, config);
        Intent state = new Intent(ACTION_STATE)
                .setPackage(getPackageName())
                .putExtra(EXTRA_STATE, linkState.name())
                .putExtra(EXTRA_BPM, lastBpm)
                .putExtra(EXTRA_WARNING, warning)
                .putExtra(EXTRA_WATCH, watchName)
                .putExtra(EXTRA_LAST_ALERT_AT, lastAlertAt);
        sendBroadcast(state);
    }

    private void updateNotification(String text) {
        if (running) notifier.showMonitorStatus(text);
    }

    private long latestAlertTime() {
        java.util.List<AlertEvent> latest = history.recent(1);
        return latest.isEmpty() ? 0L : latest.get(0).getTimestampMs();
    }

    private void closeGatt() {
        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (SecurityException ignored) {
            }
            try {
                gatt.close();
            } catch (SecurityException ignored) {
            }
            gatt = null;
        }
    }

    @Override public void onDestroy() {
        running = false;
        main.removeCallbacksAndMessages(null);
        stopScanning();
        closeGatt();
        voice.release();
        history.close();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
