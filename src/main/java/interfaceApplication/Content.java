package interfaceApplication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import esayhelper.JSONHelper;
import esayhelper.StringHelper;
import esayhelper.TimeHelper;
import model.ContentModel;
import rpc.execRequest;
import sms.ruoyaMASDB;

@SuppressWarnings("unchecked")
public class Content {
	private ContentModel content = new ContentModel();
	private HashMap<String, Object> defmap = new HashMap<>();
	// private String userId = "";

	public Content() {
		// userId = execRequest.getChan1
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
		defmap.put("tempid", 0);
		defmap.put("uplv", 2000);
		defmap.put("rplv", 1000);
		defmap.put("dplv", 3000);
		defmap.put("time", TimeHelper.nowMillis() + "");
	}

	/**
	 * 修改文章
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String EditArticle(String oid, String contents) {
		// String uPLV = content.select(oid).get("uplv").toString();
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV + "/s:" + userId,
		// null)
		// .toString();
		// if (!"0".equals(tip)) {
		// return content.resultMessage(8, "没有编辑权限");
		// }
		JSONObject infos = JSONHelper.string2json(contents);
		if (infos.containsKey("content")) {
			infos.put("content", encode(infos.get("content").toString()));
		}
		if (infos.containsKey("image")) {
			String image = infos.get("image").toString();
			image = image.replaceAll("@t", "/");
			if (image.contains("8080")) {
				image = image.split("8080")[1];
			}
			infos.put("image", image);
		}
		return content.resultMessage(content.UpdateArticle(oid, infos),
				"文章更新成功");
	}

	/**
	 * 删除文章
	 * 
	 * @param oid
	 *            唯一识别符
	 * @return
	 */
	public String DeleteArticle(String oid) {
		// String dPLV = content.select(oid).get("dplv").toString();
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/DeletePLV/s:" + dPLV + "/s:" + userId,
		// null)
		// .toString();
		// if (!"0".equals(tip)) {
		// return content.resultMessage(9, "没有删除权限");
		// }
		return content.resultMessage(content.DeleteArticle(oid), "文章删除成功");
	}

	/**
	 * 获取最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewArc() {
		return content.findnew();
	}

	/**
	 * 根据oid显示文章 同时显示上一篇文章id，名称 下一篇文章id，名称
	 * 
	 * @return
	 */
	public String findArticle(String oid) {
		return content.resultMessage(content.select(oid));
	}

	/**
	 * 设置内容组
	 * 
	 * @param oid
	 * @param ogid
	 * @return
	 */
	public String SetGroup(String oid, String ogid) {
		// String uPLV = content.select(oid).get("uplv").toString();
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV + "/s:" + userId,
		// null)
		// .toString();
		// if (!"0".equals(tip)) {
		// return content.resultMessage(8, "没有编辑权限");
		// }
		return content.resultMessage(content.setGroup(oid, ogid), "设置内容组成功");
	}

	public String SetGroupBatch(String ogid) {
		return content.resultMessage(content.setGroup(ogid), "设置内容组成功");
	}

	/**
	 * 根据用户查找文章
	 * 
	 * @param uid
	 * @return
	 */
	public String findbyUser(String uid) {
		return content.resultMessage(content.searchByUid(uid));
	}

	/**
	 * 文章模糊查询
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticle(String condString) {
		return content.resultMessage(
				content.search(JSONHelper.string2json(condString)));
	}

//	public String Search(String condString) {
//		return content.resultMessage(
//				content.search(JSONHelper.string2json(condString)));
//	}

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
		return content.page(idx, pageSize);
	}

	public String PageBy(int idx, int pageSize, String contents) {
		return content.page(idx, pageSize, JSONHelper.string2json(contents));
	}

	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowByGroupId(String ogid) {
		return content.resultMessage(content.findByGroupID(ogid));
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
		// String uPLV = content.select(oid).get("uplv").toString();
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV + "/s:" + userId,
		// null)
		// .toString();
		// if (!"0".equals(tip)) {
		// return content.resultMessage(8, "没有编辑权限");
		// }
		return content.resultMessage(content.updatesort(oid, sortNo),
				"排序值修改成功");
	}

	/**
	 * 发布文章
	 * 
	 * @param ArticleInfo
	 * @return
	 */
	public String PublishArticle(String ArticleInfo) {
		// 该用户是否拥有新增权限
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/InsertPLV/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return content.resultMessage(7, "没有新增权限");
		// }
		JSONObject object = JSONHelper.string2json(ArticleInfo);
		object = content.AddMap(defmap, JSONHelper.string2json(ArticleInfo));
		object.put("content", encode(object.get("content").toString()));
		if (object.containsKey("image")) {
			String image = object.get("image").toString();
			image = image.replaceAll("@t", "/");
			if (image.contains("8080")) {
				image = image.split("8080")[1];
			}
			object.put("image", image);
			// object.put("image", StringEscapeUtils
			// .escapeJava(object.get("image").toString()));
		}
		return content.resultMessage(0, content.insert(object));
	}

