package com.scheduling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class MyQtzJob {
	public static void main(String[] args) {
		ApplicationContext c=new ClassPathXmlApplicationContext("qtzConfig.xml");

//		qtzJob();
	}

	public void execute(){
		System.out.println(new Date());
		qtzJob();
	}
	public static void qtzJob() {
		// 加载线程池配置类
		ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
		// 线程池
		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ctx.getBean("taskExecutor");

		List<PrintTask2> lists = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			// 线程
			PrintTask2 printTask = (PrintTask2) ctx.getBean("printTask2");
			printTask.setName("Thread " + (i + 1));
			lists.add(printTask);
		}
		for (PrintTask2 pt : lists) {
			// 启动线程
			taskExecutor.execute(pt);
		}
		for (;;) {
			int count = taskExecutor.getActiveCount();
			System.out.println("Active Threads : " + count);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 关闭线程池
			if (count == 0) {
				taskExecutor.shutdown();
				break;
			}
		}
	}
}