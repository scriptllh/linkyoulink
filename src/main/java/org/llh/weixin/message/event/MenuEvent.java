package org.llh.weixin.message.event;

/**
 * �Զ���˵��¼�
 * 
 * @author llh
 * @date 2014-11-04
 */
public class MenuEvent extends BaseEvent {
	// �¼�KEYֵ�����Զ���˵��ӿ���KEYֵ��Ӧ
	private String EventKey;

	public String getEventKey() {
		return EventKey;
	}

	public void setEventKey(String eventKey) {
		EventKey = eventKey;
	}
}