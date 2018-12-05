package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import parkingos.com.bolink.models.ShortMessageTb;
import parkingos.com.bolink.service.AddValueService;
import parkingos.com.bolink.service.SupperSearchService;

import java.util.Map;

@Service
public class AddValueServiceImpl implements AddValueService {

    Logger logger = LoggerFactory.getLogger(AddValueServiceImpl.class);
    @Autowired
    private SupperSearchService supperSearchService;

    @Override
    public JSONObject getMessage(Map<String, String> reqParameterMap) {
        ShortMessageTb shortMessageTb = new ShortMessageTb();
        shortMessageTb.setState(1);
        JSONObject result = supperSearchService.supperSearch(shortMessageTb, reqParameterMap);
        return result;
    }
}