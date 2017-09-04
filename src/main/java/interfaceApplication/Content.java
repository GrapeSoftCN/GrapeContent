package interfaceApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.DBHelper;
import database.db;
import json.JSONHelper;
import model.ContentModel;
import model.WsCount;
import nlogger.nlogger;
import security.codec;
import session.session;
import string.StringHelper;
import thirdsdk.kuweiCheck;
import time.TimeHelper;

@SuppressWarnings("unchecked")
public class Content {
	private ContentModel content = new ContentModel();
	private HashMap<String, Object> defmap = new HashMap<>();
	private session se;
	private JSONObject UserInfo = new JSONObject();
	private String sid = null;

	private db getContentCache() {
		DBHelper ContentCache = new DBHelper("mongodb", "objectListCache");
		return ContentCache.bind(appsProxy.appidString());
	}

	public Content() {
		se = new session();
		sid = session.getSID();
		if (sid != null) {
			UserInfo = se.getSession();
		}
		defmap.put("subName", null);
		defmap.put("image", "");
		defmap.put("ownid",
				(UserInfo != null && UserInfo.size() != 0) ? ((JSONObject) UserInfo.get("_id")).getString("$oid") : "");
		defmap.put("manageid", "");
		defmap.put("content", "");
		defmap.put("type", 1); // 文章类型
		defmap.put("fatherid", "0");
		defmap.put("author", "0");
		defmap.put("attribute", 0);
		defmap.put("ogid", "0");
		defmap.put("attrid", "0");
		defmap.put("sort", 0);
		defmap.put("isdelete", 0);
		defmap.put("isvisble", 0);
		defmap.put("souce", null);
		defmap.put("state", 0);
		defmap.put("substate", 0);
		defmap.put("slevel", 0);
		defmap.put("readCount", 0);
		defmap.put("thirdsdkid", "");
		defmap.put("tempid", "0");
		defmap.put("wbid", (UserInfo != null && UserInfo.size() != 0) ? UserInfo.get("currentWeb").toString() : "");
		defmap.put("u", 2000);
		defmap.put("r", 1000);
		defmap.put("d", 3000);
		defmap.put("time", TimeHelper.nowMillis()); // 文章创建时间、修改时间
		defmap.put("reason", ""); // 文章未通过审核，填写理由，【未通过审核，该项为必填项】
		defmap.put("reasonRead", 0); // 未通过审核,理由是否已读，0：未读；1：已读
	}

