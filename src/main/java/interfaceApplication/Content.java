package interfaceApplication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import esayhelper.StringHelper;
import esayhelper.TimeHelper;
import esayhelper.jGrapeFW_Message;
import model.ContentModel;
import rpc.execRequest;
import session.session;

@SuppressWarnings("unchecked")
public class Content {
	private ContentModel content = new ContentModel();
	private HashMap<String, Object> defmap = new HashMap<>();
	private JSONObject _obj = new JSONObject();
	private static String info = "";
	static {
		session session = new session();
		info = session.get("username").toString();
	}

	public Content() {
		defmap.put("subName", null);
		defmap.put("image", "1,2");
		defmap.put("ownid", 0);
		defmap.put("manageid", "");
		defmap.put("content", "");
		defmap.put("fatherid", 0);
		defmap.put("ogid", 0);
		defmap.put("attrid", 0);
		defmap.put("sort", 0);
		defmap.put("isdelete", 0);
		defmap.put("isvisble", 0);
		defmap.put("souce", null);
		defmap.put("state", 0);
		defmap.put("substate", 0);
		defmap.put("slevel", 0);
		defmap.put("readCount", 0);
		defmap.put("thirdsdkid", "");
		defmap.put("time", TimeHelper.nowSecond() + "");
	}

	/**
	 * 修改文章
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String EditArticle(String oid, String contents) {
		JSONObject infos = JSONHelper.string2json(contents);
		if (infos.containsKey("content")) {
			infos.put("content", encode(infos.get("content").toString()));
		}
		return content.resultMessage(content.UpdateArticle(oid, infos), "文章更新成功");
	}

	/**
	 * 删除文章
	 * 
	 * @param oid
	 *            唯一识别符
	 * @return
	 */
	public String DeleteArticle(String oid) {
		return content.resultMessage(content.DeleteArticle(oid), "文章删除成功");
	}

	/**
	 * 显示所有的文章
	 * 
	 * @return
	 */
	// public String ShowArticle() {
	// _obj.put("records", content.select());
	// return StringEscapeUtils.unescapeJava(content.resultMessage(0,
	// _obj.toString()));
	// }

	/**
	 * 获取最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewArc() {
		_obj.put("records", content.findnew());
		return content.resultMessage(0, _obj.toString());
	}

	/**
	 * 根据oid显示文章
	 * 
	 * @return
	 */
	public String findArticle(String oid) {
		_obj.put("records", content.select(oid));
		return StringEscapeUtils.unescapeJava(content.resultMessage(0, _obj.toString()));
	}

	/**
	 * 设置内容组
	 * 
	 * @param oid
	 * @param ogid
	 * @return
	 */
	public String SetGroup(String oid, String ogid) {
		return content.resultMessage(content.setGroup(oid, ogid), "设置内容组成功");
	}

	/**
	 * 根据用户查找文章
	 * 
	 * @param uid
	 * @return
	 */
	public String findbyUser(String uid) {
		_obj.put("records", content.searchByUid(uid));
		return StringEscapeUtils.unescapeJava(content.resultMessage(0, _obj.toString()));
	}

	/**
	 * 文章搜索
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticle(String condString) {
		_obj.put("records", content.search(JSONHelper.string2json(condString)));
		return StringEscapeUtils.unescapeJava(content.resultMessage(0, _obj.toString()));
	}

	/**
	 * 分页
	 * 
	 * @param idx
	 *            当前页
	 * @param pageSize
	 *            每页显示量
	 * @return
	 */
	public String Page(int idx, int pageSize) {
		_obj.put("records", content.page(idx, pageSize));
		return content.resultMessage(0, _obj.toString());
	}

	public String PageBy(int idx, int pageSize, String contents) {
		_obj.put("records", content.page(idx, pageSize, JSONHelper.string2json(contents)));
		return content.resultMessage(0, _obj.toString());
	}

	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowByGroupId(String ogid) {
		_obj.put("records", content.findByGroupID(ogid));
		return jGrapeFW_Message.netMSG(0, _obj.toString());
	}

	/**
	 * 修改排序值
	 * 
	 * @param oid
	 *            文章id
	 * @param sortNo
	 *            排序值
	 * @return 显示修改之前的数据
	 */
	public String sort(String oid, int sortNo) {
		return content.resultMessage(content.updatesort(oid, sortNo), "排序值修改成功");
	}

	// 统计总阅读量(条件？？）
	public String CountRead() {
		return null;
	}

	/**
	 * 发布文章
	 * 
	 * @param ArticleInfo
	 * @return
	 */
	public String PublishArticle(String ArticleInfo) {
		JSONObject object = JSONHelper.string2json(ArticleInfo);
		// 获取当前站点
		String wbid = JSONHelper.string2json(info).get("currentWeb").toString();
		object = getwbid(wbid, object);
		object = content.AddMap(defmap, JSONHelper.string2json(ArticleInfo));
		object.put("content", encode(object.get("content").toString()));
		return content.resultMessage(content.insert(object), "文章发布成功");
	}

	//编码
	public String encode(String htmlContent) {
		String content=null;
		try {
			content = URLEncoder.encode(htmlContent,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return content;
	}
	public JSONObject getwbid(String wbid,JSONObject object) {
		List<String> list = new ArrayList<>();
		if (object.containsKey("wbid")) {
			String wbsid = object.get("wbid").toString();
			if (wbsid.contains(",")) {
				String[] value = wbsid.split(",");
				for (int i = 0; i < value.length; i++) {
					list.add(value[i]);
				}
			}else{
				list.add(wbid);
			}
			list.add(wbid);
			wbid = StringHelper.join(list);
		}
		object.put("wbid", wbid);
		return object;
	}
	/**
	 * 删除指定栏目下的文章
	 * 
	 * @param oid
	 *            文章id
	 * @param ogid
	 *            栏目id
	 * @return
	 */
	public String DeleteByOgid(String oid, String ogid) {
		return content.resultMessage(content.deleteByOgID(oid, ogid), "该栏目下文章删除成功");
	}

	/**
	 * 删除指定站点下的文章
	 * 
	 * @param oid
	 *            文章id
	 * @param wbid
	 *            站点id
	 * @return
	 */
	public String DeleteByWbID(String oid, String wbid) {
		return content.resultMessage(content.deleteByWbID(oid, wbid), "该站点下文章删除成功");
	}

	public String SetTempId(String oid, String tempid) {
		return content.resultMessage(content.setTempId(oid, tempid), "设置模版成功");
	}

	public String Setfatherid(String oid, String fatherid) {
		return content.resultMessage(content.setfatherid(oid, fatherid), "设置模版成功");
	}

	public String Setslevel(String oid, String slevel) {
		return content.resultMessage(content.setslevel(oid, slevel), "设置密级成功");
	}

	// 审核
	public String Review(String oid, String managerid, String state) {
		return content.resultMessage(content.review(oid, managerid, state), "审核文章操作成功");
	}

	public String BatchDelete(String oid) {
		return content.resultMessage(content.delete(oid.split(",")), "批量删除成功");
	}
}
