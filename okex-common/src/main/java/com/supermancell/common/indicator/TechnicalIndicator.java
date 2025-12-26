package com.supermancell.common.indicator;

import com.supermancell.common.model.Candle;
import com.supermancell.common.model.IndicatorParams;
import com.supermancell.common.model.IndicatorResult;

public interface TechnicalIndicator {
    /**
     * 计算技术指标
     * @param candles 蜡烛图数据列表
     * @param params 计算参数
     * @return 计算结果
     */
    IndicatorResult calculate(java.util.List<Candle> candles, IndicatorParams params);
    
    /**
     * 获取指标名称
     * @return 指标名称
     */
    String getName();
}