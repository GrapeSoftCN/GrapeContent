package model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mysql.fabric.xmlrpc.base.Value;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import authority.privilige;
import check.formHelper;
import database.db;
import database.userDBHelper;
import interfaceApplication.ContentGroup;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;

public class ContentGroupModel {
	private formHelper _form;
	private userDBHelper dbcontent;
	private session se;
	// private DBHelper dbcontent;
	private String appid = String.valueOf(appsProxy.appid());
	private JSONObject _obj = new JSONObject();
	private String sid = "";
	private JSONObject userInfo = null;
	private String currentId = null;

	public ContentGroupModel() {
		se = new session();
		dbcontent = new userDBHelper("objectGroup", (String) execRequest.getChannelValue("sid"));
		_form = dbcontent.getChecker();
		sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			userInfo = se.getSession(sid);
			if (userInfo != null && userInfo.size() != 0) {
				currentId = ((JSONObject) userInfo.get("_id")).getString("$oid");
			}
		}
	}

	private db bind() {
		return dbcontent.bind(appsProxy.appidString());
	}

	public db getdb() {
		return bind();
	}

	private formHelper getForm() {
		_form.putRule("name", formHelper.formdef.notNull);
		_form.putRule("type", formHelper.formdef.notNull);
		_form.putRule("wbid", formHelper.formdef.notNull);
		return _form;
	}

	public JSONObject find_contentnamebyName(String name, String type) {
		JSONObject object = bind().eq("name", name).eq("type", type).find();
		return object;
	}

	private JSONObject findBywbid(String name, String type, String wbid, String fatherid) {
		JSONObject object = bind().eq("name", name).eq("type", type).eq("wbid", wbid).eq("fatherid", fatherid).find();
		return object;
	}

	// 根据类型查询栏目，指定数量
	public String findByType(String wbid, String type, int no) {
		db db = bind();
		if (wbid != null && !wbid.equals("")) {
			db.eq("wbid", wbid);
		}
		JSONArray array = db.eq("contentType", type).limit(no).select();
		return resultMessage(join(array));
	}

	public String findByTypes(String wbid, String type, int no) {
		db db = bind();
		if (wbid != null && !wbid.equals("")) {
			db.eq("wbid", wbid);
		}
		JSONArray array = db.eq("contentType", type).field("_id,name").limit(no).select();
		return resultMessage(array);
	}

	/**
	 * 
	 * @param groupinfo
	 * @return 1 内容组名称超过指定长度 2必填项没有填 3 表示该内容组已存在
	 * 
	 *//*
	public String AddGroup(JSONObject groupinfo) {
		int role = getRoleSign();
		if (role == 6) {
			return resultMessage(4);
		}
		JSONObject object = null;
		try {
			if (groupinfo == null) {
				return resultMessage(0, "内容组插入失败");
			}
			if (!getForm().checkRuleEx(groupinfo)) {
				return resultMessage(2, "");
			}
			String name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过300个字数
			if (!check_name(name)) {
				return resultMessage(1, "");
			}
			String wbid = groupinfo.get("wbid").toString();
			String type = groupinfo.get("type").toString();
			String Fatherid = groupinfo.get("fatherid").toString();
			if (findBywbid(name, type, wbid, Fatherid) != null) {
				return resultMessage(3, "");
			}
			String info = bind().data(groupinfo).insertOnce().toString();
			object = new JSONObject();
			object = find(info);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}*/

	public String AddGroup(JSONObject groupInfo) {
		String result = resultMessage(99);
		String wbid = "";
		String contant = "0";
		if (groupInfo != null && groupInfo.size() != 0) {
			if (groupInfo.containsKey("Contant")) {
				contant = groupInfo.getString("Contant");
			}
			if (userInfo != null && userInfo.size() != 0) {
				wbid = userInfo.getString("currentWeb");
			} else {
				if (groupInfo.containsKey("wbid")) {
					wbid = groupInfo.getString("wbid");
				}
			}
		}
		switch (contant) {
		case "0": // 不影响下级网站
			result = Add(groupInfo);
			break;
		case "1": // 下级网站同时新增栏目
			result = AddAllColumn(groupInfo,wbid);
			break;
		}
		if (!result.contains("errorcode")) {
			db db = getdb();
			JSONObject object = db.eq("wbid", wbid).eq("name", groupInfo.getString("name")).find();
			result = object.toJSONString();
		}
		return result;
	}

	/**
	 * 新增操作
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param groupinfo
	 * @return
	 *
	 */
	private String Add(JSONObject groupinfo) {
		if (!getForm().checkRuleEx(groupinfo)) {
			return resultMessage(2, "");
		}
		String name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过300个字数
		if (!check_name(name)) {
			return resultMessage(1, "");
		}
		String wbid = groupinfo.get("wbid").toString();
		String type = groupinfo.get("type").toString();
		String Fatherid = groupinfo.get("fatherid").toString();
		if (findBywbid(name, type, wbid, Fatherid) != null) {
			return resultMessage(3, "");
		}
		String info = bind().data(groupinfo).insertOnce().toString();
		return info;
	}

	private String AddAllColumn(JSONObject groupinfo,String wbid) {
		String result = resultMessage(99);
		String[] value;
		if (groupinfo != null && groupinfo.size() != 0) {
			if (!wbid.equals("")) {
				value = getWeb(wbid);
				result = AddAll(groupinfo, value);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private String AddAll(JSONObject groupinfo, String[] wbid) {
		String fatherid, groupName,result = resultMessage(99);
		db db = getdb();
		if (groupinfo.containsKey("fatherid")) {
			fatherid = groupinfo.getString("fatherid");
			if (fatherid.equals("") || fatherid.equals("0")) { // 添加一级栏目
				if (wbid != null) {
					for (String id : wbid) {
						groupinfo.put("wbid", id);
						result = Add(groupinfo); // 新增操作
					}
				}
			} else {
				// 根据fatherid获取栏目名称
				JSONObject temp = find(fatherid);
				if (temp != null && temp.size() != 0) {
					groupName = temp.getString("name");
					db.or();
					for (String string : wbid) {
						db.eq("wbid", string);
					}
					db.and().eq("name", groupName);
					JSONArray ColumnArray = db.select();
					JSONObject tempobj = getOgid(ColumnArray);
					if (tempobj != null && tempobj.size() != 0) {
						for (String string : wbid) {
							groupinfo.put("wbid", string);
							groupinfo.put("fatherid", tempobj.getString(string));
							result = Add(groupinfo);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * 重整栏目id和网站id
	 * @project	GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param ColumnArray
	 * @return  {wbid:ogid,wbid:ogid...}
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getOgid(JSONArray ColumnArray) {
		JSONObject object, rObject = new JSONObject();
		String wbid, ogid;
		if (ColumnArray != null && ColumnArray.size() != 0) {
			for (Object obj : ColumnArray) {
				object = (JSONObject) obj;
				wbid = object.getString("wbid");
				ogid = ((JSONObject) object.get("_id")).getString("$oid");
				rObject.put(wbid, ogid);
			}
		}
		return rObject;
	}

	/**
	 * 获取当前网站所有下级网站id，包含自身网站
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param wbid
	 * @return
	 *
	 */
	private String[] getWeb(String wbid) {
		String[] value = null;
		wbid = ContentGroup.getRWbid(wbid);
		String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid, null, null);// 获取下级网站
		if (temp != null && !temp.equals("")) {
			value = temp.split(",");
		}
		return value;
	}

	/**
	 * 栏目信息修改
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param ogid
	 * @param groupinfo
	 * @return
	 *
	 */
	public int UpdateGroup(String ogid, JSONObject groupinfo) {
		String wbid = "";
		String name = "";
		// int role = getRoleSign();
		// if (role == 6) {
		// return 4;
		// }
		int i = 99;
		try {
			// 根据栏目获取网站id
			JSONObject object = findWeb(ogid);
			if (object != null && object.size() != 0) {
				wbid = object.getString("wbid");
				name = object.getString("name");
			}
			if (!wbid.equals("")) {
				wbid = ContentGroup.getRWbid(wbid);
				i = Update(ogid, groupinfo, wbid, name);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	/**
	 * 修改栏目信息，0为不影响下级栏目，1为影响下级栏目
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param ogid
	 * @param groupinfo
	 * @param wbid
	 * @return
	 *
	 */
	private int Update(String ogid, JSONObject groupinfo, String wbid, String name) {
		int i = 99;
		String contant = "0", names = "";
		db db = bind();
		if (groupinfo != null && groupinfo.size() != 0) {
//			if (groupinfo.containsKey("name")) {
//				name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过20个字数
//				if (!check_name(names)) {
//					return 1;
//				}
//			}
			if (groupinfo.containsKey("Contant")) { // contant:修改文章公开状态是否影响下级网站
				contant = groupinfo.getString("Contant");
				groupinfo.remove("Contant");
			}
			if (name.equals("")) {
				contant = "0";
			}
			switch (contant) {
			case "0":
				i = db.eq("_id", new ObjectId(ogid)).data(groupinfo).update() != null ? 0 : 99;
				break;
			case "1": // 修改栏目信息，影响下级网站同名栏目
				i = updateCid(wbid, name, groupinfo);
				break;
			}
		}
		return i;
	}

	/**
	 * 修改主站，同时修改子站
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param wbid
	 *            主站点id
	 * @param name
	 *            栏目名称
	 * @param groupinfo
	 *            待修改数据
	 * @return
	 *
	 */
	private int updateCid(String wbid, String name, JSONObject groupinfo) {
		JSONObject info;
		int i = 99;
		db db = bind();
		String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid, null, null);// 获取下级网站
		if (!temp.equals("")) {
			String[] value = temp.split(",");
			db.or();
			for (String string : value) {
				db.eq("wbid", string);
			}
			if (name.equals("")) {
				return 99;
			}
			JSONArray array = db.and().eq("name", name).select(); // 获取下级网站信息，同时包含主站
			JSONObject tempobj = getChildData(array, groupinfo);
			if (tempobj != null && tempobj.size() > 0) {
				for (String string : value) {
					if (tempobj.containsKey(string)) {
						info = JSONObject.toJSON(tempobj.getString(string));
						if (info != null && info.size() > 0) {
							i = db.eq("name", name).eq("wbid", string).data(info).update() != null ? 0 : 99;
						}
					}
				}
			}
		}
		return i;
	}

	/**
	 * 获取下级栏目数据
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param array
	 * @param objects
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getChildData(JSONArray array, JSONObject objects) {
		JSONObject objtemp, object = new JSONObject();
		String wbid;
		if (array != null && array.size() != 0) {
			for (Object obj : array) {
				objtemp = (JSONObject) obj;
				wbid = objtemp.getString("wbid");
				objtemp.remove("_id");
				objtemp.put("name", objects.getString("name"));
				objtemp.put("contentType", objects.getString("contentType"));
				objtemp.put("tempList", objects.getString("tempContent"));
				objtemp.put("tempContent", objects.getString("tempContent"));
				objtemp.put("isreview", objects.getString("isreview"));
				objtemp.put("slevel", objects.getString("slevel"));
				objtemp.put("sort", objects.getString("sort"));
				objtemp.put("editCount", objects.getString("editCount"));
				objtemp.put("timediff", Long.parseLong(objects.getString("timediff")));
				object.put(wbid, RemoveNumberLong(objtemp));
			}
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	private JSONObject RemoveNumberLong(JSONObject object) {
		String temp;
		String[] param = { "type", "sort", "isdelete", "isvisble", "clickCount", "u", "r", "d", "time", "timediff",
				"editCount","lastTime" };
		if (object.containsKey("fatherid")) {
			temp = object.getString("fatherid");
			if (temp.contains("$numberLong")) {
				temp = JSONObject.toJSON(temp).getString("$numberLong");
			}
			object.put("fatherid", temp);
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

	public int DeleteGroup(String ogid) {
		int role = getRoleSign();
		if (role == 6) {
			return 4;
		}
		return bind().eq("_id", new ObjectId(ogid)).delete() != null ? 0 : 99;
	}

	public JSONObject find(String ogid) {
		JSONObject object = bind().eq("_id", new ObjectId(ogid)).find();
		return object != null ? join(object) : null;
	}

	@SuppressWarnings("unchecked")
	public JSONArray finds(String ogid) {
		JSONArray array = null;
		String[] value = ogid.split(",");
		JSONObject object;
		int l = 0;
		db db = bind().or();
		try {
			for (String string : value) {
				db.eq("_id", string);
			}
			array = db.field("_id,tempContent,tempList").select();
			l = array.size();
			String id;
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				id = ((JSONObject) object.get("_id")).getString("$oid");
				object.put("_id", id);
				array.set(i, object);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return (array != null && array.size() != 0) ? join(array) : null;
	}

	public JSONObject findOwnid(String ogid) {
		return bind().eq("_id", new ObjectId(ogid)).field("ownid").find();
	}

	// 查询文章，显示id，name，fatherid
	public JSONObject findWeb(String ogid) {
		return bind().eq("_id", new ObjectId(ogid)).field("_id,name,fatherid,wbid").find();
	}

	public JSONArray select(String contentInfo) {
		JSONArray array = null;
		JSONObject object = JSONHelper.string2json(contentInfo);
		if (object != null) {
			try {
				for (Object object2 : object.keySet()) {
					if ("_id".equals(object2.toString())) {
						bind().eq("_id", new ObjectId(object.get("_id").toString()));
					} else {
						bind().eq(object2.toString(), object.get(object2.toString()));
					}
				}
				array = bind().select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return array;
	}

	public String pages(String wbid, int idx, int pageSize, String condString) {
		JSONArray array = null;
		JSONObject CondObject = JSONObject.toJSON(condString);
		String key;
		long totalSize = 0;
		int rolePlv = getRoleSign();
		db db = bind();
		try {
			if (wbid != null && !wbid.equals("")) {
				db.eq("wbid", wbid).mask("r,u,d");
			}
			if (CondObject != null && CondObject.size() != 0) {
				for (Object obj : CondObject.keySet()) {
					key = obj.toString();
					db.eq(key, CondObject.get(key));
				}
			}
			if (rolePlv == 7) {
				db.eq("editor", currentId);
			}
			array = db.dirty().asc("sort").asc("_id").page(idx, pageSize);
			totalSize = db.pageMax(pageSize);
		} catch (Exception e) {
			nlogger.logout(e);
		} finally {
			db.clear();
		}
		return pageShow(array, idx, pageSize, totalSize);
	}

	@SuppressWarnings("unchecked")
	public String pageShow(JSONArray array, int idx, int pageSize, long totalSize) {
		JSONObject object = new JSONObject();
		object.put("data", (array != null && array.size() != 0) ? join(array) : new JSONArray());
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("totalSize", totalSize);
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public int setfatherid(String ogid, int fatherid) {
		int role = getRoleSign();
		if (role == 6) {
			return 4;
		}
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("fatherid", fatherid);
			i = UpdateGroup(ogid, object);
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String ogid, int num) {
		int role = getRoleSign();
		if (role == 6) {
			return 4;
		}
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("sort", num);
			i = UpdateGroup(ogid, object);
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setTempId(String ogid, String tempid) {
		int role = getRoleSign();
		if (role == 6) {
			return 4;
		}
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("tempid", tempid);
			i = UpdateGroup(ogid, object);
		} catch (Exception e) {
			i = 99;
			nlogger.logout(e);
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setslevel(String ogid, int slevel) {
		int role = getRoleSign();
		if (role == 6) {
			return 4;
		}
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("slevel", slevel);
			i = UpdateGroup(ogid, object);
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	public int delete(String[] arr) {
//		int role = getRoleSign();
//		if (role == 6) {
//			return 4;
//		}
		int ir = 99;
		try {
			bind().or();
			for (int i = 0; i < arr.length; i++) {
				bind().eq("_id", new ObjectId(arr[i]));
			}
			ir = bind().deleteAll() == arr.length ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			ir = 99;
		}
		return ir;
	}

	public boolean check_name(String name) {
		return (name.length() > 0 && name.length() <= 300);
	}

	// 根据上级栏目id
	public String findByFatherid(String fatherid) {
		String name = null;
		JSONArray array = bind().eq("fatherid", fatherid).select();
		for (Object object : array) {
			JSONObject _obj = (JSONObject) object;
			name = _obj.get("name").toString();
		}
		return name;
	}

	// 根据上级栏目id，获取该栏目所有子栏目数据
	public String getColumnByFid(String ogid) {
		return resultMessage(bind().eq("fatherid", ogid).select());
	}

	// 获取所有子栏目的id
	public JSONArray getColumn(String ogid) {
		if (ogid.contains(",")) {
			bind().or();
			String[] value = ogid.split(",");
			for (int i = 0, len = value.length; i < len; i++) {
				bind().eq("fatherid", value[i]);
			}
		} else {
			bind().eq("fatherid", ogid);
		}
		return bind().field("_id").limit(20).select();
	}

	public List<String> getList(JSONArray array) {
		List<String> list = new ArrayList<>();
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			JSONObject obj = (JSONObject) object.get("_id");
			list.add(obj.get("$oid").toString());
		}
		return list;
	}

	// 获取栏目id及名称
	@SuppressWarnings("unchecked")
	public List<JSONObject> getName(List<JSONObject> list, JSONObject object) {
		JSONObject obj = new JSONObject();
		if (object != null) {
			JSONObject objID = (JSONObject) object.get("_id");
			obj.put("_id", objID.get("$oid").toString());
			obj.put("name", object.get("name").toString());
			list.add(obj);
		}
		return list;

	}

	public JSONArray getColumnByWbid(String wbid) {
		return bind().eq("wbid", wbid).select();
	}

	// 获取模版id，合并到jsonarray
	@SuppressWarnings("unchecked")
	private JSONArray join(JSONArray array) {
		JSONObject object;
		String content = "", list = "";
		int l = array.size();
		if (array == null || array.size() == 0) {
			return array;
		}
		try {
			JSONObject tempTemplateObj = getTempInfo(array);
			String tContentID = null;
			String tListID = null;
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				if (tempTemplateObj != null) {
					tContentID = object.getString("tempContent");
					if (tempTemplateObj.containsKey(tContentID)) {
						content = tempTemplateObj.getString(tContentID);
					}
					tListID = object.getString("tempList");
					if (tempTemplateObj.containsKey(tListID)) {
						list = tempTemplateObj.getString(tListID);
					}
				}
				object.put("TemplateList", list);
				object.put("TemplateContent", content);
				array.set(i, object);
			}
		} catch (Exception e) {
			nlogger.logout(e);
		}
		return array;
	}

	// 获取模版id，合并到jsonarray
	@SuppressWarnings("unchecked")
	private JSONObject join(JSONObject object) {
		JSONArray array = new JSONArray();
		array.add(object);
		JSONObject tempTemplateObj = getTempInfo(array);
		if (tempTemplateObj != null) {
			object.put("TemplateContent", tempTemplateObj.get(object.getString("tempContent")));
			object.put("TemplateList", tempTemplateObj.get(object.getString("tempList")));
		}
		return object;
	}

	/**
	 * 批量获取模版名称
	 * 
	 * @project GrapeContent
	 * @package model
	 * @file ContentGroupModel.java
	 * 
	 * @param array
	 * @return
	 *
	 */
	private JSONObject getTempInfo(JSONArray array) {
		JSONObject object = new JSONObject(), tempobj = null;
		String content = null;
		String list = null;
		String tid = "";
		String temp = "";
		if (array != null && array.size() != 0) {
			int l = array.size();
			for (int i = 0; i < l; i++) {
				object = (JSONObject) array.get(i);
				content = object.getString("tempContent");
				list = object.getString("tempList");
				if (!content.equals("") && !content.equals("0")) {
					tid += content + ",";
				}
				if (!list.equals("") && !list.equals("0")) {
					tid += list + ",";
				}
			}
			if (!tid.equals("") && tid.length() > 0) {
				tid = StringHelper.fixString(tid, ',');
				if (!tid.equals("")) {
					temp = appsProxy.proxyCall("/GrapeTemplate/TemplateContext/TempFindByTids/s:" + tid, null, null)
							.toString();
					// temp =
					// appsProxy.proxyCall("/GrapeTemplate/TemplateContext/TempFindByTids/s:"
					// + tid).toString();
					tempobj = JSONObject.toJSON(temp);
				}
			}
		}
		return (tempobj != null && tempobj.size() != 0) ? tempobj : null;
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (object != null) {
			if (map.entrySet() != null) {
				Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
					if (!object.containsKey(entry.getKey())) {
						object.put(entry.getKey(), entry.getValue());
					}
				}
			}

		}
		return object;
	}

	// 设置栏目管理员
	@SuppressWarnings("unchecked")
	public String setColumManage(String ogid, String userid) {
		int code = 99;
		JSONObject object = find(ogid);
		if (object != null) {
			try {
				String ownid = object.get("ownid").toString();
				if (!("").equals(ownid)) {
					userid = String.join(",", ownid, userid);
				}
				object.put("ownid", userid);
				code = UpdateGroup(ogid, object);
				// code = bind().eq("_id", new
				// ObjectId(ogid)).data(object).update() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return resultMessage(code, "设置栏目管理员成功");
	}

//	@SuppressWarnings("unchecked")
//	public JSONArray getPrev(String ogid) {
//		JSONArray rList = new JSONArray();
//		JSONObject temp = new JSONObject();
//		String tempID = ogid;
//		if (ogid != null && !ogid.equals("")) {
//			while (temp != null) {
//				if (!tempID.equals("0")) {
//					temp = bind().eq("_id", tempID).field("_id,wbid,name,fatherid").find();
//					if (temp != null) {
//						rList.add(temp);
//						if (temp.containsKey("fatherid")) {
//							tempID = temp.getString("fatherid");
//							if (tempID.contains("$numberLong")) {
//								tempID = JSONObject.toJSON(tempID).getString("$numberLong");
//								if (tempID.equals("0")) {
//									temp = null;
//								}
//							}
//						} else {
//							temp = null;
//						}
//					}
//				} else {
//					temp = null;
//				}
//			}
//		}
//		for (int i = 0; i < rList.size(); i++) {
//			temp = (JSONObject) rList.get(i);
//			tempID = ((JSONObject) temp.get("_id")).getString("$oid");
//			temp.put("_id", tempID);
//			rList.set(i, temp);
//		}
//		return rList;
//	}

	@SuppressWarnings("unchecked")
	public JSONArray getPrev(String ogid) {
		List<JSONObject> list = new ArrayList<>();
		JSONArray rList = new JSONArray();
		JSONObject temp = new JSONObject();
		String tempID = ogid;
		if (ogid != null && !ogid.equals("")) {
			while (temp != null) {
				if (!tempID.equals("0")) {
					temp = bind().eq("_id", tempID).field("_id,wbid,name,fatherid").find();
					if (temp != null) {
						list.add(temp);
//						rList.add(temp);
						if (temp.containsKey("fatherid")) {
							tempID = temp.getString("fatherid");
							if (tempID.contains("$numberLong")) {
								tempID = JSONObject.toJSON(tempID).getString("$numberLong");
							}
							if (tempID.equals("0")) {
								temp = null;
							}
						} else {
							temp = null;
						}
					}
				} else {
					temp = null;
				}
			}
		}
		Collections.reverse(list);
		for (Object object : list) {
			JSONObject object2 = (JSONObject)object;
			rList.add(object2);
		}
		for (int i = 0; i < rList.size(); i++) {
			temp = (JSONObject) rList.get(i);
			tempID = ((JSONObject) temp.get("_id")).getString("$oid");
			temp.put("_id", tempID);
			rList.set(i, temp);
		}
		return rList;
	}
	
	/**
	 * 根据角色plv，获取角色级别
	 * 
	 * @project GrapeSuggest
	 * @package interfaceApplication
	 * @file Suggest.java
	 * 
	 * @return
	 *
	 */
	public int getRoleSign() {
		int roleSign = 0; // 游客
		if (sid != null && !sid.equals("")) {
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
				if (roleplv >= 10000 && roleplv < 12000) {
					roleSign = 5; // 总管理员
				}
				if (roleplv >= 12000 && roleplv < 14000) {
					roleSign = 6; // 总管理员，只读权限
				}
				if (roleplv >= 14000 && roleplv < 16000) {
					roleSign = 7; // 栏目编辑人员
				}
			} catch (Exception e) {
				nlogger.logout(e);
				roleSign = 0;
			}
		}
		return roleSign;
	}

	public String resultMessage(int num) {
		return resultMessage(num, "");
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
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
			message = "不允许重复添加";
			break;
		case 4:
			message = "没有操作权限";
			break;
		default:
			message = "其他异常";
		}
		return jGrapeFW_Message.netMSG(num, message);
	}
}