	// 编码
	public String encode(String htmlContent) {
		String content = null;
		try {
			content = URLEncoder.encode(htmlContent, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return content;
	}

	public JSONObject getwbid(String wbid, JSONObject object) {
		List<String> list = new ArrayList<>();
		if (object.containsKey("wbid")) {
			String wbsid = object.get("wbid").toString();
			if (wbsid.contains(",")) {
				String[] value = wbsid.split(",");
				for (int i = 0; i < value.length; i++) {
					list.add(value[i]);
				}
			} else {
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
		return content.resultMessage(content.deleteByOgID(oid, ogid),
				"该栏目下文章删除成功");
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
		return content.resultMessage(content.deleteByWbID(oid, wbid),
				"该站点下文章删除成功");
	}

	public String SetTempId(String oid, String tempid) {
		return content.resultMessage(content.setTempId(oid, tempid), "设置模版成功");
	}

	public String Setfatherid(String oid, String fatherid) {
		return content.resultMessage(content.setfatherid(oid, fatherid),
				"设置模版成功");
	}

	public String Setslevel(String oid, String slevel) {
		return content.resultMessage(content.setslevel(oid, slevel), "设置密级成功");
	}

	// 审核
	public String Review(String oid, String managerid, String state) {
		return content.resultMessage(content.review(oid, managerid, state),
				"审核文章操作成功");
	}

	public String BatchDelete(String oid) {
		return content.resultMessage(content.delete(oid.split(",")), "批量删除成功");
	}

	// 获取图片及简介
	public String getImgs(String ogid, int no) {
		return content.resultMessage(content.find(ogid, no));
	}

	// 获取当前文章所在栏目位置
	public String getPosition(String oid) {
		List<JSONObject> list = new ArrayList<>();
		// 获取文章所属的栏目id
		String message = JSONHelper.string2json(findArticle(oid)).get("message")
				.toString();
		String records = JSONHelper.string2json(message).get("records")
				.toString();
		String ogid = JSONHelper.string2json(records).get("ogid").toString();
		// 根据栏目id查询栏目信息
		// String prevCol = execRequest
		// ._run("GrapeContent/ContentGroup/getPrevCol/s:" + ogid, null)
		// .toString();
		String prevCol = appsProxy.proxyCall("123.57.214.226:801",
				String.valueOf(appsProxy.appid())
						+ "/15/ContentGroup/getPrevCol/s:" + ogid,
				null, "").toString();
		String fatherid = JSONHelper.string2json(prevCol).get("fatherid")
				.toString();
		list = content.getName(list, JSONHelper.string2json(prevCol));
		// 根据fatherid获取上一级栏目，直到fatherid=0
		while (!"0".equals(fatherid)) {
			// prevCol = execRequest
			// ._run("GrapeContent/ContentGroup/getPrevCol/s:" + fatherid,
			// null)
			// .toString();
			prevCol = appsProxy.proxyCall("123.57.214.226:801",
					String.valueOf(appsProxy.appid())
							+ "/15/ContentGroup/getPrevCol/s:" + fatherid,
					null, "").toString();
			fatherid = JSONHelper.string2json(prevCol).get("fatherid")
					.toString();
			list = content.getName(list, JSONHelper.string2json(prevCol));
		}
		Collections.reverse(list); // list倒序排列
		return content.resultMessage(JSONHelper.string2array(list.toString()));
	}

	// 获取下级栏目的文章
	public String getContent(String ogid, int no) {
		String tips = appsProxy.proxyCall("123.57.214.226:801",
				String.valueOf(appsProxy.appid())
						+ "/15/ContentGroup/getColumnByFid/s:" + ogid,
				null, "").toString();
		// execRequest
		// ._run("GrapeContent/ContentGroup/getColumnByFid/s:" + ogid,
		// null)
		// .toString();
		String message = JSONHelper.string2json(tips).get("message").toString();
		String records = JSONHelper.string2json(message).get("records")
				.toString();
		JSONArray array = JSONHelper.string2array(records);
		String ids = getID(array);
		// 批量查询
		return content.resultMessage(content.find(ids.split(","), no));
	}

	// 获取文章id
	public String getID(JSONArray array) {
		List<String> list = new ArrayList<>();
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			JSONObject objID = (JSONObject) object.get("_id");
			list.add(objID.get("$oid").toString());
		}
		return StringHelper.join(list);
	}

	// 根据条件进行统计
	public String getCount(String info) {
		return content.getCount(JSONHelper.string2json(info));
	}
}
