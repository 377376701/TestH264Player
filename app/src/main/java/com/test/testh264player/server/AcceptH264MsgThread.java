package com.test.testh264player.server;

import android.os.SystemClock;
import android.util.Log;

import com.test.testh264player.interf.OnAcceptBuffListener;
import com.test.testh264player.interf.OnAcceptTcpStateChangeListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by xu.wang
 * Date on  2017/11/29 14:29:08.
 *
 * @Desc
 */

public class AcceptH264MsgThread extends Thread {
    private final String TAG = "AcceptH264MsgThread";
    private InputStream InputStream;
    private OutputStream outputStream;
    private boolean startFlag = true;
    private OnAcceptBuffListener listener;
    private OnAcceptTcpStateChangeListener mStateChangeListener;

    public AcceptH264MsgThread(InputStream is, OutputStream outputStream, OnAcceptBuffListener listener, OnAcceptTcpStateChangeListener disconnectListenerlistener) {
        this.InputStream = is;
        this.outputStream = outputStream;
        this.listener = listener;
        this.mStateChangeListener = disconnectListenerlistener;
    }

    @Override
    public void run() {
        super.run();
        byte[] ok = "OK".getBytes();
        try {
            outputStream.write(ok);
            if (mStateChangeListener != null) mStateChangeListener.acceptTcpConnect();
            while (startFlag) {
                byte[] length = readByte(InputStream, 4);
                if (length.length == 0) {
                    SystemClock.sleep(1);
                    continue;
                }
                int buffLength = bytesToInt(length);
                byte[] buff = readByte(InputStream, buffLength);
                listener.acceptBuff(buff);
            }
        } catch (Exception e) {
            Log.i(TAG, "read and write buff exception = " + e.toString());
            if (mStateChangeListener != null) mStateChangeListener.acceptTcpDisconnect(e);
        } finally {
            startFlag = false;
        }
    }

    public void shutdown() {
        startFlag = false;
        this.interrupt();
    }

    /**
     * 保证从流里读到指定长度数据
     *
     * @param is
     * @param readSize
     * @return
     * @throws Exception
     */
    private byte[] readByte(InputStream is, int readSize) throws IOException {
        byte[] buff = new byte[readSize];
        int len = 0;
        int eachLen = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (len < readSize) {
            eachLen = is.read(buff);
            if (eachLen != -1) {
                len += eachLen;
                baos.write(buff, 0, eachLen);
            } else {
                baos.close();
                throw new IOException(TAG + "   :tcp have diaconnect...");
            }
            if (len < readSize) {
                buff = new byte[readSize - len];
            }
        }
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src byte数组
     * @return int数值
     */
    public static int bytesToInt(byte[] src) {
        int value;
        value = (int) ((src[0] & 0xFF)
                | ((src[1] & 0xFF) << 8)
                | ((src[2] & 0xFF) << 16)
                | ((src[3] & 0xFF) << 24));
        return value;
    }
}
