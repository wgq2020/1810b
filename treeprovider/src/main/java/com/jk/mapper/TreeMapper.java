package com.jk.mapper;

import com.jk.bean.Position;
import com.jk.bean.TreeBean;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TreeMapper {

    @Select("select id,text,url as href,pid from t_tree where pid = #{value}")
    List<TreeBean> getTree(String id);

    @Select("select count(position_id) from t_position where user_id = #{value}")
    long findPositionByUserId(String userId);

    @Select("select * from t_position where user_id = #{userId} limit #{page},#{rows}")
    List<Position> findPosition(@Param("userId") String userId,@Param("page") Integer page,@Param("rows") Integer rows);

    @Insert("insert into t_position (position_id,position_name,position_createtime,user_id) values (#{psoitionId},#{psoitionName},#{psoitionCreatetime},#{userId})")
    void addPosition(Position position);

    void updatePosition(Position position);

    @Delete("delete from t_position where position_id = #{value}")
    void delPosition(String positionId);
}
