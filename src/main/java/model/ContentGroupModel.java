package model;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import authority.privilige;
import authority.userDBHelper;
import cache.redis;
import database.db;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import nlogger.nlogger;
import rpc.execRequest;

public class ContentGroupModel {
	private static formHelper _form;
	private static userDBHelper dbcontent;
	private JSONObject _obj = new JSONObject();

	static {
		dbcontent = new userDBHelper("objectGroup",(String)execRequest.getChannelValue("sid"));
		_form = dbcontent.getChecker();
	}

	public ContentGroupModel() {
		_form.putRule("name", formHelper.formdef.notNull);
		_form.putRule("type", formHelper.formdef.notNull);
	}

	private db bind() {
		return dbcontent.bind(appsProxy.appid() + "");
	}

	// private db bind() {
	// return new userDBHelper("objectGroup",
	// (String)execRequest.getChannelValue("sid")).bind(String.valueOf(appsProxy.appid()));
	// }

	private privilige getPrivil(String sid) {
		return new privilige(sid);
	}

	public JSONObject find_contentnamebyName(String name, String type) {
		JSONObject object = bind().eq("name", name).eq("type", type).find();
		return object;
	}

	// 根据类型查询栏目，指定数量
	public String findByType(String type, int no) {
		JSONArray array = bind().eq("type", type).limit(no).select();
		return resultMessage(join(array));
	}

	/**
	 * 
	 * @param groupinfo
	 * @return 1 内容组名称超过指定长度 2必填项没有填 3 表示该内容组已存在
	 * 
	 */
	public String AddGroup(JSONObject groupinfo) {
		if (groupinfo == null) {
			return resultMessage(0, "内容组插入失败");
		}
		if (!_form.checkRuleEx(groupinfo)) {
			return resultMessage(2, "");
		}
		String name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过20个字数
		if (!check_name(name)) {
			return resultMessage(1, "");
		}
		String type = groupinfo.get("type").toString();
		if (find_contentnamebyName(name, type) != null) {
			return resultMessage(3, "");
		}
		String info = bind().data(groupinfo).insertOnce().toString();
		return resultMessage(find(info));
	}

