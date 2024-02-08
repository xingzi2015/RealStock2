package com.xing.RealStock2.service;

import com.xing.RealStock2.context.StockContext;
import com.xing.RealStock2.entity.AccountEntity;
import com.xing.RealStock2.robot.RandomRobot;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@DependsOn("scheduleService")
public class RobotService {

    @Autowired
    private RandomRobot randomRobot;

    private static final List<AccountEntity>  randomUser=new ArrayList<>();

    private Thread thread;

    @PostConstruct
    public void init(){
        for(AccountEntity accountEntity:StockContext.getUserIdMap().values()){
            if(accountEntity.getUserId().startsWith("robot_random")){
                randomUser.add(accountEntity);
            }
        }
        start();
    }

    private void start() {
        thread = new Thread(this::selfRun);
        thread.start();
    }

    public void selfRun(){
        while (true){
            randomUser.stream().forEach(user->{
                randomRobot.operate(user.getUserId());
            });
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