	private String getImageUri(String imageURL) {
		int i = 0;
		if (imageURL.contains("File//upload")) {
			i = imageURL.toLowerCase().indexOf("file//upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File\\upload")) {
			i = imageURL.toLowerCase().indexOf("file\\upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File/upload")) {
			i = imageURL.toLowerCase().indexOf("file/upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		return imageURL;
	}

	/**
	 * 推送文章到上级站点
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param ArticleInfo
	 * @return
	 *
	 */
	public String pushArticle(String wbid, String ArticleInfo) {
		int code = 99;
		db db = getContentCache();
		String oid = "";
		JSONObject object = JSONHelper.string2json(ArticleInfo);
		if (wbid != null && !wbid.equals("") && object != null && object.size() != 0) {
			String[] values = wbid.split(",");
			for (String value : values) {
				if (object.containsKey("ogid")) {
					object.put("ogid", "");
				}
				if (object.containsKey("_id")) {
					oid = ((JSONObject) object.get("_id")).getString("$oid");
					object.remove("_id");
				}
				if (object.containsKey("wbid")) {
					object.remove("wbid");
				}
				object.put("wbid", value);
				object = pushDencode(object);
				if (!CheckContentCache(oid, value)) {
					object = content.AddMap(defmap, object);
					code = db.data(object).insertOnce() != null ? 0 : 99;
				}
			}
		}
		return content.resultMessage(code, "文章推送成功");
	}

	public String pushToColumn(String oid, String data) {
		String result = content.resultMessage(99);
		db db = getContentCache();
		db dbcontent = content.bind();
		JSONObject object = db.eq("_id", oid).find();
		if (object != null && object.size() != 0) {
			object.remove("_id");
			object = remoNumberLong(object);
			String info = dbcontent.data(object).insertOnce().toString();
			result = EditArticle(info, data);
			if (JSONObject.toJSON(result).getString("errorcode").equals("0")) {
				db.eq("_id", oid).delete();
			}
		}
		return result;
	}

	private JSONObject remoNumberLong(JSONObject object) {
		String temp;
		String[] param = { "type", "attribute", "sort", "type", "isdelete", "isvisble", "state", "substate", "slevel",
				"readCount", "u", "r", "d", "time" };
		if (object.containsKey("fatherid")) {
			temp = object.getString("fatherid");
			if (temp.contains("$numberLong")) {
				temp = JSONObject.toJSON(temp).getString("$numberLong");
			}
			object.put("fatherid", temp);
		}
		if (object.containsKey("tempid")) {
			temp = object.getString("tempid");
			if (temp.contains("$numberLong")) {
				temp = JSONObject.toJSON(temp).getString("$numberLong");
			}
			object.put("tempid", temp);
		}
		if (param != null && param.length > 0) {
			for (String value : param) {
				if (object.containsKey(value)) {
					temp = object.getString(value);
					if (temp.contains("$numberLong")) {
						temp = JSONObject.toJSON(temp).getString("$numberLong");
					}
					object.put(value, Long.parseLong(temp));
				}
			}
		}
		return object;
	}

	private JSONObject pushDencode(JSONObject obj) {
		String value = obj.get("content").toString();
		value = codec.DecodeHtmlTag(value);
		value = codec.decodebase64(value);
		obj.escapeHtmlPut("content", value);
		if (obj.containsKey("image")) {
			String image = obj.getString("image");
			if (!image.equals("") && image != null) {
				image = codec.DecodeHtmlTag(image);
				obj.put("image", content.RemoveUrlPrefix(image));
			}
		}
		if (obj.containsKey("thumbnail")) {
			String image = obj.getString("thumbnail");
			if (!image.equals("") && image != null) {
				image = codec.DecodeHtmlTag(image);
				obj.put("thumbnail", content.RemoveUrlPrefix(image));
			}
		}
		return obj;
	}

	/**
	 * 查看推送的文章
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @param condString
	 * @return
	 *
	 */
	public String searchPushArticle(int idx, int pageSize, String condString) {
		db db = getContentCache();
		JSONArray array = null;
		String wbid = "";
		long total = 0, totalSize = 0;
		if (UserInfo != null && UserInfo.size() != 0) {
			wbid = UserInfo.getString("currentWeb");
			if (!wbid.equals("")) {
				db.eq("wbid", wbid);
				if (condString != null) {
					JSONArray condArray = JSONArray.toJSONArray(condString);
					if (condArray != null && condArray.size() != 0) {
						db.where(condArray);
					}
				}
				array = db.dirty().page(idx, pageSize);
				total = db.dirty().count();
				totalSize = db.pageMax(pageSize);
			}
		}
		return content.PageShow(content.getImgs(getDefaultImage(wbid, array)), total, totalSize, idx, pageSize);
	}

	/**
	 * 验证文章是否存在
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param oid
	 * @param wbid
	 * @return
	 *
	 */
	private boolean CheckContentCache(String oid, String wbid) {
		db db = getContentCache();
		JSONObject object = db.eq("_id", oid).eq("wbid", wbid).find();
		return (object != null && object.size() != 0);
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
		String tip = content.resultMessage(99);
		JSONObject object;
		String info;
		List<String> list = new ArrayList<>();
		long time = 0;
		int code = 99;
		JSONArray array = JSONHelper.string2array(ArticleInfo);
		if (array != null && array.size() != 0) {
			for (Object obj : array) {
				object = (JSONObject) obj;
				if (object.containsKey("time")) {
					time = Long.parseLong(object.getString("time"));
					object.put("time", time);
				}
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
			if (code == 0) {
				tip = content.resultMessage(content.batch(list));
			} else {
				tip = content.resultMessage(99);
			}
		}
		return tip;
	}

	/**
	 * 错别字识别
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param contents
	 * @return
	 *
	 */
	public String Typos(String contents) {
		contents = codec.DecodeHtmlTag(contents);
		kuweiCheck check = new kuweiCheck("377c9dc160bff6cfa3cc0cbc749bb11a");
		contents = codec.decodebase64(contents);
		contents = check.checkContent(contents);
		return content.resultMessage(JSONHelper.string2json(contents));
	}

	/**
	 * 修改文章
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String EditArticle(String oid, String contents) {
		int code = 99;
		String image;
		JSONObject infos = JSONHelper.string2json(contents);
		if (UserInfo != null && UserInfo.size() != 0) {
			if (infos != null) {
				try {
					if (infos.containsKey("content")) {
						String value = infos.get("content").toString();
						value = codec.DecodeHtmlTag(value);
						value = codec.decodebase64(value);
						infos.escapeHtmlPut("content", value);
					}
					if (infos.containsKey("image")) {
						image = infos.get("image").toString();
						image = getImageUri(codec.DecodeHtmlTag(image));
						if (image != null) {
							infos.put("image", image);
						}
					}
				} catch (Exception e) {
					nlogger.logout(e);
				}
			}
			code = content.UpdateArticle(oid, infos);
			if (code == 0) {
				// 更改栏目最新更新时间
				ColumnTime(oid);
			}
		}
		return content.resultMessage(code, "文章更新成功");
	}

	/**
	 * 更新栏目最近更新时间
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @return
	 *
	 */
	private String ColumnTime(String oid) {
		String code = content.resultMessage(99);
		long currentTime = TimeHelper.nowMillis();
		String Data = "{\"lastTime\":" + currentTime + "}";
		db db = content.bind();
		JSONObject obj = db.eq("_id", oid).field("ogid").limit(1).find();
		if (obj != null && obj.size() != 0) {
			String ogid = obj.getString("ogid");
			code = appsProxy.proxyCall("/GrapeContent/ContentGroup/GroupEdit/" + ogid + "/" + Data, null, null)
					.toString();
			// code =
			// appsProxy.proxyCall("/GrapeContent/ContentGroup/GroupEdit/" +
			// ogid + "/" + Data).toString();
		}
		return code;
	}

	/**
	 * 删除文章
	 * 
	 * @param oid
	 *            唯一识别符
	 * @return
	 */
	public String DeleteArticle(String oid) {
		int code = 99;
		if (UserInfo != null || UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.DeleteArticle(wbid, oid);
		}
		return content.resultMessage(code, "文章删除成功");
	}

	/**
	 * 获取最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewArc(String wbid) {
		return content.findbywbid(wbid);
	}

	/**
	 * 获取某个栏目下最新的一篇文章信息 排序条件(时间优先，_id次之)
	 * 
	 * @return
	 */
	public String FindNewByOgid(String ogid) {
		return content.findnew(ogid);
	}

	/**
	 * 获取最新公开的指定数量的信息
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param size
	 * @return
	 *
	 */
	public String FindOpen(int size) {
		return content.findnew(size);
	}

	/**
	 * 根据oid显示文章 同时显示上一篇文章id，名称 下一篇文章id，名称,显示文章所有数据信息
	 * 
	 * @return
	 */
	public String findArticle(String oid) {
		return content.find(oid);
	}

	public String findArticles(String ids) {
		JSONObject object, tempObj, obj = new JSONObject();
		String oid;
		String[] value = ids.split(",");
		db db = content.bind().or();
		for (String id : value) {
			db.eq("_id", id);
		}
		JSONArray array = db.field("_id,mainName,image,wbid").select();
		int l = array.size();
		if (l > 0) {
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				tempObj = (JSONObject) object.get("_id");
				oid = tempObj.getString("$oid"); // 文章id
				object = content.getDefaultImage(object);
				object.remove("wbid");
				obj.put(oid, content.getImg(object));
			}
		}
		return obj.toString();
	}

	// 增加栏目点击次数
	public String AddClick(String oid) {
		int code = 99;
		String temp;
		JSONObject tempObj;
		String id; // 栏目id[包含本级栏目及上级栏目id]
		JSONObject obj = getOgid(oid);
		String message = content.resultMessage(99);
		if (obj != null && obj.size() != 0) {
			id = getColumnId(obj);
			// 新增栏目点击次数
			String column = AddColumnClick(id);
			tempObj = JSONObject.toJSON(column);
			if (tempObj != null && tempObj.size() != 0) {
				temp = appsProxy.proxyCall("/GrapeContent/ContentGroup/GroupEdits/" + id + "/" + column, null, null)
						.toString();
				// temp =
				// appsProxy.proxyCall("/GrapeContent/ContentGroup/GroupEdits/"
				// + id + "/" + column).toString();
				tempObj = JSONObject.toJSON(temp);
				if (tempObj != null && tempObj.size() != 0) {
					code = tempObj.getInt("errorcode");
				}
				message = content.resultMessage(code, "新增次数成功");
			}
		}
		return message;

	}

	// 获取栏目id[包含本级栏目及上级栏目id]
	private String getColumnId(JSONObject obj) {
		String ogid;
		JSONArray array;
		JSONObject object;
		String id = "";
		int l;
		ogid = obj.getString("ogid");
		ogid = appsProxy.proxyCall("/GrapeContent/ContentGroup/getPrevCol/" + ogid, null, null).toString();
		// ogid = appsProxy.proxyCall("/GrapeContent/ContentGroup/getPrevCol/" +
		// ogid).toString();
		array = JSONArray.toJSONArray(ogid);
		if (array != null && array.size() != 0) {
			l = array.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				id = object.getString("_id") + ",";
			}
		}
		if (!id.equals("") && id.length() > 0) {
			id = StringHelper.fixString(id, ',');
		}
		return id;
	}

	// 新增点击次数
	private String AddColumnClick(String id) {
		JSONObject tempObject = new JSONObject();
		String[] ids = id.split(",");
		String tempCount;
		long clickcount;
		String temp = appsProxy.proxyCall("/GrapeContent/ContentGroup/getClickCount/" + id, null, null).toString();
		// String temp =
		// appsProxy.proxyCall("/GrapeContent/ContentGroup/getClickCount/" +
		// id).toString();
		// 获取栏目点击次数
		JSONObject column = JSONObject.toJSON(temp);
		if (column != null && column.size() != 0) {
			for (String string : ids) {
				tempCount = String.valueOf(column.get("clickcount"));
				if (tempCount.contains("$numberLong")) {
					tempObject = JSONObject.toJSON(tempCount);
					tempCount = (tempObject != null && tempObject.size() != 0) ? tempObject.getString("$numberLong")
							: "0";
				}
				clickcount = Long.parseLong(tempCount) + 1;
				column.put(string, clickcount);
			}
		}
		return column.toJSONString();
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

	// 批量设置内容组
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

	/*----------------前台搜索-----------------*/
	/**
	 * 文章模糊查询
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticle(String wbid, int ids, int pageSize, String condString) {
		return content.search(wbid, ids, pageSize, condString);
	}

	/*----------------后台搜索-----------------*/
	/**
	 * 文章模糊查询
	 * 
	 * @param jsonstring
	 * @return
	 */
	public String SearchArticleBack(int ids, int pageSize, String condString) {
		String wbid = "";
		if (UserInfo != null || UserInfo.size() != 0) {
			wbid = UserInfo.get("currentWeb").toString();
		}
		return content.searchBack(wbid, ids, pageSize, condString);
	}

	/**
	 * 分页[前台],查询条件含有ogid,需要返回关联栏目的文章信息
	 * 
	 * @param idx
	 *            当前页
	 * @param pageSize
	 *            每页显示量
	 * @return
	 */
	public String Page(String wbid, int idx, int pageSize) {
		return PageBy(wbid, idx, pageSize, null);
	}

	public String PageBy(String wbid, int idx, int pageSize, String contents) {
		if (UserInfo != null && UserInfo.size() != 0) {
			wbid = UserInfo.get("currentWeb").toString();
		}
		return content.page(wbid, idx, pageSize, contents);
	}

	public String PageBySid(int idx, int pageSize, String contents) {
		JSONObject object = content.page2(idx, pageSize, JSONHelper.string2json(contents));
		return content.resultMessage(object);
	}

	/**
	 * 前台页面数据显示，显示文章内容
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param wbid
	 * @param idx
	 * @param pageSize
	 * @param condString
	 * @return
	 *
	 */
	public String ShowFront(String wbid, int idx, int pageSize, String condString) {
		JSONArray condarray = JSONArray.toJSONArray(condString);
		JSONArray array = new JSONArray();
		long total = 0, totalSize = 0;
		db db = content.bind();
		if (condarray != null && condarray.size() != 0) {
			db.desc("time").eq("wbid", wbid).eq("slevel", 0).where(condarray).field("_id,mainName,image,time,content");
			array = db.dirty().page(idx, pageSize);
			total = db.count();
			db.clear();
			totalSize = (int) Math.ceil((double) total / pageSize);
		}
		return content.PageShow(array, total, totalSize, idx, pageSize);
	}

	public String PageBack(int idx, int pageSize) {
		return PageByBack(idx, pageSize, null);
	}

	public String PageByBack(int idx, int pageSize, String contents) {
		JSONArray condArray = JSONArray.toJSONArray(contents);
		JSONArray tempArray = getTemplate(condArray);
		JSONArray array = new JSONArray();
		long total = 0, totalSize = 0;
		String wbid = "";
		int roleSign = content.getRole();
		db db = content.bind();
		if (condArray != null && condArray.size() != 0) {
			db.where(condArray);
		}
		wbid = (UserInfo != null && UserInfo.size() != 0) ? UserInfo.get("currentWeb").toString() : "";
		// 获取角色权限
		if (roleSign == 3) {
			db.eq("wbid", wbid);
		} else if (roleSign == 2 || roleSign == 1 || roleSign == 0) {
			db.eq("slevel", 0);
		}
		array = db.dirty().desc("sort").desc("time").page(idx, pageSize);
		total = db.count();
		db.clear();
		totalSize = (int) Math.ceil((double) total / pageSize);
		array = setTemplate(array, tempArray);
		array = content.dencode(array);
		return content.PageShow(content.getImgs(getDefaultImage(wbid, array)), total, totalSize, idx, pageSize);
	}

	/**
	 * 显示审核文章，condString为null，显示所有的文章
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @param condString
	 *
	 */
	public String ShowArticle(int idx, int pageSize, String condString) {
		long total = 0, totalSize = 0;
		JSONArray condArray = JSONArray.toJSONArray(condString);
		JSONArray tempArray = getTemplate(condArray);
		String wbid = (UserInfo != null && UserInfo.size() != 0) ? UserInfo.get("currentWeb").toString() : "";
		// 获取下级站点
		String[] wbids = getAllContent(wbid);
		JSONArray array = null;
		if (wbids != null && wbids.length > 0) {
			db db = content.bind();
			db.or();
			for (String id : wbids) {
				db.eq("wbid", id);
			}
			if (condString != null) {
				if (condArray != null && condArray.size() != 0) {
					db.and();
					db.where(condArray);
				}
			}
			array = db.dirty().desc("sort").desc("time").page(idx, pageSize);
			total = db.count();
			db.clear();
			totalSize = (int) Math.ceil((double) total / pageSize);
			array = setTemplate(array, tempArray);
			array = content.dencode(array);
		}
		return content.PageShow(content.getImgs(getDefaultImage(wbid, array)), total, totalSize, idx, pageSize);
	}

	/**
	 * 获取当前网站的下级站点的所有文章【提供状态显示】
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 *
	 */
	private String[] getAllContent(String wbid) {
		String wbids = "";
		String[] value = null;
		if (wbid != null && !wbid.equals("")) {
			// 获取所有下级站点
			wbids = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getChildwebs/" + wbid, null, null).toString();
			if (wbids != null && !wbids.equals("")) {
				value = wbids.split(",");
			}
		}
		return value;
	}

	/**
	 * 获取默认缩略图
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param wbid
	 * @param array
	 * @return
	 *
	 */
	private JSONArray getDefaultImage(String wbid, JSONArray array) {
		String thumbnail = "";
		if (!wbid.equals("") && array != null && array.size() != 0) {
			int l = array.size();
			// 显示默认缩略图
			String temp = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + wbid, null, null).toString();
			// String temp =
			// appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" +
			// wbid).toString();
			JSONObject Obj = JSONObject.toJSON(temp);
			if (Obj != null && Obj.size() != 0) {
				thumbnail = Obj.getString("thumbnail");
			}
			for (int i = 0; i < l; i++) {
				Obj = (JSONObject) array.get(i);
				Obj.put("thumbnail", thumbnail);
				array.set(i, Obj);
			}
		}
		return array;
	}

	/***
	 * 文章设置模版名称
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param array
	 *            文章内容信息
	 * @param tempObj
	 *            模版名称信息
	 * @return
	 *
	 */
	private JSONArray setTemplate(JSONArray array, JSONArray tempArray) {
		JSONObject obj, tempObj = new JSONObject();
		String value, TemplateContent, Templatelist, tid;
		if (array == null || array.size() == 0) {
			return new JSONArray();
		}
		if (tempArray != null && tempArray.size() != 0) {
			int l = tempArray.size();
			for (int i = 0; i < l; i++) {
				obj = (JSONObject) tempArray.get(i);
				if (obj != null && obj.size() != 0) {
					TemplateContent = obj.getString("TemplateContent");
					Templatelist = obj.getString("TemplateList");
					tid = obj.getString("_id");
					if (!TemplateContent.equals("") && !Templatelist.equals("")) {
						tempObj.put(tid, TemplateContent + "," + Templatelist);
					}
				}
			}
			JSONObject object = new JSONObject();
			String[] temp;
			String list = "", content = "";
			for (int i = 0; i < array.size(); i++) {
				object = (JSONObject) array.get(i);
				value = object.getString("ogid");
				if (tempObj != null && tempObj.size() != 0) {
					temp = tempObj.getString(value).split(",");
					content = temp[0];
					list = temp[1];
				}
				object.put("TemplateContent", content);
				object.put("Templatelist", list);
				array.set(i, object);
			}
		}
		return array;
	}

	/**
	 * 获取栏目模版
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param condArray
	 * @return
	 *
	 */
	private JSONArray getTemplate(JSONArray condArray) {
		String ogid = "";
		JSONObject object = null;
		JSONArray array = new JSONArray();
		if (condArray != null && condArray.size() != 0) {
			int l = condArray.size();
			String value, column;
			for (int i = 0; i < l; i++) {
				object = (JSONObject) condArray.get(i);
				if (object != null && object.size() != 0) {
					value = object.getString("field");
					if (value.equals("ogid")) {
						value = object.getString("value");
						ogid += value + ",";
					}
				}
			}
			if (ogid != null && !ogid.equals("") && ogid.length() > 0) {
				ogid = StringHelper.fixString(ogid, ',');
				if (ogid != null && !ogid.equals("")) {
					column = appsProxy.proxyCall("/GrapeContent/ContentGroup/getGroupByIds/" + ogid, null, null)
							.toString();
					// column =
					// appsProxy.proxyCall("/GrapeContent/ContentGroup/getGroupByIds/"
					// + ogid).toString();
					array = JSONArray.toJSONArray(column);
				}
			}
		}
		return array;
	}

	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowByGroupId(String wbid, String ogid) {
		return content.resultMessage(content.findByGroupID(wbid, ogid));
	}

	/*----------前台页面图片显示-------------*/
	/**
	 * 根据内容组id显示文章
	 * 
	 * @param ogid
	 * @return
	 */
	public String ShowPicByGroupId(String wbid, String ogid) {
		return content.resultMessage(content.findPicByGroupID(wbid, ogid));
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

	/**
	 * 判断当前文章栏目是否需要审核
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param ogid
	 * @return
	 *
	 */
	private boolean getArticleState(String ogid) {
		ContentGroup group = new ContentGroup();
		String managerid = group.getManagerByOgid(ogid);
		return !managerid.equals("");
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
		int code = 99;
		if (UserInfo != null || UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.deleteByOgID(wbid, oid, ogid);
		}
		return content.resultMessage(code, "该栏目下文章删除成功");
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
		int code = 99;
		if (UserInfo != null || UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setTempId(wbid, oid, tempid);
		}
		return content.resultMessage(code, "设置模版成功");
	}

	public String Setfatherid(String oid, String fatherid) {
		int code = 99;
		if (UserInfo != null || UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setfatherid(wbid, oid, fatherid);
		}
		return content.resultMessage(code, "设置上级文章成功");
	}

	public String Setslevel(String oid, String slevel) {
		int code = 99;
		if (UserInfo != null || UserInfo.size() != 0) {
			String wbid = UserInfo.get("currentWeb").toString();
			code = content.setslevel(wbid, oid, slevel);
		}
		return content.resultMessage(code, "设置密级成功");
	}

	// 审核
	public String review(String oid, String reviewData) {
		int code = 99;
		String key;
		JSONObject object = JSONObject.toJSON(reviewData);
		if (object != null && object.size() != 0) {
			for (Object obj : object.keySet()) {
				key = obj.toString();
				if (key.equals("state")) {
					object.put("state", Long.parseLong(object.getString(key)));
				}
			}
		}
		code = content.bind().eq("_id", oid).data(object).update() != null ? 0 : 99;
		return content.resultMessage(code, "文章审核成功");
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
		JSONObject object;
		String prevCol = "";
		if (oid.equals("") || oid.equals("0")) {
			nlogger.logout("oid = 0");
			return "";
		}
		try {
			// 获取文章所属的栏目id
			object = getOgid(oid);
			if (object != null) {
				String ogid = object.get("ogid").toString();
				prevCol = content.getPrev(ogid);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			prevCol = "";
		}
		return prevCol;
	}

	// 获取下级栏目的文章
	public String getContent(String ogid, int no) {
		String ids = null;
		try {
			String tips = appsProxy.proxyCall("/GrapeContent/ContentGroup/getColumnByFid/s:" + ogid, null, null)
					.toString();
			// String tips =
			// appsProxy.proxyCall("/GrapeContent/ContentGroup/getColumnByFid/s:"
			// + ogid).toString();
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

	// 批量查询本周内已更新的文章数量
	public String getArticleCount(String info, String ogid) {
		return content.getArticleCount(info, ogid).toJSONString();
	}

	// 获取文章所属的栏目id
	private JSONObject getOgid(String oid) {
		return content.bind().eq("_id", new ObjectId(oid)).field("ogid,clickcount").find();
	}

	/**
	 * 获取重点公开内容
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param condString
	 * @param idx
	 * @param PageSize
	 * @return
	 *
	 */
	public String getKeyArticle(String condString, int idx, int PageSize) {
		JSONArray condarray = JSONArray.toJSONArray(condString);
		JSONArray array = new JSONArray();
		long total = 0, totalSize = 0;
		db db = content.bind();
		if (condarray != null && condarray.size() != 0) {
			db.desc("time").eq("slevel", 0).where(condarray).field("_id,mainName,ogid,time");
			array = db.dirty().page(idx, PageSize);
			total = db.count();
			db.clear();
			totalSize = (int) Math.ceil((double) total / PageSize);
		}
		return content.PageShow(array, total, totalSize, idx, PageSize);
	}

	/**
	 * 获取点击量最高的公开内容
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param condString
	 * @param idx
	 * @param PageSize
	 * @return
	 *
	 */
	public String getHotArticle(String condString, int idx, int PageSize) {
		JSONArray condarray = JSONArray.toJSONArray(condString);
		JSONArray array = new JSONArray();
		long total = 0, totalSize = 0;
		String wbids = getAllWeb(condarray);
		db db = content.bind();
		db.or();
		if (condarray != null && condarray.size() != 0) {
			if (wbids != null) {
				String[] value = wbids.split(",");
				for (String wbid : value) {
					db.eq("wbid", wbid);
				}
			}
			db.and();
			db.eq("slevel", 0).desc("clickcount").desc("_id");
			db.field("_id,mainName,ogid,time");
			array = db.dirty().page(idx, PageSize);
			total = db.count();
			db.clear();
			totalSize = (int) Math.ceil((double) total / PageSize);
		}
		return content.PageShow(array, total, totalSize, idx, PageSize);
	}

	// 获取子站,去除一级站点
	private String getAllWeb(JSONArray condarray) {
		String field, value = null, wbid = null;
		List<String> list = new ArrayList<>();
		String[] wbids;
		JSONObject condObj;
		if (condarray != null && condarray.size() != 0) {
			int l = condarray.size();
			for (int i = 0; i < l; i++) {
				condObj = (JSONObject) condarray.get(i);
				field = condObj.getString("field");
				if (field.equals("wbid")) {
					value = condObj.getString("value");
					break;
				}
			}
			if (value != null && !value.equals("")) {
				wbid = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + value, null, null).toString();
			}
			if (wbid != null && !wbid.equals("")) {
				wbids = wbid.split(",");
				for (String string : wbids) {
					if (!string.equals(value)) {
						list.add(string);
					}
				}
			}
		}
		return StringHelper.join(list);
	}

	/**
	 * 追加内容数据
	 * 
	 * @project GrapeContent
	 * @package interfaceApplication
	 * @file Content.java
	 * 
	 * @param id
	 * @param info
	 * @return
	 *
	 */
	public String AddAppend(String id, String info) {
		db db = content.bind();
		int code = 99;
		String contents = "", oldcontent;
		JSONObject object = db.eq("_id", id).find();
		JSONObject obj = JSONObject.toJSON(info);
		if (obj != null && obj.size() != 0) {
			if (obj.containsKey("content")) {
				contents = obj.getString("content");
				contents = codec.DecodeHtmlTag(contents);
				contents = codec.decodebase64(contents);
				obj.escapeHtmlPut("content", contents);
				oldcontent = object.getString("content");
				oldcontent += obj.getString("content");
				obj.put("content", oldcontent);
			}
			code = db.eq("_id", id).data(obj).update() != null ? 0 : 99;
		}
		return content.resultMessage(code, "追加文档成功");
	}

	//统计
	public String totalArticle(String wbid) {
		return content.resultMessage(getGroup(wbid));
	}

	// 获取全部站点id
	public String[] getAllWeb(String wbid) {
		String wbids = "";
		String[] value = null;
		if (wbid != null && !wbid.equals("")) {
			// 获取所有下级站点
			wbids = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid, null, null).toString();
			if (wbids != null && !wbids.equals("")) {
				value = wbids.split(",");
			}
		}
		return value;
	}

	// 按状态统计数据
	private JSONArray getGroup(String wbid) {
		String webtemp = "";
		// 获取所有网站id
		String[] wbids = getAllWeb(wbid);
		webtemp = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getFWebInfo/" + StringHelper.join(wbids), null, null)
				.toString();
		db db = content.bind();
		JSONArray temparray = db.eq("state", 0).count("state").group("wbid"); // 待审核文章
		JSONArray tempArray = db.eq("state", 1).count("state").group("wbid"); // 审核不通过
		JSONArray TempArray = db.eq("state", 2).count("state").group("wbid"); // 审核通过
		return AppendState(temparray, tempArray, TempArray, wbids, webtemp);
	}

	// 合并文章统计数据
	private JSONArray AppendState(JSONArray temparray, JSONArray tempArray, JSONArray TempArray, String[] wbids,
			String WebInfo) {
		JSONObject webTemp = JSONObject.toJSON(WebInfo);
		JSONArray array = null;
		JSONObject object = null;
		String info = "", webName = "", fatherid = "0";
		long checking = 0, checked = 0, uncheck = 0;
		if (wbids != null && !wbids.equals("")) {
			array = new JSONArray();
			for (String value : wbids) {
				object = new JSONObject();
				checking = judgeWbid(value, temparray); // 待审核文章
				uncheck = judgeWbid(value, tempArray); // 审核不通过
				checked = judgeWbid(value, TempArray); // 审核通过
				if (webTemp != null && webTemp.size() != 0) {
					info = webTemp.getString(value);
					if (info != null && !info.equals("")) {
						webName = info.split(",")[0];
						fatherid = info.split(",")[1];
					}
				}
				object.put("wbid", value);
				object.put("fatherid", fatherid);
				object.put("webName", webName);
				object.put("checking", checking); // 待审核
				object.put("uncheck", uncheck); // 审核不通过
				object.put("checked", checked); // 审核通过
				array.add(object);
			}
		}
		return array;
	}

	private long judgeWbid(String id, JSONArray temparray) {
		JSONObject object;
		String wbid;
		int l = 0;
		long count = 0;
		if (temparray != null && temparray.size() != 0) {
			l = temparray.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) temparray.get(i);
				wbid = object.getString("_id");
				if (id.equals(wbid)) {
					count = Long.parseLong(object.getString("count"));
					break;
				}
			}
		}
		return count;
	}
	public String total(String rootID) {
			JSONObject json = new JSONObject();
//			JSONObject webInfo = JSONObject.toJSON((String)(String)execRequest._run("/GrapeWebInfo/WebInfo/getWebInfo/s:" + rootID));
			JSONObject webInfo = JSONObject.toJSON(appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + rootID, null,null).toString());
			json = new WsCount().getAllCount(json, rootID , webInfo.getString(rootID) , "");
			return json.toJSONString();
	}
}
