package model;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
public class WsCount {
	private ContentModel content = new ContentModel();
	
	@SuppressWarnings("unchecked")
	public JSONObject getChannleCount(JSONObject robj, String wbID, String rootName, String fatherID){
		JSONArray channelArray = JSONArray.toJSONArray( (String) appsProxy.proxyCall("/ContentGroup/getPrevColumn/s:" + wbID, null, null) );
		
		for(Object _obj : channelArray){
			//json = (JSONObject)_obj;
			
		}
		return robj;
	}
	//线性jsonarray转树形jsonobjct
	@SuppressWarnings("unchecked")
	private JSONObject line2tree(JSONArray array,String mid,String fid){
		JSONObject json;
		JSONObject rjson = new JSONObject();
		for(Object _obj : array){
			json = (JSONObject)_obj;
			if( json.get(fid).toString().isEmpty() ){
				rjson.put(json.get(mid).toString(), null);
			}
		}
		JSONObject njson;
		for(Object rootID : rjson.keySet()){
			njson = new JSONObject();
			for(Object _obj : array){
				json = (JSONObject)_obj;
				if( json.get(fid).toString().equals(rootID) ){//找到父ID为当前根目录的栏目
					
				}
			}
		}
		return rjson;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getAllCount(JSONObject robj, String rootID, String rootName, String fatherID) {
		// appsProxy.
		String[] trees = null;
		JSONObject nObj = new JSONObject();
		//String[] Allweb = getCid(rootID);
		long allCnt = getCount(rootID);
		long argCnt = getAgreeCount(rootID);
		long disArg = getDisagreeCount(rootID);
		long chking = allCnt - argCnt - disArg;
		nObj.put("id", rootID);
		nObj.put("fatherid", fatherID);
		nObj.put("name", rootName);
		String tree = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getChildrenweb/s:" + rootID, null, null);
		if (!tree.equals("")) {
			trees = tree.split(",");
		}
		JSONObject newJSON = new JSONObject();
		if (trees != null) {
			JSONObject webInfos = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + tree, null, null));
			int l = trees.length;
			for (int i = 0; i < l; i++) {
				// newJSON.put(trees[i], webInfos.getString(trees[i]) );//填充网站姓名
				getAllCount(newJSON, trees[i], webInfos.getString(trees[i]), rootID);
			}
		}
		/**/
		JSONArray jsonArray = new JSONArray();
		JSONObject json;
		
		for( Object obj : newJSON.keySet()){
			//修正总量，设置排序
			try{
				json = (JSONObject) newJSON.get(obj) ;
				allCnt += json.getLong("count");
				argCnt += json.getLong("checked");
				disArg += json.getLong("uncheck");
				chking += json.getLong("checking");
				jsonArray.add(json);
			}
			catch(Exception e){
				e.getMessage();
			}
		}
			
		sortJson(jsonArray, 0 , jsonArray.size() - 1, "count");
		nObj.put("count", allCnt);
		nObj.put("checked", argCnt);
		nObj.put("uncheck", disArg);
		nObj.put("checking", chking);
		
		nObj.put("children", jsonArray);
		
		robj.put(rootID, nObj);
		return robj;
	}
	
	@SuppressWarnings("unchecked")
	public static int partition(JSONArray array,int lo,int hi ,String keyName){
        //固定的切分方式
        long key= ((JSONObject)array.get(lo)).getLong(keyName);
        while(lo<hi){
            while(((JSONObject)array.get(hi)).getLong(keyName)>=key&&hi>lo){//从后半部分向前扫描
                hi--;
            }
            ((JSONObject)array.get(lo)).put(keyName, ((JSONObject)array.get(hi)).getLong(keyName));
            while(((JSONObject)array.get(lo)).getLong(keyName)<=key&&hi>lo){//从前半部分向后扫描
                lo++;
            }
            ((JSONObject)array.get(hi)).put(keyName, ((JSONObject)array.get(lo)).getLong(keyName));
        }
        ((JSONObject)array.get(hi)).put(keyName, key);
        return hi;
    }
    
    public static void sortJson(JSONArray array,int lo ,int hi,String keyName){
        if(lo>=hi){
            return ;
        }
        int index=partition(array,lo,hi,keyName);
        sortJson(array,lo,index-1, keyName);
        sortJson(array,index+1,hi, keyName); 
    }
    /*
	@SuppressWarnings("unchecked")
	private void sortJson(JSONArray array,int low, int hight, String key){
		JSONObject json;
		int i, j;
		long index;
		if (low > hight) {
            return;
        }
        i = low;
        j = hight;
        index = ((JSONObject)array.get(i)).getLong(key); // 用子表的第一个记录做基准
        while (i < j) { // 从表的两端交替向中间扫描
            while (i < j && ((JSONObject)array.get(j)).getLong(key) >= index)
                j--;
            
            if (i < j){// 用比基准小的记录替换低位记录
            	json = (JSONObject)array.get(i++);
            	json.put(key, ((JSONObject)array.get(j)).getLong(key) );
            	array.set(i++, json);
            }
            while (i < j && ((JSONObject)array.get(i)).getLong(key) < index)
                i++;
            
            if (i < j){ // 用比基准大的记录替换高位记录
            	json = (JSONObject)array.get(j--);
            	json.put(key, ((JSONObject)array.get(i)).getLong(key) );
            	array.set(j--, json);
            }
        }
        
        json = (JSONObject)array.get(i);
        json.put(key, index );
        array.set(i, json);
        
        sortJson(array, low, i - 1,key); // 对低子表进行递归排序
        sortJson(array, i + 1, hight, key); // 对高子表进行递归排序
	}
	*/
	private long getCount(String wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();
			count = db.eq("wbid", wid).count();
		}
		return count;
	}

	private long getAgreeCount(String wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();
			count = db.and().eq("wbid", wid).eq("state", 2).count();
		}
		return count;
	}

	private long getDisagreeCount(String wid) {
		long count = 0;
		if (wid != null) {
			db db = content.bind();
			count = db.and().eq("wbid", wid).eq("state", 1).count();
		}
		return count;
	}

	// 获取网站id，包含自身网站id
	private String[] getCid(String wid) {
		String[] trees = null;
		if (wid != null && !wid.equals("")) {
			// 获取子站点
			wid = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/s:" + wid, null, null);
			if (!wid.equals("")) {
				trees = wid.split(",");
			}
		}
		return trees;
	}
}
