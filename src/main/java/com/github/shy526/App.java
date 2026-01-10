package com.github.shy526;


import com.github.shy526.caimogu.CaiMoGuHelp;
import com.github.shy526.github.GithubHelp;
import com.github.shy526.github.LocalFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Hello world!
 */
@Slf4j
public class App {
    public static void main(String[] args) {
        log.error("启动踩蘑菇获取影响力任务");
        String caiMoGuToken = System.getenv("CAI_MO_GU_TOKEN");
        if (!caiMoGuToken.contains("cmg_token=")) {
            caiMoGuToken = "cmg_token=" + caiMoGuToken;
        }
        int clout = CaiMoGuHelp.getClout(caiMoGuToken);
        String nickname = CaiMoGuHelp.getNickname(caiMoGuToken);
        log.error("当前用户:{},影响力:{}", nickname, clout);
        if (clout == -1) {
            log.error(Base64.encodeBase64String(caiMoGuToken.getBytes()));
            log.error("CAI_MO_GU_TOKEN 已经失效 重新获取(浏览器中f12 应用程序 Cookie 中的 cmg_token )");
            return;
        }
        log.error("配置设置正常");

        String gameIdsFileName = "gameIds.txt";
        String acIdsFileName = "acIds.txt";
        String postIdsFileName = "postIds.txt";
        String runFileName = "run.txt";


        Set<String> run = LocalFile.readFile(runFileName);
        LocalDate current = LocalDate.now();
        ;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Iterator<String> iterator = run.iterator();
        String dateStr = iterator.hasNext() ? iterator.next() : null;
        if (dateStr != null) {
            LocalDate date = LocalDate.parse(dateStr, formatter);
            if (current.isEqual(date) || current.isBefore(date)) {
                log.error("今日任务已经执行,若要重复执行请删run.txt");
                return;
            }
        }

        Set<String> ids = LocalFile.readFile(gameIdsFileName);
        if (ids.isEmpty()) {
            log.error("生成gameId");
            ids = CaiMoGuHelp.ScanGameIds();
            LocalFile.writeFile(gameIdsFileName, ids);
        }

        Set<String> acIds = LocalFile.readFile(acIdsFileName);
        Map<Integer, Set<String>> replyGroup = new HashMap<>();
        if (acIds.isEmpty()) {
            //文件不存在时主动查寻回复中所有已经回复过的GameId
            replyGroup = CaiMoGuHelp.getReplyGroup(caiMoGuToken);
            Set<String> acIdSource = replyGroup.get(2);
            acIds = acIdSource == null ? acIds : acIdSource;
            LocalFile.writeFile(acIdsFileName, acIds);
        }

        //去掉交集
        if (!acIds.isEmpty()) {
            ids.removeAll(acIds);
        }
        if (ids.isEmpty()) {
            //无可用id时重新扫描
            ids = CaiMoGuHelp.ScanGameIds();
        }
        if (!acIds.isEmpty()) {
            ids.removeAll(acIds);
        }
        if (!ids.isEmpty()) {
            int trueFlag = 0;
            for (String id : ids) {
                int s = CaiMoGuHelp.actSore(id, caiMoGuToken);
                if (s == 1) {
                    trueFlag++;
                    acIds.add(id);
                    log.error("评价成功 " + id);
                } else if (s == 0) {

                    acIds.add(id);
                    log.error("重复评价 " + id);
                } else {
                    log.error("无法正常评论游戏");
                    break;
                }

            }
            log.error("成功评价游戏数量:{}", trueFlag);
        }
        LocalFile.writeFile(acIdsFileName, acIds);
        //这里开始回复帖子
        Set<String> postIds = LocalFile.readFile(postIdsFileName);
        if (postIds.isEmpty()) {
            //文件不存在时主动查寻回复中所有已经回复过的GameId
            if (replyGroup.isEmpty()) {
                replyGroup = CaiMoGuHelp.getReplyGroup(caiMoGuToken);
            }
            Set<String> postIdS = replyGroup.get(1);
            postIds = postIdS == null ? postIds : postIdS;
            LocalFile.writeFile(postIdsFileName, postIds);
        }

        List<String> qzIds = Arrays.asList("449", "329", "369", "383", "282", "466");
        int acPostNum = CaiMoGuHelp.exeAcPost(qzIds, postIds, caiMoGuToken);
        log.error("成功评论帖子数量:{}", acPostNum);
        int clout2 = CaiMoGuHelp.getClout(caiMoGuToken);
        log.error("本次任务共获取影响力:{}", clout2 - clout);
        LocalFile.writeFile(postIdsFileName, postIds);
        HashSet<String> temp = new HashSet<>();
        temp.add(formatter.format(current));
        LocalFile.writeFile(runFileName, temp);
    }


}
