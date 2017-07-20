package model;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.privilige;
import cache.redis;
import check.formHelper;
import check.formHelper.formdef;
import database.DBHelper;
import database.db;
import database.userDBHelper;
import jodd.util.ArraysUtil;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import string.StringHelper;

@SuppressWarnings("unchecked")
public class ContentModel {
	private static userDBHelper dbcontent = null;
	private static DBHelper content = null;
	private static formHelper _form;
	private JSONObject _obj = new JSONObject();
	private JSONObject UserInfo = new JSONObject();
	private static session session;
	private String appid = appsProxy.appidString();
	private String sid = null;
	private final Pattern ATTR_PATTERN = Pattern.compile("<img[^<>]*?\\ssrc=['\"]?(.*?)['\"]?\\s.*?>",
			Pattern.CASE_INSENSITIVE);

	static {

		// nlogger.logout("SID:" + (String) execRequest.getChannelValue("sid"));
		session = new session();
		dbcontent = new userDBHelper("objectList", (String) execRequest.getChannelValue("sid"));
		// _form = dbcontent.getChecker();
	}

	public ContentModel() {
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			UserInfo = session.getSession(sid);
		}
	}

	private DBHelper getDB() {
		if (content == null) {
			content = new DBHelper(appsProxy.configValue().get("db").toString(), "objectList");
		}
		return content;
	}

	private DBHelper getUserDB() {
		dbcontent = new userDBHelper("objectList", sid);
		return dbcontent;
	}

	public db bind() {
		// if (sid == null) {
		return getDB().bind(String.valueOf(appsProxy.appid()));
		// }
		// return getUserDB().bind(String.valueOf(appsProxy.appid()));
	}

	private formHelper getForm() {
		if (sid == null) {
			_form = getDB().getChecker();
		} else {
			_form = getUserDB().getChecker();
		}
		_form.putRule("mainName", formdef.notNull);
		_form.putRule("content", formdef.notNull);
		_form.putRule("wbid", formdef.notNull);
		return _form;
	}

	/**
	 * 发布文章
	 * 
	 * @param content
	 * @return && 1：判断字段是否合法 && 2：必填信息没有填 && 3：同栏目下已存在该文章 && 4：同站点下已存在该文章 &&
	 *         5：是否含有敏感词 接入第三方插件
	 */
	// 不允许重复添加，但存在同名不同内容的文章
	public String insert(JSONObject content) {
		String info = null;
		JSONObject ro = null;
		try {
			info = checkparam(content);
			if (JSONHelper.string2json(info) != null && info.contains("errorcode")) {
				return info;
			}
			info = bind().data(info).insertOnce().toString();
			ro = findOid(info);
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.insert: " + e);
		}
		return resultMessage(ro);
	}

	/**
	 * 验证待添加文章数据内容的合法性
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param content
	 * @return
	 *
	 */
	private String checkparam(JSONObject content) {
		if (content == null) {
			return resultMessage(0, "插入失败");
		}
		if (!getForm().checkRuleEx(content)) {
			return resultMessage(2, "");
		}
		String value = content.get("content").toString();
		value = codec.DecodeHtmlTag(value);
		value = codec.decodebase64(value);
		if (JSONHelper.string2json(value) == null) {
			content.escapeHtmlPut("content", value);
		} else {
			return value;
		}
		if (content.get("mainName").toString().equals("")) {
			return resultMessage(1, "");
		}
		if (!content.get("fatherid").toString().equals("0")) {
			content.remove("ogid");
		}
		if (content.containsKey("image")) {
			String image = content.getString("image");
			if (!image.equals("") && image != null) {
				image = codec.DecodeHtmlTag(image);
				content.puts("image", RemoveUrlPrefix(image));
			}
		}
		return content.toJSONString();
	}

	/**
	 * 获取文章内容，对不同的文章内容数据进行处理
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @return
	 *
	 */
	// private String getContent(String contents) {
	// if (!("").equals(contents)) {
	//// String reg = "";
	// contents = contents.toLowerCase();
	// Matcher matcher = ATTR_PATTERN.matcher(contents);
	// int code = matcher.find() ? 0
	// : contents.contains("/file/upload") ? 1 :2 ;
	// switch (code) {
	// case 0: // 文章内容为html带图片类型的内容处理
	// contents = RemoveHtmlPrefix(contents);
	// break;
	// case 1: // 文章内容图片类型处理[获取图片的相对路径]
	// contents = RemoveUrlPrefix(contents);
	// break;
	// case 2: // 文章内容为纯文字类型处理
	// contents = CheckWord(contents);
	// break;
	// case 3: // 文章内容含有html标签，但是不含有<img>
	// contents = CheckWord(contents);
	// break;
	// }
	// }
	// return contents;
	// }

	// 获取html内容中的图片地址集
	private List<String> getCommonAddr(String contents) {
		Matcher matcher = ATTR_PATTERN.matcher(contents);
		List<String> list = new ArrayList<String>();
		while (matcher.find()) {
			list.add(matcher.group(1));
		}
		return list;
	}

	/**
	 * 去除html中图片地址 http://.....
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param Contents
	 * @return
	 *
	 */
	private String RemoveHtmlPrefix(String Contents) {
		List<String> list = getCommonAddr(Contents);
		for (int i = 0; i < list.size(); i++) {
			String temp = list.get(i);
			String string2 = temp.replace(temp, RemoveUrlPrefix(temp));
			if (Contents.contains(temp)) {
				Contents = Contents.replaceAll(temp, string2);
			}
		}
		return Contents;
	}

	/**
	 * 添加html中图片地址 http://.....
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param Contents
	 * @return
	 *
	 */
	private String AddHtmlPrefix(String Contents) {
		Document doc = Jsoup.parse(Contents);
		if (doc.empty() != null) {
			String imgurl = "http://" + getFileHost(0);
			Elements element = doc.select("img");
			if (element.size() != 0) {
				for (int i = 0; i < element.size(); i++) {
					String attrValue = element.get(i).attr("src");
					if (attrValue.contains("http://")) {
						attrValue = RemoveHtmlPrefix(attrValue);
					}
					element.get(i).attr("src", imgurl + attrValue);
				}
				return doc.html();
			}
		}
		return Contents;
	}

	// 批量添加
	public String AddAll(JSONObject object) {
		Object tip;
		String info = checkparam(object);
		tip = (JSONHelper.string2json(info) != null && !info.contains("errorcode"))
				? tip = bind().data(info).insertOnce() : null;
		return tip != null ? tip.toString() : null;
	}

	// 批量查询,类方法内部使用
	public JSONArray batch(List<String> list) {
		JSONArray array = new JSONArray();
		db db = bind().or();
		for (String string : list) {
			db.eq("_id", new ObjectId(string));
		}
		array = db.select();
		return join(getImgs(dencode(array)));
	}

	public int UpdateArticle(String wbid, String oid, JSONObject content) {
		int ri = 99;
		try {
			if (content.containsKey("mainName")) {
				if (content.get("mainName").toString().equals("")) {
					return 1;
				}
			}
			if (content.containsKey("_id")) {
				content.remove("_id");
			}
			ri = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(content).update() != null ? 0 : 99;
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.UpdateArticle: " + e);
			ri = 99;
		}
		return ri;
	}

	// 删除文章
	public int DeleteArticle(String wbid, String oid) {
		return bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).delete() != null ? 0 : 99;
	}

	/**
	 * 删除指定栏目下的指定文章
	 * 
	 * @param oid
	 *            文章id
	 * @param ogid
	 *            栏目id
	 */
	public int deleteByOgID(String wbid, String oid, String ogid) {
		int i = 99;
		try {
			// 查询oid对应的文章
			JSONObject _obj = (JSONObject) bind().eq("_id", new ObjectId(oid)).find();
			// 获取栏目id
			String values = _obj.get("ogid").toString();
			values = values.replace(ogid, "");
			JSONObject obj = new JSONObject();
			obj.put("ogid", values);
			i = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(obj).update() != null ? 0 : 99;
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.deleteByOgID: " + e);
			i = 99;
		}
		return i;
	}

	/**
	 * 删除指定站点下的指定文章
	 * 
	 * @param oid
	 *            文章id
	 * @param wbid
	 *            站点id
	 * @return
	 */
	public int deleteByWbID(String oid, String wbid) {
		int i = 99;
		try {
			// 查询oid对应的文章
			JSONObject _obj = (JSONObject) bind().eq("_id", new ObjectId(oid)).find();
			// 获取站点id
			String values = _obj.get("wbid").toString();
			values = values.replace(wbid, "");
			JSONObject obj = new JSONObject();
			obj.put("wbid", values);
			i = bind().eq("_id", new ObjectId(oid)).data(obj).update() != null ? 0 : 99;
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.deleteByWbID: " + e);
			i = 99;
		}
		return i;
	}

	/**
	 * 文章已存在，设置栏目
	 * 
	 * @param oid
	 * @param ogid
	 * @return
	 */
	public int setGroup(String oid, String ogid) {
		int i = 99;
		try {
			JSONObject obj = new JSONObject();
			// 查询oid对应的文章
			JSONObject _aArray = select(oid);
			if (_aArray != null) {
				JSONObject _obj = (JSONObject) _aArray.get(0);
				// 获取栏目id
				String[] value = _obj.get("ogid").toString().split(",");
				// 判断该栏目是否存在
				if (ArraysUtil.contains(value, ogid)) {
					return 3; // 返回3 文章已存在于该栏目下
				}
				String values = StringHelper.join(ArraysUtil.append(value, ogid));
				obj.put("ogid", values);
				i = bind().eq("_id", new ObjectId(oid)).data(obj).update() != null ? 0 : 99;
			}
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("content.setGroup:oid,ogid: " + e);
			i = 99;
		}
		return i;
	}

	public int setGroup(String ogid) {
		int ir = 99;
		try {
			String[] value = null;
			db db = bind().or();
			String cont = "{\"ogid\":\"0\"}";
			if (!("").equals(ogid)) {
				value = ogid.split(",");
				int len = value.length;
				for (int i = 0; i < len; i++) {
					db.eq("ogid", value[i]);
				}
			}
			ir = db.data(cont).updateAll() != value.length ? 0 : 99;

		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("content.setGroup:ogid: " + e);
			ir = 99;
		}
		return ir;
	}

	// 前台数据显示
	public JSONObject page(String wbid, int idx, int pageSize) {
		db db = bind();
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		int roleSign = getRole();
		try {
			if (roleSign == 2 || roleSign == 1 || roleSign == 0) {
				db.eq("wbid", wbid).eq("slevel", 0);
			} else {
				db.eq("wbid", wbid);
			}
			String[] strings = db.condString().split(",");
			for (String string : strings) {
				JSONArray array2 = JSONArray.toJSONArray(string);
				if (array2 == null || array2.size() == 0) {
					nlogger.logout("条件为空");
					System.out.println("条件：" + db.condString());
				}
			}
			array = db.desc("time").field("_id,mainName,time,wbid,ogid,image").dirty().page(idx, pageSize);
			object.put("total", db.dirty().count());
			object.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.Page: " + e);
			object.put("total", 0);
			object.put("toalSize", 0);
		} finally {
			db.clear();
		}
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", getImgs(dencode(array)));
		return object;
	}

	// 模拟未登录，不处理SID的page输出
	public JSONObject page(String wbid, int idx, int pageSize, JSONObject content) {
		String key;
		String value;
		String temp = "";
		String templist = "";
		JSONObject tempJson = new JSONObject();
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		db db = bind();
		if (content != null && content.size() != 0) {
			for (Object object2 : content.keySet()) {
				key = (String) object2;
				value = content.get(key).toString();
				if (("ogid").equals(key)) {
					// 根据ogid获取模版
					tempJson = getTemp(value);
				}
				db.eq(key, value);
			}
			// 获取角色权限
			db.eq("slevel", 0);
			String[] strings = db.condString().split(",");
			for (String string : strings) {
				JSONArray array2 = JSONArray.toJSONArray(string);
				if (array2 == null || array2.size() == 0) {
					nlogger.logout("条件为空");
					System.out.println("条件：" + db.condString());
				}
			}
			array = db.dirty().desc("time").field("_id,mainName,time,wbid,ogid,image").page(idx, pageSize);
			if (array != null && array.size() != 0) {
				temp = tempJson.getString("tempContent");
				temp = tempJson.getString("tempList");
				JSONObject obj;
				for (int i = 0; i < array.size(); i++) {
					obj = (JSONObject) array.get(i);
					obj.put("TemplateContent", temp);
					obj.put("Templatelist", templist);
					array.set(i, obj);
				}
			}
		}
		object.put("total", db.dirty().count());
		object.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
		db.clear();
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", getImgs(dencode(array)));

		return object;
	}

	// 获取文章所属栏目的模版信息
	private JSONObject getTemp(String ogid) {
		JSONObject tempJson = new JSONObject();
		String columTemplateContent = "";
		String columTemplatelist = "";
		if (ogid != null && !ogid.equals("")) {
			String column = appsProxy
					.proxyCall(getHost(0), appsProxy.appid() + "/15/ContentGroup/getGroupByIds/" + ogid, null, "")
					.toString();
			JSONObject object = JSONHelper.string2json(column);
			if (object != null && object.get("TemplateContent") != null && object.get("TemplateList") != null) {
				columTemplateContent = object.get("TemplateContent").toString();
				columTemplatelist = object.get("TemplateContent").toString();
			}
		}
		tempJson.puts("tempContent", columTemplateContent);
		tempJson.puts("tempList", columTemplatelist);
		return tempJson;
	}
	/*
	 * public JSONObject page(String wbid, int idx, int pageSize, JSONObject
	 * content) { JSONObject object = new JSONObject(); JSONArray array = new
	 * JSONArray(); int roleSign = getRole(); JSONObject obj = getCond(content);
	 * if (obj != null && obj.size() != 0) { db db = (db) obj.get("db"); String
	 * temp = obj.getString("tempContent"); String templist =
	 * obj.getString("tempList"); // 获取角色权限 if (roleSign == 2 || roleSign == 1
	 * || roleSign == 0) { db.eq("wbid", wbid).eq("slevel", 0); } else {
	 * db.eq("wbid", wbid); } array =
	 * db.desc("time").field("_id,mainName,time,wbid,ogid,image").dirty().page(
	 * idx, pageSize); if (array != null && array.size() != 0) { for (int i = 0;
	 * i < array.size(); i++) { obj = (JSONObject) array.get(i);
	 * obj.put("TemplateContent", temp); obj.put("Templatelist", templist);
	 * array.set(i, obj); } } object.put("total", db.dirty().count());
	 * object.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
	 * db.clear(); object.put("currentPage", idx); object.put("pageSize",
	 * pageSize); object.put("data", getImgs(dencode(array))); } return object;
	 * }
	 */

	// 处理出现sid时的page输出
	public JSONObject page2(int idx, int pageSize, JSONObject content) {
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		int roleSign = getRole();
		JSONObject obj = getCond(content);
		if (obj != null && obj.size() != 0) {
			db db = (db) obj.get("db");
			String temp = obj.getString("tempContent");
			String templist = obj.getString("tempList");
			// 获取角色权限
			if (roleSign != 0) {
				db.eq("wbid", (String) UserInfo.get("currentWeb")).eq("slevel", 0);
			} else {
				db.eq("slevel", 0);
			}
			String[] strings = db.condString().split(",");
			for (String string : strings) {
				JSONArray array2 = JSONArray.toJSONArray(string);
				if (array2 == null || array2.size() == 0) {
					nlogger.logout("条件为空");
					System.out.println("条件：" + db.condString());
				}
			}
			array = db.field("_id,mainName,time,wbid,ogid,image").dirty().desc("sort").desc("time").page(idx, pageSize);
			for (int i = 0; i < array.size(); i++) {
				obj = (JSONObject) array.get(i);
				obj.put("TemplateContent", temp);
				obj.put("Templatelist", templist);
				array.set(i, obj);
			}
			object.put("totalSize", (int) Math.ceil((double) db.dirty().count() / pageSize));
			object.put("total", db.count());
			db.clear();
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", getImgs(dencode(array)));
		}
		return object;
	}

	public JSONObject pageBack(int idx, int pageSize) {
		int roleSign = getRole();
		db db = bind();
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		try {
			// 获取角色权限
			/*
			 * if (roleSign == 5 || roleSign == 4) {
			 * db.desc("sort").desc("time"); array = db.dirty().page(idx,
			 * pageSize); } else if (roleSign == 3) { db.eq("wbid", (String)
			 * UserInfo.get("currentWeb")).desc("sort").desc("time"); array =
			 * db.dirty().page(idx, pageSize); } else { db.eq("slevel",
			 * 2).desc("sort").desc("time"); array = db.dirty().page(idx,
			 * pageSize); }
			 */
			if (UserInfo != null && UserInfo.size() != 0) {
				String wbid = UserInfo.get("currentWeb").toString();
				if (roleSign == 2 || roleSign == 1 || roleSign == 0) {
					db.eq("wbid", wbid).eq("slevel", 0).mask("r,u,d");
				} else {
					db.eq("wbid", wbid).mask("r,u,d");
				}
				String[] strings = db.condString().split(",");
				for (String string : strings) {
					JSONArray array2 = JSONArray.toJSONArray(string);
					if (array2 == null || array2.size() == 0) {
						nlogger.logout("条件为空");
						System.out.println("条件：" + db.condString());
					}
				}
				array = db.dirty().desc("sort").desc("time").page(idx, pageSize);
				object.put("totalSize", (int) Math.ceil((double) db.dirty().count() / pageSize));
				object.put("total", db.count());
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object.put("toalSize", 0);
			object.put("total", 0);
		} finally {
			db.clear();
		}
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		JSONArray array2 = dencode(array);
		object.put("data", join(getImgs(array2)));
		return object;
	}

	public JSONObject pageBack(int idx, int pageSize, JSONObject content) {
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		int roleSign = getRole();
		JSONObject obj = getCond(content);
		if (obj != null && obj.size() != 0) {
			db db = (db) obj.get("db");
			String temp = obj.getString("tempContent");
			String templist = obj.getString("tempList");
			// 获取角色权限
			if (roleSign == 3) {
				db.eq("wbid", (String) UserInfo.get("currentWeb"));
			} else if (roleSign == 2 || roleSign == 1 || roleSign == 0) {
				db.eq("slevel", 0);
			}
			String[] strings = db.condString().split(",");
			for (String string : strings) {
				JSONArray array2 = JSONArray.toJSONArray(string);
				if (array2 == null || array2.size() == 0) {
					nlogger.logout("条件为空");
					System.out.println("条件：" + db.condString());
				}
			}
			array = db.dirty().desc("sort").desc("time").page(idx, pageSize);
			for (int i = 0; i < array.size(); i++) {
				obj = (JSONObject) array.get(i);
				obj.put("TemplateContent", temp);
				obj.put("Templatelist", templist);
				array.set(i, obj);
			}
			object.put("totalSize", (int) Math.ceil((double) db.dirty().count() / pageSize));
			object.put("total", db.count());
			db.clear();
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", join(dencode(getImgs(array))));
		}
		return object;
	}

	private JSONObject getCond(JSONObject object) {
		JSONObject obj = new JSONObject();
		String key;
		String value;
		String columTemplateContent = "";
		String columTemplatelist = "";
		db db = bind();
		try {
			if (object != null) {
				for (Object object2 : object.keySet()) {
					key = (String) object2;
					value = object.getString(key);
					if (("ogid").equals(key)) {
						String column = appsProxy.proxyCall(getHost(0),
								appsProxy.appid() + "/15/ContentGroup/getGroupByIds/" + value, null, "").toString();
						JSONObject object3 = JSONHelper.string2json(column);
						if (object3 != null && object3.get("TemplateContent") != null
								&& object3.get("TemplateList") != null) {
							columTemplateContent = object3.get("TemplateContent").toString();
							columTemplatelist = object3.get("TemplateContent").toString();
						}
					}
					if (object.containsKey("_id")) {
						db.eq("_id", new ObjectId(object.get("_id").toString()));
					}
					db.eq(object2.toString(), object.get(object2.toString()));
				}
				obj.puts("db", db);
				obj.puts("tempContent", columTemplateContent);
				obj.puts("tempList", columTemplatelist);
			}
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.getCond: " + e);
		}
		return obj;
	}

	private JSONArray dencode(JSONArray array) {
		if (array == null || array.size() == 0) {
			return null;
		}
		for (int i = 0; i < array.size(); i++) {
			JSONObject object = (JSONObject) array.get(i);
			if (object.containsKey("content") && object.get("content") != "") {
				object.put("content", object.escapeHtmlGet("content"));
			}
			array.set(i, object);
		}
		return array;
	}

	private JSONObject dencode(JSONObject obj) {
		obj.put("content", obj.escapeHtmlGet("content"));
		return obj;
	}

	public JSONObject find(String id) {
		String wbid;
		String ogid;
		JSONObject preobj;
		JSONObject nextobj;
		db db = bind();
		// 获取当前数据
		JSONObject obj = db.asc("time").eq("slevel", 0).eq("_id", id).find();
		// 从当前数据获取wbid,ogid
		if (obj != null && obj.size() != 0) {
			wbid = obj.getString("wbid");
			ogid = obj.getString("ogid");
			// 获取上一篇
			preobj = db.asc("_id").eq("wbid", wbid).eq("ogid", ogid).eq("slevel", 0).gt("_id", id).field("_id,mainName")
					.find();
			nextobj = db.desc("_id").eq("wbid", wbid).eq("ogid", ogid).eq("slevel", 0).lt("_id", id)
					.field("_id,mainName").find();
			if (preobj != null && preobj.size() != 0) {
				obj.put("previd", ((JSONObject) preobj.get("_id")).getString("$oid"));
				obj.put("prevname", preobj.get("mainName"));
			}
			if (nextobj != null && nextobj.size() != 0) {
				obj.put("nextid", ((JSONObject) nextobj.get("_id")).getString("$oid"));
				obj.put("nextname", nextobj.get("mainName"));
			}
		}
		return dencode(obj);
	}

	public JSONObject selects(String oid) {
		JSONObject object = null;
		JSONObject preobj = null;
		JSONObject nextobj = null;
		String wbid;
		String ogid;
		if (oid == null || ("").equals(oid)) {
			return null;
		}
		object = findByOid(oid);
		if (object != null) {
			ogid = object.getString("ogid");
			wbid = object.getString("wbid");
			String time = object.get("time").toString();
			if (time.contains("$numberLong")) {
				time = JSONObject.toJSON(time).getString("$numberLong");
			}
			db db = bind();
			preobj = db.gte("time", Long.parseLong(time)).eq("ogid", ogid).eq("wbid", wbid).ne("_id", new ObjectId(oid))
					.asc("time").asc("sort").field("_id,mainName").limit(1).find();
			nextobj = db.lt("time", Long.parseLong(time)).eq("ogid", ogid).eq("wbid", wbid).ne("_id", new ObjectId(oid))
					.desc("time").desc("sort").field("_id,mainName").limit(1).find();
			object.put("previd", ((JSONObject) preobj.get("id")).getString("$oid"));
			object.put("prevname", preobj.get("name"));
			object.put("nextid", ((JSONObject) nextobj.get("id")).getString("$oid"));
			object.put("nextname", nextobj.get("name"));
		}
		return join(getImg(object));
	}

	public JSONObject select(String oid) {
		// 获取角色
		int roleSign = getRole();
		JSONObject object = null;
		JSONObject preobj = null;
		JSONObject nextobj = null;
		String wbid;
		String ogid;
		if (!("").equals(oid)) {
			try {
				object = new JSONObject();
				preobj = new JSONObject();
				nextobj = new JSONObject();
				object = findByOid(oid);
				if (object != null) {
					ogid = object.getString("ogid");
					wbid = object.getString("wbid");
					String slevel = object.get("slevel").toString();
					if (slevel.contains("$numberLong")) {
						slevel = JSONHelper.string2json(slevel).get("$numberLong").toString();
					}
					switch (Integer.parseInt(slevel)) {
					case 0:
						break;
					case 1:
						if (roleSign == 0) {
							return object = null;
						}
						break;
					case 2:
						if (roleSign <= 1) {
							return object = null;
						}
						break;
					case 3:
						if (roleSign <= 1) {
							return object = null;
						}
						break;
					}
					String time = object.get("time").toString();
					if (time.contains("$numberLong")) {
						time = JSONObject.toJSON(time).getString("$numberLong");
					}
					db db = bind();
					preobj = db.gte("time", Long.parseLong(time)).eq("ogid", ogid).eq("wbid", wbid)
							.ne("_id", new ObjectId(oid)).asc("time").asc("sort").field("_id,mainName").limit(1).find();
					nextobj = db.lt("time", Long.parseLong(time)).eq("ogid", ogid).eq("wbid", wbid).desc("time")
							.desc("sort").field("_id,mainName").limit(1).find();
					if (preobj != null && preobj.size() != 0) {
						object.put("previd", ((JSONObject) preobj.get("_id")).getString("$oid"));
						object.put("prevname", preobj.get("mainName"));
					}
					if (nextobj != null && nextobj.size() != 0) {
						object.put("nextid", ((JSONObject) nextobj.get("_id")).getString("$oid"));
						object.put("nextname", nextobj.get("mainName"));
					}
					object = getImg(object);
				}
			} catch (Exception e) {
				e.printStackTrace();
				nlogger.logout("Content.Select: " + e);
				object = null;
			}
		}
		return object;
	}

	public String findnew() {
		JSONObject object = bind().eq("state", 2).desc("time").desc("_id").find();
		return resultMessage(join(getImg(object)));
	}

	// 获取某个栏目下的最新的文章
	public String findnew(String ogid) {
		JSONObject object = bind().eq("ogid", ogid).desc("time").desc("_id").field("time,").find();
		return resultMessage(join(getImg(object)));
	}

	public String findnew(int size) {
		JSONArray array = bind().eq("state", 2).eq("slevel", 0).desc("time").desc("_id")
				.field("_id,mainName,time,wbid,ogid,tempid").limit(50).select();
		return resultMessage(join(dencode(array)));
	}

	public JSONArray searchByUid(String uid) {
		JSONArray array = null;
		try {
			array = bind().eq("ownid", uid).field("_id,mainName,time,wbid,ogid,tempid").limit(30).select();
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.searchByUid: " + e);
		}
		return join(dencode(array));
	}

	public int updatesort(String oid, int sortNo) {
		int i = 99;
		JSONObject object = new JSONObject();
		object.put("sort", sortNo);
		try {
			i = bind().eq("_id", new ObjectId(oid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			e.printStackTrace();
			nlogger.logout("Content.updatesort: " + e);
			i = 99;
		}
		return i;
	}

	/** 搜索返回数据以分页模式显示 **/
	// 前台页面搜索
	public JSONObject search(String wbid, int idx, int pageSize, String condString) {
		JSONObject obj = new JSONObject();
		JSONArray condArray = JSONArray.toJSONArray(condString);
		db db = bind();
		db.eq("wbid", wbid);
		db.where(condArray);
		if (condArray != null && condArray.size() != 0) {
			JSONArray array = db.dirty().field("_id,mainName,time,wbid,ogid").page(idx, pageSize);
			obj.put("total", db.dirty().count());
			obj.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
			db.clear();
			obj.put("currentPage", idx);
			obj.put("data", array);
			obj.put("pageSize", pageSize);
			String info = AddKeyWord(condArray);
			// 添加热词
			// if (condArray.contains("content")) {
			//
			// }
			/*
			 * if (!value.equals("")) { JSONObject object = new
			 * JSONObject("content", value); appsProxy.proxyCall(getHost(0),
			 * appsProxy.appid() + "/110/Word/AddWord/" + object, null,
			 * "").toString(); }
			 */
		}
		return obj;
	}

	private String AddKeyWord(JSONArray condArray) {
		JSONObject object;
		JSONArray array = new JSONArray();
		JSONObject obj = new JSONObject();
		String field;
		String value = "";
		String key = "";
		for (int i = 0; i < condArray.size(); i++) {
			object = (JSONObject) condArray.get(i);
			field = object.getString("field");
			if (field.equals("content") || field.equals("mainName")) {
				value = object.getString("value");
				key += value + ",";
			}
		}
		System.out.println(key.length());
		if (key.length() > 0) {
			key = StringHelper.fixString(key, ',');
			String[] values = key.split(",");
			for (String string : values) {
				obj.put("content", string);
				array.add(obj);
			}
		}
		String info = appsProxy
				.proxyCall(getHost(0), appsProxy.appid() + "/110/Word/AddWords/" + array.toString(), null, "")
				.toString();
		return info;
	}

	// 后台页面搜索
	/*
	 * public JSONArray searchBack(String wbid, JSONObject condString) {
	 * JSONObject obj = getSearchCond(condString); db db = (database.db)
	 * obj.get("db"); JSONArray array = db.eq("wbid", wbid).select();
	 * db.clear(); JSONObject object = new JSONObject("content",
	 * obj.getString("value")); return join(getImgs(dencode(array))); }
	 */
	public JSONObject searchBack(String wbid, int idx, int pageSize, String condString) {
		JSONObject obj = new JSONObject();
		JSONArray condArray = JSONArray.toJSONArray(condString);
		db db = bind();
		db.eq("wbid", wbid);
		db.where(condArray);
		JSONArray array = db.dirty().page(idx, pageSize);
		obj.put("total", db.dirty().count());
		obj.put("totalSize", (int) Math.ceil((double) db.count() / pageSize));
		db.clear();
		obj.put("currentPage", idx);
		obj.put("data", array);
		obj.put("pageSize", pageSize);
		return obj;
	}

	// 获取积分价值条件？？
	public void getpoint() {
		bind().field("point").limit(20).select();
	}

	// 根据文章id查询文章
	public JSONObject findByOid(String oid) {
		JSONObject object = bind().eq("_id", new ObjectId(oid)).find();
		if (object != null) {
			object = dencode(object);
			object = join(getImg(object));
		}
		return object;
	}

	private JSONObject findOid(String oid) {
		JSONObject object = bind().eq("_id", new ObjectId(oid)).find();
		if (object != null) {
			object = dencode(object);
			object = join(getImg(object));
		}
		return object;
	}

	// 根据栏目id查询文章
	public JSONArray findByGroupID(String wbid, String ogid) {
		JSONArray array = null;
		try {
			array = bind().eq("wbid", wbid).eq("slevel", 0).eq("ogid", ogid).field("_id,mainName,ogid,time")
					.desc("time").desc("sort").limit(20).select();
			array = dencode(array);
		} catch (Exception e) {
			nlogger.logout("Content.findByGroupID: " + e);
			array = null;
		}
		return array;
	}

	public JSONArray findPicByGroupID(String wbid, String ogid) {
		JSONArray array = null;
		try {
			array = bind().eq("wbid", wbid).eq("slevel", 0).eq("ogid", ogid).field("_id,mainName,ogid,time,image")
					.desc("time").desc("sort").limit(20).select();
			array = getImgs(dencode(array));
		} catch (Exception e) {
			nlogger.logout("Content.findPicByGroupID: " + e);
			array = null;
		}
		return array;
	}

	// 修改tempid
	public int setTempId(String wbid, String oid, String tempid) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("tempid", tempid);
			i = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			i = 99;
			nlogger.logout("Content.setTempId: " + e);
		}
		return i;
	}

	// 修改fatherid，同时删除ogid（ogid=""）
	public int setfatherid(String wbid, String oid, String fatherid) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			if (fatherid == null) {
				fatherid = "0";
			} else {
				object.put("ogid", "");
			}
			object.put("fatherid", fatherid);
			i = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			i = 99;
			nlogger.logout("Content.setfatherid: " + e);
		}
		return i;
	}

	// 修改对象密级
	public int setslevel(String wbid, String oid, String slevel) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("tempid", slevel);
			i = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout("Content.setSlevel: " + e);
			i = 99;
		}
		return i;
	}

	// 文章审核 state：0 草稿，1 待审核，2 审核通过 3 审核不通过
	public int review(String wbid, String oid, String managerid, String state) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("manageid", managerid);
			object.put("state", state);
			i = bind().eq("wbid", wbid).eq("_id", new ObjectId(oid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout("Content.review: " + e);
			i = 99;
		}
		return i;
	}

	public int delete(String[] arr) {
		int ir = 99;
		try {
			db db = bind().or();
			for (int i = 0; i < arr.length; i++) {
				db.eq("_id", new ObjectId(arr[i]));
			}
			ir = db.deleteAll() == arr.length ? 0 : 99;
		} catch (Exception e) {
			ir = 99;
			nlogger.logout("Content.delete: " + e);
		}
		return ir;
	}

	public JSONArray find(String ogid, int no) {
		return bind().eq("ogid", ogid).limit(20).field("image,desp").select();
	}

	public JSONArray find(String[] ogid, int no) {
		bind().or();
		for (int i = 0; i < ogid.length; i++) {
			bind().eq("ogid", ogid[i]);
		}
		return bind().limit(20).select();
	}

	public String getPrev(String ogid) {
		String prevCol = null;
		if (ogid.contains("$numberLong")) {
			nlogger.logout("输出数字为0");
			return prevCol;
		}
		try {
			prevCol = appsProxy.proxyCall(getHost(0),
					String.valueOf(appsProxy.appid()) + "/15/ContentGroup/getPrevCol/s:" + ogid, null, "").toString();
		} catch (Exception e) {
			nlogger.logout("Content.getprev: " + e);
			prevCol = null;
		}
		return prevCol;
	}

	/**
	 * 获取待添加文章信息中图片相对路径，并赋新值
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param object
	 * @return
	 *
	 */
	/*
	 * private JSONObject getimage(JSONObject object) { JSONObject obj = new
	 * JSONObject(); try { String image = object.getString("image"); obj =
	 * object.puts("image", getimage(image)); } catch (Exception e) { obj =
	 * object; } return obj; }
	 */

	/**
	 * 获取图片相对路径，去掉前缀http://....
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param imageURL
	 * @return
	 *
	 */
	private String getimage(String imageURL) {
		int i = 0;
		if (imageURL.contains("File//upload")) {
			i = imageURL.toLowerCase().indexOf("file//upload");
		}
		if (imageURL.contains("File\\upload")) {
			i = imageURL.toLowerCase().indexOf("file\\upload");
		}
		imageURL = imageURL.substring(i);
		return "\\" + imageURL;
	}

	/**
	 * 显示文章时，文章中的图片地址，图片内容文章的图片地址添上前缀 文章内容为解码后的内容
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @return
	 *
	 */
	private JSONArray getImgs(JSONArray array) {
		JSONObject object;
		JSONObject imgobj = new JSONObject();
		JSONObject conobj = new JSONObject();
		if (array == null || array.size() == 0) {
			return new JSONArray();
		}
		String id;
		for (int i = 0; i < array.size(); i++) {
			object = (JSONObject) array.get(i);
			id = ((JSONObject) object.get("_id")).getString("$oid");
			imgobj.put(id, object.get("image"));
			conobj.put(id, object.get("content"));
		}
		imgobj = getImage(imgobj);
		conobj = getContentImgs(conobj);
		for (int i = 0; i < array.size(); i++) {
			object = (JSONObject) array.get(i);
			id = ((JSONObject) object.get("_id")).getString("$oid");
			object.put("image", imgobj.get(id));
			object.put("content", conobj.get(id));
			array.set(i, object);
		}
		return array;
	}

	/**
	 * 显示文章时，文章中的图片地址，图片内容文章的图片地址添上前缀 文章内容为解码后的内容
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @return
	 *
	 */
	private JSONObject getImg(JSONObject object) {
		JSONObject imgobj = new JSONObject();
		JSONObject conobj = new JSONObject();
		String id;
		id = ((JSONObject) object.get("_id")).getString("$oid");
		imgobj.put(id, object.get("image"));
		conobj.put(id, object.get("content"));
		imgobj = getImage(imgobj);
		conobj = getContentImgs(conobj);
		object.put("image", imgobj.get(id));
		object.put("content", conobj.get(id));
		return object;
	}

	/**
	 * 图片地址
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param objimg
	 * @return
	 *
	 */
	private JSONObject getImage(JSONObject objimg) {
		String key, value;
		for (Object obj : objimg.keySet()) {
			key = obj.toString();
			value = objimg.getString(key);
			objimg.put(key, AddUrlPrefix(value));
		}
		return objimg;
	}

	/**
	 * 内容显示
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param objcontent
	 * @return
	 *
	 */
	private JSONObject getContentImgs(JSONObject objcontent) {
		String key, value;
		for (Object obj : objcontent.keySet()) {
			key = obj.toString();
			value = objcontent.getString(key);
			if (!value.equals("")) {
				Matcher matcher = ATTR_PATTERN.matcher(value.toLowerCase());
				int code = matcher.find() ? 0 : value.contains("/file/upload") ? 1 : 2;
				switch (code) {
				case 0: // 文章内容为html带图片类型的内容处理
					value = AddHtmlPrefix(value);
					break;
				case 1: // 文章内容图片类型处理[获取图片的相对路径]
					value = AddUrlPrefix(value);
					break;
				case 2: // 文章内容文字类型处理
					break;
				default:
					break;
				}
				objcontent.put(key, value);
			}
		}
		return objcontent;
	}

	/**
	 * 添加图片前缀
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param imgUrl
	 * @return
	 *
	 */
	private String AddUrlPrefix(String imageUrl) {
		if (imageUrl.equals("") || imageUrl == null) {
			return imageUrl;
		}
		String[] imgUrl = imageUrl.split(",");
		List<String> list = new ArrayList<>();
		for (String string : imgUrl) {
			if (string.contains("http:")) {
				string = getimage(string);
			}
			string = "http://" + getFileHost(1) + string;
			list.add(string);
		}
		return StringHelper.join(list);
	}

	/**
	 * 删除图片前缀
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @param imgUrl
	 * @return
	 *
	 */
	private String RemoveUrlPrefix(String imageUrl) {
		String imgurl = "";
		if (imageUrl.equals("") || imageUrl == null) {
			return imageUrl;
		}
		String[] imgUrl = imageUrl.split(",");
		List<String> list = new ArrayList<>();
		for (String string : imgUrl) {
			if (string.contains("http://")) {
				imgurl = getimage(string);
			}
			list.add(string);
		}
		if (list.size() > 1) {
			imgurl = StringHelper.join(list);
		}
		return imgurl;
	}

	// 获取栏目id及名称
	public List<JSONObject> getName(List<JSONObject> list, JSONObject object) {
		JSONObject obj = new JSONObject();
		JSONObject objID = (JSONObject) object.get("_id");
		obj.put("_id", objID.get("$oid").toString());
		obj.put("name", object.get("name").toString());
		list.add(obj);
		return list;

	}

	// 根据自定义条件进行统计
	public String getCount(JSONObject object) {
		String rs = null;
		if (object != null) {
			try {
				for (Object obj : object.keySet()) {
					bind().like(obj.toString(), object.get(obj.toString()).toString());
				}
				rs = String.valueOf(bind().count());
			} catch (Exception e) {
				nlogger.logout("Content.getCount: " + e);
				rs = null;
			}
		}
		return resultMessage(0, rs);
	}

	public JSONObject getArticleCount(String condString, String ogid) {
		JSONObject rObject = new JSONObject();
		JSONArray condArray = JSONArray.toJSONArray(condString);
		if (condArray != null && condArray.size() != 0) {

			String[] value = ogid.split(",");
			db db = bind();
			db.where(condArray).or();
			for (String tempid : value) {
				db.eq("ogid", tempid);
			}
			JSONArray array = db.count("ogid").group("ogid");
			JSONObject object;
			if (array != null && array.size() != 0) {
				for (Object object2 : array) {
					object = (JSONObject) object2;
					rObject.put(object.getString("_id"), object.getString("count"));
				}
			}
		}
		return rObject;
	}
	// 获取图片内容
	/*
	 * private JSONObject getImg(JSONObject object) { try { String imgURL =
	 * object.get("image").toString(); if (imgURL.contains("File")) { imgURL =
	 * getAppIp("file").split("/")[1] + imgURL; object.put("image", imgURL); } }
	 * catch (Exception e) { System.out.println("getImg数据错误: " +
	 * e.getMessage()); object = null; } return object; }
	 */

	/*
	 * private JSONArray getImg(JSONArray array) { if (array.size() == 0) {
	 * return array; } JSONArray array2 = null; try { JSONObject object; String
	 * imgURL; array2 = new JSONArray(); for (int i = 0, len = array.size(); i <
	 * len; i++) { object = (JSONObject) array.get(i); if
	 * (object.containsKey("image")) { imgURL = object.get("image").toString();
	 * if (imgURL.contains("upload")) { imgURL = "http://" + getFileHost(1) +
	 * imgURL; object.put("image", imgURL); } } array2.add(object); } } catch
	 * (Exception e) { System.out.println("content.getImg-Array:" +
	 * e.getMessage()); array2 = null; } return array2; }
	 */

	private JSONArray join(JSONArray array) {
		JSONArray arrays = null;
		try {
			JSONObject object;
			arrays = new JSONArray();
			for (int i = 0, len = array.size(); i < len; i++) {
				object = (JSONObject) array.get(i);
				object = join(object);
				arrays.add(object);
			}
		} catch (Exception e) {
			System.out.println("content.join:" + e.getMessage());
			arrays = null;
		}
		return arrays;
	}

	private JSONObject join(JSONObject object) {
		JSONObject tmpJSON = object;
		if (tmpJSON != null) {
			if (!tmpJSON.containsKey("tempid")) {
				nlogger.logout(tmpJSON.toJSONString());
			} else {
				tmpJSON.put("tempContent", getTemplate(tmpJSON.get("tempid").toString()));
			}
		}
		return tmpJSON;
	}

	private String getTemplate(String tid) {
		String temp = "";
		redis redis = new redis();
		try {
			if (tid.contains("$numberLong")) {
				tid = JSONHelper.string2json(tid).getString("$numberLong");
			}
			if (!("0").equals(tid)) {
				if (redis.get(tid) != null) {
					temp = redis.get(tid).toString();
				} else {
					temp = appsProxy.proxyCall(getHost(0),
							String.valueOf(appsProxy.appid()) + "/19/TemplateContext/TempFindByTid/s:" + tid, null, "")
							.toString();
					redis.set(tid, temp);
					redis.setExpire(tid, 10 * 3600);
				}
			}
		} catch (Exception e) {
			nlogger.logout(e);
			temp = "";
		}
		return temp;
	}

	/**
	 * 不同角色区分
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentModel.java
	 * 
	 * @return roleSign 0：游客；1： 普通用户即企业员工；2：栏目管理员；3：企业管理员；4：监督管理员；5：系统管理员
	 *
	 */

	private int getRole() {
		int roleSign = 0; // 游客
		if (sid != null) {
			try {
				privilige privil = new privilige(sid);
				int roleplv = privil.getRolePV(appid);
				if (roleplv >= 1000 && roleplv < 3000) {
					roleSign = 1; // 普通用户即企业员工
				}
				if (roleplv >= 3000 && roleplv < 5000) {
					roleSign = 2; // 栏目管理员
				}
				if (roleplv >= 5000 && roleplv < 8000) {
					roleSign = 3; // 企业管理员
				}
				if (roleplv >= 8000 && roleplv < 10000) {
					roleSign = 4; // 监督管理员
				}
				if (roleplv >= 10000) {
					roleSign = 5; // 总管理员
				}
			} catch (Exception e) {
				nlogger.logout(e);
				roleSign = 0;
			}
		}
		return roleSign;
	}

	/*
	 * private int getRole() { int roleSign = 0; // 游客 if (sid != null) { try {
	 * privilige privil = new privilige(sid); int roleplv = privil.getRolePV();
	 * if (roleplv > 0 && roleplv <= 2000) { roleSign = 1; // 普通用户即企业员工 } if
	 * (roleplv > 2000 && roleplv <= 4000) { roleSign = 6; // 栏目编辑员 } if
	 * (roleplv > 4000 && roleplv <= 6000) { roleSign = 2; // 栏目管理员 } if
	 * (roleplv > 6000 && roleplv <= 8000) { roleSign = 3; // 企业管理员 } if
	 * (roleplv >= 8000 && roleplv < 10000) { roleSign = 4; // 监督管理员 } if
	 * (roleplv >= 10000) { roleSign = 5; // 总管理员 } } catch (Exception e) {
	 * nlogger.logout(e); roleSign = 0; } } return roleSign; }
	 */

	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			nlogger.logout(e);
			value = "";
		}
		return value;
	}

	// 获取应用url[内网url或者外网url]，0表示内网，1表示外网
	public String getHost(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("host").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	// 获取文件url[内网url或者外网url]，0表示内网，1表示外网
	private String getFileHost(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("file").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		int roleSign = getRole();
		if (roleSign != 6) {
			object.put("state", 2);
		} else {
			if (object.containsKey("ogid")) {
				object.put("state", 1);
				// 获取栏目管理员
				String group = appsProxy.proxyCall(getHost(0),
						appsProxy.appid() + "/15/ContentGroup/getManagerByOgid/" + object.get("ogid"), null, null)
						.toString();
				JSONObject obj = JSONObject.toJSON(group);
				String manageid = obj != null ? (String) obj.get("ownid") : "";
				object.put("manageid", manageid);
			}
		}
		return object;
	}

	public String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	public String resultMessage(int code) {
		return resultMessage(code, "");
	}

	public String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
	}

	public String resultMessage(int num, String msg) {
		String message = null;
		switch (num) {
		case 0:
			message = msg;
			break;
		case 1:
			message = "内容组名称长度不合法";
			break;
		case 2:
			message = "必填项没有填";
			break;
		case 3:
			message = "该栏目已存在本文章";
			break;
		case 4:
			message = "该站点已存在本文章";
			break;
		case 5:
			message = "存在敏感词";
			break;
		case 6:
			message = "超过限制字数";
			break;
		case 7:
			message = "需要登录，才可查看该信息";
			break;
		case 8:
			message = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 9:
			message = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 10:
			message = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			message = "其他异常";
		}
		return jGrapeFW_Message.netMSG(num, message);
	}

}
