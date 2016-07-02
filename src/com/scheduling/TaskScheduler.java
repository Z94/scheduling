/**
 * 项   目  名：NGC
 * 包          名：cn.mopon.cec.task
 * 文   件  名：TaskScheduler.java
 * 版本信息：V1.1
 * 日          期：2015年11月26日-下午5:11:07
 * Copyright (c) 2015-2015深圳市泰久信息系统股份有限公司
 * 
 */
package com.scheduling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.mopon.cec.core.entity.Cinema;
import cn.mopon.cec.core.entity.TicketOrder;
import cn.mopon.cec.core.model.FilmSyncModel;
import cn.mopon.cec.core.service.ChannelShowService;
import cn.mopon.cec.core.service.CinemaService;
import cn.mopon.cec.core.service.FilmService;
import cn.mopon.cec.core.service.ShowService;
import cn.mopon.cec.core.service.StatService;
import cn.mopon.cec.core.service.TaskExecuteService;
import cn.mopon.cec.core.service.TaskScheduleService;
import cn.mopon.cec.core.service.TicketOrderService;
import cn.mopon.cec.core.service.TicketVoucherService;
import cn.mopon.cec.core.util.DateUtil;
import cn.mopon.cec.core.util.StringUtil;
import coo.base.util.DateUtils;
import coo.core.util.SpringUtils;

/**
 * @author ruilu.li
 * @version [NGC V1.1, 2015年11月26日]
 * @备注：
 */
public class TaskScheduler {
	private Logger log = LoggerFactory.getLogger(getClass());
	private final int intervalTime = 5;
	private static int nThreads = 8;
	private static String ip = "";
	static {
		ip = StringUtil.getLocalIpStr();
		nThreads = (Runtime.getRuntime().availableProcessors()) * 2;
	}

	/**
	 * 定时线程测试类,一个小时运行一次，用于检测线程类是否异常
	 * 
	 * @author ruilu.li
	 * @time 下午5:13:01
	 */
	public void doTest() {
		log.info("TaskScheduler.dotest>>>>>>>>>>>>>>>>>>>>>>>>ip:" + ip
				+ "|now:" + DateUtil.getNowDate());
	}

