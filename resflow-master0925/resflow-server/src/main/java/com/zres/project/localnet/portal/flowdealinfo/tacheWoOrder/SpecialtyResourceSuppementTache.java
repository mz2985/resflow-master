package com.zres.project.localnet.portal.flowdealinfo.tacheWoOrder;

import java.util.HashMap;
import java.util.Map;

import com.zres.project.localnet.portal.resourceInitiate.service.ResSupplementDealServiceIntf;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zres.project.localnet.portal.flowdealinfo.data.dao.OrderDealDao;
import com.zres.project.localnet.portal.flowdealinfo.data.util.BasicCode;
import com.zres.project.localnet.portal.flowdealinfo.service.CommonMethodDealWoOrderServiceInf;
import com.zres.project.localnet.portal.flowdealinfo.service.DealTacheWoOrderIntf;
import com.zres.project.localnet.portal.listener.util.EnmuValueUtil;
import com.zres.project.localnet.portal.resourceInitiate.data.dao.ResourceInitiateDao;
import com.zres.project.localnet.portal.util.OrderTrackOperType;

import com.ztesoft.res.frame.flow.common.dto.FlowWoDTO;
import com.ztesoft.res.frame.flow.common.intf.FlowAction;
import com.ztesoft.res.frame.user.ThreadLocalInfoHolder;

/**
 * 专业资源补录环节--子流程
 */
@Service
public class SpecialtyResourceSuppementTache implements DealTacheWoOrderIntf {

    Logger logger = LoggerFactory.getLogger(SpecialtyResourceSuppementTache.class);

    @Autowired
    private CommonMethodDealWoOrderServiceInf commonMethodDealWoOrderServiceInf;
    @Autowired
    private OrderDealDao orderDealDao;
    @Autowired
    private ResourceInitiateDao resourceInitiateDao;
    @Autowired
    private FlowAction flowAction;
    @Autowired
    private ResSupplementDealServiceIntf resSupplementDealServiceIntf;

    @Override
    public Map<String, Object> tacheDoSomething(Map<String, Object> tacheDoSomeMap) {
        logger.info("--------------------进入专业资源补录环节处理--------------------------");
        String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        String woId = MapUtils.getString(tacheDoSomeMap, "woId");
        String orderId = MapUtils.getString(tacheDoSomeMap, "orderId");
        String tacheId = MapUtils.getString(tacheDoSomeMap, "tacheId");
        String remark = MapUtils.getString(tacheDoSomeMap, "remark");

        // 线条判断参数 operAttrs 获取页面传过来的线条参数
        //Map<String, String> operAttrsValMap = MapUtils.getMap(tacheDoSomeMap, "operAttrsVal");
        String action = "回单";
        String operType = OrderTrackOperType.OPER_TYPE_4;
        // 提交工单前判断是否需要调用汇总归档接口
        Map<String,Object> ret = isInvokeResArchive(tacheDoSomeMap);
        if(!MapUtils.getBoolean(ret,"success",false)){
            // 如果接口失败，不提交工单，卡住流程
            return ret;
        }
        Map<String, Object> complateMap = new HashMap<>();
        //complateMap.put("operAttrsVal", operAttrsValMap);
        complateMap.put("remark",  remark==null?"":remark);
        complateMap.put("operStaffId", operStaffId);
        complateMap.put("orderId", orderId);
        complateMap.put("woId", woId);
        complateMap.put("action", action);
        complateMap.put("operType", operType);
        complateMap.put("tacheId", tacheId);
        Map<String, Object> resultMap = commonMethodDealWoOrderServiceInf.commonComplateWo(complateMap);
        if (MapUtils.getBoolean(resultMap, "success")){
            /**
             * 回单的时候需要做的业务处理
             *    如果是资源补录子流程最后一个专业补录完回单，需要回单主流程的环节
             *      1，查询是否是最后一个专业补录完回单；
             *      2，如果是，查询主流程的等待环节；
             *      3，如果能查到主流程的等待环节状态为已启子流程的工单，则回单该工单；如果没有，有可能有其他专业补录退单了，就不用回单了；
             *    回单完主流程，需要判断整个流程是否结束，如果结束需要归档；-----这个先不做，后面看能不能写在定单结束的监听里面，定单结束掉归档；
             *      1，查询本地资源补录环节是否完成，完成则调归档；没完成不处理，等本地处理完回单的时候调归档；
             */
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("orderId", orderId);
            paramsMap.put("parentOrderCode", BasicCode.RESSUP);
            paramsMap.put("orderState", OrderTrackOperType.ORDER_STATE_2);
            int childOrderNum = resourceInitiateDao.qryIfHasChildOrder(paramsMap);
            if (childOrderNum < 1){
                paramsMap.put("woState", OrderTrackOperType.WO_ORDER_STATE_10);
                paramsMap.put("tacheCode", EnmuValueUtil.ALL_SPECIALTY_RESOURCE_SUPPLEMENT_SEC);
                Map<String, Object> parentOrderWoIdMap = resourceInitiateDao.qryParentOrderWoId(paramsMap);
                String woIdParent = MapUtils.getString(parentOrderWoIdMap, "WO_ID");

                orderDealDao.updateWoStateByWoId(woIdParent, OrderTrackOperType.WO_ORDER_STATE_2);
                FlowWoDTO woDTO = new FlowWoDTO();
                woDTO.setWoId(woIdParent);
                try{
                    flowAction.complateWo(operStaffId, woDTO);
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error("派单失败：", e);
                    resultMap.put("success", false);
                    resultMap.put("message", "派单失败!" + e);
                }
            }
        }
        return resultMap;
    }
    /**
     * 提交工单前判断是否需要调用汇总归档接口
     * 判断 专业补录子流程的所有资源补录环节工单状态，如果只有一个处理中的工单，
     *      并且本地调度资源补录环节已完成，
     *      调用归档
     * @param tacheDoSomeMap
     * @return
     */
    private Map<String,Object> isInvokeResArchive(Map<String, Object> tacheDoSomeMap) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("success",true);
        Map<String, Object> paramsMap = new HashMap<>();
        Map<String, Object> resParams = new HashMap<>();
        String orderId = MapUtils.getString(tacheDoSomeMap, "orderId");
        paramsMap.put("orderId", orderId);
        paramsMap.put("woState", OrderTrackOperType.WO_ORDER_STATE_2);
        paramsMap.put("parentOrderCode", BasicCode.RESSUP);
        // 查询是否存在其他处理中的专业补录子流程
        int childWoOrderNum = resourceInitiateDao.qryIfHasOtherWoOrder(paramsMap);
        if (childWoOrderNum == 1){ //
            paramsMap.put("woState", OrderTrackOperType.WO_ORDER_STATE_10);
            paramsMap.put("tacheCode", EnmuValueUtil.LOCAL_SCHEDULE_RESOURCE_SUPPLEMENT_SEC);
            Map<String, Object> supIdMap = resourceInitiateDao.qrySuppleInfoByChildOrderId(orderId);
            paramsMap.put("id",MapUtils.getString(supIdMap,"ID"));
            int localNum = resourceInitiateDao.qryIfHasSpecialWoOrder(paramsMap);
            if(localNum==0){
                // 查询资源补录表主键id
                resParams.put("id",MapUtils.getString(supIdMap,"ID"));
                ret = resSupplementDealServiceIntf.resArchive(resParams);
            }
        }
        return ret;
    }

}
