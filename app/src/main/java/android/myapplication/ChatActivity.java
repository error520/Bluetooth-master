package android.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
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
    ArrayAdapter<String> chatAdapter;
    private final String TAG = "ChatActivity";
    private LocalBroadcastManager localBroadcastManager;
    private BLEService mBluetoothLeService;
    private LocalReceiver localReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initService();
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
                writeData();
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

    //通过调用服务读取数据
    private void readData(){
        if ((Register.getText().toString().length()<4)||data.getText().toString().length()<4){
            Toast toast = Toast.makeText(ChatActivity.this,"请输入完整的地址或数据!",Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            Log.d(TAG,"不完整!");
        } else {
            mBluetoothLeService.readData(Register.getText().toString(),data.getText().toString());
        }
    }
    //通过调用服务发送数据
    private void writeData(){
        if ((Register.getText().toString().length()<4)||data.getText().toString().length()<4){
            Toast toast = Toast.makeText(ChatActivity.this,"请输入完整的地址或数据!",Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
        } else {
             mBluetoothLeService.writeData(Register.getText().toString(),data.getText().toString());
        }
    }
    //初始化服务和广播
    private void initService(){
        Intent BLEIntent = new Intent(this, BLEService.class);
        bindService(BLEIntent,connection,BIND_AUTO_CREATE);
        localReceiver = new LocalReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(localReceiver,util.makeGattUpdateIntentFilter());
    }
    //得到服务实体
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BLEService.localBinder) service)
                    .getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    //广播接收器
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,action);
            if(action.equals(BLEService.ACTION_DATA_AVAILABLE)) {
                String message = intent.getStringExtra(BLEService.EXTRA_MESSAGE_DATA);
                chatAdapter.add(message);
                chatAdapter.notifyDataSetChanged();
            }
        }
    }

}
