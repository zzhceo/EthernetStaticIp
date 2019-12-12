# EthernetStaticIp
For Android 9.0 set ethernet static/dhcp ip address

1.need change app's build.gradle, add device platform signature for apk. 
like this:

    //TODO cancel comment, must be use platform signature, or set ip will be error
    // 替换app目录下的platform.keystore为自己设备的对应签名，去掉以下注释，并配置签名信息
    signingConfigs {
        debug {
            storeFile file('platform.keystore')
            storePassword 'xxx'
            keyAlias 'xxx'
            keyPassword 'xxx'
        }
        release {
            storeFile file('platform.keystore')
            storePassword 'xxx'
            keyAlias 'xxx'
            keyPassword 'xxx'
        }
    }

2.need replace app directory platform.keystore use your device platform.keystore.
