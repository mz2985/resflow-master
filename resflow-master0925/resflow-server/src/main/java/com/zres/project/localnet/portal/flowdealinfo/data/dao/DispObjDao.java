package com.zres.project.localnet.portal.flowdealinfo.data.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DispObjDao {

    /**
     * 查询派发规则 是否要关联查询动作专业产品属性
     * @param params
     * @return
     */
    public Map<String, Object> qryOrderKeyInfo(Map params);

    /**
     * 查询派发对象
     * @param param
     * @return
     */
    public String qryDispObj(Map<String, Object> param);

    /**
     * 查询派发对象--上游环节以及上游环节对象类型
     * @param param
     * @return
     */
    public Map<String, Object> qryDispObjPreviousOne(Map<String, Object> param);

    /**
     * 用工单规格id查询环节信息
     * @param psId
     * @return
     */
    public Map<String, Object> qryTacheInfo(String psId);

    /**
     * 本地网主调派单服务  10101020
     * @param param
     * @return
     */
    public String mainDispService(Map<String, Object> param);

    /**
     * 本地网数据制作环节派发规则  10101060
     * @param param
     * @return
     */
    public String dataMakeDispService(Map<String, Object> param);

    /**
     * 本地网施工环节派发规则  10101100
     * @param param
     * @return
     */
    public String resourceConstructionDispService(Map<String, Object> param);

    /**
     * 本地网主调派发局内电路联调测试环节  201809051132
     * @param param
     * @return
     */
    public String adjustTestDispService(Map<String, Object> param);

    /**
     * 一干客户、局内省际全程调测派发规则  201809051197
     * @param param
     * @return
     */
    public String fullCommissioningDispService(Map<String, Object> param);

    /**
     * 二干客户、局内电路流程--全程调测派发规则  201809051198
     * @param param
     * @return
     */
    public String provincialCommissioningDispService(Map<String, Object> param);


    /**
     * 查询电路对应环节工单
     * @param qryTacheWoOrderParams
     * @return
     */
    public List<Map<String, Object>> qryCircuitTacheWoOrder(Map<String, Object> qryTacheWoOrderParams);

    /**
     * 查询本地网主调区域
     * @param orderId
     * @return
     */
    public String qryBdwMainOrg(String orderId);

    /**
     * 查询本地网主调区域 --- 二干下发单
     * @param orderId
     * @return
     */
    public String qryBdwMainOrgSec(String orderId);

    /**
     * 根据psId查询环节ID
     * @param psId
     * @return
     */
    String queryTacheIdByPsId(@Param("psId") String psId);

    /**
     * 根据orderId查询key表的专业信息
     * @param orderId
     * @return
     */
    Map<String, Object> queryKeyInfoByOrderId(@Param("orderId") String orderId);

    /**
     * 根据orderId查询parentOrderId
     * @param orderId
     * @return
     */
    String queryParentOrderId(@Param("orderId") String orderId);
}
