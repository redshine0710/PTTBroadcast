# PTT Broadcast Module - Boot Script
# 等待系统启动后启动PTT广播服务

while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done

sleep 5

# 启动PTT广播服务
if [ -f /system/app/PTTBroadcast/PTTBroadcast.apk ]; then
    # 使用am启动服务
    am startservice -n com.pttbroadcast/.PTTService --user 0
    log -t PTTBroadcast "PTT Broadcast service started"
else
    log -t PTTBroadcast "PTTBroadcast.apk not found"
fi
