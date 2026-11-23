package com.math.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.math.entity.DistributiveAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 乘法分配律学生算式 Mapper
 *
 * 继承 BaseMapper 后可直接使用 insert / selectList / update 等方法，
 * 复杂查询使用 LambdaQueryWrapper。
 */
@Mapper
public interface DistributiveAnswerMapper extends BaseMapper<DistributiveAnswer> {
}