	public int UpdateGroup(String ogid, JSONObject groupinfo) {
		int i = 99;
		try {
			if (groupinfo.containsKey("name")) {
				String name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过20个字数
				if (!check_name(name)) {
					return 1;
				}
			}
			i = bind().eq("_id", new ObjectId(ogid)).data(groupinfo).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	public int DeleteGroup(String ogid) {
		return bind().eq("_id", new ObjectId(ogid)).delete() != null ? 0 : 99;
	}

	public JSONObject find(String ogid) {
		return join(bind().eq("_id", new ObjectId(ogid)).find());
	}

	public JSONArray select(String contentInfo) {
		JSONArray array = null;
		JSONObject object = JSONHelper.string2json(contentInfo);
		if (object != null) {
			try {
				for (Object object2 : object.keySet()) {
					if ("_id".equals(object2.toString())) {
						bind().eq("_id", new ObjectId(object.get("_id").toString()));
					}
					bind().like(object2.toString(), object.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize) {
		JSONObject obj = getSessPlv(execRequest.getChannelValue("sid"));
		JSONObject object = null;
		if (obj != null) {
			try {
				JSONArray array = new JSONArray();
				// 获取角色权限
				int roleplv = Integer.parseInt(obj.get("rolePlv").toString());
				if (roleplv > 10000) {
					array = bind().page(idx, pageSize);
				}
				if (roleplv > 5000 && roleplv <= 10000) {
					array = bind().eq("wbid", (String) obj.get("currentWeb")).page(idx, pageSize);
				}
				if (roleplv > 3000 && roleplv <= 5000) {
					JSONObject oid = (JSONObject) obj.get("_id");
					array = bind().like("ownid", oid.get("$oid").toString()).eq("wbid", (String) obj.get("currentWeb"))
							.page(idx, pageSize);
				}
				if (roleplv == 0) { // 游客
					array = bind().page(idx, pageSize);
				}
				object = new JSONObject();
				object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
				object.put("currentPage", idx);
				object.put("pageSize", pageSize);
				object.put("data", join(array));
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize, JSONObject GroupInfo) {
		JSONObject obj = getSessPlv(execRequest.getChannelValue("sid"));
		JSONObject object = null;
		if (GroupInfo != null) {
//			if (obj != null) {
				try {
					JSONArray array = new JSONArray();
					for (Object object2 : GroupInfo.keySet()) {
						if (GroupInfo.containsKey("_id")) {
							bind().eq("_id", new ObjectId(GroupInfo.get("_id").toString()));
						}
						bind().eq(object2.toString(), GroupInfo.get(object2.toString()));
					}
					// 获取角色权限
					int roleplv = Integer.parseInt(obj.get("rolePlv").toString());
					if (roleplv > 10000) { // 系统管理员
						array = bind().page(idx, pageSize);
					}
					if (roleplv > 5000 && roleplv <= 10000) { // 网站管理员
						array = bind().eq("wbid", (String) obj.get("currentWeb")).page(idx, pageSize);
					}
					if (roleplv > 3000 && roleplv <= 5000) { // 栏目管理员
						JSONObject oid = (JSONObject) obj.get("_id");
						array = bind().like("ownid", oid.get("$oid").toString())
								.eq("wbid", (String) obj.get("currentWeb")).page(idx, pageSize);
					}
					// 普通用户
					if (roleplv == 0) { // 游客
						array = bind().page(idx, pageSize);
					}
					object = new JSONObject();
					object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
					object.put("currentPage", idx);
					object.put("pageSize", pageSize);
					object.put("data", join(array));
				} catch (Exception e) {
					nlogger.logout(e);
					object = null;
				}
//			}
		}
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public int setfatherid(String ogid, int fatherid) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("fatherid", fatherid);
			i = UpdateGroup(ogid, object);
//			i = bind().eq("_id", new ObjectId(ogid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String ogid, int num) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("sort", num);
			i = UpdateGroup(ogid, object);
//			i = bind().eq("_id", new ObjectId(ogid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setTempId(String ogid, String tempid) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("tempid", tempid);
			i = UpdateGroup(ogid, object);
//			i = bind().eq("_id", new ObjectId(ogid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			i = 99;
			nlogger.logout(e);
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	public int setslevel(String ogid, int slevel) {
		int i = 99;
		JSONObject object = null;
		try {
			object = new JSONObject();
			object.put("slevel", slevel);
			i = UpdateGroup(ogid, object);
//			i = bind().eq("_id", new ObjectId(ogid)).data(object).update() != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			i = 99;
		}
		return i;
	}

	public int delete(String[] arr) {
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
		return (name.length() > 0 && name.length() <= 20);
	}

	//根据上级栏目id
	public String findByFatherid(String fatherid) {
		String name = null;
		JSONArray array = bind().eq("fatherid", fatherid).select();
		for (Object object : array) {
			JSONObject _obj = (JSONObject) object;
			name = _obj.get("name").toString();
		}
		return name;
	}

	//根据上级栏目id，获取该栏目所有子栏目数据
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
		JSONObject objID = (JSONObject) object.get("_id");
		obj.put("_id", objID.get("$oid").toString());
		obj.put("name", object.get("name").toString());
		list.add(obj);
		return list;

	}

	// 获取模版id，合并到jsonarray
	@SuppressWarnings("unchecked")
	private JSONArray join(JSONArray array) {
		JSONArray arrays = null;
		try {
			arrays = new JSONArray();
			for (int i = 0, len = array.size(); i < len; i++) {
				JSONObject object = (JSONObject) array.get(i);
				object = join(object);
				if (object != null) {
					arrays.add(object);
				}
			}
		} catch (Exception e) {
			nlogger.logout(e);
			arrays = null;
		}
		return arrays;
	}

	// 获取模版id，合并到jsonarray
	@SuppressWarnings("unchecked")
	private JSONObject join(JSONObject object) {
		JSONObject oj = object;
		if (oj != null) {
			try {
				oj.put("tempContent", getTemplate(object.get("tempContent").toString()));
				oj.put("tempList", getTemplate(object.get("tempList").toString()));
			} catch (Exception e) {
				oj = null;
				nlogger.logout(e);
			}
		}
		return oj;
	}

	private String getTemplate(String tid) {
		String temp = "";
		redis redis = new redis();
		try {
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
//				code = bind().eq("_id", new ObjectId(ogid)).data(object).update() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return resultMessage(code, "设置栏目管理员成功");
	}

	// 获取会话信息数据
	@SuppressWarnings("unchecked")
	private JSONObject getSessPlv(Object object) {
		JSONObject object2 = null;
		try {
			object2 = new JSONObject();
			if (object != null) {
				object2.put("rolePlv", getPrivil(object.toString()).getRolePV());
			} else {
				object2.put("rolePlv", 0);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object2 = null;
		}
		return object2;
	}

	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
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

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONArray array) {
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
			message = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 5:
			message = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 6:
			message = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			message = "其他异常";
		}
		return jGrapeFW_Message.netMSG(num, message);
	}
}
