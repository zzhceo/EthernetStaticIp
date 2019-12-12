package com.zzh.ethernetstaticip.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class IpGetUtil {
    private static final String TAG = "IpGetUtil";

    /**
     * Ipv4 address check.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     *
     * @return True if the input parameter is a valid IPv4 address.
     */
    public static boolean isIPv4Address(String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /**
     * Get local Ip address.
     */
    public static InetAddress getLocalIPAddress() {
        Enumeration<NetworkInterface> enumeration = null;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                NetworkInterface nif = enumeration.nextElement();
                Enumeration<InetAddress> inetAddresses = nif.getInetAddresses();
                if (inetAddresses != null) {
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (!inetAddress.isLoopbackAddress() && isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                    }
                }
            }
        }
        return null;
    }

    //获取以太网的IP地址
    public static String getIpAddress(Context context) {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = mConnectivityManager.getActiveNetwork();
            LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network);
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                InetAddress address = linkAddress.getAddress();
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getIpAddress error" + e.getMessage(), e);
        }
        // IPv6 address will not be shown like WifiInfo internally does.
        return "";
    }

    /**
     * @param context use Application context is better
     * @param ipAddress ip address: like 192.168.1.168
     * @param mode : STATIC or DHCP, set static or dhcp ip mode
     * @param netmask ip mask, like 255.255.255.0
     * @param gateway gateway, like 192.168.1.1
     * @param dns1 dns 1
     * @param dns2 dns 2, if mode=static, then can use "" or null
     *   eg. dhcp mode: setEthernetIP(ApplicationContext, "DHCP", "", "", "", "", "");
     *         static mode: setEthernetIP(ApplicationContext, "STATIC",
     *                     "192.168.1.168", "255.255.255.0",
     *                     "192.168.1.1", "114.114.114.114", "8.8.8.8");
     *  for android 9.0
     * */
    public static boolean setEthernetIP(Context context, String mode, String ipAddress, String netmask,
                                     String gateway, String dns1, String dns2) {
        if (context == null || (!"STATIC".equals(mode) && !"DHCP".equals(mode))) {
            Log.d(TAG, " setEthernetIP failed, param incorrect context=" + context + ", mode=" + mode);
            return false;
        }

        try {
            // get EthernetManager instance by reflect @{
            Class<?> ethernetManagerClass = Class
                    .forName("android.net.EthernetManager");
            Class<?> iEthernetManagerClass = Class
                    .forName("android.net.IEthernetManager");
            // 获取ETHERNET_SERVICE参数
            String ETHERNET_SERVICE = (String) Context.class.getField(
                    "ETHERNET_SERVICE").get(null);
            // 获取ethernetManager服务对象
            Object ethernetManager = context.getSystemService(ETHERNET_SERVICE);
            // 获取在EthernetManager中的抽象类mService成员变量
            Field mService = ethernetManagerClass.getDeclaredField("mService");
            // 修改private权限
            mService.setAccessible(true);
            // 获取抽象类的实例化对象
            Object mServiceObject = mService.get(ethernetManager);
            Object ethernetManagerInstance = ethernetManagerClass
                    .getDeclaredConstructor(Context.class,
                            iEthernetManagerClass).newInstance(context,
                            mServiceObject);
            EthernetManager mEthManager = (EthernetManager) ethernetManagerInstance;
            // @}
            // del, make even not plug in line, we also can change ip
            /*String[] ifaces = mEthManager.getAvailableInterfaces();
            if (ifaces.length <= 0) {
                Log.e(TAG, " setEthernetIP failed ifaces.length <= 0");
                return false;
            }
            String mInterfaceName = ifaces[0];*/
            String mInterfaceName = "eth0";
            // current assignment is DHCP OR STATIC
            String assignMent = mEthManager.getConfiguration(mInterfaceName).ipAssignment.name();
            // if assignMent is dhcp, no need repeat set
            if ("DHCP".equals(mode) && assignMent.equals(mode)) {
                Log.d(TAG, " setEthernetIP mode == assignment = DHCP, no need repeat set");
                return false;
            }

            Log.d(TAG, " setEthernetIP mInterfaceName=" + mInterfaceName + ", assignment=" + assignMent);
            if ("DHCP".equals(mode)) {
                Log.i(TAG, " setEthernetIP  set dhcp started");
                IpConfiguration dhcpConfiguration = new IpConfiguration(IpConfiguration.IpAssignment.DHCP,
                        IpConfiguration.ProxySettings.NONE, null, null);
                dhcpConfiguration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                mEthManager.setConfiguration(mInterfaceName, dhcpConfiguration);
                return true;
            }

            if (TextUtils.isEmpty(ipAddress)
                    || TextUtils.isEmpty(netmask) || TextUtils.isEmpty(gateway)
                    || TextUtils.isEmpty(dns1)) {
                Log.e(TAG, "setEthernetIP error has some param is null ipAddress=" + ipAddress
                        + ", netmask=" + netmask + ", gateway=" + gateway
                        + ", dns1=" + dns1 + ", dns2=" + dns2);
                return false;
            }
            StaticIpConfiguration mStaticIpConfiguration = new StaticIpConfiguration();
            int prefixLength = maskStr2InetMask(netmask);
            InetAddress inetAddr = null;
            InetAddress gatewayAddr = getIPv4Address(gateway);
            InetAddress dnsAddr = getIPv4Address(dns1);

            if (TextUtils.isEmpty(ipAddress)) {
                inetAddr = getLocalIPAddress();
            } else {
                String[] ipStr = ipAddress.split("\\.");
                byte[] ipBuf = new byte[4];
                for (int i = 0; i < 4; i++) {
                    ipBuf[i] = (byte) (Integer.parseInt(ipStr[i]) & 0xff);
                }
                try {
                    inetAddr = InetAddress.getByAddress(ipBuf);
                    Log.d(TAG, "setEthernetIP  address correct inetAddr=" + inetAddr);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            if (inetAddr == null || inetAddr.getAddress().toString().isEmpty()
                    || prefixLength == 0 || gatewayAddr.toString().isEmpty()
                    || dnsAddr == null || dnsAddr.toString().isEmpty()) {
                Log.d(TAG, " setEthernetIP  address incorrect inetAddr=" + inetAddr);
                return false;
            }

            Class<?> linkAddressClass = null;
            linkAddressClass = Class.forName("android.net.LinkAddress");
            Class[] cl = new Class[]{InetAddress.class, int.class};
            Constructor cons = null;
            //取得所有构造函数
            try {
                cons = linkAddressClass.getConstructor(cl);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            //给传入参数赋初值
            Object[] x = {inetAddr, prefixLength};
            try {
                mStaticIpConfiguration.ipAddress = (LinkAddress) cons.newInstance(x);
                Log.d(TAG, " setEthernetIP mStaticIpConfiguration.ipAddress=" + mStaticIpConfiguration.ipAddress);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            mStaticIpConfiguration.gateway = gatewayAddr;
            mStaticIpConfiguration.dnsServers.add(dnsAddr);
            if (!dns2.isEmpty())
                mStaticIpConfiguration.dnsServers.add(getIPv4Address(dns2));

            Log.d(TAG, " setEthernetIP mStaticIpConfiguration  ====" + mStaticIpConfiguration
                    + ", inetAddr=" + inetAddr + ", mEthManager=" + mEthManager);

            IpConfiguration ipConfiguration = new IpConfiguration(IpConfiguration.IpAssignment.STATIC,
                    IpConfiguration.ProxySettings.NONE, mStaticIpConfiguration, null);
            ipConfiguration.setIpAssignment(IpConfiguration.IpAssignment.STATIC);
            ipConfiguration.setStaticIpConfiguration(mStaticIpConfiguration);
            mEthManager.setConfiguration(mInterfaceName, ipConfiguration);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setEthernetIP error e=" + e.getMessage(), e);
        }
        return false;
    }

    /*
     * convert subMask string to prefix length
     */
    public static int maskStr2InetMask(String maskStr) {
        StringBuffer sb;
        String str;
        int inetmask = 0;
        int count = 0;
        /*
         * check the subMask format
         */
        Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
        if (pattern.matcher(maskStr).matches() == false) {
            Log.e(TAG, "subMask is error");
            return 0;
        }

        String[] ipSegment = maskStr.split("\\.");
        for (int n = 0; n < ipSegment.length; n++) {
            sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
            str = sb.reverse().toString();
            count = 0;
            for (int i = 0; i < str.length(); i++) {
                i = str.indexOf("1", i);
                if (i == -1)
                    break;
                count++;
            }
            inetmask += count;
        }
        return inetmask;
    }

    public static InetAddress getIPv4Address(String text) {
        try {
            return NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

}
