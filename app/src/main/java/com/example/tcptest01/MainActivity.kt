package com.example.tcptest01
/*
伺服器跟客戶端只差在, 伺服器用的實例是ServerSock , 客戶端用的是Socket
以下的實例來看, ServerSock 產生了一個Socket 是sSocket , Socket 產生的是mSocket
伺服器端是等待人來連接所以多了一個accept() , 連好之後就可以用sSocket 來接收/傳送資料了
客戶端用connect 來連接 Server 端, 好之後就可以用sSocket 來接收/傳送資料了
傳送跟接收資料只差在, 伺服器端用sSocket , 客戶端用mSocket 後面的內容完全一模一樣都是用
InputStream  , OutputStream 來讀取資料
TCP/ip 的封裝成Socket 來相互傳資料, 資料是雙向的直接可用

 */
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


val TAG = "myTag"
const val HOSTIP = "192.168.0.100"
const val PORT = 4000
const val SERVERPORT = 4000

//const val DATA = "taonce"
const val DATA = "0fff"

class MainActivity : AppCompatActivity() {
    var host = HOSTIP // 主机是本机
    var port = PORT// 使用 2333 端口
    var serverport = SERVERPORT
    var datatext = DATA
    var hexconvert = false   // 就是ascii code

    var mSocket: Socket? = null         // 客戶端
    var sSocket: Socket? = null  // 伺服器端
    var outputStream: OutputStream? = null
//    var inputStream:InputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//初值
        editTextTargetIP.setText(HOSTIP)
        editTextTargetPort.setText(PORT.toString())
        editTextData.setText(DATA)
        editTextServerPort.setText(SERVERPORT.toString())


        // checkbox 按鍵處理
        checkBoxDefaultTargetIP.setOnClickListener {
            if (checkBoxDefaultTargetIP.isChecked)
                host = HOSTIP
            else host = ""

            editTextTargetIP.setText(host)
        }
        //
        checkBoxDefaultTargetPort.setOnClickListener {
            if (checkBoxDefaultTargetPort.isChecked)
                port = PORT
            else port = 0
            editTextTargetPort.setText(port.toString())
        }
//
        checkBoxHex.setOnClickListener {
            if (checkBoxHex.isChecked)
                hexconvert = true
            else hexconvert = false
        }

        //一般按鍵處理
        checkBoxDefaultData.setOnClickListener {
            if (checkBoxDefaultData.isChecked)
                datatext = DATA
            else datatext = ""
            editTextData.setText(datatext)
        }

        //一般按鍵處理
        checkBoxServerPort.setOnClickListener {
            if (checkBoxServerPort.isChecked)
                serverport = SERVERPORT
            else serverport = 0
            editTextServerPort.setText(serverport.toString())
        }

