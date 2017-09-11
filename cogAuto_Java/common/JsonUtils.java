package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class JsonUtils {

	public static List testJsonArray(String str) {
		JSONArray jsonArr = JSONArray.fromObject(str);
		List list = new ArrayList();
		for (Object json : jsonArr) {
			String jsonStr = json.toString();
			if (isString(jsonStr)) {
				list.add(jsonStr);
			} else if (isJson(jsonStr)) {
				list.add(testJson(jsonStr.toString()));
			} else if (isJsonArray(jsonStr)) {
				list.add(testJsonArray(jsonStr.toString()));
			}
		}
		return list;
	}

	public static Map testJson(String str) {
		JSONObject json = JSONObject.fromObject(str);
		Iterator<?> it = json.keySet().iterator();
		Map map = new HashMap();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = json.getString(key);
			if (isString(value)) {
				map.put(key, value);
			} else if (isJson(value)) {
				map.put(key, testJson(value));
			} else if (isJsonArray(value)) {
				map.put(key, testJsonArray(value));
			}
		}
		return map;
	}

	public static boolean isJson(String s) {
		boolean flag = true;
		try {
			JSONObject.fromObject(s);
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public static boolean isJsonArray(String s) {
		boolean flag = true;
		try {
			JSONArray.fromObject(s);
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public static boolean isString(String s) {
		return !isJson(s) && !isJsonArray(s);
	}


}
