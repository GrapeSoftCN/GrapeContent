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
import model.ContentGroupModel;
import nlogger.nlogger;

public class ContentGroup {
	private ContentGroupModel group = new ContentGroupModel();
	private HashMap<String, Object> defcol = new HashMap<>();

	public ContentGroup() {

		defcol.put("ownid", 0);
		defcol.put("fatherid", 0);
		defcol.put("sort", 0);
		defcol.put("isvisble", 0);
		defcol.put("slevel", 0);
		defcol.put("tempContent", 0);
		defcol.put("tempList", 0);
		defcol.put("uplv", 2000);
		defcol.put("rplv", 1000);
		defcol.put("dplv", 3000);
	}

	// 新增
	public String GroupInsert(String GroupInfo) {
		// 该用户是否拥有新增权限
		// String tip = execRequest
		// ._run("GrapeAuth/Auth/InsertPLV/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(4, "没有新增权限");
		// }
		JSONObject ginfos = group.AddMap(defcol, JSONHelper.string2json(GroupInfo));
		return group.AddGroup(ginfos);
	}

	// 编辑
	public String GroupEdit(String ogid, String groupInfo) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.UpdateGroup(ogid, JSONHelper.string2json(groupInfo)), "更新内容组数据成功");
	}

	// 删除
	public String GroupDelete(String ogid) {
		// // 获取该条数据的删除，修改，查询权限
		// String dPLV = group.find(ogid).get("dplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/DeletePLV/s:" + dPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(6, "没有删除权限");
		// }
		// 获取该栏目下的所有子栏目
		long code = -1;
		int codes = -1;
		List<String> list = new ArrayList<>();
		JSONArray arrays = group.getColumn(ogid);
		if (arrays != null) {
			try {
				while (arrays.size() > 0) {
					List<String> temp = new ArrayList<>();
					temp = group.getList(arrays);
					arrays = group.getColumn(StringHelper.join(list));
					list.addAll(temp);
				}
				list.add(ogid);
				String tips = appsProxy
						.proxyCall(group.getHost(0),
								appsProxy.appid() + "/15/content/SetGroupBatch/s:" + StringHelper.join(list), null, "")
						.toString();
				if (tips != null && !tips.equals("")) {
					code = (long) JSONHelper.string2json(tips).get("errorcode");
					codes = Integer.parseInt(String.valueOf(code));
					if (codes == 0) {
						if (list.size() > 1) {
							codes = group.delete(StringHelper.join(list).split(","));
						} else {
							codes = group.DeleteGroup(StringHelper.join(list));
						}
					}
				}
			} catch (Exception e) {
				codes = -1;
				nlogger.logout(e);
			}
		}
		return codes == -1 ? "" : group.resultMessage(codes, "栏目删除成功");
	}

	public String FindByType(String type, int no) {
		return group.findByType(type, no);
	}

	public String GroupFind(String groupinfo) {
		return group.resultMessage(group.select(groupinfo));
	}

	// 设置排序值
	public String GroupSort(String ogid, int num) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setsort(ogid, num), "顺序调整成功");
	}

	// 分页
	public String GroupPage(int idx, int pageSize) {
		return group.page(idx, pageSize);
	}

	// 条件分页
	public String GroupPageBy(int idx, int pageSize, String GroupInfo) {
		return group.page(idx, pageSize, JSONHelper.string2json(GroupInfo));
	}

	// 设置密级
	public String GroupSlevel(String ogid, int slevel) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setslevel(ogid, slevel), "密级更新成功");
	}

	// 设置模版
	public String GroupSetTemp(String ogid, String tempid) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setTempId(ogid, tempid), "更新模版成功");
	}

	// 设置上级栏目
	public String GroupSetFatherid(String ogid, int fatherid) {
		// String uPLV = group.find(ogid).get("uplv").toString();
		// String tip = execRequest._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV
		// + "/s:" + userId, null).toString();
		// if (!"0".equals(tip)) {
		// return group.resultMessage(5, "没有编辑权限");
		// }
		return group.resultMessage(group.setfatherid(ogid, fatherid), "成功设置上级栏目");
	}

	// 批量删除
	public String GroupBatchDelete(String ogid) {
		return group.resultMessage(group.delete(ogid.split(",")), "删除成功");
	}

	// 获取下级栏目名称
	public String getColumnByFid(String ogid) {
		return group.getColumnByFid(ogid);
	}

	// 获得上级栏目id，name，fatherid
	public String getPrevCol(String ogid) {
		JSONObject rs = group.find(ogid);
		return rs != null ? rs.toString() : "";
	}

	// 获取当前文章所在栏目位置
	@SuppressWarnings("unchecked")
	public String getPreColumn(String ogid) {
		List<JSONObject> list = null;
		try {
			list = new ArrayList<>();
			JSONObject object = group.find(ogid);
			JSONObject obj = new JSONObject();

			String fatherid = object.get("fatherid").toString();
			JSONObject objID = (JSONObject) object.get("_id");
			obj.put("_id", objID.get("$oid").toString());
			obj.put("name", object.get("name").toString());
			list.add(obj);
			while (!"0".equals(fatherid)) {
				String prevCol = getPrevCol(fatherid);
				fatherid = JSONHelper.string2json(prevCol).get("fatherid").toString();
				list = group.getName(list, JSONHelper.string2json(prevCol));

			}
			Collections.reverse(list); // list倒序排列
		} catch (Exception e) {
			nlogger.logout(e);
			list = null;
		}
		return list != null ? group.resultMessage(JSONHelper.string2array(list.toString())) : "";
	}

	// 设置栏目管理员 userid为用户表 _id
	public String setManage(String ogid, String userid) {
		return group.setColumManage(ogid, userid);
	}

}
