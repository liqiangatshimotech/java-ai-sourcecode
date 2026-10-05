package com.example.chapter09.dsh.plugin;

import com.example.chapter09.dsh.event.EventBus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件上下文：对应 dsh 里的 ctx。
 *
 * 它同时扮演三个角色：
 * 1. 服务容器——每个服务占据一个稳定的 key（ctx.tools、ctx.llm、ctx.fs...），
 *    其他插件通过 key 查找服务，而不是 import 具体实现类；
 * 2. 事件总线——插件用 ctx.on(...) 订阅类型化事件，用 emit / waterfall /
 *    serial / parallel 四种模式分发；
 * 3. 副作用账本——每次注册都记录对应的 disposer，按“后注册先撤销”的顺序回滚。
 *
 * dsh 启动时会把 profile 里的 bundle 按“插件树”逐层叠上来；demo 里用
 * load(...) / unloadLast() 模拟挂载和卸载一个分支，用 close() 模拟整个
 * 运行时拆除。
 */
public final class PluginContext implements AutoCloseable {

    /** 当前挂载中的插件帧：帧里登记这个插件带来的所有可逆副作用。 */
    private static final class Frame {
        final String pluginName;
        /** 本插件注册的服务 key，卸载时随帧一起移除。 */
        final Set<String> serviceKeys = new LinkedHashSet<>();
        /** 本插件登记的 disposer，卸载时逆序执行。 */
        final List<Disposer> disposers = new ArrayList<>();

        Frame(String pluginName) {
            this.pluginName = pluginName;
        }
    }

    /** 服务表：key -> 服务实例。对应 Cordis 的 ctx.&lt;key&gt; 寻址。 */
    private final Map<String, Object> services = new LinkedHashMap<>();

    /** 已挂载插件的栈，unloadLast() 只回滚栈顶插件。 */
    private final Deque<Frame> frames = new ArrayDeque<>();

    private final EventBus bus = new EventBus();

    /** 挂载一个插件：所有 apply 期间发生的注册都会记到它的帧上。 */
    public synchronized void load(String pluginName, Plugin plugin) {
        frames.push(new Frame(pluginName));
        plugin.apply(this);
    }

    /** 卸载栈顶插件：逆序执行它的 disposer，摘除它提供的服务。 */
    public synchronized void unloadLast() {
        if (frames.isEmpty()) {
            return;
        }
        Frame frame = frames.pop();
        runFrameDisposers(frame);
    }

    /**
     * 按名字卸载任意插件（不限栈顶）。
     *
     * 真实 Cordis 里任何插件分支都可以单独拆除；demo 里第三幕
     * 换 fs 提供方时就用它卸载夹在中间的 fs-local。
     */
    public synchronized void unload(String pluginName) {
        Frame target = null;
        for (Frame frame : frames) {
            if (frame.pluginName.equals(pluginName)) {
                target = frame;
                break;
            }
        }
        if (target != null) {
            frames.remove(target);
            runFrameDisposers(target);
        }
    }

    private void runFrameDisposers(Frame frame) {
        List<Disposer> disposers = frame.disposers;
        for (int i = disposers.size() - 1; i >= 0; i--) {
            disposers.get(i).dispose();
        }
        for (String key : frame.serviceKeys) {
            services.remove(key);
        }
    }

    /** 拆除整个运行时：从最后挂载的插件开始逐帧回滚。 */
    @Override
    public synchronized void close() {
        while (!frames.isEmpty()) {
            unloadLast();
        }
    }

    /** 登记一次可逆副作用。插件卸载或运行时关闭时自动撤销。 */
    public synchronized void effect(Disposer disposer) {
        if (frames.isEmpty()) {
            throw new IllegalStateException("effect 只能在插件挂载期间注册");
        }
        frames.peek().disposers.add(disposer);
    }

    /**
     * 提供一个服务并占据一个 ctx key。key 被“注册即生效、卸载即消失”地管理，
     * 这是“一切皆插件、没有特权内核”的落地点：换掉 fs 的提供方，
     * 消费方代码完全不用改。
     */
    public synchronized void provideService(String key, Object service) {
        if (services.containsKey(key)) {
            throw new IllegalStateException("服务 key 已被占用: " + key + "（先卸载旧提供方再挂新的）");
        }
        services.put(key, service);
        frames.peek().serviceKeys.add(key);
    }

    /** 按 key 查找服务。对应 Cordis 里插件间通过 ctx.tools 这样的 key 互找。 */
    public synchronized <T> T service(String key, Class<T> type) {
        Object service = services.get(key);
        if (service == null) {
            throw new IllegalStateException("服务未就绪: " + key);
        }
        return type.cast(service);
    }

    /** 当前插件树提供了哪些服务 key（打印插件树时用）。 */
    public synchronized List<String> serviceKeys() {
        return new ArrayList<>(services.keySet());
    }

    /** 从根到现在依次挂载了哪些插件（模拟 dsh --dump-config 看到的插件树）。 */
    public synchronized List<String> pluginTree() {
        List<String> names = new ArrayList<>();
        // ArrayDeque 的迭代是从栈顶到栈根，这里反过来按挂载顺序展示。
        Iterator<Frame> it = frames.descendingIterator();
        while (it.hasNext()) {
            names.add(it.next().pluginName);
        }
        return names;
    }

    /** 订阅事件，并自动注册撤销（卸载插件时自动退订）。 */
    public void on(String event, EventBus.Emitter listener) {
        bus.on(event, listener);
        effect(() -> bus.off(event, listener));
    }

    public void onWaterfall(String event, EventBus.WaterfallListener listener) {
        bus.onWaterfall(event, listener);
        effect(() -> bus.offWaterfall(event, listener));
    }

    public void onSerial(String event, EventBus.SerialListener listener) {
        bus.onSerial(event, listener);
        effect(() -> bus.offSerial(event, listener));
    }

    public EventBus bus() {
        return bus;
    }
}
