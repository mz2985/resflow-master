package com.zres.project.localnet.portal.localStanbdyInfo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zres.project.localnet.portal.flowdealinfo.data.dao.CheckFeedbackDao;
import com.zres.project.localnet.portal.flowdealinfo.data.dao.OrderDealDao;
import com.zres.project.localnet.portal.flowdealinfo.data.dao.OrderDetailsDao;
import com.zres.project.localnet.portal.flowdealinfo.data.dao.OrderQrySecondaryDao;
import com.zres.project.localnet.portal.flowdealinfo.data.util.BasicCode;
import com.zres.project.localnet.portal.flowdealinfo.service.CheckFeedbackService;
import com.zres.project.localnet.portal.flowdealinfo.service.OrderDetailsSevice;
import com.zres.project.localnet.portal.local.domain.BaseObject;
import com.zres.project.localnet.portal.local.domain.PageInfo;
import com.zres.project.localnet.portal.localStanbdyInfo.data.dao.OrderStandbyDao;
import com.zres.project.localnet.portal.localStandbyInfo.service.OrderStandbyServiceIntf;
import com.zres.project.localnet.portal.util.FtpUtils;
import com.zres.project.localnet.portal.util.UpdateToJiKe;
import com.ztesoft.res.frame.core.util.ListUtil;
import com.ztesoft.res.frame.core.util.MapUtil;
import com.ztesoft.res.frame.user.ThreadLocalInfoHolder;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;


/**
 * @author :wang.g2
 * @description :
 * @date : 2018/12/21
 */
@Service
public class OrderStandbyService implements OrderStandbyServiceIntf
{

    Logger logger = LoggerFactory.getLogger(OrderStandbyService.class);

    @Value("${ftp.address}")
    private String address;
    @Value("${ftp.port}")
    private String port;
    @Value("${ftp.username}")
    private String username;
    @Value("${ftp.password}")
    private String password;

    @Autowired
    private OrderStandbyDao orderStandbyDao;
    @Autowired
    private CheckFeedbackService checkFeedbackService;
    @Autowired
    private OrderQrySecondaryDao orderQrySecondaryDao;
    @Autowired
    private OrderDealDao orderDealDao;
    @Autowired
    private OrderStandbyServiceIntf orderStandbyServiceIntf;
    @Autowired
    private UpdateToJiKe updateToJiKe;
    @Autowired
    private OrderDetailsDao orderDetailsDao;
    @Autowired
    private CheckFeedbackDao checkFeedbackDao;
    @Autowired
    private OrderDetailsSevice orderDetailsSevice;

    private List<String> PRODUCT_NOT_SAVE =Arrays.asList("10000001","10000002","10000011");
    @Override
    public Map<String, Object> qryCstOrdList(Map<String, Object> params) {
        List<Map<String, Object>> mapListT = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        params = getTeacheId(params);
        String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        params.put("ycStaffId", operStaffId);
        PageInfo pageInfo = new PageInfo(); //分页信息
        String queryTypeLocal = MapUtils.getString(params, "queryTypeLocal");
        pageInfo.setIndexSizeData(params.get("pageIndex"), params.get("pageSize"));
        params.put("startRow", pageInfo.getRowStart()); //分页开始行
        params.put("endRow", pageInfo.getRowEnd()); //分页结束行
        int rowCount = 0;
        if ("abnormalOrder".equals(queryTypeLocal)) {
            rowCount = orderStandbyDao.qryAbnormalOrderCount(params);
            if (rowCount > 0) {
                List<Map<String, Object>> mapList = orderStandbyDao.qryAbnormalOrder(params);
                if (!CollectionUtils.isEmpty(mapList)) {
                    mapListT.addAll(mapList);
                }
            }
        }else if("dispConfirm".equals(queryTypeLocal)){
            logger.debug(" 查询类型：" + queryTypeLocal + ", start.........");
            rowCount = orderStandbyDao.queryStandbyOrderCompleteCount(params);
            logger.debug(" 查询类型：" + queryTypeLocal + ", end.........");
            if (rowCount != 0) {
                List<Map<String, Object>> mapList = orderStandbyDao.qryCstOrdCompleteList(params);
                if (!CollectionUtils.isEmpty(mapList)) {
                    mapListT.addAll(mapList);
                }
            }
        }else if ("ccOrder".equals(queryTypeLocal)){ //抄送
            logger.debug(" 查询类型：" + queryTypeLocal + ", start.........");
            rowCount = orderStandbyDao.queryStandbyCcOrderCount(params);
            logger.debug(" 查询类型：" + queryTypeLocal + ", end.........");
            if (rowCount != 0) {
                List<Map<String, Object>> mapList = orderStandbyDao.qryCstOrdCcList(params);
                if (!CollectionUtils.isEmpty(mapList)) {
                    mapListT.addAll(mapList);
                }
            }
        }else if ("applyOrder".equals(queryTypeLocal)){ //延期申请
            logger.debug(" 查询类型：" + queryTypeLocal + ", start.........");
            rowCount = orderStandbyDao.queryPostponementOrderCount(params);
            logger.debug(" 查询类型：" + queryTypeLocal + ", end.........");
            if (rowCount != 0) {
                List<Map<String, Object>> mapList = orderStandbyDao.queryPostponementOrderInfo(params);
                if (!CollectionUtils.isEmpty(mapList)) {
                    mapListT.addAll(mapList);
                }
            }
        }        else {
            logger.debug(" 查询类型：" + queryTypeLocal + ", start.........");
            rowCount = orderStandbyDao.queryStandbyOrderCount(params);
            logger.debug(" 查询类型：" + queryTypeLocal + ", end.........");
            if (rowCount != 0) {
                List<Map<String, Object>> mapList = orderStandbyDao.qryCstOrdList(params);
                if (!CollectionUtils.isEmpty(mapList)) {
                    mapListT.addAll(mapList);
                }
            }
        }
        pageInfo.setDataCount(rowCount);
        map.put("dataLength", rowCount);
        map.put("data", mapListT);
        map.put("flag", "1");
        map.put("pageIndex", pageInfo.getCurrentPage());
        map.put("rowNum", pageInfo.getPageSize());
        map.put("total", pageInfo.getPageCount());
        return map;
    }

