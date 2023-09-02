package com.core.reactive.corereactive.rpm;

import com.core.reactive.corereactive.hook.ProcessConfig;
import com.core.reactive.corereactive.hook.ProcessHook;
import com.sun.jna.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadProcessMemoryService {

    private final ProcessConfig.Kernel32 kernel32;
    private final ProcessHook processHook;

    public Memory readMemory(long address, int size) {
        Memory memory = new Memory(size);
        kernel32.ReadProcessMemory(processHook.getProcess(), processHook.getBaseAddress() + address, memory, size, null);
        return memory;
    }

    public <T> T read(long address, Class<T> tClass) {
        Integer size = this.getMemory(tClass);
        Memory memory = new Memory(size);
        kernel32.ReadProcessMemory(processHook.getProcess(), processHook.getBaseAddress()+ address, memory, size, null);
        if (tClass.isAssignableFrom(Float.class)) {
            Float f = memory.getFloat(0);
            return (T) f;
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            Integer f = memory.getInt(0);
            return (T) f;
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            Boolean f = memory.getByte(0) != 0;
            return (T) f;
        }
        return null;
    }

    private <T> Integer getMemory(Class<T> tClass) {
        if (tClass.isAssignableFrom(Float.class)) {
            return 8;
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            return 8;
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            return 1;
        }
        return -1;
    }
}