	/**
	 * 定时同步影厅和座位。
	 */
	public void syncHalls() {
		try {
			log.info("syncHalls.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				CinemaService cinemaService = SpringUtils
						.getBean("cinemaService");
				List<HallSyncTask> tasks = new ArrayList<>();
				for (String cinemaId : cinemaService.getCinemaIds()) {
					tasks.add(new HallSyncTask(cinemaId));
				}
				ExecutorService hallSyncTaskExecutor = Executors
						.newFixedThreadPool(nThreads);
				hallSyncTaskExecutor.invokeAll(tasks);
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("syncHalls.ip:" + ip + "|end<<<cost time(ms):"
						+ (System.nanoTime() - t1) * 0.000001);
			}
		} catch (Exception e) {
			log.error("syncHalls.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时同步影片。
	 */
	public void syncMovicesAndFilms() {
		try {
			log.info("syncMovicesAndFilms.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				FilmService filmService = SpringUtils.getBean("filmService");
				filmService.syncFilms(new FilmSyncModel());
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("syncMovicesAndFilms.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("syncMovicesAndFilms.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时同步排期：新的算法，需要配合缓存一起使用
	 */
	public void syncShows() {
		try {
			log.info("syncShows.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			log.info("TaskScheduler.syncShows|started>>>t:"
					+ DateUtil.getNowDate());
			CinemaService cinemaService = SpringUtils.getBean("cinemaService");
			List<String> cinemaIds = cinemaService.getCinemaIds();
			List<ShowSyncTask> tasks = new ArrayList<>();
			for (String cinemaId : cinemaIds) {
				tasks.add(new ShowSyncTask(cinemaId));
			}
			ExecutorService showSyncTaskExecutor = Executors
					.newFixedThreadPool(nThreads);
			showSyncTaskExecutor.invokeAll(tasks);
			log.info("syncShows.ip:" + ip + "|end<<<cost time(ms):"
					+ (System.nanoTime() - t1) * 0.000001);
		} catch (Exception e) {
			log.error("syncShows.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时清理超过保留天数的排期（包括影院排期、排期同步日志、渠道排期）。
	 */
	public void cleanShows() {
		try {
			log.info("cleanShows.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				CinemaService cinemaService = SpringUtils.getBean("cinemaService");
				List<ShowCleanTask> tasks = new ArrayList<>();
				for (Cinema cinema : cinemaService
						.getProvideTicketEnabledCinemas()) {
					tasks.add(new ShowCleanTask(cinema.getId()));
				}
				ExecutorService showCleanTaskExecutor = Executors
						.newFixedThreadPool(nThreads);
				showCleanTaskExecutor.invokeAll(tasks);
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("cleanShows.ip:" + ip + "|end<<<cost time(ms):"
						+ (System.nanoTime() - t1) * 0.000001);
			}
		} catch (Exception e) {
			log.error("cleanShows.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时清理失效且没有产生订单的渠道排期。
	 */
	public void cleanChannelShows() {
		try {
			log.info("cleanChannelShows.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				ChannelShowService channelShowService = SpringUtils
						.getBean("channelShowService");
				channelShowService.cleanChannelShows();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("cleanChannelShows.ip:" + ip + "|end<<<cost time(ms):"
						+ (System.nanoTime() - t1) * 0.000001);
			}
		} catch (Exception e) {
			log.error("cleanChannelShows.ip:" + ip + "异常", e);
		}
	}

	/**
	 * 定时清理取消的选座票订单。
	 */
	public void cleanExpiredTicketOrder() {
		try {
			log.info("cleanExpiredTicketOrder.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				TicketOrderService ticketOrderService = SpringUtils
						.getBean("ticketOrderService");
				ticketOrderService.cleanCanceledTicketOrder();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("cleanExpiredTicketOrder.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error(
					"cleanExpiredTicketOrder.ip:" + ip + "定时清理取消的选座票订单时发生异常。",
					e);
		}
	}

	/**
	 * 定时更新过期排期。
	 */
	public void updateExpiredShows() {
		try {
			log.info("updateExpiredShows.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				ShowService showService = SpringUtils.getBean("showService");
				showService.updateExpiredShows();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("updateExpiredShows.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("updateExpiredShows.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时更新过期渠道排期。
	 */
	public void updateExpiredChannelShows() {
		try {
			log.info("updateExpiredChannelShows.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				ChannelShowService channelShowService = SpringUtils
						.getBean("channelShowService");
				channelShowService.updateExpiredChannelShows();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("updateExpiredChannelShows.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("updateExpiredChannelShows.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时更新过期选座票订单。
	 */
	public void updateExpiredTicketOrder() {
		try {
			log.info("updateExpiredTicketOrder.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				TicketOrderService ticketOrderService = SpringUtils
						.getBean("ticketOrderService");
				ticketOrderService.updateExpiredTicketOrder();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("updateExpiredTicketOrder.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("updateExpiredTicketOrder.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时更新过期选座票凭证。
	 */
	public void updateExpiredTicketVoucher() {
		try {
			log.info("updateExpiredTicketVoucher.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				TicketVoucherService ticketVoucherService = SpringUtils
						.getBean("ticketVoucherService");
				ticketVoucherService.updateExpiredTicketVoucher();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("updateExpiredTicketVoucher.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("updateExpiredTicketVoucher.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时处理异常订单。
	 */
	public void processAbnormalOrders() {
		try {
			log.info("processAbnormalOrders.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				TicketOrderService ticketOrderService = SpringUtils
						.getBean("ticketOrderService");
				List<TicketOrderSyncTask> tasks = new ArrayList<>();
				List<String> abnormalOrderIds = ticketOrderService.searchAbnormalTicketOrderIdsBySql();
				for (String abnormalOrderId : abnormalOrderIds) {
					tasks.add(new TicketOrderSyncTask(abnormalOrderId));
				}
				ExecutorService ticketOrderSyncTaskExecutor = Executors
						.newFixedThreadPool(nThreads);
				ticketOrderSyncTaskExecutor.invokeAll(tasks);
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("processAbnormalOrders.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("processAbnormalOrders.ip:" + ip + "|error<<< e" + e);
		}
	}

	/**
	 * 定时发送异常订单处理失败邮件。
	 */
	public void sendProcessAbnormalOrdersFailMail() {
		try {
			log.info("sendProcessAbnormalOrdersFailMail.ip:" + ip
					+ "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				TicketOrderService ticketOrderService = SpringUtils
						.getBean("ticketOrderService");
				ticketOrderService.sendAbnormalOrderFailListMail();
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("sendProcessAbnormalOrdersFailMail.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("sendProcessAbnormalOrdersFailMail.ip:" + ip
					+ "|error<<< e" + e);
		}
	}

	/**
	 * 定时生成选座票订单日统计记录。
	 */
	public void syncTicketOrderDetail() {
		try {
			log.info("syncTicketOrderDetail.ip:" + ip + "|started>>>");
			long t1 = System.nanoTime();
			TaskExecuteService taskSyncService = SpringUtils
					.getBean("taskExecuteService");
			TaskScheduleService taskScheduleService = SpringUtils
					.getBean("taskScheduleService");
			if (taskSyncService.isTaskNotRunning(Thread.currentThread()
					.getStackTrace()[1].getMethodName(), intervalTime)) {
				StatService statService = SpringUtils.getBean("statService");
				Date date = DateUtils.getPrevDay();
				statService.deleteExistStatDateOrder(date);
				statService.syncTicketOrderDetail(date);
				taskScheduleService.delTaskScheduleByName(Thread
						.currentThread().getStackTrace()[1].getMethodName());
				log.info("syncTicketOrderDetail.ip:" + ip
						+ "|end<<<cost time(ms):" + (System.nanoTime() - t1)
						* 0.000001);
			}
		} catch (Exception e) {
			log.error("syncTicketOrderDetail.ip:" + ip + "|error<<< e" + e);
		}
	}
}
