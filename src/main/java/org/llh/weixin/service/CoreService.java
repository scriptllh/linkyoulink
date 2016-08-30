package org.llh.weixin.service;

import java.util.*;

import org.apache.commons.collections.map.HashedMap;
import org.llh.weixin.message.resp.Article;
import org.llh.weixin.message.resp.NewsMessage;
import org.llh.weixin.message.resp.TextMessage;
import org.llh.weixin.pojo.BaiduPlace;
import org.llh.weixin.pojo.UserLocation;
import org.llh.weixin.util.BaiduMapUtil;
import org.llh.weixin.util.GameUtil;
import org.llh.weixin.util.MessageUtil;
import org.llh.weixin.util.MySQLUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 核心服务类
 * 
 * @author llh
 * @date 2014-11-19
 */
public class CoreService {
    private  static  Map<String,String> messageFlag=new HashMap<String, String>();

	/**
	 * 处理微信发来的请求
	 * 
	 * @param request
	 * @return xml
	 */
	public static String processRequest(HttpServletRequest request) {
		// xml格式的消息数据
		String respXml = null;
		// 默认返回的文本消息内容
		String respContent = null;


		try {
			// 调用parseXml方法解析请求消息
			Map<String, String> requestMap = MessageUtil.parseXml(request);
			// 发送方帐号
			String fromUserName = requestMap.get("FromUserName");
			// 开发者微信号
			String toUserName = requestMap.get("ToUserName");
			// 消息类型
			String msgType = requestMap.get("MsgType");
			//消息创建时间
			String createTime = requestMap.get("CreateTime");


			// 回复文本消息
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(new Date().getTime());
			textMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);

			// 文本消息
			if (msgType.equals(MessageUtil.REQ_MESSAGE_TYPE_TEXT)) {


				String content = requestMap.get("Content").trim();
				if(messageFlag.get(fromUserName)==null||messageFlag.get(fromUserName)=="false") {
				   if (content.equals("附近")) {
					   respContent = getUsage();
				   }
				   else if(content.equals("聊天"))
				   {
				   	  messageFlag.put(fromUserName,"true");
					   respContent="发送任意文本，我们开始聊天吧！"+"\n"+"回复‘退出聊天’：退出聊天";
				   }

				   else if (content.equals("博客")) {

					   Article article = new Article();
					   article.setDescription("Blog for you and wonderful");
					   article.setPicUrl("http://119.29.254.223:8080/linkyoulink-1.0-SNAPSHOT/images/blog.jpg");
					   article.setTitle("个人博客");
					   article.setUrl("http://121.42.150.134/blog/");
					   List<Article> articleList = new ArrayList<Article>();
					   articleList.add(article);
					   // 回复图文消息
					   NewsMessage newsMessage = new NewsMessage();
					   newsMessage.setToUserName(fromUserName);
					   newsMessage.setFromUserName(toUserName);
					   newsMessage.setCreateTime(new Date().getTime());
					   newsMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_NEWS);
					   newsMessage.setArticles(articleList);
					   newsMessage.setArticleCount(articleList.size());
					   respXml = MessageUtil.messageToXml(newsMessage);

				   }
				   // 周边搜索
				   else if (content.startsWith("附近")) {
					   String keyWord = content.replaceAll("附近", "").trim();
					   // 获取用户最后一次发送的地理位置
					   UserLocation location = MySQLUtil.getLastLocation(request, fromUserName);
					   // 未获取到
					   if (null == location) {
						   respContent = getUsage();
					   } else {
						   // 根据转换后（纠偏）的坐标搜索周边POI
						   List<BaiduPlace> placeList = BaiduMapUtil.searchPlace(keyWord, location.getBd09Lng(), location.getBd09Lat());
						   // 未搜索到POI
						   if (null == placeList || 0 == placeList.size()) {
							   respContent = String.format("/难过，您发送的位置附近未搜索到“%s”信息！", keyWord);
						   } else {
							   List<Article> articleList = BaiduMapUtil.makeArticleList(placeList, location.getBd09Lng(), location.getBd09Lat());
							   // 回复图文消息
							   NewsMessage newsMessage = new NewsMessage();
							   newsMessage.setToUserName(fromUserName);
							   newsMessage.setFromUserName(toUserName);
							   newsMessage.setCreateTime(new Date().getTime());
							   newsMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_NEWS);
							   newsMessage.setArticles(articleList);
							   newsMessage.setArticleCount(articleList.size());
							   respXml = MessageUtil.messageToXml(newsMessage);
						   }
					   }
				   }

				   // 查看游戏帮助
				   else if (content.equalsIgnoreCase("game")) {
					   respContent = GameService.getGameRule();
				   }
				   // 查看游戏战绩
				   else if (content.equalsIgnoreCase("score")) {
					   respContent = GameService.getUserScore(request, fromUserName);
				   }
				   // 如果是4位数字并且无重复
				   else if (GameUtil.verifyNumber(content) && !GameUtil.verifyRepeat(content)) {
					   respContent = GameService.process(request, fromUserName, content);
				   }
				   // 输入的格式错误
				   else if (GameUtil.verifyNumber(content)) {
					   respContent = "请输入4个不重复的数字，例如：0269" + "\n\n" + getMainUsage();
				   }
				   else {
					   respContent = getMainUsage();
				   }
			   }
			   else if(messageFlag.get(fromUserName)=="true")
			   {
				if(content.equals("退出聊天"))
				{
				    messageFlag.put(fromUserName,"false");
					respContent="成功退出聊天系统";
				}
				else
			   	respContent = ChatService.chat(fromUserName,createTime, content);

			   }
			}
			// 地理位置消息
			else if (msgType.equals(MessageUtil.REQ_MESSAGE_TYPE_LOCATION)) {
				// 用户发送的经纬度
				String lng = requestMap.get("Location_Y");
				String lat = requestMap.get("Location_X");
				// 坐标转换后的经纬度
				String bd09Lng = null;
				String bd09Lat = null;
				// 调用接口转换坐标
				UserLocation userLocation = BaiduMapUtil.convertCoord(lng, lat);
				if (null != userLocation) {
					bd09Lng = userLocation.getBd09Lng();
					bd09Lat = userLocation.getBd09Lat();
				}
				// 保存用户地理位置
				MySQLUtil.saveUserLocation(request, fromUserName, lng, lat, bd09Lng, bd09Lat);

				StringBuffer buffer = new StringBuffer();
				buffer.append("[愉快]").append("成功接收您的位置！").append("\n\n");
				buffer.append("您可以输入搜索关键词获取周边信息了，例如：").append("\n");
				buffer.append("        附近ATM").append("\n");
				buffer.append("        附近KTV").append("\n");
				buffer.append("        附近厕所").append("\n");
				buffer.append("必须以“附近”两个字开头！");
				respContent = buffer.toString();
			}
			// 事件推送
			else if (msgType.equals(MessageUtil.REQ_MESSAGE_TYPE_EVENT)) {
				// 事件类型
				String eventType = requestMap.get("Event");
				// 关注
				if (eventType.equals(MessageUtil.EVENT_TYPE_SUBSCRIBE)) {
					String res=getSubscribeMsg()+getMainUsage();
					respContent = res;
				}
			}


			if (null != respContent) {
				// 设置文本消息的内容
				textMessage.setContent(respContent);
				// 将文本消息对象转换成xml
				respXml = MessageUtil.messageToXml(textMessage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return respXml;
	}

	/**
	 * 关注提示语
	 * 
	 * @return
	 */
	private static String getSubscribeMsg() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("少侠你终于来了！").append("\n\n");
		buffer.append("我见少侠你骨骼惊奇，一看便是练武奇才").append("\n\n");
		buffer.append("linkyoulink").append("\n\n");
		return buffer.toString();
	}

	/**
	 * 附近周边搜索
	 * @return
	 */
	private  static  String getLocationMsg()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("您是否有过出门在外四处找ATM或厕所的经历？").append("\n\n");
		buffer.append("您是否有过出差在外搜寻美食或娱乐场所的经历？").append("\n\n");
		buffer.append("周边搜索为您的出行保驾护航，为您提供专业的周边生活指南，回复“附近”开始体验吧！");
		return buffer.toString();
	}

	/**
	 * 附近搜索使用说明
	 * 
	 * @return
	 */
	private static String getUsage() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("周边搜索使用说明").append("\n\n");
		buffer.append("1）发送地理位置").append("\n");
		buffer.append("点击窗口底部的“+”按钮，选择“位置”，点“发送”").append("\n\n");
		buffer.append("2）指定关键词搜索").append("\n");
		buffer.append("格式：附近+关键词\n例如：附近ATM、附近KTV、附近厕所");
		return buffer.toString();
	}

	/**
	 * 主菜单
	 * @return
	 */
	private static  String getMainUsage()
	{
		StringBuffer buffer=new StringBuffer();
		buffer.append("-----主菜单----").append("\n");
		buffer.append("周边搜索请回复：附近").append("\n");
		buffer.append("玩游戏请回复：game").append("\n");
		buffer.append("查看博客请回复：博客").append("\n");
		buffer.append("机器人聊天请回复：聊天");
		return buffer.toString();

	}
}
