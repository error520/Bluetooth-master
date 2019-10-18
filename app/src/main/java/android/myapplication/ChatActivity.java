package android.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText Register;
    private EditText data;
    private Button btWrite;
    private Button btRead;
    private Button btClear;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattCharacteristic writeCharacteristic;
    List<BluetoothGattCharacteristic> gattCharacteristics;
    List<UUID> readUuid = new ArrayList<UUID>();
    List<UUID> writeUuid = new ArrayList<UUID>();
    List<UUID> writeServiceUuid = new ArrayList<UUID>();
    List<UUID> notifyUuid = new ArrayList<UUID>();
    UUID notify_UUID_service;
    UUID notify_UUID_chara;
    ArrayAdapter<String> chatAdapter;
    private final String TAG = "ChatActivity";
    private BLEService mBluetoothLeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        Intent BLEIntent = new Intent(this, BLEService.class);
        bindService(BLEIntent,connection,BIND_AUTO_CREATE);
        //Log.d("BLEService",mBluetoothLeService.toString());
        //mBluetoothLeService.connect(getIntent().getStringExtra("device_address"));

        //connect(getIntent().getStringExtra("device_address"));

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mBluetoothGatt.disconnect();

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btRead:
                readData();
                break;
            case R.id.btWrite:
                //writeData();

                Log.d(TAG,"点击了写按钮!");
                break;
            case R.id.btClear:
                chatAdapter.clear();
                chatAdapter.notifyDataSetChanged();
                break;
        }
    }

    private void initView(){
            setContentView(R.layout.activity_chat);
            Register = (EditText) findViewById(R.id.register);
            data = (EditText)findViewById(R.id.data);
            btClear = (Button) findViewById(R.id.btClear);
            btRead = (Button) findViewById(R.id.btRead);
            btWrite = (Button) findViewById(R.id.btWrite);
            btWrite.setOnClickListener(this);
            btClear.setOnClickListener(this);
            btRead.setOnClickListener(this);
            ListView chatLog = (ListView) findViewById(R.id.ChatLog);
            chatAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
            chatLog.setAdapter(chatAdapter);
    }

    public boolean connect(final String address) {//4
//        Log.d(TAG, "连接" + mBluetoothDeviceAddress);
        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG,"BluetoothAdapter不能初始化 or 未知 address.");
            Toast.makeText(this,"未能连接",Toast.LENGTH_LONG).show();
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Toast.makeText(this,"设备没找到，不能连接",Toast.LENGTH_SHORT).show();
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

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte bb2[] = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatAdapter.add(control_Activity.toHexString(bb2));
                    chatAdapter.notifyDataSetChanged();
                }
            });

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
    public void read() {
        try {
            mBluetoothGatt.readCharacteristic(readCharacteristic);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    //读取数据
    private void readData(){
        if ((Register.getText().toString().length()<4)||data.getText().toString().length()<4){
            Toast toast = Toast.makeText(ChatActivity.this,"请输入完整的地址或数据!",Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            Log.d(TAG,"不完整!");
        } else {
            int numA = Integer.parseInt(Register.getText().toString(), 16);
            int numB = Integer.parseInt(data.getText().toString(), 16);
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
    }
    //发送数据
    private void writeData(){
        if ((Register.getText().toString().length()<4)||data.getText().toString().length()<4){
            Toast toast = Toast.makeText(ChatActivity.this,"请输入完整的地址或数据!",Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
        } else {
            int numA = Integer.parseInt(Register.getText().toString(), 16);
            int numB = Integer.parseInt(data.getText().toString(), 16);
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


    public static byte[] intToByte4(int i) {
        byte[] targets = new byte[4];
        targets[3] = (byte) (i & 0xFF);
        targets[2] = (byte) (i >> 8 & 0xFF);
        targets[1] = (byte) (i >> 16 & 0xFF);
        targets[0] = (byte) (i >> 24 & 0xFF);
        return targets;
    }



    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BLEService.localBinder) service)
                    .getService();
            Log.d("BLEService",mBluetoothLeService.toString());
            mBluetoothLeService.hello();
            Log.d("BLEService","第二个活动bind了");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
