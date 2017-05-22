package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;

public class ContentGroupModel {
	private static formHelper _form;
	private static DBHelper dbcontent;
	private JSONObject _obj = new JSONObject();

	static {
		dbcontent = new DBHelper(appsProxy.configValue().get("db").toString(),
				"objectGroup");
		_form = dbcontent.getChecker();
	}

	public ContentGroupModel() {
		// 获取用户id

		_form.putRule("name", formHelper.formdef.notNull);
		_form.putRule("type", formHelper.formdef.notNull);
	}

	private db bind() {
		return dbcontent.bind(appsProxy.appid() + "");
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
		if (groupinfo.containsKey("name")) {
			String name = groupinfo.get("name").toString(); // 内容组名称长度最长不能超过20个字数
			if (!check_name(name)) {
				return 1;
			}
		}
		return bind().eq("_id", new ObjectId(ogid)).data(groupinfo)
				.update() != null ? 0 : 99;
	}

	public int DeleteGroup(String ogid) {
		return bind().eq("_id", new ObjectId(ogid)).delete() != null ? 0
				: 99;
	}

	public JSONObject find(String ogid) {
		return join(bind().eq("_id", new ObjectId(ogid)).find());
	}

	@SuppressWarnings("unchecked")
	public JSONArray select(String contentInfo) {
		JSONObject object = JSONHelper.string2json(contentInfo);
		Set<Object> set = object.keySet();
		for (Object object2 : set) {
			if ("_id".equals(object2.toString())) {
				bind().eq("_id", new ObjectId(object.get("_id").toString()));
			}
			bind().eq(object2.toString(), object.get(object2.toString()));
		}
		return bind().limit(20).select();
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize) {
		JSONArray array = bind().page(idx, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", join(array));
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public String page(int idx, int pageSize, JSONObject GroupInfo) {
		for (Object object2 : GroupInfo.keySet()) {
			if (GroupInfo.containsKey("_id")) {
				bind().eq("_id",
						new ObjectId(GroupInfo.get("_id").toString()));
			}
			bind().eq(object2.toString(), GroupInfo.get(object2.toString()));
		}
		JSONArray array = bind().dirty().page(idx, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", join(array));
		return resultMessage(object);
	}

	@SuppressWarnings("unchecked")
	public int setfatherid(String ogid, int fatherid) {
		JSONObject object = new JSONObject();
		object.put("fatherid", fatherid);
		return bind().eq("_id", new ObjectId(ogid)).data(object)
				.update() != null ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setsort(String ogid, int num) {
		JSONObject object = new JSONObject();
		object.put("sort", num);
		return bind().eq("_id", new ObjectId(ogid)).data(object)
				.update() != null ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setTempId(String ogid, String tempid) {
		JSONObject object = new JSONObject();
		object.put("tempid", tempid);
		return bind().eq("_id", new ObjectId(ogid)).data(object)
				.update() != null ? 0 : 99;
	}

	@SuppressWarnings("unchecked")
	public int setslevel(String ogid, int slevel) {
		JSONObject object = new JSONObject();
		object.put("slevel", slevel);
		return bind().eq("_id", new ObjectId(ogid)).data(object)
				.update() != null ? 0 : 99;
	}

	public int delete(String[] arr) {
		bind().or();
		for (int i = 0; i < arr.length; i++) {
			bind().eq("_id", new ObjectId(arr[i]));
		}
		return bind().deleteAll() == arr.length ? 0 : 99;
	}

	public boolean check_name(String name) {
		return (name.length() > 0 && name.length() <= 20);
	}

	public String findByFatherid(String fatherid) {
		String name = null;
		JSONArray array = bind().eq("fatherid", fatherid).select();
		for (Object object : array) {
			JSONObject _obj = (JSONObject) object;
			name = _obj.get("name").toString();
		}
		return name;
	}

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
		JSONArray arrays = new JSONArray();
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			if (object.get("tempContent").toString().equals("0")) {
				object.put("tempContent", null);
			} else {
				// String temp = execRequest
				// ._run("GrapeTemplate/TemplateContext/TempFindByTid/s:"
				// + object.get("tempContent").toString(), null)
				// .toString();
				String temp = appsProxy.proxyCall("123.57.214.226:801",
						String.valueOf(appsProxy.appid())
								+ "/19/TemplateContext/TempFindByTid/s:"
								+ object.get("tempContent").toString(),
						null, "").toString();
				object.put("tempContent", temp);
			}
			if (object.get("tempList").toString().equals("0")) {
				object.put("tempList", null);
			} else {
				// String temp = execRequest
				// ._run("GrapeTemplate/TemplateContext/TempFindByTid/s:"
				// + object.get("tempList").toString(), null)
				// .toString();
				String temp = appsProxy.proxyCall("123.57.214.226:801",
						String.valueOf(appsProxy.appid())
								+ "/19/TemplateContext/TempFindByTid/s:"
								+ object.get("tempList").toString(),
						null, "").toString();
				object.put("tempList", temp);
			}
			arrays.add(object);
		}
		return arrays;
	}

	// 获取模版id，合并到jsonarray
	@SuppressWarnings("unchecked")
	private JSONObject join(JSONObject object) {
		if (object.get("tempContent").toString().equals("0")) {
			object.put("tempContent", null);
		} else {
			// String temp = execRequest
			// ._run("GrapeTemplate/TemplateContext/TempFindByTid/s:"
			// + object.get("tempContent").toString(), null)
			// .toString();
			String temp = appsProxy.proxyCall("123.57.214.226:801",
					appsProxy.appid() + "/19/TemplateContext/TempFindByTid/s:"
							+ object.get("tempContent").toString(),
					null, "").toString();
			object.put("tempContent", temp);
		}
		if (object.get("tempList").toString().equals("0")) {
			object.put("tempList", null);
		} else {
			// String temp = execRequest
			// ._run("GrapeTemplate/TemplateContext/TempFindByTid/s:"
			// + object.get("tempList").toString(), null)
			// .toString();
			String temp = appsProxy.proxyCall("123.57.214.226:801",
					appsProxy.appid() + "/19/TemplateContext/TempFindByTid/s:"
							+ object.get("tempList").toString(),
					null, "").toString();
			object.put("tempList", temp);
		}
		return object;
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
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator
						.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONObject object) {
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	private String resultMessage(JSONArray array) {
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
