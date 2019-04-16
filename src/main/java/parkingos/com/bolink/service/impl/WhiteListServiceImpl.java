package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import parkingos.com.bolink.dao.spring.CommonDao;
import parkingos.com.bolink.models.FixCodeTb;
import parkingos.com.bolink.models.ParkLogTb;
import parkingos.com.bolink.models.WhiteListTb;
import parkingos.com.bolink.qo.PageOrderConfig;
import parkingos.com.bolink.qo.SearchBean;
import parkingos.com.bolink.service.CommonService;
import parkingos.com.bolink.service.SaveLogService;
import parkingos.com.bolink.service.SupperSearchService;
import parkingos.com.bolink.service.WhiteListService;
import parkingos.com.bolink.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WhiteListServiceImpl implements WhiteListService {

    Logger logger = LoggerFactory.getLogger(WhiteListServiceImpl.class);

    @Autowired
    private SupperSearchService<WhiteListTb> supperSearchService;
    @Autowired
    CommonDao commonDao;
    @Autowired
    CommonService commonService;
    @Autowired
    SaveLogService saveLogService;


    @Override
    public JSONObject groupQuery(Map<String, String> reqParameterMap) {
//        WhiteListTb whiteListTb = new WhiteListTb();
////        whiteListTb.setState(0);
//        Long groupId = Long.parseLong(reqParameterMap.get("groupid"));
//        whiteListTb.setGroupId(groupId);
//        JSONObject result = supperSearchService.supperSearch(whiteListTb,reqParameterMap);
//        return result;


        WhiteListTb whiteListTb = new WhiteListTb();
        Long groupId = Long.parseLong(reqParameterMap.get("groupid"));
        whiteListTb.setGroupId(groupId);
//        JSONObject result = supperSearchService.supperSearch(whiteListTb,reqParameterMap);
        JSONObject result = new JSONObject();
        int count =0;
        List<WhiteListTb> list =null;
        List<Map<String, Object>> resList =new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> updateList =new ArrayList<Map<String, Object>>();
        Map searchMap = supperSearchService.getBaseSearch(whiteListTb,reqParameterMap);
        if(searchMap!=null&&!searchMap.isEmpty()){
            WhiteListTb t1 =(WhiteListTb)searchMap.get("base");
            List<SearchBean> supperQuery = null;
            if(searchMap.containsKey("supper"))
                supperQuery = (List<SearchBean>)searchMap.get("supper");
            PageOrderConfig config = null;
            if(searchMap.containsKey("config"))
                config = (PageOrderConfig)searchMap.get("config");
            count = commonDao.selectCountByConditions(t1,supperQuery);
            if(count>0){
                if (config == null) {
                    config = new PageOrderConfig();
                    config.setPageInfo(1, Integer.MAX_VALUE);
                }
                list  = commonDao.selectListByConditions(t1,supperQuery,config);
                if (list != null && !list.isEmpty()) {
                    for (WhiteListTb whiteListTb1 : list) {
                        OrmUtil<WhiteListTb> otm = new OrmUtil<>();
                        Map<String, Object> map = otm.pojoToMap(whiteListTb1);

                        if((int)map.get("end_type")==0&&map.get("e_time")!=null&&(int)map.get("state")!=1){
                            if(Long.parseLong(map.get("e_time")+"")<System.currentTimeMillis()/1000){//已过期
                                map.put("state",1);
//                                updateList.add(map);
                                WhiteListTb wh = new WhiteListTb();
                                wh.setId((Long)map.get("id"));
                                wh.setState(1);
                                commonDao.updateByPrimaryKey(wh);
                            }
                        }
                        resList.add(map);
                    }
                    result.put("rows", JSON.toJSON(resList));
                }
            }
        }

        return result;

    }

    @Override
    public JSONObject add(String remark, String carNumber, Long btime, Long etime, Long comid, String userName, String mobile, String carLocation,String nickname,Long uin,Long groupId,int type,int endType) {

        if(btime>0){
            btime = btime/1000;
        }
        if(etime>0){
            etime = etime/1000;
        }

        WhiteListTb whiteListTb = new WhiteListTb();
        whiteListTb.setParkId(comid);
        whiteListTb.setCarNumber(carNumber);
        int count = commonDao.selectCountByConditions(whiteListTb);
        int doWhite = 0;
        JSONObject result = new JSONObject();
        result.put("state",0);
        result.put("msg","操作失败!");
        if(count==0){
            //插入新车牌
            whiteListTb.setGroupId(groupId);
            whiteListTb.setEndType(endType);
            whiteListTb.setRemark(remark);
            whiteListTb.setbTime(btime);
            whiteListTb.seteTime(etime);
            whiteListTb.setUserName(userName);
            whiteListTb.setMobile(mobile);
            whiteListTb.setCarLocation(carLocation);
            doWhite= commonDao.insert(whiteListTb);
        }else{
            //更新车场该车牌
            WhiteListTb update = new WhiteListTb();
            update.setEndType(endType);
            update.setRemark(remark);
            update.setbTime(btime);
            update.seteTime(etime);
            update.setuTime(System.currentTimeMillis()/1000);
            update.setUserName(userName);
            update.setMobile(mobile);
            update.setCarLocation(carLocation);
            doWhite = commonDao.updateByConditions(update,whiteListTb);
        }
        if(doWhite==1){
            result.put("state",1);
            result.put("msg","操作成功!");

            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setContent(uin+"("+nickname+")"+"新建月卡会员成功:"+carNumber);
            parkLogTb.setType("white");
            if(type==1){
                parkLogTb.setParkId(comid);
            }
            parkLogTb.setGroupId(groupId);
            if(count==0){
                parkLogTb.setOperateType(1);
            }else{
                parkLogTb.setOperateType(2);
            }
            saveLogService.saveLog(parkLogTb);

        }
        return result;
    }

    @Override
    public JSONObject delete(Long id, String nickname, Long uin,Long comid, Long groupId,int type) {
        JSONObject result = new JSONObject();
        result.put("state",0);
        if(id<0){
            result.put("msg","删除失败!");
            return result;
        }
        WhiteListTb con = new WhiteListTb();
        con.setId(id);
        int delete = commonDao.deleteByConditions(con);
        if(delete==1){
            result.put("state",1);
            result.put("msg","删除成功!");

            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setContent(uin+"("+nickname+")"+"删除月卡会员"+id);
            parkLogTb.setType("white");
            if(type==1){
                parkLogTb.setParkId(comid);
            }
            parkLogTb.setGroupId(groupId);
            parkLogTb.setOperateType(3);
            saveLogService.saveLog(parkLogTb);

        }
        return result;
    }

    @Override
    public JSONObject importExcel(MultipartFile file, String nickname, Long uin, Long groupid) {
        JSONObject result = new JSONObject();
        result.put("state",0);
        try {
            String errmsg = "";
            // 上传文件保存到服务器的文件名
            String filename = "";
            // 当前上传文件的InputStream对象
            InputStream is = null;

            if (file != null) {
                filename = file.getOriginalFilename();
                try {
                    is = file.getInputStream();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            logger.info("====上传车主会员:" + filename);

            if (is != null && filename != null) {
                List<Object[]> datas = ImportExcelUtil.generateUserSql(is, filename, 1);
                if (datas != null && !datas.isEmpty()) {
                    //车牌号*
                    int i = 1;
                    for(Object[] o:datas){
                        if(o.length<8){
                            errmsg+= i+"行数据错误</br>";
                            i++;
                            continue;
                        }
                        String comid = o[0]+"";
                        String btime = o[1]+"";
                        String etime = o[2]+"";
                        String carNumber = o[7]+"";
                        //姓名，电话，车位，备注
                        String userName = o[3]+"";
                        String mobile = o[4]+"";
                        String carLocation = o[5]+"";
                        String remark = o[6]+"";
                        logger.info("=e==>>>>第"+i+"行:"+comid+"~~~"+carNumber+"~~~"+btime+"~~~"+etime+"~~"+userName);
                        if(Check.isEmpty(comid)&&Check.isEmpty(btime)&&Check.isEmpty(carNumber)&&Check.isEmpty(etime)&&Check.isEmpty(userName)&&Check.isEmpty(mobile)&&Check.isEmpty(carLocation)&&Check.isEmpty(remark)){
                            break;
                        }
                        if(Check.isEmpty(carNumber)||Check.isEmpty(comid)||Check.isEmpty(btime)||Check.isEmpty(etime)){
                            errmsg+= i+"行缺少必传字段</br>";
                            i++;
                            continue;
                        }

                        Long beginTime = TimeTools.getLongMilliSecondFrom_HHMMDD(btime) / 1000;
                        Long endTime = TimeTools.getLongMilliSecondFrom_HHMMDD(etime) / 1000;
                        Long parkId =commonService.getParkIdByBolinkId(comid);

                        if(parkId==null||parkId<0){
                            errmsg+= i+"行车场编号错误</br>";
                            i++;
                            continue;
                        }

                        Long groupId = commonService.getGroupIdByComid(parkId);
                        if(!groupId.equals(groupid)){
                            errmsg+= i+"行车场编号错误</br>";
                            i++;
                            continue;
                        }


                        //如果判断通过
                        WhiteListTb whiteListTb = new WhiteListTb();
                        whiteListTb.setParkId(parkId);
                        whiteListTb.setCarNumber(carNumber);

                        ParkLogTb parkLogTb = new ParkLogTb();
                        parkLogTb.setOperateUser(nickname);
                        parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
                        parkLogTb.setContent(uin+"("+nickname+")"+"新建月卡会员成功:"+carNumber);
                        parkLogTb.setType("white");
                        parkLogTb.setGroupId(groupid);
                        //查询是不是已经存在这个车牌
                        int count = commonDao.selectCountByConditions(whiteListTb);
                        if(count>0){
                            //如果已经存在这个车牌，那么更新
                            WhiteListTb update = new WhiteListTb();
                            update.setbTime(beginTime);
                            update.seteTime(endTime);
                            update.setUserName(userName);
                            update.setMobile(mobile);
                            update.setCarLocation(carLocation);
                            update.setRemark(remark);
                            commonDao.updateByConditions(update,whiteListTb);

                            parkLogTb.setOperateType(2);
                            saveLogService.saveLog(parkLogTb);
                            i++;
                            continue;
                        }

                        whiteListTb.setbTime(beginTime);
                        whiteListTb.seteTime(endTime);
                        whiteListTb.setUserName(userName);
                        whiteListTb.setMobile(mobile);
                        whiteListTb.setCarLocation(carLocation);
                        whiteListTb.setRemark(remark);
                        commonDao.insert(whiteListTb);

                        parkLogTb.setOperateType(1);
                        saveLogService.saveLog(parkLogTb);

                        i++;
                    }
                } else {
                    errmsg += "请上传正确的Excel文件</br>";
                }
                if(!errmsg.contains("br")){
                    errmsg="导入成功!";
                }
                result.put("state", 1);
                logger.info(errmsg);
            }
            result.put("msg", errmsg);
            return result;
        }catch (Exception e){
            logger.error("===>>>>>导入异常",e);
        }
        return result;
    }

    @Override
    public JSONObject edit(Long id, String remark, String carNumber, Long btime, Long etime, Long comid, String userName, String mobile, String carLocation, String nickname, Long uin, Long groupid, int type,int endType) {

        if(btime>0){
            btime = btime/1000;
        }
        if(etime>0){
            etime = etime/1000;
        }
        JSONObject result = new JSONObject();
        //查询以前的车牌
        WhiteListTb con = new WhiteListTb();
        con.setId(id);
        con = (WhiteListTb) commonDao.selectObjectByConditions(con);
        String carBefore = "";
        Long comBefore = -1L;
        if(con!=null){
            carBefore = con.getCarNumber();
            comBefore=con.getParkId();
        }

        result.put("state",0);

        WhiteListTb whiteListTb = new WhiteListTb();
        //修改了车牌或者车场编号
        if(!carBefore.equals(carNumber)||!comBefore.equals(comid)){
            whiteListTb.setParkId(comid);
            whiteListTb.setCarNumber(carNumber);
            int count = commonDao.selectCountByConditions(whiteListTb);
            if(count>0) {
                result.put("msg", "车场已存在该车牌，修改失败！");
                return result;
            }
        }

        whiteListTb.setEndType(endType);
        whiteListTb.setRemark(remark);
        whiteListTb.setCarLocation(carLocation);
        whiteListTb.setMobile(mobile);
        whiteListTb.setUserName(userName);
        whiteListTb.seteTime(etime);
        whiteListTb.setbTime(btime);
        whiteListTb.setuTime(System.currentTimeMillis()/1000);
        whiteListTb.setCarNumber(carNumber);
        whiteListTb.setId(id);

        int update = commonDao.updateByPrimaryKey(whiteListTb);
        if(update==1){
            result.put("state",1);
            result.put("msg","更新成功！");

            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setContent(uin+"("+nickname+")"+"更新了月卡会员"+id);
            parkLogTb.setType("white");
            if(type==1){
                parkLogTb.setParkId(comid);
            }
            parkLogTb.setGroupId(groupid);
            parkLogTb.setOperateType(2);
            saveLogService.saveLog(parkLogTb);
        }

        return result;
    }

    @Override
    public JSONObject parkQuery(Map<String, String> reqParameterMap) {
        WhiteListTb whiteListTb = new WhiteListTb();
        Long comid = Long.parseLong(reqParameterMap.get("comid"));
        whiteListTb.setParkId(comid);
//        JSONObject result = supperSearchService.supperSearch(whiteListTb,reqParameterMap);
        JSONObject result = new JSONObject();
        int count =0;
        List<WhiteListTb> list =null;
        List<Map<String, Object>> resList =new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> updateList =new ArrayList<Map<String, Object>>();
        Map searchMap = supperSearchService.getBaseSearch(whiteListTb,reqParameterMap);
        if(searchMap!=null&&!searchMap.isEmpty()){
            WhiteListTb t1 =(WhiteListTb)searchMap.get("base");
            List<SearchBean> supperQuery = null;
            if(searchMap.containsKey("supper"))
                supperQuery = (List<SearchBean>)searchMap.get("supper");
            PageOrderConfig config = null;
            if(searchMap.containsKey("config"))
                config = (PageOrderConfig)searchMap.get("config");
            count = commonDao.selectCountByConditions(t1,supperQuery);
            if(count>0){
                if (config == null) {
                    config = new PageOrderConfig();
                    config.setPageInfo(1, Integer.MAX_VALUE);
                }
                list  = commonDao.selectListByConditions(t1,supperQuery,config);
                if (list != null && !list.isEmpty()) {
                    for (WhiteListTb whiteListTb1 : list) {
                        OrmUtil<WhiteListTb> otm = new OrmUtil<>();
                        Map<String, Object> map = otm.pojoToMap(whiteListTb1);

                        if((int)map.get("end_type")==0&&map.get("e_time")!=null&&(int)map.get("state")!=1){
                            if(Long.parseLong(map.get("e_time")+"")<System.currentTimeMillis()/1000){//已过期
                                map.put("state",1);
//                                updateList.add(map);
                                WhiteListTb wh = new WhiteListTb();
                                wh.setId((Long)map.get("id"));
                                wh.setState(1);
                                commonDao.updateByPrimaryKey(wh);
                            }
                        }
                        resList.add(map);
                    }
                    result.put("rows", JSON.toJSON(resList));
                }
            }
        }

        return result;
    }
}