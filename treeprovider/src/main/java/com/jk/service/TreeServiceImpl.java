package com.jk.service;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.jk.bean.Position;
import com.jk.bean.TreeBean;
import com.jk.mapper.TreeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class TreeServiceImpl implements TreeService {

    @Autowired
    private TreeMapper treeMapper;

    @Autowired
    private JedisPool jedisPool;

    @Override
    public String delPosition(String positionId) {

        if (StringUtils.isNotEmpty(positionId)){
            treeMapper.delPosition(positionId);
            return "删除成功";
        }else{
            return "删除失败";
        }
    }

    @Override
    public HashMap<String, Object> updatePosition(Position position) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (StringUtils.isNotEmpty(position.getPsoitionId()) && position.getPsoitionId() != null){
            treeMapper.updatePosition(position);
            hashMap.put("status", "修改成功");
            System.out.println("修改成功");
        }else{
            hashMap.put("status", "修改失败数据为空");
            System.out.println("修改失败数据为空");
        }

        return hashMap;
    }

    @Override
    public HashMap<String, Object> addPosition(Position position) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (position != null){
            position.setPsoitionCreatetime(new Date());
            position.setPsoitionId(UUID.randomUUID().toString());
            treeMapper.addPosition(position);
            hashMap.put("status","新增成功");
            System.out.println("新增成功");
        }else{
            hashMap.put("status","失败,数据为空");
            System.out.println("失败,数据为空");
        }
        return hashMap;
    }

    @Override
    public HashMap<String, Object> findPosition(String userId, Integer page, Integer rows) {

        HashMap<String, Object> hashMap = new HashMap<>();
        if (StringUtils.isNotEmpty(userId) && userId != null && userId != ""){
            long count = treeMapper.findPositionByUserId(userId);
            List<Position> positionList = treeMapper.findPosition(userId, (page-1)*rows, rows);
            hashMap.put("rows",positionList);
            hashMap.put("total", count);
        }

        return hashMap;
    }

    @Override
    public List<TreeBean> getTree() {

        Jedis redis = jedisPool.getResource();

        String id = "0";
        String tree = redis.get("Tree");

        List<TreeBean> getTree = null;

        if (StringUtils.isNotEmpty(tree) && tree != null){
            getTree = JSON.parseArray(tree, TreeBean.class);
        }else{
            getTree = getNodes(id);
            tree = JSON.toJSONString(getTree);
            redis.set("Tree", tree);
            redis.expire("Tree",1800);
        }
        redis.close();
        return getTree;
    }

    public List<TreeBean> getNodes(String id){
        List<TreeBean> treeBean = treeMapper.getTree(id);
        for (TreeBean bean : treeBean) {
            List<TreeBean> nodes = getNodes(bean.getId());
            if (nodes.size() > 0 && nodes != null){
                bean.setSelectable(false);
                bean.setLeaf(false);
                bean.setNodes(nodes);
            }else{
                bean.setSelectable(true);
                bean.setLeaf(true);
            }
        }
        return treeBean;
    }
}
