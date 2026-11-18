package com.math.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.math.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper（示例）
 *
 * 继承 BaseMapper<SysUser> 后，无需编写 SQL 即可直接使用：
 *   insert / deleteById / updateById / selectById / selectList 等。
 * 复杂查询可在 resources/mapper/ 下编写 XML，或使用 LambdaQueryWrapper。
 *
 * 已通过 @MapperScan("com.math.mapper") 统一扫描，@Mapper 注解可选保留以明确语义。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
