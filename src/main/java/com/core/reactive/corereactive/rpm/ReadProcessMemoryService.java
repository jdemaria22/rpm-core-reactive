package com.core.reactive.corereactive.rpm;

import com.core.reactive.corereactive.hook.ProcessConfig;
import com.core.reactive.corereactive.hook.ProcessHook;
import com.sun.jna.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadProcessMemoryService {

    private final ProcessConfig.Kernel32 kernel32;
    private final ProcessHook processHook;

    public Memory readMemory(long address, int size, boolean isBaseAddress) {
        Memory memory = new Memory(size);
        long value = isBaseAddress ? processHook.getBaseAddress() + address : address;
        kernel32.ReadProcessMemory(processHook.getProcess(), value, memory, size, null);
        return memory;
    }

    public Mono<Memory> reactiveReadMemory(long address, int size, boolean isBaseAddress) {
        return Mono.fromCallable(() -> {
            Memory memory = new Memory(size);
            long value = isBaseAddress ? processHook.getBaseAddress() + address : address;
            kernel32.ReadProcessMemory(processHook.getProcess(), value, memory, size, null);
            return memory;
        });
    }

    public <T> Mono<T> reactiveRead(long address, Class<T> tClass, boolean isBaseAddress) {
        return this.readMemory(address ,tClass, isBaseAddress).flatMap(memory -> this.getValue(memory, tClass));
    }

    public <T> T read(long address, Class<T> tClass, boolean isBaseAddress) {
        Integer size = this.getMemory(tClass);
        Memory memory = new Memory(size);
        long value = isBaseAddress ? processHook.getBaseAddress() + address : address;
        kernel32.ReadProcessMemory(processHook.getProcess(), value, memory, size, null);
        if (tClass.isAssignableFrom(Long.class)) {
            Long l = memory.getLong(0);
            return (T) l;
        }
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

    private <T> Mono<T> getValue(Memory memory, Class<T> tClass) {
        if (tClass.isAssignableFrom(Float.class)) {
            return Mono.fromCallable(() -> {
                Float f = memory.getFloat(0);
                return (T) f;
            });
        }
        if (tClass.isAssignableFrom(Long.class)) {
            return Mono.fromCallable(() -> {
                Long l = memory.getLong(0);
                return (T) l;
            });
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            return Mono.fromCallable(() -> {
                Boolean f = memory.getByte(0) != 0;
                return (T) f;
            });
        }
        return Mono.empty();
    }

    private <T> Mono<Memory> readMemory(long address, Class<T> tClass, boolean isBaseAddress) {
        return this.getSize(tClass)
                .flatMap(size -> Mono.fromCallable(() -> {
                    Memory memory = new Memory(size);
                    long value = isBaseAddress ? processHook.getBaseAddress() + address : address;
                    kernel32.ReadProcessMemory(processHook.getProcess(), value, memory, size, null);
                    return memory;
                }));
    }

    private <T> Integer getMemory(Class<T> tClass) {
        if (tClass.isAssignableFrom(Float.class)) {
            return 8;
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            return 8;
        }
        if (tClass.isAssignableFrom(Long.class)) {
            return 8;
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            return 1;
        }
        return -1;
    }

    private <T> Mono<Integer> getSize(Class<T> tClass) {
        return Mono.fromCallable(() -> {
            if (tClass.isAssignableFrom(Float.class)) {
                return 8;
            }
            if (tClass.isAssignableFrom(Long.class)) {
                return 8;
            }
            if (tClass.isAssignableFrom(Boolean.class)) {
                return 1;
            }
            return -1;
        });
    }

}
