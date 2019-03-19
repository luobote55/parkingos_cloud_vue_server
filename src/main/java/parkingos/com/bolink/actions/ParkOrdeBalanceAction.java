package parkingos.com.bolink.actions;


import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import parkingos.com.bolink.models.ParkLogTb;
import parkingos.com.bolink.service.ParkOrdeBalanceService;
import parkingos.com.bolink.service.ParkOrderAnlysisService;
import parkingos.com.bolink.service.SaveLogService;
import parkingos.com.bolink.utils.ExportExcelUtil;
import parkingos.com.bolink.utils.RequestUtil;
import parkingos.com.bolink.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/parkbalance")
public class ParkOrdeBalanceAction {

    Logger logger = LoggerFactory.getLogger(ParkOrdeBalanceAction.class);

    @Autowired
    private ParkOrdeBalanceService parkOrdeBalanceService;
    @Autowired
    private SaveLogService saveLogService;

    /*
    * 车场统计分析  车场日报（结算订单）
    *
    * */
    @RequestMapping(value = "/query")
    public String query(HttpServletRequest request, HttpServletResponse resp){

        Map<String, String> reqParameterMap = RequestUtil.readBodyFormRequset(request);

        JSONObject result = parkOrdeBalanceService.getBalanceTrade(reqParameterMap);
        //把结果返回页面
        StringUtils.ajaxOutput(resp,result.toJSONString());
        return null;
    }



    @RequestMapping(value = "/exportExcel")
    public String exportExcel(HttpServletRequest request, HttpServletResponse response){
        Long comid = RequestUtil.getLong(request,"comid",-1L);
        String nickname = StringUtils.decodeUTF8(RequestUtil.getString(request,"nickname1"));
        Long uin = RequestUtil.getLong(request, "loginuin", -1L);
        Map<String, String> reqParameterMap = RequestUtil.readBodyFormRequset(request);

        List<List<Object>> resList = parkOrdeBalanceService.exportExcel(reqParameterMap);
        String title = "车场日报";
        String sheeatName = "sheet1";
        String headers[] =  {  "日期", "电子收入", "电子收入", "电子收入","现金收入","现金收入","现金收入","减免金额" } ;
        String dataType []={"STR","STR","STR","STR","STR","STR","STR","STR"};
        String[] subHeads = new String[] {"场内预付", "出场结算", "合计","场内预付", "出场结算", "合计"};
        String[] headnum = new String[] { "1,2,0,0", "1,1,1,1","1,1,2,2","1,1,3,3","1,1,4,4","1,1,5,5","1,1,6,6","1,2,7,7"};
        String[] subheadnum = new String[] { "2,2,1,1", "2,2,2,2", "2,2,3,3", "2,2,4,4", "2,2,5,5","2,2,6,6"};
        ExportExcelUtil excelUtil = new ExportExcelUtil(title, headers, sheeatName, dataType, subHeads, headnum, subheadnum, new int[]{0,7});
        String fname = "车场日报-结算订单";
        fname = StringUtils.encodingFileName(fname)+".xls";
        try {
            OutputStream os = response.getOutputStream();
            response.reset();
            response.setHeader("Content-disposition", "attachment; filename="+fname);
            excelUtil.PoiWriteExcel_To2007(resList,os);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ParkLogTb parkLogTb = new ParkLogTb();
        parkLogTb.setOperateUser(nickname);
        parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
        parkLogTb.setOperateType(4);
        parkLogTb.setContent(uin+"("+nickname+")"+"导出了车场日报");
        parkLogTb.setType("order");
        parkLogTb.setParkId(comid);
        saveLogService.saveLog(parkLogTb);
        return null;
    }

}