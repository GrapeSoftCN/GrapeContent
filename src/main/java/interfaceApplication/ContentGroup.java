package interfaceApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import json.JSONHelper;
import model.ContentGroupModel;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;

public class ContentGroup {
	private ContentGroupModel group = new ContentGroupModel();
	private HashMap<String, Object> defcol = new HashMap<>();
	private static session session = new session();

	public ContentGroup() {
		JSONObject userInfo = new JSONObject();
		String sid = (String) execRequest.getChannelValue("sid");
		if (sid != null) {
			userInfo = session.getSession(sid);
		}
		String ownid = (userInfo != null && userInfo.size() != 0) ? ((JSONObject) userInfo.get("_id")).getString("$oid")
				: "";
		defcol.put("ownid", ownid);
		defcol.put("fatherid", "0");
		defcol.put("sort", 0);
		defcol.put("isvisble", 0);
		defcol.put("slevel", 0);
		defcol.put("type", 0);
		defcol.put("tempContent", "0");
		defcol.put("tempList", "0");
		defcol.put("contentType", "0"); // 该栏目下文章的类型
		defcol.put("fixed", "0"); // 是否为固定栏目，即所有子网站都显示的栏目 0：非固定栏目；1：固定栏目
		defcol.put("u", 2000);
		defcol.put("r", 1000);
		defcol.put("d", 3000);
		defcol.put("wbid", (userInfo != null && userInfo.size() != 0) ? userInfo.get("currentWeb") : "");
	}

	// 新增
	public String GroupInsert(String GroupInfo) {
		JSONObject ginfos = group.AddMap(defcol, JSONHelper.string2json(GroupInfo));
		return group.AddGroup(ginfos);
	}

	// 编辑
	public String GroupEdit(String ogid, String groupInfo) {
		return group.resultMessage(group.UpdateGroup(ogid, JSONHelper.string2json(groupInfo)), "更新内容组数据成功");
	}

	// 删除
	public String GroupDelete(String ogid) {
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
								appsProxy.appid() + "/15/Content/SetGroupBatch/s:" + StringHelper.join(list), null, "")
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
		return group.resultMessage(group.setslevel(ogid, slevel), "密级更新成功");
	}

	// 设置模版
	public String GroupSetTemp(String ogid, String tempid) {
		return group.resultMessage(group.setTempId(ogid, tempid), "更新模版成功");
	}

	// 设置上级栏目
	public String GroupSetFatherid(String ogid, int fatherid) {
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

	// 获取当前文章所在栏目位置
	public String getPrevCol(String ogid) {
		List<JSONObject> list = new ArrayList<>();
		JSONArray array = null;
		try {
			array = new JSONArray();
			list = getPrevCol(list, ogid);
			Collections.reverse(list); // list倒序排列
			array = JSONHelper.string2array(list.toString());
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return group.resultMessage(array);
	}

	// 获得上级栏目id，name，fatherid
	private List<JSONObject> getPrevCol(List<JSONObject> list, String ogid) {
		JSONObject rs = null;
		String fatherid;
		if (!ogid.contains("$numberLong") && !("0").equals(ogid)) {
			rs = new JSONObject();
			rs = group.findWeb(ogid);
			list = group.getName(list, rs);
			fatherid = (rs != null && rs.size() != 0) ? rs.getString("fatherid") : "0";
			list = getPrevCol(list, fatherid);
		}
		return list;
	}

	// 设置栏目管理员 userid为用户表 _id
	public String setManage(String ogid, String userid) {
		return group.setColumManage(ogid, userid);
	}

	// 设置栏目编辑 userid为用户表 _id !!
	public String setEditer(String ogid, String userid) {
		return group.setColumManage(ogid, userid);
	}

	// 获取某网站下的栏目
	public String getPrevColumn(String wbid) {
		JSONArray array = group.getColumnByWbid(wbid);
		return array != null ? array.toString() : "";
	}

	public String getGroupById(String ogid) {
		JSONObject object = group.find(ogid);
		return group.resultMessage(object);
	}

	public String getGroupByIds(String ogid) {
		JSONObject object = group.find(ogid);
		return object != null ? object.toString() : "";
	}

	// 获取该栏目下，栏目管理员
	public String getManagerByOgid(String ogid) {
		JSONObject object = group.findOwnid(ogid);
		return object != null ? object.toString() : "";
	}

	// 根据用户id，网站id，获取栏目id,name
	public String getColumnId(String condString) {
		JSONArray array = group.findColumn(condString);
		return (array != null && array.size() != 0) ? array.toString() : "";
	}
}
