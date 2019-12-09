package com.jibug.cetty.sample.schedule;

import com.basic.support.commons.business.logger.LogUtil;
import com.jibug.cetty.sample.common.ClassPathTxt;
import com.jibug.cetty.sample.schedule.tasks.MlTask;
import com.jibug.cetty.sample.service.DataExportService;
import com.jibug.cetty.sample.service.support.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 多线程 定时任务
 */
@Component
@EnableScheduling   // 1.开启定时任务
@EnableAsync
public class MultithreadScheduleTask {

    @Autowired
    private MlTask mlTask;

    @Autowired
    private CrawlerService crawlerService;
    @Autowired
    private ClassPathTxt classPathTxt;
    @Autowired
    private DataExportService dataExportService;

    @Scheduled(fixedDelay = 50*1000)
    public void task1(){
        try {
            LogUtil.warn("saveMx run");
            mlTask.saveMx();
        } catch (Exception e) {
            LogUtil.error("saveMx：任务执行错误");
        }

    }

    @Scheduled(fixedDelay = 70*1000)
    public void task2(){
        try {
            LogUtil.warn("saveBr run");
            mlTask.saveBr();
        } catch (Exception e) {
            LogUtil.error("saveBr：任务执行错误");
        }

    }


    @Scheduled(cron = "0 0 1 * * *")
    public void task3(){
        try {
            LogUtil.warn("定时 服务 开启 抓取 run");
            crawlerService.start();
        } catch (Exception e) {
            LogUtil.error("定时任务：抓取：任务执行错误");
        }

    }

    @Scheduled(cron = "0 0 0 * * *")
    public void task4(){
        try {
//            LogUtil.warn("定时 服务 开启 抓取 run");
            mlTask.delOldDataMx();
        } catch (Exception e) {
//            LogUtil.error("定时任务：抓取：任务执行错误");
        }

    }

    @Scheduled(cron = "0 0 0 * * *")
    public void task5(){
        try {
//            LogUtil.warn("定时 服务 开启 抓取 run");
            mlTask.delOldDataBr();
        } catch (Exception e) {
//            LogUtil.error("定时任务：抓取：任务执行错误");
        }

    }

//    @Scheduled(cron = "0 0 4 1,4,7,10,13,16,19,22,25,28 * *")
//    public void task3(){
////        try {
////            if(classPathTxt.isInvalid()){
////                LogUtil.warn("定时 服务 开启 墨西哥结果处理 run");
////                mlTask.getMxTheResult();
////            }else{
////                LogUtil.warn("定时 服务 异常 墨西哥结果处理 run");
////            }
////        } catch (Exception e) {
////            LogUtil.error("定时任务：墨西哥结果处理：任务执行错误");
////        }
//
//    }

//    @Scheduled(cron = "0 30 4 1,4,7,10,13,16,19,22,25,28 * *")
//    public void task4(){
////        try {
////            if(classPathTxt.isInvalid()){
////                LogUtil.warn("定时 服务 开启 巴西结果处理 run");
////                mlTask.getBrTheResult();
////            }else{
////                LogUtil.warn("定时 服务 异常 巴西结果处理 run");
////            }
////        } catch (Exception e) {
////            LogUtil.error("定时任务：巴西结果处理：任务执行错误");
////        }
//
//    }

//    @Scheduled(cron = "0 0 6 1,4,7,10,13,16,19,22,25,28 * *")
//    public void task5(){
////        try {
////            if(classPathTxt.isInvalid()){
////                LogUtil.warn("定时 服务 开启 导出MX run");
////                dataExportService.exportMx(null);
////            }else{
////                LogUtil.warn("定时 服务 异常 导出MX run");
////            }
////        } catch (Exception e) {
////            LogUtil.error("定时任务：导出MX：任务执行错误");
////        }
//
//    }

//    @Scheduled(cron = "0 30 6 1,4,7,10,13,16,19,22,25,28 * *")
//    public void task6(){
////        try {
////            if(classPathTxt.isInvalid()){
////                LogUtil.warn("定时 服务 开启 导出BR run");
////                dataExportService.exportBr(null);
////            }else{
////                LogUtil.warn("定时 服务 异常 导出BR run");
////            }
////        } catch (Exception e) {
////            LogUtil.error("定时任务：导出BR：任务执行错误");
////        }
//
//    }



}
