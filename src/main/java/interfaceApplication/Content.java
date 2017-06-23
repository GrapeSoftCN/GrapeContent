package interfaceApplication;

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
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;

@SuppressWarnings("unchecked")
public class Content {
	private ContentModel content = new ContentModel();
	private HashMap<String, Object> defmap = new HashMap<>();
	private static session session;
	private JSONObject UserInfo = new JSONObject();
	private String sid = null;

	static {
		session = new session();
	}

	public Content() {
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			UserInfo = session.getSession(sid);
		}
		defmap.put("subName", null);
		defmap.put("image", "");
		defmap.put("ownid", appsProxy.appid());
		defmap.put("manageid", "");
		defmap.put("content", "");
		defmap.put("fatherid", "0");
		defmap.put("author", 0);
		defmap.put("attribute", 0);
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
		defmap.put("wbid", (UserInfo != null && UserInfo.size() != 0) ? UserInfo.get("currentWeb").toString() : "");
		defmap.put("u", 2000);
		defmap.put("r", 1000);
		defmap.put("d", 3000);
		defmap.put("time", TimeHelper.nowMillis() + "");
	}

	// private String getImageUri(String imageURL) {
	// String subString="";
	// String rString = null;
	// int i = imageURL.toLowerCase().indexOf("http://");
	// if (i >= 0) {
	// subString = imageURL.substring(i + 7);
	// rString = subString.split("/")[1];
	// }
	// return rString;
	// }
	private String getImageUri(String imageURL) {
		// String rString = null;
		if (imageURL.contains("http://")) {
			int i = imageURL.toLowerCase().indexOf("/file/upload");
			imageURL = imageURL.substring(i);
		}
		return imageURL;
	}

	/**
	 * 批量添加文章
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param ArticleInfo
	 *            参数格式为[{文章数据1},{文章数据2},...]
	 * @return
	 *
	 */
	public String AddAllArticle(String ArticleInfo) {
		JSONObject object;
		String info;
		List<String> list = new ArrayList<>();
		int code = 99;
		JSONArray array = JSONHelper.string2array(ArticleInfo);
		if (array != null && array.size() != 0) {
			for (Object obj : array) {
				object = (JSONObject) obj;
				info = content.AddAll(content.AddMap(defmap, object));
				if (info == null) {
					if (list.size() != 0) {
						BatchDelete(StringHelper.join(list));
					}
					code = 99;
					break;
				}
				code = 0;
				list.add(info);
			}
		}
		return content.resultMessage(code, "批量发布文章成功");
	}

	/**
	 * 修改文章
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String EditArticle(String oid, String contents) {
		JSONObject infos = JSONHelper.string2json(contents);
		if (infos != null) {
			try {
				// String content =
				// codec.DecodeHtmlTag(infos.get("content").toString());
				// infos.put("content", codec.encodebase64(content));
				infos.put("content", codec.DecodeHtmlTag(infos.get("content").toString()));
				if (infos.containsKey("image")) {
					String image = infos.get("image").toString();
					image = getImageUri(codec.DecodeHtmlTag(image));
					/*
					 * if (image.contains("8080")) { image =
					 * image.split("8080")[1]; }
					 */
					if (image != null) {
						infos.put("image", image);
					}
				}
			} catch (Exception e) {
				nlogger.logout(e);
			}
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
	 * 获取最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewArc() {
		return content.findnew();
	}

	// 获取最新公开的指定数量的信息
	public String FindOpen(int size) {
		return content.findnew(size);
	}

	/**
	 * 根据oid显示文章 同时显示上一篇文章id，名称 下一篇文章id，名称
	 * 
	 * @return
	 */
	public String findArticle(String oid) {
		JSONObject object = content.select(oid);
		if (object == null) {
			return content.resultMessage(7, "");
		}
		return content.resultMessage(object);
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
	public String SearchArticles(String condString) {
		return content.resultMessage(content.search(JSONHelper.string2json(condString)));
	}

	public String SearchArticle(String condString, int no) {
		return content.resultMessage(content.search(JSONHelper.string2json(condString), no));
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
		if (UserInfo == null) {
			return content.resultMessage(11, "");
		}
		return content.page(idx, pageSize);
	}

	public String PageBy(int idx, int pageSize, String contents) {
		if (UserInfo == null) {
			return content.resultMessage(11, "");
		}
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
		return content.resultMessage(content.updatesort(oid, sortNo), "排序值修改成功");
	}

	/**
	 * 发布文章
	 * 
	 * @param ArticleInfo
	 * @return
	 */
	public String PublishArticle(String ArticleInfo) {
		JSONObject object = JSONHelper.string2json(ArticleInfo);
		object = content.AddMap(defmap, object);
		return content.insert(object);
	}

	public JSONObject getwbid(String wbid, JSONObject object) {
		if (object != null) {
			List<String> list = new ArrayList<>();
			if (object.containsKey("wbid")) {
				String wbsid = object.get("wbid").toString();
				String[] value = wbsid.split(",");
				for (int i = 0; i < value.length; i++) {
					list.add(value[i]);
				}
				list.add(wbid);
				wbid = StringHelper.join(list);
			}
			object.put("wbid", wbid);
		}
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

	// 获取图片及简介
	public String getImgs(String ogid, int no) {
		return content.resultMessage(content.find(ogid, no));
	}

	// 获取当前文章所在栏目位置
	public String getPosition(String oid) {
		List<JSONObject> list = new ArrayList<>();
		try {
			// 获取文章所属的栏目id
			String message = JSONHelper.string2json(findArticle(oid)).get("message").toString();
			String records = JSONHelper.string2json(message).get("records").toString();
			String ogid = JSONHelper.string2json(records).get("ogid").toString();
			String prevCol = content.getPrev(ogid);
			String fatherid = JSONHelper.string2json(prevCol).get("fatherid").toString();
			list = content.getName(list, JSONHelper.string2json(prevCol));
			// 根据fatherid获取上一级栏目，直到fatherid=0
			if (!fatherid.contains("$numberLong")) {
				while (!"0".equals(fatherid)) {
					prevCol = content.getPrev(fatherid);
					if (!prevCol.equals("")) {
						fatherid = JSONHelper.string2json(prevCol).get("fatherid").toString();
						list = content.getName(list, JSONHelper.string2json(prevCol));
					}
				}
			}
			Collections.reverse(list); // list倒序排列
		} catch (Exception e) {
			nlogger.logout(e);
			list = null;
		}
		return list != null ? content.resultMessage(JSONHelper.string2array(list.toString())) : "";
	}

	// 获取下级栏目的文章
	public String getContent(String ogid, int no) {
		String ids = null;
		try {
			String tips = appsProxy
					.proxyCall(content.getPrev(ogid),
							String.valueOf(appsProxy.appid()) + "/15/ContentGroup/getColumnByFid/s:" + ogid, null, "")
					.toString();
			String message = JSONHelper.string2json(tips).get("message").toString();
			String records = JSONHelper.string2json(message).get("records").toString();
			JSONArray array = JSONHelper.string2array(records);
			ids = getID(array);
		} catch (Exception e) {
			nlogger.logout(e);
			ids = null;
		}
		// 批量查询
		return ids == null ? "" : content.resultMessage(content.find(ids.split(","), no));
	}

	// 获取文章id
	public String getID(JSONArray array) {
		List<String> list = null;
		if (array != null) {
			list = new ArrayList<>();
			JSONObject object;
			JSONObject objID;
			for (int i = 0, len = array.size(); i < len; i++) {
				object = (JSONObject) array.get(i);
				objID = (JSONObject) object.get("_id");
				list.add(objID.get("$oid").toString());
			}
		}
		return list == null ? "" : StringHelper.join(list);
	}

	// 根据条件进行统计
	public String getCount(String info) {
		return content.getCount(JSONHelper.string2json(info));
	}
}
