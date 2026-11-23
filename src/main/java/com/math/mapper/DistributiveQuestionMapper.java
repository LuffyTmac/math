package com.math.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.math.entity.DistributiveQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 乘法分配律题目 Mapper
 *
 * 继承 BaseMapper 后可直接使用 insert / updateById / selectList 等方法，
 * 复杂查询使用 LambdaQueryWrapper。
 */
@Mapper
public interface DistributiveQuestionMapper extends BaseMapper<DistributiveQuestion> {
}
