package com.math.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.math.entity.EquationAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义等式提交记录 Mapper
 */
@Mapper
public interface EquationAnswerMapper extends BaseMapper<EquationAnswer> {
}
