package org.llh.weixin.service;

import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.llh.weixin.pojo.Game;
import org.llh.weixin.pojo.GameRound;
import org.llh.weixin.util.GameUtil;
import org.llh.weixin.util.MySQLUtil;

/**
 * 猜数字游戏业务类
 * 
 * @author llh
 * @date 2014-11-21
 */
public class GameService {
	/**
	 * “猜数字”游戏处理逻辑
	 * 
	 * @param request 请求对象
	 * @param openId 用户的OpenID
	 * @param content 消息内容
	 * @return
	 */
	public static String process(HttpServletRequest request, String openId, String content) {
		// 获取用户最近一次创建的游戏
		Game game = MySQLUtil.getLastGame(request, openId);
		// 本局游戏的正确答案
		String gameAnswer = null;
		// 本回合的猜测结果
		String guessResult = null;
		// 本局游戏的id
		int gameId = -1;
		// 是否是新的一局（默认为false）
		boolean newGameFlag = (null == game || 0 != game.getGameStatus());
		// 新的一局
		if (newGameFlag) {
			// 生成不重复的4位随机数作为答案
			gameAnswer = GameUtil.generateRandNumber();
			// 计算本回合“猜数字”的结果（xAyB）
			guessResult = GameUtil.guessResult(gameAnswer, content);
			// 创建game
			game = new Game();
			game.setOpenId(openId);
			game.setGameAnswer(gameAnswer);
			game.setCreateTime(GameUtil.getStdDateTime());
			game.setGameStatus(0);
			// 保存game并获取id
			gameId = MySQLUtil.saveGame(request, game);
		}
		// 不是新的一局
		else {
			gameAnswer = game.getGameAnswer();
			guessResult = GameUtil.guessResult(game.getGameAnswer(), content);
			gameId = game.getGameId();
		}
		// 保存当前游戏回合
		GameRound gameRound = new GameRound();
		gameRound.setGameId(gameId);
		gameRound.setOpenId(openId);
		gameRound.setGuessNumber(content);
		gameRound.setGuessTime(GameUtil.getStdDateTime());
		gameRound.setGuessResult(guessResult);
		MySQLUtil.saveGameRound(request, gameRound);
		// 查询本局游戏的所有回合
		List<GameRound> roundList = MySQLUtil.findAllRoundByGameId(request, gameId);
		// 遍历游戏回合，组装给用户回复的消息
		StringBuffer buffer = new StringBuffer();
		buffer.append("查看玩法请回复：game").append("\n");
		buffer.append("查看战绩请回复：score").append("\n");
		for (int i = 0; i < roundList.size(); i++) {
			gameRound = roundList.get(i);
			buffer.append(String.format("第%d回合： %s    %s", i + 1, gameRound.getGuessNumber(), gameRound.getGuessResult())).append("\n");
		}
		// 猜对
		if ("4A0B".equals(guessResult)) {
			buffer.append("正确答案：").append(gameAnswer).append("\n");
			buffer.append("恭喜您猜对了！[强]").append("\n");
			buffer.append("重新输入4个不重复的数字开始新的一局。");
			MySQLUtil.updateGame(request, gameId, 1, GameUtil.getStdDateTime());
		}
		// 10回合仍没猜对
		else if (roundList.size() >= 10) {
			buffer.append("正确答案：").append(gameAnswer).append("\n");
			buffer.append("唉，10回合都没猜出来，本局结束！[流泪]").append("\n");
			buffer.append("重新输入4个不重复的数字开始新的一局。");
			MySQLUtil.updateGame(request, gameId, 2, GameUtil.getStdDateTime());
		}
		// 本回合没猜对
		else {
			buffer.append("请再接再励！");
		}
		return buffer.toString();
	}

	/**
	 * “猜数字”游戏玩法
	 * 
	 * @return String
	 */
	public static String getGameRule() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("《猜数字游戏玩法》").append("\n");
		buffer.append("系统设定一个没有重复数字的4位数，由玩家来猜，每局10次机会。").append("\n");
		buffer.append("每猜一次，系统会给出猜测结果xAyB，x表示数字与位置均正确的数的个数，y表示数字正确但位置不对的数的个数。").append("\n");
		buffer.append("玩家根据猜测结果xAyB一直猜，直到猜中(4A0B)为止。").append("\n");
		buffer.append("如果10次都没猜中，系统会公布答案，游戏结束。").append("\n");
		buffer.append("玩家任意输入一个没有重复数字的4位数即开始游戏，例如：7890");
		return buffer.toString();
	}

	/**
	 * “猜数字”游戏战绩
	 * 
	 * @param request 请求对象
	 * @param openId 用户的OpenID
	 * @return
	 */
	public static String getUserScore(HttpServletRequest request, String openId) {
		StringBuffer buffer = new StringBuffer();
		HashMap<Integer, Integer> scoreMap = MySQLUtil.getScoreByOpenId(request, openId);
		if (null == scoreMap || 0 == scoreMap.size()) {
			buffer.append("您还没有玩过猜数字游戏！").append("\n");
			buffer.append("请输入4个不重复的数字开始新的一局，例如：0269");
		} else {
			buffer.append("您的游戏战绩如下：").append("\n");
			for (Integer status : scoreMap.keySet()) {
				if (0L == status) {
					buffer.append("游戏中：").append(scoreMap.get(status)).append("\n");
				} else if (1L == status) {
					buffer.append("胜利局数：").append(scoreMap.get(status)).append("\n");
				} else if (2L == status) {
					buffer.append("失败局数：").append(scoreMap.get(status)).append("\n");
				}
			}
			buffer.append("请输入4个不重复的数字开始新的一局，例如：0269");
		}
		return buffer.toString();
	}
}
