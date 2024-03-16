package com.server;

import com.lamba.OnReceivedClientMethod;
import com.lamba.WhileByteRead;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public class ForwardingHandler implements Runnable {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final String name;
    private OnReceivedClientMethod receivedClientMethod;
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private WhileByteRead whileByteRead;
    private final int bufferedSize;

    public ForwardingHandler(InputStream inputStream, int bufferedSize, OutputStream outputStream, String name, OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.name = name;
        this.receivedClientMethod = receivedClientMethod;
        this.bufferedSize = bufferedSize;
    }

    public void setOnReceive(OnReceivedClientMethod onReceive) {
        this.receivedClientMethod = onReceive;
    }

    public void setWhileByteRead(WhileByteRead whileByteRead) {
        this.whileByteRead = whileByteRead;
    }

    public ForwardingHandler(InputStream inputStream, int bufferedSize, OutputStream outputStream, OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.receivedClientMethod = receivedClientMethod;
        this.name = Thread.currentThread().getName();
        this.bufferedSize = bufferedSize;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[this.bufferedSize];
            int bytesRead;

            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (this.whileByteRead != null) {
//                        System.out.printf("已完成加密",new String(buffer,0,bytesRead));
                        buffer = this.whileByteRead.ReadByte(buffer, 0, bytesRead);
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    if (this.receivedClientMethod != null) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        this.receivedClientMethod.OnReceiveClientBytes(byteArrayOutputStream.toByteArray());
                        byteArrayOutputStream.flush();
                    }

                    outputStream.flush();
                }
                inputStream.close();
                outputStream.close();
                byteArrayOutputStream.close();
            } catch (SocketException ignored) {

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
