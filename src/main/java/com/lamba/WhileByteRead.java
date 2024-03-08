package com.lamba;
@FunctionalInterface
public interface WhileByteRead {
    //从监听端口读取的byte元数据并修改
    public byte[] ReadByte(byte[]origin,int start,int offset);
}
