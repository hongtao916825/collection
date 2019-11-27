package com.jibug.cetty.sample.web.controller;


import com.alibaba.fastjson.JSONObject;
import com.basic.support.commons.business.logger.LogUtil;
import com.jibug.cetty.sample.common.ClassPathTxt;
import com.jibug.cetty.sample.schedule.tasks.MlTask;
import com.jibug.cetty.sample.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/len")
public class TaskRunController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private MlTask mlTask;
    @Autowired
    private Executor asyncServiceExecutor;
    @Autowired
    private ClassPathTxt classPathTxt;


    @RequestMapping("/run")
    public String runTask(){
        LogUtil.info("数据获取 服务 开启 runTask");
        taskService.aSyncTaskRun();
        return "Data acquisition is running";
    }


    @GetMapping("/count")
    public String runCountResult(){
        asyncServiceExecutor.execute(()->{
            mlTask.getMxTheResult();
            mlTask.getBrTheResult();
        });
        return "MX and BR , Result the task is being executed  ...";
    }

    @GetMapping("/info")
    public JSONObject getInfo(){
        JSONObject object = new JSONObject();
        object.put("read","功能列表");
        object.put("/len/run","立即开始数据收集");
        object.put("/len/count","立即开始数据统计分析");
        object.put("/len/export/mx/{page:d+}","墨西哥分页下载结果，分页仅支持30W（大括号请用页码参数代替）");
        object.put("/len/export/br/{page:\\d+}","巴西分页下载结果，分页仅支持30W（大括号请用页码参数代替）");
        object.put("/len/status","系统运行状态");
        object.put("/len/info","功能URL列表");
        object.put("msg","外贸商品数据收集，仅作内部分析使用，不含用户数据");

        return object;
    }

    @GetMapping("/status")
    public String getStatus(){
//        Map<String, String> stausMap = classPathTxt.getConfigMap();
//        StringBuffer buffer = new StringBuffer();
//        buffer.append(stausMap.get("status"))
//                .append("|")
//                .append(stausMap.get("msg"));


        return "status:true";
    }


}
