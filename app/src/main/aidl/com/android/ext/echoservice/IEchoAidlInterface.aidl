// IEchoAidlInterface.aidl
package com.android.ext.echoservice;

// Declare any non-default types here with import statements

interface IEchoAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    oneway void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString);

    /** ping pong */
    String echo(String ping);

    /** ping pong with sleep 2s */
    String slowEcho(String ping);
}