package cn.iocoder.yudao.module.mes.service.pro.aps.strategy;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.ApsAlgorithmEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 遗传算法 APS 排产策略（简化版）
 *
 * <ul>
 *   <li>染色体：工单排列（permutation）</li>
 *   <li>适应度：总延迟时间（小时） + 换线时间（小时），越小越优</li>
 *   <li>选择：锦标赛；交叉：顺序交叉 OX；变异：交换；保留精英</li>
 * </ul>
 * 解码复用 {@link ApsAllocationHelper} 的贪婪分配。
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class GeneticApsStrategy implements ApsStrategy {

    /**
     * 种群规模
     */
    private static final int POP_SIZE = 20;
    /**
     * 进化代数
     */
    private static final int MAX_GENERATIONS = 30;
    /**
     * 变异概率
     */
    private static final double MUTATION_RATE = 0.1;
    /**
     * 单次换线惩罚时长（小时）
     */
    private static final long CHANGEOVER_HOURS = 2L;
    /**
     * 随机种子（保证结果可复现）
     */
    private static final long SEED = 20260730L;

    @Override
    public ApsAlgorithmEnum getAlgorithm() {
        return ApsAlgorithmEnum.GENETIC;
    }

    @Override
    public ApsResult schedule(List<MesProWorkOrderDO> orders, ApsContext context) {
        if (orders.isEmpty() || context.getWorkstations().isEmpty()) {
            return ApsResult.builder().plans(new ArrayList<>()).skippedWorkOrderIds(new java.util.HashSet<>()).build();
        }
        int n = orders.size();
        // 工单编号 -> 工单
        Map<Long, MesProWorkOrderDO> orderMap = new HashMap<>();
        for (MesProWorkOrderDO wo : orders) {
            orderMap.put(wo.getId(), wo);
        }
        // 初始染色体：输入顺序（已是优先级+日期序）
        List<Long> seedChromosome = new ArrayList<>();
        for (MesProWorkOrderDO wo : orders) {
            seedChromosome.add(wo.getId());
        }

        Random random = new Random(SEED);
        // 初始化种群
        List<List<Long>> population = new ArrayList<>();
        population.add(new ArrayList<>(seedChromosome));
        for (int i = 1; i < POP_SIZE; i++) {
            population.add(randomPermutation(seedChromosome, random));
        }

        // 评估并保留精英
        List<Long> best = seedChromosome;
        ApsResult bestResult = decode(best, orderMap, context);
        double bestFitness = fitness(bestResult, orderMap);

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            List<List<Long>> nextGen = new ArrayList<>();
            // 精英保留
            nextGen.add(new ArrayList<>(best));
            // 生成下一代
            while (nextGen.size() < POP_SIZE) {
                List<Long> parent1 = tournament(population, orderMap, context, random);
                List<Long> parent2 = tournament(population, orderMap, context, random);
                List<Long> child = orderCrossover(parent1, parent2, random);
                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(child, random);
                }
                nextGen.add(child);
            }
            population = nextGen;
            // 评估当代最优
            for (List<Long> chromosome : population) {
                ApsResult result = decode(chromosome, orderMap, context);
                double fit = fitness(result, orderMap);
                if (fit < bestFitness) {
                    bestFitness = fit;
                    best = new ArrayList<>(chromosome);
                    bestResult = result;
                }
            }
        }
        log.info("[schedule][遗传算法进化完成，代数={}, 最优适应度={}]", MAX_GENERATIONS, bestFitness);
        return bestResult;
    }

    // ==================== 解码与适应度 ====================

    /**
     * 将染色体（工单编号排列）解码为排产结果
     */
    private ApsResult decode(List<Long> chromosome, Map<Long, MesProWorkOrderDO> orderMap, ApsContext context) {
        List<MesProWorkOrderDO> ordered = new ArrayList<>();
        for (Long id : chromosome) {
            MesProWorkOrderDO wo = orderMap.get(id);
            if (wo != null) {
                ordered.add(wo);
            }
        }
        return ApsAllocationHelper.allocate(ordered, context);
    }

    /**
     * 适应度：总延迟时间 + 换线时间，越小越优
     */
    private double fitness(ApsResult result, Map<Long, MesProWorkOrderDO> orderMap) {
        if (result.getPlans() == null || result.getPlans().isEmpty()) {
            return Double.MAX_VALUE;
        }
        double tardiness = 0.0;
        // 总延迟：计划结束时间晚于工单需求日期的小时数
        for (MesProApsPlanDO plan : result.getPlans()) {
            MesProWorkOrderDO wo = orderMap.get(plan.getWorkOrderId());
            if (wo == null || wo.getRequestDate() == null || plan.getPlannedEndTime() == null) {
                continue;
            }
            long hours = Duration.between(wo.getRequestDate(), plan.getPlannedEndTime()).toHours();
            if (hours > 0) {
                tardiness += hours;
            }
        }
        // 换线惩罚：同一工位相邻计划产品不同则计一次换线
        double changeover = computeChangeover(result);
        return tardiness + changeover;
    }

    /**
     * 计算换线时间：按工位分组，相邻计划产品不同则累加惩罚
     */
    private double computeChangeover(ApsResult result) {
        Map<Long, List<MesProApsPlanDO>> byWorkstation = new HashMap<>();
        for (MesProApsPlanDO plan : result.getPlans()) {
            byWorkstation.computeIfAbsent(plan.getWorkstationId(), k -> new ArrayList<>()).add(plan);
        }
        double total = 0.0;
        for (List<MesProApsPlanDO> plans : byWorkstation.values()) {
            plans.sort((a, b) -> a.getPlannedStartTime().compareTo(b.getPlannedStartTime()));
            for (int i = 1; i < plans.size(); i++) {
                Long prevProduct = plans.get(i - 1).getProductId();
                Long currProduct = plans.get(i).getProductId();
                if (prevProduct != null && !prevProduct.equals(currProduct)) {
                    total += CHANGEOVER_HOURS;
                }
            }
        }
        return total;
    }

    // ==================== 遗传操作 ====================

    /**
     * 锦标赛选择：从种群随机取 2 个，返回适应度更优的
     */
    private List<Long> tournament(List<List<Long>> population, Map<Long, MesProWorkOrderDO> orderMap,
                                  ApsContext context, Random random) {
        List<Long> c1 = population.get(random.nextInt(population.size()));
        List<Long> c2 = population.get(random.nextInt(population.size()));
        double f1 = fitness(decode(c1, orderMap, context), orderMap);
        double f2 = fitness(decode(c2, orderMap, context), orderMap);
        return (f1 <= f2) ? new ArrayList<>(c1) : new ArrayList<>(c2);
    }

    /**
     * 顺序交叉（OX）：保留父代1的一段，其余按父代2的顺序填充
     */
    private List<Long> orderCrossover(List<Long> p1, List<Long> p2, Random random) {
        int n = p1.size();
        List<Long> child = new ArrayList<>(Collections.nCopies(n, null));
        int start = random.nextInt(n);
        int end = start + random.nextInt(n - start + 1);
        for (int i = start; i <= end && i < n; i++) {
            child.set(i, p1.get(i));
        }
        // 按 p2 顺序填充剩余位置
        int idx = (end + 1) % n;
        for (Long gene : p2) {
            if (!child.contains(gene)) {
                child.set(idx, gene);
                idx = (idx + 1) % n;
            }
        }
        return child;
    }

    /**
     * 变异：随机交换两个位置
     */
    private void mutate(List<Long> chromosome, Random random) {
        int i = random.nextInt(chromosome.size());
        int j = random.nextInt(chromosome.size());
        Collections.swap(chromosome, i, j);
    }

    /**
     * 生成随机排列
     */
    private List<Long> randomPermutation(List<Long> source, Random random) {
        List<Long> perm = new ArrayList<>(source);
        Collections.shuffle(perm, random);
        return perm;
    }

}