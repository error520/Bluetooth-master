package android.myapplication;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.myapplication.MainActivity.DEVICE_NAME;
import static android.myapplication.MainActivity.REQUEST_ENABLE_BT;

public class BLEService extends Service {
    private final String TAG = "BLEService";
    private BluetoothAdapter mBtAdapter;
    private Handler mHandler;
    private ArrayList<String> device_list = new ArrayList<String>();
    private LocalBroadcastManager localBroadcastManager;
    public final static String ACTION_GATT_CONNECTED = "com.kinco.BLEService.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.kinco.BLEService.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.kinco.BLEService.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.kinco.BLEService.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GET_DEVICE_NAME = "com.kinco.BLEService.ACTION_GET_DEVICE_NAME";
    public final static String ACTION_SEARCH_COMPLETED = "com.kinco.BLEService.ACTION_SEARCH_COMPLETED";
    public final static String EXTRA_DATA = "com.kinco.BLEService.EXTRA_DATA";
    public final static String DEVICE_DATA = "com.kinco.BLEService.DEVICE_DATA";
    private boolean mScanning;
    private final  IBinder mBinder = new localBinder();
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattCharacteristic> gattCharacteristics;
    private List<UUID> readUuid = new ArrayList<UUID>();
    private List<UUID> writeUuid = new ArrayList<UUID>();
    private List<UUID> writeServiceUuid = new ArrayList<UUID>();
    private List<UUID> notifyUuid = new ArrayList<UUID>();
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattCharacteristic writeCharacteristic;

