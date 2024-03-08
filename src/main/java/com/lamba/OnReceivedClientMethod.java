package com.lamba;

import java.io.OutputStream;

@FunctionalInterface
public interface OnReceivedClientMethod {
    public void OnReceiveClientBytes(byte[]bytes);
}