        btnReset.setOnClickListener {
            //沒有建Socket 關閉會當機
            mSocket?.close()
            sSocket?.close()
            runOnUiThread { textView.text = "Socket 清除完畢" }
        }

//=============================  Client 副程式處理==========================
        // Client connect 連接
        btnConnect.setOnClickListener {
            GlobalScope.launch {
                mSocket = Socket()
                try {
                    host = editTextTargetIP.text.toString()
                    port = editTextTargetPort.text.toString().toInt()
                    mSocket!!.connect(InetSocketAddress(host, port), 2000)
                    if (mSocket!!.isConnected) {
// sendData("taonce")
// receiverData()
                        runOnUiThread { textView.text = "Client 連接成功" }
                        btnConnect.setTextColor(android.graphics.Color.RED)
                    }
                } catch (e: Exception) {
                    runOnUiThread { textView.text = "Client 連接出錯" }
                    Log.d(TAG, "连接出错：${e.message}")
                }

            } //globalscope

        }

//Client 傳送資料
        btnSenData.setOnClickListener {
            GlobalScope.launch {
                if (mSocket!!.isConnected) {
                    datatext = editTextData.text.toString()
                    println("datatext = $datatext")
                    if (datatext != "") {
                        runOnUiThread { textView.text = "我送資料 = $datatext" }
                        println("我有資料 ")
                        sendData(datatext)
                    }  //送資料一定要在協程做, 不然是錯誤的

                    else {
                        runOnUiThread { textView.text = "資料不能為空" }
                        println("資料不能為空 ")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "資料不能為空", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

//Client READ 讀的按鍵
        btnReadData.setOnClickListener {
            GlobalScope.launch {
                if (mSocket!!.isConnected) {
                    receiverData() //送資料一定要在協程做, 不然是錯誤的
                }
            }
        }
//-----------------------  Server ---------------------------------------
        //Server Connect 連線
        btnServerConnect.setOnClickListener {
            GlobalScope.launch {
                val Port1 = editTextServerPort.text.toString()
                serverport = Port1.toInt()
                server_connect(serverport)
            }
        }

        //server端傳送資料
        btnServerSenData.setOnClickListener {
            GlobalScope.launch {
                if (sSocket!!.isConnected) {
                    datatext = editTextData.text.toString()
                    println("datatext = $datatext")
                    if (datatext != "") {
                        println("我有資料 ")
                        server_sendData(datatext)
                    }  //送資料一定要在協程做, 不然是錯誤的

                    else {
                        println("資料不能為空 ")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "資料不能為空", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        //Server READ 讀的按鍵
        btnServerREAD.setOnClickListener {
            GlobalScope.launch {
                if (sSocket!!.isConnected) {
                    server_receiverData() //收資料一定要在協程做, 不然是錯誤的
                }
            }
        }


    } // oncreate

    /**                  副程式開始
     * 定时发送数据
     */
    fun sendData(message: String) {
        Log.d(TAG, "Hello 我進來了 ")
        Log.d(TAG, "mSocket,$mSocket")
        try {
            outputStream = mSocket?.getOutputStream()

            if (hexconvert == true) {
                val size = message.length / 2  // 忽略奇數值, 若輸入3個字串只處理1個byte
                // val r = message.length % 2 取餘

                val msg = ByteArray(size)           // 取得size
                //取值
                for (i in 0..size - 1) {
                    msg[i] = message.subSequence(i * 2, i * 2 + 2)
                        .toString()
                        .toInt(16)
                        .toByte()
                }

                //         msg[1] = message.subSequence(2,4).toString().toInt(16).toByte()
                val a = message.substring(0, 2)
                val b = message.substring(2, 4)
                val c = a.toString().toInt(16)
                val d = b.toString().toInt(16)
                println("a= $a , b =$b,c= $c , d =$d ")
                //  msg[0]=c.toByte()
                //  msg[1]=d.toByte()
                //   msg[0] = 0xaa.toByte()
                //  msg[1] = 0xdd.toByte()
                outputStream!!.write(msg)           //寫入資料 （Hex16進制）
            } else {
                outputStream!!.write(message.toByteArray())   //寫入資料 （ASCII）
            }
            outputStream!!.flush()      //即時送出

            Log.d(TAG, "发送给服务端内容为：$message")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "sendData: 發送錯誤")
        }
    }

//================Client 接收數據 =================================
    /**
     * 定时接收数据
     */
    fun receiverData() {
        Log.d(TAG, "receiverData: 接收進來了")
        try {
            //        val data = BufferedReader(InputStreamReader(mSocket.getInputStream(), "ISO-8859-1"))
            val inputStream = mSocket?.getInputStream()
            val data = BufferedReader(InputStreamReader(inputStream, "ISO-8859-1"))

            val cbuf = CharArray(1024)            // 1次讀1024字元
            var num = data.read(cbuf)              // 這是阻塞式,就是一直等到有值為止  (讀1個字）
            Log.d(TAG, "获取服务端數目为：$num")
            var s = ""
            if (checkBoxHex.isChecked) {            //就是true = 16進制處理
                for (i in 0..num - 1) {
                    s = s + cbuf[i].toInt().toString(16)     // 重點在toString(16)
                    Log.d(TAG, "获取服务端内容为：${cbuf[i].toInt()}")   //這是10進制顯示
                }
            } else {                                // ascii 處理
                for (i in 0..num - 1) {
                    s = s + cbuf[i].toString()
                    Log.d(TAG, "获取服务端内容为：${cbuf[i]}")
                }
            }                                 //ascii
//印出結果
            runOnUiThread { textView.text = s }

// 若你是ascii 就用cbuf[i] , 若你是16進制則後面加 .toInt() 就可以知道答案了

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "receiveData: 接收錯誤")
        }
    }


    //================================  Server 伺服器端 ========================
    fun server_connect(port: Int) {
        Log.d(TAG, "server: 連線進來了")
        try {
            val serverSocket = ServerSocket(port)  //要一個實例
            //    println("Server is listening on port 4000")
            runOnUiThread { textView.text = "等待連接: Port = $port" }
            sSocket = serverSocket.accept()     // 阻塞式連接, 會等到有人來連
            // println("New client connected") }
            Log.d(TAG, "server: 連接成功")
            runOnUiThread { textView.text = "連接成功,Port:$port" }
        } catch (e: Exception) {
            Log.d(TAG, "server_connect: 錯誤")
        }
    }

    //================================  Server 伺服器端 ========================
    fun server_sendData(message: String) {
        Log.d(TAG, "Hello 我進來了 ")
        //   Log.d(TAG, "mSocket,$mSocket")
        try {
            outputStream = sSocket?.getOutputStream()

            if (hexconvert == true) {
                val size = message.length / 2  // 忽略奇數值, 若輸入3個字串只處理1個byte
                // val r = message.length % 2 取餘

                val msg = ByteArray(size)           // 取得size
                //取值
                for (i in 0..size - 1) {
                    msg[i] = message.subSequence(i * 2, i * 2 + 2)
                        .toString()
                        .toInt(16)
                        .toByte()
                }

                //         msg[1] = message.subSequence(2,4).toString().toInt(16).toByte()
                val a = message.substring(0, 2)
                val b = message.substring(2, 4)
                val c = a.toString().toInt(16)
                val d = b.toString().toInt(16)
                println("a= $a , b =$b,c= $c , d =$d ")
                //  msg[0]=c.toByte()
                //  msg[1]=d.toByte()
                //   msg[0] = 0xaa.toByte()
                //  msg[1] = 0xdd.toByte()
                outputStream!!.write(msg)           //寫入資料 （Hex16進制）
            } else {
                outputStream!!.write(message.toByteArray())   //寫入資料 （ASCII）
            }
            outputStream!!.flush()      //即時送出

            Log.d(TAG, "发送给服务端内容为：$message")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "sendData: 發送錯誤")
        }

    }

    //---------------------------------
    fun server_receiverData() {
        Log.d(TAG, "receiverData: 接收進來了")
        try {
            //        val data = BufferedReader(InputStreamReader(mSocket.getInputStream(), "ISO-8859-1"))
            val inputStream = sSocket?.getInputStream()
            val data = BufferedReader(InputStreamReader(inputStream, "ISO-8859-1"))

            val cbuf = CharArray(1024)            // 1次讀1024字元
            var num = data.read(cbuf)              // 這是阻塞式,就是一直等到有值為止  (讀1個字）
            Log.d(TAG, "获取服务端數目为：$num")
            var s = ""
            if (checkBoxHex.isChecked) {            //就是true = 16進制處理
                for (i in 0..num - 1) {
                    s = s + cbuf[i].toInt().toString(16)   // 這是重點將字串轉成16進制顯示出來
                    Log.d(TAG, "获取服务端内容为s：${cbuf[i].toInt()}")  //這是10進制顯示
                }
            } else {                                     //ascii 處理
                for (i in 0..num - 1) {
                    s = s + cbuf[i].toString()
                    Log.d(TAG, "获取服务端内容为cbuf：${cbuf[i]}")
                }
            }                                 //ascii

//印出結果
            runOnUiThread { textView.text = s }

// 若你是ascii 就用cbuf[i] , 若你是16進制則後面加 .toInt() 就可以知道答案了

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "receiveData: 接收錯誤")
        }
    }
}