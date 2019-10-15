package android.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.myapplication.MainActivity.REQUEST_ENABLE_BT;

public class control_Activity extends AppCompatActivity {
    private Button BtRead1;
    private Button BtWrite1;
    private EditText DeviceName;
    private EditText password;
    private String TAG = "control_Activity";
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;//是否正在搜索
    private Handler mHandler;
    private boolean flag= true;
    //15秒搜索时间
    private static final long SCAN_PERIOD = 10000;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    List<BluetoothGattCharacteristic> gattCharacteristics;
    List<UUID> readUuid = new ArrayList<UUID>();
    private BluetoothGattCharacteristic readCharacteristic;
    List<UUID> writeUuid = new ArrayList<UUID>();
    List<UUID> writeServiceUuid = new ArrayList<UUID>();
    List<UUID> notifyUuid = new ArrayList<UUID>();
    private BluetoothGattCharacteristic mCharacteristic;
    UUID notify_UUID_service;
    UUID notify_UUID_chara;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_ui);
        BtRead1 = (Button)findViewById(R.id.button1);
        BtWrite1 = (Button)findViewById(R.id.button2);
        password = (EditText)findViewById(R.id.editText1_1);
        DeviceName = (EditText)findViewById(R.id.editText1_2);
        Intent intent = getIntent();
        //String data = intent.getStringExtra("device_address");
        //Log.d("data",data);

        mHandler = new Handler();
        BtRead1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(control_Activity.this,"读用户密码成功:\n1010",
                        Toast.LENGTH_SHORT).show();
                read();
                password.setText("1010");
            }
        });

        BtWrite1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataSend();
                Toast.makeText(control_Activity.this,"写用户密码成功:\n"+password.getText(),
                        Toast.LENGTH_SHORT).show();
            }
        });

//        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //检查手机是否支持BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
        }
        //打开蓝牙权限(在我手机上好像不能自动打开)
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        getBlePermissionFromSys();
        //scanLeDevice(true);

        connect("78:DB:2F:C7:63:A8");
        mBluetoothGatt.discoverServices();

    }


    public boolean connect(final String address) {//4
//        Log.d(TAG, "连接" + mBluetoothDeviceAddress);
//        if (mBluetoothAdapter == null || address == null) {
//            Log.d(TAG,"BluetoothAdapter不能初始化 or 未知 address.");
//            return false;
//        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d("data", "设备没找到，不能连接");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, true, mBluetoothGattCallback);//真正的连接
        //这个方法需要三个参数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接到它），和BluetoothGattCallback调用。
        Log.d("data", "尝试新的连接.");
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

        //当连接状态发生改变
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
                            mCharacteristic = gattCharacteristic;
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
//            Log.d(TAG,bb.length+"");
////            for(int i=0;i<bb.length;i++){
////
////            }
//            Log.d(TAG,toHexString(bb));
            byte bb[] = characteristic.getValue();
            Log.d(TAG, toHexString(bb));

        }

        //发送数据后的回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte bb2[] = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    password.setText(toHexString(bb2));
                }
            });

            Log.d(TAG, toHexString(bb2));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor读
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {//descriptor写
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
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
    public void read() {
        try {

                mBluetoothGatt.readCharacteristic(readCharacteristic);


        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }
    /**
     * 向蓝牙发送数据
     */
    public void dataSend(){

        byte[] send={(byte) 0xaa,0x01,0x01,(byte)0x81,(byte) 0xff};

        BluetoothGattService service=mBluetoothGatt.getService(writeServiceUuid.get(20));
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(writeUuid.get(20));
        try {
//            for(int j=0;j<writeServiceUuid.size();j++){
//                Log.d(TAG,writeServiceUuid.get(j).toString());
//            }

            mCharacteristic.setValue(send);
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
//            for (int i = 0; i < gattCharacteristics.size(); i++) {
//
//                //if (gattCharacteristics.get(i).getUuid().toString().equals("0000fff6-0000-1000-8000-00805f9b34fb")) {//对应的uuid
//                mCharacteristic = gattCharacteristics.get(i);
//
//
//                Log.d(TAG, gattCharacteristics.size() + "");
//                mCharacteristic.setValue(send);//写入的数据
//                mBluetoothGatt.writeCharacteristic(mCharacteristic);
//                Log.d(TAG, "发送数据成功");
//                //}
//
//            }
        }catch(Exception e){
            Log.d("data",e.toString());
        }
        //mCharacteristic.setValue(send);
        //boolean status = mBluetoothGatt.writeCharacteristic(mCharacteristic);
        //Log.e("dataSend", status+"");
    }



    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();//接收广播
            Log.d(TAG, "action:" + action);

        }
    };
    private void scanLeDevice(final boolean enable) {

        if (enable) {//true
            //10秒后停止搜索
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    DeviceName.setText("10秒过去了");
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {//false
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }

    //获取位置权限
    public void getBlePermissionFromSys() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 102;
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                        password.setText(device.getAddress());

                    //在这里可以把搜索到的设备保存起来
                    //device.getName();获取蓝牙设备名字
                    //device.getAddress();获取蓝牙设备mac地址
                    //这里的rssi即信号强度，即手机与设备之间的信号强度。
                }
            });
        }
    };

    public static String toHexString(byte[] byteArray) {
        if (byteArray == null || byteArray.length < 1)
            throw new IllegalArgumentException("this byteArray must not be null or empty");

        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            if ((byteArray[i] & 0xff) < 0x10)//0~F前面不零
                hexString.append("0");
            hexString.append(Integer.toHexString(0xFF & byteArray[i])+" ");
        }
        return hexString.toString().toUpperCase();
    }

}
