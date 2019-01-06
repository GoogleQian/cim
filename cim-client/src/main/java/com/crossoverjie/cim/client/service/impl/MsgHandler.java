package com.crossoverjie.cim.client.service.impl;

import com.crossoverjie.cim.client.client.CIMClient;
import com.crossoverjie.cim.client.config.AppConfiguration;
import com.crossoverjie.cim.client.service.MsgHandle;
import com.crossoverjie.cim.client.service.MsgLogger;
import com.crossoverjie.cim.client.service.RouteRequest;
import com.crossoverjie.cim.client.vo.req.GroupReqVO;
import com.crossoverjie.cim.client.vo.req.P2PReqVO;
import com.crossoverjie.cim.client.vo.res.OnlineUsersResVO;
import com.crossoverjie.cim.common.enums.SystemCommandEnumType;
import com.crossoverjie.cim.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 2018/12/26 11:15
 * @since JDK 1.8
 */
@Service
public class MsgHandler implements MsgHandle {
    private final static Logger LOGGER = LoggerFactory.getLogger(MsgHandler.class);
    @Autowired
    private RouteRequest routeRequest ;

    @Autowired
    private AppConfiguration configuration;

    @Autowired
    private ThreadPoolExecutor executor ;

    @Autowired
    private CIMClient cimClient ;

    @Autowired
    private MsgLogger msgLogger ;

    private boolean aiModel = false ;

    @Override
    public void sendMsg(String msg) {
        if (aiModel){
            aiChat(msg);
        }else {
            normalChat(msg);
        }
    }

    /**
     * 正常聊天
     * @param msg
     */
    private void normalChat(String msg) {
        String[] totalMsg = msg.split(";;");
        if (totalMsg.length > 1) {
            //私聊
            P2PReqVO p2PReqVO = new P2PReqVO();
            p2PReqVO.setUserId(configuration.getUserId());
            p2PReqVO.setReceiveUserId(Long.parseLong(totalMsg[0]));
            p2PReqVO.setMsg(totalMsg[1]);
            try {
                p2pChat(p2PReqVO);
            } catch (Exception e) {
                LOGGER.error("Exception",e);
            }

        } else {
            //群聊
            GroupReqVO groupReqVO = new GroupReqVO(configuration.getUserId(), msg);
            try {
                groupChat(groupReqVO);
            } catch (Exception e) {
                LOGGER.error("Exception",e);
            }
        }
    }

    /**
     * AI model
     * @param msg
     */
    private void aiChat(String msg) {
        msg = msg.replace("吗","") ;
        msg = msg.replace("嘛","") ;
        msg = msg.replace("?","!");
        msg = msg.replace("？","!");
        msg = msg.replace("你","我");
        System.out.println("AI:\033[31;4m" + msg + "\033[0m");
    }

    @Override
    public void groupChat(GroupReqVO groupReqVO) throws Exception {
        routeRequest.sendGroupMsg(groupReqVO);
    }

    @Override
    public void p2pChat(P2PReqVO p2PReqVO) throws Exception {

        routeRequest.sendP2PMsg(p2PReqVO);

    }

    @Override
    public boolean checkMsg(String msg) {
        if (StringUtil.isEmpty(msg)){
            LOGGER.warn("不能发送空消息！");
            return true;
        }
        return false;
    }

    @Override
    public boolean innerCommand(String msg) {

        if (msg.startsWith(":")){
            Map<String, String> allStatusCode = SystemCommandEnumType.getAllStatusCode();

            if (SystemCommandEnumType.QUIT.getCommandType().trim().equals(msg)){
                //关闭系统
                shutdown();
            } else if (SystemCommandEnumType.ALL.getCommandType().trim().equals(msg)){
                printAllCommand(allStatusCode);

            } else if (SystemCommandEnumType.ONLINE_USER.getCommandType().toLowerCase().trim().equals(msg.toLowerCase())){
                //打印在线用户
                printOnlineUsers();

            } else if (msg.startsWith(SystemCommandEnumType.QUERY.getCommandType().trim() + " ")){
                //查询聊天记录
                queryChatHistory(msg);
            }else if (SystemCommandEnumType.AI.getCommandType().trim().equals(msg.toLowerCase())){
                //开启 AI 模式
                aiModel = true ;
                System.out.println("\033[31;4m" + "Hello,我是估值两亿的 AI 机器人！" +  "\033[0m");
            }else if (SystemCommandEnumType.QAI.getCommandType().trim().equals(msg.toLowerCase())){
                //关闭 AI 模式
                aiModel = false ;
                System.out.println("\033[31;4m" + "｡ﾟ(ﾟ´ω`ﾟ)ﾟ｡  AI 下线了！" +  "\033[0m");
            }else {
                printAllCommand(allStatusCode);
            }

            return true ;

        }else {
            return false ;
        }


    }

    /**
     * 查询聊天记录
     * @param msg
     */
    private void queryChatHistory(String msg) {
        String[] split = msg.split(" ") ;
        String res = msgLogger.query(split[1]);
        System.out.println(res);
    }

    /**
     * 打印在线用户
     */
    private void printOnlineUsers() {
        try {
            List<OnlineUsersResVO.DataBodyBean> onlineUsers = routeRequest.onlineUsers();

            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for (OnlineUsersResVO.DataBodyBean onlineUser : onlineUsers) {
                LOGGER.info("userId={}=====userName={}",onlineUser.getUserId(),onlineUser.getUserName());
            }
            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        } catch (Exception e) {
            LOGGER.error("Exception" ,e);
        }
    }

    /**
     * 关闭系统
     */
    private void shutdown() {
        LOGGER.info("系统关闭中。。。。");
        msgLogger.stop();
        executor.shutdown();
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                LOGGER.info("线程池关闭中。。。。");
            }
            cimClient.close();
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException",e);
        }
        System.exit(0);
    }

    private void printAllCommand(Map<String, String> allStatusCode) {
        LOGGER.warn("====================================");
        for (Map.Entry<String, String> stringStringEntry : allStatusCode.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            LOGGER.warn(key + "----->" + value);
        }
        LOGGER.warn("====================================");
    }
}
