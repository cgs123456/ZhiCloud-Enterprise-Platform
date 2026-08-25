package cn.zhicloud.module.aimultiagent.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker Agent 注册中心
 *
 * 设计要点：
 *  1. 所有 {@link AbstractWorkerAgent} 子类在 Spring 容器启动时通过 {@link #register} 注册到此处；
 *  2. {@link #getWorker} 按名称获取 Worker，供编排执行引擎分发任务；
 *  3. {@link #listWorkerNames} 返回所有已注册 Worker 名称，供调试和 API 查询。
 *
 * @author zhicloud
 */
@Component
@Slf4j
public class WorkerAgentRegistry {

    /**
     * Worker 注册表（name → Worker 实例）
     */
    private final Map<String, AbstractWorkerAgent> workerMap = new ConcurrentHashMap<>();

    /**
     * 注册 Worker
     *
     * @param worker Worker 实例
     */
    public void register(AbstractWorkerAgent worker) {
        if (worker == null || worker.getName() == null) {
            log.warn("[register][Worker 或其 name 为空，跳过注册]");
            return;
        }
        workerMap.put(worker.getName(), worker);
        log.info("[register][Worker 注册成功，name={}, class={}]",
                worker.getName(), worker.getClass().getSimpleName());
    }

    /**
     * 按名称获取 Worker
     *
     * @param name Worker 名称
     * @return Worker 实例，不存在则返回 null
     */
    public AbstractWorkerAgent getWorker(String name) {
        if (name == null) {
            return null;
        }
        return workerMap.get(name);
    }

    /**
     * 列出所有已注册 Worker 名称
     *
     * @return Worker 名称列表
     */
    public List<String> listWorkerNames() {
        return new ArrayList<>(workerMap.keySet());
    }

    /**
     * 列出所有已注册 Worker
     *
     * @return Worker 实例列表
     */
    public List<AbstractWorkerAgent> listWorkers() {
        return new ArrayList<>(workerMap.values());
    }

}
