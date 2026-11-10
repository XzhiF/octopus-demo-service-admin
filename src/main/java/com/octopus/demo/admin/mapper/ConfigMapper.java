package com.octopus.demo.admin.mapper;

import com.octopus.demo.admin.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConfigMapper {
    SysConfig findById(@Param("id") Long id);

    SysConfig findByKey(@Param("configKey") String configKey);

    List<SysConfig> findPage(@Param("key") String key,
                             @Param("offset") long offset,
                             @Param("size") int size);

    long count(@Param("key") String key);

    int insert(SysConfig config);

    int update(SysConfig config);

    int deleteById(@Param("id") Long id);

    /** Global config_key uniqueness check; {@code excludeId} skips the row being updated. */
    int countByKey(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);
}