    /**
     * 查询业务电路单 核查流程时（特殊：核查调度不选专业）
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> qrySrvOrdListCheck(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
      //  String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        String woIds = MapUtil.getString(params,"woIds");
        Map<String, Object> orderResource = orderQrySecondaryDao.qrySrvOrderBelongSys(params);
        String system_resource = MapUtil.getString(orderResource, "SYSTEM_RESOURCE");
        List<Map<String, Object>> list = new ArrayList<>();
        if("second-schedule-lt".equals(system_resource)){ //二干来单
            list = orderStandbyDao.qrySecondSrvOrdListCheck(params);
        }else{
            // 核查多工单改造后，重新修改查询sql
            list = orderStandbyDao.qrySrvOrdListCheckNew(params);
            if (!(list!=null && list.size() > 0)) {
                list = orderStandbyDao.qrySrvOrdListCheck(params);
            }
        }
        //modify by wang.gang2
        // 如果是核查汇总环节，那么需要先保存核查反馈信息
        if (!CollectionUtils.isEmpty(list) && BasicCode.CHECK_TOTAL.equals(MapUtils.getString(params, "tacheId")) && BasicCode.SECOND.equals(system_resource)) {
            for (Map<String, Object> temp : list) {
                // 数字电路以太网专线互联网需要重新填写核查标准化反馈信息, 不保存
                Boolean notSaveInfo = PRODUCT_NOT_SAVE.contains(MapUtil.getString(temp,"SERVICE_ID"));
                if (notSaveInfo) {
                    continue;
                }
                //  params :woId,tacheId,srvOrdId
                String regionId = MapUtil.getString(temp,"REGION_ID");
                String orgName ="";
                //  Map<String, Object> queryInfoByWoId = checkFeedbackDao.queryInfoByWoId(MapUtils.getString(temp, "WO_ID"));
           /* if("second-schedule-lt".equals(system_resource)){ //说明是二干来单
                 orgName = orderDealDao.getOrgNameByOrgId(regionId)+": \n";
            }*/
                Map<String, Object> checkInfo = new HashMap<>();
                params.put("woId", MapUtils.getString(temp, "WO_ID"));
                if("second-schedule-lt".equals(system_resource)){ //二干来单
                    params.put("srvOrdId", MapUtils.getString(temp, "RELATE_INFO_ID"));
                }else{
                    params.put("srvOrdId", MapUtils.getString(temp, "SRV_ORD_ID"));
                }
                Map<String, Object> retMap = checkFeedbackService.queryCheckInfo(params);
                if("second-schedule-lt".equals(system_resource)){ //二干来单
                    retMap.remove("A_CONSTRUCT_SCHEME"); //二干只有Z端
                    retMap.remove("A_RES_SATISFY");
                    retMap.remove("A_CONSTRUCT_PERIOD");
                    retMap.remove("A_INVESTMENT_AMOUNT");
                }
                if (MapUtils.getBoolean(retMap, "success")) {
//                        Object tempObj = org.apache.commons.collections.MapUtils.getObject(params, "data");
//                        Map<String, Object> formValue = JSON.parseObject(tempObj.toString(), Map.class);
                    Map<String, Object> formValue = (Map<String, Object>) MapUtils.getObject(retMap, "data");
                    Map<String, Object> formValueSecond = new HashMap<>();
                    if(formValue!=null){
                        formValueSecond.putAll(formValue);
                    }
                    formValueSecond.put("Z_CONSTRUCT_SCHEME",orgName+MapUtil.getString(formValueSecond,"Z_CONSTRUCT_SCHEME"));

                    checkInfo.put("formValue", formValueSecond);
                    checkInfo.put("woId", MapUtils.getString(temp, "WO_ID"));
                    if("second-schedule-lt".equals(system_resource)){ //二干来单
                        checkInfo.put("srvOrdId", MapUtils.getString(temp, "RELATE_INFO_ID"));
                    }else{
                        checkInfo.put("srvOrdId", MapUtils.getString(temp, "SRV_ORD_ID"));
                    }
                    checkInfo.put("tacheId", MapUtils.getString(params, "tacheId"));
                    checkInfo.put("areaZ", MapUtils.getString(temp, "ZREGIONNAME"));
                    checkInfo.put("areaA", MapUtils.getString(temp, "AREGIONNAME"));
                    checkInfo.put("areaId", MapUtils.getString(temp, "WOAREA"));
                    checkFeedbackService.updateCheckInfo(checkInfo);
                }
                // 最后把核查信息状态改成已保存【1代表已保存】
                temp.put("CHECK_STATE", "已保存");
            }
        }
        map.put("data", list);
        map.put("flag", "1");
        return map;
    }

    @Override
    public Map<String, Object> qrySrvOrdList(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
        //String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        Map<String, Object> mapParam = new HashMap<String, Object>();
        mapParam.put("cstOrdId", MapUtil.getString(params,"cstOrdId"));
        mapParam.put("woState", MapUtil.getString(params,"woState"));
        mapParam.put("tacheId", MapUtil.getString(params,"tacheId"));
        mapParam.put("dealUserId", MapUtil.getString(params,"dealUserId"));
        mapParam.put("dispObjTyeValue", MapUtil.getString(params,"dispObjTyeValue"));
        mapParam.put("dispObjTye", MapUtil.getString(params,"dispObjTye"));
        mapParam.put("btnFlag", MapUtil.getString(params,"btnFlag"));
        mapParam.put("applyState", MapUtil.getString(params,"applyState"));
        String woIds = MapUtil.getString(params, "woIds");
        if(StringUtils.hasText(woIds)
                &&!"undefined".equals(woIds)){
            String[] woSplit = woIds.split(",");
            mapParam.put("woIds",woSplit);
        }else{
            mapParam.put("woIds","");
        }
        //   params.put("dealUserId",operStaffId);
        Map<String, Object> orderBelongSys = orderQrySecondaryDao.qrySrvOrderBelongSys(mapParam);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (BasicCode.LOCAL.equals(MapUtils.getString(orderBelongSys, "SYSTEM_RESOURCE"))){
            list = orderStandbyDao.qrySrvOrdList(mapParam);
        }else if(BasicCode.SECOND.equals(MapUtils.getString(orderBelongSys, "SYSTEM_RESOURCE"))){
            list = orderStandbyDao.qrySrvOrdListSecond(mapParam);
        }
        if(params.keySet().contains("btnFlag")){
            if("resConfig".equals(MapUtils.getString(params,"btnFlag"))){
                Set<String> configTacheSet = new HashSet<>();
                configTacheSet.add("500001155"); // 光纤资源分配
                configTacheSet.add("500001157"); // 资源分配
                if(configTacheSet.contains(MapUtils.getString(params,"tacheId"))){
                    if(list!=null && list.size()>0){
                        for(int i=0;i<list.size();i++){
                            Map<String, Object> temp  =  list.get(i);
                            String orderId = MapUtils.getString(temp,"ORDER_ID");
                            // 根据主流程orderId查询所有子流程，如果没有专业需要进行资源配置，那么页面不展示这条电路
                            int num = orderStandbyDao.qryResConfigOrderNum(orderId,"parent");
                            if ( num < 1 ){
                                list.remove(temp);
                                --i;
                            }
                        }
                    }
                }
                if(params.keySet().contains("type") && (!ListUtil.isEmpty(list))){
                    for(int i=0;i<list.size();i++){
                        Map<String, Object> temp  =  list.get(i);
                        Map<String,Object> qryParamMap = new HashMap<>();
                        qryParamMap.put("orderId",MapUtils.getString(temp,"WO_ID",""));
                        if (BasicCode.LOCAL.equals(MapUtils.getString(orderBelongSys, "SYSTEM_RESOURCE"))){
                            qryParamMap.put("srvOrdId",MapUtils.getString(temp,"SRV_ORD_ID",""));
                        }else if(BasicCode.SECOND.equals(MapUtils.getString(orderBelongSys, "SYSTEM_RESOURCE"))){
                            qryParamMap.put("srvOrdId",MapUtils.getString(temp,"RELATE_INFO_ID",""));
                        }
                        // 判断电路是否已经进行了资源分配
                        int isRes = orderStandbyDao.qryIsResConfigNum(qryParamMap);
                        // 未配置:0;已配置：1
                        if("0".equals(MapUtils.getString(params,"type","")) && isRes > 0){
                            list.remove(temp);
                            --i;
                        } else if("1".equals(MapUtils.getString(params,"type","")) && isRes < 1){
                            list.remove(temp);
                            --i;
                        }
                    }
                }
            }
        }
        Map<String, Object> ifHasMap = new HashMap<>();
        // 默认值1，代表电路调度配置的专业区域信息，如果不传值就是默认
        String newCreateResource = "1";
        if(params.keySet().contains("newCreateResource")){
            // 0代表电路调度环节保存的新建资源录入派发区域信息
            if("0".equals(MapUtils.getString(params,"newCreateResource"))){
                newCreateResource = MapUtils.getString(params,"newCreateResource");
            }
        }
        ifHasMap.put("newCreateResource", newCreateResource);
        for(Map<String, Object> srvOrder : list){
            ifHasMap.put("orderId", MapUtils.getString(srvOrder, "ORDER_ID"));
            ifHasMap.put("cstOrdId", MapUtils.getString(srvOrder, "CST_ORD_ID"));
            ifHasMap.put("srvOrdId", MapUtils.getString(srvOrder, "SRV_ORD_ID"));
            Map<String, Object> resMap = orderDealDao.queryConfigInfoBySrvOrdIdRes(ifHasMap);
            if(MapUtils.isEmpty(resMap)){
                if("已配置".equals(MapUtils.getString(srvOrder, "STATE"))){
                    srvOrder.put("STATE", "未配置");
                }
            } else{
                if("未配置".equals(MapUtils.getString(srvOrder, "STATE"))){
                    srvOrder.put("STATE", "已配置");
                }
            }
        }
        map.put("data", list);
        map.put("flag", "1");
        return map;
    }

    @Override
    public Map<String, Object> qrySrvOrdChildList(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
        String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        params.put("dealUserId",operStaffId);
        List<Map<String, Object>> list = orderStandbyDao.qrySrvOrdChildList(params);
        if(params.keySet().contains("btnFlag")){
            if("resConfig".equals(MapUtils.getString(params,"btnFlag"))){
                if(params.keySet().contains("type") && (!ListUtil.isEmpty(list))){
                    for(int i=0;i<list.size();i++){
                        Map<String, Object> temp  =  list.get(i);
                        Map<String,Object> qryParamMap = new HashMap<>();
                        qryParamMap.put("orderId",MapUtils.getString(temp,"ORDER_ID",""));
                        if("".equals(MapUtils.getString(temp,"RELATE_INFO_ID",""))){
                            qryParamMap.put("srvOrdId",MapUtils.getString(temp,"SRV_ORD_ID",""));
                        }else {
                            qryParamMap.put("srvOrdId",MapUtils.getString(temp,"RELATE_INFO_ID",""));
                        }
                        // 判断电路是否已经进行了资源分配
                        int isRes = orderStandbyDao.qryIsResConfigNum(qryParamMap);
                        // 未配置:0;已配置：1
                        if("0".equals(MapUtils.getString(params,"type","")) && isRes > 0){
                            list.remove(temp);
                            --i;
                        } else if("1".equals(MapUtils.getString(params,"type","")) && isRes < 1){
                            list.remove(temp);
                            --i;
                        }
                    }
                }
            }
        }
        map.put("data", list);
        map.put("flag", "1");
        return map;
    }

    /*@Override
    public Map<String, Object> qryChildFlowList(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
        String operStaffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        params.put("dealUserId",operStaffId);
        List<Map<String, Object>> list = orderStandbyDao.qryChildFlowList(params);
        if(params.keySet().contains("currentTacheId")){
            Set<String> configTacheSet = new HashSet<>();
            configTacheSet.add("500001155"); // 光纤资源分配
            configTacheSet.add("500001157"); // 资源分配
            if(configTacheSet.contains(MapUtils.getString(params,"currentTacheId"))){
                if(list!=null && list.size()>0){
                    // 根据子流程orderId查询专业，如果不需要进行资源配置，那么页面不展示这个子流程工单
                    for(int i=0;i<list.size();i++){
                        Map<String, Object> temp  =  list.get(i);
                        String orderId = MapUtils.getString(temp,"ORDER_ID");
                        int num = orderStandbyDao.qryResConfigOrderNum(orderId,"child");
                        if(num < 1){
                            list.remove(temp);
                            --i;
                        }
                    }
                }
            }
        }
        map.put("data", list);
        map.put("flag", "1");
        return map;
    }*/

    @Override
    public Map<String, Object> queryOrderInfo(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("data", orderStandbyDao.queryOrderInfo(params));
        map.put("flag", "1");
        return map;
    }
    @Override
    public Map<String, Object> querySubOrderInfoColl(Map<String, Object> params) {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> mapParam = new HashMap<String, Object>();
        String srvOrdIds = org.apache.commons.collections.MapUtils.getString(params, "srvOrdIds");
        String cstOrdId = org.apache.commons.collections.MapUtils.getString(params, "cstOrdId");
        String tacheId = org.apache.commons.collections.MapUtils.getString(params, "tacheId");
        String woId = org.apache.commons.collections.MapUtils.getString(params, "woId");
        String regionId = org.apache.commons.collections.MapUtils.getString(params, "regionId");
        String specialtyCode = org.apache.commons.collections.MapUtils.getString(params, "specialtyCode");
        String queryTypeLocal = org.apache.commons.collections.MapUtils.getString(params, "queryTypeLocal");
        String resourceType = org.apache.commons.collections.MapUtils.getString(params, "resourceType");
        String dispObjTyeValue = org.apache.commons.collections.MapUtils.getString(params, "dispObjTyeValue");
        String dispObjTye = org.apache.commons.collections.MapUtils.getString(params, "dispObjTye");

        String compUserId = org.apache.commons.collections.MapUtils.getString(params, "compUserId");
        String woState = org.apache.commons.collections.MapUtils.getString(params, "woState");
        String dispType = org.apache.commons.collections.MapUtils.getString(params, "dispType");
        String staffId = org.apache.commons.collections.MapUtils.getString(params, "staffId");
        String dealUserId = org.apache.commons.collections.MapUtils.getString(params, "dealUserId");

        if(StringUtils.hasText(srvOrdIds)){
                if(StringUtils.hasText(srvOrdIds)){
                String[] splitStr = srvOrdIds.split(",");
                mapParam.put("srvOrdId",splitStr);
            }
        }
        if(StringUtils.hasText(tacheId)){
                tacheId = tacheId.replace("[", "");
                tacheId = tacheId.replace("]", "");
                tacheId = tacheId.replaceAll("\"", "");
                if(StringUtils.hasText(tacheId)){
                String[] splitStr = tacheId.split(",");
                mapParam.put("tacheId",splitStr);
            }
        }
        if(StringUtils.hasText(woId)){
            woId = woId.replace("[", "");
            woId = woId.replace("]", "");
            woId = woId.replaceAll("\"", "");
            if(StringUtils.hasText(woId)){
                String[] splitStr = woId.split(",");
                mapParam.put("woId",splitStr);
            }
        }
        if(StringUtils.hasText(regionId)){
                regionId = regionId.replace("[", "");
                regionId = regionId.replace("]", "");
                regionId = regionId.replaceAll("\"", "");
                if(StringUtils.hasText(regionId)){
                    String[] splitStr = regionId.split(",");
                    mapParam.put("regionId",splitStr);
            }
        }
        if(StringUtils.hasText(cstOrdId)){
                cstOrdId = cstOrdId.replace("[", "");
                cstOrdId = cstOrdId.replace("]", "");
                cstOrdId = cstOrdId.replaceAll("\"", "");
                if(StringUtils.hasText(cstOrdId)){
                String[] splitStr = cstOrdId.split(",");
                mapParam.put("cstOrdId",splitStr);
            }
        }
        if(StringUtils.hasText(dispObjTyeValue)){
            dispObjTyeValue = dispObjTyeValue.replace("[", "");
            dispObjTyeValue = dispObjTyeValue.replace("]", "");
            dispObjTyeValue = dispObjTyeValue.replaceAll("\"", "");
            if(StringUtils.hasText(dispObjTyeValue)){
                String[] splitStr = dispObjTyeValue.split(",");
                String[] splitStr1 = new String[splitStr.length];
                for (int i = 0; i < splitStr.length; i++) {
                    if(!"personalValue".equals(splitStr[i])){
                        splitStr1[i]=splitStr[i];
                    }
                }
                mapParam.put("dispObjTyeValue",splitStr1);
            }
        }
       /* if(StringUtils.hasText(dispType)){
            dispType = dispType.replace("[", "");
            dispType = dispType.replace("]", "");
            dispType = dispType.replaceAll("\"", "");
                if(StringUtils.hasText(dispType)){
                    String[] splitStr = dispType.split(",");
                    mapParam.put("dispType",splitStr);
            }
        }
        if(StringUtils.hasText(dispObjTye)){
                dispObjTye = dispObjTye.replace("[", "");
                dispObjTye = dispObjTye.replace("]", "");
                dispObjTye = dispObjTye.replaceAll("\"", "");
                if(StringUtils.hasText(dispObjTye)){
                String[] splitStr = dispObjTye.split(",");
                mapParam.put("dispObjTye",splitStr);
            }
        }*/


//        mapParam.put("cstOrdId",cstOrdId);
//        mapParam.put("tacheId",tacheId);
//        mapParam.put("regionId",regionId);
//        mapParam.put("specialtyCode",specialtyCode);
//        mapParam.put("dispObjTye",dispObjTye);
//        mapParam.put("dispType",dispType);
        mapParam.put("queryTypeLocal",queryTypeLocal);
        mapParam.put("resourceType",resourceType);
//        mapParam.put("dispObjTyeValue","personalValue".equals(dispObjTyeValue)?"":dispObjTyeValue);

        mapParam.put("compUserId",compUserId);
        mapParam.put("woState",woState);
        mapParam.put("staffId",staffId);
        mapParam.put("dealUserId",dealUserId);

        List<Map<String, Object>> mapsSubEmpty = new ArrayList<Map<String, Object>>();
        Map<String, Object> mapsSubH = new HashMap<String, Object>();
        List<Map<String, Object>> mapsSub = orderStandbyDao.querySubOrderInfoColl(mapParam);
        if("290000004".equals(woState)){
            for(int i = 0; i<mapsSub.size(); i++){
                Map<String, Object> subMap = mapsSub.get(i);
                String order_id = MapUtil.getString(subMap, "ORDER_ID");
                String service_id = MapUtil.getString(subMap, "SERVICE_ID");
                String srvOrdId = MapUtil.getString(subMap, "SRV_ORD_ID");
                String active_type = MapUtil.getString(subMap, "ACTIVE_TYPE");
                String tache_id = MapUtil.getString(subMap, "TACHE_ID");
                String REGION_ID = MapUtil.getString(subMap, "REGION_ID");
                String woKey = order_id+"_"+service_id+"_"+srvOrdId+"_"+active_type+"_"+tache_id+"_"+REGION_ID;
                if(mapsSubH.get(woKey)!=null){
                    continue;
                }else{
                    mapsSubH.put(woKey,woKey);
                    mapsSubEmpty.add(subMap);
                }
            }
        }else{
            mapsSubEmpty.addAll(mapsSub);
        }
        map.put("data", mapsSubEmpty);
        map.put("flag", "1");
        return map;
    }

    /**
     * 待办中查询除自己以外其他tab页的计数。是否有必要去除自己？
     * @param params
     * @return
     */
    /*@Override
    public Map<String, Object> queryStandbyOrderCount(Map<String, Object> params) {
        Map<String, Object> daoMap = new HashMap<String, Object>();
        params = getTeacheId(params);
        String queryTypeLocal = (String) params.get("queryTypeLocal");
        String staffId = (String) params.get("staffId");
        if (StringUtils.isEmpty(staffId)) {
            staffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        }

        //部门待办
        params.put("staffId", staffId);
        params.put("dealUserId", "");
        params.put("compUserId", "");
        params.put("dispType", "260000001");
        params.put("woState", "290000002");
        params.put("queryTypeLocal","deptStandny");
        queryStandbyOrderCount("deptStandny", queryTypeLocal, params, daoMap);

        //岗位待办
        params.put("staffId", staffId);
        params.put("dealUserId", "");
        params.put("compUserId", "");
        params.put("dispType", "260000002");
        params.put("woState", "290000002");
        params.put("queryTypeLocal","jobStandby");
        queryStandbyOrderCount("jobStandby", queryTypeLocal, params, daoMap);

        //个人待办
        params.put("staffId", staffId);
        params.put("dispType", "260000003");
        params.put("woState", "290000002"); //处理中
        params.put("dealUserId", "");
        params.put("compUserId", "");
        params.put("queryTypeLocal","staffStandby");
        queryStandbyOrderCount("staffStandby", queryTypeLocal, params, daoMap);

        //处理中 dealOrder
        params.put("dealUserId", staffId);
        params.put("compUserId", "");
        params.put("staffId", "");
        params.put("dispType", "260000003");
        params.put("woState", "290000002");
        params.put("queryTypeLocal","dealOrder");
        queryStandbyOrderCount("dealOrder", queryTypeLocal, params, daoMap);

        //确认完成单
        params.put("compUserId", staffId);
        params.put("dealUserId", "");
        params.put("staffId", "");
        params.put("dispType", "260000004");
        params.put("woState", "290000004"); //处理中
        params.put("queryTypeLocal","dispConfirm");//已完成
        queryStandbyOrderCount("dispConfirm", queryTypeLocal, params, daoMap);

        //异常通知
        params.put("compUserId", "");
        params.put("dealUserId", staffId);
        params.put("ycStaffId", staffId);
        params.put("staffId", "");
        params.put("dispType", "260000003");
        params.put("woState", "290000002");
        params.put("queryTypeLocal","exceptionOrder");
        queryStandbyOrderCount("exceptionOrder", queryTypeLocal, params, daoMap);

        //异常单
        params.put("compUserId", "");
        params.put("dealUserId", staffId);
        params.put("staffId", staffId);
        params.put("dispType", "260000003");
        params.put("woState", "290000002"); //处理中
        params.put("queryTypeLocal","abnormalOrder");//已完成
        queryStandbyOrderCount("abnormalOrder", queryTypeLocal, params, daoMap);

        params.put("compUserId", "");
        params.put("dealUserId", "");
        params.put("staffId", staffId);
        params.put("dispType", "260000005");
        params.put("woState", ""); //处理中
        params.put("queryTypeLocal","ccOrder");//抄送
        queryStandbyOrderCount("ccOrder", queryTypeLocal, params, daoMap);
        return daoMap;
    }*/

    @Override
    public Map<String, Object> queryStandbyOrderEachCount(Map<String, Object> params) {
        Map<String, Object> daoMap = new HashMap<String, Object>();
        String  staffId = ThreadLocalInfoHolder.getLoginUser().getUserId();
        daoMap.put("messages","success");
        params = getTeacheId(params);
        try {
                    params.put("staffId", staffId);
                    params.put("dealUserId", "");
                    params.put("compUserId", "");
                    params.put("dispType", "260000001");
                    params.put("woState", "290000002");
                    //params.put("queryTypeLocal","deptStandny");
                    daoMap.put("deptStandny", orderStandbyDao.queryStandbyOrderCount(params));

                    params.put("staffId", staffId);
                    params.put("dealUserId", "");
                    params.put("compUserId", "");
                    params.put("dispType", "260000002");
                    params.put("woState", "290000002");
                    params.put("queryTypeLocal","jobStandby");
                    daoMap.put("jobStandby", orderStandbyDao.queryStandbyOrderCount(params));
                    //个人待办
                    params.put("staffId", staffId);
                    params.put("dispType", "260000003");
                    params.put("woState", "290000002"); //处理中
                    params.put("dealUserId", "");
                    params.put("compUserId", "");
                    //params.put("queryTypeLocal","staffStandby");
                    daoMap.put("staffStandby", orderStandbyDao.queryStandbyOrderCount(params));
                    //处理中 dealOrder
                    params.put("dealUserId", staffId);
                    params.put("compUserId", "");
                    params.put("staffId", "");
                    params.put("dispType", "260000003");
                    params.put("woState", "290000002");
                    //params.put("queryTypeLocal","dealOrder");
                    daoMap.put("dealOrder", orderStandbyDao.queryStandbyOrderCount(params));

                    //确认完成单
                    params.put("compUserId", staffId);
                    params.put("dealUserId", "");
                    params.put("staffId", "");
                    params.put("dispType", "260000004");
                    params.put("woState", "290000004"); //处理中
                    //params.put("queryTypeLocal","dispConfirm");//已完成
                    daoMap.put("dispConfirm", orderStandbyDao.queryStandbyOrderCompleteCount(params));

                    //异常单
                    params.put("compUserId", "");
                    params.put("dealUserId", staffId);
                    params.put("staffId", staffId);
                    params.put("dispType", "260000003");
                    params.put("woState", "290000002"); //处理中
                    //params.put("queryTypeLocal","abnormalOrder");
                    daoMap.put("abnormalOrder", orderStandbyDao.qryAbnormalOrderCount(params));

                    params.put("staffId", staffId);
                    //params.put("queryTypeLocal","ccOrder");
                    daoMap.put("ccOrder", orderStandbyDao.queryStandbyCcOrderCount(params));
                    //延期申请单
                    params.put("staffId", staffId);
                    daoMap.put("applyOrder", orderStandbyDao.queryPostponementOrderCount(params));


        }catch (Exception e){
            daoMap.put("messages","fail");
            daoMap.put("message",e.getMessage());
        }
        return daoMap;
    }

    /**
     * 待办中查询除自己以外其他tab页的计数
     * @param constant tab页常量
     * @param type 页面入参
     * @param param dao层查询参数
     * @param daoMap 返回值
     * @return
     */
    /*private int queryStandbyOrderCount(String constant, String type, Map<String, Object> param, Map<String, Object> daoMap) {
        int tmpCount;
        if ("abnormalOrder".equals(constant)) {
            tmpCount = orderStandbyDao.qryAbnormalOrderCount(param);
        } //已完成单独查询
        else if ("dispConfirm".equals(constant)) {
            tmpCount = orderStandbyDao.queryStandbyOrderCompleteCount(param);
        }else if ("ccOrder".equals(constant)){ //抄送
            tmpCount = orderStandbyDao.queryStandbyCcOrderCount(param);
        }else{
            tmpCount = orderStandbyDao.queryStandbyOrderCount(param);
        }
        daoMap.put(constant, tmpCount);
        return tmpCount;
    }
*/
    /**
     * 代办页面环节名称查询 整理环节id
     * @param params
     * @return
     */
    public Map<String,Object> getTeacheId(Map<String, Object> params){
        if(params.containsKey("tacheIds")){
            String tn = MapUtils.getString(params,"tacheIds");
            if(StringUtils.hasText(tn)){
                String[] splitStr = tn.split(",");
                Set<String> list = new HashSet<>();
                List<String> resList = new ArrayList<>();
                List<String> dataMakeList = new ArrayList<>();
                List<String> constructList = new ArrayList<>();
                for(String tmp :splitStr){
                    if(tmp.contains(BasicCode.RES_ALLOCATE)){
                        list.add("0");
                        resList.add(orderStandbyDao.qrySpecName(tmp));
                    } else if(tmp.contains(BasicCode.DATA_MAKE)){
                        list.add("0");
                        dataMakeList.add(orderStandbyDao.qrySpecName(tmp));
                    } else if(tmp.contains(BasicCode.RES_CONSTRUCT)){
                        list.add("0");
                        constructList.add(orderStandbyDao.qrySpecName(tmp));
                    } else{
                        list.add(tmp);
                    }
                }
                params.put("teacheName",list);
                if (!CollectionUtils.isEmpty(resList)) {
                    params.put("resList",resList);
                }
                if (!CollectionUtils.isEmpty(dataMakeList)) {
                    params.put("dataMakeList",dataMakeList);
                }
                if (!CollectionUtils.isEmpty(constructList)) {
                    params.put("constructList",constructList);
                }
            }
        }
        return params;
    }

    @Override
    public Map<String, Object> queryAllStandbyOrderCount(Map<String, Object> params) {
        params = getTeacheId(params);
        Map<String, Object> daoMap = new HashMap<String, Object>();
        String staffId = ThreadLocalInfoHolder.getLoginUser().getUserId();

        params.put("compUserId", staffId);
        //部门待办
        params.put("staffId", staffId);
        params.put("dealUserId", "");
        params.put("compUserId", "");
        params.put("dispType", "260000002");
        params.put("woState", "290000002");
        int jobStandby = orderStandbyDao.queryStandbyOrderCount(params);
        daoMap.put("jobStandby", jobStandby);
        //个人待办
        params.put("staffId", staffId);
        params.put("dispType", "260000003");
        params.put("woState", "290000002"); //处理中
        params.put("dealUserId", "");
        params.put("compUserId", "");
        int staffStandby = orderStandbyDao.queryStandbyOrderCount(params);
        daoMap.put("staffStandby", staffStandby);
        //处理中 dealOrder
        params.put("dealUserId", staffId);
        params.put("compUserId", "");
        params.put("staffId", "");
        params.put("dispType", "260000003");
        params.put("woState", "290000002");
        int dealOrder = orderStandbyDao.queryStandbyOrderCount(params);
        daoMap.put("dealOrder", dealOrder);
        //确认完成单
        params.put("compUserId", staffId);
        params.put("dealUserId", "");
        params.put("staffId", "");
        params.put("dispType", "260000004");
        params.put("woState", "290000004"); //处理中
        params.put("queryTypeLocal","dispConfirm");//已完成
        int dispConfirm = orderStandbyDao.queryStandbyOrderCompleteCount(params);
        daoMap.put("dispConfirm", dispConfirm);
        //岗位待办
        params.put("staffId", params.get("staffId"));
        params.put("dealUserId", "");
        params.put("compUserId", "");
        params.put("dispType", "260000001");
        params.put("woState", "290000002");
        int deptStandny = orderStandbyDao.queryStandbyOrderCount(params);
        daoMap.put("deptStandny", deptStandny);
        //异常单
        params.put("compUserId", "");
        params.put("dealUserId", staffId);
        params.put("staffId", staffId);
        params.put("dispType", "260000003");
        params.put("woState", "290000002"); //处理中
        params.put("queryTypeLocal","abnormalOrder");
        int abnormalCount = orderStandbyDao.qryAbnormalOrderCount(params);
        daoMap.put("abnormalCount", abnormalCount);
        //抄送
        params.put("staffId", staffId);
        int ccOrderCount = orderStandbyDao.queryStandbyCcOrderCount(params);
        daoMap.put("ccOrderCount", ccOrderCount); params.put("staffId", staffId);

        //延期
        int applyOrderCount = orderStandbyDao.queryPostponementOrderCount(params);
        daoMap.put("applyOrderCount", applyOrderCount);

        return daoMap;
    }

    @Override
    public List<BaseObject> querySysDict(Map<String, Object> mapJson) {
            //类型code
            Object codeType = mapJson.get("codeType");
            //类型名称
            Object codeTypeName = mapJson.get("codeTypeName");
            Map<String, Object> params = new HashMap<String, Object>();
            List<BaseObject> dictList = new ArrayList<BaseObject>();
            if (codeType != null || codeTypeName != null) {
                params.put("codeType", codeType);
                params.put("codeTypeName", codeTypeName);
                List<BaseObject> dataMaps = orderStandbyDao.querySysDictData(params);
                if (!CollectionUtils.isEmpty(dataMaps)) {
                    dictList.addAll(dataMaps);
                }
            }
            return dictList;
    }

    /**
     * 填写阶段性意见意见
     * @param
     * @return
     */
    @Override
    public Map<String, Object> updateCollapsible(List<Map> blockInfo, Map<String, Object> updloadMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            updateCollapsibleTransactional(blockInfo,updloadMap,multiFileMap);
            map.put("message", "成功");
            map.put("updateCount", 1);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
            map.put("message", e.getMessage());
            map.put("updateCount", 0);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCollapsibleTransactional(List<Map> blockInfo,Map<String, Object> updloadMap, MultiValueMap<String, MultipartFile> multiFileMap) throws Exception {
        try{
            for(Map mapInfo:blockInfo){
                Map<String, Object> updloadMapT = new HashMap<String, Object>();
                updloadMapT.putAll(updloadMap);
                updloadMapT.put("woId", mapInfo.get("WO_ID"));
                updloadMapT.put("srvOrdId", mapInfo.get("SRV_ORD_ID"));
                updloadMapT.put("orderId", mapInfo.get("ORDER_ID"));
                if(CollectionUtils.isEmpty(multiFileMap)){
                    orderStandbyDao.updateCollapsible(updloadMapT); //插入说明
                }else{
                    Map<String, Object> result = uploadFiles(updloadMapT, multiFileMap);
                    if((Boolean) result.get("flag")){
                        List<Map<String, Object>> fileInfo = (List<Map<String, Object>>) result.get("result");
                        for (int i = 0; i < fileInfo.size(); i++) {
                            updloadMapT.put("fileId", fileInfo.get(i).get("fileId"));
                            orderStandbyDao.updateCollapsible(updloadMapT); //插入说明
                        }
                    }else{
                        throw new Exception((String)result.get("message"));
                    }
                }
            }
        }catch (Exception e){
            throw new Exception();
        }
    }


    @Override
    public Map<String, Object> updateCollapsibleSingle(Map<String, Object> updloadMap) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Map<String, Object> uploadMapTemp = new HashMap();
            String selarrrowStr = (String) updloadMap.get("selarrrow");
            List<Map> mapsList = JSON.parseArray(selarrrowStr, Map.class);

            String staffId = (String)updloadMap.get("name");
            String remark = (String)updloadMap.get("remark");

            uploadMapTemp.put("staffId", staffId);
            uploadMapTemp.put("ctxPath", "");
            uploadMapTemp.put("origin", "YJ");
            uploadMapTemp.put("remark", remark);
            uploadMapTemp.put("fileId", "");

            updateCollapsibleTransactional(mapsList,uploadMapTemp,null);

            map.put("message", "成功");
            map.put("updateCount", 1);
        }catch (Exception e){
            logger.info(e.getMessage());
            map.put("message", "失败:"+e.getMessage());
            map.put("updateCount", 0);
        }
        return map;


    }
    /**
     * 附件上传
     * @param updloadMap
     * @param
     * @return
     */
    @Override
    public Map<String, Object> uploadFiles(Map<String, Object> updloadMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        List filesInfo =  new ArrayList();
        File uploadFile = null;
        File newUploadfile = null;

        Map<String, Object> retMessage = new HashMap<String, Object>();
        // 默认设置为true
        retMessage.put("flag", true);
        try {
            String ctxPath = (String) updloadMap.get("ctxPath");
            String staffId = (String) updloadMap.get("staffId");
            String delFiles = MapUtil.getString(updloadMap, "delFiles", "");
            //String woId = MapUtil.getString(updloadMap, "woId", "");
            /*if ((!"".equals(delFiles)) && (!"".equals(woId))) {
                String[] delFilesList = delFiles.split(",");
                orderStandbyDao.removeAttach(delFilesList, woId);
            }*/
            /**
             * 这里删除附件，我觉得可以直接用附件id删除，不用工单id去查询过滤了
             * 因为如果流程退单了，工单会产生新的，环节一样，这样用工单id去删除，删不了
             */
            if (!StringUtils.isEmpty(delFiles)){
                String[] delFilesList = delFiles.split(",");
                if(delFilesList.length > 0){
                    orderStandbyDao.removeAttach(delFilesList);
                }
            }
            updloadMap.put("staffId", staffId);
            updloadMap.put("ctxPath", ctxPath);
            List<MultipartFile> fileSet = new LinkedList<MultipartFile>();
            for (Map.Entry<String, List<MultipartFile>> temp : multiFileMap.entrySet()) {
                fileSet = temp.getValue();
                for (int i = 0; i < fileSet.size(); i++) {
                    Map<String, Object> fileInfo = new HashMap<String, Object>();
                    MultipartFile file = fileSet.get(i);
                    int fileId = getFileId();
                    String nameByType = file.getName();
                    String filename = file.getOriginalFilename();
                    float fileNum = (float) file.getSize() / 1024;
                    // 附件大小保留两位小数
                    DecimalFormat df = new DecimalFormat("0.00");
                    String fileSize = df.format(fileNum) + "KB";
                    String fileNameString = file.getOriginalFilename();
                    String fileType = fileNameString.substring(fileNameString.lastIndexOf('.') + 1).toLowerCase();
                    uploadFile = new File(ctxPath + File.separator + fileNameString);
                    newUploadfile = new File(fileId + "." + fileType);
                    uploadFile.renameTo(newUploadfile);
                    FileCopyUtils.copy(file.getBytes(), newUploadfile);
                    // 订单Id
                    fileInfo.put("orderId", updloadMap.get("orderId"));
                    // 工单Id
                    fileInfo.put("woId", updloadMap.get("woId"));
                    // 调单信息Id
                    fileInfo.put("srvOrdId", updloadMap.get("srvOrdId"));
                    // 文件标识
                    fileInfo.put("fileId", fileId);
                    // 文件名
                    fileInfo.put("fileName", fileNameString);
                    // 上传人
                    fileInfo.put("staffId", staffId);
                    // 文件大小
                    fileInfo.put("fileSize", fileSize);
                    // 文件类型
                    fileInfo.put("fileType", fileType);
                    fileInfo.put("origin", updloadMap.get("origin"));
                    // 相对路径/年月/时间搓/文件名
                    fileInfo.put("filePath", "ftpattach");
                    //调单ID
                    if (updloadMap.containsKey("dispatchOrderId")){
                        fileInfo.put("dispatchOrderId", updloadMap.get("dispatchOrderId"));
                    }else{
                        fileInfo.put("dispatchOrderId", "");
                    }

                    filesInfo.add(fileInfo);
                    //上传自己的服务器
                    retMessage = uploadFtpAndInsert(fileInfo, newUploadfile);
                    boolean flag = MapUtils.getBoolean(retMessage, "flag");
                    //全程调测本地测试环节--上传给集客服务器
                    /**
                     * 需要上传给集客服务器的环节：
                     * 1、集客来单本地测试、全程调测环节
                     * 2、集客下发到二干，二干下发到本地，本地的跨域全程调测环节 CROSS_WHOLE_COURDER_TEST
                     * 3.核查单核查汇总环节也需要上传到集客
                     */
                    if(flag && (BasicCode.WHOLE_COURSE_TEST.equals(MapUtils.getString(updloadMap, "tacheId"))
                                    || BasicCode.LOCAL_TEST.equals(MapUtils.getString(updloadMap, "tacheId")))
                            ||BasicCode.CROSS_WHOLE_COURDER_TEST.equals(MapUtils.getString(updloadMap, "tacheId"))
                            ||BasicCode.CHECK_TOTAL.equals(MapUtils.getString(updloadMap, "tacheId")) ){
                        String orderSource = "";
                        Map<String, Object> ifFromSecondaryMap = orderQrySecondaryDao.qryIfFromSecondary(MapUtils.getString(updloadMap, "orderId"));
                        //判断是否由二干下发
                        if(MapUtils.isEmpty(ifFromSecondaryMap)){
                            orderSource = orderQrySecondaryDao.qrySrvOrderSource(MapUtils.getString(updloadMap, "orderId"));
                        }else {
                            orderSource = orderQrySecondaryDao.qrySrvOrderSourceFromSec(MapUtils.getString(updloadMap, "orderId"));
                        }
                        if (BasicCode.JIKE.equals(orderSource)){
                            logger.info(">>>>>>>>>>>>>>>>>>>>全程调测环节上传附件给集客能力平台>>>>>>>>>>>>>>>>>>>>>>>");
                            Map<String, Object> attachMap = new HashMap<>();
                            attachMap.put("path", "ftpattach");
                            attachMap.put("name", fileId+"."+fileType);
                            if (!orderStandbyServiceIntf.upJiKeFtp(attachMap)){
                                logger.info(">>>>>>>>>全程调测环节上传附件给集客能力平台>>>>>>>>>>上传失败！>>>>>>>>>>>>>");
                                retMessage.put("flag", false);
                                retMessage.put("message", "附件上传集客能力平台失败！");
                            }else {
                                logger.info(">>>>>>>>>全程调测环节上传附件给集客能力平台>>>>>>>>>>上传成功！>>>>>>>>>>>>>");
                            }

                        }
                    }
                    newUploadfile.delete();
                }
            }
            retMessage.put("result", filesInfo);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            retMessage.put("flag", false);
            retMessage.put("message", e.getMessage());
        }
        return retMessage;
    }

    /**
     * 导出
     * @param excelData
     * @return
     */
    @Override
    public List<Map<String, Object>> exportStandbyOrderData(Map<String, Object> excelData) {
        return orderStandbyDao.queryOrderInfo(excelData);
    }
   /**
     * 上传对标文件到ftp和记录信息
     */
    public Map<String, Object>  uploadFtpAndInsert(Map<String, Object> map, File uploadFile) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        //1:上传到ftp
        if (uploadFileToFtp(map, uploadFile)) {
            //2：写入到数据库
            if (orderStandbyDao.upLoadAttach(map) > 0) {
                returnMap.put("flag", true);
                returnMap.put("message", "上传成功");
            }
            else {
                //写数据库失败
                returnMap.put("flag", false);
                returnMap.put("message", "附件上传记录入库失败");
            }
        }
        else {
            //上传到ftp失败
            returnMap.put("flag", false);
            returnMap.put("message", "附件上传ftp失败");
        }

        return returnMap;
    }

    /**
     * 上传文件到ftp
     * @Description:
     * @author zheng.zhijian
     *
     * @date 2018-3-16
     */
    public boolean uploadFileToFtp(Map<String, Object> map, File localFile) {

        try {
//            FtpUtils.uploadFtp(SysConfig.FILE_SERVER.getFtpIp(), SysConfig.FILE_SERVER.getFtpPort(), SysConfig.FILE_SERVER.getFtpUserName(),
//                    SysConfig.FILE_SERVER.getFtpPassword(), localFile, MapUtils.getString(map, "filePath"));

            FtpUtils.uploadFtp(address, Integer.parseInt(port), username,
                    password, localFile, MapUtils.getString(map, "filePath"));
            return true;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }
    /*
     * 删除
     * @author ren.jiahang
     * @date 2019/1/18 16:20
     * @param map
     * @param localFile
     * @return boolean
     */
    public boolean delFileToFtp(Map<String, Object> map) {
        try {
            FtpUtils.deleteFtpFile(address, Integer.parseInt(port), username,
                    password, MapUtils.getString(map, "filePath"), MapUtils.getString(map, "fileName"));
            return true;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    public  int getFileId() {
        int fileId = orderStandbyDao.getSequence("SEQ_GOM_BDW_SRV_ORD_REMARK.nextval");
        return fileId;
    }


    @Override
    public Map<String, Object> qryCustInfo(String cstOrdId) {
        Map<String, Object> returnMap = new HashMap<>();
        List<Map<String, Object>> cstInfo = orderStandbyDao.qryCustInfo(cstOrdId);
        returnMap.put("cstInfo", cstInfo);
        return returnMap;
    }

    @Override
    public Map<String, Object> qrySrvOrderInfo(String cstOrdId) {
        Map<String, Object> returnMap = new HashMap<>();
        List<Map<String, Object>> cstInfo = orderStandbyDao.qrySrvOrderInfo(cstOrdId);
        returnMap.put("srvOrderInfo", cstInfo);
        return returnMap;
    }

    @Override
    public Map<String, Object> qryDispatchOrderInfo(String cstOrdId) {
        Map<String, Object> returnMap = new HashMap<>();
        List<Map<String, Object>> cstInfo = orderStandbyDao.qryDispatchOrderInfo(cstOrdId);
        returnMap.put("dispatchOrderInfo", cstInfo);
        return returnMap;
    }

    @Override
    public Map<String, Object> queryOrderCircuitInfo(Map params) {

            String srvOrdId = MapUtils.getString(params, "srvOrdId");
            String serviceId = MapUtils.getString(params, "serviceId");
            List<Map<String, Object>> pubList = new ArrayList();
            List<Map<String, Object>> listPub = orderDetailsDao.queryCircuitInfo(srvOrdId, serviceId);
            List<Map<String, Object>> listA = orderDetailsDao.queryCircuitInfoAZ(srvOrdId, serviceId, "A");
            List<Map<String, Object>> listZ = orderDetailsDao.queryCircuitInfoAZ(srvOrdId, serviceId, "Z");
            List<Map<String, Object>> listPE = orderDetailsDao.queryCircuitInfoPE(srvOrdId, serviceId, "PE");
            List<Map<String, Object>> listCE = orderDetailsDao.queryCircuitInfoPE(srvOrdId, serviceId, "CE");

            if (!CollectionUtils.isEmpty(listPub)) {
                for (Map<String, Object> pubMap:listPub) {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("ATTR_NAME", MapUtils.getString(pubMap, "PROPERTY_NAME"));
                    attrMap.put("ATTR_VALUE", MapUtils.getString(pubMap, "ATTR_VALUE"));
                    pubList.add(attrMap);
                }
            }
            if (!CollectionUtils.isEmpty(listA)) {
                for (Map<String, Object> pubMap:listA) {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("ATTR_NAME", MapUtils.getString(pubMap, "PROPERTY_NAME"));
                    attrMap.put("ATTR_VALUE", MapUtils.getString(pubMap, "ATTR_VALUE"));
                    pubList.add(attrMap);
                }
            }
            if (!CollectionUtils.isEmpty(listZ)) {
                for (Map<String, Object> pubMap:listZ) {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("ATTR_NAME", MapUtils.getString(pubMap, "PROPERTY_NAME"));
                    attrMap.put("ATTR_VALUE", MapUtils.getString(pubMap, "ATTR_VALUE"));
                    pubList.add(attrMap);
                }
            }
            if (!CollectionUtils.isEmpty(listPE)) {
                for (Map<String, Object> pubMap:listPE) {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("ATTR_NAME", MapUtils.getString(pubMap, "PROPERTY_NAME"));
                    attrMap.put("ATTR_VALUE", MapUtils.getString(pubMap, "ATTR_VALUE"));
                    pubList.add(attrMap);
                }
            }
            if (!CollectionUtils.isEmpty(listCE)) {
                for (Map<String, Object> pubMap:listCE) {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("ATTR_NAME", MapUtils.getString(pubMap, "PROPERTY_NAME"));
                    attrMap.put("ATTR_VALUE", MapUtils.getString(pubMap, "ATTR_VALUE"));
                    pubList.add(attrMap);
                }
            }

            Map<String, Object> returnMap = new HashMap<>();
    //        List<Map<String, Object>> cstInfo = orderStandbyDao.queryOrderCircuitInfo(params);
            returnMap.put("orderCircuitInfo", pubList);
            return returnMap;
    }

    @Override
    public boolean upJiKeFtp(Map<String, Object> params) {
        boolean flag = false;
        try {
            flag = updateToJiKe.updateFileToFtp(params, "uploadJiKe");
        }
        catch (Exception e) {
            flag = false;
            logger.error(e.getMessage());
        }
        return flag;
    }

/*
     * 操作抄送表 增加修改删除
     * @author ren.jiahang
     * @date 2019/6/1 15:24
     * @param params
     * @return int
     */
    @Override
    public int addCC(Map<String, Object> params) {
        String woId = org.apache.commons.collections.MapUtils.getString(params, "woId");
        String objId = org.apache.commons.collections.MapUtils.getString(params, "objId");
        String objType = org.apache.commons.collections.MapUtils.getString(params, "objType");
        String srvOrdId = org.apache.commons.collections.MapUtils.getString(params, "srvOrdId");
        List<Map<String,Object>> ccList = new ArrayList<>();
        if(objId!=null && objType.equals("260000003")){
            String[] orjIdsplit = objId.split(",");
            for(String objIdItem : orjIdsplit){
                Map<String,Object> ccMap = new HashMap<>();
                ccMap.put("woId",woId);
                ccMap.put("dispObjId",objIdItem);
                ccMap.put("dispObjType",objType);
                ccMap.put("srvOrdId",srvOrdId);
                ccList.add(ccMap);
            }
        }else if(objId!=null && objType.equals("260000002")){
            String[] orjIdsplit = objId.split(",");
            for(String objIdItem : orjIdsplit){
                //根据objIdItem查询岗位下人员，遍历人员信息组装map
                List<Map<String, Object>> staffList = orderDealDao.qryUserByGroup(objIdItem);
                for(Map<String, Object> staffMap: staffList){
                    Map<String,Object> ccMap = new HashMap<>();
                    ccMap.put("woId",woId);
                    ccMap.put("dispObjId", MapUtil.getString(staffMap,"STAFFID"));
                    ccMap.put("dispObjType","260000003");
                    ccMap.put("srvOrdId",srvOrdId);
                    ccList.add(ccMap);
                }
            }
        }else if(objId!=null && objType.equals("260000001")){
            String[] orjIdsplit = objId.split(",");

            for(String objIdItem : orjIdsplit){
                //根据objIdItem查询部门下人员，遍历人员信息组装map
                List<Map<String, Object>> staffList = orderDealDao.qryUserByDept(objIdItem);
                for(Map<String, Object> staffItem : staffList){
                    Map<String,Object> ccMap = new HashMap<>();
                    ccMap.put("woId",woId);
                    ccMap.put("dispObjId",MapUtil.getString(staffItem,"STAFFID"));
                    ccMap.put("dispObjType","260000003");
                    ccMap.put("srvOrdId",srvOrdId);
                    ccList.add(ccMap);
                }
            }
        }
        return orderStandbyDao.addCC(ccList);
    }

    @Override
    public int updateCC(Map<String, Object> params)  {
        String srvOrdIds = MapUtil.getString(params, "srvOrdIds");
        if(StringUtils.hasText(srvOrdIds)){
            String[] splitStr = srvOrdIds.split(",");
            params.put("srvOrdId",splitStr);
        }
       // orderStandbyDao.updateCC(params);
        return orderStandbyDao.delCC(params);
    }

    @Override
    public int delCC(Map<String, Object> params) {
        return orderStandbyDao.delCC(params);
    }

    @Override
    public List<Map<String, Object>> queryTaskInfo(Map<String, Object> params) {
        //定单查询需要单独处理
        String selectType = org.apache.commons.collections.MapUtils.getString(params, "selectType");
        List<Map<String, Object>> taskInfos = new ArrayList<>();
        String orderIds = org.apache.commons.collections.MapUtils.getString(params, "orderIds");
        String orderId = org.apache.commons.collections.MapUtils.getString(params, "orderId");
        if(!StringUtils.hasText(orderIds)){
            orderIds = orderId;
        }
        if(StringUtils.hasText(orderIds)){
            String[] splitStr = orderIds.split(",");
            for (int i = 0; i < splitStr.length; i++) {
                params.put("orderId", splitStr[i]);
                List<Map<String, Object>> taskInfo = new ArrayList<>();
                List<Map<String, Object>> maps = orderDetailsSevice.querySec2LocalInfo(splitStr[i]);
                if("gomOrderQuery".equals(selectType) && maps.size()>0){//需要先查下发到本地得关联单id
                    for (int m=0;m<maps.size();m++){
                        taskInfo = orderDetailsSevice.querySecTaskInfo(MapUtil.getString(maps.get(m),"ORDER_ID"));
                    }
                }else {
                     taskInfo = orderDetailsSevice.querySecTaskInfo(splitStr[i]);
                }
                taskInfos.addAll(taskInfo);
            }
        }
        return taskInfos;
    }

    @Override
    public List<Map<String, Object>> querySecToLocalTaskInfo(Map<String, Object> params) {

        //定单查询需要单独处理
        String selectType = org.apache.commons.collections.MapUtils.getString(params, "selectType");
        List<Map<String, Object>> secToLocalTaskInfos = new ArrayList<>();
        String orderIds = org.apache.commons.collections.MapUtils.getString(params, "orderIds");
        String orderId = org.apache.commons.collections.MapUtils.getString(params, "orderId");
        if(!StringUtils.hasText(orderIds)){
            orderIds = orderId;
        }
        if(StringUtils.hasText(orderIds)){
            String[] splitStr = orderIds.split(",");
            for (int i = 0; i < splitStr.length; i++) {
                List<Map<String, Object>> secToLocalTaskInfo = new ArrayList<>();
                List<Map<String, Object>> maps = orderDetailsSevice.querySec2LocalInfo(splitStr[i]);
                if("gomOrderQuery".equals(selectType) && maps.size()>0){//需要先查下发到本地得关联单id
                    for (int m=0;m<maps.size();m++){
                        secToLocalTaskInfo = orderDetailsSevice.queryTaskInfo(MapUtil.getString(maps.get(m),"ORDER_ID"));
                    }
                }else {
                    secToLocalTaskInfo = orderDetailsSevice.queryTaskInfo(splitStr[i]);
                }
                secToLocalTaskInfos.addAll(secToLocalTaskInfo);
            }
        }
        return secToLocalTaskInfos;

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertAbnomrmalInfo(Map<String, Object> updloadMap, MultiValueMap<String, MultipartFile> multiFileMap) {

        Map<String, Object> map = new HashMap();
        String ctxPath = MapUtils.getString(updloadMap, "ctxPath");
        //1.上传附件
        List<MultipartFile> fileSet = new LinkedList<MultipartFile>();
        try {
            for (Map.Entry<String, List<MultipartFile>> temp : multiFileMap.entrySet()) {
                fileSet = temp.getValue();
                for (int i = 0; i < fileSet.size(); i++) {
                    Map<String, Object> fileInfo = new HashMap<String, Object>();
                    MultipartFile file = fileSet.get(i);
                    int fileId = getFileId();
                    String nameByType = file.getName();
                    String filename = file.getOriginalFilename();
                    float fileNum = (float) file.getSize() / 1024;
                    // 附件大小保留两位小数
                    DecimalFormat df = new DecimalFormat("0.00");
                    String fileSize = df.format(fileNum) + "KB";
                    String fileNameString = file.getOriginalFilename();
                    String fileType = fileNameString.substring(fileNameString.lastIndexOf('.') + 1).toLowerCase();
                    File newUploadfile = null;
                    File uploadFile = new File(ctxPath + File.separator + fileNameString);
                    newUploadfile = new File(fileId + "." + fileType);
                    uploadFile.renameTo(newUploadfile);
                    FileCopyUtils.copy(file.getBytes(), newUploadfile);
                    // 订单Id
                    fileInfo.put("srvOrdId", updloadMap.get("orderId"));// 异常单没有srvOrdId 用orderId?
                    // 工单Id
                    fileInfo.put("woId", updloadMap.get("woId"));
                    // 调单信息Id
                    fileInfo.put("cstOrdId", updloadMap.get("cstOrdId"));
                    // 文件标识
                    fileInfo.put("fileId", fileId);
                    // 文件名
                    fileInfo.put("fileName", fileNameString);
                    // 上传人
                    fileInfo.put("staffId", MapUtils.getString(updloadMap, "staffId"));
                    // 文件大小
                    fileInfo.put("fileSize", fileSize);
                    // 文件类型
                    fileInfo.put("fileType", fileType);
                    fileInfo.put("origin", updloadMap.get("origin"));
                    // 相对路径/年月/时间搓/文件名
                    fileInfo.put("filePath", "ftpattach");
                    //调单ID
                   fileInfo.put("dispatchOrderId", "");
                    //上传自己的服务器
                    Map<String, Object> retMessage = uploadFtpAndInsert(fileInfo, newUploadfile);
                    boolean flag = MapUtils.getBoolean(retMessage, "flag");
                    newUploadfile.delete();
                }
            }
            //2.写入流程日志表
            String remark = MapUtils.getString(updloadMap, "remark");
            if(!StringUtils.isEmpty(remark)){
//                104-追单 108-加急 109-延期 110-挂起 111-解挂 112撤单作废
                String changeType = "";
                if("104".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "追单";
                } if("108".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "加急";
                } if("109".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "延期";
                } if("110".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "挂起";
                }if("111".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "解挂";
                }if("112".equals(MapUtils.getString(updloadMap,"chgType"))){
                    changeType = "撤单作废";
                }
                StringBuffer trackContent = new StringBuffer();
                trackContent.append(MapUtils.getString(updloadMap,"userName"))
                            .append(" [提交] ").append(changeType).append("确认 说明：")
                            .append(MapUtils.getString(updloadMap,"remark"));
                updloadMap.put("trackContent", trackContent.toString());
                orderStandbyDao.insertFlowLogInfo(updloadMap);
            }
            map.put("message", "成功");
            map.put("updateCount", 1);
        } catch (IOException e) {
            map.put("message", e.getMessage());
            map.put("updateCount", 0);
        }
        return map;
    }
}
