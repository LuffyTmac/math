package com.math.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.math.entity.ShelfAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义书架提交记录 Mapper
 */
@Mapper
public interface ShelfAnswerMapper extends BaseMapper<ShelfAnswer> {
}
