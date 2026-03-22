#!/system/bin/sh
# PTT广播监听脚本（替代方案）
# 直接通过getevent监听PTT按键，通过su发送广播

INPUT_DEV="/dev/input/event1"
BTN9_DOWN=false

while true; do
    # 监听event1设备，查找BTN_9事件
    getevent -l $INPUT_DEV 2>/dev/null | while read line; do
        case "$line" in
            *"BTN_9"*DOWN*)
                if [ "$BTN9_DOWN" = "false" ]; then
                    BTN9_DOWN=true
                    # 发送PTT DOWN广播（需要ROOT）
                    su -c "am broadcast -a android.intent.action.PTT.down --user 0"
                    log -t PTTBroadcast "PTT DOWN sent"
                fi
                ;;
            *"BTN_9"*UP*)
                if [ "$BTN9_DOWN" = "true" ]; then
                    BTN9_DOWN=false
                    # 发送PTT UP广播（需要ROOT）
                    su -c "am broadcast -a android.intent.action.PTT.up --user 0"
                    log -t PTTBroadcast "PTT UP sent"
                fi
                ;;
        esac
    done
    sleep 1
done
