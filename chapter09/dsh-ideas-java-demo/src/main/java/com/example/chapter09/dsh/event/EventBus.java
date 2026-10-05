package com.example.chapter09.dsh.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 类型化事件总线：对应 Cordis 的四种分发模式。
 *
 * dsh 里事件的分发模式是事件公开契约的一部分：
 * - emit：通知型，观察者拿不到返回值，也不阻塞语义；
 * - waterfall：环绕中间件，监听器可以修改参数、包装返回值，或者不调 next() 直接短路；
 * - serial：按注册顺序逐个执行并返回结果（demo 里同步实现，真实 dsh 是 async）；
 * - parallel：并行扇出，等待全部完成。
 *
 * 这个 demo 用同步实现，是为了让日志顺序稳定、便于阅读；真实 Cordis 里
 * serial/parallel 是异步语义。
 */
public final class EventBus {

    /** 观察型监听器：对应 emit。 */
    @FunctionalInterface
    public interface Emitter {
        void on(Object[] args);
    }

    /** 环绕型监听器：对应 waterfall。args + next()，不调 next() 即短路。 */
    @FunctionalInterface
    public interface WaterfallListener {
        Object around(Object[] args, Next next);

        interface Next {
            Object call();
        }
    }

    /** 串行决策型监听器：对应 serial，返回值参与决策。 */
    @FunctionalInterface
    public interface SerialListener {
        Object handle(Object[] args);
    }

    private final Map<String, List<Emitter>> emitters = new LinkedHashMap<>();
    private final Map<String, List<WaterfallListener>> waterfalls = new LinkedHashMap<>();
    private final Map<String, List<SerialListener>> serials = new LinkedHashMap<>();

    private final ExecutorService parallelExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "dsh-demo-parallel");
        t.setDaemon(true);
        return t;
    });

    public synchronized void on(String event, Emitter listener) {
        emitters.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public synchronized void onWaterfall(String event, WaterfallListener listener) {
        waterfalls.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public synchronized void onSerial(String event, SerialListener listener) {
        serials.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public synchronized void off(String event, Emitter listener) {
        List<Emitter> list = emitters.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    public synchronized void offWaterfall(String event, WaterfallListener listener) {
        List<WaterfallListener> list = waterfalls.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    public synchronized void offSerial(String event, SerialListener listener) {
        List<SerialListener> list = serials.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    /** emit：按注册顺序通知所有观察者，不关心返回值。 */
    public void emit(String event, Object... args) {
        List<Emitter> list;
        synchronized (this) {
            list = emitters.get(event);
        }
        if (list != null) {
            for (Emitter emitter : new ArrayList<>(list)) {
                emitter.on(args);
            }
        }
    }

    /**
     * waterfall：把监听器串成洋葱圈。
     * 决策权监听器（如权限策略）可以直接返回、不调 next()——短路是设计意图；
     * 观察型监听器改完共享对象后必须 next() 往下传。
     */
    public Object waterfall(String event, Object... args) {
        List<WaterfallListener> list;
        synchronized (this) {
            list = waterfalls.get(event);
        }
        if (list == null) {
            return null;
        }
        return chain(new ArrayList<>(list), 0, args);
    }

    private Object chain(List<WaterfallListener> list, int index, Object[] args) {
        if (index >= list.size()) {
            return null; // 链尾：没有任何监听器改写时返回 null，由调用方决定默认值
        }
        WaterfallListener listener = list.get(index);
        return listener.around(args, () -> chain(list, index + 1, args));
    }

    /** serial：按注册顺序逐个执行，返回最后一个非 null 结果（终止检查点语义）。 */
    public Object serial(String event, Object... args) {
        List<SerialListener> list;
        synchronized (this) {
            list = serials.get(event);
        }
        if (list == null) {
            return null;
        }
        Object result = null;
        for (SerialListener listener : new ArrayList<>(list)) {
            Object current = listener.handle(args);
            if (current != null) {
                result = current;
            }
        }
        return result;
    }

    /** parallel：并行扇出，等待全部完成（demo 中用于 telemetry 这类观察者）。 */
    public void parallel(String event, Object... args) {
        List<Emitter> list;
        synchronized (this) {
            list = emitters.get(event);
        }
        if (list == null) {
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Emitter emitter : new ArrayList<>(list)) {
            futures.add(CompletableFuture.runAsync(() -> emitter.on(args), parallelExecutor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