    public boolean init(){
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        //检查手机是否支持BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d("BLEService","不支持BLE!");
            return false;
        }
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
              mBtAdapter.enable();//强制打开蓝牙
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        return true;
    }
    private void broadcastUpdate(final String action){
        Intent intent = new Intent(action);
        localBroadcastManager.sendBroadcast(intent);
    }
    /**
     * 返回搜索到的设备信息
     */
    private void broadcastUpdate(final String action, final String device){
        Intent intent = new Intent(action);
        intent.putExtra(DEVICE_DATA,device);
        localBroadcastManager.sendBroadcast(intent);
    }
    public void sendBroadcast(){
        Intent intent = new Intent("send a local broadcast");
        localBroadcastManager.sendBroadcast(intent);
    }

    public void scanLeDevice(final boolean enable) {

        if (enable) {//true
            device_list.clear();
            //10秒后停止搜索
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    Log.d(TAG,"扫描完成");
                    broadcastUpdate(ACTION_SEARCH_COMPLETED);

                }
            }, 5000);
            mScanning = true;
            mBtAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {//false
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }
    public void stopLeScan(){
        if (mScanning == true)
            mBtAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!device_list.contains(device.getName()+"\n" +
                    device.getAddress())) {
                device_list.add(device.getName()+"\n" +
                        device.getAddress());
                broadcastUpdate(ACTION_GET_DEVICE_NAME,device.getName()+"\n" +
                        device.getAddress());
                Log.d(TAG,device.getName()+"\n" +
                        device.getAddress());
            }

        }
    };

    public boolean connect(final String address) {//4
//        Log.d(TAG, "连接" + mBluetoothDeviceAddress);
        if (mBtAdapter == null || address == null) {
            Log.d(TAG,"BluetoothAdapter不能初始化 or 未知 address.");
            Toast.makeText(this,"未能连接",Toast.LENGTH_LONG).show();
            return false;
        }

        final BluetoothDevice device = mBtAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Toast.makeText(this,"设备没找到，不能连接",Toast.LENGTH_SHORT).show();
            Log.d("data", "设备没找到，不能连接");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, true, mBluetoothGattCallback);//真正的连接
        //这个方法需要三个参数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接到它），和BluetoothGattCallback调用。
        Log.d(TAG, "尝试新的连接.");
        return true;
    }
    BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        //当连接状态发生改变(有信息来)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG,"onConnectionStateChange()");
            if (status==BluetoothGatt.GATT_SUCCESS){
                //连接成功
                if (newState== BluetoothGatt.STATE_CONNECTED){
                    Log.e(TAG,"连接成功");
                    //发现服务
                    gatt.discoverServices();
                }
            }else{
                //连接失败
                Log.e(TAG,"失败=="+status);
                mBluetoothGatt.close();

            }
        }

        //发现新服务，即调用了mBluetoothGatt.discoverServices()后，返回的数据
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //得到所有Service
                List<BluetoothGattService> supportedGattServices = gatt.getServices();

                for (BluetoothGattService gattService : supportedGattServices) {
                    //得到每个Service的Characteristics
                    gattCharacteristics = gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        int charaProp = gattCharacteristic.getProperties();
                        //所有Characteristics按属性分类
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid());
                            Log.d(TAG, "gattCharacteristic的属性为:  可读");
                            readUuid.add(gattCharacteristic.getUuid());
                            readCharacteristic = gattCharacteristic;
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid());
                            Log.d(TAG, "gattCharacteristic的属性为:  可写");
                            writeServiceUuid.add(gattService.getUuid());
                            writeUuid.add(gattCharacteristic.getUuid());
                            writeCharacteristic = gattCharacteristic;
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid() + gattCharacteristic);
                            Log.d(TAG, "gattCharacteristic的属性为:  具备通知属性");
                            notifyUuid.add(gattCharacteristic.getUuid());
                            notify_UUID_service = gattService.getUuid();
                            notify_UUID_chara = gattCharacteristic.getUuid();
                        }
                    }
                }
            }

            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt
                    .getService(notify_UUID_service).getCharacteristic(notify_UUID_chara),true);

        }

        //调用mBluetoothGatt.readCharacteristic(characteristic)读取数据回调，在这里面接收数据
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
//            byte bb[] = characteristic.getValue();
//            Log.d(TAG, toHexString(bb));
        }

        //发送数据后的回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        //数据产生变化
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte bb2[] = characteristic.getValue();


            Log.d(TAG, control_Activity.toHexString(bb2));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor读
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor写
            super.onDescriptorWrite(gatt, descriptor, status);
        }



        //调用mBluetoothGatt.readRemoteRssi()时的回调，rssi即信号强度
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {//读Rssi
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    class localBinder extends Binder {
        public BLEService getService(){
            return BLEService.this;
        };
    }
    public void hello(){
        broadcastUpdate(ACTION_GET_DEVICE_NAME,"nihao");
        Log.d(TAG,"hello");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        init();

        Log.d(TAG,"创建了服务");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    //读取数据
    private void readData(String data){
            int numA = Integer.parseInt(data, 16);
            int numB = Integer.parseInt(data, 16);
            byte dataA[] = util.intToByte2(numA);
            byte dataB[] = util.intToByte2(numB);
            byte CRC[] = new byte[2];
            byte[] send = {0X05, 0X03, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};
            send[2] = dataA[0];
            send[3] = dataA[1];
            send[4] = dataB[0];
            send[5] = dataB[1];
            CRC = util.CRC16_Check(send, 6);
            send[6] = CRC[0];
            send[7] = CRC[1];
            try {
                writeCharacteristic.setValue(send);
                mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            } catch (Exception e) {
                Log.d("data", e.toString());
            }

    }
    //发送数据
    private void writeData(String data){
            int numA = Integer.parseInt(data, 16);
            int numB = Integer.parseInt(data, 16);
            byte dataA[] = util.intToByte2(numA);
            byte dataB[] = util.intToByte2(numB);
            byte CRC[] = new byte[2];
            byte[] send = {0X05, 0X06, 0X00, 0X00, 0X00, 0X00, 0X00, 0X00};
            send[2] = dataA[0];
            send[3] = dataA[1];
            send[4] = dataB[0];
            send[5] = dataB[1];
            CRC = util.CRC16_Check(send, 6);
            send[6] = CRC[0];
            send[7] = CRC[1];
            try {
                writeCharacteristic.setValue(send);
                mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            } catch (Exception e) {
                Log.d("data", e.toString());
            }

    }



}
